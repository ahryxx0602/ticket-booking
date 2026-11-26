<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { catalogBookingApi } from '@/modules/catalog-booking/api'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Bus,
  Search,
  ArrowLeftRight,
  Calendar,
  MapPin,
  Sparkles,
  ShieldCheck,
  Zap,
  CheckCircle2,
  AlertCircle,
  ArrowRight
} from 'lucide-vue-next'

const router = useRouter()

// =========================================================================
// 1. DATA LOADERS: STATIONS & ACTIVE BANNERS
// =========================================================================
const stations = ref([])
const isLoadingStations = ref(false)
const stationsError = ref(null)

const banners = ref([])
const isLoadingBanners = ref(false)
const bannersError = ref(null)

// Form fields
const departureCity = ref('Hà Nội')
const arrivalCity = ref('Đà Nẵng')
const departureDate = ref(getTodayIsoDate())
const searchError = ref('')

function getTodayIsoDate() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

async function fetchStations() {
  isLoadingStations.value = true
  stationsError.value = null
  try {
    const res = await catalogBookingApi.getStations()
    const list = res?.data || res || []
    stations.value = Array.isArray(list) ? list : []
    // Set sensible defaults if available
    if (stations.value.length > 0) {
      const cities = Array.from(new Set(stations.value.map(s => s.city).filter(Boolean)))
      if (cities.length >= 2) {
        if (!cities.includes(departureCity.value)) departureCity.value = cities[0]
        if (!cities.includes(arrivalCity.value)) arrivalCity.value = cities[1]
      }
    }
  } catch (err) {
    stationsError.value = err?.message || 'Không thể tải danh sách điểm đón/trả từ máy chủ'
  } finally {
    isLoadingStations.value = false
  }
}

async function fetchBanners() {
  isLoadingBanners.value = true
  bannersError.value = null
  try {
    const res = await catalogBookingApi.getPublicBanners(6)
    const list = res?.data || res || []
    banners.value = Array.isArray(list) ? list : []
  } catch (err) {
    bannersError.value = err?.message || 'Không thể tải biểu ngữ khuyến mãi'
    banners.value = []
  } finally {
    isLoadingBanners.value = false
  }
}

onMounted(() => {
  fetchStations()
  fetchBanners()
})

// Unique cities for selection
const availableCities = computed(() => {
  if (!stations.value || stations.value.length === 0) {
    return ['Hà Nội', 'Đà Nẵng', 'Sài Gòn', 'Hải Phòng', 'Đà Lạt', 'Nha Trang']
  }
  const citySet = new Set(stations.value.map(s => s.city).filter(Boolean))
  return Array.from(citySet)
})

// Quick route suggestions derived from cities
const popularRoutes = computed(() => [
  { from: 'Hà Nội', to: 'Đà Nẵng', label: 'Hà Nội → Đà Nẵng', tag: 'Phổ biến nhất' },
  { from: 'Sài Gòn', to: 'Đà Lạt', label: 'Sài Gòn → Đà Lạt', tag: 'Du lịch cuối tuần' },
  { from: 'Hà Nội', to: 'Hải Phòng', label: 'Hà Nội → Hải Phòng', tag: 'Tuyến nhanh' },
  { from: 'Sài Gòn', to: 'Nha Trang', label: 'Sài Gòn → Nha Trang', tag: 'Nghỉ dưỡng biển' }
])

function handleSwap() {
  searchError.value = ''
  const tmp = departureCity.value
  departureCity.value = arrivalCity.value
  arrivalCity.value = tmp
}

function selectQuickRoute(route) {
  searchError.value = ''
  departureCity.value = route.from
  arrivalCity.value = route.to
  handleSearch()
}

function handleSearch() {
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

  if (!departureDate.value) {
    searchError.value = 'Vui lòng chọn ngày khởi hành'
    return
  }

  router.push({
    path: '/search',
    query: {
      departureCity: departureCity.value,
      arrivalCity: arrivalCity.value,
      departureDate: departureDate.value,
      // Backwards compatible aliases
      from: departureCity.value,
      to: arrivalCity.value,
      date: departureDate.value
    }
  })
}
</script>

