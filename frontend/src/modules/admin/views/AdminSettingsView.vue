<script setup>
import { ref, computed, watch, onMounted } from 'vue'
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
  AlertTriangle,
  Save,
  Info,
  Search,
  ChevronLeft,
  ChevronRight,
  X
} from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogCancel,
  AlertDialogAction
} from '@/components/ui/alert-dialog'
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
const isSubmittingStation = ref(false)
const stationToDelete = ref(null)

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
      throw new Error('Dữ liệu danh mục bến xe không đúng định dạng.')
    }
  } catch (err) {
    stationsError.value = err?.response?.data?.message || err?.message || 'Không thể tải danh sách bến xe.'
    stations.value = []
  } finally {
    stationsLoading.value = false
  }
}

// =========================================================================
// 1.1 STATIONS SEARCH, FILTER & PAGINATION
// =========================================================================
const stationSearchQuery = ref('')
const stationProvinceFilter = ref('ALL')
const stationStatusFilter = ref('ALL')
const stationCurrentPage = ref(1)
const stationPageSize = ref(10)

const uniqueStationProvinces = computed(() => {
  const set = new Set(stations.value.map(s => s.province).filter(Boolean))
  return Array.from(set).sort((a, b) => a.localeCompare(b, 'vi'))
})

const filteredStations = computed(() => {
  let list = stations.value
  const query = stationSearchQuery.value.trim().toLowerCase()
  if (query) {
    list = list.filter(s =>
      (s.name && s.name.toLowerCase().includes(query)) ||
      (s.code && s.code.toLowerCase().includes(query)) ||
      (s.province && s.province.toLowerCase().includes(query))
    )
  }
  if (stationProvinceFilter.value !== 'ALL') {
    list = list.filter(s => s.province === stationProvinceFilter.value)
  }
  if (stationStatusFilter.value !== 'ALL') {
    list = list.filter(s => s.status === stationStatusFilter.value)
  }
  return list
})

watch([stationSearchQuery, stationProvinceFilter, stationStatusFilter, stationPageSize], () => {
  stationCurrentPage.value = 1
})

const stationTotalPages = computed(() => {
  return Math.max(1, Math.ceil(filteredStations.value.length / stationPageSize.value))
})

const paginatedStations = computed(() => {
  const start = (stationCurrentPage.value - 1) * stationPageSize.value
  return filteredStations.value.slice(start, start + stationPageSize.value)
})

async function handleAddOrUpdateStation() {
  if (!hasPermission('STATION_CREATE_UPDATE_DELETE')) {
    toast.error('Bạn không có quyền quản lý bến xe (Yêu cầu STATION_CREATE_UPDATE_DELETE).')
    return
  }
  if (!newStation.value.id.trim() || !newStation.value.name.trim()) {
    toast.error('Vui lòng nhập Mã bến xe và Tên bến xe')
    return
  }

  const cleanCode = newStation.value.id.trim().toUpperCase()
  const cleanName = newStation.value.name.trim()
  const cleanCity = newStation.value.province.trim() || 'Việt Nam'

  isSubmittingStation.value = true
  try {
    if (editingStationId.value) {
      const target = stations.value.find(s => s.id === editingStationId.value)
      const targetId = target?.rawId || editingStationId.value
      await adminApi.updateStation(targetId, {
        code: cleanCode,
        name: cleanName,
        city: cleanCity
      })
      toast.success(`Cập nhật bến xe ${cleanName} thành công`)
      editingStationId.value = null
    } else {
      await adminApi.createStation({
        code: cleanCode,
        name: cleanName,
        city: cleanCity
      })
      toast.success(`Thêm bến xe ${cleanName} thành công`)
    }
    newStation.value = { id: '', name: '', province: '' }
    await fetchStations()
  } catch (err) {
    const msg = err?.response?.data?.message || err?.message || 'Có lỗi xảy ra khi lưu bến xe.'
    toast.error(msg)
    // Preserve form values on failure for user correction
  } finally {
    isSubmittingStation.value = false
  }
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

function confirmDeleteStation(station) {
  stationToDelete.value = station
}

async function executeDeleteStation() {
  if (!stationToDelete.value || !hasPermission('STATION_CREATE_UPDATE_DELETE')) return
  const target = stationToDelete.value
  isSubmittingStation.value = true
  try {
    const targetId = target.rawId || target.id
    await adminApi.deleteStation(targetId)
    toast.success(`Đã xóa bến xe ${target.name} thành công`)
    stationToDelete.value = null
    await fetchStations()
  } catch (err) {
    const msg = err?.response?.data?.message || err?.message || 'Không thể xóa bến xe do ràng buộc dữ liệu chuyến.'
    toast.error(msg)
  } finally {
    isSubmittingStation.value = false
  }
}

// =========================================================================
// 2. SYSTEM TRIPS CATALOG (TBL-TRIPS) - CreateTripCommand compliant
// =========================================================================
const trips = ref([])
const tripsLoading = ref(false)
const tripsError = ref('')
const tripForm = ref({
  tripCode: '',
  departureStationId: '',
  arrivalStationId: '',
  departureTime: '',
  arrivalTime: '',
  transportType: 'BUS',
  basePrice: 400000,
  totalRows: 9,
  seatsPerRow: 4,
  vipRows: 2
})
const isSubmittingTrip = ref(false)
const tripToCancel = ref(null)

const calculatedSeats = computed(() => {
  const rows = Number(tripForm.value.totalRows) || 0
  const perRow = Number(tripForm.value.seatsPerRow) || 0
  return rows * perRow
})

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
        departureTime: t.departureTime ? String(t.departureTime).replace('T', ' ').substring(0, 16) : '---',
        arrivalTime: t.arrivalTime ? String(t.arrivalTime).replace('T', ' ').substring(0, 16) : '---',
        transportType: t.transportType || 'BUS',
        basePrice: Number(t.basePrice) || 0,
        totalSeats: t.totalSeats || 0,
        status: t.status || 'SCHEDULED'
      }))
    } else {
      throw new Error('Dữ liệu danh sách chuyến xe không đúng định dạng.')
    }
  } catch (err) {
    tripsError.value = err?.response?.data?.message || err?.message || 'Không thể tải danh sách chuyến xe.'
    trips.value = []
  } finally {
    tripsLoading.value = false
  }
}

