<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { catalogBookingApi } from '@/modules/catalog-booking/api'
import TripCard from '../components/TripCard.vue'
import TripSearchForm from '../components/TripSearchForm.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert'
import { Skeleton } from '@/components/ui/skeleton'
import { Checkbox } from '@/components/ui/checkbox'
import { Slider } from '@/components/ui/slider'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from '@/components/ui/select'
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
  Calendar,
  SlidersHorizontal,
  RefreshCw,
  RotateCcw,
  AlertCircle,
  X,
  Bus
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

function normalizeDate(dateVal) {
  const todayIso = getTodayIsoDate()
  if (!dateVal || dateVal < todayIso) {
    return todayIso
  }
  return dateVal
}

const departureCity = ref(route.query.departureCity || route.query.from || 'Hà Nội')
const arrivalCity = ref(route.query.arrivalCity || route.query.to || 'Đà Nẵng')
const departureDate = ref(normalizeDate(route.query.departureDate || route.query.date))

// Editable search header state
const isEditingSearch = ref(false)
const stations = ref([])
const isLoadingStations = ref(false)
const stationsError = ref(null)

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
    // Discard response if a newer search was initiated (Out-of-order response cancellation)
    if (currentReqId !== activeRequestId) return

    const data = res?.data || res || []
    rawTrips.value = Array.isArray(data) ? data : []

    // PRE-04: Initialize price ceiling to highest price found (avoid excluding >1,000,000 VND trips)
    if (rawTrips.value.length > 0) {
      const prices = rawTrips.value.map(getTripPrice).filter(p => p > 0)
      if (prices.length > 0) {
        const highest = Math.max(...prices)
        if (highest > maxPrice.value) {
          maxPrice.value = highest
        }
      }
    }
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
  isLoadingStations.value = true
  stationsError.value = null
  try {
    const res = await catalogBookingApi.getStations()
    const list = res?.data || res || []
    stations.value = Array.isArray(list) ? list : []
  } catch (err) {
    stationsError.value = err?.message || 'Không thể tải danh bạ bến xe'
  } finally {
    isLoadingStations.value = false
  }
}

onMounted(() => {
  fetchStationsList()
  fetchTrips()
})

// Watch route changes to support browser back/forward navigation
watch(
  () => [route.query.departureCity, route.query.arrivalCity, route.query.departureDate, route.query.from, route.query.to, route.query.date],
  () => {
    const newFrom = route.query.departureCity || route.query.from
    const newTo = route.query.arrivalCity || route.query.to
    const newDate = normalizeDate(route.query.departureDate || route.query.date)

    if (newFrom && newFrom !== departureCity.value) departureCity.value = newFrom
    if (newTo && newTo !== arrivalCity.value) arrivalCity.value = newTo
    if (newDate && newDate !== departureDate.value) departureDate.value = newDate

    fetchTrips()
  }
)

function onHeaderSearchSubmit({ departureCity: from, arrivalCity: to, departureDate: date }) {
  isEditingSearch.value = false
  departureCity.value = from
  arrivalCity.value = to
  departureDate.value = date

  router.push({
    path: '/search',
    query: {
      departureCity: from,
      arrivalCity: to,
      departureDate: date,
      from,
      to,
      date
    }
  })
}

// =========================================================================
// 3. FILTERING & SORTING LOGIC
// =========================================================================
const selectedTimeSlots = ref([])
const coachTypeFilter = ref('ALL')
const sortBy = ref('DEPARTURE_ASC')
const maxPrice = ref(2000000)

function parseTripTimestamp(trip) {
  const timeVal = trip.departureTime
  if (!timeVal) return 0
  try {
    const d = new Date(timeVal)
    if (!isNaN(d.getTime())) return d.getTime()
  } catch (_) {}
  return 0
}

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

function getTripCoachCategory(trip) {
  if (trip.coachCategory) return trip.coachCategory
  const type = (trip.transportType || trip.coachType || '').toLowerCase()
  if (type.includes('limousine')) return 'LIMOUSINE'
  if (type.includes('giường') || type.includes('sleeper')) return 'SLEEPER'
  if (type.includes('ghế') || type.includes('seat')) return 'SEAT'
  return 'OTHER'
}

// Dynamic price bounds
const minAvailablePrice = computed(() => {
  if (rawTrips.value.length === 0) return 100000
  const prices = rawTrips.value.map(getTripPrice).filter(p => p > 0)
  return prices.length > 0 ? Math.min(...prices) : 100000
})

