<script setup>
import { ref } from 'vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import FormField from '@/shared/components/FormField.vue'
import AppButton from '@/shared/components/AppButton.vue'
import { useAdminPermissions } from '@/modules/admin/composables/useAdminPermissions'
import { adminApi } from '@/modules/admin/api'

const { hasPermission } = useAdminPermissions()

const globalDefaultRate = ref(10.0)

// Operators list (corresponds to API-ADM-PARTNER-01 & API-ADM-PARTNER-02)
const operators = ref([
  {
    id: 'OP-01',
    name: 'Hoàng Long Limousine',
    representative: 'Nguyễn Hoàng Long',
    phone: '0901234567',
    email: 'hoanglong@velobus.vn',
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
    email: 'phuongtrang@velobus.vn',
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
    email: 'haivan@velobus.vn',
    taxCode: '0405566778',
    customCommissionRate: null,
    vehicleCount: 10,
    status: 'PENDING_APPROVAL'
  }
])

// Operator CRUD Modal State (Create / Edit Operator)
const isOperatorModalOpen = ref(false)
const editingOperator = ref(null)
const operatorForm = ref({
  name: '',
  representative: '',
  phone: '',
  email: '',
  taxCode: '',
  vehicleCount: 5
})
const operatorFormError = ref('')

function openCreateOperatorModal() {
  editingOperator.value = null
  operatorFormError.value = ''
  operatorForm.value = {
    name: '',
    representative: '',
    phone: '',
    email: '',
    taxCode: '',
    vehicleCount: 5
  }
  isOperatorModalOpen.value = true
}

function openEditOperatorModal(op) {
  editingOperator.value = op
  operatorFormError.value = ''
  operatorForm.value = {
    name: op.name,
    representative: op.representative,
    phone: op.phone,
    email: op.email || 'partner@velobus.vn',
    taxCode: op.taxCode,
    vehicleCount: op.vehicleCount
  }
  isOperatorModalOpen.value = true
}

function closeOperatorModal() {
  isOperatorModalOpen.value = false
  editingOperator.value = null
}

async function handleSaveOperator() {
  operatorFormError.value = ''
  if (!operatorForm.value.name.trim() || !operatorForm.value.taxCode.trim()) {
    operatorFormError.value = 'Vui lòng nhập Tên nhà xe và Mã số thuế'
    return
  }

  if (!editingOperator.value) {
    const newOp = {
      id: `OP-${String(operators.value.length + 1).padStart(2, '0')}`,
      name: operatorForm.value.name.trim(),
      representative: operatorForm.value.representative.trim() || 'Đại diện đối tác',
      phone: operatorForm.value.phone.trim() || '0901234567',
      email: operatorForm.value.email.trim() || 'partner@velobus.vn',
      taxCode: operatorForm.value.taxCode.trim(),
      customCommissionRate: null,
      vehicleCount: Number(operatorForm.value.vehicleCount) || 5,
      status: 'ACTIVE'
    }
    operators.value.unshift(newOp)
    adminApi.createOperator({
      businessName: newOp.name,
      brandName: newOp.name,
      taxCode: newOp.taxCode,
      contactPhone: newOp.phone,
      contactEmail: newOp.email
    }).then(res => {
      if (res?.data?.id) newOp.id = res.data.id
    }).catch(() => {})
  } else {
    editingOperator.value.name = operatorForm.value.name.trim()
    editingOperator.value.representative = operatorForm.value.representative.trim()
    editingOperator.value.phone = operatorForm.value.phone.trim()
    editingOperator.value.email = operatorForm.value.email.trim()
    editingOperator.value.taxCode = operatorForm.value.taxCode.trim()
    editingOperator.value.vehicleCount = Number(operatorForm.value.vehicleCount) || editingOperator.value.vehicleCount
    try {
      await adminApi.updateOperator(editingOperator.value.id, {
        businessName: editingOperator.value.name,
        brandName: editingOperator.value.name,
        taxCode: editingOperator.value.taxCode,
        contactPhone: editingOperator.value.phone,
        contactEmail: editingOperator.value.email
      })
    } catch (_) {}
  }

  closeOperatorModal()
}

