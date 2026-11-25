# 13 – Báo Cáo Thẩm Định Kế Hoạch Độc Lập (Plan Review)

> **Mã tài liệu:** `FE-V2-REV-13`  
> **Thời điểm thẩm định:** 25/11/2026  
> **Vai trò:** Independent Plan Reviewer & Adversarial Red-Team Auditor.  
> **Đối tượng thẩm định:** Tài liệu Master Plan [12-modernization-master-plan.md](file:///home/vanthanh/Ind-Project/Java/ticket-booking/docs/frontend-v2/12-modernization-master-plan.md).

---

## 1. Phương Pháp & Tiêu Chí Thẩm Định

Thẩm định dựa trên 6 góc độ khắt khe:
1. **Tính hoàn chỉnh & Độ bao phủ nghiệp vụ:** Có bỏ sót màn hình, API hoặc quyền RBAC nào không?
2. **Nguy cơ gây Regression Backend & Bảo mật:** Có làm suy giảm JWT Revocation, Role Preview hay Concurrency không?
3. **Rủi ro Over-engineering:** Có đưa vào các thư viện thừa thãi gây phình to bundle size không?
4. **Tính khả thi của TypeScript Migration:** Trình tự di chuyển có an toàn không, có nguy cơ "gãy" build giữa chừng không?
5. **Tính thực tiễn của Trải nghiệm Loading:** Có dính bẫy fake delay hay layout shift không?
6. **Tuân thủ bản sắc thiết kế:** Có bị biến thành bản sao vô hồn của template Sakai không?

---

## 2. Bảng Phân Loại Các Phát Hiện Thẩm Định (Review Findings)

| Mã Finding | Mức độ nghiêm trọng | Hạng mục ảnh hưởng | Vấn đề phát hiện | Khuyến nghị điều chỉnh (Remediation) |
| :--- | :---: | :--- | :--- | :--- |
| **REV-FND-01** | **HIGH** | Routing & Deep Links | Khi tách `AdminUsersBookingsView` và `AdminSettingsView` thành các route mới, các URL cũ (`/admin/users-bookings`, `/admin/settings`) sẽ bị lỗi 404 nếu người dùng đã lưu bookmark hoặc qua deep link. | **Bổ sung Vue Router Redirect:** Thêm cấu hình redirect tự động: `/admin/users-bookings` $\to$ `/admin/identity/users` và `/admin/settings` $\to$ `/admin/system/settings` để bảo toàn tương thích ngược 100%. |
| **REV-FND-02** | **HIGH** | Role Preview Security | Cơ chế `activePreviewRoleCode` hiện tại chặn request dạng regex `/\/admin(\/\|$)/.test(url)`. Khi tách route mới thành `/admin/identity/*`, `/admin/operations/*`, cần đảm bảo chốt chặn này không bị lọt lỗ hổng. | Khẳng định chốt chặn trong `httpClient.ts` sử dụng regex `/\/admin(\/|$)/` vẫn bao quát 100% toàn bộ các endpoint con bắt đầu bằng `/admin/`. Bổ sung Unit Test xác minh cho các URL mới. |
| **REV-FND-03** | **MEDIUM** | Bundle Size & PrimeIcons | Dự án đề xuất chuyển sang Lucide Icons nhưng file `main.js` vẫn import `'primeicons/primeicons.css'`. Trong `dist/assets/`, font `primeicons.svg` nặng tới 342KB. | Lập kế hoạch gỡ bỏ hoàn toàn `primeicons` khỏi `package.json` và `main.js` ở Phase 8 sau khi đã thay thế 100% icon sang `lucide-vue-next`. Giảm ngay ~500KB bundle size. |
| **REV-FND-04** | **MEDIUM** | Unit Tests Regressions | Các file test hiện tại như `admin-views.spec.js` đang mount trực tiếp `AdminUsersBookingsView.vue`. Khi tách file này thành 3 views mới, bộ test cũ sẽ fail nếu không được cập nhật tương ứng. | Quy định rõ trong Phase 7: Khi tách view phải cập nhật đồng thời các file spec tương ứng (`admin-users.spec.ts`, `admin-roles.spec.ts`, `admin-bookings.spec.ts`), đảm bảo không có test suite nào bị bỏ rơi. |
| **REV-FND-05** | **LOW** | TypeScript Incremental Build | Trong quá trình di chuyển từng phần sang TypeScript, nếu `vue-tsc` được bật chặn build ngay từ Phase 1 thì các file `.vue` cũ (chưa có types) có thể gây cảnh báo hoặc lỗi. | Áp dụng cấu hình `allowJs: true` trong `tsconfig.json` ở các Phase đầu và chỉ kích hoạt `strict` tuyệt đối cho toàn bộ component ở Phase 8. |
