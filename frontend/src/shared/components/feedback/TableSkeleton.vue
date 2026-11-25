<script setup lang="ts">
import AppSkeleton from './AppSkeleton.vue'

withDefaults(
  defineProps<{
    rows?: number
    columns?: number
    showHeader?: boolean
  }>(),
  {
    rows: 5,
    columns: 5,
    showHeader: true,
  }
)
</script>

<template>
  <div class="w-full bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 overflow-hidden shadow-xs">
    <!-- Header Skeleton -->
    <div v-if="showHeader" class="grid grid-cols-12 gap-4 px-6 py-3.5 bg-slate-50 dark:bg-slate-800/50 border-b border-slate-200 dark:border-slate-800">
      <div v-for="c in columns" :key="'th-' + c" class="col-span-2">
        <AppSkeleton height="0.875rem" width="70%" />
      </div>
    </div>

    <!-- Rows Skeleton -->
    <div class="divide-y divide-slate-100 dark:divide-slate-800">
      <div
        v-for="r in rows"
        :key="'tr-' + r"
        class="grid grid-cols-12 gap-4 px-6 py-4 items-center"
      >
        <div v-for="c in columns" :key="'td-' + r + '-' + c" class="col-span-2">
          <AppSkeleton height="0.875rem" :width="c === 1 ? '90%' : '60%'" />
        </div>
      </div>
    </div>
  </div>
</template>
