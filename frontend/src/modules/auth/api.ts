import httpClient from '@/shared/api/httpClient'
import type { LoginCredentials, RegisterPayload } from '@/types/auth'

export const authApi = {
  login(credentials: LoginCredentials | Record<string, any>) {
    return httpClient.post('/auth/login', credentials)
  },
  register(payload: RegisterPayload | Record<string, any>) {
    return httpClient.post('/auth/register', payload)
  },
  getMe() {
    return httpClient.get('/auth/me')
  },
  generateTelegramOtp() {
    return httpClient.post('/notifications/telegram/link-token')
  },
}
