#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WEBUI_DIR="$ROOT_DIR/webui"

start_in_terminal() {
  local title="$1"
  local command_text="$2"

  if command -v gnome-terminal >/dev/null 2>&1; then
    gnome-terminal --title="$title" -- bash -lc "$command_text; exec bash"
    return
  fi

  if command -v x-terminal-emulator >/dev/null 2>&1; then
    x-terminal-emulator -T "$title" -e bash -lc "$command_text; exec bash" &
    return
  fi

  if command -v xterm >/dev/null 2>&1; then
    xterm -T "$title" -e bash -lc "$command_text; exec bash" &
    return
  fi

  echo "No supported terminal emulator found. Please install gnome-terminal or xterm." >&2
  exit 1
}

if [[ ! -d "$WEBUI_DIR" ]]; then
  echo "Frontend directory not found: $WEBUI_DIR" >&2
  exit 1
fi

if ! command -v npm >/dev/null 2>&1; then
  echo "npm is not installed or not in PATH." >&2
  exit 1
fi

if command -v mvn >/dev/null 2>&1; then
  BACKEND_CMD="cd \"$ROOT_DIR\" && mvn spring-boot:run"
elif [[ -x "$ROOT_DIR/mvnw" ]]; then
  BACKEND_CMD="cd \"$ROOT_DIR\" && ./mvnw spring-boot:run"
else
  echo "Neither mvn nor executable ./mvnw is available." >&2
  exit 1
fi

FRONTEND_CMD="cd \"$WEBUI_DIR\" && npm run dev"

start_in_terminal "backend-dev" "$BACKEND_CMD"
echo "backend success"

sleep 3

start_in_terminal "frontend-dev" "$FRONTEND_CMD"
echo "frontend success"
