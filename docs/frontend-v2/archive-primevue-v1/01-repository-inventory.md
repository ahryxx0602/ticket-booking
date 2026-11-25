# 01 – Kiểm Kê Kho Mã Nguồn & Công Nghệ (Repository Inventory)

> **Mã tài liệu:** `FE-V2-INV-01`  
> **Thời điểm rà soát:** 25/11/2026  
> **Baseline Commit:** `2659e1c` (HEAD -> main)  
> **Phạm vi kiểm kê:** Thư mục `frontend/` và cấu hình CI/CD liên quan.

---

## 1. Bảng Kiểm Kê Công Nghệ & Phiên Bản (Technology Inventory)

Đối chiếu giữa khai báo `frontend/package.json` và phiên bản thực tế đã resolve trong `frontend/package-lock.json`:

| Hạng mục | Tên gói (Package Name) | Khai báo `package.json` | Thực tế `package-lock.json` | Giấy phép (License) | Đánh giá & Rủi ro |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **Core Framework** | `vue` | `^3.5.13` | **3.5.13** | MIT | Ổn định, hỗ trợ đầy đủ Composition API `<script setup>` và TypeScript 5. |
| **Routing** | `vue-router` | `^4.5.0` | **4.5.0** | MIT | Hỗ trợ Route Guards, dynamic imports, scrollBehavior. |
| **State Management** | `pinia` | `^2.3.0` | **2.3.0** | MIT | Chuẩn chính thức của Vue 3, hỗ trợ DevTools và modular stores. |
| **UI Components** | `primevue` | `^4.2.5` | **4.2.5** | MIT | Phiên bản v4 mới nhất, kiến trúc Pass Through (PT), Preset theme tokens. |
| **Theme Engine** | `@primeuix/themes` | `^1.0.0` | **1.0.0** | MIT | Cung cấp Aura preset và hàm `definePreset`. |
| **Tailwind PrimeUI**| `tailwindcss-primeui`| `^0.6.1` | **0.6.1** | MIT | Ánh xạ token PrimeVue sang Tailwind utility classes. |
| **CSS Utility** | `tailwindcss` | `^3.4.17` | **3.4.17** | MIT | Phiên bản v3 ổn định cao, tương thích hoàn toàn PostCSS 8. |
| **HTTP Client** | `axios` | `^1.7.9` | **1.7.9** | MIT | Xử lý Bearer Token, Request/Response interceptors, CancelToken. |
| **Icon System 1** | `lucide-vue-next` | `^0.469.0` | **0.469.0** | ISC | Bộ icon vector hiện đại, cây rung (tree-shakeable), dung lượng nhẹ. |
| **Icon System 2** | `primeicons` | `^7.0.0` | **7.0.0** | MIT | Bộ icon font truyền thống (~500KB webfont bundle). Cần chuẩn hóa giảm tải. |
| **Chart Library** | `chart.js` | `^4.4.7` | **4.4.7** | MIT | Dùng cho biểu đồ đối soát và doanh thu. |
| **Utilities** | `qrcode` | `^1.5.4` | **1.5.4** | MIT | Sinh mã QR vé xe khách chuẩn HMAC-SHA256. |
| **Build Tool** | `vite` | `^6.0.5` | **6.0.5** | MIT | Vite v6 hiện đại, thời gian build production ~3.3s. |
| **Unit Testing** | `vitest` | `^4.1.12` | **4.1.12** | MIT | Tốc độ cao, tương thích cấu hình Vite. |
| **DOM Environment** | `happy-dom` | `^21.0.4` | **21.0.4** | MIT | Giả lập DOM siêu nhẹ cho Unit Test (nhẹ hơn JSDOM 3-4 lần). |
| **Test Utilities** | `@vue/test-utils` | `^2.6.0` | **2.6.0** | MIT | Tiện ích mount component chính thức của Vue. |
| **E2E Testing** | `@playwright/test` | `^1.65.1` | **1.65.1** | Apache-2.0 | Bộ kiểm thử đầu cuối headless đa trình duyệt (Chromium, Firefox, WebKit). |

