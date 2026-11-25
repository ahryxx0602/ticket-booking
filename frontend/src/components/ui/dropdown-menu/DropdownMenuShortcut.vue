<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { cn } from '@/lib/utils'

const props = defineProps<{
  class?: HTMLAttributes['class']
}>()
</script>

<template>
  <span
    :class="
      cn(
        'ml-auto text-[10px] tracking-widest text-muted-foreground font-mono font-semibold',
        props.class
      )
    "
  >
    <slot />
  </span>
</template>
