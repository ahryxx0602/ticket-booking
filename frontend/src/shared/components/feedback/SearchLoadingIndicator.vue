<script setup lang="ts">
defineProps<{
  active?: boolean
}>()
</script>

<template>
  <div
    v-if="active"
    class="w-full h-1 overflow-hidden bg-slate-100 rounded-full relative my-1"
    role="status"
    aria-label="Đang tìm kiếm chuyến xe..."
  >
    <div class="shimmer-bar absolute inset-0 bg-gradient-to-r from-transparent via-primary-500 to-transparent w-1/3" />
  </div>
</template>

<style scoped>
@keyframes shimmer {
  0% {
    transform: translateX(-150%);
  }
  100% {
    transform: translateX(350%);
  }
}

.shimmer-bar {
  animation: shimmer 1.2s infinite ease-in-out;
}

@media (prefers-reduced-motion: reduce) {
  .shimmer-bar {
    animation: none;
    transform: none;
    width: 100%;
    background: #cbd5e1;
  }
}
</style>
