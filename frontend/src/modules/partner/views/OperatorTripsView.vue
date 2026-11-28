<script setup>
import { ref, computed, onMounted } from 'vue'
import { partnerApi } from '../api'
import { catalogBookingApi } from '@/modules/catalog-booking/api'
import { toast } from 'vue-sonner'
import {
  Calendar,
  Clock,
  MapPin,
  Truck,
  AlertTriangle,
  Plus,
  CheckCircle,
  RefreshCw,
  Info,
  DollarSign,
  Route,
  ArrowRight
} from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle
} from '@/components/ui/dialog'

// Supporting metadata
const vehicles = ref([])
const stations = ref([])
const isLoadingMetadata = ref(false)
const metadataError = ref('')

// Authoritative trips from Backend (FQA-05 & FQA-06)
const sessionTrips = ref([])
const trips = sessionTrips
const isLoadingTrips = ref(false)
const tripsError = ref('')
const currentPage = ref(0)
const pageSize = ref(10)
const totalPages = ref(1)
const totalElements = ref(0)
const filterStatus = ref('')
const filterVehicleId = ref('')

const isCreatingTrip = ref(false)
const isScheduleModalOpen = ref(false)
const tripFormError = ref('')

const tripForm = ref({
  tripCode: '',
  vehicleId: '',
  departureStationId: '',
  arrivalStationId: '',
  departureTime: '',
  arrivalTime: '',
  basePrice: 250000
})

// Station lookups
const stationMap = computed(() => {
  const map = new Map()
  for (const s of stations.value) {
    map.set(Number(s.id), s)
  }
  return map
})

// Active vehicles available for scheduling
const activeVehicles = computed(() => {
  return vehicles.value.filter(v => v.status === 'ACTIVE')
})

const vehicleMap = computed(() => {
  const map = new Map()
  for (const v of vehicles.value) {
    map.set(Number(v.id), v)
  }
  return map
})

// Invariant: Turnaround Buffer 30 minutes check
const turnaroundBufferViolation = computed(() => {
  if (!tripForm.value.vehicleId || !tripForm.value.departureTime) return null

  const selectedVehicleId = Number(tripForm.value.vehicleId)
  const newDepTime = new Date(tripForm.value.departureTime).getTime()
  if (isNaN(newDepTime)) return null

  // Check against all known trips of this vehicle
  for (const existing of trips.value) {
    if (Number(existing.vehicleId) === selectedVehicleId) {
      const existingArrTime = new Date(existing.arrivalTime).getTime()
      const existingDepTime = new Date(existing.departureTime).getTime()

      // Turnaround buffer: new departure must be at least 30 mins (1,800,000 ms) AFTER previous arrival
      if (newDepTime >= existingDepTime && newDepTime < existingArrTime + 30 * 60 * 1000) {
        const requiredTime = new Date(existingArrTime + 30 * 60 * 1000)
        const hh = String(requiredTime.getHours()).padStart(2, '0')
        const mm = String(requiredTime.getMinutes()).padStart(2, '0')
        const plate = vehicleMap.value.get(selectedVehicleId)?.plateNumber || `#${selectedVehicleId}`
        return `Xe ${plate} hoàn thành chuyến trước lúc ${new Date(existing.arrivalTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}. Yêu cầu thời gian đệm bảo dưỡng/vệ sinh tối thiểu 30 phút (sớm nhất có thể xuất bến là ${hh}:${mm})!`
      }
    }
  }

  return null
})

async function fetchMetadata() {
  isLoadingMetadata.value = true
  metadataError.value = ''
  try {
    const [vehiclesRes, stationsRes] = await Promise.all([
      partnerApi.getVehicles(),
      catalogBookingApi.getStations()
    ])
    const vList = vehiclesRes?.data?.data || vehiclesRes?.data || []
    const sList = stationsRes?.data?.data || stationsRes?.data || []
    vehicles.value = Array.isArray(vList) ? vList : []
    stations.value = Array.isArray(sList) ? sList : []
  } catch (err) {
    metadataError.value = err?.response?.data?.message || err?.message || 'Không thể nạp dữ liệu bến xe hoặc đội xe'
    toast.error(metadataError.value)
  } finally {
    isLoadingMetadata.value = false
  }
}

