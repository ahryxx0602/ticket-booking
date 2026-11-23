# 🎫 VeloBus — Nền Tảng Đặt Vé Xe Khách & Điều Phối Đội Xe Hiệu Năng Cao

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

Nền tảng thương mại điện tử phân phối vé xe khách liên tỉnh, điều phối đội xe và quyết toán ký quỹ (Fintech Escrow) đa đối tác cấp doanh nghiệp. Hệ thống được kiến trúc theo chuẩn mực **Kiến trúc Lục giác (Hexagonal / Clean Architecture)**, **Thiết kế hướng miền (DDD)**, và mô hình **Modular Monolith** hiện đại.

Lấy cảm hứng từ các nền tảng hàng đầu thực tế như **Vexere** và **Shopee**, VeloBus giải quyết dứt điểm các bài toán hóc búa của hệ thống phân tán: **truy xuất danh mục dưới 20ms nhờ Redis Cache, cam kết tuyệt đối không bán trùng ghế (Zero Overbooking) qua Redisson kết hợp ràng buộc Database, cổng thanh toán VNPay xử lý lặp (Idempotent), tự động hóa quyết toán ví ký quỹ nhà xe và giao diện đa phân hệ chuẩn mực theo ngôn ngữ thiết kế Kinetic Executive**.

---

## 📑 Tài Liệu Kiến Trúc Chuyên Sâu

Vui lòng tham khảo tài liệu chuyên biệt của từng phân hệ:
- ⚙️ **[Tài Liệu Kỹ Thuật Backend](backend/README.vi.md) ([English](backend/README.md)):** Thiết kế DDD, 8 Bounded Contexts, 33 Maven Modules, Khóa phân tán Redisson, Virtual Threads Java 21, và FSM.
- 🖥️ **[Tài Liệu Kỹ Thuật Frontend](frontend/README.vi.md) ([English](frontend/README.md)):** Vue 3, PrimeVue 4, Tailwind CSS, Ngôn ngữ thiết kế Kinetic Executive, 3 Portals, và kiểm thử Playwright E2E.

---

## 🏛️ Cấu Trúc Monorepo Toàn Sàn

```
ticket-booking/
├── backend/                               # Lõi phân tán Spring Boot 3.4.1 + Java 21
│   ├── shared-kernel/                     # Value Objects, Ngoại lệ lõi, Kiểu dữ liệu chung
│   ├── context-identity/                  # Xác thực, JWT, Phân quyền RBAC, Quản trị người dùng
│   ├── context-catalog/                   # Bến xe, Tuyến đường, Chuyến đi, Tầng Cache Redis
│   ├── context-booking/                   # Khóa phân tán Redisson, FSM Vòng đời đơn vé
│   ├── context-payment/                   # Cổng thanh toán VNPay HMAC-SHA512, Idempotent IPN
│   ├── context-notification/              # Thông báo đẩy FCM, Telegram Bot, Firebase Storage
│   ├── context-partner/                   # Nhà xe đối tác, Quản lý đội xe & tài xế, Ví ký quỹ
│   └── ticket-start/                      # Điểm khởi động và đóng gói toàn bộ Monolith
│
├── frontend/                              # Giao diện người dùng Vue 3 + PrimeVue 4 + Tailwind
│   ├── src/app/                           # Khung giao diện (Khách, Nhà xe, Admin, Auth) & Router
│   ├── src/modules/auth/                  # Đăng nhập, Đăng ký, Hồ sơ cá nhân & Mã OTP Telegram
│   ├── src/modules/catalog-booking/       # Tra cứu, Sơ đồ ghế 2 tầng, Thanh toán VNPay, Vé điện tử
│   ├── src/modules/partner/               # Ví ký quỹ, Quản lý đội xe, Lập lịch trình chuyến
│   ├── src/modules/admin/                 # Chỉ số KPI sàn, Cảnh báo đối soát, Backoffice
│   └── src/shared/                        # HTTP Client, Design System Kit, Composables
│
├── environment/                           # Script khởi tạo Database, stress test K6, seed data
├── docs/                                  # Toàn bộ tài liệu đặc tả kiến trúc & hướng dẫn thiết kế
├── docker-compose.yml                     # Điều phối full-stack phát triển cục bộ
├── docker-compose.prod.yml                # Đóng gói và triển khai production
└── .github/workflows/ci.yml               # Đường ống CI/CD tự động & Quét mã độc hại
```

---

## 🎨 Hệ Thống Thiết Kế UI/UX

VeloBus áp dụng ngôn ngữ thiết kế **Kinetic Executive**, được tối ưu cho các tác vụ vận hành chuyên sâu:
- **Hệ màu chuẩn:** Brand Navy (`#142B4A`), Primary Blue (`#2563EB`), Accent Amber (`#F59E0B`), Nền Canvas (`#F8FAFC`).
- **Typography:** Font chữ Inter định dạng nghiêm ngặt `font-variant-numeric: tabular-nums` ngăn chặn hiện tượng nhảy layout khi đồng hồ đếm ngược và bảng dữ liệu cập nhật theo thời gian thực.

