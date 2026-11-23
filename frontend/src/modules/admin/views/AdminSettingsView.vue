<script setup>
import { ref } from 'vue'
import FormField from '@/shared/components/FormField.vue'
import AppButton from '@/shared/components/AppButton.vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'

// Global System Settings (corresponds to TBL-SYSTEM-SETTINGS / API-PUB-SETTING-01)
const systemSettings = ref({
  platformName: 'VeloBus - Sàn Vé Xe Khách Cao Cấp',
  defaultCommissionRate: 10.0,
  seatHoldTtlMinutes: 10,
  turnaroundBufferMinutes: 30,
  escrowSettlementDelayHours: 2
})

const saveSuccessMsg = ref('')

function handleSaveSettings() {
  saveSuccessMsg.value = 'Đã lưu cấu hình tham số toàn sàn và đồng bộ Redis Cache thành công!'
  setTimeout(() => {
    saveSuccessMsg.value = ''
  }, 3000)
}

// Master Stations Catalog (corresponds to TBL-STATIONS / API-ADM-CAT-01)
const stations = ref([
  { id: 'HN-GP', name: 'Bến xe Giáp Bát', province: 'Hà Nội', status: 'ACTIVE' },
  { id: 'HN-MY', name: 'Bến xe Mỹ Đình', province: 'Hà Nội', status: 'ACTIVE' },
  { id: 'DN-TT', name: 'Bến xe Trung Tâm Đà Nẵng', province: 'Đà Nẵng', status: 'ACTIVE' },
  { id: 'SG-MD', name: 'Bến xe Miền Đông Mới', province: 'TP. Hồ Chí Minh', status: 'ACTIVE' },
  { id: 'DL-LD', name: 'Bến xe Liên Tỉnh Đà Lạt', province: 'Lâm Đồng', status: 'ACTIVE' }
])

const newStation = ref({ id: '', name: '', province: '' })

function handleAddStation() {
  if (!newStation.value.id.trim() || !newStation.value.name.trim()) return
  stations.value.push({
    id: newStation.value.id.toUpperCase(),
    name: newStation.value.name,
    province: newStation.value.province || 'Việt Nam',
    status: 'ACTIVE'
  })
  newStation.value = { id: '', name: '', province: '' }
}
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-text-primary">Quản Trị Danh Mục Bến Xe & Cấu Hình Tham Số Sàn</h1>
      <p class="text-xs text-text-muted mt-1">
        Thiết lập tham số vận hành toàn cục (`TBL-SYSTEM-SETTINGS`) và quản lý danh mục nhà ga/bến xe liên tỉnh (`TBL-STATIONS`).
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
            class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600"
          />
        </FormField>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <FormField label="Hoa hồng mặc định toàn sàn (%)" required>
            <input
              v-model.number="systemSettings.defaultCommissionRate"
              type="number"
              step="0.5"
              data-testid="input-global-commission"
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs font-bold text-text-primary tabular-nums"
            />
          </FormField>

          <FormField label="Thời gian khóa ghế Redis (Phút)" required>
            <input
              v-model.number="systemSettings.seatHoldTtlMinutes"
              type="number"
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs font-bold text-text-primary tabular-nums"
            />
          </FormField>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <FormField label="Đệm quay đầu xe Turnaround (Phút)" required>
            <input
              v-model.number="systemSettings.turnaroundBufferMinutes"
              type="number"
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs font-bold text-text-primary tabular-nums"
            />
          </FormField>

          <FormField label="Trễ quyết toán ký quỹ sau chuyến (Giờ)" required>
            <input
              v-model.number="systemSettings.escrowSettlementDelayHours"
              type="number"
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs font-bold text-text-primary tabular-nums"
            />
          </FormField>
        </div>

        <div class="pt-2">
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

        <div class="grid grid-cols-1 sm:grid-cols-3 gap-2">
          <input
            v-model="newStation.id"
            type="text"
            placeholder="Mã (VD: HP-CR)"
            class="h-10 px-3 rounded-lg border border-border-strong text-xs uppercase tabular-nums"
          />
          <input
            v-model="newStation.name"
            type="text"
            placeholder="Tên bến xe..."
            class="h-10 px-3 rounded-lg border border-border-strong text-xs"
          />
          <button
            type="button"
            @click="handleAddStation"
            class="h-10 bg-brand-navy hover:bg-slate-800 text-white font-semibold rounded-lg text-xs transition-all"
          >
            + Thêm Bến Xe
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
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>
