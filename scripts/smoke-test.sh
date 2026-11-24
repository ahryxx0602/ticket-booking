#!/usr/bin/env bash
# ==============================================================================
# TICKET BOOKING SYSTEM - DEPLOYMENT & HYBRID DEV SMOKE TEST
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

BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-5173}"
WAIT_SECONDS="${1:-45}"

log_info()  { printf "\033[1;34m[SMOKE]\033[0m %s\n" "$*"; }
log_ok()    { printf "\033[1;32m[PASS]\033[0m  %s\n" "$*"; }
log_error() { printf "\033[1;31m[FAIL]\033[0m  %s\n" "$*" >&2; }

wait_for_url() {
  local name="$1"
  local url="$2"
  local elapsed=0
  while [[ "$elapsed" -lt "$WAIT_SECONDS" ]]; do
    if curl -fsS --max-time 3 "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
  log_error "$name did not respond at $url within ${WAIT_SECONDS}s"
  return 1
}

echo "=============================================================================="
echo "  TICKET BOOKING SYSTEM - SMOKE TEST SUITE"
echo "=============================================================================="

# 1. Infrastructure check
"$ROOT_DIR/scripts/healthcheck.sh" --infra-only

# 2. Backend Actuator Health
BACKEND_HEALTH_URL="http://127.0.0.1:${BACKEND_PORT}/actuator/health"
wait_for_url "Backend Actuator" "$BACKEND_HEALTH_URL"
health_json="$(curl -fsS "$BACKEND_HEALTH_URL")"
if echo "$health_json" | grep -q '"status":"UP"'; then
  log_ok "Backend Actuator health is UP"
else
  log_error "Backend Actuator returned unexpected payload: $health_json"
  exit 1
fi

# 3. Backend Public Catalog API
CATALOG_URL="http://127.0.0.1:${BACKEND_PORT}/api/v1/catalog/trips"
catalog_json="$(curl -fsS "$CATALOG_URL")"
if echo "$catalog_json" | grep -q '"success":true'; then
  log_ok "Backend Public Catalog API (/api/v1/catalog/trips) responded with success=true"
else
  log_error "Backend Catalog API check failed: $catalog_json"
  exit 1
fi

# 4. Frontend & Proxy Connectivity
FRONTEND_URL="http://127.0.0.1:${FRONTEND_PORT}"
wait_for_url "Frontend Server" "$FRONTEND_URL"
frontend_html="$(curl -fsS "$FRONTEND_URL")"
if echo "$frontend_html" | grep -qi "<!DOCTYPE html>"; then
  log_ok "Frontend index.html served properly on port ${FRONTEND_PORT}"
else
  log_error "Frontend did not return valid HTML"
  exit 1
fi

PROXY_URL="http://127.0.0.1:${FRONTEND_PORT}/api/v1/catalog/trips"
proxy_json="$(curl -fsS "$PROXY_URL")"
if echo "$proxy_json" | grep -q '"success":true'; then
  log_ok "Frontend -> Backend API Proxy ($PROXY_URL) verified end-to-end"
else
  log_error "Frontend API Proxy check failed: $proxy_json"
  exit 1
fi

echo "------------------------------------------------------------------------------"
log_ok "ALL SMOKE TESTS PASSED!"
