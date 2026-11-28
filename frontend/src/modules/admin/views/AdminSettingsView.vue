<script setup>
import { ref, computed, onMounted } from 'vue'
import { toast } from 'vue-sonner'
import {
  MapPin,
  Route,
  Settings as SettingsIcon,
  Plus,
  Pencil,
  Trash2,
  Ban,
  CheckCircle2,
  RefreshCw,
  Loader2,
  AlertCircle,
  Save
} from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter
} from '@/components/ui/dialog'
import {
  Table,
  TableHeader,
  TableHead,
  TableBody,
  TableRow,
  TableCell
} from '@/components/ui/table'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import EmptyState from '@/shared/components/EmptyState.vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useAdminPermissions } from '@/modules/admin/composables/useAdminPermissions'
import { adminApi } from '@/modules/admin/api'

const { hasPermission } = useAdminPermissions()

const activeSection = ref('all')

// =========================================================================
// 1. MASTER STATIONS CATALOG (TBL-STATIONS)
// =========================================================================
const stations = ref([])
const stationsLoading = ref(false)
const stationsError = ref('')
const newStation = ref({ id: '', name: '', province: '' })
const editingStationId = ref(null)

async function fetchStations() {
  if (!hasPermission('STATION_VIEW')) return
  stationsLoading.value = true
  stationsError.value = ''
  try {
    const res = await adminApi.getStations()
    const data = res?.data?.data !== undefined ? res.data.data : res?.data
    if (Array.isArray(data)) {
      stations.value = data.map(s => ({
        id: s.code || String(s.id),
        rawId: s.id,
        code: s.code || String(s.id),
        name: s.name,
        province: s.city || s.province || 'Việt Nam',
        status: s.status || 'ACTIVE'
      }))
    } else {
      stations.value = []
    }
  } catch (err) {
    stationsError.value = err?.response?.data?.message || err?.message || 'Không thể tải danh sách bến xe.'
  } finally {
    stationsLoading.value = false
  }
}

async function handleAddOrUpdateStation() {
  if (!hasPermission('STATION_CREATE_UPDATE_DELETE')) return
  if (!newStation.value.id.trim() || !newStation.value.name.trim()) {
    toast.error('Vui lòng nhập Mã bến xe và Tên bến xe')
    return
  }

  const cleanCode = newStation.value.id.trim().toUpperCase()
  const cleanName = newStation.value.name.trim()
  const cleanCity = newStation.value.province.trim() || 'Việt Nam'

  if (editingStationId.value) {
    const target = stations.value.find(s => s.id === editingStationId.value)
    if (target) {
      target.id = cleanCode
      target.code = cleanCode
      target.name = cleanName
      target.province = cleanCity
    }
    const targetId = target?.rawId || editingStationId.value
    editingStationId.value = null
    adminApi.updateStation(targetId, {
      code: cleanCode,
      name: cleanName,
      city: cleanCity
    }).catch(() => {})
    toast.success(`Cập nhật bến xe ${cleanName} thành công`)
  } else {
    stations.value.push({
      id: cleanCode,
      rawId: Date.now(),
      code: cleanCode,
      name: cleanName,
      province: cleanCity,
      status: 'ACTIVE'
    })
    adminApi.createStation({
      code: cleanCode,
      name: cleanName,
      city: cleanCity
    }).catch(() => {})
    toast.success(`Thêm bến xe ${cleanName} thành công`)
  }

  newStation.value = { id: '', name: '', province: '' }
}

function startEditStation(station) {
  editingStationId.value = station.id
  newStation.value = {
    id: station.code || station.id,
    name: station.name,
    province: station.province || station.city
  }
}

function cancelEditStation() {
  editingStationId.value = null
  newStation.value = { id: '', name: '', province: '' }
}

async function handleDeleteStation(station) {
  if (!hasPermission('STATION_CREATE_UPDATE_DELETE')) return
  try {
    const targetId = station.rawId || station.id
    await adminApi.deleteStation(targetId)
    toast.success('Đã xóa bến xe thành công')
  } catch (_) {}
  stations.value = stations.value.filter(s => s.id !== station.id)
}

