<script setup>
import { ref, computed, onMounted, h } from 'vue'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert'
import { Skeleton } from '@/components/ui/skeleton'
import FormField from '@/shared/components/FormField.vue'
import DataTable from '@/components/ui/table/DataTable.vue'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
  DialogClose
} from '@/components/ui/dialog'
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
import { partnerApi } from '@/modules/partner/api'
import { toast } from 'vue-sonner'
import {
  Wallet,
  Lock,
  Clock,
  Building,
  ArrowDownLeft,
  AlertCircle,
  RefreshCw,
  AlertTriangle,
  ShieldCheck,
  CreditCard
} from 'lucide-vue-next'

// =========================================================================
// 1. FINTECH WALLET STATE (Authoritative Backend State — Zero Hardcoding)
// =========================================================================
const isLoadingWallet = ref(false)
const walletError = ref(null)
const walletData = ref(null)

const availableBalance = computed(() => walletData.value?.availableBalance ?? null)
const pendingSettlementBalance = computed(() => walletData.value?.pendingSettlementBalance ?? null)
const lockedBalance = computed(() => walletData.value?.lockedBalance ?? null)
const debtBalance = computed(() => walletData.value?.debtBalance ?? null)
const currency = computed(() => walletData.value?.currency || 'VND')

// =========================================================================
// 2. TRANSACTION HISTORY & WITHDRAWALS STATE
// =========================================================================
const isLoadingTransactions = ref(false)
const transactionsError = ref(null)
const transactionsList = ref([])

const isLoadingWithdrawals = ref(false)
const withdrawalsError = ref(null)
const withdrawalsList = ref([])

const totalWithdrawn = computed(() => {
  if (isLoadingWithdrawals.value || withdrawalsError.value) return null
  if (!withdrawalsList.value || withdrawalsList.value.length === 0) return 0
  return withdrawalsList.value
    .filter(w => w.status === 'APPROVED')
    .reduce((sum, w) => sum + (Number(w.amount) || 0), 0)
})

// Format helpers with proper precision handling (BigDecimal parity)
function formatCurrency(val) {
  if (val === undefined || val === null) return '—'
  return Number(val).toLocaleString('vi-VN')
}