// =========================================================================
// 2.1 TRIPS SEARCH, FILTER & PAGINATION
// =========================================================================
const tripSearchQuery = ref('')
const tripStatusFilter = ref('ALL')
const tripCurrentPage = ref(1)
const tripPageSize = ref(10)

const filteredTrips = computed(() => {
  let list = trips.value
  const query = tripSearchQuery.value.trim().toLowerCase()
  if (query) {
    list = list.filter(t =>
      (t.tripCode && t.tripCode.toLowerCase().includes(query)) ||
      (t.route && t.route.toLowerCase().includes(query)) ||
      (t.transportType && t.transportType.toLowerCase().includes(query))
    )
  }
  if (tripStatusFilter.value !== 'ALL') {
    list = list.filter(t => t.status === tripStatusFilter.value)
  }
  return list
})

watch([tripSearchQuery, tripStatusFilter, tripPageSize], () => {
  tripCurrentPage.value = 1
})

const tripTotalPages = computed(() => {
  return Math.max(1, Math.ceil(filteredTrips.value.length / tripPageSize.value))
})

const paginatedTrips = computed(() => {
  const start = (tripCurrentPage.value - 1) * tripPageSize.value
  return filteredTrips.value.slice(start, start + tripPageSize.value)
})

async function handleAddTrip() {
  if (!hasPermission('TRIP_MANAGE_CRUD')) {
    toast.error('Bạn không có quyền quản lý chuyến xe (Yêu cầu TRIP_MANAGE_CRUD).')
    return
  }

  const cleanTripCode = tripForm.value.tripCode.trim().toUpperCase()
  if (!cleanTripCode || cleanTripCode.length < 3 || cleanTripCode.length > 20) {
    toast.error('Mã chuyến xe phải từ 3 đến 20 ký tự (chữ cái, số hoặc gạch ngang).')
    return
  }

  if (!tripForm.value.departureStationId || !tripForm.value.arrivalStationId) {
    toast.error('Vui lòng chọn bến đi và bến đến.')
    return
  }

  if (Number(tripForm.value.departureStationId) === Number(tripForm.value.arrivalStationId)) {
    toast.error('Bến đi và bến đến không được trùng nhau.')
    return
  }

  if (!tripForm.value.departureTime || !tripForm.value.arrivalTime) {
    toast.error('Vui lòng chọn thời gian khởi hành và thời gian đến nơi.')
    return
  }

  const depMs = new Date(tripForm.value.departureTime).getTime()
  const arrMs = new Date(tripForm.value.arrivalTime).getTime()
  if (isNaN(depMs) || isNaN(arrMs) || arrMs <= depMs) {
    toast.error('Thời gian đến nơi phải sau thời gian khởi hành.')
    return
  }

  const price = Number(tripForm.value.basePrice)
  if (!price || price <= 0) {
    toast.error('Giá vé cơ bản phải lớn hơn 0.')
    return
  }

  const totalRows = Number(tripForm.value.totalRows)
  const seatsPerRow = Number(tripForm.value.seatsPerRow)
  const vipRows = Number(tripForm.value.vipRows)

  if (totalRows < 1 || totalRows > 60 || seatsPerRow < 1 || seatsPerRow > 10 || vipRows < 0 || vipRows > totalRows) {
    toast.error('Cấu hình số hàng ghế không hợp lệ (Tổng hàng: 1-60, Ghế/hàng: 1-10, Hàng VIP <= Tổng hàng).')
    return
  }

  // Format to standard ISO-8601 without timezone offset for backend LocalDateTime
  const formatIso = (val) => {
    return val.length === 16 ? `${val}:00` : val
  }

  const payload = {
    tripCode: cleanTripCode,
    departureStationId: Number(tripForm.value.departureStationId),
    arrivalStationId: Number(tripForm.value.arrivalStationId),
    departureTime: formatIso(tripForm.value.departureTime),
    arrivalTime: formatIso(tripForm.value.arrivalTime),
    transportType: tripForm.value.transportType,
    basePrice: price,
    totalRows,
    seatsPerRow,
    vipRows
  }

  isSubmittingTrip.value = true
  try {
    await adminApi.createTrip(payload)
    toast.success(`Tạo mới chuyến xe ${cleanTripCode} thành công!`)
    tripForm.value = {
      tripCode: '',
      departureStationId: '',
      arrivalStationId: '',
      departureTime: '',
      arrivalTime: '',
      transportType: 'BUS',
      basePrice: 400000,
      totalRows: 9,
      seatsPerRow: 4,
      vipRows: 2
    }
    await fetchTrips()
  } catch (err) {
    const msg = err?.response?.data?.message || err?.message || 'Có lỗi xảy ra khi tạo chuyến xe.'
    toast.error(msg)
  } finally {
    isSubmittingTrip.value = false
  }
}

function confirmCancelTrip(trip) {
  tripToCancel.value = trip
}

