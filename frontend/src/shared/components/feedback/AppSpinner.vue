<script setup lang="ts">
import { Loader2 } from 'lucide-vue-next'

withDefaults(
  defineProps<{
    size?: string
    strokeWidth?: string
    message?: string
    centered?: boolean
  }>(),
  {
    size: '40px',
    strokeWidth: '4',
    message: '',
    centered: false,
  }
)
</script>

<template>
  <div
    :class="[
      'flex items-center gap-3',
      centered ? 'justify-center p-8 w-full' : 'inline-flex'
    ]"
    role="status"
    :aria-label="message || 'Đang tải...'"
  >
    <Loader2
      class="animate-spin text-primary shrink-0"
      :style="{ width: size, height: size }"
      aria-hidden="true"
    />
    <span v-if="message" class="text-sm text-muted-foreground font-medium">
      {{ message }}
    </span>
  </div>
</template>
