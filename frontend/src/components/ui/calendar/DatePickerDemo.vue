<script setup lang="ts">
import { ref } from 'vue'
import { Calendar } from '@/components/ui/calendar'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { Button } from '@/components/ui/button'
import { TimePicker } from '@/components/ui/time-picker'
import { Calendar as CalendarIcon } from 'lucide-vue-next'
import { today, getLocalTimeZone } from '@internationalized/date'

// Use any to prevent TypeScript private field conflicts with transitive @internationalized/date
const selectedDate = ref<any>(today(getLocalTimeZone()))
const isPopoverOpen = ref(false)
const selectedTime = ref('08:30')

function formatDate(date: any) {
  if (!date) return 'Chọn ngày khởi hành'
  const day = String(date.day).padStart(2, '0')
  const month = String(date.month).padStart(2, '0')
  const year = date.year
  return `${day}/${month}/${year}`
}
</script>

<template>
  <div class="space-y-4">
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4 items-start">
      <!-- Popover Date Picker -->
      <div class="space-y-1.5">
        <label class="text-xs font-semibold text-foreground flex items-center justify-between">
          <span>Date Picker (Popover Mode):</span>
          <span class="text-[11px] font-mono text-muted-foreground font-normal">dd/MM/yyyy</span>
        </label>
        <Popover v-model:open="isPopoverOpen">
          <PopoverTrigger as-child>
            <Button
              variant="outline"
              class="w-full justify-start text-left font-normal h-10 border-border bg-background cursor-pointer"
            >
              <CalendarIcon class="mr-2 h-4 w-4 text-primary" />
              <span class="text-xs font-medium">{{ formatDate(selectedDate) }}</span>
            </Button>
          </PopoverTrigger>
          <PopoverContent class="w-auto p-0 border border-border rounded-xl shadow-lg" align="start">
            <Calendar
              v-model="selectedDate"
              :min-value="(today(getLocalTimeZone()) as any)"
              @update:model-value="isPopoverOpen = false"
            />
          </PopoverContent>
        </Popover>
      </div>

      <!-- Time Picker Reusable Component (Direct Custom Select Dropdowns) -->
      <TimePicker
        v-model="selectedTime"
        label="Time Picker (24H Format):"
      />
    </div>

    <!-- Selected Summary & Popover Variant -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4 items-center pt-3 border-t border-border/60">
      <div class="p-3 bg-muted/40 rounded-lg border border-border flex items-center justify-between text-xs">
        <span class="text-muted-foreground">Giá trị đã chọn (ISO format):</span>
        <span class="font-mono font-bold text-primary">
          {{ selectedDate ? `${selectedDate.toString()}T${selectedTime}:00` : 'Chưa chọn' }}
        </span>
      </div>

      <!-- Popover Mode Variant -->
      <TimePicker
        v-model="selectedTime"
        variant="popover"
        label="Time Picker (Popover Variant):"
      />
    </div>
  </div>
</template>
