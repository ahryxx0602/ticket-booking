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
├── scripts/                               # Bộ script tự động hóa Hybrid Dev, quản lý PID & smoke-test
├── docs/                                  # Toàn bộ tài liệu đặc tả kiến trúc & hướng dẫn thiết kế
├── Makefile                               # Điểm vào CLI thống nhất với 20 lệnh phát triển
├── docker-compose.dev.yml                 # Hạ tầng Hybrid Dev (MySQL & Redis bind 127.0.0.1)
├── docker-compose.yml                     # Điều phối Full Docker Integration (4 containers)
├── docker-compose.prod.yml                # Đóng gói và triển khai Production
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

## ⚡ Môi Trường Phát Triển Lai — Hybrid Development Environment (Khuyên Dùng Hằng Ngày)

Để tối ưu tốc độ phản hồi khi phát triển trên **WSL2 Ubuntu** mà không cần rebuild Docker image mỗi lần sửa code, VeloBus triển khai kiến trúc **Hybrid Development Environment**:
- **Hạ tầng trong Docker (`docker-compose.dev.yml`):** Chỉ chạy `ticket-mysql` (`127.0.0.1:3307`) và `ticket-redis` (`127.0.0.1:6379`) với named volumes bền vững (`mysql_dev_data`, `redis_dev_data`).
- **Frontend Chạy Local:** Vite Dev Server (`http://127.0.0.1:5173`) hỗ trợ HMR tức thì cho Vue 3 + Tailwind CSS và reverse proxy `/api` sang `http://127.0.0.1:8080`.
- **Backend Chạy Local:** Java 21 + Spring Boot (`http://127.0.0.1:8080`, profile `dev`) tích hợp **Spring Boot DevTools** + **Multi-Module Reactor Workspace Classpath** (`*/target/classes` của toàn bộ 26 modules) + **Automatic Incremental Compiler Watcher** (`mvn compile -pl <changed-module> -am -T 1C`). Khi sửa code ở bất kỳ module nào, hệ thống tự động biên dịch incremental (`~1s`) và restart Spring context (`~2.6s`).

### 1. Kiểm Tra Công Cụ & Khởi Tạo Lần Đầu
```bash
# Kiểm tra JDK 21, Maven, Node 22+, Docker Compose V2 và vị trí filesystem WSL2
make doctor

# Khởi tạo .env từ .env.example, cài đặt npm dependencies và biên dịch Maven reactor
make setup
```

### 2. Khởi Động Hằng Ngày & Kiểm Chứng Auto-Reload
```bash
# Khởi động MySQL & Redis (Docker) + Backend (Local DevTools + Auto-Compiler) + Frontend (Vite HMR)
make dev
```
- **Chạy từng phần hoặc kết hợp với IDE (IntelliJ / VS Code):**
  - `make infra` — Chỉ bật MySQL (`127.0.0.1:3307`) và Redis (`127.0.0.1:6379`).
  - `make backend` — Chạy Backend local kèm trình theo dõi tự động biên dịch multi-module (hoặc chạy/debug `com.ticket.TicketApplication` trực tiếp trong IDE; `make dev` tự động phát hiện và tái sử dụng tiến trình Backend do IDE quản lý trên cổng `8080`, hoặc bật `DEBUG=true make backend` để mở cổng debug JDWP `127.0.0.1:5005`).
  - `make frontend` — Chạy Vite Dev Server local (`127.0.0.1:5173`).
- **Cách kiểm tra Auto-Reload:**
  - Sửa file `.vue` / `.js` / Tailwind trong `frontend/src/` $\to$ Vite HMR cập nhật giao diện ngay lập tức.
  - Sửa file `.java` / `.yml` trong `ticket-start` hoặc bất kỳ module phụ thuộc nào (`context-catalog`, `context-booking`, v.v.) $\to$ watcher tự động biên dịch module thay đổi và DevTools tự động khởi động lại ngữ cảnh Spring trong ~2–3 giây.
- **Dừng Hệ Thống An Toàn:**
  - `make stop` — Dừng các tiến trình local theo đúng PID đã xác thực (không ảnh hưởng các tiến trình Java/Node khác trên máy và giữ nguyên dữ liệu DB).
  - `make infra-stop` — Dừng container `ticket-mysql` và `ticket-redis` (bảo toàn volumes).

### 3. Bảng Tham Chiếu Lệnh Makefile (20 Commands)

