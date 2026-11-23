import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from './api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('velobus_access_token') || '')
  const user = ref(JSON.parse(localStorage.getItem('velobus_user') || 'null'))

  const isAuthenticated = computed(() => Boolean(token.value))
  const role = computed(() => user.value?.role || 'GUEST')
  const isAdmin = computed(() => role.value === 'ROLE_ADMIN')
  const isOperator = computed(() => role.value === 'ROLE_OPERATOR')
  const isCustomer = computed(() => role.value === 'ROLE_CUSTOMER')

  async function login(username, password) {
    const res = await authApi.login({ username, password })
    if (res?.data?.accessToken) {
      token.value = res.data.accessToken
      user.value = {
        username: res.data.username,
        role: res.data.role,
      }
      localStorage.setItem('velobus_access_token', token.value)
      localStorage.setItem('velobus_user', JSON.stringify(user.value))
    }
    return res
  }

  async function register(payload) {
    const res = await authApi.register(payload)
    if (res?.data?.accessToken) {
      token.value = res.data.accessToken
      user.value = {
        username: res.data.username,
        role: res.data.role,
      }
      localStorage.setItem('velobus_access_token', token.value)
      localStorage.setItem('velobus_user', JSON.stringify(user.value))
    }
    return res
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
    login,
    register,
    logout,
  }
})
