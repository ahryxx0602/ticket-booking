# 16 – Ma Trận Hợp Đồng API & Đặc Tả Kiểu Dữ Liệu (API Contract Matrix)

> **Mã tài liệu:** `FE-V2-CONTR-16`  
> **Phiên bản:** `2.1.0` (Đối chiếu & hiệu chỉnh chính xác 100% theo Backend source code)  
> **Thời điểm xác lập:** 25/11/2026  
> **Nguồn đối chiếu:** Mã nguồn Backend Spring Boot thực tế (`ApiResponse.java`, `PageResponse.java`, `AdminPermissionCatalog.java`, `AuthResponse.java`, `HoldSeatCommand.java`, `CreatePaymentCommand.java`, `BookingResponse.java`, `PaymentInitResponse.java`).  
> **Mục tiêu:** Khóa chặt hợp đồng kiểu dữ liệu TypeScript tĩnh, loại bỏ hoàn toàn các giả định sai lệch giữa Frontend và Backend.

---

## 1. Cấu Trúc Envelope Chuẩn Của Backend Spring Boot

### 1.1. `ApiResponse<T>` (Single Resource Envelope)
Đối chiếu trực tiếp mã nguồn `backend/shared-kernel/src/main/java/com/ticket/shared/dto/ApiResponse.java`:

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private String code;       // String (nullable, ví dụ: "SEAT_ALREADY_RESERVED", "INVALID_CREDENTIALS"), KHÔNG PHẢI number!
    private T data;            // Có thể null hoặc bị Jackson NON_NULL lược bỏ khi null
    private Instant timestamp; // ISO-8601 UTC Instant (ví dụ: "2026-11-25T03:40:00.123456Z")
}
```

#### TypeScript Interface Chuẩn (`src/types/api.ts`):
```typescript
/**
 * Envelope phản hồi chuẩn từ Spring Boot Backend (/api/v1/*).
 * Lưu ý: Jackson sử dụng NON_NULL nên `code` và `data` có thể bị omitted khi null.
 */
export interface ApiResponse<T> {
  success: boolean
  message: string
  code?: string | null     // Chuỗi mã lỗi nghiệp vụ (chỉ xuất hiện khi có lỗi hoặc code cụ thể)
  data?: T                 // Kiểu dữ liệu payload thực tế, có thể undefined nếu backend trả null
  timestamp: string        // ISO-8601 UTC string
}
```

### 1.2. `PageResponse<T>` (Paginated Collection Envelope)
Đối chiếu trực tiếp mã nguồn `backend/shared-kernel/src/main/java/com/ticket/shared/dto/PageResponse.java`:

```java
public class PageResponse<T> {
    private List<T> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;      // Boolean chuẩn, KHÔNG PHẢI hasMore!
    private boolean hasPrevious;
}
```

#### TypeScript Interface Chuẩn (`src/types/api.ts`):
```typescript
export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean        // True nếu còn trang tiếp theo (Pagination next)
  hasPrevious: boolean    // True nếu có trang trước đó (Pagination previous)
}
```

### 1.3. Hành Vi Bóc Tách Của Axios Interceptor (`src/shared/api/httpClient.ts`)
Đối chiếu chính xác mã nguồn `frontend/src/shared/api/httpClient.js` (dòng 100-101):
```javascript
httpClient.interceptors.response.use(
  (response) => response.data,
  (error) => { ... }
)
```
> [!IMPORTANT]
> **Axios Interceptor hiện tại trả về `response.data` — tức TOÀN BỘ API ENVELOPE (`ApiResponse<T>`), KHÔNG tự động unwrap `response.data.data`.**  
> Tuyệt đối không thay đổi hành vi của interceptor này trong quá trình TypeScript Migration để tránh làm gãy toàn bộ các consumers hiện hữu.
> Các API caller khi gọi `httpClient.get<ApiResponse<T>>()` sẽ nhận về `Promise<ApiResponse<T>>`, và truy cập payload qua `res.data`.

---

## 2. Hệ Thống Định Danh Người Dùng & Phân Quyền (Identity & RBAC Types)

### 2.1. Phân Biệt Base Role và Custom Role:
Đối chiếu `com.ticket.identity.domain.model.UserRole`:
```typescript
/**
 * Base Roles định nghĩa tại Backend enum UserRole.java.
 * Chỉ có đúng 3 Base Roles vật lý trong hệ thống!
 */
