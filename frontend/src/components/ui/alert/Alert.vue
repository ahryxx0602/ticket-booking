<script lang="ts">
import { cva, type VariantProps } from 'class-variance-authority'

export const alertVariants = cva(
  'relative w-full rounded-xl border p-4 text-sm [&>svg~*]:pl-7 [&>svg+div]:translate-y-[-3px] [&>svg]:absolute [&>svg]:left-4 [&>svg]:top-4 [&>svg]:text-foreground',
  {
    variants: {
      variant: {
        default: 'bg-background text-foreground border-border',
        destructive: 'border-status-danger/40 bg-status-danger-bg text-status-danger [&>svg]:text-status-danger',
        warning: 'border-status-warning/40 bg-status-warning-bg text-status-warning [&>svg]:text-status-warning',
        success: 'border-status-success/40 bg-status-success-bg text-status-success [&>svg]:text-status-success',
        info: 'border-status-info/40 bg-status-info-bg text-status-info [&>svg]:text-status-info',
      },
    },
    defaultVariants: {
      variant: 'default',
    },
  }
)

export type AlertVariants = VariantProps<typeof alertVariants>
</script>

<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { cn } from '@/lib/utils'

defineProps<{
  class?: HTMLAttributes['class']
  variant?: AlertVariants['variant']
}>()
</script>

<template>
  <div :class="cn(alertVariants({ variant }), $props.class)" role="alert">
    <slot />
  </div>
</template>
