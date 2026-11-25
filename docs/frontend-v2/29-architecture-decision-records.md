# TỔNG HỢP CÁC QUYẾT ĐỊNH KIẾN TRÚC FRONTEND V2 (ARCHITECTURE DECISION RECORDS)
**Mã tài liệu:** `DOC-FE-V2-29`  
**Phiên bản:** `2.1.0` (Updated post-review) | **Trạng thái:** `APPROVED FOR IMPLEMENTATION PLANNING`  
**Tài liệu liên quan:** `30-frontend-plan-review-report.md`, `34-frontend-quality-gates-and-acceptance.md`  

---

## ADR-01: Chấp thuận Theme Claude+ (OKLCH) làm Nguồn Tham chiếu Thị giác Duy nhất

### Bối cảnh
Giao diện trước đây mang phong cách SaaS lạnh lẽo (`#142B4A`), thiếu bản sắc lữ hành và gặp phản hồi tiêu cực về việc lạm dụng gradient/badge. Dự án cần một phong cách đẳng cấp, ấm áp và tối giản.

### Quyết định
Chính thức áp dụng theme **Claude+** của tác giả Luis Llanes trên TweakCN làm Canonical Source of Truth:
- Bảng màu: OKLCH, tông màu ấm Warm Minimalist (Terracotta `#C96442` Luminance `0.2192`, Warm Charcoal, Warm Off-white `#FAF9F5`).
- Typography: Font chính `Outfit`, font mã nguồn `Geist Mono`.
- Base Radius: `1rem` (16px).
- Kiến trúc Token 3 Tầng: Lớp 1 (Canonical bất biến), Lớp 2 (VeloBus Extended Semantic cho trạng thái nghiệp vụ), Lớp 3 (Accessibility Overrides: nâng độ đậm `font-semibold` trên nút Primary để đạt WCAG 2.1 Controls/Large Text 3.0:1 và dùng `--status-danger` cho các nút phá hủy).

### Hệ quả
- Nhận diện thương hiệu độc đáo, ấm áp, cao cấp.
- Giải quyết triệt để vấn đề tương phản toán học và semantic cảnh báo nguy hiểm.

---

## ADR-02: Chiến lược Tương thích Tailwind CSS (Cầu nối CSS Variable Bridging)

### Bối cảnh
Claude+ xuất theo định dạng Tailwind CSS v4 (`@theme inline`), trong khi dự án hiện đang chạy ổn định trên Tailwind CSS `v3.4.17` và Vite 6. Nâng cấp nóng lên Tailwind v4 tiềm ẩn nguy cơ phá vỡ build tools và plugins.

### Quyết định
Áp dụng **Chiến lược Cầu nối CSS Variables (CSS Variable Bridging)**:
1. Đặt toàn bộ biến OKLCH của Claude+ vào `:root` và `.dark` trong `src/assets/main.css`.
2. Trong `tailwind.config.js`, mở rộng `colors` tham chiếu trực tiếp tới các biến CSS này qua hàm `var(...)`.
3. Cho phép hệ thống vừa sử dụng đầy đủ các tiện ích của Tailwind v3 hiện tại, vừa tương thích 100% với định dạng biến màu của Claude+.

---

## ADR-03: Thay thế Toàn diện PrimeVue bằng Shadcn Vue (Reka UI Primitives)

### Bối cảnh
PrimeVue 4 chiếm bundle nặng, trong khi khảo sát thực tế phát hiện 95% màn hình sản xuất không sử dụng component của PrimeVue mà viết bằng HTML/Tailwind thủ công, dẫn đến sự chắp vá, thiếu đồng bộ về accessibility.

### Quyết định
Loại bỏ hoàn toàn PrimeVue, chuyển sang **Shadcn Vue** trên nền **Reka UI**:
- Shadcn Vue cung cấp source code component trực tiếp (`src/components/ui/`), toàn quyền kiểm soát mã nguồn.
- Tree-shaking 100% đối với Lucide Icons, loại bỏ 500KB font PrimeIcons tĩnh.
- Quản lý gói độc quyền qua `npm` (không dùng `pnpm`).

---

## ADR-04: Áp dụng TanStack Vue Table với 6 Chiến lược Đáp ứng

### Bối cảnh
Các màn hình Admin và Operator hiện tự viết thẻ `<table>` HTML thủ công, thiếu phân trang chuẩn, không có sorting và vỡ giao diện trên mobile.

