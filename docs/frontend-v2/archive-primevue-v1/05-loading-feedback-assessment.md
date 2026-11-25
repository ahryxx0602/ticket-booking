# 05 – Khảo Sát Trải Nghiệm Loading & Phản Hồi Tương Tác (Loading & Feedback Assessment)

> **Mã tài liệu:** `FE-V2-LOAD-05`  
> **Phiên bản:** `2.1.0` (Corrected per Pre-implementation Mandates & Async State Invariants)  
> **Thời điểm rà soát:** 25/11/2026  
> **Mục tiêu:** Xóa bỏ hoàn toàn loading giả (`setTimeout`), giải quyết hiện tượng giật layout và thiết lập hệ sinh thái Loading phản ánh chính xác 100% trạng thái bất đồng bộ.

---

## 1. Hiện Trạng Trải Nghiệm Loading Tại Codebase Hiện Tại

Khảo sát mã nguồn các View và Component cho thấy các vấn đề:

1. **Thiếu Tín Hiệu Chuyển Trang (Navigation Progress Blindness):**
   - Khi người dùng bấm chuyển các route lớn (ví dụ từ `/` sang `/search`, hoặc chuyển giữa các trang quản trị), Vue Router tải dynamic chunk mất từ 100ms - 500ms nhưng toàn bộ màn hình đứng yên, không có thanh tiến trình ở đầu trang.
2. **DataTable Giật Khung Hình (Layout Jitter):**
   - Khi bảng dữ liệu gọi API, bảng co lại chỉ còn 1 dòng trống hoặc thông báo rỗng rồi bất ngờ bung to ra khi dữ liệu về, gây cảm giác giật cục và mỏi mắt cho người vận hành.