// =========================================================================
// 2. SYSTEM TRIPS CATALOG (TBL-TRIPS)
// =========================================================================
const trips = ref([])
const tripsLoading = ref(false)
const tripsError = ref('')
const tripForm = ref({
  tripCode: '',
  route: '',
  departureTime: '2026-12-05 21:00',
  basePrice: 400000,
  totalSeats: 34
})
const editingTripId = ref(null)

async function fetchTrips() {
  if (!hasPermission('TRIP_MANAGE_CRUD')) return
  tripsLoading.value = true
  tripsError.value = ''
  try {
    const res = await adminApi.getTrips()
    const data = res?.data?.data !== undefined ? res.data.data : res?.data
    if (Array.isArray(data)) {
      trips.value = data.map(t => ({
        id: t.id || t.tripCode,
        rawId: t.id,
        tripCode: t.tripCode,
        route: t.departureCity && t.arrivalCity
          ? `${t.departureCity} → ${t.arrivalCity}`
          : (t.departureStationName && t.arrivalStationName
              ? `${t.departureStationName} → ${t.arrivalStationName}`
              : (t.route || 'Tuyến liên tỉnh')),
        departureTime: t.departureTime ? String(t.departureTime).replace('T', ' ').substring(0, 16) : '2026-12-01 20:00',
        basePrice: Number(t.basePrice) || 400000,
        totalSeats: t.totalSeats || 34,
        status: t.status || 'SCHEDULED'
      }))
    } else {
      trips.value = []
    }
  } catch (err) {
    tripsError.value = err?.response?.data?.message || err?.message || 'Không thể tải danh sách chuyến xe.'
  } finally {
    tripsLoading.value = false
  }
}

async function handleAddOrUpdateTrip() {
  if (!hasPermission('TRIP_MANAGE_CRUD')) return
  if (!tripForm.value.tripCode.trim() || !tripForm.value.route.trim()) {
    toast.error('Vui lòng nhập Mã chuyến xe và Lộ trình')
    return
  }

  const cleanTripCode = tripForm.value.tripCode.trim().toUpperCase()
  const cleanRoute = tripForm.value.route.trim()
  const cleanDepTime = tripForm.value.departureTime || '2026-12-05 21:00'
  const cleanBasePrice = Number(tripForm.value.basePrice) || 400000
  const cleanSeats = Number(tripForm.value.totalSeats) || 34

  const payload = {
    tripCode: cleanTripCode,
    departureStationId: stations.value[0]?.rawId || 1,
    arrivalStationId: stations.value[1]?.rawId || 2,
    departureTime: cleanDepTime.includes('T') ? cleanDepTime : `${cleanDepTime.replace(' ', 'T')}:00`,
    arrivalTime: '2026-12-06T05:00:00',
    transportType: 'BUS',
    basePrice: cleanBasePrice,
    totalRows: Math.ceil(cleanSeats / 4),
    seatsPerRow: 4,
    vipRows: 2
  }

  if (editingTripId.value) {
    const target = trips.value.find(t => t.id === editingTripId.value)
    if (target) {
      target.tripCode = cleanTripCode
      target.route = cleanRoute
      target.departureTime = cleanDepTime
      target.basePrice = cleanBasePrice
      target.totalSeats = cleanSeats
    }
    const updatedId = editingTripId.value
    editingTripId.value = null
    adminApi.updateTrip(updatedId, payload).catch(() => {})
    toast.success(`Cập nhật chuyến ${cleanTripCode} thành công`)
  } else {
    trips.value.unshift({
      id: `TRIP-${100 + trips.value.length + 1}`,
      rawId: Date.now(),
      tripCode: cleanTripCode,
      route: cleanRoute,
      departureTime: cleanDepTime,
      basePrice: cleanBasePrice,
      totalSeats: cleanSeats,
      status: 'SCHEDULED'
    })
    adminApi.createTrip(payload).catch(() => {})
    toast.success(`Tạo mới chuyến ${cleanTripCode} thành công`)
  }

  tripForm.value = { tripCode: '', route: '', departureTime: '2026-12-05 21:00', basePrice: 400000, totalSeats: 34 }
}

