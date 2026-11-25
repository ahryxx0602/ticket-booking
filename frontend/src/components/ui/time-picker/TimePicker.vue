<script setup lang="ts">
import { ref, computed } from 'vue'
import { useId } from 'reka-ui'
import { Clock, ChevronDown } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import {
  Popover,
  PopoverTrigger,
  PopoverContent
} from '@/components/ui/popover'
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem
} from '@/components/ui/select'
import { cn } from '@/lib/utils'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    defaultValue?: string
    disabled?: boolean
    invalid?: boolean
    id?: string
    label?: string
    placeholder?: string
    class?: any
    minuteStep?: number
    variant?: 'select' | 'popover'
    presets?: string[]
  }>(),
  {
    modelValue: '08:00',
    placeholder: 'HH:mm',
    disabled: false,
    invalid: false,
    minuteStep: 5,
    variant: 'select',
    presets: () => ['06:00', '08:30', '12:00', '14:30', '18:00', '21:00']
  }
)

const emits = defineEmits<{
  (e: 'update:modelValue', val: string): void
}>()

const instanceId = useId(props.id, 'velobus-timepicker')
const uniqueId = computed(() => props.id || instanceId)
const isPopoverOpen = ref(false)

const rawTime = computed(() => props.modelValue ?? props.defaultValue ?? '08:00')

const currentHours = computed({
  get: () => {
    const [h] = rawTime.value.split(':')
    return (h || '08').padStart(2, '0')
  },
  set: (newHour: string) => {
    const [, m] = rawTime.value.split(':')
    emits('update:modelValue', `${newHour.padStart(2, '0')}:${(m || '00').padStart(2, '0')}`)
  }
})

const currentMinutes = computed({
  get: () => {
    const [, m] = rawTime.value.split(':')
    return (m || '00').padStart(2, '0')
  },
  set: (newMinute: string) => {
    const [h] = rawTime.value.split(':')
    emits('update:modelValue', `${(h || '08').padStart(2, '0')}:${newMinute.padStart(2, '0')}`)
  }
})

// 24 Hours: 00 to 23
const hourOptions = Array.from({ length: 24 }, (_, i) => String(i).padStart(2, '0'))

// Minutes: by step (default 5 min: 00, 05, 10, ..., 55; includes currentMinute if odd)
const minuteOptions = computed(() => {
  const step = Math.max(1, props.minuteStep)
  const list: string[] = []
  for (let i = 0; i < 60; i += step) {
    list.push(String(i).padStart(2, '0'))
  }
  if (!list.includes(currentMinutes.value)) {
    list.push(currentMinutes.value)
    list.sort((a, b) => Number(a) - Number(b))
  }
  return list
})

const quickPresets = computed(() => props.presets && props.presets.length > 0 ? props.presets : ['06:00', '08:30', '12:00', '14:30', '18:00', '21:00'])

function setPreset(preset: string) {
  emits('update:modelValue', preset)
}

function setCurrentTime() {
  const now = new Date()
  const h = String(now.getHours()).padStart(2, '0')
  const m = String(now.getMinutes()).padStart(2, '0')
  emits('update:modelValue', `${h}:${m}`)
}
</script>

