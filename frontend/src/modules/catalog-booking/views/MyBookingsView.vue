<script setup>
import { ref, computed } from 'vue'
import QRCode from 'qrcode'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import AppButton from '@/shared/components/AppButton.vue'

// Status filter tabs
const currentTab = ref('ALL')

// Mock bookings list (corresponds to API-BKG-03)
const bookings = ref([
  {
    id: 'BKG-20261123-9988',
    tripId: 'TRIP-01',
    route: 'Hà Nội (Giáp Bát) → Đà Nẵng (Trung Tâm)',
    departureTime: '19:00 - 23/11/2026',
    operator: 'Hoàng Long Limousine',
    plate: '29B-888.99',
    seats: ['A05', 'A06'],
    totalPrice: 700000,
    status: 'CONFIRMED',
    passenger: {
      name: 'Phan Văn Thành',
      phone: '0912345678',
      email: 'thanh.pv@gmail.com'
    },
    // Payload with HMAC-SHA256 signature for ticket inspection
    qrPayload: 'VELOBUS|BKG-20261123-9988|TRIP-01|A05,A06|HMAC:a8f9c2d1e4b78345'
  },
  {
    id: 'BKG-20261120-1122',
    tripId: 'TRIP-02',
    route: 'Sài Gòn (Miền Đông) → Đà Lạt (Liên Tỉnh)',
    departureTime: '23:00 - 20/11/2026',
    operator: 'Phương Trang Velo',
    plate: '51B-999.22',
    seats: ['B02'],
    totalPrice: 380000,
    status: 'COMPLETED',
    passenger: {
      name: 'Phan Văn Thành',
      phone: '0912345678',
      email: 'thanh.pv@gmail.com'
    },
    qrPayload: 'VELOBUS|BKG-20261120-1122|TRIP-02|B02|HMAC:f10928a38c8234be'
  },
  {
    id: 'BKG-20261115-4455',
    tripId: 'TRIP-03',
    route: 'Hà Nội (Giáp Bát) → Hải Phòng (Cầu Rào)',
    departureTime: '08:30 - 15/11/2026',
    operator: 'Hải Âu Express',
    plate: '15B-123.45',
    seats: ['A01'],
    totalPrice: 180000,
    status: 'CANCELLED',
    passenger: {
      name: 'Phan Văn Thành',
      phone: '0912345678',
      email: 'thanh.pv@gmail.com'
    },
    qrPayload: 'VELOBUS|BKG-20261115-4455|TRIP-03|A01|HMAC:cancelled'
  }
])

const filteredBookings = computed(() => {
  if (currentTab.value === 'ALL') return bookings.value
  return bookings.value.filter(b => b.status === currentTab.value)
})

// QR Code Modal State
const selectedBookingForQr = ref(null)
const qrDataUrl = ref('')
const isGeneratingQr = ref(false)

async function openQrModal(booking) {
  selectedBookingForQr.value = booking
  isGeneratingQr.value = true
  try {
    qrDataUrl.value = await QRCode.toDataURL(booking.qrPayload, {
      width: 240,
      margin: 2,
      color: {
        dark: '#142B4A', // Brand Navy
        light: '#FFFFFF'
      }
    })
  } catch (err) {
    console.error('Lỗi sinh mã QR vé xe:', err)
  } finally {
    isGeneratingQr.value = false
  }
}

function closeQrModal() {
  selectedBookingForQr.value = null
  qrDataUrl.value = ''
}

// Cancel Booking Confirmation Modal State
const selectedBookingForCancel = ref(null)
const isCancelling = ref(false)

function openCancelModal(booking) {
  selectedBookingForCancel.value = booking
}

function closeCancelModal() {
  selectedBookingForCancel.value = null
}

function confirmCancelBooking() {
  if (!selectedBookingForCancel.value) return
  isCancelling.value = true

  // Simulate API-BKG-02 cancel call
  setTimeout(() => {
    selectedBookingForCancel.value.status = 'CANCELLED'
    isCancelling.value = false
    closeCancelModal()
  }, 800)
}
</script>

