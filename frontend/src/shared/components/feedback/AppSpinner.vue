<script setup lang="ts">
import ProgressSpinner from 'primevue/progressspinner'

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
    <ProgressSpinner
      :style="{ width: size, height: size }"
      :strokeWidth="strokeWidth"
      fill="transparent"
      animationDuration=".8s"
      aria-label="Loading"
    />
    <span v-if="message" class="text-sm text-slate-500 font-medium">
      {{ message }}
    </span>
  </div>
</template>
