/**
 * Kiểm tra và phân giải đường dẫn an toàn chống Open Redirect và kiểm tra quyền của role
 */
export function resolveSafeRedirect(rawRedirect, role, routerInstance) {
  if (!rawRedirect || typeof rawRedirect !== 'string') return null

  // Chặn ký tự điều khiển (ASCII control chars) và ký tự backslash
  if (/[\x00-\x1f\x7f\\]/.test(rawRedirect)) return null

  // Chặn tuyệt đối Open Redirect (chỉ chấp nhận path nội bộ /...)
  if (!/^\/(?!\/)/.test(rawRedirect)) return null

  // Loại trừ route auth-login, forbidden và các tham số login
  if (rawRedirect === '/login' || rawRedirect === '/403' || rawRedirect.startsWith('/login?') || rawRedirect.startsWith('/login#')) {
    return null
  }

  const resolved = routerInstance.resolve(rawRedirect)
  if (!resolved || !resolved.matched || resolved.matched.length === 0) return null
  if (resolved.name === 'not-found' || resolved.name === 'forbidden' || resolved.name === 'auth-login') return null

  // Trích xuất roles yêu cầu từ route record
  const requiredRoles = [...resolved.matched].reverse().find(r => r.meta?.roles)?.meta?.roles
  if (requiredRoles && Array.isArray(requiredRoles)) {
    if (!requiredRoles.includes(role)) {
      return null // Role vừa đăng nhập không đủ quyền vào trang này
    }
  }

  return resolved.fullPath
}
