# 🎫 VeloBus — High-Concurrency Ticket Booking & Mobility Platform

<p align="center">
  🌐 <b><a href="README.md">English</a></b> | <b><a href="README.vi.md">Tiếng Việt</a></b>
</p>

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.1-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue-3.5.13-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white)](https://vuejs.org/)
[![PrimeVue 4](https://img.shields.io/badge/PrimeVue-4.2.5-41B883?style=for-the-badge&logo=primevue&logoColor=white)](https://primevue.org/)
[![Tailwind CSS](https://img.shields.io/badge/TailwindCSS-3.4.17-38B2AC?style=for-the-badge&logo=tailwind-css&logoColor=white)](https://tailwindcss.com/)
[![MySQL](https://img.shields.io/badge/MySQL-8.4%20LTS-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7.2-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![CI Pipeline](https://img.shields.io/badge/CI%2FCD-GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)](.github/workflows/ci.yml)
[![Tests](https://img.shields.io/badge/Tests-325%20Passed-success?style=for-the-badge&logo=junit5&logoColor=white)]()

An enterprise-grade, high-concurrency multi-tenant ticket booking and fleet mobility marketplace built with **Hexagonal / Clean Architecture**, **Domain-Driven Design (DDD)**, and **Modern Modular Monolith** patterns. 

Inspired by real-world mobility leaders such as **Vexere** and **Shopee**, the platform resolves mission-critical distributed systems challenges: **sub-20ms catalog caching, zero-overbooking seat concurrency via Redisson & DB constraints, idempotent VNPay payments, automated bus operator escrow settlement, and rich multi-portal frontend experiences**.

---

## 📑 Dedicated Architecture Documentation

For granular technical specifications, please consult the sub-system documentation:
- ⚙️ **[Backend Core Documentation](backend/README.md) ([Tiếng Việt](backend/README.vi.md)):** Domain-Driven Design, 8 Bounded Contexts, 33 Maven Modules, Redisson Locks, Virtual Threads, and FSM.
- 🖥️ **[Frontend Interface Documentation](frontend/README.md) ([Tiếng Việt](frontend/README.vi.md)):** Vue 3, PrimeVue 4, Tailwind CSS, Kinetic Executive Design System, 3 Portals, and Playwright E2E suites.

---

## 🏛️ Monorepo System Architecture

```
velobus-ticketing-platform/
├── backend/                               # Spring Boot 3.4.1 + Java 21 Distributed Core
│   ├── shared-kernel/                     # Core Value Objects, Domain Exceptions, Common Types
│   ├── context-identity/                  # Auth, JWT, RBAC, User Management
│   ├── context-catalog/                   # Stations, Routes, Trips, Caching Layer
│   ├── context-booking/                   # Redisson Distributed Locks, Booking FSM
│   ├── context-payment/                   # VNPay HMAC-SHA512 Gateway, Idempotent IPN
│   ├── context-notification/              # FCM Push, Telegram Bot, Firebase Storage
│   ├── context-partner/                   # Bus Operators, Fleet & Driver Roster, Escrow
│   └── ticket-start/                      # Application Bootstrap & Monolith Assembly
│
├── frontend/                              # Vue 3 + PrimeVue 4 + Tailwind CSS Interface
│   ├── src/app/                           # Layouts (Customer, Operator, Admin, Auth) & Router
│   ├── src/modules/auth/                  # Authentication & Profile Management
│   ├── src/modules/catalog-booking/       # Search, 2-Deck SeatMap, Checkout, E-Tickets
│   ├── src/modules/partner/               # Escrow Wallet, Fleet Roster, Trip Scheduling
│   ├── src/modules/admin/                 # KPI Dashboard, Reconciliation Alerts, Backoffice
│   └── src/shared/                        # HTTP Client, Design System Kit, Composables
│
├── environment/                           # Database init scripts, stress test scripts, seeds
├── scripts/                               # Developer automation, lifecycle & smoke-test scripts
├── docs/                                  # Comprehensive architectural & UI/UX specifications
├── Makefile                               # Unified 20-command Developer CLI entry point
├── docker-compose.dev.yml                 # Hybrid Dev Infrastructure (MySQL & Redis on 127.0.0.1)
├── docker-compose.yml                     # Full-stack Docker Integration orchestration
├── docker-compose.prod.yml                # Production full-stack deployment
└── .github/workflows/ci.yml               # Automated CI/CD & Secret Scanning Pipeline
```

---

## 🎨 UI/UX Design System

VeloBus features the **Kinetic Executive** design system, tailored for high-density transport and logistics operations:
- **Design Tokens:** Brand Navy (`#142B4A`), Primary Blue (`#2563EB`), Accent Amber (`#F59E0B`), Glare-free Canvas (`#F8FAFC`).
- **Typography:** Inter with strict OpenType `tabular-nums` formatting to prevent layout jitter during real-time schedule updates.

| Screen Key | Portal & Screen Title | Key Engineering Highlight |
| :--- | :--- | :--- |
| `seat-map` | **Customer: 2-Deck Seat Map** | 40-seat matrix, 10-minute hold countdown, sticky VNPay summary. |
| `operator-escrow` | **Operator: Fintech Escrow Dashboard** | 4 balances (`AVAILABLE`, `HOLDING`, `SETTLING`, `WITHDRAWN`), Vietcombank KYC. |
| `admin-reconciliation` | **Admin: Governance & VNPay Reconciliation** | Real-time IPN discrepancy alert, 2-tier commission, Audit Trail. |
| `operator-fleet` | **Operator: Fleet & Schedules** | Turnaround Buffer (30m) invariant guard, vehicle drawer. |
| `0c0c51cfabda47c08fb8b230245acd33` | **Customer: Digital E-Ticket** | Airline-style perforated ticket, HMAC-SHA256 encrypted QR Code. |

---

## 🔒 Key Engineering & Concurrency Invariants

1. **Zero Overbooking via Defense-in-Depth:**
   - Distributed Lock: Redisson fair lock on `lock:trip:{tripId}:seat:{seatCode}` (10m TTL).
   - Database Safeguard: MySQL composite unique index `uk_trip_seat_status` on `(trip_id, seat_code, status_active)`.
2. **Turnaround Buffer (30 Minutes):**
   - Mandatory minimum 30-minute interval between consecutive trips for the same vehicle or driver to prevent fatigue and operational clashes.
3. **Fail-Closed Security & Tenant Isolation:**
   - All partner operations enforce `findByIdAndOperatorId(id, operator.getId())` to eliminate Insecure Direct Object References (IDOR).
   - Administrative actions require verified identity resolution with zero fallback defaults.
4. **Idempotent Financial Settlement:**
   - VNPay IPN handler performs strict checksum validation (HMAC-SHA512), amount verification, and state transition idempotency.

---

## 🧪 Comprehensive Automated Testing (325 Tests)

The repository enforces end-to-end quality with **325 automated passing tests**:

```
TOTAL TESTS: 325 PASSED
├── Backend (Java 21 / JUnit 5 / Testcontainers): 243 Passed
├── Frontend Unit (Vitest / Happy-DOM):              60 Passed
└── Frontend E2E & Visual (Playwright / Chromium):   22 Passed
```

```bash
# Run backend test suite
cd backend && mvn test

# Run frontend unit tests
cd frontend && npm run test:unit

# Run frontend E2E and responsive tests
cd frontend && npm run test:e2e
```

---

## ⚡ Hybrid Development Environment (Recommended Daily Workflow)

For daily engineering on **WSL2 Ubuntu**, VeloBus uses a **Hybrid Development Environment**:
- **Infrastructure in Docker (`docker-compose.dev.yml`):** Only `ticket-mysql` (`127.0.0.1:3307`) and `ticket-redis` (`127.0.0.1:6379`) run in containers with persistent volumes (`mysql_dev_data`, `redis_dev_data`).
- **Frontend Local Runtime:** Vite Dev Server (`http://127.0.0.1:5173`) with instant Vue 3 + Tailwind HMR and `/api` reverse proxy to `http://127.0.0.1:8080`.
- **Backend Local Runtime:** Java 21 + Spring Boot (`http://127.0.0.1:8080`, profile `dev`) with **Spring Boot DevTools** + **Multi-Module Reactor Workspace Classpath** (`*/target/classes` across all 26 modules) + **Automatic Incremental Compiler Watcher** (`mvn compile -pl <changed-module> -am -T 1C`). No Docker image rebuild or `mvn install` is required when editing Java or Vue source files.

### 1. First-Time Setup & Prerequisite Check
```bash
# Check JDK 21, Maven, Node 22+, Docker Compose V2, and WSL2 native filesystem
make doctor

# Initialize .env from .env.example, install frontend dependencies, and compile backend reactor
make setup
```

### 2. Daily Development Startup & Auto-Reload
```bash
# Start MySQL & Redis (Docker) + Backend (Local DevTools + Auto-Compiler) + Frontend (Vite HMR)
make dev
```
- **Alternatively, run components separately or with an IDE (IntelliJ / VS Code):**
  - `make infra` — Start only MySQL (`127.0.0.1:3307`) and Redis (`127.0.0.1:6379`).
  - `make backend` — Start local Backend with multi-module auto-compile watcher (or run `com.ticket.TicketApplication` directly in your IDE; `make dev` automatically detects and reuses an IDE-managed Backend on port `8080`, or set `DEBUG=true make backend` for JDWP on `127.0.0.1:5005`).
  - `make frontend` — Start local Vite Dev Server (`127.0.0.1:5173`).
- **Verifying Auto-Reload:**
  - Edit any `.vue` / `.js` / Tailwind file in `frontend/src/` $\to$ Vite HMR updates instantly without rebuilding Docker.
  - Edit any `.java` / `.yml` file in `ticket-start` or any dependent module (`context-catalog`, `context-booking`, etc.) $\to$ the background watcher incrementally compiles the changed module (`~1s`) and Spring Boot DevTools restarts the context (`~2.6s`).
- **Stopping Safely:**
  - `make stop` — Gracefully terminates local Backend, Watcher, and Frontend processes by verified PID (never kills unrelated Java/Node processes and preserves database volumes).
  - `make infra-stop` — Stops `ticket-mysql` and `ticket-redis` containers while preserving persistent volumes.

### 3. Unified Makefile Command Reference

| Command | Description |
| :--- | :--- |
| `make help` | Display all available developer commands |
| `make doctor` | Validate prerequisites (JDK 21, Maven, Node, Docker Compose V2, WSL2 path) |
| `make setup` | Initialize `.env`, install npm dependencies, and compile Maven reactor |
| `make infra` | Start MySQL and Redis in Docker (`docker-compose.dev.yml`) and wait for healthchecks |
| `make infra-stop` | Stop MySQL and Redis containers (preserves volumes) |
| `make infra-status` | Show status of development infrastructure containers |
| `make dev` | Start full Hybrid Development Environment (Infra + Backend + Vite HMR) |
| `make backend` | Start local Spring Boot Backend + Multi-Module Auto-Compile Watcher |
| `make frontend` | Start local Vite Dev Server with HMR |
| `make stop` | Stop local development application processes safely |
| `make logs` | Stream logs from MySQL and Redis containers |
| `make smoke-test` | Run end-to-end health & proxy verification against running services |
| `make test` | Run all Backend and Frontend automated tests |
| `make test-backend` | Run Backend Maven unit & integration tests |
| `make test-frontend` | Run Frontend Vitest unit tests |
| `make build` | Build Docker images for Backend and Frontend |
| `make integration` | Launch all 4 containers in Full Docker Integration mode (`docker-compose.yml`) |
| `make prod` | Launch hardened Production containers (`docker-compose.prod.yml`) |
| `make clean` | Clean Maven `target/` and Frontend `dist/` build artifacts safely |
| `make reset-db` | Reset development database volume with interactive `[y/N]` confirmation |

---

## 🚀 Full-Stack Docker Integration & Production Deployment

To test the containerized integration environment (all 4 containers in Docker) or build immutable Production images:

```bash
# Full Docker Integration Environment (MySQL + Redis + Backend JAR + Frontend Nginx)
make integration

# Verify deployment health and API proxy
make smoke-test
```

### Access Endpoints
- **Frontend Web Application:** [http://127.0.0.1:5173](http://127.0.0.1:5173) (or `http://localhost:80` in prod mode)
- **Backend REST API:** [http://127.0.0.1:8080](http://127.0.0.1:8080)
- **Swagger / OpenAPI Documentation:** [http://127.0.0.1:8080/swagger-ui.html](http://127.0.0.1:8080/swagger-ui.html)
- **Actuator Health Probe:** [http://127.0.0.1:8080/actuator/health](http://127.0.0.1:8080/actuator/health)
- **MySQL Database:** `127.0.0.1:3307` (`ticket_booking_db`)
- **Redis Cache:** `127.0.0.1:6379`

### Default Seed Accounts (Development Profile Only)
- **Super Admin:** `admin` / `admin_dev_password`
- **Bus Operator:** `operator1` / `operator_dev_password`
- **Customer:** `customer1` / `customer_dev_password`

### Troubleshooting Common Issues
- **Port `8080` or `5173` already in use:** If `ticket-backend` or `ticket-frontend` Docker containers are still running from full-docker mode, `make infra` or `make dev` automatically removes the orphan application containers. To check manually, run `ss -ltnp | grep -E "8080|5173"`.
- **MySQL Port `3306` vs `3307`:** Host port `3307` is used by default (`ADR-01`) to avoid colliding with a Windows host MySQL service on WSL2. Override `MYSQL_PORT` in `.env` if needed.
- **Slow File Watching on WSL2:** Always keep the repository inside the Linux native filesystem (`/home/<user>/...`) rather than `/mnt/c/...` (`make doctor` verifies this automatically).

---

## 📄 License & Authors

- **Author:** Phan Van Thanh (Software Engineering)
- **Repository:** [ahryxx0602/velobus-ticketing-platform](https://github.com/ahryxx0602/velobus-ticketing-platform)
- **License:** MIT License — Open for architectural review, portfolio inspection, and enterprise adaptation.
