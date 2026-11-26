<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import { useAuthStore } from '@/modules/auth/store'
import SeatMap2Deck from '@/shared/components/SeatMap2Deck.vue'
import { useCountdown } from '@/shared/composables/useCountdown'
import { catalogBookingApi } from '../api'
import { validateBookingResponse } from '../utils/booking-validator'
import {
  Bus,
  ArrowLeft,
  Clock,
  AlertCircle,
  AlertTriangle,
  RefreshCw,
  Loader2,
  Trash2,
  ArrowRight,
  ShieldAlert
} from 'lucide-vue-next'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger
} from '@/components/ui/alert-dialog'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const tripId = computed(() => route.params.id)
const loading = ref(false)
const error = ref(null)
const holding = ref(false)
const cancelling = ref(false)
const isCancelUnresolved = ref(false)
const isVerifyingHoldState = ref(false)
const isRefreshingAvailability = ref(false)
const selectedSeats = ref([])
const backendSeats = ref([])
const unavailableSeats = ref([])
const availabilityMap = ref({})

// Unknown outcome state for hold mutation (fail-closed guard)
const isHoldUnknownOutcome = ref(false)
const holdUnknownContext = ref(null)
const activeClientRequestId = ref(null)

// Active booking information after successful hold
const heldBooking = ref(null)

// Storage keys partitioned by user and trip
const getHoldUnknownStorageKey = () => `velobus_hold_unknown_${auth.user?.id || 'guest'}_${tripId.value}`
const getActiveHoldStorageKey = () => `velobus_active_hold_${auth.user?.id || 'guest'}_${tripId.value}`
const getHoldRequestStorageKey = () => `velobus_hold_request_${auth.user?.id || 'guest'}_${tripId.value}`
const getCancelUnresolvedStorageKey = () => `velobus_cancel_unresolved_${auth.user?.id || 'guest'}_${tripId.value}`

// Trip Metadata initialized from route history.state or defaults based on tripId
const initialTrip = history.state?.trip
const trip = ref(initialTrip ? {
  id: initialTrip.id,
  tripCode: initialTrip.tripCode || `TRIP-${initialTrip.id}`,
  route: initialTrip.route || `${initialTrip.departureStationName || initialTrip.departureStationCode || 'Điểm đi'} → ${initialTrip.arrivalStationName || initialTrip.arrivalStationCode || 'Điểm đến'}`,
  departureTime: initialTrip.departureTime || '',
  operatorName: initialTrip.operator || initialTrip.operatorName || 'Nhà xe đối tác VeloBus',
  coachType: initialTrip.transportType || 'Xe khách',
  vehiclePlate: initialTrip.plate || '',
  duration: initialTrip.duration || '',
  basePrice: Number(initialTrip.basePrice) || 0
} : {
  id: tripId.value,
  tripCode: `TRIP-${tripId.value}`,
  route: `Chuyến xe #${tripId.value}`,
  departureTime: '',
  operatorName: 'Nhà xe đối tác VeloBus',
  coachType: 'Xe khách',
  vehiclePlate: '',
  duration: '',
  basePrice: 0
})

// Countdown Hook (Server expiresAt sync)
const { remainingSeconds, formattedTime, isRunning, start: startCountdown, stop: stopCountdown } = useCountdown(() => {
  toast.warning('Thời gian 10 phút giữ chỗ đã hết. Vui lòng chọn lại chỗ ngồi.', {
    description: 'Phiên giữ chỗ của bạn đã kết thúc tự động.'
  })
  heldBooking.value = null
  selectedSeats.value = []
  try {
    sessionStorage.removeItem(getActiveHoldStorageKey())
  } catch (_) {}
  fetchTripSeats()
  fetchAvailability()
})

// Calculate total price: server authoritative amount when held, otherwise client sum
const totalPrice = computed(() => {
  if (heldBooking.value?.totalAmount !== undefined && heldBooking.value?.totalAmount !== null) {
    return Number(heldBooking.value.totalAmount)
  }
  if (selectedSeats.value.length === 0) return 0
  return selectedSeats.value.reduce((sum, seatNum) => {
    const seatObj = backendSeats.value.find(s => (s.seatNumber || s.code) === seatNum)
    const price = Number(seatObj?.finalPrice) || trip.value.basePrice || 0
    return sum + price
  }, 0)
})

const heldSeatCodes = computed(() => {
  if (!heldBooking.value?.seatNumbers) return []
  return heldBooking.value.seatNumbers
})

function recordHoldUnknownOutcome(seats, customReason) {
  isHoldUnknownOutcome.value = true
  const ctx = {
    userId: auth.user?.id || null,
    tripId: Number(tripId.value),
    seatNumbers: [...seats],
    clientRequestId: activeClientRequestId.value || null,
    timestamp: Date.now(),
    reason: customReason || 'Unknown outcome'
  }
  holdUnknownContext.value = ctx
  try {
    sessionStorage.setItem(getHoldUnknownStorageKey(), JSON.stringify(ctx))
  } catch (_) {}
}