### Quyết định
Sử dụng **TanStack Vue Table (`@tanstack/vue-table`)** kết hợp 6 chiến lược phân hóa cho từng loại bảng:
1. Priority Column Table (Ẩn cột theo độ ưu tiên).
2. Mobile List Card (Chuyển thành danh sách thẻ).
3. Horizontally Scrollable Table (Cuộn ngang nội bộ có bóng mờ).
4. Responsive Permission Matrix (Lưới Accordion cho 7 Modules × 26 APIs).
5. Master-Detail Layout (Danh bạ 2 bước).
6. Standard Responsive Table (Co giãn fluid).

---

## ADR-05: Áp dụng Vue Sonner làm Hệ thống Thông báo Trung tâm

### Bối cảnh
`App.vue` cũ dùng `<Toast />` của PrimeVue và `useToast()`, tạo ra sự phụ thuộc runtime không cần thiết.

### Quyết định
Chuyển đổi toàn bộ thông báo sang **Vue Sonner (`vue-sonner`)**:
- Hỗ trợ stacking tối đa 3 toasts, tự động đóng sau 4000ms, vuốt để tắt trên mobile.
- API đơn giản, độc lập: `toast.success()`, `toast.error()`, `toast.warning()`, `toast.info()`.

---

## ADR-06: Chuẩn hóa Chính sách Xác nhận Thao tác (Confirmation Policy)

### Bối cảnh
Các hành động nhạy cảm (Khóa user, Duyệt giải ngân Escrow, Đổi hoa hồng, Cưỡng chế hoàn vé) trước đây thiếu quy chuẩn thống nhất về cảnh báo hệ quả.

### Quyết định
- Áp dụng **Shadcn AlertDialog** bắt buộc cho mọi thao tác phá hủy hoặc thay đổi tài chính quan trọng.
- Dialog bắt buộc nêu rõ: Tên đối tượng, Mô tả hệ quả không thể hoàn tác, Nút Hủy và Nút Xác nhận có trạng thái loading/chống submit đúp.
- Không tự giả định các tính năng backend chưa hỗ trợ (idempotency keys, undo tự động).

---

## ADR-07: Kiến trúc Theme Customizer Runtime & Chống FOUC

### Bối cảnh
Cần cho phép người dùng tùy biến giao diện trực tiếp (Light/Dark/System, màu sắc, bo góc) và lưu trữ bền vững mà không phụ thuộc backend API.

### Quyết định
- Composable `useTheme.ts` quản lý state và cập nhật trực tiếp lên `:root` và `.dark`.
- Lưu trữ vào `localStorage:velobus_theme_config_v2`.
- Script inline nhỏ gọn trong `<head>` của `index.html` ngăn chặn hoàn toàn hiện tượng chớp nháy giao diện (FOUC).
- Lắng nghe sự kiện `matchMedia` để đồng bộ khi hệ điều hành đổi theme.

---

## ADR-08: Kiểm soát Tràn viền & Nghiệm thu Responsive Nghiêm ngặt trên 9 Viewports

### Bối cảnh
Yêu cầu responsive trên đa kích thước màn hình là tiêu chuẩn bắt buộc cho nền tảng vận tải lữ hành.

### Quyết định
- Cưỡng chế quy tắc: **100% màn hình phải đạt `scrollWidth <= innerWidth` (0px horizontal overflow ngoài ý muốn)**.
- Kiểm thử tự động bằng Playwright trên 9 viewports chuẩn (`1920x1080` đến `320x568`).
- Lệnh kiểm tra chạy đơn lẻ, nghiêm cấm dùng toán tử `||` để che giấu lỗi kiểm thử.

---

## ADR-09: Quy trình Cổng Chất lượng Phân tầng (Multi-Tiered Quality Gates A - D)

### Bối cảnh
Tránh nguy cơ triển khai đại trà khi các thành phần nền tảng hoặc định hướng trực quan chưa đạt yêu cầu của người dùng.

### Quyết định
Thiết lập 4 Cổng Kiểm soát Nghiêm ngặt:
- **Cổng A (Foundation):** Design tokens, Typography, Base Primitives, Theme Customizer.
- **Cổng B (Application Shell):** Layouts Admin, Operator, Customer, Mobile Drawer.
- **Cổng C (Representative Screens):** 4 màn hình đại diện (**BẮT BUỘC CÓ USER VISUAL APPROVAL**).
- **Cổng D (Full Migration):** 100% 20 màn hình hoàn tất, feature/contract/RBAC parity, 0px overflow, xóa sạch PrimeVue, 100% test xanh.
