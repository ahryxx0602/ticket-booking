<script setup>
import { ref, computed, onMounted } from 'vue'
import { toast } from 'vue-sonner'
import {
  Wallet,
  CheckCircle2,
  XCircle,
  AlertCircle,
  Eye,
  EyeOff,
  RefreshCw,
  Loader2,
  Filter,
  ShieldAlert,
  Building2,
  Calendar,
  Lock,
  FileText
} from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter
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
import {
  Table,
  TableHeader,
  TableHead,
  TableBody,
  TableRow,
  TableCell
} from '@/components/ui/table'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import EmptyState from '@/shared/components/EmptyState.vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useAdminPermissions } from '@/modules/admin/composables/useAdminPermissions'
import { useAuthStore } from '@/modules/auth/store'
import { adminApi } from '@/modules/admin/api'

const { hasPermission } = useAdminPermissions()
const authStore = useAuthStore()

const withdrawals = ref([])
const isLoading = ref(false)
const loadError = ref('')
const selectedFilter = ref('PENDING') // Default is 'PENDING' according to backend default contract
const processingIds = ref(new Set())
const unmaskedAccountIds = ref(new Set())

// Persistent Unknown Outcome Registry (D2-PRE-02)
const unresolvedOperations = ref(new Map())
const isVerifying = ref(false)

const unresolvedList = computed(() => {
  return Array.from(unresolvedOperations.value.values())
})

const currentAdminId = computed(() => {
  return authStore.user?.id != null
    ? String(authStore.user.id)
    : (authStore.userId != null
      ? String(authStore.userId)
      : (authStore.jwtPayload?.userId != null
        ? String(authStore.jwtPayload.userId)
        : (authStore.jwtPayload?.id != null
          ? String(authStore.jwtPayload.id)
          : (authStore.jwtPayload?.sub != null ? String(authStore.jwtPayload.sub) : null))))
})

const WITHDRAWAL_FILTERS = [
  { value: 'PENDING', label: 'Chờ giải ngân' },
  { value: 'APPROVED', label: 'Đã duyệt chi' },
  { value: 'REJECTED', label: 'Bị từ chối' },
  { value: 'ALL', label: 'Tất cả lệnh' }
]

function getStorageKey(adminId, withdrawalId) {
  return `velobus_unknown_wdr_${adminId}_${withdrawalId}`
}

function loadPersistedUnknownOutcomes() {
  try {
    const adminId = currentAdminId.value
    if (!adminId) {
      unresolvedOperations.value = new Map()
      return
    }
    const prefix = `velobus_unknown_wdr_${adminId}_`
    const recovered = new Map()

    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key && key.startsWith(prefix)) {
        const raw = localStorage.getItem(key)
        if (raw) {
          try {
            const context = JSON.parse(raw)
            // Strict recovery context schema validation (FQA-03)
            if (
              context &&
              context.withdrawalId != null &&
              String(context.withdrawalId).length > 0 &&
              (context.action === 'APPROVE' || context.action === 'REJECT') &&
              typeof context.amount === 'number' &&
              typeof context.timestamp === 'number'
            ) {
              recovered.set(String(context.withdrawalId), {
                withdrawalId: context.withdrawalId,
                action: context.action,
                amount: context.amount,
                timestamp: context.timestamp
              })
            }
          } catch (_) {}
        }
      }
    }
    unresolvedOperations.value = recovered
  } catch (_) {
    // localStorage may be restricted in private/sandbox mode
  }
}

function persistUnknownOutcome(context) {
  const adminId = currentAdminId.value
  if (adminId && context && context.withdrawalId != null) {
    const numAmount = Number(context.amount)
    const validAmount = Number.isFinite(numAmount) && numAmount >= 0 ? numAmount : null
    const safeContext = {
      withdrawalId: context.withdrawalId,
      action: context.action === 'APPROVE' ? 'APPROVE' : 'REJECT',
      amount: validAmount,
      timestamp: typeof context.timestamp === 'number' ? context.timestamp : Date.now()
    }
    const key = getStorageKey(adminId, safeContext.withdrawalId)
    try {
      if (typeof window !== 'undefined' && window.localStorage && typeof window.localStorage.setItem === 'function') {
        window.localStorage.setItem(key, JSON.stringify(safeContext))
      }
    } catch (err) {
      console.warn('[AdminWithdrawals] Failed to persist unknown outcome to localStorage:', err)
      toast.warning('Bộ nhớ lưu trữ trình duyệt bị giới hạn; khóa an toàn chỉ duy trì trong phiên làm việc hiện tại.')
    }
    const updated = new Map(unresolvedOperations.value)
    updated.set(String(safeContext.withdrawalId), safeContext)
    unresolvedOperations.value = updated
  }
}

