# Mini ADR: Seat Hold Command Idempotency (Revised Option B)

- **Status:** Approved for Implementation Planning (Gate B5.2-I)
- **Context:** VeloBus Booking Context — `POST /api/v1/bookings/hold`
- **Database Engine:** MySQL 8.4 InnoDB (Relational Storage) + Redis 7.2 (Distributed Locks)
- **Date:** 2026-11-26
- **Authors:** Core Architecture Team & AI Engineering Assistant

---

## 1. Context and Problem Statement

When a customer initiates seat reservation (`POST /api/v1/bookings/hold`), network timeouts, client disconnects, or browser retries can create ambiguous mutation states.
Currently, `HoldSeatUseCase` acquires Redis distributed locks via `SeatLockRepository.acquireLocks` and writes a `Booking` aggregate with status `HELD` to MySQL.
If a request times out from the client's perspective after Redis locks are acquired or after MySQL commits, subsequent retries by the user could:
1. Fail with `SEAT_ALREADY_RESERVED` because the seats are already locked in Redis/MySQL by the prior attempt.
2. Create duplicate booking records if seat keys differ or if retry recovery generates fresh booking references.
3. Leave the frontend unable to authoritatively correlate whether an interrupted mutation succeeded.

We require a durable request correlation and hold command idempotency mechanism scoped to the Booking Context.

---

## 2. Technical Corrections to Original ADR (Gate B5.2-I Survey)

The preliminary ADR draft contained several architectural inaccuracies that are corrected in this revision:
1. **Target Database Engine:** The actual database is **MySQL 8.4 (InnoDB)**, not PostgreSQL.
2. **Partial Index Incompatibility:** MySQL InnoDB does **not** support partial indexes (`WHERE status = 'HELD'`). An attempt to declare a filtered unique index fails in MySQL syntax.
3. **Status Independence:** Idempotency identity must **not** depend on mutable domain status (`BookingStatus`). If an index were scoped only to `HELD`, transitioning a booking to `CONFIRMED`, `CANCELLED`, or `EXPIRED` would allow duplicate holds for the same client request key, breaking replay semantics. Request correlation must remain authoritative across the entire booking lifecycle: `HELD`, `CONFIRMED`, `CANCELLED`, and `EXPIRED`.
4. **Scope of Guarantees:** Removed inaccurate claims of "perfect consistency" or "exactly-once execution". Aggregate-level idempotency primarily deduplicates **successfully committed booking effects** and provides authoritative replay for committed outcomes. It does not replace network transport resilience, replay arbitrary uncommitted failure payloads, or prevent races prior to the database transaction boundary.
5. **Latency Claims:** Removed unmeasured claims regarding zero overhead. Any index lookup and unique constraint evaluation carries measurable B-tree traversal cost.

---

## 3. Options Evaluated

### Option A: Dedicated Idempotency Table (`idempotency_keys`)
- **Design:** Generic table `idempotency_keys` storing `(key, context, request_hash, response_payload, status, expires_at)`.
- **Pros:** Generalized across contexts (payments, refunds, withdrawals).
- **Cons:** Two additional database round-trips (INSERT + UPDATE) on every reservation command; requires scheduled background jobs to purge expired keys; introduces entity sprawl outside domain boundaries.

### Option B (Revised & Selected): Durable Request Identity on Booking Aggregate
- **Design:**
  - Add nullable column `client_request_id VARCHAR(64) NULL` to table `bookings`.
  - Add composite unique constraint `CONSTRAINT uk_booking_user_client_request UNIQUE (user_id, client_request_id)`.
- **Key Characteristics:**
  - **Standard SQL / MySQL 8.x Compatibility:** In MySQL B-tree indexes, multiple `NULL` values do not collide. Legacy and existing bookings with `client_request_id = NULL` remain 100% compatible.
  - **Lifecycle Durability:** Once committed, the association between `(user_id, client_request_id)` and the booking record is permanent and status-agnostic. Replays during `HELD`, `CONFIRMED`, `CANCELLED`, or `EXPIRED` consistently locate the same authoritative aggregate.
  - **Payload Verification:** Before replaying an existing booking, the server verifies canonical payload equality (`tripId` match and exact normalized `seatNumbers` match). If a client reuses an idempotency key with a conflicting payload, the request is definitively rejected with `IDEMPOTENCY_KEY_PAYLOAD_MISMATCH`.
  - **Transactional Atomicity:** The request identifier is persisted in the exact same database transaction that creates the `Booking` and its `BookingItem` children.

