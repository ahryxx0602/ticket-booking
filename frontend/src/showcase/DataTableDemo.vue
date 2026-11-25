<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell
} from '@/components/ui/table'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Badge, StatusBadge } from '@/components/ui/badge'
import { Checkbox } from '@/components/ui/checkbox'
import {
  Search,
  ArrowUpDown,
  Filter,
  Eye,
  Pencil,
  Trash2,
  Download,
  AlertCircle,
  SlidersHorizontal,
  ChevronLeft,
  ChevronRight
} from 'lucide-vue-next'

interface BookingRecord {
  id: string
  customer: string
  phone: string
  route: string
  seats: string[]
  amount: number
  status: 'CONFIRMED' | 'PENDING' | 'REFUNDED' | 'CANCELLED'
  createdAt: string
}

const bookings = ref<BookingRecord[]>([
  {
    id: 'BKG-20261125-001',
    customer: 'Nguyễn Văn An',
    phone: '0901234567',
    route: 'Hà Nội → Đà Nẵng',
    seats: ['A01', 'A02'],
    amount: 850000,
    status: 'CONFIRMED',
    createdAt: '25/11/2026 08:30'
  },
  {
    id: 'BKG-20261125-002',
    customer: 'Trần Thị Bích',
    phone: '0912345678',
    route: 'TP.HCM → Đà Lạt',
    seats: ['B05'],
    amount: 380000,
    status: 'CONFIRMED',
    createdAt: '25/11/2026 09:15'
  },
  {
    id: 'BKG-20261125-003',
    customer: 'Lê Hoàng Nam',
    phone: '0987654321',
    route: 'Đà Nẵng → Quy Nhơn',
    seats: ['A07'],
    amount: 240000,
    status: 'PENDING',
    createdAt: '25/11/2026 10:00'
  },
  {
    id: 'BKG-20261125-004',
    customer: 'Phạm Minh Đức',
    phone: '0933445566',
    route: 'Hà Nội → Sapa',
    seats: ['A03', 'A04'],
    amount: 620000,
    status: 'REFUNDED',
    createdAt: '24/11/2026 14:20'
  },
  {
    id: 'BKG-20261125-005',
    customer: 'Vũ Thị Hằng',
    phone: '0977889900',
    route: 'TP.HCM → Vũng Tàu',
    seats: ['C01'],
    amount: 190000,
    status: 'CANCELLED',
    createdAt: '24/11/2026 16:45'
  },
  {
    id: 'BKG-20261125-006',
    customer: 'Đỗ Quốc Cường',
    phone: '0945678901',
    route: 'Hà Nội → Hải Phòng',
    seats: ['B01', 'B02'],
    amount: 300000,
    status: 'CONFIRMED',
    createdAt: '25/11/2026 11:20'
  },
  {
    id: 'BKG-20261125-007',
    customer: 'Hoàng Lan Anh',
    phone: '0966112233',
    route: 'Huế → Đà Nẵng',
    seats: ['A12'],
    amount: 160000,
    status: 'CONFIRMED',
    createdAt: '25/11/2026 12:00'
  }
])

const searchQuery = ref('')
const selectedStatus = ref<string>('ALL')
const selectedRows = ref<string[]>([])
const sortAsc = ref(true)
const tableDensity = ref<'compact' | 'default' | 'comfortable'>('default')

// Pagination state
const currentPage = ref(1)
const pageSize = ref(4)

const filteredBookings = computed(() => {
  return bookings.value.filter(b => {
    const matchesSearch = b.id.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
                          b.customer.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
                          b.route.toLowerCase().includes(searchQuery.value.toLowerCase())
    const matchesStatus = selectedStatus.value === 'ALL' || b.status === selectedStatus.value
    return matchesSearch && matchesStatus
  }).sort((a, b) => {
    return sortAsc.value ? a.amount - b.amount : b.amount - a.amount
  })
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredBookings.value.length / pageSize.value)))

const paginatedBookings = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredBookings.value.slice(start, start + pageSize.value)
})

const isAllSelected = computed(() => {
  return paginatedBookings.value.length > 0 &&
    paginatedBookings.value.every(b => selectedRows.value.includes(b.id))
})