---

## 2. Thống Kê Tài Sản Mã Nguồn (Source Code Inventory)

| Loại tài nguyên | Số lượng | Chi tiết đường dẫn | Ghi chú kiến trúc |
| :--- | :---: | :--- | :--- |
| **Tổng số files mã nguồn** | **69** | Toàn bộ trong thư mục `frontend/` (không tính `node_modules`, `dist`) | Quản lý dạng Monorepo tích hợp |
| **Vue Single-File Components**| **41** | `src/app/`, `src/modules/`, `src/shared/` | 100% sử dụng cú pháp `<script setup>` |
| **JavaScript Modules (Code)** | **15** | Router (1), Stores (3), APIs (5), Composables (3), Utils (2), Entry (1) | Chưa có TypeScript |
| **Unit Test Suites** | **8** | `src/**/__tests__/*.spec.js` | 67 tests PASS (0 fail) |
| **E2E Test Suites** | **5** | `e2e/*.spec.js` (4 file) + `e2e-fullstack/*.spec.js` (1 file) | 22 tests mock + 1 test fullstack PASS |
| **App Shells / Layouts** | **5** | `AdminLayout`, `OperatorLayout`, `CustomerLayout`, `AuthLayout`, `DashboardLayout` | `AdminLayout` và `OperatorLayout` kế thừa `DashboardLayout` |
| **Views / Pages** | **16** | Customer (5), Operator (5), Admin (6) | Đang chứa một số God Views |
| **Shared Components** | **15** | `src/shared/components/*.vue` | Tái sử dụng giữa các modules |
| **CSS & Theme files** | **2** | `assets/main.css`, `tailwind.config.js` | Quy định biến CSS và Design Tokens |

---

## 3. Danh Mục Chi Tiết 16 Màn Hình Hiện Tại (Screen Catalog)

| Mã màn hình | Tên màn hình / View | Tệp nguồn Vue | Số dòng | URL Route | Vai trò truy cập |
| :--- | :--- | :--- | :---: | :--- | :--- |
| `FE-SCR-AUTH-01` | Đăng nhập & Đăng ký | `modules/auth/views/LoginView.vue` | 106 | `/login` | Public (Khách vãng lai) |
| `FE-SCR-AUTH-02` | Hồ sơ người dùng | `modules/auth/views/UserProfileView.vue` | 151 | `/profile` | Authenticated (Mọi role) |
| `FE-SCR-CUST-01` | Trang chủ & Hero | `modules/catalog-booking/views/HomeView.vue` | 201 | `/` | Public |
| `FE-SCR-CUST-02` | Tra cứu chuyến xe | `modules/catalog-booking/views/TripListView.vue` | 285 | `/search` | Public |
| `FE-SCR-CUST-03` | Sơ đồ ghế 2 tầng | `modules/catalog-booking/views/SeatSelectionView.vue` | **422** | `/trips/:id/seats` | Public |
| `FE-SCR-CUST-04` | Đặt vé & Thanh toán | `modules/catalog-booking/views/CheckoutView.vue` | 308 | `/checkout/:bookingId` | `ROLE_CUSTOMER` |
| `FE-SCR-CUST-05` | Vé của tôi & QR | `modules/catalog-booking/views/MyBookingsView.vue` | 351 | `/my-bookings` | `ROLE_CUSTOMER` |
| `FE-SCR-OPER-01` | Tổng quan Ví ký quỹ | `modules/partner/views/OperatorDashboardView.vue` | 327 | `/operator` | `ROLE_OPERATOR` |
| `FE-SCR-OPER-02` | Đội xe & Tài xế | `modules/partner/views/FleetAndDriversView.vue` | **467** | `/operator/fleet` | `ROLE_OPERATOR` |
| `FE-SCR-OPER-03` | Lịch trình chuyến xe | `modules/partner/views/OperatorTripsView.vue` | 323 | `/operator/trips` | `ROLE_OPERATOR` |
| `FE-SCR-OPER-04` | Banner quảng cáo | `modules/partner/views/OperatorBannersView.vue` | 227 | `/operator/banners` | `ROLE_OPERATOR` |
| `FE-SCR-OPER-05` | Hồ sơ & Nhận diện | `modules/partner/views/OperatorProfileView.vue` | 232 | `/operator/profile` | `ROLE_OPERATOR` |
| `FE-SCR-ADM-01`  | Dashboard & Đối soát | `modules/admin/views/AdminDashboardView.vue` | 219 | `/admin` | `ROLE_ADMIN` (Payment perms) |
| `FE-SCR-ADM-02`  | Duyệt Nhà xe & Commission | `modules/admin/views/AdminOperatorsView.vue` | **497** | `/admin/operators` | `ROLE_ADMIN` (`OPERATOR_VIEW`) |
| `FE-SCR-ADM-03`  | Rút tiền Escrow | `modules/admin/views/AdminWithdrawalsView.vue` | 108 | `/admin/withdrawals` | `ROLE_ADMIN` (`WITHDRAWAL_VIEW`) |
| `FE-SCR-ADM-04`  | Kiểm duyệt Banner | `modules/admin/views/AdminBannersView.vue` | 289 | `/admin/banners` | `ROLE_ADMIN` (`BANNER_VIEW`) |
| `FE-SCR-ADM-05`  | Users, Roles & Refund | `modules/admin/views/AdminUsersBookingsView.vue`| **1.701**| `/admin/users-bookings` | `ROLE_ADMIN` (Gộp 3 quyền) |
| `FE-SCR-ADM-06`  | Bến, Chuyến & Cấu hình | `modules/admin/views/AdminSettingsView.vue` | **498** | `/admin/settings` | `ROLE_ADMIN` (Gộp 3 quyền) |

