<script setup lang="ts" generic="TData extends Record<string, any>">
import { ref, computed, watch } from 'vue'
import {
  type ColumnDef,
  type SortingState,
  type ColumnFiltersState,
  type ColumnVisibilityState,
  type RowSelectionState,
  FlexRender,
  createCoreRowModel,
  createPaginatedRowModel,
  createSortedRowModel,
  createFilteredRowModel,
  stockFeatures,
  useTable
} from '@tanstack/vue-table'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow
} from '@/components/ui/table'
import { Button } from '@/components/ui/button'
import {
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
  AlertCircle,
  Search
} from 'lucide-vue-next'

const props = withDefaults(
  defineProps<{
    columns: ColumnDef<any, TData, any>[]
    data: TData[]
    pageSize?: number
    loading?: boolean
    error?: string | null
    emptyText?: string
    density?: 'compact' | 'default' | 'comfortable'
    globalFilter?: string
    statusFilter?: string
    columnVisibility?: ColumnVisibilityState
    getRowId?: (row: TData, index: number) => string
    manualPagination?: boolean
    pageCount?: number
    pageIndex?: number
    totalElements?: number
  }>(),
  {
    pageSize: 5,
    loading: false,
    error: null,
    emptyText: 'Không tìm thấy dữ liệu phù hợp.',
    density: 'default',
    manualPagination: false,
    pageIndex: 0
  }
)

const emits = defineEmits<{
  (e: 'update:selection', rows: TData[]): void
  (e: 'row-click', row: TData): void
  (e: 'update:pageIndex', page: number): void
  (e: 'page-change', page: number): void
}>()

const sorting = ref<SortingState>([])
const columnFilters = ref<ColumnFiltersState>([])
const rowSelection = ref<RowSelectionState>({})
const columnVisibility = ref<ColumnVisibilityState>(props.columnVisibility || {})
const globalFilter = ref(props.globalFilter || '')

function resolveRowId(row: any, index: number): string {
  if (props.getRowId) return props.getRowId(row, index)
  if (row && typeof row === 'object') {
    if (row.id !== undefined && row.id !== null) return String(row.id)
    if (row.bookingId !== undefined && row.bookingId !== null) return String(row.bookingId)
    if (row._id !== undefined && row._id !== null) return String(row._id)
    if (row.uuid !== undefined && row.uuid !== null) return String(row.uuid)
    if (row.code !== undefined && row.code !== null) return String(row.code)
  }
  return String(index)
}

function emitSelectedRows() {
  const selectedMap = rowSelection.value || {}
  const selectedKeys = Object.keys(selectedMap).filter((k) => selectedMap[k])
  const selected = props.data.filter((item, idx) => {
    const id = resolveRowId(item, idx)
    return selectedKeys.includes(id)
  })
  emits('update:selection', selected)
}

// TanStack Table v9 Instance
const table = useTable<any, any>({
  features: {
    ...stockFeatures,
    coreRowModel: createCoreRowModel(),
    filteredRowModel: createFilteredRowModel(),
    sortedRowModel: createSortedRowModel(),
    paginatedRowModel: createPaginatedRowModel()
  },
  getRowId: (row: any, index: number) => resolveRowId(row, index),
  manualPagination: props.manualPagination,
  pageCount: props.pageCount,
  get data() {
    return props.data
  },
  get columns() {
    return props.columns as any
  },
  state: {
    get sorting() {
      return sorting.value
    },
    get columnFilters() {
      return columnFilters.value
    },
    get rowSelection() {
      return rowSelection.value
    },
    get columnVisibility() {
      return columnVisibility.value
    },
    get globalFilter() {
      return globalFilter.value
    }
  },
  initialState: {
    pagination: {
      pageIndex: 0,
      pageSize: props.pageSize || 5
    }
  },
  onSortingChange: (updater: any) => {
    sorting.value = typeof updater === 'function' ? updater(sorting.value) : updater
  },
  onColumnFiltersChange: (updater: any) => {
    columnFilters.value = typeof updater === 'function' ? updater(columnFilters.value) : updater
    table.setPageIndex(0) // Search/filter page reset
  },
  onRowSelectionChange: (updater: any) => {
    rowSelection.value = typeof updater === 'function' ? updater(rowSelection.value) : updater
    emitSelectedRows()
  },
  onColumnVisibilityChange: (updater: any) => {
    columnVisibility.value = typeof updater === 'function' ? updater(columnVisibility.value) : updater
  },
  onGlobalFilterChange: (updater: any) => {
    globalFilter.value = typeof updater === 'function' ? updater(globalFilter.value) : updater
    table.setPageIndex(0) // Search/filter page reset
  }
})

