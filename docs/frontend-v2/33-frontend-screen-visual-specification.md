# ĐẶC TẢ THỊ GIÁC CHI TIẾT TỪNG MÀN HÌNH (SCREEN-LEVEL VISUAL SPECIFICATION)
**Mã tài liệu:** `DOC-FE-V2-33`  
**Phiên bản:** `2.1.0` | **Trạng thái:** `APPROVED AS SPECIFICATION`  
**Phạm vi:** Đặc tả chi tiết 20 màn hình sản xuất thực tế của hệ thống VeloBus (18 Production Views + 2 System Views).

---

## 1. PHÂN HỆ KHÁCH HÀNG (CUSTOMER PORTAL)

### SCR-CUST-01: Trang chủ Tra cứu Chuyến xe (`HomeView.vue`)
- **Route:** `/` | **Phân quyền:** Public (Khách vãng lai & Người dùng)
- **Tham chiếu:** `REF-01` (Claude+), `REF-05` (Vexere Search Widget)
- **Layout Type:** `CustomerLayout.vue` (Header 64px, Container 1280px)
- **Page Header:** Tiêu đề Hero: *"Hành trình an tâm, trải nghiệm thoải mái"* kèm mô tả phụ.
- **Thành phần chính:**
  - *Search Box trung tâm:* 4 khối tương tác:
    1. Điểm đi (Combobox bến xe/tỉnh thành).
    2. Nút hoán đổi (Swap button) tròn 40px ở giữa với micro-animation xoay 180°.
    3. Điểm đến (Combobox bến xe/tỉnh thành).
    4. Bộ chọn ngày đi (Popover Calendar với định dạng ngày Việt Nam `dd/mm/yyyy`).
    5. Nút bấm *"Tìm chuyến xe"* kích thước lớn `lg` (h-11) màu Terracotta `#C96442`.
  - *Banner quảng cáo:* Lưới 3 cột hiển thị các banner khuyến mãi được duyệt từ backend.
  - *Lý do chọn VeloBus:* 3 thẻ giá trị cốt lõi (Vé điện tử tức thì, Nhà xe thẩm định, Hỗ trợ 24/7).
- **Trạng thái:** Shimmer skeleton khi load banner; Toast cảnh báo nếu điểm đi trùng điểm đến.
- **Responsive:** Trên mobile, thanh tìm kiếm chuyển sang xếp dọc (Vertical Stack), các ô nhập full-width.

### SCR-CUST-02: Kết quả Tìm kiếm Chuyến xe (`TripListView.vue`)
- **Route:** `/search` | **Phân quyền:** Public
- **Tham chiếu:** `REF-05` (Vexere Trip List), `REF-06` (FlixBus Summary)
- **Layout Type:** `CustomerLayout.vue`
- **Cấu trúc Màn hình (2 Cột):**
  - *Cột trái (280px Desktop):* Bộ lọc nâng cao (Khoảng giá, Giờ khởi hành: Sáng/Chiều/Tối, Nhà xe, Loại xe giường nằm).
  - *Cột phải (Còn lại):* Thanh sắp xếp (Giá tăng dần, Giờ đi sớm nhất) + Danh sách các `TripCard`.
- **Thành phần `TripCard`:**
  - Timeline khởi hành trực quan: Giờ đi - Bến đi -> Giờ đến - Bến đến kèm tổng thời gian di chuyển.
  - Tên nhà xe, loại xe (VIP 36 giường / Limousine 24 phòng), tiện ích (Wifi, Sạc, Nước).
  - Giá vé chuẩn định dạng VNĐ (Geist Mono font, màu Terracotta, kích thước `text-xl font-bold`).
  - Nút *"Chọn chỗ"* dẫn tới màn hình chọn ghế.
- **Trạng thái:** `SearchLoadingIndicator` dạng thanh tiến trình shimmer chạy dưới thanh tìm kiếm; `EmptyState` khi không tìm thấy chuyến phù hợp kèm nút "Đặt lại bộ lọc".
- **Responsive:** Trên mobile (`< 1024px`), bộ lọc ẩn vào nút *"Bộ lọc (3)"* mở Sheet trượt từ cạnh phải.

