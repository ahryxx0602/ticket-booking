# 03 – Khảo Sát Giao Diện, Trải Nghiệm & Khả Năng Tiếp Cận (UI/UX Audit)

> **Mã tài liệu:** `FE-V2-AUD-03`  
> **Thời điểm rà soát:** 25/11/2026  
> **Phương pháp:** Runtime Visual Inspection qua Playwright Test Automation trên 4 kích thước màn hình (Desktop 1440px, Laptop 1280px, Tablet 768px, Mobile 375px/390px) kết hợp phân tích mã nguồn CSS/Tokens.

---

## 1. Đánh Giá Tính Nhất Quán Trực Quan (Visual Consistency)

### 1.1. Bảng Màu & Nhận Diện Thương Hiệu
- **Màu sắc chủ đạo:** Hiện tại hệ thống đang sử dụng `--brand-navy` (`#142B4A`) cho Sidebar/Footer và `--primary-600` (`#2563EB`) cho CTA/Active items.
- **Vấn đề phát hiện:**
  - Một số file vẫn còn tồn dư các lớp màu Tailwind mặc định không thuộc token của dự án (ví dụ `text-indigo-600`, `bg-purple-50`, `bg-blue-600`) gây xung đột với bảng mã token chính thức tại `assets/main.css`.
  - Độ tương phản màu (Color Contrast) của nhãn trạng thái (Status Badges) và text phụ (`text-slate-400` trên nền xám nhạt `#f1f5f9`) ở một số màn hình chưa đạt ngưỡng tối thiểu WCAG AA (tỷ lệ tương phản < 4.5:1).

### 1.2. Typography & Phân Cấp Nội Dung
- **Font gia đình:** Khai báo `['Inter', 'Be Vietnam Pro', 'system-ui', 'sans-serif']`.
- **Vấn đề phát hiện:**
  - Các giá trị số liệu tiền tệ VND và đồng hồ đếm ngược giữ chỗ ở một số View (`MyBookingsView.vue`, `AdminDashboardView.vue`) chưa áp dụng triệt để thuộc tính `tabular-nums` (`font-variant-numeric: tabular-nums`), dẫn đến hiện tượng giật rung layout (layout jitter) khi các chữ số thay đổi giá trị.
  - Phân cấp kích thước font chữ chưa chặt chẽ: Sự chênh lệch giữa tiêu đề cấp 1 (H1 - 24px) và tiêu đề bảng dữ liệu (18px) chưa đủ tạo điểm nhấn thị giác cho một trang Enterprise Dashboard.

### 1.3. Spacing, Bo Góc (Radius) & Đổ Bóng (Elevation)
- Bo góc các thẻ Card là `rounded-xl` (12px), nút bấm là `rounded-md` (8px).
- Vấn đề: Các Modal Dialog và Form Drawer sử dụng đổ bóng quá nặng (`shadow-2xl`) nhưng thiếu lớp backdrop blur nhẹ, làm giảm cảm giác tinh tế cao cấp của giao diện SaaS.

---

## 2. Kết Quả Khảo Sát Đa Kích Thước Viewport (Responsive Behavior)

| Kích thước Viewport | Thiết bị đại diện | Mức độ tương thích | Vấn đề UX quan sát được |
| :--- | :--- | :---: | :--- |
| **Desktop 1440px** | Màn hình ngoài / iMac | **Tốt (95%)** | Bố cục 256px Sidebar và thanh công cụ hiển thị chuẩn mực. |
| **Laptop 1280px** | MacBook / Ultrabook | **Khá (85%)** | Bảng dữ liệu DataTable tại `AdminUsersBookingsView` bắt đầu xuất hiện thanh cuộn ngang do quá nhiều cột (7 cột). |
| **Tablet 768px** | iPad Portrait / Galaxy Tab | **Trung bình (70%)** | Sidebar chuyển sang ẩn, nhưng nút kích hoạt mở Sidebar trên Topbar nằm quá sát logo; sơ đồ ghế 2 tầng bị thu nhỏ khó bấm. |
| **Mobile 375px - 390px**| iPhone 13/14/15 | **Cần cải thiện (60%)** | - Modal xác nhận hoàn tiền bị bóp méo nội dung do fix cứng độ rộng `w-[500px]`.<br>- Bảng dữ liệu vỡ layout nếu không bọc trong container cuộn riêng.<br>- Vùng bấm (Touch target) một số icon action nhỏ hơn chuẩn 44×44px. |

