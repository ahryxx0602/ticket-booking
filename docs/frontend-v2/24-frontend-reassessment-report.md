# BÁO CÁO TÁI KHẢO SÁT TOÀN DIỆN FRONTEND VELOBUS & ĐÁNH GIÁ CHUYỂN ĐỔI SANG SHADCN VUE / CLAUDE+
**Mã tài liệu:** `DOC-FE-V2-24`  
**Phiên bản:** `2.1.0` (Updated post-review) | **Trạng thái:** `PENDING REVIEW & APPROVAL`  
**Thời điểm cập nhật:** `2026-11-25T13:42:00+07:00`  
**Tài liệu thẩm định liên quan:** `docs/frontend-v2/30-frontend-plan-review-report.md`  

---

## 1. TỔNG QUAN ĐIỀU HÀNH (EXECUTIVE SUMMARY)

Dự án VeloBus Ticketing Platform là hệ thống đặt vé xe khách liên tỉnh chịu tải cao (High-Concurrency Bus Ticket Booking System) xây dựng theo kiến trúc Modular Monolith. Phân hệ Frontend được cấu hình ban đầu dựa trên Vue 3, Vite, PrimeVue 4, Tailwind CSS v3, Pinia và Vue Router.

Sau đợt khảo sát chi tiết source code thực tế và đối chiếu lại theo thẩm định độc lập:
1. **Thực trạng PrimeVue bất ngờ:** Mặc dù `package.json` và `main.js` đăng ký PrimeVue 4 (`@primeuix/themes/aura`, `ToastService`, `ConfirmationService`, `tailwindcss-primeui`), **hầu hết toàn bộ 18 màn hình nghiệp vụ sản xuất (Production Views) KHÔNG sử dụng các PrimeVue UI Components cốt lõi** (`<DataTable>`, `<Dialog>`, `<InputText>`, `<Button>`, `<Dropdown>`, v.v.). Thay vào đó, toàn bộ bảng biểu, form, modal, drawer và button hiện tại đều được viết bằng HTML chuẩn kết hợp Tailwind CSS tiện ích và Vue Transitions!
2. **Sự phụ thuộc thực tế vào PrimeVue cực kỳ tối thiểu:**
   - `App.vue`: Chỉ dùng `<Toast />` từ `primevue/toast`.
   - `SeatSelectionView.vue`: Dùng `useToast` từ `primevue/usetoast`.
   - `AppSpinner.vue`: Dùng `<ProgressSpinner />` từ `primevue/progressspinner`.
   - `main.js`: Đăng ký plugin PrimeVue + Theme preset Aura + PrimeIcons CSS.
   - Các template: Dùng một số class icon `pi pi-*` (PrimeIcons).
3. **Ý nghĩa then chốt đối với kế hoạch chuyển đổi sang Shadcn Vue:**
   - Bản chất của cuộc chuyển đổi này là: **Chuẩn hóa hệ thống UI component phân tán, chắp vá hiện tại thành một Design System có cấu trúc chính quy (Shadcn Vue + Reka UI), tích hợp Theme Claude+ (OKLCH, Warm Minimalist), thay thế `useToast` bằng `vue-sonner`, và đưa TanStack Table vào thay thế các `<table>` HTML thủ công.**
4. **Mức độ khả thi:** **RẤT CAO (Feasibility: 94/100)**, rủi ro hồi quy nghiệp vụ ở mức thấp và được kiểm soát chặt chẽ (Low/Controlled Risk) do các service cốt lõi (`httpClient.ts`, `authStore.js`, RBAC router guard) hoàn toàn độc lập với UI library.

---

## 2. PHÂN LOẠI & ĐỐI CHIẾU KẾT QUẢ KHẢO SÁT (FINDINGS TAXONOMY)