### SCR-CUST-03: Chọn Ghế 2 Tầng Vật Lý (`SeatSelectionView.vue`)
- **Route:** `/trips/:id/seats` | **Phân quyền:** Public (Giữ chỗ yêu cầu session)
- **Tham chiếu:** `REF-01` (Claude+), `REF-06` (FlixBus Seat Map)
- **Layout Type:** `CustomerLayout.vue`
- **Cấu trúc Màn hình:**
  - *Sơ đồ xe khách 2 tầng vật lý (`SeatMap2Deck.vue`):*
    - Mô phỏng thân xe: Mũi xe bo tròn, cabin lái xe có biểu tượng vô lăng và cửa lên xuống.
    - Lối đi trung tâm (Central Aisle) xuyên suốt.
    - Tabs chuyển tầng: Tầng dưới (Deck 1) và Tầng trên (Deck 2).
    - Dãy giường nằm bên trái (A-series) và bên phải (B-series).
    - 3 trạng thái ghế: Còn trống (Viền mảnh), Đang chọn (Nền Terracotta `#C96442`, chữ trắng), Đã bán/Đang giữ (Xám mờ, gạch chéo, disabled).
  - *Sidebar tóm tắt đặt vé:*
    - Danh sách ghế đang chọn (tối đa 4 ghế/lần).
    - Bộ đếm ngược giữ chỗ 10:00 phút (`useCountdown.js`) sau khi bấm Giữ chỗ thành công.
    - Tổng tiền tạm tính, nút *"Tiến hành đặt vé"*.
- **Hệ thống Feedback:** Dùng Vue Sonner toast khi giữ chỗ thành công hoặc thông báo khi ghế bị người khác chọn trước.
- **Responsive:** Trên mobile, sơ đồ xe căn giữa màn hình với kích thước chạm ghế tối thiểu `44×44px`. Thanh tóm tắt tiền và nút đặt vé cố định dưới đáy màn hình (Sticky Bottom Bar).

### SCR-CUST-04: Thanh toán & Thông tin Hành khách (`CheckoutView.vue`)
- **Route:** `/checkout/:bookingId` | **Phân quyền:** `ROLE_CUSTOMER`
- **Layout Type:** `CustomerLayout.vue`
- **Page Header:** Breadcrumbs (Trang chủ > Tra cứu > Chọn ghế > Thanh toán).
- **Thành phần chính:**
  - *Biểu mẫu thông tin hành khách:* Họ tên, Số điện thoại (kiểm tra regex VN 10 số), Email nhận vé điện tử, Điểm đón trả cố định.
  - *Cổng thanh toán:* Cổng VNPay chính thức với logo phương thức thẻ ATM/QR Pay.
  - *Đồng hồ đếm ngược giữ chỗ:* Nổi bật ở góc trên, cảnh báo đỏ khi còn dưới 2 phút.
- **Hành động chính:** Nút *"Thanh toán qua VNPay"* chuyển hướng an toàn tới VNPay Sandbox.
- **Xác nhận & Chống Submit đúp:** Tự động vô hiệu hóa nút bấm và hiển thị spinner ngay khi nhấn gửi.

### SCR-CUST-05: Quản lý Vé của tôi (`MyBookingsView.vue`)
- **Route:** `/my-bookings` | **Phân quyền:** `ROLE_CUSTOMER`
- **Chiến lược Bảng:** **Strategy 2 (Mobile List Card)**.
- **Thành phần:** Danh sách thẻ vé dạng Card lữ hành. Mỗi vé hiển thị:
  - Mã đặt chỗ (PNR) in đậm dạng `font-mono`.
  - Tuyến đường, ngày giờ khởi hành, biển số xe và vị trí ghế.
  - `StatusBadge` (CONFIRMED, CANCELLED, COMPLETED).
  - Mã QR Code vé điện tử sẵn sàng quét tại bến xe.
  - Nút *"Hủy vé"* (chỉ hiển thị nếu chuyến đi còn cách giờ xuất bến > 24h).
- **Hộp thoại xác nhận:** Khi bấm "Hủy vé", mở `AlertDialog` nêu rõ quy định khấu trừ phí hoàn vé (nếu có).

---

## 2. PHÂN HỆ XÁC THỰC (AUTHENTICATION)

### SCR-AUTH-01: Đăng nhập & Đăng ký (`LoginView.vue`)
- **Route:** `/login` | **Phân quyền:** Public (Chưa đăng nhập)
- **Tham chiếu:** `REF-01` (Claude+), `REF-04` (Shadcn Blocks Auth)
- **Layout Type:** `AuthLayout.vue` (Căn giữa màn hình, nền ấm `var(--background)`)
- **Thiết kế:**
  - Logo VeloBus tối giản ở đầu form.
  - Tabs chuyển đổi: *"Đăng nhập"* và *"Tạo tài khoản"*.
  - Ô nhập Tên đăng nhập / Email và Mật khẩu (có nút ẩn/hiện mật khẩu).
  - Nút bấm chính màu Terracotta full-width `h-10`.
  - Tuyệt đối loại bỏ các thuật ngữ kỹ thuật (JWT, Stateless, Token Version, Epoch).
- **Xử lý lỗi:** Thông báo lỗi inline màu đỏ ngay dưới ô nhập liệu khi tài khoản hoặc mật khẩu không chính xác.

