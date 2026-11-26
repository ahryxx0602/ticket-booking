<script setup>
import { ref, computed, onMounted, h } from 'vue'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import DataTable from '@/components/ui/table/DataTable.vue'
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogCancel,
  AlertDialogAction
} from '@/components/ui/alert-dialog'
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert'
import { Skeleton } from '@/components/ui/skeleton'
import { useAdminPermissions } from '@/modules/admin/composables/useAdminPermissions'
import { adminApi } from '@/modules/admin/api'
import { toast } from 'vue-sonner'
import {
  TrendingUp,
  DollarSign,
  Ticket,
  Bus,
  RefreshCw,
  ShieldCheck,
  AlertTriangle,
  AlertCircle
} from 'lucide-vue-next'

const { hasPermission } = useAdminPermissions()

// Real Backend State — Strictly separated permissions and loading/error states
const isLoadingReport = ref(false)
const reportError = ref(null)
const reportData = ref(null)

const isLoadingPayments = ref(false)
const paymentsError = ref(null)
const reconciliationList = ref([])
const reconciliationTotal = ref(0)

const isRunningReconciliation = ref(false)
const selectedAlertForConfirm = ref(null)
const isConfirmResolveOpen = ref(false)
const isResolving = ref(false)
const resolveNote = ref('')

// Formatting helpers — Distinction between Valid Zero and Unconfirmed Data (dash)
function formatCurrency(val) {
  if (val === undefined || val === null) return '—'
  return Number(val).toLocaleString('vi-VN')
}

function formatCount(val) {
  if (val === undefined || val === null) return '—'
  return Number(val).toLocaleString('vi-VN')
}

// 1. Fetch Payment Report (PAYMENT_REPORT_VIEW)
async function fetchReport() {
  if (!hasPermission('PAYMENT_REPORT_VIEW')) {
    reportData.value = null
    reportError.value = null
    return
  }

  isLoadingReport.value = true
  reportError.value = null

  try {
    const res = await adminApi.getPaymentReport()
    const report = res?.data || res || null
    reportData.value = report
    if (Array.isArray(report?.reconciliationTransactions)) {
      reconciliationList.value = report.reconciliationTransactions
      reconciliationTotal.value = report.reconciliationCount ?? report.reconciliationTransactions.length
    }
  } catch (err) {
    reportError.value = err?.message || 'Không thể tải báo cáo doanh thu từ máy chủ Backend'
    reportData.value = null
  } finally {
    isLoadingReport.value = false
  }
}

// 2. Fetch Reconciliation Payments (PAYMENT_LIST_VIEW)
async function fetchReconciliationPayments() {
  if (!hasPermission('PAYMENT_LIST_VIEW')) {
    reconciliationList.value = []
    paymentsError.value = null
    return
  }

  // If already populated by authoritative report, skip redundant fetch
  if (reconciliationList.value.length > 0) {
    return
  }

  isLoadingPayments.value = true
  paymentsError.value = null

  try {
    const res = await adminApi.getPayments({ reconciliationRequired: true, page: 0, size: 20 })
    const pageData = res?.data || res || {}
    reconciliationList.value = Array.isArray(pageData.items) ? pageData.items : (Array.isArray(pageData) ? pageData : [])
    reconciliationTotal.value = pageData.totalElements ?? reconciliationList.value.length
  } catch (err) {
    paymentsError.value = err?.message || 'Không thể tải danh sách cảnh báo đối soát từ máy chủ Backend'
    reconciliationList.value = []
  } finally {
    isLoadingPayments.value = false
  }
}

// Composite loader for mounted and refresh
async function fetchDashboardData() {
  if (hasPermission('PAYMENT_REPORT_VIEW')) {
    await fetchReport()
  }
  if (hasPermission('PAYMENT_LIST_VIEW') && reconciliationList.value.length === 0 && !reportError.value) {
    await fetchReconciliationPayments()
  }
}

onMounted(() => {
  fetchDashboardData()
})