function formatDateTime(val) {
  if (!val) return '—'
  try {
    const d = new Date(val)
    if (isNaN(d.getTime())) return String(val)
    return d.toLocaleString('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch (_) {
    return String(val)
  }
}

function maskAccountNumber(acc) {
  if (!acc) return ''
  const str = String(acc).trim()
  if (str.length <= 4) return str
  return '•'.repeat(Math.max(4, str.length - 4)) + str.slice(-4)
}

// =========================================================================
// 3. DATA LOADERS (Independent Execution & Explicit Error Propagation)
// =========================================================================
async function fetchWallet() {
  isLoadingWallet.value = true
  walletError.value = null
  try {
    const res = await partnerApi.getWallet()
    walletData.value = res?.data || res || null
  } catch (err) {
    walletError.value = err?.message || 'Không thể tải thông tin ví nhà xe từ máy chủ Backend'
    walletData.value = null
  } finally {
    isLoadingWallet.value = false
  }
}

async function fetchTransactions() {
  isLoadingTransactions.value = true
  transactionsError.value = null
  try {
    const res = await partnerApi.getWalletTransactions()
    const items = res?.data || res || []
    transactionsList.value = Array.isArray(items) ? items : []
  } catch (err) {
    transactionsError.value = err?.message || 'Không thể tải lịch sử giao dịch từ máy chủ Backend'
    transactionsList.value = []
  } finally {
    isLoadingTransactions.value = false
  }
}

async function fetchWithdrawals() {
  isLoadingWithdrawals.value = true
  withdrawalsError.value = null
  try {
    const res = await partnerApi.getWithdrawals()
    const items = res?.data || res || []
    withdrawalsList.value = Array.isArray(items) ? items : []
    return { success: true, items: withdrawalsList.value }
  } catch (err) {
    withdrawalsError.value = err?.message || 'Không thể tải danh sách lệnh rút tiền từ máy chủ Backend'
    withdrawalsList.value = []
    return { success: false, error: withdrawalsError.value }
  } finally {
    isLoadingWithdrawals.value = false
  }
}

const isRefreshing = ref(false)
async function fetchDashboardData() {
  isRefreshing.value = true
  try {
    await Promise.allSettled([
      fetchWallet(),
      fetchTransactions(),
      fetchWithdrawals()
    ])
  } finally {
    isRefreshing.value = false
  }
}

onMounted(() => {
  restoreUnknownWithdrawOutcome()
  fetchDashboardData()
})

// =========================================================================
// 4. WITHDRAWAL MUTATION FLOW (Pre-validate -> Confirm -> Await -> Refetch)
// =========================================================================
const showWithdrawDialog = ref(false)
const isConfirmWithdrawOpen = ref(false)
const isSubmittingWithdraw = ref(false)
const isUnknownOutcome = ref(false)
const isVerifyingWalletState = ref(false)
const pendingMutationContext = ref(null)

const withdrawAmount = ref(1000000)
const withdrawBank = ref('Vietcombank')
const withdrawAccountNumber = ref('')
const withdrawAccountHolder = ref('')
const withdrawError = ref('')

function getUnknownWithdrawStorageKey() {
  const opId = walletData.value?.operatorId || 'default'
  return `velobus_unknown_withdraw_${opId}`
}

function restoreUnknownWithdrawOutcome() {
  try {
    const key = getUnknownWithdrawStorageKey()
    const stored = sessionStorage.getItem(key)
    if (stored) {
      const parsed = JSON.parse(stored)
      if (parsed && typeof parsed === 'object') {
        isUnknownOutcome.value = true
        pendingMutationContext.value = parsed
        withdrawError.value = 'Hệ thống phát hiện lệnh rút tiền trước đó chưa xác định kết quả do mất kết nối mạng / timeout. Hệ thống đã KHÓA gửi lại để phòng ngừa tạo nhiều lệnh rút trùng lặp. Vui lòng bấm "Kiểm tra trạng thái ví" để đối soát.'
      }
    }
  } catch (_) {}
}

function recordUnknownOutcome(payload, customReason) {
  isUnknownOutcome.value = true
  const opId = walletData.value?.operatorId || auth.user?.operatorId || auth.user?.id
  const context = {
    operatorId: opId || null,
    amount: Number(payload.amount),
    bankName: payload.bankName,
    maskedAccount: maskAccountNumber(payload.accountNumber),
    timestamp: Date.now()
  }
  pendingMutationContext.value = context
  const prefix = customReason ? `${customReason} ` : 'Yêu cầu rút tiền bị mất kết nối mạng hoặc quá hạn phản hồi (Network Timeout). '
  withdrawError.value = `${prefix}Do backend chưa hỗ trợ Idempotency Key, kết quả giao dịch tại máy chủ chưa thể xác định. Hệ thống giữ khóa an toàn fail-closed để phòng ngừa trừ tiền trùng lặp (KHÓA gửi lại). Vui lòng bấm "Kiểm tra trạng thái ví" hoặc đối soát thủ công với Admin.`

  try {
    const key = getUnknownWithdrawStorageKey()
    sessionStorage.setItem(key, JSON.stringify(context))
  } catch (_) {}
}

function clearUnknownOutcome() {
  isUnknownOutcome.value = false
  pendingMutationContext.value = null
  withdrawError.value = ''
  try {
    const key = getUnknownWithdrawStorageKey()
    sessionStorage.removeItem(key)
  } catch (_) {}
}

function openWithdrawModal() {
  if (!isUnknownOutcome.value) {
    withdrawError.value = ''
    const debt = Number(debtBalance.value) || 0
    if (debt > 0) {
      withdrawError.value = `Không thể tạo lệnh rút khi đang có khoản dư nợ đối soát (${formatCurrency(debt)} đ)`
    } else {
      const avail = Number(availableBalance.value) || 0
      withdrawAmount.value = avail >= 100000 ? Math.min(10000000, avail) : 100000
    }
  }
  showWithdrawDialog.value = true
}

function closeWithdrawModal() {
  showWithdrawDialog.value = false
}

function validateWithdrawForm() {
  withdrawError.value = ''
  const amount = Number(withdrawAmount.value)
  const avail = availableBalance.value !== null && availableBalance.value !== undefined ? Number(availableBalance.value) : null
  const debt = Number(debtBalance.value) || 0

  if (debt > 0) {
    withdrawError.value = `Không thể tạo lệnh rút khi đang có khoản dư nợ đối soát (${formatCurrency(debt)} đ)`
    return false
  }

  if (avail !== null && amount > avail) {
    withdrawError.value = `Số tiền rút vượt quá số dư khả dụng (${formatCurrency(avail)} đ)`
    return false
  }

  if (!amount || isNaN(amount) || amount < 100000) {
    withdrawError.value = 'Số tiền rút tối thiểu là 100.000 đ'
    return false
  }

  if (!withdrawBank.value || !withdrawBank.value.trim()) {
    withdrawError.value = 'Vui lòng chọn hoặc nhập tên ngân hàng thụ hưởng'
    return false
  }

  if (!withdrawAccountNumber.value || !withdrawAccountNumber.value.trim()) {
    withdrawError.value = 'Vui lòng nhập số tài khoản ngân hàng thụ hưởng'
    return false
  }

  if (!withdrawAccountHolder.value || !withdrawAccountHolder.value.trim()) {
    withdrawError.value = 'Vui lòng nhập tên chủ tài khoản thụ hưởng'
    return false
  }

  return true
}

function handleOpenConfirmDialog() {
  if (!validateWithdrawForm()) return
  isConfirmWithdrawOpen.value = true
}

async function confirmWithdrawSubmit() {
  if (isSubmittingWithdraw.value || isUnknownOutcome.value) return

  const payload = {
    amount: Number(withdrawAmount.value),
    bankName: withdrawBank.value.trim(),
    accountNumber: withdrawAccountNumber.value.trim(),
    accountHolderName: withdrawAccountHolder.value.trim().toUpperCase()
  }

  isSubmittingWithdraw.value = true
  withdrawError.value = ''
  try {
    const res = await partnerApi.requestWithdrawal(payload)
    const data = res?.data || res

    // PRE-02: Strict response shape & semantics validation
    if (!data || typeof data !== 'object') {
      recordUnknownOutcome(payload, 'Máy chủ trả về phản hồi không hợp lệ cho lệnh rút tiền (Malformed WithdrawalResponse).')
      throw new Error('Malformed WithdrawalResponse')
    }

    if (!data.id || !data.status) {
      recordUnknownOutcome(payload, 'Phản hồi rút tiền từ máy chủ thiếu mã định danh (id) hoặc trạng thái (status).')
      throw new Error('Incomplete WithdrawalResponse')
    }

    if (data.amount !== undefined && Number(data.amount) !== Number(payload.amount)) {
      recordUnknownOutcome(payload, 'Số tiền máy chủ ghi nhận không khớp với số tiền đã yêu cầu rút.')
      throw new Error('Withdrawal amount mismatch')
    }

    const statusText = data.status === 'PENDING'
      ? 'Đang chờ Admin duyệt chi (Trạng thái: PENDING)'
      : data.status === 'APPROVED'
        ? 'Đã được phê duyệt giải ngân (Trạng thái: APPROVED)'
        : data.status === 'REJECTED'
          ? 'Đã bị từ chối (Trạng thái: REJECTED)'
          : `Trạng thái: ${data.status}`

    toast.success('Gửi yêu cầu rút tiền thành công!', {
      description: `Đã tạo lệnh rút #${data.id} số tiền ${formatCurrency(data.amount || payload.amount)} đ về ${data.bankName || payload.bankName}. ${statusText}.`
    })

    // Mutation succeeded: clean up form and outcome
    clearUnknownOutcome()
    isConfirmWithdrawOpen.value = false
    showWithdrawDialog.value = false
    withdrawAccountNumber.value = ''
    withdrawAccountHolder.value = ''

    // Refetch authoritative backend data (Differentiate mutation success from refetch failure)
    try {
      await fetchDashboardData()
    } catch (_) {
      toast.warning('Lệnh rút tiền đã thành công nhưng không thể tự động làm mới số dư ví. Vui lòng bấm "Làm Mới".')
    }
  } catch (err) {
    isConfirmWithdrawOpen.value = false
    const errMsg = err?.message || ''
    const errCode = err?.code || ''
    const isTimeoutOrNetwork =
      errCode === 'NETWORK_ERROR' ||
      errCode === 'ECONNABORTED' ||
      errCode === 'ERR_NETWORK' ||
      errMsg.toLowerCase().includes('timeout') ||
      errMsg.toLowerCase().includes('kết nối') ||
      err?.name === 'TimeoutError' ||
      errMsg.includes('chưa xác định') ||
      errMsg.includes('Malformed') ||
      errMsg.includes('Incomplete') ||
      errMsg.includes('mismatch')

    if (isTimeoutOrNetwork) {
      if (!isUnknownOutcome.value) {
        recordUnknownOutcome(payload)
      }
    } else {
      withdrawError.value = errMsg || 'Máy chủ từ chối yêu cầu rút tiền'
    }
  } finally {
    isSubmittingWithdraw.value = false
  }
}

async function verifyWalletServerState() {
  isVerifyingWalletState.value = true
  try {
    const [wRes, _] = await Promise.all([
      fetchWithdrawals(),
      fetchWallet()
    ])

    // PRE-01: Verification failure MUST preserve unknown outcome lock!
    if (!wRes.success) {
      toast.error('Không thể kết nối đến máy chủ để xác minh danh sách lệnh rút. Tiếp tục KHÓA thao tác để đảm bảo an toàn.')
      return
    }

    // PRE-01 Fail-Closed Policy:
    // Backend Withdrawal API does not provide an authoritative Idempotency Key or Request Correlation ID.
    // Matching purely on amount/bankName is non-unique and unsafe.
    // Empty list or non-match is also NOT evidence that the mutation was not committed on the server.
    withdrawError.value = 'Hệ thống máy chủ chưa cung cấp Idempotency Key hoặc Correlation ID để xác định duy nhất kết quả của lệnh rút vừa bị gián đoạn. Để bảo vệ an toàn số dư ví và ngăn ngừa trừ tiền trùng lặp, hệ thống duy trì KHÓA thao tác rút tiền (fail-closed). Vui lòng liên hệ Ban quản trị để đối soát thủ công.'
    toast.warning('Chưa có bằng chứng xác thực giao dịch trước đã kết thúc. Tiếp tục KHÓA thao tác để bảo vệ số dư ví.')
  } catch (err) {
    toast.error('Lỗi khi kiểm tra trạng thái máy chủ: ' + (err?.message || ''))
  } finally {
    isVerifyingWalletState.value = false
  }
}

// =========================================================================
// 5. TANSTACK DATATABLE COLUMNS FOR WALLET TRANSACTIONS
// =========================================================================
const transactionColumns = [
  {
    accessorKey: 'id',
    header: 'Mã GD',
    cell: ({ row }) => {
      const item = row.original
      const displayId = item.paymentReference || `#TXN-${item.id}`
      return h('span', { class: 'font-mono font-bold text-foreground text-xs whitespace-nowrap' }, displayId)
    }
  },
  {
    accessorKey: 'transactionType',
    header: 'Loại Giao Dịch',
    cell: ({ row }) => {
      const type = row.original.transactionType
      const labelMap = {
        TICKET_ESCROW_CREDIT: 'Cộng tiền vé (Ký quỹ)',
        TRIP_SETTLEMENT: 'Quyết toán chuyến hoàn tất',
        WITHDRAWAL_LOCK: 'Khóa số dư rút tiền',
        WITHDRAWAL_PAYOUT: 'Giải ngân rút tiền',
        WITHDRAWAL_REFUND: 'Hoàn trả tiền rút',
        REFUND_REVERSAL: 'Thu hồi hoàn tiền'
      }
      return h('span', { class: 'text-xs text-foreground font-medium whitespace-nowrap' }, labelMap[type] || type || 'Giao dịch ví')
    }
  },
  {
    accessorKey: 'tripId',
    header: 'Chuyến / Đơn',
    cell: ({ row }) => {
      const item = row.original
      if (!item.tripId && !item.bookingId) {
        return h('span', { class: 'text-xs text-muted-foreground' }, '—')
      }
      return h('div', { class: 'space-y-0.5 text-xs whitespace-nowrap' }, [
        item.tripId ? h('span', { class: 'font-mono text-primary font-semibold block' }, `TRIP-${item.tripId}`) : null,
        item.bookingId ? h('span', { class: 'font-mono text-muted-foreground text-[11px] block' }, `BKG-${item.bookingId}`) : null
      ])
    }
  },
  {
    accessorKey: 'netAmount',
    header: () => h('div', { class: 'text-right' }, 'Biến Động (VNĐ)'),
    cell: ({ row }) => {
      const amount = Number(row.original.netAmount || 0)
      const isPositive = amount > 0
      const prefix = isPositive ? '+' : ''
      return h(
        'div',
        {
          class: [
            'text-right font-mono font-bold text-xs tabular-nums whitespace-nowrap',
            isPositive ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400'
          ]
        },
        `${prefix}${formatCurrency(amount)} đ`
      )
    }
  },
  {
    accessorKey: 'balanceAfter',
    header: () => h('div', { class: 'text-right' }, 'Số Dư Sau GD'),
    cell: ({ row }) => {
      const bal = row.original.balanceAfter
      return h(
        'div',
        { class: 'text-right font-mono font-medium text-xs tabular-nums text-foreground whitespace-nowrap' },
        bal !== undefined && bal !== null ? `${formatCurrency(bal)} đ` : '—'
      )
    }
  },
  {
    accessorKey: 'createdAt',
    header: 'Thời Gian',
    cell: ({ row }) => {
      const val = row.original.createdAt
      return h('span', { class: 'text-xs text-muted-foreground whitespace-nowrap' }, formatDateTime(val))
    }
  },
  {
    accessorKey: 'description',
    header: 'Diễn Giải',
    cell: ({ row }) => {
      return h('span', { class: 'text-xs text-muted-foreground line-clamp-1 max-w-[220px]' }, row.original.description || '—')
    }
  }
]
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-xl sm:text-2xl font-bold tracking-tight text-foreground">
          Tổng Quan Ví Ký Quỹ Escrow (4 Số Dư)
        </h1>
        <p class="text-xs text-muted-foreground mt-1">
          Hệ thống ví Fintech đa số dư: Quyết toán sau chuyến đi +2h • Chống rút trùng bằng cơ chế Khóa số dư (Locked Balance).
        </p>
      </div>

      <div class="flex items-center gap-2">
        <Button
          variant="outline"
          class="self-start sm:self-auto px-4 py-2 text-xs font-semibold gap-2 shadow-xs"
          data-testid="btn-refresh-wallet"
          :disabled="isRefreshing || isLoadingWallet || isLoadingTransactions"
          @click="fetchDashboardData"
        >
          <RefreshCw class="w-3.5 h-3.5" :class="{ 'animate-spin': isRefreshing || isLoadingWallet || isLoadingTransactions }" />
          <span>{{ isRefreshing ? 'Đang tải...' : 'Làm Mới' }}</span>
        </Button>

        <Button
          type="button"
          @click="openWithdrawModal"
          data-testid="btn-open-withdraw"
          :disabled="isLoadingWallet || !!walletError || !walletData"
          class="self-start sm:self-auto px-4 py-2 text-xs font-semibold gap-2 shadow-xs"
        >
          <ArrowDownLeft class="w-4 h-4" />
          <span>Yêu Cầu Rút Tiền</span>
        </Button>
      </div>
    </div>

    <!-- Error State for Wallet Summary -->
    <Alert v-if="walletError" variant="destructive" data-testid="wallet-error-alert">
      <AlertCircle class="h-4 w-4" />
      <AlertTitle>Không thể tải thông tin ví</AlertTitle>
      <AlertDescription class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mt-2 text-xs">
        <span>{{ walletError }}</span>
        <Button size="sm" variant="outline" class="self-start sm:self-auto h-7 px-3 text-xs bg-background text-foreground" @click="fetchWallet">
          Thử lại
        </Button>
      </AlertDescription>
    </Alert>

    <!-- Loading State: Skeletons for 4 Cards -->
    <div v-else-if="isLoadingWallet && !walletData" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4" data-testid="wallet-loading">
      <Card v-for="i in 4" :key="i" class="border-border bg-card p-4 space-y-3">
        <Skeleton class="h-4 w-28" />
        <Skeleton class="h-8 w-36" />
        <Skeleton class="h-3 w-40" />
      </Card>
    </div>

    <!-- 4 Fintech Balance Cards Grid (Authoritative Backend State) -->
    <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4" data-testid="wallet-cards-grid">
      <!-- 1. Available Balance -->
      <Card class="border-border bg-card shadow-xs border-emerald-500/40">
        <CardHeader class="pb-2 flex flex-row items-center justify-between space-y-0">
          <CardTitle class="text-xs font-bold uppercase tracking-wider text-muted-foreground">
            Số Dư Khả Dụng
          </CardTitle>
          <div class="p-2 rounded-lg bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 flex-shrink-0">
            <Wallet class="w-4 h-4" />
          </div>
        </CardHeader>
        <CardContent class="space-y-1">
          <p class="text-2xl font-extrabold text-emerald-600 dark:text-emerald-400 tabular-nums font-mono whitespace-nowrap" data-testid="available-balance">
            {{ formatCurrency(availableBalance) }}<span v-if="availableBalance !== null" class="text-xs font-bold ml-1">đ</span>
          </p>
          <p class="text-[11px] text-muted-foreground">
            Có thể tạo lệnh rút về ngân hàng ngay lập tức
          </p>
        </CardContent>
      </Card>

      <!-- 2. Pending Settlement Escrow -->
      <Card class="border-border bg-card shadow-xs">
        <CardHeader class="pb-2 flex flex-row items-center justify-between space-y-0">
          <CardTitle class="text-xs font-bold uppercase tracking-wider text-muted-foreground">
            Đang Ký Quỹ Chuyến
          </CardTitle>
          <div class="p-2 rounded-lg bg-amber-500/10 text-amber-600 dark:text-amber-400 flex-shrink-0">
            <Lock class="w-4 h-4" />
          </div>
        </CardHeader>
        <CardContent class="space-y-1">
          <p class="text-2xl font-extrabold text-amber-600 dark:text-amber-400 tabular-nums font-mono whitespace-nowrap" data-testid="pending-settlement-balance">
            {{ formatCurrency(pendingSettlementBalance) }}<span v-if="pendingSettlementBalance !== null" class="text-xs font-bold ml-1">đ</span>
          </p>
          <p class="text-[11px] text-muted-foreground">
            Tự động quyết toán sau khi xe đến đích +2 giờ
          </p>
        </CardContent>
      </Card>

      <!-- 3. Locked Balance for Withdrawal -->
      <Card class="border-border bg-card shadow-xs border-primary/30">
        <CardHeader class="pb-2 flex flex-row items-center justify-between space-y-0">
          <CardTitle class="text-xs font-bold uppercase tracking-wider text-muted-foreground">
            Đang Xử Lý Rút
          </CardTitle>
          <div class="p-2 rounded-lg bg-primary/10 text-primary flex-shrink-0">
            <Clock class="w-4 h-4" />
          </div>
        </CardHeader>
        <CardContent class="space-y-1">
          <p class="text-2xl font-extrabold text-primary tabular-nums font-mono whitespace-nowrap" data-testid="locked-balance">
            {{ formatCurrency(lockedBalance) }}<span v-if="lockedBalance !== null" class="text-xs font-bold ml-1">đ</span>
          </p>
          <p class="text-[11px] text-muted-foreground">
            Lệnh rút tiền đang chờ Admin phê duyệt chi
          </p>
        </CardContent>
      </Card>

      <!-- 4. Total Withdrawn or Debt Balance -->
      <Card
        class="border-border bg-card shadow-xs"
        :class="{ 'border-destructive/40 bg-destructive/5': debtBalance && Number(debtBalance) > 0 }"
      >
        <CardHeader class="pb-2 flex flex-row items-center justify-between space-y-0">
          <CardTitle class="text-xs font-bold uppercase tracking-wider text-muted-foreground">
            {{ debtBalance && Number(debtBalance) > 0 ? 'Dư Nợ Đối Soát' : 'Tổng Tiền Đã Rút' }}
          </CardTitle>
          <div
            class="p-2 rounded-lg flex-shrink-0"
            :class="debtBalance && Number(debtBalance) > 0 ? 'bg-destructive/10 text-destructive' : 'bg-muted text-muted-foreground'"
          >
            <AlertTriangle v-if="debtBalance && Number(debtBalance) > 0" class="w-4 h-4" />
            <Building v-else class="w-4 h-4" />
          </div>
        </CardHeader>
        <CardContent class="space-y-1">
          <p
            class="text-2xl font-extrabold tabular-nums font-mono whitespace-nowrap"
            :class="debtBalance && Number(debtBalance) > 0 ? 'text-destructive' : 'text-foreground'"
            data-testid="total-withdrawn"
          >
            <template v-if="debtBalance && Number(debtBalance) > 0">
              -{{ formatCurrency(debtBalance) }}<span class="text-xs font-bold ml-1 text-destructive">đ</span>
            </template>
            <template v-else-if="totalWithdrawn !== null">
              {{ formatCurrency(totalWithdrawn) }}<span class="text-xs font-bold ml-1 text-muted-foreground">đ</span>
            </template>
            <template v-else>
              <span class="text-muted-foreground font-normal text-xl">—</span>
            </template>
          </p>
          <p class="text-[11px] text-muted-foreground">
            {{ debtBalance && Number(debtBalance) > 0 ? 'Tự động cấn trừ khi có dòng tiền mới' : 'Lũy kế giải ngân thành công từ khi mở sàn' }}
          </p>
        </CardContent>
      </Card>
    </div>

    <!-- Transaction History Table with Reusable TanStack DataTable -->
    <Card class="border-border bg-card shadow-xs overflow-hidden">
      <CardHeader class="p-5 border-b border-border flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <CardTitle class="text-base font-bold text-foreground flex items-center gap-2">
            <ShieldCheck class="w-4 h-4 text-primary" />
            <span>Lịch Sử Biến Động Số Dư & Đối Soát</span>
          </CardTitle>
          <CardDescription class="text-xs text-muted-foreground mt-0.5">
            Mọi biến động ký quỹ, quyết toán và rút tiền được ghi nhận kiểm toán theo thời gian thực.
          </CardDescription>
        </div>
        <Badge variant="outline" class="text-xs font-semibold self-start sm:self-auto whitespace-nowrap">
          {{ transactionsList.length }} giao dịch
        </Badge>
      </CardHeader>

      <CardContent class="p-4 space-y-3">
        <!-- Error Alert if Transactions fail -->
        <Alert v-if="transactionsError" variant="destructive" data-testid="transactions-error-alert">
          <AlertCircle class="h-4 w-4" />
          <AlertTitle>Không thể tải lịch sử giao dịch</AlertTitle>
          <AlertDescription class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mt-2 text-xs">
            <span>{{ transactionsError }}</span>
            <Button size="sm" variant="outline" class="self-start sm:self-auto h-7 px-3 text-xs bg-background text-foreground" @click="fetchTransactions">
              Thử lại
            </Button>
          </AlertDescription>
        </Alert>

        <DataTable
          v-else
          :columns="transactionColumns"
          :data="transactionsList"
          :loading="isLoadingTransactions"
          :page-size="10"
          :get-row-id="(row) => String(row.id || row.paymentReference)"
          empty-text="Hiện chưa có giao dịch ví nào được ghi nhận từ máy chủ."
        />
      </CardContent>
    </Card>

    <!-- Withdrawal Request Modal Dialog (Section 8) -->
    <Dialog v-model:open="showWithdrawDialog">
      <DialogContent class="max-w-md">
        <DialogHeader>
          <DialogTitle class="text-base font-bold text-foreground flex items-center gap-2">
            <ArrowDownLeft class="w-4 h-4 text-primary" />
            <span>Yêu Cầu Rút Tiền Về Ngân Hàng</span>
          </DialogTitle>
          <DialogDescription class="text-xs text-muted-foreground">
            Lập lệnh rút số dư khả dụng về tài khoản ngân hàng thụ hưởng đã xác thực.
          </DialogDescription>
        </DialogHeader>

        <div class="space-y-4 py-2 text-xs">
          <!-- Balance Summary Banner -->
          <div class="p-3 rounded-lg border border-primary/20 bg-primary/10 text-foreground space-y-1">
            <div class="flex justify-between font-medium">
              <span>Số dư khả dụng hiện tại:</span>
              <strong class="text-primary tabular-nums font-mono">{{ formatCurrency(availableBalance) }} đ</strong>
            </div>
            <p class="text-[11px] text-muted-foreground">
              Khoản tiền rút sẽ được chuyển sang trạng thái KHÓA (LOCKED) và chờ Admin giải ngân.
            </p>
          </div>

          <!-- Alert for Error or Unknown Outcome Lock -->
          <Alert v-if="withdrawError" variant="destructive" class="py-2.5 px-3">
            <AlertCircle class="h-4 w-4" />
            <AlertDescription class="text-xs leading-relaxed font-medium">
              {{ withdrawError }}
            </AlertDescription>
          </Alert>

          <form @submit.prevent="handleOpenConfirmDialog" class="space-y-3.5">
            <FormField label="Số tiền muốn rút (VNĐ)" required>
              <Input
                v-model.number="withdrawAmount"
                type="number"
                min="100000"
                step="50000"
                :disabled="isSubmittingWithdraw || isUnknownOutcome"
                data-testid="input-withdraw-amount"
                class="h-10 text-xs font-mono font-bold"
              />
              <p class="text-[11px] text-muted-foreground mt-1">Số tiền rút tối thiểu: 100.000 đ</p>
            </FormField>

            <FormField label="Ngân hàng thụ hưởng" required>
              <Input
                v-model="withdrawBank"
                type="text"
                placeholder="Ví dụ: Vietcombank, Techcombank, MB Bank..."
                :disabled="isSubmittingWithdraw || isUnknownOutcome"
                data-testid="input-withdraw-bank"
                class="h-10 text-xs"
              />
            </FormField>

            <FormField label="Số tài khoản ngân hàng" required>
              <Input
                v-model="withdrawAccountNumber"
                type="text"
                placeholder="Nhập số tài khoản thụ hưởng..."
                :disabled="isSubmittingWithdraw || isUnknownOutcome"
                data-testid="input-withdraw-account-number"
                class="h-10 text-xs font-mono"
              />
            </FormField>

            <FormField label="Tên chủ tài khoản thụ hưởng" required>
              <Input
                v-model="withdrawAccountHolder"
                type="text"
                placeholder="Ví dụ: NGUYEN VAN A"
                :disabled="isSubmittingWithdraw || isUnknownOutcome"
                data-testid="input-withdraw-account-holder"
                class="h-10 text-xs uppercase"
              />
            </FormField>

            <DialogFooter class="pt-2 flex flex-col sm:flex-row gap-2">
              <Button
                v-if="isUnknownOutcome"
                type="button"
                variant="secondary"
                :disabled="isVerifyingWalletState"
                data-testid="btn-verify-wallet-state"
                @click="verifyWalletServerState"
              >
                <RefreshCw v-if="isVerifyingWalletState" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
                <span>{{ isVerifyingWalletState ? 'Đang xác minh...' : 'Kiểm tra trạng thái ví' }}</span>
              </Button>

              <Button
                type="button"
                variant="outline"
                size="sm"
                class="text-xs"
                :disabled="isSubmittingWithdraw || isVerifyingWalletState"
                @click="closeWithdrawModal"
              >
                Hủy Bỏ
              </Button>

              <Button
                type="submit"
                size="sm"
                data-testid="btn-submit-withdraw"
                :disabled="isSubmittingWithdraw || isUnknownOutcome || isVerifyingWalletState || !availableBalance || availableBalance < 100000 || (debtBalance && Number(debtBalance) > 0)"
                class="text-xs font-semibold shadow-xs"
                @click.prevent="handleOpenConfirmDialog"
              >
                Gửi Yêu Cầu Rút
              </Button>
            </DialogFooter>
          </form>
        </div>
      </DialogContent>
    </Dialog>

    <!-- Withdrawal Confirmation AlertDialog (Section 8: Mandatory Confirmation Step) -->
    <AlertDialog v-model:open="isConfirmWithdrawOpen">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle class="flex items-center gap-2">
            <CreditCard class="w-5 h-5 text-primary" />
            <span>Xác nhận thông tin rút tiền</span>
          </AlertDialogTitle>
          <AlertDialogDescription class="text-xs space-y-2.5 mt-2">
            <div class="p-3 rounded-lg bg-muted/60 border border-border space-y-1.5 text-foreground">
              <div class="flex justify-between">
                <span class="text-muted-foreground">Số tiền yêu cầu rút:</span>
                <strong class="font-mono text-primary text-sm">{{ formatCurrency(withdrawAmount) }} đ</strong>
              </div>
              <div class="flex justify-between">
                <span class="text-muted-foreground">Ngân hàng:</span>
                <span class="font-semibold">{{ withdrawBank }}</span>
              </div>
              <div class="flex justify-between">
                <span class="text-muted-foreground">Số tài khoản:</span>
                <span class="font-mono font-medium">{{ maskAccountNumber(withdrawAccountNumber) }}</span>
              </div>
              <div class="flex justify-between">
                <span class="text-muted-foreground">Chủ tài khoản:</span>
                <span class="font-semibold uppercase">{{ withdrawAccountHolder }}</span>
              </div>
            </div>
            <p class="text-muted-foreground text-[11px] leading-relaxed">
              Sau khi xác nhận, số tiền {{ formatCurrency(withdrawAmount) }} đ sẽ được chuyển sang số dư khóa (LOCKED) và trừ khỏi số dư khả dụng. Yêu cầu sẽ được chuyển đến Admin phê duyệt.
            </p>
          </AlertDialogDescription>
        </AlertDialogHeader>

        <AlertDialogFooter>
          <AlertDialogCancel :disabled="isSubmittingWithdraw" @click="isConfirmWithdrawOpen = false">
            Quay lại
          </AlertDialogCancel>
          <AlertDialogAction as-child>
            <Button
              :disabled="isSubmittingWithdraw"
              data-testid="btn-confirm-withdraw-action"
              @click.prevent="confirmWithdrawSubmit"
            >
              <RefreshCw v-if="isSubmittingWithdraw" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
              <span>{{ isSubmittingWithdraw ? 'Đang gửi...' : 'Xác nhận tạo lệnh' }}</span>
            </Button>
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>
