# 06 – Đánh Giá & Kế Hoạch Di Chuyển Sang TypeScript (TypeScript Migration Assessment)

> **Mã tài liệu:** `FE-V2-TS-06`  
> **Phiên bản:** `2.0.0` (Updated per REV-V2-001 & Refinement)  
> **Thời điểm rà soát:** 25/11/2026  
> **Mục tiêu:** Đánh giá mức độ sẵn sàng, phân tích rủi ro và thiết lập quy trình chuyển đổi toàn diện từ JavaScript sang TypeScript cho Frontend VeloBus mà không gây gián đoạn nghiệp vụ.

---

## 1. Kiểm Kê Danh Mục Mã Nguồn Cần Di Chuyển (Migration Inventory)

Toàn bộ 69 tệp tin của Frontend hiện tại được phân loại theo mức độ ưu tiên và độ phức tạp di chuyển:

| Nhóm tệp tin | Số lượng | Độ phức tạp | Tệp tin tiêu biểu | Trình tự di chuyển đề xuất |
| :--- | :---: | :---: | :--- | :---: |
| **1. Hạ tầng kiểu dữ liệu (Types & Enums)** | Mới hoàn toàn | Thấp | `src/types/api.ts`, `src/types/models.ts`, `src/types/rbac.ts` | **Tầng 1 (Đầu tiên)** |
| **2. Tầng Tiện ích & API Client** | 3 files | Trung bình | `httpClient.ts`, `formatters.ts`, `redirect.ts` | **Tầng 1** |
| **3. API Services Contracts** | 5 files | Trung bình | `auth/api.ts`, `catalog-booking/api.ts`, `partner/api.ts`, `admin/api.ts` | **Tầng 2** |
| **4. Pinia Feature Stores** | 3 files | Trung bình | `auth/store.ts`, `catalog-booking/store.ts`, `notificationStore.ts` | **Tầng 2** |
| **5. Composables Dùng Chung** | 3 files | Trung bình | `useCountdown.ts`, `useLayoutWidth.ts`, `useAdminPermissions.ts` | **Tầng 2** |
| **6. Shared UI Components** | 15 files | Trung bình | `AppButton.vue`, `StatusBadge.vue`, `SeatMap2Deck.vue`, `FormField.vue` | **Tầng 3** |
| **7. App Shells & Router** | 6 files | Cao | `router/index.ts`, `DashboardLayout.vue`, `CustomerLayout.vue` | **Tầng 3** |
| **8. Customer & Auth Views** | 7 files | Cao | `LoginView.vue`, `HomeView.vue`, `SeatSelectionView.vue`, `CheckoutView.vue` | **Tầng 4** |
| **9. Operator & Admin Views** | 11 files | Rất cao | `AdminUsersView.vue`, `AdminRolesView.vue`, `FleetView.vue`, ... | **Tầng 4** |
| **10. Unit Tests & E2E Tests** | 13 files | Trung bình | `*.spec.ts` | **Tầng 5 (Cuối cùng)** |

---

## 2. Thiết Kế Hệ Thống Kiểu Dữ Liệu Tĩnh (Type System Architecture)

