import httpClient from '@/shared/api/httpClient'

export const catalogBookingApi = {
  // Public & Catalog
  getPublicBanners(limit: number = 10) {
    return httpClient.get('/public/banners/active', { params: { limit } })
  },
  getPublicSettings() {
    return httpClient.get('/public/settings')
  },
  getStations() {
    return httpClient.get('/catalog/stations')
  },
  searchTrips(params?: Record<string, any>) {
    return httpClient.get('/catalog/trips', { params })
  },
  getTripSeats(tripId: number | string) {
    return httpClient.get(`/catalog/trips/${tripId}/seats`)
  },

  // Customer Bookings
  getSeatAvailability(tripId: number | string) {
    return httpClient.get(`/bookings/trips/${tripId}/availability`)
  },
  holdSeats(payload: Record<string, any>, idempotencyKey?: string | null) {
    const config: Record<string, any> = {}
    if (idempotencyKey) {
      config.headers = { 'Idempotency-Key': String(idempotencyKey).trim() }
    }
    return httpClient.post('/bookings/hold', payload, config)
  },
  cancelBooking(bookingReference: string) {
    return httpClient.post(`/bookings/${bookingReference}/cancel`)
  },
  getMyBookings(params?: Record<string, any>) {
    return httpClient.get('/bookings/my-bookings', { params })
  },
  getBookingDetail(bookingReference: string) {
    return httpClient.get(`/bookings/${bookingReference}`)
  },
  getElectronicTicket(bookingReference: string) {
    return httpClient.get(`/bookings/${bookingReference}/ticket`)
  },

  // Payment
  createVnPayCheckout(payload: Record<string, any>) {
    return httpClient.post('/payments/checkout', payload)
  },
  getPaymentStatus(bookingReference: string) {
    return httpClient.get(`/payments/bookings/${bookingReference}/status`)
  },
}
