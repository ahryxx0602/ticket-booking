#!/usr/bin/env bash
# ==============================================================================
# TICKET BOOKING SYSTEM - HYBRID DEVELOPMENT RUNNER & MULTI-MODULE AUTO-COMPILER
# ==============================================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_DIR="$ROOT_DIR/.dev-pids"
LOG_DIR="$ROOT_DIR/.dev-logs"
mkdir -p "$PID_DIR" "$LOG_DIR"

cd "$ROOT_DIR"

if [[ -f "$ROOT_DIR/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT_DIR/.env"
  set +a
fi

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"
export MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
export MYSQL_PORT="${MYSQL_PORT:-3307}"
export MYSQL_DATABASE="${MYSQL_DATABASE:-ticket_booking_db}"
export MYSQL_USER="${MYSQL_USER:-ticket_user}"
export MYSQL_PASSWORD="${MYSQL_PASSWORD:-ticket_password}"
export REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
export REDIS_PORT="${REDIS_PORT:-6379}"
export BACKEND_PORT="${BACKEND_PORT:-8080}"
export FRONTEND_PORT="${FRONTEND_PORT:-5173}"

TARGET_COMPONENT="${1:-all}"
FOREGROUND="${2:---foreground}"

log_info()  { printf "\033[1;34m[DEV]\033[0m   %s\n" "$*"; }
log_ok()    { printf "\033[1;32m[OK]\033[0m    %s\n" "$*"; }
log_warn()  { printf "\033[1;33m[WARN]\033[0m  %s\n" "$*"; }
log_error() { printf "\033[1;31m[ERROR]\033[0m %s\n" "$*" >&2; }

is_pid_running() {
  local pid_file="$1"
  if [[ -f "$pid_file" ]]; then
    local pid
    pid="$(cat "$pid_file" 2>/dev/null || true)"
    if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
      return 0
    fi
    rm -f "$pid_file"
  fi
  return 1
}

get_port_owner_pid() {
  local port="$1"
  ss -ltnp 2>/dev/null | awk -v p=":$port" '$4 ~ p"$" { match($0, /pid=[0-9]+/); if (RSTART) print substr($0, RSTART+4, RLENGTH-4) }' | head -n 1
}

build_backend_reactor_classpath() {
  local cp_cache="$LOG_DIR/maven-deps.classpath"
  local pom_hash_file="$LOG_DIR/poms.sha256"
  local current_hash
  current_hash="$(find "$ROOT_DIR/backend" -name "pom.xml" -print0 | sort -z | xargs -0 sha256sum | sha256sum | awk '{print $1}')"

  # Ensure all workspace modules are up-to-date via fast incremental compile
  log_info "Performing fast incremental compile of backend workspace..." >&2
  (cd "$ROOT_DIR/backend" && mvn compile -T 1C -q)

  # Cache external Maven dependency classpath across runs unless pom.xml changes
  if [[ ! -f "$cp_cache" ]] || [[ ! -f "$pom_hash_file" ]] || [[ "$(cat "$pom_hash_file")" != "$current_hash" ]]; then
    log_info "Resolving Maven external dependency classpath across reactor..." >&2
    rm -f "$cp_cache"
    (cd "$ROOT_DIR/backend" && mvn dependency:build-classpath -Dmdep.includeScope=runtime -Dmdep.outputFile="$cp_cache" -Dmdep.appendOutput=true -T 1C -q)
    echo "$current_hash" > "$pom_hash_file"
  fi

  # Filter out any stale com/ticket/**/*.jar from ~/.m2/repository and deduplicate so workspace target/classes are always Single Source of Truth
  local external_cp
  external_cp="$(tr ':' '\n' < "$cp_cache" | grep -v '^$' | grep -v '/com/ticket/' | awk '!seen[$0]++' | paste -sd ':' -)"

  # Collect every module's target/classes directory in the reactor (depth 3 for shared-kernel/ticket-start, depth 4 for context-*/*)
  local workspace_cp=""
  while IFS= read -r dir; do
    workspace_cp="${workspace_cp:+$workspace_cp:}$dir"
  done < <(find "$ROOT_DIR/backend" -mindepth 2 -maxdepth 5 -type d -path "*/target/classes" | sort)

  echo "${workspace_cp}:${external_cp}"
}

start_backend_watcher() {
  if is_pid_running "$PID_DIR/backend-watcher.pid"; then
    return 0
  fi

  log_info "Starting Multi-Module Automatic Incremental Compiler Watcher..."
  (
    local stamp_file="$LOG_DIR/watcher.stamp"
    touch "$stamp_file"
    while true; do
      sleep 2
      local changed_files
      changed_files="$(find "$ROOT_DIR/backend" -path "*/target" -prune -o -path "*/src/main/*" -type f \( -name "*.java" -o -name "*.yml" -o -name "*.properties" \) -newer "$stamp_file" -print 2>/dev/null || true)"
      if [[ -n "$changed_files" ]]; then
        touch "$stamp_file"
        # Extract unique relative module paths that changed
        local modules=()
        while IFS= read -r file; do
          [[ -z "$file" ]] && continue
          local rel="${file#"$ROOT_DIR/backend/"}"
          local mod_dir="${rel%%/src/main/*}"
          modules+=("$mod_dir")
        done <<< "$changed_files"

        local unique_mods
        unique_mods="$(printf "%s\n" "${modules[@]}" | sort -u | paste -sd ',' -)"
        if [[ -n "$unique_mods" ]]; then
          printf "\033[1;35m[AUTO-COMPILE]\033[0m Detected source change in [%s] -> compiling...\n" "$unique_mods" >> "$LOG_DIR/backend.log"
          if (cd "$ROOT_DIR/backend" && mvn compile -pl "$unique_mods" -am -T 1C -q >> "$LOG_DIR/backend.log" 2>&1); then
            printf "\033[1;32m[AUTO-COMPILE]\033[0m Compiled [%s] -> DevTools triggering context restart.\n" "$unique_mods" >> "$LOG_DIR/backend.log"
          else
            printf "\033[1;31m[AUTO-COMPILE]\033[0m Compilation error in [%s]. Check backend.log.\n" "$unique_mods" >> "$LOG_DIR/backend.log"
          fi
        fi
      fi
    done
  ) &
  echo $! > "$PID_DIR/backend-watcher.pid"
  log_ok "Multi-module compiler watcher started (PID $(cat "$PID_DIR/backend-watcher.pid"))."
}

start_backend() {
  if is_pid_running "$PID_DIR/backend.pid"; then
    log_info "Backend is already running (PID $(cat "$PID_DIR/backend.pid"))."
    start_backend_watcher
    return 0
  fi

  local existing_pid
  existing_pid="$(get_port_owner_pid "$BACKEND_PORT" || true)"
  if [[ -n "$existing_pid" ]]; then
    local cmdline=""
    [[ -r "/proc/$existing_pid/cmdline" ]] && cmdline="$(tr '\0' ' ' < "/proc/$existing_pid/cmdline" || true)"
    if echo "$cmdline" | grep -qE "TicketApplication|ticket-start"; then
      log_ok "Detected IDE/External TicketApplication running on port $BACKEND_PORT (PID $existing_pid). Reusing it."
      return 0
    else
      log_error "Port $BACKEND_PORT is already in use by another process (PID $existing_pid: $cmdline)."
      return 1
    fi
  fi

  local full_cp
  full_cp="$(build_backend_reactor_classpath)"

  local jvm_args=("-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}" "-Dserver.port=${BACKEND_PORT}")
  if [[ "${DEBUG:-false}" == "true" ]]; then
    local debug_port="${DEBUG_PORT:-5005}"
    jvm_args+=("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:${debug_port}")
    log_info "JVM Remote Debugging enabled on 127.0.0.1:${debug_port}"
  fi

  log_info "Starting Local Spring Boot Backend (profile=${SPRING_PROFILES_ACTIVE}, port=${BACKEND_PORT})..."
  : > "$LOG_DIR/backend.log"
  (
    cd "$ROOT_DIR/backend"
    exec java "${jvm_args[@]}" -cp "$full_cp" com.ticket.TicketApplication
  ) >> "$LOG_DIR/backend.log" 2>&1 &
  echo $! > "$PID_DIR/backend.pid"
  log_ok "Backend process started (PID $(cat "$PID_DIR/backend.pid"), log: .dev-logs/backend.log)."

  start_backend_watcher
}

start_frontend() {
  if is_pid_running "$PID_DIR/frontend.pid"; then
    log_info "Frontend is already running (PID $(cat "$PID_DIR/frontend.pid"))."
    return 0
  fi

  local existing_pid
  existing_pid="$(get_port_owner_pid "$FRONTEND_PORT" || true)"
  if [[ -n "$existing_pid" ]]; then
    local cmdline=""
    [[ -r "/proc/$existing_pid/cmdline" ]] && cmdline="$(tr '\0' ' ' < "/proc/$existing_pid/cmdline" || true)"
    if echo "$cmdline" | grep -q "vite"; then
      log_ok "Detected Vite Dev Server already running on port $FRONTEND_PORT (PID $existing_pid). Reusing it."
      return 0
    else
      log_error "Port $FRONTEND_PORT is already in use by another process (PID $existing_pid: $cmdline)."
      return 1
    fi
  fi

  log_info "Starting Local Vite Frontend Dev Server (HMR enabled, port=${FRONTEND_PORT})..."
  : > "$LOG_DIR/frontend.log"
  (
    cd "$ROOT_DIR/frontend"
    export VITE_BACKEND_PROXY_TARGET="http://127.0.0.1:${BACKEND_PORT}"
    exec npx vite --host 127.0.0.1 --port "$FRONTEND_PORT" --strictPort
  ) >> "$LOG_DIR/frontend.log" 2>&1 &
  echo $! > "$PID_DIR/frontend.pid"
  log_ok "Frontend process started (PID $(cat "$PID_DIR/frontend.pid"), log: .dev-logs/frontend.log)."
}

ensure_infra() {
  if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
    # Stop containerized backend/frontend if running from full-docker mode so ports 8080/5173 are free for local dev
    if docker ps --format '{{.Names}}' 2>/dev/null | grep -qE '^ticket-(backend|frontend)$'; then
      log_info "Stopping containerized ticket-backend / ticket-frontend to free ports for local Hybrid Dev..."
      docker rm -f ticket-backend ticket-frontend >/dev/null 2>&1 || true
    fi
  fi

  if "$ROOT_DIR/scripts/healthcheck.sh" --infra-only >/dev/null 2>&1; then
    log_ok "MySQL (${MYSQL_PORT}) and Redis (${REDIS_PORT}) are healthy and reachable."
    return 0
  fi

  if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
    log_info "Starting Docker development infrastructure (MySQL & Redis)..."
    docker compose -f "$ROOT_DIR/docker-compose.dev.yml" up -d --remove-orphans
    "$ROOT_DIR/scripts/healthcheck.sh" --wait-infra 60
  else
    log_error "MySQL (${MYSQL_HOST}:${MYSQL_PORT}) or Redis (${REDIS_HOST}:${REDIS_PORT}) is not running and Docker daemon is unavailable."
    log_error "Please start infrastructure from your WSL2 terminal: docker compose -f docker-compose.dev.yml up -d"
    return 1
  fi
}

print_banner() {
  echo "=============================================================================="
  echo "  HYBRID DEVELOPMENT ENVIRONMENT ACTIVE"
  echo "------------------------------------------------------------------------------"
  echo "  Frontend (Vite HMR) : http://127.0.0.1:${FRONTEND_PORT}"
  echo "  Backend (Spring)    : http://127.0.0.1:${BACKEND_PORT}"
  echo "  Swagger UI          : http://127.0.0.1:${BACKEND_PORT}/swagger-ui.html"
  echo "  Actuator Health     : http://127.0.0.1:${BACKEND_PORT}/actuator/health"
  echo "  MySQL               : ${MYSQL_HOST}:${MYSQL_PORT}"
  echo "  Redis               : ${REDIS_HOST}:${REDIS_PORT}"
  echo "  Logs                : .dev-logs/backend.log | .dev-logs/frontend.log"
  echo "=============================================================================="
}

case "$TARGET_COMPONENT" in
  backend)
    start_backend
    ;;
  frontend)
    start_frontend
    ;;
  all|*)
    ensure_infra
    start_backend
    start_frontend
    print_banner
    ;;
esac

if [[ "$FOREGROUND" == "--foreground" ]]; then
  trap '"$ROOT_DIR/scripts/dev-stop.sh" "$TARGET_COMPONENT"; exit 0' INT TERM
  log_info "Streaming logs for [$TARGET_COMPONENT] (Press Ctrl+C to stop)..."
  case "$TARGET_COMPONENT" in
    backend)
      touch "$LOG_DIR/backend.log"
      tail -f "$LOG_DIR/backend.log"
      ;;
    frontend)
      touch "$LOG_DIR/frontend.log"
      tail -f "$LOG_DIR/frontend.log"
      ;;
    *)
      touch "$LOG_DIR/backend.log" "$LOG_DIR/frontend.log"
      tail -f "$LOG_DIR/backend.log" "$LOG_DIR/frontend.log"
      ;;
  esac
fi
