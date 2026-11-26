<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import { useAuthStore } from '@/modules/auth/store'
import SeatMap2Deck from '@/shared/components/SeatMap2Deck.vue'
import { useCountdown } from '@/shared/composables/useCountdown'
import { catalogBookingApi } from '../api'
import {
  Bus,
  ArrowLeft,
  Clock,
  ShieldCheck,
  Sparkles,
  AlertCircle,
  RefreshCw,
  Loader2,
  Trash2,
  ArrowRight
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
const selectedSeats = ref([])
const backendSeats = ref([])

// Active booking information after successful hold
const heldBooking = ref(null)

// Trip Metadata initialized from route history.state or defaults based on tripId
const initialTrip = history.state?.trip
const trip = ref(initialTrip ? {
  id: initialTrip.id,
  tripCode: initialTrip.tripCode || `TRIP-${initialTrip.id}`,
  route: initialTrip.route || `${initialTrip.departureStationName || initialTrip.departureStationCode || 'Điểm đi'} → ${initialTrip.arrivalStationName || initialTrip.arrivalStationCode || 'Điểm đến'}`,
  departureTime: initialTrip.departureTime || '',
  operatorName: initialTrip.operator || initialTrip.operatorName || 'Nhà xe đối tác VeloBus',
  coachType: initialTrip.transportType || 'Xe khách cao cấp',
  vehiclePlate: initialTrip.plate || '',
  duration: initialTrip.duration || '',
  basePrice: Number(initialTrip.basePrice) || 0
} : {
  id: tripId.value,
  tripCode: `TRIP-${tripId.value}`,
  route: `Chuyến xe #${tripId.value}`,
  departureTime: '',
  operatorName: 'Nhà xe đối tác VeloBus',
  coachType: 'Xe khách cao cấp',
  vehiclePlate: '',
  duration: '',
  basePrice: 0
})

// Countdown Hook (Server expiresAt sync)
const { remainingSeconds, formattedTime, isRunning, start: startCountdown, stop: stopCountdown } = useCountdown(() => {
  toast.warning('Thời gian 10 phút đã hết. Vui lòng chọn lại giường nằm.', {
    description: 'Phiên giữ chỗ của bạn đã kết thúc tự động.'
  })
  heldBooking.value = null
  selectedSeats.value = []
  fetchTripSeats()
})

// Calculate total price based on individual seat finalPrice or basePrice
const totalPrice = computed(() => {
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

function onToggleSeat(seatCode) {
  if (isRunning.value && heldBooking.value) {
    toast.info('Phiên giữ chỗ đang kích hoạt. Vui lòng thanh toán hoặc hủy giữ chỗ để đổi ghế.')
    return
  }

  const idx = selectedSeats.value.indexOf(seatCode)
  if (idx > -1) {
    selectedSeats.value.splice(idx, 1)
  } else {
    if (selectedSeats.value.length >= 10) {
      toast.warning('Mỗi hành khách được chọn tối đa 10 giường trong một lần đặt vé.')
      return
    }
    selectedSeats.value.push(seatCode)
  }
}

function removeSeat(seatCode) {
  if (isRunning.value && heldBooking.value) {
    toast.info('Không thể xóa ghế khi đang trong phiên giữ chỗ. Vui lòng hủy phiên nếu muốn chọn lại.')
    return
  }
  const idx = selectedSeats.value.indexOf(seatCode)
  if (idx > -1) selectedSeats.value.splice(idx, 1)
}

async function handleHoldSeats() {
  if (holding.value) return
  if (selectedSeats.value.length === 0) {
    toast.warning('Vui lòng chọn ít nhất 1 giường trên sơ đồ xe.')
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

  holding.value = true
  try {
    const res = await catalogBookingApi.holdSeats({
      tripId: Number(tripId.value),
      seatNumbers: selectedSeats.value
    })

    const booking = res?.data || res
    if (!booking || !booking.bookingReference) {
      throw new Error('Dữ liệu phản hồi giữ chỗ từ máy chủ không hợp lệ.')
    }

    heldBooking.value = booking

    if (booking.expiresAt) {
      startCountdown(booking.expiresAt)
    }

    toast.success(`Giữ chỗ thành công! Đã khóa ${selectedSeats.value.join(', ')} trong 10 phút. Vui lòng thanh toán!`)
  } catch (err) {
    console.error('Hold seats failed:', err)
    if (err?.code === 'SEAT_ALREADY_RESERVED' || err?.message?.includes('SEAT_ALREADY_RESERVED')) {
      toast.error('Một hoặc nhiều ghế đã bị hành khách khác chọn. Vui lòng chọn ghế khác!')
    } else {
      toast.error(err?.message ? `Không thể giữ chỗ: ${err.message}` : 'Không thể giữ chỗ do lỗi kết nối hoặc hệ thống bận.')
    }
    await fetchTripSeats()
  } finally {
    holding.value = false
  }
}

async function handleCancelBooking() {
  if (!heldBooking.value?.bookingReference || cancelling.value) return
  cancelling.value = true
  try {
    await catalogBookingApi.cancelBooking(heldBooking.value.bookingReference)
    stopCountdown()
    heldBooking.value = null
    selectedSeats.value = []
    toast.success('Đã hủy giữ chỗ thành công.')
    await fetchTripSeats()
  } catch (err) {
    console.error('Cancel booking failed:', err)
    toast.error('Không thể hủy giữ chỗ trên máy chủ: ' + (err?.message || ''))
  } finally {
    cancelling.value = false
  }
}

function proceedToCheckout() {
  if (!heldBooking.value) {
    handleHoldSeats()
    return
  }
  router.push(`/checkout/${heldBooking.value.bookingReference}`)
}

function formatCurrency(val) {
  if (!val) return '0'
  return Number(val).toLocaleString('vi-VN')
}

onMounted(() => {
  fetchTripSeats()

  // Khôi phục danh sách ghế đã chọn trước khi bị điều hướng đăng nhập (nếu có)
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
          <span class="text-xs text-muted-foreground flex items-center gap-1">
            <ShieldCheck class="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" />
            Nhà xe uy tín đã xác thực
          </span>
          <span v-if="trip.vehiclePlate" class="text-xs text-muted-foreground">
            • Biển số: <strong class="text-foreground">{{ trip.vehiclePlate }}</strong>
          </span>
          <span class="text-xs text-muted-foreground font-mono">
            • Mã: <strong class="text-foreground">{{ trip.tripCode }}</strong>
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
          <span v-if="trip.duration">⏳ {{ trip.duration }}</span>
          <span>🚌 {{ trip.coachType }}</span>
        </p>
      </div>

      <router-link
        to="/search"
        class="inline-flex items-center justify-center px-4 py-2 text-xs font-semibold rounded-lg border border-border text-foreground hover:bg-muted transition-all self-start md:self-auto"
      >
        <ArrowLeft class="w-3.5 h-3.5 mr-1.5" />
        Đổi chuyến xe khác
      </router-link>
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
      <h3 class="text-sm font-bold text-foreground">Không thể tải danh sách ghế</h3>
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
          :selected-seats="selectedSeats"
          :held-seats="heldSeatCodes"
          :base-price="trip.basePrice"
          :max-selectable="10"
          @toggle-seat="onToggleSeat"
        />

        <!-- Amenities Info Card -->
        <div class="bg-card text-card-foreground rounded-xl border border-border p-4 shadow-xs">
          <h4 class="text-xs font-semibold text-foreground mb-2 flex items-center gap-1.5">
            <Sparkles class="w-3.5 h-3.5 text-primary" />
            <span>Tiện ích khoang giường VIP bao gồm:</span>
          </h4>
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs text-muted-foreground">
            <div class="flex items-center gap-1.5">✓ Rèm che riêng tư</div>
            <div class="flex items-center gap-1.5">✓ Cổng sạc Type-C & USB</div>
            <div class="flex items-center gap-1.5">✓ Nước suối & khăn lạnh</div>
            <div class="flex items-center gap-1.5">✓ Wifi tốc độ cao miễn phí</div>
          </div>
        </div>
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
              <span class="text-lg animate-pulse">⏳</span>
              <div>
                <p class="text-[11px] font-semibold text-amber-800 dark:text-amber-300 uppercase tracking-wide">
                  Thời gian giữ chỗ của bạn
                </p>
                <p class="text-xs text-amber-700 dark:text-amber-400">Ghế đang được khóa trên hệ thống</p>
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
                {{ selectedSeats.length }}/10 giường
              </span>
            </div>

            <div v-if="selectedSeats.length === 0" class="py-6 text-center text-xs text-muted-foreground" data-testid="no-seats-selected">
              <p>Chưa có giường nào được chọn</p>
              <p class="text-[11px] mt-1 text-muted-foreground/80">Vui lòng nhấp vào giường trống ở sơ đồ xe bên trái</p>
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
                    <span class="font-medium text-foreground">Giường {{ seatCode }}</span>
                    <span class="text-[11px] text-muted-foreground ml-1">
                      ({{ seatCode.startsWith('A') ? 'Tầng 1' : 'Tầng 2' }})
                    </span>
                  </div>
                </div>

                <div class="flex items-center gap-3">
                  <span class="font-semibold text-foreground tabular-nums">
                    {{ formatCurrency(backendSeats.find(s => (s.seatNumber || s.code) === seatCode)?.finalPrice || trip.basePrice) }} đ
                  </span>
                  <button
                    v-if="!isRunning"
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
              <span>Giá vé giường nằm (x{{ selectedSeats.length }}):</span>
              <span class="tabular-nums font-medium text-foreground" data-testid="subtotal-amount">
                {{ formatCurrency(totalPrice) }} đ
              </span>
            </div>
            <div class="flex justify-between">
              <span>Bảo hiểm hành khách:</span>
              <span class="text-emerald-600 dark:text-emerald-400 font-medium">Miễn phí (0 đ)</span>
            </div>
            <div class="flex justify-between">
              <span>Phí tiện ích & dịch vụ:</span>
              <span class="text-emerald-600 dark:text-emerald-400 font-medium">0 đ</span>
            </div>
          </div>

          <!-- Total Price Section -->
          <div class="pt-3 border-t border-border flex items-baseline justify-between">
            <div>
              <span class="text-xs text-muted-foreground block">Tổng tiền thanh toán</span>
              <span class="text-[10px] text-muted-foreground/70">(Đã bao gồm VAT & bảo hiểm)</span>
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
              :disabled="selectedSeats.length === 0 || holding"
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
                    Hành động này sẽ giải phóng toàn bộ giường {{ selectedSeats.join(', ') }} đang được khóa cho quý khách để người khác có thể chọn. Quý khách có chắc chắn muốn hủy?
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

          <!-- Trust & Security Badges -->
          <div class="pt-2 text-center text-[11px] text-muted-foreground space-y-1">
            <p class="flex items-center justify-center gap-1.5 text-emerald-600 dark:text-emerald-400 font-medium">
              <ShieldCheck class="w-3.5 h-3.5" />
              Thanh toán bảo mật chuẩn VNPay Sandbox
            </p>
            <p>Hỗ trợ đổi trả & hoàn tiền 100% trước 24h xuất bến</p>
          </div>
        </div>

        <!-- Hotline Support -->
        <div class="text-center text-xs text-muted-foreground">
          Cần hỗ trợ chọn giường? Hotline <strong class="text-foreground">1900 6868</strong> (24/7)
        </div>
      </div>
    </div>
  </div>
</template>