### SCR-AUTH-02: Hồ sơ Cá nhân & Liên kết Telegram (`UserProfileView.vue`)
- **Route:** `/profile` | **Phân quyền:** Authenticated (Mọi vai trò)
- **Thành phần:**
  - Form xem/cập nhật thông tin họ tên, email, số điện thoại.
  - Form đổi mật khẩu bảo mật (Mật khẩu cũ, Mật khẩu mới, Xác nhận mật khẩu mới).
  - Khối liên kết tài khoản Telegram nhận vé và OTP tức thì: Nút mở Shadcn `Dialog` hiển thị mã OTP 6 số và liên kết bot Telegram.

---

## 3. PHÂN HỆ QUẢN TRỊ NỀN TẢNG (ADMIN BACKOFFICE)

### SCR-ADM-01: Tổng quan & Đối soát Doanh thu (`AdminDashboardView.vue`)
- **Route:** `/admin` | **Quyền:** `PAYMENT_REPORT_VIEW`, `PAYMENT_LIST_VIEW`
- **Layout Type:** `AdminLayout.vue` (Sidebar mở rộng 256px / thu gọn 64px, Header 64px)
- **Thành phần chính:**
  - *4 Thẻ KPI nền tảng:* GMV toàn sàn, Doanh thu hoa hồng VeloBus, Tổng đơn vé xuất, Số lượng đối tác nhà xe hoạt động.
  - *Bảng Cảnh báo Đối soát VNPay (Strategy 3 - Horizontally Scrollable Table):*
    - Hiển thị các giao dịch phát sinh chênh lệch số dư giữa cổng thanh toán VNPay và ví ký quỹ Escrow.
    - Cột thao tác: Nút *"Xử lý thủ công"* mở `AlertDialog` xác nhận giải quyết chênh lệch (chỉ kích hoạt khi user có quyền `PAYMENT_RECONCILE_RESOLVE`).

### SCR-ADM-02: Quản lý Đối tác & Cấu hình Hoa hồng (`AdminOperatorsView.vue`)
- **Route:** `/admin/operators` | **Quyền:** `OPERATOR_VIEW`
- **Chiến lược Bảng:** **Strategy 1 (Priority Column Table)**.
- **Thành phần:**
  - Bảng danh sách nhà xe: Tên doanh nghiệp, Biển hiệu, Mã số thuế, Số điện thoại, Tỷ lệ hoa hồng (%), Trạng thái.
  - Cột thao tác: Duyệt nhà xe, Từ chối kèm lý do, Cập nhật hoa hồng.
  - Form Sheet (Drawer 480px): Cập nhật thông tin chi tiết nhà xe.
  - `AlertDialog` phê duyệt hoa hồng: Kiểm tra biên giới hạn hợp lệ 0% đến 50%.

### SCR-ADM-03: Phê duyệt Rút tiền Ví Ký quỹ (`AdminWithdrawalsView.vue`)
- **Route:** `/admin/withdrawals` | **Quyền:** `WITHDRAWAL_VIEW`
- **Chiến lược Bảng:** **Strategy 1 (Priority Column Table)**.
- **Thành phần:** Danh sách yêu cầu rút tiền của nhà xe. Mỗi dòng hiển thị: Mã lệnh, Nhà xe, Số tiền yêu cầu rút (VNĐ), Tài khoản ngân hàng thụ hưởng, Thời gian yêu cầu, Trạng thái (PENDING, APPROVED, REJECTED).
- **Hành động:** Nút "Phê duyệt giải ngân" mở `AlertDialog` xác nhận trừ số dư ví ký quỹ và đánh dấu hoàn tất.

### SCR-ADM-04: Kiểm duyệt Banner Tiếp thị (`AdminBannersView.vue`)
- **Route:** `/admin/banners` | **Quyền:** `BANNER_VIEW`
- **Chiến lược:** **Strategy 2 (Card Grid)**.
- **Thành phần:** Lưới thẻ hình ảnh banner kèm tiêu đề, đường dẫn chuyển hướng, vị trí hiển thị và nút Toggle bật/tắt kích hoạt. Nút "Duyệt" và "Từ chối" đối với các banner do nhà xe tải lên.

### SCR-ADM-05: Quản trị Người dùng, Phân quyền RBAC & Hoàn vé (`AdminUsersBookingsView.vue`)
- **Route:** `/admin/users-bookings` | **Quyền:** `USER_VIEW`, `ROLE_MANAGE_CRUD`, `BOOKING_VIEW`
- **Độ phức tạp:** Cao nhất hệ thống.
- **Cấu trúc 3 Tabs Độc lập:**
  1. *Tab 1: Quản trị Người dùng:* Bảng danh sách tài khoản, vai trò, trạng thái khóa. Hành động khóa tài khoản có chốt chặn **ADMIN_CANNOT_LOCK_SELF** (nút khóa bị disabled đối với chính tài khoản đang đăng nhập).
  2. *Tab 2: Ma trận Phân quyền (Strategy 4 - Responsive Permission Matrix):* Hiển thị 7 Modules và 26 Quyền API dạng lưới toggles. Cho phép tạo Role mới hoặc sửa quyền của Custom Role.
  3. *Tab 3: Cưỡng chế Hoàn vé Admin:* Tra cứu mã vé PNR, xem chi tiết và kích hoạt hoàn vé. Bắt buộc nhập lý do kiểm toán (Audit Reason) trên `AlertDialog`.

