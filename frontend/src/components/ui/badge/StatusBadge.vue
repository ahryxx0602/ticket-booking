<script lang="ts">
export type DomainStatus =
  // Booking statuses
  | 'PENDING'
  | 'CONFIRMED'
  | 'CANCELLED'
  | 'EXPIRED'
  | 'COMPLETED'
  // Payment statuses
  | 'PAYMENT_PENDING'
  | 'PAYMENT_SUCCESS'
  | 'PAYMENT_FAILED'
  | 'REFUNDED'
  // Trip / Schedule statuses
  | 'SCHEDULED'
  | 'ACTIVE'
  | 'DELAYED'
  | 'IN_TRANSIT'
  | 'ARRIVED'
  // Bus / Maintenance statuses
  | 'MAINTENANCE'
  | 'INACTIVE'
</script>

<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

const props = defineProps<{
  status: DomainStatus | string
  label?: string
  class?: any
}>()

const statusConfig = computed(() => {
  const s = props.status.toUpperCase()
  switch (s) {
    case 'CONFIRMED':
    case 'PAYMENT_SUCCESS':
    case 'COMPLETED':
    case 'ARRIVED':
    case 'ACTIVE':
      return {
        label: props.label || s.replace('_', ' '),
        variant: 'default' as const,
        classes: 'bg-emerald-50 text-emerald-800 border-emerald-300 dark:bg-emerald-950/60 dark:text-emerald-300 dark:border-emerald-800'
      }
    case 'PENDING':
    case 'PAYMENT_PENDING':
    case 'SCHEDULED':
      return {
        label: props.label || s.replace('_', ' '),
        variant: 'outline' as const,
        classes: 'bg-amber-50 text-amber-900 border-amber-300 dark:bg-amber-950/60 dark:text-amber-300 dark:border-amber-800'
      }
    case 'CANCELLED':
    case 'PAYMENT_FAILED':
    case 'EXPIRED':
    case 'INACTIVE':
      return {
        label: props.label || s.replace('_', ' '),
        variant: 'destructive' as const,
        classes: 'bg-red-50 text-red-900 border-red-300 dark:bg-red-950/60 dark:text-red-300 dark:border-red-800'
      }
    case 'DELAYED':
    case 'MAINTENANCE':
      return {
        label: props.label || s.replace('_', ' '),
        variant: 'secondary' as const,
        classes: 'bg-orange-50 text-orange-900 border-orange-300 dark:bg-orange-950/60 dark:text-orange-300 dark:border-orange-800'
      }
    case 'IN_TRANSIT':
    case 'REFUNDED':
    default:
      return {
        label: props.label || s.replace('_', ' '),
        variant: 'secondary' as const,
        classes: 'bg-sky-50 text-sky-900 border-sky-300 dark:bg-sky-950/60 dark:text-sky-300 dark:border-sky-800'
      }
  }
})
</script>

<template>
  <span
    :class="
      cn(
        'inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2',
        statusConfig.classes,
        props.class
      )
    "
  >
    <span class="mr-1.5 h-1.5 w-1.5 rounded-full bg-current opacity-70" />
    {{ statusConfig.label }}
  </span>
</template>
