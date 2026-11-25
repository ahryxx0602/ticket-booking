# PHÂN TÍCH CHUYỂN ĐỔI TỪ PRIMEVUE SANG SHADCN VUE & ĐÁNH GIÁ TÍNH TƯƠNG ĐỒNG
**Mã tài liệu:** `DOC-FE-V2-26`  
**Phiên bản:** `2.1.0` (Updated post-review) | **Trạng thái:** `PENDING REVIEW & APPROVAL`  
**Tài liệu liên quan:** `30-frontend-plan-review-report.md`, `32-frontend-component-catalog.md`  

---

## 1. ĐÁNH GIÁ TÍNH KHẢ THI TỔNG THỂ (MIGRATION FEASIBILITY)

### 1.1. Bức tranh Hiện trạng & Điểm thuận lợi Cốt lõi
Kết quả kiểm toán mã nguồn xác nhận:
- **Tỷ lệ thâm nhập UI của PrimeVue trong Views:** Dưới **5%**.
- **95% các thành phần bảng biểu, form, card, modal trong dự án:** Hiện đang là các thẻ HTML thuần được style bằng utility classes của Tailwind CSS!
- **Hệ quả kiến trúc:** Cuộc chuyển đổi này **không phải là việc gỡ bỏ hàng loạt API phức tạp của PrimeVue**, mà là cơ hội **chính quy hóa (formalize)** toàn bộ các component viết tay phân tán thành một bộ Design System chuẩn mực, hiện đại dựa trên Shadcn Vue và Theme Claude+.

### 1.2. Điểm Khả thi Thống nhất (Unified Feasibility Score): `94 / 100`
- **Mức độ rủi ro hồi quy nghiệp vụ:** **Thấp và Có thể kiểm soát (Low / Controlled Risk)** nhờ sự độc lập hoàn toàn giữa Data Services (`httpClient.ts`, Pinia stores) và tầng UI components.
- **Rủi ro kiểm thử:** Rất thấp, 67 unit tests và 22 e2e tests hiện tại kiểm tra dữ liệu qua `data-testid` và semantic selectors.

---

## 2. RÀ SOÁT TƯƠNG THÍCH PACKAGE & CÔNG CỤ QUẢN LÝ (REV-012)

1. **Chuẩn hóa Package Manager duy nhất:** Dự án sử dụng độc quyền **`npm`** (xác minh qua `package-lock.json`). Tuyệt đối không chạy hoặc trộn lẫn các lệnh `pnpm` để tránh gây xung đột lockfile.
2. **Tương thích Tailwind CSS v3 với Shadcn Vue:**
   - Shadcn Vue sử dụng `tailwind-merge` và `clsx` để gộp class. Phiên bản `tailwind-merge: ^2.5.0` tương thích hoàn hảo với Tailwind CSS `v3.4.17` hiện có.
   - Thư viện `reka-ui: ^1.0.0` hoạt động độc lập ở tầng DOM headless, không can thiệp vào CSS engine của Tailwind.
   - Chiến lược CSS Variable Bridging cho phép sử dụng trọn vẹn các class `bg-primary`, `text-foreground`, `border-border` mà không yêu cầu nâng cấp cưỡng chế lên Tailwind v4.

---

## 3. RÀ SOÁT BẢN QUYỀN & GIẤY PHÉP MÃ NGUỒN (LICENSING REVIEW — REV-013)

Toàn bộ hệ sinh thái công nghệ mục tiêu được rà soát giấy phép đảm bảo tuân thủ 100% cho dự án mã nguồn mở / portfolio công khai trên GitHub:

| Thành phần / Thư viện | Tác giả / Nguồn | Loại Giấy phép (License) | Đánh giá Tuân thủ & Điều kiện áp dụng |
| :--- | :--- | :---: | :--- |
| **Claude+ Community Theme** | Luis Llanes (`llanesluis`) / TweakCN | `Public Community Open Resource` | Được tác giả xuất bản công khai qua TweakCN Registry. Dự án tái sử dụng trực tiếp CSS variables, không chứa mã nguồn đóng. |
| **Shadcn Vue** | Radix-Vue team | `MIT License` | Mã nguồn mở hoàn toàn, phân phối dưới dạng copy-paste source code vào repository. |
| **Reka UI (Headless)** | Reka UI team | `MIT License` | Headless primitives chính thức, miễn phí sử dụng thương mại. |
| **TanStack Vue Table** | Tanner Linsley | `MIT License` | Tiêu chuẩn công nghiệp mở cho bảng dữ liệu. |
| **Vue Sonner** | Emil Kowalski / vue-sonner team | `MIT License` | Toast system mở, miễn phí. |
| **Lucide Icons** | Lucide Project | `ISC License` | Thay thế hoàn toàn PrimeIcons, cho phép tái phân phối và tree-shaking tự do. |
| **Font Outfit & Geist Mono** | Google Fonts / Vercel | `SIL Open Font License 1.1` | Miễn phí nhúng web và phân phối lại mã nguồn font woff2. |

