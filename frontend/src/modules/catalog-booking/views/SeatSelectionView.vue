<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useAuthStore } from '@/modules/auth/store'
import SeatMap2Deck from '@/shared/components/SeatMap2Deck.vue'
import { useCountdown } from '@/shared/composables/useCountdown'
import { catalogBookingApi } from '../api'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const auth = useAuthStore()

const tripId = computed(() => route.params.id || 'TRIP-DEMO-01')
const loading = ref(false)
const holding = ref(false)
const selectedSeats = ref([])
const backendSeats = ref([])

// Active booking information after hold
const heldBooking = ref(null)

// Trip Metadata (Mock/Fallback or from API)
const trip = ref({
  id: tripId.value,
  route: 'Hà Nội (BX. Giáp Bát) → Đà Nẵng (BX. Trung Tâm)',
  departureTime: '19:00 - Thứ Tư, 25/11/2026',
  operatorName: 'Hoàng Long Limousine',
  coachType: 'VIP Luxury 36 Giường Nằm',
  vehiclePlate: '29B-888.99',
  duration: '14 giờ (760 km)',
  basePrice: 350000
})

// Countdown Hook (10 minutes)
const { remainingSeconds, formattedTime, isRunning, start: startCountdown, stop: stopCountdown } = useCountdown(() => {
  toast.add({
    severity: 'warn',
    summary: 'Hết phiên giữ chỗ',
    detail: 'Thời gian 10 phút đã hết. Vui lòng chọn lại giường nằm.',
    life: 5000
  })
  heldBooking.value = null
  selectedSeats.value = []
  fetchTripSeats()
})

const totalPrice = computed(() => {
  return selectedSeats.value.length * trip.value.basePrice
})

async function fetchTripSeats() {
  loading.value = true
  try {
    const res = await catalogBookingApi.getTripSeats(tripId.value)
    if (res && Array.isArray(res)) {
      backendSeats.value = res
    }
  } catch (err) {
    console.warn('Using default demo seats map', err)
  } finally {
    loading.value = false
  }
}

function onToggleSeat(seatCode) {
  if (isRunning.value && heldBooking.value) {
    toast.add({
      severity: 'info',
      summary: 'Phiên giữ chỗ đang kích hoạt',
      detail: 'Bạn đã khóa các ghế này. Vui lòng tiếp tục thanh toán hoặc đợi hết phiên.',
      life: 3000
    })
    return
  }

  const idx = selectedSeats.value.indexOf(seatCode)
  if (idx > -1) {
    selectedSeats.value.splice(idx, 1)
  } else {
    if (selectedSeats.value.length >= 4) {
      toast.add({
        severity: 'warn',
        summary: 'Giới hạn chọn chỗ',
        detail: 'Mỗi hành khách được chọn tối đa 4 giường trong một lần đặt vé.',
        life: 3000
      })
      return
    }
    selectedSeats.value.push(seatCode)
  }
}

function removeSeat(seatCode) {
  const idx = selectedSeats.value.indexOf(seatCode)
  if (idx > -1) selectedSeats.value.splice(idx, 1)
}

