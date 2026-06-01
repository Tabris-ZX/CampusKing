#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WEBUI_DIR="$ROOT_DIR/webui"
LOG_DIR="/home/zx/work/cpk/logs"

mkdir -p "$LOG_DIR"

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

free_port 8080 "backend"
free_port 5173 "frontend"

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
    if curl -sf http://localhost:8080 >/dev/null 2>&1; then
        echo "[+] Backend is ready!"
        break
    fi
    sleep 1
done

echo "[*] Starting frontend..."
cd "$WEBUI_DIR" && npm run dev > "$LOG_DIR/frontend.log" 2>&1 &
FRONTEND_PID=$!
echo "[+] Frontend PID: $FRONTEND_PID"
echo "[+] Frontend dev server started. Check $LOG_DIR/frontend.log for the actual port."

echo ""
echo "========== Services Running =========="
echo "Backend:   PID=$BACKEND_PID  log=$LOG_DIR/backend.log"
echo "Frontend:  PID=$FRONTEND_PID log=$LOG_DIR/frontend.log"
echo "Press Ctrl+C to stop all services"
echo "======================================"

wait -n 2>/dev/null