### SCR-ADM-06: Bến xe, Lịch trình & Tham số Hệ thống (`AdminSettingsView.vue`)
- **Route:** `/admin/settings` | **Quyền:** `STATION_VIEW`, `TRIP_MANAGE_CRUD`, `SYSTEM_SETTINGS_MANAGE`
- **Chiến lược:** **Strategy 5 (Master-Detail Layout)**.
- **Thành phần:** Danh bạ bến xe toàn quốc (Thêm/Sửa vị trí địa lý), danh mục tuyến đường liên tỉnh, cấu hình thời gian giữ chỗ vé xe mặc định (5 - 15 phút).

---

## 4. PHÂN HỆ VẬN HÀNH ĐỐI TÁC (OPERATOR WORKSPACE)

### SCR-OP-01: Tổng quan Ví Ký quỹ & Doanh thu (`OperatorDashboardView.vue`)
- **Route:** `/operator` | **Quyền:** `ROLE_OPERATOR`
- **Layout Type:** `OperatorLayout.vue`
- **Thành phần:** Thẻ số dư ví ký quỹ khả dụng (Available Balance) và số dư phong tỏa (Locked Balance), nút *"Yêu cầu rút tiền"*, biểu đồ cột doanh thu 7 ngày gần nhất dùng bảng màu Claude+ Charts.

### SCR-OP-02: Quản lý Đội xe & Đội ngũ Tài xế (`FleetAndDriversView.vue`)
- **Route:** `/operator/fleet` | **Quyền:** `ROLE_OPERATOR`
- **Cấu trúc 2 Tabs:**
  - *Tab Phương tiện:* Biển số xe, Loại xe (VIP 36 giường / Limousine 24 phòng), Hạn kiểm định. Form Sheet thêm xe mới.
  - *Tab Tài xế:* Họ tên tài xế, Số điện thoại, Hạng giấy phép lái xe, Trạng thái phân công.

### SCR-OP-03: Lập lịch & Điều phối Chuyến xe (`OperatorTripsView.vue`)
- **Route:** `/operator/trips` | **Quyền:** `ROLE_OPERATOR`
- **Thành phần:** Lịch trình xuất bến các chuyến đi, bộ lọc ngày tháng, nút *"Tạo chuyến xe mới"* mở Form Drawer chọn bến đi, bến đến, giờ khởi hành và giá vé cơ sở cho từng tầng.

### SCR-OP-04: Banner Quảng cáo Nhà xe (`OperatorBannersView.vue`)
- **Route:** `/operator/banners` | **Quyền:** `ROLE_OPERATOR`
- **Thành phần:** Danh sách banner tiếp thị của riêng nhà xe gửi lên sàn, tích hợp bộ tải ảnh `FirebaseUploader`, trạng thái kiểm duyệt (Chờ duyệt, Đã duyệt, Bị từ chối kèm ghi chú).

### SCR-OP-05: Hồ sơ Pháp lý & Tài khoản Ngân hàng (`OperatorProfileView.vue`)
- **Route:** `/operator/profile` | **Quyền:** `ROLE_OPERATOR`
- **Thành phần:** Thông tin pháp nhân công ty vận tải, giấy phép kinh doanh, số tài khoản ngân hàng nhận tiền giải ngân ký quỹ Escrow.

---

## 5. CÁC MÀN HÌNH MÃ TRẠNG THÁI HỆ THỐNG (SYSTEM STATUS VIEWS)

### SCR-SYS-01: Trang Từ chối Truy cập 403 Forbidden (`ForbiddenView.vue`)
- **Route:** `/403` | **Phân quyền:** Public (Whitelist chống vòng lặp redirect)
- **Thiết kế:** Minh họa tối giản bằng icon Lucide `ShieldAlert`, thông báo bạn không có quyền hạn API cần thiết để truy cập phân hệ này, nút *"Quay lại trang an toàn"* tự động điều hướng theo vai trò người dùng (`authStore.getHomeRoute()`).

### SCR-SYS-02: Trang Không tìm thấy 404 Not Found (`NotFoundView.vue`)
- **Route:** `/:pathMatch(.*)*` | **Phân quyền:** Public
- **Thiết kế:** Tiêu đề lớn `404`, thông điệp trang không tồn tại hoặc đã được di chuyển, nút dẫn về trang chủ tra cứu chuyến xe.