function clearHoldUnknownOutcome() {
  isHoldUnknownOutcome.value = false
  holdUnknownContext.value = null
  try {
    sessionStorage.removeItem(getHoldUnknownStorageKey())
  } catch (_) {}
}

/**
 * PRE-01 & PRE-02: Authoritative Server State Verification for Hold Mutation
 * - Reads PageResponse<BookingResponse> ({ items, page, size, totalPages, totalElements, hasNext })
 * - Does not treat malformed responses as valid empty lists
 * - Does not assume page 0 is complete dataset if hasNext is true
 * - Matches exact requested seat set (not any arbitrary unexpired booking)
 * - Retains fail-closed lock if no exact matching booking is found on server
 * - Preserves lock on verification failure
 */
async function verifyHoldServerState() {
  if (isVerifyingHoldState.value) return
  isVerifyingHoldState.value = true

  try {
    const targetSeats = holdUnknownContext.value?.seatNumbers || selectedSeats.value

    // If clientRequestId is available, first attempt authoritative idempotent replay via hold API
    const reqKey = activeClientRequestId.value || holdUnknownContext.value?.clientRequestId
    if (reqKey && targetSeats.length > 0) {
      try {
        const replayRes = await catalogBookingApi.holdSeats({
          tripId: Number(tripId.value),
          seatNumbers: targetSeats
        }, reqKey)
        const replayBooking = replayRes?.data || replayRes
        const validation = validateBookingResponse(replayBooking, {
          expectedTripId: tripId.value,
          expectedUserId: auth.user?.id,
          expectedSeats: targetSeats,
          allowTerminalReplay: true
        })

        if (validation.valid) {
          if (replayBooking.status === 'HELD') {
            heldBooking.value = replayBooking
            selectedSeats.value = [...(replayBooking.seatNumbers || [])]
            startCountdown(replayBooking.expiresAt)
            try {
              sessionStorage.setItem(getActiveHoldStorageKey(), JSON.stringify({
                bookingReference: replayBooking.bookingReference,
                tripId: Number(tripId.value),
                userId: auth.user?.id
              }))
              sessionStorage.removeItem(getHoldRequestStorageKey())
            } catch (_) {}
            clearHoldUnknownOutcome()
            activeClientRequestId.value = null
            toast.success(`Máy chủ đã xác nhận phiên giữ chỗ #${replayBooking.bookingReference} cho ghế ${replayBooking.seatNumbers.join(', ')}.`)
            await fetchAvailability()
            return
          } else {
            clearHoldUnknownOutcome()
            activeClientRequestId.value = null
            try {
              sessionStorage.removeItem(getHoldRequestStorageKey())
            } catch (_) {}
            toast.info(`Phiên giữ chỗ #${replayBooking.bookingReference} trước đó đã ở trạng thái ${replayBooking.status}.`)
            await fetchAvailability()
            return
          }
        }
      } catch (replayErr) {
        console.warn('Direct idempotent hold replay check did not resolve, falling back to getMyBookings:', replayErr)
      }
    }

    let allItems = []
    let currentPage = 0
    let hasMore = true
    const maxPages = 5

    while (hasMore && currentPage < maxPages) {
      const res = await catalogBookingApi.getMyBookings({ page: currentPage, size: 20 })
      const pageData = res?.data

      // PRE-01: PageResponse contract validation
      if (!pageData || !Array.isArray(pageData.items)) {
        throw new Error('Dữ liệu phản hồi từ máy chủ không đúng định dạng phân trang PageResponse (thiếu mảng items).')
      }

      allItems.push(...pageData.items)
      hasMore = Boolean(pageData.hasNext) && (currentPage + 1 < (pageData.totalPages || 1))
      currentPage++
    }

    // PRE-02 & PRE-03: Strict match against exact requested seats on this trip (supporting terminal replay)
    const matched = allItems.find(b => {
      const validation = validateBookingResponse(b, {
        expectedTripId: tripId.value,
        expectedUserId: auth.user?.id,
        expectedSeats: targetSeats,
        allowTerminalReplay: true
      })
      return validation.valid
    })

    if (matched) {
      if (matched.status === 'HELD') {
        heldBooking.value = matched
        selectedSeats.value = [...(matched.seatNumbers || [])]
        startCountdown(matched.expiresAt)
        try {
          sessionStorage.setItem(getActiveHoldStorageKey(), JSON.stringify({
            bookingReference: matched.bookingReference,
            tripId: Number(tripId.value),
            userId: auth.user?.id
          }))
          sessionStorage.removeItem(getHoldRequestStorageKey())
        } catch (_) {}
        clearHoldUnknownOutcome()
        activeClientRequestId.value = null
        toast.success(`Máy chủ đã xác nhận phiên giữ chỗ #${matched.bookingReference} cho ghế ${matched.seatNumbers.join(', ')}.`)
        await fetchAvailability()
      } else {
        clearHoldUnknownOutcome()
        activeClientRequestId.value = null
        try {
          sessionStorage.removeItem(getHoldRequestStorageKey())
        } catch (_) {}
        toast.info(`Phiên giữ chỗ #${matched.bookingReference} trước đó đã ở trạng thái ${matched.status}.`)
        await fetchAvailability()
      }
    } else {
      // PRE-02: Do NOT clear unknown outcome merely because no booking was found!
      // Fail-closed security rule: retain lock to prevent double booking.
      toast.warning('Máy chủ chưa ghi nhận phiên giữ chỗ khớp với ghế đã yêu cầu. Hệ thống tiếp tục KHÓA thao tác để đảm bảo an toàn. Vui lòng đối soát lịch sử đặt chỗ hoặc thử lại sau.')
    }
  } catch (err) {
    // PRE-01 / PRE-02: Verification failure preserves lock!
    console.error('Hold state verification failed:', err)
    toast.error('Không thể kết nối đến máy chủ để xác minh giữ chỗ: ' + (err?.message || 'Tiếp tục KHÓA thao tác để đảm bảo an toàn.'))
  } finally {
    isVerifyingHoldState.value = false
  }
}

