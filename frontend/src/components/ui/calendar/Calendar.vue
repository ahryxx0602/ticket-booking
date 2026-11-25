<script setup lang="ts">
import { computed, type HTMLAttributes } from 'vue'
import {
  CalendarCell,
  CalendarCellTrigger,
  CalendarGrid,
  CalendarGridBody,
  CalendarGridHead,
  CalendarGridRow,
  CalendarHeadCell,
  CalendarHeader,
  CalendarHeading,
  CalendarNext,
  CalendarPrev,
  CalendarRoot,
  type CalendarRootEmits,
  type CalendarRootProps,
  useForwardPropsEmits,
} from 'reka-ui'
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { cn } from '@/lib/utils'

defineOptions({
  inheritAttrs: false,
})

const props = withDefaults(
  defineProps<CalendarRootProps & { class?: HTMLAttributes['class'] }>(),
  {
    locale: 'vi-VN',
    weekStartsOn: 1,
    weekdayFormat: 'short',
    fixedWeeks: false,
  }
)

const emits = defineEmits<CalendarRootEmits>()

const delegatedProps = computed(() => {
  const { class: _, ...delegated } = props
  return delegated
})

const forwarded = useForwardPropsEmits(delegatedProps, emits)
</script>

<template>
  <CalendarRoot
    v-slot="{ grid, weekDays }"
    :class="cn('p-3 bg-card border border-border rounded-xl shadow-xs', props.class)"
    v-bind="forwarded"
  >
    <CalendarHeader class="flex items-center justify-between pt-1 pb-2">
      <CalendarHeading class="text-sm font-bold text-foreground" />
      <div class="flex items-center space-x-1">
        <CalendarPrev
          class="inline-flex items-center justify-center rounded-lg text-sm font-medium transition-colors border border-border bg-background hover:bg-muted text-foreground h-7 w-7 p-0 cursor-pointer disabled:opacity-40 disabled:pointer-events-none"
        >
          <ChevronLeft class="h-4 w-4" />
        </CalendarPrev>
        <CalendarNext
          class="inline-flex items-center justify-center rounded-lg text-sm font-medium transition-colors border border-border bg-background hover:bg-muted text-foreground h-7 w-7 p-0 cursor-pointer disabled:opacity-40 disabled:pointer-events-none"
        >
          <ChevronRight class="h-4 w-4" />
        </CalendarNext>
      </div>
    </CalendarHeader>

    <div class="flex flex-col gap-y-4 sm:flex-row sm:gap-x-4 sm:gap-y-0">
      <CalendarGrid v-for="month in grid" :key="month.value.toString()" class="w-full border-collapse space-y-1">
        <CalendarGridHead>
          <CalendarGridRow class="mb-1 flex justify-between">
            <CalendarHeadCell
              v-for="day in weekDays"
              :key="day"
              class="text-muted-foreground rounded-md w-8 font-semibold text-[0.8rem] text-center"
            >
              {{ day }}
            </CalendarHeadCell>
          </CalendarGridRow>
        </CalendarGridHead>
        <CalendarGridBody>
          <CalendarGridRow
            v-for="(weekDate, index) in month.rows"
            :key="index"
            class="flex w-full mt-1.5 justify-between"
          >
            <CalendarCell
              v-for="weekDay in weekDate"
              :key="weekDay.toString()"
              :date="weekDay"
              class="relative text-center text-sm focus-within:relative focus-within:z-20 p-0"
            >
              <CalendarCellTrigger
                :day="weekDay"
                :month="month.value"
                :class="cn(
                  'inline-flex items-center justify-center whitespace-nowrap rounded-[6px] text-xs font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-30 hover:bg-muted text-foreground h-8 w-8 p-0 cursor-pointer',
                  // Today marker
                  '[&[data-today]:not([data-selected])]:border [&[data-today]:not([data-selected])]:border-primary/50 [&[data-today]:not([data-selected])]:font-bold',
                  // Selected state with canonical Claude+ primary token
                  'data-[selected]:bg-primary data-[selected]:text-primary-foreground data-[selected]:font-bold data-[selected]:hover:bg-primary/90',
                  // Outside current month
                  'data-[outside-view]:text-muted-foreground/40',
                  // Disabled state
                  'data-[disabled]:text-muted-foreground/30 data-[disabled]:cursor-not-allowed'
                )"
              />
            </CalendarCell>
          </CalendarGridRow>
        </CalendarGridBody>
      </CalendarGrid>
    </div>
  </CalendarRoot>
</template>