export type UserBaseRole = 'ROLE_CUSTOMER' | 'ROLE_OPERATOR' | 'ROLE_ADMIN'

/**
 * Trạng thái tài khoản người dùng
 */
export type UserStatus = 'ACTIVE' | 'LOCKED'
```

### 2.2. DTO Xác Thực Chuẩn (`AuthResponse.java`):
Đối chiếu chính xác `com.ticket.identity.application.dto.AuthResponse.java`:
```typescript
/**
 * DTO trả về từ POST /api/v1/auth/login, POST /api/v1/auth/register, GET /api/v1/auth/me.
 * Backend trả về: accessToken, tokenType, expiresIn, username, role, customRole, roleName, permissions.
 * KHÔNG tự ý thêm các fields như email, phone, fullName, status nếu endpoint này không cung cấp!
 */
export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  username: string
  role: UserBaseRole | string
  customRole?: string | null
  roleName?: string | null
  permissions: string[]
}

export interface AuthUserSession {
  username: string
  role: string
  customRole?: string | null
  roleName?: string | null
  permissions: string[]
}
```

### 2.3. Danh Mục 26 Mã Quyền Quản Trị Hệ Thống Chuẩn (`AdminPermissionCatalog.java`):
Đối chiếu 100% với `backend/context-identity/identity-domain/src/main/java/com/ticket/identity/domain/model/AdminPermissionCatalog.java`:

```typescript
export type AdminPermissionCode =
  // 1. Module MOD_DASHBOARD (Tổng quan & Đối soát)
  | 'PAYMENT_REPORT_VIEW'
  | 'PAYMENT_LIST_VIEW'
  | 'PAYMENT_RECONCILE_RESOLVE'

  // 2. Module MOD_OPERATORS (Nhà xe & Hoa hồng)
  | 'OPERATOR_VIEW'
  | 'OPERATOR_CREATE_UPDATE'
  | 'OPERATOR_APPROVE_REJECT'
  | 'OPERATOR_SUSPEND'
  | 'OPERATOR_COMMISSION_MANAGE'

  // 3. Module MOD_WITHDRAWALS (Giải ngân Ví Escrow)
  | 'WITHDRAWAL_VIEW'
  | 'WITHDRAWAL_APPROVE'
  | 'WITHDRAWAL_REJECT'

  // 4. Module MOD_BANNERS (Banner & Truyền thông)
  | 'BANNER_VIEW'
  | 'BANNER_CREATE_DELETE'
  | 'BANNER_APPROVE_REJECT'

  // 5. Module MOD_IAM (Người dùng & Phân quyền RBAC)
  | 'USER_VIEW'
  | 'USER_CREATE_UPDATE_DELETE'
  | 'USER_STATUS_LOCK'
  | 'USER_PASSWORD_RESET'
  | 'USER_ROLE_ASSIGN'
  | 'ROLE_MANAGE_CRUD'

  // 6. Module MOD_BOOKINGS (Đơn vé & Hoàn tiền Admin)
  | 'BOOKING_VIEW'
  | 'BOOKING_FORCE_REFUND'

  // 7. Module MOD_SETTINGS (Bến xe, Chuyến xe & Cấu hình)
  | 'STATION_VIEW'
  | 'STATION_CREATE_UPDATE_DELETE'
  | 'TRIP_MANAGE_CRUD'
  | 'SYSTEM_SETTINGS_MANAGE'

export interface PermissionItemDto {
  code: AdminPermissionCode
  name: string
  httpMethod: string
  apiPattern: string
  description: string
  viewPermission: boolean
}