---

## 3. Khảo Sát Hành Trình Người Dùng (User Journey Audit)

### 3.1. Luồng Khách Hàng (Customer Booking Journey)
1. **Tìm kiếm chuyến (`/` $\to$ `/search`):**
   - *Pain Point:* Khi bấm nút "Tìm Chuyến Xe", giao diện chuyển trang tức thì nhưng không có Skeleton hiển thị trước danh sách chuyến; các thẻ `TripCard` xuất hiện đột ngột gây cảm giác giật cục.
2. **Chọn ghế & Giữ chỗ (`/trips/:id/seats`):**
   - *Ưu điểm:* Sơ đồ 2 tầng trực quan, có đồng hồ đếm ngược 10 phút.
   - *Pain Point:* Khi một ghế bị người khác giữ trên Redis, client không có thông báo Toast cụ thể mà chỉ hiển thị màu ghế xám/khóa, khiến người dùng bối rối tưởng lỗi ứng dụng.
3. **Thanh toán (`/checkout/:id`):**
   - *Pain Point:* Nút "Thanh Toán Qua VNPay" khi click gọi API chưa có trạng thái Spinner loading và disabled, cho phép người dùng click nhiều lần (double submit).

### 3.2. Luồng Đối Tác Nhà Xe (Operator Journey)
1. **Quản lý Đội xe & Tài xế (`/operator/fleet`):**
   - *Pain Point:* Phải chuyển tab qua lại giữa Phương tiện và Tài xế dù 2 thực thể này thường xuyên cần gán ghép với nhau khi lập lịch chuyến xe.
   - *Pain Point:* Thao tác tạo mới phương tiện dùng Drawer bên phải nhưng khi submit thành công Drawer đóng lại quá nhanh mà không có visual feedback xác nhận dòng dữ liệu mới trong bảng.

### 3.3. Luồng Quản Trị Sàn (Admin Governance Journey)
1. **Quản trị Người dùng & Phân quyền (`/admin/users-bookings`):**
   - *Pain Point trầm trọng nhất:* Quản trị viên phải cuộn trang liên tục giữa tab Users và Roles. Khi cấu hình quyền trong Ma trận 7x26, giao diện chiếm trọn chiều cao màn hình khiến người dùng mất phương hướng không biết đang thao tác trên Role nào.
   - *Pain Point:* Chức năng hoàn vé (Refund) bị giấu chung trong trang này thay vì nằm ở phân hệ Vận hành / Đơn vé (Operations / Bookings).

---

## 4. Đánh Giá Khả Năng Tiếp Cận & Phím Tắt (Accessibility - WCAG AA)

1. **Keyboard Navigation (Duyệt bằng bàn phím):**
   - Các nút bấm chính `AppButton.vue` hỗ trợ focus-visible tốt với ring xanh `#2563EB`.
   - Tuy nhiên, sơ đồ ghế xe khách (`SeatMap2Deck.vue`) **hoàn toàn không hỗ trợ duyệt bằng phím mũi tên hoặc phím Tab**. Người dùng khiếm thị hoặc người chỉ dùng bàn phím không thể chọn ghế.
2. **Screen Reader (Đọc màn hình):**
   - Các icon hành động trong bảng dữ liệu (như nút Edit, Delete, Lock) chỉ có icon SVG mà thiếu thuộc tính `aria-label` hoặc thẻ ẩn `sr-only`, khiến trình đọc màn hình chỉ phát âm "button".
3. **Prefers-Reduced-Motion:**
   - Ứng dụng chưa khai báo CSS query `@media (prefers-reduced-motion: reduce)` để tắt hiệu ứng trượt của Drawer và hiệu ứng đếm ngược cho người dùng nhạy cảm với chuyển động.
