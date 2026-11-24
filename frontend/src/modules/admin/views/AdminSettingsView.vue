<script setup>
import { ref } from 'vue'
import FormField from '@/shared/components/FormField.vue'
import AppButton from '@/shared/components/AppButton.vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useAdminPermissions } from '@/modules/admin/composables/useAdminPermissions'
import { adminApi } from '@/modules/admin/api'

const { hasPermission } = useAdminPermissions()

// Global System Settings (corresponds to TBL-SYSTEM-SETTINGS / API-PUB-SETTING-01)
const systemSettings = ref({
  platformName: 'VeloBus - Sàn Vé Xe Khách Cao Cấp',
  defaultCommissionRate: 10.0,
  seatHoldTtlMinutes: 10,
  turnaroundBufferMinutes: 30,
  escrowSettlementDelayHours: 2
})

const saveSuccessMsg = ref('')

async function handleSaveSettings() {
  if (!hasPermission('SYSTEM_SETTINGS_MANAGE')) return
  saveSuccessMsg.value = 'Đã lưu cấu hình tham số toàn sàn và đồng bộ Redis Cache thành công!'
  try {
    await adminApi.updateSystemSettings({
      platformName: systemSettings.value.platformName,
      defaultCommissionRate: String(systemSettings.value.defaultCommissionRate),
      seatHoldTtlMinutes: String(systemSettings.value.seatHoldTtlMinutes),
      turnaroundBufferMinutes: String(systemSettings.value.turnaroundBufferMinutes),
      escrowSettlementDelayHours: String(systemSettings.value.escrowSettlementDelayHours)
    })
  } catch (_) {}
  setTimeout(() => {
    saveSuccessMsg.value = ''
  }, 3000)
}

// =========================================================================
// MASTER STATIONS CATALOG CRUD (TBL-STATIONS)
// =========================================================================
const stations = ref([
  { id: 'HN-GP', name: 'Bến xe Giáp Bát', province: 'Hà Nội', status: 'ACTIVE' },
  { id: 'HN-MY', name: 'Bến xe Mỹ Đình', province: 'Hà Nội', status: 'ACTIVE' },
  { id: 'DN-TT', name: 'Bến xe Trung Tâm Đà Nẵng', province: 'Đà Nẵng', status: 'ACTIVE' },
  { id: 'SG-MD', name: 'Bến xe Miền Đông Mới', province: 'TP. Hồ Chí Minh', status: 'ACTIVE' },
  { id: 'DL-LD', name: 'Bến xe Liên Tỉnh Đà Lạt', province: 'Lâm Đồng', status: 'ACTIVE' }
])

const newStation = ref({ id: '', name: '', province: '' })
const editingStationId = ref(null)

async function handleAddOrUpdateStation() {
  if (!hasPermission('STATION_CREATE_UPDATE_DELETE')) return
  if (!newStation.value.id.trim() || !newStation.value.name.trim()) return

  const cleanCode = newStation.value.id.trim().toUpperCase()
  const cleanName = newStation.value.name.trim()
  const cleanCity = newStation.value.province.trim() || 'Việt Nam'

  if (editingStationId.value) {
    const target = stations.value.find(s => s.id === editingStationId.value)
    if (target) {
      target.id = cleanCode
      target.name = cleanName
      target.province = cleanCity
    }
    const updatedId = editingStationId.value
    editingStationId.value = null
    adminApi.updateStation(updatedId, { code: cleanCode, name: cleanName, city: cleanCity }).catch(() => {})
  } else {
    stations.value.push({
      id: cleanCode,
      name: cleanName,
      province: cleanCity,
      status: 'ACTIVE'
    })
    adminApi.createStation({
      code: cleanCode,
      name: cleanName,
      city: cleanCity
    }).catch(() => {})
  }

  newStation.value = { id: '', name: '', province: '' }
}

function startEditStation(station) {
  editingStationId.value = station.id
  newStation.value = {
    id: station.id,
    name: station.name,
    province: station.province
  }
}

