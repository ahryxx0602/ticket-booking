# 🖥️ VeloBus Frontend — Giao Diện Nền Tảng Premium Mobility SaaS

<p align="center">
  🌐 <b><a href="README.md">English</a></b> | <b><a href="README.vi.md">Tiếng Việt</a></b>
</p>

[![Vue 3](https://img.shields.io/badge/Vue-3.5.13-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white)](https://vuejs.org/)
[![Vite](https://img.shields.io/badge/Vite-6.0.5-646CFF?style=for-the-badge&logo=vite&logoColor=white)](https://vitejs.dev/)
[![PrimeVue 4](https://img.shields.io/badge/PrimeVue-4.2.5-41B883?style=for-the-badge&logo=primevue&logoColor=white)](https://primevue.org/)
[![Tailwind CSS](https://img.shields.io/badge/TailwindCSS-3.4.17-38B2AC?style=for-the-badge&logo=tailwind-css&logoColor=white)](https://tailwindcss.com/)
[![Pinia](https://img.shields.io/badge/Pinia-2.3.0-FFE57F?style=for-the-badge&logo=pinia&logoColor=black)](https://pinia.vuejs.org/)
[![Playwright](https://img.shields.io/badge/Playwright-E2E%20Tests%20(21%20Passed)-2EAD33?style=for-the-badge&logo=playwright&logoColor=white)](https://playwright.dev/)
[![Vitest](https://img.shields.io/badge/Vitest-Unit%20Tests%20(29%20Passed)-729B1B?style=for-the-badge&logo=vitest&logoColor=white)](https://vitest.dev/)

Giao diện web hiện đại, hiệu năng cao của nền tảng bán vé xe khách liên tỉnh và quản trị đội xe đa đối tác **VeloBus**. Được xây dựng trên nền tảng **Vue 3 (Composition API)**, **PrimeVue 4**, và **Tailwind CSS**, hệ thống chuyển hóa các quy chuẩn nghiệp vụ phức tạp thành ngôn ngữ thiết kế **Kinetic Executive** chuẩn mực phối hợp cùng **Google Stitch**.

---

## 🎨 Hệ Thống Thiết Kế UI/UX & Tích Hợp Google Stitch Cloud

Giao diện áp dụng ngôn ngữ thiết kế **Kinetic Executive** tối ưu hóa cho các thao tác nghiệp vụ vận hành mật độ cao:
- **Brand Navy (`#142B4A`):** Màu thương hiệu chủ đạo, sidebar điều hướng của Admin và Nhà xe, các khối thông tin cấu trúc lớn.
- **Primary Blue (`#2563EB`):** Nút hành động chính (CTA), trạng thái ghế đang chọn, các bước thanh toán.
- **Accent Amber (`#F59E0B`):** Cảnh báo khẩn cấp, đếm ngược giữ chỗ dưới 2 phút, cảnh báo vi phạm khoảng đệm 30 phút.
- **Canvas (`#F8FAFC`):** Nền làm việc êm dịu, không gây chói mắt.
- **Typography:** Phông chữ Inter áp dụng quy chuẩn bắt buộc `font-variant-numeric: tabular-nums` cho toàn bộ số tiền, đồng hồ đếm ngược và bảng dữ liệu.

### Dự Án Google Stitch Cloud Chính Thức
Toàn bộ các màn hình cốt lõi đã được khởi tạo và đồng bộ trên **Google Stitch Cloud**:
- **Liên kết dự án:** [https://stitch.withgoogle.com/projects/7031998889146609699](https://stitch.withgoogle.com/projects/7031998889146609699)
- **Design System Asset:** `assets/a83bf80db97045319502dd4fa3a461f3` (*Kinetic Executive*)
- **Danh sách màn hình độ phân giải cao:**
  1. `d5b0031050ff4ae4a8382fbfca348063`: *VeloBus - Chọn chỗ & Đặt vé* (Sơ đồ ghế 2 tầng, đếm ngược 10:00, tóm tắt VNPay)
  2. `74bd918ef9e741dd9624de8f1b7165e8`: *Fintech Escrow Dashboard & Payout Management* (4 thẻ ví ký quỹ, xác minh KYC VCB)
  3. `4e909110885f4aa4b302b413417bcfa7`: *Platform Governance & VNPay Reconciliation Dashboard* (Cảnh báo lệch IPN, hoa hồng 2 cấp)
  4. `a1c763d98f854dc4a5859dbcac5698b1`: *Fleet & Trip Schedule Management* (Khoảng đệm an toàn 30 phút, Drawer tạo chuyến)
  5. `0c0c51cfabda47c08fb8b230245acd33`: *Customer Digital Boarding Pass & E-Ticket* (Thẻ lên xe điện tử mã hóa QR HMAC-SHA256)

---

## 🏛️ Cấu Trúc Mã Nguồn Phân Tách Theo Tính Năng (Feature-Sliced)

```
frontend/src/
├── app/
│   ├── layouts/
│   │   ├── CustomerLayout.vue             # Header sticky 72px, container căn giữa, Footer tối
│   │   ├── OperatorLayout.vue             # Khung Partner Hub (Sidebar Navy 256px / 76px)
│   │   ├── AdminLayout.vue                # Khung điều hành sàn với thanh nút xử lý khẩn cấp
│   │   └── AuthLayout.vue                 # Split-screen 45% Navy / 55% Form
│   └── router/index.js                    # Danh mục định tuyến Vue Router 4
├── modules/
│   ├── auth/                              # Đăng nhập, Đăng ký, Hồ sơ cá nhân, Mã liên kết Telegram
│   ├── catalog-booking/                   # Tìm kiếm, Sơ đồ ghế 2 tầng, Đặt vé VNPay, Vé điện tử
│   ├── partner/                           # Ví ký quỹ, Đội xe & Tài xế, Lập lịch chuyến, Hồ sơ nhà xe
│   └── admin/                             # Chỉ số KPI sàn, Cảnh báo đối soát, Quản lý User & Đơn vé
└── shared/
    ├── api/httpClient.js                  # Axios instance tích hợp Bearer token interceptor
    ├── components/                        # SeatMap2Deck, FirebaseUploader, StatusBadge, v.v.
    ├── composables/                       # useCountdown, useLayoutWidth
    └── stores/notificationStore.js        # Quản lý thông báo thời gian thực & Telegram Bot
```

---

## 🚀 Các Tính Năng Trọng Yếu Từng Phân Hệ

### 1. Phân Hệ Khách Hàng (`CustomerLayout`)
- **Sơ Đồ Ghế 2 Tầng Tương Tác:** Hiển thị trực quan Tầng 1 (`A01..A20`) và Tầng 2 (`B01..B20`), 5 màu sắc trạng thái ghế chuẩn mực, giá vé căn chỉnh hoàn hảo.
- **Đếm Ngược Giữ Chỗ 10 Phút:** Bộ đếm thời gian thực `useCountdown.js` tự động cảnh báo màu hổ phách khi còn dưới 2 phút và điều hướng an toàn khi hết hạn.
- **Thẻ Lên Xe Kỹ Thuật Số (E-Ticket):** Giao diện vé điện tử phân tách đường răng cưa chuẩn hàng không, tích hợp mã QR mã hóa `HMAC-SHA256` chống làm giả vé.

### 2. Không Gian Nhà Xe Đối Tác (`OperatorLayout`)
- **Quản Trị Ví Ký Quỹ 4 Số Dư:** Theo dõi minh bạch `AVAILABLE`, `HOLDING_ESCROW`, `SETTLING`, và `WITHDRAWN`.
- **Hồ Sơ Đội Xe & Tài Xế:** Sử dụng Slide-over Drawer (520px) nhập liệu tốc độ cao và upload giấy tờ phương tiện qua Firebase Storage.
- **Kiểm Soát Turnaround Buffer 30 Phút:** Tự động phát hiện và cảnh báo xung đột lịch trình phương tiện / tài xế dưới 30 phút giữa các chuyến.

### 3. Trung Tâm Điều Hành Quản Trị Viên (`AdminLayout`)
- **Cảnh Báo Lệch Đối Soát IPN VNPay:** Hiển thị nổi bật các giao dịch bất thường giữa cổng thanh toán và hệ thống sàn để xử lý khẩn cấp.
- **Cấu Hình Hoa Hồng 2 Cấp:** Tùy biến tỷ lệ chiết khấu sàn toàn hệ thống và ghi đè riêng cho từng hợp đồng nhà xe (5.0% vs 7.0%).
- **Chốt Chặn `ADMIN_CANNOT_LOCK_SELF`:** Khóa an toàn ngăn chặn Admin tự khóa tài khoản hoặc hạ quyền chính mình.

---

## 🧪 Hệ Thống Kiểm Thử & Đảm Bảo Chất Lượng

### Kiểm Thử Unit & Composables (Vitest)
```bash
npm run test:unit
# Kết quả: 29/29 tests PASS trên 6 test suites (1.03s)
```

### Kiểm Thử End-to-End & Trực Quan Đa Kích Thước Màn Hình (Playwright)
```bash
npm run test:e2e
# Kết quả: 21/21 tests PASS trên Chromium, Mobile (375x667), Tablet (768x1024), Desktop (1440x900)
```

---

## 📦 Đóng Gói Production & Chạy Docker

Frontend được đóng gói qua **Dockerfile đa tầng (Multi-stage)** tạo ra Nginx container siêu nhẹ (< 25MB):
- **Stage 1 (Builder):** Node 22 Alpine chạy `npm ci && npm run build` (hoàn tất trong ~2.5 giây).
- **Stage 2 (Runtime):** Nginx 1.27 Alpine với nén Gzip, điều hướng SPA fallback và Reverse Proxy chuyển tiếp `/api/` về Backend.

```bash
# Biên dịch mã nguồn production
npm run build

# Khởi chạy preview server cục bộ
npm run preview -- --port 4173
```
