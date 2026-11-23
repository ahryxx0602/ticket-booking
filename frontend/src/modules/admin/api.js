import httpClient from '@/shared/api/httpClient'

export const adminApi = {
  getOperators(status) {
    return httpClient.get('/admin/partners/operators', { params: status ? { status } : {} })
  },
  approveOperator(id) {
    return httpClient.post(`/admin/partners/operators/${id}/approve`)
  },
  rejectOperator(id, reason) {
    return httpClient.post(`/admin/partners/operators/${id}/reject`, { reason })
  },
  suspendOperator(id, reason) {
    return httpClient.post(`/admin/partners/operators/${id}/suspend`, { reason })
  },
  updateOperatorCommission(id, commissionRate) {
    return httpClient.put(`/admin/partners/operators/${id}/commission`, { commissionRate })
  },
  updateGlobalCommission(commissionRate) {
    return httpClient.put('/admin/partners/settings/commission', { commissionRate })
  },
  getWithdrawals(status) {
    return httpClient.get('/admin/partners/withdrawals', { params: status ? { status } : {} })
  },
  approveWithdrawal(id, adminNote, transactionRef) {
    return httpClient.post(`/admin/partners/withdrawals/${id}/approve`, { adminNote, transactionRef })
  },
  rejectWithdrawal(id, adminNote) {
    return httpClient.post(`/admin/partners/withdrawals/${id}/reject`, { adminNote })
  },
  getBanners(status) {
    return httpClient.get('/admin/partners/banners', { params: status ? { status } : {} })
  },
  approveBanner(id) {
    return httpClient.post(`/admin/partners/banners/${id}/approve`)
  },
  rejectBanner(id) {
    return httpClient.post(`/admin/partners/banners/${id}/reject`)
  },
  getPaymentReport() {
    return httpClient.get('/admin/payments/report')
  },
  refundBooking(bookingReference, payload) {
    return httpClient.post(`/admin/bookings/${bookingReference}/refund`, payload)
  },
}
