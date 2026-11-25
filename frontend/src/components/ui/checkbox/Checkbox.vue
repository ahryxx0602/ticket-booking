<script setup lang="ts">
import { computed } from 'vue'
import { CheckboxRoot, CheckboxIndicator, type CheckboxRootProps } from 'reka-ui'
import { Check } from 'lucide-vue-next'
import { cn } from '@/lib/utils'

const props = defineProps<CheckboxRootProps & { class?: any; checked?: boolean }>()
const emits = defineEmits<{
  (e: 'update:modelValue', val: boolean | 'indeterminate'): void
  (e: 'update:checked', val: boolean | 'indeterminate'): void
}>()

const resolvedValue = computed(() => {
  if (props.modelValue !== undefined) return props.modelValue
  if (props.checked !== undefined) return props.checked
  return undefined
})

function handleUpdate(val: boolean | 'indeterminate') {
  emits('update:modelValue', val)
  emits('update:checked', val)
}
</script>

<template>
  <CheckboxRoot
    :model-value="resolvedValue"
    :disabled="props.disabled"
    :id="props.id"
    :name="props.name"
    :value="props.value"
    @update:model-value="handleUpdate"
    :class="
      cn(
        'peer h-4 w-4 shrink-0 rounded-[4px] border border-primary ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 data-[state=checked]:bg-primary data-[state=checked]:text-primary-foreground transition-all duration-150',
        props.class
      )
    "
  >
    <CheckboxIndicator class="flex h-full w-full items-center justify-center text-current">
      <Check class="h-3 w-3 stroke-[3]" />
    </CheckboxIndicator>
  </CheckboxRoot>
</template>
