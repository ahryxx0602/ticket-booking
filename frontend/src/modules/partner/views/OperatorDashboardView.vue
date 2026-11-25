<script setup>
import { ref } from 'vue'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Alert, AlertDescription } from '@/components/ui/alert'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
  DialogClose
} from '@/components/ui/dialog'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { toast } from 'vue-sonner'
import {
  Wallet,
  Lock,
  Clock,
  Building,
  DollarSign,
  ArrowDownLeft,
  AlertCircle,
  CheckCircle2,
  Receipt
} from 'lucide-vue-next'

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
    toast.success('Gửi yêu cầu rút tiền thành công!', {
      description: `Đã khởi tạo lệnh rút ${withdrawAmount.value.toLocaleString('vi-VN')} đ về ngân hàng.`
    })
  }, 800)
}
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold tracking-tight text-foreground">Tổng Quan Ví Ký Quỹ Escrow (4 Số Dư)</h1>
        <p class="text-xs text-muted-foreground mt-1">
          Hệ thống ví Fintech đa số dư: Quyết toán sau chuyến đi +2h • Chống rút trùng bằng cơ chế Khóa số dư (Locked Balance).
        </p>
      </div>

      <Button
        type="button"
        @click="openWithdrawModal"
        data-testid="btn-open-withdraw"
        class="self-start sm:self-auto px-4 py-2 text-xs font-semibold gap-2 shadow-xs"
      >
        <ArrowDownLeft class="w-4 h-4" />
        <span>Yêu Cầu Rút Tiền</span>
      </Button>
    </div>

    <!-- 4 Fintech Balance Cards Grid -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <!-- 1. Available Balance -->
      <Card class="border-border bg-card shadow-xs border-emerald-500/40 space-y-1.5">
        <CardHeader class="pb-2 flex flex-row items-center justify-between space-y-0">
          <CardTitle class="text-xs font-bold uppercase tracking-wider text-muted-foreground">Số Dư Khả Dụng</CardTitle>
          <div class="p-2 rounded-lg bg-emerald-500/10 text-emerald-600 dark:text-emerald-400">
            <Wallet class="w-4 h-4" />
          </div>
        </CardHeader>
        <CardContent class="space-y-1">
          <p class="text-2xl font-extrabold text-emerald-600 dark:text-emerald-400 tabular-nums font-mono" data-testid="available-balance">
            {{ wallet.availableBalance.toLocaleString('vi-VN') }} <span class="text-xs font-bold">đ</span>
          </p>
          <p class="text-[11px] text-muted-foreground">Có thể tạo lệnh rút về ngân hàng ngay lập tức</p>
        </CardContent>
      </Card>

      <!-- 2. Holding Escrow -->
      <Card class="border-border bg-card shadow-xs space-y-1.5">
        <CardHeader class="pb-2 flex flex-row items-center justify-between space-y-0">
          <CardTitle class="text-xs font-bold uppercase tracking-wider text-muted-foreground">Đang Ký Quỹ Chuyến</CardTitle>
          <div class="p-2 rounded-lg bg-amber-500/10 text-amber-600 dark:text-amber-400">
            <Lock class="w-4 h-4" />
          </div>
        </CardHeader>
        <CardContent class="space-y-1">
          <p class="text-2xl font-extrabold text-amber-600 dark:text-amber-400 tabular-nums font-mono">
            {{ wallet.holdingEscrow.toLocaleString('vi-VN') }} <span class="text-xs font-bold">đ</span>
          </p>
          <p class="text-[11px] text-muted-foreground">Tự động quyết toán sau khi xe đến đích +2 giờ</p>
        </CardContent>
      </Card>

      <!-- 3. Settling Balance -->
      <Card class="border-border bg-card shadow-xs space-y-1.5">
        <CardHeader class="pb-2 flex flex-row items-center justify-between space-y-0">
          <CardTitle class="text-xs font-bold uppercase tracking-wider text-muted-foreground">Đang Xử Lý Rút</CardTitle>
          <div class="p-2 rounded-lg bg-primary/10 text-primary">
            <Clock class="w-4 h-4" />
          </div>
        </CardHeader>
        <CardContent class="space-y-1">
          <p class="text-2xl font-extrabold text-primary tabular-nums font-mono">
            {{ wallet.settlingBalance.toLocaleString('vi-VN') }} <span class="text-xs font-bold">đ</span>
          </p>
          <p class="text-[11px] text-muted-foreground">Lệnh rút tiền đang chờ Admin phê duyệt chi</p>
        </CardContent>
      </Card>

      <!-- 4. Total Withdrawn -->
      <Card class="border-border bg-card shadow-xs space-y-1.5">
        <CardHeader class="pb-2 flex flex-row items-center justify-between space-y-0">
          <CardTitle class="text-xs font-bold uppercase tracking-wider text-muted-foreground">Tổng Tiền Đã Rút</CardTitle>
          <div class="p-2 rounded-lg bg-muted text-muted-foreground">
            <Building class="w-4 h-4" />
          </div>
        </CardHeader>
        <CardContent class="space-y-1">
          <p class="text-2xl font-extrabold text-foreground tabular-nums font-mono">
            {{ wallet.totalWithdrawn.toLocaleString('vi-VN') }} <span class="text-xs font-bold text-muted-foreground">đ</span>
          </p>
          <p class="text-[11px] text-muted-foreground">Lũy kế giải ngân thành công từ khi mở sàn</p>
        </CardContent>
      </Card>
    </div>

    <!-- Transaction History Table -->
    <Card class="border-border bg-card shadow-xs overflow-hidden">
      <CardHeader class="p-5 border-b border-border flex flex-row items-center justify-between">
        <div>
          <CardTitle class="text-base font-bold text-foreground">Lịch Sử Biến Động Số Dư & Đối Soát</CardTitle>
          <CardDescription class="text-xs text-muted-foreground mt-0.5">
            Mọi giao dịch đều được ký snapshot tỷ lệ hoa hồng ({{ wallet.commissionRate }}%) tại thời điểm giao dịch
          </CardDescription>
        </div>
        <Badge variant="outline" class="text-xs font-semibold">
          Hoa hồng sàn: {{ wallet.commissionRate }}%
        </Badge>
      </CardHeader>

      <CardContent class="p-0">
        <div class="overflow-x-auto">
          <Table class="min-w-[700px]">
            <TableHeader>
              <TableRow class="bg-muted/50 hover:bg-muted/50">
                <TableHead class="font-semibold text-xs uppercase">Mã GD</TableHead>
                <TableHead class="font-semibold text-xs uppercase">Loại Giao Dịch</TableHead>
                <TableHead class="font-semibold text-xs uppercase">Chuyến Xe</TableHead>
                <TableHead class="font-semibold text-xs uppercase text-right">Biến Động</TableHead>
                <TableHead class="font-semibold text-xs uppercase text-right">Số Dư Sau GD</TableHead>
                <TableHead class="font-semibold text-xs uppercase">Thời Gian</TableHead>
                <TableHead class="font-semibold text-xs uppercase text-center">Trạng Thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow
                v-for="tx in transactions"
                :key="tx.id"
                class="hover:bg-muted/40 transition-colors"
              >
                <TableCell class="font-bold text-foreground tabular-nums font-mono">{{ tx.id }}</TableCell>
                <TableCell class="text-muted-foreground text-xs">{{ tx.typeName }}</TableCell>
                <TableCell class="text-muted-foreground text-xs tabular-nums font-mono">{{ tx.tripId || '—' }}</TableCell>
                <TableCell
                  class="text-right font-bold tabular-nums font-mono"
                  :class="tx.amount > 0 ? 'text-emerald-600 dark:text-emerald-400' : 'text-status-danger'"
                >
                  {{ tx.amount > 0 ? '+' : '' }}{{ tx.amount.toLocaleString('vi-VN') }} đ
                </TableCell>
                <TableCell class="text-right font-medium text-foreground tabular-nums font-mono">
                  {{ tx.balanceAfter.toLocaleString('vi-VN') }} đ
                </TableCell>
                <TableCell class="text-muted-foreground text-xs">{{ tx.createdAt }}</TableCell>
                <TableCell class="text-center">
                  <StatusBadge :status="tx.status" />
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </div>
      </CardContent>
    </Card>

    <!-- Withdrawal Request Modal Dialog -->
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
          <div class="p-3 rounded-lg border border-primary/20 bg-primary/10 text-foreground space-y-1">
            <div class="flex justify-between font-medium">
              <span>Số dư khả dụng hiện tại:</span>
              <strong class="text-primary tabular-nums font-mono">{{ wallet.availableBalance.toLocaleString('vi-VN') }} đ</strong>
            </div>
            <p class="text-[11px] text-muted-foreground">Khoản tiền rút sẽ được khóa tạm thời (LOCKED) chờ Admin giải ngân.</p>
          </div>

          <div class="space-y-1.5">
            <label class="font-semibold text-foreground">Số tiền muốn rút (VNĐ) <span class="text-status-danger">*</span></label>
            <Input
              v-model.number="withdrawAmount"
              type="number"
              min="50000"
              :max="wallet.availableBalance"
              step="50000"
              data-testid="input-withdraw-amount"
              class="h-10 text-xs font-mono font-bold"
              :class="{ 'border-status-danger': withdrawError }"
            />
            <Alert v-if="withdrawError" variant="destructive" class="py-2 px-3 mt-1.5">
              <AlertCircle class="h-3.5 w-3.5 text-status-danger" />
              <AlertDescription class="text-xs text-status-danger font-medium">{{ withdrawError }}</AlertDescription>
            </Alert>
          </div>

          <div class="space-y-1.5">
            <label class="font-semibold text-foreground">Tài khoản ngân hàng thụ hưởng <span class="text-status-danger">*</span></label>
            <select
              v-model="withdrawBank"
              class="w-full h-10 px-3 rounded-lg border border-border text-xs text-foreground bg-background focus:ring-2 focus:ring-primary focus:outline-none"
            >
              <option value="VCB-0071001234567">Vietcombank - 0071001234567 (Hoàng Long Ltd)</option>
              <option value="TCB-1903456789001">Techcombank - 1903456789001 (Hoàng Long Ltd)</option>
            </select>
          </div>

          <div class="space-y-1.5">
            <label class="font-semibold text-foreground">Ghi chú đối soát (Không bắt buộc)</label>
            <Input
              v-model="withdrawNote"
              type="text"
              placeholder="Ví dụ: Rút doanh thu tuần 3 tháng 11..."
              class="h-10 text-xs"
            />
          </div>
        </div>

        <DialogFooter class="gap-2 sm:gap-0">
          <DialogClose as-child>
            <Button variant="outline" size="sm" class="text-xs" @click="closeWithdrawModal">
              Hủy Bỏ
            </Button>
          </DialogClose>
          <Button
            size="sm"
            data-testid="btn-submit-withdraw"
            :loading="isSubmittingWithdraw"
            :disabled="isSubmittingWithdraw"
            class="text-xs font-semibold shadow-xs"
            @click="handleWithdrawSubmit"
          >
            Gửi Yêu Cầu Rút
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>
