<script setup lang="ts">
import { computed, inject, type HTMLAttributes } from 'vue'
import {
  ToggleGroupItem,
  type ToggleGroupItemProps,
  useForwardProps,
} from 'reka-ui'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'

const toggleGroupItemVariants = cva(
  'inline-flex items-center justify-center rounded-md px-2.5 py-1 text-xs font-semibold ring-offset-background transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 data-[state=on]:bg-background data-[state=on]:text-foreground data-[state=on]:shadow-xs cursor-pointer',
  {
    variants: {
      variant: {
        default: '',
        outline: 'border border-input hover:bg-accent hover:text-accent-foreground',
      },
      size: {
        default: 'h-7',
        sm: 'h-6 text-[10px]',
        lg: 'h-8 text-sm',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  }
)

type ToggleGroupItemVariants = VariantProps<typeof toggleGroupItemVariants>

const context = inject<{ variant?: ToggleGroupItemVariants['variant']; size?: ToggleGroupItemVariants['size'] }>('toggleGroup', {})

const props = defineProps<
  ToggleGroupItemProps & {
    class?: HTMLAttributes['class']
    variant?: ToggleGroupItemVariants['variant']
    size?: ToggleGroupItemVariants['size']
  }
>()

const delegatedProps = computed(() => {
  const { class: _, variant: _v, size: _s, ...delegated } = props
  return delegated
})

const forwarded = useForwardProps(delegatedProps)
</script>

<template>
  <ToggleGroupItem
    v-bind="forwarded"
    :class="
      cn(
        toggleGroupItemVariants({
          variant: context.variant || variant,
          size: context.size || size,
        }),
        props.class
      )
    "
  >
    <slot />
  </ToggleGroupItem>
</template>
