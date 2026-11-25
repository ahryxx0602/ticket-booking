<script setup lang="ts">
import { computed, type HTMLAttributes } from 'vue'
import {
  RangeCalendarCell,
  RangeCalendarCellTrigger,
  RangeCalendarGrid,
  RangeCalendarGridBody,
  RangeCalendarGridHead,
  RangeCalendarGridRow,
  RangeCalendarHeadCell,
  RangeCalendarHeader,
  RangeCalendarHeading,
  RangeCalendarNext,
  RangeCalendarPrev,
  RangeCalendarRoot,
  type RangeCalendarRootEmits,
  type RangeCalendarRootProps,
  useForwardPropsEmits,
} from 'reka-ui'
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { cn } from '@/lib/utils'

defineOptions({
  inheritAttrs: false,
})

const props = withDefaults(
  defineProps<RangeCalendarRootProps & { class?: HTMLAttributes['class'] }>(),
  {
    locale: 'vi-VN',
    weekStartsOn: 1,
    weekdayFormat: 'short',
    fixedWeeks: false,
  }
)

const emits = defineEmits<RangeCalendarRootEmits>()

const delegatedProps = computed(() => {
  const { class: _, ...delegated } = props
  return delegated
})

const forwarded = useForwardPropsEmits(delegatedProps, emits)
</script>

<template>
  <RangeCalendarRoot
    v-slot="{ grid, weekDays }"
    :class="cn('p-3 bg-card border border-border rounded-xl shadow-xs', props.class)"
    v-bind="forwarded"
  >
    <RangeCalendarHeader class="flex items-center justify-between pt-1 pb-2">
      <RangeCalendarHeading class="text-sm font-bold text-foreground" />
      <div class="flex items-center space-x-1">
        <RangeCalendarPrev
          class="inline-flex items-center justify-center rounded-lg text-sm font-medium transition-colors border border-border bg-background hover:bg-muted text-foreground h-7 w-7 p-0 cursor-pointer disabled:opacity-40 disabled:pointer-events-none"
        >
          <ChevronLeft class="h-4 w-4" />
        </RangeCalendarPrev>
        <RangeCalendarNext
          class="inline-flex items-center justify-center rounded-lg text-sm font-medium transition-colors border border-border bg-background hover:bg-muted text-foreground h-7 w-7 p-0 cursor-pointer disabled:opacity-40 disabled:pointer-events-none"
        >
          <ChevronRight class="h-4 w-4" />
        </RangeCalendarNext>
      </div>
    </RangeCalendarHeader>

    <div class="flex flex-col gap-y-4 sm:flex-row sm:gap-x-4 sm:gap-y-0">
      <RangeCalendarGrid v-for="month in grid" :key="month.value.toString()" class="w-full border-collapse space-y-1">
        <RangeCalendarGridHead>
          <RangeCalendarGridRow class="mb-1 flex justify-between">
            <RangeCalendarHeadCell
              v-for="day in weekDays"
              :key="day"
              class="text-muted-foreground rounded-md w-8 font-semibold text-[0.8rem] text-center"
            >
              {{ day }}
            </RangeCalendarHeadCell>
          </RangeCalendarGridRow>
        </RangeCalendarGridHead>
        <RangeCalendarGridBody>
          <RangeCalendarGridRow
            v-for="(weekDate, index) in month.rows"
            :key="index"
            class="flex w-full mt-1.5 justify-between"
          >
            <RangeCalendarCell
              v-for="weekDay in weekDate"
              :key="weekDay.toString()"
              :date="weekDay"
              class="relative text-center text-sm focus-within:relative focus-within:z-20 p-0 first:[&:has([data-selected])]:rounded-l-lg last:[&:has([data-selected])]:rounded-r-lg [&:has([data-highlighted])]:bg-primary/10"
            >
              <RangeCalendarCellTrigger
                :day="weekDay"
                :month="month.value"
                :class="cn(
                  'inline-flex items-center justify-center whitespace-nowrap rounded-[6px] text-xs font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-30 hover:bg-muted text-foreground h-8 w-8 p-0 cursor-pointer',
                  // Selection states
                  'data-[selection-start]:bg-primary data-[selection-start]:text-primary-foreground data-[selection-start]:font-bold data-[selection-start]:rounded-l-[6px]',
                  'data-[selection-end]:bg-primary data-[selection-end]:text-primary-foreground data-[selection-end]:font-bold data-[selection-end]:rounded-r-[6px]',
                  'data-[highlighted]:bg-transparent data-[highlighted]:text-foreground',
                  // Today marker
                  '[&[data-today]:not([data-selected])]:border [&[data-today]:not([data-selected])]:border-primary/50',
                  // Outside current month
                  'data-[outside-view]:text-muted-foreground/40',
                  // Disabled state
                  'data-[disabled]:text-muted-foreground/30 data-[disabled]:cursor-not-allowed'
                )"
              />
            </RangeCalendarCell>
          </RangeCalendarGridRow>
        </RangeCalendarGridBody>
      </RangeCalendarGrid>
    </div>
  </RangeCalendarRoot>
</template>