async function fetchTrips(page = currentPage.value) {
  isLoadingTrips.value = true
  tripsError.value = ''
  try {
    const params = {
      page,
      size: pageSize.value
    }
    if (filterStatus.value) params.status = filterStatus.value
    if (filterVehicleId.value) params.vehicleId = Number(filterVehicleId.value)

    const res = await partnerApi.getMyTrips(params)
    const pageData = res?.data?.data || res?.data
    if (pageData && Array.isArray(pageData.items)) {
      sessionTrips.value = pageData.items
      totalPages.value = pageData.totalPages || 1
      totalElements.value = pageData.totalElements || pageData.items.length
      currentPage.value = pageData.page || 0
    } else if (Array.isArray(pageData)) {
      sessionTrips.value = pageData
      totalPages.value = 1
      totalElements.value = pageData.length
      currentPage.value = 0
    }
  } catch (err) {
    tripsError.value = err?.response?.data?.message || err?.message || 'Không thể tải danh sách chuyến xe từ máy chủ'
    toast.error(tripsError.value)
  } finally {
    isLoadingTrips.value = false
  }
}

function resetTripForm() {
  tripForm.value = {
    tripCode: '',
    vehicleId: '',
    departureStationId: '',
    arrivalStationId: '',
    departureTime: '',
    arrivalTime: '',
    basePrice: 250000
  }
  tripFormError.value = ''
}

function openScheduleModal() {
  resetTripForm()
  isScheduleModalOpen.value = true
}

async function handleScheduleTrip() {
  tripFormError.value = ''

  if (turnaroundBufferViolation.value) {
    tripFormError.value = turnaroundBufferViolation.value
    return
  }

  const code = tripForm.value.tripCode?.trim()
  if (!code) {
    tripFormError.value = 'Mã chuyến xe không được để trống'
    return
  }

  if (!tripForm.value.vehicleId) {
    tripFormError.value = 'Vui lòng chọn phương tiện vận hành'
    return
  }

  const depStationId = Number(tripForm.value.departureStationId)
  const arrStationId = Number(tripForm.value.arrivalStationId)
  if (!depStationId || !arrStationId) {
    tripFormError.value = 'Vui lòng chọn bến đi và bến đến'
    return
  }
  if (depStationId === arrStationId) {
    tripFormError.value = 'Bến đến phải khác bến đi'
    return
  }

  if (!tripForm.value.departureTime || !tripForm.value.arrivalTime) {
    tripFormError.value = 'Vui lòng nhập đầy đủ thời gian xuất bến và cập bến'
    return
  }

  const depDate = new Date(tripForm.value.departureTime)
  const arrDate = new Date(tripForm.value.arrivalTime)
  if (arrDate.getTime() <= depDate.getTime()) {
    tripFormError.value = 'Thời gian cập bến phải sau thời gian xuất bến'
    return
  }

  const price = Number(tripForm.value.basePrice)
  if (!price || price < 10000) {
    tripFormError.value = 'Giá vé cơ sở tối thiểu là 10.000 VNĐ'
    return
  }

  const payload = {
    tripCode: code.toUpperCase(),
    vehicleId: Number(tripForm.value.vehicleId),
    departureStationId: depStationId,
    arrivalStationId: arrStationId,
    departureTime: tripForm.value.departureTime,
    arrivalTime: tripForm.value.arrivalTime,
    basePrice: price
  }

  isCreatingTrip.value = true
  try {
    const res = await partnerApi.createPartnerTrip(payload)
    const createdData = res?.data?.data || res?.data || {}

    if (!createdData || createdData.id == null) {
      throw new Error('Máy chủ phản hồi tạo chuyến không hợp lệ (thiếu mã định danh chuyến xe)')
    }

    sessionTrips.value.unshift({
      id: createdData.id,
      tripCode: payload.tripCode,
      vehicleId: payload.vehicleId,
      departureStationId: payload.departureStationId,
      arrivalStationId: payload.arrivalStationId,
      departureTime: payload.departureTime,
      arrivalTime: payload.arrivalTime,
      basePrice: payload.basePrice,
      status: 'SCHEDULED'
    })

    toast.success(`Tạo lịch chuyến xe ${payload.tripCode} thành công`)
    isScheduleModalOpen.value = false
    try {
      await fetchTrips(0)
    } catch (_) {}
  } catch (err) {
    tripFormError.value = err?.response?.data?.message || err?.message || 'Lỗi khi lập lịch chuyến xe'
    toast.error(tripFormError.value)
  } finally {
    isCreatingTrip.value = false
  }
}