function clearPersistedUnknownOutcome(withdrawalId) {
  const adminId = currentAdminId.value
  if (adminId && withdrawalId != null) {
    const key = getStorageKey(adminId, withdrawalId)
    try {
      if (typeof window !== 'undefined' && window.localStorage && typeof window.localStorage.removeItem === 'function') {
        window.localStorage.removeItem(key)
      }
    } catch (_) {}
    const updated = new Map(unresolvedOperations.value)
    updated.delete(String(withdrawalId))
    unresolvedOperations.value = updated
  }
}

// Authoritative API fetching (D2-PRE-03)
async function fetchWithdrawals() {
  if (!hasPermission('WITHDRAWAL_VIEW')) {
    loadError.value = 'Bạn không có quyền xem danh sách lệnh rút tiền (Yêu cầu WITHDRAWAL_VIEW).'
    return
  }

  isLoading.value = true
  loadError.value = ''
  try {
    if (selectedFilter.value === 'ALL') {
      // Explicitly aggregate all three supported status lists with independent failure handling
      const [pendingRes, approvedRes, rejectedRes] = await Promise.all([
        adminApi.getWithdrawals('PENDING'),
        adminApi.getWithdrawals('APPROVED'),
        adminApi.getWithdrawals('REJECTED')
      ])

      const pData = pendingRes?.data?.data !== undefined ? pendingRes.data.data : pendingRes?.data
      const aData = approvedRes?.data?.data !== undefined ? approvedRes.data.data : approvedRes?.data
      const rData = rejectedRes?.data?.data !== undefined ? rejectedRes.data.data : rejectedRes?.data

      if (!Array.isArray(pData) || !Array.isArray(aData) || !Array.isArray(rData)) {
        throw new Error('Dữ liệu danh sách lệnh rút tiền từ máy chủ không hợp lệ.')
      }

      withdrawals.value = [...pData, ...aData, ...rData].map(normalizeWithdrawalItem)
    } else {
      const res = await adminApi.getWithdrawals(selectedFilter.value)
      const data = res?.data?.data !== undefined ? res.data.data : res?.data
      if (!Array.isArray(data)) {
        throw new Error('Dữ liệu danh sách lệnh rút tiền từ máy chủ không hợp lệ.')
      }
      withdrawals.value = data.map(normalizeWithdrawalItem)
    }
  } catch (err) {
    const status = err?.response?.status
    if (status === 403 || status === 401) {
      loadError.value = 'Truy cập bị từ chối: Phiên làm việc hết hạn hoặc thiếu quyền WITHDRAWAL_VIEW.'
    } else {
      loadError.value = err?.response?.data?.message || err?.message || 'Không thể kết nối đến máy chủ để tải danh sách rút tiền.'
    }
    withdrawals.value = []
  } finally {
    isLoading.value = false
  }
}

function normalizeWithdrawalItem(item) {
  const parsedAmount = Number(item.amount)
  const safeAmount = Number.isFinite(parsedAmount) && parsedAmount >= 0 ? parsedAmount : null
  return {
    ...item,
    id: item.id,
    operatorId: item.operatorId,
    amount: safeAmount,
    bankName: item.bankName,
    accountNumber: item.accountNumber,
    accountHolderName: item.accountHolderName,
    status: item.status, // Canonical: PENDING | APPROVED | REJECTED
    operatorName: item.operatorName || (item.operatorId ? `Nhà xe đối tác #${item.operatorId}` : 'Nhà xe đối tác')
  }
}

const filteredWithdrawals = computed(() => {
  if (selectedFilter.value === 'ALL') return withdrawals.value
  return withdrawals.value.filter(w => w.status === selectedFilter.value)
})

