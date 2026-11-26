<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import { catalogBookingApi } from '../api'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import AppButton from '@/shared/components/AppButton.vue'
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
  Info,
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

// State
const bookings = ref([])
const isLoading = ref(true)
const loadError = ref('')
const currentTab = ref('ALL')

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

// Filtered Bookings computed for active tab
const filteredBookings = computed(() => {
  if (currentTab.value === 'ALL') return bookings.value
  return bookings.value.filter(b => b.status === currentTab.value)
})

// Tab counts computed from loaded page
const statusCounts = computed(() => {
  const counts = { ALL: bookings.value.length, HELD: 0, CONFIRMED: 0, CANCELLED: 0, EXPIRED: 0 }
  for (const b of bookings.value) {
    if (counts[b.status] !== undefined) {
      counts[b.status]++
    }
  }
  return counts
})

async function fetchMyBookings(page = 0) {
  isLoading.value = true
  loadError.value = ''

  try {
    const res = await catalogBookingApi.getMyBookings({
      page,
      size: pageSize.value
    })

    const pageData = res.data?.data || res.data

    if (pageData && Array.isArray(pageData.items)) {
      bookings.value = pageData.items
      currentPage.value = Number(pageData.page ?? 0)
      pageSize.value = Number(pageData.size ?? 10)
      totalPages.value = Number(pageData.totalPages ?? 1)
      totalElements.value = Number(pageData.totalElements ?? pageData.items.length)
      hasNext.value = Boolean(pageData.hasNext)
      hasPrevious.value = Boolean(pageData.hasPrevious)
    } else if (Array.isArray(pageData)) {
      // Direct array fallback
      bookings.value = pageData
      currentPage.value = 0
      totalPages.value = 1
      totalElements.value = pageData.length
      hasNext.value = false
      hasPrevious.value = false
    } else {
      bookings.value = []
      totalElements.value = 0
    }
  } catch (err) {
    console.error('[MyBookings] Failed to fetch bookings:', err)
    loadError.value = err.response?.data?.message || err.message || 'Không thể tải danh sách đơn vé từ máy chủ.'
  } finally {
    isLoading.value = false
  }
}

function handlePageChange(newPage) {
  if (newPage < 0 || newPage >= totalPages.value || newPage === currentPage.value) return
  fetchMyBookings(newPage)
}

// Cancel Booking Flow
function openCancelDialog(booking) {
  bookingToCancel.value = booking
}

async function confirmCancelBooking() {
  if (!bookingToCancel.value) return
  isCancelling.value = true

  try {
    await catalogBookingApi.cancelBooking(bookingToCancel.value.bookingReference)
    toast.success(`Đã hủy thành công đơn giữ chỗ #${bookingToCancel.value.bookingReference}.`)
    bookingToCancel.value = null
    // Refresh page
    await fetchMyBookings(currentPage.value)
  } catch (err) {
    console.error('[MyBookings] Cancel failed:', err)
    const msg = err.response?.data?.message || err.message || 'Không thể hủy đơn đặt chỗ trên máy chủ.'
    toast.error(msg)
  } finally {
    isCancelling.value = false
  }
}