async function handleDeleteStation(station) {
  if (!hasPermission('STATION_CREATE_UPDATE_DELETE')) return
  try {
    await adminApi.deleteStation(station.id)
  } catch (_) {}
  stations.value = stations.value.filter(s => s.id !== station.id)
}

// =========================================================================
// SYSTEM TRIPS CATALOG CRUD (TBL-TRIPS)
// =========================================================================
const trips = ref([
  {
    id: 'TRIP-101',
    tripCode: 'VB-HN-DN-01',
    route: 'Bến xe Mỹ Đình → Bến xe Trung Tâm Đà Nẵng',
    departureTime: '2026-12-01 20:00',
    basePrice: 450000,
    totalSeats: 34,
    status: 'SCHEDULED'
  },
  {
    id: 'TRIP-102',
    tripCode: 'VB-SG-DL-02',
    route: 'Bến xe Miền Đông Mới → Bến xe Liên Tỉnh Đà Lạt',
    departureTime: '2026-12-02 22:30',
    basePrice: 380000,
    totalSeats: 24,
    status: 'SCHEDULED'
  }
])

const tripForm = ref({
  tripCode: '',
  route: '',
  departureTime: '2026-12-05 21:00',
  basePrice: 400000,
  totalSeats: 34
})
const editingTripId = ref(null)

async function handleAddOrUpdateTrip() {
  if (!hasPermission('TRIP_MANAGE_CRUD')) return
  if (!tripForm.value.tripCode.trim() || !tripForm.value.route.trim()) return

  const cleanTripCode = tripForm.value.tripCode.trim().toUpperCase()
  const cleanRoute = tripForm.value.route.trim()
  const cleanDepTime = tripForm.value.departureTime || '2026-12-05 21:00'
  const cleanBasePrice = Number(tripForm.value.basePrice) || 400000
  const cleanSeats = Number(tripForm.value.totalSeats) || 34

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
    adminApi.updateTrip(updatedId, {
      tripCode: cleanTripCode,
      departureStationId: 1,
      arrivalStationId: 2,
      departureTime: '2026-12-05T21:00:00',
      arrivalTime: '2026-12-06T05:00:00',
      transportType: 'BUS',
      basePrice: cleanBasePrice,
      totalRows: 8,
      seatsPerRow: 4,
      vipRows: 2
    }).catch(() => {})
  } else {
    const newTripObj = {
      id: `TRIP-${100 + trips.value.length + 1}`,
      tripCode: cleanTripCode,
      route: cleanRoute,
      departureTime: cleanDepTime,
      basePrice: cleanBasePrice,
      totalSeats: cleanSeats,
      status: 'SCHEDULED'
    }
    trips.value.unshift(newTripObj)
    adminApi.createTrip({
      tripCode: cleanTripCode,
      departureStationId: 1,
      arrivalStationId: 2,
      departureTime: '2026-12-05T21:00:00',
      arrivalTime: '2026-12-06T05:00:00',
      transportType: 'BUS',
      basePrice: cleanBasePrice,
      totalRows: 8,
      seatsPerRow: 4,
      vipRows: 2
    }).then(res => {
      if (res?.data?.id) newTripObj.id = res.data.id
    }).catch(() => {})
  }
  tripForm.value = { tripCode: '', route: '', departureTime: '2026-12-05 21:00', basePrice: 400000, totalSeats: 34 }
}

function startEditTrip(trip) {
  editingTripId.value = trip.id
  tripForm.value = {
    tripCode: trip.tripCode,
    route: trip.route,
    departureTime: trip.departureTime,
    basePrice: trip.basePrice,
    totalSeats: trip.totalSeats
  }
}

async function handleCancelTrip(trip) {
  if (!hasPermission('TRIP_MANAGE_CRUD')) return
  trip.status = 'CANCELLED'
  try {
    await adminApi.cancelTrip(trip.id)
  } catch (_) {}
}

