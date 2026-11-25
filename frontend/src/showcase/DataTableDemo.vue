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
  AlertCircle
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
  }
])

const searchQuery = ref('')
const selectedStatus = ref<string>('ALL')
const selectedRows = ref<string[]>([])
const sortAsc = ref(true)

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

const isAllSelected = computed(() => {
  return filteredBookings.value.length > 0 && selectedRows.value.length === filteredBookings.value.length
})

function toggleSelectAll(checked: boolean) {
  if (checked) {
    selectedRows.value = filteredBookings.value.map(b => b.id)
  } else {
    selectedRows.value = []
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
  <div class="space-y-4">
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

      <div class="flex items-center gap-2">
        <select
          v-model="selectedStatus"
          class="h-9 px-3 rounded-lg border border-input text-xs font-semibold text-foreground bg-background focus:ring-2 focus:ring-ring"
        >
          <option value="ALL">Tất cả trạng thái</option>
          <option value="CONFIRMED">Đã xác nhận</option>
          <option value="PENDING">Chờ thanh toán</option>
          <option value="REFUNDED">Đã hoàn tiền</option>
          <option value="CANCELLED">Đã hủy</option>
        </select>

        <Button variant="outline" size="sm" class="h-9 text-xs gap-1.5" @click="sortAsc = !sortAsc">
          <ArrowUpDown class="h-3.5 w-3.5" />
          <span>{{ sortAsc ? 'Giá: Thấp &rarr; Cao' : 'Giá: Cao &rarr; Thấp' }}</span>
        </Button>
      </div>
    </div>

    <!-- Selection Bar -->
    <div v-if="selectedRows.length > 0" class="p-2.5 bg-primary/10 border border-primary/20 rounded-lg flex items-center justify-between text-xs">
      <span class="font-semibold text-primary">
        Đã chọn <strong>{{ selectedRows.length }}</strong> đơn vé
      </span>
      <div class="flex items-center gap-2">
        <Button variant="outline" size="sm" class="h-7 text-xs bg-background">Xuất PDF</Button>
        <Button variant="destructive" size="sm" class="h-7 text-xs">Hủy hàng loạt</Button>
      </div>
    </div>

    <!-- Table with horizontal scroll -->
    <div class="rounded-xl border border-border bg-card shadow-xs overflow-hidden">
      <div class="overflow-x-auto">
        <Table class="w-full min-w-[760px] text-xs">
          <TableHeader>
            <TableRow class="bg-muted/40">
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
              <TableHead class="font-bold text-right">Thao Tác</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow
              v-for="b in filteredBookings"
              :key="b.id"
              class="hover:bg-muted/30 transition-colors"
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
                <div class="text-[11px] text-muted-foreground tabular-nums">{{ b.phone }}</div>
              </TableCell>
              <TableCell class="font-medium text-foreground">{{ b.route }}</TableCell>
              <TableCell class="text-center font-bold text-primary">{{ b.seats.join(', ') }}</TableCell>
              <TableCell class="text-right font-bold text-foreground tabular-nums">
                {{ b.amount.toLocaleString('vi-VN') }} đ
              </TableCell>
              <TableCell class="text-center">
                <StatusBadge :status="b.status" />
              </TableCell>
              <TableCell class="text-right">
                <div class="inline-flex items-center gap-1">
                  <Button variant="ghost" size="icon" class="h-7 w-7 text-muted-foreground hover:text-foreground">
                    <Eye class="h-3.5 w-3.5" />
                  </Button>
                  <Button variant="ghost" size="icon" class="h-7 w-7 text-muted-foreground hover:text-foreground">
                    <Pencil class="h-3.5 w-3.5" />
                  </Button>
                </div>
              </TableCell>
            </TableRow>

            <TableRow v-if="filteredBookings.length === 0">
              <TableCell colspan="8" class="text-center py-8 text-muted-foreground">
                Không tìm thấy đơn vé nào phù hợp.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </div>
    </div>
  </div>
</template>
