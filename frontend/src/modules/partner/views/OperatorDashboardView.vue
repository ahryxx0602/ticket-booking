<script setup>
import { ref, computed } from 'vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import FormField from '@/shared/components/FormField.vue'
import AppButton from '@/shared/components/AppButton.vue'

// Operator Wallet State (corresponds to API-PARTNER-05)
const wallet = ref({
  operatorId: 'OP-01',
  operatorName: 'Hoàng Long Limousine',
  availableBalance: 42500000,   // Số dư khả dụng có thể rút
  holdingEscrow: 18700000,      // Đang ký quỹ chuyến xe (chờ chạy +2h)
  settlingBalance: 5200000,     // Đang xử lý đối soát
  totalWithdrawn: 120000000,    // Tổng tiền đã rút thành công
  commissionRate: 10.0          // Tỷ lệ hoa hồng sàn (%)
})

// Transaction History (corresponds to API-PARTNER-05 history)
const transactions = ref([
  {
    id: 'TXN-9081',
    type: 'TICKET_ESCROW_CREDIT',
    typeName: 'Cộng tiền vé (Ký quỹ)',
    tripId: 'TRIP-01',
    amount: 700000,
    direction: 'PLUS',
    balanceAfter: 42500000,
    createdAt: '23/11/2026 14:15',
    status: 'COMPLETED'
  },
  {
    id: 'TXN-9075',
    type: 'COMMISSION_DEDUCTION',
    typeName: 'Trừ hoa hồng sàn (10%)',
    tripId: 'TRIP-01',
    amount: -70000,
    direction: 'MINUS',
    balanceAfter: 41800000,
    createdAt: '23/11/2026 14:15',
    status: 'COMPLETED'
  },
  {
    id: 'TXN-8890',
    type: 'WITHDRAWAL',
    typeName: 'Rút tiền về Vietcombank',
    tripId: null,
    amount: -15000000,
    direction: 'MINUS',
    balanceAfter: 41870000,
    createdAt: '21/11/2026 09:30',
    status: 'COMPLETED'
  },
  {
    id: 'TXN-8712',
    type: 'ESCROW_SETTLED',
    typeName: 'Quyết toán chuyến hoàn tất',
    tripId: 'TRIP-99',
    amount: 14200000,
    direction: 'PLUS',
    balanceAfter: 56870000,
    createdAt: '20/11/2026 23:00',
    status: 'COMPLETED'
  }
])

// Withdrawal Dialog State
const showWithdrawDialog = ref(false)
const withdrawAmount = ref(10000000)
const withdrawBank = ref('VCB-0071001234567')
const withdrawNote = ref('')
const withdrawError = ref('')
const isSubmittingWithdraw = ref(false)

function openWithdrawModal() {
  withdrawError.value = ''
  showWithdrawDialog.value = true
}

function closeWithdrawModal() {
  showWithdrawDialog.value = false
}