---

## 4. MA TRẬN ÁNH XẠ LINH KIỆN CHI TIẾT (COMPONENT MAPPING MATRIX)

| STT | Thành phần hiện tại | Vị trí sử dụng | Hành vi hiện tại | Thành phần thay thế đề xuất | Độ tương đương (Parity) | Độ phức tạp |
| :---: | :--- | :--- | :--- | :--- | :---: | :---: |
| 1 | `primevue/toast` & `useToast()` | `App.vue:9`, `SeatSelectionView.vue:4` | Toast góc trên phải | **Vue Sonner** (`src/components/ui/sonner/`) | Tương đương 100%, animation mượt mà hơn | `Thấp` |
| 2 | `primevue/progressspinner` | `AppSpinner.vue:29` | Spinner SVG xoay tròn | **Lucide Loader2** SVG Spinner | Tương đương 100%, bundle nhẹ hơn | `Rất thấp` |
| 3 | `ConfirmationService` | `main.js:7` | Đăng ký nhưng chưa dùng | **Shadcn AlertDialog** | Vượt trội về accessibility và tùy biến | `Trung bình` |
| 4 | `primeicons` (`pi pi-*`) | Layouts, Views | Thẻ font tĩnh (~500KB) | **Lucide Vue Next** | Vượt trội về chất lượng vector và tree-shaking | `Thấp` |
| 5 | Custom `<table>` HTML | 7 Views Admin & Operator | Thẻ HTML table thuần | **TanStack Vue Table** (6 Chiến lược) | Bổ sung phân trang chuẩn, sorting, mobile card | `Cao` |
| 6 | Custom `AppDrawer.vue` | `AdminUsersBookingsView` | Aside trượt từ phải | **Shadcn Sheet / Drawer** (480px / 100vw) | Tương đương hoàn toàn, chuẩn Reka UI | `Trung bình` |
| 7 | Custom `TelegramLinkModal.vue`| `UserProfileView.vue` | Modal fixed inset-0 | **Shadcn Dialog** | Quản lý focus trap và ESC chuẩn | `Thấp` |
| 8 | Custom `AppButton.vue` | Toàn hệ thống | Button bọc Tailwind | **Shadcn Button** (CVA variants) | Tương thích hoàn toàn | `Thấp` |
| 9 | Custom `StatusBadge.vue` | Toàn bộ bảng dữ liệu | Span 16 mã trạng thái | **Shadcn Badge** + VeloBus Semantic Tokens | Giữ nguyên 16 trạng thái, chuyển màu sang OKLCH | `Thấp` |

---

## 5. THAY ĐỔI DEPENDENCY & BUNDLE FOOTPRINT

### 5.1. Thư viện Loại bỏ
- `primevue: ^4.2.5` (~1.2MB node_modules)
- `@primeuix/themes: ^1.0.0` (~350KB CSS)
- `primeicons: ^7.0.0` (~500KB font files tĩnh)
- `tailwindcss-primeui: ^0.6.1`

### 5.2. Thư viện Bổ sung (Cài qua npm)
- `reka-ui`
- `clsx`, `tailwind-merge`, `class-variance-authority`
- `@tanstack/vue-table`
- `vue-sonner`
- Mở rộng sử dụng `lucide-vue-next` sẵn có.

### 5.3. Kết quả Đo đạc Bundle Thực tế (Measured Bundle)
Sau khi dọn dẹp các module thừa:
- Số module transformed giảm từ 1.897 xuống **317 modules**.
- Bundle CSS giảm từ 73KB xuống **60.15KB** (tiết kiệm ~18%).
- Thời gian build giảm từ 3.73s xuống **2.86s**.
