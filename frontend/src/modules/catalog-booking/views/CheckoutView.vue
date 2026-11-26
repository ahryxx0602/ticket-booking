<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import { useAuthStore } from '@/modules/auth/store'
import { catalogBookingApi } from '../api'
import { validateBookingResponse, parseIsoDate } from '../utils/booking-validator'
import { useCountdown } from '@/shared/composables/useCountdown'
import FormField from '@/shared/components/FormField.vue'
import AppButton from '@/shared/components/AppButton.vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import {
  Clock,
  CreditCard,
  ShieldCheck,
  AlertCircle,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  ArrowLeft,
  Loader2,
  RefreshCw,
  Bus,
  User,
  Phone,
  Mail,
  FileText,
  ChevronRight,
  ExternalLink
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const bookingReference = computed(() => {
  return String(route.params.bookingId || route.params.reference || '').trim()
})

// Core State
const booking = ref(null)
const isLoading = ref(true)
const loadError = ref('')
const isSubmittingPayment = ref(false)
const isVerifyingPayment = ref(false)
const verifyPollCount = ref(0)
const paymentGatewayError = ref('')

// Passenger contact state (for customer confirmation & contact)
const passengerForm = ref({
  fullName: authStore.user?.fullName || '',
  phone: authStore.user?.phone || '',
  email: authStore.user?.email || '',
  note: ''
})

const formErrors = ref({
  fullName: '',
  phone: '',
  email: ''
})

// Server-synced countdown
let timerInterval = null
const remainingSeconds = ref(0)
const isExpired = computed(() => {
  return remainingSeconds.value <= 0 && booking.value?.status === 'HELD'
})
const isExpiringSoon = computed(() => {
  return remainingSeconds.value > 0 && remainingSeconds.value < 60 && booking.value?.status === 'HELD'
})

const formattedTime = computed(() => {
  const mins = Math.floor(remainingSeconds.value / 60)
  const secs = remainingSeconds.value % 60
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
})

function updateCountdown() {
  if (!booking.value || booking.value.status !== 'HELD' || !booking.value.expiresAt) {
    remainingSeconds.value = 0
    return
  }
  const expiryMs = parseIsoDate(booking.value.expiresAt)
  if (!Number.isFinite(expiryMs)) {
    remainingSeconds.value = 0
    return
  }
  const diffSec = Math.max(0, Math.floor((expiryMs - Date.now()) / 1000))
  remainingSeconds.value = diffSec
}

function startCountdown() {
  stopCountdown()
  updateCountdown()
  if (remainingSeconds.value > 0) {
    timerInterval = setInterval(updateCountdown, 1000)
  }
}

function stopCountdown() {
  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }
}

// Bounded polling for authoritative confirmation
let pollTimer = null

function stopPolling() {
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
  isVerifyingPayment.value = false
}

async function pollAuthoritativeConfirmation(attempt = 1) {
  if (attempt > 10) {
    isVerifyingPayment.value = false
    toast.info('Hệ thống đang xử lý xác nhận thanh toán từ VNPay. Vui lòng bấm "Làm mới" hoặc kiểm tra lại sau ít phút.')
    return
  }

  verifyPollCount.value = attempt
  isVerifyingPayment.value = true

  try {
    const res = await catalogBookingApi.getBookingDetail(bookingReference.value)
    const data = res.data?.data || res.data
    const validation = validateBookingResponse(data, {
      expectedUserId: authStore.user?.id,
      allowTerminalReplay: true,
      allowPastExpiry: true
    })

    if (validation.valid && data) {
      booking.value = data
      if (data.status === 'CONFIRMED') {
        stopPolling()
        toast.success('Xác nhận thanh toán thành công! Vé của bạn đã được đảm bảo.')
        return
      }
      if (data.status === 'CANCELLED' || data.status === 'EXPIRED') {
        stopPolling()
        toast.error(`Đơn đặt chỗ đã ở trạng thái ${data.status}.`)
        return
      }
    }
  } catch (err) {
    console.warn('[Checkout] Confirmation poll error:', err)
  }

  pollTimer = setTimeout(() => {
    pollAuthoritativeConfirmation(attempt + 1)
  }, 2000)
}