async function fetchTripSeats() {
  if (!tripId.value) return
  loading.value = true
  error.value = null
  try {
    const res = await catalogBookingApi.getTripSeats(tripId.value)
    const data = Array.isArray(res?.data) ? res.data : (Array.isArray(res) ? res : [])
    backendSeats.value = data
    if (trip.value.basePrice === 0 && data.length > 0 && data[0].finalPrice) {
      trip.value.basePrice = Number(data[0].finalPrice)
    }
  } catch (err) {
    console.error('Failed to load trip seats:', err)
    error.value = err?.message || 'Không thể tải sơ đồ ghế từ máy chủ.'
    backendSeats.value = []
  } finally {
    loading.value = false
  }
}

/**
 * Dynamic Availability Fetching (Option B API)
 * GET /api/v1/bookings/trips/{tripId}/availability
 */
async function fetchAvailability() {
  if (!tripId.value) return
  isRefreshingAvailability.value = true
  try {
    const res = await catalogBookingApi.getSeatAvailability(tripId.value)
    const availData = res?.data || res
    const seatsList = Array.isArray(availData?.seats) ? availData.seats : []
    const newMap = {}
    seatsList.forEach(item => {
      if (item.seatNumber && item.status) {
        newMap[item.seatNumber] = item.status
      }
    })
    availabilityMap.value = newMap
  } catch (err) {
    console.warn('Seat availability query failed (non-blocking fallback to catalog):', err)
  } finally {
    isRefreshingAvailability.value = false
  }
}

function onToggleSeat(seatCode) {
  if (isHoldUnknownOutcome.value) {
    toast.warning('Giao dịch giữ chỗ trước đang bị gián đoạn. Vui lòng bấm kiểm tra trạng thái máy chủ.')
    return
  }
  if (isRunning.value && heldBooking.value) {
    toast.info('Phiên giữ chỗ đang kích hoạt. Vui lòng thanh toán hoặc hủy giữ chỗ để đổi ghế.')
    return
  }

  const idx = selectedSeats.value.indexOf(seatCode)
  if (idx > -1) {
    selectedSeats.value.splice(idx, 1)
  } else {
    if (selectedSeats.value.length >= 10) {
      toast.warning('Mỗi hành khách được chọn tối đa 10 chỗ trong một lần đặt vé.')
      return
    }
    selectedSeats.value.push(seatCode)
  }
}

function removeSeat(seatCode) {
  if (isHoldUnknownOutcome.value) return
  if (isRunning.value && heldBooking.value) {
    toast.info('Không thể xóa ghế khi đang trong phiên giữ chỗ. Vui lòng hủy phiên nếu muốn chọn lại.')
    return
  }
  const idx = selectedSeats.value.indexOf(seatCode)
  if (idx > -1) selectedSeats.value.splice(idx, 1)
}

/**
 * PRE-03: Handle Seat Hold with Strict BookingResponse Validation
 */