function maskAccountNumber(accNo, id) {
  if (!accNo) return '••••'
  if (unmaskedAccountIds.value.has(id)) return accNo
  const clean = String(accNo)
  if (clean.length <= 4) return clean
  return '••••' + clean.slice(-4)
}

function toggleMask(id) {
  if (unmaskedAccountIds.value.has(id)) {
    unmaskedAccountIds.value.delete(id)
  } else {
    unmaskedAccountIds.value.add(id)
  }
}

// =========================================================================
// REAL CONFIRMATION DIALOG & AUTHORITATIVE MUTATIONS (D2-PRE-01)
// =========================================================================
const confirmDialog = ref({
  isOpen: false,
  item: null,
  action: 'APPROVE', // 'APPROVE' | 'REJECT'
  adminNote: '',
  isSubmitting: false,
  error: ''
})

function openConfirmDialog(item, action) {
  if (!item || item.status !== 'PENDING') {
    toast.error('Chỉ có thể thao tác với lệnh rút tiền ở trạng thái PENDING.')
    return
  }

  if (!currentAdminId.value) {
    toast.error('Không thể xác thực danh tính quản trị viên. Vui lòng đăng nhập lại.')
    return
  }

  const isApprove = action === 'APPROVE'
  if (isApprove && !hasPermission('WITHDRAWAL_APPROVE')) {
    toast.error('Thiếu quyền phê duyệt giải ngân WITHDRAWAL_APPROVE')
    return
  }
  if (!isApprove && !hasPermission('WITHDRAWAL_REJECT')) {
    toast.error('Thiếu quyền từ chối giải ngân WITHDRAWAL_REJECT')
    return
  }

  if (processingIds.value.has(item.id)) {
    toast.warning('Yêu cầu xử lý lệnh này đang được thực hiện.')
    return
  }

  if (unresolvedOperations.value.has(String(item.id))) {
    toast.error('Lệnh này đang bị khóa an toàn do kết quả xử lý trước đó chưa xác định. Vui lòng xác thực trước.')
    return
  }

  confirmDialog.value = {
    isOpen: true,
    item,
    action,
    adminNote: '',
    isSubmitting: false,
    error: ''
  }
}

function closeConfirmDialog() {
  if (confirmDialog.value.isSubmitting) return
  confirmDialog.value.isOpen = false
  confirmDialog.value.item = null
  confirmDialog.value.error = ''
}

