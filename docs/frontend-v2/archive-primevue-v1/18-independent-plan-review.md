# 18 – Báo Cáo Thẩm Định Kế Hoạch Lần 2 (Independent Plan Review 2)

> **Mã tài liệu:** `FE-V2-REV2-18`  
> **Thời điểm thẩm định:** 25/11/2026  
> **Vai trò:** Senior Technical Reviewer & Lead Architect.  
> **Đối tượng thẩm định:** Kế hoạch tinh chỉnh theo các yêu cầu từ `REV-V2-001` đến `REV-V2-008`.

---

## 1. Đánh Giá Khắc Phục Các Sai Lệch Kỹ Thuật (Review Checkpoints)

| Mã Checkpoint | Hạng mục kiểm tra | Đánh giá hiện tại | Kết luận thẩm định |
| :--- | :--- | :--- | :---: |
| **REV-V2-001** | Hợp đồng `ApiResponse.code` (String, không phải number), `PageResponse` (`hasNext`/`hasPrevious`, không phải `hasMore`), `UserRole` (3 Base Roles + Custom Roles). | Đã sửa đổi chính xác 100% trong [16-api-contract-matrix.md](file:///home/vanthanh/Ind-Project/Java/ticket-booking/docs/frontend-v2/16-api-contract-matrix.md), khớp hoàn toàn mã nguồn Java Spring Boot. | **PASSED** |
| **REV-V2-002** | Thẩm tra `GET /partners/trips` và `GET /catalog/trips` query params. | Đã lập sổ bộ [17-backend-api-gap-register.md](file:///home/vanthanh/Ind-Project/Java/ticket-booking/docs/frontend-v2/17-backend-api-gap-register.md). Xác nhận `GET /partners/trips` bị thiếu ở Backend và phân loại `BACKEND_API_MISSING`, không giả định tồn tại. | **PASSED** |
| **REV-V2-003** | Luồng Checkout VNPay thật, xóa `setTimeout` giả lập, xác thực thanh toán từ Backend. | Đã lập thiết kế luồng thanh toán VNPay chuẩn: redirect `redirectPaymentUrl`, kiểm tra trạng thái vé `CONFIRMED` từ `GET /bookings/{ref}`, không tự tạo QR fake. | **PASSED** |
| **REV-V2-004** | Permission-aware redirects cho legacy routes (`/admin/users-bookings`, `/admin/settings`), tránh đẩy user vào trang 403. | Đã bổ sung logic điều hướng thông minh dựa trên `auth.hasAnyPermission()` trong Route Guard thay vì redirect tĩnh cố định. | **PASSED** |
| **REV-V2-005** | Bật `strict: true` ngay từ Phase 1 của TypeScript Migration, không trì hoãn đến cuối. | Đã cập nhật cấu hình `tsconfig.json` với `strict: true`, dùng `allowJs: true` trong giai đoạn chuyển đổi và cấm triệt để `any` hay `ts-ignore`. | **PASSED** |
| **REV-V2-006** | Sắp xếp thứ tự Phase phụ thuộc: Types $\to$ API Client $\to$ Auth $\to$ Shell $\to$ Shared UI $\to$ Views. | Đã sắp xếp lại sơ đồ Phase trong Master Plan, giải quyết triệt để vấn đề phụ thuộc vòng (circular dependencies). | **PASSED** |
| **REV-V2-007** | Loading System toàn diện (Initial, Refreshing, Submitting, Cancel, Stale protection, Debounce). | Đã thiết kế ma trận tương tác chi tiết, cấm fake delay, đảm bảo reset loading trong `finally`. | **PASSED** |
| **REV-V2-008** | Tính xác thực của bằng chứng: Không đưa tỷ lệ giả định, đối chiếu phiên bản lockfile thật. | Đã rà soát và hiệu chỉnh toàn bộ dữ liệu kiểm kê trong các tài liệu. | **PASSED** |

---

## 2. Kết Luận Thẩm Định Độc Lập

Kế hoạch Hiện đại hóa Frontend VeloBus V2 đã được bổ sung đầy đủ các bằng chứng đối chiếu mã nguồn thực tế, khắc phục triệt để các lỗ hổng giả định API và thiết lập lộ trình kiểm soát rủi ro nghiêm ngặt.

$\to$ **KẾT QUẢ THẨM ĐỊNH:** **APPROVED CONDITIONALLY — SẴN SÀNG CHO GIAI ĐOẠN DUYỆT THIẾT KẾ UI (MANDATORY UI DESIGN APPROVAL GATE).**
