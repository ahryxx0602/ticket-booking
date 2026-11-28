# Frontend Coding & Visual Fidelity Enforcement Guidelines

> **TÀI LIỆU QUY CHUẨN THI CÔNG FRONTEND — VELOBUS FRONTEND V2.1**
> Tất cả các kỹ sư và AI Agents tham gia phát triển giao diện VeloBus **BẮT BUỘC** đọc và tuân thủ tuyệt đối quy định trong văn bản này trước khi viết bất kỳ dòng mã nào.

---

## 1. Nguyên Tắc Cốt Lõi (Core Principles)

> **"Feature views compose components. Feature views do not reinvent components."**
> *(Các màn hình chức năng chỉ lắp ghép components. Tuyệt đối không tự viết lại components.)*

### 4 Phân Lớp Kiến Trúc (4-Layer Hierarchy)
1. **Layer 1 — UI Primitives (`src/components/ui/`):**
   - Chịu trách nhiệm về anatomy chuẩn Shadcn Vue / Reka UI, variants (`class-variance-authority`), semantic tokens, accessibility (`aria-*`), và generic interaction behavior.
   - Các components: `Button`, `Input`, `Textarea`, `Checkbox`, `RadioGroup`, `Switch`, `Select`, `Dialog`, `Sheet`, `Drawer`, `Popover`, `Tooltip`, `Card`, `Table`, `Badge`, `Calendar`, `RangeCalendar`, `Toggle`, `ToggleGroup`, `Accordion`, `Separator`, `Avatar`.
2. **Layer 2 — Shared Composed Components (`src/shared/components/`):**
   - Các tổ hợp chuẩn dùng xuyên suốt hệ thống: `PageHeader`, `FormField`, `StatusBadge`, `EmptyState`, `ThemeCustomizer`, `NotificationCenter`, `FeedbackModal`.
3. **Layer 3 — Business Domain Components (Modules):**
   - Các linh kiện gắn liền đặc thù nghiệp vụ: `TripCard`, `SeatMap2Deck`, `BookingSummary`, `TicketCard`, `RoleMatrix`.
4. **Layer 4 — Feature Views (`src/modules/*/views/`):**
   - **CHỈ ĐƯỢC PHÉP:** Data fetching, State management, Business validation, Event orchestration, Permission checks (RBAC), Page composition.
   - **CẤM TUYỆT ĐỐI:** Tự dựng `<button>`, `<input>`, `<table border>`, `<modal>` thô sơ bên trong view khi đã có component hệ thống.

---

## 2. Quy Tắc Semantic Tokens & Màu Sắc (No Hardcoded Colors)

1. **Tuân thủ bảng màu chuẩn Claude+:**
   - Dùng các semantic classes: `bg-background`, `bg-card`, `bg-muted`, `bg-primary`, `bg-secondary`, `bg-accent`, `bg-destructive`.
   - Dùng các text tokens: `text-foreground`, `text-muted-foreground`, `text-primary`, `text-primary-foreground`, `text-destructive`.
   - Dùng các border tokens: `border-border`, `border-input`, `ring-ring`.
2. **CẤM TUYỆT ĐỐI trong các migrated feature views:**
   - Cấm hardcode các class Tailwind màu cố định như `bg-blue-600`, `bg-orange-500`, `text-slate-800`, `border-gray-200`.
   - Cấm mã HEX (`#142B4A`, `#FFFFFF`), RGB, OKLCH tùy tiện trong style inline hoặc class tùy biến.
   - Cấm lạm dụng bo góc dị biệt `rounded-full` (trừ avatar/badge), `rounded-3xl`, hoặc bóng đổ quá mức `shadow-2xl`.

---

## 3. Quy Chuẩn Hệ Thống Icon (Lucide Icons Only)

1. **Thư viện duy nhất:** `lucide-vue-next`.
2. **CẤM TUYỆT ĐỐI:**
   - Cấm sử dụng emoji (`👥`, `🛡️`, `🎫`, `⚡`, `⚙️`, `🗑️`, `💸`, `🔑`, `⚠️`, `✓`, `✕`, v.v.) làm biểu tượng giao diện.
   - Cấm import PrimeIcons (`pi pi-*`) trong components mới hoặc các view đã migration.
   - Cấm vẽ inline `<svg>` khi Lucide đã có icon tương ứng.
3. **Kích thước hình học tiêu chuẩn:**
   - Badge icon: `w-3 h-3` đến `w-3.5 h-3.5` (12–14px).
   - Button / Input / Table action icon: `w-4 h-4` (16px).
   - Sidebar menu icon: `w-4.5 h-4.5` (18px).
   - Header icon: `w-5 h-5` (20px).
   - Alert / Dialog header icon: `w-5 h-5` (20px).
   - EmptyState icon: `w-8 h-8` (32px).
   - Default color: `currentColor` (thừa hưởng từ text color).

---

## 4. Quy Chuẩn Xử Lý Ngày Tháng (Date/Time & Calendar)

1. **Component chính thức:** Sử dụng `Calendar` và `RangeCalendar` tại `src/components/ui/calendar/` và `src/components/ui/range-calendar/` dựa trên Reka UI và `@internationalized/date`.
2. **Định dạng hiển thị:** Luôn định dạng ngày theo chuẩn Việt Nam `dd/MM/yyyy` và giờ `HH:mm` (24 giờ).
3. **Tuần bắt đầu từ Thứ Hai (Monday-first):** Thiết lập `weekStartsOn: 1`.
4. **Bảo toàn dữ liệu API:** Giữ nguyên chuỗi ISO `YYYY-MM-DD` khi gửi lên Backend; không tự ý convert date-only string sang UTC Date làm nhảy lệch 1 ngày do múi giờ GMT+7.

---

## 5. Quy Trình 8 Bước Khi Viết Hoặc Sửa Giao Diện (AI Execution Protocol)

Trước khi thực hiện bất kỳ thay đổi nào liên quan đến Frontend, AI Agent phải tuần tự:
1. Đọc kỹ file hướng dẫn này (`docs/frontend-v2/FRONTEND_CODING_RULES.md`).
2. Tra cứu Component Catalog và mở `/showcase` trên môi trường preview.
3. Tìm kiếm component đã tồn tại trong `src/components/ui/` hoặc `src/shared/components/`.
4. Nếu chưa có component: Tạo official component tương thích Shadcn Vue/Reka UI tại `src/components/ui/`, thêm ví dụ vào Showcase và kiểm thử trước.
5. Tra cứu icon tương ứng trong Icon Registry chuẩn Lucide.
6. Xác định semantic color tokens và component variants (`primary`, `outline`, `ghost`, v.v.).
7. Soạn thảo mã nguồn và lắp ghép (compose) vào Feature View.
8. Kiểm tra tương thích Light/Dark mode và responsive (Mobile/Tablet/Desktop), chạy test tự động (`npm run test:unit`, `npm run test:e2e`).
