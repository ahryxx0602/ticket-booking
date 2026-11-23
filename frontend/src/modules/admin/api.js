import httpClient from '@/shared/api/httpClient'

export const adminApi = {
  // 1. Operators Governance & Commission
  getOperators(status) {
    return httpClient.get('/admin/partners/operators', { params: status ? { status } : {} })
  },
  approveOperator(id) {
    return httpClient.put(`/admin/partners/operators/${id}/approve`)
  },
  rejectOperator(id, note) {
    return httpClient.put(`/admin/partners/operators/${id}/reject`, { note })
  },
  suspendOperator(id, note) {
    return httpClient.put(`/admin/partners/operators/${id}/suspend`, { note })
  },
  updateOperatorCommission(id, commissionRate) {
    return httpClient.put(`/admin/partners/operators/${id}/commission`, { commissionRate })
  },
  updateGlobalCommission(commissionRate) {
    return httpClient.put('/admin/partners/commission/global', { commissionRate })
  },
  getGlobalCommission() {
    return httpClient.get('/admin/partners/commission/global')
  },

  // 2. Withdrawals
  getWithdrawals(status) {
    return httpClient.get('/admin/partners/withdrawals', { params: status ? { status } : {} })
  },
  approveWithdrawal(id, note) {
    return httpClient.put(`/admin/partners/withdrawals/${id}/approve`, { note })
  },
  rejectWithdrawal(id, note) {
    return httpClient.put(`/admin/partners/withdrawals/${id}/reject`, { note })
  },

  // 3. Banners
  getBanners(status) {
    return httpClient.get('/admin/partners/banners', { params: status ? { status } : {} })
  },
  approveBanner(id) {
    return httpClient.put(`/admin/partners/banners/${id}/approve`)
  },
  rejectBanner(id, note) {
    return httpClient.put(`/admin/partners/banners/${id}/reject`, { note })
  },
  createPlatformBanner(payload) {
    return httpClient.post('/admin/partners/banners/platform', payload)
  },

  // 4. Financial & Payments Reports
  getPaymentReport(startDate, endDate) {
    return httpClient.get('/admin/payments/reports', { params: { startDate, endDate } })
  },
  getPayments(params) {
    return httpClient.get('/admin/payments', { params })
  },

  // 5. User Management
  getUsers(params) {
    return httpClient.get('/admin/users', { params })
  },
  getUserDetail(id) {
    return httpClient.get(`/admin/users/${id}`)
  },
  updateUserStatus(id, status) {
    return httpClient.patch(`/admin/users/${id}/status`, { status })
  },
  changeUserRole(id, role) {
    return httpClient.patch(`/admin/users/${id}/role`, { role })
  },
  resetUserPassword(id, newPassword) {
    return httpClient.post(`/admin/users/${id}/reset-password`, { newPassword })
  },

  // 6. Bookings & Admin Refund
  getBookings(params) {
    return httpClient.get('/admin/bookings', { params })
  },
  refundBooking(bookingReference, payload) {
    return httpClient.post(`/admin/bookings/${bookingReference}/refund`, payload)
  },

  // 7. Stations & Trips Management
  createStation(payload) {
    return httpClient.post('/admin/stations', payload)
  },
  createTrip(payload) {
    return httpClient.post('/admin/trips', payload)
  },
  cancelTrip(id) {
    return httpClient.patch(`/admin/trips/${id}/cancel`)
  },
}
