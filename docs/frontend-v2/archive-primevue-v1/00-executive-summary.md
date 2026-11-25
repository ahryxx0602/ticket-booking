# 00 – Báo Cáo Tóm Tắt Điều Hành (Executive Summary)
## VELOBUS FRONTEND V2: DISCOVERY, AUDIT & MODERNIZATION PLANNING

> **Mã kế hoạch:** `FE-V2-PLAN-20261125`  
> **Baseline Commit:** `2659e1c` (nhánh `main`)  
> **Thời điểm khảo sát:** 25/11/2026  
> **Vai trò thực hiện:** Senior Frontend Architect, Vue 3 & TypeScript Specialist, UI/UX Designer, Design System Engineer, Frontend Security Engineer, QA & Accessibility Auditor.  
> **Trạng thái:** `DISCOVERY & PLANNING COMPLETED — AWAITING USER APPROVAL`

---

### 1. Hiện Trạng Tổng Quan & Phát Hiện Cốt Lõi

Khảo sát toàn bộ mã nguồn Frontend tại thư mục `frontend/` (41 Vue Single-File Components, 15 production JavaScript modules, 8 file Unit Tests, 5 file E2E Tests, tổng cộng ~8.778 dòng Vue template/script) cho thấy ứng dụng đang sở hữu nền tảng công nghệ hiện đại (**Vue 3.5.13**, **Vite 6.0.5**, **PrimeVue 4.2.5** với **Aura Preset**, **Tailwind CSS 3.4.17**, **Pinia 2.3.0**, **Vue Router 4.5.0**). Cơ chế bảo mật JWT, RBAC fail-closed, đồng bộ role-preview và bảo toàn dữ liệu backend đạt độ tin cậy cao.

Tuy nhiên, Frontend đang tồn tại **4 nhóm vấn đề kỹ thuật lớn** cản trở chất lượng sản phẩm chuẩn Portfolio chuyên nghiệp:

