# 02 – Đánh Giá Kiến Trúc Frontend Hiện Tại (Current Frontend Audit)

> **Mã tài liệu:** `FE-V2-AUD-02`  
> **Thời điểm rà soát:** 25/11/2026  
> **Phương pháp đánh giá:** White-Box Codebase Archaeology & Static Code Analysis.

---

## 1. Đánh Giá Tổ Chức Module (Module Organization)

Cấu trúc thư mục hiện tại của `frontend/src/` được tổ chức như sau:

```
frontend/src/
├── app/
│   ├── layouts/       # 5 App Layouts (Admin, Operator, Customer, Auth, Dashboard)
│   └── router/        # Vue Router, navigation guards, route metadata
├── assets/            # main.css, SVG/styles
├── modules/           # Feature-sliced modules
│   ├── admin/         # api.js, composables, views
│   ├── auth/          # api.js, store.js, views
│   ├── catalog-booking/# api.js, store.js, components, views
│   └── partner/       # api.js, views (không có store.js!)
└── shared/            # Shared cross-cutting concerns
    ├── api/           # httpClient.js (Axios)
    ├── components/    # 15 Shared UI components
    ├── composables/   # useCountdown, useLayoutWidth
    ├── stores/        # notificationStore.js
    ├── utils/         # formatters, redirect
    └── views/         # 403 Forbidden, 404 Not Found
```

### Điểm mạnh:
- Kiến trúc có định hướng rõ ràng theo **Feature-Sliced Design** (`modules/` phân tách `admin`, `auth`, `catalog-booking`, `partner`).
- Tách bạch tầng kết nối mạng trung tâm `src/shared/api/httpClient.js` sử dụng Axios Interceptor xử lý Bearer Token, kiểm tra hết hạn JWT client-side và bắt sự kiện `401 Unauthorized` fail-closed.
- Các helper định dạng tiền tệ `formatVnd`, ngày giờ `formatDateTime`, `formatTime` được tập trung tại `src/shared/utils/formatters.js`.

### Khiếm khuyết kiến trúc:
1. **Thiếu tính nhất quán giữa các Modules:**
   - Module `auth` và `catalog-booking` có `store.js` riêng.
   - Module `partner` và `admin` **hoàn toàn không có store.js**, dẫn tới tình trạng toàn bộ trạng thái dữ liệu (state) bị nhét thẳng vào `ref()` cục bộ trong các View, gây khó khăn cho việc chia sẻ dữ liệu hoặc tái sử dụng giữa các View con.
