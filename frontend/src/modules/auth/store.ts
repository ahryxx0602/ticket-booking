import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from './api'
import { setHttpClientPreviewRole } from '@/shared/api/httpClient'

/**
 * Danh mục chuẩn 7 Modules & 26 API Permissions của hệ thống Quản trị (/admin)
 * Đồng bộ 1:1 với AdminPermissionCatalog.java ở Backend
 */
export const ADMIN_PERMISSION_MODULES = [
  {
    moduleCode: 'MOD_DASHBOARD',
    moduleName: '1. Tổng quan & Đối soát',
    routePath: '/admin',
    viewPermissionCode: 'PAYMENT_REPORT_VIEW',
    permissions: [
      { code: 'PAYMENT_REPORT_VIEW', name: 'Xem báo cáo KPI doanh thu', httpMethod: 'GET', apiPattern: '/api/v1/admin/payments/reports', description: 'Xem 4 thẻ KPI GMV, hoa hồng, tổng đơn vé và nhà xe', viewPermission: true },
      { code: 'PAYMENT_LIST_VIEW', name: 'Xem danh sách giao dịch & đối soát', httpMethod: 'GET', apiPattern: '/api/v1/admin/payments', description: 'Xem bảng lịch sử giao dịch và cảnh báo lệch số dư VNPay', viewPermission: true },
      { code: 'PAYMENT_RECONCILE_RESOLVE', name: 'Xử lý cảnh báo đối soát', httpMethod: 'POST', apiPattern: '/api/v1/admin/payments/*/reconcile', description: 'Xác nhận đã xử lý cảnh báo lệch đối soát VNPay ↔ Escrow', viewPermission: false }
    ]
  },
  {
    moduleCode: 'MOD_OPERATORS',
    moduleName: '2. Nhà xe & Hoa hồng',
    routePath: '/admin/operators',
    viewPermissionCode: 'OPERATOR_VIEW',
    permissions: [
      { code: 'OPERATOR_VIEW', name: 'Xem danh sách đối tác nhà xe', httpMethod: 'GET', apiPattern: '/api/v1/admin/partners/operators*', description: 'Hiển thị menu Nhà xe & xem danh sách hồ sơ đối tác vận tải', viewPermission: true },
      { code: 'OPERATOR_CREATE_UPDATE', name: 'Thêm / Sửa / Xóa hồ sơ nhà xe', httpMethod: 'POST|PUT|DELETE', apiPattern: '/api/v1/admin/partners/operators*', description: 'Tạo mới, cập nhật thông tin hoặc xóa hồ sơ đối tác nhà xe', viewPermission: false },
      { code: 'OPERATOR_APPROVE_REJECT', name: 'Phê duyệt / Từ chối nhà xe', httpMethod: 'PUT', apiPattern: '/api/v1/admin/partners/operators/*/approve|reject', description: 'Thẩm định duyệt hoặc từ chối hồ sơ đăng ký nhà xe mới', viewPermission: false },
      { code: 'OPERATOR_SUSPEND', name: 'Đình chỉ / Mở khóa nhà xe', httpMethod: 'PUT', apiPattern: '/api/v1/admin/partners/operators/*/suspend', description: 'Tạm đình chỉ hoặc kích hoạt lại hoạt động của nhà xe', viewPermission: false },
      { code: 'OPERATOR_COMMISSION_MANAGE', name: 'Cấu hình hoa hồng 2 cấp', httpMethod: 'PUT', apiPattern: '/api/v1/admin/partners/operators/*/commission', description: 'Thiết lập tỷ lệ chiết khấu hoa hồng riêng cho từng nhà xe', viewPermission: false }
    ]
  },
  {
    moduleCode: 'MOD_WITHDRAWALS',
    moduleName: '3. Giải ngân Ví Escrow',
    routePath: '/admin/withdrawals',
    viewPermissionCode: 'WITHDRAWAL_VIEW',
    permissions: [
      { code: 'WITHDRAWAL_VIEW', name: 'Xem danh sách lệnh rút tiền', httpMethod: 'GET', apiPattern: '/api/v1/admin/partners/withdrawals*', description: 'Hiển thị menu Giải ngân & xem các yêu cầu rút tiền ví ký quỹ', viewPermission: true },
      { code: 'WITHDRAWAL_APPROVE', name: 'Phê duyệt giải ngân', httpMethod: 'PUT', apiPattern: '/api/v1/admin/partners/withdrawals/*/approve', description: 'Duyệt chi chuyển khoản và khấu trừ số dư Locked Balance', viewPermission: false },
      { code: 'WITHDRAWAL_REJECT', name: 'Từ chối lệnh rút tiền', httpMethod: 'PUT', apiPattern: '/api/v1/admin/partners/withdrawals/*/reject', description: 'Từ chối lệnh rút và hoàn trả số dư về ví khả dụng của nhà xe', viewPermission: false }
    ]
  },
  {
    moduleCode: 'MOD_BANNERS',
    moduleName: '4. Banner & Truyền thông',
    routePath: '/admin/banners',
    viewPermissionCode: 'BANNER_VIEW',
    permissions: [
      { code: 'BANNER_VIEW', name: 'Xem danh sách banner toàn sàn', httpMethod: 'GET', apiPattern: '/api/v1/admin/partners/banners*', description: 'Hiển thị menu Banner & xem danh sách chiến dịch quảng cáo', viewPermission: true },
      { code: 'BANNER_CREATE_DELETE', name: 'Tạo / Sửa / Xóa banner sàn', httpMethod: 'POST|PUT|DELETE', apiPattern: '/api/v1/admin/partners/banners*', description: 'Đăng mới, chỉnh sửa hoặc xóa banner truyền thông của sàn', viewPermission: false },
      { code: 'BANNER_APPROVE_REJECT', name: 'Phê duyệt / Gỡ bỏ banner nhà xe', httpMethod: 'PUT', apiPattern: '/api/v1/admin/partners/banners/*/approve|reject', description: 'Duyệt hiển thị hoặc từ chối banner do nhà xe gửi lên', viewPermission: false }
    ]
  },
  {
    moduleCode: 'MOD_IAM',
    moduleName: '5. Người dùng & Phân quyền RBAC',
    routePath: '/admin/users-bookings',
    viewPermissionCode: 'USER_VIEW',
    permissions: [
      { code: 'USER_VIEW', name: 'Xem 3 phân mục người dùng', httpMethod: 'GET', apiPattern: '/api/v1/admin/users*', description: 'Xem danh sách Khách hàng (Customer), Chủ xe (Operator) và Hệ thống (Admin)', viewPermission: true },
      { code: 'USER_CREATE_UPDATE_DELETE', name: 'Tạo / Sửa / Xóa tài khoản', httpMethod: 'POST|PUT|DELETE', apiPattern: '/api/v1/admin/users*', description: 'Tạo mới, cập nhật hồ sơ hoặc xóa tài khoản người dùng', viewPermission: false },
      { code: 'USER_STATUS_LOCK', name: 'Khóa / Mở khóa tài khoản', httpMethod: 'PATCH', apiPattern: '/api/v1/admin/users/*/status', description: 'Khóa hoặc kích hoạt lại trạng thái hoạt động của tài khoản', viewPermission: false },
      { code: 'USER_PASSWORD_RESET', name: 'Đặt lại mật khẩu người dùng', httpMethod: 'POST', apiPattern: '/api/v1/admin/users/*/reset-password', description: 'Cấp lại mật khẩu mới cho tài khoản người dùng', viewPermission: false },
      { code: 'USER_ROLE_ASSIGN', name: 'Gán / Thay đổi Role cho người dùng', httpMethod: 'PATCH', apiPattern: '/api/v1/admin/users/*/role', description: 'Gán hoặc chuyển đổi Role cho tài khoản người dùng', viewPermission: false },
      { code: 'ROLE_MANAGE_CRUD', name: 'Quản lý Role & Phân quyền API', httpMethod: 'GET|POST|PUT|DELETE', apiPattern: '/api/v1/admin/roles*', description: 'Tạo mới, sửa ma trận 26 quyền API và xóa Custom Role hệ thống', viewPermission: true }
    ]
  },
  {
    moduleCode: 'MOD_BOOKINGS',
    moduleName: '6. Đơn vé & Hoàn tiền Admin',
    routePath: '/admin/users-bookings',
    viewPermissionCode: 'BOOKING_VIEW',
    permissions: [
      { code: 'BOOKING_VIEW', name: 'Xem danh sách đơn vé toàn sàn', httpMethod: 'GET', apiPattern: '/api/v1/admin/bookings*', description: 'Tra cứu toàn bộ đơn đặt vé của hành khách trên hệ thống', viewPermission: true },
      { code: 'BOOKING_FORCE_REFUND', name: 'Hoàn tiền cưỡng chế (Admin Refund)', httpMethod: 'POST', apiPattern: '/api/v1/admin/bookings/*/refund', description: 'Thực thi lệnh hoàn tiền 100% và thu hồi ví ký quỹ khi có khiếu nại', viewPermission: false }
    ]
  },
  {
    moduleCode: 'MOD_SETTINGS',
    moduleName: '7. Bến xe, Chuyến xe & Cấu hình',
    routePath: '/admin/settings',
    viewPermissionCode: 'STATION_VIEW',
    permissions: [
      { code: 'STATION_VIEW', name: 'Xem danh mục bến xe liên tỉnh', httpMethod: 'GET', apiPattern: '/api/v1/admin/stations*', description: 'Hiển thị menu Cấu hình & xem danh sách nhà ga, bến xe', viewPermission: true },
      { code: 'STATION_CREATE_UPDATE_DELETE', name: 'Thêm / Sửa / Xóa bến xe', httpMethod: 'POST|PUT|DELETE', apiPattern: '/api/v1/admin/stations*', description: 'Thêm mới, cập nhật hoặc xóa bến xe liên tỉnh', viewPermission: false },
      { code: 'TRIP_MANAGE_CRUD', name: 'Quản lý chuyến xe (Tạo/Sửa/Hủy/Xóa)', httpMethod: 'GET|POST|PUT|PATCH|DELETE', apiPattern: '/api/v1/admin/trips*', description: 'Tạo chuyến xe, cập nhật giá/lịch trình, hủy hoặc xóa chuyến xe', viewPermission: true },
      { code: 'SYSTEM_SETTINGS_MANAGE', name: 'Quản lý cấu hình tham số toàn sàn', httpMethod: 'GET|PUT', apiPattern: '/api/v1/admin/settings & /commission/global', description: 'Cập nhật hoa hồng mặc định, TTL khóa ghế Redis và tham số vận hành', viewPermission: true }
    ]
  }
]

