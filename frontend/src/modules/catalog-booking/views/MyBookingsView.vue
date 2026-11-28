<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import { catalogBookingApi } from '../api'
import { useBookingOperationGuard } from '../composables/useBookingOperationGuard'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import { Button } from '@/components/ui/button'
import {
  Ticket,
  Clock,
  Calendar,
  AlertCircle,
  Loader2,
  RefreshCw,
  CreditCard,
  XCircle,
  QrCode,
  ChevronLeft,
  ChevronRight,
  ShieldCheck,
  Bus
} from 'lucide-vue-next'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle
} from '@/components/ui/alert-dialog'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle
} from '@/components/ui/dialog'

const router = useRouter()
const operationGuard = useBookingOperationGuard()

// State
const bookings = ref([])
const isLoading = ref(true)
const loadError = ref('')
const currentTab = ref('ALL')

const tabs = [
  { key: 'ALL', label: 'Tất cả' },
  { key: 'HELD', label: 'Đang giữ chỗ' },
  { key: 'CONFIRMED', label: 'Đã xác nhận' },
  { key: 'CANCELLED', label: 'Đã hủy' },
  { key: 'EXPIRED', label: 'Hết hạn' }
]

// Pagination state from PageResponse
const currentPage = ref(0) // 0-indexed per PageResponse
const pageSize = ref(10)
const totalPages = ref(1)
const totalElements = ref(0)
const hasNext = ref(false)
const hasPrevious = ref(false)

// Cancel Modal State
const bookingToCancel = ref(null)
const isCancelling = ref(false)

// Ticket Detail Modal State
const selectedBookingForDetail = ref(null)
const showDetailModal = ref(false)
const ticketLoading = ref(false)
const ticketError = ref('')
const ticketData = ref(null)

// Out-of-order request sequence guards
let fetchSequence = 0
let ticketSequence = 0

// Client-side fallback filter if backend returned un-filtered items
const filteredBookings = computed(() => {
  if (currentTab.value === 'ALL') return bookings.value
  return bookings.value.filter(b => b.status === currentTab.value)
})

async function fetchMyBookings(page = 0, status = currentTab.value === 'ALL' ? undefined : currentTab.value) {
  const currentSeq = ++fetchSequence
  isLoading.value = true
  loadError.value = ''

  try {
    const res = await catalogBookingApi.getMyBookings({
      page,
      size: pageSize.value,
      status: status || undefined
    })

    if (currentSeq !== fetchSequence) return

    const pageData = res.data?.data || res.data

    if (!pageData || typeof pageData !== 'object') {
      throw new Error('Dữ liệu danh sách đơn đặt vé không hợp lệ.')
    }

    if (Array.isArray(pageData.items)) {
      bookings.value = pageData.items
      currentPage.value = Number(pageData.page ?? 0)
      pageSize.value = Number(pageData.size ?? 10)
      totalPages.value = Number(pageData.totalPages ?? 1)
      totalElements.value = Number(pageData.totalElements ?? pageData.items.length)
      hasNext.value = Boolean(pageData.hasNext)
      hasPrevious.value = Boolean(pageData.hasPrevious)
    } else if (Array.isArray(pageData)) {
      bookings.value = pageData
      currentPage.value = 0
      totalPages.value = 1
      totalElements.value = pageData.length
      hasNext.value = false
      hasPrevious.value = false
    } else {
      throw new Error('Cấu trúc dữ liệu đơn đặt vé từ máy chủ không đúng định dạng (thiếu items).')
    }
  } catch (err) {
    if (currentSeq !== fetchSequence) return
    console.error('[MyBookings] Failed to fetch bookings:', err)
    loadError.value = err.response?.data?.message || err.message || 'Không thể tải danh sách đơn vé từ máy chủ.'
  } finally {
    if (currentSeq === fetchSequence) {
      isLoading.value = false
    }
  }
}

function selectTab(tabKey) {
  currentTab.value = tabKey
  currentPage.value = 0
  fetchMyBookings(0, tabKey === 'ALL' ? undefined : tabKey)
}

