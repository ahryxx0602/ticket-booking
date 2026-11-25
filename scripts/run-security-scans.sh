#!/usr/bin/env bash
# ==============================================================================
# VELOBUS TICKETING PLATFORM - CI & LOCAL SECURITY SCANNER RUNNER
# Fixes CI-SEC-001 (OSV-Scanner v1.9.2 real CycloneDX SBOM + NPM lockfile scan
# without || true) and CI-SEC-002 (TruffleHog non-zero git scan scope).
# ==============================================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

OSV_VERSION="v1.9.2"
OSV_SHA256="d6af4b67fa5de658598bd2d445efb99e90d1734b3146962418719c4350ecb74b"
OSV_BIN="/tmp/osv-scanner-${OSV_VERSION}"

TRUFFLEHOG_VERSION="3.88.0"
TRUFFLEHOG_BIN="/tmp/trufflehog-${TRUFFLEHOG_VERSION}"

log_info()  { printf "\033[1;34m[SEC-SCAN]\033[0m %s\n" "$*"; }
log_ok()    { printf "\033[1;32m[PASS]\033[0m     %s\n" "$*"; }
log_error() { printf "\033[1;31m[FAIL]\033[0m     %s\n" "$*" >&2; }

install_osv_scanner() {
  if [[ ! -x "$OSV_BIN" ]]; then
    log_info "Downloading OSV-Scanner ${OSV_VERSION}..."
    curl -fsSL "https://github.com/google/osv-scanner/releases/download/${OSV_VERSION}/osv-scanner_linux_amd64" -o "$OSV_BIN"
  fi
  echo "${OSV_SHA256}  ${OSV_BIN}" | sha256sum -c -
  chmod +x "$OSV_BIN"
  "$OSV_BIN" --version
}

run_osv_scan() {
  log_info "Generating aggregate CycloneDX SBOM across all 33 Backend Maven modules..."
  (cd backend && mvn -B -q org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom)
  if [[ ! -s "backend/target/bom.json" ]]; then
    log_error "Failed to generate backend/target/bom.json!"
    exit 1
  fi

  log_info "Running OSV-Scanner across Backend CycloneDX SBOM and Frontend package-lock.json (NO || true)..."
  local osv_log="/tmp/velobus-osv-scan.log"
  "$OSV_BIN" --config osv-scanner.toml -S backend/target/bom.json -L frontend/package-lock.json 2>&1 | tee "$osv_log"

  # Verify actual manifests were scanned (prevent 0-file false-green)
  if ! grep -q "backend/target/bom.json" "$osv_log"; then
    log_error "OSV-Scanner did not scan backend/target/bom.json!"
    exit 1
  fi
  if ! grep -q "frontend/package-lock.json" "$osv_log"; then
    log_error "OSV-Scanner did not scan frontend/package-lock.json!"
    exit 1
  fi
  log_ok "OSV-Scanner verified Backend Maven SBOM (33 modules) and Frontend NPM lockfile with 0 unmitigated vulnerabilities."
}

verify_osv_fails_on_vulnerable_fixture() {
  log_info "Verifying OSV-Scanner gate fails with non-zero exit code on synthetic vulnerable package-lock.json..."
  local tmp_dir
  tmp_dir="$(mktemp -d)"
  cat > "${tmp_dir}/package-lock.json" <<'EOF'
{
  "name": "synthetic-vuln-verify",
  "version": "1.0.0",
  "lockfileVersion": 2,
  "requires": true,
  "packages": {
    "": {
      "name": "synthetic-vuln-verify",
      "version": "1.0.0"
    },
    "node_modules/lodash": {
      "version": "4.17.15",
      "resolved": "https://registry.npmjs.org/lodash/-/lodash-4.17.15.tgz"
    }
  },
  "dependencies": {
    "lodash": {
      "version": "4.17.15"
    }
  }
}
EOF
  set +e
  "$OSV_BIN" -L "${tmp_dir}/package-lock.json" >/dev/null 2>&1
  local exit_code=$?
  set -e
  rm -rf "$tmp_dir"
  if [[ "$exit_code" -eq 0 ]]; then
    log_error "OSV-Scanner returned 0 on a known vulnerable fixture (lodash 4.17.15)!"
    exit 1
  fi
  log_ok "OSV-Scanner negative gate test passed (returned non-zero exit code ${exit_code} on vulnerable fixture)."
}

install_trufflehog() {
  if [[ ! -x "$TRUFFLEHOG_BIN" ]]; then
    log_info "Downloading TruffleHog v${TRUFFLEHOG_VERSION}..."
    local tgz="/tmp/trufflehog_${TRUFFLEHOG_VERSION}_linux_amd64.tar.gz"
    curl -fsSL "https://github.com/trufflesecurity/trufflehog/releases/download/v${TRUFFLEHOG_VERSION}/trufflehog_${TRUFFLEHOG_VERSION}_linux_amd64.tar.gz" -o "$tgz"
    tar -xzf "$tgz" -C /tmp trufflehog
    mv /tmp/trufflehog "$TRUFFLEHOG_BIN"
    chmod +x "$TRUFFLEHOG_BIN"
  fi
  "$TRUFFLEHOG_BIN" --version
}

run_trufflehog_scan() {
  log_info "Running TruffleHog across valid git commit history..."
  local th_log="/tmp/velobus-trufflehog.log"
  local since_commit=""
  if git rev-parse --verify HEAD~5 >/dev/null 2>&1; then
    since_commit="$(git rev-parse HEAD~5)"
  fi

  if [[ -n "$since_commit" ]]; then
    "$TRUFFLEHOG_BIN" git "file://${ROOT_DIR}" \
      --since-commit "$since_commit" \
      --branch HEAD \
      --results=verified,unknown \
      --fail \
      --json > "$th_log" 2>&1
  else
    "$TRUFFLEHOG_BIN" git "file://${ROOT_DIR}" \
      --results=verified,unknown \
      --fail \
      --json > "$th_log" 2>&1
  fi

  cat "$th_log"
  if grep -q '"chunks":[1-9]' "$th_log" || grep -q '"bytes":[1-9]' "$th_log"; then
    log_ok "TruffleHog scanned non-zero git history bytes/chunks with 0 verified secrets."
  else
    log_error "TruffleHog scanned 0 bytes/chunks! Check git history depth and arguments."
    exit 1
  fi
}

install_osv_scanner
run_osv_scan
verify_osv_fails_on_vulnerable_fixture
install_trufflehog
run_trufflehog_scan
log_ok "ALL SECURITY SCANS COMPLETED SUCCESSFULLY."