async function handleHoldSeats() {
  if (holding.value || isHoldUnknownOutcome.value) return
  if (selectedSeats.value.length === 0) {
    toast.warning('Vui lòng chọn ít nhất 1 chỗ trên sơ đồ xe.')
    return
  }

  // 1. Chặn khách vãng lai (Guest) chưa đăng nhập
  if (!auth.isAuthenticated) {
    try {
      sessionStorage.setItem(`velobus_pending_seats_${tripId.value}`, JSON.stringify(selectedSeats.value))
    } catch (_) {}

    toast.info('Vui lòng đăng nhập tài khoản Khách hàng để thực hiện giữ chỗ.')
    router.push({
      path: '/login',
      query: {
        redirect: route.fullPath,
        reason: 'auth_required'
      }
    })
    return
  }

  // 2. Chặn tài khoản Quản trị / Đối tác nhà xe đặt vé khách hàng
  if (!auth.isCustomer) {
    toast.warning('Tài khoản Quản trị viên / Nhà xe không được thực hiện đặt vé khách hàng.')
    return
  }

  const requestedSeats = [...selectedSeats.value]
  holding.value = true

  // Ensure stable clientRequestId across retries/timeouts
  if (!activeClientRequestId.value) {
    try {
      const storedReq = sessionStorage.getItem(getHoldRequestStorageKey())
      if (storedReq) {
        const parsed = JSON.parse(storedReq)
        if (parsed?.clientRequestId && String(parsed.tripId) === String(tripId.value)) {
          activeClientRequestId.value = parsed.clientRequestId
        }
      }
    } catch (_) {}
  }
  if (!activeClientRequestId.value) {
    activeClientRequestId.value = (typeof crypto !== 'undefined' && crypto.randomUUID)
      ? crypto.randomUUID()
      : 'req-' + Date.now() + '-' + Math.random().toString(36).substring(2, 10)
  }

  try {
    sessionStorage.setItem(getHoldRequestStorageKey(), JSON.stringify({
      clientRequestId: activeClientRequestId.value,
      tripId: Number(tripId.value),
      seatNumbers: requestedSeats,
      timestamp: Date.now()
    }))
  } catch (_) {}

  try {
    const res = await catalogBookingApi.holdSeats({
      tripId: Number(tripId.value),
      seatNumbers: requestedSeats
    }, activeClientRequestId.value)

    const booking = res?.data || res

    // PRE-03: Strict shared validator for BookingResponse (supporting terminal replay)
    const validation = validateBookingResponse(booking, {
      expectedTripId: tripId.value,
      expectedUserId: auth.user?.id,
      expectedSeats: requestedSeats,
      allowTerminalReplay: true
    })

    if (!validation.valid) {
      recordHoldUnknownOutcome(requestedSeats, validation.error)
      throw new Error(validation.error)
    }

    if (booking.status === 'HELD') {
      // Success: Server state is authoritative (no fallback to requestedSeats)
      heldBooking.value = {
        ...booking,
        seatNumbers: booking.seatNumbers
      }
      selectedSeats.value = [...booking.seatNumbers]

      startCountdown(booking.expiresAt)
      clearHoldUnknownOutcome()

      try {
        sessionStorage.setItem(getActiveHoldStorageKey(), JSON.stringify({
          bookingReference: booking.bookingReference,
          tripId: Number(tripId.value),
          userId: auth.user?.id
        }))
        sessionStorage.removeItem(getHoldRequestStorageKey())
      } catch (_) {}
      activeClientRequestId.value = null

      toast.success(`Giữ chỗ thành công! Đã khóa ${selectedSeats.value.join(', ')} trong 10 phút. Vui lòng thanh toán!`)
      await fetchAvailability()
    } else {
      // Terminal booking replay (CONFIRMED, CANCELLED, EXPIRED)
      clearHoldUnknownOutcome()
      try {
        sessionStorage.removeItem(getHoldRequestStorageKey())
      } catch (_) {}
      activeClientRequestId.value = null
      toast.info(`Phiên giữ chỗ trước đó đã ở trạng thái ${booking.status}.`)
      await fetchAvailability()
    }
  } catch (err) {
    console.error('Hold seats failed:', err)
    const errMsg = err?.message || ''
    const errCode = err?.code || ''
    const isConflict = errCode === 'SEAT_ALREADY_RESERVED' || errMsg.includes('SEAT_ALREADY_RESERVED')

    if (isConflict) {
      // PRE-05: Definitive business rejection: remove only requested seats from selection,
      // and refresh authoritative availability rather than permanently disabling all seats.
      selectedSeats.value = selectedSeats.value.filter(s => !requestedSeats.includes(s))
      activeClientRequestId.value = null
      try {
        sessionStorage.removeItem(getHoldRequestStorageKey())
      } catch (_) {}
      toast.error('Một hoặc nhiều ghế đã bị hành khách khác chọn. Vui lòng chọn ghế khác!')
      await fetchAvailability()
    } else {
      // Ambiguous outcome (timeout, network error, malformed response)
      const isTimeoutOrNetwork =
        errCode === 'NETWORK_ERROR' ||
        errCode === 'ECONNABORTED' ||
        errCode === 'ERR_NETWORK' ||
        errMsg.toLowerCase().includes('timeout') ||
        errMsg.toLowerCase().includes('kết nối') ||
        errMsg.includes('không hợp lệ') ||
        errMsg.includes('khuyết') ||
        errMsg.includes('không khớp') ||
        err?.name === 'TimeoutError'

      if (isTimeoutOrNetwork) {
        if (!isHoldUnknownOutcome.value) {
          recordHoldUnknownOutcome(requestedSeats, errMsg)
        }
        toast.error('Không thể xác định kết quả giữ chỗ do lỗi kết nối mạng hoặc timeout. Đã khóa thao tác để bảo vệ giao dịch.')
      } else {
        toast.error(errMsg ? `Không thể giữ chỗ: ${errMsg}` : 'Không thể giữ chỗ do hệ thống bận.')
      }
    }
  } finally {
    holding.value = false
  }
}

