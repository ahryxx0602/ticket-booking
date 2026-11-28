# 19 – Đề Xuất Các Hướng Thiết Kế Giao Diện UI & Khung Duyệt Thiết Kế (UI Design Direction Proposal)

> **Mã tài liệu:** `FE-V2-UI-19`  
> **Thời điểm xây dựng:** 25/11/2026  
> **Mục tiêu:** Xây dựng **3 Hướng Thiết Kế Trực Quan (Visual Directions)** độc đáo, chuyên nghiệp, vượt xa giao diện template mặc định, kèm kế hoạch phát triển **Bộ Trình Diễn Giao Diện (UI Showcase)** 9 màn hình trọng yếu trình duyệt tại **Mandatory UI Design Approval Gate**.

---

## 1. Ba Hướng Thiết Kế Trực Quan Đề Xuất (3 Visual Directions)

### Hướng 1: Modern Enterprise SaaS (Cảm hứng: Stripe & Linear) — [KHUYẾN NGHỊ]
- **Triết lý:** Tối ưu hóa cho mật độ thông tin cao, trải nghiệm làm việc liền mạch, chính xác và sắc sảo của phần mềm quản trị doanh nghiệp cao cấp.
- **Bảng màu chủ đạo:**
  - Sidebar: Neutral Slate sáng hoặc Dark Graphite (`#0F172A`) với đường viền siêu mỏng 1px (`#E2E8F0` / `#1E293B`).
  - Brand Accent: Electric Indigo (`#4F46E5` / `#6366F1`) kết hợp Primary Blue (`#2563EB`).
  - Nền ứng dụng: Zinc Off-White (`#F8FAFC`).
  - Bề mặt Card: Trắng tinh khiết (`#FFFFFF`) với viền `border border-slate-200/80` và đổ bóng siêu tinh tế `shadow-[0_1px_3px_rgba(0,0,0,0.05)]`.
- **Typography:** Inter 100%, tỷ lệ font chữ compact (Data font: 13-14px), các số liệu tiền tệ và số ghế áp dụng triệt để `tabular-nums`.
- **Mật độ bảng dữ liệu (DataTable Density):** Compact/Dense (chiều cao hàng 44px), hiển thị được nhiều dòng trên màn hình mà không cần cuộn trang.
- **Header & Navigation:** Thanh Topbar mờ bán trong suốt (Glassmorphism nhẹ `backdrop-blur-md bg-white/90`), tích hợp Breadcrumb động và ô tìm kiếm toàn cục (Global Cmd+K style).

### Hướng 2: Premium Mobility & Travel (Cảm hứng: Airbnb Luxe & Uber Transit)
- **Triết lý:** Tập trung vào trải nghiệm cảm xúc của hành khách và đối tác vận tải, tạo cảm giác sang trọng, tin cậy và thư thái trên mỗi hành trình.
- **Bảng màu chủ đạo:**
  - Sidebar: Deep Ocean Navy (`#142B4A`) kết hợp ánh vàng hoàng kim Amber mềm (`#F59E0B`).
  - Nền ứng dụng: Ấm nhẹ Warm Canvas (`#F9FAFB`).
  - Thẻ Card: Bo góc lớn `rounded-2xl` (16px - 20px), đổ bóng mềm đa tầng `shadow-lg shadow-indigo-950/5`.
- **Typography:** Be Vietnam Pro cho Tiêu đề lớn (Display) tạo nét ấm cúng, gần gũi của thương hiệu Việt; Inter cho nội dung chi tiết.
- **Mật độ bảng dữ liệu:** Comfortable (chiều cao hàng 52px), ưu tiên hình ảnh xe, logo nhà xe và trạng thái trực quan.
- **Hiệu ứng chuyển động (Motion):** Mượt mà, thời gian chuyển đổi 200ms với đường cong gia tốc `cubic-bezier(0.16, 1, 0.3, 1)`.

