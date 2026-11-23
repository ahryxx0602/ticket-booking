<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCountdown } from '@/shared/composables/useCountdown'
import FormField from '@/shared/components/FormField.vue'
import AppButton from '@/shared/components/AppButton.vue'

const route = useRoute()
const router = useRouter()

const bookingId = ref(route.params.bookingId || 'BKG-20261123-9988')
const isSubmitting = ref(false)
const showExpiredModal = ref(false)

// Sample booking data held in Redis 10 minutes (corresponds to API-BKG-04)
const bookingDetail = ref({
  id: bookingId.value,
  route: 'Hà Nội (Bến xe Giáp Bát) → Đà Nẵng (Bến xe Trung Tâm)',
  departureTime: '19:00 - 23/11/2026',
  operator: 'Hoàng Long Limousine',
  plate: '29B-888.99',
  seats: ['A05', 'A06'],
  ticketPrice: 350000,
  heldUntil: Date.now() + 10 * 60 * 1000 // 10 minutes from now
})

const totalAmount = computed(() => {
  return bookingDetail.value.seats.length * bookingDetail.value.ticketPrice
})

// Passenger Form State
const passengerForm = ref({
  fullName: 'Phan Văn Thành',
  phone: '0912345678',
  email: 'thanh.pv@gmail.com',
  note: 'Đón tại sảnh chính bến xe Giáp Bát'
})

const formErrors = ref({
  fullName: '',
  phone: '',
  email: ''
})

// 10-Minute Hold Countdown Timer
const { formattedTime, isExpired } = useCountdown(bookingDetail.value.heldUntil, {
  onExpire: () => {
    showExpiredModal.value = true
  }
})

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
    formErrors.value.email = 'Email không hợp lệ để nhận vé điện tử'
    isValid = false
  }

  return isValid
}

// Submit payment to VNPay (calls API-PAY-01)
async function onProceedPayment() {
  if (!validateForm()) return
  if (isExpired.value) {
    showExpiredModal.value = true
    return
  }

  isSubmitting.value = true

  // Simulate VNPay payment gateway redirect
  setTimeout(() => {
    isSubmitting.value = false
    // Navigate to My Bookings with confirmed mock state
    router.push('/my-bookings?payment_success=true')
  }, 1200)
}
</script>

