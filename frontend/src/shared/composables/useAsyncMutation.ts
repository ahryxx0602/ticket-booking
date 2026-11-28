import { ref, type Ref } from 'vue'

export interface UseAsyncMutationReturn<TPayload, TResult> {
  isSubmitting: Ref<boolean>
  error: Ref<Error | null>
  execute: (payload: TPayload) => Promise<TResult>
}

/**
 * Composable thực thi tác vụ ghi dữ liệu (Mutation) có bảo vệ chống Double Submit.
 * - Non-idempotent: Tuyệt đối không tự động retry khi gặp lỗi mạng.
 * - Khóa chặt isSubmitting trong toàn bộ vòng đời tác vụ.
 */
export function useAsyncMutation<TPayload, TResult>(
  mutationFn: (payload: TPayload) => Promise<TResult>
): UseAsyncMutationReturn<TPayload, TResult> {
  const isSubmitting = ref<boolean>(false)
  const error = ref<Error | null>(null)

  async function execute(payload: TPayload): Promise<TResult> {
    if (isSubmitting.value) {
      throw new Error('Thao tác đang được xử lý, vui lòng không bấm liên tục')
    }

    isSubmitting.value = true
    error.value = null

    try {
      const result = await mutationFn(payload)
      return result
    } catch (err: unknown) {
      error.value = err instanceof Error ? err : new Error(String(err))
      throw err
    } finally {
      isSubmitting.value = false
    }
  }

  return { isSubmitting, error, execute }
}