const maxAvailablePrice = computed(() => {
  if (rawTrips.value.length === 0) return 2000000
  const prices = rawTrips.value.map(getTripPrice).filter(p => p > 0)
  const max = prices.length > 0 ? Math.max(...prices) : 2000000
  return Math.max(max, 1000000)
})

// Filtered & sorted trips
const filteredTrips = computed(() => {
  return rawTrips.value
    .filter(trip => {
      // Price filter
      const price = getTripPrice(trip)
      if (price > maxPrice.value) return false

      // Time Slot
      if (selectedTimeSlots.value.length > 0) {
        const slot = getTripTimeSlot(trip)
        if (!selectedTimeSlots.value.includes(slot)) return false
      }

      // Coach Category
      if (coachTypeFilter.value !== 'ALL') {
        const cat = getTripCoachCategory(trip)
        if (cat !== coachTypeFilter.value) return false
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
        return parseTripTimestamp(b) - parseTripTimestamp(a)
      }
      // Default: DEPARTURE_ASC (Sort by full timestamp)
      return parseTripTimestamp(a) - parseTripTimestamp(b)
    })
})

function resetFilters() {
  selectedTimeSlots.value = []
  coachTypeFilter.value = 'ALL'
  sortBy.value = 'DEPARTURE_ASC'
  maxPrice.value = maxAvailablePrice.value || 2000000
}