<template>
  <div class="max-w-6xl mx-auto px-4 py-8 space-y-6">
    <!-- Countdown Expiry Warning Banner -->
    <div
      class="bg-amber-50 border border-amber-200 rounded-xl p-4 flex flex-col sm:flex-row items-center justify-between gap-3 shadow-sm"
      :class="{ 'bg-rose-50 border-rose-200': isExpired }"
    >
      <div class="flex items-center gap-3">
        <span class="text-2xl">⏳</span>
        <div>
          <h2 class="text-xs font-bold uppercase tracking-wider text-amber-800" :class="{ 'text-rose-800': isExpired }">
            {{ isExpired ? 'Phiên giữ chỗ đã hết hạn!' : 'Phiên giữ chỗ có thời hạn 10 phút' }}
          </h2>
          <p class="text-xs text-amber-700" :class="{ 'text-rose-700': isExpired }">
            {{ isExpired ? 'Ghế của bạn đã được giải phóng trên hệ thống Redis.' : 'Vui lòng hoàn tất thanh toán trước khi thời gian đếm ngược kết thúc.' }}
          </p>
        </div>
      </div>

      <div class="flex items-center gap-2 bg-white px-3.5 py-1.5 rounded-lg border border-amber-300 shadow-xs">
        <span class="text-xs font-medium text-text-secondary">Thời gian còn lại:</span>
        <span class="text-base font-extrabold text-amber-600 tabular-nums" :class="{ 'text-rose-600': isExpired }">
          {{ formattedTime }}
        </span>
      </div>
    </div>

    <!-- Main Two-Column Layout -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
      <!-- Left Column: Passenger Info & Payment Method -->
      <div class="lg:col-span-2 space-y-6">
        <!-- Passenger Info Card -->
        <div class="bg-surface rounded-xl border border-border-default p-6 shadow-card space-y-4">
          <h2 class="text-base font-bold text-text-primary flex items-center gap-2 border-b border-border-default pb-3">
            <span>👤</span>
            <span>Thông Tin Người Đi & Nhận Vé Điện Tử</span>
          </h2>

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
                class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 bg-white"
                :class="{ 'border-rose-500': formErrors.fullName }"
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
                class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 bg-white"
                :class="{ 'border-rose-500': formErrors.phone }"
              />
            </FormField>
          </div>

          <FormField
            label="Email nhận vé điện tử (Kèm mã QR)"
            :error="formErrors.email"
            required
          >
            <input
              v-model="passengerForm.email"
              type="email"
              placeholder="example@gmail.com"
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 bg-white"
              :class="{ 'border-rose-500': formErrors.email }"
            />
          </FormField>

          <FormField label="Ghi chú điểm đón (Không bắt buộc)">
            <textarea
              v-model="passengerForm.note"
              rows="2"
              placeholder="Ví dụ: Đón tại cổng số 2 bến xe Giáp Bát..."
              class="w-full p-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 bg-white resize-none"
            ></textarea>
          </FormField>
        </div>

        <!-- Payment Method Card -->
        <div class="bg-surface rounded-xl border border-border-default p-6 shadow-card space-y-4">
          <h2 class="text-base font-bold text-text-primary flex items-center gap-2 border-b border-border-default pb-3">
            <span>💳</span>
            <span>Phương Thức Thanh Toán</span>
          </h2>

          <div class="border-2 border-primary-600 bg-blue-50/40 rounded-xl p-4 flex items-center justify-between">
            <div class="flex items-center gap-3">
              <span class="text-2xl">🏦</span>
              <div>
                <span class="text-xs font-bold text-text-primary block">Cổng Thanh Toán Trực Tuyến VNPay Sandbox</span>
                <span class="text-[11px] text-text-secondary">Hỗ trợ QR Code ngân hàng, Thẻ ATM Nội địa & Quốc tế Visa/Mastercard</span>
              </div>
            </div>
            <span class="text-xs font-semibold px-2 py-0.5 rounded bg-primary-600 text-white">Khuyên dùng</span>
          </div>
        </div>
      </div>

      <!-- Right Column: Booking Summary Card -->
      <div class="space-y-4 lg:sticky lg:top-24">
        <div class="bg-surface rounded-xl border border-border-default p-5 shadow-card space-y-4">
          <h2 class="text-sm font-bold text-text-primary uppercase tracking-wider pb-3 border-b border-border-default">
            Tóm Tắt Đơn Vé
          </h2>

          <div class="space-y-3 text-xs">
            <div>
              <span class="text-text-muted block text-[11px]">Tuyến đường:</span>
              <strong class="text-text-primary">{{ bookingDetail.route }}</strong>
            </div>

            <div class="flex justify-between">
              <span class="text-text-muted">Nhà xe:</span>
              <strong class="text-text-primary">{{ bookingDetail.operator }}</strong>
            </div>

            <div class="flex justify-between">
              <span class="text-text-muted">Biển số:</span>
              <strong class="text-text-primary">{{ bookingDetail.plate }}</strong>
            </div>

            <div class="flex justify-between">
              <span class="text-text-muted">Thời gian:</span>
              <strong class="text-text-primary">{{ bookingDetail.departureTime }}</strong>
            </div>

            <div class="flex justify-between items-center pt-2 border-t border-border-default/60">
              <span class="text-text-muted">Vị trí ghế đã chọn:</span>
              <div class="flex gap-1.5">
                <span
                  v-for="seat in bookingDetail.seats"
                  :key="seat"
                  class="px-2 py-0.5 rounded bg-blue-100 text-primary-700 font-bold tabular-nums"
                >
                  {{ seat }}
                </span>
              </div>
            </div>

            <div class="flex justify-between pt-2 border-t border-border-default/60">
              <span class="text-text-muted">Đơn giá vé:</span>
              <span class="tabular-nums font-medium">{{ bookingDetail.ticketPrice.toLocaleString('vi-VN') }} đ/ghế</span>
            </div>

            <div class="flex justify-between">
              <span class="text-text-muted">Phí tiện ích sàn:</span>
              <span class="text-emerald-600 font-semibold">Miễn phí (0 đ)</span>
            </div>
          </div>

          <!-- Total Calculation -->
          <div class="pt-4 border-t border-border-default flex items-center justify-between">
            <span class="text-xs font-bold text-text-primary">Tổng Tiền Cần Trả:</span>
            <span class="text-xl font-extrabold text-primary-600 tabular-nums">
              {{ totalAmount.toLocaleString('vi-VN') }} <span class="text-xs font-bold">đ</span>
            </span>
          </div>

          <!-- Submit CTA Button -->
          <AppButton
            variant="primary"
            class="w-full h-11 text-xs font-bold shadow-md"
            :loading="isSubmitting"
            :disabled="isExpired"
            @click="onProceedPayment"
          >
            <span>Thanh Toán Qua VNPay Ngay</span>
            <span>→</span>
          </AppButton>

          <p class="text-[11px] text-text-muted text-center italic">
            🔒 Bảo mật theo chuẩn PCI-DSS & Ký số HMAC-SHA512
          </p>
        </div>
      </div>
    </div>

    <!-- Expired Modal Popup -->
    <div
      v-if="showExpiredModal"
      class="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4"
    >
      <div class="bg-surface rounded-xl max-w-md w-full p-6 space-y-4 shadow-dialog border border-border-default text-center">
        <span class="text-4xl block">⏰</span>
        <h3 class="text-lg font-bold text-text-primary">Phiên Giữ Chỗ Đã Hết Hạn</h3>
        <p class="text-xs text-text-secondary leading-relaxed">
          Đã quá thời gian 10 phút khóa chỗ tạm thời trên Redis. Vị trí ghế của bạn đã được giải phóng để những hành khách khác có thể đặt.
        </p>
        <button
          type="button"
          @click="router.push(`/trips/TRIP-01/seats`)"
          class="w-full h-10 bg-primary-600 hover:bg-primary-700 text-white font-semibold rounded-lg text-xs transition-all"
        >
          Chọn Lại Ghế Mới
        </button>
      </div>
    </div>
  </div>
</template>
