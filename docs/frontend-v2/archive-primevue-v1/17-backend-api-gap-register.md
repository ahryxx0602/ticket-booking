# 17 – Sổ Bộ Khoảng Cách API Backend & Đề Xuất Bổ Sung (Backend API Gap Register)

> **Mã tài liệu:** `FE-V2-GAP-17`  
> **Phiên bản:** `2.1.0` (Updated per Pre-implementation Mandates & QR Security)  
> **Thời điểm thẩm tra:** 25/11/2026  
> **Phương pháp kiểm chứng:** Đối chiếu 1-1 giữa khai báo gọi API trong `frontend/src/**/api.js` và các `@RestController` trong Backend Spring Boot.  
> **Quy tắc:** Tuyệt đối không giả định API tồn tại; phân loại rõ ràng trạng thái khoảng cách kỹ thuật; không triển khai code backend khi chưa được duyệt đặc tả.

---

## 1. Bảng Đăng Ký Khoảng Cách API (API Gap Register)

| Mã Gap | Tên Endpoint & Method | Phân loại trạng thái | Hiện trạng Backend thực tế | Hiện trạng Frontend thực tế | Biện pháp xử lý kỹ thuật (V2 Plan) |
| :--- | :--- | :---: | :--- | :--- | :--- |
| **GAP-01** | `GET /api/v1/partners/trips` | **BACKEND_API_MISSING** | `PartnerController.java` chỉ có `@PostMapping("/trips")`. **Hoàn toàn không có `@GetMapping("/trips")`**! | `partnerApi.js` không khai báo; `OperatorTripsView.vue` dùng mảng mock `const trips = ref([...])`. | **Không giả lập qua Public Catalog hay Session.** Chuẩn bị đặc tả chi tiết bên dưới trình duyệt; tạm thời cô lập trạng thái trên UI khi chưa có endpoint. |
| **GAP-02** | `GET /api/v1/catalog/trips` | **CONTRACT_MISMATCH** | `TripCatalogController.java` yêu cầu query params: `departureCity`, `arrivalCity`, `departureDate` (ISO `LocalDate`). | `TripListView.vue` lấy từ URL: `from` (mã bến `HN-GP`), `to` (mã bến `DN-TT`), `date`. Không gọi API mà tự filter mảng mock. | **Căn chỉnh hợp đồng tại Client:** Ánh xạ mã bến xe sang thành phố tương ứng qua danh mục stations hợp lệ; chỉ hiển thị bộ lọc Backend hỗ trợ. |
| **GAP-03** | `POST /api/v1/payments/checkout` | **FRONTEND_INTEGRATION_MISSING** | `PaymentController.java` đã có endpoint nhận `{ bookingReference, paymentMethod }`, trả về `{ paymentReference, redirectPaymentUrl }`. | `CheckoutView.vue` dùng `setTimeout(..., 1200)` mô phỏng thanh toán, không gọi API, redirect sang `/my-bookings?payment_success=true`. | **Xóa bỏ `setTimeout` giả lập.** Gọi đúng API nhận `redirectPaymentUrl` và chuyển sang VNPay. Khi quay lại ứng dụng, xác minh trạng thái thật từ Backend, không suy diễn từ query param URL. |
| **GAP-04** | Dữ liệu Mock tĩnh tại các Views | **FRONTEND_INTEGRATION_MISSING** | Backend đã có đầy đủ API cho Vehicles, Drivers, Wallet, Users, Banners, Bookings. | Các views dùng mảng mock tĩnh và nuốt lỗi API khi gặp sự cố. | **Xóa bỏ toàn bộ dữ liệu mock.** Tích hợp API thật; bắt lỗi hiển thị ErrorState và Toast rõ ràng, không bao giờ báo thành công khi API thất bại. |
| **GAP-05** | Server-Signed QR Ticket Payload | **BACKEND_API_MISSING** | `BookingResponse.java` không trả về chuỗi signed payload hoặc QR code image URL. | `MyBookingsView.vue` dùng chuỗi tĩnh `VELOBUS|...|HMAC:...` và render bằng `qrcode`. | **Bảo mật tuyệt đối:** Cấm sinh HMAC hay lưu secret tại client. Frontend chỉ render QR từ payload đã ký sẵn do Backend trả về. Đề xuất bổ sung trường `qrPayload` vào DTO vé. |

