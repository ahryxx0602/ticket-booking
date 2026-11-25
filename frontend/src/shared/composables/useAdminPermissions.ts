import { computed } from 'vue'
import { useAuthStore } from '@/modules/auth/store'
import type { AdminPermissionCode } from '@/types/rbac'

/**
 * Composable kiểm tra quyền hạn quản trị (RBAC) cho giao diện Admin.
 * Tự động đồng bộ với Role Preview mode của Super Admin.
 */
export function useAdminPermissions() {
  const authStore = useAuthStore()

  const permissions = computed<string[]>(() => authStore.permissions || [])
  const isSuperAdmin = computed<boolean>(() => authStore.role === 'ROLE_ADMIN' && !authStore.previewRole)
  const isPreviewMode = computed<boolean>(() => Boolean(authStore.previewRole))
  const previewRoleCode = computed<string | null>(() => {
    const pr = authStore.previewRole as { code?: string; name?: string } | null
    return pr?.code || null
  })

  function hasPermission(code: AdminPermissionCode): boolean {
    if (typeof authStore.hasPermission === 'function') {
      return authStore.hasPermission(code)
    }
    if (isSuperAdmin.value) return true
    return permissions.value.includes(code)
  }

  function hasAnyPermission(codes: AdminPermissionCode[]): boolean {
    return codes.some(code => hasPermission(code))
  }

  function hasAllPermissions(codes: AdminPermissionCode[]): boolean {
    return codes.every(code => hasPermission(code))
  }

  return {
    permissions,
    isSuperAdmin,
    isPreviewMode,
    previewRoleCode,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
  }
}