| Câu Lệnh | Chức Năng |
| :--- | :--- |
| `make help` | Hiển thị danh sách toàn bộ câu lệnh hỗ trợ |
| `make doctor` | Kiểm tra prerequisites (JDK 21, Maven, Node, Docker Compose V2, WSL2 filesystem) |
| `make setup` | Khởi tạo `.env`, cài đặt dependencies frontend và biên dịch backend |
| `make infra` | Khởi động MySQL và Redis trong Docker (`docker-compose.dev.yml`) và chờ healthcheck |
| `make infra-stop` | Dừng MySQL và Redis containers (bảo toàn volumes) |
| `make infra-status` | Kiểm tra trạng thái các container hạ tầng |
| `make dev` | Khởi động trọn bộ Hybrid Development Environment (Infra + Backend + Vite HMR) |
| `make backend` | Khởi động Backend Spring Boot local + Multi-module Auto-Compile Watcher |
| `make frontend` | Khởi động Frontend Vite Dev Server local (HMR) |
| `make stop` | Dừng các tiến trình ứng dụng local an toàn theo PID |
| `make logs` | Xem log trực tiếp của MySQL và Redis containers |
| `make smoke-test` | Kiểm thử khói tự động (Infra + Actuator Health + Catalog API + Vite/Nginx Proxy) |
| `make test` | Chạy toàn bộ unit & integration tests của Backend và Frontend |
| `make test-backend` | Chạy bộ test Maven của Backend |
| `make test-frontend` | Chạy bộ test Vitest của Frontend |
| `make build` | Build Docker images cho Backend và Frontend |
| `make integration` | Khởi động Full Docker Integration Environment gồm 4 containers (`docker-compose.yml`) |
| `make prod` | Khởi động môi trường Production (`docker-compose.prod.yml`) |
| `make clean` | Dọn dẹp thư mục build `target/` và `dist/` an toàn (không xóa DB volumes) |
| `make reset-db` | Xóa và khởi tạo lại DB dev từ `environment/init.sql` (có xác nhận `[y/N]`) |

---

## 🚀 Chạy Tích Hợp Full Docker & Triển Khai Production

Khi cần kiểm thử tích hợp toàn bộ 4 containers trong Docker hoặc kiểm tra bản build Production:

```bash
# Khởi chạy cả 4 containers (MySQL + Redis + Backend JAR + Frontend Nginx)
make integration

# Kiểm tra sức khỏe toàn hệ thống và API proxy
make smoke-test
```

### Các Cổng Dịch Vụ
- **Ứng dụng Web Frontend:** [http://127.0.0.1:5173](http://127.0.0.1:5173) (hoặc `http://localhost:80` ở chế độ Production)
- **Máy chủ Backend REST API:** [http://127.0.0.1:8080](http://127.0.0.1:8080)
- **Tài liệu Swagger / OpenAPI:** [http://127.0.0.1:8080/swagger-ui.html](http://127.0.0.1:8080/swagger-ui.html)
- **Actuator Health Probe:** [http://127.0.0.1:8080/actuator/health](http://127.0.0.1:8080/actuator/health)
- **Cơ sở dữ liệu MySQL:** `127.0.0.1:3307` (`ticket_booking_db`)
- **Bộ đệm Redis Cache:** `127.0.0.1:6379`

### Tài Khoản Seed Mặc Định (Chỉ Dùng Cho Profile `dev`)
- **Super Admin:** `admin` / `admin_dev_password`
- **Nhà xe (Bus Operator):** `operator1` / `operator_dev_password`
- **Khách hàng (Customer):** `customer1` / `customer_dev_password`

### Xử Lý Sự Cố Thường Gặp (Troubleshooting)
- **Trùng cổng `8080` hoặc `5173`:** Nếu trước đó bạn đang chạy container `ticket-backend` hoặc `ticket-frontend` bằng `docker compose up`, lệnh `make infra` và `make dev` sẽ tự động dọn container ứng dụng thừa để giải phóng cổng cho tiến trình local.
- **Cổng MySQL `3307` thay vì `3306`:** Dự án mặc định ánh xạ MySQL ra `127.0.0.1:3307` (`ADR-01`) để tránh xung đột với dịch vụ MySQL cài trên Windows Host khi chạy WSL2.
- **Tốc độ theo dõi file trên WSL2:** Luôn đặt mã nguồn trong phân vùng Linux (`/home/<user>/...`), tránh đặt trong `/mnt/c/...` (lệnh `make doctor` sẽ tự động kiểm tra điều này).

---

## 📄 Bản Quyền & Tác Giả

- **Tác giả:** Phan Văn Thành (Kỹ thuật Phần mềm)
- **Repository:** [ahryxx0602/ticket-booking](https://github.com/ahryxx0602/ticket-booking)
- **Giấy phép:** MIT License — Sẵn sàng cho việc đánh giá kiến trúc, kiểm tra portfolio CV và ứng dụng thực tiễn.
