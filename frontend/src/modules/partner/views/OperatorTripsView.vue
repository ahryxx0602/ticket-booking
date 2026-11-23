<script setup>
import { ref, computed } from 'vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import FormField from '@/shared/components/FormField.vue'
import AppButton from '@/shared/components/AppButton.vue'

// Trips List (corresponds to API-PARTNER-06 / API-CAT-01)
const trips = ref([
  {
    id: 'TRIP-01',
    route: 'Hà Nội (Giáp Bát) → Đà Nẵng (Trung Tâm)',
    departureTime: '2026-11-23 19:00',
    arrivalTime: '2026-11-24 09:00',
    vehiclePlate: '29B-888.99',
    driverName: 'Trần Văn Đức',
    price: 350000,
    status: 'ACTIVE',
    settlementStatus: 'UNSETTLED'
  },
  {
    id: 'TRIP-04',
    route: 'Hà Nội (Giáp Bát) → Đà Nẵng (Trung Tâm)',
    departureTime: '2026-11-24 14:00',
    arrivalTime: '2026-11-25 04:00',
    vehiclePlate: '29B-888.99',
    driverName: 'Lê Hoàng Nam',
    price: 320000,
    status: 'ACTIVE',
    settlementStatus: 'UNSETTLED'
  },
  {
    id: 'TRIP-99',
    route: 'Hà Nội (Giáp Bát) → Hải Phòng (Cầu Rào)',
    departureTime: '2026-11-20 08:00',
    arrivalTime: '2026-11-20 10:00',
    vehiclePlate: '29B-456.78',
    driverName: 'Trần Văn Đức',
    price: 180000,
    status: 'COMPLETED',
    settlementStatus: 'SETTLED'
  }
])

// Vehicles and Drivers for dropdowns
const availableVehicles = ref([
  { plate: '29B-888.99', name: '29B-888.99 (Giường Nằm 36 Chỗ)' },
  { plate: '29B-999.11', name: '29B-999.11 (Limousine 24 Phòng)' },
  { plate: '29B-456.78', name: '29B-456.78 (Ghế Ngồi 28 Chỗ)' }
])

const availableDrivers = ref([
  { id: 'DRV-01', name: 'Trần Văn Đức (Hạng E - 12 năm kn)' },
  { id: 'DRV-02', name: 'Lê Hoàng Nam (Hạng E - 8 năm kn)' }
])

// Schedule Drawer State (680px Right Drawer)
const showScheduleDrawer = ref(false)
const tripForm = ref({
  route: 'Hà Nội (Giáp Bát) → Đà Nẵng (Trung Tâm)',
  vehiclePlate: '29B-888.99',
  driverName: 'Trần Văn Đức',
  departureTime: '2026-11-24 09:15', // Cố ý để giờ vi phạm buffer (chuyến trước đến lúc 09:00)
  arrivalTime: '2026-11-24 23:15',
  price: 350000
})

const isScheduling = ref(false)

// CORE BUSINESS INVARIANT: Turnaround Buffer 30 minutes check
const turnaroundBufferViolation = computed(() => {
  if (!tripForm.value.vehiclePlate || !tripForm.value.departureTime) return null

  const newDep = new Date(tripForm.value.departureTime.replace(' ', 'T')).getTime()

  for (const existing of trips.value) {
    if (existing.vehiclePlate === tripForm.value.vehiclePlate) {
      const existingArr = new Date(existing.arrivalTime.replace(' ', 'T')).getTime()
      const existingDep = new Date(existing.departureTime.replace(' ', 'T')).getTime()

      // Turnaround buffer: new departure must be at least 30 mins (1800000 ms) AFTER previous arrival
      if (newDep >= existingDep && newDep < existingArr + 30 * 60 * 1000) {
        const requiredTime = new Date(existingArr + 30 * 60 * 1000)
        const hh = String(requiredTime.getHours()).padStart(2, '0')
        const mm = String(requiredTime.getMinutes()).padStart(2, '0')
        return `Xe ${existing.vehiclePlate} hoàn thành chuyến trước lúc ${existing.arrivalTime.split(' ')[1]}. Cần thời gian đệm bảo dưỡng/vệ sinh tối thiểu 30 phút (sớm nhất có thể xuất bến là ${hh}:${mm})!`
      }
    }
  }

  return null
})