// Trigger manual refresh / reconciliation sync with strict error propagation
async function triggerManualReconciliation() {
  isRunningReconciliation.value = true
  try {
    if (hasPermission('PAYMENT_REPORT_VIEW')) {
      await fetchReport()
    }
    if (hasPermission('PAYMENT_LIST_VIEW')) {
      if (reconciliationList.value.length === 0 || !hasPermission('PAYMENT_REPORT_VIEW')) {
        await fetchReconciliationPayments()
      }
    }

    if (reportError.value || paymentsError.value) {
      throw new Error(reportError.value || paymentsError.value)
    }

    toast.success('Đồng bộ đối soát hoàn tất', {
      description: 'Đã tải dữ liệu đối soát và báo cáo mới nhất từ máy chủ Backend.'
    })
  } catch (err) {
    toast.error('Đồng bộ thất bại', {
      description: err?.message || 'Lỗi khi đồng bộ dữ liệu đối soát từ máy chủ'
    })
  } finally {
    isRunningReconciliation.value = false
  }
}

function openResolveDialog(item) {
  selectedAlertForConfirm.value = item
  resolveNote.value = `Admin đối soát thủ công mã GD #${item.id || item.paymentReference}`
  isConfirmResolveOpen.value = true
}

// Production Mutation: Invariant enforcement & Mutation Safety
async function confirmResolveAlert() {
  const target = selectedAlertForConfirm.value
  if (!target || !hasPermission('PAYMENT_RECONCILE_RESOLVE')) {
    toast.error('Bạn không có quyền thực hiện thao tác này (PAYMENT_RECONCILE_RESOLVE)')
    return
  }

  if (isResolving.value) return

  isResolving.value = true
  try {
    const paymentId = target.id || target.paymentId
    if (!paymentId) {
      throw new Error('Mã định danh giao dịch (Payment ID) không hợp lệ')
    }

    // Call backend API
    await adminApi.reconcilePayment(paymentId, resolveNote.value)

    toast.success('Đã xác nhận xử lý đối soát', {
      description: `Giao dịch #${paymentId} đã được điều chỉnh và ghi nhận vào Audit Log.`
    })

    isConfirmResolveOpen.value = false
    selectedAlertForConfirm.value = null

    // Authoritative state refetch
    try {
      await fetchDashboardData()
    } catch (_) {
      toast.warning('Xử lý thành công nhưng làm mới dữ liệu thất bại. Vui lòng bấm làm mới.')
    }
  } catch (err) {
    toast.error('Xử lý đối soát thất bại', {
      description: err?.message || 'Lỗi kết nối hoặc máy chủ từ chối yêu cầu xử lý'
    })
  } finally {
    isResolving.value = false
  }
}

// Columns definition for reusable DataTable
const reconciliationColumns = [
  {
    accessorKey: 'id',
    header: 'Mã Đối Soát',
    cell: ({ row }) => {
      const item = row.original
      const displayId = item.paymentReference || `REC-${item.id}`
      return h('span', { class: 'font-mono font-bold text-foreground whitespace-nowrap' }, displayId)
    }
  },
  {
    accessorKey: 'bookingId',
    header: 'Mã Đơn / Tham Chiếu',
    cell: ({ row }) => {
      const item = row.original
      return h('div', { class: 'space-y-0.5 whitespace-nowrap' }, [
        h('span', { class: 'font-mono font-semibold text-primary block' }, item.bookingId ? `BKG-${item.bookingId}` : 'N/A'),
        h('span', { class: 'font-mono text-[11px] text-muted-foreground block' }, item.transactionId || item.vnpayTxnRef || '—')
      ])
    }
  },
  {
    accessorKey: 'amount',
    header: () => h('div', { class: 'text-right' }, 'Số Tiền'),
    cell: ({ row }) => {
      const item = row.original
      return h('div', { class: 'text-right font-mono font-bold tabular-nums whitespace-nowrap' }, [
        formatCurrency(item.amount || item.vnpayAmount),
        h('span', { class: 'text-muted-foreground ml-1 font-normal text-[11px]' }, 'đ')
      ])
    }
  },
  {
    accessorKey: 'paymentMethod',
    header: 'Phương Thức',
    cell: ({ row }) => {
      const method = row.original.paymentMethod || 'VNPAY'
      return h(Badge, { variant: 'outline', class: 'font-mono text-[11px] uppercase whitespace-nowrap' }, () => method)
    }
  },
  {
    accessorKey: 'status',
    header: 'Trạng Thái',
    cell: ({ row }) => {
      const isReq = row.original.reconciliationRequired
      const resolved = !isReq && row.original.status === 'SUCCESS'
      return h(
        Badge,
        {
          variant: 'outline',
          class: [
            'whitespace-nowrap font-semibold text-[11px]',
            resolved
              ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-400'
              : 'border-destructive/30 bg-destructive/10 text-destructive'
          ]
        },
        () => (resolved ? 'Đã Khớp' : 'Cần Đối Soát')
      )
    }
  },
  {
    id: 'actions',
    header: () => h('div', { class: 'text-right' }, 'Thao Tác'),
    cell: ({ row }) => {
      const item = row.original
      const isReq = item.reconciliationRequired
      if (!isReq) {
        return h('div', { class: 'text-right text-[11px] text-muted-foreground whitespace-nowrap' }, 'Hoàn tất')
      }
      if (!hasPermission('PAYMENT_RECONCILE_RESOLVE')) {
        return h('div', { class: 'text-right text-[11px] text-muted-foreground whitespace-nowrap' }, 'Chỉ xem')
      }
      return h(
        'div',
        { class: 'text-right whitespace-nowrap' },
        h(
          Button,
          {
            size: 'sm',
            class: 'h-8 text-xs font-semibold shadow-xs',
            'data-testid': 'btn-resolve-alert',
            onClick: () => openResolveDialog(item)
          },
          () => 'Xác Nhận Đã Xử Lý'
        )
      )
    }
  }
]

