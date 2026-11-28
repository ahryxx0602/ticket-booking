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
  Lock
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
import { adminApi } from '@/modules/admin/api'

const { hasPermission } = useAdminPermissions()

const withdrawals = ref([])
const isLoading = ref(false)
const loadError = ref('')
const selectedFilter = ref('ALL') // 'ALL' | 'PENDING' | 'APPROVED' | 'REJECTED'
const processingIds = ref(new Set())
const unmaskedAccountIds = ref(new Set())
const unknownOutcome = ref(null)

const WITHDRAWAL_FILTERS = [
  { value: 'ALL', label: 'Tất cả lệnh' },
  { value: 'PENDING', label: 'Chờ giải ngân' },
  { value: 'APPROVED', label: 'Đã duyệt chi' },
  { value: 'REJECTED', label: 'Bị từ chối' }
]

// Authoritative API fetching
async function fetchWithdrawals() {
  if (!hasPermission('WITHDRAWAL_VIEW')) {
    loadError.value = 'Bạn không có quyền xem danh sách lệnh rút tiền (Yêu cầu WITHDRAWAL_VIEW).'
    return
  }

  isLoading.value = true
  loadError.value = ''
  try {
    const statusParam = selectedFilter.value !== 'ALL' ? selectedFilter.value : null
    const res = await adminApi.getWithdrawals(statusParam)
    const data = res?.data?.data !== undefined ? res.data.data : res?.data
    if (Array.isArray(data)) {
      withdrawals.value = data.map(item => ({
        ...item,
        // Normalize status: map legacy COMPLETED or PENDING_APPROVAL to backend WithdrawalStatus
        status: item.status === 'COMPLETED' ? 'APPROVED' : (item.status === 'PENDING_APPROVAL' ? 'PENDING' : item.status),
        operatorName: item.operatorName || (item.operatorId ? `Nhà xe đối tác #${item.operatorId}` : 'Nhà xe đối tác')
      }))
    } else {
      withdrawals.value = []
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

// Processing Approvals & Rejections
async function processWithdrawal(item, action, customNote = null) {
  const isApprove = action === 'APPROVED' || action === 'COMPLETED'
  if (isApprove && !hasPermission('WITHDRAWAL_APPROVE')) {
    toast.error('Thiếu quyền phê duyệt giải ngân WITHDRAWAL_APPROVE')
    return
  }
  if (!isApprove && !hasPermission('WITHDRAWAL_REJECT')) {
    toast.error('Thiếu quyền từ chối giải ngân WITHDRAWAL_REJECT')
    return
  }

  const previousStatus = item.status
  item.status = isApprove ? 'APPROVED' : 'REJECTED'
  processingIds.value.add(item.id)
  unknownOutcome.value = null
  const defaultNote = isApprove ? 'Duyệt chi giải ngân thành công qua tài khoản ngân hàng' : 'Từ chối giải ngân và hoàn trả số dư về ví khả dụng'
  const note = customNote || defaultNote

  try {
    if (isApprove) {
      await adminApi.approveWithdrawal(item.id, note)
      toast.success(`Đã duyệt chi giải ngân lệnh #${item.id}`)
    } else {
      await adminApi.rejectWithdrawal(item.id, note)
      toast.success(`Đã từ chối lệnh #${item.id}`)
    }
  } catch (err) {
    const errorMsg = err?.response?.data?.message || err?.message || 'Lỗi xử lý giao dịch'
    if (err?.code === 'ECONNABORTED' || err?.message?.includes('timeout') || err?.message?.includes('Network Error')) {
      unknownOutcome.value = {
        id: item.id,
        action: isApprove ? 'Duyệt Chi' : 'Từ Chối',
        message: 'Giao dịch gặp sự cố kết nối. Vui lòng làm mới để xác thực trạng thái từ máy chủ.'
      }
    } else {
      item.status = previousStatus
      toast.error(errorMsg)
    }
  } finally {
    processingIds.value.delete(item.id)
  }
}

onMounted(() => {
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
          Duyệt giải ngân chuyển khoản ngân hàng (trừ số dư Locked Balance) hoặc từ chối để hoàn trả số dư về ví khả dụng của nhà xe.
        </p>
      </div>

      <div class="flex items-center gap-2">
        <Button variant="outline" size="sm" @click="fetchWithdrawals" class="h-8 text-xs">
          <RefreshCw class="w-3.5 h-3.5 mr-1" /> Làm mới
        </Button>
      </div>
    </div>

    <!-- Unknown Outcome Guard Alert -->
    <Alert v-if="unknownOutcome" variant="destructive" class="border-destructive/50 bg-destructive/5">
      <AlertCircle class="h-4 w-4" />
      <AlertTitle>Cảnh báo an toàn tài chính: Kết quả chưa xác định</AlertTitle>
      <AlertDescription class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-1">
        <span>Lệnh #{{ unknownOutcome.id }} ({{ unknownOutcome.action }}): {{ unknownOutcome.message }}</span>
        <Button
          size="sm"
          variant="outline"
          @click="fetchWithdrawals; unknownOutcome = null"
          class="h-7 text-xs whitespace-nowrap"
        >
          <RefreshCw class="w-3.5 h-3.5 mr-1" /> Kiểm tra lại trạng thái
        </Button>
      </AlertDescription>
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

      <!-- Real Table -->
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
                {{ w.id }}
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
                <template v-if="w.status === 'PENDING' || w.status === 'PENDING_APPROVAL'">
                  <Button
                    v-if="hasPermission('WITHDRAWAL_APPROVE')"
                    type="button"
                    size="sm"
                    @click="processWithdrawal(w, 'APPROVED')"
                    :data-testid="`btn-approve-withdraw-${w.id}`"
                    :disabled="processingIds.has(w.id)"
                    class="h-8 text-xs font-semibold text-emerald-600 border-emerald-500/30 hover:bg-emerald-500/10"
                    variant="outline"
                  >
                    <Loader2 v-if="processingIds.has(w.id)" class="w-3.5 h-3.5 mr-1 animate-spin" />
                    <CheckCircle2 v-else class="w-3.5 h-3.5 mr-1" /> Duyệt Chi
                  </Button>

                  <Button
                    v-if="hasPermission('WITHDRAWAL_REJECT')"
                    type="button"
                    size="sm"
                    variant="destructive"
                    @click="processWithdrawal(w, 'REJECTED')"
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

                <span v-else class="text-muted-foreground text-[11px] font-medium">
                  Đã đóng lệnh
                </span>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </div>
    </Card>
  </div>
</template>