| ID | Hạng mục khảo sát | Phân loại | Chi tiết xác minh & Dẫn chứng source code thực tế |
| :--- | :--- | :---: | :--- |
| **FND-TX-01** | **Backend API Envelope & Axios Unwrapping** | `REVALIDATED` | **Xác minh chính xác:** `httpClient.ts` (dòng 100-101) cấu hình `(response) => response.data`. Interceptor này chỉ unwrap transport wrapper của Axios (`response.status`, `response.headers`), trả về nguyên vẹn JSON envelope `ApiResponse<T>` từ Spring Boot (`ApiResponse.java:15-21`). Các consumer (như `authStore.js:148`) truy cập `res?.data` để lấy payload `T`. Jackson `NON_NULL` có thể lược bỏ các trường null (`code`, `data`). |
| **FND-TX-02** | **RBAC 26 API Permissions Catalog** | `REVALIDATED` | Xác nhận 26 permission codes trong `src/types/rbac.ts` đồng bộ hoàn toàn với Backend `AdminPermissionCatalog.java` và navigation guard trong `src/app/router/index.js`. |
| **FND-TX-03** | **Thực tế sử dụng PrimeVue Components** | `REVALIDATED` | Khảo sát tĩnh xác nhận: 0 màn hình dùng `<DataTable>`, 0 màn hình dùng `<Dialog>`, 0 màn hình dùng `<Button>` của PrimeVue. Chỉ có 3 file phụ thuộc vào `primevue/toast` và `primevue/progressspinner`. |
| **FND-TX-04** | **Trạng thái Tailwind CSS** | `REVALIDATED` | Đang dùng Tailwind CSS `v3.4.17` kèm `postcss: ^8.4.49` và `tailwindcss-primeui: ^0.6.1`. Áp dụng chiến lược CSS Variable Bridging để tương thích OKLCH mà không cần nâng cấp vội lên Tailwind v4. |
| **FND-TX-05** | **Kiến trúc Layouts & Navigation** | `REUSED` | Phân hệ Admin và Operator dùng chung `DashboardLayout.vue`. Phân hệ Customer dùng `CustomerLayout.vue`. |
| **FND-TX-06** | **Chất lượng Typography & Khoảng trắng** | `REVALIDATED` | Chốt chuẩn Typography scale theo Claude+ (Outfit font chính, Geist Mono cho mã và tiền tệ, row height 44px/52px). |
| **FND-TX-07** | **Theme Claude+ Registry & Tokens** | `REVALIDATED` | Kéo từ `https://tweakcn.com/r/themes/cmdght103000n04lh3e2ae93r`: Primary `oklch(0.6171 0.1375 39.0427)` tương đương `#C96442` (tương phản 3.90:1 với trắng). Phân 3 lớp tokens để khắc phục vấn đề tương phản và destructive color. |
| **FND-TX-08** | **Cơ chế Quản lý State & Storage** | `REUSED` | `authStore.js` lưu session vào `localStorage`. `useLayoutWidth.js` lưu độ rộng layout. |
| **FND-TX-09** | **Trạng thái Kiểm thử Tự động** | `REVALIDATED` | Xác minh lại: 67/67 Unit Tests (Vitest) PASS, 22/22 E2E Tests (Playwright) PASS, `vue-tsc --noEmit` 0 lỗi, `vite build` 100% thành công trong 2.86s. |
| **FND-TX-10** | **Backend GAP-01 & GAP-05** | `REUSED` | GAP-01 (Backend thiếu API unhold batch khi navigate away) và GAP-05 (Backend chưa có endpoint cập nhật theme) tiếp tục duy trì giải pháp client-side fallback. |

---

## 3. KIỂM KÊ TOÀN DIỆN MÀN HÌNH (SCREEN INVENTORY)

Hiện tại toàn bộ hệ thống gồm **18 màn hình sản xuất** và **2 trang thông báo mã lỗi**:

| STT | Phân hệ | Route | Tên View (File Path) | Quyền truy cập (RBAC) | API Endpoints | Chiến lược Bảng | Độ phức tạp |
| :---: | :--- | :--- | :--- | :--- | :--- | :---: | :---: |
| 1 | **Customer** | `/` | `HomeView.vue` | Public | `GET /api/v1/banners/active` | Không có bảng | `Trung bình` |
| 2 | **Customer** | `/search` | `TripListView.vue` | Public | `GET /api/v1/catalog/trips` | Không có bảng | `Trung bình` |
| 3 | **Customer** | `/trips/:id/seats` | `SeatSelectionView.vue` | Public | `GET /api/v1/catalog/trips/{id}/seats`, `POST /hold` | Không có bảng | `Cao` |
| 4 | **Customer** | `/checkout/:bookingId`| `CheckoutView.vue` | `ROLE_CUSTOMER` | `POST /api/v1/payments/create-vnpay-url` | Không có bảng | `Trung bình` |
| 5 | **Customer** | `/my-bookings` | `MyBookingsView.vue` | `ROLE_CUSTOMER` | `GET /api/v1/bookings/my-bookings`, `/cancel` | `Strategy 2` (Mobile Card) | `Trung bình` |
| 6 | **Customer/All** | `/profile` | `UserProfileView.vue` | Authenticated | `GET /api/v1/auth/me`, Telegram link | `Strategy 6` (Standard Fluid) | `Thấp` |
| 7 | **Auth** | `/login` | `LoginView.vue` | Public | `POST /api/v1/auth/login`, `/register` | Không có bảng | `Thấp` |
| 8 | **Admin** | `/admin` | `AdminDashboardView.vue`| `ROLE_ADMIN` (`PAYMENT_REPORT_VIEW`) | `GET /api/v1/admin/payments/reports` | `Strategy 3` (Horizontal Scroll) | `Trung bình` |
| 9 | **Admin** | `/admin/operators` | `AdminOperatorsView.vue`| `ROLE_ADMIN` (`OPERATOR_VIEW`) | `GET /api/v1/admin/partners/operators` | `Strategy 1` (Priority Column) | `Cao` |
| 10 | **Admin** | `/admin/withdrawals`| `AdminWithdrawalsView.vue`| `ROLE_ADMIN` (`WITHDRAWAL_VIEW`)| `GET /api/v1/admin/partners/withdrawals` | `Strategy 1` (Priority Column) | `Trung bình` |
| 11 | **Admin** | `/admin/banners` | `AdminBannersView.vue` | `ROLE_ADMIN` (`BANNER_VIEW`) | `GET /api/v1/admin/partners/banners` | `Strategy 2` (Card Grid) | `Thấp` |
| 12 | **Admin** | `/admin/users-bookings`| `AdminUsersBookingsView.vue`| `ROLE_ADMIN` (`USER_VIEW`, `ROLE_...`) | `GET /api/v1/admin/users`, `/roles`, `/refund` | `Strategy 4` (Permission Matrix) | `Rất cao` |
| 13 | **Admin** | `/admin/settings` | `AdminSettingsView.vue` | `ROLE_ADMIN` (`STATION_VIEW`, v.v.) | `GET /api/v1/catalog/stations`, `/config` | `Strategy 5` (Master-Detail) | `Cao` |
| 14 | **Operator** | `/operator` | `OperatorDashboardView.vue`| `ROLE_OPERATOR` | `GET /api/v1/partner/wallet` | `Strategy 1` (Priority Column) | `Trung bình` |
| 15 | **Operator** | `/operator/fleet` | `FleetAndDriversView.vue` | `ROLE_OPERATOR` | `GET /api/v1/partner/vehicles`, `/drivers` | `Strategy 1` (Priority Column) | `Cao` |
| 16 | **Operator** | `/operator/trips` | `OperatorTripsView.vue` | `ROLE_OPERATOR` | `GET /api/v1/partner/trips`, `/schedules` | `Strategy 1` (Priority Column) | `Cao` |
| 17 | **Operator** | `/operator/banners` | `OperatorBannersView.vue` | `ROLE_OPERATOR` | `GET /api/v1/partner/banners`, `/upload` | `Strategy 2` (Card Grid) | `Trung bình` |
| 18 | **Operator** | `/operator/profile` | `OperatorProfileView.vue` | `ROLE_OPERATOR` | `GET /api/v1/partner/profile` | Không có bảng | `Thấp` |
| 19 | **System** | `/403` | `ForbiddenView.vue` | Public | None | Không có bảng | `Rất thấp` |
| 20 | **System** | `/:pathMatch(.*)*` | `NotFoundView.vue` | Public | None | Không có bảng | `Rất thấp` |

