# 07 – Nghiên Cứu Tài Nguyên Mở Miễn Phí (Free Resource Research)

> **Mã tài liệu:** `FE-V2-RES-07`  
> **Thời điểm khảo sát:** 25/11/2026  
> **Phương pháp:** Khảo sát thực tế các kho mã nguồn mở chính thức của hệ sinh thái Vue 3, PrimeVue và Tailwind CSS.

---

## 1. Khảo Sát Chi Tiết Các Ứng Viên Tài Nguyên Mở (Resource Candidates)

### 1.1. PrimeVue Sakai (Official Admin Template)
- **Website chính thức:** https://sakai.primevue.org/
- **Kho mã nguồn:** https://github.com/primefaces/sakai-vue
- **Giấy phép (License):** **MIT License** (Miễn phí 100% cho mục đích thương mại và cá nhân, được phép sửa đổi và phân phối).
- **Trạng thái bảo trì:** Cập nhật liên tục bởi PrimeTek theo các bản phát hành của PrimeVue v4.
- **Tương thích:** Tương thích tuyệt đối với PrimeVue v4, Vue 3.5, Vite và Tailwind CSS.
- **Giá trị cốt lõi cho VeloBus:**
  - Cung cấp cấu trúc App Shell quản trị chuẩn mực: Bố cục Sidebar có khả năng Collapse mượt mà, Topbar responsive với avatar/menu người dùng, thanh Breadcrumb tự động theo Route.
  - Tách bạch rõ ràng giữa Layout Container và View Content.

### 1.2. PrimeVue Volt & PrimeBlocks Free
- **Website chính thức:** https://primevue.org/volt/ & https://primeblocks.org/
- **Giấy phép:** **MIT License** (Cho các block và component mã nguồn mở công khai).
- **Giá trị cốt lõi cho VeloBus:**
  - Cung cấp các khối giao diện chuyên dụng (DataTable Toolbar, Filter Drawer, KPI Stat Cards) viết bằng Tailwind CSS kết hợp PrimeVue Pass Through (PT) API.
  - Thiết kế sạch sẽ, hiện đại, không bị cảm giác "cũ kỹ" của các thư viện giao diện truyền thống.

### 1.3. PrimeVue Aura Preset & Pass Through (PT) API
- **Kho mã nguồn:** `@primeuix/themes/aura` (Đã cài đặt sẵn trong `package.json`).
- **Giấy phép:** **MIT License**.
- **Đánh giá kiến trúc:** Aura là Preset hiện đại nhất của PrimeTek, được thiết kế phẳng, đường viền thanh mảnh, phù hợp hoàn hảo với định hướng **Modern Enterprise Dashboard**.
- **Pass Through API:** Cho phép can thiệp trực tiếp vào từng thẻ HTML con bên trong component của PrimeVue mà không cần ghi đè CSS `!important`.

### 1.4. Lucide Icons (`lucide-vue-next`)
- **Website chính thức:** https://lucide.dev/
- **Giấy phép:** **ISC License** (Tương đương MIT, hoàn toàn tự do sử dụng).
- **Đánh giá kỹ thuật:** Đã có trong `package.json` (`v0.469.0`). Cung cấp hơn 1.400 icon vector dạng Vue Component, hỗ trợ Tree-shaking (chỉ đóng gói icon nào thực sự import, giúp giảm bundle size tới 90% so với font icon).

### 1.5. NProgress vs Custom Vue Router Navigation Progress Bar
- **So sánh:**
  - `nprogress` (MIT): Thư viện cũ, thao tác trực tiếp trên DOM ngoài tầm kiểm soát của Vue Reactive System.
  - **Custom `AppProgressBar.vue` (Đề xuất):** Tự xây dựng component thanh tiến trình đỉnh màn hình bằng Vue 3 + CSS Transition (~40 dòng code). Kết nối trực tiếp vào sự kiện `router.beforeEach()` và `router.afterEach()`. Hoàn toàn không tốn thêm dependency, kiểm soát 100% màu sắc theo `--primary-600` và hỗ trợ cả trạng thái lỗi màu đỏ.

---

## 2. Ma Trận Quyết Định Tài Nguyên (Resource Decision Matrix)

| Tài nguyên | Giấy phép | Mức độ phổ biến | Độ tương thích | Chất lượng trực quan | Chi phí tích hợp | Khuyến nghị sử dụng cho VeloBus |
| :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| **PrimeVue Sakai** | MIT | Rất cao (>3.5k stars) | 100% PrimeVue 4 | Cao (Chuẩn Enterprise) | Thấp (Tham khảo shell) | **SỬ DỤNG:** Học hỏi kiến trúc Application Shell, Topbar, Responsive Mobile Navigation. |
| **PrimeVue Volt** | MIT | Cao (Xu hướng 2026) | 100% Tailwind PrimeUI | Rất cao (Hiện đại) | Thấp (Copy pattern) | **SỬ DỤNG:** Tham khảo pattern Table Toolbar và Filter Panel. |
| **Aura Preset** | MIT | Chính chủ PrimeVue | Tuyệt đối | Tinh tế, cao cấp | Sẵn có | **GIỮ VỮNG:** Tiếp tục dùng `definePreset(Aura, ...)` làm Theme Core. |
| **Lucide Icons** | ISC | Rất cao (>15k stars) | Vue 3 Component | Rất cao (Đồng nhất nét vẽ)| Cực thấp | **CHUẨN HÓA 100%:** Thay thế toàn bộ PrimeIcons cũ. |
| **Custom AppProgressBar**| In-house | Tối ưu riêng | Tuyệt đối | Hoàn hảo | Rất thấp (40 dòng code)| **SỬ DỤNG:** Thay thế nprogress bên ngoài. |

---

## 3. So Sánh Các Phương Án Kiến Trúc Giao Diện (Theme Architecture Comparison)

### Phương án A: PrimeVue Styled Mode + Custom Aura Preset (ĐƯỢC ĐỀ XUẤT)
- **Mô tả:** Sử dụng PrimeVue Styled Mode với Preset thương hiệu `VeloBusPreset` kế thừa từ `Aura`, kết hợp `tailwindcss-primeui` để dùng class tiện ích.
- **Ưu điểm:**
  - Giữ lại toàn bộ sức mạnh, tính năng tương tác phức tạp và tính tiếp cận a11y của PrimeVue (DataTable sorting, pagination, datepicker, drawer).
  - Tận dụng mã nguồn hiện tại của dự án mà không cần viết lại CSS từ đầu.
  - Dễ bảo trì, tài liệu chính thức phong phú.
- **Nhược điểm:** Cần hiểu cơ chế token của `@primeuix/themes`.

### Phương án B: PrimeVue Unstyled Mode + Tailwind CSS Thuần
- **Mô tả:** Vô hiệu hóa toàn bộ CSS của PrimeVue, tự định nghĩa class Tailwind cho từng component qua Pass Through (PT).
- **Nhược điểm chí tử:** Khối lượng công việc khổng lồ (phải tự viết style cho hơn 30 component: Calendar, Dropdown, Table, Modal). Dễ gây lỗi vỡ giao diện trên các trường hợp biên và không phù hợp với mục tiêu portfolio tinh gọn, không over-engineering.

$\to$ **KẾT LUẬN KIẾN TRÚC:** Lựa chọn **Phương án A (Styled Mode + Tinh chỉnh Aura Preset)** làm kim chỉ nam cho toàn bộ giai đoạn Modernization V2.