function handlePageChange(newPage) {
  if (newPage < 0 || newPage >= totalPages.value || newPage === currentPage.value) return
  fetchMyBookings(newPage, currentTab.value === 'ALL' ? undefined : currentTab.value)
}

// Cancel Booking Flow
function openCancelDialog(booking) {
  bookingToCancel.value = booking
}

async function confirmCancelBooking() {
  if (!bookingToCancel.value) return
  const refCode = bookingToCancel.value.bookingReference
  isCancelling.value = true
  operationGuard.setCancelPending(refCode)

  try {
    await catalogBookingApi.cancelBooking(refCode)
    operationGuard.clearCancelPending(refCode)
    operationGuard.clearUnresolvedCancel(refCode)
    toast.success(`Đã hủy thành công đơn giữ chỗ #${refCode}.`)
    bookingToCancel.value = null
    // Refresh page
    await fetchMyBookings(currentPage.value, currentTab.value === 'ALL' ? undefined : currentTab.value)
  } catch (err) {
    console.error('[MyBookings] Cancel failed:', err)
    operationGuard.clearCancelPending(refCode)
    const status = err.response?.status
    // D0-PRE-03: Distinguish definitive business rejection (4xx) from ambiguous outcome (5xx or network disconnect)
    if (status && status >= 400 && status < 500) {
      operationGuard.clearUnresolvedCancel(refCode)
    } else {
      operationGuard.setUnresolvedCancel(refCode, err)
    }
    const msg = err.response?.data?.message || err.message || 'Không thể hủy đơn đặt chỗ trên máy chủ.'
    toast.error(msg)
  } finally {
    isCancelling.value = false
  }
}

// Ticket Detail / E-Ticket Modal
async function openTicketDetail(booking) {
  const currentTicketSeq = ++ticketSequence
  selectedBookingForDetail.value = booking
  showDetailModal.value = true
  ticketLoading.value = true
  ticketError.value = ''
  ticketData.value = null

  try {
    const res = await catalogBookingApi.getElectronicTicket(booking.bookingReference)
    if (currentTicketSeq !== ticketSequence) return
    const data = res.data?.data || res.data
    // D0-PRE-03: Verify returned bookingReference against requested booking and validate canonical fields
    if (!data || typeof data !== 'object') {
      throw new Error('Dữ liệu vé điện tử từ máy chủ không hợp lệ.')
    }
    if (data.bookingReference !== booking.bookingReference) {
      throw new Error(`Mã vé không khớp: yêu cầu ${booking.bookingReference} nhưng máy chủ trả về ${data.bookingReference}`)
    }
    if (!data.qrImageBase64 || !data.tripId || !data.seatNumbers || !data.status) {
      throw new Error('Dữ liệu vé điện tử từ máy chủ không đầy đủ thông tin chuẩn hóa.')
    }
    ticketData.value = data
  } catch (err) {
    if (currentTicketSeq !== ticketSequence) return
    console.error('[MyBookings] Failed to load electronic ticket:', err)
    ticketError.value = err.response?.data?.message || err.message || 'Không thể lấy thông tin vé điện tử từ máy chủ.'
  } finally {
    if (currentTicketSeq === ticketSequence) {
      ticketLoading.value = false
    }
  }
}