/**
 * PRE-04: Cancellation Safety
 * - Tracks pending and unresolved cancellation states
 * - Preserves recovery context on failure
 * - Blocks checkout while cancellation is pending or unresolved
 */
async function handleCancelBooking() {
  if (!heldBooking.value?.bookingReference || cancelling.value) return
  cancelling.value = true
  const ref = heldBooking.value.bookingReference

  try {
    await catalogBookingApi.cancelBooking(ref)
    stopCountdown()
    heldBooking.value = null
    selectedSeats.value = []
    isCancelUnresolved.value = false
    try {
      sessionStorage.removeItem(getActiveHoldStorageKey())
      sessionStorage.removeItem(getCancelUnresolvedStorageKey())
    } catch (_) {}
    toast.success('Đã hủy giữ chỗ thành công.')
    await Promise.all([fetchTripSeats(), fetchAvailability()])
  } catch (err) {
    console.error('Cancel booking failed:', err)
    const errMsg = err?.message || ''
    const isTimeoutOrNetwork =
      errMsg.toLowerCase().includes('timeout') ||
      errMsg.toLowerCase().includes('kết nối') ||
      err?.code === 'NETWORK_ERROR'

    if (isTimeoutOrNetwork) {
      isCancelUnresolved.value = true
      try {
        sessionStorage.setItem(getCancelUnresolvedStorageKey(), JSON.stringify({
          bookingReference: ref,
          tripId: Number(tripId.value),
          userId: auth.user?.id,
          timestamp: Date.now()
        }))
      } catch (_) {}
      toast.error('Yêu cầu hủy giữ chỗ gặp sự cố mạng hoặc timeout. Đã khóa thanh toán để đối soát trạng thái máy chủ.')
    } else {
      toast.error('Không thể hủy giữ chỗ trên máy chủ: ' + errMsg)
    }
  } finally {
    cancelling.value = false
  }
}

/**
 * PRE-03 & PRE-04: Proceed to Checkout with Strict Verification
 */
function proceedToCheckout() {
  if (isHoldUnknownOutcome.value) {
    toast.warning('Giao dịch giữ chỗ đang bị gián đoạn. Vui lòng bấm "Kiểm tra trạng thái giữ chỗ".')
    return
  }
  if (cancelling.value) {
    toast.warning('Đang xử lý hủy giữ chỗ, không thể tiến hành thanh toán.')
    return
  }
  if (isCancelUnresolved.value) {
    toast.error('Yêu cầu hủy giữ chỗ trước đó chưa xác định được kết quả trên máy chủ. Đã khóa thanh toán để bảo vệ giao dịch.')
    return
  }
  if (!heldBooking.value) {
    handleHoldSeats()
    return
  }

  // PRE-03: Validate held booking before navigating to checkout
  const validation = validateBookingResponse(heldBooking.value, {
    expectedTripId: tripId.value,
    expectedUserId: auth.user?.id
  })

  if (!validation.valid) {
    toast.error(`Phiên giữ chỗ không hợp lệ để thanh toán: ${validation.error}`)
    return
  }

  router.push(`/checkout/${heldBooking.value.bookingReference}`)
}

function formatCurrency(val) {
  if (!val) return '0'
  return Number(val).toLocaleString('vi-VN')
}

onMounted(async () => {
  await Promise.all([fetchTripSeats(), fetchAvailability()])

  // 1. Restore pending seat intent after returning from login
  try {
    const pending = sessionStorage.getItem(`velobus_pending_seats_${tripId.value}`)
    if (pending) {
      const parsed = JSON.parse(pending)
      if (Array.isArray(parsed) && parsed.length > 0) {
        selectedSeats.value = parsed
        sessionStorage.removeItem(`velobus_pending_seats_${tripId.value}`)
      }
    }
  } catch (_) {}

  // 2. Restore Hold Unknown Outcome state if previously interrupted
  try {
    const storedUnknown = sessionStorage.getItem(getHoldUnknownStorageKey())
    if (storedUnknown) {
      const parsed = JSON.parse(storedUnknown)
      if (parsed && typeof parsed === 'object') {
        isHoldUnknownOutcome.value = true
        holdUnknownContext.value = parsed
      }
    }
  } catch (_) {}

  // 3. Restore Active Held Booking from Server with strict validator
  try {
    const storedActiveHold = sessionStorage.getItem(getActiveHoldStorageKey())
    if (storedActiveHold) {
      const parsed = JSON.parse(storedActiveHold)
      if (parsed?.bookingReference && String(parsed.tripId) === String(tripId.value)) {
        try {
          const detailRes = await catalogBookingApi.getBookingDetail(parsed.bookingReference)
          const booking = detailRes?.data || detailRes
          const validation = validateBookingResponse(booking, {
            expectedTripId: tripId.value,
            expectedUserId: auth.user?.id
          })
          if (validation.valid) {
            heldBooking.value = booking
            selectedSeats.value = [...(booking.seatNumbers || [])]
            startCountdown(booking.expiresAt)
          } else {
            sessionStorage.removeItem(getActiveHoldStorageKey())
          }
        } catch (_) {
          // If server check fails, keep client state clean
        }
      }
    }
  } catch (_) {}

  // 4. Restore Cancel Unresolved state if previously interrupted
  try {
    const storedCancel = sessionStorage.getItem(getCancelUnresolvedStorageKey())
    if (storedCancel) {
      const parsed = JSON.parse(storedCancel)
      if (parsed && String(parsed.tripId) === String(tripId.value)) {
        isCancelUnresolved.value = true
      }
    }
  } catch (_) {}

  // 5. Restore active clientRequestId if previously pending
  try {
    const storedReq = sessionStorage.getItem(getHoldRequestStorageKey())
    if (storedReq) {
      const parsed = JSON.parse(storedReq)
      if (parsed?.clientRequestId && String(parsed.tripId) === String(tripId.value)) {
        activeClientRequestId.value = parsed.clientRequestId
      }
    }
  } catch (_) {}
})
</script>