function openScheduleDrawer() {
  tripForm.value = {
    route: 'Hà Nội (Giáp Bát) → Đà Nẵng (Trung Tâm)',
    vehiclePlate: '29B-888.99',
    driverName: 'Trần Văn Đức',
    departureTime: '2026-11-24 10:00', // Giờ an toàn (sau 09:00 + 30p)
    arrivalTime: '2026-11-25 00:00',
    price: 350000
  }
  showScheduleDrawer.value = true
}

function closeScheduleDrawer() {
  showScheduleDrawer.value = false
}

function handleScheduleTrip() {
  if (turnaroundBufferViolation.value) return

  isScheduling.value = true

  setTimeout(() => {
    trips.value.unshift({
      id: `TRIP-${Math.floor(10 + Math.random() * 90)}`,
      route: tripForm.value.route,
      departureTime: tripForm.value.departureTime,
      arrivalTime: tripForm.value.arrivalTime,
      vehiclePlate: tripForm.value.vehiclePlate,
      driverName: tripForm.value.driverName,
      price: tripForm.value.price,
      status: 'ACTIVE',
      settlementStatus: 'UNSETTLED'
    })

    isScheduling.value = false
    closeScheduleDrawer()
  }, 800)
}
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-text-primary">Lập Lịch Chuyến Xe & Quản Lý Giờ Chạy</h1>
        <p class="text-xs text-text-muted mt-1">
          Hệ thống kiểm soát lịch trình thông minh: Chặn trùng lịch & Tự động áp dụng Turnaround Buffer 30 phút giữa các chuyến.
        </p>
      </div>

      <button
        type="button"
        @click="openScheduleDrawer"
        data-testid="btn-open-schedule"
        class="self-start sm:self-auto px-4 py-2.5 bg-primary-600 hover:bg-primary-700 text-white font-semibold rounded-lg text-xs shadow-sm flex items-center gap-1.5 transition-all"
      >
        <span>📅</span>
        <span>+ Lập Chuyến Mới</span>
      </button>
    </div>

    <!-- Trips DataTable -->
    <div class="bg-surface rounded-xl border border-border-default shadow-card overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse text-xs">
          <thead>
            <tr class="bg-slate-50/80 border-b border-border-default text-text-secondary uppercase tracking-wider text-[11px]">
              <th class="py-3 px-4 font-semibold">Mã Chuyến</th>
              <th class="py-3 px-4 font-semibold">Tuyến Đường</th>
              <th class="py-3 px-4 font-semibold">Giờ Khởi Hành</th>
              <th class="py-3 px-4 font-semibold">Giờ Dự Kiến Đến</th>
              <th class="py-3 px-4 font-semibold">Biển Số Xe</th>
              <th class="py-3 px-4 font-semibold">Bác Tài</th>
              <th class="py-3 px-4 font-semibold text-right">Giá Vé Niêm Yết</th>
              <th class="py-3 px-4 font-semibold text-center">Trạng Thái</th>
              <th class="py-3 px-4 font-semibold text-center">Đối Soát Ví</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border-default">
            <tr v-for="t in trips" :key="t.id" class="hover:bg-slate-50/60 transition-colors">
              <td class="py-3.5 px-4 font-bold text-primary-600 tabular-nums">{{ t.id }}</td>
              <td class="py-3.5 px-4 font-bold text-text-primary">{{ t.route }}</td>
              <td class="py-3.5 px-4 text-emerald-700 font-medium tabular-nums">{{ t.departureTime }}</td>
              <td class="py-3.5 px-4 text-text-muted tabular-nums">{{ t.arrivalTime }}</td>
              <td class="py-3.5 px-4 font-bold text-text-primary tabular-nums">{{ t.vehiclePlate }}</td>
              <td class="py-3.5 px-4 text-text-secondary">{{ t.driverName }}</td>
              <td class="py-3.5 px-4 text-right font-extrabold text-primary-600 tabular-nums">
                {{ t.price.toLocaleString('vi-VN') }} đ
              </td>
              <td class="py-3.5 px-4 text-center">
                <StatusBadge :status="t.status" />
              </td>
              <td class="py-3.5 px-4 text-center">
                <span
                  class="px-2 py-0.5 rounded text-[11px] font-semibold"
                  :class="t.settlementStatus === 'SETTLED' ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-amber-50 text-amber-700 border border-amber-200'"
                >
                  {{ t.settlementStatus === 'SETTLED' ? 'Đã Quyết Toán' : 'Chưa Quyết Toán' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Right-Side Drawer (680px) Tạo Chuyến Đi Mới -->
    <div
      v-if="showScheduleDrawer"
      class="fixed inset-0 bg-black/50 z-50 flex justify-end"
    >
      <div class="bg-surface w-full max-w-[680px] h-full shadow-2xl flex flex-col justify-between">
        <!-- Drawer Header -->
        <div class="p-5 border-b border-border-default flex items-center justify-between bg-slate-50">
          <div>
            <h2 class="text-base font-bold text-text-primary flex items-center gap-2">
              <span>📅</span>
              <span>Lập Lịch Chuyến Xe Mới (30-Min Buffer Check)</span>
            </h2>
            <p class="text-[11px] text-text-muted mt-0.5">Tự động đối chiếu lịch chạy của phương tiện và tài xế để chống trùng lặp</p>
          </div>
          <button @click="closeScheduleDrawer" class="text-slate-400 hover:text-slate-600 text-lg">✕</button>
        </div>

        <!-- Drawer Body -->
        <div class="p-6 overflow-y-auto space-y-4 flex-1">
          <!-- Turnaround Buffer Warning Alert (Hiển thị nếu vi phạm 30p) -->
          <div
            v-if="turnaroundBufferViolation"
            class="bg-rose-50 border border-rose-300 rounded-xl p-4 flex items-start gap-3 shadow-sm"
            data-testid="buffer-violation-alert"
          >
            <span class="text-xl">⚠️</span>
            <div class="space-y-1">
              <h4 class="text-xs font-bold text-rose-800 uppercase tracking-wider">Vi Phạm Quy Chuẩn Đệm Quay Đầu 30 Phút</h4>
              <p class="text-xs text-rose-700 leading-relaxed">{{ turnaroundBufferViolation }}</p>
            </div>
          </div>

          <FormField label="Tuyến đường vận hành" required>
            <select
              v-model="tripForm.route"
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 bg-white"
            >
              <option value="Hà Nội (Giáp Bát) → Đà Nẵng (Trung Tâm)">Hà Nội (Giáp Bát) → Đà Nẵng (Trung Tâm)</option>
              <option value="Sài Gòn (Miền Đông) → Đà Lạt (Liên Tỉnh)">Sài Gòn (Miền Đông) → Đà Lạt (Liên Tỉnh)</option>
              <option value="Hà Nội (Giáp Bát) → Hải Phòng (Cầu Rào)">Hà Nội (Giáp Bát) → Hải Phòng (Cầu Rào)</option>
            </select>
          </FormField>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <FormField label="Chọn phương tiện vận chuyển" required>
              <select
                v-model="tripForm.vehiclePlate"
                class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 bg-white"
              >
                <option v-for="v in availableVehicles" :key="v.plate" :value="v.plate">
                  {{ v.name }}
                </option>
              </select>
            </FormField>

            <FormField label="Phân công tài xế phụ trách" required>
              <select
                v-model="tripForm.driverName"
                class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 bg-white"
              >
                <option v-for="d in availableDrivers" :key="d.id" :value="d.name.split(' (')[0]">
                  {{ d.name }}
                </option>
              </select>
            </FormField>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <FormField label="Ngày & Giờ xuất bến" required>
              <input
                v-model="tripForm.departureTime"
                type="text"
                placeholder="YYYY-MM-DD HH:mm"
                data-testid="input-trip-dep-time"
                class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 tabular-nums font-bold"
              />
            </FormField>

            <FormField label="Ngày & Giờ dự kiến đến" required>
              <input
                v-model="tripForm.arrivalTime"
                type="text"
                placeholder="YYYY-MM-DD HH:mm"
                class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 tabular-nums"
              />
            </FormField>
          </div>

          <FormField label="Giá vé cơ sở niêm yết (VNĐ/Ghế)" required>
            <input
              v-model.number="tripForm.price"
              type="number"
              step="10000"
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 tabular-nums font-bold"
            />
          </FormField>
        </div>

        <!-- Drawer Footer -->
        <div class="p-5 border-t border-border-default bg-slate-50 flex items-center justify-end gap-3">
          <button
            type="button"
            @click="closeScheduleDrawer"
            class="px-4 py-2.5 border border-border-strong hover:bg-slate-100 text-text-secondary text-xs font-semibold rounded-lg transition-all"
          >
            Hủy Bỏ
          </button>

          <AppButton
            variant="primary"
            class="px-5 py-2.5 text-xs font-bold"
            data-testid="btn-submit-trip"
            :loading="isScheduling"
            :disabled="!!turnaroundBufferViolation"
            @click="handleScheduleTrip"
          >
            Xác Nhận Lập Chuyến
          </AppButton>
        </div>
      </div>
    </div>
  </div>
</template>
