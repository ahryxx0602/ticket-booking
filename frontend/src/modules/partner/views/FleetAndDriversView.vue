<script setup>
import { ref, computed, onMounted } from 'vue'
import { partnerApi } from '../api'
import { toast } from 'vue-sonner'
import {
  Truck,
  Users,
  Plus,
  Wrench,
  CheckCircle,
  AlertTriangle,
  RefreshCw,
  Pencil,
  IdCard,
  Phone,
  Car,
  Layers
} from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle
} from '@/components/ui/dialog'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle
} from '@/components/ui/alert-dialog'
import {
  Tabs,
  TabsList,
  TabsTrigger,
  TabsContent
} from '@/components/ui/tabs'

// Active tab
const activeTab = ref('VEHICLES')

// Loading & error states
const isLoadingVehicles = ref(false)
const isLoadingDrivers = ref(false)
const vehiclesError = ref('')
const driversError = ref('')

// Authoritative data lists
const vehicles = ref([])
const drivers = ref([])

// ---------------------------------------------------------------------------
// VEHICLE DATA FETCHING & MUTATIONS
// ---------------------------------------------------------------------------
async function fetchVehicles() {
  isLoadingVehicles.value = true
  vehiclesError.value = ''
  try {
    const res = await partnerApi.getVehicles()
    const data = res?.data?.data !== undefined ? res.data.data : res?.data
    if (!Array.isArray(data)) {
      throw new Error('Dữ liệu danh sách phương tiện từ máy chủ không hợp lệ (không phải danh sách)')
    }
    vehicles.value = data.filter(v => v && v.id != null)
  } catch (err) {
    vehiclesError.value = err?.response?.data?.message || err?.message || 'Không thể tải danh sách phương tiện'
    toast.error('Lỗi khi tải danh sách phương tiện: ' + vehiclesError.value)
  } finally {
    isLoadingVehicles.value = false
  }
}

// Vehicle Modal Form State
const isVehicleModalOpen = ref(false)
const isEditingVehicle = ref(false)
const editingVehicleId = ref(null)
const isSavingVehicle = ref(false)
const vehicleFormError = ref('')

const vehicleForm = ref({
  plateNumber: '',
  modelName: 'Xe Khách Tiêu Chuẩn',
  vehicleType: 'SLEEPER',
  totalSeats: 36,
  seatRows: 9,
  seatsPerRow: 4,
  vipRows: 2
})

function resetVehicleForm() {
  vehicleForm.value = {
    plateNumber: '',
    modelName: 'Xe Khách Tiêu Chuẩn',
    vehicleType: 'SLEEPER',
    totalSeats: 36,
    seatRows: 9,
    seatsPerRow: 4,
    vipRows: 2
  }
  vehicleFormError.value = ''
  editingVehicleId.value = null
  isEditingVehicle.value = false
}

function openAddVehicleModal() {
  resetVehicleForm()
  isEditingVehicle.value = false
  isVehicleModalOpen.value = true
}

function openEditVehicleModal(vehicle) {
  if (!vehicle || vehicle.id == null) {
    toast.error('Thông tin phương tiện không hợp lệ.')
    return
  }
  resetVehicleForm()
  isEditingVehicle.value = true
  editingVehicleId.value = vehicle.id
  vehicleForm.value = {
    plateNumber: vehicle.plateNumber || '',
    modelName: vehicle.modelName || '',
    vehicleType: vehicle.vehicleType || 'SLEEPER',
    totalSeats: Number(vehicle.totalSeats) > 0 ? Number(vehicle.totalSeats) : 0,
    seatRows: Number(vehicle.seatRows) > 0 ? Number(vehicle.seatRows) : 0,
    seatsPerRow: Number(vehicle.seatsPerRow) > 0 ? Number(vehicle.seatsPerRow) : 0,
    vipRows: Number(vehicle.vipRows) >= 0 ? Number(vehicle.vipRows) : 0
  }
  isVehicleModalOpen.value = true
}

