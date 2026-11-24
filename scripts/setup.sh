#!/usr/bin/env bash
# ==============================================================================
# TICKET BOOKING SYSTEM - DEVELOPMENT SETUP & DOCTOR SCRIPT
# ==============================================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

MODE="${1:---setup}"

log_info()  { printf "\033[1;34m[INFO]\033[0m  %s\n" "$*"; }
log_ok()    { printf "\033[1;32m[OK]\033[0m    %s\n" "$*"; }
log_warn()  { printf "\033[1;33m[WARN]\033[0m  %s\n" "$*"; }
log_error() { printf "\033[1;31m[ERROR]\033[0m %s\n" "$*" >&2; }

check_prerequisites() {
  local failures=0
  local warnings=0

  echo "=============================================================================="
  echo "  TICKET BOOKING SYSTEM - PREREQUISITE DOCTOR"
  echo "=============================================================================="

  # 1. WSL2 / Filesystem check
  if grep -qi "microsoft" /proc/version 2>/dev/null; then
    log_ok "Platform: WSL2 Linux ($(uname -r))"
  else
    log_info "Platform: Native Linux ($(uname -r))"
  fi

  if [[ "$ROOT_DIR" == /mnt/* ]]; then
    log_warn "Repository is located on Windows mounted filesystem ($ROOT_DIR). Move to /home/<user>/... for optimal WSL2 I/O & file watching."
    warnings=$((warnings + 1))
  else
    log_ok "Filesystem location: Linux native ($ROOT_DIR)"
  fi

  # 2. Bash
  if [[ -n "${BASH_VERSION:-}" ]]; then
    log_ok "Bash: $BASH_VERSION"
  else
    log_error "Bash is required."
    failures=$((failures + 1))
  fi

  # 3. Git
  if command -v git >/dev/null 2>&1; then
    log_ok "Git: $(git --version)"
  else
    log_error "Git is missing. Install via: sudo apt-get install git"
    failures=$((failures + 1))
  fi

  # 4. Make
  if command -v make >/dev/null 2>&1; then
    log_ok "Make: $(make --version | head -n 1)"
  else
    log_error "GNU Make is missing. Install via: sudo apt-get install make"
    failures=$((failures + 1))
  fi

  # 5. Java 21
  if command -v java >/dev/null 2>&1; then
    local java_ver
    java_ver="$(java -version 2>&1 | head -n 1)"
    if echo "$java_ver" | grep -qE '"21(\.|")'; then
      log_ok "Java: $java_ver"
    else
      log_warn "Java found ($java_ver), but JDK 21 is required by the project."
      warnings=$((warnings + 1))
    fi
  else
    log_error "Java (JDK 21) is missing. Install Eclipse Temurin 21 or OpenJDK 21."
    failures=$((failures + 1))
  fi

  # 6. Maven
  if [[ -x "$ROOT_DIR/backend/mvnw" ]]; then
    log_ok "Maven Wrapper: $ROOT_DIR/backend/mvnw"
  elif command -v mvn >/dev/null 2>&1; then
    log_ok "Maven: $(mvn -v | head -n 1)"
  else
    log_error "Maven is missing. Install via: sudo apt-get install maven"
    failures=$((failures + 1))
  fi

  # 7. Node.js (>= 20 / 22 recommended)
  if command -v node >/dev/null 2>&1; then
    local node_ver
    node_ver="$(node -v)"
    log_ok "Node.js: $node_ver"
  else
    log_error "Node.js is missing. Install Node.js 22 LTS."
    failures=$((failures + 1))
  fi

  # 8. npm
  if command -v npm >/dev/null 2>&1; then
    log_ok "npm: v$(npm -v)"
  else
    log_error "npm is missing."
    failures=$((failures + 1))
  fi

  # 9. Docker Engine & Docker Compose V2
  if command -v docker >/dev/null 2>&1; then
    if docker info >/dev/null 2>&1; then
      log_ok "Docker Engine: $(docker --version)"
    else
      log_warn "Docker CLI is installed ($(docker --version)), but Docker daemon is not reachable."
      warnings=$((warnings + 1))
    fi
    if docker compose version >/dev/null 2>&1; then
      log_ok "Docker Compose: $(docker compose version)"
    else
      log_error "Docker Compose V2 ('docker compose') is missing."
      failures=$((failures + 1))
    fi
  else
    log_warn "Docker CLI not found in current PATH. Required for 'make infra', 'make integration', and 'make prod'."
    warnings=$((warnings + 1))
  fi

  echo "------------------------------------------------------------------------------"
  if [[ "$failures" -gt 0 ]]; then
    log_error "Doctor check failed with $failures critical issue(s) and $warnings warning(s)."
    return 1
  fi
  log_ok "Doctor check passed ($warnings warning(s))."
  return 0
}

run_setup() {
  check_prerequisites

  mkdir -p "$ROOT_DIR/.dev-pids" "$ROOT_DIR/.dev-logs"

  if [[ ! -f "$ROOT_DIR/.env" ]]; then
    cp "$ROOT_DIR/.env.example" "$ROOT_DIR/.env"
    log_ok "Created .env from .env.example"
  else
    log_info "Preserving existing .env file"
  fi

  log_info "Installing frontend dependencies (npm ci)..."
  (cd "$ROOT_DIR/frontend" && npm ci)
  log_ok "Frontend dependencies ready."

  log_info "Compiling backend Maven multi-module reactor (mvn test-compile)..."
  (cd "$ROOT_DIR/backend" && mvn test-compile -T 1C -q)
  log_ok "Backend workspace compiled."

  log_ok "Local development setup complete. Run 'make dev' to start."
}

case "$MODE" in
  --doctor|doctor)
    check_prerequisites
    ;;
  --setup|setup|*)
    run_setup
    ;;
esac