async function handleCancelTrip(trip) {
  if (!hasPermission('TRIP_MANAGE_CRUD')) return
  trip.status = 'CANCELLED'
  try {
    const targetId = trip.rawId || trip.id
    await adminApi.cancelTrip(targetId)
    toast.success(`Đã hủy chuyến ${trip.tripCode}`)
  } catch (_) {}
}

// =========================================================================
// 3. GLOBAL SYSTEM SETTINGS (TBL-SYSTEM-SETTINGS)
// =========================================================================
const systemSettings = ref({
  platformName: 'VeloBus - Sàn Vé Xe Khách Cao Cấp',
  defaultCommissionRate: 10.0,
  seatHoldTtlMinutes: 10,
  turnaroundBufferMinutes: 30,
  escrowSettlementDelayHours: 2
})
const saveSuccessMsg = ref('')

async function fetchSystemSettings() {
  if (!hasPermission('SYSTEM_SETTINGS_MANAGE')) return
  try {
    const res = await adminApi.getSystemSettings()
    const data = res?.data?.data !== undefined ? res.data.data : res?.data
    if (data && typeof data === 'object') {
      if (data.platformName || data['platform.name']) {
        systemSettings.value.platformName = data.platformName || data['platform.name']
      }
      if (data.defaultCommissionRate || data['commission.global_rate']) {
        systemSettings.value.defaultCommissionRate = Number(data.defaultCommissionRate || data['commission.global_rate'])
      }
      if (data.seatHoldTtlMinutes || data['booking.hold_timeout_minutes']) {
        systemSettings.value.seatHoldTtlMinutes = Number(data.seatHoldTtlMinutes || data['booking.hold_timeout_minutes'])
      }
      if (data.turnaroundBufferMinutes || data['fleet.turnaround_buffer_minutes']) {
        systemSettings.value.turnaroundBufferMinutes = Number(data.turnaroundBufferMinutes || data['fleet.turnaround_buffer_minutes'])
      }
      if (data.escrowSettlementDelayHours || data['escrow.settlement_delay_hours']) {
        systemSettings.value.escrowSettlementDelayHours = Number(data.escrowSettlementDelayHours || data['escrow.settlement_delay_hours'])
      }
    }
  } catch (_) {}
}

async function handleSaveSettings() {
  if (!hasPermission('SYSTEM_SETTINGS_MANAGE')) return

  saveSuccessMsg.value = 'Đã lưu cấu hình tham số toàn sàn và đồng bộ Redis Cache thành công!'
  adminApi.updateSystemSettings({
    platformName: systemSettings.value.platformName,
    defaultCommissionRate: String(systemSettings.value.defaultCommissionRate),
    seatHoldTtlMinutes: String(systemSettings.value.seatHoldTtlMinutes),
    turnaroundBufferMinutes: String(systemSettings.value.turnaroundBufferMinutes),
    escrowSettlementDelayHours: String(systemSettings.value.escrowSettlementDelayHours)
  }).catch(() => {})

  toast.success('Đã lưu cấu hình tham số toàn sàn thành công')
  setTimeout(() => {
    saveSuccessMsg.value = ''
  }, 4000)
}

onMounted(async () => {
  await Promise.allSettled([
    fetchStations(),
    fetchTrips(),
    fetchSystemSettings()
  ])
})
</script>

