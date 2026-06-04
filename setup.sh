#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WEBUI_DIR="$ROOT_DIR/webui"
LOG_DIR="/home/zx/work/cpk/logs"
CONFIG_FILE="${CAMPUSKING_WEBUI_CONFIG:-$ROOT_DIR/config/config.yaml}"

mkdir -p "$LOG_DIR"

config_value() {
    local section="$1"
    local key="$2"
    local fallback="$3"

    if [[ ! -f "$CONFIG_FILE" ]]; then
        echo "$fallback"
        return
    fi

    awk -v section="$section" -v key="$key" -v fallback="$fallback" '
        function trim(value) {
            sub(/^[[:space:]]+/, "", value)
            sub(/[[:space:]]+$/, "", value)
            return value
        }
        function clean(value) {
            value = trim(value)
            sub(/[[:space:]]+#.*$/, "", value)
            value = trim(value)
            if ((substr(value, 1, 1) == "\"" && substr(value, length(value), 1) == "\"") ||
                (substr(value, 1, 1) == "'"'"'" && substr(value, length(value), 1) == "'"'"'")) {
                value = substr(value, 2, length(value) - 2)
            }
            return value
        }
        /^[[:space:]]*#/ || /^[[:space:]]*$/ {
            next
        }
        /^[^[:space:]][^:]*:[[:space:]]*$/ {
            current = trim(substr($0, 1, index($0, ":") - 1))
            next
        }
        {
            line = $0
            indent = match(line, /[^[:space:]]/) - 1
            separator = index(line, ":")
            if (separator <= 0) {
                next
            }
            name = trim(substr(line, 1, separator - 1))
            value = clean(substr(line, separator + 1))
            if (indent > 0 && current == section && name == key) {
                print value
                found = 1
                exit
            }
            if (indent == 0 && section == "" && name == key) {
                print value
                found = 1
                exit
            }
        }
        END {
            if (!found) {
                print fallback
            }
        }
    ' "$CONFIG_FILE"
}

BACKEND_PORT="$(config_value server backendPort 8080)"
FRONTEND_PORT="$(config_value server frontendPort 5173)"

validate_port() {
    local name="$1"
    local port="$2"
    if [[ ! "$port" =~ ^[0-9]+$ ]] || (( port < 1 || port > 65535 )); then
        echo "Invalid $name in $CONFIG_FILE: $port" >&2
        exit 1
    fi
}

validate_port "server.backendPort" "$BACKEND_PORT"
validate_port "server.frontendPort" "$FRONTEND_PORT"

find_port_pids() {
    local port="$1"
    if command -v lsof >/dev/null 2>&1; then
        lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true
        return
    fi
    if command -v fuser >/dev/null 2>&1; then
        fuser "$port"/tcp 2>/dev/null || true
        return
    fi
}

free_port() {
    local port="$1"
    local service_name="$2"
    local pids
    pids="$(find_port_pids "$port")"
    if [[ -z "${pids// }" ]]; then
        return
    fi

    echo "[*] Port $port is occupied by: $pids"
    echo "[*] Stopping existing $service_name process on port $port..."
    kill $pids 2>/dev/null || true

    for _ in $(seq 1 10); do
        sleep 1
        pids="$(find_port_pids "$port")"
        if [[ -z "${pids// }" ]]; then
            echo "[+] Port $port is free now."
            return
        fi
    done

    echo "[*] Port $port still busy, forcing stop..."
    kill -9 $pids 2>/dev/null || true
    sleep 1
    pids="$(find_port_pids "$port")"
    if [[ -n "${pids// }" ]]; then
        echo "Failed to free port $port. Remaining PID(s): $pids" >&2
        exit 1
    fi
    echo "[+] Port $port is free now."
}

cleanup() {
    echo ""
    echo "Shutting down..."
    [[ -n "${BACKEND_PID:-}" ]] && kill "$BACKEND_PID" 2>/dev/null
    [[ -n "${FRONTEND_PID:-}" ]] && kill "$FRONTEND_PID" 2>/dev/null
    wait 2>/dev/null
    echo "All services stopped."
}

trap cleanup EXIT INT TERM

if [[ ! -d "$WEBUI_DIR" ]]; then
    echo "Frontend directory not found: $WEBUI_DIR" >&2
    exit 1
fi

if ! command -v npm >/dev/null 2>&1; then
    echo "npm is not installed or not in PATH." >&2
    exit 1
fi

echo "[*] Using backend port $BACKEND_PORT and frontend port $FRONTEND_PORT from $CONFIG_FILE"

free_port "$BACKEND_PORT" "backend"
free_port "$FRONTEND_PORT" "frontend"

echo "[*] Converting source images to WebP..."
bash "$ROOT_DIR/scripts/png-to-webp.sh"

echo "[*] Starting backend..."
if command -v mvn >/dev/null 2>&1; then
    cd "$ROOT_DIR" && mvn spring-boot:run > "$LOG_DIR/backend.log" 2>&1 &
elif [[ -x "$ROOT_DIR/mvnw" ]]; then
    cd "$ROOT_DIR" && ./mvnw spring-boot:run > "$LOG_DIR/backend.log" 2>&1 &
else
    echo "Neither mvn nor ./mvnw available." >&2
    exit 1
fi
BACKEND_PID=$!
echo "[+] Backend PID: $BACKEND_PID"

echo "[*] Waiting for backend to start..."
for i in $(seq 1 30); do
    if curl -sf "http://localhost:$BACKEND_PORT" >/dev/null 2>&1; then
        echo "[+] Backend is ready!"
        break
    fi
    sleep 1
done

echo "[*] Starting frontend..."
cd "$WEBUI_DIR" && npm run dev > "$LOG_DIR/frontend.log" 2>&1 &
FRONTEND_PID=$!
echo "[+] Frontend PID: $FRONTEND_PID"
echo "[+] Frontend dev server started on port $FRONTEND_PORT. "
echo "All Successful!"
echo ""
echo "========== Services Running =========="

wait -n 2>/dev/null
