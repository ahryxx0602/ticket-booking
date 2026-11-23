<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const departureStation = ref('HN-GP')
const arrivalStation = ref('DN-TT')
const departureDate = ref(new Date().toISOString().split('T')[0])

// Featured sample trips
const featuredTrips = ref([
  {
    id: 'TRIP-01',
    route: 'Hà Nội → Đà Nẵng',
    operator: 'Hoàng Long Limousine',
    plate: '29B-888.99',
    departureTime: '19:00',
    arrivalTime: '09:00 (+1 ngày)',
    duration: '14 giờ',
    coachType: 'VIP Luxury 36 Giường',
    availableSeats: 26,
    price: 350000
  },
  {
    id: 'TRIP-02',
    route: 'Sài Gòn → Đà Lạt',
    operator: 'Phương Trang Velo',
    plate: '51B-999.22',
    departureTime: '23:00',
    arrivalTime: '06:00 (+1 ngày)',
    duration: '7 giờ',
    coachType: 'Limousine Phòng Đôi 24',
    availableSeats: 8,
    price: 380000
  },
  {
    id: 'TRIP-03',
    route: 'Hà Nội → Hải Phòng',
    operator: 'Hải Âu Express',
    plate: '15B-123.45',
    departureTime: '08:30',
    arrivalTime: '10:30',
    duration: '2 giờ',
    coachType: 'Limousine Ghế Massage 16',
    availableSeats: 12,
    price: 180000
  }
])

function navigateToSeats(tripId) {
  router.push(`/trips/${tripId}/seats`)
}

function handleSearch() {
  router.push({
    path: '/search',
    query: {
      from: departureStation.value,
      to: arrivalStation.value,
      date: departureDate.value
    }
  })
}
</script>

<template>
  <div class="space-y-8 pb-12">
    <!-- Hero Banner with Navy Gradient -->
    <div class="bg-gradient-to-r from-brand-navy via-[#1e3a5f] to-brand-navy text-white py-12 px-4 shadow-md">
      <div class="max-w-7xl mx-auto space-y-6">
        <div class="max-w-2xl space-y-2">
          <span class="inline-block px-3 py-1 rounded-full text-xs font-semibold bg-white/10 text-blue-200 border border-white/20">
            Sàn Vé Xe Khách Cao Cấp • Mobility SaaS
          </span>
          <h1 class="text-3xl sm:text-4xl font-extrabold tracking-tight">
            Đặt Vé Xe Khách Liên Tỉnh Thông Minh & Tiện Lợi
          </h1>
          <p class="text-slate-300 text-sm">
            Khóa giữ chỗ thời gian thực với Redis Distributed Lock • Thanh toán bảo mật VNPay • Vé điện tử QR HMAC
          </p>
        </div>

        <!-- Search Form Card -->
        <div class="bg-surface rounded-xl p-4 sm:p-5 shadow-dialog border border-border-default text-text-primary grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 items-end">
          <div>
            <label class="block text-xs font-semibold text-text-secondary mb-1.5">Trạm khởi hành</label>
            <select
              v-model="departureStation"
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 focus:outline-none bg-white"
            >
              <option value="HN-GP">Hà Nội - Bến xe Giáp Bát</option>
              <option value="HN-MY">Hà Nội - Bến xe Mỹ Đình</option>
              <option value="SG-MD">Sài Gòn - Bến xe Miền Đông</option>
            </select>
          </div>

          <div>
            <label class="block text-xs font-semibold text-text-secondary mb-1.5">Trạm đến</label>
            <select
              v-model="arrivalStation"
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 focus:outline-none bg-white"
            >
              <option value="DN-TT">Đà Nẵng - Bến xe Trung Tâm</option>
              <option value="DL-LD">Đà Lạt - Bến xe Liên Tỉnh</option>
              <option value="HP-CL">Hải Phòng - Bến xe Cầu Rào</option>
            </select>
          </div>

          <div>
            <label class="block text-xs font-semibold text-text-secondary mb-1.5">Ngày khởi hành</label>
            <input
              type="date"
              v-model="departureDate"
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 focus:outline-none bg-white"
            />
          </div>

          <div>
            <button
              type="button"
              @click="handleSearch"
              class="w-full h-11 bg-primary-600 hover:bg-primary-700 text-white font-semibold rounded-lg text-xs shadow-sm flex items-center justify-center gap-2 transition-all"
            >
              <span>Tìm Chuyến Xe Ngay</span>
              <span>→</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Featured Trips Section -->
    <div class="max-w-7xl mx-auto px-4 space-y-4">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-lg font-bold text-text-primary">Chuyến Xe Phổ Biến & Nổi Bật</h2>
          <p class="text-xs text-text-muted">Các tuyến xe liên tỉnh có lượt đặt vé cao nhất hôm nay</p>
        </div>
        <button
          type="button"
          @click="handleSearch"
          class="text-xs text-primary-600 font-semibold cursor-pointer hover:underline"
        >
          Xem tất cả chuyến →
        </button>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        <div
          v-for="trip in featuredTrips"
          :key="trip.id"
          class="bg-surface rounded-xl border border-border-default p-5 shadow-card hover:shadow-dropdown transition-all flex flex-col justify-between"
        >
          <div class="space-y-3">
            <div class="flex items-center justify-between">
              <span class="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-blue-50 text-primary-600 border border-blue-200">
                {{ trip.operator }}
              </span>
              <span class="text-xs text-emerald-600 font-medium tabular-nums">
                Còn {{ trip.availableSeats }} chỗ trống
              </span>
            </div>

            <div>
              <h3 class="text-base font-bold text-text-primary">{{ trip.route }}</h3>
              <p class="text-xs text-text-secondary flex items-center gap-2 mt-1">
                <span>🕒 {{ trip.departureTime }}</span>
                <span>→</span>
                <span>{{ trip.arrivalTime }}</span>
                <span class="text-text-muted">({{ trip.duration }})</span>
              </p>
            </div>

            <p class="text-xs text-text-muted">
              🚌 {{ trip.coachType }} • Biển số: <strong class="text-text-primary">{{ trip.plate }}</strong>
            </p>
          </div>

          <div class="pt-4 mt-4 border-t border-border-default flex items-center justify-between">
            <div>
              <span class="text-[11px] text-text-muted block">Giá vé từ</span>
              <span class="text-lg font-bold text-primary-600 tabular-nums">
                {{ trip.price.toLocaleString('vi-VN') }} <span class="text-xs font-semibold">đ</span>
              </span>
            </div>

            <button
              type="button"
              @click="navigateToSeats(trip.id)"
              class="px-4 py-2 bg-primary-600 hover:bg-primary-700 text-white font-semibold rounded-lg text-xs shadow-sm transition-all flex items-center gap-1.5"
            >
              <span>Chọn Chỗ</span>
              <span>→</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