### Hướng 3: Minimal Monochromatic Dashboard (Cảm hứng: Vercel & Raycast)
- **Triết lý:** Tối giản tuyệt đối, triệt tiêu mọi màu sắc thừa thãi, để dữ liệu và hành động là trung tâm của sự chú ý.
- **Bảng màu chủ đạo:**
  - Đơn sắc tương phản cao (Monochrome): Đen sâu (`#000000`), Xám trung tính (`#71717A`), Trắng tuyết (`#FFFFFF`).
  - Điểm nhấn trạng thái: Chỉ dùng màu sắc cho trạng thái nghiệp vụ (Xanh lá thành công, Đỏ nguy hiểm).
  - Khung viền: Đường kẻ kỹ thuật sắc nét (High-contrast hairline borders).
- **Typography:** Inter kết hợp JetBrains Mono cho các mã đơn vé (`BKG-20261125`), biển số xe và thời gian.
- **Đặc trưng:** Cực kỳ nhẹ, tốc độ phản hồi mắt tức thì, mang phong cách công nghệ tân tiến.

---

## 2. Kế Hoạch Bộ Trình Diễn Giao Diện UI Showcase (9 Màn Hình Trọng Yếu)

Trước khi tiến hành viết lại toàn bộ mã nguồn ứng dụng, đội ngũ kỹ thuật sẽ xây dựng một bản nguyên mẫu độc lập (Isolated UI Prototype) tại môi trường nội bộ để Quý người dùng kiểm duyệt trực quan:

1. **Admin Application Shell:**
   - Trình diễn Sidebar 256px có thể Collapse về 76px, Topbar với Breadcrumb tự động, thanh tiến trình điều hướng `AppProgressBar`, Avatar và Bộ chọn chiều rộng giao diện (`LayoutWidthSwitcher`).
2. **Admin Reference CRUD Page (Trang Quản trị Mẫu):**
   - Áp dụng đầy đủ khung `CrudPageLayout.vue`: PageHeader, 4 thẻ Summary KPIs, TableToolbar tìm kiếm Debounced 300ms, bộ lọc Status và DataTable phân trang thật.
3. **Right-Side Create/Edit Drawer (520px / 680px):**
   - Trình diễn Form tạo/sửa dữ liệu có Header cố định, Body cuộn riêng, validation inline và nút submit có Spinner loading.
4. **Operator Dashboard:**
   - Trình diễn 4 khối thẻ số dư ví ký quỹ Fintech Escrow (Khả dụng, Tạm giữ, Đang quyết toán, Đã rút) và Bảng lịch sử giao dịch.
5. **Customer Trip Search Bar:**
   - Thanh tìm kiếm trạm đi, trạm đến, ngày đi với dải shimmer loading khi gõ tìm kiếm.
6. **Trip Search Results List:**
   - Danh sách thẻ chuyến xe `TripCard` hiển thị giờ xuất bến, giá vé VND, nhà xe và số ghế trống.
7. **Two-Deck Seat Selection (Sơ đồ ghế 2 tầng):**
   - Sơ đồ tầng 1 và tầng 2 chuẩn 5 trạng thái màu, hỗ trợ duyệt bằng phím Tab/Mũi tên (WCAG AA) và đồng hồ đếm ngược 10 phút.
8. **Auth Split-Screen (Đăng nhập / Đăng ký):**
   - Giao diện 45% Brand Panel bên trái và 55% Form đăng nhập bên phải.
9. **Loading, Empty & Error States:**
   - Trình diễn `TableSkeleton`, `EmptyState` có CTA "Thêm mới ngay", `ErrorState` có nút "Thử lại kết nối".

---

## 3. Quy Trình Duyệt Tại Mandatory UI Design Approval Gate

```
[Hoàn tất Đề xuất 3 Hướng Thiết Kế] 
            │
            ▼
[Người dùng chọn 1 trong 3 Hướng Visual] 
            │
            ▼
[Xây dựng Isolated UI Showcase (9 màn hình)] 
            │
            ▼
[Người dùng thẩm định trực tiếp trên trình duyệt] 
            │
            ▼
[CẤP PHÊ DUYỆT THIẾT KẾ] ───► [Bắt đầu Triển Khai Mã Nguồn Các Module]
```