async function executeWithdrawalMutation() {
  const { item, action, adminNote } = confirmDialog.value
  if (!item) return

  if (item.status !== 'PENDING') {
    confirmDialog.value.error = 'Lệnh rút tiền không còn ở trạng thái PENDING hợp lệ.'
    return
  }

  if (unresolvedOperations.value.has(String(item.id))) {
    confirmDialog.value.error = 'Lệnh này đang bị khóa an toàn do kết quả xử lý trước đó chưa xác định.'
    return
  }

  if (!currentAdminId.value) {
    confirmDialog.value.error = 'Không thể xác thực danh tính quản trị viên.'
    return
  }

  const isApprove = action === 'APPROVE'
  if (isApprove && !hasPermission('WITHDRAWAL_APPROVE')) {
    confirmDialog.value.error = 'Thiếu quyền WITHDRAWAL_APPROVE'
    return
  }
  if (!isApprove && !hasPermission('WITHDRAWAL_REJECT')) {
    confirmDialog.value.error = 'Thiếu quyền WITHDRAWAL_REJECT'
    return
  }

  if (!isApprove && (!adminNote || adminNote.trim().length < 5)) {
    confirmDialog.value.error = 'Vui lòng nhập lý do từ chối (tối thiểu 5 ký tự) để nhà xe đối soát.'
    return
  }

  // Synchronous double-submission check
  if (processingIds.value.has(item.id)) return
  processingIds.value.add(item.id)
  confirmDialog.value.isSubmitting = true
  confirmDialog.value.error = ''

  const noteToSend = adminNote && adminNote.trim().length > 0
    ? adminNote.trim()
    : (isApprove ? 'Duyệt chi giải ngân sổ cái nội bộ' : 'Từ chối giải ngân')

  // Persist minimal recovery context before mutation dispatch (D2-PRE-02 / FQA-03)
  const operationContext = {
    withdrawalId: item.id,
    action: isApprove ? 'APPROVE' : 'REJECT',
    amount: item.amount,
    timestamp: Date.now()
  }
  persistUnknownOutcome(operationContext)

  try {
    const res = isApprove
      ? await adminApi.approveWithdrawal(item.id, noteToSend)
      : await adminApi.rejectWithdrawal(item.id, noteToSend)

    const resData = res?.data?.data !== undefined ? res.data.data : res?.data
    const expectedStatus = isApprove ? 'APPROVED' : 'REJECTED'

    if (!resData || typeof resData !== 'object') {
      throw new Error('Máy chủ trả về phản hồi không hợp lệ')
    }
    if (resData.id == null || String(resData.id) !== String(item.id)) {
      throw new Error(`Mã định danh trong phản hồi không khớp (#${resData.id} !== #${item.id})`)
    }
    if (resData.status !== expectedStatus) {
      throw new Error(`Trạng thái phản hồi không khớp kỳ vọng (${resData.status} !== ${expectedStatus})`)
    }
    if (resData.amount == null || resData.operatorId == null) {
      throw new Error('Dữ liệu phản hồi thiếu thông tin bắt buộc (amount hoặc operatorId)')
    }
    if (item.operatorId != null && resData.operatorId != null && String(resData.operatorId) !== String(item.operatorId)) {
      throw new Error(`Mã nhà xe đối tác trong phản hồi không khớp (#${resData.operatorId} !== #${item.operatorId})`)
    }
    if (Number(resData.amount) !== Number(item.amount)) {
      throw new Error(`Số tiền giải ngân trong phản hồi không khớp (#${resData.amount} !== #${item.amount})`)
    }

    // Definitive server success -> clear persistent recovery guard
    clearPersistedUnknownOutcome(item.id)
    confirmDialog.value.isOpen = false
    confirmDialog.value.item = null
    toast.success(`Đã ${isApprove ? 'phê duyệt' : 'từ chối'} giải ngân lệnh #${item.id} thành công.`)

    // Authoritative refresh
    try {
      await fetchWithdrawals()
    } catch (_) {
      toast.warning('Đã ghi nhận kết quả giải ngân, nhưng làm mới danh sách thất bại. Bấm Làm Mới để xem trạng thái mới nhất.')
    }
  } catch (err) {
    const isNetworkOrTimeout = !err.response || err.code === 'ECONNABORTED' || err.message?.includes('timeout') || err.message?.includes('Network Error')
    const isServerError = err?.response?.status && err.response.status >= 500

    if (isNetworkOrTimeout || isServerError) {
      // Retain guard in localStorage and surface Unknown Outcome Alert
      confirmDialog.value.isOpen = false
      confirmDialog.value.item = null
      toast.error('Kết nối máy chủ bị gián đoạn hoặc lỗi 5xx. Lệnh đã được khóa an toàn để xác thực.')
    } else {
      // Definitive backend business rejection (e.g. 400 Invalid Status, 404, 403, 409, 422)
      clearPersistedUnknownOutcome(item.id)
      confirmDialog.value.error = err?.response?.data?.message || err?.message || 'Thao tác giải ngân bị máy chủ từ chối.'
    }
  } finally {
    processingIds.value.delete(item.id)
    confirmDialog.value.isSubmitting = false
  }
}

