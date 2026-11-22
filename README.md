# 🎫 High-Concurrency Ticket Booking System (Modular Monolith)

<p align="center">
  🌐 <b><a href="README.md">English</a></b> | <b><a href="README.vi.md">Tiếng Việt</a></b>
</p>

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.1-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.4%20LTS-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7.2-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![Redisson](https://img.shields.io/badge/Redisson-Distributed_Lock-009688?style=for-the-badge)](https://redisson.org/)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%26%20Hexagonal%20%7C%20DDD-blue?style=for-the-badge)]()
[![Tests](https://img.shields.io/badge/Tests-209%20Passed%20(33%20Modules)-success?style=for-the-badge&logo=junit5&logoColor=white)]()

An enterprise-grade, high-concurrency multi-tenant ticket booking platform built with **Clean / Hexagonal Architecture** and **Domain-Driven Design (DDD)** on **Java 21** and **Spring Boot 3.4.1**. Inspired by real-world marketplace leaders like **Vexere** and **Shopee**, the platform resolves complex distributed challenges: **sub-millisecond catalog retrieval, zero-overbooking seat concurrency, idempotent payment gateways, multi-channel notifications, and multi-tenant fleet operations with automated escrow settlement**.

---

## 🏛️ Clean Architecture & DDD Layering Paradigm

The project strictly adheres to the **Dependency Rule** of Clean Architecture: inner layers define business invariants and remain 100% agnostic to frameworks, ORMs, and external infrastructure.

```
       ┌────────────────────────────────────────────────────────┐
       │              Presentation Layer (REST Controllers)     │
       └──────────────────────────┬─────────────────────────────┘
                                  ▼
       ┌────────────────────────────────────────────────────────┐
       │       Application Layer (UseCases, DTOs, Listeners)    │
       └──────────────────────────┬─────────────────────────────┘
                                  ▼
       ┌────────────────────────────────────────────────────────┐
       │     Domain Layer (Models, Enums, Events, Ports, FSM)   │  ◄── CORE DOMAIN (0% Framework)
       └──────────────────────────▲─────────────────────────────┘
                                  │ (Implements Outbound Ports)
       ┌──────────────────────────┴─────────────────────────────┐
       │      Infrastructure Layer (JPA Entities, Redis, Adapters)│
       └────────────────────────────────────────────────────────┘
```

### Layer Responsibility Breakdown

| Architectural Layer | Core Components | Enterprise Design Pattern & Mapping |
| :--- | :--- | :--- |
| **Domain Layer** (`*-domain`) | • **Models:** Pure Domain POJOs (`Booking`, `Trip`, `OperatorWallet`, `Banner`)<br>• **Enums:** Strongly typed constants (`BookingStatus`, `OperatorStatus`, `VehicleType`)<br>• **Events:** Immutable Domain Events (`SeatHeldEvent`, `PaymentConfirmedEvent`, `BookingRefundedEvent`)<br>• **FSM:** Finite State Machines (`BookingStateMachine`)<br>• **Ports:** Repository & Outbound Interfaces (`BookingRepository`, `PartnerCatalogPort`) | **Core Domain (Pure Java):** Zero framework imports (`org.springframework.*`, `jakarta.persistence.*` forbidden). High cohesion, rich domain invariants. |
| **Application Layer** (`*-application`) | • **UseCases:** Command/Query execution handlers (`HoldSeatUseCase`, `PartnerCommandService`)<br>• **DTOs:** Request/Response payloads with Jakarta Validation (`@Valid`)<br>• **Event Listeners:** Cross-context transactional coordination (`@TransactionalEventListener`) | **Inbound Ports & CQRS Orchestration:** Validates inputs, coordinates domain aggregates, dispatches integration events. |
| **Infrastructure Layer** (`*-infrastructure`) | • **JPA Entities & Repos:** Database schema mappings & Spring Data adapters<br>• **Distributed Locks:** Redis Redisson & Lua CAS token adapters<br>• **Schedulers:** Escrow settlement scheduler & booking auto-expiry worker<br>• **External Gateways:** VNPay HMAC-SHA512, Telegram Bot API, SMTP Mailer, ZXing QR | **Outbound Adapters:** Handles database I/O, cache invalidation, network latency isolation, and third-party integrations. |
| **Presentation Layer** (`*-presentation`) | • **REST Controllers:** HTTP routing, serialization, role-based authorization (`@PreAuthorize`)<br>• **Global Error Advice:** Standardized HTTP response envelope (`ApiResponse<T>`) | **Inbound Adapters:** Web layer mapping HTTP requests to application commands and queries. |

---

## 🌟 Key Technical Highlights

1. **Zero Overbooking Guarantee (Distributed Locking & Owner-Token Pattern):**
   - Combines **Redisson Distributed Locks** and **Owner-Token Lua CAS** (`setIfAbsent(ownerToken, ttl)`) outside DB transactions to prevent lock-stealing and eliminate deadlocks during flash-sale spikes.
2. **Sub-20ms Catalog Latency (Cache-Aside with Non-blocking SCAN/UNLINK):**
   - High-throughput trip search backed by Redis caching. Eviction executes asynchronously in `TransactionSynchronization.afterCommit()` via `SCAN` + `UNLINK` to eliminate Redis single-thread blocking.
3. **Multi-Tenant Operator Marketplace & IDOR Elimination (ADR-16):**
   - Bus operators (`ROLE_OPERATOR`) register and manage their own fleets, seat layouts, drivers, and trip schedules.
   - Enforces strict multi-tenancy: every mutation validates tenant ownership via `findByIdAndOperatorId(id, operatorId)` derived from verified JWT principals.
4. **2-Tier Commission Engine & Automated Escrow Settlement (ADR-13, 14, 15):**
   - **2-Tier Commission:** Platform-wide default commission (e.g., 10%) with per-operator custom override (e.g., 7% for VIP operators), snapshotted upon booking payment.
   - **Escrow Wallet (4 Balances):** `pending_balance`, `available_balance`, `locked_balance`, `debt_balance`.
   - **Atomic Check-and-Set Settlement:** `EscrowSettlementScheduler` executes periodic settlement (+2h post-departure) using database-level CAS (`UPDATE trips SET settlement_status = 'SETTLED' WHERE id = :id AND settlement_status = 'UNSETTLED'`), eliminating race conditions across multiple scheduler replicas.
   - **Debt Protection:** Late refunds trigger automatic debt tracking and prevent withdrawals until debts are cleared.
5. **Turnaround Buffer Safety Guardrail (LSN-14):**
   - Prevents cascading trip delays by enforcing a strict **30-minute vehicle turnaround buffer** between consecutive departures.
6. **Multi-Channel Notification Architecture (Virtual Threads & Event-Driven):**
   - **Telegram Bot:** Multi-role bot supporting secure OTP linking (Atomic Redis `GETDEL`), public ticket lookup (`/ve`), and admin alerts.
   - **Cryptographic E-Ticket QR:** Generates Base64 QR codes signed via **HMAC-SHA256** (`v1|ref|tripId|hash`) for offline ticket validation.
   - **Non-blocking Dispatch:** Dispatched via Java 21 **Virtual Threads** (`Executors.newVirtualThreadPerTaskExecutor()`) to decouple network latency from core transactions.
7. **Idempotent Payment & Webhook Processing (ADR-07, ADR-08):**
   - VNPay v2.1.0 gateway integration with strict HMAC-SHA512 ASCII sorting and timing-safe signature verification (`MessageDigest.isEqual`).

---

## 📁 Modular Monolith Architecture (33 Modules)

```text
ticket-booking/
├── environment/                         # Containerized Local Infrastructure
│   ├── docker-compose.yml               # MySQL 8.4 LTS (Port 3307) + Redis 7.2 (Port 6379)
│   └── init.sql                         # Database DDL, composite indexes & initial seed data
│
├── shared-kernel/                       # Shared domain primitives & integration events
│   ├── BaseEntity, ApiResponse, DistributedLockPort
│   └── Integration Events (PaymentConfirmedEvent, BookingRefundedEvent, etc.)
│
├── context-identity/                    # BOUNDED CONTEXT 1: Identity & Access Management (IAM)
│   ├── identity-domain/                 # User Aggregate, UserRole, UserStatus, Repository Port
│   ├── identity-application/            # LoginUseCase, RegisterUseCase, Profile Management
│   ├── identity-infrastructure/         # Spring Security 6, Stateless JJWT, BCrypt, JPA Adapter
│   └── identity-presentation/           # REST Controllers (/api/v1/auth, /api/v1/users)
│
├── context-catalog/                     # BOUNDED CONTEXT 2: Trips, Routes & Fleet Catalog
│   ├── catalog-domain/                  # Trip Aggregate, Station, Seat, Pricing Policy
│   ├── catalog-application/             # SearchTripsUseCase, AdminTripService
│   ├── catalog-infrastructure/          # Redis Cache-Aside Adapter, Spring Data JPA
│   └── catalog-presentation/            # REST Controllers (/api/v1/catalog)
│
├── context-booking/                     # BOUNDED CONTEXT 3: Reservation & Distributed Concurrency
│   ├── booking-domain/                  # Booking Aggregate Root, Booking FSM, SeatHoldingInvariants
│   ├── booking-application/             # HoldSeatUseCase, ConfirmBookingUseCase, CancelBookingUseCase
│   ├── booking-infrastructure/          # Redisson Distributed Lock Adapter, Spring Data JPA
│   └── booking-presentation/            # REST Controllers (/api/v1/bookings)
│
├── context-payment/                     # BOUNDED CONTEXT 4: Financial Transactions & Gateways
│   ├── payment-domain/                  # Payment Aggregate, PaymentStatus, PaymentMethod
│   ├── payment-application/             # CreatePaymentUseCase, ProcessIpnUseCase, AdminRefundService
│   ├── payment-infrastructure/          # VNPay HMAC-SHA512 Adapter, Webhook Idempotency Guard
│   └── payment-presentation/            # Payment & Webhook Controllers (/api/v1/payments)
│
├── context-notification/                # BOUNDED CONTEXT 5: Multi-Channel Event Notification
│   ├── notification-domain/             # Notification Aggregate, NotificationChannel, TelegramAccount
│   ├── notification-application/        # SendNotificationUseCase, TelegramBotUseCase, OtpService
│   ├── notification-infrastructure/     # Virtual Threads Executor, SMTP, Telegram API, ZXing QR
│   └── notification-presentation/       # Notification & Telegram Webhook Controllers (/api/v1/notifications)
│
├── context-partner/                     # BOUNDED CONTEXT 6: Multi-Tenant Marketplace & Escrow
│   ├── partner-domain/                  # BusOperator, Vehicle, Driver, OperatorWallet, Banner
│   ├── partner-application/             # PartnerCommandService, PartnerAdminService, Escrow Logic
│   ├── partner-infrastructure/          # EscrowSettlementScheduler, Atomic CAS Repositories, JPA
│   └── partner-presentation/            # Partner, Admin & Public REST Controllers (/api/v1/partners, /api/v1/public)
│
└── ticket-start/                        # RUNNABLE SPRING BOOT ENTRYPOINT
    ├── pom.xml                          # Assembly & Spring Boot repackage plugin
    └── src/main/
        ├── java/com/ticket/
        │   ├── TicketApplication.java   # Main entry point
        │   ├── adapter/                 # Cross-context hexagonal port adapters
        │   └── config/                  # GlobalExceptionHandler, SecurityConfig, RedissonConfig
        └── resources/                   # application.yml, application-dev.yml
```

---

## ⚡ Concurrency & State Machine Workflows

### 1. Two-Tier Distributed Seat Holding Flow
```
User Request ──► BookingController ──► HoldSeatUseCase
                                             │
               ┌─────────────────────────────┴─────────────────────────────┐
               ▼                                                           ▼
     Acquire Redisson Mutex Key                                 Lock Already Taken?
     "lock:seat:{tripId}:{seatNum}"                                         │
     (TTL: 10 mins, Wait: 2s)                                               │
               │                                                           ▼
               ├────────► Success ──► Create Booking (HELD) ──► 200 OK  400 Bad Request
               │                      Dispatches SeatHeldEvent          (SEAT_ALREADY_RESERVED)
               └────────► Timeout / Failure ──► Auto-Release Key
```

### 2. Complete Booking Lifecycle (Deterministic FSM)
```
  [PENDING] ────► acquireLocks() ────► [ HELD ] ────► IPN Webhook (Success) ────► [ CONFIRMED ]
      │                                    │                                           │
      └─────────► Cancel / Fail            ├────────► Manual Cancel ──► [ CANCELLED ]  ├─► Admin Refund
                       │                   │                                           │         │
                       ▼                   └────────► 10m TTL Timeout ─► [ EXPIRED ]   ▼         ▼
                  [TERMINATED]                                                        [ REFUNDED ]
```

### 3. Escrow Settlement & Wallet Flow
```
Passenger Purchases Ticket
       │
       ▼
PaymentConfirmedEvent ──► Credit Pending Balance (Gross - Commission Rate snapshot)
       │
       ▼
Trip Departs ──► +2 Hours Passed
       │
       ▼
EscrowSettlementScheduler (Atomic DB CAS Update)
       │
       ├─► CAS Succeeded (First replica) ──► Settle Pending to Available Balance
       │                                      (Auto-deduct debtBalance if present)
       └─► CAS Failed (Subsequent replicas) ──► Skip (Double-settlement avoided)
```

---

## 🛠️ Tech Stack & Tooling

| Domain | Technologies |
| :--- | :--- |
| **Language & Runtime** | **Java 21 LTS**, Virtual Threads (Project Loom) |
| **Frameworks** | **Spring Boot 3.4.1**, Spring Data JPA, Spring Security 6 |
| **Distributed Systems & Cache** | **Redis 7.2**, **Redisson 3.40** (Distributed Locks & In-Memory Caching) |
| **Relational Database** | **MySQL 8.4 LTS**, HikariCP Connection Pooling |
| **Security & Cryptography** | JJWT 0.12, BCrypt, HMAC-SHA512 (VNPay), HMAC-SHA256 (Signed QR) |
| **Integrations** | VNPay Gateway, Telegram Bot API, ZXing QR Generator, Jakarta Mail |
| **Testing & Quality** | JUnit 5, Mockito, AssertJ, Spring MockMvc, Postman / Newman E2E |

---

## 🚀 Quick Start Guide

### Prerequisites
- **JDK 21** installed and configured in `JAVA_HOME`
- **Apache Maven 3.8+**
- **Docker & Docker Compose**

### 1. Launch Infrastructure (Docker)
Start containerized instances of MySQL 8.4 and Redis 7.2:
```bash
cd environment
docker compose up -d
```
> **Default Connections:**
> - **MySQL 8.4:** `localhost:3307` | Database: `ticket_booking_db` | User: `ticket_user` | Pass: `ticket_password`
> - **Redis 7.2:** `localhost:6379` | Auth: *Disabled (Default)*

### 2. Build & Verify All 33 Modules
Compile the multi-module reactor and run the complete test suite (209 automated tests):
```bash
mvn clean test
```

### 3. Run Application
Start the application from the entrypoint module `ticket-start`:
```bash
mvn spring-boot:run -pl ticket-start
```
The application will boot and bind to **`http://localhost:8080`**.

---

## 👥 Seed Accounts for Testing

The system seeds ready-to-test accounts upon initialization:

| Role | Username | Password | Access Capabilities |
| :--- | :--- | :--- | :--- |
| **Platform Admin** | `admin` | `admin_dev_password` | Full governance, commission overrides, withdrawal approvals, banner reviews |
| **Bus Operator** | `operator1` | `operator_dev_password` | Fleet management, seat layout, vehicle turnaround scheduling, wallet withdrawals |
| **Customer** | `customer1` | `customer_dev_password` | Trip search, seat reservation, payment, personal booking management |

---

## 📡 API Verification (cURL Snippets)

### 1. User Authentication (JWT Login)
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "customer1",
    "password": "customer_dev_password"
  }'
```

### 2. Search Trips (Redis Cached)
```bash
curl -X GET "http://localhost:8080/api/v1/catalog/trips"
```

### 3. Concurrent Seat Hold (10-minute Distributed Mutex)
```bash
curl -X POST http://localhost:8080/api/v1/bookings/hold \
  -H "Authorization: Bearer <CUSTOMER_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "tripId": 1,
    "seatNumbers": ["12A", "12B"]
  }'
```

### 4. Operator Fleet Registration (Multi-Tenant)
```bash
curl -X POST http://localhost:8080/api/v1/partners/vehicles \
  -H "Authorization: Bearer <OPERATOR_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "plateNumber": "29B-999.88",
    "model": "Hyundai Universe Limousine",
    "vehicleType": "LIMOUSINE",
    "totalSeats": 34
  }'
