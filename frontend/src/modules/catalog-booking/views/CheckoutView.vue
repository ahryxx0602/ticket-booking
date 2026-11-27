<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import { useAuthStore } from '@/modules/auth/store'
import { catalogBookingApi } from '../api'
import { validateBookingResponse, parseIsoDate } from '../utils/booking-validator'
import { useBookingOperationGuard } from '../composables/useBookingOperationGuard'
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert'
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
  ExternalLink,
  ShieldAlert
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const operationGuard = useBookingOperationGuard()

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
const isReconciliationRequired = ref(false)

// PRE-03: Cancellation block guard
const isBlockedByCancel = computed(() => {
  return operationGuard.isCheckoutBlocked(bookingReference.value)
})

// BLOCKER-03: Unresolved payment guard
const isPaymentUnresolved = computed(() => {
  return bookingReference.value ? operationGuard.isPaymentUnresolved(bookingReference.value) : false
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
const pollGeneration = ref(0)
const paymentStatusInfo = ref(null)

function stopPolling() {
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
  isVerifyingPayment.value = false
}

async function pollAuthoritativeConfirmation(attempt = 1) {
  const currentGen = ++pollGeneration.value

  if (attempt > 10) {
    isVerifyingPayment.value = false
    toast.info('Hệ thống đang xử lý xác nhận thanh toán từ VNPay. Vui lòng bấm "Làm mới" hoặc kiểm tra lại sau ít phút.')
    return
  }

  verifyPollCount.value = attempt
  isVerifyingPayment.value = true

  try {
    // 1. Authoritative booking status from server
    const resBooking = await catalogBookingApi.getBookingDetail(bookingReference.value)
    if (currentGen !== pollGeneration.value) return

    const bookingData = resBooking.data?.data || resBooking.data
    let isBookingConfirmed = false
    if (bookingData) {
      if (bookingData.bookingReference && bookingData.bookingReference !== bookingReference.value) {
        throw new Error('Mã tham chiếu đơn đặt chỗ từ máy chủ không khớp')
      }
      const validation = validateBookingResponse(bookingData, {
        expectedUserId: authStore.user?.id,
        allowTerminalReplay: true,
        allowPastExpiry: true
      })
      if (validation.valid) {
        booking.value = bookingData
        if (bookingData.status === 'CONFIRMED') {
          isBookingConfirmed = true
        }
      }
    }

    // 2. Authoritative payment status query (D0-PRE-02: verify independently, preserve reconciliation even when CONFIRMED)
    const resPayment = await catalogBookingApi.getPaymentStatus(bookingReference.value)
    if (currentGen !== pollGeneration.value) return

    const payData = resPayment.data?.data || resPayment.data
    if (payData) {
      paymentStatusInfo.value = payData
      if (payData.reconciliationRequired) {
        isReconciliationRequired.value = true
      }

      if (isBookingConfirmed) {
        stopPolling()
        operationGuard.clearUnresolvedPayment(bookingReference.value)
        toast.success('Xác nhận thanh toán thành công! Vé của bạn đã được đảm bảo.')
        return
      }

      // BLOCKER-01: Supported backend status combinations without synthetic booking mutation
      if (payData.hasSuccessfulPayment || payData.paymentStatus === 'SUCCESS') {
        // Re-fetch authoritative booking status rather than synthetically assigning
        const refreshBookingRes = await catalogBookingApi.getBookingDetail(bookingReference.value)
        if (currentGen !== pollGeneration.value) return
        const freshBooking = refreshBookingRes.data?.data || refreshBookingRes.data

        if (freshBooking && freshBooking.status === 'CONFIRMED') {
          booking.value = freshBooking
          stopPolling()
          operationGuard.clearUnresolvedPayment(bookingReference.value)
          toast.success('Xác nhận thanh toán thành công từ cổng VNPay!')
          return
        }

        if (freshBooking?.status === 'EXPIRED') {
          // PAID_AFTER_BOOKING_EXPIRED
          stopPolling()
          booking.value = freshBooking
          isReconciliationRequired.value = true
          paymentGatewayError.value = 'Đơn giữ chỗ đã hết hạn trước khi cổng thanh toán ghi nhận. Khoản thanh toán của bạn đã được bảo lưu để nhân viên đối soát hoàn tiền.'
          return
        }

        if (freshBooking?.status === 'CANCELLED') {
          // SUCCESS_CALLBACK_AFTER_FAILED_STATUS
          stopPolling()
          booking.value = freshBooking
          isReconciliationRequired.value = true
          paymentGatewayError.value = 'Đơn giữ chỗ đã bị hủy trước khi nhận được phản hồi thanh toán. Khoản tiền sẽ được đối soát hoàn trả.'
          return
        }
      } else if (payData.paymentStatus === 'FAILED' && !payData.hasPaymentAttempt) {
        stopPolling()
        operationGuard.clearUnresolvedPayment(bookingReference.value)
        paymentGatewayError.value = 'Giao dịch thanh toán đã bị cổng VNPay từ chối hoặc hết hạn.'
        toast.error('Thanh toán không thành công')
        return
      }
    } else if (isBookingConfirmed) {
      stopPolling()
      operationGuard.clearUnresolvedPayment(bookingReference.value)
      toast.success('Xác nhận thanh toán thành công! Vé của bạn đã được đảm bảo.')
      return
    }
  } catch (err) {
    console.warn('[Checkout] Confirmation poll warning:', err)
  }

  if (currentGen === pollGeneration.value) {
    pollTimer = setTimeout(() => {
      pollAuthoritativeConfirmation(attempt + 1)
    }, 2000)
  }
}

// Load Booking Details
async function fetchBookingDetail() {
  if (!bookingReference.value) {
    const vnpError = route.query.vnp_Error
    if (vnpError) {
      paymentGatewayError.value = 'Phát hiện phản hồi không hợp lệ từ cổng thanh toán. Vui lòng kiểm tra lại đơn vé tại mục Vé Của Tôi.'
    } else {
      loadError.value = 'Mã tham chiếu đơn đặt chỗ không hợp lệ'
    }
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
    const vnpError = route.query.vnp_Error

    if (vnpError === 'INVALID_SIGNATURE') {
      paymentGatewayError.value = 'Phát hiện phản hồi không hợp lệ từ cổng thanh toán (Chữ ký điện tử không khớp). Vui lòng đối soát lại trạng thái.'
      toast.error('Lỗi chữ ký cổng thanh toán')
    } else if (vnpCode !== undefined && vnpCode !== null) {
      if (vnpCode === '00') {
        // Returned with 00: poll authoritative backend state
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

// PRE-04, BLOCKER-05 & D0-PRE-03: Strict Redirect Security Validation
function validateRedirectUrl(redirectUrl) {
  if (!redirectUrl || typeof redirectUrl !== 'string') {
    throw new Error('Máy chủ không cung cấp đường dẫn chuyển tiếp VNPay hợp lệ.')
  }

  let parsed
  try {
    parsed = new URL(redirectUrl)
  } catch {
    throw new Error('Đường dẫn chuyển tiếp cổng thanh toán không đúng định dạng URL.')
  }

  // BLOCKER-05: Require HTTPS strictly (no arbitrary origin / no localhost bypass)
  if (parsed.protocol !== 'https:') {
    throw new Error('Giao thức chuyển tiếp thanh toán không an toàn (Yêu cầu HTTPS).')
  }

  // Reject embedded credentials
  if (parsed.username || parsed.password) {
    throw new Error('URL chuyển tiếp chứa thông tin đăng nhập không được phép.')
  }

  // Reject arbitrary non-standard ports
  if (parsed.port && parsed.port !== '443') {
    throw new Error(`Cổng mạng ${parsed.port} không được phép cho giao dịch thanh toán trực tuyến.`)
  }

  // Trusted host verification (exact approved sandbox or live vnpayment.vn origins)
  const trustedOrigins = ['https://sandbox.vnpayment.vn', 'https://vnpayment.vn', 'https://pay.vnpay.vn']
  if (!trustedOrigins.includes(parsed.origin)) {
    throw new Error(`Nguồn cổng thanh toán không thuộc danh sách tin cậy được phê duyệt: ${parsed.origin}`)
  }

  return parsed.toString()
}

// Payment Action
async function handleProceedPayment() {
  // BLOCKER-03: Synchronous submit guard
  if (isSubmittingPayment.value) return

  // D0-PRE-01: Prevent new payment mutations while previous payment outcome is unresolved
  if (isPaymentUnresolved.value) {
    toast.error('Giao dịch thanh toán trước đó chưa hoàn tất xác nhận. Vui lòng bấm kiểm tra trạng thái trước khi thực hiện giao dịch mới.')
    return
  }

  if (isBlockedByCancel.value) {
    toast.error('Đơn đặt chỗ này đang trong tiến trình hủy. Không thể thực hiện thanh toán.')
    return
  }

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

  isSubmittingPayment.value = true
  paymentGatewayError.value = ''

  // BLOCKER-03: Persist minimal recovery context before sending checkout POST
  operationGuard.setUnresolvedPayment(booking.value.bookingReference, {
    initiatedAt: Date.now(),
    status: 'INITIATED'
  })

  try {
    const res = await catalogBookingApi.createVnPayCheckout({
      bookingReference: booking.value.bookingReference,
      paymentMethod: 'VNPAY'
    })

    const data = res.data?.data || res.data
    if (!data || typeof data !== 'object' || !data.paymentReference || typeof data.paymentReference !== 'string') {
      throw new Error('Phản hồi khởi tạo giao dịch từ máy chủ không hợp lệ.')
    }

    const redirectUrl = validateRedirectUrl(data?.redirectPaymentUrl)

    // D0-PRE-03: Save minimal recovery state without serializing sensitive gateway URLs
    operationGuard.setUnresolvedPayment(booking.value.bookingReference, {
      paymentReference: data.paymentReference,
      startedAt: Date.now(),
      status: 'REDIRECTED'
    })

    toast.info('Đang chuyển hướng sang cổng thanh toán VNPay...')
    window.location.href = redirectUrl
  } catch (err) {
    console.error('[Checkout] Payment initialization failed:', err)
    const code = err.response?.data?.code
    const message = err.response?.data?.message || err.message

    // BLOCKER-03 & BLOCKER-05: Preserve unknown outcome context on failure
    operationGuard.setUnresolvedPayment(booking.value.bookingReference, {
      failedAt: Date.now(),
      error: message || 'Khởi tạo thanh toán bị gián đoạn'
    })

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

onMounted(async () => {
  await fetchBookingDetail()

  // D0-PRE-02: Check relevant server payment state on initial Checkout load regardless of browser storage
  if (bookingReference.value) {
    try {
      const resPayment = await catalogBookingApi.getPaymentStatus(bookingReference.value)
      const payData = resPayment.data?.data || resPayment.data
      if (payData) {
        paymentStatusInfo.value = payData
        if (payData.reconciliationRequired) {
          isReconciliationRequired.value = true
        }
        if (payData.hasSuccessfulPayment || payData.paymentStatus === 'SUCCESS') {
          await pollAuthoritativeConfirmation(1)
        } else if (payData.hasPaymentAttempt && payData.paymentStatus === 'PENDING') {
          // Establish lock if server confirms active pending attempt exists
          operationGuard.setUnresolvedPayment(bookingReference.value, {
            paymentReference: payData.paymentReference || payData.activePaymentReference,
            status: 'PENDING',
            startedAt: Date.now()
          })
        }
      }
    } catch (err) {
      console.warn('[Checkout] Failed to inspect initial payment status:', err)
    }
  }
})

// D0-PRE-01: Explicit read-only verification action
async function handleVerifyPaymentOutcome() {
  await pollAuthoritativeConfirmation(1)
}

onUnmounted(() => {
  // BLOCKER-03: Invalidate polling generation to cancel stale handlers
  pollGeneration.value++
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

    <!-- Cancellation Guard Warning (PRE-03) -->
    <Alert v-if="isBlockedByCancel" variant="destructive">
      <ShieldAlert class="w-4 h-4" />
      <AlertTitle>Thao Tác Bị Khóa Do Yêu Cầu Hủy Vé</AlertTitle>
      <AlertDescription>
        Đơn đặt chỗ này đang trong tiến trình hủy hoặc chưa hoàn tất đối soát kết quả hủy chỗ.
        Để đảm bảo an toàn giao dịch, tính năng thanh toán tạm thời bị khóa.
      </AlertDescription>
    </Alert>

    <!-- Unresolved Payment Recovery Banner (BLOCKER-03 & D0-PRE-01) -->
    <Alert v-if="isPaymentUnresolved && !paymentGatewayError && booking?.status === 'HELD' && !isVerifyingPayment" class="border-blue-500/30 bg-blue-500/10 text-blue-700 dark:text-blue-300">
      <RefreshCw class="w-4 h-4 text-blue-600 animate-spin" />
      <div class="flex-1 space-y-2">
        <AlertTitle>Đang Có Giao Dịch Thanh Toán Chưa Rõ Kết Quả</AlertTitle>
        <AlertDescription>
          Hệ thống phát hiện giao dịch thanh toán trước đó chưa hoàn tất xác nhận. Đang đối soát hoặc bạn có thể bấm kiểm tra lại để cập nhật trạng thái từ máy chủ.
        </AlertDescription>
        <div class="pt-1">
          <Button
            variant="outline"
            size="sm"
            class="h-7 text-xs bg-background/80"
            :disabled="isVerifyingPayment"
            @click="handleVerifyPaymentOutcome"
          >
            <RefreshCw class="w-3.5 h-3.5 mr-1" :class="{ 'animate-spin': isVerifyingPayment }" />
            Kiểm Tra Lại Trạng Thái
          </Button>
        </div>
      </div>
    </Alert>

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
        <Button variant="outline" size="sm" @click="fetchBookingDetail">
          <RefreshCw class="w-3.5 h-3.5 mr-1" />
          Thử Lại
        </Button>
        <router-link to="/search">
          <Button size="sm">Tìm Chuyến Khác</Button>
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

      <!-- Reconciliation Alert -->
      <Alert v-if="isReconciliationRequired" class="border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-300">
        <AlertTriangle class="w-4 h-4 text-amber-600" />
        <AlertTitle>Đang Đối Soát Giao Dịch</AlertTitle>
        <AlertDescription>
          {{ paymentGatewayError || 'Giao dịch đã được ghi nhận tại cổng thanh toán. Hệ thống đang tiến hành đối soát kết quả với ngân hàng để cập nhật trạng thái đơn vé.' }}
        </AlertDescription>
      </Alert>

      <!-- 2. CONFIRMED State Screen -->
      <Card v-if="booking.status === 'CONFIRMED'" class="p-8 text-center space-y-4">
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
            <Button size="sm">
              <FileText class="w-3.5 h-3.5 mr-1" />
              Xem Vé Của Tôi
            </Button>
          </router-link>
          <router-link to="/">
            <Button variant="outline" size="sm">
              Về Trang Chủ
            </Button>
          </router-link>
        </div>
      </Card>

      <!-- 3. CANCELLED or EXPIRED State Screen -->
      <Card
        v-else-if="booking.status === 'CANCELLED' || booking.status === 'EXPIRED'"
        class="p-8 text-center space-y-4"
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
            <Button size="sm">
              <Bus class="w-3.5 h-3.5 mr-1" />
              Chọn Lại Ghế Chuyến Xe Này
            </Button>
          </router-link>
          <router-link to="/search">
            <Button variant="outline" size="sm">Tìm Chuyến Khác</Button>
          </router-link>
        </div>
      </Card>

      <!-- 4. HELD: Active Two-Column Checkout Form -->
      <div v-else-if="booking.status === 'HELD'" class="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
        <!-- Left: Account Info & Payment Gateway Card -->
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
          <Alert v-if="paymentGatewayError && !isReconciliationRequired" variant="destructive">
            <AlertCircle class="w-4 h-4" />
            <AlertTitle>Thông Báo Thanh Toán</AlertTitle>
            <AlertDescription>{{ paymentGatewayError }}</AlertDescription>
          </Alert>

          <!-- PRE-02: Read-Only Customer Contact Information Card -->
          <Card>
            <CardHeader class="pb-3 border-b border-border">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <User class="w-4 h-4 text-primary" />
                  <CardTitle class="text-sm font-bold">Thông Tin Người Nhận Vé</CardTitle>
                </div>
                <router-link to="/profile" class="text-xs text-primary hover:underline flex items-center gap-1">
                  <span>Hồ Sơ Cá Nhân</span>
                  <ExternalLink class="w-3 h-3" />
                </router-link>
              </div>
            </CardHeader>
            <CardContent class="pt-4 space-y-3 text-xs">
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div class="space-y-1">
                  <span class="text-muted-foreground text-[11px] block">Họ và tên hành khách:</span>
                  <strong class="text-foreground text-sm block">{{ authStore.user?.fullName || 'Hành khách VeloBus' }}</strong>
                </div>
                <div class="space-y-1">
                  <span class="text-muted-foreground text-[11px] block">Số điện thoại nhận tin nhắn:</span>
                  <strong class="text-foreground text-sm font-mono block">{{ authStore.user?.phone || '0981111222' }}</strong>
                </div>
              </div>

              <div class="space-y-1 pt-2 border-t border-border/50">
                <span class="text-muted-foreground text-[11px] block">Email nhận thông báo vé:</span>
                <strong class="text-foreground block font-mono">{{ authStore.user?.email || 'customer@velobus.vn' }}</strong>
              </div>

              <p class="text-[11px] text-muted-foreground italic pt-2">
                * Thông tin liên hệ nhận vé được lấy trực tiếp từ tài khoản đăng nhập của bạn.
              </p>
            </CardContent>
          </Card>

          <!-- Payment Method Card (Strictly VNPay Gateway) -->
          <Card>
            <CardHeader class="pb-3 border-b border-border">
              <div class="flex items-center gap-2">
                <CreditCard class="w-4 h-4 text-primary" />
                <CardTitle class="text-sm font-bold">Phương Thức Thanh Toán Trực Tuyến</CardTitle>
              </div>
            </CardHeader>
            <CardContent class="pt-4 space-y-4">
              <div class="border-2 border-primary bg-primary/5 rounded-xl p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-lg bg-background border border-border flex items-center justify-center text-primary font-bold text-xs shrink-0">
                    VNPAY
                  </div>
                  <div>
                    <span class="text-xs font-bold text-foreground block">Cổng Thanh Toán Trực Tuyến VNPay</span>
                    <span class="text-[11px] text-muted-foreground">
                      Hỗ trợ quét mã QR Ngân hàng (VNPAY-QR), Thẻ ATM Nội địa & Thẻ Quốc tế Visa/Mastercard.
                    </span>
                  </div>
                </div>
                <Badge variant="outline" class="self-start sm:self-auto shrink-0 text-[10px]">Môi Trường Thử Nghiệm Sandbox</Badge>
              </div>

              <div class="p-3 bg-muted/40 rounded-lg text-[11px] text-muted-foreground flex items-center gap-2">
                <ShieldCheck class="w-4 h-4 text-emerald-600 shrink-0" />
                <span>Giao dịch được bảo mật SSL qua cổng thanh toán đối tác. Máy chủ VeloBus không lưu trữ thông tin thẻ ngân hàng.</span>
              </div>
            </CardContent>
          </Card>
        </div>

        <!-- Right: Order Summary Sticky Card -->
        <div class="space-y-4 lg:sticky lg:top-24">
          <Card class="p-5 space-y-4">
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

            <!-- Submit VNPay Button (Shadcn Button) -->
            <Button
              class="w-full h-11 text-xs font-bold"
              :disabled="isSubmittingPayment || isVerifyingPayment || isExpiringSoon || isExpired || isBlockedByCancel || isPaymentUnresolved"
              @click="handleProceedPayment"
            >
              <Loader2 v-if="isSubmittingPayment" class="w-4 h-4 animate-spin mr-1.5" />
              <ExternalLink v-else class="w-4 h-4 mr-1.5" />
              <span>
                {{
                  isBlockedByCancel
                    ? 'Đang khóa thanh toán do hủy vé'
                    : isExpired
                      ? 'Phiên giữ chỗ đã hết hạn'
                      : isExpiringSoon
                        ? 'Thời gian < 60s - Vui lòng chọn lại'
                        : isPaymentUnresolved
                          ? 'Giao dịch trước chưa hoàn tất'
                          : isSubmittingPayment
                            ? 'Đang chuyển sang VNPay...'
                            : 'Thanh Toán Qua VNPay'
                }}
              </span>
            </Button>

            <p class="text-[10px] text-center text-muted-foreground">
              Bằng việc bấm thanh toán, bạn đồng ý với Điều khoản đặt vé & Chính sách bảo mật của VeloBus.
            </p>
          </Card>
        </div>
      </div>
    </div>
  </div>
</template>