---

## 4. MA TRẬN SO SÁNH API ENVELOPE (CONTRACT COMPARISON MATRIX — REV-001)

| Tầng Kiến trúc (Layer) | Đối tượng Dữ liệu (Object / Signature) | Trích dẫn Mã nguồn Cụ thể | Hành vi Xử lý Envelope | Jackson NON_NULL & Null Payload |
| :--- | :--- | :--- | :--- | :--- |
| **1. Spring Boot Backend** | `ApiResponse<T>` | `backend/.../ApiResponse.java:15-21` | Trả về JSON: `{ success: boolean, message: string, code?: string, data?: T, timestamp: Instant }`. | `@JsonInclude(NON_NULL)` loại bỏ các key có giá trị `null`. |
| **2. Axios HTTP Client** | `httpClient.interceptors.response` | `frontend/src/shared/api/httpClient.ts:100-101` | `(response) => response.data`. Unwrap `AxiosResponse` của transport layer, trả về nguyên vẹn đối tượng `ApiResponse<T>`. | Nhận nguyên vẹn JSON body từ server; nếu data null thì `res.data` là undefined hoặc null. |
| **3. API Service Layer** | `authApi`, `adminApi`, `catalogBookingApi` | `frontend/src/modules/auth/api.js:4-16` | Return Promise giải quyết về `ApiResponse<T>`. | Không bóc tách thêm; giữ nguyên envelope cho caller. |
| **4. Consumer (Store / Views)**| `authStore.login()`, `AdminOperatorsView` | `frontend/src/modules/auth/store.js:148` | `const res = await authApi.login(...)`; truy cập `res?.data?.accessToken`. | An toàn trước optional chaining `res?.data`. |
| **5. Test Coverage** | `admin-views.spec.js`, `rbac-contract-parity.spec.js` | `frontend/src/modules/admin/__tests__/` | Mock trả về `{ success: true, data: [...] }`. | 100% test cases khớp với cấu trúc envelope này. |

---

## 5. ĐÁNH GIÁ RỦI RO & MỨC ĐỘ KHẢ THI (FEASIBILITY & RISK ASSESSMENT)

- **Điểm khả thi thống nhất:** **`94 / 100`**.
- **Đánh giá rủi ro nghiệp vụ:** Ở mức **Thấp và Có thể kiểm soát (Low / Controlled Risk)**. Không có sự xáo trộn nào đối với endpoints, params, DTO payloads hoặc logic xác thực JWT.
- **Rủi ro tương thích CSS:** Khắc phục bằng chiến lược CSS Variable Bridging, giữ Tailwind v3.4.17 ổn định và cấu hình fallbacks cho dải màu OKLCH.
