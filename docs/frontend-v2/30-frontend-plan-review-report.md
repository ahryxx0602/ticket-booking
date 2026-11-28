# BÁO CÁO THẨM ĐỊNH & HIỆU CHỈNH KẾ HOẠCH FRONTEND V2.1 (PLAN REVIEW & CORRECTION REPORT)
**Mã tài liệu:** `DOC-FE-V2-30`  
**Phiên bản:** `2.1.1` (Updated post-review) | **Trạng thái:** `PENDING USER FINAL APPROVAL`  
**Phạm vi:** Tổng hợp toàn bộ 15 Findings ban đầu và 8 Hiệu chỉnh bắt buộc (FIX-01 đến FIX-08), đối chiếu bằng chứng mã nguồn, công thức đo tương phản toán học WCAG 2.1 AA, xác định tài liệu bị ảnh hưởng và kết quả kiểm chứng chéo.  
**Tài liệu liên kết:** [25-claude-plus-design-system-spec.md](25-claude-plus-design-system-spec.md), [27-frontend-implementation-master-plan.md](27-frontend-implementation-master-plan.md), [28-frontend-traceability-matrix.md](28-frontend-traceability-matrix.md), [34-frontend-quality-gates-and-acceptance.md](34-frontend-quality-gates-and-acceptance.md)  

---

## 1. BẢNG THEO DÕI CÁC HIỆU CHỈNH BẮT BUỘC (MANDATORY CORRECTIONS LOG — FIX-01 ĐẾN FIX-08)

| Mã Hiệu chỉnh | Hạng mục Bắt buộc | Vấn đề Phát hiện & Dẫn chứng Toán học/Mã nguồn | Giải pháp Hiệu chỉnh Triệt để | Tài liệu Cập nhật | Trạng thái |
| :---: | :--- | :--- | :--- | :--- | :---: |
| **FIX-01** | **Chuẩn hóa Tương phản WCAG AA** | Đánh đồng `font-semibold 14px` là Large text là sai quy chuẩn W3C (Large text đòi hỏi >= 24px regular hoặc 18.67px bold). Đo đạc toán học:<br>- Light Primary `#C96442` + White: `3.90:1` (FAIL AA).<br>- Dark Destructive `#EF4444` + White: `3.76:1` (FAIL AA).<br>- Extended Danger `#DE3D34` + White: `4.35:1` (FAIL AA). | - Light Primary dùng Dark FG `#141413` đạt **`4.72:1` (PASS AA)**; hoặc dùng Accessible Terracotta `#BC5937` (`oklch(0.58 0.1375 39.0427)`) với White đạt **`4.54:1` (PASS AA)**.<br>- Dark Destructive dùng Dark FG `#141413` đạt **`4.89:1` (PASS AA)**.<br>- Extended Danger chỉnh L=0.585 (`#D9372F`) đạt **`4.63:1` (PASS AA)**. | `25-spec`, `29-adr`, `31-ref-spec`, `32-catalog`, `34-gates` | **RESOLVED** |
| **FIX-02** | **Sửa Lỗ hổng DAG & Thứ tự Phê duyệt Cổng C** | Master Plan cũ đặt Gate C sau khi Phase 4-7 đã migrate toàn bộ modules, và Gate A đòi hỏi Customizer nhưng Customizer nằm sau Gate C. | Sắp xếp lại DAG chuẩn mực:<br>1. Baseline -> 2. Theme -> 3. Primitives & Feedback -> 4. Customizer -> 5. Showcase -> **GATE A** -> 6. App Shells -> **GATE B** -> 7. Representative Screens -> **GATE C (User Visual Approval BẮT BUỘC)** -> 8. Bulk Migration -> 9. Hardening -> 10. Gỡ PrimeVue -> **GATE D**. | `27-plan`, `34-gates` | **RESOLVED** |
| **FIX-03** | **Hoàn thiện Theme Customizer Runtime** | Thiếu FOUC prevention, cross-tab sync, reset logic, system mode listener. | Sử dụng storage key `velobus_theme_config_v2`, script inline trong `<head>` chống FOUC, `matchMedia` listener, tách biệt Draft và Committed state, khôi phục Claude+ Effective Defaults. | `25-spec`, `27-plan`, `32-catalog` | **RESOLVED** |
| **FIX-04** | **Khôi phục Showcase Hub làm Playground Nghiệm thu** | Showcase đã bị dọn dẹp cần được phục hồi làm Design System Playground chính thức phục vụ Cổng A. | Khôi phục route `/showcase` với đầy đủ 21 nhóm thành phần: Palette, Typography, Button variants, Form controls, DataTables, Cards, Badges, Overlays, Feedback playground, Theme Customizer live. Mock data biệt lập. | `27-plan`, `28-matrix`, `34-gates` | **RESOLVED** |
| **FIX-05** | **Hoàn thiện Action-Level Traceability Matrix** | Số lượng actions ước tính không đồng bộ (Doc 30 ghi 35+, Doc 28 chỉ có 20). | Mở rộng Doc 28 lên hơn 36 actions cụ thể, đối chiếu 1:1 với Backend Controllers, HTTP methods, full API paths, DTOs, Permissions và Test case IDs. | `28-matrix` | **RESOLVED** |
| **FIX-06** | **Hoàn thiện Danh mục Linh kiện & Chính sách Phản hồi** | Doc 32 còn sơ sài ở một số thành phần nền tảng. | Bổ sung đầy đủ chi tiết variants, sizes, states, accessibility, testing requirements cho 27+ component nền tảng. Bỏ giả định idempotency backend, cấm optimistic success cho tài chính. | `32-catalog` | **RESOLVED** |
| **FIX-07** | **Xác minh Tương thích Dependencies** | Cần đảm bảo `reka-ui`, `vue-sonner`, `tailwind-merge` hoạt động trơn tru với Tailwind v3.4.17 mà không nâng cấp v4. | Xác minh phiên bản: `reka-ui: ^1.0.0`, `tailwind-merge: ^2.5.0`, `clsx: ^2.1.0`, `class-variance-authority: ^0.7.0`, `@tanstack/vue-table: ^8.20.0`, `vue-sonner: ^1.3.0`. Quản lý qua `npm`. | `26-analysis`, `27-plan` | **RESOLVED** |
| **FIX-08** | **Chuẩn hóa Trích dẫn, Giấy phép & Minh bạch Dữ liệu** | Dùng absolute links, tự đánh dấu APPROVED, chưa rõ giấy phép theme. | Chuyển 100% sang relative Markdown links; đổi trạng thái thành `PENDING USER FINAL APPROVAL`; ghi nhận giấy phép Claude+ là `UNVERIFIED Community Theme`; bỏ các thông số mock/giả định. | Toàn bộ 11 tài liệu | **RESOLVED** |

