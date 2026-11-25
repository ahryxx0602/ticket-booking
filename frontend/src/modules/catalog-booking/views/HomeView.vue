<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import {
  Bus,
  Search,
  ArrowLeftRight,
  Calendar,
  Clock,
  MapPin,
  Users,
  ArrowRight,
  ShieldCheck,
  Zap,
  Sparkles
} from 'lucide-vue-next'

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

function handleSwap() {
  const tmp = departureStation.value
  departureStation.value = arrivalStation.value
  arrivalStation.value = tmp
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
  <div class="space-y-10 pb-16">
    <!-- Hero Banner with Claude+ Minimalist Mobility Identity -->
    <section class="bg-card border-b border-border py-12 px-4 transition-colors">
      <div class="max-w-7xl mx-auto space-y-6">
        <div class="max-w-2xl space-y-3">
          <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold bg-primary/10 text-primary border border-primary/20">
            <Sparkles class="w-3.5 h-3.5" />
            <span>Sàn Vé Xe Khách Cao Cấp • Mobility SaaS</span>
          </div>
          <h1 class="text-3xl sm:text-4xl font-extrabold tracking-tight text-foreground leading-tight">
            Đặt Vé Xe Khách Liên Tỉnh Thông Minh & Tiện Lợi
          </h1>
          <p class="text-muted-foreground text-xs sm:text-sm leading-relaxed">
            Khóa giữ chỗ thời gian thực với Redis Distributed Lock • Thanh toán bảo mật VNPay • Vé điện tử QR HMAC-SHA256
          </p>
        </div>

        <!-- Search Form Card (Vexere & FlixBus UX Pattern) -->
        <Card class="shadow-lg border-border bg-background">
          <CardContent class="p-4 sm:p-5">
            <div class="grid grid-cols-1 md:grid-cols-12 lg:grid-cols-12 gap-3 items-end">
              <!-- Origin Station -->
              <div class="col-span-1 md:col-span-5 lg:col-span-3 space-y-1.5">
                <label class="block text-xs font-semibold text-foreground flex items-center gap-1.5">
                  <MapPin class="w-3.5 h-3.5 text-primary" />
                  <span>Nơi xuất phát</span>
                </label>
                <select
                  v-model="departureStation"
                  class="w-full h-11 px-3 rounded-lg border border-border text-xs text-foreground bg-card focus:ring-2 focus:ring-primary focus:outline-none transition-all cursor-pointer font-medium"
                >
                  <option value="HN-GP">Hà Nội - Bến xe Giáp Bát</option>
                  <option value="HN-MY">Hà Nội - Bến xe Mỹ Đình</option>
                  <option value="SG-MD">Sài Gòn - Bến xe Miền Đông</option>
                </select>
              </div>

              <!-- Swap Button (FlixBus Pattern) -->
              <div class="col-span-1 md:col-span-2 lg:col-span-1 flex justify-center pb-1">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  class="h-9 w-9 rounded-full p-0 border-border hover:bg-muted"
                  @click="handleSwap"
                  title="Đổi chiều điểm đi và điểm đến"
                >
                  <ArrowLeftRight class="w-4 h-4 text-muted-foreground" />
                </Button>
              </div>

              <!-- Destination Station -->
              <div class="col-span-1 md:col-span-5 lg:col-span-3 space-y-1.5">
                <label class="block text-xs font-semibold text-foreground flex items-center gap-1.5">
                  <MapPin class="w-3.5 h-3.5 text-primary" />
                  <span>Nơi đến</span>
                </label>
                <select
                  v-model="arrivalStation"
                  class="w-full h-11 px-3 rounded-lg border border-border text-xs text-foreground bg-card focus:ring-2 focus:ring-primary focus:outline-none transition-all cursor-pointer font-medium"
                >
                  <option value="DN-TT">Đà Nẵng - Bến xe Trung Tâm</option>
                  <option value="DL-LD">Đà Lạt - Bến xe Liên Tỉnh</option>
                  <option value="HP-CL">Hải Phòng - Bến xe Cầu Rào</option>
                </select>
              </div>

              <!-- Date Picker -->
              <div class="col-span-1 md:col-span-7 lg:col-span-3 space-y-1.5">
                <label class="block text-xs font-semibold text-foreground flex items-center gap-1.5">
                  <Calendar class="w-3.5 h-3.5 text-primary" />
                  <span>Ngày khởi hành</span>
                </label>
                <input
                  type="date"
                  v-model="departureDate"
                  class="w-full h-11 px-3 rounded-lg border border-border text-xs text-foreground bg-card focus:ring-2 focus:ring-primary focus:outline-none transition-all font-medium"
                />
              </div>

              <!-- Search Action Button -->
              <div class="col-span-1 md:col-span-5 lg:col-span-2">
                <Button
                  type="button"
                  size="sm"
                  class="w-full h-11 font-semibold text-xs shadow-xs gap-2"
                  @click="handleSearch"
                >
                  <Search class="w-4 h-4" />
                  <span>Tìm Chuyến Xe Ngay</span>
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </section>

    <!-- Featured Trips Section -->
    <section class="max-w-7xl mx-auto px-4 space-y-6">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-xl font-bold tracking-tight text-foreground">Chuyến Xe Phổ Biến & Nổi Bật</h2>
          <p class="text-xs text-muted-foreground mt-0.5">Các tuyến xe liên tỉnh có lượt đặt vé cao nhất hôm nay</p>
        </div>
        <button
          type="button"
          @click="handleSearch"
          class="text-xs text-primary font-semibold hover:underline cursor-pointer flex items-center gap-1"
        >
          <span>Xem tất cả chuyến</span>
          <ArrowRight class="w-3.5 h-3.5" />
        </button>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        <Card
          v-for="trip in featuredTrips"
          :key="trip.id"
          class="border-border bg-card shadow-xs hover:shadow-md transition-all flex flex-col justify-between"
        >
          <CardHeader class="pb-3 space-y-2">
            <div class="flex items-center justify-between">
              <Badge variant="outline" class="border-primary/30 bg-primary/10 text-primary text-[11px] font-semibold">
                {{ trip.operator }}
              </Badge>
              <span class="text-xs text-emerald-600 dark:text-emerald-400 font-medium tabular-nums flex items-center gap-1">
                <Users class="w-3.5 h-3.5" />
                Còn {{ trip.availableSeats }} chỗ trống
              </span>
            </div>

            <div>
              <CardTitle class="text-base font-bold text-foreground">{{ trip.route }}</CardTitle>
              <p class="text-xs text-muted-foreground flex items-center gap-2 mt-1.5 font-medium">
                <span class="flex items-center gap-1"><Clock class="w-3.5 h-3.5 text-primary" /> {{ trip.departureTime }}</span>
                <span>→</span>
                <span>{{ trip.arrivalTime }}</span>
                <span class="text-muted-foreground/80">({{ trip.duration }})</span>
              </p>
            </div>

            <p class="text-xs text-muted-foreground flex items-center gap-1.5 pt-1">
              <Bus class="w-3.5 h-3.5 text-muted-foreground" />
              <span>{{ trip.coachType }} • Biển số: <strong class="text-foreground font-mono">{{ trip.plate }}</strong></span>
            </p>
          </CardHeader>

          <CardFooter class="pt-3 border-t border-border flex items-center justify-between">
            <div>
              <span class="text-[11px] text-muted-foreground block">Giá vé từ</span>
              <span class="text-lg font-bold text-primary tabular-nums font-mono">
                {{ trip.price.toLocaleString('vi-VN') }} <span class="text-xs font-semibold">đ</span>
              </span>
            </div>

            <Button
              type="button"
              size="sm"
              class="h-9 px-4 text-xs font-semibold gap-1.5 shadow-xs"
              @click="navigateToSeats(trip.id)"
            >
              <span>Chọn Chỗ</span>
              <ArrowRight class="w-3.5 h-3.5" />
            </Button>
          </CardFooter>
        </Card>
      </div>
    </section>
  </div>
</template>
