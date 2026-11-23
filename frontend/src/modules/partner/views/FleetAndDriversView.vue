<script setup>
import { ref } from 'vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import FormField from '@/shared/components/FormField.vue'
import AppButton from '@/shared/components/AppButton.vue'

// Current active tab: 'VEHICLES' | 'DRIVERS'
const activeTab = ref('VEHICLES')

// Vehicles List (corresponds to API-PARTNER-03)
const vehicles = ref([
  {
    id: 'VEH-01',
    plate: '29B-888.99',
    type: 'SLEEPER',
    typeName: 'Xe Giường Nằm VIP',
    seatCount: 36,
    seatMapType: '2-DECK-36',
    status: 'ACTIVE',
    inspectionExpiry: '15/06/2027'
  },
  {
    id: 'VEH-02',
    plate: '29B-999.11',
    type: 'LIMOUSINE',
    typeName: 'Limousine Phòng Đôi',
    seatCount: 24,
    seatMapType: '2-DECK-24',
    status: 'ACTIVE',
    inspectionExpiry: '20/12/2026'
  },
  {
    id: 'VEH-03',
    plate: '29B-456.78',
    type: 'SEAT',
    typeName: 'Xe Ghế Ngồi Massage',
    seatCount: 28,
    seatMapType: '1-DECK-28',
    status: 'MAINTENANCE',
    inspectionExpiry: '10/01/2027'
  }
])

// Drivers List (corresponds to API-PARTNER-04)
const drivers = ref([
  {
    id: 'DRV-01',
    fullName: 'Trần Văn Đức',
    phone: '0987654321',
    licenseClass: 'Hạng E (Xe khách > 30 chỗ)',
    experienceYears: 12,
    status: 'ACTIVE'
  },
  {
    id: 'DRV-02',
    fullName: 'Lê Hoàng Nam',
    phone: '0912987654',
    licenseClass: 'Hạng E (Xe khách > 30 chỗ)',
    experienceYears: 8,
    status: 'ACTIVE'
  },
  {
    id: 'DRV-03',
    fullName: 'Nguyễn Tiến Dũng',
    phone: '0933445566',
    licenseClass: 'Hạng D (Xe khách 16-30 chỗ)',
    experienceYears: 5,
    status: 'INACTIVE'
  }
])

// Vehicle Drawer State (520px Right Drawer)
const showVehicleDrawer = ref(false)
const vehicleForm = ref({
  plate: '',
  type: 'SLEEPER',
  seatCount: 36,
  inspectionExpiry: ''
})
const vehicleError = ref('')
const isSavingVehicle = ref(false)

function openVehicleDrawer() {
  vehicleError.value = ''
  vehicleForm.value = {
    plate: '',
    type: 'SLEEPER',
    seatCount: 36,
    inspectionExpiry: '2027-12-31'
  }
  showVehicleDrawer.value = true
}

function closeVehicleDrawer() {
  showVehicleDrawer.value = false
}

function handleSaveVehicle() {
  vehicleError.value = ''

  const plateRegex = /^[0-9]{2}[A-Z]-[0-9]{3}\.[0-9]{2}$/
  if (!vehicleForm.value.plate.trim() || !plateRegex.test(vehicleForm.value.plate.trim())) {
    vehicleError.value = 'Biển số xe không đúng định dạng chuẩn VN (Ví dụ: 29B-888.99)'
    return
  }

  isSavingVehicle.value = true

  setTimeout(() => {
    vehicles.value.unshift({
      id: `VEH-${Math.floor(10 + Math.random() * 90)}`,
      plate: vehicleForm.value.plate.toUpperCase(),
      type: vehicleForm.value.type,
      typeName: vehicleForm.value.type === 'SLEEPER' ? 'Xe Giường Nằm' : 'Limousine Phòng Đôi',
      seatCount: vehicleForm.value.seatCount,
      seatMapType: '2-DECK-36',
      status: 'ACTIVE',
      inspectionExpiry: vehicleForm.value.inspectionExpiry
    })

    isSavingVehicle.value = false
    closeVehicleDrawer()
  }, 800)
}

