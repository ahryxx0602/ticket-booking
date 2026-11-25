# ĐẶC TẢ THAM CHIẾU THIẾT KẾ & QUY CHUẨN KÍCH THƯỚC CHÍNH XÁC (VISUAL REFERENCE & EXACT METRICS SPECIFICATION)
**Mã tài liệu:** `DOC-FE-V2-31`  
**Phiên bản:** `2.1.0` | **Trạng thái:** `APPROVED AS SPECIFICATION`  
**Định hướng thị giác:** `Warm Minimalist Enterprise Mobility SaaS`  

---

## 1. DANH MỤC NGUỒN THAM CHIẾU THIẾT KẾ CHÍNH THỨC (DESIGN REFERENCE REGISTRY)

Quy định thứ tự ưu tiên và phạm vi áp dụng cho 6 nguồn tham chiếu chính thức:

| Mã Tham chiếu (Ref ID) | Tên Hệ thống & URL | Mục đích & Phạm vi Tham chiếu | Thành phần Được phép Kế thừa (Allowed) | Thành phần Nghiêm cấm Sao chép (Forbidden) | Màn hình VeloBus áp dụng |
| :---: | :--- | :--- | :--- | :--- | :--- |
| **REF-01** | **Claude+ Theme**<br>`https://tweakcn.com/themes/cmdght103000n04lh3e2ae93r`<br>`https://tweakcn.com/r/themes/cmdght103000n04lh3e2ae93r` | **Visual Identity & Tokens**<br>Nguồn duy nhất quyết định màu sắc, font, radius, shadow. | Bảng màu OKLCH (Terracotta `#C96442`, Warm Slate, Off-White), font Outfit & Geist Mono, Base radius 1rem (16px), Shadow tokens. | Không tự chuyển sang HEX/HSL làm lệch gam màu gốc; không áp dụng các theme khác pha trộn. | Toàn bộ 20 màn hình, tất cả layouts, tất cả UI components. |
| **REF-02** | **Shadcn Vue Official**<br>`https://www.shadcn-vue.com/`<br>`https://www.shadcn-vue.com/docs/theming` | **Component System & Headless Primitives**<br>Quy chuẩn cấu trúc code component. | Kiến trúc copy-paste source component (`src/components/ui/`), headless state trên Reka UI, utility `cn()`, accessibility keyboard navigation. | Không copy code Vue 2 hoặc Radix React; không import component đóng gói dạng monolithic runtime. | Toàn bộ shared primitives tại `src/components/ui/`. |
| **REF-03** | **Shadcn Vue Admin**<br>`https://shadcn-vue-admin.netlify.app/`<br>`https://github.com/Whbbit1999/shadcn-vue-admin` | **Admin & Operator Application Shell**<br>Bố cục điều hướng không gian làm việc. | Cấu trúc Sidebar 2 cấp phân nhóm Business Domains, 64px collapsed icon bar, header breadcrumbs, user profile dropdown, data table toolbar. | Không sao chép logo, brand color xanh tím của template; không dùng mock analytics giả lập. | `AdminLayout.vue`, `OperatorLayout.vue`, `DashboardLayout.vue`. |
| **REF-04** | **Shadcn Blocks**<br>`https://www.shadcn-vue.com/blocks` | **Authentication & Dashboard Grid**<br>Bố cục khối trang tiêu chuẩn. | Cấu trúc form đăng nhập/đăng ký tập trung sạch sẽ, grid 4 thẻ KPI đồng dạng, responsive form drawer 480px. | Không sao chép hình ảnh minh họa stock, banner quảng cáo hoặc nội dung text mẫu tiếng Anh. | `LoginView.vue`, `UserProfileView.vue`, `AdminDashboardView.vue`. |
| **REF-05** | **Vexere UX Patterns**<br>`https://vexere.com/` | **Customer Search & Booking Flow**<br>Trải nghiệm tra cứu và đặt vé xe khách VN. | Bố cục thanh tìm kiếm trung tâm (Điểm đi, nút đảo chiều 1-click, Điểm đến, Chọn ngày), bộ lọc chuyến đi bên trái, cấu trúc hiển thị giờ đi/đến timeline. | Nghiêm cấm sao chép logo, màu cam/xanh nhận diện Vexere, hình ảnh xe, biểu tượng linh vật hoặc nội dung bản quyền. | `HomeView.vue`, `TripListView.vue`, `SeatSelectionView.vue`. |
| **REF-06** | **FlixBus UX Patterns**<br>`https://www.flixbus.com/` | **International Mobility & Ticket Management**<br>Trải nghiệm quản lý vé & checkout. | Bố cục tóm tắt hành trình (Trip Summary Sticky Sidebar), vé điện tử QR Code rõ nét, sơ đồ ghế tối giản phân biệt rõ cabin tài xế. | Nghiêm cấm sao chép màu xanh nõn chuối FlixBus, logo, typography hoặc các chính sách vận tải đặc thù Châu Âu. | `CheckoutView.vue`, `MyBookingsView.vue`, `SeatSelectionView.vue`. |