---

## 2. KẾT QUẢ ĐO ĐẠC WCAG 2.1 CONTRAST AUDIT CHÍNH XÁC (TÁI LẬP)

```
================================================================================
WCAG 2.1 AA Relative Luminance & Contrast Ratio Audit:
- Normal Text Threshold: >= 4.5:1
- Large Text Threshold (>= 24px regular hoặc >= 18.67px bold): >= 3.0:1
================================================================================
1. Canonical Light Primary oklch(0.6171 0.1375 39.0427) -> #C96442 (Luminance: 0.2192)
   - With White (#FFFFFF, Lum 1.0000)     : 3.90:1 [FAIL Normal Text]
   - With Dark FG (#141413, Lum 0.0070)   : 4.72:1 [PASS WCAG AA Normal Text]
   - Accessible Primary Variant (L=0.58)   : #BC5937 + White = 4.54:1 [PASS WCAG AA]

2. Canonical Dark Destructive oklch(0.6368 0.2078 25.3313) -> #EF4444 (Luminance: 0.2290)
   - With White (#FFFFFF, Lum 1.0000)     : 3.76:1 [FAIL Normal Text]
   - With Dark FG (#141413, Lum 0.0070)   : 4.89:1 [PASS WCAG AA Normal Text]

3. Extended Danger Token oklch(0.60 0.20 28.0) -> #DE3D34 (Luminance: 0.1915)
   - With White (#FFFFFF, Lum 1.0000)     : 4.35:1 [FAIL Normal Text]
   - Adjusted Danger Token (L=0.585)      : #D9372F + White = 4.63:1 [PASS WCAG AA]
================================================================================
```