// Load Booking Details
async function fetchBookingDetail() {
  if (!bookingReference.value) {
    loadError.value = 'Mã tham chiếu đơn đặt chỗ không hợp lệ'
    isLoading.value = false
    return
  }

  isLoading.value = true
  loadError.value = ''
  paymentGatewayError.value = ''

  try {
    const res = await catalogBookingApi.getBookingDetail(bookingReference.value)
    const data = res.data?.data || res.data

    const validation = validateBookingResponse(data, {
      expectedUserId: authStore.user?.id,
      allowTerminalReplay: true,
      allowPastExpiry: true
    })

    if (!validation.valid) {
      loadError.value = validation.error || 'Dữ liệu đơn đặt chỗ từ máy chủ không hợp lệ'
      isLoading.value = false
      return
    }

    booking.value = data

    if (data.status === 'HELD') {
      startCountdown()
    } else {
      stopCountdown()
    }

    // Inspect route query for VNPay return redirect
    const vnpCode = route.query.vnp_ResponseCode
    if (vnpCode !== undefined && vnpCode !== null) {
      if (vnpCode === '00') {
        // Returned with success code: poll authoritative backend state
        pollAuthoritativeConfirmation(1)
      } else {
        paymentGatewayError.value = `Giao dịch thanh toán qua VNPay không thành công hoặc đã bị hủy bởi khách hàng (Mã kết quả: ${vnpCode}).`
      }
    }
  } catch (err) {
    console.error('[Checkout] Failed to fetch booking:', err)
    const msg = err.response?.data?.message || err.message || 'Không thể kết nối đến máy chủ để lấy thông tin đơn vé.'
    loadError.value = msg
  } finally {
    isLoading.value = false
  }
}

// Form Validation
function validateForm() {
  let isValid = true
  formErrors.value = { fullName: '', phone: '', email: '' }

  if (!passengerForm.value.fullName.trim()) {
    formErrors.value.fullName = 'Vui lòng nhập họ và tên hành khách'
    isValid = false
  }

  const phoneRegex = /(84|0[3|5|7|8|9])+([0-9]{8})\b/
  if (!passengerForm.value.phone.trim() || !phoneRegex.test(passengerForm.value.phone)) {
    formErrors.value.phone = 'Số điện thoại không đúng định dạng Việt Nam (10 số)'
    isValid = false
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!passengerForm.value.email.trim() || !emailRegex.test(passengerForm.value.email)) {
    formErrors.value.email = 'Email không hợp lệ để nhận thông báo xác nhận vé'
    isValid = false
  }

  return isValid
}

// Payment Action
async function handleProceedPayment() {
  if (isExpired.value) {
    toast.error('Phiên giữ chỗ đã hết hạn. Vui lòng quay lại sơ đồ chọn ghế mới.')
    return
  }

  if (isExpiringSoon.value) {
    toast.warning('Thời gian giữ chỗ còn dưới 60 giây. Cổng thanh toán yêu cầu tối thiểu 60 giây để xử lý an toàn.')
    return
  }

  if (booking.value?.status !== 'HELD') {
    toast.error(`Không thể thanh toán đơn vé đang ở trạng thái ${booking.value?.status}.`)
    return
  }

  if (!validateForm()) {
    toast.warning('Vui lòng kiểm tra lại thông tin người liên hệ nhận vé.')
    return
  }

  isSubmittingPayment.value = true
  paymentGatewayError.value = ''

  try {
    const res = await catalogBookingApi.createVnPayCheckout({
      bookingReference: booking.value.bookingReference,
      paymentMethod: 'VNPAY'
    })

    const data = res.data?.data || res.data
    const redirectUrl = data?.redirectPaymentUrl

    if (!redirectUrl || typeof redirectUrl !== 'string') {
      throw new Error('Máy chủ không cung cấp đường dẫn chuyển tiếp VNPay hợp lệ.')
    }

    // Validate URL scheme
    if (!redirectUrl.startsWith('http://') && !redirectUrl.startsWith('https://')) {
      throw new Error('Đường dẫn chuyển tiếp cổng thanh toán không an toàn.')
    }

    toast.info('Đang chuyển hướng sang cổng thanh toán VNPay...')

    // Navigate to VNPay Gateway
    window.location.href = redirectUrl
  } catch (err) {
    console.error('[Checkout] Payment initialization failed:', err)
    const code = err.response?.data?.code
    const message = err.response?.data?.message || err.message

    if (code === 'BOOKING_EXPIRING_SOON') {
      paymentGatewayError.value = 'Phiên giữ chỗ sắp hết hạn (dưới 60 giây). Vui lòng quay lại chọn ghế mới để hoàn tất đặt vé an toàn.'
      toast.warning('Thời gian giữ chỗ không đủ để xử lý giao dịch qua cổng VNPay.')
    } else {
      paymentGatewayError.value = message || 'Không thể khởi tạo phiên thanh toán VNPay. Vui lòng thử lại.'
      toast.error(message || 'Lỗi khởi tạo thanh toán')
    }
  } finally {
    isSubmittingPayment.value = false
  }
}