function formatDate(dateInput) {
  if (!dateInput) return '—'
  try {
    const d = new Date(dateInput)
    if (isNaN(d.getTime())) return String(dateInput)
    return new Intl.DateTimeFormat('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    }).format(d)
  } catch {
    return String(dateInput)
  }
}

onMounted(() => {
  fetchMyBookings(0)
})
</script>

<template>
  <div class="max-w-5xl mx-auto px-4 py-8 space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-4">
      <div>
        <h1 class="text-xl font-bold text-foreground">Lịch Sử Đặt Vé & Vé Điện Tử</h1>
        <p class="text-xs text-muted-foreground mt-1">
          Theo dõi trạng thái các đơn giữ chỗ, hoàn tất thanh toán VNPay và quản lý vé chuyến xe của bạn.
        </p>
      </div>

      <router-link to="/search">
        <Button size="sm" class="gap-1.5 font-semibold">
          <Bus class="w-4 h-4" />
          <span>+ Đặt Vé Mới</span>
        </Button>
      </router-link>
    </div>

    <!-- Status Tabs -->
    <div class="flex gap-2 border-b border-border pb-2 overflow-x-auto text-xs font-semibold">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        type="button"
        @click="selectTab(tab.key)"
        class="px-3.5 py-1.5 rounded-lg transition-colors"
        :class="currentTab === tab.key ? 'bg-primary text-primary-foreground shadow-xs' : 'text-muted-foreground hover:bg-muted'"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- Loading Skeleton -->
    <div v-if="isLoading" class="p-12 text-center bg-card rounded-xl border border-border space-y-3">
      <Loader2 class="w-8 h-8 animate-spin mx-auto text-primary" />
      <p class="text-xs text-muted-foreground">Đang tải lịch sử đơn đặt chỗ từ máy chủ...</p>
    </div>

    <!-- Error State -->
    <div
      v-else-if="loadError"
      class="bg-destructive/10 border border-destructive/20 rounded-xl p-6 text-center space-y-3"
    >
      <AlertCircle class="w-8 h-8 text-destructive mx-auto" />
      <h3 class="text-sm font-bold text-destructive">Không Thể Tải Danh Sách Đơn Vé</h3>
      <p class="text-xs text-muted-foreground max-w-md mx-auto">{{ loadError }}</p>
      <Button size="sm" variant="outline" class="gap-1.5" @click="fetchMyBookings(currentPage)">
        <RefreshCw class="w-3.5 h-3.5" />
        <span>Thử Lại</span>
      </Button>
    </div>

    <!-- Empty State -->
    <div v-else-if="filteredBookings.length === 0" class="py-12 bg-card rounded-xl border border-border">
      <EmptyState
        title="Chưa có đơn đặt vé nào"
        description="Bạn chưa có giao dịch đặt chỗ nào trong danh mục này. Hãy bắt đầu chọn chuyến và đặt vé ngay!"
        action-label="Tìm Chuyến Xe"
        @action="router.push('/search')"
      />
    </div>

    <!-- Bookings List -->
    <div v-else class="space-y-4">
      <div
        v-for="item in filteredBookings"
        :key="item.bookingReference"
        class="bg-card rounded-xl border border-border p-5 shadow-xs space-y-4 hover:border-border/80 transition-all"
      >
        <!-- Unresolved cancel warning if any -->
        <div
          v-if="operationGuard.isCancelUnresolved(item.bookingReference)"
          class="bg-amber-500/10 border border-amber-500/30 rounded-lg p-3 text-xs text-amber-700 dark:text-amber-300 space-y-1"
        >
          <div class="flex items-center gap-1.5 font-semibold">
            <AlertCircle class="w-4 h-4 shrink-0" />
            <span>Yêu cầu hủy đơn chưa hoàn tất</span>
          </div>
          <p class="text-muted-foreground text-[11px]">
            {{ operationGuard.getCancelError(item.bookingReference) || 'Đã có lỗi xảy ra trong quá trình hủy đơn. Vui lòng thử lại.' }}
          </p>
        </div>

        <!-- Top Bar: Reference & Status -->
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-border">
          <div class="flex items-center gap-3">
            <span class="text-xs font-bold text-foreground">
              Mã tham chiếu:
              <strong class="font-mono text-primary">{{ item.bookingReference }}</strong>
            </span>
            <span class="text-xs text-muted-foreground">• Chuyến xe #{{ item.tripId }}</span>
          </div>

          <StatusBadge :status="item.status" />
        </div>

        <!-- Middle Bar: Details -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
          <div class="space-y-1.5 md:col-span-2">
            <div class="flex items-center gap-2 text-foreground font-semibold">
              <Bus class="w-4 h-4 text-primary" />
              <span>Chuyến xe định danh hệ thống: #{{ item.tripId }}</span>
            </div>
            <div class="flex items-center gap-4 text-muted-foreground text-[11px]">
              <span class="flex items-center gap-1">
                <Calendar class="w-3.5 h-3.5" />
                <span>Tạo lúc: {{ formatDate(item.createdAt) }}</span>
              </span>
              <span v-if="item.status === 'HELD' && item.expiresAt" class="flex items-center gap-1 text-amber-600 dark:text-amber-400 font-medium">
                <Clock class="w-3.5 h-3.5" />
                <span>Hết hạn giữ chỗ: {{ formatDate(item.expiresAt) }}</span>
              </span>
            </div>
          </div>

          <div class="bg-muted/40 p-3 rounded-lg border border-border space-y-1">
            <div class="flex justify-between">
              <span class="text-muted-foreground">Vị trí ghế:</span>
              <span class="font-mono font-bold text-primary">{{ (item.seatNumbers || []).join(', ') }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-muted-foreground">Tổng tiền:</span>
              <span class="font-mono font-bold text-foreground tabular-nums">
                {{ Number(item.totalAmount).toLocaleString('vi-VN') }} đ
              </span>
            </div>
          </div>
        </div>

        <!-- Bottom Action Bar -->
        <div class="pt-3 border-t border-border flex items-center justify-end gap-2.5">
          <!-- HELD Actions -->
          <template v-if="item.status === 'HELD'">
            <Button
              variant="destructive"
              size="sm"
              class="gap-1.5"
              :disabled="operationGuard.isCancelPending(item.bookingReference)"
              @click="openCancelDialog(item)"
            >
              <Loader2 v-if="operationGuard.isCancelPending(item.bookingReference)" class="w-3.5 h-3.5 animate-spin" />
              <XCircle v-else class="w-3.5 h-3.5" />
              <span>Hủy Giữ Chỗ</span>
            </Button>

            <router-link :to="`/checkout/${item.bookingReference}`">
              <Button size="sm" class="gap-1.5">
                <CreditCard class="w-3.5 h-3.5" />
                <span>Tiếp Tục Thanh Toán</span>
              </Button>
            </router-link>
          </template>

          <!-- CONFIRMED Actions -->
          <template v-else-if="item.status === 'CONFIRMED'">
            <Button size="sm" variant="outline" class="gap-1.5" @click="openTicketDetail(item)">
              <QrCode class="w-3.5 h-3.5" />
              <span>Xem Chi Tiết & Vé Điện Tử</span>
            </Button>
          </template>

          <!-- CANCELLED / EXPIRED Actions -->
          <template v-else>
            <router-link :to="`/trips/${item.tripId}/seats`">
              <Button variant="outline" size="sm" class="gap-1.5">
                <RefreshCw class="w-3.5 h-3.5" />
                <span>Đặt Lại Chuyến Này</span>
              </Button>
            </router-link>
          </template>
        </div>
      </div>

      <!-- Pagination Footer -->
      <div
        v-if="totalPages > 1"
        class="flex items-center justify-between pt-4 border-t border-border text-xs text-muted-foreground"
      >
        <div>
          Trang <strong>{{ currentPage + 1 }}</strong> / <strong>{{ totalPages }}</strong> (Tổng {{ totalElements }} đơn vé)
        </div>

        <div class="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            class="gap-1.5"
            :disabled="!hasPrevious"
            @click="handlePageChange(currentPage - 1)"
          >
            <ChevronLeft class="w-4 h-4" />
            <span>Trước</span>
          </Button>

          <Button
            variant="outline"
            size="sm"
            class="gap-1.5"
            :disabled="!hasNext"
            @click="handlePageChange(currentPage + 1)"
          >
            <span>Sau</span>
            <ChevronRight class="w-4 h-4" />
          </Button>
        </div>
      </div>
    </div>

    <!-- Cancel Confirmation AlertDialog -->
    <AlertDialog :open="!!bookingToCancel" @update:open="val => { if (!val) bookingToCancel = null }">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Xác Nhận Hủy Giữ Chỗ?</AlertDialogTitle>
          <AlertDialogDescription>
            Bạn có chắc chắn muốn hủy phiên giữ chỗ cho đơn vé
            <strong class="font-mono text-foreground">#{{ bookingToCancel?.bookingReference }}</strong>?
            Ghế <strong class="text-primary font-mono">{{ (bookingToCancel?.seatNumbers || []).join(', ') }}</strong>
            sẽ được gửi yêu cầu lên hệ thống để xử lý giải phóng ghế.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel :disabled="isCancelling">Bỏ Qua</AlertDialogCancel>
          <AlertDialogAction
            class="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            :disabled="isCancelling"
            @click="confirmCancelBooking"
          >
            <Loader2 v-if="isCancelling" class="w-4 h-4 animate-spin mr-1" />
            <span>Xác Nhận Hủy</span>
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <!-- Ticket Detail Modal -->
    <Dialog :open="showDetailModal" @update:open="val => showDetailModal = val">
      <DialogContent class="max-w-md">
        <DialogHeader>
          <DialogTitle class="flex items-center gap-2">
            <Ticket class="w-5 h-5 text-primary" />
            <span>Thông Tin Vé Điện Tử VeloBus</span>
          </DialogTitle>
          <DialogDescription>
            Vé điện tử xác thực với mã QR có chữ ký số bảo mật.
          </DialogDescription>
        </DialogHeader>

        <!-- Loading -->
        <div v-if="ticketLoading" class="py-8 text-center space-y-3">
          <Loader2 class="w-8 h-8 animate-spin mx-auto text-primary" />
          <p class="text-xs text-muted-foreground">Đang xác thực và tải vé điện tử từ máy chủ...</p>
        </div>

        <!-- Error -->
        <div v-else-if="ticketError" class="py-4 space-y-3">
          <div class="bg-destructive/10 border border-destructive/20 rounded-lg p-4 space-y-2 text-xs">
            <div class="flex items-center gap-2 font-semibold text-destructive">
              <AlertCircle class="w-4 h-4 shrink-0" />
              <span>Không Thể Cấp Vé Điện Tử</span>
            </div>
            <p class="text-muted-foreground">{{ ticketError }}</p>
          </div>
        </div>

        <!-- Ticket Content -->
        <div v-else-if="ticketData" class="space-y-4 text-xs py-2">
          <!-- QR Code Block -->
          <div class="bg-white dark:bg-card p-4 rounded-xl border border-border shadow-xs flex flex-col items-center justify-center space-y-2">
            <img
              v-if="ticketData.qrImageBase64"
              :src="'data:image/png;base64,' + ticketData.qrImageBase64"
              alt="Mã QR Vé Điện Tử"
              class="w-48 h-48 rounded-lg border border-border/50 p-2 bg-white"
            />
            <div class="flex items-center gap-1.5 text-emerald-600 dark:text-emerald-400 font-semibold text-xs">
              <ShieldCheck class="w-4 h-4" />
              <span>Vé Điện Tử Hợp Lệ & Có Chữ Ký Số</span>
            </div>
            <p class="text-[11px] text-muted-foreground text-center px-2">
              Mã QR đã được ký số bảo mật điện tử. Xuất trình khi lên xe.
            </p>
          </div>

          <!-- Booking Metadata -->
          <div class="bg-muted/50 p-4 rounded-lg border border-border space-y-2">
            <div class="flex justify-between">
              <span class="text-muted-foreground">Mã tham chiếu:</span>
              <strong class="font-mono text-foreground">{{ ticketData.bookingReference }}</strong>
            </div>
            <div class="flex justify-between">
              <span class="text-muted-foreground">Chuyến xe:</span>
              <strong class="text-foreground">#{{ ticketData.tripId }}</strong>
            </div>
            <div class="flex justify-between">
              <span class="text-muted-foreground">Vị trí ghế:</span>
              <strong class="font-mono text-primary">{{ (ticketData.seatNumbers || []).join(', ') }}</strong>
            </div>
            <div class="flex justify-between">
              <span class="text-muted-foreground">Tổng thanh toán:</span>
              <strong class="font-mono text-foreground">{{ Number(ticketData.totalAmount).toLocaleString('vi-VN') }} đ</strong>
            </div>
            <div class="flex justify-between">
              <span class="text-muted-foreground">Trạng thái:</span>
              <StatusBadge :status="ticketData.status" />
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" size="sm" @click="showDetailModal = false">
            Đóng
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>
