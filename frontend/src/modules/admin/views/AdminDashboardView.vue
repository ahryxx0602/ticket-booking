<script setup>
import { ref } from 'vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import AppButton from '@/shared/components/AppButton.vue'

// 4 KPI Metrics (corresponds to API-ADM-02 & API-PAY-ADM-01)
const platformMetrics = ref({
  totalGmv: 1450800000,         // Tổng doanh thu GMV toàn sàn
  totalCommission: 142500000,   // Tổng hoa hồng sàn thực thu
  totalConfirmedBookings: 3842, // Tổng số đơn đặt vé thành công
  activeOperators: 28           // Số nhà xe đang hoạt động
})

// Reconciliation Alerts (corresponds to API-PAY-ADM-02)
const reconciliationAlerts = ref([
  {
    id: 'REC-20261123-01',
    bookingId: 'BKG-20261123-7712',
    vnpayTxnRef: 'VNP-99881122',
    vnpayAmount: 700000,
    internalEscrowAmount: 700000,
    discrepancy: 0,
    issueType: 'MATCHED',
    description: 'Khớp lệnh tự động giữa VNPay IPN và Ví ký quỹ nhà xe Hoàng Long',
    detectedAt: '23/11/2026 14:20',
    resolved: true
  },
  {
    id: 'REC-20261122-04',
    bookingId: 'BKG-20261122-3319',
    vnpayTxnRef: 'VNP-77441092',
    vnpayAmount: 380000,
    internalEscrowAmount: 0,
    discrepancy: 380000,
    issueType: 'WEBHOOK_DELAY_MISMATCH',
    description: 'VNPay ghi nhận thanh toán thành công nhưng IPN Webhook trễ sau khi đơn hết hạn giữ chỗ Redis (10p)',
    detectedAt: '22/11/2026 21:05',
    resolved: false
  }
])

const isRunningReconciliation = ref(false)

function triggerManualReconciliation() {
  isRunningReconciliation.value = true
  setTimeout(() => {
    isRunningReconciliation.value = false
  }, 800)
}