---

## 2. QUY CHUẨN THÔNG SỐ HÌNH HỌC CHÍNH XÁC (EXACT METRICS SPECIFICATION)

Tất cả AI agents khi triển khai code màn hình và component bắt buộc phải tuân thủ chính xác các hằng số hình học dưới đây, không được tự ý biến tấu:

### 2.1. Không gian Khung Giao diện (Layout Shell Metrics)
- **Sidebar Width (Mở rộng):** Cố định `256px` (`w-64`), viền phải `1px solid var(--border)`.
- **Sidebar Width (Thu gọn):** Cố định `64px` (`w-16`), chỉ hiển thị icon căn giữa kèm tooltip.
- **Header Height:** Cố định `64px` (`h-16`), sticky `top-0 z-30`, viền dưới `1px solid var(--border)`, nền `var(--card)`.
- **Page Container Widths (Chế độ độ rộng nội dung):**
  - Narrow Mode: `max-w-[1024px]` (Căn giữa)
  - Default Mode: `max-w-[1280px]` (Căn giữa)
  - Wide Mode: `max-w-[1536px]` (Căn giữa)
  - Full Width: `w-full px-6` (Tràn màn hình cho bảng dữ liệu lớn)
- **Khoảng cách đệm trang (Page Padding):**
  - Desktop (`>= 1024px`): `24px` (`p-6`)
  - Tablet (`768px - 1023px`): `20px` (`p-5`)
  - Mobile (`< 768px`): `16px` (`p-4`)

### 2.2. Thang Typography & Phân cấp Nhãn (Typography Scale)
- **Display / Page Title:** `24px` (`text-2xl`), `font-semibold` (weight 600), `leading-8`, `tracking-tight`.
- **Section Heading / Dialog Title:** `18px` (`text-lg`), `font-semibold` (weight 600), `leading-6`.
- **Card Title / Sub-section:** `16px` (`text-base`), `font-semibold` (weight 600), `leading-snug`.
- **Body Text (Chuẩn):** `14px` (`text-sm`), `font-normal` (weight 400), `leading-relaxed` (20px).
- **Table Column Header:** `13px` (`text-xs`), `font-semibold` (weight 600), uppercase, `tracking-wider`, `text-muted-foreground`.
- **Table Data Cell:** `14px` (`text-sm`), `font-normal` (weight 400), `tabular-nums` cho tiền tệ và ngày tháng.
- **Caption / Meta / Badge Text:** `12px` (`text-xs`), `font-medium` (weight 500), `leading-none`.
- **Micro Code / PNR / Biển số:** `13px font-mono` (`text-xs font-mono tracking-wide`).

### 2.3. Quy chuẩn Kích thước Thành phần (Component Sizing & Geometry)
- **Chiều cao Hàng Bảng (Table Row Heights):**
  - Compact Row (Dành cho bảng dày đặc như Role Matrix, đối soát): Cố định `44px` (`h-11`).
  - Default Row (Dành cho bảng chuẩn như Users, Operators, Trips): Cố định `52px` (`h-[52px]`).
- **Chiều cao Input & Controls:**
  - Standard Input: Chiều cao `40px` (`h-10`), `px-3 text-sm`, viền `1px solid var(--input)`, bo góc `var(--radius)`.
  - Small Input / Filter Select: Chiều cao `36px` (`h-9`), `px-2.5 text-xs`.
- **Kích thước Button:**
  - Size `sm`: Chiều cao `36px` (`h-9`), `px-3 text-xs font-medium`.
  - Size `default`: Chiều cao `40px` (`h-10`), `px-4 text-sm font-medium`.
  - Size `lg`: Chiều cao `44px` (`h-11`), `px-6 text-base font-semibold`.
  - Size `icon`: Cố định `40×40px` (`w-10 h-10 flex items-center justify-center`).