<template>
  <div class="max-w-5xl mx-auto px-4 py-8 space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border-default pb-4">
      <div>
        <h1 class="text-2xl font-bold text-text-primary">Lịch Sử Vé Đã Đặt & Vé Điện Tử</h1>
        <p class="text-xs text-text-muted mt-1">
          Tra cứu thông tin vé, xuất mã QR HMAC-SHA256 lên xe hoặc yêu cầu hoàn/hủy vé trực tuyến.
        </p>
      </div>

      <router-link
        to="/"
        class="self-start sm:self-auto px-4 py-2 bg-primary-600 hover:bg-primary-700 text-white font-semibold rounded-lg text-xs shadow-sm transition-all"
      >
        + Đặt Thêm Vé Mới
      </router-link>
    </div>

    <!-- Status Tabs -->
    <div class="flex gap-2 border-b border-border-default pb-2 overflow-x-auto text-xs font-semibold">
      <button
        type="button"
        @click="currentTab = 'ALL'"
        class="px-3.5 py-2 rounded-lg transition-all"
        :class="currentTab === 'ALL' ? 'bg-primary-600 text-white' : 'text-text-secondary hover:bg-slate-100'"
      >
        Tất cả ({{ bookings.length }})
      </button>

      <button
        type="button"
        @click="currentTab = 'CONFIRMED'"
        class="px-3.5 py-2 rounded-lg transition-all"
        :class="currentTab === 'CONFIRMED' ? 'bg-primary-600 text-white' : 'text-text-secondary hover:bg-slate-100'"
      >
        Đã xác nhận ({{ bookings.filter(b => b.status === 'CONFIRMED').length }})
      </button>

      <button
        type="button"
        @click="currentTab = 'COMPLETED'"
        class="px-3.5 py-2 rounded-lg transition-all"
        :class="currentTab === 'COMPLETED' ? 'bg-primary-600 text-white' : 'text-text-secondary hover:bg-slate-100'"
      >
        Đã hoàn thành
      </button>

      <button
        type="button"
        @click="currentTab = 'CANCELLED'"
        class="px-3.5 py-2 rounded-lg transition-all"
        :class="currentTab === 'CANCELLED' ? 'bg-primary-600 text-white' : 'text-text-secondary hover:bg-slate-100'"
      >
        Đã hủy
      </button>
    </div>

    <!-- Bookings List -->
    <div v-if="filteredBookings.length > 0" class="space-y-4">
      <div
        v-for="booking in filteredBookings"
        :key="booking.id"
        class="bg-surface rounded-xl border border-border-default p-5 shadow-card space-y-4 hover:border-slate-300 transition-all"
      >
        <!-- Top Bar: Booking ID & Status -->
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-border-default/80">
          <div class="flex items-center gap-3">
            <span class="text-xs font-bold text-text-primary tracking-wide">
              Mã đơn: <span class="text-primary-600 tabular-nums">{{ booking.id }}</span>
            </span>
            <span class="text-xs text-text-muted">• {{ booking.operator }}</span>
          </div>

          <StatusBadge :status="booking.status" />
        </div>

        <!-- Middle Bar: Trip Details & Passenger -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
          <div class="space-y-1 md:col-span-2">
            <span class="text-text-muted text-[11px] block">Hành trình chuyến xe:</span>
            <p class="text-sm font-bold text-text-primary">{{ booking.route }}</p>
            <p class="text-text-secondary flex items-center gap-2">
              <span>🕒 Khởi hành: <strong>{{ booking.departureTime }}</strong></span>
              <span>•</span>
              <span>Biển số: <strong>{{ booking.plate }}</strong></span>
            </p>
          </div>

          <div class="space-y-1 bg-slate-50/70 p-3 rounded-lg border border-border-default/60">
            <div class="flex justify-between">
              <span class="text-text-muted">Ghế đã đặt:</span>
              <span class="font-bold text-primary-600 tabular-nums">{{ booking.seats.join(', ') }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-text-muted">Tổng tiền:</span>
              <span class="font-bold text-text-primary tabular-nums">{{ booking.totalPrice.toLocaleString('vi-VN') }} đ</span>
            </div>
            <div class="flex justify-between">
              <span class="text-text-muted">Hành khách:</span>
              <span class="font-medium text-text-primary">{{ booking.passenger.name }}</span>
            </div>
          </div>
        </div>

        <!-- Bottom Action Bar -->
        <div class="pt-3 border-t border-border-default/80 flex items-center justify-end gap-3">
          <button
            v-if="booking.status === 'CONFIRMED'"
            type="button"
            @click="openCancelModal(booking)"
            class="px-3.5 py-1.5 text-xs text-rose-600 hover:bg-rose-50 font-medium rounded-lg transition-all"
          >
            Yêu Cầu Hủy Vé
          </button>

          <button
            type="button"
            @click="openQrModal(booking)"
            class="px-4 py-2 bg-brand-navy hover:bg-slate-800 text-white font-semibold rounded-lg text-xs shadow-sm flex items-center gap-1.5 transition-all"
          >
            <span>📱</span>
            <span>Mã QR Lên Xe</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="bg-surface rounded-xl border border-border-default p-12 text-center shadow-card">
      <EmptyState
        title="Không tìm thấy đơn vé nào"
        description="Bạn chưa có đơn vé nào ở trạng thái này. Hãy tìm kiếm chuyến xe để đặt vé ngay hôm nay!"
        icon="🎫"
      />
    </div>

    <!-- QR Code Modal Dialog -->
    <div
      v-if="selectedBookingForQr"
      class="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4"
    >
      <div class="bg-surface rounded-2xl max-w-sm w-full p-6 shadow-dialog border border-border-default text-center space-y-4">
        <div class="flex items-center justify-between pb-2 border-b border-border-default">
          <h3 class="text-sm font-bold text-text-primary">Vé Xe Khách Điện Tử VeloBus</h3>
          <button @click="closeQrModal" class="text-slate-400 hover:text-slate-600 text-lg">✕</button>
        </div>

        <div class="p-3 bg-slate-50 rounded-xl border border-border-default flex justify-center">
          <img
            v-if="qrDataUrl"
            :src="qrDataUrl"
            alt="Mã QR vé xe điện tử"
            class="w-56 h-56 rounded-lg shadow-xs"
          />
          <div v-else class="w-56 h-56 flex items-center justify-center text-xs text-text-muted">
            Đang sinh mã QR bảo mật...
          </div>
        </div>

        <div class="space-y-1 text-xs">
          <p class="font-bold text-text-primary">{{ selectedBookingForQr.route }}</p>
          <p class="text-text-secondary">
            Mã đơn: <strong class="text-primary-600">{{ selectedBookingForQr.id }}</strong> • Ghế: <strong class="text-emerald-600">{{ selectedBookingForQr.seats.join(', ') }}</strong>
          </p>
          <p class="text-[11px] text-text-muted italic pt-1">
            🔒 Xác thực bằng chữ ký số HMAC-SHA256 • Trình mã QR cho phụ xe khi lên xe
          </p>
        </div>

        <button
          type="button"
          @click="closeQrModal"
          class="w-full h-10 bg-brand-navy hover:bg-slate-800 text-white font-semibold rounded-lg text-xs transition-all"
        >
          Đóng Cửa Sổ
        </button>
      </div>
    </div>

    <!-- Cancel Booking Confirm Dialog -->
    <div
      v-if="selectedBookingForCancel"
      class="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4"
    >
      <div class="bg-surface rounded-xl max-w-md w-full p-6 shadow-dialog border border-border-default space-y-4">
        <h3 class="text-base font-bold text-text-primary flex items-center gap-2 text-rose-600">
          <span>⚠️</span>
          <span>Xác Nhận Hủy Đơn Vé {{ selectedBookingForCancel.id }}</span>
        </h3>

        <div class="text-xs text-text-secondary space-y-2 bg-slate-50 p-3.5 rounded-lg border border-border-default">
          <p class="font-semibold text-text-primary">Chính sách hoàn tiền hủy vé sàn VeloBus:</p>
          <ul class="list-disc list-inside space-y-1 text-slate-600">
            <li>Hủy trước 24 giờ khởi hành: Hoàn lại <strong>100%</strong> giá vé.</li>
            <li>Hủy từ 12 - 24 giờ trước khởi hành: Hoàn lại <strong>80%</strong> giá vé.</li>
            <li>Hủy dưới 12 giờ: Không được hỗ trợ hoàn tiền.</li>
          </ul>
        </div>

        <p class="text-xs text-slate-500">
          Số tiền hoàn lại sẽ được chuyển về tài khoản thanh toán ban đầu trong vòng 3 - 5 ngày làm việc.
        </p>

        <div class="flex justify-end gap-3 pt-2">
          <button
            type="button"
            @click="closeCancelModal"
            class="px-4 py-2 border border-border-strong hover:bg-slate-50 text-text-secondary text-xs font-semibold rounded-lg transition-all"
          >
            Giữ Lại Vé
          </button>

          <AppButton
            variant="danger"
            class="px-4 py-2 text-xs font-bold"
            :loading="isCancelling"
            @click="confirmCancelBooking"
          >
            Đồng Ý Hủy Vé
          </AppButton>
        </div>
      </div>
    </div>
  </div>
</template>