| Mã Giao Diện | Vai Trò & Tên Giao Diện | Điểm Nhấn Kỹ Thuật Nghiệp Vụ |
| :--- | :--- | :--- |
| `seat-map` | **Khách hàng: Sơ đồ ghế 2 tầng** | Ma trận 40 ghế, đếm ngược giữ chỗ 10:00, tóm tắt đơn vé VNPay. |
| `operator-escrow` | **Chủ xe: Fintech Escrow Dashboard** | 4 thẻ ví (`AVAILABLE`, `HOLDING`, `SETTLING`, `WITHDRAWN`), KYC Vietcombank. |
| `admin-reconciliation` | **Admin: Governance & Đối Soát VNPay** | Cảnh báo IPN lệch đối soát thời gian thực, chiết khấu 2 cấp, Audit Trail. |
| `operator-fleet` | **Chủ xe: Đội xe & Lập lịch** | Bất biến Turnaround Buffer (30 phút), ngăn kéo tạo chuyến thông minh. |
| `0c0c51cfabda47c08fb8b230245acd33` | **Khách hàng: Thẻ lên xe điện tử** | Vé điện tử chuẩn hàng không, mã QR bảo mật mã hóa HMAC-SHA256. |

---

## 🔒 Các Bất Biến Nghiệp Vụ & Kỹ Thuật Trọng Yếu

1. **Cam Kết Zero Overbooking (Phòng thủ 2 lớp):**
   - Khóa phân tán: Redisson Fair Lock trên khóa `lock:trip:{tripId}:seat:{seatCode}` (TTL 10 phút).
   - Ràng buộc tầng CSDL: Bảng `booking_seats` của MySQL thiết lập Unique Composite Index `uk_trip_seat_status` trên `(trip_id, seat_code, status_active)`.
2. **Khoảng Đệm Vận Hành (Turnaround Buffer 30 Phút):**
   - Bắt buộc duy trì khoảng cách tối thiểu 30 phút giữa hai chuyến xe kế tiếp của cùng một xe hoặc một tài xế nhằm chống trùng lịch và bảo dưỡng phương tiện.
3. **Bảo Mật Fail-Closed & Cô Lập Dữ Liệu Nhà Xe (Tenant Isolation):**
   - Mọi thao tác của nhà xe bắt buộc sử dụng `findByIdAndOperatorId(id, operator.getId())`, triệt tiêu lỗ hổng IDOR.
   - Các hành động quản trị viên yêu cầu xác thực danh tính `Principal` nghiêm ngặt, tuyệt đối không dùng giá trị mặc định ngầm định.
4. **Quyết Toán Ký Quỹ Đảm Bảo Idempotency:**
   - Webhook IPN từ VNPay kiểm tra tính toàn vẹn chữ ký (HMAC-SHA512), số tiền thanh toán và chuyển trạng thái FSM an toàn ngay cả khi webhook bị gọi lặp lại.

---

## 🧪 Hệ Thống Kiểm Thử Tự Động (259 Tests)

Repository đảm bảo chất lượng với **259 bài kiểm thử tự động toàn diện**:

```
TỔNG SỐ TESTS: 259 PASSED
├── Backend (Java 21 / JUnit 5 / Testcontainers): 209 Passed
├── Frontend Unit (Vitest / Happy-DOM):              29 Passed
└── Frontend E2E & Visual (Playwright / Chromium):   21 Passed
```

```bash
# Chạy bộ test suite của Backend
cd backend && mvn test

# Chạy kiểm thử Unit của Frontend
cd frontend && npm run test:unit

# Chạy kiểm thử End-to-End và Responsive của Frontend
cd frontend && npm run test:e2e
```

---

## 🚀 Khởi Động Nhanh (Full-Stack Docker Compose)

Khởi chạy trọn bộ hệ sinh thái (Database, Cache, Backend API và Frontend SPA) chỉ với một câu lệnh:

```bash
# Clone repository về máy
git clone git@github.com:ahryxx0602/ticket-booking.git
cd ticket-booking

# Khởi chạy toàn bộ 4 containers ở chế độ chạy nền
docker compose up -d --build
```

### Các Cổng Dịch Vụ
- **Ứng dụng Web Frontend:** [http://localhost:5173](http://localhost:5173) (hoặc `http://localhost:80` ở chế độ Production)
- **Máy chủ Backend REST API:** [http://localhost:8080](http://localhost:8080)
- **Tài liệu Swagger / OpenAPI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Cơ sở dữ liệu MySQL:** `localhost:3307` (`ticket_booking_db`)
- **Bộ đệm Redis Cache:** `localhost:6379`

### Tài Khoản Seed Mặc Định (Môi Trường Dev)
- **Super Admin:** `admin` / `password123`
- **Nhà xe (Bus Operator):** `operator1` / `password123`
- **Khách hàng (Customer):** `customer1` / `password123`

---

## 📄 Bản Quyền & Tác Giả

- **Tác giả:** Phan Văn Thành (Kỹ thuật Phần mềm)
- **Repository:** [ahryxx0602/ticket-booking](https://github.com/ahryxx0602/ticket-booking)
- **Giấy phép:** MIT License — Sẵn sàng cho việc đánh giá kiến trúc, kiểm tra portfolio CV và ứng dụng thực tiễn.