function handleWithdrawSubmit() {
  withdrawError.value = ''

  if (!withdrawAmount.value || withdrawAmount.value < 50000) {
    withdrawError.value = 'Số tiền rút tối thiểu là 50.000 đ'
    return
  }

  if (withdrawAmount.value > wallet.value.availableBalance) {
    withdrawError.value = `Số tiền rút vượt quá số dư khả dụng (${wallet.value.availableBalance.toLocaleString('vi-VN')} đ)`
    return
  }

  isSubmittingWithdraw.value = true

  // Simulate API-PARTNER-06 call
  setTimeout(() => {
    wallet.value.availableBalance -= withdrawAmount.value
    wallet.value.settlingBalance += withdrawAmount.value

    transactions.value.unshift({
      id: `TXN-${Math.floor(1000 + Math.random() * 9000)}`,
      type: 'WITHDRAWAL_REQUEST',
      typeName: 'Lệnh rút tiền đang xử lý',
      tripId: null,
      amount: -withdrawAmount.value,
      direction: 'MINUS',
      balanceAfter: wallet.value.availableBalance,
      createdAt: 'Vừa xong',
      status: 'HELD'
    })

    isSubmittingWithdraw.value = false
    closeWithdrawModal()
  }, 1000)
}
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-text-primary">Tổng Quan Ví Ký Quỹ Escrow (4 Số Dư)</h1>
        <p class="text-xs text-text-muted mt-1">
          Hệ thống ví Fintech đa số dư: Quyết toán sau chuyến đi +2h • Chống rút trùng bằng cơ chế Khóa số dư (Locked Balance).
        </p>
      </div>

      <button
        type="button"
        @click="openWithdrawModal"
        data-testid="btn-open-withdraw"
        class="self-start sm:self-auto px-4 py-2.5 bg-primary-600 hover:bg-primary-700 text-white font-semibold rounded-lg text-xs shadow-sm flex items-center gap-2 transition-all"
      >
        <span>💸</span>
        <span>Yêu Cầu Rút Tiền</span>
      </button>
    </div>

    <!-- 4 Fintech Balance Cards Grid -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <!-- 1. Available Balance -->
      <div class="bg-surface rounded-xl border-2 border-emerald-500/40 p-5 shadow-card space-y-2">
        <div class="flex items-center justify-between">
          <span class="text-xs font-bold uppercase tracking-wider text-text-secondary">Số Dư Khả Dụng</span>
          <span class="p-2 rounded-lg bg-emerald-50 text-emerald-600 text-lg">💰</span>
        </div>
        <p class="text-2xl font-extrabold text-emerald-600 tabular-nums" data-testid="available-balance">
          {{ wallet.availableBalance.toLocaleString('vi-VN') }} <span class="text-xs font-bold">đ</span>
        </p>
        <p class="text-[11px] text-text-muted">Có thể tạo lệnh rút về ngân hàng ngay lập tức</p>
      </div>

      <!-- 2. Holding Escrow -->
      <div class="bg-surface rounded-xl border border-border-default p-5 shadow-card space-y-2">
        <div class="flex items-center justify-between">
          <span class="text-xs font-bold uppercase tracking-wider text-text-secondary">Đang Ký Quỹ Chuyến</span>
          <span class="p-2 rounded-lg bg-amber-50 text-amber-600 text-lg">🔒</span>
        </div>
        <p class="text-2xl font-extrabold text-amber-600 tabular-nums">
          {{ wallet.holdingEscrow.toLocaleString('vi-VN') }} <span class="text-xs font-bold">đ</span>
        </p>
        <p class="text-[11px] text-text-muted">Tự động quyết toán sau khi xe đến đích +2 giờ</p>
      </div>

      <!-- 3. Settling Balance -->
      <div class="bg-surface rounded-xl border border-border-default p-5 shadow-card space-y-2">
        <div class="flex items-center justify-between">
          <span class="text-xs font-bold uppercase tracking-wider text-text-secondary">Đang Xử Lý Rút</span>
          <span class="p-2 rounded-lg bg-blue-50 text-primary-600 text-lg">⏳</span>
        </div>
        <p class="text-2xl font-extrabold text-primary-600 tabular-nums">
          {{ wallet.settlingBalance.toLocaleString('vi-VN') }} <span class="text-xs font-bold">đ</span>
        </p>
        <p class="text-[11px] text-text-muted">Lệnh rút tiền đang chờ Admin phê duyệt chi</p>
      </div>

      <!-- 4. Total Withdrawn -->
      <div class="bg-surface rounded-xl border border-border-default p-5 shadow-card space-y-2">
        <div class="flex items-center justify-between">
          <span class="text-xs font-bold uppercase tracking-wider text-text-secondary">Tổng Tiền Đã Rút</span>
          <span class="p-2 rounded-lg bg-slate-100 text-slate-700 text-lg">🏦</span>
        </div>
        <p class="text-2xl font-extrabold text-text-primary tabular-nums">
          {{ wallet.totalWithdrawn.toLocaleString('vi-VN') }} <span class="text-xs font-bold">đ</span>
        </p>
        <p class="text-[11px] text-text-muted">Lũy kế giải ngân thành công từ khi mở sàn</p>
      </div>
    </div>

    <!-- Transaction History Table -->
    <div class="bg-surface rounded-xl border border-border-default shadow-card overflow-hidden space-y-0">
      <div class="p-5 border-b border-border-default flex items-center justify-between">
        <div>
          <h2 class="text-base font-bold text-text-primary">Lịch Sử Biến Động Số Dư & Đối Soát</h2>
          <p class="text-xs text-text-muted mt-0.5">Mọi giao dịch đều được ký snapshot tỷ lệ hoa hồng ({{ wallet.commissionRate }}%) tại thời điểm giao dịch</p>
        </div>
        <span class="text-xs font-semibold px-2.5 py-1 rounded bg-slate-100 text-text-secondary">
          Hoa hồng sàn: {{ wallet.commissionRate }}%
        </span>
      </div>

      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse text-xs">
          <thead>
            <tr class="bg-slate-50/80 border-b border-border-default text-text-secondary uppercase tracking-wider text-[11px]">
              <th class="py-3 px-4 font-semibold">Mã GD</th>
              <th class="py-3 px-4 font-semibold">Loại Giao Dịch</th>
              <th class="py-3 px-4 font-semibold">Chuyến Xe</th>
              <th class="py-3 px-4 font-semibold text-right">Biến Động</th>
              <th class="py-3 px-4 font-semibold text-right">Số Dư Sau GD</th>
              <th class="py-3 px-4 font-semibold">Thời Gian</th>
              <th class="py-3 px-4 font-semibold text-center">Trạng Thái</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border-default">
            <tr
              v-for="tx in transactions"
              :key="tx.id"
              class="hover:bg-slate-50/60 transition-colors"
            >
              <td class="py-3 px-4 font-bold text-text-primary tabular-nums">{{ tx.id }}</td>
              <td class="py-3 px-4 text-text-secondary">{{ tx.typeName }}</td>
              <td class="py-3 px-4 text-text-muted tabular-nums">{{ tx.tripId || '—' }}</td>
              <td
                class="py-3 px-4 text-right font-bold tabular-nums"
                :class="tx.amount > 0 ? 'text-emerald-600' : 'text-rose-600'"
              >
                {{ tx.amount > 0 ? '+' : '' }}{{ tx.amount.toLocaleString('vi-VN') }} đ
              </td>
              <td class="py-3 px-4 text-right font-medium text-text-primary tabular-nums">
                {{ tx.balanceAfter.toLocaleString('vi-VN') }} đ
              </td>
              <td class="py-3 px-4 text-text-muted">{{ tx.createdAt }}</td>
              <td class="py-3 px-4 text-center">
                <StatusBadge :status="tx.status" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Withdrawal Request Modal Dialog -->
    <div
      v-if="showWithdrawDialog"
      class="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4"
    >
      <div class="bg-surface rounded-xl max-w-md w-full p-6 shadow-dialog border border-border-default space-y-4">
        <div class="flex items-center justify-between pb-2 border-b border-border-default">
          <h3 class="text-base font-bold text-text-primary flex items-center gap-2">
            <span>💸</span>
            <span>Yêu Cầu Rút Tiền Về Ngân Hàng</span>
          </h3>
          <button @click="closeWithdrawModal" class="text-slate-400 hover:text-slate-600 text-lg">✕</button>
        </div>

        <div class="bg-blue-50/70 p-3 rounded-lg border border-blue-200 text-xs text-text-secondary space-y-1">
          <div class="flex justify-between">
            <span>Số dư khả dụng hiện tại:</span>
            <strong class="text-primary-700 tabular-nums">{{ wallet.availableBalance.toLocaleString('vi-VN') }} đ</strong>
          </div>
          <p class="text-[11px] text-text-muted">Khoản tiền rút sẽ được khóa tạm thời (`LOCKED`) chờ Admin giải ngân.</p>
        </div>

        <FormField
          label="Số tiền muốn rút (VNĐ)"
          :error="withdrawError"
          required
        >
          <input
            v-model.number="withdrawAmount"
            type="number"
            min="50000"
            :max="wallet.availableBalance"
            step="50000"
            data-testid="input-withdraw-amount"
            class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 bg-white tabular-nums font-bold"
            :class="{ 'border-rose-500': withdrawError }"
          />
        </FormField>

        <FormField label="Tài khoản ngân hàng thụ hưởng" required>
          <select
            v-model="withdrawBank"
            class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 bg-white"
          >
            <option value="VCB-0071001234567">Vietcombank - 0071001234567 (Hoàng Long Ltd)</option>
            <option value="TCB-1903456789001">Techcombank - 1903456789001 (Hoàng Long Ltd)</option>
          </select>
        </FormField>

        <FormField label="Ghi chú đối soát (Không bắt buộc)">
          <input
            v-model="withdrawNote"
            type="text"
            placeholder="Ví dụ: Rút doanh thu tuần 3 tháng 11..."
            class="w-full h-10 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 bg-white"
          />
        </FormField>

        <div class="flex justify-end gap-3 pt-3 border-t border-border-default">
          <button
            type="button"
            @click="closeWithdrawModal"
            class="px-4 py-2 border border-border-strong hover:bg-slate-50 text-text-secondary text-xs font-semibold rounded-lg transition-all"
          >
            Hủy Bỏ
          </button>

          <AppButton
            variant="primary"
            class="px-4 py-2 text-xs font-bold"
            data-testid="btn-submit-withdraw"
            :loading="isSubmittingWithdraw"
            @click="handleWithdrawSubmit"
          >
            Gửi Yêu Cầu Rút
          </AppButton>
        </div>
      </div>
    </div>
  </div>
</template>
