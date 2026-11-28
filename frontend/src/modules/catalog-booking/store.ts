import { defineStore } from 'pinia'
import { ref } from 'vue'
import { catalogBookingApi } from './api'
import type { BookingResponse } from '@/types/booking'

export const useBookingStore = defineStore('booking', () => {
  const activeHeldBooking = ref<BookingResponse | null>(JSON.parse(localStorage.getItem('velobus_held_booking') || 'null'))
  const selectedTrip = ref<any>(null)
  const selectedSeats = ref<string[]>([])

  async function holdSelectedSeats(tripId: number | string, seatNumbers: string[]) {
    const res = await catalogBookingApi.holdSeats({ tripId: Number(tripId), seatNumbers })
    if (res?.data) {
      activeHeldBooking.value = res.data
      localStorage.setItem('velobus_held_booking', JSON.stringify(res.data))
    }
    return res
  }

  function clearHeldBooking(): void {
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