// Auto recalculate total seats when rows change
function updateSeatCalculation() {
  const rows = Number(vehicleForm.value.seatRows) || 0
  const cols = Number(vehicleForm.value.seatsPerRow) || 0
  if (rows > 0 && cols > 0) {
    vehicleForm.value.totalSeats = rows * cols
  }
}

async function handleSaveVehicle() {
  vehicleFormError.value = ''

  const plate = vehicleForm.value.plateNumber?.trim()
  if (!plate) {
    vehicleFormError.value = 'Biển số xe không được để trống'
    return
  }

  const plateRegex = /^[0-9]{2}[A-Z]-[0-9]{3,5}(\.[0-9]{2})?$/i
  if (!plateRegex.test(plate)) {
    vehicleFormError.value = 'Biển số xe không đúng định dạng chuẩn Bộ GTVT (VD: 29B-123.45)'
    return
  }

  const model = vehicleForm.value.modelName?.trim()
  if (!model) {
    vehicleFormError.value = 'Tên model xe không được để trống'
    return
  }

  if (!vehicleForm.value.seatRows || vehicleForm.value.seatRows <= 0) {
    vehicleFormError.value = 'Số hàng ghế phải lớn hơn 0'
    return
  }
  if (!vehicleForm.value.seatsPerRow || vehicleForm.value.seatsPerRow <= 0) {
    vehicleFormError.value = 'Số ghế mỗi hàng phải lớn hơn 0'
    return
  }
  if (vehicleForm.value.vipRows < 0 || vehicleForm.value.vipRows > vehicleForm.value.seatRows) {
    vehicleFormError.value = 'Số hàng ghế VIP không hợp lệ (không vượt quá tổng hàng ghế)'
    return
  }

  const calculatedSeats = Number(vehicleForm.value.totalSeats)
  if (!calculatedSeats || calculatedSeats <= 0 || calculatedSeats > 100) {
    vehicleFormError.value = 'Tổng số ghế xe phải từ 1 đến 100 theo tiêu chuẩn đăng kiểm'
    return
  }

  const payload = {
    plateNumber: plate.toUpperCase(),
    modelName: model,
    vehicleType: vehicleForm.value.vehicleType,
    totalSeats: Number(vehicleForm.value.totalSeats),
    seatRows: Number(vehicleForm.value.seatRows),
    seatsPerRow: Number(vehicleForm.value.seatsPerRow),
    vipRows: Number(vehicleForm.value.vipRows) || 0
  }

  isSavingVehicle.value = true
  try {
    if (isEditingVehicle.value && editingVehicleId.value) {
      await partnerApi.updateVehicle(editingVehicleId.value, payload)
      toast.success(`Cập nhật xe ${payload.plateNumber} thành công`)
    } else {
      await partnerApi.createVehicle(payload)
      toast.success(`Thêm mới xe ${payload.plateNumber} thành công`)
    }
    isVehicleModalOpen.value = false
    await fetchVehicles()
  } catch (err) {
    vehicleFormError.value = err?.response?.data?.message || err?.message || 'Lỗi khi lưu thông tin phương tiện'
    toast.error(vehicleFormError.value)
  } finally {
    isSavingVehicle.value = false
  }
}

// Vehicle Status Maintenance / Activate Modal
const isStatusConfirmOpen = ref(false)
const selectedVehicleForStatus = ref(null)
const isUpdatingStatus = ref(false)

function openStatusConfirm(vehicle) {
  selectedVehicleForStatus.value = vehicle
  isStatusConfirmOpen.value = true
}

async function handleToggleVehicleStatus() {
  if (!selectedVehicleForStatus.value) return
  const vehicle = selectedVehicleForStatus.value
  const isMaintenance = vehicle.status === 'MAINTENANCE'

  isUpdatingStatus.value = true
  try {
    if (isMaintenance) {
      await partnerApi.activateVehicle(vehicle.id)
      toast.success(`Đã kích hoạt xe ${vehicle.plateNumber} vào hoạt động`)
    } else {
      await partnerApi.setVehicleMaintenance(vehicle.id)
      toast.success(`Đã chuyển xe ${vehicle.plateNumber} sang trạng thái bảo dưỡng`)
    }
    isStatusConfirmOpen.value = false
    selectedVehicleForStatus.value = null
    await fetchVehicles()
  } catch (err) {
    toast.error('Lỗi khi cập nhật trạng thái xe: ' + (err?.response?.data?.message || err?.message))
  } finally {
    isUpdatingStatus.value = false
  }
}

