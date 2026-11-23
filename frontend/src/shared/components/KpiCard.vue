<script setup>
defineProps({
  label: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number],
    required: true
  },
  unit: {
    type: String,
    default: ''
  },
  subtitle: {
    type: String,
    default: ''
  },
  trend: {
    type: String,
    default: ''
  },
  trendPositive: {
    type: Boolean,
    default: true
  }
})
</script>

<template>
  <div class="bg-surface rounded-xl border border-border-default p-5 shadow-card flex flex-col justify-between">
    <div class="flex items-center justify-between text-xs">
      <span class="font-medium text-text-secondary uppercase tracking-wider text-[11px]">{{ label }}</span>
      <div v-if="$slots.icon" class="text-text-muted">
        <slot name="icon" />
      </div>
    </div>

    <div class="my-3">
      <div class="text-2xl sm:text-3xl font-bold text-text-primary tabular-nums tracking-tight flex items-baseline gap-1">
        <span>{{ value }}</span>
        <span v-if="unit" class="text-sm font-semibold text-text-secondary">{{ unit }}</span>
      </div>
    </div>

    <div class="flex items-center justify-between text-xs pt-2 border-t border-border-default text-text-muted">
      <span>{{ subtitle }}</span>
      <span
        v-if="trend"
        class="font-semibold tabular-nums"
        :class="trendPositive ? 'text-emerald-600' : 'text-red-600'"
      >
        {{ trend }}
      </span>
    </div>
  </div>
</template>
