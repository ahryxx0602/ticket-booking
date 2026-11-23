<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TripCard from '../components/TripCard.vue'
import EmptyState from '@/shared/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()

// Query params from search bar
const fromStation = ref(route.query.from || 'HN-GP')
const toStation = ref(route.query.to || 'DN-TT')
const travelDate = ref(route.query.date || new Date().toISOString().split('T')[0])

// Filters state
const selectedTimeSlots = ref([])
const selectedOperators = ref([])
const maxPrice = ref(500000)
const coachTypeFilter = ref('ALL')

// Mock catalog trips data (corresponds to API-CAT-01)
const allTrips = ref([
  {
    id: 'TRIP-01',
    from: 'HN-GP',
    to: 'DN-TT',
    route: 'Hà Nội → Đà Nẵng',
    operator: 'Hoàng Long Limousine',
    plate: '29B-888.99',
    departureTime: '19:00',
    arrivalTime: '09:00 (+1 ngày)',
    duration: '14 giờ',
    timeSlot: 'EVENING',
    coachType: 'VIP Luxury 36 Giường',
    coachCategory: 'SLEEPER',
    availableSeats: 26,
    price: 350000
  },
  {
    id: 'TRIP-02',
    from: 'SG-MD',
    to: 'DL-LD',
    route: 'Sài Gòn → Đà Lạt',
    operator: 'Phương Trang Velo',
    plate: '51B-999.22',
    departureTime: '23:00',
    arrivalTime: '06:00 (+1 ngày)',
    duration: '7 giờ',
    timeSlot: 'EVENING',
    coachType: 'Limousine Phòng Đôi 24',
    coachCategory: 'LIMOUSINE',
    availableSeats: 8,
    price: 380000
  },
  {
    id: 'TRIP-03',
    from: 'HN-GP',
    to: 'HP-CL',
    route: 'Hà Nội → Hải Phòng',
    operator: 'Hải Âu Express',
    plate: '15B-123.45',
    departureTime: '08:30',
    arrivalTime: '10:30',
    duration: '2 giờ',
    timeSlot: 'MORNING',
    coachType: 'Limousine Ghế Massage 16',
    coachCategory: 'SEAT',
    availableSeats: 12,
    price: 180000
  },
  {
    id: 'TRIP-04',
    from: 'HN-GP',
    to: 'DN-TT',
    route: 'Hà Nội → Đà Nẵng',
    operator: 'Văn Minh Express',
    plate: '37B-777.88',
    departureTime: '14:00',
    arrivalTime: '04:00 (+1 ngày)',
    duration: '14 giờ',
    timeSlot: 'AFTERNOON',
    coachType: 'VIP Luxury 34 Giường',
    coachCategory: 'SLEEPER',
    availableSeats: 18,
    price: 320000
  },
  {
    id: 'TRIP-05',
    from: 'HN-GP',
    to: 'DN-TT',
    route: 'Hà Nội → Đà Nẵng',
    operator: 'Hải Vân Velo',
    plate: '29B-456.78',
    departureTime: '07:00',
    arrivalTime: '21:00',
    duration: '14 giờ',
    timeSlot: 'MORNING',
    coachType: 'Limousine Giường Nằm 32',
    coachCategory: 'LIMOUSINE',
    availableSeats: 4,
    price: 420000
  }
])

// Unique operators list for filter
const availableOperators = computed(() => {
  return [...new Set(allTrips.value.map(t => t.operator))]
})

// Filtered trips logic
const filteredTrips = computed(() => {
  return allTrips.value.filter(trip => {
    // Price filter
    if (trip.price > maxPrice.value) return false

    // Time slot filter (Morning: 00:00-12:00, Afternoon: 12:00-18:00, Evening: 18:00-24:00)
    if (selectedTimeSlots.value.length > 0 && !selectedTimeSlots.value.includes(trip.timeSlot)) {
      return false
    }

    // Operator filter
    if (selectedOperators.value.length > 0 && !selectedOperators.value.includes(trip.operator)) {
      return false
    }

    // Coach type filter
    if (coachTypeFilter.value !== 'ALL' && trip.coachCategory !== coachTypeFilter.value) {
      return false
    }

    return true
  })
})

function resetFilters() {
  selectedTimeSlots.value = []
  selectedOperators.value = []
  maxPrice.value = 500000
  coachTypeFilter.value = 'ALL'
}
</script>

