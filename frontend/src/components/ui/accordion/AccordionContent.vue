<script setup lang="ts">
import { computed, type HTMLAttributes } from 'vue'
import {
  AccordionContent,
  type AccordionContentProps,
} from 'reka-ui'
import { cn } from '@/lib/utils'

const props = defineProps<
  AccordionContentProps & {
    class?: HTMLAttributes['class']
  }
>()

const delegatedProps = computed(() => {
  const { class: _, ...delegated } = props
  return delegated
})
</script>

<template>
  <AccordionContent
    v-bind="delegatedProps"
    class="overflow-hidden text-xs sm:text-sm transition-all data-[state=closed]:animate-accordion-up data-[state=open]:animate-accordion-down"
  >
    <div :class="cn('pb-4 pt-0 text-muted-foreground leading-relaxed', props.class)">
      <slot />
    </div>
  </AccordionContent>
</template>