### Option C: Redis-Only Idempotency Key (`SETNX` + Reference Cache)
- **Design:** Cache `{ bookingReference, status }` in Redis with TTL.
- **Cons:** Dual-write failure mode: if node crashes between Redis lock acquisition and DB commit, or between DB commit and cache update, state becomes inconsistent. Lacks durable ACID correlation across restarts or Redis eviction.

---

## 4. Comparison Matrix

| Architectural Factor | Option A (Generic Table) | Option B Revised (Booking Aggregate) | Option C (Redis-Only) |
| :--- | :--- | :--- | :--- |
| **Relational Engine** | MySQL compatible | Fully MySQL 8.x compatible | N/A (Redis only) |
| **Status Dependency** | Key status state machine | Status-independent (durable) | TTL-dependent |
| **Consistency Scope** | Strong (relational ACID) | Strong (same transaction as aggregate) | Eventual / Dual-write race |
| **Storage Overhead** | New table + cleanup cron | 1 nullable column + 1 index | In-memory RAM |
| **Failure Modes** | Orphaned keys on abort | Caught via unique constraint rollback | Split-brain on crash |
| **Lifecycle Durability** | Requires retention policy | Permanent across all booking states | Lost on eviction/expiry |

---

## 5. Architectural Decision & Concurrency Contract

### Selected Architecture: Revised Option B
We adopt **Revised Option B** for `POST /api/v1/bookings/hold`.

### Execution Flow:
1. **Authentication & Identity:** Resolve `userId` strictly from JWT Principal. Never trust client-supplied user IDs.
2. **Canonical Normalization:** Normalize seat list (trim, uppercase, distinct, alphabetical sort).
3. **Idempotency Lookup:**
   - If header `Idempotency-Key` is provided (UUID format, max 64 chars):
     - Query `bookingRepository.findByUserIdAndClientRequestId(userId, clientRequestId)`.
     - If existing booking found:
       - Validate canonical payload equality (`tripId` and `seatNumbers`).
       - If mismatch: throw `BusinessException("IDEMPOTENCY_KEY_PAYLOAD_MISMATCH", ...)`.
       - If match: return existing authoritative `BookingResponse` immediately (zero new Redis locks acquired, zero new DB rows inserted).
4. **First-Time Execution:**
   - Acquire Redis locks with `bookingReference = UUID.randomUUID().toString()`.
   - In database transaction, perform MySQL row lock (`lockSeatsForBooking`) and `findActiveBookedSeatNumbers`.
   - Persist `Booking` with `client_request_id = clientRequestId`.
   - Return committed `BookingResponse`.
5. **Race Resolution (Concurrent Same-Key Requests):**
   - If concurrent requests with the same key race past the initial check:
     - Request 1 wins DB commit and holds the unique constraint.
     - Request 2 fails with `DataIntegrityViolationException` on `uk_booking_user_client_request`.
     - Request 2 catches the violation, rolls back its transaction, safely releases only its own acquired Redis locks, opens a clean read transaction to fetch Request 1's committed booking, verifies payload equality, and replays the response.
   - If Redis rejects Request 2 while Request 1 is still in-flight:
     - Verify if a committed booking exists. If not yet committed, return fail-closed in-progress response (`HOLD_IN_PROGRESS`), advising client to retry or poll.

---

## 6. Schema Migration & Rollback Specification

### Schema DDL (MySQL 8.4):
```sql
ALTER TABLE bookings
ADD COLUMN client_request_id VARCHAR(64) NULL;

ALTER TABLE bookings
ADD CONSTRAINT uk_booking_user_client_request
UNIQUE (user_id, client_request_id);
```

### Rollback DDL:
```sql
ALTER TABLE bookings
DROP INDEX uk_booking_user_client_request;

ALTER TABLE bookings
DROP COLUMN client_request_id;
```

### Migration Execution Path:
- Integrated into `DatabaseSchemaMigrationConfig` as version `V3` (`applyV3AddBookingClientRequestIdIfAbsent`).
- Synchronized with `environment/init.sql` for clean container provisioning.