async function handleDeleteOperator(op) {
  try {
    await adminApi.deleteOperator(op.id)
  } catch (_) {}
  operators.value = operators.value.filter(item => item.id !== op.id)
}

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

async function handleSaveCommission() {
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

  try {
    await adminApi.updateOperatorCommission(
      selectedOperatorForCommission.value.id,
      selectedOperatorForCommission.value.customCommissionRate
    )
  } catch (_) {}

  closeCommissionModal()
}

// Status Approval / Rejection / Suspension
async function updateOperatorStatus(op, newStatus) {
  op.status = newStatus
  try {
    if (newStatus === 'ACTIVE') await adminApi.approveOperator(op.id)
    else if (newStatus === 'REJECTED') await adminApi.rejectOperator(op.id, 'Rejected by admin')
    else if (newStatus === 'SUSPENDED') await adminApi.suspendOperator(op.id, 'Suspended by admin')
  } catch (_) {}
}
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-text-primary">Phê Duyệt Hồ Sơ Nhà Xe & Cấu Hình Hoa Hồng 2 Cấp</h1>
        <p class="text-xs text-text-muted mt-1">
          Thẩm định pháp lý đối tác vận tải mới, CRUD hồ sơ nhà xe và thiết lập mức chiết khấu hoa hồng riêng (Custom Rate) hoặc mặc định toàn sàn ({{ globalDefaultRate }}%).
        </p>
      </div>

      <div class="flex flex-wrap items-center gap-2">
        <span class="px-3.5 py-2 rounded-lg bg-blue-50 text-primary-700 border border-blue-200 text-xs font-bold tabular-nums">
          Mặc định toàn sàn: {{ globalDefaultRate.toFixed(1) }}%
        </span>

        <button
          v-if="hasPermission('OPERATOR_CREATE_UPDATE')"
          type="button"
          data-testid="open-create-operator-btn"
          @click="openCreateOperatorModal"
          class="px-3.5 py-2 rounded-lg bg-brand-navy hover:bg-slate-800 text-white font-bold text-xs transition-all shadow-sm"
        >
          🚌 + Thêm Nhà Xe Mới
        </button>
      </div>
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
              <td class="py-3.5 px-4 text-right space-x-1.5 whitespace-nowrap">
                <button
                  v-if="hasPermission('OPERATOR_COMMISSION_MANAGE')"
                  type="button"
                  @click="openCommissionModal(op)"
                  :data-testid="`btn-commission-${op.id}`"
                  class="px-2.5 py-1.5 border border-border-strong hover:bg-slate-100 text-text-primary font-semibold rounded-lg text-xs transition-all"
                >
                  ⚙️ Hoa Hồng
                </button>

                <button
                  v-if="op.status === 'PENDING_APPROVAL' && hasPermission('OPERATOR_APPROVE_REJECT')"
                  type="button"
                  @click="updateOperatorStatus(op, 'ACTIVE')"
                  :data-testid="`btn-approve-${op.id}`"
                  class="px-2.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold rounded-lg text-xs transition-all"
                >
                  ✓ Duyệt Hồ Sơ
                </button>

                <button
                  v-if="op.status === 'PENDING_APPROVAL' && hasPermission('OPERATOR_APPROVE_REJECT')"
                  type="button"
                  @click="updateOperatorStatus(op, 'REJECTED')"
                  class="px-2.5 py-1.5 bg-rose-600 hover:bg-rose-700 text-white font-semibold rounded-lg text-xs transition-all"
                >
                  ✕ Từ Chối
                </button>

                <button
                  v-if="op.status !== 'PENDING_APPROVAL' && hasPermission('OPERATOR_SUSPEND')"
                  type="button"
                  @click="updateOperatorStatus(op, op.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE')"
                  :data-testid="`btn-suspend-${op.id}`"
                  class="px-2.5 py-1.5 rounded-lg font-semibold text-xs transition-all"
                  :class="op.status === 'ACTIVE' ? 'bg-amber-50 text-amber-800 hover:bg-amber-100' : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100'"
                >
                  {{ op.status === 'ACTIVE' ? '⏸️ Đình Chỉ' : '▶️ Mở Lại' }}
                </button>

                <button
                  v-if="hasPermission('OPERATOR_CREATE_UPDATE')"
                  type="button"
                  @click="openEditOperatorModal(op)"
                  :data-testid="`btn-edit-operator-${op.id}`"
                  class="px-2.5 py-1.5 border border-border-strong hover:bg-slate-100 text-text-primary font-semibold rounded-lg text-xs transition-all"
                >
                  ✏️ Sửa
                </button>

                <button
                  v-if="hasPermission('OPERATOR_CREATE_UPDATE')"
                  type="button"
                  @click="handleDeleteOperator(op)"
                  :data-testid="`btn-delete-operator-${op.id}`"
                  class="px-2.5 py-1.5 bg-rose-50 text-rose-700 hover:bg-rose-100 font-semibold rounded-lg text-xs transition-all"
                >
                  🗑️ Xóa
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modal 1: Create / Edit Operator -->
    <div
      v-if="isOperatorModalOpen"
      class="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4"
    >
      <div class="bg-surface rounded-xl max-w-md w-full p-6 shadow-dialog border border-border-default space-y-4">
        <div class="flex items-center justify-between pb-2 border-b border-border-default">
          <h3 class="text-base font-bold text-text-primary">
            {{ editingOperator ? `Chỉnh Sửa Nhà Xe — ${editingOperator.name}` : 'Thêm Đối Tác Nhà Xe Mới' }}
          </h3>
          <button type="button" @click="closeOperatorModal" class="text-slate-400 hover:text-slate-600 text-lg">✕</button>
        </div>

        <div v-if="operatorFormError" class="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs font-semibold">
          ⚠️ {{ operatorFormError }}
        </div>

        <FormField label="Tên thương hiệu nhà xe" required>
          <input
            v-model="operatorForm.name"
            type="text"
            data-testid="input-operator-name"
            placeholder="VD: Sao Việt Limousine"
            class="w-full h-10 px-3 rounded-lg border border-border-strong text-xs text-text-primary"
          />
        </FormField>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <FormField label="Mã số thuế doanh nghiệp" required>
            <input
              v-model="operatorForm.taxCode"
              type="text"
              data-testid="input-operator-tax"
              placeholder="0109988776"
              class="w-full h-10 px-3 rounded-lg border border-border-strong text-xs text-text-primary tabular-nums"
            />
          </FormField>

          <FormField label="Số lượng xe đăng ký">
            <input
              v-model.number="operatorForm.vehicleCount"
              type="number"
              min="1"
              class="w-full h-10 px-3 rounded-lg border border-border-strong text-xs text-text-primary tabular-nums"
            />
          </FormField>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <FormField label="Người đại diện pháp luật">
            <input
              v-model="operatorForm.representative"
              type="text"
              placeholder="Họ tên người đại diện..."
              class="w-full h-10 px-3 rounded-lg border border-border-strong text-xs text-text-primary"
            />
          </FormField>

          <FormField label="Số điện thoại liên hệ">
            <input
              v-model="operatorForm.phone"
              type="text"
              placeholder="0901234567"
              class="w-full h-10 px-3 rounded-lg border border-border-strong text-xs text-text-primary tabular-nums"
            />
          </FormField>
        </div>

        <div class="flex justify-end gap-3 pt-2 border-t border-border-default">
          <button
            type="button"
            @click="closeOperatorModal"
            class="px-4 py-2 border border-border-strong hover:bg-slate-50 text-text-secondary text-xs font-semibold rounded-lg"
          >
            Hủy Bỏ
          </button>
          <AppButton
            variant="primary"
            class="px-4 py-2 text-xs font-bold"
            data-testid="btn-save-operator"
            @click="handleSaveOperator"
          >
            {{ editingOperator ? 'Lưu Cập Nhật' : 'Tạo Nhà Xe' }}
          </AppButton>
        </div>
      </div>
    </div>

    <!-- Modal 2: 2-Tier Commission Config Dialog -->
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
