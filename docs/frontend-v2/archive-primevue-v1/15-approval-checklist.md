# 15 – Danh Mục Quyết Định Cần Phê Duyệt (Approval Checklist)

> **Mã tài liệu:** `FE-V2-APP-15`  
> **Thời điểm lập:** 25/11/2026  
> **Trạng thái cổng kiểm soát:** **APPROVAL GATE #1 — CHỜ PHÊ DUYỆT TỪ NGƯỜI DÙNG**

---

Kính gửi Quý người dùng (Tech Lead / Product Owner),

Giai đoạn **Discovery, Audit & Planning** của dự án Hiện đại hóa Frontend VeloBus V2 đã hoàn tất 100% trên cơ sở khảo sát thực tế tại commit `2659e1c`.

Để đội ngũ kỹ thuật có thể bắt tay vào thực hiện mã nguồn từ **Phase 1**, vui lòng xem xét và cho ý kiến phê duyệt đối với **11 quyết định kỹ thuật then chốt** dưới đây:

---

### Bảng Danh Mục Quyết Định Kỹ Thuật (Decision Checklist)

| STT | Vấn đề kỹ thuật | Phương án đề xuất (Recommended) | Phương án thay thế | Lựa chọn của bạn |
|:---:|:---|:---|:---|:---:|
| **1** | **Chế độ PrimeVue Theme Engine** | **PrimeVue Styled Mode** với **VeloBus Custom Aura Preset** + **Tailwind PrimeUI** (Giữ vững ổn định, hiệu năng cao, chi phí bảo trì thấp). | Unstyled Mode (Phải tự viết CSS lại cho hơn 30 component, chi phí rất cao). | [ ] Đồng ý đề xuất<br>[ ] Chọn phương án khác |
| **2** | **Tham chiếu Kiến trúc Application Shell** | **Học hỏi kiến trúc Shell từ PrimeVue Sakai** (Sidebar 256px/76px collapse, Topbar Breadcrumb, responsive Drawer mobile). | Tự thiết kế thủ công từ đầu. | [ ] Đồng ý đề xuất<br>[ ] Chọn phương án khác |
| **3** | **Tham khảo Component Patterns** | **Tham khảo patterns từ PrimeVue Volt & PrimeBlocks Free** (TableToolbar, Filter Panel, KPI Stat Cards). | Tự sáng tạo không tham khảo. | [ ] Đồng ý đề xuất<br>[ ] Chọn phương án khác |
| **4** | **Phiên bản Tailwind CSS** | **Giữ nguyên Tailwind CSS v3.4.17** (Tương thích tuyệt đối với `tailwindcss-primeui` v0.6 và PostCSS 8 hiện tại). | Nâng lên Tailwind v4 (Chưa ổn định với hệ sinh thái PrimeUI). | [ ] Đồng ý đề xuất<br>[ ] Chọn phương án khác |
| **5** | **Phạm vi TypeScript Migration** | **Di chuyển toàn diện theo 4 tầng tăng dần** (Types $\to$ Shared Core $\to$ Stores $\to$ Components & Views mới), tích hợp `vue-tsc` vào CI. | Chỉ viết TypeScript cho code mới, giữ JS cũ. | [ ] Đồng ý đề xuất<br>[ ] Chọn phương án khác |
| **6** | **Tái cấu trúc Sidebar Quản trị (Admin IA)** | **Phân nhóm thành 6 Business Domains**: Overview, Operations & Catalog, Partners & Escrow, Identity & Access, Marketing, System. | Giữ nguyên menu cũ 6 mục gộp. | [ ] Đồng ý đề xuất<br>[ ] Chọn phương án khác |
| **7** | **Phân tách các God Views (> 400 dòng)** | **Bóc tách triệt để**: `AdminUsersBookingsView` (1.701 dòng) $\to$ 3 Views; `AdminSettingsView` (498 dòng) $\to$ 3 Views; `FleetAndDriversView` (467 dòng) $\to$ 2 Views. | Giữ nguyên 1 file và chia tab con bên trong. | [ ] Đồng ý đề xuất<br>[ ] Chọn phương án khác |
| **8** | **Chuẩn hóa trang CRUD (CRUD Standard)** | **Áp dụng bố cục chuẩn `CrudPageLayout.vue`**: PageHeader, Summary KPIs, TableToolbar (Debounced 300ms), AppDataTable (Skeleton/Pagination), Right-Side Drawer (520px/680px). | Mỗi màn hình tự tổ chức tự do. | [ ] Đồng ý đề xuất<br>[ ] Chọn phương án khác |
| **9** | **Xóa bỏ Mock Data & Chuẩn hóa Loading** | **Loại bỏ 100% mảng tĩnh hardcoded**, nạp dữ liệu từ Backend trên `onMounted` kèm Skeleton Loading, Error State có nút Thử lại, xóa `setTimeout`. | Giữ mảng tĩnh làm dữ liệu dự phòng. | [ ] Đồng ý đề xuất<br>[ ] Chọn phương án khác |
| **10**| **Chuẩn hóa Icon System** | **Chuyển đổi 100% sang Lucide Icons** (`lucide-vue-next`), gỡ bỏ `primeicons` để giảm ngay ~500KB bundle font assets. | Tiếp tục dùng song song 2 bộ icon. | [ ] Đồng ý đề xuất<br>[ ] Chọn phương án khác |
| **11**| **Phạm vi Hỗ trợ Dark Mode** | **Ưu tiên 100% hoàn thiện Light Theme cao cấp** ở V2; cấu hình sẵn biến CSS để sẵn sàng bật Dark Mode ở giai đoạn kế tiếp. | Triển khai song song cả Light và Dark Mode ngay. | [ ] Đồng ý đề xuất<br>[ ] Chọn phương án khác |

---

### Hướng Dẫn Phê Duyệt:
Bạn có thể phản hồi chấp thuận toàn bộ đề xuất bằng cú pháp:
> **"Phê duyệt toàn bộ kế hoạch Frontend V2 theo đề xuất, tiến hành triển khai."**

Hoặc đưa ra chỉ đạo điều chỉnh riêng cho từng mục nếu cần thay đổi!
