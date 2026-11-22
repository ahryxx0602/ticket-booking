# 🎫 Hệ Thống Đặt Vé Xe Trực Tuyến Tải Cao (Modular Monolith)

<p align="center">
  🌐 <b><a href="README.md">English</a></b> | <b><a href="README.vi.md">Tiếng Việt</a></b>
</p>

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.1-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.4%20LTS-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7.2-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![Redisson](https://img.shields.io/badge/Redisson-Distributed_Lock-009688?style=for-the-badge)](https://redisson.org/)
[![Architecture](https://img.shields.io/badge/Kiến_Trúc-Clean%20%26%20Hexagonal%20%7C%20DDD-blue?style=for-the-badge)]()
[![Tests](https://img.shields.io/badge/Tests-209%20Đạt%20(33%20Modules)-success?style=for-the-badge&logo=junit5&logoColor=white)]()

Nền tảng đặt vé xe khách trực tuyến đa nhà xe (Multi-Tenant Marketplace), chịu tải cao cấp độ doanh nghiệp, được thiết kế theo kiến trúc **Clean / Hexagonal Architecture** kết hợp phương pháp luận **Domain-Driven Design (DDD)** trên nền tảng **Java 21** và **Spring Boot 3.4.1**. Lấy cảm hứng từ mô hình sàn thương mại vận tải thực tế như **Vexere** và **Shopee**, hệ thống giải quyết trọn vẹn các bài toán phân tán phức tạp: **truy vấn danh mục mili-giây, chống bán vé trùng lặp tuyệt đối (Zero Overbooking) bằng khóa phân tán, xử lý thanh toán bất biến (Idempotent Payment), thông báo đa kênh (Telegram Bot, Email QR, FCM) và sàn quản trị đội xe với cơ chế quyết toán ký quỹ tự động (Escrow Settlement)**.

---

## 🏛️ Mô Hình Phân Lớp Clean Architecture & DDD

Dự án tuân thủ nghiêm ngặt **Quy tắc Phụ thuộc (Dependency Rule)** của Clean Architecture: chiều phụ thuộc luôn hướng vào trong. Lớp **Domain Layer** chứa toàn bộ quy tắc nghiệp vụ cốt lõi, hoàn toàn thuần khiết và không phụ thuộc (0%) vào bất kỳ framework, ORM hay thư viện hạ tầng bên ngoài nào.

```
       ┌────────────────────────────────────────────────────────┐
       │             Presentation Layer (REST Controllers)      │
       └──────────────────────────┬─────────────────────────────┘
                                  ▼
       ┌────────────────────────────────────────────────────────┐
       │       Application Layer (UseCases, DTOs, Listeners)    │
       └──────────────────────────┬─────────────────────────────┘
                                  ▼
       ┌────────────────────────────────────────────────────────┐
       │     Domain Layer (Models, Enums, Events, Ports, FSM)   │  ◄── CORE DOMAIN (Thuần Java 100%)
       └──────────────────────────▲─────────────────────────────┘
                                  │ (Thực thi Outbound Ports)
       ┌──────────────────────────┴─────────────────────────────┐
       │      Infrastructure Layer (JPA Entities, Redis, Adapters)│
       └────────────────────────────────────────────────────────┘
```

### Bảng Phân Tách Trách Nhiệm Từng Tầng

| Tầng Kiến Trúc | Thành Phần Cốt Lõi | Mẫu Thiết Kế Doanh Nghiệp & Ánh Xạ |
| :--- | :--- | :--- |
| **Domain Layer** (`*-domain`) | • **Models:** Pure Domain POJOs (`Booking`, `Trip`, `OperatorWallet`, `Banner`)<br>• **Enums:** Kiểu dữ liệu an toàn (`BookingStatus`, `OperatorStatus`, `VehicleType`)<br>• **Events:** Sự kiện miền bất biến (`SeatHeldEvent`, `PaymentConfirmedEvent`, `BookingRefundedEvent`)<br>• **FSM:** Máy trạng thái hữu hạn (`BookingStateMachine`)<br>• **Ports:** Giao diện Outbound Ports (`BookingRepository`, `PartnerCatalogPort`) | **Core Domain (Pure Java):** Nghiêm cấm mọi import framework (`org.springframework.*`, `jakarta.persistence.*`). Đóng gói trọn vẹn quy tắc và bất biến nghiệp vụ. |
| **Application Layer** (`*-application`) | • **UseCases:** Xử lý Command/Query độc lập (`HoldSeatUseCase`, `PartnerCommandService`)<br>• **DTOs:** Định dạng dữ liệu vào/ra kèm Jakarta Validation (`@Valid`)<br>• **Event Listeners:** Điều phối sự kiện xuyên ngữ cảnh (`@TransactionalEventListener`) | **Inbound Ports & CQRS Orchestration:** Thẩm định đầu vào, điều phối Aggregate Roots, phát sự kiện tích hợp. |
| **Infrastructure Layer** (`*-infrastructure`) | • **JPA Entities & Repos:** Ánh xạ CSDL và triển khai Spring Data adapters<br>• **Distributed Locks:** Bộ điều hợp Redisson Redis & Token Lua CAS<br>• **Schedulers:** Tác vụ quét quyết toán ký quỹ & worker giải phóng đơn quá hạn<br>• **External Gateways:** Cổng VNPay HMAC-SHA512, Telegram Bot API, SMTP Mailer, ZXing QR | **Outbound Adapters:** Xử lý I/O cơ sở dữ liệu, xóa cache Redis, cô lập độ trễ mạng và tích hợp dịch vụ bên thứ 3. |
| **Presentation Layer** (`*-presentation`) | • **REST Controllers:** Điều hướng HTTP, tuần tự hóa JSON, phân quyền RBAC (`@PreAuthorize`)<br>• **Global Error Advice:** Chuẩn hóa vỏ bọc phản hồi HTTP (`ApiResponse<T>`) | **Inbound Adapters:** Chuyển đổi HTTP requests thành commands/queries cho tầng ứng dụng. |

---

## 🌟 Điểm Nhấn Kỹ Thuật Đột Phá

1. **Cam Kết Không Bán Trùng Ghế (Zero Overbooking via Distributed Locking):**
   - Kết hợp **Khóa phân tán Redisson** (`RLock`) và **Token Owner Lua CAS** (`setIfAbsent(ownerToken, ttl)`) thực thi **bên ngoài** database transaction. Triệt tiêu hoàn toàn race condition, deadlock và lock-stealing khi có hàng ngàn yêu cầu giữ cùng một ghế.
2. **Độ Trễ Tra Cứu Dưới 20ms (Cache-Aside với Non-blocking SCAN/UNLINK):**
   - Bộ nhớ đệm Redis hấp thụ hơn 90% lưu lượng đọc trong giờ cao điểm. Cơ chế làm mới cache thực thi bất đồng bộ trong `TransactionSynchronization.afterCommit()` bằng con trỏ `SCAN` + `UNLINK` không chặn luồng chính của Redis.
3. **Mô Hình Sàn Đa Nhà Xe & Triệt Tiêu Lỗ Hổng IDOR (ADR-16):**
   - Chủ nhà xe (`ROLE_OPERATOR`) đăng ký tài khoản, nộp hồ sơ chờ Admin duyệt, chủ động quản lý đội xe, sơ đồ ghế, danh sách tài xế và lập lịch trình.
   - Bảo mật Multi-Tenant tuyệt đối: Mọi thao tác đều giải mã danh tính thực từ JWT Token và truy vấn có ràng buộc kép `findByIdAndOperatorId(id, operatorId)`.
4. **Động Cơ Hoa Hồng 2 Cấp & Ví Ký Quỹ Quyết Toán Tự Động (ADR-13, 14, 15):**
   - **Hoa hồng 2 cấp:** Tỷ lệ mặc định toàn sàn (10%) kết hợp quyền ghi đè tỷ lệ riêng cho từng nhà xe (ví dụ: 7% cho đối tác VIP), được snapshot cố định ngay tại thời điểm thanh toán vé.
   - **Ví ký quỹ Escrow 4 số dư:** `pending_balance` (chờ quyết toán), `available_balance` (khả dụng), `locked_balance` (tạm khóa rút tiền), `debt_balance` (nợ xấu).
   - **Quyết toán nguyên tử (Atomic Check-and-Set):** `EscrowSettlementScheduler` quét các chuyến đi sau giờ khởi hành +2 giờ, sử dụng câu lệnh DB CAS (`UPDATE trips SET settlement_status = 'SETTLED' WHERE id = :id AND settlement_status = 'UNSETTLED'`), triệt tiêu rủi ro quyết toán tiền 2 lần khi chạy nhiều replica.
   - **Bảo vệ nợ xấu:** Các đơn hoàn vé sau khi chuyến đi đã quyết toán được ghi nhận vào nợ xấu và tự động cấn trừ ở các chuyến tiếp theo; chặn tạo lệnh rút tiền khi còn nợ xấu.
5. **Bộ Đệm An Toàn Xoay Vòng Phương Tiện (Turnaround Buffer - LSN-14):**
   - Ngăn chặn trễ chuyến dây chuyền bằng cơ chế tự động chặn xếp chuyến nếu khoảng cách giữa 2 chuyến đi của cùng 1 xe nhỏ hơn **30 phút xoay vòng**.
6. **Kiến Trúc Thông Báo Đa Kênh Bất Đồng Bộ (Virtual Threads & Event-Driven):**
   - **Telegram Bot thông minh:** Hỗ trợ đa vai trò với mã liên kết OTP 8 ký tự an toàn (Atomic Redis `GETDEL`), tra cứu vé công khai `/ve`, và cảnh báo tài chính cho Admin.
   - **Vé điện tử mã hóa QR:** Nhúng mã QR Base64 ký số bằng **HMAC-SHA256** (`v1|ref|tripId|hash`) cho phép nhân viên nhà xe soát vé offline không cần Internet.
   - **Virtual Threads (Project Loom):** Đẩy toàn bộ tác vụ mạng sang Java 21 Virtual Threads, đảm bảo thời gian phản hồi API core luôn dưới 200ms.
7. **Xử Lý Thanh Toán Bất Biến (Idempotent Payment - ADR-07, ADR-08):**
   - Tích hợp cổng thanh toán VNPay v2.1.0 với chữ ký số HMAC-SHA512 nghiêm ngặt, đối chiếu chữ ký an toàn tránh Timing Attack (`MessageDigest.isEqual`), và cơ chế xử lý webhook chống retry storm.

---

## 📁 Cấu Trúc Dự Án Modular Monolith (33 Modules)

```text
ticket-booking/
├── environment/                         # Hạ tầng Containerized Local
│   ├── docker-compose.yml               # MySQL 8.4 LTS (Cổng 3307) + Redis 7.2 (Cổng 6379)
│   └── init.sql                         # Khởi tạo Schema DDL, Composite Indexes & Seed Data
│
├── shared-kernel/                       # Nguyên thủy chia sẻ & Sự kiện tích hợp toàn hệ thống
│   ├── BaseEntity, ApiResponse, DistributedLockPort
│   └── Integration Events (PaymentConfirmedEvent, BookingRefundedEvent, etc.)
│
├── context-identity/                    # BOUNDED CONTEXT 1: Định Danh & Phân Quyền (IAM)
│   ├── identity-domain/                 # User Aggregate, UserRole, UserStatus, Repository Port
│   ├── identity-application/            # LoginUseCase, RegisterUseCase, Quản lý tài khoản
│   ├── identity-infrastructure/         # Spring Security 6, Stateless JJWT, BCrypt, JPA Adapter
│   └── identity-presentation/           # REST Controllers (/api/v1/auth, /api/v1/users)
│
├── context-catalog/                     # BOUNDED CONTEXT 2: Tuyến Đường, Chuyến Xe & Lịch Trình
│   ├── catalog-domain/                  # Trip Aggregate, Station, Seat, Chính sách định giá
│   ├── catalog-application/             # SearchTripsUseCase, AdminTripService
│   ├── catalog-infrastructure/          # Redis Cache-Aside Adapter, Spring Data JPA
│   └── catalog-presentation/            # REST Controllers (/api/v1/catalog)
│
├── context-booking/                     # BOUNDED CONTEXT 3: Đặt Chỗ & Kiểm Soát Đồng Thời
│   ├── booking-domain/                  # Booking Aggregate Root, Booking FSM, Bất biến giữ chỗ
│   ├── booking-application/             # HoldSeatUseCase, ConfirmBookingUseCase, CancelBookingUseCase
│   ├── booking-infrastructure/          # Redisson Distributed Lock Adapter, Spring Data JPA
│   └── booking-presentation/            # REST Controllers (/api/v1/bookings)
│
├── context-payment/                     # BOUNDED CONTEXT 4: Giao Dịch Tài Chính & Cổng Thanh Toán
│   ├── payment-domain/                  # Payment Aggregate, PaymentStatus, PaymentMethod
│   ├── payment-application/             # CreatePaymentUseCase, ProcessIpnUseCase, AdminRefundService
│   ├── payment-infrastructure/          # VNPay HMAC-SHA512 Adapter, Chốt chặn Idempotent Webhook
│   └── payment-presentation/            # Payment & Webhook Controllers (/api/v1/payments)
│
├── context-notification/                # BOUNDED CONTEXT 5: Thông Báo Sự Kiện Đa Kênh
│   ├── notification-domain/             # Notification Aggregate, NotificationChannel, TelegramAccount
│   ├── notification-application/        # SendNotificationUseCase, TelegramBotUseCase, OtpService
│   ├── notification-infrastructure/     # Virtual Threads Executor, SMTP Mailer, Telegram API, ZXing QR
│   └── notification-presentation/       # Notification & Telegram Webhook Controllers (/api/v1/notifications)
│
├── context-partner/                     # BOUNDED CONTEXT 6: Sàn Đa Nhà Xe, Đội Xe & Ví Ký Quỹ
│   ├── partner-domain/                  # BusOperator, Vehicle, Driver, OperatorWallet, Banner
│   ├── partner-application/             # PartnerCommandService, PartnerAdminService, Escrow Logic
│   ├── partner-infrastructure/          # EscrowSettlementScheduler, Atomic CAS Repositories, JPA
│   └── partner-presentation/            # Partner, Admin & Public REST Controllers (/api/v1/partners, /api/v1/public)
│
└── ticket-start/                        # RUNNABLE SPRING BOOT ENTRYPOINT
    ├── pom.xml                          # Assembly & Spring Boot repackage plugin
    └── src/main/
        ├── java/com/ticket/
        │   ├── TicketApplication.java   # Điểm khởi chạy hệ thống
        │   ├── adapter/                 # Bộ điều hợp nối chéo các Bounded Contexts
        │   └── config/                  # GlobalExceptionHandler, SecurityConfig, RedissonConfig
        └── resources/                   # application.yml, application-dev.yml
```

---

## ⚡ Quy Trình Xử Lý Đồng Thời & Máy Trạng Thái (FSM)

### 1. Luồng Giữ Ghế Nguyên Tử 2 Cấp (Redisson Lock Flow)
```
Yêu cầu khách hàng ──► BookingController ──► HoldSeatUseCase
                                                   │
               ┌───────────────────────────────────┴───────────────────────────────────┐
               ▼                                                                       ▼
     Chiếm khóa Redisson Mutex Key                                          Ghế đã bị người khác giữ?
     "lock:seat:{tripId}:{seatNum}"                                                    │
     (TTL: 10 phút, Chờ: 2s)                                                           │
               │                                                                       ▼
               ├────────► Thành công ──► Tạo Đơn (HELD) ──► 200 OK                  400 Bad Request
               │                         Phát SeatHeldEvent                         (SEAT_ALREADY_RESERVED)
               └────────► Timeout / Thất bại ──► Tự động nhả khóa
```

### 2. Vòng Đời Đơn Hàng Hoàn Chỉnh (Deterministic FSM)
```
  [PENDING] ────► acquireLocks() ────► [ HELD ] ────► IPN Webhook (Thành công) ────► [ CONFIRMED ]
      │                                    │                                                 │
      └─────────► Hủy / Thất bại           ├────────► Hủy thủ công ──► [ CANCELLED ]         ├─► Admin Hoàn tiền
                       │                   │                                                 │         │
                       ▼                   └────────► Hết hạn 10m TTL ─► [ EXPIRED ]         ▼         ▼
                  [TERMINATED]                                                              [ REFUNDED ]
```

### 3. Quy Trình Ký Quỹ & Quyết Toán Ví Nhà Xe
```
Khách hàng mua vé & thanh toán thành công
       │
       ▼
PaymentConfirmedEvent ──► Ghi có vào Pending Balance (Doanh thu trừ hoa hồng snapshot)
       │
       ▼
Xe xuất bến ──► Trôi qua 2 giờ sau giờ khởi hành
       │
       ▼
EscrowSettlementScheduler (Thực thi Atomic DB CAS Update)
       │
       ├─► CAS thành công (Replica đầu tiên) ──► Chuyển Pending sang Available Balance
       │                                         (Tự động cấn trừ debtBalance nếu có)
       └─► CAS thất bại (Các replica đến sau) ──► Bỏ qua (Triệt tiêu 100% quyết toán trùng)
```

---

## 🛠️ Công Nghệ & Công Cụ Sử Dụng

| Lĩnh Vực | Danh Mục Công Nghệ |
| :--- | :--- |
| **Ngôn Ngữ & Môi Trường** | **Java 21 LTS**, Virtual Threads (Project Loom) |
| **Khung Ứng Dụng (Frameworks)** | **Spring Boot 3.4.1**, Spring Data JPA, Spring Security 6 |
| **Hệ Thống Phân Tán & Cache** | **Redis 7.2**, **Redisson 3.40** (Distributed Locks & In-Memory Caching) |
| **Cơ Sở Dữ Liệu Quan Hệ** | **MySQL 8.4 LTS**, HikariCP Connection Pooling |
| **Bảo Mật & Mật Mã Học** | JJWT 0.12, BCrypt, HMAC-SHA512 (VNPay), HMAC-SHA256 (Signed QR) |
| **Tích Hợp Dịch Vụ Ngoài** | Cổng VNPay, Telegram Bot API, ZXing QR Generator, Jakarta Mail |
| **Kiểm Thử & Đảm Bảo Chất Lượng** | JUnit 5, Mockito, AssertJ, Spring MockMvc, Postman / Newman E2E |

---

## 🚀 Hướng Dẫn Cài Đặt & Khởi Chạy Nhanh

### Yêu Cầu Môi Trường
- **JDK 21** trở lên (đã cấu hình biến môi trường `JAVA_HOME`)
- **Apache Maven 3.8+**
- **Docker & Docker Compose**

### 1. Khởi Chạy Hạ Tầng (Docker)
Khởi chạy container MySQL 8.4 và Redis 7.2:
```bash
cd environment
docker compose up -d
```
> **Thông tin kết nối mặc định:**
> - **MySQL 8.4:** `localhost:3307` | Database: `ticket_booking_db` | User: `ticket_user` | Mật khẩu: `ticket_password`
> - **Redis 7.2:** `localhost:6379` | Mật khẩu: *Không cài (Mặc định)*

### 2. Biên Dịch & Chạy Toàn Bộ Test Suite (33 Modules)
Thực thi kiểm thử tự động trên toàn bộ 33 modules (209 automated unit/integration tests):
```bash
mvn clean test
```

### 3. Khởi Chạy Ứng Dụng
Chạy ứng dụng từ module thực thi `ticket-start`:
```bash
mvn spring-boot:run -pl ticket-start
```
Ứng dụng sẽ khởi động và lắng nghe tại cổng **`http://localhost:8080`**.

---

## 👥 Danh Sách Tài Khoản Mẫu Để Kiểm Thử

Hệ thống đã nạp sẵn các tài khoản mẫu khi khởi động CSDL:

| Vai Trò (Role) | Tên Đăng Nhập | Mật Khẩu Khởi Tạo | Quyền Hạn Thực Tế |
| :--- | :--- | :--- | :--- |
| **Platform Admin** | `admin` | `admin_dev_password` | Quản trị toàn sàn, duyệt hồ sơ nhà xe, cấu hình hoa hồng riêng, duyệt rút tiền, duyệt banner |
| **Bus Operator** | `operator1` | `operator_dev_password` | Quản trị đội xe, cấu hình sơ đồ ghế, lập lịch chuyến xe (turnaround buffer 30p), ví tiền & rút tiền |
| **Customer** | `customer1` | `customer_dev_password` | Tìm kiếm chuyến, giữ chỗ thời gian thực, thanh toán vé, quản lý lịch sử đặt vé |

---

## 📡 Mẫu Lệnh Kiểm Tra API Thực Tế (cURL Snippets)

### 1. Đăng Nhập Lấy Token JWT
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "customer1",
    "password": "customer_dev_password"
  }'
```

### 2. Tra Cứu Chuyến Xe (Có Cache Redis)
```bash
curl -X GET "http://localhost:8080/api/v1/catalog/trips"
```

### 3. Giữ Chỗ Đồng Thời (Khóa Phân Tán 10 Phút)
```bash
curl -X POST http://localhost:8080/api/v1/bookings/hold \
  -H "Authorization: Bearer <CUSTOMER_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "tripId": 1,
    "seatNumbers": ["12A", "12B"]
  }'
```

### 4. Nhà Xe Đăng Ký Thêm Phương Tiện Vào Đội Xe
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

### 5. Nhà Xe Yêu Cầu Rút Tiền Từ Số Dư Khả Dụng
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

## 📖 Tài Liệu API Trực Quan (Swagger UI) & Kiểm Thử Tải Cao (k6)

### 1. Giao Diện OpenAPI 3.0 / Swagger UI
Khi ứng dụng khởi chạy, truy cập bảng điều khiển tương tác trực quan ngay trên trình duyệt:
👉 **`http://localhost:8080/swagger-ui.html`**
- Hiển thị đầy đủ 45+ endpoints, cấu trúc DTO đầu vào/đầu ra và mã lỗi hệ thống.
- **Nút Authorize tiện lợi:** Bấm nút **Authorize** màu xanh để dán mã JWT Token kiểm thử trực tiếp các API phân quyền (`ROLE_CUSTOMER`, `ROLE_OPERATOR`, `ROLE_ADMIN`) mà không cần cài đặt Postman.
- File đặc tả OpenAPI JSON: `http://localhost:8080/v3/api-docs`.

### 2. Kiểm Thử Tải Cao Flash-Sale Thực Nghiệm với k6
Dự án tích hợp sẵn kịch bản kiểm thử hiệu năng chịu tải phân tán mô phỏng kịch bản flash-sale khốc liệt:
```bash
# Thực thi kịch bản kiểm thử tải cao với k6
k6 run environment/stress-test-k6.js
```
- **Kịch bản mô phỏng:** Đẩy tải lên đến **1,000 Người dùng ảo (VUs)** đồng thời đăng nhập và tranh nhau giữ đúng 2 ghế VIP `12A`, `12B`.
- **Bất biến được kiểm chứng:** Chứng minh **Zero Overbooking** (`successful_hold_seats <= 1`), xác nhận trả về 400 Bad Request hợp lệ cho các request đến sau, và tỷ lệ lỗi hệ thống `< 1%`.

### 3. Đóng Gói Triển Khai Thực Tế Với Docker
Khởi chạy toàn bộ hệ sinh thái (Backend Spring Boot + MySQL 8.4 + Redis 7.2) chỉ với 1 câu lệnh:
```bash
docker compose -f docker-compose.prod.yml up -d --build
```

---

## 📚 Bộ Sưu Tập Postman & Mục Lục Tài Liệu Kỹ Thuật

### Bộ Kiểm Thử Hợp Đồng API Tự Động (Postman)
Tải và nhập bộ sưu tập Postman hoàn chỉnh:
👉 **[`docs/ticket-booking-postman-collection.json`](docs/ticket-booking-postman-collection.json)**
- Gồm **7 Thư mục kiểm thử** bao phủ toàn bộ Bounded Contexts.
- Tích hợp trích xuất biến môi trường liên hoàn và các bài kiểm tra tự động `pm.test`.

### Mục Lục Đặc Tả Kiến Trúc & Thiết Kế Hệ Thống
| Chuyên Đề & Thư Mục | Phạm Vi Đặc Tả & Tập Tin Chi Tiết |
| :--- | :--- |
| **[01-business-requirements/](docs/01-business-requirements/)** | **Yêu Cầu Nghiệp Vụ (PRD):** Bài toán nghiệp vụ cốt lõi, phạm vi bounded contexts ([Tổng quan](docs/01-business-requirements/01.overview-and-scope.md), [IAM](docs/01-business-requirements/02.identity-and-access.md), [Catalog](docs/01-business-requirements/03.catalog-and-caching.md), [Booking](docs/01-business-requirements/04.booking-and-concurrency.md), [Payment](docs/01-business-requirements/05.payment-and-integration.md)). |
| **[02-domain-model/](docs/02-domain-model/)** | **Mô Hình Miền & Ngôn Ngữ Chung (DDD):** [Từ điển Ubiquitous Language](docs/02-domain-model/01.ubiquitous-language.md), [Bản đồ Ngữ cảnh](docs/02-domain-model/02.context-mapping.md), [Gốc Tổng Hợp Aggregate Roots](docs/02-domain-model/03.aggregate-roots.md), [Sự Kiện Miền](docs/02-domain-model/04.domain-events.md). |
| **[03-database-design/](docs/03-database-design/)** | **Thiết Kế CSDL & Đánh Chỉ Mục:** [Toàn văn Schema ERD & DDL](docs/03-database-design/01.schema-and-erd.md), [Chiến lược Composite Indexes](docs/03-database-design/02.indexing-strategy.md), [Cấu trúc dữ liệu Redis In-Memory](docs/03-database-design/03.redis-data-structures.md). |
| **[04-api-and-events/](docs/04-api-and-events/)** | **Đặc Tả Hợp Đồng REST API & Sự Kiện:** [Chuẩn vỏ bọc ApiResponse](docs/04-api-and-events/01.api-envelope-and-standards.md), [Đặc tả Endpoints](docs/04-api-and-events/02.endpoints-specification.md), [Hợp đồng Thông điệp Sự kiện](docs/04-api-and-events/03.event-message-contracts.md). |
| **[05-architecture-fsm/](docs/05-architecture-fsm/)** | **Kiến Trúc & Máy Trạng Thái:** [Kiến trúc Clean Hexagonal](docs/05-architecture-fsm/01.clean-hexagonal-architecture.md), [Máy trạng thái Booking FSM](docs/05-architecture-fsm/02.booking-state-machine.md), [Luồng xử lý đồng thời](docs/05-architecture-fsm/03.concurrency-workflows.md), [Ma trận truy vết Master Traceability](docs/05-architecture-fsm/04.master-traceability-matrix.md). |

---

## 👤 Tác Giả & Định Hướng Kỹ Thuật
- **Vai trò:** Backend Developer
- **Chuyên môn trọng tâm:** Hệ thống phân tán (Distributed Systems), Kiểm soát đồng thời tải cao (High Concurrency), Kiến trúc Hexagonal / Clean Architecture, Thiết kế hướng miền (DDD), Java 21 / Spring Boot.
- **Bản quyền:** Sẵn sàng cho việc thẩm định kỹ thuật, kiểm toán mã nguồn và tuyển dụng kỹ sư phần mềm.
