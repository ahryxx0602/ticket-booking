<script setup lang="ts">
import { computed, type HTMLAttributes } from 'vue'
import {
  AccordionRoot,
  type AccordionRootEmits,
  type AccordionRootProps,
  useForwardPropsEmits,
} from 'reka-ui'
import { cn } from '@/lib/utils'

const props = defineProps<
  AccordionRootProps & {
    class?: HTMLAttributes['class']
  }
>()

const emits = defineEmits<AccordionRootEmits>()

const delegatedProps = computed(() => {
  const { class: _, ...delegated } = props
  return delegated
})

const forwarded = useForwardPropsEmits(delegatedProps, emits)
</script>

<template>
  <AccordionRoot
    v-bind="forwarded"
    :class="cn('w-full', props.class)"
  >
    <slot />
  </AccordionRoot>
</template>