function formatPrice(val) {
  if (val == null) return '0 ₫'
  return Number(val).toLocaleString('vi-VN') + ' ₫'
}

function formatDateTime(val) {
  if (!val) return '—'
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  return d.toLocaleString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  })
}

function formatTripStatus(status) {
  switch (status) {
    case 'SCHEDULED':
      return { text: 'Đã Lên Lịch', class: 'bg-emerald-500/10 text-emerald-600 border-emerald-500/20' }
    case 'RUNNING':
      return { text: 'Đang Chạy', class: 'bg-blue-500/10 text-blue-600 border-blue-500/20' }
    case 'COMPLETED':
      return { text: 'Hoàn Thành', class: 'bg-muted text-muted-foreground border-border' }
    case 'CANCELLED':
      return { text: 'Đã Hủy', class: 'bg-destructive/10 text-destructive border-destructive/20' }
    default:
      return { text: status || 'Chưa rõ', class: 'bg-muted text-muted-foreground border-border' }
  }
}

onMounted(async () => {
  await fetchMetadata()
  if (typeof partnerApi.getMyTrips === 'function') {
    try {
      await fetchTrips(0)
    } catch (_) {}
  }
})
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-border">
      <div>
        <h1 class="text-2xl font-bold tracking-tight text-foreground flex items-center gap-2">
          <Calendar class="w-6 h-6 text-primary" />
          <span>Lập Lịch Chuyến Xe & Điều Độ Vận Hành</span>
        </h1>
        <p class="text-xs text-muted-foreground mt-1">
          Hệ thống điều phối tuyến đường liên tỉnh, bảo đảm quy tắc thời gian đệm bảo dưỡng 30 phút giữa các chuyến.
        </p>
      </div>

      <div class="flex items-center gap-2">
        <Button
          size="sm"
          data-testid="btn-open-schedule"
          class="bg-primary hover:bg-primary/90 text-primary-foreground text-xs font-semibold"
          @click="openScheduleModal"
        >
          <Plus class="w-3.5 h-3.5 mr-1" /> Lập Lịch Chuyến Mới
        </Button>
      </div>
    </div>

    <!-- Backend Contract Capability Note -->
    <Alert class="border-primary/20 bg-primary/5">
      <Info class="h-4 w-4 text-primary" />
      <AlertTitle class="text-xs font-semibold text-primary">Thông báo điều độ & Dữ liệu chuyến</AlertTitle>
      <AlertDescription class="text-xs text-muted-foreground">
        Hệ thống tích hợp trực tiếp lệnh tạo lịch trình <code>POST /api/v1/partners/trips</code>. Các chuyến xe tạo mới sẽ được kiểm tra ràng buộc thời gian đệm bảo dưỡng (Turnaround Buffer &ge; 30 phút) tự động trước khi ghi vào sổ điều độ.
      </AlertDescription>
    </Alert>

    <!-- Error states -->
    <div v-if="metadataError" class="p-6 bg-destructive/10 border border-destructive/20 rounded-lg text-xs text-destructive flex items-center justify-between">
      <div class="flex items-center gap-2">
        <AlertTriangle class="w-4 h-4" />
        <span>{{ metadataError }}</span>
      </div>
      <Button variant="outline" size="sm" @click="fetchMetadata" class="text-xs">Thử lại</Button>
    </div>

    <div v-if="tripsError" class="p-6 bg-destructive/10 border border-destructive/20 rounded-lg text-xs text-destructive flex items-center justify-between">
      <div class="flex items-center gap-2">
        <AlertTriangle class="w-4 h-4" />
        <span>{{ tripsError }}</span>
      </div>
      <Button variant="outline" size="sm" @click="fetchTrips(currentPage)" class="text-xs">Thử lại</Button>
    </div>

    <!-- Filter Bar -->
    <div class="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 bg-card p-3 rounded-xl border border-border">
      <div class="flex flex-wrap items-center gap-2">
        <span class="text-xs font-semibold text-muted-foreground mr-1">Lọc:</span>
        <Button
          size="sm"
          variant="ghost"
          class="h-8 text-xs font-medium"
          :class="filterStatus === '' ? 'bg-primary/10 text-primary font-bold' : ''"
          @click="filterStatus = ''; fetchTrips(0)"
        >
          Tất cả
        </Button>
        <Button
          size="sm"
          variant="ghost"
          class="h-8 text-xs font-medium"
          :class="filterStatus === 'SCHEDULED' ? 'bg-primary/10 text-primary font-bold' : ''"
          @click="filterStatus = 'SCHEDULED'; fetchTrips(0)"
        >
          Đã lên lịch
        </Button>
        <Button
          size="sm"
          variant="ghost"
          class="h-8 text-xs font-medium"
          :class="filterStatus === 'RUNNING' ? 'bg-primary/10 text-primary font-bold' : ''"
          @click="filterStatus = 'RUNNING'; fetchTrips(0)"
        >
          Đang chạy
        </Button>
        <Button
          size="sm"
          variant="ghost"
          class="h-8 text-xs font-medium"
          :class="filterStatus === 'COMPLETED' ? 'bg-primary/10 text-primary font-bold' : ''"
          @click="filterStatus = 'COMPLETED'; fetchTrips(0)"
        >
          Hoàn thành
        </Button>
      </div>

      <div class="flex items-center gap-2">
        <Button variant="outline" size="sm" @click="fetchTrips(currentPage)" class="h-8 text-xs" :disabled="isLoadingTrips">
          <RefreshCw class="w-3.5 h-3.5 mr-1" :class="isLoadingTrips ? 'animate-spin' : ''" /> Làm mới
        </Button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="isLoadingTrips" class="p-16 text-center border rounded-xl bg-card">
      <RefreshCw class="w-6 h-6 mx-auto animate-spin mb-3 text-primary" />
      <p class="text-xs text-muted-foreground">Đang tải danh sách chuyến xe từ máy chủ...</p>
    </div>

    <!-- Empty State -->
    <div v-else-if="trips.length === 0" class="p-12 text-center border border-dashed rounded-xl bg-muted/20">
      <Route class="w-10 h-10 mx-auto text-muted-foreground/50 mb-2" />
      <h3 class="text-sm font-semibold text-foreground">Không tìm thấy chuyến xe nào</h3>
      <p class="text-xs text-muted-foreground mt-1 max-w-sm mx-auto">
        Hiện tại chưa có chuyến xe nào theo bộ lọc đã chọn. Bắt đầu lập lịch chuyến xe đầu tiên ngay hôm nay.
      </p>
      <Button size="sm" class="mt-4 text-xs" @click="openScheduleModal">
        <Plus class="w-3.5 h-3.5 mr-1" /> Lập Lịch Chuyến Ngay
      </Button>
    </div>

    <!-- Trips Table -->
    <div v-else class="rounded-xl border border-border bg-card shadow-sm overflow-hidden">
      <div class="p-4 border-b border-border bg-muted/20 flex items-center justify-between">
        <h2 class="text-xs font-bold text-foreground uppercase tracking-wider">
          Danh Sách Chuyến Xe Điều Độ ({{ totalElements }})
        </h2>
        <Badge variant="outline" class="text-[11px] font-mono">Trang {{ currentPage + 1 }} / {{ totalPages }}</Badge>
      </div>

      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse text-xs">
          <thead>
            <tr class="bg-muted/40 border-b border-border text-muted-foreground uppercase tracking-wider text-[11px]">
              <th class="py-3 px-4 font-semibold">Mã Chuyến</th>
              <th class="py-3 px-4 font-semibold">Tuyến Đường</th>
              <th class="py-3 px-4 font-semibold">Phương Tiện</th>
              <th class="py-3 px-4 font-semibold">Giờ Xuất Bến</th>
              <th class="py-3 px-4 font-semibold">Giờ Cập Bến</th>
              <th class="py-3 px-4 font-semibold text-right">Giá Vé Cơ Sở</th>
              <th class="py-3 px-4 font-semibold text-center">Trạng Thái</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border">
            <tr v-for="trip in trips" :key="trip.id" class="hover:bg-muted/30 transition-colors">
              <td class="py-3.5 px-4 font-mono font-bold text-primary">{{ trip.tripCode }}</td>
              <td class="py-3.5 px-4">
                <div class="flex items-center gap-1.5 font-medium text-foreground">
                  <span>{{ stationMap.get(Number(trip.departureStationId))?.name || `Bến #${trip.departureStationId}` }}</span>
                  <ArrowRight class="w-3 h-3 text-muted-foreground" />
                  <span>{{ stationMap.get(Number(trip.arrivalStationId))?.name || `Bến #${trip.arrivalStationId}` }}</span>
                </div>
              </td>
              <td class="py-3.5 px-4">
                <div class="flex items-center gap-1.5">
                  <Truck class="w-3.5 h-3.5 text-muted-foreground" />
                  <span class="font-mono font-semibold text-foreground">
                    {{ vehicleMap.get(Number(trip.vehicleId))?.plateNumber || `#${trip.vehicleId}` }}
                  </span>
                </div>
              </td>
              <td class="py-3.5 px-4 font-mono text-foreground">{{ formatDateTime(trip.departureTime) }}</td>
              <td class="py-3.5 px-4 font-mono text-muted-foreground">{{ formatDateTime(trip.arrivalTime) }}</td>
              <td class="py-3.5 px-4 text-right font-mono font-bold text-foreground">{{ formatPrice(trip.basePrice) }}</td>
              <td class="py-3.5 px-4 text-center">
                <Badge :class="formatTripStatus(trip.status).class" class="text-[10px] font-semibold border">
                  {{ formatTripStatus(trip.status).text }}
                </Badge>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="p-3 border-t border-border bg-muted/10 flex items-center justify-between text-xs text-muted-foreground">
        <span>Hiển thị {{ trips.length }} trên tổng số {{ totalElements }} chuyến</span>
        <div class="flex items-center gap-1">
          <Button
            size="sm"
            variant="outline"
            class="h-7 text-xs"
            :disabled="currentPage === 0 || isLoadingTrips"
            @click="fetchTrips(currentPage - 1)"
          >
            Trang trước
          </Button>
          <span class="px-2 font-mono font-semibold text-foreground">{{ currentPage + 1 }} / {{ totalPages }}</span>
          <Button
            size="sm"
            variant="outline"
            class="h-7 text-xs"
            :disabled="currentPage >= totalPages - 1 || isLoadingTrips"
            @click="fetchTrips(currentPage + 1)"
          >
            Trang sau
          </Button>
        </div>
      </div>
    </div>

    <!-- SCHEDULE MODAL (Dialog) -->
    <Dialog :open="isScheduleModalOpen" @update:open="isScheduleModalOpen = $event">
      <DialogContent class="sm:max-w-[560px]">
        <DialogHeader>
          <DialogTitle class="text-base font-bold text-foreground flex items-center gap-2">
            <Calendar class="w-4 h-4 text-primary" />
            <span>Lập Lịch Chuyến Xe Mới</span>
          </DialogTitle>
          <DialogDescription class="text-xs text-muted-foreground">
            Khởi tạo chuyến xe mới, gắn phương tiện và thiết lập giá vé cơ sở.
          </DialogDescription>
        </DialogHeader>

        <div v-if="tripFormError" class="p-3 bg-destructive/10 border border-destructive/20 rounded-md text-xs text-destructive">
          {{ tripFormError }}
        </div>

        <div v-if="turnaroundBufferViolation" class="p-3 bg-amber-500/10 border border-amber-500/30 rounded-md text-xs text-amber-700 flex items-start gap-2">
          <AlertTriangle class="w-4 h-4 mt-0.5 text-amber-600 shrink-0" />
          <span>{{ turnaroundBufferViolation }}</span>
        </div>

        <div class="space-y-3.5 py-2">
          <div>
            <label class="text-xs font-semibold text-foreground">Mã chuyến xe <span class="text-destructive">*</span></label>
            <Input
              v-model="tripForm.tripCode"
              placeholder="VD: SG-DL-0800"
              class="mt-1 font-mono uppercase text-xs font-bold"
              :disabled="isCreatingTrip"
            />
          </div>

          <div>
            <label class="text-xs font-semibold text-foreground">Phương tiện vận hành <span class="text-destructive">*</span></label>
            <select
              v-model="tripForm.vehicleId"
              class="mt-1 w-full h-9 px-3 rounded-md border border-input bg-background text-xs text-foreground focus:ring-1 focus:ring-primary focus:outline-none"
              :disabled="isCreatingTrip"
            >
              <option value="" disabled>-- Chọn phương tiện đang sẵn sàng --</option>
              <option
                v-for="v in activeVehicles"
                :key="v.id"
                :value="String(v.id)"
              >
                {{ v.plateNumber }} ({{ v.modelName || 'Xe' }} - {{ v.totalSeats }} chỗ)
              </option>
            </select>
            <p v-if="activeVehicles.length === 0" class="text-[11px] text-destructive mt-1">
              Không có xe nào ở trạng thái sẵn sàng (ACTIVE). Hãy thêm xe hoặc kích hoạt lại xe từ tab Đội xe.
            </p>
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="text-xs font-semibold text-foreground">Bến xuất phát (Đi) <span class="text-destructive">*</span></label>
              <select
                v-model="tripForm.departureStationId"
                class="mt-1 w-full h-9 px-3 rounded-md border border-input bg-background text-xs text-foreground focus:ring-1 focus:ring-primary focus:outline-none"
                :disabled="isCreatingTrip"
              >
                <option value="" disabled>-- Chọn bến đi --</option>
                <option v-for="s in stations" :key="s.id" :value="String(s.id)">
                  {{ s.name }} ({{ s.city }})
                </option>
              </select>
            </div>
            <div>
              <label class="text-xs font-semibold text-foreground">Bến đến <span class="text-destructive">*</span></label>
              <select
                v-model="tripForm.arrivalStationId"
                class="mt-1 w-full h-9 px-3 rounded-md border border-input bg-background text-xs text-foreground focus:ring-1 focus:ring-primary focus:outline-none"
                :disabled="isCreatingTrip"
              >
                <option value="" disabled>-- Chọn bến đến --</option>
                <option v-for="s in stations" :key="s.id" :value="String(s.id)">
                  {{ s.name }} ({{ s.city }})
                </option>
              </select>
            </div>
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="text-xs font-semibold text-foreground">Thời gian xuất bến <span class="text-destructive">*</span></label>
              <Input
                v-model="tripForm.departureTime"
                type="datetime-local"
                class="mt-1 text-xs font-mono"
                :disabled="isCreatingTrip"
              />
            </div>
            <div>
              <label class="text-xs font-semibold text-foreground">Thời gian cập bến dự kiến <span class="text-destructive">*</span></label>
              <Input
                v-model="tripForm.arrivalTime"
                type="datetime-local"
                class="mt-1 text-xs font-mono"
                :disabled="isCreatingTrip"
              />
            </div>
          </div>

          <div>
            <label class="text-xs font-semibold text-foreground">Giá vé cơ sở (VNĐ) <span class="text-destructive">*</span></label>
            <Input
              v-model.number="tripForm.basePrice"
              type="number"
              min="10000"
              step="10000"
              class="mt-1 text-xs font-mono font-bold"
              :disabled="isCreatingTrip"
            />
            <p class="text-[11px] text-muted-foreground mt-1">
              Giá hiển thị: <strong class="text-foreground">{{ formatPrice(tripForm.basePrice) }}</strong>
            </p>
          </div>
        </div>

        <DialogFooter class="gap-2 sm:gap-0">
          <Button variant="outline" size="sm" @click="isScheduleModalOpen = false" :disabled="isCreatingTrip" class="text-xs">
            Hủy Bỏ
          </Button>
          <Button
            size="sm"
            @click="handleScheduleTrip"
            :disabled="isCreatingTrip || !!turnaroundBufferViolation"
            class="text-xs"
          >
            <RefreshCw v-if="isCreatingTrip" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
            Lập Lịch Chuyến Xe
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>