onMounted(() => {
  fetchBookingDetail()
})

onUnmounted(() => {
  stopCountdown()
  stopPolling()
})
</script>

<template>
  <div class="max-w-6xl mx-auto px-4 py-8 space-y-6">
    <!-- Top Navigation / Breadcrumb -->
    <div class="flex items-center justify-between">
      <router-link
        to="/search"
        class="inline-flex items-center gap-1.5 text-xs font-semibold text-muted-foreground hover:text-foreground transition-colors"
      >
        <ArrowLeft class="w-4 h-4" />
        <span>Quay lại tìm chuyến xe</span>
      </router-link>

      <span v-if="booking" class="text-xs font-mono text-muted-foreground">
        Mã đơn: <strong class="text-foreground">{{ booking.bookingReference }}</strong>
      </span>
    </div>

    <!-- Loading Skeleton -->
    <div v-if="isLoading" class="p-12 text-center bg-card rounded-xl border border-border space-y-4">
      <Loader2 class="w-8 h-8 animate-spin mx-auto text-primary" />
      <p class="text-xs text-muted-foreground">Đang tải và xác thực chi tiết đơn đặt chỗ từ máy chủ...</p>
    </div>

    <!-- Error State -->
    <div
      v-else-if="loadError"
      class="bg-destructive/10 border border-destructive/20 rounded-xl p-6 text-center space-y-3"
    >
      <AlertCircle class="w-8 h-8 text-destructive mx-auto" />
      <h3 class="text-sm font-bold text-destructive">Không Thể Xác Thực Đơn Đặt Chỗ</h3>
      <p class="text-xs text-muted-foreground max-w-md mx-auto">{{ loadError }}</p>
      <div class="pt-2 flex justify-center gap-3">
        <AppButton variant="outline" size="sm" @click="fetchBookingDetail">
          <RefreshCw class="w-3.5 h-3.5 mr-1" />
          Thử Lại
        </AppButton>
        <router-link to="/search">
          <AppButton size="sm">Tìm Chuyến Khác</AppButton>
        </router-link>
      </div>
    </div>

    <!-- Main Content -->
    <div v-else-if="booking" class="space-y-6">
      <!-- 1. HELD: Active Hold Countdown Banner -->
      <div
        v-if="booking.status === 'HELD'"
        class="rounded-xl p-4 flex flex-col sm:flex-row items-center justify-between gap-4 border transition-colors shadow-xs"
        :class="[
          isExpired
            ? 'bg-destructive/10 border-destructive/30 text-destructive'
            : isExpiringSoon
              ? 'bg-amber-500/10 border-amber-500/30 text-amber-600 dark:text-amber-400'
              : 'bg-primary/5 border-primary/20 text-primary'
        ]"
      >
        <div class="flex items-center gap-3">
          <Clock class="w-6 h-6 shrink-0" :class="{ 'animate-pulse': isExpiringSoon }" />
          <div>
            <h2 class="text-xs font-bold uppercase tracking-wider">
              {{
                isExpired
                  ? 'Phiên Giữ Chỗ Đã Hết Hạn'
                  : isExpiringSoon
                    ? 'Thời Gian Giữ Chỗ Sắp Hết (Dưới 60 Giây)'
                    : 'Phiên Giữ Chỗ 10 Phút Đang Hiệu Lực'
              }}
            </h2>
            <p class="text-xs text-muted-foreground">
              {{
                isExpired
                  ? 'Vị trí ghế đã được giải phóng trên hệ thống. Quý khách vui lòng chọn lại ghế mới.'
                  : isExpiringSoon
                    ? 'Cổng thanh toán yêu cầu tối thiểu 60 giây để xử lý an toàn. Vui lòng quay lại chọn ghế mới để tránh lỗi.'
                    : 'Ghế của bạn đã được khóa an toàn trên máy chủ. Vui lòng thanh toán trước khi hết hạn.'
              }}
            </p>
          </div>
        </div>

        <div class="flex items-center gap-2 bg-background px-3.5 py-1.5 rounded-lg border border-border shadow-xs">
          <span class="text-xs font-medium text-muted-foreground">Thời gian còn lại:</span>
          <span
            class="text-base font-mono font-extrabold tabular-nums"
            :class="[
              isExpired
                ? 'text-destructive'
                : isExpiringSoon
                  ? 'text-amber-500 animate-pulse'
                  : 'text-primary'
            ]"
          >
            {{ formattedTime }}
          </span>
        </div>
      </div>

      <!-- 2. CONFIRMED State Screen -->
      <div
        v-if="booking.status === 'CONFIRMED'"
        class="bg-card rounded-xl border border-border p-8 text-center space-y-4 shadow-sm"
      >
        <div class="w-16 h-16 rounded-full bg-emerald-500/10 text-emerald-600 flex items-center justify-center mx-auto">
          <CheckCircle2 class="w-10 h-10" />
        </div>
        <div class="space-y-1">
          <h2 class="text-xl font-bold text-foreground">Thanh Toán Thành Công & Đã Xác Nhận Vé</h2>
          <p class="text-xs text-muted-foreground max-w-lg mx-auto">
            Giao dịch thanh toán đã hoàn tất. Đơn vé của bạn đã được máy chủ ghi nhận an toàn và chuyển trạng thái xác nhận chính thức.
          </p>
        </div>

        <div class="max-w-md mx-auto bg-muted/50 rounded-lg p-4 border border-border text-xs text-left space-y-2">
          <div class="flex justify-between">
            <span class="text-muted-foreground">Mã tham chiếu:</span>
            <strong class="font-mono text-foreground">{{ booking.bookingReference }}</strong>
          </div>
          <div class="flex justify-between">
            <span class="text-muted-foreground">Mã chuyến xe:</span>
            <strong class="text-foreground">#{{ booking.tripId }}</strong>
          </div>
          <div class="flex justify-between">
            <span class="text-muted-foreground">Chỗ ngồi đã xác nhận:</span>
            <strong class="text-primary font-mono">{{ (booking.seatNumbers || []).join(', ') }}</strong>
          </div>
          <div class="flex justify-between">
            <span class="text-muted-foreground">Tổng tiền thanh toán:</span>
            <strong class="text-foreground font-mono">{{ Number(booking.totalAmount).toLocaleString('vi-VN') }} đ</strong>
          </div>
        </div>

        <div class="pt-4 flex flex-wrap justify-center gap-3">
          <router-link to="/my-bookings">
            <AppButton size="sm">
              <FileText class="w-3.5 h-3.5 mr-1" />
              Xem Vé Của Tôi
            </AppButton>
          </router-link>
          <router-link to="/">
            <AppButton variant="outline" size="sm">
              Về Trang Chủ
            </AppButton>
          </router-link>
        </div>
      </div>

      <!-- 3. CANCELLED or EXPIRED State Screen -->
      <div
        v-else-if="booking.status === 'CANCELLED' || booking.status === 'EXPIRED'"
        class="bg-card rounded-xl border border-border p-8 text-center space-y-4 shadow-sm"
      >
        <div class="w-16 h-16 rounded-full bg-destructive/10 text-destructive flex items-center justify-center mx-auto">
          <XCircle class="w-10 h-10" />
        </div>
        <div class="space-y-1">
          <h2 class="text-xl font-bold text-foreground">
            {{ booking.status === 'CANCELLED' ? 'Đơn Đặt Chỗ Đã Hủy' : 'Phiên Giữ Chỗ Đã Hết Hạn' }}
          </h2>
          <p class="text-xs text-muted-foreground max-w-lg mx-auto">
            {{
              booking.status === 'CANCELLED'
                ? 'Đơn đặt chỗ đã được hủy theo yêu cầu. Toàn bộ ghế đã được giải phóng.'
                : 'Thời gian 10 phút giữ chỗ đã kết thúc. Vị trí ghế đã được giải phóng để hành khách khác có thể đặt.'
            }}
          </p>
        </div>

        <div class="pt-4 flex justify-center gap-3">
          <router-link :to="`/trips/${booking.tripId}/seats`">
            <AppButton size="sm">
              <Bus class="w-3.5 h-3.5 mr-1" />
              Chọn Lại Ghế Chuyến Xe Này
            </AppButton>
          </router-link>
          <router-link to="/search">
            <AppButton variant="outline" size="sm">Tìm Chuyến Khác</AppButton>
          </router-link>
        </div>
      </div>

      <!-- 4. HELD: Active Two-Column Checkout Form -->
      <div v-else-if="booking.status === 'HELD'" class="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
        <!-- Left: Passenger Form & Payment Method -->
        <div class="lg:col-span-2 space-y-6">
          <!-- Gateway Return Polling Notice -->
          <div
            v-if="isVerifyingPayment"
            class="bg-primary/5 border border-primary/20 rounded-xl p-4 flex items-center gap-3"
          >
            <Loader2 class="w-5 h-5 text-primary animate-spin shrink-0" />
            <div class="text-xs">
              <strong class="text-foreground block">Đang đối soát kết quả giao dịch từ máy chủ VeloBus...</strong>
              <span class="text-muted-foreground">Lần kiểm tra {{ verifyPollCount }}/10. Vui lòng giữ kết nối.</span>
            </div>
          </div>

          <!-- Gateway Error Notice -->
          <div
            v-if="paymentGatewayError"
            class="bg-destructive/10 border border-destructive/20 rounded-xl p-4 flex items-center gap-3"
          >
            <AlertCircle class="w-5 h-5 text-destructive shrink-0" />
            <div class="text-xs">
              <strong class="text-destructive block">Thông Báo Thanh Toán</strong>
              <span class="text-muted-foreground">{{ paymentGatewayError }}</span>
            </div>
          </div>

          <!-- Passenger Info Card -->
          <div class="bg-card rounded-xl border border-border p-6 shadow-xs space-y-4">
            <div class="flex items-center gap-2 border-b border-border pb-3">
              <User class="w-5 h-5 text-primary" />
              <h2 class="text-sm font-bold text-foreground">Thông Tin Người Nhận Xác Nhận Vé</h2>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <FormField
                label="Họ và tên hành khách"
                :error="formErrors.fullName"
                required
              >
                <input
                  v-model="passengerForm.fullName"
                  type="text"
                  placeholder="Ví dụ: Nguyễn Văn A"
                  class="w-full h-10 px-3 rounded-lg border border-input text-xs text-foreground focus:ring-2 focus:ring-primary bg-background"
                  :class="{ 'border-destructive': formErrors.fullName }"
                />
              </FormField>

              <FormField
                label="Số điện thoại nhận tin nhắn"
                :error="formErrors.phone"
                required
              >
                <input
                  v-model="passengerForm.phone"
                  type="tel"
                  placeholder="0912345678"
                  class="w-full h-10 px-3 rounded-lg border border-input text-xs text-foreground focus:ring-2 focus:ring-primary bg-background"
                  :class="{ 'border-destructive': formErrors.phone }"
                />
              </FormField>
            </div>

            <FormField
              label="Email nhận vé điện tử"
              :error="formErrors.email"
              required
            >
              <input
                v-model="passengerForm.email"
                type="email"
                placeholder="example@gmail.com"
                class="w-full h-10 px-3 rounded-lg border border-input text-xs text-foreground focus:ring-2 focus:ring-primary bg-background"
                :class="{ 'border-destructive': formErrors.email }"
              />
            </FormField>

            <FormField label="Ghi chú thêm cho nhà xe (Không bắt buộc)">
              <textarea
                v-model="passengerForm.note"
                rows="2"
                placeholder="Ví dụ: Đón tại điểm dừng hoặc ghi chú hành lý..."
                class="w-full p-3 rounded-lg border border-input text-xs text-foreground focus:ring-2 focus:ring-primary bg-background resize-none"
              ></textarea>
            </FormField>
          </div>

          <!-- Payment Method Card (Strictly VNPay Gateway) -->
          <div class="bg-card rounded-xl border border-border p-6 shadow-xs space-y-4">
            <div class="flex items-center gap-2 border-b border-border pb-3">
              <CreditCard class="w-5 h-5 text-primary" />
              <h2 class="text-sm font-bold text-foreground">Phương Thức Thanh Toán Trực Tuyến</h2>
            </div>

            <div class="border-2 border-primary bg-primary/5 rounded-xl p-4 flex items-center justify-between">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-lg bg-background border border-border flex items-center justify-center text-primary font-bold text-xs">
                  VNPAY
                </div>
                <div>
                  <span class="text-xs font-bold text-foreground block">Cổng Thanh Toán Trực Tuyến VNPay (Sandbox)</span>
                  <span class="text-[11px] text-muted-foreground">
                    Hỗ trợ quét mã QR Ngân hàng (VNPAY-QR), Thẻ ATM Nội địa & Thẻ Quốc tế Visa/Mastercard.
                  </span>
                </div>
              </div>
              <span class="text-[11px] font-semibold px-2 py-0.5 rounded bg-primary text-primary-foreground">
                Chính thức
              </span>
            </div>

            <div class="p-3 bg-muted/40 rounded-lg text-[11px] text-muted-foreground flex items-center gap-2">
              <ShieldCheck class="w-4 h-4 text-emerald-600 shrink-0" />
              <span>Giao dịch được bảo mật SSL qua cổng thanh toán đối tác. Máy chủ VeloBus không lưu trữ thông tin thẻ ngân hàng.</span>
            </div>
          </div>
        </div>

        <!-- Right: Order Summary Sticky Card -->
        <div class="space-y-4 lg:sticky lg:top-24">
          <div class="bg-card rounded-xl border border-border p-5 shadow-xs space-y-4">
            <h2 class="text-xs font-bold text-foreground uppercase tracking-wider pb-3 border-b border-border">
              Tóm Tắt Đơn Vé
            </h2>

            <div class="space-y-3 text-xs">
              <div class="flex justify-between">
                <span class="text-muted-foreground">Mã đơn vé:</span>
                <strong class="font-mono text-foreground text-[11px]">{{ booking.bookingReference }}</strong>
              </div>

              <div class="flex justify-between">
                <span class="text-muted-foreground">Mã chuyến xe:</span>
                <strong class="text-foreground">#{{ booking.tripId }}</strong>
              </div>

              <div class="flex justify-between items-center pt-2 border-t border-border">
                <span class="text-muted-foreground">Ghế đã giữ:</span>
                <div class="flex flex-wrap gap-1">
                  <span
                    v-for="seat in booking.seatNumbers"
                    :key="seat"
                    class="px-2 py-0.5 rounded bg-primary/10 text-primary font-mono font-bold text-xs"
                  >
                    {{ seat }}
                  </span>
                </div>
              </div>

              <div class="flex justify-between pt-2 border-t border-border">
                <span class="text-muted-foreground">Số lượng ghế:</span>
                <strong class="text-foreground">{{ (booking.seatNumbers || []).length }} chỗ</strong>
              </div>

              <div class="flex justify-between items-baseline pt-2 border-t border-border">
                <span class="text-xs font-semibold text-muted-foreground">Tổng thanh toán:</span>
                <strong class="text-base font-bold text-primary font-mono tabular-nums">
                  {{ Number(booking.totalAmount).toLocaleString('vi-VN') }} đ
                </strong>
              </div>
            </div>

            <!-- Expiring soon guard alert -->
            <div
              v-if="isExpiringSoon"
              class="p-2.5 rounded-lg bg-amber-500/10 border border-amber-500/30 text-[11px] text-amber-600 dark:text-amber-400 space-y-1"
            >
              <div class="flex items-center gap-1 font-bold">
                <AlertTriangle class="w-3.5 h-3.5" />
                <span>Thời gian còn dưới 60 giây</span>
              </div>
              <p>Cổng thanh toán tạm dừng nhận yêu cầu mới do không đủ thời gian xử lý an toàn.</p>
            </div>

            <!-- Submit VNPay Button -->
            <AppButton
              class="w-full h-11 text-xs font-bold"
              :disabled="isSubmittingPayment || isVerifyingPayment || isExpiringSoon || isExpired"
              @click="handleProceedPayment"
            >
              <Loader2 v-if="isSubmittingPayment" class="w-4 h-4 animate-spin mr-1.5" />
              <ExternalLink v-else class="w-4 h-4 mr-1.5" />
              <span>
                {{
                  isExpired
                    ? 'Phiên giữ chỗ đã hết hạn'
                    : isExpiringSoon
                      ? 'Thời gian < 60s - Vui lòng chọn lại'
                      : isSubmittingPayment
                        ? 'Đang chuyển sang VNPay...'
                        : 'Thanh Toán Qua VNPay'
                }}
              </span>
            </AppButton>

            <p class="text-[10px] text-center text-muted-foreground">
              Bằng việc bấm thanh toán, bạn đồng ý với Điều khoản đặt vé & Chính sách bảo mật của VeloBus.
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