---

## 2. Đề Xuất Đặc Tả Kỹ Thuật Backend API Cho GAP-01 (`GET /api/v1/partners/trips`)

> [!IMPORTANT]
> **Đặc tả kỹ thuật giới hạn phạm vi — Chờ người dùng phê duyệt riêng trước khi viết code Backend.**

### 2.1. Mục Tiêu Nghiệp Vụ
Cho phép đối tác Nhà xe (Operator) tra cứu và quản lý danh sách các chuyến xe thuộc quyền sở hữu của chính nhà xe mình (Ownership Filtering).

### 2.2. Chi Tiết Endpoint
- **URL:** `GET /api/v1/partners/trips`
- **Authentication:** Bắt buộc (JWT Bearer Token trong header `Authorization`).
- **Authorization:** Yêu cầu quyền `ROLE_OPERATOR`.
- **Ownership Isolation:** Backend tự động trích xuất `partnerId` (hoặc `operatorId`) gắn liền với tài khoản đang đăng nhập từ JWT Principal / SecurityContext. Tuyệt đối không cho phép Operator truyền `partnerId` tùy ý qua query params (chống IDOR).

### 2.3. Query Parameters (Hỗ trợ phân trang và lọc)
| Tên tham số | Kiểu dữ liệu | Bắt buộc | Mặc định | Mô tả |
| :--- | :---: | :---: | :---: | :--- |
| `status` | `String` | Không | `null` | Lọc theo trạng thái (`SCHEDULED`, `RUNNING`, `COMPLETED`, `CANCELLED`) |
| `departureDate` | `LocalDate` | Không | `null` | Lọc theo ngày khởi hành (`YYYY-MM-DD`) |
| `page` | `Integer` | Không | `0` | Số thứ tự trang (0-based) |
| `size` | `Integer` | Không | `10` | Kích thước trang (tối đa 50) |

### 2.4. Response DTO
Trả về envelope `ApiResponse<PageResponse<PartnerTripDto>>`:
```java
public class PartnerTripDto {
    private Long tripId;
    private String tripCode;
    private Long routeId;
    private String routeName;
    private Long vehicleId;
    private String vehiclePlate;
    private String vehicleType;
    private Long driverId;
    private String driverName;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private BigDecimal basePrice;
    private Integer totalSeats;
    private Integer bookedSeats;
    private String status; // SCHEDULED, RUNNING, COMPLETED, CANCELLED
}
```

### 2.5. Kế Hoạch Integration Tests (Spring Boot Test)
1. `shouldReturnOnlyTripsOwnedByAuthenticatedPartner()`: Đăng nhập Operator A, kiểm tra kết quả chỉ chứa các chuyến của Operator A, không lộ chuyến của Operator B.
2. `shouldReturnForbiddenWhenCustomerAccessesPartnerTrips()`: Đăng nhập tài khoản `ROLE_CUSTOMER`, mong đợi HTTP 403 Forbidden.
3. `shouldReturnUnauthorizedWhenUnauthenticated()`: Gọi không có Bearer token, mong đợi HTTP 401 Unauthorized.
4. `shouldFilterTripsByStatusAndDateCorrectly()`: Truy vấn theo status và ngày khởi hành, kiểm tra kết quả trả về chính xác.

---

## 3. Đề Xuất Đặc Tả Kỹ Thuật Cho GAP-05 (Server-Signed QR Payload)
- Bổ sung trường `qrPayload` (String) vào `BookingResponse.java`.
- Backend sinh chuỗi `qrPayload` được ký bằng HMAC-SHA256 trên server:
  `VELOBUS|{bookingReference}|{tripId}|{seatNumbers}|{timestamp}|HMAC:{signature}`
- Frontend chỉ nhận chuỗi này và chuyển thành hình ảnh QR hiển thị cho hành khách, bảo đảm zero-secret exposure tại client.
