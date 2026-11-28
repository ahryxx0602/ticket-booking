<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from '@/components/ui/select'
import {
  Popover,
  PopoverContent,
  PopoverTrigger
} from '@/components/ui/popover'
import { Calendar } from '@/components/ui/calendar'
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Skeleton } from '@/components/ui/skeleton'
import {
  MapPin,
  Calendar as CalendarIcon,
  ArrowLeftRight,
  Search,
  AlertCircle
} from 'lucide-vue-next'
import { today, getLocalTimeZone, parseDate } from '@internationalized/date'

interface Station {
  id?: number | string
  code?: string
  name?: string
  city?: string
}

const props = withDefaults(
  defineProps<{
    initialDepartureCity?: string
    initialArrivalCity?: string
    initialDepartureDate?: string
    stations?: Station[]
    isLoadingStations?: boolean
    stationsError?: string | null
    compact?: boolean
  }>(),
  {
    initialDepartureCity: '',
    initialArrivalCity: '',
    initialDepartureDate: '',
    stations: () => [],
    isLoadingStations: false,
    stationsError: null,
    compact: false
  }
)

const emits = defineEmits<{
  (e: 'search', payload: { departureCity: string; arrivalCity: string; departureDate: string }): void
}>()

const timeZone = getLocalTimeZone()
const todayDate = today(timeZone)

// Model states
const departureCity = ref<string>(props.initialDepartureCity || 'Hà Nội')
const arrivalCity = ref<string>(props.initialArrivalCity || 'Đà Nẵng')

function parseInitialDate(dateStr?: string) {
  if (!dateStr) return todayDate
  try {
    return parseDate(dateStr)
  } catch (_) {
    return todayDate
  }
}

const selectedDate = ref<any>(parseInitialDate(props.initialDepartureDate))
const isDatePickerOpen = ref(false)
const searchError = ref('')

// Watch for external query changes
watch(
  () => props.initialDepartureCity,
  (val) => {
    if (val) departureCity.value = val
  }
)
watch(
  () => props.initialArrivalCity,
  (val) => {
    if (val) arrivalCity.value = val
  }
)
watch(
  () => props.initialDepartureDate,
  (val) => {
    if (val) selectedDate.value = parseInitialDate(val)
  }
)

// PRE-04: Derive available cities purely from authentic stations without hardcoded fallbacks
const availableCities = computed(() => {
  if (!props.stations || props.stations.length === 0) return []
  const citySet = new Set<string>()
  props.stations.forEach(s => {
    if (s.city && s.city.trim()) {
      citySet.add(s.city.trim())
    }
  })
  return Array.from(citySet).sort()
})

// Auto-select first two distinct cities if not set
watch(
  availableCities,
  (cities) => {
    if (cities.length >= 2) {
      if (!departureCity.value || !cities.includes(departureCity.value)) {
        departureCity.value = cities[0]
      }
      if (!arrivalCity.value || !cities.includes(arrivalCity.value)) {
        arrivalCity.value = cities[1]
      }
    }
  },
  { immediate: true }
)

function handleSwap() {
  searchError.value = ''
  const tmp = departureCity.value
  departureCity.value = arrivalCity.value
  arrivalCity.value = tmp
}

function formatDateDisplay(d: any) {
  if (!d) return 'Chọn ngày đi'
  const day = String(d.day).padStart(2, '0')
  const month = String(d.month).padStart(2, '0')
  const year = d.year
  return `${day}/${month}/${year}`
}

function handleDateSelect(val: any) {
  selectedDate.value = val
  isDatePickerOpen.value = false
  searchError.value = ''
}

function validateAndSubmit() {
  searchError.value = ''

  if (!departureCity.value || !departureCity.value.trim()) {
    searchError.value = 'Vui lòng chọn nơi xuất phát'
    return
  }

  if (!arrivalCity.value || !arrivalCity.value.trim()) {
    searchError.value = 'Vui lòng chọn nơi đến'
    return
  }

  if (departureCity.value.trim().toLowerCase() === arrivalCity.value.trim().toLowerCase()) {
    searchError.value = 'Điểm đi và điểm đến không được trùng nhau. Vui lòng chọn lộ trình khác.'
    return
  }

  if (!selectedDate.value) {
    searchError.value = 'Vui lòng chọn ngày khởi hành'
    return
  }

  // PRE-04: Validate departure date is not in the past (Timezone-safe)
  const isoDate = selectedDate.value.toString()
  const todayIso = todayDate.toString()
  if (isoDate < todayIso) {
    searchError.value = 'Ngày khởi hành không được ở trong quá khứ'
    return
  }

  emits('search', {
    departureCity: departureCity.value.trim(),
    arrivalCity: arrivalCity.value.trim(),
    departureDate: isoDate
  })
}
</script>

