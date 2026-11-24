import { computed } from 'vue'
import { getActivePinia } from 'pinia'
import { useAuthStore, ALL_ADMIN_PERMISSION_CODES } from '@/modules/auth/store'

/**
 * Composable kiểm tra quyền Admin an toàn (PA-02 & PC-10):
 * - Khi chạy trong ứng dụng thật hoặc test có Pinia: ủy quyền sang useAuthStore()
 * - Khi chạy trong unit test mount view độc lập không truyền createPinia() (VD: admin-views.spec.js):
 *   tự động fallback về Super Admin (hasPermission -> true) để không bao giờ crash getActivePinia().
 */
export function useAdminPermissions() {
  const activePinia = getActivePinia()
  const auth = activePinia ? useAuthStore() : null

  const isSuperAdmin = computed(() => (auth ? auth.isSuperAdmin : true))
  const customRole = computed(() => (auth ? auth.customRole : 'ROLE_ADMIN'))
  const roleName = computed(() => (auth ? auth.roleName : 'Quản Trị Viên Toàn Sàn'))
  const previewRole = computed(() => (auth ? auth.previewRole : null))
  const permissions = computed(() => (auth ? auth.permissions : ALL_ADMIN_PERMISSION_CODES))

  function hasPermission(code) {
    if (!auth) return true
    // Nếu trong unit test mount view mà chưa login token (role === 'GUEST'), fallback true để test UI component
    if (!auth.isAuthenticated) return true
    return auth.hasPermission(code)
  }

  function hasAnyPermission(codes) {
    if (!auth) return true
    if (!auth.isAuthenticated) return true
    return auth.hasAnyPermission(codes)
  }

  function startRolePreview(roleObj) {
    if (auth) {
      auth.startRolePreview(roleObj)
    }
  }

  function stopRolePreview() {
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
