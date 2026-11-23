import { defineStore } from 'pinia'
import { ref } from 'vue'
import { catalogBookingApi } from './api'

export const useBookingStore = defineStore('booking', () => {
  const activeHeldBooking = ref(JSON.parse(localStorage.getItem('velobus_held_booking') || 'null'))
  const selectedTrip = ref(null)
  const selectedSeats = ref([])

  async function holdSelectedSeats(tripId, seatNumbers) {
    const res = await catalogBookingApi.holdSeats({ tripId, seatNumbers })
    if (res?.data) {
      activeHeldBooking.value = res.data
      localStorage.setItem('velobus_held_booking', JSON.stringify(res.data))
    }
    return res
  }

  function clearHeldBooking() {
    activeHeldBooking.value = null
    selectedSeats.value = []
    localStorage.removeItem('velobus_held_booking')
  }

  return {
    activeHeldBooking,
    selectedTrip,
    selectedSeats,
    holdSelectedSeats,
    clearHeldBooking,
  }
})