// Sync props with table state
watch(
  () => props.globalFilter,
  (val) => {
    globalFilter.value = val || ''
    table.setPageIndex(0)
  }
)

watch(
  () => props.statusFilter,
  (val) => {
    if (!val || val === 'ALL') {
      columnFilters.value = columnFilters.value.filter((f) => f.id !== 'status')
    } else {
      const remaining = columnFilters.value.filter((f) => f.id !== 'status')
      columnFilters.value = [...remaining, { id: 'status', value: val }]
    }
    table.setPageIndex(0)
  }
)

watch(
  () => props.pageSize,
  (val) => {
    if (val) table.setPageSize(val)
  }
)

watch(
  rowSelection,
  () => {
    emitSelectedRows()
  },
  { deep: true }
)

const currentPageIndex = computed(() => {
  if (props.manualPagination && props.pageIndex !== undefined) {
    return props.pageIndex
  }
  return (table.atoms?.pagination as any)?.value?.pageIndex ?? 0
})

const totalPages = computed(() => {
  if (props.manualPagination && props.pageCount !== undefined) {
    return Math.max(1, props.pageCount)
  }
  return Math.max(1, table.getPageCount())
})

const totalRowCount = computed(() => {
  if (props.manualPagination && props.totalElements !== undefined) {
    return props.totalElements
  }
  return table.getFilteredRowModel().rows.length
})

const canPreviousPage = computed(() => {
  if (props.manualPagination) {
    return (props.pageIndex ?? 0) > 0
  }
  return table.getCanPreviousPage()
})

const canNextPage = computed(() => {
  if (props.manualPagination) {
    const count = props.pageCount ?? 1
    return (props.pageIndex ?? 0) + 1 < count
  }
  return table.getCanNextPage()
})

function handleFirstPage() {
  if (props.manualPagination) {
    emits('update:pageIndex', 0)
    emits('page-change', 0)
  } else {
    table.firstPage()
  }
}

function handlePreviousPage() {
  if (props.manualPagination) {
    const prev = Math.max(0, (props.pageIndex ?? 0) - 1)
    emits('update:pageIndex', prev)
    emits('page-change', prev)
  } else {
    table.previousPage()
  }
}

function handleNextPage() {
  if (props.manualPagination) {
    const maxPage = Math.max(0, (props.pageCount ?? 1) - 1)
    const next = Math.min(maxPage, (props.pageIndex ?? 0) + 1)
    emits('update:pageIndex', next)
    emits('page-change', next)
  } else {
    table.nextPage()
  }
}

function handleLastPage() {
  if (props.manualPagination) {
    const last = Math.max(0, (props.pageCount ?? 1) - 1)
    emits('update:pageIndex', last)
    emits('page-change', last)
  } else {
    table.lastPage()
  }
}

const selectedCount = computed(() => {
  return table.getSelectedRowModel().rows.length
})

defineExpose({
  table,
  getSelectedRows: () => table.getSelectedRowModel().rows.map((r: any) => r.original),
  clearSelection: () => table.resetRowSelection()
})
</script>