<template>
  <div class="space-y-12 pb-16">
    <!-- Hero Banner with Warm Minimalist Mobility Identity -->
    <section class="bg-card border-b border-border py-10 sm:py-14 px-4 transition-colors">
      <div class="max-w-7xl mx-auto space-y-6">
        <div class="max-w-3xl space-y-3">
          <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold bg-primary/10 text-primary border border-primary/20">
            <Sparkles class="w-3.5 h-3.5" />
            <span>Sàn Vé Xe Khách Cao Cấp • Warm Minimalist Mobility</span>
          </div>
          <h1 class="text-2xl sm:text-4xl font-extrabold tracking-tight text-foreground leading-tight">
            Đặt Vé Xe Khách Liên Tỉnh Thông Minh & Tiện Lợi
          </h1>
          <p class="text-muted-foreground text-xs sm:text-sm leading-relaxed max-w-2xl">
            Tra cứu tuyến đường, đối soát lịch trình thời gian thực, giữ chỗ với Redis Distributed Lock và thanh toán bảo mật cùng VeloBus.
          </p>
        </div>

        <!-- Search Form Card (Vexere & FlixBus UX Pattern) -->
        <Card class="shadow-lg border-border bg-background">
          <CardContent class="p-4 sm:p-6 space-y-4">
            <!-- Inline Search Error -->
            <Alert v-if="searchError" variant="destructive" class="py-2.5 px-3">
              <AlertCircle class="h-4 w-4" />
              <AlertDescription class="text-xs font-medium">
                {{ searchError }}
              </AlertDescription>
            </Alert>

            <form @submit.prevent="handleSearch" class="grid grid-cols-1 md:grid-cols-12 gap-3 items-end">
              <!-- Origin Station / City -->
              <div class="col-span-1 md:col-span-5 lg:col-span-3 space-y-1.5">
                <label for="departure-city-input" class="block text-xs font-semibold text-foreground flex items-center gap-1.5">
                  <MapPin class="w-3.5 h-3.5 text-primary" />
                  <span>Nơi xuất phát</span>
                </label>
                <div class="relative">
                  <select
                    id="departure-city-input"
                    data-testid="departure-city-select"
                    v-model="departureCity"
                    class="w-full h-11 px-3.5 rounded-lg border border-border text-xs text-foreground bg-card focus:ring-2 focus:ring-primary focus:outline-none transition-all cursor-pointer font-medium"
                  >
                    <option v-for="c in availableCities" :key="'from-' + c" :value="c">
                      {{ c }}
                    </option>
                  </select>
                </div>
              </div>

              <!-- Swap Button (FlixBus UX Pattern) -->
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

              <!-- Destination Station / City -->
              <div class="col-span-1 md:col-span-5 lg:col-span-3 space-y-1.5">
                <label for="arrival-city-input" class="block text-xs font-semibold text-foreground flex items-center gap-1.5">
                  <MapPin class="w-3.5 h-3.5 text-primary" />
                  <span>Nơi đến</span>
                </label>
                <div class="relative">
                  <select
                    id="arrival-city-input"
                    data-testid="arrival-city-select"
                    v-model="arrivalCity"
                    class="w-full h-11 px-3.5 rounded-lg border border-border text-xs text-foreground bg-card focus:ring-2 focus:ring-primary focus:outline-none transition-all cursor-pointer font-medium"
                  >
                    <option v-for="c in availableCities" :key="'to-' + c" :value="c">
                      {{ c }}
                    </option>
                  </select>
                </div>
              </div>

              <!-- Departure Date -->
              <div class="col-span-1 md:col-span-6 lg:col-span-3 space-y-1.5">
                <label for="departure-date-input" class="block text-xs font-semibold text-foreground flex items-center gap-1.5">
                  <Calendar class="w-3.5 h-3.5 text-primary" />
                  <span>Ngày khởi hành</span>
                </label>
                <input
                  id="departure-date-input"
                  v-model="departureDate"
                  type="date"
                  :min="getTodayIsoDate()"
                  class="w-full h-11 px-3.5 rounded-lg border border-border text-xs text-foreground bg-card focus:ring-2 focus:ring-primary focus:outline-none transition-all font-mono font-medium"
                />
              </div>

              <!-- Search Action Button -->
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
          </CardContent>
        </Card>

        <!-- Quick Route Suggestions (Derived from real catalog) -->
        <div class="flex flex-wrap items-center gap-2 pt-1 text-xs text-muted-foreground">
          <span class="font-semibold text-foreground flex items-center gap-1">
            <Bus class="w-3.5 h-3.5 text-primary" />
            Tuyến phổ biến:
          </span>
          <button
            v-for="route in popularRoutes"
            :key="route.label"
            type="button"
            class="px-3 py-1 rounded-full border border-border bg-background hover:bg-muted hover:border-primary/40 text-xs font-medium text-foreground transition-all cursor-pointer flex items-center gap-1.5"
            @click="selectQuickRoute(route)"
          >
            <span>{{ route.label }}</span>
            <span class="text-[10px] text-muted-foreground">({{ route.tag }})</span>
          </button>
        </div>
      </div>
    </section>

    <!-- Marketing Banners Carousel / Grid (Backend-fed via /api/v1/public/banners/active) -->
    <section class="max-w-7xl mx-auto px-4 space-y-4">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-lg sm:text-xl font-bold text-foreground">
            Ưu Đãi & Tuyến Xe Nổi Bật
          </h2>
          <p class="text-xs text-muted-foreground mt-0.5">
            Các chương trình khuyến mại đối tác nhà xe được phê duyệt chính thức
          </p>
        </div>
        <Badge variant="outline" class="text-xs font-semibold">
          {{ banners.length }} ưu đãi khả dụng
        </Badge>
      </div>

      <!-- Banners Loading Skeletons -->
      <div v-if="isLoadingBanners" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <Skeleton v-for="i in 3" :key="i" class="h-44 w-full rounded-xl" />
      </div>

      <!-- Banners Grid -->
      <div v-else-if="banners.length > 0" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <Card
          v-for="b in banners"
          :key="b.id"
          class="overflow-hidden border border-border bg-card shadow-xs hover:border-primary/50 transition-all cursor-pointer group"
        >
          <div class="relative h-40 bg-muted overflow-hidden">
            <img
              v-if="b.imageUrl"
              :src="b.imageUrl"
              :alt="b.title"
              class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
              loading="lazy"
            />
            <div v-else class="w-full h-full flex items-center justify-center bg-primary/5 text-primary">
              <Bus class="w-10 h-10 stroke-[1.5]" />
            </div>
            <div class="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent flex items-end p-3.5">
              <p class="text-xs font-bold text-white line-clamp-1">
                {{ b.title }}
              </p>
            </div>
          </div>
        </Card>
      </div>

      <!-- Empty Banners State -->
      <div v-else class="p-6 rounded-xl border border-dashed border-border text-center text-xs text-muted-foreground">
        Hiện chưa có biểu ngữ quảng bá hoạt động từ các đối tác nhà xe.
      </div>
    </section>

    <!-- Trust & Platform Architecture Invariants -->
    <section class="max-w-7xl mx-auto px-4">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card class="border border-border bg-card p-4 space-y-2">
          <div class="flex items-center gap-2.5 text-primary font-bold text-sm">
            <Zap class="w-4 h-4" />
            <span>Redis Distributed Lock</span>
          </div>
          <p class="text-xs text-muted-foreground leading-relaxed">
            Khóa giữ chỗ thời gian thực 10 phút, ngăn chặn tình trạng chọn trùng ghế tuyệt đối giữa hàng ngàn hành khách.
          </p>
        </Card>

        <Card class="border border-border bg-card p-4 space-y-2">
          <div class="flex items-center gap-2.5 text-primary font-bold text-sm">
            <ShieldCheck class="w-4 h-4" />
            <span>Thanh Toán Escrow Ký Quỹ</span>
          </div>
          <p class="text-xs text-muted-foreground leading-relaxed">
            Dòng tiền được bảo vệ an toàn qua cổng VNPay, quyết toán cho nhà xe sau khi chuyến xe hoàn thành an toàn +2h.
          </p>
        </Card>

        <Card class="border border-border bg-card p-4 space-y-2">
          <div class="flex items-center gap-2.5 text-primary font-bold text-sm">
            <CheckCircle2 class="w-4 h-4" />
            <span>Vé Điện Tử QR Chuẩn Hóa</span>
          </div>
          <p class="text-xs text-muted-foreground leading-relaxed">
            Mã QR vé điện tử xác thực bằng chữ ký số HMAC-SHA256, lên xe nhanh chóng không cần in vé giấy.
          </p>
        </Card>
      </div>
    </section>
  </div>
</template>
