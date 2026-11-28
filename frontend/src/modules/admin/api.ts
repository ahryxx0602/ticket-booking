import httpClient from '@/shared/api/httpClient'

export const adminApi = {
  // 1. Operators Governance & Commission
  getOperators(status?: string) {
    return httpClient.get('/admin/partners/operators', { params: status ? { status } : {} })
  },
  createOperator(payload: Record<string, any>) {
    return httpClient.post('/admin/partners/operators', payload)
  },
  updateOperator(id: number | string, payload: Record<string, any>) {
    return httpClient.put(`/admin/partners/operators/${id}`, payload)
  },
  deleteOperator(id: number | string) {
    return httpClient.delete(`/admin/partners/operators/${id}`)
  },
  approveOperator(id: number | string) {
    return httpClient.put(`/admin/partners/operators/${id}/approve`)
  },
  rejectOperator(id: number | string, note?: string) {
    return httpClient.put(`/admin/partners/operators/${id}/reject`, { note })
  },
  suspendOperator(id: number | string, note?: string) {
    return httpClient.put(`/admin/partners/operators/${id}/suspend`, { note })
  },
  updateOperatorCommission(id: number | string, commissionRate: number | string) {
    return httpClient.put(`/admin/partners/operators/${id}/commission`, { commissionRate })
  },
  updateGlobalCommission(commissionRate: number | string) {
    return httpClient.put('/admin/partners/commission/global', { commissionRate })
  },
  getGlobalCommission() {
    return httpClient.get('/admin/partners/commission/global')
  },

  // 2. Withdrawals
  getWithdrawals(status?: string) {
    return httpClient.get('/admin/partners/withdrawals', { params: status ? { status } : {} })
  },
  approveWithdrawal(id: number | string, note?: string) {
    return httpClient.put(`/admin/partners/withdrawals/${id}/approve`, { note })
  },
  rejectWithdrawal(id: number | string, note?: string) {
    return httpClient.put(`/admin/partners/withdrawals/${id}/reject`, { note })
  },

  // 3. Banners
  getBanners(status?: string) {
    return httpClient.get('/admin/partners/banners', { params: status ? { status } : {} })
  },
  approveBanner(id: number | string) {
    return httpClient.put(`/admin/partners/banners/${id}/approve`)
  },
  rejectBanner(id: number | string, note?: string) {
    return httpClient.put(`/admin/partners/banners/${id}/reject`, { note })
  },
  createPlatformBanner(payload: Record<string, any>) {
    return httpClient.post('/admin/partners/banners/platform', payload)
  },
  updateBanner(id: number | string, payload: Record<string, any>) {
    return httpClient.put(`/admin/partners/banners/${id}`, payload)
  },
  deleteBanner(id: number | string) {
    return httpClient.delete(`/admin/partners/banners/${id}`)
  },

  // 4. Financial & Payments Reports
  getPaymentReport(startDate?: string, endDate?: string) {
    return httpClient.get('/admin/payments/reports', { params: { startDate, endDate } })
  },
  getPayments(params?: Record<string, any>) {
    return httpClient.get('/admin/payments', { params })
  },
  reconcilePayment(id: number | string, note?: string) {
    return httpClient.post(`/admin/payments/${id}/reconcile`, { note })
  },

  // 5. User Management (3 Categories: Customer, Operator, System Staff)
  getUsers(params?: Record<string, any>) {
    return httpClient.get('/admin/users', { params })
  },
  getUserDetail(id: number | string) {
    return httpClient.get(`/admin/users/${id}`)
  },
  createUser(payload: Record<string, any>) {
    return httpClient.post('/admin/users', payload)
  },
  updateUser(id: number | string, payload: Record<string, any>) {
    return httpClient.put(`/admin/users/${id}`, payload)
  },
  deleteUser(id: number | string) {
    return httpClient.delete(`/admin/users/${id}`)
  },
  updateUserStatus(id: number | string, status: string) {
    return httpClient.patch(`/admin/users/${id}/status`, { status })
  },
  changeUserRole(id: number | string, role: string) {
    return httpClient.patch(`/admin/users/${id}/role`, { role })
  },
  resetUserPassword(id: number | string, newPassword: string) {
    return httpClient.post(`/admin/users/${id}/reset-password`, { newPassword })
  },

  // 6. Dynamic System Roles & 7x26 API Permissions Matrix
  getPermissionsCatalog() {
    return httpClient.get('/admin/roles/permissions-catalog')
  },
  getRoles() {
    return httpClient.get('/admin/roles')
  },
  createRole(payload: Record<string, any>) {
    return httpClient.post('/admin/roles', payload)
  },
  updateRole(id: number | string, payload: Record<string, any>) {
    return httpClient.put(`/admin/roles/${id}`, payload)
  },
  deleteRole(id: number | string) {
    return httpClient.delete(`/admin/roles/${id}`)
  },

  // 7. Bookings & Admin Refund
  getBookings(params?: Record<string, any>) {
    return httpClient.get('/admin/bookings', { params })
  },
  refundBooking(bookingReference: string, payload: Record<string, any>) {
    return httpClient.post(`/admin/bookings/${bookingReference}/refund`, payload)
  },

  // 8. Stations, Trips & Global Settings Management
  getStations() {
    return httpClient.get('/admin/stations')
  },
  createStation(payload: Record<string, any>) {
    return httpClient.post('/admin/stations', payload)
  },
  updateStation(id: number | string, payload: Record<string, any>) {
    return httpClient.put(`/admin/stations/${id}`, payload)
  },
  deleteStation(id: number | string) {
    return httpClient.delete(`/admin/stations/${id}`)
  },
  getTrips() {
    return httpClient.get('/admin/trips')
  },
  createTrip(payload: Record<string, any>) {
    return httpClient.post('/admin/trips', payload)
  },
  updateTrip(id: number | string, payload: Record<string, any>) {
    return httpClient.put(`/admin/trips/${id}`, payload)
  },
  cancelTrip(id: number | string) {
    return httpClient.patch(`/admin/trips/${id}/cancel`)
  },
  deleteTrip(id: number | string) {
    return httpClient.delete(`/admin/trips/${id}`)
  },
  getSystemSettings() {
    return httpClient.get('/admin/settings')
  },
  updateSystemSettings(payload: Record<string, any>) {
    return httpClient.put('/admin/settings', payload)
  }
}
