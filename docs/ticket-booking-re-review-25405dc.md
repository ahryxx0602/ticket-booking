# Ticket Booking System — Security & CI Re-review #3

## 1. Review metadata

- Repository: [ahryxx0602/ticket-booking](https://github.com/ahryxx0602/ticket-booking)
- Branch: `main`
- Commit: [`25405dc`](https://github.com/ahryxx0602/ticket-booking/commit/25405dc)
- Review date: 2026-11-24
- Scope: Backend, Frontend, Production Security, Docker, CI/CD.
- Method: Static review of commit changes, previous findings and GitHub Actions logs. No independent production deployment or penetration test was performed.
- Verdict: **CI PASS — Production readiness not yet verified.**

## 2. Executive summary

The latest commit resolves HTTP 500 information disclosure and repairs the failing HTTP 403 handler unit test. All three GitHub Actions jobs passed. However, MySQL TLS remains optional, Docker healthcheck variable expansion is incorrect, and Bean Validation warnings remain in controller tests. Security integration coverage and automated vulnerability scanning still require improvement.

## 3. CI verification

Workflow: [Run #45304762086](https://github.com/ahryxx0602/ticket-booking/actions/runs/45304762086)

| Job | Result | Evidence |
|---|---|---|
| Backend Build & Test Suite | PASS | [Job log](https://github.com/ahryxx0602/ticket-booking/actions/runs/45304762086/job/138145763235) |
| Frontend Build, Unit & E2E Tests | PASS | [Job log](https://github.com/ahryxx0602/ticket-booking/actions/runs/45304762086/job/138145763639) |
| Secret & Vulnerability Scan | PASS | [Job log](https://github.com/ahryxx0602/ticket-booking/actions/runs/45304762086/job/138145763860) |

- Maven `clean verify`: BUILD SUCCESS.
- Frontend: npm ci, Vitest, Vite build and Playwright passed.
- TruffleHog secret scan passed; this is not a comprehensive vulnerability scan.
- Previous failed run #45221603928 was triggered by a pull request, not the latest main push.
- The reported 210/210 test count is from the remediation report; the CI success and selected test results were independently verified, not the aggregate count.

## 4. Remediation verification

| Finding | Status |
|---|---|
| HTTP 500 information disclosure | RESOLVED |
| HTTP 403 handler unit test | RESOLVED |
| HTTP 500 regression test | ADDED |
| Main CI failure | RESOLVED |
| MySQL TLS | PARTIALLY RESOLVED |
| Docker MySQL healthcheck | NEEDS FIX |
| Security integration tests | OPEN |
| Dependency vulnerability scanning | OPEN |

## 5. Backend findings

### SEC-01 — MySQL TLS is not enforced

- Priority: P1
- Status: OPEN
- Evidence: [`application-prod.yml`](https://github.com/ahryxx0602/ticket-booking/blob/25405dc/backend/ticket-start/src/main/resources/application-prod.yml)

Current configuration:

```text
useSSL=true&requireSSL=false&allowPublicKeyRetrieval=true
```

`requireSSL=false` permits fallback to an unencrypted connection. Server identity is not verified.

| sslMode | Encryption required | Hostname verified |
|---|---|---|
| DISABLED | No | No |
| PREFERRED | No | No |
| REQUIRED | Yes | No |
| VERIFY_CA | Yes | No |
| VERIFY_IDENTITY | Yes | Yes |

Recommended minimum:

```yaml
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:ticket_booking_db}?sslMode=REQUIRED&serverTimezone=UTC&characterEncoding=UTF-8
```

For production, prefer `VERIFY_IDENTITY` with a trusted CA and matching hostname. Remove unnecessary legacy SSL parameters and public-key retrieval settings after validating authentication compatibility.

Acceptance criteria:
- [ ] TLS connection succeeds with the intended database configuration.
- [ ] Connection fails when TLS is unavailable.
- [ ] Production certificate and hostname verification are configured where applicable.
- [ ] Actual session TLS cipher is verified.

### INFRA-01 — Docker healthcheck does not expand environment variables

- Priority: P1
- Status: OPEN
- Evidence: [`docker-compose.prod.yml`](https://github.com/ahryxx0602/ticket-booking/blob/25405dc/docker-compose.prod.yml#L24)

Current healthcheck uses `CMD` with `$${MYSQL_USER:-ticket_user}` and `$${MYSQL_PASSWORD}`. Exec-form CMD does not invoke a shell, so these expressions are not expanded as intended. Additionally, `mysqladmin ping` can return success even when authentication is denied.

Recommended configuration:

```yaml
healthcheck:
  test:
    - CMD-SHELL
    - >-
      MYSQL_PWD="$${MYSQL_PASSWORD}"
      mysql --protocol=TCP
      -h 127.0.0.1
      -u "$${MYSQL_USER:-ticket_user}"
      -D "$${MYSQL_DATABASE:-ticket_booking_db}"
      -e "SELECT 1"
      >/dev/null 2>&1
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 30s
```

Acceptance criteria:
- [ ] Healthcheck succeeds with valid credentials, including a custom MYSQL_USER.
- [ ] Healthcheck fails with invalid credentials.
- [ ] Production Compose starts successfully with healthy dependencies.
- [ ] No secrets are printed in CI logs.

### TEST-01 — Bean Validation provider missing in controller test environment

- Priority: P1
- Status: OPEN
- Evidence: Backend CI log.

```text
jakarta.validation.NoProviderFoundException:
Unable to create a Configuration, because no Jakarta Bean Validation provider could be found.
```

Observed in PartnerControllerTest, AdminPartnerControllerTest and PublicBannerControllerTest. This is confirmed in the test environment, not necessarily production.

Check module dependencies and add the appropriate validation provider if missing:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

Acceptance criteria:
- [ ] Validation provider is available in relevant test classpaths.
- [ ] Invalid payloads return HTTP 400 and the expected error envelope.
- [ ] Validation warnings are eliminated without suppressing logs.

### SEC-02 — HTTP 500 information disclosure

- Status: RESOLVED
- Evidence: `GlobalExceptionHandler.java` and `CrossContextAdaptersTest.java` in commit 25405dc.

The generic exception handler now returns a fixed safe message rather than `ex.getMessage()`. A regression test verifies that database error details are not returned to clients.

Optional follow-up: correlation IDs, structured logging and sensitive-data masking.

### TEST-02 — Security integration coverage

- Priority: P1
- Status: OPEN

The replacement HTTP 403 unit test correctly validates the handler response object, but does not exercise JSON serialization or the real Spring Security filter chain. Keep the unit test and add integration tests using the actual security configuration.

| Scenario | Expected result |
|---|---|
| Anonymous request to Admin API | 401 |
| Customer request to Admin or Operator API | 403 |
| Operator accesses another operator's data | 403 or 404 |
| Customer accesses another customer's booking | 403 or 404 |
| Expired or invalid-signature JWT | 401 |
| Anonymous upload | 401 |
| Invalid webhook signature | Rejected; no state mutation |

Acceptance criteria: tests exercise real authorization, ownership checks and response envelopes; failures block CI.

## 6. Frontend review

Commit 25405dc does not modify frontend code. All frontend CI steps passed.

| Area | Status |
|---|---|
| npm ci / Vitest / Vite / Playwright | PASS |
| Session expiration event | Added in earlier commit |
| JWT in localStorage | Residual XSS exposure risk |
| Refresh token rotation / revocation | Not verified |
| CSP and security headers | Audit required |

Prioritize XSS prevention, CSP, token lifetime and session revocation. Do not redesign authentication solely to remove localStorage without evaluating CSRF and the full token lifecycle.

## 7. CI/CD improvements

| ID | Improvement | Priority |
|---|---|---|
| CI-01 | Production Docker Compose smoke test | P1 |
| CI-02 | CodeQL or equivalent SAST | P1 |
| CI-03 | Java dependency scanning (OWASP Dependency-Check or equivalent) | P1 |
| CI-04 | Frontend dependency scanning (npm audit / OSV-Scanner) | P1 |
| CI-05 | Trivy container image scanning | P1 |
| CI-06 | Pin GitHub Actions to immutable commit SHAs | P2 |
| CI-07 | CycloneDX SBOM | P2 |
| CI-08 | JaCoCo coverage reporting | P2 |
| CI-09 | k6 concurrency/load regression tests | P2 |

Current TruffleHog action uses `@main` and `--only-verified`. Pin to a reviewed immutable SHA. Do not permanently disable dependency updates merely to avoid CI failures; review compatibility and security patches separately.

The production smoke test should build images, start Compose with isolated test credentials, verify service readiness, check a backend endpoint through the gateway and clean up containers and volumes.

## 8. Next phase — Deep Business Logic & Security Audit

The following are audit targets, not confirmed vulnerabilities.

| Context | Review scope |
|---|---|
| Booking | Distributed lock ownership, TTL, transaction boundaries, overbooking, FSM, expiration races |
| Payment | Webhook signature, replay, idempotency, amount validation, refunds, reconciliation |
| Identity | JWT lifecycle, account lockout, RBAC, privilege escalation, brute-force protection |
| Partner | Tenant isolation, wallet consistency, commission, withdrawal, escrow settlement |
| Notification | Webhook authentication, retry, duplicate events, async processing, PII protection |

## 9. Remaining remediation checklist

- [ ] P1 — Enforce MySQL TLS with an appropriate sslMode.
- [ ] P1 — Fix MySQL healthcheck using CMD-SHELL and authenticated query.
- [ ] P1 — Resolve Bean Validation warnings and add negative tests.
- [ ] P1 — Add security integration tests for 401/403 and IDOR.
- [ ] P1 — Add Production Docker Compose smoke test.
- [ ] P1 — Add SAST, dependency and container vulnerability scanning.
- [ ] P2 — Pin GitHub Actions to immutable SHAs.
- [ ] P2 — Audit JWT lifecycle and CSP.
- [ ] P2 — Evaluate versioned database migrations (Flyway).
- [ ] P2 — Review container hardening, resource limits, Redis credentials and optional Firebase mounts.
- [ ] Next phase — Audit Booking, Payment, Identity, Partner and Notification business logic.

## 10. Final verdict

| Criterion | Result |
|---|---|
| Main CI | PASS |
| Backend build | PASS |
| Frontend build and tests | PASS |
| HTTP 500 information disclosure | RESOLVED |
| Production TLS | PARTIALLY RESOLVED |
| Docker healthcheck | NEEDS FIX |
| Security testing | NEEDS IMPROVEMENT |
| Business logic security | NOT FULLY AUDITED |
| Portfolio / CV readiness | YES |
| Production readiness | NOT YET VERIFIED |

The repository is suitable as a Junior Backend Java/Spring Boot portfolio project. A green CI pipeline does not establish production security or high-concurrency correctness. Complete the outstanding infrastructure findings, then prioritize business logic and concurrency auditing rather than repeatedly refactoring configuration.
