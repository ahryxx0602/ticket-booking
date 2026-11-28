<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { catalogBookingApi } from '@/modules/catalog-booking/api'
import TripSearchForm from '../components/TripSearchForm.vue'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Bus,
  ShieldCheck,
  Zap,
  CheckCircle2,
  Sparkles
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

async function fetchStations() {
  isLoadingStations.value = true
  stationsError.value = null
  try {
    const res = await catalogBookingApi.getStations()
    const list = res?.data || res || []
    stations.value = Array.isArray(list) ? list : []
  } catch (err) {
    stationsError.value = err?.message || 'Không thể tải danh sách điểm đón/trả từ máy chủ'
    stations.value = []
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

function onSearchSubmit({ departureCity, arrivalCity, departureDate }) {
  router.push({
    path: '/search',
    query: {
      departureCity,
      arrivalCity,
      departureDate,
      from: departureCity,
      to: arrivalCity,
      date: departureDate
    }
  })
}
</script>

<template>
  <div class="space-y-12 pb-16">
    <!-- Hero Section with Clean Hierarchy & TripSearchForm -->
    <section class="bg-card border-b border-border py-10 sm:py-14 px-4 transition-colors">
      <div class="max-w-7xl mx-auto space-y-6">
        <div class="max-w-3xl space-y-3">
          <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold bg-primary/10 text-primary border border-primary/20">
            <Bus class="w-3.5 h-3.5" />
            <span>Hệ Thống Đặt Vé Xe Khách Trực Tuyến</span>
          </div>
          <h1 class="text-2xl sm:text-4xl font-extrabold tracking-tight text-foreground leading-tight">
            Đặt Vé Xe Khách Liên Tỉnh Tiện Lợi & Nhanh Chóng
          </h1>
          <p class="text-muted-foreground text-xs sm:text-sm leading-relaxed max-w-2xl">
            Tra cứu lịch trình, lựa chọn vị trí chỗ ngồi ưng ý và thanh toán an toàn trực tuyến cùng VeloBus.
          </p>
        </div>

        <!-- Search Form Card with Shared TripSearchForm -->
        <Card class="shadow-lg border-border bg-background">
          <CardContent class="p-4 sm:p-6">
            <TripSearchForm
              :stations="stations"
              :is-loading-stations="isLoadingStations"
              :stations-error="stationsError"
              @search="onSearchSubmit"
            />
          </CardContent>
        </Card>
      </div>
    </section>

    <!-- Marketing Banners Carousel / Grid (Backend-fed via /api/v1/public/banners/active) -->
    <section class="max-w-7xl mx-auto px-4 space-y-4">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-lg sm:text-xl font-bold text-foreground">
            Ưu Đãi & Thông Tin Tuyến Xe
          </h2>
          <p class="text-xs text-muted-foreground mt-0.5">
            Các chương trình nổi bật được cập nhật từ hệ thống đối tác nhà xe
          </p>
        </div>
        <Badge v-if="banners.length > 0" variant="outline" class="text-xs font-semibold">
          {{ banners.length }} thông tin
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
          class="overflow-hidden border border-border bg-card shadow-xs hover:border-primary/50 transition-all cursor-pointer group p-4 space-y-3"
        >
          <div v-if="b.imageUrl" class="relative h-40 bg-muted rounded-lg overflow-hidden">
            <img
              :src="b.imageUrl"
              :alt="b.title"
              class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
              loading="lazy"
            />
          </div>
          <div class="space-y-1.5">
            <Badge v-if="b.badge" variant="secondary" class="text-[10px] font-semibold">
              {{ b.badge }}
            </Badge>
            <h3 class="text-sm font-bold text-foreground line-clamp-1 group-hover:text-primary transition-colors">
              {{ b.title }}
            </h3>
            <p v-if="b.description" class="text-xs text-muted-foreground line-clamp-2">
              {{ b.description }}
            </p>
          </div>
        </Card>
      </div>

      <!-- Empty Banners State -->
      <div v-else class="p-6 rounded-xl border border-dashed border-border text-center text-xs text-muted-foreground">
        Hiện chưa có biểu ngữ hoặc chương trình khuyến mại nào từ hệ thống đối tác nhà xe.
      </div>
    </section>

    <!-- Trust & Platform Values (Customer-Facing & Authentic) -->
    <section class="max-w-7xl mx-auto px-4">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card class="border border-border bg-card p-4 space-y-2">
          <div class="flex items-center gap-2.5 text-primary font-bold text-sm">
            <Zap class="w-4 h-4" />
            <span>Giữ Chỗ Trực Tuyến Đảm Bảo</span>
          </div>
          <p class="text-xs text-muted-foreground leading-relaxed">
            Giữ chỗ trực tuyến tức thời trong 10 phút, đảm bảo vị trí giường nằm đã chọn không bị trùng lặp.
          </p>
        </Card>

        <Card class="border border-border bg-card p-4 space-y-2">
          <div class="flex items-center gap-2.5 text-primary font-bold text-sm">
            <ShieldCheck class="w-4 h-4" />
            <span>Thanh Toán An Toàn & Bảo Mật</span>
          </div>
          <p class="text-xs text-muted-foreground leading-relaxed">
            Tích hợp cổng thanh toán trực tuyến bảo mật, hỗ trợ đa dạng phương thức thanh toán thẻ và ví điện tử.
          </p>
        </Card>

        <Card class="border border-border bg-card p-4 space-y-2">
          <div class="flex items-center gap-2.5 text-primary font-bold text-sm">
            <CheckCircle2 class="w-4 h-4" />
            <span>Vé Điện Tử Tiện Lợi</span>
          </div>
          <p class="text-xs text-muted-foreground leading-relaxed">
            Nhận mã vé điện tử ngay sau khi đặt vé thành công, lên xe thuận tiện qua điện thoại không cần in vé giấy.
          </p>
        </Card>
      </div>
    </section>
  </div>
</template>
