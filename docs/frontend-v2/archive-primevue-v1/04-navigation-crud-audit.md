   # 04 – Khảo Sát Điều Hướng & Chuẩn Hóa CRUD (Navigation & CRUD Audit)

   > **Mã tài liệu:** `FE-V2-NAV-04`  
   > **Thời điểm rà soát:** 25/11/2026  
   > **Mục tiêu:** Tái cấu trúc Kiến trúc Thông tin (Information Architecture - IA), chuẩn hóa Sidebar theo Business Domains và xây dựng quy chuẩn CRUD chuẩn mực.

   ---

   ## 1. Khảo Sát Bảng Điều Hướng & Sidebar Hiện Tại (Current Navigation Mapping)

   Hiện tại, toàn bộ menu Sidebar của Admin và Operator được khai báo cứng trong file [DashboardLayout.vue](file:///home/vanthanh/Ind-Project/Java/ticket-booking/frontend/src/app/layouts/DashboardLayout.vue):

   ### 1.1. Thực Trạng Menu Quản Trị Viên (Admin Sidebar):
   | Nhãn Menu Hiện Tại | Route Path | View Xử Lý | Business Domains Bị Gộp | Quyền Hạn Bắt Buộc (Permissions) | Nhược Điểm Nghiệp Vụ & UX |
   | :--- | :--- | :--- | :--- | :--- | :--- |
   | **Tổng quan & Đối soát** | `/admin` | `AdminDashboardView.vue` | Finance, Payment Analytics | `PAYMENT_REPORT_VIEW`, `PAYMENT_LIST_VIEW` | Menu tốt, tập trung đúng báo cáo doanh thu & đối soát ví. |
   | **Duyệt Nhà xe & Hoa hồng** | `/admin/operators` | `AdminOperatorsView.vue` | Partner Onboarding, Commission Policy | `OPERATOR_VIEW` | Hợp lý, kết nối duyệt tài khoản đối tác và hoa hồng sàn. |
   | **Duyệt Rút tiền Escrow** | `/admin/withdrawals`| `AdminWithdrawalsView.vue` | Escrow Finance, Payout | `WITHDRAWAL_VIEW` | Menu đơn mục tiêu, chuẩn trách nhiệm. |
   | **Kiểm duyệt Banner** | `/admin/banners` | `AdminBannersView.vue` | Marketing & Content | `BANNER_VIEW` | Menu đơn mục tiêu, chuẩn trách nhiệm. |
   | **User, Role & Hoàn vé Admin** | `/admin/users-bookings` | `AdminUsersBookingsView.vue` | **Identity + Security + Customer Operations** | `USER_VIEW`, `ROLE_MANAGE_CRUD`, `BOOKING_VIEW` | **Bất hợp lý nghiêm trọng:** Gom 3 nghiệp vụ không liên quan vào 1 màn hình dài 1.701 dòng. Người quản lý vé bị buộc phải truy cập trang có quản lý mật khẩu tài khoản! |
   | **Bến xe, Chuyến & Cấu hình** | `/admin/settings` | `AdminSettingsView.vue` | **Master Data + Operations + System Config** | `STATION_VIEW`, `TRIP_MANAGE_CRUD`, `SYSTEM_SETTINGS_MANAGE`| **Bất hợp lý:** Gom quản lý danh mục địa lý (Bến xe), điều hành lịch trình chuyến xe và tham số toàn sàn vào 1 màn hình. |

   ### 1.2. Thực Trạng Menu Đối Tác Nhà Xe (Operator Sidebar):
   | Nhãn Menu Hiện Tại | Route Path | View Xử Lý | Nhược Điểm UX |
   | :--- | :--- | :--- | :--- |
   | **Tổng quan & Ví Ký Quỹ** | `/operator` | `OperatorDashboardView.vue` | Tốt, tập trung vào 4 số dư ví ký quỹ và rút tiền. |
   | **Đội xe & Tài xế** | `/operator/fleet` | `FleetAndDriversView.vue` | Gom 2 đối tượng CRUD vào 1 trang chuyển Tab. |
   | **Lập lịch Chuyến xe** | `/operator/trips` | `OperatorTripsView.vue` | Đạt chuẩn, hiển thị lịch trình và form tạo chuyến. |
   | **Banner & Thương hiệu** | `/operator/banners` | `OperatorBannersView.vue` | Tốt, quản lý banner quảng cáo nhà xe. |
   | *(Thiếu trên Sidebar)* | `/operator/profile` | `OperatorProfileView.vue` | View có tồn tại nhưng **không có link trên Sidebar**, người dùng phải bấm vào avatar góc phải mới vào được! |

   ---

   ## 2. Kiến Trúc Thông Tin & Sidebar Mục Tiêu (Target Information Architecture)

   Quy hoạch lại hệ thống Menu phân cấp tối đa 2 cấp, nhóm chặt chẽ theo **Business Domains**:

   ### 2.1. Cấu Trúc Sidebar Mới Cho Admin (Platform Governance):
   ```
   PLATFORM GOVERNANCE
   ├── 1. OVERVIEW & ANALYTICS (Tổng quan & Đối soát)
   │   ├── [Icon: LayoutDashboard] Bảng Điều Khiển Tổng Quan    -> /admin
   │   └── [Icon: FileCheck2]       Đối Soát Thanh Toán & VNPay   -> /admin/finance/reconciliation
   │
   ├── 2. OPERATIONS & CATALOG (Vận hành & Danh mục)
   │   ├── [Icon: Ticket]           Quản Lý Đơn Vé & Hoàn Vé     -> /admin/operations/bookings [TÁCH TỪ USERS-BOOKINGS]
   │   ├── [Icon: Bus]              Điều Hành Lịch Trình Chuyến  -> /admin/operations/trips    [TÁCH TỪ SETTINGS]
   │   └── [Icon: MapPin]           Danh Mục Bến Xe Toàn Quốc    -> /admin/operations/stations [TÁCH TỪ SETTINGS]
   │
   ├── 3. PARTNERS & ESCROW (Đối tác & Ví ký quỹ)
   │   ├── [Icon: Building2]        Duyệt Nhà Xe & Hoa Hồng      -> /admin/partners
   │   └── [Icon: Banknote]         Duyệt Lệnh Rút Tiền Escrow   -> /admin/partners/withdrawals
   │
   ├── 4. IDENTITY & ACCESS (Định danh & Phân quyền)
   │   ├── [Icon: Users]            Quản Trị Người Dùng          -> /admin/identity/users      [TÁCH TỪ USERS-BOOKINGS]
   │   └── [Icon: ShieldAlert]      Ma Trận Vai Trò & Quyền Hạn  -> /admin/identity/roles      [TÁCH TỪ USERS-BOOKINGS]
   │
   ├── 5. MARKETING & CONTENT (Tiếp thị & Nội dung)
   │   └── [Icon: Image]            Kiểm Duyệt Banner Quảng Cáo  -> /admin/marketing/banners
   │
   └── 6. SYSTEM CONFIGURATION (Hệ thống sàn)
      └── [Icon: Settings]         Tham Số & Cấu Hình Toàn Sàn  -> /admin/system/settings     [TÁCH TỪ SETTINGS]
   ```

   ### 2.2. Cấu Trúc Sidebar Mới Cho Operator (Partner Workspace):
   ```
   PARTNER WORKSPACE
   ├── 1. OVERVIEW (Tổng quan)
   │   └── [Icon: LayoutDashboard] Tổng Quan & Ví Ký Quỹ         -> /operator
   │
   ├── 2. FLEET & RESOURCES (Tài nguyên vận tải)
   │   ├── [Icon: BusFront]        Quản Lý Đội Xe               -> /operator/fleet/vehicles    [TÁCH RIÊNG VIEW]
   │   └── [Icon: UserCheck]       Đội Ngũ Tài Xế               -> /operator/fleet/drivers     [TÁCH RIÊNG VIEW]
   │
   ├── 3. TRIPS & SCHEDULE (Lịch trình vận hành)
   │   └── [Icon: CalendarDays]    Lập Lịch Chuyến Xe           -> /operator/trips
   │
   ├── 4. MARKETING (Thương hiệu)
   │   └── [Icon: Megaphone]       Banner & Thương Hiệu         -> /operator/banners
   │
   └── 5. SETTINGS (Hồ sơ)
      └── [Icon: Building]        Hồ Sơ Doanh Nghiệp           -> /operator/profile          [HIỆN LINK CHÍNH THỨC]
   ```

   ### 2.3. Chiến Lược Điều Hướng Tương Thích Ngược Có Ý Thức Về Quyền Hạn (Permission-Aware Legacy Redirects)
   Để bảo toàn tuyệt đối các liên kết sâu (deep links), lịch sử duyệt web và bookmark cũ mà không đẩy người dùng vào lỗi `403 Forbidden`, hệ thống Route Guard áp dụng điều hướng thông minh dựa trên quyền hạn thực tế (`auth.hasPermission`):

   1. **Legacy Route `/admin/users-bookings`:**
      ```typescript
      beforeEnter: (to, from, next) => {
        const auth = useAuthStore()
        if (auth.hasPermission('USER_VIEW')) return next('/admin/identity/users')
        if (auth.hasPermission('ROLE_MANAGE_CRUD')) return next('/admin/identity/roles')
        if (auth.hasPermission('BOOKING_VIEW')) return next('/admin/operations/bookings')
        return next('/403')
      }
      ```
   2. **Legacy Route `/admin/settings`:**
      ```typescript
      beforeEnter: (to, from, next) => {
        const auth = useAuthStore()
        if (auth.hasPermission('STATION_VIEW')) return next('/admin/operations/stations')
        if (auth.hasPermission('TRIP_MANAGE_CRUD')) return next('/admin/operations/trips')
        if (auth.hasPermission('SYSTEM_SETTINGS_MANAGE')) return next('/admin/system/settings')
        return next('/403')
      }
      ```
   3. **Legacy Route `/operator/fleet`:**
      ```typescript
      redirect: '/operator/fleet/vehicles'
      ```
   4. **Cập nhật đồng bộ các chức năng liên quan:**
      - `auth.getHomeRoute()`: Trả về route đầu tiên mà người dùng có quyền trong danh mục menu thay vì mặc định cố định.
      - Nút "Thoát xem thử" (`stopRolePreview`): Điều hướng về `/admin/identity/roles` (thay vì link cũ `/admin/users-bookings`).
      - Breadcrumb: Tự động phân giải cây cha-con theo Business Domain chuẩn.

   ---

   ## 3. Tiêu Chuẩn Trang CRUD Thống Nhất (CRUD Page Standard)

   Toàn bộ các trang quản trị danh mục (CRUD) sẽ tuân theo layout khung chuẩn duy nhất **`CrudPageLayout.vue`**:

   ```
   +----------------------------------------------------------------------------------------+
   | Breadcrumb: Trang chủ / Quản lý / Danh sách người dùng                                   |
   |                                                                                        |
   | PAGE HEADER:                                                                           |
   | [Tiêu đề trang: Quản trị Người Dùng]           [Action 1: Xuất Excel] [Primary: + Thêm mới] |
   | Mô tả ngắn mục đích nghiệp vụ trang...                                                 |
   +----------------------------------------------------------------------------------------+
   | SUMMARY KPI CARDS (Tùy chọn nếu có giá trị):                                           |
   | [ Thẻ 1: Tổng số 1.240 ]  [ Thẻ 2: Đang hoạt động 1.180 ]  [ Thẻ 3: Đã khóa 60 ]        |
   +----------------------------------------------------------------------------------------+
   | TABLE TOOLBAR:                                                                         |
   | [ Tìm kiếm từ khóa... (Debounced 300ms) ]  [ Dropdown Bộ Lọc ]  [ Nút Xóa Lọc ]       |
   +----------------------------------------------------------------------------------------+
   | DATA TABLE:                                                                            |
   | [x] Mã        Tên          Email         Vai trò       Trạng thái       Thao tác   |
   | [ ] USR-01    Nguyễn Văn A a@velobus.vn [Badge Khách] [Badge Active]   [Sửa] [Khóa] [Xóa]|
   | ... (Hiển thị DataTable Loading Skeleton khi đang tải)                                 |
   |                                                                                        |
   | PAGINATION: Hiển thị 1 - 10 / Tổng số 1.240 kết quả           [ < 1 2 3 4 5 > ]        |
   +----------------------------------------------------------------------------------------+
   | RIGHT-SIDE DRAWER (Kích thước 520px hoặc 680px - Mở khi Thêm mới / Chỉnh sửa)          |
   | Header: "Thêm Mới Người Dùng"                                                  [ Đóng X]|
   | Body: Form Fields tuần tự, validation inline bên dưới.                                 |
   | Footer: [ Hủy bỏ ]                                          [ Lưu Thay Đổi (Spinner) ] |
   +----------------------------------------------------------------------------------------+
   ```

   ### Quy tắc sử dụng Container Tương Tác:
   1. **Right-Side Drawer (Độ rộng 520px / 680px):**
      - Sử dụng cho: Mọi Form tạo mới (Create) và chỉnh sửa (Edit) có nhiều hơn 3 trường nhập liệu. Giúp người dùng vừa điền form vừa đối chiếu được bảng dữ liệu phía sau.
   2. **Modal Dialog (Tối đa 480px, bo góc 16px):**
      - Sử dụng cho: Các thao tác xác nhận đơn giản, hành động nguy hiểm (Confirm Delete, Confirm Rejection, Confirm Refund) hoặc form nhập lý do ngắn (1-2 trường).
   3. **Detail Page Riêng Biệt (`/admin/.../:id`):**
      - Sử dụng cho: Các đối tượng có dữ liệu phức tạp nhiều tầng như Chi tiết đơn hàng booking (kèm timeline thanh toán, ghế, vé điện tử) hoặc Chi tiết hồ sơ nhà xe.
