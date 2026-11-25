<script setup lang="ts">
import { ref } from 'vue'
import { RangeCalendar } from '@/components/ui/range-calendar'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator
} from '@/components/ui/dropdown-menu'
import { Calendar as CalendarIcon, ChevronDown, Sparkles } from 'lucide-vue-next'
import { today, getLocalTimeZone } from '@internationalized/date'

const todayDate = today(getLocalTimeZone())
const start = todayDate
const end = todayDate.add({ days: 4 })

const dateRange = ref<any>({
  start,
  end
})

const isPopoverOpen = ref(false)

function formatDateRange() {
  if (!dateRange.value.start) return 'Chọn khoảng thời gian'
  const fmt = (d: any) => `${String(d.day).padStart(2, '0')}/${String(d.month).padStart(2, '0')}/${d.year}`
  if (dateRange.value.end) {
    return `${fmt(dateRange.value.start)} — ${fmt(dateRange.value.end)}`
  }
  return `${fmt(dateRange.value.start)} — ...`
}

function selectPreset(preset: 'today' | 'next3days' | 'next7days' | 'thisMonth') {
  const t = today(getLocalTimeZone())
  switch (preset) {
    case 'today':
      dateRange.value = { start: t, end: t }
      break
    case 'next3days':
      dateRange.value = { start: t, end: t.add({ days: 3 }) }
      break
    case 'next7days':
      dateRange.value = { start: t, end: t.add({ days: 7 }) }
      break
    case 'thisMonth':
      dateRange.value = { start: t, end: t.add({ days: 30 }) }
      break
  }
  isPopoverOpen.value = false
}
</script>

<template>
  <div class="space-y-4">
    <div class="space-y-2">
      <div class="flex items-center justify-between">
        <label class="text-xs font-semibold text-foreground">Date Range Picker (Khứ Hồi / Báo Cáo):</label>
        
        <!-- Preset Dropdown Menu -->
        <DropdownMenu>
          <DropdownMenuTrigger as-child>
            <Button variant="ghost" size="sm" class="h-7 text-[11px] gap-1 text-primary hover:text-primary">
              <Sparkles class="h-3.5 w-3.5" />
              <span>Chọn nhanh khoảng ngày</span>
              <ChevronDown class="h-3 w-3 opacity-60" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" class="w-48 text-xs">
            <DropdownMenuLabel>Khoảng thời gian mẫu</DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem @click="selectPreset('today')">Hôm nay (1 ngày)</DropdownMenuItem>
            <DropdownMenuItem @click="selectPreset('next3days')">3 ngày tới (Cuối tuần)</DropdownMenuItem>
            <DropdownMenuItem @click="selectPreset('next7days')">7 ngày tới (1 tuần)</DropdownMenuItem>
            <DropdownMenuItem @click="selectPreset('thisMonth')">30 ngày tới (1 tháng)</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      <!-- Popover Range Picker -->
      <Popover v-model:open="isPopoverOpen">
        <PopoverTrigger as-child>
          <Button
            variant="outline"
            class="w-full justify-start text-left font-normal h-10 border-border bg-background"
          >
            <CalendarIcon class="mr-2 h-4 w-4 text-primary" />
            <span class="text-xs font-medium">{{ formatDateRange() }}</span>
          </Button>
        </PopoverTrigger>
        <PopoverContent class="w-auto p-0" align="start">
          <RangeCalendar
            v-model="dateRange"
            :min-value="(today(getLocalTimeZone()) as any)"
          />
        </PopoverContent>
      </Popover>
    </div>

    <!-- Output summary bar -->
    <div class="p-3 bg-muted/40 rounded-lg border border-border flex items-center justify-between text-xs">
      <span class="text-muted-foreground">Khoảng thời gian đã chọn:</span>
      <span class="font-mono font-bold text-primary">
        {{ dateRange.start ? dateRange.start.toString() : 'None' }} &rarr; {{ dateRange.end ? dateRange.end.toString() : 'None' }}
      </span>
    </div>
  </div>
</template>