// ---------------------------------------------------------------------------
// DRIVER DATA FETCHING & MUTATIONS
// ---------------------------------------------------------------------------
async function fetchDrivers() {
  isLoadingDrivers.value = true
  driversError.value = ''
  try {
    const res = await partnerApi.getDrivers()
    const data = res?.data?.data !== undefined ? res.data.data : res?.data
    if (!Array.isArray(data)) {
      throw new Error('Dữ liệu danh sách tài xế từ máy chủ không hợp lệ (không phải danh sách)')
    }
    drivers.value = data.filter(d => d && d.id != null)
  } catch (err) {
    driversError.value = err?.response?.data?.message || err?.message || 'Không thể tải danh sách tài xế'
    toast.error('Lỗi khi tải danh sách tài xế: ' + driversError.value)
  } finally {
    isLoadingDrivers.value = false
  }
}

// Driver Modal Form State
const isDriverModalOpen = ref(false)
const isEditingDriver = ref(false)
const editingDriverId = ref(null)
const isSavingDriver = ref(false)
const driverFormError = ref('')

const driverForm = ref({
  fullName: '',
  phone: '',
  licenseNumber: ''
})

function resetDriverForm() {
  driverForm.value = {
    fullName: '',
    phone: '',
    licenseNumber: ''
  }
  driverFormError.value = ''
  editingDriverId.value = null
  isEditingDriver.value = false
}

function openAddDriverModal() {
  resetDriverForm()
  isEditingDriver.value = false
  isDriverModalOpen.value = true
}

function openEditDriverModal(driver) {
  resetDriverForm()
  isEditingDriver.value = true
  editingDriverId.value = driver.id
  driverForm.value = {
    fullName: driver.fullName || '',
    phone: driver.phone || '',
    licenseNumber: driver.licenseNumber || ''
  }
  isDriverModalOpen.value = true
}

async function handleSaveDriver() {
  driverFormError.value = ''

  const name = driverForm.value.fullName?.trim()
  if (!name) {
    driverFormError.value = 'Họ và tên tài xế không được để trống'
    return
  }

  const phone = driverForm.value.phone?.trim()
  const phoneRegex = /(84|0[3|5|7|8|9])+([0-9]{8})\b/
  if (!phone || !phoneRegex.test(phone)) {
    driverFormError.value = 'Số điện thoại không hợp lệ (định dạng 10 số VN)'
    return
  }

  const license = driverForm.value.licenseNumber?.trim()
  if (!license) {
    driverFormError.value = 'Số giấy phép lái xe không được để trống'
    return
  }

  const payload = {
    fullName: name,
    phone: phone,
    licenseNumber: license
  }

  isSavingDriver.value = true
  try {
    if (isEditingDriver.value && editingDriverId.value) {
      await partnerApi.updateDriver(editingDriverId.value, payload)
      toast.success(`Cập nhật tài xế ${payload.fullName} thành công`)
    } else {
      await partnerApi.createDriver(payload)
      toast.success(`Thêm mới tài xế ${payload.fullName} thành công`)
    }
    isDriverModalOpen.value = false
    await fetchDrivers()
  } catch (err) {
    driverFormError.value = err?.response?.data?.message || err?.message || 'Lỗi khi lưu thông tin tài xế'
    toast.error(driverFormError.value)
  } finally {
    isSavingDriver.value = false
  }
}

function formatVehicleTypeName(type) {
  switch (type) {
    case 'SLEEPER': return 'Giường nằm cao cấp'
    case 'LIMOUSINE': return 'Limousine VIP'
    case 'SEAT': return 'Ghế ngồi tiêu chuẩn'
    default: return type || 'Khác'
  }
}

