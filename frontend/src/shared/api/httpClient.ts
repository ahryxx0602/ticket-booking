import axios, { type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'

let activePreviewRoleCode: string | null = null

export function setHttpClientPreviewRole(roleCode: string | null): void {
  activePreviewRoleCode = roleCode || null
}

function safeGetStorage(key: string): string | null {
  try {
    if (typeof localStorage !== 'undefined' && typeof localStorage.getItem === 'function') {
      return localStorage.getItem(key)
    }
  } catch (_) {}
  return null
}

function safeRemoveStorage(key: string): void {
  try {
    if (typeof localStorage !== 'undefined' && typeof localStorage.removeItem === 'function') {
      localStorage.removeItem(key)
    }
  } catch (_) {}
}

/**
 * FE-SEC-004: Kiểm tra hạn sử dụng JWT ngay tại client trước khi đính kèm vào request
 */
export function isJwtTokenExpired(token: string | null | undefined): boolean {
  if (!token || typeof token !== 'string') return true
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return true
    let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    base64 = base64.padEnd(base64.length + (4 - (base64.length % 4)) % 4, '=')
    const payload = JSON.parse(atob(base64))
    if (!payload || typeof payload !== 'object') return true
    if (payload.exp !== undefined) {
      if (typeof payload.exp !== 'number' || Number.isNaN(payload.exp) || payload.exp * 1000 <= Date.now()) {
        return true
      }
    }
    return false
  } catch (_) {
    return true
  }
}

const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

let isDispatchingUnauthorized = false

function dispatchUnauthorizedOnce(): void {
  activePreviewRoleCode = null
  safeRemoveStorage('velobus_access_token')
  safeRemoveStorage('velobus_user')
  safeRemoveStorage('velobus_preview_role')
  if (!isDispatchingUnauthorized && typeof window !== 'undefined' && window.dispatchEvent) {
    isDispatchingUnauthorized = true
    window.dispatchEvent(new CustomEvent('velobus:unauthorized'))
    setTimeout(() => {
      isDispatchingUnauthorized = false
    }, 1000)
  }
}

httpClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  // FE-SEC-005 & FE-STATE-002: Chốt chặn Readonly dựa trên in-memory activePreviewRoleCode (Memory-Only Preview)
  const previewRoleCode = activePreviewRoleCode
  const method = (config.method || 'get').toUpperCase()
  const url = config.url || ''
  if (previewRoleCode && ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method) && /\/admin(\/|$)/.test(url)) {
    return Promise.reject({
      code: 'PREVIEW_MODE_READONLY',
      message: `Đang ở chế độ Xem thử giao diện (${previewRoleCode}). Vui lòng tắt chế độ Xem thử trước khi thực hiện thao tác thay đổi dữ liệu.`,
    })
  }

  const token = safeGetStorage('velobus_access_token')
  if (token) {
    // FE-SEC-004: Nếu token đã hết hạn trên client -> xóa ngay và phát sự kiện unauthorized
    if (isJwtTokenExpired(token)) {
      dispatchUnauthorizedOnce()
      return Promise.reject({
        code: 'TOKEN_EXPIRED',
        message: 'Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại',
      })
    }
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

httpClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      // Không dispatch unauthorized nếu lỗi từ chính form login/register
      const isAuthUrl = error.config?.url?.includes('/auth/login') || error.config?.url?.includes('/auth/register')
      if (!isAuthUrl) {
        dispatchUnauthorizedOnce()
      }
    } else if (error.response?.status === 403 && typeof window !== 'undefined' && window.dispatchEvent) {
      // FE-SEC-002 & FE-STATE-001: Phát sự kiện đồng bộ quyền khi gặp 403, nhưng bỏ qua nếu chính /auth/me trả 403 (chống đệ quy)
      const isMeUrl = error.config?.url?.includes('/auth/me')
      if (!isMeUrl) {
        window.dispatchEvent(new CustomEvent('velobus:forbidden'))
      }
    }
    const payload = error.response?.data || (error.code ? error : {
      code: 'NETWORK_ERROR',
      message: error.message || 'Không thể kết nối tới máy chủ Backend',
    })
    return Promise.reject(payload)
  }
)

export default httpClient