2. **God Component & Trách Nhiệm Chồng Chéo:**
   - [AdminUsersBookingsView.vue](file:///home/vanthanh/Ind-Project/Java/ticket-booking/frontend/src/modules/admin/views/AdminUsersBookingsView.vue) vừa quản lý User CRUD, vừa điều khiển Ma trận phân quyền Role Preview, vừa xử lý hoàn tiền Booking Refund.
   - Logic kiểm tra phân quyền `hasPermission` bị rải rác: vừa nằm ở Router Guard, vừa nằm ở Sidebar Filter trong `DashboardLayout.vue`, vừa lặp lại trong nội dung của View.
3. **Phụ thuộc lỏng lẻo & Bỏ qua Exception:**
   - Tại các View quản trị, nhiều hành động mutation gọi API nhưng bọc trong `.catch(() => {})` hoặc bỏ qua khối xử lý lỗi, sau đó hiển thị Toast thành công dạng Optimistic UI giả lập. Nếu Backend trả về lỗi (như vi phạm ràng buộc dữ liệu hoặc quyền hạn), giao diện vẫn báo thành công giả tạo!

---

## 2. Đánh Giá Quản Lý Trạng Thái (State Management Audit)

Hệ thống hiện tại sử dụng 3 Pinia Stores:
1. `useAuthStore` (`src/modules/auth/store.js`):
   - Lưu trữ: `token`, `user`, `role`, `customRole`, `permissions`, `previewRole`, `sessionEpoch`.
   - Đồng bộ `localStorage` với tiền tố `velobus_*`.
   - **Đánh giá:** Rất chặt chẽ về bảo mật; có cơ chế Memory-Only Preview Role (`FE-SEC-005`), ngăn chặn rò rỉ quyền vào LocalStorage.
2. `useBookingStore` (`src/modules/catalog-booking/store.js`):
   - Lưu trữ: `selectedTrip`, `selectedSeats`, `activeBooking`, `holdExpiresAt`.
   - Quản lý luồng giữ chỗ 10 phút và gọi API checkout VNPay.
   - **Đánh giá:** Vận hành tốt cho luồng đặt vé đơn luồng, nhưng chưa có cơ chế khóa tương tác khi chuyển route đột ngột.
3. `useNotificationStore` (`src/shared/stores/notificationStore.js`):
   - Quản lý thông báo in-app realtime và đếm số lượng chưa đọc.

### Các thiếu sót về State Management:
- **Thiếu Server-State Caching / Invalidation:** Không có cơ chế quản lý vòng đời dữ liệu tải từ server (Server State). Khi người dùng chuyển tab hoặc quay lại trang, dữ liệu không được tự động revalidate hoặc hiển thị stale-while-revalidate.
- **Race Conditions:** Các tác vụ tìm kiếm chuyến xe khi người dùng thay đổi liên tục bộ lọc ngày/trạm không có `AbortController` (CancelToken), dẫn đến nguy cơ kết quả của request trước ghi đè lên request sau nếu thời gian phản hồi mạng bất đồng bộ.

---

## 3. Bảng Đối Chiếu Nguồn Dữ Liệu Thực Tế vs Dữ Liệu Tĩnh (Data Source Audit Matrix)

Khảo sát chi tiết 100% các View trong hệ thống để xác định mức độ tích hợp API thật:

| Phân hệ / Module | Tên View | Nguồn dữ liệu khởi tạo | API Backend thực tế khả dụng | Dữ liệu tĩnh (Mock) phát hiện | Trạng thái tích hợp API thực tế | Khuyến nghị khắc phục |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Auth** | `LoginView.vue` | API thật | `POST /auth/login`<br>`POST /auth/register` | Nút quick-seed tiện ích dev | **TÍCH HỢP THỰC TẾ** | Giữ nguyên luồng API. |
| **Auth** | `UserProfileView.vue`| API thật (`onMounted`) | `GET /auth/me`<br>`POST /notifications/telegram/*`| Không có | **TÍCH HỢP THỰC TẾ** | Bổ sung Skeleton loading khi tải profile. |
| **Customer** | `HomeView.vue` | Mảng tĩnh `ref([...])` | `GET /public/banners/active`<br>`GET /catalog/stations` | `featuredTrips` hardcoded 3 chuyến | **CHƯA TÍCH HỢP (MOCK TĨNH)** | Tích hợp nạp banner thật và chuyến xe thật từ API Catalog. |
| **Customer** | `TripListView.vue` | Mảng tĩnh `ref([...])` | `GET /catalog/trips` | `allTrips` hardcoded 4 chuyến xe tĩnh | **CHƯA TÍCH HỢP & CONTRACT MISMATCH** | View chưa hề gọi `searchTrips()`. URL query dùng mã bến (`from`, `to`) trong khi Backend đòi `departureCity`, `arrivalCity`. Cần Composable ánh xạ và gọi API thật. |
| **Customer** | `SeatSelectionView.vue`| API thật (`onMounted`) | `GET /catalog/trips/{id}/seats`<br>`POST /bookings/hold` | Mảng fallback nếu API fail | **TÍCH HỢP PHẦN LỚN** | Gọi API ghế thật và giữ chỗ 10p thật. Cần loại bỏ mảng mock fallback khi lỗi. |
| **Customer** | `CheckoutView.vue` | State Pinia + `setTimeout` giả | `POST /payments/checkout` | Giả lập VNPay bằng `setTimeout(..., 1200)` | **GIẢ LẬP THANH TOÁN (FAKE)** | **Phát hiện nghiêm trọng:** View hoàn toàn không gọi `catalogBookingApi.createVnPayCheckout()` mà dùng `setTimeout` rồi chuyển hướng về `/my-bookings?payment_success=true`. Bắt buộc gọi API thật nhận `redirectPaymentUrl`. |
| **Customer** | `MyBookingsView.vue` | Mảng tĩnh `ref([...])` | `GET /bookings/my-bookings`<br>`GET /bookings/{ref}` | `bookings` hardcoded 2 đơn vé + chuỗi QR HMAC giả | **CHƯA TÍCH HỢP (MOCK TĨNH)** | Nạp 100% từ `getMyBookings()`, hỗ trợ phân trang thật và chỉ hiển thị vé khi trạng thái Backend là `CONFIRMED`. |
| **Operator** | `OperatorDashboardView.vue`| Mảng tĩnh `ref([...])` | `GET /partners/wallet`<br>`GET /partners/wallet/transactions`| 4 số dư ví và 4 giao dịch mẫu | **CHƯA TÍCH HỢP (MOCK TĨNH)** | Nạp ví ký quỹ và bảng lịch sử giao dịch từ API thật. |
| **Operator** | `FleetAndDriversView.vue` | Mảng tĩnh `ref([...])` | `GET /partners/vehicles`<br>`GET /partners/drivers` | `vehicles` (3 xe), `drivers` (3 tài xế) | **TÍCH HỢP MỘT PHẦN (CHỈ MUTATION)** | Dữ liệu khởi tạo là mock tĩnh; chỉ gọi API khi submit Drawer tạo mới. Cần nạp danh sách thật trên `onMounted`. |
| **Operator** | `OperatorTripsView.vue` | Mảng tĩnh `ref([...])` | `GET /catalog/trips`<br>`POST /partners/trips` | `trips` hardcoded 3 chuyến xe | **CHƯA TÍCH HỢP & THIẾU API BACKEND** | Backend thiếu `GET /partners/trips` (chỉ có `POST /trips`). Cần chiến lược nạp từ Catalog có filter hoặc lưu session. |
| **Operator** | `OperatorBannersView.vue` | Mảng tĩnh `ref([...])` | `GET /partners/banners`<br>`POST /partners/banners` | `banners` hardcoded 2 banner | **TÍCH HỢP MỘT PHẦN (CHỈ MUTATION)** | Nạp danh sách banner nhà xe từ `getMyBanners()`. |
| **Operator** | `OperatorProfileView.vue` | API thật (`onMounted`) | `GET /partners/me`<br>`PUT /partners/me` | Dữ liệu khởi tạo rỗng | **TÍCH HỢP THỰC TẾ** | Giữ nguyên luồng API, cải thiện form validation. |
| **Admin** | `AdminDashboardView.vue` | Mảng tĩnh `ref([...])` | `GET /admin/payments/reports`<br>`GET /admin/payments` | `reconciliationAlerts` hardcoded 2 cảnh báo | **CHƯA TÍCH HỢP (MOCK TĨNH)** | Nạp KPI và danh sách lệch đối soát từ `getPaymentReport()`. |
| **Admin** | `AdminOperatorsView.vue` | Mảng tĩnh `ref([...])` | `GET /admin/partners/operators`<br>`PUT .../commission` | `operators` hardcoded 3 nhà xe | **TÍCH HỢP MỘT PHẦN (CHỈ MUTATION)** | Nạp danh sách nhà xe và trạng thái duyệt từ `getOperators()`. |
| **Admin** | `AdminWithdrawalsView.vue`| Mảng tĩnh `ref([...])` | `GET /admin/partners/withdrawals`<br>`PUT .../approve` | `withdrawals` hardcoded 3 lệnh rút | **TÍCH HỢP MỘT PHẦN (CHỈ MUTATION)** | Nạp danh sách yêu cầu rút tiền từ `getWithdrawals()`. |
| **Admin** | `AdminBannersView.vue` | Mảng tĩnh `ref([...])` | `GET /admin/partners/banners`<br>`PUT .../approve` | `banners` hardcoded 3 banner | **TÍCH HỢP MỘT PHẦN (CHỈ MUTATION)** | Nạp danh sách kiểm duyệt banner từ `adminApi.getBanners()`. |
| **Admin** | `AdminUsersBookingsView.vue`| Mảng tĩnh `ref([...])` | `GET /admin/users`<br>`GET /admin/roles`<br>`GET /admin/bookings` | `users` (7 người), `systemRoles` (3 role), `allBookings` (2 đơn) | **TÍCH HỢP MỘT PHẦN (CHỈ MUTATION)** | Bóc tách 3 views, nạp 100% dữ liệu từ các API tương ứng trên `onMounted`. |
| **Admin** | `AdminSettingsView.vue` | Mảng tĩnh `ref([...])` | `GET /admin/settings`<br>`GET /admin/stations`<br>`GET /admin/trips` | `systemSettings`, `stations` (5 bến), `trips` (2 chuyến) | **TÍCH HỢP MỘT PHẦN (CHỈ MUTATION)** | Bóc tách 3 views, nạp cấu hình toàn sàn và danh mục bến/chuyến từ API. |

---

## 4. Tổng Kết Khoảng Cách Kỹ Thuật (Architecture Gap Summary)

1. **Gap lớn nhất:** Sự bất đối xứng nghiêm trọng giữa **Backend năng lực cao** (đầy đủ JPA repositories, REST endpoints, phân quyền RBAC đa cấp) và **Frontend tiêu thụ dữ liệu thụ động** (dùng mảng tĩnh nạp sẵn, chỉ kích hoạt API khi bấm nút Submit).
2. **Nguy cơ lỗi:** Do phụ thuộc vào dữ liệu mock nội bộ, các bài kiểm thử tự động (Unit Test / Playwright mock) luôn báo xanh dù Backend có thể trả về lỗi hoặc thay đổi cấu trúc envelope.
3. **Giải pháp bắt buộc:** Loại bỏ toàn bộ các mảng dữ liệu tĩnh hardcoded, xây dựng cơ chế nạp dữ liệu chuẩn RESTful trên `onMounted` kèm trạng thái Skeleton Loading và Error Handling đầy đủ.