export const ALL_ADMIN_PERMISSION_CODES = ADMIN_PERMISSION_MODULES.flatMap(m => m.permissions.map(p => p.code))

/**
 * Giải mã Base64URL JWT payload an toàn chống lỗi cú pháp và kiểm tra hạn sử dụng
 */
export function parseJwt(token: string | null | undefined): Record<string, any> | null {
  if (!token || typeof token !== 'string') return null
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    base64 = base64.padEnd(base64.length + (4 - (base64.length % 4)) % 4, '=')
    
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    const payload = JSON.parse(jsonPayload)

    // Xác thực cấu trúc payload là plain object hợp lệ
    if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
      return null
    }

    // Kiểm tra thời hạn token chặt chẽ (bảo vệ chống exp = 0, exp không phải number, exp quá khứ)
    if (payload.exp !== undefined) {
      if (typeof payload.exp !== 'number' || Number.isNaN(payload.exp) || payload.exp * 1000 <= Date.now()) {
        return null
      }
    }

    return payload
  } catch (e) {
    return null
  }
}

function safeGetStorage(key: string): string | null {
  try {
    if (typeof localStorage !== 'undefined' && typeof localStorage.getItem === 'function') {
      return localStorage.getItem(key)
    }
  } catch (_) {}
  return null
}

