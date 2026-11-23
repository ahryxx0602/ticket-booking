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

httpClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('velobus_access_token')
      localStorage.removeItem('velobus_user')
    }
    const payload = error.response?.data || {
      code: 'NETWORK_ERROR',
      message: error.message || 'Không thể kết nối tới máy chủ Backend',
    }
    return Promise.reject(payload)
  }
)

export default httpClient