// Ticket Detail / E-Ticket Modal
function openTicketDetail(booking) {
  selectedBookingForDetail.value = booking
  showDetailModal.value = true
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

      <router-link
        to="/search"
        class="self-start sm:self-auto px-4 py-2 bg-primary hover:bg-primary/90 text-primary-foreground font-semibold rounded-lg text-xs shadow-xs transition-colors inline-flex items-center gap-1.5"
      >
        <Bus class="w-3.5 h-3.5" />
        <span>+ Đặt Vé Mới</span>
      </router-link>
    </div>

    <!-- Status Tabs -->
    <div class="flex gap-2 border-b border-border pb-2 overflow-x-auto text-xs font-semibold">
      <button
        type="button"
        @click="currentTab = 'ALL'"
        class="px-3.5 py-1.5 rounded-lg transition-colors"
        :class="currentTab === 'ALL' ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:bg-muted'"
      >
        Tất cả ({{ statusCounts.ALL }})
      </button>

      <button
        type="button"
        @click="currentTab = 'HELD'"
        class="px-3.5 py-1.5 rounded-lg transition-colors"
        :class="currentTab === 'HELD' ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:bg-muted'"
      >
        Đang giữ chỗ ({{ statusCounts.HELD }})
      </button>

      <button
        type="button"
        @click="currentTab = 'CONFIRMED'"
        class="px-3.5 py-1.5 rounded-lg transition-colors"
        :class="currentTab === 'CONFIRMED' ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:bg-muted'"
      >
        Đã xác nhận ({{ statusCounts.CONFIRMED }})
      </button>

      <button
        type="button"
        @click="currentTab = 'CANCELLED'"
        class="px-3.5 py-1.5 rounded-lg transition-colors"
        :class="currentTab === 'CANCELLED' ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:bg-muted'"
      >
        Đã hủy ({{ statusCounts.CANCELLED }})
      </button>

      <button
        type="button"
        @click="currentTab = 'EXPIRED'"
        class="px-3.5 py-1.5 rounded-lg transition-colors"
        :class="currentTab === 'EXPIRED' ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:bg-muted'"
      >
        Hết hạn ({{ statusCounts.EXPIRED }})
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
      <AppButton size="sm" variant="outline" @click="fetchMyBookings(currentPage)">
        <RefreshCw class="w-3.5 h-3.5 mr-1" />
        Thử Lại
      </AppButton>
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
            <AppButton
              variant="outline"
              size="sm"
              class="text-destructive hover:bg-destructive/10 border-destructive/30"
              @click="openCancelDialog(item)"
            >
              <XCircle class="w-3.5 h-3.5 mr-1" />
              Hủy Giữ Chỗ
            </AppButton>

            <router-link :to="`/checkout/${item.bookingReference}`">
              <AppButton size="sm">
                <CreditCard class="w-3.5 h-3.5 mr-1" />
                Tiếp Tục Thanh Toán
              </AppButton>
            </router-link>
          </template>

          <!-- CONFIRMED Actions -->
          <template v-else-if="item.status === 'CONFIRMED'">
            <AppButton size="sm" @click="openTicketDetail(item)">
              <QrCode class="w-3.5 h-3.5 mr-1" />
              Xem Chi Tiết & Vé Điện Tử
            </AppButton>
          </template>

          <!-- CANCELLED / EXPIRED Actions -->
          <template v-else>
            <router-link :to="`/trips/${item.tripId}/seats`">
              <AppButton variant="outline" size="sm">
                <RefreshCw class="w-3.5 h-3.5 mr-1" />
                Đặt Lại Chuyến Này
              </AppButton>
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
          <AppButton
            variant="outline"
            size="sm"
            :disabled="!hasPrevious"
            @click="handlePageChange(currentPage - 1)"
          >
            <ChevronLeft class="w-4 h-4 mr-1" />
            Trước
          </AppButton>

          <AppButton
            variant="outline"
            size="sm"
            :disabled="!hasNext"
            @click="handlePageChange(currentPage + 1)"
          >
            Sau
            <ChevronRight class="w-4 h-4 ml-1" />
          </AppButton>
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
            sẽ được giải phóng ngay lập tức trên hệ thống để người khác có thể đặt.
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
            Thông tin chi tiết vé đã được xác nhận thanh toán an toàn.
          </DialogDescription>
        </DialogHeader>

        <div v-if="selectedBookingForDetail" class="space-y-4 text-xs py-2">
          <div class="bg-muted/50 p-4 rounded-lg border border-border space-y-2.5">
            <div class="flex justify-between">
              <span class="text-muted-foreground">Mã tham chiếu vé:</span>
              <strong class="font-mono text-foreground">{{ selectedBookingForDetail.bookingReference }}</strong>
            </div>
            <div class="flex justify-between">
              <span class="text-muted-foreground">Chuyến xe:</span>
              <strong class="text-foreground">#{{ selectedBookingForDetail.tripId }}</strong>
            </div>
            <div class="flex justify-between">
              <span class="text-muted-foreground">Ghế đã xác nhận:</span>
              <strong class="font-mono text-primary text-sm">{{ (selectedBookingForDetail.seatNumbers || []).join(', ') }}</strong>
            </div>
            <div class="flex justify-between">
              <span class="text-muted-foreground">Tổng tiền:</span>
              <strong class="font-mono text-foreground">{{ Number(selectedBookingForDetail.totalAmount).toLocaleString('vi-VN') }} đ</strong>
            </div>
            <div class="flex justify-between">
              <span class="text-muted-foreground">Thời gian đặt vé:</span>
              <span class="text-foreground">{{ formatDate(selectedBookingForDetail.createdAt) }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-muted-foreground">Trạng thái:</span>
              <StatusBadge :status="selectedBookingForDetail.status" />
            </div>
          </div>

          <!-- Authoritative Capability Note -->
          <div class="p-3 bg-primary/5 rounded-lg border border-primary/20 space-y-1.5 text-[11px]">
            <div class="flex items-center gap-1.5 font-bold text-primary">
              <ShieldCheck class="w-4 h-4 text-primary shrink-0" />
              <span>Hướng Dẫn Lên Xe</span>
            </div>
            <p class="text-muted-foreground leading-relaxed">
              Quý khách vui lòng xuất trình <strong>Mã tham chiếu vé</strong> cùng với Số điện thoại hoặc CCCD của hành khách khi đến bến xe hoặc điểm đón.
            </p>
            <p class="text-[10px] text-muted-foreground/80 italic">
              * Tính năng hiển thị QR điện tử trực tiếp trên ứng dụng web đang hoàn thiện tích hợp với cổng soát vé của đối tác nhà xe.
            </p>
          </div>
        </div>

        <DialogFooter>
          <AppButton variant="outline" size="sm" @click="showDetailModal = false">
            Đóng
          </AppButton>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>