// =========================================================================
// READ-ONLY AUTHORITATIVE STATUS VERIFICATION (D2-PRE-02)
// =========================================================================
async function verifyUnknownOutcome(withdrawalId) {
  if (!withdrawalId) return
  isVerifying.value = true
  try {
    // Read-only queries to check if record reached terminal APPROVED or REJECTED
    const [appRes, rejRes, pendRes] = await Promise.allSettled([
      adminApi.getWithdrawals('APPROVED'),
      adminApi.getWithdrawals('REJECTED'),
      adminApi.getWithdrawals('PENDING')
    ])

    let matchingRecord = null

    if (appRes.status === 'fulfilled') {
      const data = appRes.value?.data?.data || appRes.value?.data
      if (Array.isArray(data)) matchingRecord = data.find(w => String(w.id) === String(withdrawalId))
    }
    if (!matchingRecord && rejRes.status === 'fulfilled') {
      const data = rejRes.value?.data?.data || rejRes.value?.data
      if (Array.isArray(data)) matchingRecord = data.find(w => String(w.id) === String(withdrawalId))
    }

    if (matchingRecord && (matchingRecord.status === 'APPROVED' || matchingRecord.status === 'REJECTED')) {
      // Terminal state confirmed! Clear guard
      clearPersistedUnknownOutcome(withdrawalId)
      toast.success(`Xác thực thành công: Lệnh #${withdrawalId} đã ở trạng thái ${matchingRecord.status}. Đã gỡ bỏ khóa an toàn.`)
      await fetchWithdrawals()
    } else {
      // Check if still in PENDING
      let stillPending = false
      if (pendRes.status === 'fulfilled') {
        const data = pendRes.value?.data?.data || pendRes.value?.data
        if (Array.isArray(data) && data.some(w => String(w.id) === String(withdrawalId))) {
          stillPending = true
        }
      }

      if (stillPending) {
        toast.warning(`Lệnh #${withdrawalId} hiện vẫn ở trạng thái PENDING trên máy chủ. Giao dịch chưa được áp dụng hoặc đã bị hủy.`)
      } else {
        toast.error(`Chưa thể xác thực trạng thái dứt điểm của lệnh #${withdrawalId}. Khóa an toàn tiếp tục được duy trì.`)
      }
    }
  } catch (err) {
    toast.error('Lỗi khi truy vấn máy chủ để xác thực: ' + (err?.message || 'Lỗi mạng'))
  } finally {
    isVerifying.value = false
  }
}

