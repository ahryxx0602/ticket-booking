import { ref, onBeforeUnmount, type Ref } from 'vue'
import axios from 'axios'

export interface UseCancelableQueryReturn<TParams, TResult> {
  data: Ref<TResult | null>
  isLoading: Ref<boolean>
  error: Ref<Error | null>
  execute: (params: TParams) => Promise<TResult | null>
  cancel: () => void
}

/**
 * Composable thực thi query bất đồng bộ với cơ chế hủy request tự động.
 * - Request-scoped AbortController
 * - Latest-request-wins (requestId counter)
 * - Stale response protection
 * - Không diễn giải cancelled request thành empty success
 * - Dọn dẹp tài nguyên khi component unmount
 */
export function useCancelableQuery<TParams, TResult>(
  queryFn: (params: TParams, signal: AbortSignal) => Promise<TResult>
): UseCancelableQueryReturn<TParams, TResult> {
  let activeController: AbortController | null = null
  let activeRequestId = 0

  const data = ref<TResult | null>(null) as Ref<TResult | null>
  const isLoading = ref<boolean>(false)
  const error = ref<Error | null>(null)

  function cancel() {
    if (activeController) {
      activeController.abort('Request canceled by user')
      activeController = null
    }
  }

  async function execute(params: TParams): Promise<TResult | null> {
    // 1. Hủy request cũ đang thực thi
    if (activeController) {
      activeController.abort('Superceded by new request')
    }

    // 2. Tạo controller và gắn ID duy nhất cho request mới
    const controller = new AbortController()
    activeController = controller
    const currentRequestId = ++activeRequestId

    isLoading.value = true
    error.value = null

    try {
      const result = await queryFn(params, controller.signal)

      // 3. Stale Response Protection: Chỉ nhận kết quả nếu đây là request mới nhất
      if (currentRequestId === activeRequestId) {
        data.value = result
        return result
      }
      return null
    } catch (err: unknown) {
      // 4. Nếu request bị cancel có chủ đích -> Không xem là kết quả rỗng thành công, cũng không xem là lỗi hệ thống
      if (axios.isCancel(err) || (err instanceof Error && err.name === 'AbortError')) {
        return null
      }
      // Chỉ gán lỗi nếu request này vẫn là request hiện hành
      if (currentRequestId === activeRequestId) {
        error.value = err instanceof Error ? err : new Error(String(err))
      }
      throw err
    } finally {
      // 5. Tránh lỗi kinh điển: Chỉ gỡ bỏ controller và tắt loading nếu activeController chưa bị thay thế
      if (activeController === controller) {
        activeController = null
        isLoading.value = false
      }
    }
  }

  // 6. Cleanup on unmount
  onBeforeUnmount(() => {
    cancel()
  })

  return { data, isLoading, error, execute, cancel }
}
