<script setup lang="ts">
import { AlertCircle, RotateCcw } from 'lucide-vue-next'

withDefaults(
  defineProps<{
    title?: string
    message?: string
    showRetry?: boolean
    retryText?: string
    isRetrying?: boolean
  }>(),
  {
    title: 'Không thể tải dữ liệu',
    message: 'Đã xảy ra sự cố khi kết nối tới máy chủ. Vui lòng kiểm tra lại kết nối mạng hoặc thử lại.',
    showRetry: true,
    retryText: 'Thử lại ngay',
    isRetrying: false,
  }
)

defineEmits<{
  (e: 'retry'): void
}>()
</script>

<template>
  <div
    class="bg-white dark:bg-slate-900 rounded-xl border border-red-200 dark:border-red-900/50 p-8 sm:p-12 text-center flex flex-col items-center justify-center space-y-3.5 shadow-xs"
    role="alert"
  >
    <div class="w-12 h-12 rounded-full bg-red-50 dark:bg-red-950/50 flex items-center justify-center text-red-600 dark:text-red-400 mb-1 ring-8 ring-red-50/50 dark:ring-red-950/20">
      <AlertCircle class="w-6 h-6 stroke-[2]" />
    </div>

    <h3 class="text-sm font-semibold text-slate-900 dark:text-slate-100">
      {{ title }}
    </h3>

    <p class="text-xs text-slate-500 dark:text-slate-400 max-w-md mx-auto leading-relaxed">
      {{ message }}
    </p>

    <div v-if="showRetry" class="pt-2">
      <button
        type="button"
        :disabled="isRetrying"
        @click="$emit('retry')"
        class="inline-flex items-center gap-2 px-4 py-2 text-xs font-semibold rounded-lg bg-red-600 hover:bg-red-700 text-white shadow-xs focus:outline-hidden focus:ring-2 focus:ring-red-500 focus:ring-offset-2 transition-all disabled:opacity-60 disabled:cursor-not-allowed cursor-pointer"
      >
        <RotateCcw class="w-3.5 h-3.5" :class="{ 'animate-spin': isRetrying }" />
        <span>{{ isRetrying ? 'Đang thử lại...' : retryText }}</span>
      </button>
    </div>
  </div>
</template>