```

### 5. Operator Withdrawal Request (Escrow Wallet)
```bash
curl -X POST http://localhost:8080/api/v1/partners/wallet/withdraw \
  -H "Authorization: Bearer <OPERATOR_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 5000000,
    "bankName": "Vietcombank",
    "accountNumber": "0011001234567",
    "accountHolder": "CONG TY TNHH VAN TAI PHUONG TRANG"
  }'
```

---

## 📚 Postman Collection & Documentation Index

### Automated API Test Suite
Import the comprehensive Postman Collection:
👉 **[`docs/ticket-booking-postman-collection.json`](docs/ticket-booking-postman-collection.json)**
- **7 Test Folders** covering all Bounded Contexts.
- Includes dynamic environment variable extractions and embedded `pm.test` assertions for end-to-end verification.

### Technical Blueprint Documentation
| Topic & Directory | Core Coverage & Specification Files |
| :--- | :--- |
| **[01-business-requirements/](docs/01-business-requirements/)** | **PRD & Scope:** Core problem statement, bounded contexts ([Overview](docs/01-business-requirements/01.overview-and-scope.md), [IAM](docs/01-business-requirements/02.identity-and-access.md), [Catalog](docs/01-business-requirements/03.catalog-and-caching.md), [Booking](docs/01-business-requirements/04.booking-and-concurrency.md), [Payment](docs/01-business-requirements/05.payment-and-integration.md)). |
| **[02-domain-model/](docs/02-domain-model/)** | **Domain & Ubiquitous Language:** [Ubiquitous Dictionary](docs/02-domain-model/01.ubiquitous-language.md), [Context Mapping](docs/02-domain-model/02.context-mapping.md), [Aggregate Roots](docs/02-domain-model/03.aggregate-roots.md), [Domain Events](docs/02-domain-model/04.domain-events.md). |
| **[03-database-design/](docs/03-database-design/)** | **Database & Indexing:** [Complete ERD & DDL](docs/03-database-design/01.schema-and-erd.md), [Composite Indexing Strategy](docs/03-database-design/02.indexing-strategy.md), [Redis In-Memory Topologies](docs/03-database-design/03.redis-data-structures.md). |
| **[04-api-and-events/](docs/04-api-and-events/)** | **API & Event Specs:** [ApiResponse Envelope & Error Codes](docs/04-api-and-events/01.api-envelope-and-standards.md), [RESTful Endpoints Spec](docs/04-api-and-events/02.endpoints-specification.md), [Message Contracts](docs/04-api-and-events/03.event-message-contracts.md). |
| **[05-architecture-fsm/](docs/05-architecture-fsm/)** | **Architecture & FSM:** [Hexagonal Clean Architecture](docs/05-architecture-fsm/01.clean-hexagonal-architecture.md), [Booking FSM](docs/05-architecture-fsm/02.booking-state-machine.md), [Concurrency Workflows](docs/05-architecture-fsm/03.concurrency-workflows.md), [Master Traceability Matrix](docs/05-architecture-fsm/04.master-traceability-matrix.md). |

---

## 👤 Author & Engineering Focus
- **Role:** Backend Developer
- **Technical Expertise:** Distributed Systems, High-Concurrency Concurrency Controls, Hexagonal / Clean Architecture, Domain-Driven Design, Java 21 / Spring Boot.
- **License:** Open for technical evaluation, code review, and engineering recruitment.
