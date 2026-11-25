<script setup lang="ts">
import { ref } from 'vue'

const isVisible = ref(false)
const progress = ref(0)
const status = ref<'running' | 'error' | 'success'>('running')
let timer: ReturnType<typeof setInterval> | null = null

function start(): void {
  status.value = 'running'
  progress.value = 15
  isVisible.value = true

  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    if (progress.value < 85) {
      progress.value += Math.random() * 15
    }
  }, 200)
}

function finish(): void {
  if (timer) clearInterval(timer)
  progress.value = 100
  status.value = 'success'
  setTimeout(() => {
    isVisible.value = false
    progress.value = 0
  }, 300)
}

function fail(): void {
  if (timer) clearInterval(timer)
  status.value = 'error'
  progress.value = 100
  setTimeout(() => {
    isVisible.value = false
    progress.value = 0
  }, 400)
}

defineExpose({ start, finish, fail })
</script>

<template>
  <Transition name="fade">
    <div
      v-if="isVisible"
      role="progressbar"
      :aria-valuenow="progress"
      aria-valuemin="0"
      aria-valuemax="100"
      class="fixed top-0 left-0 right-0 z-[9999] h-1 bg-transparent overflow-hidden pointer-events-none"
    >
      <div
        class="h-full transition-all duration-200 ease-out"
        :class="[
          status === 'error'
            ? 'bg-red-500 shadow-[0_0_8px_rgba(239,68,68,0.8)]'
            : 'bg-primary-600 shadow-[0_0_8px_rgba(37,99,235,0.8)]'
        ]"
        :style="{ width: `${Math.min(progress, 100)}%` }"
      />
    </div>
  </Transition>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
