import { computed } from 'vue'
import { getActivePinia } from 'pinia'
import { useAuthStore } from '@/modules/auth/store'

/**
 * Composable kiểm tra quyền Admin theo chuẩn Fail-Closed (FE-SEC-001):
 * - Ủy quyền sang useAuthStore() khi có active Pinia instance và người dùng đã xác thực.
 * - Nếu chưa khởi tạo Pinia hoặc chưa đăng nhập (!auth || !auth.isAuthenticated):
 *   tuyệt đối Fail-Closed (isSuperAdmin = false, permissions = [], hasPermission = false).
 */
export function useAdminPermissions() {
  const activePinia = getActivePinia()
  const auth = activePinia ? useAuthStore() : null

  const isSuperAdmin = computed(() => (auth && auth.isAuthenticated ? auth.isSuperAdmin : false))
  const customRole = computed(() => (auth && auth.isAuthenticated ? auth.customRole : 'GUEST'))
  const roleName = computed(() => (auth && auth.isAuthenticated ? auth.roleName : 'Khách vãng lai'))
  const previewRole = computed(() => (auth ? auth.previewRole : null))
  const permissions = computed<string[]>(() => (auth && auth.isAuthenticated ? auth.permissions : []))

  function hasPermission(code?: string | null): boolean {
    if (!auth || !auth.isAuthenticated || !code) return false
    return auth.hasPermission(code)
  }

  function hasAnyPermission(codes?: string[] | null): boolean {
    if (!auth || !auth.isAuthenticated || !Array.isArray(codes) || codes.length === 0) return false
    return auth.hasAnyPermission(codes)
  }

  function startRolePreview(roleObj: any): void {
    if (auth && auth.isAuthenticated) {
      auth.startRolePreview(roleObj)
    }
  }

  function stopRolePreview(): void {
    if (auth) {
      auth.stopRolePreview()
    }
  }

  return {
    auth,
    isSuperAdmin,
    customRole,
    roleName,
    previewRole,
    permissions,
    hasPermission,
    hasAnyPermission,
    startRolePreview,
    stopRolePreview
  }
}
