#!/usr/bin/env bash
# ==============================================================================
# TICKET BOOKING SYSTEM - HEALTHCHECK SCRIPT
# ==============================================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ -f "$ROOT_DIR/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT_DIR/.env"
  set +a
fi

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3307}"
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-5173}"

MODE="${1:---all}"
WAIT_SECONDS="${2:-60}"

log_info()  { printf "\033[1;34m[HEALTH]\033[0m %s\n" "$*"; }
log_ok()    { printf "\033[1;32m[UP]\033[0m     %s\n" "$*"; }
log_warn()  { printf "\033[1;33m[DOWN]\033[0m   %s\n" "$*"; }
log_error() { printf "\033[1;31m[ERROR]\033[0m  %s\n" "$*" >&2; }

check_tcp_port() {
  local host="$1"
  local port="$2"
  (echo >/dev/tcp/"$host"/"$port") >/dev/null 2>&1
}

check_mysql() {
  if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' 2>/dev/null | grep -q '^ticket-mysql$'; then
    local status
    status="$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' ticket-mysql 2>/dev/null || echo "unknown")"
    if [[ "$status" == "healthy" ]]; then
      log_ok "MySQL container (ticket-mysql) is healthy on ${MYSQL_HOST}:${MYSQL_PORT}"
      return 0
    fi
    log_warn "MySQL container (ticket-mysql) status is '${status}' at ${MYSQL_HOST}:${MYSQL_PORT}"
    return 1
  fi
  if check_tcp_port "$MYSQL_HOST" "$MYSQL_PORT"; then
    log_ok "MySQL TCP port is reachable at ${MYSQL_HOST}:${MYSQL_PORT}"
    return 0
  fi
  log_warn "MySQL is not ready at ${MYSQL_HOST}:${MYSQL_PORT}"
  return 1
}

check_redis() {
  if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' 2>/dev/null | grep -q '^ticket-redis$'; then
    local status
    status="$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' ticket-redis 2>/dev/null || echo "unknown")"
    if [[ "$status" == "healthy" ]]; then
      log_ok "Redis container (ticket-redis) is healthy on ${REDIS_HOST}:${REDIS_PORT}"
      return 0
    fi
    log_warn "Redis container (ticket-redis) status is '${status}' at ${REDIS_HOST}:${REDIS_PORT}"
    return 1
  fi
  if check_tcp_port "$REDIS_HOST" "$REDIS_PORT"; then
    log_ok "Redis TCP port is reachable at ${REDIS_HOST}:${REDIS_PORT}"
    return 0
  fi
  log_warn "Redis is not ready at ${REDIS_HOST}:${REDIS_PORT}"
  return 1
}

check_backend() {
  local dev_url="http://127.0.0.1:${BACKEND_PORT}/actuator/health"
  local prod_url="http://127.0.0.1:${BACKEND_PORT}/internal/actuator/health"
  local resp
  for url in "$dev_url" "$prod_url"; do
    if resp="$(curl -fsS --max-time 3 "$url" 2>/dev/null)"; then
      if echo "$resp" | grep -q '"status":"UP"'; then
        log_ok "Backend Spring Boot is UP ($url)"
        return 0
      fi
    fi
  done
  log_warn "Backend is not ready ($dev_url or $prod_url)"
  return 1
}

check_frontend() {
  local url="http://127.0.0.1:${FRONTEND_PORT}"
  if curl -fsS --max-time 3 "$url" >/dev/null 2>&1; then
    log_ok "Frontend Server is UP ($url)"
    return 0
  fi
  log_warn "Frontend is not ready ($url)"
  return 1
}

wait_for_infra() {
  local elapsed=0
  log_info "Waiting up to ${WAIT_SECONDS}s for MySQL (${MYSQL_PORT}) and Redis (${REDIS_PORT})..."
  while [[ "$elapsed" -lt "$WAIT_SECONDS" ]]; do
    if check_mysql >/dev/null 2>&1 && check_redis >/dev/null 2>&1; then
      check_mysql
      check_redis
      return 0
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
  log_error "Infrastructure failed to become healthy within ${WAIT_SECONDS}s."
  check_mysql || true
  check_redis || true
  return 1
}

case "$MODE" in
  --wait-infra)
    wait_for_infra
    ;;
  --infra-only)
    rc=0
    check_mysql || rc=1
    check_redis || rc=1
    exit "$rc"
    ;;
  --backend-only)
    check_backend
    ;;
  --frontend-only)
    check_frontend
    ;;
  --all|*)
    rc=0
    check_mysql || rc=1
    check_redis || rc=1
    check_backend || rc=1
    check_frontend || rc=1
    exit "$rc"
    ;;
esac