async function handleHoldSeats() {
  if (selectedSeats.value.length === 0) {
    toast.add({
      severity: 'warn',
      summary: 'Chưa chọn chỗ',
      detail: 'Vui lòng chọn ít nhất 1 giường trên sơ đồ xe.',
      life: 3000
    })
    return
  }

  // Chặn khách vãng lai (Guest) chưa đăng nhập: chuyển hướng sang login kèm lưu query redirect
  if (!auth.isAuthenticated) {
    try {
      sessionStorage.setItem(`velobus_pending_seats_${tripId.value}`, JSON.stringify(selectedSeats.value))
    } catch (e) {}

    toast.add({
      severity: 'info',
      summary: 'Yêu cầu đăng nhập',
      detail: 'Vui lòng đăng nhập tài khoản Khách hàng để thực hiện giữ chỗ.',
      life: 4000
    })
    router.push({
      path: '/login',
      query: {
        redirect: route.fullPath,
        reason: 'auth_required'
      }
    })
    return
  }

  // Chặn tài khoản Quản trị / Đối tác nhà xe đặt vé khách hàng
  if (!auth.isCustomer) {
    toast.add({
      severity: 'warn',
      summary: 'Quyền hạn không hợp lệ',
      detail: 'Tài khoản Quản trị viên / Nhà xe không được thực hiện đặt vé khách hàng.',
      life: 4000
    })
    return
  }

  holding.value = true
  try {
    const res = await catalogBookingApi.holdSeats({
      tripId: tripId.value,
      seatCodes: selectedSeats.value
    })

    heldBooking.value = res || {
      bookingReference: `VB-${Date.now().toString().slice(-6)}`,
      expiresAt: new Date(Date.now() + 10 * 60 * 1000).toISOString()
    }

    // Start 10-min countdown timer
    startCountdown(heldBooking.value.expiresAt || new Date(Date.now() + 10 * 60 * 1000))

    toast.add({
      severity: 'success',
      summary: 'Giữ chỗ thành công',
      detail: `Đã khóa ${selectedSeats.value.join(', ')} trong 10 phút. Vui lòng thanh toán!`,
      life: 4000
    })
  } catch (err) {
    toast.add({
      severity: 'error',
      summary: 'Không thể giữ chỗ',
      detail: err.message || 'Ghế đã bị khách khác chọn hoặc hệ thống bận. Vui lòng thử lại!',
      life: 4000
    })
    fetchTripSeats()
  } finally {
    holding.value = false
  }
}

function proceedToCheckout() {
  if (!heldBooking.value) {
    handleHoldSeats()
    return
  }
  router.push(`/checkout/${heldBooking.value.bookingReference || tripId.value}`)
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
  } catch (e) {}
})
</script>