const activeAlertsCount = computed(() => {
  return reconciliationList.value.filter(a => a.reconciliationRequired).length
})
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-xl sm:text-2xl font-bold tracking-tight text-foreground">
          Tổng Quan Toàn Sàn & Báo Cáo Đối Soát
        </h1>
        <p class="text-xs text-muted-foreground mt-1">
          Giám sát doanh thu gộp, doanh thu ròng và đối soát tự động dòng tiền VNPay ↔ Ví Ký Quỹ Escrow.
        </p>
      </div>

      <div class="flex items-center gap-2">
        <Button
          v-if="hasPermission('PAYMENT_RECONCILE_RESOLVE') || hasPermission('PAYMENT_REPORT_VIEW')"
          variant="outline"
          class="self-start sm:self-auto px-4 py-2 text-xs font-semibold gap-2 shadow-xs"
          data-testid="btn-run-reconciliation"
          :disabled="isRunningReconciliation || isLoadingReport || isLoadingPayments"
          @click="triggerManualReconciliation"
        >
          <RefreshCw class="w-3.5 h-3.5" :class="{ 'animate-spin': isRunningReconciliation || isLoadingReport || isLoadingPayments }" />
          <span>{{ isRunningReconciliation ? 'Đang đồng bộ...' : 'Làm Mới Báo Cáo' }}</span>
        </Button>
      </div>
    </div>

    <!-- Error State for Payment Report with Retry Button (B3-FINAL-02: Isolated State) -->
    <Alert v-if="reportError" variant="destructive" data-testid="dashboard-report-error">
      <AlertCircle class="h-4 w-4" />
      <AlertTitle>Không thể tải báo cáo doanh thu tài chính</AlertTitle>
      <AlertDescription class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mt-2 text-xs">
        <span>{{ reportError }}</span>
        <Button size="sm" variant="outline" class="self-start sm:self-auto h-7 px-3 text-xs bg-background text-foreground" @click="fetchReport">
          Thử lại
        </Button>
      </AlertDescription>
    </Alert>

    <!-- Loading State: Skeletons for KPI Report -->
    <div v-else-if="isLoadingReport && !reportData" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4" data-testid="dashboard-report-loading">
      <Card v-for="i in 4" :key="i" class="border-border bg-card p-4 space-y-3">
        <Skeleton class="h-4 w-24" />
        <Skeleton class="h-8 w-36" />
        <Skeleton class="h-3 w-28" />
      </Card>
    </div>

    <!-- 4 KPI System Cards (B3-FINAL-01: Correct Financial Semantics & Zero Hardcoded Fallbacks) -->
    <div v-else-if="hasPermission('PAYMENT_REPORT_VIEW') && reportData" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4" data-testid="dashboard-kpi-grid">
      <!-- 1. Gross Revenue (Doanh thu gộp: grossRevenue) -->
      <Card class="border-border bg-card shadow-xs">
        <CardHeader class="pb-2 flex flex-row items-center justify-between space-y-0">
          <CardTitle class="text-xs font-bold uppercase tracking-wider text-muted-foreground">
            Doanh Thu Gộp
          </CardTitle>
          <div class="p-2 rounded-lg bg-primary/10 text-primary flex-shrink-0">
            <TrendingUp class="w-4 h-4" />
          </div>
        </CardHeader>
        <CardContent class="space-y-1">
          <p class="text-2xl font-extrabold text-foreground tabular-nums font-mono whitespace-nowrap" data-testid="kpi-gmv" data-kpi="gross-revenue">
            {{ formatCurrency(reportData.grossRevenue) }}<span v-if="reportData.grossRevenue !== null && reportData.grossRevenue !== undefined" class="text-xs font-bold text-muted-foreground ml-1">đ</span>
          </p>
          <p class="text-[11px] text-muted-foreground">
            Tổng giá trị thanh toán phát sinh qua cổng VNPay
          </p>
        </CardContent>
      </Card>

      <!-- 2. Net Revenue (Doanh thu ròng: netRevenue) -->
      <Card class="border-border bg-card shadow-xs border-primary/30">
        <CardHeader class="pb-2 flex flex-row items-center justify-between space-y-0">
          <CardTitle class="text-xs font-bold uppercase tracking-wider text-muted-foreground">
            Doanh Thu Ròng
          </CardTitle>
          <div class="p-2 rounded-lg bg-primary/10 text-primary flex-shrink-0">
            <DollarSign class="w-4 h-4" />
          </div>
        </CardHeader>
        <CardContent class="space-y-1">
          <p class="text-2xl font-extrabold text-primary tabular-nums font-mono whitespace-nowrap" data-testid="kpi-commission" data-kpi="net-revenue">
            {{ formatCurrency(reportData.netRevenue) }}<span v-if="reportData.netRevenue !== null && reportData.netRevenue !== undefined" class="text-xs font-bold ml-1">đ</span>
          </p>
          <p class="text-[11px] text-muted-foreground">
            Doanh thu thực tế sau khi trừ các khoản hoàn tiền
          </p>
        </CardContent>
      </Card>

      <!-- 3. Successful Payments (Thanh toán thành công: successCount) -->
      <Card class="border-border bg-card shadow-xs">
        <CardHeader class="pb-2 flex flex-row items-center justify-between space-y-0">
          <CardTitle class="text-xs font-bold uppercase tracking-wider text-muted-foreground">
            Thanh Toán Thành Công
          </CardTitle>
          <div class="p-2 rounded-lg bg-amber-500/10 text-amber-600 dark:text-amber-400 flex-shrink-0">
            <Ticket class="w-4 h-4" />
          </div>
        </CardHeader>
        <CardContent class="space-y-1">
          <p class="text-2xl font-extrabold text-foreground tabular-nums font-mono whitespace-nowrap" data-testid="kpi-bookings" data-kpi="success-count">
            {{ formatCount(reportData.successCount) }}<span v-if="reportData.successCount !== null && reportData.successCount !== undefined" class="text-xs font-bold text-muted-foreground ml-1">GD</span>
          </p>
          <p class="text-[11px] text-muted-foreground">
            Tổng số giao dịch thanh toán thành công qua cổng
          </p>
        </CardContent>
      </Card>

      <!-- 4. Pending Reconciliation (Cần đối soát: reconciliationCount) -->
      <Card class="border-border bg-card shadow-xs">
        <CardHeader class="pb-2 flex flex-row items-center justify-between space-y-0">
          <CardTitle class="text-xs font-bold uppercase tracking-wider text-muted-foreground">
            Cần Đối Soát
          </CardTitle>
          <div class="p-2 rounded-lg bg-rose-500/10 text-rose-600 dark:text-rose-400 flex-shrink-0">
            <AlertTriangle class="w-4 h-4" />
          </div>
        </CardHeader>
        <CardContent class="space-y-1">
          <p class="text-2xl font-extrabold text-foreground tabular-nums font-mono whitespace-nowrap" data-testid="kpi-operators" data-kpi="reconciliation-count">
            {{ formatCount(reportData.reconciliationCount) }}<span v-if="reportData.reconciliationCount !== null && reportData.reconciliationCount !== undefined" class="text-xs font-bold text-muted-foreground ml-1">GD</span>
          </p>
          <p class="text-[11px] text-muted-foreground">
            Số giao dịch lệch số dư VNPay ↔ Escrow cần xử lý
          </p>
        </CardContent>
      </Card>
    </div>

    <!-- Reconciliation Alerts Panel with Reusable TanStack DataTable (Isolated PAYMENT_LIST_VIEW) -->
    <Card v-if="hasPermission('PAYMENT_LIST_VIEW')" class="border-border bg-card shadow-xs overflow-hidden">
      <CardHeader class="p-5 border-b border-border flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <CardTitle class="text-base font-bold text-foreground flex items-center gap-2">
            <ShieldCheck class="w-4 h-4 text-primary" />
            <span>Nhật Ký Cảnh Báo Đối Soát Tự Động (VNPay ↔ Ví Ký Quỹ Escrow)</span>
          </CardTitle>
          <CardContent class="p-0 text-xs text-muted-foreground mt-0.5">
            Dữ liệu tải trực tiếp từ máy chủ Backend, phát hiện lệch số dư và cho phép xử lý có kiểm toán.
          </CardContent>
        </div>
        <Badge
          v-if="!paymentsError"
          variant="outline"
          class="font-semibold text-xs self-start sm:self-auto whitespace-nowrap"
          :class="activeAlertsCount > 0 ? 'border-amber-500/30 bg-amber-500/10 text-amber-800 dark:text-amber-300' : 'border-emerald-500/30 bg-emerald-500/10 text-emerald-800 dark:text-emerald-300'"
        >
          {{ activeAlertsCount }} cảnh báo cần xử lý
        </Badge>
      </CardHeader>

      <CardContent class="p-4 space-y-3">
        <!-- Error Alert if Payment List fails -->
        <Alert v-if="paymentsError" variant="destructive" data-testid="payments-list-error">
          <AlertCircle class="h-4 w-4" />
          <AlertTitle>Không thể tải danh sách giao dịch đối soát</AlertTitle>
          <AlertDescription class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mt-2 text-xs">
            <span>{{ paymentsError }}</span>
            <Button size="sm" variant="outline" class="self-start sm:self-auto h-7 px-3 text-xs bg-background text-foreground" @click="fetchReconciliationPayments">
              Thử lại
            </Button>
          </AlertDescription>
        </Alert>

        <DataTable
          v-else
          :columns="reconciliationColumns"
          :data="reconciliationList"
          :loading="isLoadingPayments"
          :page-size="10"
          :get-row-id="(row) => String(row.id || row.paymentReference)"
          empty-text="Hiện không có giao dịch nào cần đối soát từ máy chủ."
        />
      </CardContent>
    </Card>

    <!-- Confirmation AlertDialog for Resolve Mutation (Section 6 & 10) -->
    <AlertDialog v-model:open="isConfirmResolveOpen">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle class="flex items-center gap-2">
            <AlertTriangle class="w-5 h-5 text-amber-500" />
            <span>Xác nhận xử lý cảnh báo đối soát</span>
          </AlertDialogTitle>
          <AlertDialogDescription class="text-xs space-y-2 mt-2">
            <p>
              Bạn sắp xác nhận đã xử lý đối soát cho giao dịch:
              <strong class="text-foreground font-mono">{{ selectedAlertForConfirm?.paymentReference || selectedAlertForConfirm?.id }}</strong>
              <span v-if="selectedAlertForConfirm?.bookingId"> (Đơn: <strong class="text-foreground font-mono">BKG-{{ selectedAlertForConfirm?.bookingId }}</strong>)</span>.
            </p>
            <p class="text-muted-foreground">
              Thao tác này sẽ gửi lệnh xác nhận đến máy chủ Backend và ghi nhận vào nhật ký kiểm toán hệ thống. Vui lòng xác thực trước khi tiếp tục.
            </p>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel :disabled="isResolving" @click="isConfirmResolveOpen = false">Hủy bỏ</AlertDialogCancel>
          <AlertDialogAction
            :disabled="isResolving"
            @click="confirmResolveAlert"
          >
            <RefreshCw v-if="isResolving" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
            <span>{{ isResolving ? 'Đang xử lý...' : 'Xác nhận xử lý' }}</span>
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>