function toggleSelectAll(checked: boolean) {
  if (checked) {
    const pageIds = paginatedBookings.value.map(b => b.id)
    selectedRows.value = Array.from(new Set([...selectedRows.value, ...pageIds]))
  } else {
    const pageIds = new Set(paginatedBookings.value.map(b => b.id))
    selectedRows.value = selectedRows.value.filter(id => !pageIds.has(id))
  }
}

function toggleSelectRow(id: string) {
  const idx = selectedRows.value.indexOf(id)
  if (idx >= 0) {
    selectedRows.value.splice(idx, 1)
  } else {
    selectedRows.value.push(id)
  }
}
</script>

<template>
  <div class="space-y-4" :data-density="tableDensity">
    <!-- Toolbar -->
    <div class="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
      <div class="flex items-center gap-2 flex-1 max-w-md">
        <div class="relative w-full">
          <Search class="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground pointer-events-none" />
          <Input
            v-model="searchQuery"
            placeholder="Tìm theo mã vé, hành khách, tuyến đường..."
            class="pl-9 h-9 text-xs"
          />
        </div>
      </div>

      <div class="flex flex-wrap items-center gap-2">
        <select
          v-model="selectedStatus"
          class="h-9 px-3 rounded-md border border-input text-xs font-semibold text-foreground bg-background focus:ring-2 focus:ring-ring"
        >
          <option value="ALL">Tất cả trạng thái</option>
          <option value="CONFIRMED">Đã xác nhận</option>
          <option value="PENDING">Chờ thanh toán</option>
          <option value="REFUNDED">Đã hoàn tiền</option>
          <option value="CANCELLED">Đã hủy</option>
        </select>

        <Button variant="outline" size="sm" class="h-9 text-xs gap-1.5" @click="sortAsc = !sortAsc">
          <ArrowUpDown class="h-3.5 w-3.5" />
          <span>{{ sortAsc ? 'Giá: Tăng dần' : 'Giá: Giảm dần' }}</span>
        </Button>

        <!-- Density Selector (UI-05 & UI-08) -->
        <div class="inline-flex rounded-md border border-input p-0.5 bg-muted/40">
          <button
            type="button"
            class="px-2 py-1 text-[11px] rounded transition-colors font-medium cursor-pointer"
            :class="tableDensity === 'compact' ? 'bg-background font-bold shadow-xs text-foreground' : 'text-muted-foreground hover:text-foreground'"
            @click="tableDensity = 'compact'"
          >
            Compact (44px)
          </button>
          <button
            type="button"
            class="px-2 py-1 text-[11px] rounded transition-colors font-medium cursor-pointer"
            :class="tableDensity === 'default' ? 'bg-background font-bold shadow-xs text-foreground' : 'text-muted-foreground hover:text-foreground'"
            @click="tableDensity = 'default'"
          >
            Default (52px)
          </button>
          <button
            type="button"
            class="px-2 py-1 text-[11px] rounded transition-colors font-medium cursor-pointer"
            :class="tableDensity === 'comfortable' ? 'bg-background font-bold shadow-xs text-foreground' : 'text-muted-foreground hover:text-foreground'"
            @click="tableDensity = 'comfortable'"
          >
            Comfortable (60px)
          </button>
        </div>
      </div>
    </div>

    <!-- Selection Bar -->
    <div v-if="selectedRows.length > 0" class="p-2.5 bg-primary/10 border border-primary/20 rounded-md flex items-center justify-between text-xs animate-in fade-in duration-200">
      <span class="font-semibold text-primary">
        Đã chọn <strong>{{ selectedRows.length }}</strong> đơn vé trên toàn bộ bảng
      </span>
      <div class="flex items-center gap-2">
        <Button variant="outline" size="sm" class="h-7 text-xs bg-background">Xuất PDF</Button>
        <Button variant="destructive" size="sm" class="h-7 text-xs" @click="selectedRows = []">Bỏ chọn tất cả</Button>
      </div>
    </div>

    <!-- Single Main Table Surface (UI-06: No nested boxed frames) -->
    <div class="rounded-lg border border-border bg-card shadow-xs overflow-hidden">
      <div class="overflow-x-auto">
        <Table data-table class="w-full min-w-[760px] text-xs">
          <TableHeader>
            <TableRow class="bg-muted/50 border-b border-border">
              <TableHead class="w-10 text-center">
                <Checkbox
                  :checked="isAllSelected"
                  @update:checked="toggleSelectAll"
                />
              </TableHead>
              <TableHead class="font-bold">Mã Đơn Vé</TableHead>
              <TableHead class="font-bold">Hành Khách</TableHead>
              <TableHead class="font-bold">Tuyến Đường</TableHead>
              <TableHead class="font-bold text-center">Ghế</TableHead>
              <TableHead class="font-bold text-right">Tổng Tiền</TableHead>
              <TableHead class="font-bold text-center">Trạng Thái</TableHead>
              <TableHead class="font-bold text-right pr-4">Thao Tác</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow
              v-for="b in paginatedBookings"
              :key="b.id"
              class="hover:bg-muted/30 transition-colors border-b border-border/60 last:border-0"
              :class="{ 'bg-primary/5': selectedRows.includes(b.id) }"
            >
              <TableCell class="text-center">
                <Checkbox
                  :checked="selectedRows.includes(b.id)"
                  @update:checked="toggleSelectRow(b.id)"
                />
              </TableCell>
              <TableCell class="font-mono font-bold text-primary">{{ b.id }}</TableCell>
              <TableCell>
                <div class="font-semibold text-foreground">{{ b.customer }}</div>
                <div class="text-[11px] text-muted-foreground tabular-nums font-mono">{{ b.phone }}</div>
              </TableCell>
              <TableCell class="font-medium text-foreground">{{ b.route }}</TableCell>
              <TableCell class="text-center font-bold text-primary">{{ b.seats.join(', ') }}</TableCell>
              <TableCell class="text-right font-bold text-foreground font-mono tabular-nums">
                {{ b.amount.toLocaleString('vi-VN') }} đ
              </TableCell>
              <TableCell class="text-center">
                <StatusBadge :status="b.status" />
              </TableCell>
              <TableCell class="text-right pr-4">
                <div class="inline-flex items-center gap-1">
                  <Button variant="ghost" size="icon" class="h-8 w-8 text-muted-foreground hover:text-foreground hover:bg-muted" title="Xem chi tiết">
                    <Eye class="h-4 w-4" />
                  </Button>
                  <Button variant="ghost" size="icon" class="h-8 w-8 text-muted-foreground hover:text-foreground hover:bg-muted" title="Chỉnh sửa">
                    <Pencil class="h-4 w-4" />
                  </Button>
                </div>
              </TableCell>
            </TableRow>

            <TableRow v-if="filteredBookings.length === 0">
              <TableCell colspan="8" class="text-center py-10 text-muted-foreground">
                <div class="flex flex-col items-center justify-center space-y-1">
                  <AlertCircle class="h-8 w-8 text-muted-foreground/60 mb-1" />
                  <div class="font-medium">Không tìm thấy đơn vé nào phù hợp.</div>
                  <div class="text-[11px]">Thử điều chỉnh từ khóa tìm kiếm hoặc bỏ chọn bộ lọc trạng thái.</div>
                </div>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </div>

      <!-- Pagination Footer -->
      <div class="flex items-center justify-between px-4 py-3 bg-muted/20 border-t border-border text-xs text-muted-foreground">
        <div>
          Hiển thị trang <strong class="text-foreground">{{ currentPage }}</strong> / {{ totalPages }} (Tổng {{ filteredBookings.length }} bản ghi)
        </div>
        <div class="flex items-center space-x-2">
          <Button
            variant="outline"
            size="sm"
            class="h-8 px-2.5 text-xs gap-1"
            :disabled="currentPage <= 1"
            @click="currentPage--"
          >
            <ChevronLeft class="h-3.5 w-3.5" />
            <span>Trước</span>
          </Button>
          <Button
            variant="outline"
            size="sm"
            class="h-8 px-2.5 text-xs gap-1"
            :disabled="currentPage >= totalPages"
            @click="currentPage++"
          >
            <span>Sau</span>
            <ChevronRight class="h-3.5 w-3.5" />
          </Button>
        </div>
      </div>
    </div>
  </div>
</template>
