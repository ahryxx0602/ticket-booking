# 09 – Kiến Trúc Frontend Mục Tiêu V2 (Target Frontend Architecture)

> **Mã tài liệu:** `FE-V2-ARCH-09`  
> **Thời điểm thiết kế:** 25/11/2026  
> **Mục tiêu:** Tái cấu trúc thư mục chuẩn Feature-Sliced Architecture, phân tách các God Views thành các Micro-Modules độc lập và chuẩn hóa thư viện Shared Components.

---

## 1. Cấu Trúc Thư Mục Mục Tiêu (Target Folder Structure)

```
frontend/src/
├── app/
│   ├── layouts/                  # Cấu trúc App Shells (Dựa trên Sakai Pattern)
│   │   ├── AuthLayout.vue        # Split-screen Auth Shell (45% Navy / 55% Form)
│   │   ├── CustomerLayout.vue    # Header trắng 72px sticky, Footer Navy
│   │   └── DashboardLayout.vue   # Unified Shell cho Admin & Operator (256px Collapsible Sidebar)
│   ├── router/
│   │   ├── index.ts              # Vue Router 4, ScrollBehavior, Typed Route Guards
│   │   └── routes/               # Phân rã routes theo module tránh file router khổng lồ
│   │       ├── auth.routes.ts
│   │       ├── customer.routes.ts
│   │       ├── operator.routes.ts
│   │       └── admin.routes.ts
│   └── App.vue
│
├── types/                        # Hệ thống Kiểu Dữ Liệu TypeScript Trung Tâm
│   ├── api.ts                    # ApiResponse<T>, ApiPageResponse<T>, ApiError
│   ├── auth.ts                   # UserProfile, UserRole, LoginCredentials
│   ├── booking.ts                # TripSummary, TripDetail, Seat, BookingPayload
│   ├── partner.ts                # PartnerWallet, Vehicle, Driver, Withdrawal
│   ├── admin.ts                  # AdminUser, SystemRole, SystemSettings, Station
│   └── rbac.ts                   # 26 Permission Codes, PermissionCatalog
│
├── shared/                       # Tầng Hạ Tầng & Dùng Chung Toàn Sàn
│   ├── api/
│   │   └── httpClient.ts         # Axios Interceptor, Bearer JWT, Fail-closed 401
│   ├── components/               # Bộ Thư Viện UI Dùng Chung Chuẩn Hóa
│   │   ├── button/AppButton.vue
│   │   ├── feedback/AppProgressBar.vue      # Thanh tiến trình chuyển route đỉnh trang
│   │   ├── feedback/EmptyState.vue          # Trạng thái danh sách rỗng kèm CTA
│   │   ├── feedback/ErrorState.vue          # Trạng thái lỗi mạng/API kèm nút Thử lại
│   │   ├── feedback/TableSkeleton.vue       # Khung xương tải bảng dữ liệu DataTable
│   │   ├── form/FormField.vue
│   │   ├── layout/PageHeader.vue            # Tiêu đề trang + Breadcrumb + Primary Actions
│   │   ├── layout/CrudPageLayout.vue        # Khung bố cục chuẩn cho mọi màn hình CRUD
│   │   ├── table/AppDataTable.vue           # Wrapper DataTable tích hợp Skeleton & Empty
│   │   ├── table/TableToolbar.vue           # Ô tìm kiếm Debounce 300ms + Bộ lọc
│   │   ├── data/StatusBadge.vue             # Huy hiệu trạng thái nghiệp vụ chuẩn màu
│   │   └── data/CurrencyText.vue            # Định dạng VND với tabular-nums
│   ├── composables/
│   │   ├── useCountdown.ts       # Bộ đếm ngược thời gian thực (giữ chỗ 10p)
│   │   └── useDebounce.ts        # Tiện ích hoãn xử lý cho ô tìm kiếm
│   └── utils/
│       └── formatters.ts         # formatVnd, formatDateTime, formatTime
│
└── modules/                      # Feature-Sliced Business Modules
    ├── auth/
    │   ├── api.ts                # authApi (login, register, me, telegram)
    │   ├── store.ts              # useAuthStore (JWT, roles, memory preview)
    │   └── views/
    │       ├── LoginView.vue     # Đăng nhập & Đăng ký
    │       └── UserProfileView.vue# Hồ sơ cá nhân & Liên kết Telegram
    │
    ├── catalog-booking/
    │   ├── api.ts                # catalogBookingApi
    │   ├── store.ts              # useBookingStore
    │   ├── components/
    │   │   ├── TripCard.vue
    │   │   ├── TripSearchBar.vue # Thanh tìm kiếm có dải xung điện loading
    │   │   └── SeatMap2Deck.vue  # Sơ đồ ghế 2 tầng 5 trạng thái
    │   └── views/
    │       ├── HomeView.vue
    │       ├── TripListView.vue
    │       ├── SeatSelectionView.vue
    │       ├── CheckoutView.vue
    │       └── MyBookingsView.vue
    │
    ├── partner/
    │   ├── api.ts                # partnerApi
    │   ├── store.ts              # usePartnerStore (Ví ký quỹ, danh mục xe)
    │   └── views/
    │       ├── OperatorDashboardView.vue # Tổng quan 4 số dư ví ký quỹ
    │       ├── OperatorVehiclesView.vue  # [TÁCH RA] Quản lý đội xe
    │       ├── OperatorDriversView.vue   # [TÁCH RA] Quản lý đội ngũ tài xế
    │       ├── OperatorTripsView.vue     # Lập lịch trình chuyến xe
    │       ├── OperatorBannersView.vue   # Banner quảng cáo nhà xe
    │       └── OperatorProfileView.vue   # Hồ sơ thương hiệu nhà xe
    │
    └── admin/
        ├── api.ts                # adminApi
        ├── store.ts              # useAdminStore (KPIs, Cache danh mục)
        ├── composables/
        │   └── useAdminPermissions.ts
        └── views/
            ├── AdminDashboardView.vue    # KPI toàn sàn & Đối soát thanh toán
            ├── AdminBookingsView.vue     # [TÁCH RA TỪ 1701 DÒNG] Quản lý đơn & Hoàn vé
            ├── AdminUsersView.vue        # [TÁCH RA TỪ 1701 DÒNG] Quản trị người dùng
            ├── AdminRolesView.vue        # [TÁCH RA TỪ 1701 DÒNG] Ma trận 7x26 & Xem thử Role
            ├── AdminOperatorsView.vue    # Phê duyệt nhà xe & Hoa hồng 2 cấp
            ├── AdminWithdrawalsView.vue  # Phê duyệt lệnh rút tiền Escrow
            ├── AdminBannersView.vue      # Kiểm duyệt banner quảng cáo
            ├── AdminTripsView.vue        # [TÁCH RA TỪ SETTINGS] Quản lý chuyến xe sàn
            ├── AdminStationsView.vue     # [TÁCH RA TỪ SETTINGS] Danh mục bến xe toàn quốc
            └── AdminSettingsView.vue     # [TÁCH RA TỪ SETTINGS] Cấu hình tham số toàn sàn
```

