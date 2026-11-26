<script setup lang="ts">
import { ref, type InputHTMLAttributes } from 'vue'
import { cn } from '@/lib/utils'
import { Eye, EyeOff } from 'lucide-vue-next'

defineOptions({
  inheritAttrs: false
})

const props = defineProps<{
  modelValue?: string | number
  defaultValue?: string | number
  class?: any
  placeholder?: string
  disabled?: boolean
  required?: boolean
  id?: string
  name?: string
  autocomplete?: string
  ariaInvalid?: boolean | 'true' | 'false'
  ariaDescribedby?: string
}>()

const emits = defineEmits<{
  (e: 'update:modelValue', payload: string): void
}>()

const showPassword = ref(false)
const inputRef = ref<HTMLInputElement | null>(null)

defineExpose({
  focus: () => inputRef.value?.focus()
})
</script>

<template>
  <div class="relative w-full">
    <input
      ref="inputRef"
      :id="id"
      :name="name"
      :type="showPassword ? 'text' : 'password'"
      :value="modelValue ?? defaultValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :required="required"
      :autocomplete="autocomplete"
      :aria-invalid="ariaInvalid"
      :aria-describedby="ariaDescribedby"
      v-bind="$attrs"
      :class="
        cn(
          'flex h-10 w-full rounded-md border border-input bg-background pl-3 pr-10 py-2 text-sm text-foreground shadow-xs ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 transition-colors',
          $props.class
        )
      "
      @input="emits('update:modelValue', ($event.target as HTMLInputElement).value)"
    />
    <button
      type="button"
      :disabled="disabled"
      @click="showPassword = !showPassword"
      class="absolute right-1.5 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground h-7 w-7 flex items-center justify-center transition-colors rounded-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50"
      :aria-label="showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
      :title="showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
    >
      <EyeOff v-if="showPassword" class="w-4 h-4" />
      <Eye v-else class="w-4 h-4" />
    </button>
  </div>
</template>