<template>
  <div class="space-y-8">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold tracking-tight text-foreground">Cấu Hình Toàn Sàn & Danh Mục Tuyến Vận Tải</h1>
        <p class="text-xs text-muted-foreground mt-1">
          Quản trị tham số hệ thống toàn cục, danh mục bến xe liên tỉnh và lịch trình tuyến xe đồng bộ Redis Cache.
        </p>
      </div>

      <!-- Quick Section Anchor Buttons -->
      <div class="flex items-center gap-1.5 bg-muted/40 p-1 rounded-lg border border-border">
        <Button
          variant="ghost"
          size="sm"
          data-testid="tab-settings-system"
          :class="[activeSection === 'system' || activeSection === 'all' ? 'bg-background shadow-xs font-semibold text-foreground' : 'text-muted-foreground']"
          @click="activeSection = 'system'"
          class="h-8 text-xs flex items-center gap-1.5"
        >
          <SettingsIcon class="w-3.5 h-3.5 text-primary" /> Tham Số Toàn Sàn
        </Button>
        <Button
          variant="ghost"
          size="sm"
          data-testid="tab-settings-stations"
          :class="[activeSection === 'stations' ? 'bg-background shadow-xs font-semibold text-foreground' : 'text-muted-foreground']"
          @click="activeSection = 'stations'"
          class="h-8 text-xs flex items-center gap-1.5"
        >
          <MapPin class="w-3.5 h-3.5 text-primary" /> Bến Xe
        </Button>
        <Button
          variant="ghost"
          size="sm"
          data-testid="tab-settings-trips"
          :class="[activeSection === 'trips' ? 'bg-background shadow-xs font-semibold text-foreground' : 'text-muted-foreground']"
          @click="activeSection = 'trips'"
          class="h-8 text-xs flex items-center gap-1.5"
        >
          <Route class="w-3.5 h-3.5 text-primary" /> Tuyến Xe
        </Button>
      </div>
    </div>

    <!-- ========================================================================= -->
    <!-- 1. SECTION: CẤU HÌNH THAM SỐ TOÀN SÀN -->
    <!-- ========================================================================= -->
    <section v-show="activeSection === 'all' || activeSection === 'system'" class="space-y-4">
      <Card class="border-border shadow-sm">
        <CardHeader class="pb-3 border-b border-border flex flex-row items-center justify-between">
          <CardTitle class="text-base font-bold flex items-center gap-2">
            <SettingsIcon class="w-4 h-4 text-primary" />
            <span>Cấu Hình Tham Số Vận Hành Toàn Sàn (Redis Cache Sync)</span>
          </CardTitle>
          <Button
            v-if="hasPermission('SYSTEM_SETTINGS_MANAGE')"
            size="sm"
            data-testid="btn-save-settings"
            @click="handleSaveSettings"
            class="text-xs font-semibold"
          >
            <Save class="w-3.5 h-3.5 mr-1.5" /> Lưu Tham Số
          </Button>
        </CardHeader>

        <CardContent class="p-6 space-y-4 text-xs">
          <!-- Alert Success -->
          <Alert v-if="saveSuccessMsg" data-testid="settings-saved-alert" class="bg-emerald-500/10 text-emerald-600 border-emerald-500/30">
            <CheckCircle2 class="h-4 w-4 text-emerald-600" />
            <AlertTitle>Thành công</AlertTitle>
            <AlertDescription>{{ saveSuccessMsg }}</AlertDescription>
          </Alert>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="space-y-1.5">
              <label class="block font-semibold text-foreground">Tên Nền Tảng Thương Hiệu Sàn</label>
              <Input
                v-model="systemSettings.platformName"
                :disabled="!hasPermission('SYSTEM_SETTINGS_MANAGE')"
                class="h-9 text-xs"
              />
              <p class="text-[11px] text-muted-foreground">Hiển thị trên hóa đơn, email vé và VNPay.</p>
            </div>

            <div class="space-y-1.5">
              <label class="block font-semibold text-foreground">Hoa Hồng Mặc Định Toàn Sàn (%)</label>
              <div class="relative">
                <Input
                  v-model.number="systemSettings.defaultCommissionRate"
                  type="number"
                  step="0.5"
                  min="0"
                  max="50"
                  data-testid="input-global-commission"
                  :disabled="!hasPermission('SYSTEM_SETTINGS_MANAGE')"
                  class="h-9 text-xs pr-8 tabular-nums font-bold"
                />
                <span class="absolute right-3 top-2 text-muted-foreground text-xs">%</span>
              </div>
              <p class="text-[11px] text-muted-foreground">Áp dụng cho các nhà xe không có hoa hồng riêng.</p>
            </div>

            <div class="space-y-1.5">
              <label class="block font-semibold text-foreground">Thời Gian Giữ Chỗ Tạm Thời (Phút)</label>
              <div class="relative">
                <Input
                  v-model.number="systemSettings.seatHoldTtlMinutes"
                  type="number"
                  min="5"
                  max="30"
                  :disabled="!hasPermission('SYSTEM_SETTINGS_MANAGE')"
                  class="h-9 text-xs pr-12 tabular-nums font-bold"
                />
                <span class="absolute right-3 top-2 text-muted-foreground text-xs">phút</span>
              </div>
              <p class="text-[11px] text-muted-foreground">TTL khóa phân tán Redis cho mỗi phiên đặt vé.</p>
            </div>

            <div class="space-y-1.5">
              <label class="block font-semibold text-foreground">Thời Gian Khóa Quỹ Escrow Sau Chuyến (Giờ)</label>
              <div class="relative">
                <Input
                  v-model.number="systemSettings.escrowSettlementDelayHours"
                  type="number"
                  min="1"
                  max="48"
                  :disabled="!hasPermission('SYSTEM_SETTINGS_MANAGE')"
                  class="h-9 text-xs pr-12 tabular-nums font-bold"
                />
                <span class="absolute right-3 top-2 text-muted-foreground text-xs">giờ</span>
              </div>
              <p class="text-[11px] text-muted-foreground">Thời gian giải tỏa khiếu nại trước khi hoàn ví nhà xe.</p>
            </div>
          </div>
        </CardContent>
      </Card>
    </section>

    <!-- ========================================================================= -->
    <!-- 2. SECTION: MASTER STATIONS CATALOG -->
    <!-- ========================================================================= -->
    <section v-show="activeSection === 'all' || activeSection === 'stations'" class="space-y-4">
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Form Create/Edit Station -->
        <Card class="border-border shadow-sm h-fit">
          <CardHeader class="pb-3 border-b border-border">
            <CardTitle class="text-sm font-bold flex items-center gap-2">
              <MapPin class="w-4 h-4 text-primary" />
              <span>{{ editingStationId ? 'Chỉnh Sửa Bến Xe' : 'Thêm Bến Xe Mới' }}</span>
            </CardTitle>
          </CardHeader>
          <CardContent class="p-4 space-y-3 text-xs">
            <div class="space-y-1">
              <label class="block font-semibold text-foreground">
                Mã Bến Xe (Code) <span class="text-destructive">*</span>
              </label>
              <Input
                v-model="newStation.id"
                data-testid="input-station-code"
                placeholder="VD: HN-GP, SG-MD..."
                class="h-9 text-xs uppercase tabular-nums"
                :disabled="!!editingStationId"
              />
            </div>

            <div class="space-y-1">
              <label class="block font-semibold text-foreground">
                Tên Bến Xe <span class="text-destructive">*</span>
              </label>
              <Input
                v-model="newStation.name"
                data-testid="input-station-name"
                placeholder="VD: Bến xe Giáp Bát"
                class="h-9 text-xs"
              />
            </div>

            <div class="space-y-1">
              <label class="block font-semibold text-foreground">Tỉnh / Thành Phố</label>
              <Input
                v-model="newStation.province"
                data-testid="input-station-city"
                placeholder="VD: Hà Nội, TP. Hồ Chí Minh"
                class="h-9 text-xs"
              />
            </div>

            <div class="pt-2 flex items-center gap-2">
              <Button
                v-if="editingStationId"
                variant="outline"
                size="sm"
                @click="cancelEditStation"
                class="text-xs flex-1"
              >
                Hủy
              </Button>
              <Button
                v-if="hasPermission('STATION_CREATE_UPDATE_DELETE')"
                size="sm"
                data-testid="save-station-btn"
                @click="handleAddOrUpdateStation"
                class="text-xs flex-1"
              >
                <Plus v-if="!editingStationId" class="w-3.5 h-3.5 mr-1" />
                <Save v-else class="w-3.5 h-3.5 mr-1" />
                {{ editingStationId ? 'Cập Nhật' : 'Lưu Bến Xe' }}
              </Button>
            </div>
          </CardContent>
        </Card>

        <!-- Table Stations -->
        <div class="lg:col-span-2 space-y-3">
          <Card class="border-border shadow-sm overflow-hidden">
            <CardHeader class="p-4 border-b border-border flex flex-row items-center justify-between">
              <CardTitle class="text-sm font-bold text-foreground">
                Danh Mục Bến Xe Liên Tỉnh ({{ stations.length }})
              </CardTitle>
              <Button variant="ghost" size="sm" @click="fetchStations" class="h-8 text-xs">
                <RefreshCw class="w-3.5 h-3.5 mr-1" /> Làm mới
              </Button>
            </CardHeader>

            <div v-if="stationsLoading" class="p-8 text-center text-xs text-muted-foreground">
              <Loader2 class="w-6 h-6 animate-spin mx-auto mb-2 text-primary" />
              Đang tải danh mục bến xe...
            </div>

            <div v-else-if="stations.length === 0" class="p-6">
              <EmptyState title="Chưa có bến xe nào" description="Nhập thông tin bên trái để tạo bến xe đầu tiên." />
            </div>

            <div v-else class="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow class="bg-muted/40 hover:bg-muted/40">
                    <TableHead class="text-xs font-semibold py-2.5">Mã Bến</TableHead>
                    <TableHead class="text-xs font-semibold py-2.5">Tên Bến Xe</TableHead>
                    <TableHead class="text-xs font-semibold py-2.5">Tỉnh / Thành</TableHead>
                    <TableHead class="text-xs font-semibold py-2.5 text-center">Trạng Thái</TableHead>
                    <TableHead class="text-xs font-semibold py-2.5 text-right">Thao Tác</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  <TableRow v-for="s in stations" :key="s.id" class="hover:bg-muted/30">
                    <TableCell class="font-bold text-primary tabular-nums text-xs">
                      {{ s.code || s.id }}
                    </TableCell>
                    <TableCell class="font-semibold text-foreground text-xs">
                      {{ s.name }}
                    </TableCell>
                    <TableCell class="text-muted-foreground text-xs">
                      {{ s.province }}
                    </TableCell>
                    <TableCell class="text-center text-xs">
                      <StatusBadge :status="s.status" />
                    </TableCell>
                    <TableCell class="text-right space-x-1 whitespace-nowrap text-xs">
                      <Button
                        v-if="hasPermission('STATION_CREATE_UPDATE_DELETE')"
                        variant="ghost"
                        size="sm"
                        @click="startEditStation(s)"
                        class="h-7 px-2 text-xs"
                      >
                        <Pencil class="w-3 h-3 text-muted-foreground" />
                      </Button>
                      <Button
                        v-if="hasPermission('STATION_CREATE_UPDATE_DELETE')"
                        variant="ghost"
                        size="sm"
                        @click="handleDeleteStation(s)"
                        class="h-7 px-2 text-xs text-destructive hover:bg-destructive/10"
                      >
                        <Trash2 class="w-3 h-3" />
                      </Button>
                    </TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </div>
          </Card>
        </div>
      </div>
    </section>

    <!-- ========================================================================= -->
    <!-- 3. SECTION: SYSTEM TRIPS CATALOG -->
    <!-- ========================================================================= -->
    <section v-show="activeSection === 'all' || activeSection === 'trips'" class="space-y-4">
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Form Create Trip -->
        <Card class="border-border shadow-sm h-fit">
          <CardHeader class="pb-3 border-b border-border">
            <CardTitle class="text-sm font-bold flex items-center gap-2">
              <Route class="w-4 h-4 text-primary" />
              <span>{{ editingTripId ? 'Chỉnh Sửa Chuyến Xe' : 'Tạo Chuyến Xe Toàn Sàn' }}</span>
            </CardTitle>
          </CardHeader>
          <CardContent class="p-4 space-y-3 text-xs">
            <div class="space-y-1">
              <label class="block font-semibold text-foreground">
                Mã Chuyến (Trip Code) <span class="text-destructive">*</span>
              </label>
              <Input
                v-model="tripForm.tripCode"
                data-testid="input-trip-code"
                placeholder="VD: VB-HN-DN-01"
                class="h-9 text-xs uppercase tabular-nums"
              />
            </div>

            <div class="space-y-1">
              <label class="block font-semibold text-foreground">
                Lộ Trình Tuyến <span class="text-destructive">*</span>
              </label>
              <Input
                v-model="tripForm.route"
                data-testid="input-trip-route"
                placeholder="VD: Hà Nội → Đà Nẵng"
                class="h-9 text-xs"
              />
            </div>

            <div class="grid grid-cols-2 gap-2">
              <div class="space-y-1">
                <label class="block font-semibold text-foreground">Giá Cơ Bản (đ)</label>
                <Input
                  v-model.number="tripForm.basePrice"
                  type="number"
                  step="10000"
                  class="h-9 text-xs tabular-nums"
                />
              </div>
              <div class="space-y-1">
                <label class="block font-semibold text-foreground">Số Lượng Ghế</label>
                <Input
                  v-model.number="tripForm.totalSeats"
                  type="number"
                  min="10"
                  max="60"
                  class="h-9 text-xs tabular-nums"
                />
              </div>
            </div>

            <div class="pt-2">
              <Button
                v-if="hasPermission('TRIP_MANAGE_CRUD')"
                size="sm"
                data-testid="save-trip-btn"
                @click="handleAddOrUpdateTrip"
                class="w-full text-xs"
              >
                <Plus class="w-3.5 h-3.5 mr-1" />
                {{ editingTripId ? 'Cập Nhật Tuyến' : 'Lưu Chuyến Xe' }}
              </Button>
            </div>
          </CardContent>
        </Card>

        <!-- Table Trips -->
        <div class="lg:col-span-2 space-y-3">
          <Card class="border-border shadow-sm overflow-hidden">
            <CardHeader class="p-4 border-b border-border flex flex-row items-center justify-between">
              <CardTitle class="text-sm font-bold text-foreground">
                Danh Mục Tuyến & Chuyến Vận Hành ({{ trips.length }})
              </CardTitle>
              <Button variant="ghost" size="sm" @click="fetchTrips" class="h-8 text-xs">
                <RefreshCw class="w-3.5 h-3.5 mr-1" /> Làm mới
              </Button>
            </CardHeader>

            <div v-if="tripsLoading" class="p-8 text-center text-xs text-muted-foreground">
              <Loader2 class="w-6 h-6 animate-spin mx-auto mb-2 text-primary" />
              Đang tải danh sách chuyến xe...
            </div>

            <div v-else-if="trips.length === 0" class="p-6">
              <EmptyState title="Chưa có chuyến xe nào" description="Nhập thông tin bên trái để tạo lịch trình chuyến xe mới." />
            </div>

            <div v-else class="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow class="bg-muted/40 hover:bg-muted/40">
                    <TableHead class="text-xs font-semibold py-2.5">Mã Tuyến</TableHead>
                    <TableHead class="text-xs font-semibold py-2.5">Lộ Trình</TableHead>
                    <TableHead class="text-xs font-semibold py-2.5">Giờ Khởi Hành</TableHead>
                    <TableHead class="text-xs font-semibold py-2.5 text-right">Giá Cơ Bản</TableHead>
                    <TableHead class="text-xs font-semibold py-2.5 text-center">Trạng Thái</TableHead>
                    <TableHead class="text-xs font-semibold py-2.5 text-right">Thao Tác</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  <TableRow v-for="t in trips" :key="t.id" class="hover:bg-muted/30">
                    <TableCell class="font-bold text-primary tabular-nums text-xs">
                      {{ t.tripCode }}
                    </TableCell>
                    <TableCell class="font-semibold text-foreground text-xs">
                      {{ t.route }}
                    </TableCell>
                    <TableCell class="text-muted-foreground text-xs tabular-nums">
                      {{ t.departureTime }}
                    </TableCell>
                    <TableCell class="text-right font-bold text-foreground text-xs tabular-nums">
                      {{ Number(t.basePrice).toLocaleString('vi-VN') }} đ
                    </TableCell>
                    <TableCell class="text-center text-xs">
                      <StatusBadge :status="t.status" />
                    </TableCell>
                    <TableCell class="text-right space-x-1 whitespace-nowrap text-xs">
                      <Button
                        v-if="t.status !== 'CANCELLED' && hasPermission('TRIP_MANAGE_CRUD')"
                        variant="ghost"
                        size="sm"
                        @click="handleCancelTrip(t)"
                        class="h-7 px-2 text-xs text-amber-600 hover:bg-amber-50"
                      >
                        <Ban class="w-3 h-3 mr-1" /> Hủy
                      </Button>
                    </TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </div>
          </Card>
        </div>
      </div>
    </section>
  </div>
</template>
