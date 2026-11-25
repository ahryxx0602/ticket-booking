<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  type ColumnDef,
  type SortingState,
  type ColumnFiltersState,
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

const props = defineProps<{
  columns: any[]
  data: any[]
  pageSize?: number
}>()

const sorting = ref<SortingState>([])
const columnFilters = ref<ColumnFiltersState>([])

const table = useTable({
  features: Object.values(stockFeatures),
  get data() {
    return props.data
  },
  get columns() {
    return props.columns
  },
  state: {
    get sorting() {
      return sorting.value
    },
    get columnFilters() {
      return columnFilters.value
    }
  },
  initialState: {
    pagination: {
      pageIndex: 0,
      pageSize: props.pageSize || 10
    }
  },
  onSortingChange: (updaterOrValue: any) => {
    sorting.value = typeof updaterOrValue === 'function' ? updaterOrValue(sorting.value) : updaterOrValue
  },
  onColumnFiltersChange: (updaterOrValue: any) => {
    columnFilters.value = typeof updaterOrValue === 'function' ? updaterOrValue(columnFilters.value) : updaterOrValue
  },
  getCoreRowModel: createCoreRowModel(),
  getPaginationRowModel: createPaginatedRowModel(),
  getSortedRowModel: createSortedRowModel(),
  getFilteredRowModel: createFilteredRowModel()
} as any) as any

const currentPageIndex = computed(() => {
  return (table.atoms?.pagination as any)?.value?.pageIndex ?? 0
})

const totalPages = computed(() => {
  if (typeof table.getPageCount === 'function') {
    return Math.max(1, table.getPageCount())
  }
  return 1
})

// Compatibility polyfill for table.getState()
if (!table.getState) {
  table.getState = () => ({
    pagination: (table.atoms?.pagination as any)?.value ?? { pageIndex: 0, pageSize: props.pageSize || 10 },
    sorting: sorting.value,
    columnFilters: columnFilters.value
  })
}
</script>

<template>
  <div class="space-y-4">
    <div class="rounded-md border">
      <Table>
        <TableHeader>
          <TableRow v-for="headerGroup in table.getHeaderGroups()" :key="headerGroup.id">
            <TableHead v-for="header in headerGroup.headers" :key="header.id">
              <FlexRender
                v-if="!header.isPlaceholder"
                :render="header.column.columnDef.header"
                :props="header.getContext()"
              />
            </TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          <template v-if="table.getRowModel().rows?.length">
            <TableRow
              v-for="row in table.getRowModel().rows"
              :key="row.id"
              :data-state="row.getIsSelected?.() ? 'selected' : undefined"
            >
              <TableCell v-for="cell in (row.getVisibleCells ? row.getVisibleCells() : row.getAllCells?.() || [])" :key="cell.id">
                <FlexRender :render="cell.column.columnDef.cell" :props="cell.getContext()" />
              </TableCell>
            </TableRow>
          </template>
          <template v-else>
            <TableRow>
              <TableCell :colspan="columns.length" class="h-24 text-center text-muted-foreground text-xs">
                Không tìm thấy dữ liệu phù hợp.
              </TableCell>
            </TableRow>
          </template>
        </TableBody>
      </Table>
    </div>

    <!-- Pagination Controls -->
    <div class="flex items-center justify-between px-2 text-xs text-muted-foreground">
      <div>
        Trang {{ currentPageIndex + 1 }} / {{ totalPages }} (Tổng {{ data.length }} dòng)
      </div>
      <div class="flex items-center space-x-2">
        <Button
          variant="outline"
          size="sm"
          class="h-7 text-xs"
          :disabled="!table.getCanPreviousPage?.()"
          @click="table.previousPage?.()"
        >
          Trước
        </Button>
        <Button
          variant="outline"
          size="sm"
          class="h-7 text-xs"
          :disabled="!table.getCanNextPage?.()"
          @click="table.nextPage?.()"
        >
          Sau
        </Button>
      </div>
    </div>
  </div>
</template>
