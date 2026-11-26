import httpClient from '@/shared/api/httpClient'

export const catalogBookingApi = {
  // Public & Catalog
  getPublicBanners(limit = 10) {
    return httpClient.get('/public/banners/active', { params: { limit } })
  },
  getPublicSettings() {
    return httpClient.get('/public/settings')
  },
  getStations() {
    return httpClient.get('/catalog/stations')
  },
  searchTrips(params) {
    return httpClient.get('/catalog/trips', { params })
  },
  getTripSeats(tripId) {
    return httpClient.get(`/catalog/trips/${tripId}/seats`)
  },

  // Customer Bookings
  getSeatAvailability(tripId) {
    return httpClient.get(`/bookings/trips/${tripId}/availability`)
  },
  holdSeats(payload, idempotencyKey) {
    const config = {}
    if (idempotencyKey) {
      config.headers = { 'Idempotency-Key': String(idempotencyKey).trim() }
    }
    return httpClient.post('/bookings/hold', payload, config)
  },
  cancelBooking(bookingReference) {
    return httpClient.post(`/bookings/${bookingReference}/cancel`)
  },
  getMyBookings(params) {
    return httpClient.get('/bookings/my-bookings', { params })
  },
  getBookingDetail(bookingReference) {
    return httpClient.get(`/bookings/${bookingReference}`)
  },

  // Payment
  createVnPayCheckout(payload) {
    return httpClient.post('/payments/checkout', payload)
  },
}
