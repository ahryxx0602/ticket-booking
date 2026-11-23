<template>
  <span
    class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold border"
    :class="badgeStyle"
  >
    <span class="w-1.5 h-1.5 rounded-full" :class="dotStyle"></span>
    {{ label || status }}
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: {
    type: String,
    required: true,
  },
  label: {
    type: String,
    default: '',
  },
})

const COLOR_MAP = {
  ACTIVE: { badge: 'bg-emerald-50 text-emerald-700 border-emerald-200', dot: 'bg-emerald-500' },
  CONFIRMED: { badge: 'bg-emerald-50 text-emerald-700 border-emerald-200', dot: 'bg-emerald-500' },
  APPROVED: { badge: 'bg-emerald-50 text-emerald-700 border-emerald-200', dot: 'bg-emerald-500' },
  SUCCESS: { badge: 'bg-emerald-50 text-emerald-700 border-emerald-200', dot: 'bg-emerald-500' },
  SETTLED: { badge: 'bg-emerald-50 text-emerald-700 border-emerald-200', dot: 'bg-emerald-500' },
  SCHEDULED: { badge: 'bg-blue-50 text-blue-700 border-blue-200', dot: 'bg-blue-500' },
  HELD: { badge: 'bg-amber-50 text-amber-700 border-amber-200', dot: 'bg-amber-500' },
  PENDING: { badge: 'bg-amber-50 text-amber-700 border-amber-200', dot: 'bg-amber-500' },
  PENDING_APPROVAL: { badge: 'bg-amber-50 text-amber-700 border-amber-200', dot: 'bg-amber-500' },
  UNSETTLED: { badge: 'bg-amber-50 text-amber-700 border-amber-200', dot: 'bg-amber-500' },
  CANCELLED: { badge: 'bg-rose-50 text-rose-700 border-rose-200', dot: 'bg-rose-500' },
  REJECTED: { badge: 'bg-rose-50 text-rose-700 border-rose-200', dot: 'bg-rose-500' },
  SUSPENDED: { badge: 'bg-rose-50 text-rose-700 border-rose-200', dot: 'bg-rose-500' },
  LOCKED: { badge: 'bg-rose-50 text-rose-700 border-rose-200', dot: 'bg-rose-500' },
  EXPIRED: { badge: 'bg-slate-100 text-slate-600 border-slate-200', dot: 'bg-slate-400' },
  REFUNDED: { badge: 'bg-purple-50 text-purple-700 border-purple-200', dot: 'bg-purple-500' },
}

const badgeStyle = computed(() => COLOR_MAP[props.status]?.badge || 'bg-slate-100 text-slate-700 border-slate-200')
const dotStyle = computed(() => COLOR_MAP[props.status]?.dot || 'bg-slate-400')
</script>