function toggleTimeSlot(slot) {
  const idx = selectedTimeSlots.value.indexOf(slot)
  if (idx > -1) {
    selectedTimeSlots.value.splice(idx, 1)
  } else {
    selectedTimeSlots.value.push(slot)
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
              Lịch Trình Chuyến Xe
            </Badge>
            <span class="text-xs text-muted-foreground">• Khởi hành theo tuyến đường</span>
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
              Đang truy vấn lịch trình từ hệ thống...
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
                  <Badge v-if="selectedTimeSlots.length > 0 || coachTypeFilter !== 'ALL'" class="ml-1 px-1.5 py-0 text-[10px] bg-primary text-primary-foreground">
                    {{ selectedTimeSlots.length + (coachTypeFilter !== 'ALL' ? 1 : 0) }}
                  </Badge>
                </Button>
              </SheetTrigger>
              <SheetContent side="bottom" class="max-h-[85vh] overflow-y-auto rounded-t-2xl p-6" data-testid="filter-sheet-content">
                <SheetHeader class="text-left pb-4 border-b border-border">
                  <SheetTitle class="flex items-center gap-2 text-base font-bold">
                    <SlidersHorizontal class="w-4 h-4 text-primary" />
                    <span>Bộ lọc chuyến xe</span>
                  </SheetTitle>
                  <SheetDescription class="text-xs">
                    Lọc theo khung giờ xuất bến, loại xe và mức giá vé.
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

                  <!-- Loại xe (Approved Select Component) -->
                  <div class="space-y-2 pt-3 border-t border-border">
                    <label class="text-xs font-semibold text-foreground uppercase tracking-wider block">Loại xe</label>
                    <Select v-model="coachTypeFilter">
                      <SelectTrigger class="w-full h-9 bg-card border-border text-xs">
                        <SelectValue placeholder="Chọn loại xe" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="ALL" class="text-xs">Tất cả loại xe</SelectItem>
                        <SelectItem value="LIMOUSINE" class="text-xs">Xe Limousine VIP</SelectItem>
                        <SelectItem value="SLEEPER" class="text-xs">Xe Giường Nằm</SelectItem>
                        <SelectItem value="SEAT" class="text-xs">Xe Ghế Ngồi</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>

                  <!-- Mức giá (Approved Slider Component) -->
                  <div class="space-y-3 pt-3 border-t border-border">
                    <div class="flex items-center justify-between text-xs">
                      <span class="font-semibold text-foreground uppercase tracking-wider">Mức giá tối đa</span>
                      <span class="font-bold text-primary font-mono tabular-nums">{{ maxPrice.toLocaleString('vi-VN') }} đ</span>
                    </div>
                    <Slider
                      :model-value="[maxPrice]"
                      :min="minAvailablePrice"
                      :max="maxAvailablePrice"
                      :step="25000"
                      @update:model-value="(val) => maxPrice = val[0]"
                      data-testid="filter-max-price-slider-mobile"
                    />
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

      <!-- Expandable TripSearchForm modifier -->
      <div v-if="isEditingSearch" class="mt-4 pt-4 border-t border-border" data-testid="edit-search-panel">
        <TripSearchForm
          :initial-departure-city="departureCity"
          :initial-arrival-city="arrivalCity"
          :initial-departure-date="departureDate"
          :stations="stations"
          :is-loading-stations="isLoadingStations"
          :stations-error="stationsError"
          @search="onHeaderSearchSubmit"
        />
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

        <!-- Loại Xe (Approved Select Component) -->
        <div class="space-y-2.5 pt-4 border-t border-border">
          <label class="text-xs font-semibold text-muted-foreground uppercase tracking-wider block">Loại Xe</label>
          <Select v-model="coachTypeFilter">
            <SelectTrigger data-testid="filter-coach-type" class="w-full h-9 bg-card border-border text-xs">
              <SelectValue placeholder="Chọn loại xe" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL" class="text-xs">Tất cả loại xe</SelectItem>
              <SelectItem value="LIMOUSINE" class="text-xs">Xe Limousine VIP</SelectItem>
              <SelectItem value="SLEEPER" class="text-xs">Xe Giường Nằm</SelectItem>
              <SelectItem value="SEAT" class="text-xs">Xe Ghế Ngồi</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <!-- Mức Giá Vé Tối Đa (Approved Slider Primitive) -->
        <div class="space-y-3 pt-4 border-t border-border">
          <div class="flex items-center justify-between text-xs">
            <label class="font-semibold text-muted-foreground uppercase tracking-wider">Mức Giá Tối Đa</label>
            <span class="font-bold text-primary font-mono tabular-nums">{{ maxPrice.toLocaleString('vi-VN') }} đ</span>
          </div>
          <Slider
            :model-value="[maxPrice]"
            :min="minAvailablePrice"
            :max="maxAvailablePrice"
            :step="25000"
            @update:model-value="(val) => maxPrice = val[0]"
            data-testid="filter-max-price-slider"
          />
          <div class="flex justify-between text-[11px] text-muted-foreground font-mono tabular-nums">
            <span>{{ minAvailablePrice.toLocaleString('vi-VN') }} đ</span>
            <span>{{ maxAvailablePrice.toLocaleString('vi-VN') }} đ</span>
          </div>
        </div>
      </aside>

      <!-- Main Results Column (col-span-3) -->
      <main class="lg:col-span-3 space-y-4">
        <!-- Top Toolbar: Quick Sort & Active Filter Tags -->
        <div class="flex flex-wrap items-center justify-between gap-3 bg-card p-3 rounded-xl border border-border shadow-xs">
          <div class="flex items-center gap-2 flex-wrap">
            <span class="text-xs font-medium text-muted-foreground">Sắp xếp theo:</span>
            <Select v-model="sortBy">
              <SelectTrigger data-testid="sort-select" class="h-8 w-48 text-xs bg-card border-border font-medium">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="DEPARTURE_ASC" class="text-xs">Giờ xuất phát: Sớm nhất</SelectItem>
                <SelectItem value="DEPARTURE_DESC" class="text-xs">Giờ xuất phát: Muộn nhất</SelectItem>
                <SelectItem value="PRICE_ASC" class="text-xs">Giá vé: Thấp đến cao</SelectItem>
                <SelectItem value="PRICE_DESC" class="text-xs">Giá vé: Cao đến thấp</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <!-- Active filter pills indicator -->
          <div v-if="selectedTimeSlots.length > 0 || coachTypeFilter !== 'ALL'" class="flex items-center gap-1.5 flex-wrap">
            <Badge
              v-for="slot in selectedTimeSlots"
              :key="slot"
              variant="secondary"
              class="text-[11px] cursor-pointer hover:bg-destructive/10 inline-flex items-center gap-1"
              @click="toggleTimeSlot(slot)"
            >
              <span>{{ slot === 'MORNING' ? 'Sáng' : slot === 'AFTERNOON' ? 'Chiều' : 'Tối' }}</span>
              <X class="w-3 h-3 text-muted-foreground" />
            </Badge>
            <Badge
              v-if="coachTypeFilter !== 'ALL'"
              variant="secondary"
              class="text-[11px] cursor-pointer hover:bg-destructive/10 inline-flex items-center gap-1"
              @click="coachTypeFilter = 'ALL'"
            >
              <span>{{ coachTypeFilter }}</span>
              <X class="w-3 h-3 text-muted-foreground" />
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