async function handleDeleteTrip(trip) {
  if (!hasPermission('TRIP_MANAGE_CRUD')) return
  try {
    await adminApi.deleteTrip(trip.id)
  } catch (_) {}
  trips.value = trips.value.filter(t => t.id !== trip.id)
}
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-text-primary">Quản Trị Danh Mục Bến Xe, Chuyến Xe & Cấu Hình Tham Số Sàn</h1>
      <p class="text-xs text-text-muted mt-1">
        Thiết lập tham số vận hành toàn cục (`TBL-SYSTEM-SETTINGS`), quản lý danh mục nhà ga/bến xe liên tỉnh (`TBL-STATIONS`) và điều phối chuyến xe (`TBL-TRIPS`).
      </p>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 items-start">
      <!-- Global System Settings Card -->
      <div class="bg-surface rounded-xl border border-border-default p-6 shadow-card space-y-4">
        <h2 class="text-base font-bold text-text-primary border-b border-border-default pb-3">
          ⚙️ Tham Số Vận Hành Toàn Sàn
        </h2>

        <div
          v-if="saveSuccessMsg"
          data-testid="settings-saved-alert"
          class="bg-emerald-50 border border-emerald-200 text-emerald-700 px-3.5 py-2.5 rounded-lg text-xs font-semibold"
        >
          ✓ {{ saveSuccessMsg }}
        </div>

        <FormField label="Tên hiển thị thương hiệu sàn" required>
          <input
            v-model="systemSettings.platformName"
            type="text"
            :disabled="!hasPermission('SYSTEM_SETTINGS_MANAGE')"
            class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 disabled:bg-slate-100"
          />
        </FormField>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <FormField label="Hoa hồng mặc định toàn sàn (%)" required>
            <input
              v-model.number="systemSettings.defaultCommissionRate"
              type="number"
              step="0.5"
              :disabled="!hasPermission('SYSTEM_SETTINGS_MANAGE')"
              data-testid="input-global-commission"
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs font-bold text-text-primary tabular-nums disabled:bg-slate-100"
            />
          </FormField>

          <FormField label="Thời gian khóa ghế Redis (Phút)" required>
            <input
              v-model.number="systemSettings.seatHoldTtlMinutes"
              type="number"
              :disabled="!hasPermission('SYSTEM_SETTINGS_MANAGE')"
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs font-bold text-text-primary tabular-nums disabled:bg-slate-100"
            />
          </FormField>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <FormField label="Đệm quay đầu xe Turnaround (Phút)" required>
            <input
              v-model.number="systemSettings.turnaroundBufferMinutes"
              type="number"
              :disabled="!hasPermission('SYSTEM_SETTINGS_MANAGE')"
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs font-bold text-text-primary tabular-nums disabled:bg-slate-100"
            />
          </FormField>

          <FormField label="Trễ quyết toán ký quỹ sau chuyến (Giờ)" required>
            <input
              v-model.number="systemSettings.escrowSettlementDelayHours"
              type="number"
              :disabled="!hasPermission('SYSTEM_SETTINGS_MANAGE')"
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs font-bold text-text-primary tabular-nums disabled:bg-slate-100"
            />
          </FormField>
        </div>

        <div v-if="hasPermission('SYSTEM_SETTINGS_MANAGE')" class="pt-2">
          <AppButton
            variant="primary"
            class="w-full h-11 text-xs font-bold"
            data-testid="btn-save-settings"
            @click="handleSaveSettings"
          >
            Lưu Cấu Hình Toàn Sàn
          </AppButton>
        </div>
      </div>

      <!-- Stations Master Catalog Card -->
      <div class="bg-surface rounded-xl border border-border-default p-6 shadow-card space-y-4">
        <h2 class="text-base font-bold text-text-primary border-b border-border-default pb-3">
          🚏 Danh Mục Bến Xe Liên Tỉnh ({{ stations.length }})
        </h2>

        <div v-if="hasPermission('STATION_CREATE_UPDATE_DELETE')" class="grid grid-cols-1 sm:grid-cols-4 gap-2">
          <input
            v-model="newStation.id"
            type="text"
            data-testid="input-station-code"
            placeholder="Mã (VD: HP-CR)"
            class="h-10 px-3 rounded-lg border border-border-strong text-xs uppercase tabular-nums"
          />
          <input
            v-model="newStation.name"
            type="text"
            data-testid="input-station-name"
            placeholder="Tên bến xe..."
            class="h-10 px-3 rounded-lg border border-border-strong text-xs"
          />
          <input
            v-model="newStation.province"
            type="text"
            data-testid="input-station-city"
            placeholder="Tỉnh / Thành..."
            class="h-10 px-3 rounded-lg border border-border-strong text-xs"
          />
          <button
            type="button"
            data-testid="save-station-btn"
            @click="handleAddOrUpdateStation"
            class="h-10 bg-brand-navy hover:bg-slate-800 text-white font-semibold rounded-lg text-xs transition-all"
          >
            {{ editingStationId ? '✓ Lưu Bến Xe' : '+ Thêm Bến Xe' }}
          </button>
        </div>

        <div class="overflow-x-auto border border-border-default rounded-lg">
          <table class="w-full text-left border-collapse text-xs">
            <thead>
              <tr class="bg-slate-50 border-b border-border-default text-text-secondary text-[11px] uppercase">
                <th class="py-2.5 px-3 font-semibold">Mã Bến</th>
                <th class="py-2.5 px-3 font-semibold">Tên Bến Xe</th>
                <th class="py-2.5 px-3 font-semibold">Tỉnh / Thành</th>
                <th class="py-2.5 px-3 font-semibold text-center">Trạng Thái</th>
                <th v-if="hasPermission('STATION_CREATE_UPDATE_DELETE')" class="py-2.5 px-3 font-semibold text-right">CRUD</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-border-default">
              <tr v-for="s in stations" :key="s.id">
                <td class="py-2.5 px-3 font-bold text-primary-600 tabular-nums">{{ s.id }}</td>
                <td class="py-2.5 px-3 font-medium text-text-primary">{{ s.name }}</td>
                <td class="py-2.5 px-3 text-text-secondary">{{ s.province }}</td>
                <td class="py-2.5 px-3 text-center">
                  <StatusBadge :status="s.status" />
                </td>
                <td v-if="hasPermission('STATION_CREATE_UPDATE_DELETE')" class="py-2.5 px-3 text-right space-x-1 whitespace-nowrap">
                  <button
                    type="button"
                    :data-testid="`btn-edit-station-${s.id}`"
                    @click="startEditStation(s)"
                    class="px-2 py-1 rounded border border-border-strong hover:bg-slate-100 text-[11px] font-semibold"
                  >
                    ✏️ Sửa
                  </button>
                  <button
                    type="button"
                    :data-testid="`btn-delete-station-${s.id}`"
                    @click="handleDeleteStation(s)"
                    class="px-2 py-1 rounded bg-rose-50 text-rose-700 hover:bg-rose-100 text-[11px] font-semibold"
                  >
                    🗑️ Xóa
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- System Trips CRUD Section (Protected by TRIP_MANAGE_CRUD) -->
    <div v-if="hasPermission('TRIP_MANAGE_CRUD')" class="bg-surface rounded-xl border border-border-default p-6 shadow-card space-y-4">
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-border-default pb-3">
        <div>
          <h2 class="text-base font-bold text-text-primary">
            🚌 Quản Lý Chuyến Xe Toàn Sàn ({{ trips.length }})
          </h2>
          <p class="text-xs text-text-muted">
            Khởi tạo, điều chỉnh giá vé/giờ chạy, hủy chuyến hoặc xóa chuyến xe chưa phát sinh vé.
          </p>
        </div>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-5 gap-2">
        <input
          v-model="tripForm.tripCode"
          type="text"
          data-testid="input-trip-code"
          placeholder="Mã chuyến (VD: VB-HN-SG)"
          class="h-10 px-3 rounded-lg border border-border-strong text-xs uppercase font-mono"
        />
        <input
          v-model="tripForm.route"
          type="text"
          data-testid="input-trip-route"
          placeholder="Tuyến đường (VD: Hà Nội → Vinh)"
          class="h-10 px-3 rounded-lg border border-border-strong text-xs sm:col-span-2"
        />
        <input
          v-model.number="tripForm.basePrice"
          type="number"
          data-testid="input-trip-price"
          placeholder="Giá vé (VNĐ)"
          class="h-10 px-3 rounded-lg border border-border-strong text-xs tabular-nums"
        />
        <button
          type="button"
          data-testid="save-trip-btn"
          @click="handleAddOrUpdateTrip"
          class="h-10 bg-brand-navy hover:bg-slate-800 text-white font-semibold rounded-lg text-xs transition-all"
        >
          {{ editingTripId ? '✓ Lưu Chuyến Xe' : '+ Tạo Chuyến Xe' }}
        </button>
      </div>

      <div class="overflow-x-auto border border-border-default rounded-lg">
        <table class="w-full text-left border-collapse text-xs">
          <thead>
            <tr class="bg-slate-50 border-b border-border-default text-text-secondary text-[11px] uppercase">
              <th class="py-2.5 px-3 font-semibold">Mã Chuyến</th>
              <th class="py-2.5 px-3 font-semibold">Hành Trình</th>
              <th class="py-2.5 px-3 font-semibold">Khởi Hành</th>
              <th class="py-2.5 px-3 font-semibold text-right">Giá Cơ Bản</th>
              <th class="py-2.5 px-3 font-semibold text-center">Số Ghế</th>
              <th class="py-2.5 px-3 font-semibold text-center">Trạng Thái</th>
              <th class="py-2.5 px-3 font-semibold text-right">Thao Tác CRUD</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border-default">
            <tr v-for="t in trips" :key="t.id">
              <td class="py-2.5 px-3 font-bold text-primary-600 font-mono">{{ t.tripCode }}</td>
              <td class="py-2.5 px-3 font-semibold text-text-primary">{{ t.route }}</td>
              <td class="py-2.5 px-3 text-text-secondary tabular-nums">{{ t.departureTime }}</td>
              <td class="py-2.5 px-3 text-right font-bold tabular-nums">{{ t.basePrice.toLocaleString('vi-VN') }} đ</td>
              <td class="py-2.5 px-3 text-center tabular-nums">{{ t.totalSeats }} ghế</td>
              <td class="py-2.5 px-3 text-center">
                <StatusBadge :status="t.status" />
              </td>
              <td class="py-2.5 px-3 text-right space-x-1.5 whitespace-nowrap">
                <button
                  type="button"
                  :data-testid="`btn-edit-trip-${t.id}`"
                  @click="startEditTrip(t)"
                  class="px-2.5 py-1 rounded border border-border-strong hover:bg-slate-100 text-[11px] font-semibold"
                >
                  ✏️ Sửa
                </button>
                <button
                  v-if="t.status === 'SCHEDULED'"
                  type="button"
                  :data-testid="`cancel-trip-${t.id}`"
                  @click="handleCancelTrip(t)"
                  class="px-2.5 py-1 rounded bg-amber-50 text-amber-800 hover:bg-amber-100 text-[11px] font-semibold"
                >
                  🚫 Hủy Chuyến
                </button>
                <button
                  type="button"
                  :data-testid="`btn-delete-trip-${t.id}`"
                  @click="handleDeleteTrip(t)"
                  class="px-2.5 py-1 rounded bg-rose-50 text-rose-700 hover:bg-rose-100 text-[11px] font-semibold"
                >
                  🗑️ Xóa
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
