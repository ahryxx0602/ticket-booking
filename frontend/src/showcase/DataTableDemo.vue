<script setup lang="ts">
import { ref, h, computed } from 'vue'
import type { ColumnDef } from '@tanstack/vue-table'
import DataTable from '@/components/ui/table/DataTable.vue'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Badge, StatusBadge } from '@/components/ui/badge'
import { Checkbox } from '@/components/ui/checkbox'
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem
} from '@/components/ui/select'
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuCheckboxItem,
  DropdownMenuLabel,
  DropdownMenuSeparator
} from '@/components/ui/dropdown-menu'
import {
  Search,
  ArrowUpDown,
  Eye,
  Pencil,
  Trash2,
  SlidersHorizontal,
  ArrowRight
} from 'lucide-vue-next'

export interface BookingRecord {
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

const dataTableRef = ref<any>(null)
const searchQuery = ref('')
const selectedStatus = ref<string>('ALL')
const tableDensity = ref<'compact' | 'default' | 'comfortable'>('default')
const selectedRecords = ref<BookingRecord[]>([])
const sortAsc = ref(true)

// Column Visibility State
const columnVisibility = ref<Record<string, boolean>>({
  phone: true,
  createdAt: true
})

function formatCurrency(amount: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount)
}

function handleSortToggle() {
  sortAsc.value = !sortAsc.value
  if (dataTableRef.value?.table) {
    dataTableRef.value.table.setSorting([{ id: 'amount', desc: !sortAsc.value }])
  }
}

// Columns definition for TanStack Table (REV-03 & REV-04)
const columns: ColumnDef<any, BookingRecord, any>[] = [
  // 1. Select Checkbox Column
  {
    id: 'select',
    header: ({ table }: { table: any }) =>
      h(
        'div',
        { class: 'flex justify-center items-center w-10' },
        h(Checkbox, {
          modelValue: table.getIsAllPageRowsSelected(),
          'onUpdate:modelValue': (val: boolean | 'indeterminate') => table.toggleAllPageRowsSelected(!!val),
          'aria-label': 'Chọn tất cả dòng'
        })
      ),
    cell: ({ row }: { row: any }) =>
      h(
        'div',
        { class: 'flex justify-center items-center w-10' },
        h(Checkbox, {
          modelValue: row.getIsSelected(),
          'onUpdate:modelValue': (val: boolean | 'indeterminate') => row.toggleSelected(!!val),
          'aria-label': `Chọn dòng ${row.original.id}`
        })
      ),
    enableSorting: false,
    enableHiding: false
  },

  // 2. Booking ID Column (whitespace-nowrap, font-mono, min-w 150px)
  {
    accessorKey: 'id',
    header: 'Mã Đặt Vé',
    cell: ({ row }) =>
      h(
        'span',
        { class: 'font-mono font-bold text-primary whitespace-nowrap min-w-[140px] inline-block' },
        row.original.id
      )
  },

  // 3. Customer Info Column
  {
    accessorKey: 'customer',
    header: 'Hành Khách',
    cell: ({ row }) =>
      h('div', { class: 'min-w-[150px]' }, [
        h('div', { class: 'font-semibold text-foreground' }, row.original.customer),
        columnVisibility.value.phone !== false
          ? h('div', { class: 'text-[11px] font-mono text-muted-foreground' }, row.original.phone)
          : null
      ])
  },

  // 4. Route Column
  {
    accessorKey: 'route',
    header: 'Tuyến Đường',
    cell: ({ row }) => {
      const parts = row.original.route.split('→')
      return h('div', { class: 'flex items-center gap-1.5 min-w-[160px] text-foreground' }, [
        h('span', { class: 'font-medium' }, parts[0]?.trim()),
        h(ArrowRight, { class: 'h-3 w-3 text-muted-foreground shrink-0' }),
        h('span', { class: 'font-medium' }, parts[1]?.trim())
      ])
    }
  },

  // 5. Seats Column
  {
    accessorKey: 'seats',
    header: 'Ghế',
    cell: ({ row }) =>
      h(
        'div',
        { class: 'flex flex-wrap gap-1 min-w-[90px]' },
        row.original.seats.map((seat: string) =>
          h(
            Badge,
            { variant: 'secondary', class: 'font-mono text-[10px] py-0 px-1.5 h-4.5 font-bold' },
            () => seat
          )
        )
      )
  },

  // 6. Monetary Value Column (Right-aligned, Tabular Numbers, whitespace-nowrap, REV-04)
  {
    accessorKey: 'amount',
    header: () => h('div', { class: 'text-right min-w-[120px]' }, 'Tổng Tiền'),
    cell: ({ row }) =>
      h(
        'div',
        {
          class:
            'text-right font-mono font-bold text-foreground tabular-nums whitespace-nowrap min-w-[120px]'
        },
        formatCurrency(row.original.amount)
      )
  },

  // 7. Status Badge Column (shrink-0, no distortion, REV-04)
  {
    accessorKey: 'status',
    header: 'Trạng Thái',
    cell: ({ row }) =>
      h(
        'div',
        { class: 'min-w-[120px] whitespace-nowrap' },
        h(StatusBadge, { status: row.original.status })
      )
  },

  // 8. Created Date Column
  {
    accessorKey: 'createdAt',
    header: 'Ngày Đặt',
    cell: ({ row }) =>
      h(
        'span',
        { class: 'font-mono text-muted-foreground tabular-nums whitespace-nowrap min-w-[120px]' },
        row.original.createdAt
      )
  },

  // 9. Actions Column (16px icon, proper hit area)
  {
    id: 'actions',
    header: () => h('div', { class: 'text-right min-w-[100px]' }, 'Thao Tác'),
    cell: ({ row }) =>
      h('div', { class: 'flex items-center justify-end gap-1 min-w-[100px]' }, [
        h(
          Button,
          {
            variant: 'ghost',
            size: 'sm',
            class: 'h-8 w-8 p-0 text-muted-foreground hover:text-foreground cursor-pointer',
            title: `Xem chi tiết ${row.original.id}`
          },
          () => h(Eye, { class: 'h-4 w-4' })
        ),
        h(
          Button,
          {
            variant: 'ghost',
            size: 'sm',
            class: 'h-8 w-8 p-0 text-muted-foreground hover:text-foreground cursor-pointer',
            title: `Chỉnh sửa ${row.original.id}`
          },
          () => h(Pencil, { class: 'h-4 w-4' })
        ),
        h(
          Button,
          {
            variant: 'ghost',
            size: 'sm',
            class: 'h-8 w-8 p-0 text-destructive hover:bg-destructive/10 cursor-pointer',
            title: `Hủy vé ${row.original.id}`
          },
          () => h(Trash2, { class: 'h-4 w-4' })
        )
      ])
  }
]
</script>

