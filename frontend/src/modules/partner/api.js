import httpClient from '@/shared/api/httpClient'

export const partnerApi = {
  // Operator Profile
  registerOperator(payload) {
    return httpClient.post('/partners/register', payload)
  },
  getProfile() {
    return httpClient.get('/partners/me')
  },
  updateProfile(payload) {
    return httpClient.put('/partners/me', payload)
  },

  // Vehicle Fleet
  getVehicles() {
    return httpClient.get('/partners/vehicles')
  },
  createVehicle(payload) {
    return httpClient.post('/partners/vehicles', payload)
  },
  updateVehicle(id, payload) {
    return httpClient.put(`/partners/vehicles/${id}`, payload)
  },
  setVehicleMaintenance(id) {
    return httpClient.put(`/partners/vehicles/${id}/maintenance`)
  },
  activateVehicle(id) {
    return httpClient.put(`/partners/vehicles/${id}/activate`)
  },

  // Drivers
  getDrivers() {
    return httpClient.get('/partners/drivers')
  },
  createDriver(payload) {
    return httpClient.post('/partners/drivers', payload)
  },
  updateDriver(id, payload) {
    return httpClient.put(`/partners/drivers/${id}`, payload)
  },

  // Trips
  getMyTrips(params) {
    return httpClient.get('/partners/trips', { params })
  },
  createPartnerTrip(payload) {
    return httpClient.post('/partners/trips', payload)
  },

  // Fintech Escrow & Wallet
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

  // Banners
  getMyBanners() {
    return httpClient.get('/partners/banners')
  },
  createBanner(payload) {
    return httpClient.post('/partners/banners', payload)
  },
}
