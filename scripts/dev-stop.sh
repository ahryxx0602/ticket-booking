#!/usr/bin/env bash
# ==============================================================================
# TICKET BOOKING SYSTEM - SAFE DEVELOPMENT SHUTDOWN SCRIPT
# ==============================================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_DIR="$ROOT_DIR/.dev-pids"
TARGET_COMPONENT="${1:-all}"
STOP_INFRA="${2:-false}"

log_info() { printf "\033[1;34m[STOP]\033[0m  %s\n" "$*"; }
log_ok()   { printf "\033[1;32m[OK]\033[0m    %s\n" "$*"; }
log_warn() { printf "\033[1;33m[WARN]\033[0m  %s\n" "$*"; }

stop_pid_file() {
  local name="$1"
  local pid_file="$PID_DIR/${name}.pid"

  if [[ ! -f "$pid_file" ]]; then
    return 0
  fi

  local pid
  pid="$(cat "$pid_file" 2>/dev/null || true)"
  if [[ -z "$pid" ]] || ! [[ "$pid" =~ ^[0-9]+$ ]]; then
    rm -f "$pid_file"
    return 0
  fi

  if ! kill -0 "$pid" 2>/dev/null; then
    rm -f "$pid_file"
    return 0
  fi

  # Verify the process actually belongs to this repository/runtime (prevents recycled PID kill)
  local cmdline=""
  if [[ -r "/proc/$pid/cmdline" ]]; then
    cmdline="$(tr '\0' ' ' < "/proc/$pid/cmdline" 2>/dev/null || true)"
  fi

  if [[ -n "$cmdline" ]] && ! echo "$cmdline" | grep -qE "TicketApplication|ticket-booking|vite|dev\.sh|bash"; then
    log_warn "PID $pid ($cmdline) does not belong to ticket-booking. Cleaning stale PID file without signaling."
    rm -f "$pid_file"
    return 0
  fi

  log_info "Stopping $name (PID $pid)..."

  # Terminate child processes of this PID first
  pkill -TERM -P "$pid" 2>/dev/null || true
  kill -TERM "$pid" 2>/dev/null || true

  local waited=0
  while kill -0 "$pid" 2>/dev/null && [[ "$waited" -lt 10 ]]; do
    sleep 1
    waited=$((waited + 1))
  done

  if kill -0 "$pid" 2>/dev/null; then
    log_warn "$name (PID $pid) did not exit after 10s; sending SIGKILL..."
    pkill -KILL -P "$pid" 2>/dev/null || true
    kill -KILL "$pid" 2>/dev/null || true
  fi

  rm -f "$pid_file"
  log_ok "Stopped $name (PID $pid)."
}

mkdir -p "$PID_DIR"

case "$TARGET_COMPONENT" in
  backend)
    stop_pid_file "backend-watcher"
    stop_pid_file "backend"
    ;;
  frontend)
    stop_pid_file "frontend"
    ;;
  --with-infra)
    stop_pid_file "backend-watcher"
    stop_pid_file "backend"
    stop_pid_file "frontend"
    STOP_INFRA="true"
    ;;
  all|*)
    stop_pid_file "backend-watcher"
    stop_pid_file "backend"
    stop_pid_file "frontend"
    ;;
esac

if [[ "$STOP_INFRA" == "--with-infra" ]] || [[ "$STOP_INFRA" == "true" ]]; then
  if command -v docker >/dev/null 2>&1; then
    log_info "Stopping Docker development infrastructure (preserving volumes)..."
    docker compose -f "$ROOT_DIR/docker-compose.dev.yml" stop
    log_ok "Docker development infrastructure stopped (volumes preserved)."
  fi
fi

log_ok "Development processes ($TARGET_COMPONENT) stopped safely."