onMounted(() => {
  fetchVehicles()
  fetchDrivers()
})
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-border">
      <div>
        <h1 class="text-2xl font-bold tracking-tight text-foreground flex items-center gap-2">
          <Truck class="w-6 h-6 text-primary" />
          <span>Quản Trị Đội Xe & Danh Sách Tài Xế</span>
        </h1>
        <p class="text-xs text-muted-foreground mt-1">
          Hồ sơ phương tiện vận tải và đội ngũ tài xế được ủy quyền vận hành theo quy chuẩn Bộ Giao Thông Vận Tải.
        </p>
      </div>

      <div class="flex items-center gap-2">
        <Button
          v-if="activeTab === 'VEHICLES'"
          size="sm"
          data-testid="btn-add-vehicle"
          class="bg-primary hover:bg-primary/90 text-primary-foreground text-xs font-semibold"
          @click="openAddVehicleModal"
        >
          <Plus class="w-3.5 h-3.5 mr-1" /> Thêm Phương Tiện
        </Button>

        <Button
          v-else
          size="sm"
          data-testid="btn-add-driver"
          class="bg-primary hover:bg-primary/90 text-primary-foreground text-xs font-semibold"
          @click="openAddDriverModal"
        >
          <Plus class="w-3.5 h-3.5 mr-1" /> Thêm Tài Xế
        </Button>
      </div>
    </div>

    <!-- Tabs Container -->
    <Tabs v-model="activeTab" class="w-full space-y-4">
      <div class="flex items-center justify-between">
        <TabsList class="grid w-full max-w-[360px] grid-cols-2">
          <TabsTrigger value="VEHICLES" data-testid="tab-vehicles" class="text-xs">
            <Car class="w-3.5 h-3.5 mr-1.5" />
            Phương Tiện ({{ vehicles.length }})
          </TabsTrigger>
          <TabsTrigger value="DRIVERS" data-testid="tab-drivers" class="text-xs">
            <Users class="w-3.5 h-3.5 mr-1.5" />
            Tài Xế ({{ drivers.length }})
          </TabsTrigger>
        </TabsList>

        <Button
          variant="outline"
          size="sm"
          class="h-8 text-xs"
          @click="activeTab === 'VEHICLES' ? fetchVehicles() : fetchDrivers()"
        >
          <RefreshCw class="w-3.5 h-3.5 mr-1" /> Làm mới
        </Button>
      </div>

      <!-- TAB 1: VEHICLES -->
      <TabsContent value="VEHICLES" class="space-y-4 m-0">
        <div v-if="isLoadingVehicles" class="p-8 text-center text-xs text-muted-foreground">
          <RefreshCw class="w-5 h-5 mx-auto animate-spin mb-2 text-primary" />
          Đang tải dữ liệu đội xe từ máy chủ...
        </div>

        <div v-else-if="vehiclesError" class="p-6 bg-destructive/10 border border-destructive/20 rounded-lg text-xs text-destructive flex items-center justify-between">
          <div class="flex items-center gap-2">
            <AlertTriangle class="w-4 h-4" />
            <span>{{ vehiclesError }}</span>
          </div>
          <Button variant="outline" size="sm" @click="fetchVehicles" class="text-xs">Thử lại</Button>
        </div>

        <div v-else-if="vehicles.length === 0" class="p-12 text-center border border-dashed rounded-xl bg-muted/20">
          <Car class="w-10 h-10 mx-auto text-muted-foreground/50 mb-2" />
          <h3 class="text-sm font-semibold text-foreground">Chưa có phương tiện nào</h3>
          <p class="text-xs text-muted-foreground mt-1 max-w-sm mx-auto">
            Nhà xe chưa đăng ký xe nào vào đội xe. Hãy thêm xe đầu tiên để bắt đầu lập lịch trình.
          </p>
          <Button size="sm" class="mt-4 text-xs" @click="openAddVehicleModal">
            <Plus class="w-3.5 h-3.5 mr-1" /> Thêm Phương Tiện Đầu Tiên
          </Button>
        </div>

        <div v-else class="rounded-xl border border-border bg-card shadow-sm overflow-hidden">
          <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse text-xs">
              <thead>
                <tr class="bg-muted/40 border-b border-border text-muted-foreground uppercase tracking-wider text-[11px]">
                  <th class="py-3 px-4 font-semibold">Biển Số Xe</th>
                  <th class="py-3 px-4 font-semibold">Dòng Xe / Model</th>
                  <th class="py-3 px-4 font-semibold">Phân Loại</th>
                  <th class="py-3 px-4 font-semibold text-center">Cấu Hình Ghế</th>
                  <th class="py-3 px-4 font-semibold text-center">Hàng VIP</th>
                  <th class="py-3 px-4 font-semibold text-center">Trạng Thái</th>
                  <th class="py-3 px-4 font-semibold text-right">Thao Tác</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-border">
                <tr v-for="v in vehicles" :key="v.id" class="hover:bg-muted/30 transition-colors">
                  <td class="py-3.5 px-4 font-mono font-bold text-primary text-sm">{{ v.plateNumber }}</td>
                  <td class="py-3.5 px-4 font-medium text-foreground">{{ v.modelName || 'Chưa cập nhật' }}</td>
                  <td class="py-3.5 px-4 text-muted-foreground">
                    <Badge variant="outline" class="text-[11px] font-normal">
                      {{ formatVehicleTypeName(v.vehicleType) }}
                    </Badge>
                  </td>
                  <td class="py-3.5 px-4 text-center">
                    <span class="font-bold tabular-nums text-foreground">{{ v.totalSeats }}</span> chỗ
                    <span class="text-[11px] text-muted-foreground">({{ v.seatRows }}x{{ v.seatsPerRow }})</span>
                  </td>
                  <td class="py-3.5 px-4 text-center">
                    <Badge v-if="v.vipRows > 0" variant="secondary" class="text-[10px] bg-amber-500/10 text-amber-600 border-amber-500/20">
                      {{ v.vipRows }} hàng
                    </Badge>
                    <span v-else class="text-muted-foreground text-[11px]">0</span>
                  </td>
                  <td class="py-3.5 px-4 text-center">
                    <Badge
                      :variant="v.status === 'ACTIVE' ? 'default' : 'outline'"
                      :class="v.status === 'ACTIVE'
                        ? 'bg-emerald-500/10 text-emerald-600 hover:bg-emerald-500/20 border-emerald-500/20'
                        : 'bg-amber-500/10 text-amber-600 hover:bg-amber-500/20 border-amber-500/20'"
                      class="text-[10px] font-semibold"
                    >
                      {{ v.status === 'ACTIVE' ? 'Sẵn Sàng Chạy' : 'Bảo Dưỡng' }}
                    </Badge>
                  </td>
                  <td class="py-3.5 px-4 text-right">
                    <div class="flex items-center justify-end gap-1.5">
                      <Button
                        variant="ghost"
                        size="sm"
                        class="h-7 px-2 text-xs"
                        @click="openEditVehicleModal(v)"
                        title="Chỉnh sửa thông số xe"
                      >
                        <Pencil class="w-3.5 h-3.5 mr-1" /> Sửa
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        class="h-7 px-2 text-xs"
                        :class="v.status === 'ACTIVE' ? 'text-amber-600 border-amber-200 hover:bg-amber-50' : 'text-emerald-600 border-emerald-200 hover:bg-emerald-50'"
                        @click="openStatusConfirm(v)"
                      >
                        <Wrench v-if="v.status === 'ACTIVE'" class="w-3.5 h-3.5 mr-1" />
                        <CheckCircle v-else class="w-3.5 h-3.5 mr-1" />
                        {{ v.status === 'ACTIVE' ? 'Bảo Dưỡng' : 'Kích Hoạt' }}
                      </Button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </TabsContent>

      <!-- TAB 2: DRIVERS -->
      <TabsContent value="DRIVERS" class="space-y-4 m-0">
        <div v-if="isLoadingDrivers" class="p-8 text-center text-xs text-muted-foreground">
          <RefreshCw class="w-5 h-5 mx-auto animate-spin mb-2 text-primary" />
          Đang tải danh sách tài xế từ máy chủ...
        </div>

        <div v-else-if="driversError" class="p-6 bg-destructive/10 border border-destructive/20 rounded-lg text-xs text-destructive flex items-center justify-between">
          <div class="flex items-center gap-2">
            <AlertTriangle class="w-4 h-4" />
            <span>{{ driversError }}</span>
          </div>
          <Button variant="outline" size="sm" @click="fetchDrivers" class="text-xs">Thử lại</Button>
        </div>

        <div v-else-if="drivers.length === 0" class="p-12 text-center border border-dashed rounded-xl bg-muted/20">
          <Users class="w-10 h-10 mx-auto text-muted-foreground/50 mb-2" />
          <h3 class="text-sm font-semibold text-foreground">Chưa có tài xế nào</h3>
          <p class="text-xs text-muted-foreground mt-1 max-w-sm mx-auto">
            Nhà xe chưa khai báo tài xế. Hãy thêm bác tài đầu tiên để phục vụ vận hành chuyến đi.
          </p>
          <Button size="sm" class="mt-4 text-xs" @click="openAddDriverModal">
            <Plus class="w-3.5 h-3.5 mr-1" /> Thêm Tài Xế Đầu Tiên
          </Button>
        </div>

        <div v-else class="rounded-xl border border-border bg-card shadow-sm overflow-hidden">
          <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse text-xs">
              <thead>
                <tr class="bg-muted/40 border-b border-border text-muted-foreground uppercase tracking-wider text-[11px]">
                  <th class="py-3 px-4 font-semibold">Họ Và Tên Tài Xế</th>
                  <th class="py-3 px-4 font-semibold">Số Điện Thoại</th>
                  <th class="py-3 px-4 font-semibold">Số Bằng Lái (GPLX)</th>
                  <th class="py-3 px-4 font-semibold text-center">Trạng Thái</th>
                  <th class="py-3 px-4 font-semibold text-right">Thao Tác</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-border">
                <tr v-for="d in drivers" :key="d.id" class="hover:bg-muted/30 transition-colors">
                  <td class="py-3.5 px-4 font-bold text-foreground">{{ d.fullName }}</td>
                  <td class="py-3.5 px-4 font-mono text-primary font-medium">{{ d.phone }}</td>
                  <td class="py-3.5 px-4 font-mono text-muted-foreground">{{ d.licenseNumber }}</td>
                  <td class="py-3.5 px-4 text-center">
                    <Badge
                      :variant="d.status === 'ACTIVE' ? 'default' : 'outline'"
                      :class="d.status === 'ACTIVE'
                        ? 'bg-emerald-500/10 text-emerald-600 hover:bg-emerald-500/20 border-emerald-500/20'
                        : 'bg-muted text-muted-foreground'"
                      class="text-[10px] font-semibold"
                    >
                      {{ d.status === 'ACTIVE' ? 'Đang Hoạt Động' : 'Ngưng Chạy' }}
                    </Badge>
                  </td>
                  <td class="py-3.5 px-4 text-right">
                    <Button
                      variant="ghost"
                      size="sm"
                      class="h-7 px-2 text-xs"
                      @click="openEditDriverModal(d)"
                      title="Chỉnh sửa thông tin tài xế"
                    >
                      <Pencil class="w-3.5 h-3.5 mr-1" /> Sửa
                    </Button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </TabsContent>
    </Tabs>

    <!-- MODAL 1: ADD / EDIT VEHICLE (Dialog) -->
    <Dialog :open="isVehicleModalOpen" @update:open="isVehicleModalOpen = $event">
      <DialogContent class="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle class="text-base font-bold text-foreground flex items-center gap-2">
            <Car class="w-4 h-4 text-primary" />
            <span>{{ isEditingVehicle ? 'Cập Nhật Hồ Sơ Phương Tiện' : 'Thêm Phương Tiện Mới Vào Đội Xe' }}</span>
          </DialogTitle>
          <DialogDescription class="text-xs text-muted-foreground">
            Thông tin kỹ thuật cấu hình ghế phục vụ sơ đồ đặt chỗ thời gian thực.
          </DialogDescription>
        </DialogHeader>

        <div v-if="vehicleFormError" class="p-3 bg-destructive/10 border border-destructive/20 rounded-md text-xs text-destructive">
          {{ vehicleFormError }}
        </div>

        <div class="space-y-3.5 py-2">
          <div>
            <label class="text-xs font-semibold text-foreground">Biển số xe đăng ký <span class="text-destructive">*</span></label>
            <Input
              v-model="vehicleForm.plateNumber"
              placeholder="VD: 29B-888.99"
              class="mt-1 font-mono uppercase text-xs font-bold"
              :disabled="isSavingVehicle"
            />
          </div>

          <div>
            <label class="text-xs font-semibold text-foreground">Dòng xe / Model xe <span class="text-destructive">*</span></label>
            <Input
              v-model="vehicleForm.modelName"
              placeholder="VD: Thaco Mobihome 2024"
              class="mt-1 text-xs"
              :disabled="isSavingVehicle"
            />
          </div>

          <div>
            <label class="text-xs font-semibold text-foreground">Phân loại khoang xe</label>
            <select
              v-model="vehicleForm.vehicleType"
              class="mt-1 w-full h-9 px-3 rounded-md border border-input bg-background text-xs text-foreground focus:ring-1 focus:ring-primary focus:outline-none"
              :disabled="isSavingVehicle"
            >
              <option value="SLEEPER">Giường Nằm Cao Cấp (SLEEPER)</option>
              <option value="LIMOUSINE">Limousine VIP Phòng Đôi (LIMOUSINE)</option>
              <option value="SEAT">Ghế Ngồi Tiêu Chuẩn (SEAT)</option>
            </select>
          </div>

          <div class="grid grid-cols-3 gap-3">
            <div>
              <label class="text-xs font-semibold text-foreground">Hàng ghế</label>
              <Input
                v-model.number="vehicleForm.seatRows"
                type="number"
                min="1"
                class="mt-1 text-xs tabular-nums"
                :disabled="isSavingVehicle"
                @input="updateSeatCalculation"
              />
            </div>
            <div>
              <label class="text-xs font-semibold text-foreground">Ghế / Hàng</label>
              <Input
                v-model.number="vehicleForm.seatsPerRow"
                type="number"
                min="1"
                class="mt-1 text-xs tabular-nums"
                :disabled="isSavingVehicle"
                @input="updateSeatCalculation"
              />
            </div>
            <div>
              <label class="text-xs font-semibold text-foreground">Tổng chỗ</label>
              <Input
                v-model.number="vehicleForm.totalSeats"
                type="number"
                class="mt-1 text-xs tabular-nums font-bold"
                disabled
              />
            </div>
          </div>

          <div>
            <label class="text-xs font-semibold text-foreground">Số hàng ghế VIP (Từ hàng đầu)</label>
            <Input
              v-model.number="vehicleForm.vipRows"
              type="number"
              min="0"
              class="mt-1 text-xs tabular-nums"
              :disabled="isSavingVehicle"
            />
            <p class="text-[11px] text-muted-foreground mt-1">
              Các ghế thuộc hàng VIP sẽ được đánh dấu phụ thu giá vé trong cấu hình tuyến.
            </p>
          </div>
        </div>

        <DialogFooter class="gap-2 sm:gap-0">
          <Button variant="outline" size="sm" @click="isVehicleModalOpen = false" :disabled="isSavingVehicle" class="text-xs">
            Hủy Bỏ
          </Button>
          <Button size="sm" @click="handleSaveVehicle" :disabled="isSavingVehicle" class="text-xs">
            <RefreshCw v-if="isSavingVehicle" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
            {{ isEditingVehicle ? 'Lưu Cập Nhật' : 'Lưu Phương Tiện' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- MODAL 2: ADD / EDIT DRIVER (Dialog) -->
    <Dialog :open="isDriverModalOpen" @update:open="isDriverModalOpen = $event">
      <DialogContent class="sm:max-w-[460px]">
        <DialogHeader>
          <DialogTitle class="text-base font-bold text-foreground flex items-center gap-2">
            <Users class="w-4 h-4 text-primary" />
            <span>{{ isEditingDriver ? 'Cập Nhật Hồ Sơ Bác Tài' : 'Thêm Bác Tài Mới' }}</span>
          </DialogTitle>
          <DialogDescription class="text-xs text-muted-foreground">
            Thông tin định danh và số giấy phép lái xe phục vụ điều độ chuyến an toàn.
          </DialogDescription>
        </DialogHeader>

        <div v-if="driverFormError" class="p-3 bg-destructive/10 border border-destructive/20 rounded-md text-xs text-destructive">
          {{ driverFormError }}
        </div>

        <div class="space-y-3.5 py-2">
          <div>
            <label class="text-xs font-semibold text-foreground">Họ và tên tài xế <span class="text-destructive">*</span></label>
            <Input
              v-model="driverForm.fullName"
              placeholder="VD: Nguyễn Văn Tuấn"
              class="mt-1 text-xs font-semibold"
              :disabled="isSavingDriver"
            />
          </div>

          <div>
            <label class="text-xs font-semibold text-foreground">Số điện thoại di động <span class="text-destructive">*</span></label>
            <Input
              v-model="driverForm.phone"
              type="tel"
              placeholder="VD: 0987654321"
              class="mt-1 font-mono text-xs"
              :disabled="isSavingDriver"
            />
          </div>

          <div>
            <label class="text-xs font-semibold text-foreground">Số GPLX (Hạng D / E) <span class="text-destructive">*</span></label>
            <Input
              v-model="driverForm.licenseNumber"
              placeholder="VD: GPLX-790123456"
              class="mt-1 font-mono uppercase text-xs"
              :disabled="isSavingDriver"
            />
          </div>
        </div>

        <DialogFooter class="gap-2 sm:gap-0">
          <Button variant="outline" size="sm" @click="isDriverModalOpen = false" :disabled="isSavingDriver" class="text-xs">
            Hủy Bỏ
          </Button>
          <Button size="sm" @click="handleSaveDriver" :disabled="isSavingDriver" class="text-xs">
            <RefreshCw v-if="isSavingDriver" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
            {{ isEditingDriver ? 'Lưu Cập Nhật' : 'Thêm Tài Xế' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- ALERT DIALOG: VEHICLE STATUS TOGGLE CONFIRMATION -->
    <AlertDialog :open="isStatusConfirmOpen" @update:open="isStatusConfirmOpen = $event">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle class="text-base font-bold text-foreground flex items-center gap-2">
            <AlertTriangle class="w-4 h-4 text-amber-500" />
            <span>Xác nhận thay đổi trạng thái phương tiện</span>
          </AlertDialogTitle>
          <AlertDialogDescription class="text-xs text-muted-foreground space-y-2">
            <p v-if="selectedVehicleForStatus?.status === 'ACTIVE'">
              Bạn có chắc muốn chuyển xe <strong class="text-foreground">{{ selectedVehicleForStatus?.plateNumber }}</strong> sang trạng thái <strong>Bảo Dưỡng</strong>?
              Xe đang bảo dưỡng sẽ tạm thời không thể gắn vào các chuyến mới.
            </p>
            <p v-else>
              Bạn có chắc muốn kích hoạt lại xe <strong class="text-foreground">{{ selectedVehicleForStatus?.plateNumber }}</strong> vào hoạt động?
            </p>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel :disabled="isUpdatingStatus" class="text-xs">Hủy</AlertDialogCancel>
          <AlertDialogAction
            :disabled="isUpdatingStatus"
            class="text-xs bg-primary hover:bg-primary/90 text-primary-foreground"
            @click="handleToggleVehicleStatus"
          >
            <RefreshCw v-if="isUpdatingStatus" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
            Xác Nhận Thay Đổi
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>