<template>
  <div class="space-y-3" data-testid="trip-search-form">
    <!-- Inline Validation Error -->
    <Alert v-if="searchError" variant="destructive" class="py-2.5 px-3" data-testid="search-validation-error">
      <AlertCircle class="h-4 w-4 shrink-0 text-destructive" />
      <AlertDescription class="text-xs font-medium">
        {{ searchError }}
      </AlertDescription>
    </Alert>

    <!-- Stations Loading Skeleton -->
    <div v-if="isLoadingStations" class="grid grid-cols-1 md:grid-cols-12 gap-3 items-end">
      <div class="md:col-span-4 space-y-1.5">
        <Skeleton class="h-4 w-24 rounded" />
        <Skeleton class="h-11 w-full rounded-lg" />
      </div>
      <div class="md:col-span-1 flex justify-center pb-2">
        <Skeleton class="h-9 w-9 rounded-full" />
      </div>
      <div class="md:col-span-4 space-y-1.5">
        <Skeleton class="h-4 w-24 rounded" />
        <Skeleton class="h-11 w-full rounded-lg" />
      </div>
      <div class="md:col-span-3 space-y-1.5">
        <Skeleton class="h-4 w-24 rounded" />
        <Skeleton class="h-11 w-full rounded-lg" />
      </div>
    </div>

    <!-- Stations Loading Error Alert -->
    <Alert v-if="stationsError" variant="destructive" class="py-2.5 px-3 mb-3">
      <AlertCircle class="h-4 w-4 shrink-0" />
      <AlertDescription class="text-xs">
        Lỗi tải danh mục bến xe: {{ stationsError }}
      </AlertDescription>
    </Alert>

    <!-- Actual Search Form with Approved Shared Components -->
    <form v-if="!isLoadingStations" @submit.prevent="validateAndSubmit" class="grid grid-cols-1 md:grid-cols-12 gap-3 items-end">
      <!-- 1. Origin Station / City (Select / Combobox) -->
      <div class="col-span-1 md:col-span-5 lg:col-span-3 space-y-1.5">
        <label for="departure-city-select" class="block text-xs font-semibold text-foreground flex items-center gap-1.5">
          <MapPin class="w-3.5 h-3.5 text-primary" />
          <span>Nơi xuất phát</span>
        </label>
        <Select v-model="departureCity">
          <SelectTrigger
            id="departure-city-select"
            data-testid="departure-city-select"
            class="h-11 w-full bg-card border-border text-xs font-medium text-foreground focus:ring-1 focus:ring-primary"
          >
            <SelectValue placeholder="Chọn thành phố đi" />
          </SelectTrigger>
          <SelectContent class="max-h-60">
            <SelectItem
              v-for="city in availableCities"
              :key="'dep-' + city"
              :value="city"
              class="text-xs cursor-pointer"
            >
              {{ city }}
            </SelectItem>
          </SelectContent>
        </Select>
      </div>

      <!-- 2. Swap Button -->
      <div class="col-span-1 md:col-span-2 lg:col-span-1 flex justify-center pb-1">
        <Button
          type="button"
          variant="outline"
          size="sm"
          class="h-9 w-9 rounded-full p-0 border-border hover:bg-muted"
          @click="handleSwap"
          title="Đổi chiều điểm đi và điểm đến"
          aria-label="Đổi chiều điểm đi và điểm đến"
        >
          <ArrowLeftRight class="w-4 h-4 text-muted-foreground" />
        </Button>
      </div>

      <!-- 3. Destination Station / City (Select / Combobox) -->
      <div class="col-span-1 md:col-span-5 lg:col-span-3 space-y-1.5">
        <label for="arrival-city-select" class="block text-xs font-semibold text-foreground flex items-center gap-1.5">
          <MapPin class="w-3.5 h-3.5 text-primary" />
          <span>Nơi đến</span>
        </label>
        <Select v-model="arrivalCity">
          <SelectTrigger
            id="arrival-city-select"
            data-testid="arrival-city-select"
            class="h-11 w-full bg-card border-border text-xs font-medium text-foreground focus:ring-1 focus:ring-primary"
          >
            <SelectValue placeholder="Chọn thành phố đến" />
          </SelectTrigger>
          <SelectContent class="max-h-60">
            <SelectItem
              v-for="city in availableCities"
              :key="'arr-' + city"
              :value="city"
              class="text-xs cursor-pointer"
            >
              {{ city }}
            </SelectItem>
          </SelectContent>
        </Select>
      </div>

      <!-- 4. Departure Date (Popover + Calendar vi-VN Monday-first) -->
      <div class="col-span-1 md:col-span-6 lg:col-span-3 space-y-1.5">
        <label class="block text-xs font-semibold text-foreground flex items-center gap-1.5">
          <CalendarIcon class="w-3.5 h-3.5 text-primary" />
          <span>Ngày khởi hành</span>
        </label>
        <Popover v-model:open="isDatePickerOpen">
          <PopoverTrigger as-child>
            <Button
              variant="outline"
              type="button"
              data-testid="departure-datepicker-trigger"
              class="w-full h-11 justify-start text-left font-normal bg-card border-border text-foreground hover:bg-muted/50 cursor-pointer text-xs"
            >
              <CalendarIcon class="mr-2 h-4 w-4 text-primary shrink-0" />
              <span class="font-mono font-medium">{{ formatDateDisplay(selectedDate) }}</span>
            </Button>
          </PopoverTrigger>
          <PopoverContent class="w-auto p-0 border border-border rounded-xl shadow-lg bg-card" align="start">
            <Calendar
              v-model="selectedDate"
              :min-value="(todayDate as any)"
              @update:model-value="handleDateSelect"
            />
          </PopoverContent>
        </Popover>
      </div>

      <!-- 5. Search Action Button -->
      <div class="col-span-1 md:col-span-6 lg:col-span-2 flex items-end">
        <Button
          type="submit"
          size="lg"
          data-testid="btn-search-trips"
          class="w-full h-11 text-xs font-bold gap-2 shadow-sm"
        >
          <Search class="w-4 h-4" />
          <span>Tìm Chuyến Xe</span>
        </Button>
      </div>
    </form>
  </div>
</template>