3. **Nút Bấm Cho Phép Bấm Trùng Lặp (Double Submit):**
   - Tại các form như [CheckoutView.vue](file:///home/vanthanh/Ind-Project/Java/ticket-booking/frontend/src/modules/catalog-booking/views/CheckoutView.vue), nút "Thanh toán VNPay" không bị vô hiệu hóa (`disabled`) trong thời gian chờ response, dễ dẫn tới gửi 2 request checkout liên tiếp.
4. **Lạm Dụng `setTimeout` Để Đóng Thông Báo Giả:**
   - [AdminUsersBookingsView.vue](file:///home/vanthanh/Ind-Project/Java/ticket-booking/frontend/src/modules/admin/views/AdminUsersBookingsView.vue) dùng `setTimeout(..., 4500)` để xóa feedback; [AdminSettingsView.vue](file:///home/vanthanh/Ind-Project/Java/ticket-booking/frontend/src/modules/admin/views/AdminSettingsView.vue) dùng `setTimeout(..., 3000)`. Đây là xử lý thụ động, không liên kết với trạng thái thực của Promise.
5. **Thiếu Error State Kèm Nút Thử Lại (Retry Mechanism):**
   - Khi mất mạng hoặc API lỗi 500, các bảng chỉ hiển thị danh sách rỗng (Empty State) thay vì phân biệt giữa "Không có dữ liệu thật" và "Tải dữ liệu thất bại". Người dùng không biết tại sao không có dữ liệu và không có nút để tải lại.

---

## 2. Hệ Thống Chiến Lược Loading Thống Nhất (Unified Loading System)

Xây dựng ma trận phản hồi tương tác chuẩn mực cho mọi tác vụ bất đồng bộ:

| Hành vi người dùng | Phạm vi ảnh hưởng (Scope) | Thành phần UI sử dụng | Cơ chế kích hoạt (Trigger) | Điều kiện kết thúc (Resolution) | Hành vi khi lỗi (Error Behavior) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Chuyển Route qua Vue Router** | Toàn màn hình (Toàn cục) | `AppProgressBar` (Thanh bar 3px xanh `#2563EB` chạy trên đỉnh viewport) | `router.beforeEach` | `router.afterEach` | Đổi màu bar sang đỏ `#EF4444`, dừng lại và biến mất sau 300ms. |
| **Tìm kiếm chuyến tại Search Bar** | Khu vực dưới Search Bar | `SearchLoadingIndicator` (Dải xung điện shimmer thanh mảnh dưới input) | Bấm nút "Tìm chuyến" / Input change | API Catalog trả kết quả | Hiển thị thông báo inline dưới trường tìm kiếm. |
| **Nạp dữ liệu trang lần đầu (Initial Load)** | Khung nội dung trang | `PageSkeleton` hoặc `TableSkeleton` (Giữ nguyên chiều cao và số cột của bảng) | `onMounted()` gọi API | Promise settled | Hiển thị `ErrorState` kèm nút "Tải lại trang". |
| **Lọc dữ liệu bảng (Filter / Search Table)** | Vùng bảng DataTable | `DataTable` overlay loading spinner hoặc mờ mờ nhẹ 50% opacity | Bộ lọc thay đổi (Debounced 300ms) | API trả về danh sách đã lọc | Toast thông báo lỗi lọc, giữ nguyên kết quả cũ. |
| **Nạp ngầm làm mới dữ liệu (Background Refresh)** | Header bảng / Nút làm mới | Subtle spinning icon tại nút Refresh hoặc thanh loading 1px trên đỉnh bảng | Polling định kỳ hoặc thao tác làm mới nền | Request hoàn tất | Dữ liệu cập nhật tại chỗ, không giật màn hình hoặc reset scroll. |
| **Bấm nút hành động (Submit Form / CTA)** | Trong phạm vi nút bấm | `AppButton :loading="true"` (Icon xoay spinner thay thế icon thường, khóa click) | Bấm nút submit | Request API hoàn tất | Mở lại nút bấm, hiển thị Toast cảnh báo lỗi cụ thể. |
| **Thao tác tải file / ảnh lên Firebase** | Trong component uploader | `ProgressBar` phần trăm thật từ Axios `onUploadProgress` | Người dùng chọn file | Upload hoàn tất 100% | Báo lỗi file vượt dung lượng hoặc sai định dạng. |

---

## 3. Kiến Trúc Kiểm Soát Tương Tác & Bất Biến Bất Đồng Bộ (Async State Invariants)

### 3.1. Phân Biệt Loading Giả Với Bộ Đếm Thời Gian Hợp Lệ
- **Loading Giả (Cấm Tuyệt Đối):** Sử dụng `setTimeout(..., 1000)` hoặc bộ đếm thời gian tự tăng `%` để tạo cảm giác ứng dụng đang làm việc trong khi không có tác vụ I/O nào đang thực thi.
- **Timer Hợp Lệ (Được Phép & Khuyến Khích):**
  1. *Debounce timer* (ví dụ 300ms khi gõ ô tìm kiếm trước khi phát request).
  2. *Animation & Transition timer* (thời gian chạy CSS cubic-bezier 150ms-250ms).
  3. *Toast Notification auto-dismiss* (tự đóng thông báo thành công sau 3-5 giây).
  4. *Security Throttling / Rate-limit cooldown* (đếm ngược 60s trước khi cho gửi lại mã OTP).

### 3.2. Quản Lý Vòng Đời Request với Request-Scoped `AbortController`
Để đảm bảo **Latest-Request-Wins**, bảo vệ chống **Stale Responses**, và tránh việc request cũ kết thúc trong `finally` ghi đè hoặc vô hiệu hóa controller của request mới:

```typescript
import { ref, onBeforeUnmount } from 'vue'
import axios, { type CanceledError } from 'axios'

export function useCancelableQuery<TParams, TResult>(
  queryFn: (params: TParams, signal: AbortSignal) => Promise<TResult>
) {
  let activeController: AbortController | null = null
  let activeRequestId = 0

  const data = ref<TResult | null>(null)
  const isLoading = ref(false)
  const error = ref<Error | null>(null)

  async function execute(params: TParams): Promise<TResult | null> {
    // 1. Hủy bỏ request trước đó nếu đang chạy dở
    if (activeController) {
      activeController.abort('New request initiated')
    }

    // 2. Tạo controller và gắn ID duy nhất cho request mới
    const controller = new AbortController()
    activeController = controller
    const currentRequestId = ++activeRequestId

    isLoading.value = true
    error.value = null

    try {
      const result = await queryFn(params, controller.signal)

      // 3. Stale Response Protection: Chỉ gán kết quả nếu đây là request mới nhất
      if (currentRequestId === activeRequestId) {
        data.value = result
        return result
      }
      return null
    } catch (err: unknown) {
      // 4. Nếu request bị cancel có chủ đích -> Không xem là kết quả rỗng thành công, cũng không xem là lỗi hệ thống
      if (axios.isCancel(err) || (err instanceof Error && err.name === 'AbortError')) {
        return null // Bỏ qua an toàn
      }
      // Chỉ gán lỗi nếu request này vẫn là request hiện hành
      if (currentRequestId === activeRequestId) {
        error.value = err instanceof Error ? err : new Error(String(err))
      }
      throw err
    } finally {
      // 5. Tránh bug kinh điển: Chỉ gỡ bỏ controller và tắt loading nếu activeController chưa bị thay thế bởi request mới hơn
      if (activeController === controller) {
        activeController = null
        isLoading.value = false
      }
    }
  }

  // 6. Cleanup on unmount: Hủy ngay khi component bị unmount
  onBeforeUnmount(() => {
    if (activeController) {
      activeController.abort('Component unmounted')
      activeController = null
    }
  })

  return { data, isLoading, error, execute }
}
```

### 3.3. Bảo Vệ Thao Tác Đột Biến Không Thể Thử Lại Tự Động (Non-Idempotent Mutation Invariant)
- **Tuyệt đối không tự động retry (auto-retry)** các request dạng mutation như `POST /bookings/hold`, `POST /payments/checkout`, `POST /admin/bookings/{ref}/refund`.
- Tự động retry các tác vụ này khi mạng chập chờn có nguy cơ tạo giao dịch trùng lặp hoặc trừ tiền hai lần.
- Chỉ cho phép Retry có chủ đích từ phía người dùng (User-Triggered Retry) qua nút bấm UI, kết hợp khóa `isSubmitting` để ngăn ngừa double submit.

### 3.4. Bắt Lỗi Điều Hướng Toàn Cục với `router.onError()`
- Tích hợp hook `router.onError((error) => { ... })` để:
  1. Dừng ngay lập tức và ẩn `AppProgressBar`.
  2. Bắt lỗi dynamic import chunk failure (thường gặp khi deploy bản mới làm hash chunk cũ mất hiệu lực) và hướng dẫn người dùng làm mới trang.
