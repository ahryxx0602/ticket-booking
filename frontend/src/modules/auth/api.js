import httpClient from '@/shared/api/httpClient'

export const authApi = {
  login(credentials) {
    return httpClient.post('/auth/login', credentials)
  },
  register(payload) {
    return httpClient.post('/auth/register', payload)
  },
  getMe() {
    return httpClient.get('/auth/me')
  },
  generateTelegramOtp() {
    return httpClient.post('/notifications/telegram/link-token')
  },
}