function safeSetStorage(key: string, val: string): void {
  try {
    if (typeof localStorage !== 'undefined' && typeof localStorage.setItem === 'function') {
      localStorage.setItem(key, val)
    }
  } catch (_) {}
}

function safeRemoveStorage(key: string): void {
  try {
    if (typeof localStorage !== 'undefined' && typeof localStorage.removeItem === 'function') {
      localStorage.removeItem(key)
    }
  } catch (_) {}
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(safeGetStorage('velobus_access_token') || '')
  let initialUser: any = null
  try {
    initialUser = JSON.parse(safeGetStorage('velobus_user') || 'null')
  } catch (e) {
    initialUser = null
  }
  const user = ref<any>(initialUser)

  // State phục vụ chế độ "⚡ Xem thử giao diện với Role này (Preview Role)" của Super Admin (FE-STATE-002: Memory-Only)
  const previewRole = ref<any>(null)
  setHttpClientPreviewRole(null)
  safeRemoveStorage('velobus_preview_role')

  // FE-SEC-002, FE-SEC-003 & FE-STATE-001: Quyền thực tế đồng bộ từ GET /api/v1/auth/me kèm Single-Flight & Session Epoch Guard
  const liveProfile = ref<any>(null)
  const permissionCatalog = ref<any[]>(ADMIN_PERMISSION_MODULES)
  let sessionEpoch = 0
  let inflightRefreshPromise: Promise<any> | null = null

  // Single Source of Truth cho JWT payload
  const jwtPayload = computed(() => parseJwt(token.value))
  const isAuthenticated = computed<boolean>(() => Boolean(token.value && jwtPayload.value))

  // Trích xuất vai trò ưu tiên từ liveProfile (nếu đã gọi /auth/me) hoặc JWT payload
  const role = computed<string>(() => {
    if (!isAuthenticated.value) return 'GUEST'
    return liveProfile.value?.role || jwtPayload.value?.role || 'GUEST'
  })

  const customRole = computed<string>(() => {
    if (!isAuthenticated.value) return 'GUEST'
    if (previewRole.value?.code) return previewRole.value.code
    return liveProfile.value?.customRole || jwtPayload.value?.customRole || role.value
  })

  const roleName = computed<string>(() => {
    if (!isAuthenticated.value) return 'Khách vãng lai'
    if (previewRole.value?.name) return previewRole.value.name
    return liveProfile.value?.roleName || jwtPayload.value?.roleName || customRole.value
  })

  const isAdmin = computed<boolean>(() => role.value === 'ROLE_ADMIN')
  const isOperator = computed<boolean>(() => role.value === 'ROLE_OPERATOR')
  const isCustomer = computed<boolean>(() => role.value === 'ROLE_CUSTOMER')

  /**
   * PC-01: Chỉ coi là Super Admin khi role === 'ROLE_ADMIN', không ở chế độ preview,
   * và (customRole không khai báo hoặc customRole === 'ROLE_ADMIN').
   * Nếu customRole khác 'ROLE_ADMIN' (VD: ROLE_INTERN có permissions = []), tuyệt đối KHÔNG phải Super Admin!
   */
  const isSuperAdmin = computed<boolean>(() => {
    if (!isAdmin.value) return false
    if (previewRole.value) return previewRole.value.code === 'ROLE_ADMIN'
    const claimCustomRole = liveProfile.value?.customRole || jwtPayload.value?.customRole
    return !claimCustomRole || claimCustomRole === 'ROLE_ADMIN'
  })

  const permissions = computed<string[]>(() => {
    if (!isAdmin.value) return []
    if (isSuperAdmin.value) return ALL_ADMIN_PERMISSION_CODES
    if (previewRole.value) {
      return Array.isArray(previewRole.value.permissions) ? previewRole.value.permissions : []
    }
    if (Array.isArray(liveProfile.value?.permissions)) {
      return liveProfile.value.permissions
    }
    const claimPerms = jwtPayload.value?.permissions
    return Array.isArray(claimPerms) ? claimPerms : []
  })

  function hasPermission(code?: string | null): boolean {
    if (!isAdmin.value || !code || typeof code !== 'string') return false
    if (isSuperAdmin.value) return true
    return permissions.value.includes(code)
  }

  function hasAnyPermission(codes?: string[] | null): boolean {
    if (!isAdmin.value || !codes || !Array.isArray(codes) || codes.length === 0) return false
    if (isSuperAdmin.value) return true
    return codes.some(c => permissions.value.includes(c))
  }

  function startRolePreview(targetRole: any): void {
    if (!targetRole) return
    previewRole.value = {
      code: targetRole.code,
      name: targetRole.name,
      permissions: Array.isArray(targetRole.permissions) ? [...targetRole.permissions] : []
    }
    // FE-SEC-005 & FE-STATE-002: Memory-Only Preview (không ghi xuống localStorage để tránh lỗi kẹt chặn ghi sau khi F5)
    setHttpClientPreviewRole(targetRole.code)
  }

  function stopRolePreview(): void {
    previewRole.value = null
    setHttpClientPreviewRole(null)
    safeRemoveStorage('velobus_preview_role')
  }

  /**
   * FE-SEC-002, BE-SEC-007 & FE-STATE-001:
   * Đồng bộ lại thông tin role & permissions mới nhất từ GET /api/v1/auth/me:
   * - Single-Flight Deduplication: nhiều sự kiện 403 đồng thời chỉ dùng chung 1 request /auth/me.
   * - Session Epoch Guard: nếu user logout hoặc đổi tài khoản khi request đang bay, phản hồi cũ sẽ bị bỏ qua.
   * - Fixed-Expiry Session: không phát hành hay ghi đè accessToken.
   */
  async function refreshProfile(): Promise<any> {
    if (!isAuthenticated.value) return null
    if (inflightRefreshPromise) {
      return inflightRefreshPromise
    }

    const capturedEpoch = sessionEpoch
    const capturedToken = token.value

    inflightRefreshPromise = (async () => {
      try {
        const res = await authApi.getMe()
        if (capturedEpoch !== sessionEpoch || capturedToken !== token.value || !isAuthenticated.value) {
          return null
        }
        if (res?.data) {
          liveProfile.value = {
            role: res.data.role,
            customRole: res.data.customRole,
            roleName: res.data.roleName,
            permissions: Array.isArray(res.data.permissions) ? res.data.permissions : []
          }
          return liveProfile.value
        }
      } catch (_) {
        // Giữ nguyên state hiện tại nếu mất mạng tạm thời; nếu 401 thì httpClient đã tự động logout
      } finally {
        inflightRefreshPromise = null
      }
      return null
    })()

    return inflightRefreshPromise
  }

  /**
   * FE-SEC-003: Đồng bộ danh mục 7 Modules x 26 Permissions từ Backend API
   */
  function syncPermissionCatalog(modulesFromApi: any): void {
    if (Array.isArray(modulesFromApi) && modulesFromApi.length > 0) {
      permissionCatalog.value = modulesFromApi
    }
  }

  /**
   * PC-07: Tìm trang /admin/* đầu tiên mà nhân sự hệ thống có quyền xem.
   * Nếu Custom Role không có bất kỳ quyền xem nào -> trả về '/403' để tránh vòng lặp redirect vô tận.
   */
  function getHomeRoute(): string {
    if (isAdmin.value) {
      if (isSuperAdmin.value) return '/admin'
      const adminRouteCandidates = [
        { path: '/admin', perms: ['PAYMENT_REPORT_VIEW', 'PAYMENT_LIST_VIEW'] },
        { path: '/admin/operators', perms: ['OPERATOR_VIEW'] },
        { path: '/admin/withdrawals', perms: ['WITHDRAWAL_VIEW'] },
        { path: '/admin/banners', perms: ['BANNER_VIEW'] },
        { path: '/admin/users-bookings', perms: ['USER_VIEW', 'ROLE_MANAGE_CRUD', 'BOOKING_VIEW'] },
        { path: '/admin/settings', perms: ['STATION_VIEW', 'TRIP_MANAGE_CRUD', 'SYSTEM_SETTINGS_MANAGE'] }
      ]
      const matched = adminRouteCandidates.find(r => hasAnyPermission(r.perms))
      return matched ? matched.path : '/403'
    }
    if (isOperator.value) return '/operator'
    return '/'
  }

  async function login(username: string, password: string): Promise<any> {
    const res = await authApi.login({ username, password })
    if (res?.data?.accessToken) {
      const payload = parseJwt(res.data.accessToken)
      if (!payload) {
        throw new Error('Mã xác thực từ máy chủ không hợp lệ hoặc đã hết hạn')
      }
      sessionEpoch++
      inflightRefreshPromise = null
      previewRole.value = null
      setHttpClientPreviewRole(null)
      liveProfile.value = null
      token.value = res.data.accessToken
      user.value = {
        username: res.data.username || payload?.sub || username,
        role: payload?.role || 'ROLE_CUSTOMER',
        customRole: res.data.customRole || payload?.customRole || payload?.role || 'ROLE_CUSTOMER',
        roleName: res.data.roleName || payload?.roleName || payload?.role || 'ROLE_CUSTOMER'
      }
      safeSetStorage('velobus_access_token', token.value)
      safeSetStorage('velobus_user', JSON.stringify(user.value))
      return res
    }
    throw new Error(res?.data?.message || (res as any)?.message || 'Không nhận được mã xác thực hợp lệ')
  }

  async function register(payload: any): Promise<any> {
    const res = await authApi.register(payload)
    if (res?.data?.accessToken) {
      const jwtData = parseJwt(res.data.accessToken)
      if (!jwtData) {
        throw new Error('Mã xác thực từ máy chủ không hợp lệ')
      }
      sessionEpoch++
      inflightRefreshPromise = null
      previewRole.value = null
      setHttpClientPreviewRole(null)
      liveProfile.value = null
      token.value = res.data.accessToken
      user.value = {
        username: res.data.username || jwtData?.sub || payload.username,
        role: jwtData?.role || 'ROLE_CUSTOMER',
        customRole: jwtData?.customRole || 'ROLE_CUSTOMER',
        roleName: jwtData?.roleName || 'Khách Hàng Đặt Vé'
      }
      safeSetStorage('velobus_access_token', token.value)
      safeSetStorage('velobus_user', JSON.stringify(user.value))
      return res
    }
    throw new Error(res?.data?.message || (res as any)?.message || 'Không nhận được mã xác thực hợp lệ')
  }

  function logout(): void {
    sessionEpoch++
    inflightRefreshPromise = null
    token.value = ''
    user.value = null
    previewRole.value = null
    liveProfile.value = null
    setHttpClientPreviewRole(null)
    safeRemoveStorage('velobus_access_token')
    safeRemoveStorage('velobus_user')
    safeRemoveStorage('velobus_preview_role')
  }

  // FE-SEC-004 & FE-SEC-002: Lắng nghe sự kiện 401/403 và đồng bộ đăng xuất đa tab qua StorageEvent
  if (typeof window !== 'undefined' && typeof window.addEventListener === 'function') {
    window.addEventListener('velobus:unauthorized', () => {
      logout()
    })
    window.addEventListener('velobus:forbidden', () => {
      refreshProfile()
    })
    window.addEventListener('storage', (event: StorageEvent) => {
      if (event.key === 'velobus_access_token') {
        if (!event.newValue) {
          logout()
        } else if (event.newValue !== token.value) {
          sessionEpoch++
          inflightRefreshPromise = null
          token.value = event.newValue
          liveProfile.value = null
        }
      }
    })
  }

  return {
    token,
    user,
    previewRole,
    liveProfile,
    permissionCatalog,
    isAuthenticated,
    jwtPayload,
    role,
    customRole,
    roleName,
    isAdmin,
    isSuperAdmin,
    isOperator,
    isCustomer,
    permissions,
    hasPermission,
    hasAnyPermission,
    startRolePreview,
    stopRolePreview,
    refreshProfile,
    syncPermissionCatalog,
    getHomeRoute,
    login,
    register,
    logout,
  }
})
