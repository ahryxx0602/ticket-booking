<script setup lang="ts">
import { computed } from 'vue'
import { Clock } from 'lucide-vue-next'
import { cn } from '@/lib/utils'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    defaultValue?: string
    disabled?: boolean
    id?: string
    label?: string
    placeholder?: string
    class?: any
  }>(),
  {
    modelValue: '08:00',
    placeholder: 'HH:mm',
    disabled: false
  }
)

const emits = defineEmits<{
  (e: 'update:modelValue', val: string): void
}>()

const timeValue = computed({
  get: () => props.modelValue ?? props.defaultValue ?? '08:00',
  set: (val: string) => emits('update:modelValue', val)
})

const quickPresets = ['06:00', '08:30', '12:00', '14:30', '18:00', '21:00']
</script>

<template>
  <div class="space-y-1.5" :class="props.class">
    <label
      v-if="label"
      :for="id || 'time-picker-input'"
      class="text-xs font-semibold text-foreground flex items-center justify-between"
    >
      <span>{{ label }}</span>
      <span class="text-[11px] font-mono text-muted-foreground font-normal">24-Hour</span>
    </label>

    <div class="relative flex items-center">
      <!-- Single unambiguous Lucide Clock affordance -->
      <Clock class="absolute left-3 h-4 w-4 text-primary pointer-events-none" />
      <input
        :id="id || 'time-picker-input'"
        v-model="timeValue"
        type="time"
        step="60"
        :disabled="disabled"
        :placeholder="placeholder"
        class="w-full h-10 pl-9 pr-3 rounded-md border border-input text-xs font-semibold text-foreground bg-background transition-colors focus:ring-2 focus:ring-ring focus:outline-hidden disabled:cursor-not-allowed disabled:opacity-50 [color-scheme:light] dark:[color-scheme:dark] [&::-webkit-calendar-picker-indicator]:cursor-pointer [&::-webkit-calendar-picker-indicator]:opacity-40 hover:[&::-webkit-calendar-picker-indicator]:opacity-80"
      />
    </div>

    <!-- Quick Presets -->
    <div class="flex items-center gap-1.5 pt-1 overflow-x-auto">
      <span class="text-[10px] text-muted-foreground shrink-0">Nhanh:</span>
      <button
        v-for="preset in quickPresets"
        :key="preset"
        type="button"
        :disabled="disabled"
        :class="
          cn(
            'px-1.5 py-0.5 rounded text-[10px] font-mono font-medium transition-colors cursor-pointer border',
            timeValue === preset
              ? 'bg-primary text-primary-foreground border-primary font-bold'
              : 'bg-muted/50 text-muted-foreground border-transparent hover:bg-muted hover:text-foreground'
          )
        "
        @click="timeValue = preset"
      >
        {{ preset }}
      </button>
    </div>
  </div>
</template>
