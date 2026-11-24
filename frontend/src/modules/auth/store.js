import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from './api'

/**
 * Giải mã Base64URL JWT payload an toàn chống lỗi cú pháp và kiểm tra hạn sử dụng
 */
export function parseJwt(token) {
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

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('velobus_access_token') || '')
  let initialUser = null
  try {
    initialUser = JSON.parse(localStorage.getItem('velobus_user') || 'null')
  } catch (e) {
    initialUser = null
  }
  const user = ref(initialUser)

  // Single Source of Truth cho JWT payload
  const jwtPayload = computed(() => parseJwt(token.value))
  const isAuthenticated = computed(() => Boolean(token.value && jwtPayload.value))

  // Trích xuất vai trò TUYỆT ĐỐI từ JWT payload (chống sửa đổi localStorage)
  const role = computed(() => {
    if (!isAuthenticated.value) return 'GUEST'
    return jwtPayload.value?.role || 'GUEST'
  })

  const isAdmin = computed(() => role.value === 'ROLE_ADMIN')
  const isOperator = computed(() => role.value === 'ROLE_OPERATOR')
  const isCustomer = computed(() => role.value === 'ROLE_CUSTOMER')

  function getHomeRoute() {
    if (isAdmin.value) return '/admin'
    if (isOperator.value) return '/operator'
    return '/'
  }

  async function login(username, password) {
    const res = await authApi.login({ username, password })
    if (res?.data?.accessToken) {
      const payload = parseJwt(res.data.accessToken)
      if (!payload) {
        throw new Error('Mã xác thực từ máy chủ không hợp lệ hoặc đã hết hạn')
      }
      token.value = res.data.accessToken
      user.value = {
        username: res.data.username || payload?.sub || username,
        role: payload?.role || 'ROLE_CUSTOMER',
      }
      localStorage.setItem('velobus_access_token', token.value)
      localStorage.setItem('velobus_user', JSON.stringify(user.value))
      return res
    }
    throw new Error(res?.message || 'Không nhận được mã xác thực hợp lệ')
  }

  async function register(payload) {
    const res = await authApi.register(payload)
    if (res?.data?.accessToken) {
      const jwtData = parseJwt(res.data.accessToken)
      if (!jwtData) {
        throw new Error('Mã xác thực từ máy chủ không hợp lệ')
      }
      token.value = res.data.accessToken
      user.value = {
        username: res.data.username || jwtData?.sub || payload.username,
        role: jwtData?.role || 'ROLE_CUSTOMER',
      }
      localStorage.setItem('velobus_access_token', token.value)
      localStorage.setItem('velobus_user', JSON.stringify(user.value))
      return res
    }
    throw new Error(res?.message || 'Không nhận được mã xác thực hợp lệ')
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('velobus_access_token')
    localStorage.removeItem('velobus_user')
  }

  return {
    token,
    user,
    isAuthenticated,
    role,
    isAdmin,
    isOperator,
    isCustomer,
    getHomeRoute,
    login,
    register,
    logout,
  }
})
