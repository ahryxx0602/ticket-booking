import axios from 'axios'

const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

httpClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('velobus_access_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let isDispatchingUnauthorized = false

httpClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      // Không dispatch unauthorized nếu lỗi từ chính form login/register
      const isAuthUrl = error.config?.url?.includes('/auth/login') || error.config?.url?.includes('/auth/register')
      if (!isAuthUrl) {
        localStorage.removeItem('velobus_access_token')
        localStorage.removeItem('velobus_user')

        // Khóa debounce chống storm 401 khi nhiều API gọi song song
        if (!isDispatchingUnauthorized && typeof window !== 'undefined' && window.dispatchEvent) {
          isDispatchingUnauthorized = true
          window.dispatchEvent(new CustomEvent('velobus:unauthorized'))
          setTimeout(() => {
            isDispatchingUnauthorized = false
          }, 1000)
        }
      }
    }
    const payload = error.response?.data || {
      code: 'NETWORK_ERROR',
      message: error.message || 'Không thể kết nối tới máy chủ Backend',
    }
    return Promise.reject(payload)
  }
)

export default httpClient
