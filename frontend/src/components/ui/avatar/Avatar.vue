<script setup lang="ts">
import { computed, type HTMLAttributes } from 'vue'
import {
  AvatarRoot,
  type AvatarRootProps,
} from 'reka-ui'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'

const avatarVariants = cva(
  'relative flex shrink-0 overflow-hidden rounded-full',
  {
    variants: {
      size: {
        sm: 'h-8 w-8 text-xs',
        default: 'h-10 w-10 text-sm',
        lg: 'h-12 w-12 text-base',
      },
      shape: {
        circle: 'rounded-full',
        square: 'rounded-lg',
      },
    },
    defaultVariants: {
      size: 'default',
      shape: 'circle',
    },
  }
)

type AvatarVariants = VariantProps<typeof avatarVariants>

const props = withDefaults(
  defineProps<
    AvatarRootProps & {
      class?: HTMLAttributes['class']
      size?: AvatarVariants['size']
      shape?: AvatarVariants['shape']
    }
  >(),
  {
    size: 'default',
    shape: 'circle',
  }
)

const delegatedProps = computed(() => {
  const { class: _, size: _s, shape: _sh, ...delegated } = props
  return delegated
})
</script>

<template>
  <AvatarRoot
    v-bind="delegatedProps"
    :class="cn(avatarVariants({ size, shape }), props.class)"
  >
    <slot />
  </AvatarRoot>
</template>
