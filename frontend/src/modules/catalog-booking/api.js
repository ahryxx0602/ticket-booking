import httpClient from '@/shared/api/httpClient'

export const catalogBookingApi = {
  getPublicBanners(position) {
    return httpClient.get('/public/banners', { params: position ? { position } : {} })
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
  holdSeats(payload) {
    return httpClient.post('/bookings/hold', payload)
  },
  cancelBooking(bookingReference) {
    return httpClient.post(`/bookings/${bookingReference}/cancel`)
  },
  getMyBookings(params) {
    return httpClient.get('/bookings/my', { params })
  },
  getBookingDetail(bookingReference) {
    return httpClient.get(`/bookings/${bookingReference}`)
  },
  createVnPayCheckout(payload) {
    return httpClient.post('/payments/checkout', payload)
  },
}