---

## 4. Phân Tích Độ Lớn Bất Thường Của Mã Nguồn (File Size Outliers)

Thống kê Top 5 tệp tin lớn nhất vượt ngưỡng khuyến nghị:

1. **`AdminUsersBookingsView.vue` (1.701 dòng):**
   - Vượt **425%** ngưỡng tối đa 400 dòng của `frontend-architect`.
   - Chứa 3 tab lớn: Danh sách User (khách hàng, nhà xe, nhân viên sàn), Ma trận 7 Modules x 26 APIs quản trị kèm xem thử (Role Preview), và Bảng đơn vé toàn sàn kèm form Hoàn tiền cưỡng chế 100%.
   - Chứa 4 Modal Dialogs, 2 Drawer, 15 hàm xử lý cục bộ và 3 mảng dữ liệu mock độc lập.

2. **`AdminSettingsView.vue` (498 dòng):**
   - Vượt **124%** ngưỡng 400 dòng.
   - Gộp form Cấu hình hệ thống (Platform Name, Seat Hold TTL, Commission), Bảng CRUD Bến xe (Stations) và Bảng CRUD Lịch trình chuyến xe (Trips) kèm Form Drawer.

3. **`AdminOperatorsView.vue` (497 dòng):**
   - Vượt **124%** ngưỡng 400 dòng.
   - Gộp Bảng quản lý nhà xe, Dialog điều chỉnh hoa hồng 2 cấp (Global vs Custom), Dialog phê duyệt/từ chối/đình chỉ nhà xe.

4. **`FleetAndDriversView.vue` (467 dòng):**
   - Vượt **116%** ngưỡng 400 dòng.
   - Gộp 2 đối tượng thực thể hoàn toàn tách biệt: Phương tiện (Vehicles) và Tài xế (Drivers) trong cùng 1 View điều khiển bằng Tab.

5. **`SeatSelectionView.vue` (422 dòng):**
   - Vượt nhẹ ngưỡng 400 dòng do tích hợp sơ đồ xe 2 tầng, tính giá vé, bộ chọn điểm đón/trả và đếm ngược giữ chỗ.
