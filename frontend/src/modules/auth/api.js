import httpClient from '@/shared/api/httpClient'

export const authApi = {
  login(credentials) {
    return httpClient.post('/auth/login', credentials)
  },
  register(payload) {
    return httpClient.post('/auth/register', payload)
  },
  generateTelegramOtp() {
    return httpClient.post('/notifications/telegram/link-token')
  },
}