function resolveAlert(alertItem) {
  alertItem.resolved = true
  alertItem.discrepancy = 0
  alertItem.issueType = 'RESOLVED_MANUAL'
}
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-text-primary">Tổng Quan Toàn Sàn & Báo Cáo Đối Soát</h1>
        <p class="text-xs text-text-muted mt-1">
          Giám sát chỉ số GMV, doanh thu hoa hồng 2 cấp và đối soát tự động dòng tiền VNPay ↔ Ví Ký Quỹ Escrow.
        </p>
      </div>

      <AppButton
        variant="primary"
        class="self-start sm:self-auto px-4 py-2.5 text-xs font-bold"
        data-testid="btn-run-reconciliation"
        :loading="isRunningReconciliation"
        @click="triggerManualReconciliation"
      >
        <span>🔄</span>
        <span>Chạy Đối Soát Ngay</span>
      </AppButton>
    </div>

    <!-- 4 KPI System Cards -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="bg-surface rounded-xl border border-border-default p-5 shadow-card space-y-2">
        <div class="flex items-center justify-between">
          <span class="text-xs font-bold uppercase tracking-wider text-text-secondary">Tổng GMV Toàn Sàn</span>
          <span class="p-2 rounded-lg bg-blue-50 text-primary-600 text-lg">📊</span>
        </div>
        <p class="text-2xl font-extrabold text-primary-600 tabular-nums" data-testid="kpi-gmv">
          {{ platformMetrics.totalGmv.toLocaleString('vi-VN') }} <span class="text-xs font-bold">đ</span>
        </p>
        <p class="text-[11px] text-emerald-600 font-medium">↑ +18.4% so với tháng trước</p>
      </div>

      <div class="bg-surface rounded-xl border-2 border-emerald-500/40 p-5 shadow-card space-y-2">
        <div class="flex items-center justify-between">
          <span class="text-xs font-bold uppercase tracking-wider text-text-secondary">Hoa Hồng Thực Thu</span>
          <span class="p-2 rounded-lg bg-emerald-50 text-emerald-600 text-lg">💎</span>
        </div>
        <p class="text-2xl font-extrabold text-emerald-600 tabular-nums" data-testid="kpi-commission">
          {{ platformMetrics.totalCommission.toLocaleString('vi-VN') }} <span class="text-xs font-bold">đ</span>
        </p>
        <p class="text-[11px] text-text-muted">Đã khấu trừ tự động qua Escrow Settlement</p>
      </div>

      <div class="bg-surface rounded-xl border border-border-default p-5 shadow-card space-y-2">
        <div class="flex items-center justify-between">
          <span class="text-xs font-bold uppercase tracking-wider text-text-secondary">Đơn Vé Thành Công</span>
          <span class="p-2 rounded-lg bg-amber-50 text-amber-600 text-lg">🎫</span>
        </div>
        <p class="text-2xl font-extrabold text-text-primary tabular-nums" data-testid="kpi-bookings">
          {{ platformMetrics.totalConfirmedBookings.toLocaleString('vi-VN') }} <span class="text-xs font-bold">đơn</span>
        </p>
        <p class="text-[11px] text-text-muted">Tỷ lệ thanh toán thành công 96.2%</p>
      </div>

      <div class="bg-surface rounded-xl border border-border-default p-5 shadow-card space-y-2">
        <div class="flex items-center justify-between">
          <span class="text-xs font-bold uppercase tracking-wider text-text-secondary">Nhà Xe Hoạt Động</span>
          <span class="p-2 rounded-lg bg-slate-100 text-slate-700 text-lg">🚌</span>
        </div>
        <p class="text-2xl font-extrabold text-text-primary tabular-nums" data-testid="kpi-operators">
          {{ platformMetrics.activeOperators }} <span class="text-xs font-bold">đối tác</span>
        </p>
        <p class="text-[11px] text-text-muted">100% đã xác thực giấy phép kinh doanh vận tải</p>
      </div>
    </div>

    <!-- Reconciliation Alerts Panel -->
    <div class="bg-surface rounded-xl border border-border-default shadow-card overflow-hidden">
      <div class="p-5 border-b border-border-default flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <div>
          <h2 class="text-base font-bold text-text-primary flex items-center gap-2">
            <span>🛡️</span>
            <span>Nhật Ký Cảnh Báo Đối Soát Tự Động (VNPay IPN ↔ Ví Ký Quỹ)</span>
          </h2>
          <p class="text-xs text-text-muted mt-0.5">
            Phát hiện tức thời các giao dịch lệch số dư và tự động đẩy cảnh báo sang Telegram Bot Ban Quản Trị.
          </p>
        </div>
        <span class="text-xs font-semibold px-3 py-1 rounded-full bg-amber-50 text-amber-700 border border-amber-200">
          {{ reconciliationAlerts.filter(a => !a.resolved).length }} cảnh báo cần xử lý
        </span>
      </div>

      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse text-xs">
          <thead>
            <tr class="bg-slate-50/80 border-b border-border-default text-text-secondary uppercase tracking-wider text-[11px]">
              <th class="py-3 px-4 font-semibold">Mã Đối Soát</th>
              <th class="py-3 px-4 font-semibold">Mã Đơn / VNPay Ref</th>
              <th class="py-3 px-4 font-semibold text-right">Tiền VNPay</th>
              <th class="py-3 px-4 font-semibold text-right">Tiền Ví Sàn</th>
              <th class="py-3 px-4 font-semibold text-right">Chênh Lệch</th>
              <th class="py-3 px-4 font-semibold">Chi Tiết Cảnh Báo</th>
              <th class="py-3 px-4 font-semibold text-center">Trạng Thái</th>
              <th class="py-3 px-4 font-semibold text-right">Thao Tác</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border-default">
            <tr
              v-for="item in reconciliationAlerts"
              :key="item.id"
              class="hover:bg-slate-50/60 transition-colors"
            >
              <td class="py-3.5 px-4 font-bold text-text-primary tabular-nums">{{ item.id }}</td>
              <td class="py-3.5 px-4 tabular-nums">
                <span class="font-bold text-primary-600 block">{{ item.bookingId }}</span>
                <span class="text-[11px] text-text-muted">{{ item.vnpayTxnRef }}</span>
              </td>
              <td class="py-3.5 px-4 text-right font-semibold tabular-nums">
                {{ item.vnpayAmount.toLocaleString('vi-VN') }} đ
              </td>
              <td class="py-3.5 px-4 text-right font-semibold tabular-nums">
                {{ item.internalEscrowAmount.toLocaleString('vi-VN') }} đ
              </td>
              <td
                class="py-3.5 px-4 text-right font-extrabold tabular-nums"
                :class="item.discrepancy > 0 ? 'text-rose-600' : 'text-emerald-600'"
              >
                {{ item.discrepancy.toLocaleString('vi-VN') }} đ
              </td>
              <td class="py-3.5 px-4 text-text-secondary max-w-xs">{{ item.description }}</td>
              <td class="py-3.5 px-4 text-center">
                <span
                  class="px-2.5 py-0.5 rounded-full text-[11px] font-bold"
                  :class="item.resolved ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-rose-50 text-rose-700 border border-rose-200'"
                >
                  {{ item.resolved ? 'Đã Khớp' : 'Lệch Số Dư' }}
                </span>
              </td>
              <td class="py-3.5 px-4 text-right">
                <button
                  v-if="!item.resolved"
                  type="button"
                  @click="resolveAlert(item)"
                  data-testid="btn-resolve-alert"
                  class="px-3 py-1.5 bg-primary-600 hover:bg-primary-700 text-white font-semibold rounded-lg text-xs transition-all"
                >
                  Xác Nhận Đã Xử Lý
                </button>
                <span v-else class="text-text-muted text-[11px]">Hoàn tất</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