// Driver Drawer State (520px Right Drawer)
const showDriverDrawer = ref(false)
const driverForm = ref({
  fullName: '',
  phone: '',
  licenseClass: 'Hạng E',
  experienceYears: 5
})
const driverError = ref('')
const isSavingDriver = ref(false)

function openDriverDrawer() {
  driverError.value = ''
  driverForm.value = {
    fullName: '',
    phone: '',
    licenseClass: 'Hạng E (Xe khách > 30 chỗ)',
    experienceYears: 5
  }
  showDriverDrawer.value = true
}

function closeDriverDrawer() {
  showDriverDrawer.value = false
}

function handleSaveDriver() {
  driverError.value = ''

  if (!driverForm.value.fullName.trim()) {
    driverError.value = 'Vui lòng nhập họ tên tài xế'
    return
  }

  const phoneRegex = /(84|0[3|5|7|8|9])+([0-9]{8})\b/
  if (!phoneRegex.test(driverForm.value.phone)) {
    driverError.value = 'Số điện thoại không đúng định dạng VN (10 số)'
    return
  }

  isSavingDriver.value = true

  setTimeout(() => {
    drivers.value.unshift({
      id: `DRV-${Math.floor(10 + Math.random() * 90)}`,
      fullName: driverForm.value.fullName,
      phone: driverForm.value.phone,
      licenseClass: driverForm.value.licenseClass,
      experienceYears: driverForm.value.experienceYears,
      status: 'ACTIVE'
    })

    isSavingDriver.value = false
    closeDriverDrawer()
  }, 800)
}
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-text-primary">Quản Trị Đội Xe & Danh Sách Tài Xế</h1>
        <p class="text-xs text-text-muted mt-1">
          Quản lý hồ sơ phương tiện, sơ đồ ghế 2 tầng, và đội ngũ bác tài đủ điều kiện vận hành chuyến xe.
        </p>
      </div>

      <div class="flex gap-2">
        <button
          v-if="activeTab === 'VEHICLES'"
          type="button"
          @click="openVehicleDrawer"
          data-testid="btn-add-vehicle"
          class="px-4 py-2.5 bg-primary-600 hover:bg-primary-700 text-white font-semibold rounded-lg text-xs shadow-sm flex items-center gap-1.5 transition-all"
        >
          <span>🚌</span>
          <span>+ Thêm Xe Mới</span>
        </button>

        <button
          v-else
          type="button"
          @click="openDriverDrawer"
          data-testid="btn-add-driver"
          class="px-4 py-2.5 bg-primary-600 hover:bg-primary-700 text-white font-semibold rounded-lg text-xs shadow-sm flex items-center gap-1.5 transition-all"
        >
          <span>👨‍✈️</span>
          <span>+ Thêm Tài Xế</span>
        </button>
      </div>
    </div>

    <!-- Tabs Bar -->
    <div class="flex gap-2 border-b border-border-default pb-2 text-xs font-semibold">
      <button
        type="button"
        @click="activeTab = 'VEHICLES'"
        data-testid="tab-vehicles"
        class="px-4 py-2 rounded-lg transition-all"
        :class="activeTab === 'VEHICLES' ? 'bg-brand-navy text-white' : 'text-text-secondary hover:bg-slate-100'"
      >
        Danh Sách Phương Tiện ({{ vehicles.length }})
      </button>

      <button
        type="button"
        @click="activeTab = 'DRIVERS'"
        data-testid="tab-drivers"
        class="px-4 py-2 rounded-lg transition-all"
        :class="activeTab === 'DRIVERS' ? 'bg-brand-navy text-white' : 'text-text-secondary hover:bg-slate-100'"
      >
        Đội Ngũ Tài Xế ({{ drivers.length }})
      </button>
    </div>

    <!-- Tab 1: Vehicles Table -->
    <div v-if="activeTab === 'VEHICLES'" class="bg-surface rounded-xl border border-border-default shadow-card overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse text-xs">
          <thead>
            <tr class="bg-slate-50/80 border-b border-border-default text-text-secondary uppercase tracking-wider text-[11px]">
              <th class="py-3 px-4 font-semibold">Biển Số Xe</th>
              <th class="py-3 px-4 font-semibold">Loại Xe</th>
              <th class="py-3 px-4 font-semibold">Sơ Đồ Ghế</th>
              <th class="py-3 px-4 font-semibold text-center">Số Chỗ</th>
              <th class="py-3 px-4 font-semibold">Hạn Đăng Kiểm</th>
              <th class="py-3 px-4 font-semibold text-center">Trạng Thái</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border-default">
            <tr v-for="v in vehicles" :key="v.id" class="hover:bg-slate-50/60 transition-colors">
              <td class="py-3.5 px-4 font-extrabold text-primary-600 tabular-nums">{{ v.plate }}</td>
              <td class="py-3.5 px-4 font-medium text-text-primary">{{ v.typeName }}</td>
              <td class="py-3.5 px-4 text-text-muted">{{ v.seatMapType }}</td>
              <td class="py-3.5 px-4 text-center font-bold tabular-nums">{{ v.seatCount }} chỗ</td>
              <td class="py-3.5 px-4 text-text-muted tabular-nums">{{ v.inspectionExpiry }}</td>
              <td class="py-3.5 px-4 text-center">
                <StatusBadge :status="v.status" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Tab 2: Drivers Table -->
    <div v-else class="bg-surface rounded-xl border border-border-default shadow-card overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse text-xs">
          <thead>
            <tr class="bg-slate-50/80 border-b border-border-default text-text-secondary uppercase tracking-wider text-[11px]">
              <th class="py-3 px-4 font-semibold">Họ Và Tên</th>
              <th class="py-3 px-4 font-semibold">Số Điện Thoại</th>
              <th class="py-3 px-4 font-semibold">Hạng Bằng Lái</th>
              <th class="py-3 px-4 font-semibold text-center">Kinh Nghiệm</th>
              <th class="py-3 px-4 font-semibold text-center">Trạng Thái</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border-default">
            <tr v-for="d in drivers" :key="d.id" class="hover:bg-slate-50/60 transition-colors">
              <td class="py-3.5 px-4 font-bold text-text-primary">{{ d.fullName }}</td>
              <td class="py-3.5 px-4 text-primary-600 font-medium tabular-nums">{{ d.phone }}</td>
              <td class="py-3.5 px-4 text-text-secondary">{{ d.licenseClass }}</td>
              <td class="py-3.5 px-4 text-center font-bold tabular-nums">{{ d.experienceYears }} năm</td>
              <td class="py-3.5 px-4 text-center">
                <StatusBadge :status="d.status" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Right-Side Drawer (520px) Thêm Xe Mới -->
    <div
      v-if="showVehicleDrawer"
      class="fixed inset-0 bg-black/50 z-50 flex justify-end"
    >
      <div class="bg-surface w-full max-w-[520px] h-full shadow-2xl flex flex-col justify-between">
        <!-- Drawer Header -->
        <div class="p-5 border-b border-border-default flex items-center justify-between bg-slate-50">
          <div>
            <h2 class="text-base font-bold text-text-primary flex items-center gap-2">
              <span>🚌</span>
              <span>Thêm Phương Tiện Mới Vào Đội Xe</span>
            </h2>
            <p class="text-[11px] text-text-muted mt-0.5">Quy chuẩn biển số và kiểm định Bộ Giao Thông Vận Tải</p>
          </div>
          <button @click="closeVehicleDrawer" class="text-slate-400 hover:text-slate-600 text-lg">✕</button>
        </div>

        <!-- Drawer Body (Cuộn độc lập) -->
        <div class="p-6 overflow-y-auto space-y-4 flex-1">
          <FormField
            label="Biển số đăng ký xe"
            :error="vehicleError"
            required
          >
            <input
              v-model="vehicleForm.plate"
              type="text"
              placeholder="Ví dụ: 29B-888.99"
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs font-bold text-text-primary focus:ring-2 focus:ring-primary-600 uppercase tabular-nums"
              :class="{ 'border-rose-500': vehicleError }"
            />
          </FormField>

          <FormField label="Phân loại phương tiện" required>
            <select
              v-model="vehicleForm.type"
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 bg-white"
            >
              <option value="SLEEPER">Xe Giường Nằm 36 Chỗ (2 Tầng)</option>
              <option value="LIMOUSINE">Xe Limousine VIP 24 Phòng (2 Tầng)</option>
              <option value="SEAT">Xe Ghế Ngồi Massage 28 Chỗ</option>
            </select>
          </FormField>

          <FormField label="Số lượng chỗ ngồi / giường nằm" required>
            <input
              v-model.number="vehicleForm.seatCount"
              type="number"
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 tabular-nums font-bold"
            />
          </FormField>

          <FormField label="Hạn đăng kiểm phương tiện" required>
            <input
              v-model="vehicleForm.inspectionExpiry"
              type="date"
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600"
            />
          </FormField>
        </div>

        <!-- Drawer Footer (Dính cố định) -->
        <div class="p-5 border-t border-border-default bg-slate-50 flex items-center justify-end gap-3">
          <button
            type="button"
            @click="closeVehicleDrawer"
            class="px-4 py-2.5 border border-border-strong hover:bg-slate-100 text-text-secondary text-xs font-semibold rounded-lg transition-all"
          >
            Hủy Bỏ
          </button>

          <AppButton
            variant="primary"
            class="px-5 py-2.5 text-xs font-bold"
            :loading="isSavingVehicle"
            @click="handleSaveVehicle"
          >
            Lưu Phương Tiện
          </AppButton>
        </div>
      </div>
    </div>

    <!-- Right-Side Drawer (520px) Thêm Tài Xế Mới -->
    <div
      v-if="showDriverDrawer"
      class="fixed inset-0 bg-black/50 z-50 flex justify-end"
    >
      <div class="bg-surface w-full max-w-[520px] h-full shadow-2xl flex flex-col justify-between">
        <div class="p-5 border-b border-border-default flex items-center justify-between bg-slate-50">
          <div>
            <h2 class="text-base font-bold text-text-primary flex items-center gap-2">
              <span>👨‍✈️</span>
              <span>Thêm Tài Xế Mới</span>
            </h2>
            <p class="text-[11px] text-text-muted mt-0.5">Xác thực bằng lái hạng D/E và số điện thoại liên hệ</p>
          </div>
          <button @click="closeDriverDrawer" class="text-slate-400 hover:text-slate-600 text-lg">✕</button>
        </div>

        <div class="p-6 overflow-y-auto space-y-4 flex-1">
          <FormField
            label="Họ và tên bác tài"
            :error="driverError"
            required
          >
            <input
              v-model="driverForm.fullName"
              type="text"
              placeholder="Ví dụ: Trần Văn Đức"
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600"
              :class="{ 'border-rose-500': driverError }"
            />
          </FormField>

          <FormField label="Số điện thoại di động" required>
            <input
              v-model="driverForm.phone"
              type="tel"
              placeholder="0987654321"
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 tabular-nums"
            />
          </FormField>

          <FormField label="Hạng giấy phép lái xe" required>
            <select
              v-model="driverForm.licenseClass"
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 bg-white"
            >
              <option value="Hạng E (Xe khách > 30 chỗ)">Hạng E (Xe khách trên 30 chỗ ngồi/giường)</option>
              <option value="Hạng D (Xe khách 16-30 chỗ)">Hạng D (Xe khách 16 đến 30 chỗ ngồi)</option>
            </select>
          </FormField>

          <FormField label="Số năm kinh nghiệm lái xe đường dài" required>
            <input
              v-model.number="driverForm.experienceYears"
              type="number"
              min="1"
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 tabular-nums"
            />
          </FormField>
        </div>

        <div class="p-5 border-t border-border-default bg-slate-50 flex items-center justify-end gap-3">
          <button
            type="button"
            @click="closeDriverDrawer"
            class="px-4 py-2.5 border border-border-strong hover:bg-slate-100 text-text-secondary text-xs font-semibold rounded-lg transition-all"
          >
            Hủy Bỏ
          </button>

          <AppButton
            variant="primary"
            class="px-5 py-2.5 text-xs font-bold"
            :loading="isSavingDriver"
            @click="handleSaveDriver"
          >
            Lưu Hồ Sơ Tài Xế
          </AppButton>
        </div>
      </div>
    </div>
  </div>
</template>