- **Mobile Touch Target:** Tối thiểu tuyệt đối `44×44px` cho tất cả các nút bấm, icon thao tác và ô chọn ghế trên thiết bị cảm ứng.
- **Form Layout Spacing:** Khoảng cách giữa các trường trong form là `16px` (`space-y-4` hoặc `gap-4`). Form 2 cột trên Desktop/Tablet, 1 cột trên Mobile.
- **Kích thước Overlays (Dialog & Sheet):**
  - Modal Small: `max-w-[400px]` (Xác nhận, Alert)
  - Modal Default: `max-w-[540px]` (Form thông thường, đổi mật khẩu)
  - Modal Large: `max-w-[720px]` (Sơ đồ ghế, chi tiết đối soát)
  - Form Sheet (Drawer trượt từ phải): Cố định `480px` trên Desktop (`>= 640px`), `100vw` bung toàn màn hình trên Mobile (`< 640px`).
- **Card Geometry:** Padding `20px` - `24px` (`p-5` hoặc `p-6`), viền `1px solid var(--border)`, bo góc `var(--radius)` (16px), nền `var(--card)`.
- **Motion & Micro-interactions:** Thời lượng chuyển cảnh `150ms - 200ms ease-out` cho hover/active states, `200ms ease-in-out` cho drawer/dialog transitions.
- **Thang Z-Index Toàn cục:**
  - `z-10`: Elements nổi cục bộ (badge, tooltip layer)
  - `z-20`: Sticky sub-headers / table sticky columns
  - `z-30`: Application Header chính (Sticky 64px)
  - `z-40`: Role Preview Active Banner / Thông báo khẩn cấp
  - `z-50`: Dropdown Menus, Popover, Combobox list
  - `z-60`: Backdrop che phủ mờ (Dialog/Sheet overlay)
  - `z-70`: Modal Dialog / Slide-over Sheet container
  - `z-100`: Sonner Toast Notification Center (Cao nhất toàn màn hình)

---

## 3. QUY TẮC BẢO VỆ CHẤT LƯỢNG THIẾT KẾ (ANTI-AI DESIGN RULES)

Để ngăn chặn giao diện bị biến chất thành sản phẩm demo hời hợt hoặc AI template giả lập:

1. **Tuyệt đối KHÔNG sử dụng Glassmorphism:** Nghiêm cấm các thuộc tính `backdrop-blur-md` kết hợp nền trong suốt lòe loẹt trên cards và containers nội dung. Chỉ cho phép duy nhất backdrop mờ tối giản (`bg-black/40 backdrop-blur-xs`) làm nền che phủ phía sau modal dialog.
2. **Tuyệt đối KHÔNG dùng Gradient trang trí:** Không dùng `bg-gradient-to-r` đa sắc trên nút bấm, tiêu đề hoặc thẻ thống kê. Tất cả diện tích bề mặt phải sử dụng màu khối phẳng (solid semantic color) theo Claude+ tokens.
3. **Tuyệt đối KHÔNG dùng Glow hoặc Shadow phát sáng:** Nghiêm cấm các hiệu ứng `shadow-[0_0_20px_#...]` hoặc màu neon. Toàn bộ bóng đổ phải tuân thủ chuẩn shadow vật lý tinh gọn (`var(--shadow-sm)`).
4. **Tuyệt đối KHÔNG tự sáng tác KPI giả:** Mọi số liệu trên Admin Dashboard và Operator Dashboard phải phản ánh đúng dữ liệu từ API Backend (`revenue-reconciliation`, `wallet`). Không thêm các chỉ số vô căn cứ như "Tỷ lệ tăng trưởng AI +35%", "Độ hài lòng 99.8%".
5. **Cấm hỗn tạp Icon Libraries:** Chỉ sử dụng duy nhất một thư viện icon là **Lucide Icons** (`lucide-vue-next`). Loại bỏ 100% các thẻ `pi pi-*` của PrimeIcons và SVG tự vẽ không theo quy chuẩn kích thước `w-4 h-4` (16px) hoặc `w-5 h-5` (20px).
6. **Cấm bo góc tùy tiện (No Pill-Shape Chaos):** Trừ các status badges dạng con nhộng (`rounded-full`), toàn bộ input, button, card, modal phải sử dụng giá trị bo góc thống nhất theo `var(--radius)` (`1rem` / 16px).