<template>
  <div class="space-y-3" :data-density="density">
    <!-- Main Table Container: Single border, no nested cards (REV-03, REV-04) -->
    <div class="rounded-lg border border-border bg-card shadow-xs overflow-hidden">
      <div class="overflow-x-auto">
        <Table data-table class="w-full text-xs">
          <TableHeader>
            <TableRow
              v-for="headerGroup in table.getHeaderGroups()"
              :key="headerGroup.id"
              class="bg-muted/50 border-b border-border"
            >
              <TableHead
                v-for="header in headerGroup.headers"
                :key="header.id"
                class="font-semibold text-muted-foreground"
              >
                <FlexRender
                  v-if="!header.isPlaceholder"
                  :render="header.column.columnDef.header"
                  :props="header.getContext()"
                />
              </TableHead>
            </TableRow>
          </TableHeader>

          <TableBody>
            <!-- Loading Skeleton State -->
            <template v-if="loading">
              <TableRow v-for="i in (pageSize || 5)" :key="`loading-${i}`">
                <TableCell :colspan="columns.length" class="py-4">
                  <div class="h-6 w-full animate-pulse bg-muted rounded" />
                </TableCell>
              </TableRow>
            </template>

            <!-- Error State -->
            <template v-else-if="error">
              <TableRow>
                <TableCell :colspan="columns.length" class="h-28 text-center text-destructive text-xs py-6">
                  <div class="flex flex-col items-center gap-1.5">
                    <AlertCircle class="h-5 w-5" />
                    <span>{{ error }}</span>
                  </div>
                </TableCell>
              </TableRow>
            </template>

            <!-- Paginated Data Rows -->
            <template v-else-if="table.getRowModel().rows?.length">
              <TableRow
                v-for="row in table.getRowModel().rows"
                :key="row.id"
                :data-state="row.getIsSelected() ? 'selected' : undefined"
                class="transition-colors hover:bg-muted/40 cursor-default"
                @click="emits('row-click', row.original)"
              >
                <TableCell
                  v-for="cell in row.getVisibleCells()"
                  :key="cell.id"
                >
                  <FlexRender
                    :render="cell.column.columnDef.cell"
                    :props="cell.getContext()"
                  />
                </TableCell>
              </TableRow>
            </template>

            <!-- Empty State -->
            <template v-else>
              <TableRow>
                <TableCell :colspan="columns.length" class="h-28 text-center text-muted-foreground text-xs py-8">
                  <div class="flex flex-col items-center gap-2">
                    <Search class="h-6 w-6 text-muted-foreground/40" />
                    <span>{{ emptyText }}</span>
                  </div>
                </TableCell>
              </TableRow>
            </template>
          </TableBody>
        </Table>
      </div>
    </div>

    <!-- Responsive Pagination Toolbar (REV-04) -->
    <div class="flex flex-col sm:flex-row items-center justify-between gap-3 px-1 text-xs text-muted-foreground">
      <div class="flex items-center gap-2">
        <span>
          Trang <strong class="font-mono text-foreground font-bold">{{ currentPageIndex + 1 }}</strong> / <strong class="font-mono text-foreground font-bold">{{ totalPages }}</strong> (Tổng {{ totalRowCount }} dòng)
        </span>
        <span v-if="selectedCount > 0" class="text-primary font-semibold">
          • Đã chọn <strong class="font-mono">{{ selectedCount }}</strong> dòng
        </span>
      </div>

      <div class="flex items-center space-x-1.5">
        <Button
          variant="outline"
          size="sm"
          class="h-8 w-8 p-0"
          :disabled="!canPreviousPage"
          title="Trang đầu"
          data-testid="pagination-first"
          @click="handleFirstPage"
        >
          <ChevronsLeft class="h-3.5 w-3.5" />
        </Button>
        <Button
          variant="outline"
          size="sm"
          class="h-8 px-2.5 text-xs gap-1"
          :disabled="!canPreviousPage"
          data-testid="pagination-prev"
          @click="handlePreviousPage"
        >
          <ChevronLeft class="h-3.5 w-3.5" />
          <span>Trước</span>
        </Button>
        <Button
          variant="outline"
          size="sm"
          class="h-8 px-2.5 text-xs gap-1"
          :disabled="!canNextPage"
          data-testid="pagination-next"
          @click="handleNextPage"
        >
          <span>Sau</span>
          <ChevronRight class="h-3.5 w-3.5" />
        </Button>
        <Button
          variant="outline"
          size="sm"
          class="h-8 w-8 p-0"
          :disabled="!canNextPage"
          title="Trang cuối"
          data-testid="pagination-last"
          @click="handleLastPage"
        >
          <ChevronsRight class="h-3.5 w-3.5" />
        </Button>
      </div>
    </div>
  </div>
</template>