---

## 2. Kế Hoạch Bóc Tách Chi Tiết "God Views"

### 2.1. Phân Rã `AdminUsersBookingsView.vue` (1.701 dòng $\to$ 3 Views độc lập):
1. **`AdminUsersView.vue` (~250 dòng):**
   - Chuyên biệt quản lý 3 nhóm người dùng: Khách hàng (Customer), Chủ xe (Operator) và Nhân viên vận hành sàn (System Staff).
   - Gọi trực tiếp `adminApi.getUsers(params)`, hỗ trợ tìm kiếm từ khóa, khóa/mở khóa tài khoản (`ADMIN_CANNOT_LOCK_SELF`), reset mật khẩu.
2. **`AdminRolesView.vue` (~350 dòng):**
   - Quản lý Ma trận phân quyền 7 Modules x 26 APIs quản trị và tính năng Xem thử giao diện (Role Preview Mode).
   - Nạp danh mục quyền động từ `adminApi.getPermissionsCatalog()`.
3. **`AdminBookingsView.vue` (~280 dòng):**
   - Chuyên biệt quản lý danh sách đơn đặt vé toàn sàn và nghiệp vụ Hoàn tiền cưỡng chế (Admin Refund 100%).
   - Nạp danh sách từ `adminApi.getBookings()`, xử lý Modal nhập lý do hoàn vé tối thiểu 10 ký tự lưu Audit Log.

### 2.2. Phân Rã `AdminSettingsView.vue` (498 dòng $\to$ 3 Views độc lập):
1. **`AdminStationsView.vue` (~180 dòng):** Quản lý danh mục bến xe (`adminApi.getStations()`), form tạo/sửa bến xe.
2. **`AdminTripsView.vue` (~220 dòng):** Quản lý điều hành danh sách chuyến xe toàn sàn.
3. **`AdminSettingsView.vue` (~120 dòng):** Form điều chỉnh các tham số toàn sàn (Platform Name, Seat Hold TTL, Commission, Turnaround Buffer).

### 2.3. Phân Rã `FleetAndDriversView.vue` (467 dòng $\to$ 2 Views độc lập):
1. **`OperatorVehiclesView.vue` (~220 dòng):** Quản lý danh sách phương tiện, loại ghế, hạn đăng kiểm và trạng thái bảo dưỡng.
2. **`OperatorDriversView.vue` (~200 dòng):** Quản lý danh sách tài xế, hạng bằng lái và số năm kinh nghiệm.