<template>
  <div class="space-y-4">
    <!-- Toolbar: Search, Filters, Sorting, Density, Column Visibility (REV-03, REV-04) -->
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
        <!-- Status Filter Custom Select (Shadcn Vue) -->
        <Select v-model="selectedStatus">
          <SelectTrigger class="h-9 w-[150px] text-xs font-semibold bg-background border-input">
            <SelectValue placeholder="Tất cả trạng thái" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">Tất cả trạng thái</SelectItem>
            <SelectItem value="CONFIRMED">Đã xác nhận</SelectItem>
            <SelectItem value="PENDING">Chờ thanh toán</SelectItem>
            <SelectItem value="REFUNDED">Đã hoàn tiền</SelectItem>
            <SelectItem value="CANCELLED">Đã hủy</SelectItem>
          </SelectContent>
        </Select>

        <!-- Sort Button: Triggers TanStack sorting -->
        <Button variant="outline" size="sm" class="h-9 text-xs gap-1.5 cursor-pointer" @click="handleSortToggle">
          <ArrowUpDown class="h-3.5 w-3.5" />
          <span>{{ sortAsc ? 'Giá: Tăng dần' : 'Giá: Giảm dần' }}</span>
        </Button>

        <!-- Density Selector: Tối ưu nhỏ gọn, hiển thị rõ độ cao 44/52/60px -->
        <div class="inline-flex items-center rounded-md border border-input p-0.5 bg-muted/40 text-[11px]">
          <button
            type="button"
            class="px-2 py-1 rounded transition-colors font-medium cursor-pointer"
            :class="tableDensity === 'compact' ? 'bg-background font-bold shadow-xs text-foreground' : 'text-muted-foreground hover:text-foreground'"
            title="Độ cao dòng Compact (44px)"
            @click="tableDensity = 'compact'"
          >
            Gọn (44px)
          </button>
          <button
            type="button"
            class="px-2 py-1 rounded transition-colors font-medium cursor-pointer"
            :class="tableDensity === 'default' ? 'bg-background font-bold shadow-xs text-foreground' : 'text-muted-foreground hover:text-foreground'"
            title="Độ cao dòng Default (52px)"
            @click="tableDensity = 'default'"
          >
            Chuẩn (52px)
          </button>
          <button
            type="button"
            class="px-2 py-1 rounded transition-colors font-medium cursor-pointer"
            :class="tableDensity === 'comfortable' ? 'bg-background font-bold shadow-xs text-foreground' : 'text-muted-foreground hover:text-foreground'"
            title="Độ cao dòng Comfortable (60px)"
            @click="tableDensity = 'comfortable'"
          >
            Rộng (60px)
          </button>
        </div>

        <!-- Column Visibility Dropdown Menu -->
        <DropdownMenu>
          <DropdownMenuTrigger as-child>
            <Button variant="outline" size="sm" class="h-9 text-xs gap-1.5 cursor-pointer">
              <SlidersHorizontal class="h-3.5 w-3.5" />
              <span>Cột</span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent class="w-48" align="end">
            <DropdownMenuLabel class="text-xs">Hiển thị cột bổ sung</DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuCheckboxItem v-model:checked="columnVisibility.phone">
              Số điện thoại
            </DropdownMenuCheckboxItem>
            <DropdownMenuCheckboxItem v-model:checked="columnVisibility.createdAt">
              Ngày đặt vé
            </DropdownMenuCheckboxItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </div>

    <!-- Selection Bar -->
    <div
      v-if="selectedRecords.length > 0"
      class="p-2.5 bg-primary/10 border border-primary/20 rounded-md flex items-center justify-between text-xs animate-in fade-in duration-200"
    >
      <span class="font-semibold text-primary">
        Đã chọn <strong>{{ selectedRecords.length }}</strong> đơn vé trên bảng dữ liệu
      </span>
      <div class="flex items-center gap-2">
        <Button variant="outline" size="sm" class="h-7 text-xs bg-background cursor-pointer">Xuất PDF</Button>
        <Button
          variant="destructive"
          size="sm"
          class="h-7 text-xs cursor-pointer"
          @click="dataTableRef?.clearSelection()"
        >
          Bỏ chọn tất cả
        </Button>
      </div>
    </div>

    <!-- Reusable TanStack DataTable Component (REV-03, REV-04) -->
    <DataTable
      ref="dataTableRef"
      :columns="columns"
      :data="bookings"
      :page-size="4"
      :density="tableDensity"
      :global-filter="searchQuery"
      :status-filter="selectedStatus"
      @update:selection="selectedRecords = $event"
    />
  </div>
</template>