export interface ModuleCatalogDto {
  moduleCode: string
  moduleName: string
  routePath: string
  viewPermissionCode: AdminPermissionCode
  permissions: PermissionItemDto[]
}
```

---

## 3. Ma Trận Hợp Đồng Nghiệp Vụ Theo Module (Module Contract Matrix)

### 3.1. Catalog & Customer Booking Contracts (`src/types/booking.ts`)
| Backend Endpoint | HTTP Method | Request Payload Interface | Response Envelope | Ghi chú trường thực tế |
| :--- | :---: | :--- | :--- | :--- |
| `/api/v1/catalog/stations` | `GET` | Không | `ApiResponse<StationDto[]>` | Danh sách bến xe |
| `/api/v1/catalog/trips` | `GET` | `SearchTripQuery` (`departureCity`, `arrivalCity`, `departureDate`) | `ApiResponse<TripDto[]>` | Tra cứu chuyến (GAP-02: query nhận city string) |
| `/api/v1/catalog/trips/{id}/seats` | `GET` | Path `id: number` | `ApiResponse<SeatDto[]>` | Danh sách ghế kèm status |
| `/api/v1/bookings/hold` | `POST` | `HoldSeatCommand` (`userId?: number`, `tripId: number`, `seatNumbers: string[]`) | `ApiResponse<BookingResponse>` | Khóa giữ chỗ. **Không có** passengerName/phone trong body |
| `/api/v1/bookings/{ref}` | `GET` | Path `ref: string` | `ApiResponse<BookingResponse>` | Chi tiết đơn đặt vé |
| `/api/v1/bookings/{ref}/cancel`| `POST`| Path `ref: string` | `ApiResponse<void>` | Hủy đơn vé |
| `/api/v1/bookings/my-bookings` | `GET` | Query `page?: number`, `size?: number` | `ApiResponse<PageResponse<BookingResponse>>` | Phân trang vé của tôi |
| `/api/v1/payments/checkout` | `POST` | `CreatePaymentCommand` (`bookingReference: string`, `paymentMethod: PaymentMethod`) | `ApiResponse<PaymentInitResponse>` | Trả về `redirectPaymentUrl` VNPay thật |

#### Chi tiết DTO Booking & Payment thực tế:
```typescript
export type PaymentMethod = 'VNPAY' | 'MOMO' | 'ZALOPAY'

export interface CreatePaymentCommand {
  bookingReference: string
  paymentMethod: PaymentMethod
}

export interface PaymentInitResponse {
  paymentReference: string
  redirectPaymentUrl: string
}

export interface HoldSeatCommand {
  userId?: number // Optional: Backend giải quyết từ JWT Principal; nếu truyền sẽ kiểm tra IDOR
  tripId: number
  seatNumbers: string[]
}

export interface BookingItemDto {
  seatId: number
  seatNumber: string
  price: number
}

export type BookingStatus = 'HELD' | 'CONFIRMED' | 'CANCELLED' | 'REFUNDED' | 'EXPIRED'

export interface BookingResponse {
  bookingId: number
  bookingReference: string
  userId: number
  tripId: number
  status: BookingStatus
  totalAmount: number
  expiresAt?: string // LocalDateTime ISO string: "2026-11-25T12:00:00"
  createdAt?: string
  seatNumbers: string[]
  items?: BookingItemDto[]
  qrPayload?: string // Server-signed QR data (xem GAP-05)
}
```

---

## 4. Quy Chuẩn Timezone Cho Đếm Ngược Giữ Chỗ (Countdown Handling)
- Spring Boot `LocalDateTime` (ví dụ `expiresAt: "2026-11-25T14:30:00"`) không mang thông tin timezone offset.
- Server Backend chạy theo múi giờ hệ thống (mặc định là GMT+7 / Asia/Ho_Chi_Minh trong môi trường triển khai của ứng dụng).
- Khi parse `expiresAt` tại Frontend, parser phải đảm bảo hiểu theo múi giờ địa phương tương thích hoặc quy đổi chuẩn về epoch millis:
  ```typescript
  export function parseServerLocalDateTime(isoStr: string): number {
    // Nếu chuỗi không có Z hoặc offset, chuẩn hóa an toàn
    const safeStr = isoStr.includes('Z') || /[+-]\d{2}:\d{2}$/.test(isoStr)
      ? isoStr
      : `${isoStr}+07:00` // Áp dụng múi giờ chuẩn của sàn
    return new Date(safeStr).getTime()
  }
  ```