async function executeCancelTrip() {
  if (!tripToCancel.value || !hasPermission('TRIP_MANAGE_CRUD')) return
  const trip = tripToCancel.value
  isSubmittingTrip.value = true
  try {
    const targetId = trip.rawId || trip.id
    await adminApi.cancelTrip(targetId)
    toast.success(`Đã hủy lịch trình chuyến xe ${trip.tripCode}`)
    tripToCancel.value = null
    await fetchTrips()
  } catch (err) {
    const msg = err?.response?.data?.message || err?.message || 'Không thể hủy chuyến xe do ràng buộc vé đã bán.'
    toast.error(msg)
  } finally {
    isSubmittingTrip.value = false
  }
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
const settingsLoading = ref(false)
const settingsError = ref('')
const isSavingSettings = ref(false)
const saveSuccessMsg = ref('')
const isConfirmSaveOpen = ref(false)

async function fetchSystemSettings() {
  if (!hasPermission('SYSTEM_SETTINGS_MANAGE')) return
  settingsLoading.value = true
  settingsError.value = ''
  try {
    const res = await adminApi.getSystemSettings()
    const data = res?.data?.data !== undefined ? res.data.data : res?.data
    if (data && typeof data === 'object') {
      if (data.platformName || data['platform.name']) {
        systemSettings.value.platformName = data.platformName || data['platform.name']
      }
      if (data.defaultCommissionRate !== undefined || data['commission.global_rate'] !== undefined) {
        systemSettings.value.defaultCommissionRate = Number(data.defaultCommissionRate || data['commission.global_rate'])
      }
      if (data.seatHoldTtlMinutes !== undefined || data['booking.hold_timeout_minutes'] !== undefined) {
        systemSettings.value.seatHoldTtlMinutes = Number(data.seatHoldTtlMinutes || data['booking.hold_timeout_minutes'])
      }
      if (data.turnaroundBufferMinutes !== undefined || data['fleet.turnaround_buffer_minutes'] !== undefined) {
        systemSettings.value.turnaroundBufferMinutes = Number(data.turnaroundBufferMinutes || data['fleet.turnaround_buffer_minutes'])
      }
      if (data.escrowSettlementDelayHours !== undefined || data['escrow.settlement_delay_hours'] !== undefined) {
        systemSettings.value.escrowSettlementDelayHours = Number(data.escrowSettlementDelayHours || data['escrow.settlement_delay_hours'])
      }
    }
  } catch (err) {
    settingsError.value = err?.response?.data?.message || err?.message || 'Không thể tải cấu hình tham số hệ thống.'
  } finally {
    settingsLoading.value = false
  }
}

function handleOpenConfirmSave() {
  if (!hasPermission('SYSTEM_SETTINGS_MANAGE')) {
    toast.error('Bạn không có quyền quản trị tham số hệ thống (Yêu cầu SYSTEM_SETTINGS_MANAGE).')
    return
  }
  isConfirmSaveOpen.value = true
}

async function executeSaveSettings() {
  if (!hasPermission('SYSTEM_SETTINGS_MANAGE')) return
  isSavingSettings.value = true
  saveSuccessMsg.value = ''
  isConfirmSaveOpen.value = false

  try {
    await adminApi.updateSystemSettings({
      platformName: systemSettings.value.platformName,
      defaultCommissionRate: String(systemSettings.value.defaultCommissionRate),
      seatHoldTtlMinutes: String(systemSettings.value.seatHoldTtlMinutes),
      turnaroundBufferMinutes: String(systemSettings.value.turnaroundBufferMinutes),
      escrowSettlementDelayHours: String(systemSettings.value.escrowSettlementDelayHours)
    })

    saveSuccessMsg.value = 'Đã lưu cấu hình tham số toàn sàn vào cơ sở dữ liệu thành công!'
    toast.success('Đã lưu cấu hình tham số toàn sàn thành công')
    setTimeout(() => {
      saveSuccessMsg.value = ''
    }, 5000)
    await fetchSystemSettings()
  } catch (err) {
    const msg = err?.response?.data?.message || err?.message || 'Lỗi khi lưu tham số toàn sàn.'
    toast.error(msg)
  } finally {
    isSavingSettings.value = false
  }
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
          Quản trị tham số hệ thống toàn cục, danh mục bến xe liên tỉnh và lịch trình tuyến xe.
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
          <div>
            <CardTitle class="text-base font-bold flex items-center gap-2">
              <SettingsIcon class="w-4 h-4 text-primary" />
              <span>Cấu Hình Tham Số Vận Hành Toàn Sàn</span>
            </CardTitle>
            <p class="text-[11px] text-muted-foreground mt-0.5">
              Các tham số được lưu trong TBL-SYSTEM-SETTINGS và tiêu thụ theo từng dịch vụ miền chuyên biệt.
            </p>
          </div>
          <Button
            v-if="hasPermission('SYSTEM_SETTINGS_MANAGE')"
            size="sm"
            data-testid="btn-save-settings"
            :disabled="isSavingSettings || settingsLoading"
            @click="handleOpenConfirmSave"
            class="text-xs font-semibold"
          >
            <Loader2 v-if="isSavingSettings" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
            <Save v-else class="w-3.5 h-3.5 mr-1.5" />
            Lưu Tham Số
          </Button>
        </CardHeader>

        <CardContent class="p-6 space-y-4 text-xs">
          <!-- Alert Success -->
          <Alert v-if="saveSuccessMsg" data-testid="settings-saved-alert" class="bg-emerald-500/10 text-emerald-600 border-emerald-500/30">
            <CheckCircle2 class="h-4 w-4 text-emerald-600" />
            <AlertTitle>Thành công</AlertTitle>
            <AlertDescription>{{ saveSuccessMsg }}</AlertDescription>
          </Alert>

          <!-- Alert Error -->
          <Alert v-if="settingsError" variant="destructive">
            <AlertCircle class="h-4 w-4" />
            <AlertTitle>Lỗi tải tham số</AlertTitle>
            <AlertDescription>{{ settingsError }}</AlertDescription>
          </Alert>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="space-y-1.5">
              <label class="block font-semibold text-foreground">Tên Nền Tảng Thương Hiệu Sàn</label>
              <Input
                v-model="systemSettings.platformName"
                :disabled="!hasPermission('SYSTEM_SETTINGS_MANAGE') || isSavingSettings"
                class="h-9 text-xs"
              />
              <p class="text-[11px] text-muted-foreground">
                Tiêu thụ thực tế: PublicSettingsController đọc để hiển thị thông tin nền tảng trên giao diện.
              </p>
            </div>

            <div class="space-y-1.5">
              <label class="block font-semibold text-foreground">Hoa Hồng Mặc Định Toàn Sàn (%)</label>
              <div class="relative">
                <Input
                  v-model.number="systemSettings.defaultCommissionRate"
                  type="number"
                  step="0.5"
                  min="0"
                  max="100"
                  data-testid="input-global-commission"
                  :disabled="!hasPermission('SYSTEM_SETTINGS_MANAGE') || isSavingSettings"
                  class="h-9 text-xs pr-8 tabular-nums font-bold"
                />
                <span class="absolute right-3 top-2 text-muted-foreground text-xs">%</span>
              </div>
              <p class="text-[11px] text-muted-foreground">
                Tiêu thụ thực tế: PartnerQueryService.getGlobalCommissionRate() áp dụng cho nhà xe không có tỷ lệ riêng.
              </p>
            </div>

            <div class="space-y-1.5">
              <label class="block font-semibold text-foreground">
                Thời Gian Giữ Chỗ Tạm Thời (Phút)
                <Badge variant="outline" class="ml-1 text-[9px] text-amber-600 border-amber-300">Lưu DB (Static 10m in code)</Badge>
              </label>
              <div class="relative">
                <Input
                  v-model.number="systemSettings.seatHoldTtlMinutes"
                  type="number"
                  min="5"
                  max="30"
                  :disabled="!hasPermission('SYSTEM_SETTINGS_MANAGE') || isSavingSettings"
                  class="h-9 text-xs pr-12 tabular-nums font-bold"
                />
                <span class="absolute right-3 top-2 text-muted-foreground text-xs">phút</span>
              </div>
              <p class="text-[11px] text-muted-foreground">
                Lưu trữ cấu hình DB. Lưu ý: BookingCommandServiceImpl hiện cố định thời lượng giữ chỗ 10 phút qua hằng số miền.
              </p>
            </div>

            <div class="space-y-1.5">
              <label class="block font-semibold text-foreground">
                Khoảng Đệm Quay Đầu Tuyến (Phút)
                <Badge variant="outline" class="ml-1 text-[9px] text-amber-600 border-amber-300">Lưu DB (Static 30m in code)</Badge>
              </label>
              <div class="relative">
                <Input
                  v-model.number="systemSettings.turnaroundBufferMinutes"
                  type="number"
                  min="15"
                  max="120"
                  :disabled="!hasPermission('SYSTEM_SETTINGS_MANAGE') || isSavingSettings"
                  class="h-9 text-xs pr-12 tabular-nums font-bold"
                />
                <span class="absolute right-3 top-2 text-muted-foreground text-xs">phút</span>
              </div>
              <p class="text-[11px] text-muted-foreground">
                Lưu trữ cấu hình DB. Lưu ý: PartnerCommandServiceImpl hiện cố định thời gian quay đầu 30 phút khi lập lịch.
              </p>
            </div>

            <div class="space-y-1.5 md:col-span-2">
              <label class="block font-semibold text-foreground">Thời Gian Khóa Quỹ Escrow Sau Chuyến (Giờ)</label>
              <div class="relative max-w-xs">
                <Input
                  v-model.number="systemSettings.escrowSettlementDelayHours"
                  type="number"
                  min="1"
                  max="48"
                  :disabled="!hasPermission('SYSTEM_SETTINGS_MANAGE') || isSavingSettings"
                  class="h-9 text-xs pr-12 tabular-nums font-bold"
                />
                <span class="absolute right-3 top-2 text-muted-foreground text-xs">giờ</span>
              </div>
              <p class="text-[11px] text-muted-foreground">
                Tiêu thụ thực tế: Tham số quyết toán ví Escrow giải tỏa khiếu nại trước khi hoàn trả số dư khả dụng.
              </p>
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
                :disabled="!!editingStationId || isSubmittingStation"
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
                :disabled="isSubmittingStation"
                class="h-9 text-xs"
              />
            </div>

            <div class="space-y-1">
              <label class="block font-semibold text-foreground">Tỉnh / Thành Phố</label>
              <Input
                v-model="newStation.province"
                data-testid="input-station-city"
                placeholder="VD: Hà Nội, TP. Hồ Chí Minh"
                :disabled="isSubmittingStation"
                class="h-9 text-xs"
              />
            </div>

            <div class="pt-2 flex items-center gap-2">
              <Button
                v-if="editingStationId"
                variant="outline"
                size="sm"
                :disabled="isSubmittingStation"
                @click="cancelEditStation"
                class="text-xs flex-1"
              >
                Hủy
              </Button>
              <Button
                v-if="hasPermission('STATION_CREATE_UPDATE_DELETE')"
                size="sm"
                data-testid="save-station-btn"
                :disabled="isSubmittingStation"
                @click="handleAddOrUpdateStation"
                class="text-xs flex-1"
              >
                <Loader2 v-if="isSubmittingStation" class="w-3.5 h-3.5 mr-1 animate-spin" />
                <Plus v-else-if="!editingStationId" class="w-3.5 h-3.5 mr-1" />
                <Save v-else class="w-3.5 h-3.5 mr-1" />
                {{ editingStationId ? 'Cập Nhật' : 'Lưu Bến Xe' }}
              </Button>
            </div>
          </CardContent>
        </Card>

        <!-- Table Stations with Search, Filter & Pagination -->
        <div class="lg:col-span-2 space-y-3">
          <Card class="border-border shadow-sm overflow-hidden">
            <CardHeader class="p-4 border-b border-border space-y-3">
              <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <CardTitle class="text-sm font-bold text-foreground flex items-center gap-2">
                    <span>Danh Mục Bến Xe Liên Tỉnh ({{ stations.length }})</span>
                    <Badge v-if="filteredStations.length !== stations.length" variant="secondary" class="text-[10px]">
                      Khớp {{ filteredStations.length }}
                    </Badge>
                  </CardTitle>
                  <p class="text-xs text-muted-foreground mt-0.5">
                    Quản lý danh sách các bến xe, trạm trung chuyển trong hệ sinh thái toàn quốc.
                  </p>
                </div>
                <Button variant="ghost" size="sm" :disabled="stationsLoading" @click="fetchStations" class="h-8 text-xs shrink-0 self-start sm:self-auto">
                  <RefreshCw class="w-3.5 h-3.5 mr-1" :class="{ 'animate-spin': stationsLoading }" /> Làm mới
                </Button>
              </div>

              <!-- Search & Filter Controls -->
              <div v-if="stations.length > 0" class="grid grid-cols-1 sm:grid-cols-12 gap-2 pt-1">
                <!-- Search by name/code -->
                <div class="sm:col-span-6 relative">
                  <Search class="w-3.5 h-3.5 absolute left-2.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    v-model="stationSearchQuery"
                    placeholder="Tìm theo tên bến, mã bến, tỉnh thành..."
                    class="h-8 text-xs pl-8 bg-background"
                    data-testid="input-station-search"
                  />
                  <button
                    v-if="stationSearchQuery"
                    @click="stationSearchQuery = ''"
                    class="absolute right-2.5 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground text-xs"
                    title="Xóa tìm kiếm"
                  >
                    <X class="w-3 h-3" />
                  </button>
                </div>

                <!-- Province Filter -->
                <div class="sm:col-span-4">
                  <select
                    v-model="stationProvinceFilter"
                    class="w-full h-8 px-2 text-xs rounded-md border border-input bg-background text-foreground focus:outline-none focus:ring-1 focus:ring-ring"
                    data-testid="select-station-province-filter"
                  >
                    <option value="ALL">Tất cả tỉnh / thành ({{ uniqueStationProvinces.length }})</option>
                    <option v-for="prov in uniqueStationProvinces" :key="prov" :value="prov">
                      {{ prov }}
                    </option>
                  </select>
                </div>

                <!-- Status Filter -->
                <div class="sm:col-span-2">
                  <select
                    v-model="stationStatusFilter"
                    class="w-full h-8 px-2 text-xs rounded-md border border-input bg-background text-foreground focus:outline-none focus:ring-1 focus:ring-ring"
                    data-testid="select-station-status-filter"
                  >
                    <option value="ALL">Mọi trạng thái</option>
                    <option value="ACTIVE">Hoạt động</option>
                    <option value="INACTIVE">Tạm khóa</option>
                  </select>
                </div>
              </div>
            </CardHeader>

            <div v-if="stationsLoading" class="p-8 text-center text-xs text-muted-foreground">
              <Loader2 class="w-6 h-6 animate-spin mx-auto mb-2 text-primary" />
              Đang tải danh mục bến xe...
            </div>

            <div v-else-if="stationsError" class="p-6 text-center text-xs text-destructive space-y-2">
              <AlertCircle class="w-6 h-6 mx-auto" />
              <p>{{ stationsError }}</p>
              <Button variant="outline" size="sm" @click="fetchStations" class="text-xs">Thử lại</Button>
            </div>

            <div v-else-if="stations.length === 0" class="p-6">
              <EmptyState title="Chưa có bến xe nào" description="Nhập thông tin bên trái để tạo bến xe đầu tiên." />
            </div>

            <div v-else-if="filteredStations.length === 0" class="p-8 text-center text-xs text-muted-foreground space-y-2">
              <p>Không tìm thấy bến xe nào khớp với tiêu chí tìm kiếm.</p>
              <Button
                variant="outline"
                size="sm"
                @click="stationSearchQuery = ''; stationProvinceFilter = 'ALL'; stationStatusFilter = 'ALL'"
                class="text-xs"
              >
                Đặt lại bộ lọc
              </Button>
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
                  <TableRow v-for="s in paginatedStations" :key="s.id" class="hover:bg-muted/30">
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
                        @click="confirmDeleteStation(s)"
                        class="h-7 px-2 text-xs text-destructive hover:bg-destructive/10"
                      >
                        <Trash2 class="w-3 h-3" />
                      </Button>
                    </TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </div>

            <!-- Station Pagination Toolbar -->
            <div
              v-if="filteredStations.length > 0"
              class="p-3 border-t border-border bg-muted/10 flex flex-col sm:flex-row items-center justify-between gap-2 text-xs text-muted-foreground"
              data-testid="stations-pagination"
            >
              <div class="flex items-center gap-2">
                <span>
                  Hiển thị 
                  <strong class="text-foreground font-mono">
                    {{ (stationCurrentPage - 1) * stationPageSize + 1 }} - {{ Math.min(stationCurrentPage * stationPageSize, filteredStations.length) }}
                  </strong> 
                  trên tổng số <strong class="text-foreground font-mono">{{ filteredStations.length }}</strong> bến xe
                </span>
                <div class="flex items-center gap-1.5 ml-2">
                  <label for="station-page-size" class="text-xs">Mỗi trang:</label>
                  <select
                    id="station-page-size"
                    v-model.number="stationPageSize"
                    class="h-7 px-1.5 text-xs rounded border border-input bg-background text-foreground focus:outline-none"
                  >
                    <option :value="10">10</option>
                    <option :value="25">25</option>
                    <option :value="50">50</option>
                    <option :value="100">100</option>
                  </select>
                </div>
              </div>

              <div class="flex items-center gap-1">
                <Button
                  size="sm"
                  variant="outline"
                  class="h-7 text-xs"
                  :disabled="stationCurrentPage <= 1"
                  @click="stationCurrentPage--"
                  data-testid="btn-station-prev-page"
                >
                  <ChevronLeft class="w-3.5 h-3.5 mr-0.5" /> Trước
                </Button>

                <span class="px-2 font-mono font-semibold text-foreground text-xs">
                  {{ stationCurrentPage }} / {{ stationTotalPages }}
                </span>

                <Button
                  size="sm"
                  variant="outline"
                  class="h-7 text-xs"
                  :disabled="stationCurrentPage >= stationTotalPages"
                  @click="stationCurrentPage++"
                  data-testid="btn-station-next-page"
                >
                  Sau <ChevronRight class="w-3.5 h-3.5 ml-0.5" />
                </Button>
              </div>
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
              <span>Tạo Chuyến Xe Toàn Sàn (CreateTripCommand)</span>
            </CardTitle>
            <p class="text-[11px] text-muted-foreground mt-0.5">
              Khởi tạo chuyến xe mới với dữ liệu bến và sơ đồ ghế xác thực từ cơ sở dữ liệu.
            </p>
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
                :disabled="isSubmittingTrip"
              />
            </div>

            <!-- Station Selectors -->
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-2">
              <div class="space-y-1">
                <label class="block font-semibold text-foreground">
                  Bến Đi <span class="text-destructive">*</span>
                </label>
                <select
                  v-model="tripForm.departureStationId"
                  data-testid="select-dep-station"
                  :disabled="isSubmittingTrip || stations.length === 0"
                  class="w-full h-9 px-2 text-xs rounded-md border border-input bg-background text-foreground focus:outline-none focus:ring-1 focus:ring-ring"
                >
                  <option value="" disabled>-- Chọn bến đi --</option>
                  <option v-for="s in stations" :key="s.rawId || s.id" :value="s.rawId || s.id">
                    {{ s.name }} ({{ s.province }})
                  </option>
                </select>
              </div>

              <div class="space-y-1">
                <label class="block font-semibold text-foreground">
                  Bến Đến <span class="text-destructive">*</span>
                </label>
                <select
                  v-model="tripForm.arrivalStationId"
                  data-testid="select-arr-station"
                  :disabled="isSubmittingTrip || stations.length === 0"
                  class="w-full h-9 px-2 text-xs rounded-md border border-input bg-background text-foreground focus:outline-none focus:ring-1 focus:ring-ring"
                >
                  <option value="" disabled>-- Chọn bến đến --</option>
                  <option v-for="s in stations" :key="s.rawId || s.id" :value="s.rawId || s.id">
                    {{ s.name }} ({{ s.province }})
                  </option>
                </select>
              </div>
            </div>

            <!-- Departure and Arrival Datetime -->
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-2">
              <div class="space-y-1">
                <label class="block font-semibold text-foreground">
                  Giờ Khởi Hành <span class="text-destructive">*</span>
                </label>
                <Input
                  v-model="tripForm.departureTime"
                  type="datetime-local"
                  data-testid="input-dep-time"
                  :disabled="isSubmittingTrip"
                  class="h-9 text-xs tabular-nums"
                />
              </div>

              <div class="space-y-1">
                <label class="block font-semibold text-foreground">
                  Giờ Đến Nơi <span class="text-destructive">*</span>
                </label>
                <Input
                  v-model="tripForm.arrivalTime"
                  type="datetime-local"
                  data-testid="input-arr-time"
                  :disabled="isSubmittingTrip"
                  class="h-9 text-xs tabular-nums"
                />
              </div>
            </div>

            <!-- Transport Type & Base Price -->
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-2">
              <div class="space-y-1">
                <label class="block font-semibold text-foreground">Loại Phương Tiện</label>
                <select
                  v-model="tripForm.transportType"
                  :disabled="isSubmittingTrip"
                  class="w-full h-9 px-2 text-xs rounded-md border border-input bg-background text-foreground focus:outline-none focus:ring-1 focus:ring-ring"
                >
                  <option value="BUS">Xe Khách (BUS)</option>
                  <option value="TRAIN">Tàu Hỏa (TRAIN)</option>
                  <option value="FLIGHT">Máy Bay (FLIGHT)</option>
                </select>
              </div>

              <div class="space-y-1">
                <label class="block font-semibold text-foreground">Giá Vé Cơ Bản (đ)</label>
                <Input
                  v-model.number="tripForm.basePrice"
                  type="number"
                  step="10000"
                  min="10000"
                  :disabled="isSubmittingTrip"
                  class="h-9 text-xs tabular-nums font-bold"
                />
              </div>
            </div>

            <!-- Seating Geometry (totalRows, seatsPerRow, vipRows) -->
            <div class="p-3 bg-muted/30 rounded-lg border border-border space-y-2">
              <span class="font-semibold text-foreground flex items-center justify-between">
                <span>Cấu Hình Sơ Đồ Ghế</span>
                <span class="text-primary font-mono font-bold">{{ calculatedSeats }} Chỗ</span>
              </span>
              <div class="grid grid-cols-3 gap-2">
                <div class="space-y-1">
                  <label class="block text-[11px] text-muted-foreground">Tổng Hàng (1-60)</label>
                  <Input
                    v-model.number="tripForm.totalRows"
                    type="number"
                    min="1"
                    max="60"
                    :disabled="isSubmittingTrip"
                    class="h-8 text-xs tabular-nums"
                  />
                </div>
                <div class="space-y-1">
                  <label class="block text-[11px] text-muted-foreground">Ghế/Hàng (1-10)</label>
                  <Input
                    v-model.number="tripForm.seatsPerRow"
                    type="number"
                    min="1"
                    max="10"
                    :disabled="isSubmittingTrip"
                    class="h-8 text-xs tabular-nums"
                  />
                </div>
                <div class="space-y-1">
                  <label class="block text-[11px] text-muted-foreground">Hàng VIP</label>
                  <Input
                    v-model.number="tripForm.vipRows"
                    type="number"
                    min="0"
                    :max="tripForm.totalRows"
                    :disabled="isSubmittingTrip"
                    class="h-8 text-xs tabular-nums"
                  />
                </div>
              </div>
            </div>

            <div class="pt-2">
              <Button
                v-if="hasPermission('TRIP_MANAGE_CRUD')"
                size="sm"
                data-testid="save-trip-btn"
                :disabled="isSubmittingTrip"
                @click="handleAddTrip"
                class="w-full text-xs font-semibold"
              >
                <Loader2 v-if="isSubmittingTrip" class="w-3.5 h-3.5 mr-1 animate-spin" />
                <Plus v-else class="w-3.5 h-3.5 mr-1" />
                Tạo Chuyến Xe
              </Button>
            </div>
          </CardContent>
        </Card>

        <!-- Table Trips with Search, Filter & Pagination -->
        <div class="lg:col-span-2 space-y-3">
          <Card class="border-border shadow-sm overflow-hidden">
            <CardHeader class="p-4 border-b border-border space-y-3">
              <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <CardTitle class="text-sm font-bold text-foreground flex items-center gap-2">
                    <span>Danh Mục Tuyến & Chuyến Vận Hành ({{ trips.length }})</span>
                    <Badge v-if="filteredTrips.length !== trips.length" variant="secondary" class="text-[10px]">
                      Khớp {{ filteredTrips.length }}
                    </Badge>
                  </CardTitle>
                  <p class="text-[11px] text-muted-foreground mt-0.5">
                    Quản lý danh sách lịch trình, giờ xuất bến và kiểm soát trạng thái vận hành.
                  </p>
                </div>
                <Button variant="ghost" size="sm" :disabled="tripsLoading" @click="fetchTrips" class="h-8 text-xs shrink-0 self-start sm:self-auto">
                  <RefreshCw class="w-3.5 h-3.5 mr-1" :class="{ 'animate-spin': tripsLoading }" /> Làm mới
                </Button>
              </div>

              <!-- Search & Filter Controls -->
              <div v-if="trips.length > 0" class="grid grid-cols-1 sm:grid-cols-12 gap-2 pt-1">
                <!-- Search by tripCode, route -->
                <div class="sm:col-span-8 relative">
                  <Search class="w-3.5 h-3.5 absolute left-2.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    v-model="tripSearchQuery"
                    placeholder="Tìm theo mã chuyến, lộ trình, loại xe..."
                    class="h-8 text-xs pl-8 bg-background"
                    data-testid="input-trip-search"
                  />
                  <button
                    v-if="tripSearchQuery"
                    @click="tripSearchQuery = ''"
                    class="absolute right-2.5 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground text-xs"
                    title="Xóa tìm kiếm"
                  >
                    <X class="w-3 h-3" />
                  </button>
                </div>

                <!-- Status Filter -->
                <div class="sm:col-span-4">
                  <select
                    v-model="tripStatusFilter"
                    class="w-full h-8 px-2 text-xs rounded-md border border-input bg-background text-foreground focus:outline-none focus:ring-1 focus:ring-ring"
                    data-testid="select-trip-status-filter"
                  >
                    <option value="ALL">Mọi trạng thái</option>
                    <option value="SCHEDULED">Đã lên lịch (SCHEDULED)</option>
                    <option value="RUNNING">Đang chạy (RUNNING)</option>
                    <option value="COMPLETED">Hoàn thành (COMPLETED)</option>
                    <option value="CANCELLED">Đã hủy (CANCELLED)</option>
                  </select>
                </div>
              </div>
            </CardHeader>

            <div v-if="tripsLoading" class="p-8 text-center text-xs text-muted-foreground">
              <Loader2 class="w-6 h-6 animate-spin mx-auto mb-2 text-primary" />
              Đang tải danh sách chuyến xe...
            </div>

            <div v-else-if="tripsError" class="p-6 text-center text-xs text-destructive space-y-2">
              <AlertCircle class="w-6 h-6 mx-auto" />
              <p>{{ tripsError }}</p>
              <Button variant="outline" size="sm" @click="fetchTrips" class="text-xs">Thử lại</Button>
            </div>

            <div v-else-if="trips.length === 0" class="p-6">
              <EmptyState title="Chưa có chuyến xe nào" description="Nhập thông tin bên trái để tạo lịch trình chuyến xe mới." />
            </div>

            <div v-else-if="filteredTrips.length === 0" class="p-8 text-center text-xs text-muted-foreground space-y-2">
              <p>Không tìm thấy chuyến xe nào khớp với tiêu chí tìm kiếm.</p>
              <Button
                variant="outline"
                size="sm"
                @click="tripSearchQuery = ''; tripStatusFilter = 'ALL'"
                class="text-xs"
              >
                Đặt lại bộ lọc
              </Button>
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
                  <TableRow v-for="t in paginatedTrips" :key="t.id" class="hover:bg-muted/30">
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
                        @click="confirmCancelTrip(t)"
                        class="h-7 px-2 text-xs text-amber-600 hover:bg-amber-50"
                      >
                        <Ban class="w-3 h-3 mr-1" /> Hủy
                      </Button>
                      <Badge v-else-if="t.status === 'CANCELLED'" variant="outline" class="text-[10px] text-muted-foreground">
                        Đã Hủy
                      </Badge>
                    </TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </div>

            <!-- Trip Pagination Toolbar -->
            <div
              v-if="filteredTrips.length > 0"
              class="p-3 border-t border-border bg-muted/10 flex flex-col sm:flex-row items-center justify-between gap-2 text-xs text-muted-foreground"
              data-testid="trips-pagination"
            >
              <div class="flex items-center gap-2">
                <span>
                  Hiển thị 
                  <strong class="text-foreground font-mono">
                    {{ (tripCurrentPage - 1) * tripPageSize + 1 }} - {{ Math.min(tripCurrentPage * tripPageSize, filteredTrips.length) }}
                  </strong> 
                  trên tổng số <strong class="text-foreground font-mono">{{ filteredTrips.length }}</strong> chuyến xe
                </span>
                <div class="flex items-center gap-1.5 ml-2">
                  <label for="trip-page-size" class="text-xs">Mỗi trang:</label>
                  <select
                    id="trip-page-size"
                    v-model.number="tripPageSize"
                    class="h-7 px-1.5 text-xs rounded border border-input bg-background text-foreground focus:outline-none"
                  >
                    <option :value="10">10</option>
                    <option :value="25">25</option>
                    <option :value="50">50</option>
                    <option :value="100">100</option>
                  </select>
                </div>
              </div>

              <div class="flex items-center gap-1">
                <Button
                  size="sm"
                  variant="outline"
                  class="h-7 text-xs"
                  :disabled="tripCurrentPage <= 1"
                  @click="tripCurrentPage--"
                  data-testid="btn-trip-prev-page"
                >
                  <ChevronLeft class="w-3.5 h-3.5 mr-0.5" /> Trước
                </Button>

                <span class="px-2 font-mono font-semibold text-foreground text-xs">
                  {{ tripCurrentPage }} / {{ tripTotalPages }}
                </span>

                <Button
                  size="sm"
                  variant="outline"
                  class="h-7 text-xs"
                  :disabled="tripCurrentPage >= tripTotalPages"
                  @click="tripCurrentPage++"
                  data-testid="btn-trip-next-page"
                >
                  Sau <ChevronRight class="w-3.5 h-3.5 ml-0.5" />
                </Button>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </section>

    <!-- Confirmation Dialog: Delete Station -->
    <AlertDialog :open="!!stationToDelete" @update:open="(val) => { if (!val) stationToDelete = null }">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle class="flex items-center gap-2 text-destructive">
            <AlertTriangle class="w-5 h-5" />
            Xác Nhận Xóa Bến Xe Liên Tỉnh
          </AlertDialogTitle>
          <AlertDialogDescription class="text-xs space-y-2">
            <p>
              Bạn có chắc chắn muốn xóa bến xe: <strong>{{ stationToDelete?.name }}</strong> (Mã: {{ stationToDelete?.code || stationToDelete?.id }})?
            </p>
            <p class="text-muted-foreground">
              Lưu ý: Bến xe đã có chuyến liên kết trong quá khứ hoặc hiện tại sẽ bị máy chủ từ chối xóa để bảo đảm toàn vẹn dữ liệu.
            </p>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel :disabled="isSubmittingStation">Hủy bỏ</AlertDialogCancel>
          <AlertDialogAction
            :disabled="isSubmittingStation"
            class="bg-destructive text-destructive-foreground hover:bg-destructive/90 text-xs"
            @click="executeDeleteStation"
          >
            <Loader2 v-if="isSubmittingStation" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
            Xác Nhận Xóa
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <!-- Confirmation Dialog: Cancel Trip -->
    <AlertDialog :open="!!tripToCancel" @update:open="(val) => { if (!val) tripToCancel = null }">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle class="flex items-center gap-2 text-amber-600">
            <AlertTriangle class="w-5 h-5" />
            Xác Nhận Hủy Lịch Trình Chuyến Xe
          </AlertDialogTitle>
          <AlertDialogDescription class="text-xs space-y-2">
            <p>
              Bạn có chắc chắn muốn hủy chuyến xe mã: <strong>{{ tripToCancel?.tripCode }}</strong> (Lộ trình: {{ tripToCancel?.route }})?
            </p>
            <p class="text-muted-foreground">
              Thao tác này sẽ chuyển trạng thái chuyến xe sang CANCELLED và ngăn chặn các giao dịch đặt chỗ mới.
            </p>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel :disabled="isSubmittingTrip">Không Hủy</AlertDialogCancel>
          <AlertDialogAction
            :disabled="isSubmittingTrip"
            class="bg-amber-600 text-white hover:bg-amber-700 text-xs"
            @click="executeCancelTrip"
          >
            <Loader2 v-if="isSubmittingTrip" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
            Xác Nhận Hủy Chuyến
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <!-- Confirmation Dialog: Save System Settings -->
    <AlertDialog :open="isConfirmSaveOpen" @update:open="(val) => { isConfirmSaveOpen = val }">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle class="flex items-center gap-2 text-primary">
            <Info class="w-5 h-5" />
            Xác Nhận Cập Nhật Tham Số Toàn Sàn
          </AlertDialogTitle>
          <AlertDialogDescription class="text-xs space-y-2">
            <p>
              Các thay đổi sẽ được ghi nhận vào cơ sở dữ liệu hệ thống (TBL-SYSTEM-SETTINGS) và có hiệu lực tức thì đối với các dịch vụ miền đang tiêu thụ tham số này:
            </p>
            <ul class="list-disc list-inside space-y-1 text-foreground font-mono text-[11px]">
              <li>Tên sàn: {{ systemSettings.platformName }}</li>
              <li>Hoa hồng mặc định: {{ systemSettings.defaultCommissionRate }}%</li>
              <li>Giữ chỗ DB: {{ systemSettings.seatHoldTtlMinutes }} phút (Static 10m trong Booking)</li>
              <li>Đệm quay đầu DB: {{ systemSettings.turnaroundBufferMinutes }} phút (Static 30m trong Partner)</li>
              <li>Giải tỏa Escrow: {{ systemSettings.escrowSettlementDelayHours }} giờ</li>
            </ul>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel :disabled="isSavingSettings">Đóng</AlertDialogCancel>
          <AlertDialogAction
            :disabled="isSavingSettings"
            class="text-xs font-semibold"
            @click="executeSaveSettings"
          >
            <Loader2 v-if="isSavingSettings" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
            Lưu Cấu Hình
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>