<template>
  <div class="space-y-1.5" :class="props.class">
    <!-- Component Label with 24H Badge -->
    <label
      v-if="label"
      :for="uniqueId"
      class="text-xs font-semibold text-foreground flex items-center justify-between"
    >
      <span>{{ label }}</span>
      <span class="text-[11px] font-mono text-muted-foreground font-normal">24-Hour</span>
    </label>

    <!-- Variant: Popover Modal Trigger -->
    <Popover v-if="variant === 'popover'" v-model:open="isPopoverOpen">
      <PopoverTrigger as-child>
        <Button
          variant="outline"
          :disabled="disabled"
          class="w-full justify-between text-left font-normal h-10 border-border bg-background hover:bg-accent cursor-pointer"
        >
          <div class="flex items-center gap-2">
            <Clock class="h-4 w-4 text-primary shrink-0" />
            <span class="text-xs font-semibold font-mono text-foreground">{{ rawTime }}</span>
            <span class="text-[10px] px-1.5 py-0.5 rounded bg-muted text-muted-foreground font-mono">24H</span>
          </div>
          <ChevronDown
            class="h-4 w-4 text-muted-foreground opacity-50 transition-transform duration-200"
            :class="{ 'rotate-180': isPopoverOpen }"
          />
        </Button>
      </PopoverTrigger>

      <PopoverContent class="w-72 p-4 border border-border rounded-xl shadow-lg bg-popover text-popover-foreground space-y-3" align="start">
        <div class="flex items-center justify-between border-b border-border pb-2">
          <span class="text-xs font-bold text-foreground">Chọn giờ xuất bến (24H)</span>
          <span class="text-xs font-mono font-bold text-primary px-2 py-0.5 rounded bg-primary/10">{{ rawTime }}</span>
        </div>

        <div class="flex items-center gap-2">
          <!-- Select Giờ -->
          <div class="flex-1 space-y-1">
            <label class="text-[10px] font-medium text-muted-foreground">Giờ (00 - 23)</label>
            <Select v-model="currentHours">
              <SelectTrigger class="h-9 text-xs font-mono font-semibold bg-background">
                <SelectValue placeholder="Giờ" />
              </SelectTrigger>
              <SelectContent class="max-h-48">
                <SelectItem v-for="h in hourOptions" :key="h" :value="h" class="text-xs font-mono">
                  {{ h }} giờ
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <span class="text-sm font-bold text-muted-foreground pt-4 select-none">:</span>

          <!-- Select Phút -->
          <div class="flex-1 space-y-1">
            <label class="text-[10px] font-medium text-muted-foreground">Phút (00 - 59)</label>
            <Select v-model="currentMinutes">
              <SelectTrigger class="h-9 text-xs font-mono font-semibold bg-background">
                <SelectValue placeholder="Phút" />
              </SelectTrigger>
              <SelectContent class="max-h-48">
                <SelectItem v-for="m in minuteOptions" :key="m" :value="m" class="text-xs font-mono">
                  {{ m }} phút
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>

        <!-- Presets inside popover -->
        <div class="space-y-1 pt-1">
          <span class="text-[10px] text-muted-foreground">Khung giờ phổ biến:</span>
          <div class="grid grid-cols-3 gap-1">
            <button
              v-for="preset in quickPresets"
              :key="preset"
              type="button"
              class="px-1.5 py-1 rounded text-[11px] font-mono border text-center transition-colors cursor-pointer"
              :class="rawTime === preset ? 'bg-primary text-primary-foreground border-primary font-bold' : 'bg-muted/40 text-muted-foreground hover:bg-muted hover:text-foreground border-transparent'"
              @click="setPreset(preset)"
            >
              {{ preset }}
            </button>
          </div>
        </div>

        <div class="flex items-center justify-between pt-2 border-t border-border">
          <Button variant="ghost" size="sm" class="text-xs h-7 px-2" @click="setCurrentTime">
            Hiện tại
          </Button>
          <Button size="sm" class="text-xs h-7 px-3" @click="isPopoverOpen = false">
            Áp dụng
          </Button>
        </div>
      </PopoverContent>
    </Popover>

    <!-- Variant: Direct Custom Select Dropdowns (Default) -->
    <div v-else class="flex items-center gap-2">
      <!-- Dropdown Giờ (Custom Shadcn Select) -->
      <div class="relative flex-1">
        <Select v-model="currentHours" :disabled="disabled">
          <SelectTrigger class="h-10 text-xs font-semibold bg-background border-input">
            <div class="flex items-center gap-1.5">
              <Clock class="h-4 w-4 text-primary shrink-0" />
              <SelectValue placeholder="Giờ" />
              <span class="text-[11px] text-muted-foreground font-normal">giờ</span>
            </div>
          </SelectTrigger>
          <SelectContent class="max-h-56">
            <SelectItem
              v-for="h in hourOptions"
              :key="h"
              :value="h"
              class="font-mono text-xs"
            >
              {{ h }} giờ
            </SelectItem>
          </SelectContent>
        </Select>
      </div>

      <span class="text-sm font-bold text-muted-foreground select-none">:</span>

      <!-- Dropdown Phút (Custom Shadcn Select) -->
      <div class="relative flex-1">
        <Select v-model="currentMinutes" :disabled="disabled">
          <SelectTrigger class="h-10 text-xs font-semibold bg-background border-input">
            <div class="flex items-center gap-1.5">
              <SelectValue placeholder="Phút" />
              <span class="text-[11px] text-muted-foreground font-normal">phút</span>
            </div>
          </SelectTrigger>
          <SelectContent class="max-h-56">
            <SelectItem
              v-for="m in minuteOptions"
              :key="m"
              :value="m"
              class="font-mono text-xs"
            >
              {{ m }} phút
            </SelectItem>
          </SelectContent>
        </Select>
      </div>
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
            rawTime === preset
              ? 'bg-primary text-primary-foreground border-primary font-bold'
              : 'bg-muted/50 text-muted-foreground border-transparent hover:bg-muted hover:text-foreground'
          )
        "
        @click="setPreset(preset)"
      >
        {{ preset }}
      </button>
    </div>
  </div>
</template>
