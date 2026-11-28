<script setup lang="ts">
import { computed, type HTMLAttributes } from 'vue'
import {
  DropdownMenuLabel,
  type DropdownMenuLabelProps,
  useForwardProps,
} from 'reka-ui'
import { cn } from '@/lib/utils'

const props = defineProps<
  DropdownMenuLabelProps & {
    class?: HTMLAttributes['class']
    inset?: boolean
  }
>()

const delegatedProps = computed(() => {
  const { class: _, inset: _i, ...delegated } = props
  return delegated
})

const forwardedProps = useForwardProps(delegatedProps)
</script>

<template>
  <DropdownMenuLabel
    v-bind="forwardedProps"
    :class="
      cn(
        'px-2 py-1.5 text-xs font-bold text-foreground',
        inset && 'pl-8',
        props.class
      )
    "
  >
    <slot />
  </DropdownMenuLabel>
</template>
