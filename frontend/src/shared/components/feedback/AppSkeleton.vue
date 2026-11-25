<script setup lang="ts">
withDefaults(
  defineProps<{
    width?: string
    height?: string
    rounded?: string
    count?: number
  }>(),
  {
    width: '100%',
    height: '1rem',
    rounded: 'rounded-md',
    count: 1,
  }
)
</script>

<template>
  <div class="flex flex-col gap-2 w-full" aria-hidden="true">
    <div
      v-for="i in count"
      :key="i"
      :class="['skeleton-pulse bg-slate-200 dark:bg-slate-700', rounded]"
      :style="{ width, height }"
    />
  </div>
</template>

<style scoped>
@keyframes pulse-custom {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.4;
  }
}

.skeleton-pulse {
  animation: pulse-custom 1.5s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

@media (prefers-reduced-motion: reduce) {
  .skeleton-pulse {
    animation: none;
    opacity: 0.6;
    background-color: #e2e8f0;
  }
}
</style>
