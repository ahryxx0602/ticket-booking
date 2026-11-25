<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { cn } from '@/lib/utils'
import { buttonVariants, type ButtonVariants } from './buttonVariants'
import { Loader2 } from 'lucide-vue-next'

interface Props {
  variant?: ButtonVariants['variant']
  size?: ButtonVariants['size']
  class?: HTMLAttributes['class']
  disabled?: boolean
  loading?: boolean
  as?: string
}

const props = withDefaults(defineProps<Props>(), {
  as: 'button',
  variant: 'default',
  size: 'default',
  disabled: false,
  loading: false,
})
</script>

<template>
  <component
    :is="as"
    :class="cn(buttonVariants({ variant, size }), props.class)"
    :disabled="disabled || loading"
    :aria-disabled="disabled || loading"
  >
    <Loader2 v-if="loading" class="w-4 h-4 animate-spin text-current" />
    <slot />
  </component>
</template>
