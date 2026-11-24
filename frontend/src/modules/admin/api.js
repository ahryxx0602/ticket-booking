import httpClient from '@/shared/api/httpClient'

export const adminApi = {
  // 1. Operators Governance & Commission
  getOperators(status) {
    return httpClient.get('/admin/partners/operators', { params: status ? { status } : {} })
  },
  createOperator(payload) {
    return httpClient.post('/admin/partners/operators', payload)
  },
  updateOperator(id, payload) {
    return httpClient.put(`/admin/partners/operators/${id}`, payload)
  },
  deleteOperator(id) {
    return httpClient.delete(`/admin/partners/operators/${id}`)
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
  updateBanner(id, payload) {
    return httpClient.put(`/admin/partners/banners/${id}`, payload)
  },
  deleteBanner(id) {
    return httpClient.delete(`/admin/partners/banners/${id}`)
  },

  // 4. Financial & Payments Reports
  getPaymentReport(startDate, endDate) {
    return httpClient.get('/admin/payments/reports', { params: { startDate, endDate } })
  },
  getPayments(params) {
    return httpClient.get('/admin/payments', { params })
  },
  reconcilePayment(id, note) {
    return httpClient.post(`/admin/payments/${id}/reconcile`, { note })
  },

  // 5. User Management (3 Categories: Customer, Operator, System Staff)
  getUsers(params) {
    return httpClient.get('/admin/users', { params })
  },
  getUserDetail(id) {
    return httpClient.get(`/admin/users/${id}`)
  },
  createUser(payload) {
    return httpClient.post('/admin/users', payload)
  },
  updateUser(id, payload) {
    return httpClient.put(`/admin/users/${id}`, payload)
  },
  deleteUser(id) {
    return httpClient.delete(`/admin/users/${id}`)
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

  // 6. Dynamic System Roles & 7x26 API Permissions Matrix
  getPermissionsCatalog() {
    return httpClient.get('/admin/roles/permissions-catalog')
  },
  getRoles() {
    return httpClient.get('/admin/roles')
  },
  createRole(payload) {
    return httpClient.post('/admin/roles', payload)
  },
  updateRole(id, payload) {
    return httpClient.put(`/admin/roles/${id}`, payload)
  },
  deleteRole(id) {
    return httpClient.delete(`/admin/roles/${id}`)
  },

  // 7. Bookings & Admin Refund
  getBookings(params) {
    return httpClient.get('/admin/bookings', { params })
  },
  refundBooking(bookingReference, payload) {
    return httpClient.post(`/admin/bookings/${bookingReference}/refund`, payload)
  },

  // 8. Stations, Trips & Global Settings Management
  getStations() {
    return httpClient.get('/admin/stations')
  },
  createStation(payload) {
    return httpClient.post('/admin/stations', payload)
  },
  updateStation(id, payload) {
    return httpClient.put(`/admin/stations/${id}`, payload)
  },
  deleteStation(id) {
    return httpClient.delete(`/admin/stations/${id}`)
  },
  getTrips() {
    return httpClient.get('/admin/trips')
  },
  createTrip(payload) {
    return httpClient.post('/admin/trips', payload)
  },
  updateTrip(id, payload) {
    return httpClient.put(`/admin/trips/${id}`, payload)
  },
  cancelTrip(id) {
    return httpClient.patch(`/admin/trips/${id}/cancel`)
  },
  deleteTrip(id) {
    return httpClient.delete(`/admin/trips/${id}`)
  },
  getSystemSettings() {
    return httpClient.get('/admin/settings')
  },
  updateSystemSettings(payload) {
    return httpClient.put('/admin/settings', payload)
  }
}