<template>
  <div class="space-y-6 pb-12">
    <!-- Search Query Summary Header -->
    <div class="bg-surface border-b border-border-default py-4 px-4 shadow-sm">
      <div class="max-w-7xl mx-auto flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <span class="text-xs font-semibold text-primary-600 uppercase tracking-wider">Kết Quả Tìm Kiếm Chuyến Đi</span>
          <h1 class="text-xl sm:text-2xl font-bold text-text-primary flex items-center gap-2 mt-0.5">
            <span>Hà Nội (Giáp Bát)</span>
            <span class="text-slate-400">→</span>
            <span>Đà Nẵng (Trung Tâm)</span>
          </h1>
          <p class="text-xs text-text-muted mt-0.5">
            Ngày khởi hành: <strong class="text-text-primary">{{ travelDate }}</strong> • Tìm thấy <strong class="text-primary-600 tabular-nums">{{ filteredTrips.length }}</strong> chuyến xe phù hợp
          </p>
        </div>

        <button
          type="button"
          @click="router.push('/')"
          class="self-start md:self-auto px-3.5 py-1.5 border border-border-strong hover:bg-slate-50 text-text-secondary text-xs font-medium rounded-lg transition-all"
        >
          ✏️ Đổi Tuyến Đường
        </button>
      </div>
    </div>

    <!-- Main Content: Sidebar Filter + Results Grid -->
    <div class="max-w-7xl mx-auto px-4 grid grid-cols-1 lg:grid-cols-4 gap-6 items-start">
      <!-- Filter Sidebar -->
      <aside class="bg-surface rounded-xl border border-border-default p-5 shadow-card space-y-6">
        <div class="flex items-center justify-between pb-3 border-b border-border-default">
          <h2 class="text-sm font-bold text-text-primary flex items-center gap-1.5">
            <span>⚡</span>
            <span>Bộ Lọc Chuyến Xe</span>
          </h2>
          <button
            type="button"
            @click="resetFilters"
            class="text-xs text-primary-600 hover:underline font-medium"
          >
            Đặt lại
          </button>
        </div>

        <!-- Khung Giờ Khởi Hành -->
        <div class="space-y-2.5">
          <label class="text-xs font-semibold text-text-secondary uppercase tracking-wider block">Khung Giờ Xuất Bến</label>
          <div class="space-y-2 text-xs">
            <label class="flex items-center gap-2 cursor-pointer">
              <input type="checkbox" value="MORNING" v-model="selectedTimeSlots" class="rounded text-primary-600 focus:ring-primary-500" />
              <span>Sáng sớm (06:00 - 12:00)</span>
            </label>
            <label class="flex items-center gap-2 cursor-pointer">
              <input type="checkbox" value="AFTERNOON" v-model="selectedTimeSlots" class="rounded text-primary-600 focus:ring-primary-500" />
              <span>Buổi chiều (12:00 - 18:00)</span>
            </label>
            <label class="flex items-center gap-2 cursor-pointer">
              <input type="checkbox" value="EVENING" v-model="selectedTimeSlots" class="rounded text-primary-600 focus:ring-primary-500" />
              <span>Buổi tối (18:00 - 24:00)</span>
            </label>
          </div>
        </div>

        <!-- Loại Xe -->
        <div class="space-y-2.5 pt-3 border-t border-border-default">
          <label class="text-xs font-semibold text-text-secondary uppercase tracking-wider block">Loại Xe</label>
          <select
            v-model="coachTypeFilter"
            class="w-full h-9 px-2.5 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 bg-white"
          >
            <option value="ALL">Tất cả loại xe</option>
            <option value="LIMOUSINE">Xe Limousine VIP</option>
            <option value="SLEEPER">Xe Giường Nằm</option>
            <option value="SEAT">Xe Ghế Ngồi</option>
          </select>
        </div>

        <!-- Mức Giá Vé Tối Đa -->
        <div class="space-y-2.5 pt-3 border-t border-border-default">
          <div class="flex items-center justify-between text-xs">
            <label class="font-semibold text-text-secondary uppercase tracking-wider">Mức Giá Tối Đa</label>
            <span class="font-bold text-primary-600 tabular-nums">{{ maxPrice.toLocaleString('vi-VN') }} đ</span>
          </div>
          <input
            type="range"
            min="150000"
            max="500000"
            step="25000"
            v-model.number="maxPrice"
            class="w-full accent-primary-600 cursor-pointer"
          />
          <div class="flex justify-between text-[11px] text-text-muted tabular-nums">
            <span>150.000 đ</span>
            <span>500.000 đ</span>
          </div>
        </div>

        <!-- Nhà Xe -->
        <div class="space-y-2.5 pt-3 border-t border-border-default">
          <label class="text-xs font-semibold text-text-secondary uppercase tracking-wider block">Hãng Xe</label>
          <div class="space-y-2 text-xs max-h-40 overflow-y-auto pr-1">
            <label
              v-for="op in availableOperators"
              :key="op"
              class="flex items-center gap-2 cursor-pointer"
            >
              <input type="checkbox" :value="op" v-model="selectedOperators" class="rounded text-primary-600 focus:ring-primary-500" />
              <span>{{ op }}</span>
            </label>
          </div>
        </div>
      </aside>

      <!-- Results Grid -->
      <main class="lg:col-span-3 space-y-4">
        <div v-if="filteredTrips.length > 0" class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <TripCard
            v-for="trip in filteredTrips"
            :key="trip.id"
            :trip="trip"
          />
        </div>

        <!-- Empty State -->
        <div v-else class="bg-surface rounded-xl border border-border-default p-10 shadow-card text-center">
          <EmptyState
            title="Không tìm thấy chuyến xe phù hợp"
            description="Hãy thử nới lỏng mức giá hoặc chọn khung giờ khác để tìm thấy các chuyến xe sẵn có."
            icon="🚌"
          />
          <button
            type="button"
            @click="resetFilters"
            class="mt-4 px-4 py-2 bg-primary-600 hover:bg-primary-700 text-white font-semibold rounded-lg text-xs shadow-sm transition-all"
          >
            Khôi Phục Bộ Lọc
          </button>
        </div>
      </main>
    </div>
  </div>
</template>
