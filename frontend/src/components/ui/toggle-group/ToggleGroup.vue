<script setup lang="ts">
import { computed, type HTMLAttributes, provide } from 'vue'
import {
  ToggleGroupRoot,
  type ToggleGroupRootEmits,
  type ToggleGroupRootProps,
  useForwardPropsEmits,
} from 'reka-ui'
import { toggleGroupVariants, type ToggleGroupVariants } from './toggleGroupVariants'
import { cn } from '@/lib/utils'

const props = withDefaults(
  defineProps<
    ToggleGroupRootProps & {
      class?: HTMLAttributes['class']
      variant?: ToggleGroupVariants['variant']
      size?: ToggleGroupVariants['size']
    }
  >(),
  {
    variant: 'default',
    size: 'default',
  }
)

const emits = defineEmits<ToggleGroupRootEmits>()

provide('toggleGroup', {
  variant: props.variant,
  size: props.size,
})

const delegatedProps = computed(() => {
  const { class: _, variant: _v, size: _s, ...delegated } = props
  return delegated
})

const forwarded = useForwardPropsEmits(delegatedProps, emits)
</script>

<template>
  <ToggleGroupRoot
    v-bind="forwarded"
    :class="cn(toggleGroupVariants({ variant, size }), props.class)"
  >
    <slot />
  </ToggleGroupRoot>
</template>
