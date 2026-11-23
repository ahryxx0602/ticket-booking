<script setup>
import { ref } from 'vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import FormField from '@/shared/components/FormField.vue'
import AppButton from '@/shared/components/AppButton.vue'

const globalDefaultRate = ref(10.0)

// Operators list (corresponds to API-ADM-PARTNER-01 & API-ADM-PARTNER-02)
const operators = ref([
  {
    id: 'OP-01',
    name: 'Hoàng Long Limousine',
    representative: 'Nguyễn Hoàng Long',
    phone: '0901234567',
    taxCode: '0109876543',
    customCommissionRate: 8.5, // Custom tier rate
    vehicleCount: 18,
    status: 'ACTIVE'
  },
  {
    id: 'OP-02',
    name: 'Phương Trang Velo',
    representative: 'Lê Minh Phương',
    phone: '0918765432',
    taxCode: '0301122334',
    customCommissionRate: null, // Uses global fallback (10.0%)
    vehicleCount: 32,
    status: 'ACTIVE'
  },
  {
    id: 'OP-03',
    name: 'Hải Vân Express',
    representative: 'Trần Hải Vân',
    phone: '0989112233',
    taxCode: '0405566778',
    customCommissionRate: null,
    vehicleCount: 10,
    status: 'PENDING_APPROVAL'
  }
])

// Commission Modal State (2-tier commission configuration)
const selectedOperatorForCommission = ref(null)
const commissionInput = ref(10.0)
const useGlobalFallback = ref(false)
const commissionError = ref('')

function openCommissionModal(op) {
  selectedOperatorForCommission.value = op
  commissionError.value = ''
  if (op.customCommissionRate === null) {
    useGlobalFallback.value = true
    commissionInput.value = globalDefaultRate.value
  } else {
    useGlobalFallback.value = false
    commissionInput.value = op.customCommissionRate
  }
}

function closeCommissionModal() {
  selectedOperatorForCommission.value = null
}

function handleSaveCommission() {
  commissionError.value = ''

  if (!useGlobalFallback.value) {
    if (commissionInput.value === '' || commissionInput.value < 0 || commissionInput.value > 50) {
      commissionError.value = 'Tỷ lệ hoa hồng riêng phải nằm trong khoảng từ 0% đến 50%'
      return
    }
    selectedOperatorForCommission.value.customCommissionRate = Number(commissionInput.value)
  } else {
    selectedOperatorForCommission.value.customCommissionRate = null
  }

  closeCommissionModal()
}