<template>
  <div class="w-full py-6 space-y-6">
    <!-- Breadcrumb -->
    <nav class="flex items-center gap-2 text-xs text-muted-foreground" data-testid="seat-breadcrumb">
      <router-link to="/" class="hover:text-foreground">Trang chủ</router-link>
      <span>/</span>
      <router-link to="/search" class="hover:text-foreground">Tìm vé</router-link>
      <span>/</span>
      <span class="text-foreground font-medium">{{ trip.route }}</span>
      <span>/</span>
      <span class="text-primary font-semibold">Chọn chỗ & Đặt vé</span>
    </nav>

    <!-- Top Trip Info Bar -->
    <div class="bg-card text-card-foreground rounded-xl border border-border p-5 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div class="space-y-1.5">
        <div class="flex items-center gap-2.5 flex-wrap">
          <span class="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-primary/10 text-primary border border-primary/20">
            {{ trip.operatorName }}
          </span>
          <span v-if="trip.vehiclePlate" class="text-xs text-muted-foreground">
            Biển số: <strong class="text-foreground">{{ trip.vehiclePlate }}</strong>
          </span>
          <span class="text-xs text-muted-foreground font-mono">
            Mã: <strong class="text-foreground">{{ trip.tripCode }}</strong>
          </span>
        </div>
        <h1 class="text-xl font-bold text-foreground flex items-center gap-2">
          {{ trip.route }}
        </h1>
        <p class="text-xs text-muted-foreground flex items-center gap-3 flex-wrap">
          <span v-if="trip.departureTime" class="flex items-center gap-1">
            <Clock class="w-3.5 h-3.5 text-primary" />
            <span>Khởi hành: <strong class="text-foreground">{{ trip.departureTime }}</strong></span>
          </span>
          <span v-if="trip.duration">Thời gian: {{ trip.duration }}</span>
          <span>Loại xe: {{ trip.coachType }}</span>
        </p>
      </div>

      <div class="flex items-center gap-2 self-start md:self-auto">
        <button
          type="button"
          data-testid="btn-refresh-availability"
          :disabled="isRefreshingAvailability"
          @click="fetchAvailability"
          class="inline-flex items-center justify-center px-3 py-2 text-xs font-semibold rounded-lg border border-border text-foreground hover:bg-muted transition-all disabled:opacity-50"
          title="Cập nhật tình trạng chỗ trống từ máy chủ"
        >
          <RefreshCw class="w-3.5 h-3.5 mr-1.5" :class="{ 'animate-spin': isRefreshingAvailability }" />
          <span>Cập nhật chỗ</span>
        </button>
        <router-link
          to="/search"
          class="inline-flex items-center justify-center px-4 py-2 text-xs font-semibold rounded-lg border border-border text-foreground hover:bg-muted transition-all"
        >
          <ArrowLeft class="w-3.5 h-3.5 mr-1.5" />
          Đổi chuyến xe khác
        </router-link>
      </div>
    </div>

    <!-- Unknown Outcome Warning Alert (PRE-02: Fail-Closed Protection) -->
    <div
      v-if="isHoldUnknownOutcome"
      data-testid="hold-unknown-alert"
      class="bg-amber-500/10 border-2 border-amber-500/40 rounded-xl p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4 text-xs"
    >
      <div class="flex items-start gap-3">
        <AlertTriangle class="w-5 h-5 text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
        <div class="space-y-1">
          <p class="font-bold text-amber-900 dark:text-amber-200">
            Giao dịch giữ chỗ chưa xác định kết quả (Mất kết nối / Timeout)
          </p>
          <p class="text-amber-800 dark:text-amber-300">
            Hệ thống đã tạm KHÓA thao tác để phòng ngừa tạo nhiều phiên giữ chỗ trùng lặp. Vui lòng bấm kiểm tra trạng thái máy chủ để đối soát.
          </p>
        </div>
      </div>
      <button
        type="button"
        data-testid="btn-verify-hold-state"
        :disabled="isVerifyingHoldState"
        @click="verifyHoldServerState"
        class="inline-flex items-center justify-center gap-1.5 px-4 py-2 rounded-lg bg-amber-600 hover:bg-amber-700 text-white font-semibold text-xs transition-colors shrink-0 disabled:opacity-50"
      >
        <Loader2 v-if="isVerifyingHoldState" class="w-3.5 h-3.5 animate-spin" />
        <RefreshCw v-else class="w-3.5 h-3.5" />
        <span>Kiểm tra trạng thái giữ chỗ</span>
      </button>
    </div>

    <!-- Cancellation Unresolved Warning Alert (PRE-04) -->
    <div
      v-if="isCancelUnresolved"
      data-testid="cancel-unresolved-alert"
      class="bg-destructive/10 border-2 border-destructive/40 rounded-xl p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4 text-xs"
    >
      <div class="flex items-start gap-3">
        <ShieldAlert class="w-5 h-5 text-destructive shrink-0 mt-0.5" />
        <div class="space-y-1">
          <p class="font-bold text-destructive">
            Yêu cầu hủy giữ chỗ chưa xác định kết quả
          </p>
          <p class="text-muted-foreground">
            Thao tác hủy gặp sự cố mạng hoặc timeout. Đã khóa chức năng thanh toán để tránh tranh chấp giao dịch. Vui lòng tải lại trang hoặc kiểm tra danh sách vé của bạn.
          </p>
        </div>
      </div>
    </div>

    <!-- Loading Skeleton State -->
    <div v-if="loading" class="space-y-6" data-testid="seat-loading">
      <div class="bg-card rounded-xl border border-border p-8 text-center space-y-4">
        <Loader2 class="w-8 h-8 text-primary animate-spin mx-auto" />
        <p class="text-sm font-medium text-foreground">Đang tải sơ đồ chỗ ngồi từ máy chủ...</p>
      </div>
    </div>

    <!-- Error State with Retry Button -->
    <div v-else-if="error" class="bg-destructive/10 border border-destructive/20 rounded-xl p-6 text-center space-y-3" data-testid="seat-error">
      <AlertCircle class="w-8 h-8 text-destructive mx-auto" />
      <h3 class="text-sm font-bold text-foreground">Không thể tải danh sách chỗ ngồi</h3>
      <p class="text-xs text-muted-foreground max-w-md mx-auto">{{ error }}</p>
      <button
        type="button"
        data-testid="seat-retry-btn"
        @click="fetchTripSeats"
        class="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg bg-primary text-primary-foreground text-xs font-semibold shadow-xs hover:bg-primary/90 transition-colors"
      >
        <RefreshCw class="w-3.5 h-3.5" />
        <span>Thử lại</span>
      </button>
    </div>

    <!-- Main Content Split Grid (65% seat selection, 35% sticky summary) -->
    <div v-else class="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
      <!-- Left Column: Seat Map (65% / 8 cols) -->
      <div class="lg:col-span-8 space-y-6">
        <div v-if="backendSeats.length === 0" class="bg-card rounded-xl border border-border p-8 text-center space-y-2" data-testid="seat-empty">
          <Bus class="w-8 h-8 text-muted-foreground mx-auto" />
          <h4 class="text-sm font-semibold text-foreground">Chưa có thông tin chỗ ngồi</h4>
          <p class="text-xs text-muted-foreground">Chuyến xe này chưa mở bán vé hoặc dữ liệu ghế đang được cập nhật.</p>
        </div>

        <SeatMap2Deck
          v-else
          :seats="backendSeats"
          :availability-map="availabilityMap"
          :selected-seats="selectedSeats"
          :held-seats="heldSeatCodes"
          :unavailable-seats="unavailableSeats"
          :base-price="trip.basePrice"
          :max-selectable="10"
          @toggle-seat="onToggleSeat"
        />
      </div>

      <!-- Right Column: Sticky Booking Summary Card (35% / 4 cols) -->
      <div class="lg:col-span-4 lg:sticky lg:top-24 space-y-4">
        <div class="bg-card text-card-foreground rounded-xl border border-border p-5 shadow-xs space-y-5">
          <!-- Active Hold Timer Countdown Widget -->
          <div
            v-if="isRunning"
            data-testid="hold-countdown-widget"
            class="bg-amber-500/10 border border-amber-500/30 rounded-lg p-3.5 flex items-center justify-between"
          >
            <div class="flex items-center gap-2">
              <Clock class="w-4 h-4 text-amber-600 dark:text-amber-400 animate-pulse" />
              <div>
                <p class="text-[11px] font-semibold text-amber-800 dark:text-amber-300 uppercase tracking-wide">
                  Thời gian giữ chỗ của bạn
                </p>
                <p class="text-xs text-amber-700 dark:text-amber-400">Chỗ đang được khóa trên máy chủ</p>
              </div>
            </div>
            <span
              data-testid="countdown-timer"
              class="text-xl font-bold tabular-nums text-amber-900 dark:text-amber-200 bg-card px-2.5 py-1 rounded border border-amber-500/30 shadow-xs"
            >
              {{ formattedTime }}
            </span>
          </div>

          <!-- Section: Selected Seats -->
          <div>
            <div class="flex items-center justify-between pb-2 border-b border-border">
              <h3 class="text-sm font-semibold text-foreground">
                Chỗ ngồi đã chọn ({{ selectedSeats.length }})
              </h3>
              <span class="text-xs text-primary font-medium tabular-nums">
                {{ selectedSeats.length }}/10 chỗ
              </span>
            </div>

            <div v-if="selectedSeats.length === 0" class="py-6 text-center text-xs text-muted-foreground" data-testid="no-seats-selected">
              <p>Chưa có chỗ nào được chọn</p>
              <p class="text-[11px] mt-1 text-muted-foreground/80">Vui lòng nhấp vào chỗ ngồi ở sơ đồ bên trái</p>
            </div>

            <div v-else class="divide-y divide-border max-h-48 overflow-y-auto" data-testid="selected-seats-list">
              <div
                v-for="seatCode in selectedSeats"
                :key="seatCode"
                :data-testid="'selected-seat-item-' + seatCode"
                class="py-2.5 flex items-center justify-between text-xs"
              >
                <div class="flex items-center gap-2">
                  <span class="w-6 h-6 rounded bg-primary/10 text-primary font-bold flex items-center justify-center text-[11px]">
                    {{ seatCode }}
                  </span>
                  <div>
                    <span class="font-medium text-foreground">Chỗ {{ seatCode }}</span>
                  </div>
                </div>

                <div class="flex items-center gap-3">
                  <span class="font-semibold text-foreground tabular-nums">
                    {{ formatCurrency(backendSeats.find(s => (s.seatNumber || s.code) === seatCode)?.finalPrice || trip.basePrice) }} đ
                  </span>
                  <button
                    v-if="!isRunning && !isHoldUnknownOutcome"
                    type="button"
                    :data-testid="'remove-seat-' + seatCode"
                    @click="removeSeat(seatCode)"
                    class="text-muted-foreground hover:text-destructive transition-colors p-1"
                    title="Bỏ chọn"
                  >
                    ✕
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Section: Price Breakdown -->
          <div class="space-y-1.5 text-xs text-muted-foreground pt-2 border-t border-border">
            <div class="flex justify-between">
              <span>Đơn giá danh mục (x{{ selectedSeats.length }}):</span>
              <span class="tabular-nums font-medium text-foreground" data-testid="subtotal-amount">
                {{ formatCurrency(totalPrice) }} đ
              </span>
            </div>
          </div>

          <!-- Total Price Section -->
          <div class="pt-3 border-t border-border flex items-baseline justify-between">
            <div>
              <span class="text-xs text-muted-foreground block">Tổng tiền thanh toán</span>
            </div>
            <span class="text-2xl font-bold text-primary tabular-nums" data-testid="total-price-display">
              {{ formatCurrency(totalPrice) }} <span class="text-sm font-semibold">đ</span>
            </span>
          </div>

          <!-- Primary CTA Button -->
          <div class="space-y-2">
            <button
              type="button"
              data-testid="btn-proceed-booking"
              @click="proceedToCheckout"
              :disabled="selectedSeats.length === 0 || holding || isHoldUnknownOutcome || cancelling || isCancelUnresolved"
              class="w-full h-12 bg-primary hover:bg-primary/90 disabled:bg-muted disabled:text-muted-foreground disabled:cursor-not-allowed text-primary-foreground font-semibold rounded-lg shadow-xs transition-all flex items-center justify-center gap-2 text-sm"
            >
              <Loader2 v-if="holding" class="w-4 h-4 animate-spin text-primary-foreground" />
              <span>{{ isRunning ? 'Tiến Hành Thanh Toán' : 'Giữ Chỗ & Tiếp Tục' }}</span>
              <ArrowRight v-if="!holding" class="w-4 h-4" />
            </button>

            <!-- Cancel Held Booking CTA Button (Visible when Hold is Active) -->
            <AlertDialog v-if="isRunning && heldBooking">
              <AlertDialogTrigger as-child>
                <button
                  type="button"
                  data-testid="btn-cancel-hold-trigger"
                  :disabled="cancelling"
                  class="w-full py-2 text-xs font-semibold text-destructive hover:bg-destructive/10 rounded-lg transition-colors flex items-center justify-center gap-1.5"
                >
                  <Trash2 class="w-3.5 h-3.5" />
                  <span>Hủy phiên giữ chỗ</span>
                </button>
              </AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>Hủy giữ chỗ hiện tại?</AlertDialogTitle>
                  <AlertDialogDescription>
                    Hành động này sẽ giải phóng toàn bộ chỗ ngồi {{ selectedSeats.join(', ') }} đang được khóa cho quý khách để người khác có thể chọn. Quý khách có chắc chắn muốn hủy?
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Không, giữ lại</AlertDialogCancel>
                  <AlertDialogAction
                    data-testid="btn-confirm-cancel-hold"
                    @click="handleCancelBooking"
                    class="bg-destructive hover:bg-destructive/90 text-destructive-foreground"
                  >
                    Xác nhận hủy
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