<template>
  <div class="w-full py-6 space-y-6">
    <!-- Breadcrumb -->
    <nav class="flex items-center gap-2 text-xs text-text-muted">
      <router-link to="/" class="hover:text-primary-600">Trang chủ</router-link>
      <span>/</span>
      <router-link to="/" class="hover:text-primary-600">Tìm vé</router-link>
      <span>/</span>
      <span class="text-text-primary font-medium">Hà Nội - Đà Nẵng</span>
      <span>/</span>
      <span class="text-primary-600 font-semibold">Chọn chỗ & Đặt vé</span>
    </nav>

    <!-- Top Trip Info Bar (Clean Card with Border) -->
    <div class="bg-surface rounded-xl border border-border-default p-5 shadow-card flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div class="space-y-1.5">
        <div class="flex items-center gap-2.5 flex-wrap">
          <span class="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-50 text-primary-600 border border-blue-200">
            {{ trip.operatorName }}
          </span>
          <span class="text-xs text-text-muted flex items-center gap-1">
            <svg class="w-3.5 h-3.5 text-emerald-600" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
            </svg>
            Nhà xe uy tín đã xác thực
          </span>
          <span class="text-xs text-text-muted">• Biển số: <strong class="text-text-primary">{{ trip.vehiclePlate }}</strong></span>
        </div>
        <h1 class="text-xl font-bold text-text-primary flex items-center gap-2">
          {{ trip.route }}
        </h1>
        <p class="text-xs text-text-secondary flex items-center gap-3">
          <span>🕒 Khởi hành: <strong class="text-text-primary">{{ trip.departureTime }}</strong></span>
          <span>⏳ Thời gian chạy: {{ trip.duration }}</span>
          <span>🚌 {{ trip.coachType }}</span>
        </p>
      </div>

      <router-link
        to="/"
        class="inline-flex items-center justify-center px-4 py-2 text-xs font-semibold rounded-lg border border-border-default text-text-secondary hover:bg-canvas hover:text-text-primary transition-all self-start md:self-auto"
      >
        Đổi chuyến xe khác
      </router-link>
    </div>

    <!-- Main Content Split Grid (65% seat selection, 35% sticky summary) -->
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
      <!-- Left Column: Seat Map (65% / 8 cols) -->
      <div class="lg:col-span-8 space-y-6">
        <SeatMap2Deck
          :seats="backendSeats"
          :selected-seats="selectedSeats"
          :base-price="trip.basePrice"
          @toggle-seat="onToggleSeat"
        />

        <!-- Amenities Info Card -->
        <div class="bg-surface rounded-xl border border-border-default p-4 shadow-card">
          <h4 class="text-xs font-semibold text-text-primary mb-2 flex items-center gap-1.5">
            <span>✨</span> Tiện ích khoang giường VIP bao gồm:
          </h4>
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs text-text-secondary">
            <div class="flex items-center gap-1.5">✓ Rèm che riêng tư</div>
            <div class="flex items-center gap-1.5">✓ Cổng sạc Type-C & USB</div>
            <div class="flex items-center gap-1.5">✓ Nước suối & khăn lạnh</div>
            <div class="flex items-center gap-1.5">✓ Wifi tốc độ cao miễn phí</div>
          </div>
        </div>
      </div>

      <!-- Right Column: Sticky Booking Summary Card (35% / 4 cols) -->
      <div class="lg:col-span-4 lg:sticky lg:top-24 space-y-4">
        <div class="bg-surface rounded-xl border border-border-default p-5 shadow-card space-y-5">
          <!-- Active Hold Timer Countdown Widget -->
          <div
            v-if="isRunning"
            class="bg-amber-50 border border-amber-200 rounded-lg p-3.5 flex items-center justify-between"
          >
            <div class="flex items-center gap-2">
              <span class="text-lg animate-pulse">⏳</span>
              <div>
                <p class="text-[11px] font-semibold text-amber-900 uppercase tracking-wide">Thời gian giữ chỗ của bạn</p>
                <p class="text-xs text-amber-800">Ghế đang được khóa trên hệ thống</p>
              </div>
            </div>
            <span class="text-xl font-bold tabular-nums text-amber-900 bg-white px-2.5 py-1 rounded border border-amber-200 shadow-sm">
              {{ formattedTime }}
            </span>
          </div>

          <!-- Section: Selected Seats -->
          <div>
            <div class="flex items-center justify-between pb-2 border-b border-border-default">
              <h3 class="text-sm font-semibold text-text-primary">
                Chỗ ngồi đã chọn ({{ selectedSeats.length }})
              </h3>
              <span class="text-xs text-primary-600 font-medium tabular-nums">
                {{ selectedSeats.length }}/4 giường
              </span>
            </div>

            <div v-if="selectedSeats.length === 0" class="py-6 text-center text-xs text-text-muted">
              <p>Chưa có giường nào được chọn</p>
              <p class="text-[11px] mt-1 text-slate-400">Vui lòng nhấp vào giường trống ở sơ đồ xe bên trái</p>
            </div>

            <div v-else class="divide-y divide-border-default max-h-48 overflow-y-auto">
              <div
                v-for="seatCode in selectedSeats"
                :key="seatCode"
                class="py-2.5 flex items-center justify-between text-xs"
              >
                <div class="flex items-center gap-2">
                  <span class="w-6 h-6 rounded bg-blue-50 text-primary-600 font-bold flex items-center justify-center text-[11px]">
                    {{ seatCode }}
                  </span>
                  <div>
                    <span class="font-medium text-text-primary">Giường {{ seatCode }}</span>
                    <span class="text-[11px] text-text-muted ml-1">({{ seatCode.startsWith('A') ? 'Tầng 1' : 'Tầng 2' }})</span>
                  </div>
                </div>

                <div class="flex items-center gap-3">
                  <span class="font-semibold text-text-primary tabular-nums">
                    {{ trip.basePrice.toLocaleString('vi-VN') }} đ
                  </span>
                  <button
                    type="button"
                    @click="removeSeat(seatCode)"
                    class="text-slate-400 hover:text-red-500 transition-colors p-1"
                    title="Bỏ chọn"
                  >
                    ✕
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Section: Pick-up & Drop-off details -->
          <div class="bg-canvas rounded-lg p-3 border border-border-default space-y-2 text-xs">
            <div class="flex items-start gap-2">
              <div class="w-2 h-2 rounded-full bg-emerald-500 mt-1"></div>
              <div>
                <p class="font-medium text-text-primary">Đón: 19:00 • Bến xe Giáp Bát</p>
                <p class="text-[11px] text-text-muted">Quầy vé Hoàng Long số 12, Giải Phóng, Hà Nội</p>
              </div>
            </div>
            <div class="flex items-start gap-2">
              <div class="w-2 h-2 rounded-full bg-red-500 mt-1"></div>
              <div>
                <p class="font-medium text-text-primary">Trả: 09:00 • Bến xe Trung Tâm Đà Nẵng</p>
                <p class="text-[11px] text-text-muted">Số 159 Tôn Đức Thắng, Cẩm Lệ, Đà Nẵng</p>
              </div>
            </div>
          </div>

          <!-- Section: Price Breakdown -->
          <div class="space-y-1.5 text-xs text-text-secondary pt-2 border-t border-border-default">
            <div class="flex justify-between">
              <span>Giá vé giường nằm (x{{ selectedSeats.length }}):</span>
              <span class="tabular-nums font-medium text-text-primary">{{ totalPrice.toLocaleString('vi-VN') }} đ</span>
            </div>
            <div class="flex justify-between">
              <span>Bảo hiểm hành khách:</span>
              <span class="text-emerald-600 font-medium">Miễn phí (0 đ)</span>
            </div>
            <div class="flex justify-between">
              <span>Phí tiện ích & dịch vụ:</span>
              <span class="text-emerald-600 font-medium">0 đ</span>
            </div>
          </div>

          <!-- Total Price Section -->
          <div class="pt-3 border-t border-border-default flex items-baseline justify-between">
            <div>
              <span class="text-xs text-text-muted block">Tổng tiền thanh toán</span>
              <span class="text-[10px] text-slate-400">(Đã bao gồm VAT & bảo hiểm)</span>
            </div>
            <span class="text-2xl font-bold text-primary-600 tabular-nums">
              {{ totalPrice.toLocaleString('vi-VN') }} <span class="text-sm font-semibold">đ</span>
            </span>
          </div>

          <!-- Primary CTA Button -->
          <button
            type="button"
            @click="proceedToCheckout"
            :disabled="selectedSeats.length === 0 || holding"
            class="w-full h-12 bg-primary-600 hover:bg-primary-700 disabled:bg-slate-200 disabled:text-slate-400 disabled:cursor-not-allowed text-white font-semibold rounded-lg shadow-sm transition-all flex items-center justify-center gap-2 text-sm"
          >
            <svg v-if="holding" class="animate-spin w-4 h-4 text-white" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
            </svg>
            <span>{{ isRunning ? 'Tiến Hành Thanh Toán' : 'Giữ Chỗ & Tiếp Tục' }}</span>
            <span v-if="!holding">→</span>
          </button>

          <!-- Trust & Security Badges -->
          <div class="pt-2 text-center text-[11px] text-text-muted space-y-1">
            <p class="flex items-center justify-center gap-1.5 text-emerald-700">
              <svg class="w-3.5 h-3.5 text-emerald-600" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M2.166 4.999A11.954 11.954 0 0010 1.944 11.954 11.954 0 0017.834 5c.11.65.166 1.32.166 2.001 0 5.225-3.34 9.67-8 11.317C5.34 16.67 2 12.225 2 7c0-.682.057-1.35.166-2.001zm11.541 3.708a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
              </svg>
              Thanh toán bảo mật chuẩn VNPay Sandbox
            </p>
            <p>Hỗ trợ đổi trả & hoàn tiền 100% trước 24h xuất bến</p>
          </div>
        </div>

        <!-- Hotline Support -->
        <div class="text-center text-xs text-text-muted">
          Cần hỗ trợ chọn giường? Hotline <strong class="text-text-primary">1900 6868</strong> (24/7)
        </div>
      </div>
    </div>
  </div>
</template>