Hệ thống kiểu dữ liệu TypeScript sẽ phản ánh chính xác cấu trúc API và Domain Model của Backend Spring Boot (tham chiếu chi tiết tại [16-api-contract-matrix.md](file:///home/vanthanh/Ind-Project/Java/ticket-booking/docs/frontend-v2/16-api-contract-matrix.md)):

### 2.1. Hợp Đồng API Envelope Chuẩn (`src/types/api.ts`):
```typescript
/**
 * Phản ánh chính xác ApiResponse.java của Spring Boot.
 * Chú ý: code là String (nullable/omitted when null via Jackson NON_NULL), timestamp là ISO string.
 */
export interface ApiResponse<T> {
  success: boolean
  code?: string | null
  message: string
  data: T
  timestamp: string
}

/**
 * Phản ánh chính xác PageResponse.java của Spring Boot.
 * Sử dụng hasNext và hasPrevious (KHÔNG DÙNG hasMore).
 */
export interface ApiPageResponse<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}

export interface ApiError {
  code?: string
  message: string
  timestamp?: string
  fieldErrors?: Record<string, string>
}
```

### 2.2. Domain Models & Enums (`src/types/models.ts`):
- **User & Base Roles:** `UserRole = 'ROLE_CUSTOMER' | 'ROLE_OPERATOR' | 'ROLE_ADMIN'`. (Chỉ có 3 Base Roles enum trong database Spring Boot enum `UserRole.java`. Các vai trò nghiệp vụ nâng cao như `'ROLE_ACCOUNTANT'`, `'ROLE_CSKH'` được lưu trong trường `users.custom_role_code`).
- **Profile & Auth:** `UserProfile`, `LoginCredentials`, `AuthResponse` (`accessToken`, `tokenType`, `expiresIn`, `user`).
- **Catalog & Booking:** `TripSummaryDto`, `TripDetailDto`, `SeatDto` (`seatNumber`, `deck`, `status`: `'AVAILABLE' | 'HELD_OTHER' | 'SOLD' | 'SELECTED'`), `BookingHoldRequest`, `BookingHoldResponse`, `BookingDetailDto`, `PaymentInitResponse` (`redirectPaymentUrl`).
- **Partner & Escrow:** `PartnerWalletDto` (`availableBalance`, `holdingBalance`, `settlingBalance`, `withdrawnTotal`), `VehicleDto`, `DriverDto`, `WithdrawalRequestDto`.
- **RBAC Matrix:** `AdminPermissionCode` (26 mã quyền từ Backend), `PermissionCatalogModule`.

---

## 3. Cấu Hình Hạ Tầng TypeScript (`tsconfig.json` & `vue-tsc`)

Xây dựng cấu hình `tsconfig.json` nghiêm ngặt với `strict: true` ngay từ Phase 1, nhưng cho phép `allowJs: true` trong giai đoạn chuyển đổi dần (incremental migration) để không làm gãy các file JavaScript cũ chưa kịp chuyển đổi:

```json
{
  "compilerOptions": {
    "target": "ESNext",
    "useDefineForClassFields": true,
    "module": "ESNext",
    "moduleResolution": "bundler",
    "strict": true,
    "allowJs": true,
    "checkJs": false,
    "noImplicitAny": true,
    "strictNullChecks": true,
    "strictFunctionTypes": true,
    "jsx": "preserve",
    "resolveJsonModule": true,
    "isolatedModules": true,
    "esModuleInterop": true,
    "lib": ["ESNext", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "noEmit": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    },
    "types": ["vite/client", "vitest/globals"]
  },
  "include": ["src/**/*.ts", "src/**/*.d.ts", "src/**/*.tsx", "src/**/*.vue"],
  "exclude": ["node_modules", "dist"]
}
```

Bổ sung script vào `package.json`:
- `"type-check": "vue-tsc --noEmit"`
- `"build": "vue-tsc --noEmit && vite build"` (Bảo vệ CI không bao giờ build nếu còn lỗi kiểu dữ liệu).

---

## 4. Các Quy Tắc Di Chuyển Bất Biến (Migration Rules)

1. **Tuyệt Đối Không "Fake Migration":** Nghiêm cấm hành vi chỉ đổi đuôi file `.js` sang `.ts` rồi ép kiểu bằng `any` vô tội vạ hoặc thêm chú thích `// @ts-ignore` để qua mắt compiler. Mọi trường dữ liệu không chắc chắn phải sử dụng `unknown` kết hợp type guards (ví dụ: `isApiError(err)`).
2. **Không Thay Đổi Logic Nghiệp Vụ Trong Giai Đoạn Migration:** Mục tiêu của TypeScript Migration là khóa chặt hợp đồng kiểu dữ liệu, không được vừa di chuyển vừa tự ý thay đổi thuật toán hoặc hành vi giao diện đã được kiểm chứng.
3. **So Sánh Manual Types vs OpenAPI Generation:**
   - *Đánh giá:* Backend hiện tại chưa có cấu hình OpenAPI/Swagger sinh JSON tự động trong CI pipeline. Do đó, việc tự viết và duy trì bộ Type Definitions thủ công ánh xạ trực tiếp từ DTOs Backend (vốn rất ổn định qua 8 Controller) là phương án tối ưu, chi phí bảo trì thấp nhất và không gây phụ thuộc công cụ ngoài.
4. **Thứ Tự Tối Ưu Với Redesign:**
   - **Chiến lược:** Thực hiện **TypeScript Migration cho tầng Foundation & Shared Core trước** (Types, HttpClient, Stores), sau đó khi tiến hành Redesign và tách các God Views thì viết luôn mã mới bằng `<script setup lang="ts">`. Điều này giúp tiết kiệm 50% thời gian so với việc chuyển đổi TypeScript trên các View cũ rồi lại đập đi viết lại khi Redesign.
