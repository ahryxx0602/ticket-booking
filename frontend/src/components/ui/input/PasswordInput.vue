<script setup lang="ts">
import { ref, type HTMLAttributes } from 'vue'
import { cn } from '@/lib/utils'
import { Eye, EyeOff } from 'lucide-vue-next'

defineProps<{
  modelValue?: string
  defaultValue?: string
  class?: HTMLAttributes['class']
  placeholder?: string
  disabled?: boolean
  required?: boolean
  id?: string
}>()

const emits = defineEmits<{
  (e: 'update:modelValue', payload: string): void
}>()

const showPassword = ref(false)
</script>

<template>
  <div class="relative w-full">
    <input
      :id="id"
      :type="showPassword ? 'text' : 'password'"
      :value="modelValue ?? defaultValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :required="required"
      :class="
        cn(
          'flex h-10 w-full rounded-lg border border-input bg-background pl-3 pr-10 py-2 text-sm text-foreground shadow-xs ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 transition-colors',
          $props.class
        )
      "
      @input="emits('update:modelValue', ($event.target as HTMLInputElement).value)"
    />
    <button
      type="button"
      @click="showPassword = !showPassword"
      class="absolute right-2.5 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground p-1 transition-colors rounded-md focus:outline-none"
      :title="showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
      tabindex="-1"
    >
      <EyeOff v-if="showPassword" class="w-4 h-4" />
      <Eye v-else class="w-4 h-4" />
    </button>
  </div>
</template>
