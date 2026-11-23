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
[![Tests](https://img.shields.io/badge/Tests-259%20Passed-success?style=for-the-badge&logo=junit5&logoColor=white)]()

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
ticket-booking/
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
├── docs/                                  # Comprehensive architectural & UI/UX specifications
├── docker-compose.yml                     # Local development full-stack orchestration
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

## 🧪 Comprehensive Automated Testing (259 Tests)

The repository enforces end-to-end quality with **259 automated passing tests**:

```
TOTAL TESTS: 259 PASSED
├── Backend (Java 21 / JUnit 5 / Testcontainers): 209 Passed
├── Frontend Unit (Vitest / Happy-DOM):              29 Passed
└── Frontend E2E & Visual (Playwright / Chromium):   21 Passed
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

## 🚀 Instant Quickstart (Full-Stack Docker Compose)

You can launch the entire ecosystem (Database, Cache, Backend, and Frontend SPA) with a single command:

```bash
# Clone the repository
git clone git@github.com:ahryxx0602/ticket-booking.git
cd ticket-booking

# Spin up all 4 containers in background
docker compose up -d --build
```

### Access Endpoints
- **Frontend Web Application:** [http://localhost:5173](http://localhost:5173) (or `http://localhost:80` in prod mode)
- **Backend REST API:** [http://localhost:8080](http://localhost:8080)
- **Swagger / OpenAPI Documentation:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **MySQL Database:** `localhost:3307` (`ticket_booking_db`)
- **Redis Cache:** `localhost:6379`

### Default Seed Accounts (Development Mode)
- **Super Admin:** `admin` / `password123`
- **Bus Operator:** `operator1` / `password123`
- **Customer:** `customer1` / `password123`

---

## 📄 License & Authors

- **Author:** Phan Van Thanh (Software Engineering)
- **Repository:** [ahryxx0602/ticket-booking](https://github.com/ahryxx0602/ticket-booking)
- **License:** MIT License — Open for architectural review, portfolio inspection, and enterprise adaptation.