onMounted(() => {
  loadPersistedUnknownOutcomes()
  fetchWithdrawals()
})
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold tracking-tight text-foreground">Kiểm Duyệt Lệnh Rút Tiền Ví Ký Quỹ Escrow</h1>
        <p class="text-xs text-muted-foreground mt-1">
          Duyệt giải ngân nội bộ (khấu trừ Locked Balance và cập nhật sổ cái) hoặc từ chối để hoàn trả số dư về ví khả dụng của đối tác.
        </p>
      </div>

      <div class="flex items-center gap-2">
        <Button variant="outline" size="sm" @click="fetchWithdrawals" class="h-8 text-xs">
          <RefreshCw class="w-3.5 h-3.5 mr-1" /> Làm mới
        </Button>
      </div>
    </div>

    <!-- Unknown Outcome Guard Alert Banner (D2-PRE-02) -->
    <Alert
      v-if="unresolvedList.length > 0"
      variant="destructive"
      class="border-destructive/50 bg-destructive/5 space-y-2"
    >
      <div class="flex items-start gap-2">
        <AlertCircle class="h-4 w-4 mt-0.5 text-destructive" />
        <div class="space-y-1 flex-1">
          <AlertTitle class="font-bold text-xs text-destructive">
            Cảnh báo an toàn tài chính: Kết quả chưa xác định (Phát hiện {{ unresolvedList.length }} giao dịch)
          </AlertTitle>
          <AlertDescription class="text-xs text-muted-foreground">
            Các yêu cầu sau gặp sự cố mạng hoặc phản hồi chưa dứt điểm. Thao tác tiếp theo trên các lệnh này bị khóa an toàn cho đến khi xác thực máy chủ.
          </AlertDescription>
        </div>
      </div>

      <div class="divide-y divide-border/40 pt-1">
        <div
          v-for="ctx in unresolvedList"
          :key="ctx.withdrawalId"
          class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 py-1.5 text-xs"
        >
          <div class="flex items-center gap-2">
            <Badge variant="outline" class="font-mono text-[11px]">#{{ ctx.withdrawalId }}</Badge>
            <span class="font-semibold text-foreground">
              Thao tác: {{ ctx.action === 'APPROVE' ? 'Duyệt Chi' : 'Từ Chối' }}
            </span>
            <span class="text-muted-foreground text-[11px] tabular-nums">
              ({{ new Date(ctx.timestamp).toLocaleTimeString('vi-VN') }})
            </span>
          </div>

          <Button
            size="sm"
            variant="outline"
            :disabled="isVerifying"
            @click="verifyUnknownOutcome(ctx.withdrawalId)"
            class="h-7 text-xs font-semibold"
          >
            <Loader2 v-if="isVerifying" class="w-3 h-3 mr-1 animate-spin" />
            <RefreshCw v-else class="w-3 h-3 mr-1" />
            Kiểm tra lại trạng thái
          </Button>
        </div>
      </div>
    </Alert>

    <!-- Error State -->
    <Alert v-if="loadError" variant="destructive" class="border-destructive/50 bg-destructive/5">
      <AlertCircle class="h-4 w-4" />
      <AlertTitle>Lỗi tải danh sách lệnh rút tiền</AlertTitle>
      <AlertDescription class="flex items-center justify-between">
        <span>{{ loadError }}</span>
        <Button variant="outline" size="sm" @click="fetchWithdrawals" class="ml-4 h-7 text-xs">
          <RefreshCw class="w-3.5 h-3.5 mr-1" /> Thử lại
        </Button>
      </AlertDescription>
    </Alert>

    <!-- Filter Toolbar -->
    <Card class="border-border">
      <CardContent class="p-4 flex items-center justify-between">
        <div class="flex items-center gap-2">
          <Filter class="w-4 h-4 text-muted-foreground" />
          <span class="text-xs font-semibold text-muted-foreground">Lọc theo:</span>
          <div class="flex flex-wrap gap-1">
            <Button
              v-for="f in WITHDRAWAL_FILTERS"
              :key="f.value"
              variant="ghost"
              size="sm"
              :class="[
                'h-8 text-xs font-medium',
                selectedFilter === f.value
                  ? 'bg-primary text-primary-foreground hover:bg-primary/90'
                  : 'text-muted-foreground hover:text-foreground'
              ]"
              @click="selectedFilter = f.value; fetchWithdrawals()"
            >
              {{ f.label }}
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- Withdrawals Table Card -->
    <Card class="border-border shadow-sm overflow-hidden">
      <!-- Loading State -->
      <div v-if="isLoading" class="p-12 text-center text-muted-foreground text-xs flex flex-col items-center justify-center space-y-3">
        <Loader2 class="w-8 h-8 animate-spin text-primary" />
        <p>Đang tải danh sách lệnh rút tiền từ máy chủ...</p>
      </div>

      <!-- Empty State -->
      <div v-else-if="filteredWithdrawals.length === 0" class="p-8">
        <EmptyState
          title="Không có yêu cầu rút tiền"
          description="Hiện tại không có yêu cầu giải ngân nào phù hợp với bộ lọc trạng thái đã chọn."
        />
      </div>

      <!-- Authoritative Table -->
      <div v-else class="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow class="bg-muted/40 hover:bg-muted/40">
              <TableHead class="py-3 px-4 font-semibold text-xs">Mã Yêu Cầu</TableHead>
              <TableHead class="py-3 px-4 font-semibold text-xs">Đối Tác Nhà Xe</TableHead>
              <TableHead class="py-3 px-4 font-semibold text-xs text-right">Số Tiền Rút (Locked)</TableHead>
              <TableHead class="py-3 px-4 font-semibold text-xs">Tài Khoản Nhận</TableHead>
              <TableHead class="py-3 px-4 font-semibold text-xs">Ngày Tạo Lệnh</TableHead>
              <TableHead class="py-3 px-4 font-semibold text-xs text-center">Trạng Thái</TableHead>
              <TableHead class="py-3 px-4 font-semibold text-xs text-right">Phê Duyệt Giải Ngân</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow
              v-for="w in filteredWithdrawals"
              :key="w.id"
              class="hover:bg-muted/30 transition-colors"
            >
              <TableCell class="py-3.5 px-4 font-bold text-primary tabular-nums text-xs">
                #{{ w.id }}
              </TableCell>

              <TableCell class="py-3.5 px-4 text-xs">
                <span class="font-bold text-foreground block">{{ w.operatorName }}</span>
                <span class="text-[11px] text-muted-foreground tabular-nums">Mã đối tác: #{{ w.operatorId || 'N/A' }}</span>
              </TableCell>

              <TableCell class="py-3.5 px-4 text-right font-extrabold text-emerald-600 tabular-nums text-xs">
                {{ Number(w.amount).toLocaleString('vi-VN') }} đ
              </TableCell>

              <TableCell class="py-3.5 px-4 text-xs">
                <div class="space-y-0.5">
                  <span class="font-semibold text-foreground block">{{ w.bankName || 'Ngân hàng đối tác' }}</span>
                  <div class="flex items-center gap-1.5 text-muted-foreground text-[11px] tabular-nums">
                    <span>{{ maskAccountNumber(w.accountNumber, w.id) }}</span>
                    <button
                      type="button"
                      @click="toggleMask(w.id)"
                      class="text-muted-foreground hover:text-foreground inline-flex items-center"
                      title="Ẩn/Hiện số tài khoản"
                    >
                      <Eye v-if="!unmaskedAccountIds.has(w.id)" class="w-3 h-3" />
                      <EyeOff v-else class="w-3 h-3" />
                    </button>
                    <span>({{ w.accountHolderName || 'Chủ tài khoản' }})</span>
                  </div>
                </div>
              </TableCell>

              <TableCell class="py-3.5 px-4 text-muted-foreground tabular-nums text-xs">
                {{ w.createdAt || w.requestedAt || 'N/A' }}
              </TableCell>

              <TableCell class="py-3.5 px-4 text-center text-xs">
                <StatusBadge :status="w.status" />
              </TableCell>

              <TableCell class="py-3.5 px-4 text-right space-x-2 text-xs">
                <template v-if="w.status === 'PENDING'">
                  <!-- Locked due to unknown outcome -->
                  <Badge
                    v-if="unresolvedOperations.has(String(w.id))"
                    variant="outline"
                    class="border-destructive/40 text-destructive text-[11px]"
                  >
                    <Lock class="w-3 h-3 mr-1 inline" /> Đang khóa an toàn
                  </Badge>

                  <template v-else>
                    <Button
                      v-if="hasPermission('WITHDRAWAL_APPROVE')"
                      type="button"
                      size="sm"
                      variant="outline"
                      @click="openConfirmDialog(w, 'APPROVE')"
                      :data-testid="`btn-approve-withdraw-${w.id}`"
                      :disabled="processingIds.has(w.id)"
                      class="h-8 text-xs font-semibold text-emerald-600 border-emerald-500/30 hover:bg-emerald-500/10"
                    >
                      <Loader2 v-if="processingIds.has(w.id)" class="w-3.5 h-3.5 mr-1 animate-spin" />
                      <CheckCircle2 v-else class="w-3.5 h-3.5 mr-1" /> Duyệt Chi
                    </Button>

                    <Button
                      v-if="hasPermission('WITHDRAWAL_REJECT')"
                      type="button"
                      size="sm"
                      variant="destructive"
                      @click="openConfirmDialog(w, 'REJECT')"
                      :data-testid="`btn-reject-withdraw-${w.id}`"
                      :disabled="processingIds.has(w.id)"
                      class="h-8 text-xs font-semibold"
                    >
                      <Loader2 v-if="processingIds.has(w.id)" class="w-3.5 h-3.5 mr-1 animate-spin" />
                      <XCircle v-else class="w-3.5 h-3.5 mr-1" /> Từ Chối
                    </Button>

                    <span
                      v-if="!hasPermission('WITHDRAWAL_APPROVE') && !hasPermission('WITHDRAWAL_REJECT')"
                      class="text-muted-foreground text-[11px]"
                    >
                      Chỉ xem (Thiếu quyền duyệt)
                    </span>
                  </template>
                </template>

                <span v-else class="text-muted-foreground text-[11px] font-medium">
                  Đã đóng lệnh
                </span>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </div>
    </Card>

    <!-- REAL CONFIRMATION DIALOG (D2-PRE-01) -->
    <AlertDialog :open="confirmDialog.isOpen" @update:open="(v) => !v && closeConfirmDialog()">
      <AlertDialogContent class="max-w-md">
        <AlertDialogHeader>
          <AlertDialogTitle class="text-base font-bold flex items-center gap-2">
            <CheckCircle2 v-if="confirmDialog.action === 'APPROVE'" class="w-5 h-5 text-emerald-600" />
            <XCircle v-else class="w-5 h-5 text-destructive" />
            <span>
              {{ confirmDialog.action === 'APPROVE' ? 'Xác Nhận Phê Duyệt Giải Ngân' : 'Xác Nhận Từ Chối Yêu Cầu Rút Tiền' }}
            </span>
          </AlertDialogTitle>
          <AlertDialogDescription class="text-xs space-y-2 pt-1 text-muted-foreground">
            <p>
              {{ confirmDialog.action === 'APPROVE'
                ? 'Hành động này sẽ ghi nhận phê duyệt lệnh giải ngân, khấu trừ số dư ký quỹ bị khóa (Locked Balance) và ghi nhận vào sổ cái ví đối tác.'
                : 'Hành động này sẽ từ chối lệnh rút tiền và hoàn trả toàn bộ số tiền đang khóa về ví khả dụng (Available Balance) của nhà xe.'
              }}
            </p>
          </AlertDialogDescription>
        </AlertDialogHeader>

        <!-- Summary of verified financial context -->
        <div v-if="confirmDialog.item" class="p-3 rounded-lg bg-muted/40 border border-border space-y-1.5 text-xs">
          <div class="flex justify-between">
            <span class="text-muted-foreground">Mã yêu cầu:</span>
            <span class="font-bold text-foreground">#{{ confirmDialog.item.id }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-muted-foreground">Nhà xe thụ hưởng:</span>
            <span class="font-bold text-foreground">{{ confirmDialog.item.operatorName }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-muted-foreground">Số tiền yêu cầu:</span>
            <span class="font-extrabold text-emerald-600 tabular-nums">
              {{ Number(confirmDialog.item.amount).toLocaleString('vi-VN') }} đ
            </span>
          </div>
          <div class="flex justify-between">
            <span class="text-muted-foreground">Tài khoản nhận:</span>
            <span class="font-mono text-[11px] text-foreground">
              {{ confirmDialog.item.bankName }} - {{ maskAccountNumber(confirmDialog.item.accountNumber, confirmDialog.item.id) }}
            </span>
          </div>
        </div>

        <!-- Note input -->
        <div class="space-y-1.5 py-1 text-xs">
          <label class="block font-semibold text-foreground">
            {{ confirmDialog.action === 'APPROVE' ? 'Ghi chú phê duyệt (Tùy chọn)' : 'Lý do từ chối (Bắt buộc tối thiểu 5 ký tự) *' }}
          </label>
          <textarea
            v-model="confirmDialog.adminNote"
            rows="2"
            :placeholder="confirmDialog.action === 'APPROVE' ? 'Nhập mã giao dịch ngân hàng hoặc ghi chú nội bộ...' : 'Nhập chi tiết lý do từ chối để đối tác nhận thông báo...'"
            class="w-full p-2 rounded-md border border-input bg-background text-xs text-foreground focus:outline-none focus:ring-1 focus:ring-ring"
          ></textarea>
        </div>

        <Alert v-if="confirmDialog.error" variant="destructive" class="py-2 text-xs">
          <AlertCircle class="h-4 w-4" />
          <AlertDescription>{{ confirmDialog.error }}</AlertDescription>
        </Alert>

        <AlertDialogFooter>
          <AlertDialogCancel :disabled="confirmDialog.isSubmitting" class="text-xs">
            Hủy Bỏ
          </AlertDialogCancel>
          <AlertDialogAction
            @click="executeWithdrawalMutation"
            :disabled="confirmDialog.isSubmitting"
            data-testid="btn-confirm-withdrawal-action"
            :class="[
              'text-xs font-semibold',
              confirmDialog.action === 'APPROVE'
                ? 'bg-primary hover:bg-primary/90 text-primary-foreground'
                : 'bg-destructive text-destructive-foreground hover:bg-destructive/90'
            ]"
          >
            <Loader2 v-if="confirmDialog.isSubmitting" class="w-3.5 h-3.5 mr-1.5 animate-spin inline" />
            {{ confirmDialog.action === 'APPROVE' ? 'Xác Nhận Duyệt Chi' : 'Xác Nhận Từ Chối' }}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>
