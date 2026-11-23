import httpClient from '@/shared/api/httpClient'

export const partnerApi = {
  registerOperator(payload) {
    return httpClient.post('/partners/register', payload)
  },
  getProfile() {
    return httpClient.get('/partners/profile')
  },
  updateProfile(payload) {
    return httpClient.put('/partners/profile', payload)
  },
  getVehicles() {
    return httpClient.get('/partners/vehicles')
  },
  createVehicle(payload) {
    return httpClient.post('/partners/vehicles', payload)
  },
  updateVehicle(id, payload) {
    return httpClient.put(`/partners/vehicles/${id}`, payload)
  },
  deleteVehicle(id) {
    return httpClient.delete(`/partners/vehicles/${id}`)
  },
  getDrivers() {
    return httpClient.get('/partners/drivers')
  },
  createDriver(payload) {
    return httpClient.post('/partners/drivers', payload)
  },
  createPartnerTrip(payload) {
    return httpClient.post('/partners/trips', payload)
  },
  getWallet() {
    return httpClient.get('/partners/wallet')
  },
  getWalletTransactions() {
    return httpClient.get('/partners/wallet/transactions')
  },
  requestWithdrawal(payload) {
    return httpClient.post('/partners/wallet/withdraw', payload)
  },
  getWithdrawals() {
    return httpClient.get('/partners/wallet/withdrawals')
  },
  getMyBanners() {
    return httpClient.get('/partners/banners')
  },
  createBanner(payload) {
    return httpClient.post('/partners/banners', payload)
  },
}