1. **"God Views" & Vi Phạm Phân Rã Trách Nhiệm Nặng Nề:**
   - [AdminUsersBookingsView.vue](file:///home/vanthanh/Ind-Project/Java/ticket-booking/frontend/src/modules/admin/views/AdminUsersBookingsView.vue) dài tới **1.701 dòng**, nhồi nhét 3 phân hệ CRUD hoàn toàn khác nhau vào tab nội bộ: Quản lý Người dùng (Users), Ma trận Phân quyền Vai trò (Roles 7x26) và Đơn Đặt vé / Hoàn tiền cưỡng chế (Admin Refund).
   - [AdminSettingsView.vue](file:///home/vanthanh/Ind-Project/Java/ticket-booking/frontend/src/modules/admin/views/AdminSettingsView.vue) dài **498 dòng**, gộp chung Bến xe (Stations), Chuyến xe (Trips) và Cấu hình toàn sàn (System Settings).
   - [FleetAndDriversView.vue](file:///home/vanthanh/Ind-Project/Java/ticket-booking/frontend/src/modules/partner/views/FleetAndDriversView.vue) dài **467 dòng**, gộp chung Phương tiện và Tài xế.
   - [SeatSelectionView.vue](file:///home/vanthanh/Ind-Project/Java/ticket-booking/frontend/src/modules/catalog-booking/views/SeatSelectionView.vue) dài **422 dòng**.
   - Các file này vi phạm nghiêm trọng quy chuẩn kỹ thuật `frontend-architect` (giới hạn tối đa 400 dòng/file).

2. **Khủng Hoảng Dữ Liệu Khởi Tạo (In-Memory Hardcoded Mock Arrays):**
   - Mặc dù Backend Spring Boot đã cài đặt đầy đủ 8 Controller quản trị và Client [adminApi](file:///home/vanthanh/Ind-Project/Java/ticket-booking/frontend/src/modules/admin/api.js) đã định nghĩa đủ 100% các API, nhưng hầu hết các View (`AdminUsersBookingsView`, `AdminSettingsView`, `FleetAndDriversView`, `AdminBannersView`, `AdminOperatorsView`, `AdminWithdrawalsView`, `TripListView`, `HomeView`, `MyBookingsView`) lại khởi tạo dữ liệu ban đầu bằng các mảng `ref([...])` hardcoded tĩnh trong component.
   - Chỉ có **4 view** có hook `onMounted` gọi API (`UserProfileView`, `OperatorProfileView`, `SeatSelectionView`, `DashboardLayout`). Khi backend down, giao diện vẫn hiển thị mảng tĩnh giả tạo và nuốt lỗi (`.catch(() => {})`).

3. **Trải Nghiệm Loading & Tương Tác Rời Rạc (Interaction Debt):**
   - Chưa có Global Navigation Progress Bar khi chuyển trang qua Vue Router (người dùng bấm chuyển route không có tín hiệu phản hồi ngay).
   - Thiếu đồng bộ Skeleton Loading: Nhiều bảng dữ liệu DataTable giật layout khi tải; thiếu trạng thái Empty State có hành động điều hướng; thiếu Error State kèm nút Thử lại (Retry Mechanism).
   - Một số chức năng dùng `setTimeout` giả lập để tắt thông báo hoặc giấu trạng thái xử lý thật.

4. **Nguy Cơ Lỗi Runtime Do Thiếu Hệ Thống Kiểu Dữ Liệu (JavaScript Plain):**
   - Toàn bộ codebase frontend là JavaScript thuần (`.js`, `.vue` không TypeScript). DTOs và API Envelopes (`ApiResponse<T>`) từ Backend không có hợp đồng kiểu dữ liệu tĩnh ở client, dễ gây lỗi `Cannot read properties of undefined` khi Backend thay đổi trường dữ liệu.

---

### 2. Định Hướng Hiện Đại Hóa VeloBus Frontend V2

- **Thiết kế & Bản sắc:** **Direction A — Premium Mobility SaaS + Modern Enterprise Dashboard**. Tối ưu hóa cho mật độ dữ liệu cao, bố cục 256px Sidebar gọn gàng, thanh điều hướng Breadcrumb động, phân nhóm theo Business Domain (Operations, Finance, Identity & Access, System) thay vì gom cụm CRUD.
- **Tài nguyên mở (Open-source Free Stack):**
  - Giữ vững kiến trúc **PrimeVue Styled Mode** với **VeloBus Custom Aura Preset** + **Tailwind CSS**.
  - Tái sử dụng tinh hoa cấu trúc Shell, Topbar và Responsive Navigation từ **PrimeVue Sakai** (MIT License, chính chủ PrimeTek) kết hợp các pattern UI hiện đại từ **PrimeVue Volt / PrimeBlocks Free**.
  - Chuẩn hóa toàn bộ icon sang **Lucide Icons** (`lucide-vue-next`), loại bỏ phụ thuộc nặng nề vào font web PrimeIcons.
  - Tích hợp thanh tiến trình điều hướng Router nhẹ, không dependency ngoài.
- **Chiến lược TypeScript Migration:** Di chuyển theo mô hình **Incremental Migration** (Hạ tầng Shared Core $\to$ API Envelopes/DTOs $\to$ Pinia Stores $\to$ Composables $\to$ Layouts & Components $\to$ Views). Kích hoạt `vue-tsc` trong CI gate.

---

### 3. Ma Trận Quyết Định Cần Phê Duyệt Tại Approval Gate

| STT | Vấn đề kỹ thuật | Đề xuất khuyến nghị (Recommended) | Phương án thay thế |
|:---:|:---|:---|:---|
| 1 | **Chế độ PrimeVue** | **Styled Mode** (Aura Preset tinh chỉnh + Tailwind PrimeUI) | Unstyled Mode (Phải viết lại CSS cho toàn bộ PrimeVue components, chi phí x3) |
| 2 | **Tham chiếu Shell** | **Sakai Shell Architecture** (Học hỏi layout, responsive drawer, topbar) | Tự dựng thủ công 100% từ đầu |
| 3 | **Hệ Icon** | **Lucide Icons** (`lucide-vue-next`) 100% cho toàn bộ component | Giữ hỗn hợp Lucide + PrimeIcons |
| 4 | **Phân tách God Views** | **Tách 1 View 1.701 dòng thành 3 Modules độc lập** (`/admin/users`, `/admin/roles`, `/admin/bookings`) | Giữ nguyên 1 View và dùng component con bên trong tab |
| 5 | **Kế hoạch TypeScript** | **Di chuyển theo 4 tầng tăng dần** (Types $\to$ Core/Stores $\to$ Shared UI $\to$ Views) | Giữ nguyên JavaScript thuần |
| 6 | **Dữ liệu Mock** | **Triệt tiêu mảng tĩnh hardcoded**, nạp 100% qua API thật Backend đã có, xử lý Skeleton + Error/Retry | Giữ mảng tĩnh làm fallback |
| 7 | **Loading System** | **Event-driven Navigation Bar + Component Skeletons + Button Spinners** | Giữ trạng thái rời rạc hiện tại |

---

### 4. Kết Luận Khảo Sát

Toàn bộ 16 tài liệu khảo sát, phân tích khoảng cách, nghiên cứu tài nguyên và kế hoạch tổng thể đã sẵn sàng tại [docs/frontend-v2/](file:///home/vanthanh/Ind-Project/Java/ticket-booking/docs/frontend-v2/). Quá trình khảo sát không chỉnh sửa bất kỳ dòng mã nguồn ứng dụng nào. Dự án dừng lại ở **Approval Gate** chờ phê duyệt chính thức từ người dùng.
