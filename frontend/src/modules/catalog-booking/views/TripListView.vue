<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { catalogBookingApi } from '@/modules/catalog-booking/api'
import TripCard from '../components/TripCard.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert'
import { Skeleton } from '@/components/ui/skeleton'
import { Checkbox } from '@/components/ui/checkbox'
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
  SheetFooter,
  SheetClose
} from '@/components/ui/sheet'
import {
  Search,
  ArrowRight,
  ArrowLeftRight,
  Calendar,
  MapPin,
  Clock,
  Filter,
  SlidersHorizontal,
  RefreshCw,
  RotateCcw,
  AlertCircle,
  Bus,
  Armchair,
  CheckCircle2
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()

// =========================================================================
// 1. SEARCH PARAMETERS & ROUTE SYNC
// =========================================================================
function getTodayIsoDate() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const departureCity = ref(route.query.departureCity || route.query.from || 'Hà Nội')
const arrivalCity = ref(route.query.arrivalCity || route.query.to || 'Đà Nẵng')
const departureDate = ref(route.query.departureDate || route.query.date || getTodayIsoDate())

// Editable search header state
const isEditingSearch = ref(false)
const editFrom = ref(departureCity.value)
const editTo = ref(arrivalCity.value)
const editDate = ref(departureDate.value)
const editError = ref('')

const stations = ref([])
const availableCities = computed(() => {
  if (!stations.value || stations.value.length === 0) {
    return ['Hà Nội', 'Đà Nẵng', 'Sài Gòn', 'Hải Phòng', 'Đà Lạt', 'Nha Trang']
  }
  const citySet = new Set(stations.value.map(s => s.city).filter(Boolean))
  return Array.from(citySet)
})

// =========================================================================
// 2. TRIPS LOADING & OUT-OF-ORDER CANCELLATION
// =========================================================================
const rawTrips = ref([])
const isLoading = ref(false)
const searchError = ref(null)
let activeRequestId = 0

async function fetchTrips() {
  const currentReqId = ++activeRequestId
  isLoading.value = true
  searchError.value = null

  try {
    const params = {
      departureCity: departureCity.value,
      arrivalCity: arrivalCity.value,
      departureDate: departureDate.value
    }

    const res = await catalogBookingApi.searchTrips(params)
    // Discard response if a newer search was initiated
    if (currentReqId !== activeRequestId) return

    const data = res?.data || res || []
    rawTrips.value = Array.isArray(data) ? data : []
  } catch (err) {
    if (currentReqId !== activeRequestId) return
    searchError.value = err?.message || 'Không thể tải danh sách chuyến xe từ máy chủ. Vui lòng thử lại.'
    rawTrips.value = []
  } finally {
    if (currentReqId === activeRequestId) {
      isLoading.value = false
    }
  }
}

async function fetchStationsList() {
  try {
    const res = await catalogBookingApi.getStations()
    const list = res?.data || res || []
    stations.value = Array.isArray(list) ? list : []
  } catch (_) {
    // Non-critical background loader
  }
}

onMounted(() => {
  fetchStationsList()
  fetchTrips()
})

// Watch route changes to support back/forward navigation
watch(
  () => [route.query.departureCity, route.query.arrivalCity, route.query.departureDate, route.query.from, route.query.to, route.query.date],
  () => {
    const newFrom = route.query.departureCity || route.query.from
    const newTo = route.query.arrivalCity || route.query.to
    const newDate = route.query.departureDate || route.query.date

    if (newFrom && newFrom !== departureCity.value) departureCity.value = newFrom
    if (newTo && newTo !== arrivalCity.value) arrivalCity.value = newTo
    if (newDate && newDate !== departureDate.value) departureDate.value = newDate

    editFrom.value = departureCity.value
    editTo.value = arrivalCity.value
    editDate.value = departureDate.value

    fetchTrips()
  }
)

function submitSearchModification() {
  editError.value = ''
  if (!editFrom.value || !editFrom.value.trim()) {
    editError.value = 'Vui lòng chọn nơi xuất phát'
    return
  }
  if (!editTo.value || !editTo.value.trim()) {
    editError.value = 'Vui lòng chọn nơi đến'
    return
  }
  if (editFrom.value.trim().toLowerCase() === editTo.value.trim().toLowerCase()) {
    editError.value = 'Điểm đi và điểm đến không được trùng nhau'
    return
  }
  if (!editDate.value) {
    editError.value = 'Vui lòng chọn ngày đi'
    return
  }

  isEditingSearch.value = false
  departureCity.value = editFrom.value
  arrivalCity.value = editTo.value
  departureDate.value = editDate.value

  router.push({
    path: '/search',
    query: {
      departureCity: departureCity.value,
      arrivalCity: arrivalCity.value,
      departureDate: departureDate.value,
      from: departureCity.value,
      to: arrivalCity.value,
      date: departureDate.value
    }
  })
}

function handleSwapEdit() {
  editError.value = ''
  const tmp = editFrom.value
  editFrom.value = editTo.value
  editTo.value = tmp
}

// =========================================================================
// 3. FILTERING & SORTING LOGIC
// =========================================================================
const selectedTimeSlots = ref([])
const selectedOperators = ref([])
const coachTypeFilter = ref('ALL')
const sortBy = ref('DEPARTURE_ASC')
const maxPrice = ref(1000000)

function parseTripHour(trip) {
  const timeVal = trip.departureTime
  if (!timeVal) return 0
  if (typeof timeVal === 'string' && timeVal.includes('T')) {
    const d = new Date(timeVal)
    if (!isNaN(d.getTime())) return d.getHours()
  }
  if (typeof timeVal === 'string' && timeVal.includes(':')) {
    return parseInt(timeVal.split(':')[0], 10) || 0
  }
  return 0
}

function getTripTimeSlot(trip) {
  if (trip.timeSlot) return trip.timeSlot
  const hour = parseTripHour(trip)
  if (hour < 12) return 'MORNING'
  if (hour < 18) return 'AFTERNOON'
  return 'EVENING'
}

function getTripPrice(trip) {
  if (trip.basePrice !== undefined && trip.basePrice !== null) return Number(trip.basePrice)
  return Number(trip.price || 0)
}

function getTripOperator(trip) {
  return trip.operator || trip.tripCode || 'VeloBus Partner'
}

function getTripCoachCategory(trip) {
  if (trip.coachCategory) return trip.coachCategory
  const type = (trip.transportType || trip.coachType || '').toLowerCase()
  if (type.includes('limousine')) return 'LIMOUSINE'
  if (type.includes('giường') || type.includes('sleeper')) return 'SLEEPER'
  if (type.includes('ghế') || type.includes('seat')) return 'SEAT'
  return 'ALL'
}

// Operators extracted dynamically from raw trips
const availableOperators = computed(() => {
  const ops = new Set(rawTrips.value.map(getTripOperator).filter(Boolean))
  return Array.from(ops)
})

// Dynamic price ceiling from data
const minAvailablePrice = computed(() => {
  if (rawTrips.value.length === 0) return 100000
  return Math.min(...rawTrips.value.map(getTripPrice))
})

const maxAvailablePrice = computed(() => {
  if (rawTrips.value.length === 0) return 1000000
  const max = Math.max(...rawTrips.value.map(getTripPrice))
  return max > 0 ? max : 1000000
})

// Filtered & sorted trips
const filteredTrips = computed(() => {
  return rawTrips.value
    .filter(trip => {
      // Price
      const price = getTripPrice(trip)
      if (price > maxPrice.value) return false

      // Time Slot
      if (selectedTimeSlots.value.length > 0) {
        const slot = getTripTimeSlot(trip)
        if (!selectedTimeSlots.value.includes(slot)) return false
      }

      // Operator
      if (selectedOperators.value.length > 0) {
        const op = getTripOperator(trip)
        if (!selectedOperators.value.includes(op)) return false
      }

      // Coach Category
      if (coachTypeFilter.value !== 'ALL') {
        const cat = getTripCoachCategory(trip)
        if (cat !== coachTypeFilter.value && cat !== 'ALL') return false
      }

      return true
    })
    .sort((a, b) => {
      if (sortBy.value === 'PRICE_ASC') {
        return getTripPrice(a) - getTripPrice(b)
      }
      if (sortBy.value === 'PRICE_DESC') {
        return getTripPrice(b) - getTripPrice(a)
      }
      if (sortBy.value === 'DEPARTURE_DESC') {
        return parseTripHour(b) - parseTripHour(a)
      }
      // Default: DEPARTURE_ASC
      return parseTripHour(a) - parseTripHour(b)
    })
})

function resetFilters() {
  selectedTimeSlots.value = []
  selectedOperators.value = []
  coachTypeFilter.value = 'ALL'
  sortBy.value = 'DEPARTURE_ASC'
  maxPrice.value = maxAvailablePrice.value || 1000000
}

function toggleTimeSlot(slot) {
  const idx = selectedTimeSlots.value.indexOf(slot)
  if (idx > -1) {
    selectedTimeSlots.value.splice(idx, 1)
  } else {
    selectedTimeSlots.value.push(slot)
  }
}

function toggleOperator(op) {
  const idx = selectedOperators.value.indexOf(op)
  if (idx > -1) {
    selectedOperators.value.splice(idx, 1)
  } else {
    selectedOperators.value.push(op)
  }
}
</script>

<template>
  <div class="space-y-6 pb-16">
    <!-- ===================================================================== -->
    <!-- 1. SEARCH SUMMARY HEADER / QUICK BAR -->
    <!-- ===================================================================== -->
    <div
      class="bg-card border-b border-border shadow-xs py-4 px-4 sm:px-6 transition-all"
      data-testid="search-summary-header"
    >
      <div class="max-w-7xl mx-auto flex flex-col md:flex-row md:items-center justify-between gap-4">
        <!-- Route and date summary -->
        <div class="space-y-1">
          <div class="flex items-center gap-2">
            <Badge variant="outline" class="text-[11px] font-semibold border-primary/30 text-primary bg-primary/5 uppercase tracking-wider">
              Tìm kiếm chuyến xe
            </Badge>
            <span class="text-xs text-muted-foreground">• Khởi hành theo lịch trình</span>
          </div>

          <h1 class="text-xl sm:text-2xl font-bold text-foreground flex items-center gap-2.5">
            <span data-testid="summary-origin">{{ departureCity }}</span>
            <ArrowRight class="w-4 h-4 text-primary shrink-0" />
            <span data-testid="summary-destination">{{ arrivalCity }}</span>
          </h1>

          <p class="text-xs text-muted-foreground flex items-center gap-2 flex-wrap">
            <span class="flex items-center gap-1">
              <Calendar class="w-3.5 h-3.5 text-muted-foreground" />
              <span>Ngày đi: <strong class="text-foreground" data-testid="summary-date">{{ departureDate }}</strong></span>
            </span>
            <span class="text-border">•</span>
            <span v-if="!isLoading && !searchError">
              Tìm thấy <strong class="text-primary font-bold font-mono tabular-nums">{{ filteredTrips.length }}</strong> chuyến xe phù hợp
            </span>
            <span v-else-if="isLoading" class="text-muted-foreground animate-pulse">
              Đang tìm kiếm chuyến xe từ nhà xe...
            </span>
          </p>
        </div>

        <!-- Action buttons: Modify route & Mobile filter sheet trigger -->
        <div class="flex items-center gap-2">
          <!-- Mobile filter sheet button -->
          <div class="lg:hidden">
            <Sheet>
              <SheetTrigger as-child>
                <Button variant="outline" size="sm" class="gap-1.5 text-xs h-9" data-testid="filter-sheet-trigger">
                  <SlidersHorizontal class="w-3.5 h-3.5" />
                  <span>Bộ lọc</span>
                  <Badge v-if="selectedTimeSlots.length + selectedOperators.length > 0" class="ml-1 px-1.5 py-0 text-[10px] bg-primary text-primary-foreground">
                    {{ selectedTimeSlots.length + selectedOperators.length }}
                  </Badge>
                </Button>
              </SheetTrigger>
              <SheetContent side="bottom" class="max-h-[85vh] overflow-y-auto rounded-t-2xl p-6">
                <div data-testid="filter-sheet-content" class="h-full">
                  <SheetHeader class="text-left pb-4 border-b border-border">
                    <SheetTitle class="flex items-center gap-2 text-base font-bold">
                      <SlidersHorizontal class="w-4 h-4 text-primary" />
                      <span>Bộ lọc chuyến xe</span>
                    </SheetTitle>
                    <SheetDescription class="text-xs">
                      Lọc theo khung giờ, hãng xe và mức giá vé phù hợp.
                    </SheetDescription>
                  </SheetHeader>

                  <!-- Mobile Filter Content -->
                  <div class="space-y-5 py-4">
                  <!-- Khung giờ -->
                  <div class="space-y-2">
                    <label class="text-xs font-semibold text-foreground uppercase tracking-wider block">Khung giờ xuất bến</label>
                    <div class="grid grid-cols-1 gap-2 text-xs">
                      <label
                        class="flex items-center gap-2.5 p-2 rounded-lg border border-border hover:bg-muted/50 cursor-pointer transition-colors"
                        :class="selectedTimeSlots.includes('MORNING') ? 'bg-primary/5 border-primary/30' : ''"
                      >
                        <Checkbox
                          :checked="selectedTimeSlots.includes('MORNING')"
                          @update:checked="toggleTimeSlot('MORNING')"
                        />
                        <span>Sáng sớm (00:00 - 12:00)</span>
                      </label>
                      <label
                        class="flex items-center gap-2.5 p-2 rounded-lg border border-border hover:bg-muted/50 cursor-pointer transition-colors"
                        :class="selectedTimeSlots.includes('AFTERNOON') ? 'bg-primary/5 border-primary/30' : ''"
                      >
                        <Checkbox
                          :checked="selectedTimeSlots.includes('AFTERNOON')"
                          @update:checked="toggleTimeSlot('AFTERNOON')"
                        />
                        <span>Buổi chiều (12:00 - 18:00)</span>
                      </label>
                      <label
                        class="flex items-center gap-2.5 p-2 rounded-lg border border-border hover:bg-muted/50 cursor-pointer transition-colors"
                        :class="selectedTimeSlots.includes('EVENING') ? 'bg-primary/5 border-primary/30' : ''"
                      >
                        <Checkbox
                          :checked="selectedTimeSlots.includes('EVENING')"
                          @update:checked="toggleTimeSlot('EVENING')"
                        />
                        <span>Buổi tối (18:00 - 24:00)</span>
                      </label>
                    </div>
                  </div>

                  <!-- Loại xe -->
                  <div class="space-y-2 pt-3 border-t border-border">
                    <label class="text-xs font-semibold text-foreground uppercase tracking-wider block">Loại xe</label>
                    <select
                      v-model="coachTypeFilter"
                      class="w-full h-9 px-3 rounded-lg border border-input text-xs text-foreground bg-background focus:ring-1 focus:ring-primary"
                    >
                      <option value="ALL">Tất cả loại xe</option>
                      <option value="LIMOUSINE">Xe Limousine VIP</option>
                      <option value="SLEEPER">Xe Giường Nằm</option>
                      <option value="SEAT">Xe Ghế Ngồi</option>
                    </select>
                  </div>

                  <!-- Mức giá -->
                  <div class="space-y-2 pt-3 border-t border-border">
                    <div class="flex items-center justify-between text-xs">
                      <span class="font-semibold text-foreground uppercase tracking-wider">Mức giá tối đa</span>
                      <span class="font-bold text-primary font-mono tabular-nums">{{ maxPrice.toLocaleString('vi-VN') }} đ</span>
                    </div>
                    <input
                      type="range"
                      :min="minAvailablePrice"
                      :max="maxAvailablePrice"
                      step="25000"
                      v-model.number="maxPrice"
                      class="w-full accent-primary cursor-pointer h-2 bg-muted rounded-lg"
                    />
                  </div>

                  <!-- Hãng xe -->
                  <div v-if="availableOperators.length > 0" class="space-y-2 pt-3 border-t border-border">
                    <label class="text-xs font-semibold text-foreground uppercase tracking-wider block">Hãng xe</label>
                    <div class="space-y-1.5 max-h-36 overflow-y-auto">
                      <label
                        v-for="op in availableOperators"
                        :key="op"
                        class="flex items-center gap-2 text-xs cursor-pointer p-1.5 rounded hover:bg-muted/40"
                      >
                        <Checkbox
                          :checked="selectedOperators.includes(op)"
                          @update:checked="toggleOperator(op)"
                        />
                        <span>{{ op }}</span>
                      </label>
                    </div>
                  </div>
                </div>

                <SheetFooter class="pt-4 border-t border-border flex flex-row gap-2 justify-end">
                  <Button variant="ghost" size="sm" @click="resetFilters" class="text-xs">
                    Đặt lại
                  </Button>
                  <SheetClose as-child>
                    <Button size="sm" class="text-xs">
                      Áp dụng ({{ filteredTrips.length }})
                    </Button>
                  </SheetClose>
                </SheetFooter>
                </div>
              </SheetContent>
            </Sheet>
          </div>

          <!-- Edit search toggle button -->
          <Button
            variant="outline"
            size="sm"
            @click="isEditingSearch = !isEditingSearch"
            class="text-xs h-9 gap-1.5 font-medium"
            data-testid="toggle-edit-search-btn"
          >
            <Search class="w-3.5 h-3.5 text-muted-foreground" />
            <span>{{ isEditingSearch ? 'Đóng chỉnh sửa' : 'Đổi tuyến đường' }}</span>
          </Button>
        </div>
      </div>

      <!-- Expandable inline search modifier -->
      <div v-if="isEditingSearch" class="mt-4 pt-4 border-t border-border" data-testid="edit-search-panel">
        <form @submit.prevent="submitSearchModification" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 items-end">
          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Điểm đi</label>
            <select
              v-model="editFrom"
              class="w-full h-9 px-3 rounded-lg border border-input text-xs text-foreground bg-background focus:ring-1 focus:ring-primary"
              data-testid="edit-departure-city"
            >
              <option v-for="c in availableCities" :key="c" :value="c">{{ c }}</option>
            </select>
          </div>

          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Điểm đến</label>
            <select
              v-model="editTo"
              class="w-full h-9 px-3 rounded-lg border border-input text-xs text-foreground bg-background focus:ring-1 focus:ring-primary"
              data-testid="edit-arrival-city"
            >
              <option v-for="c in availableCities" :key="c" :value="c">{{ c }}</option>
            </select>
          </div>

          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Ngày khởi hành</label>
            <input
              type="date"
              v-model="editDate"
              :min="getTodayIsoDate()"
              class="w-full h-9 px-3 rounded-lg border border-input text-xs text-foreground bg-background focus:ring-1 focus:ring-primary"
              data-testid="edit-departure-date"
            />
          </div>

          <div class="flex items-center gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              @click="handleSwapEdit"
              class="h-9 px-2.5 shrink-0"
              title="Đổi chiều đi/đến"
            >
              <ArrowLeftRight class="w-3.5 h-3.5 text-muted-foreground" />
            </Button>
            <Button
              type="submit"
              size="sm"
              class="h-9 flex-1 text-xs gap-1.5 font-medium"
              data-testid="apply-search-modification-btn"
            >
              <Search class="w-3.5 h-3.5" />
              <span>Tìm lại</span>
            </Button>
          </div>
        </form>

        <p v-if="editError" class="text-xs text-destructive mt-2 flex items-center gap-1">
          <AlertCircle class="w-3.5 h-3.5" />
          <span>{{ editError }}</span>
        </p>
      </div>
    </div>

    <!-- ===================================================================== -->
    <!-- 2. MAIN LAYOUT: SIDEBAR FILTERS + RESULTS -->
    <!-- ===================================================================== -->
    <div class="max-w-7xl mx-auto px-4 sm:px-6 grid grid-cols-1 lg:grid-cols-4 gap-6 items-start">
      <!-- Desktop Sidebar Filter (col-span-1) -->
      <aside
        class="hidden lg:block bg-card rounded-xl border border-border p-5 shadow-xs space-y-6 sticky top-20"
        data-testid="filter-sidebar"
      >
        <div class="flex items-center justify-between pb-3 border-b border-border">
          <h2 class="text-sm font-bold text-foreground flex items-center gap-1.5">
            <SlidersHorizontal class="w-4 h-4 text-primary" />
            <span>Bộ lọc chuyến xe</span>
          </h2>
          <Button
            variant="ghost"
            size="sm"
            @click="resetFilters"
            class="text-xs text-primary hover:text-primary/80 h-7 px-2 font-medium"
          >
            <RotateCcw class="w-3 h-3 mr-1" />
            <span>Đặt lại</span>
          </Button>
        </div>

        <!-- Khung Giờ Xuất Bến -->
        <div class="space-y-2.5">
          <label class="text-xs font-semibold text-muted-foreground uppercase tracking-wider block">Khung Giờ Xuất Bến</label>
          <div class="space-y-2 text-xs">
            <label
              class="flex items-center gap-2.5 cursor-pointer p-1.5 rounded-md hover:bg-muted/50 transition-colors"
              :class="selectedTimeSlots.includes('MORNING') ? 'bg-primary/5 text-primary font-medium' : 'text-foreground'"
            >
              <Checkbox
                :checked="selectedTimeSlots.includes('MORNING')"
                @update:checked="toggleTimeSlot('MORNING')"
              />
              <span>Sáng sớm (00:00 - 12:00)</span>
            </label>
            <label
              class="flex items-center gap-2.5 cursor-pointer p-1.5 rounded-md hover:bg-muted/50 transition-colors"
              :class="selectedTimeSlots.includes('AFTERNOON') ? 'bg-primary/5 text-primary font-medium' : 'text-foreground'"
            >
              <Checkbox
                :checked="selectedTimeSlots.includes('AFTERNOON')"
                @update:checked="toggleTimeSlot('AFTERNOON')"
              />
              <span>Buổi chiều (12:00 - 18:00)</span>
            </label>
            <label
              class="flex items-center gap-2.5 cursor-pointer p-1.5 rounded-md hover:bg-muted/50 transition-colors"
              :class="selectedTimeSlots.includes('EVENING') ? 'bg-primary/5 text-primary font-medium' : 'text-foreground'"
            >
              <Checkbox
                :checked="selectedTimeSlots.includes('EVENING')"
                @update:checked="toggleTimeSlot('EVENING')"
              />
              <span>Buổi tối (18:00 - 24:00)</span>
            </label>
          </div>
        </div>

        <!-- Loại Xe -->
        <div class="space-y-2.5 pt-4 border-t border-border">
          <label class="text-xs font-semibold text-muted-foreground uppercase tracking-wider block">Loại Xe</label>
          <select
            v-model="coachTypeFilter"
            class="w-full h-9 px-2.5 rounded-lg border border-input text-xs text-foreground bg-background focus:ring-1 focus:ring-primary"
            data-testid="filter-coach-type"
          >
            <option value="ALL">Tất cả loại xe</option>
            <option value="LIMOUSINE">Xe Limousine VIP</option>
            <option value="SLEEPER">Xe Giường Nằm</option>
            <option value="SEAT">Xe Ghế Ngồi</option>
          </select>
        </div>

        <!-- Mức Giá Vé Tối Đa -->
        <div class="space-y-2.5 pt-4 border-t border-border">
          <div class="flex items-center justify-between text-xs">
            <label class="font-semibold text-muted-foreground uppercase tracking-wider">Mức Giá Tối Đa</label>
            <span class="font-bold text-primary font-mono tabular-nums">{{ maxPrice.toLocaleString('vi-VN') }} đ</span>
          </div>
          <input
            type="range"
            :min="minAvailablePrice"
            :max="maxAvailablePrice"
            step="25000"
            v-model.number="maxPrice"
            class="w-full accent-primary cursor-pointer h-2 bg-muted rounded-lg"
            data-testid="filter-max-price"
          />
          <div class="flex justify-between text-[11px] text-muted-foreground font-mono tabular-nums">
            <span>{{ minAvailablePrice.toLocaleString('vi-VN') }} đ</span>
            <span>{{ maxAvailablePrice.toLocaleString('vi-VN') }} đ</span>
          </div>
        </div>

        <!-- Nhà Xe / Hãng Xe -->
        <div v-if="availableOperators.length > 0" class="space-y-2.5 pt-4 border-t border-border">
          <label class="text-xs font-semibold text-muted-foreground uppercase tracking-wider block">Hãng Xe</label>
          <div class="space-y-1.5 text-xs max-h-48 overflow-y-auto pr-1">
            <label
              v-for="op in availableOperators"
              :key="op"
              class="flex items-center gap-2 cursor-pointer p-1.5 rounded-md hover:bg-muted/50 transition-colors"
              :class="selectedOperators.includes(op) ? 'bg-primary/5 text-primary font-medium' : 'text-foreground'"
            >
              <Checkbox
                :checked="selectedOperators.includes(op)"
                @update:checked="toggleOperator(op)"
              />
              <span class="truncate">{{ op }}</span>
            </label>
          </div>
        </div>
      </aside>

      <!-- Main Results Column (col-span-3) -->
      <main class="lg:col-span-3 space-y-4">
        <!-- Top Toolbar: Quick Sort & Active Filter Tags -->
        <div class="flex flex-wrap items-center justify-between gap-3 bg-card p-3 rounded-xl border border-border shadow-xs">
          <div class="flex items-center gap-2 flex-wrap">
            <span class="text-xs font-medium text-muted-foreground">Sắp xếp theo:</span>
            <select
              v-model="sortBy"
              class="h-8 px-2.5 rounded-md border border-input text-xs text-foreground bg-background focus:ring-1 focus:ring-primary font-medium"
              data-testid="sort-select"
            >
              <option value="DEPARTURE_ASC">Giờ xuất phát: Sớm nhất</option>
              <option value="DEPARTURE_DESC">Giờ xuất phát: Muộn nhất</option>
              <option value="PRICE_ASC">Giá vé: Thấp đến cao</option>
              <option value="PRICE_DESC">Giá vé: Cao đến thấp</option>
            </select>
          </div>

          <!-- Active filter pills indicator -->
          <div v-if="selectedTimeSlots.length > 0 || selectedOperators.length > 0 || coachTypeFilter !== 'ALL'" class="flex items-center gap-1.5 flex-wrap">
            <Badge
              v-for="slot in selectedTimeSlots"
              :key="slot"
              variant="secondary"
              class="text-[11px] cursor-pointer hover:bg-destructive/10"
              @click="toggleTimeSlot(slot)"
            >
              {{ slot === 'MORNING' ? 'Sáng' : slot === 'AFTERNOON' ? 'Chiều' : 'Tối' }} ✕
            </Badge>
            <Badge
              v-if="coachTypeFilter !== 'ALL'"
              variant="secondary"
              class="text-[11px] cursor-pointer hover:bg-destructive/10"
              @click="coachTypeFilter = 'ALL'"
            >
              {{ coachTypeFilter }} ✕
            </Badge>
          </div>
        </div>

        <!-- ================================================================= -->
        <!-- STATE A: LOADING SKELETONS -->
        <!-- ================================================================= -->
        <div v-if="isLoading" class="space-y-4" data-testid="trip-list-loading">
          <Card v-for="i in 3" :key="i" class="border border-border bg-card p-5 space-y-4">
            <div class="flex items-center justify-between pb-3 border-b border-border">
              <Skeleton class="h-5 w-32 rounded-md" />
              <Skeleton class="h-4 w-20 rounded-md" />
            </div>
            <div class="space-y-2">
              <Skeleton class="h-6 w-56 rounded-md" />
              <div class="flex items-center gap-6 pt-1">
                <Skeleton class="h-8 w-24 rounded-md" />
                <Skeleton class="h-4 w-8 rounded-full" />
                <Skeleton class="h-8 w-24 rounded-md" />
              </div>
            </div>
            <div class="flex items-center justify-between pt-3 border-t border-border">
              <Skeleton class="h-6 w-28 rounded-md" />
              <Skeleton class="h-9 w-24 rounded-lg" />
            </div>
          </Card>
        </div>

        <!-- ================================================================= -->
        <!-- STATE B: ERROR WITH RETRY -->
        <!-- ================================================================= -->
        <div v-else-if="searchError" class="space-y-4" data-testid="trip-list-error">
          <Alert variant="destructive" class="border-destructive/30 bg-destructive/5">
            <AlertCircle class="w-4 h-4 text-destructive" />
            <AlertTitle class="font-bold">Lỗi tải danh sách chuyến xe</AlertTitle>
            <AlertDescription class="text-xs mt-1">
              {{ searchError }}
            </AlertDescription>
          </Alert>

          <div class="bg-card rounded-xl border border-border p-8 text-center space-y-4">
            <p class="text-xs text-muted-foreground">
              Không thể kết nối đến máy chủ quản lý lịch trình. Vui lòng kiểm tra lại kết nối mạng hoặc thử lại.
            </p>
            <Button
              variant="default"
              size="sm"
              @click="fetchTrips"
              class="gap-1.5 text-xs font-semibold"
              data-testid="retry-search-btn"
            >
              <RefreshCw class="w-3.5 h-3.5" />
              <span>Thử lại</span>
            </Button>
          </div>
        </div>

        <!-- ================================================================= -->
        <!-- STATE C: SUCCESS - RESULTS GRID -->
        <!-- ================================================================= -->
        <div
          v-else-if="filteredTrips.length > 0"
          class="grid grid-cols-1 md:grid-cols-2 gap-4"
          data-testid="trip-list-results"
        >
          <TripCard
            v-for="trip in filteredTrips"
            :key="trip.id"
            :trip="trip"
          />
        </div>

        <!-- ================================================================= -->
        <!-- STATE D: EMPTY STATE -->
        <!-- ================================================================= -->
        <div
          v-else
          class="bg-card rounded-xl border border-border p-10 shadow-xs text-center space-y-4"
          data-testid="trip-list-empty"
        >
          <EmptyState
            title="Không tìm thấy chuyến xe phù hợp"
            description="Hãy thử nới lỏng bộ lọc mức giá, chọn khung giờ khác hoặc đổi ngày khởi hành để tìm thấy lịch trình khả dụng."
            icon="🚌"
          />
          <div class="flex items-center justify-center gap-3 pt-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              @click="resetFilters"
              class="text-xs font-medium gap-1.5"
              data-testid="reset-filters-btn"
            >
              <RotateCcw class="w-3.5 h-3.5" />
              <span>Khôi phục bộ lọc</span>
            </Button>
            <Button
              type="button"
              size="sm"
              @click="isEditingSearch = true"
              class="text-xs font-medium gap-1.5"
            >
              <Search class="w-3.5 h-3.5" />
              <span>Đổi tuyến đường</span>
            </Button>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>
