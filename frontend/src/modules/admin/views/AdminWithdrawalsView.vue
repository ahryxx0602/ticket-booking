<script setup>
import { ref } from 'vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useAdminPermissions } from '@/modules/admin/composables/useAdminPermissions'
import { adminApi } from '@/modules/admin/api'

const { hasPermission } = useAdminPermissions()

// Withdrawal requests (corresponds to API-ADM-PARTNER-03)
const withdrawals = ref([
  {
    id: 'WDR-20261123-01',
    operatorName: 'Hoàng Long Limousine',
    amount: 15000000,
    bankInfo: 'Vietcombank - 0071001234567 (Hoàng Long Ltd)',
    requestedAt: '23/11/2026 10:15',
    status: 'PENDING_APPROVAL'
  },
  {
    id: 'WDR-20261122-09',
    operatorName: 'Phương Trang Velo',
    amount: 35000000,
    bankInfo: 'Techcombank - 1903456789001 (Phương Trang Corp)',
    requestedAt: '22/11/2026 16:40',
    status: 'COMPLETED'
  }
])

async function processWithdrawal(item, newStatus) {
  item.status = newStatus
  try {
    if (newStatus === 'COMPLETED') await adminApi.approveWithdrawal(item.id, 'Approved payout')
    else await adminApi.rejectWithdrawal(item.id, 'Rejected payout')
  } catch (_) {}
}
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-text-primary">Kiểm Duyệt Lệnh Rút Tiền Ví Ký Quỹ Escrow</h1>
      <p class="text-xs text-text-muted mt-1">
        Duyệt giải ngân chuyển khoản ngân hàng (trừ số dư Locked Balance) hoặc từ chối để hoàn trả số dư về ví khả dụng của nhà xe.
      </p>
    </div>

    <div class="bg-surface rounded-xl border border-border-default shadow-card overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse text-xs">
          <thead>
            <tr class="bg-slate-50/80 border-b border-border-default text-text-secondary uppercase tracking-wider text-[11px]">
              <th class="py-3 px-4 font-semibold">Mã Yêu Cầu</th>
              <th class="py-3 px-4 font-semibold">Đối Tác Nhà Xe</th>
              <th class="py-3 px-4 font-semibold text-right">Số Tiền Rút (Locked)</th>
              <th class="py-3 px-4 font-semibold">Tài Khoản Ngân Hàng Nhận</th>
              <th class="py-3 px-4 font-semibold">Ngày Tạo Lệnh</th>
              <th class="py-3 px-4 font-semibold text-center">Trạng Thái</th>
              <th class="py-3 px-4 font-semibold text-right">Phê Duyệt Giải Ngân</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border-default">
            <tr v-for="w in withdrawals" :key="w.id" class="hover:bg-slate-50/60 transition-colors">
              <td class="py-3.5 px-4 font-bold text-primary-600 tabular-nums">{{ w.id }}</td>
              <td class="py-3.5 px-4 font-bold text-text-primary">{{ w.operatorName }}</td>
              <td class="py-3.5 px-4 text-right font-extrabold text-emerald-600 tabular-nums">
                {{ w.amount.toLocaleString('vi-VN') }} đ
              </td>
              <td class="py-3.5 px-4 text-text-secondary font-medium">{{ w.bankInfo }}</td>
              <td class="py-3.5 px-4 text-text-muted tabular-nums">{{ w.requestedAt }}</td>
              <td class="py-3.5 px-4 text-center">
                <StatusBadge :status="w.status" />
              </td>
              <td class="py-3.5 px-4 text-right space-x-2">
                <template v-if="w.status === 'PENDING_APPROVAL'">
                  <button
                    v-if="hasPermission('WITHDRAWAL_APPROVE')"
                    type="button"
                    @click="processWithdrawal(w, 'COMPLETED')"
                    :data-testid="`btn-approve-withdraw-${w.id}`"
                    class="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold rounded-lg text-xs transition-all"
                  >
                    ✓ Duyệt Chi
                  </button>
                  <button
                    v-if="hasPermission('WITHDRAWAL_REJECT')"
                    type="button"
                    @click="processWithdrawal(w, 'REJECTED')"
                    :data-testid="`btn-reject-withdraw-${w.id}`"
                    class="px-3 py-1.5 bg-rose-600 hover:bg-rose-700 text-white font-semibold rounded-lg text-xs transition-all"
                  >
                    ✕ Từ Chối (Hoàn Ví)
                  </button>
                  <span
                    v-if="!hasPermission('WITHDRAWAL_APPROVE') && !hasPermission('WITHDRAWAL_REJECT')"
                    class="text-text-muted text-[11px]"
                  >
                    Chỉ xem (Thiếu quyền duyệt)
                  </span>
                </template>
                <span v-else class="text-text-muted text-[11px]">Đã đóng lệnh</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