// Status Approval / Rejection
function updateOperatorStatus(op, newStatus) {
  op.status = newStatus
}
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-text-primary">Phê Duyệt Hồ Sơ Nhà Xe & Cấu Hình Hoa Hồng 2 Cấp</h1>
        <p class="text-xs text-text-muted mt-1">
          Thẩm định pháp lý đối tác vận tải mới và thiết lập mức chiết khấu hoa hồng riêng (Custom Rate) hoặc mặc định toàn sàn ({{ globalDefaultRate }}%).
        </p>
      </div>

      <span class="self-start sm:self-auto px-3.5 py-2 rounded-lg bg-blue-50 text-primary-700 border border-blue-200 text-xs font-bold tabular-nums">
        Mặc định toàn sàn: {{ globalDefaultRate.toFixed(1) }}%
      </span>
    </div>

    <!-- Operators Table -->
    <div class="bg-surface rounded-xl border border-border-default shadow-card overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse text-xs">
          <thead>
            <tr class="bg-slate-50/80 border-b border-border-default text-text-secondary uppercase tracking-wider text-[11px]">
              <th class="py-3 px-4 font-semibold">Mã Đối Tác</th>
              <th class="py-3 px-4 font-semibold">Tên Nhà Xe & Mã Số Thuế</th>
              <th class="py-3 px-4 font-semibold">Người Đại Diện</th>
              <th class="py-3 px-4 font-semibold text-center">Quy Mô Xe</th>
              <th class="py-3 px-4 font-semibold text-center">Hoa Hồng Áp Dụng</th>
              <th class="py-3 px-4 font-semibold text-center">Trạng Thái</th>
              <th class="py-3 px-4 font-semibold text-right">Thao Tác Quản Trị</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border-default">
            <tr v-for="op in operators" :key="op.id" class="hover:bg-slate-50/60 transition-colors">
              <td class="py-3.5 px-4 font-bold text-primary-600 tabular-nums">{{ op.id }}</td>
              <td class="py-3.5 px-4">
                <span class="font-bold text-text-primary block">{{ op.name }}</span>
                <span class="text-[11px] text-text-muted tabular-nums">MST: {{ op.taxCode }}</span>
              </td>
              <td class="py-3.5 px-4">
                <span class="font-medium text-text-primary block">{{ op.representative }}</span>
                <span class="text-[11px] text-text-muted tabular-nums">{{ op.phone }}</span>
              </td>
              <td class="py-3.5 px-4 text-center font-bold tabular-nums">{{ op.vehicleCount }} xe</td>
              <td class="py-3.5 px-4 text-center">
                <div class="inline-flex flex-col items-center">
                  <span
                    class="px-2.5 py-0.5 rounded-full font-extrabold tabular-nums text-xs"
                    :class="op.customCommissionRate !== null ? 'bg-amber-50 text-amber-700 border border-amber-200' : 'bg-slate-100 text-slate-700'"
                  >
                    {{ op.customCommissionRate !== null ? `${op.customCommissionRate.toFixed(1)}% (Riêng)` : `${globalDefaultRate.toFixed(1)}% (Chuẩn)` }}
                  </span>
                </div>
              </td>
              <td class="py-3.5 px-4 text-center">
                <StatusBadge :status="op.status" />
              </td>
              <td class="py-3.5 px-4 text-right space-x-2">
                <button
                  type="button"
                  @click="openCommissionModal(op)"
                  :data-testid="`btn-commission-${op.id}`"
                  class="px-3 py-1.5 border border-border-strong hover:bg-slate-100 text-text-primary font-semibold rounded-lg text-xs transition-all"
                >
                  ⚙️ Hoa Hồng
                </button>

                <button
                  v-if="op.status === 'PENDING_APPROVAL'"
                  type="button"
                  @click="updateOperatorStatus(op, 'ACTIVE')"
                  :data-testid="`btn-approve-${op.id}`"
                  class="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold rounded-lg text-xs transition-all"
                >
                  ✓ Duyệt Hồ Sơ
                </button>

                <button
                  v-if="op.status === 'PENDING_APPROVAL'"
                  type="button"
                  @click="updateOperatorStatus(op, 'REJECTED')"
                  class="px-3 py-1.5 bg-rose-600 hover:bg-rose-700 text-white font-semibold rounded-lg text-xs transition-all"
                >
                  ✕ Từ Chối
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 2-Tier Commission Config Dialog -->
    <div
      v-if="selectedOperatorForCommission"
      class="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4"
    >
      <div class="bg-surface rounded-xl max-w-md w-full p-6 shadow-dialog border border-border-default space-y-4">
        <div class="flex items-center justify-between pb-2 border-b border-border-default">
          <h3 class="text-base font-bold text-text-primary">
            Cấu Hình Hoa Hồng 2 Cấp — {{ selectedOperatorForCommission.name }}
          </h3>
          <button @click="closeCommissionModal" class="text-slate-400 hover:text-slate-600 text-lg">✕</button>
        </div>

        <label class="flex items-center gap-2.5 text-xs font-semibold text-text-primary cursor-pointer bg-slate-50 p-3 rounded-lg border border-border-default">
          <input
            type="checkbox"
            v-model="useGlobalFallback"
            class="rounded text-primary-600 focus:ring-primary-500"
          />
          <span>Sử dụng tỷ lệ mặc định toàn sàn ({{ globalDefaultRate.toFixed(1) }}%)</span>
        </label>

        <FormField
          v-if="!useGlobalFallback"
          label="Tỷ lệ hoa hồng ưu đãi riêng cho nhà xe (%)"
          :error="commissionError"
          required
        >
          <input
            v-model.number="commissionInput"
            type="number"
            min="0"
            max="50"
            step="0.5"
            data-testid="input-custom-commission"
            class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs font-bold text-text-primary focus:ring-2 focus:ring-primary-600 tabular-nums"
            :class="{ 'border-rose-500': commissionError }"
          />
        </FormField>

        <p class="text-[11px] text-text-muted">
          💡 Tỷ lệ hoa hồng mới sẽ được snapshot tự động vào các giao dịch đặt vé phát sinh kể từ thời điểm lưu.
        </p>

        <div class="flex justify-end gap-3 pt-2 border-t border-border-default">
          <button
            type="button"
            @click="closeCommissionModal"
            class="px-4 py-2 border border-border-strong hover:bg-slate-50 text-text-secondary text-xs font-semibold rounded-lg"
          >
            Hủy Bỏ
          </button>

          <AppButton
            variant="primary"
            class="px-4 py-2 text-xs font-bold"
            data-testid="btn-save-commission"
            @click="handleSaveCommission"
          >
            Lưu Cấu Hình
          </AppButton>
        </div>
      </div>
    </div>
  </div>
</template>
