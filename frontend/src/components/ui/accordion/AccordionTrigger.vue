<script setup lang="ts">
import { computed, type HTMLAttributes } from 'vue'
import {
  AccordionHeader,
  AccordionTrigger,
  type AccordionTriggerProps,
} from 'reka-ui'
import { ChevronDown } from 'lucide-vue-next'
import { cn } from '@/lib/utils'

const props = defineProps<
  AccordionTriggerProps & {
    class?: HTMLAttributes['class']
  }
>()

const delegatedProps = computed(() => {
  const { class: _, ...delegated } = props
  return delegated
})
</script>

<template>
  <AccordionHeader class="flex">
    <AccordionTrigger
      v-bind="delegatedProps"
      :class="
        cn(
          'flex flex-1 items-center justify-between py-4 text-xs sm:text-sm font-semibold transition-all hover:underline text-foreground cursor-pointer [&[data-state=open]>svg]:rotate-180',
          props.class
        )
      "
    >
      <slot />
      <ChevronDown
        class="h-4 w-4 shrink-0 transition-transform duration-200 text-muted-foreground"
      />
    </AccordionTrigger>
  </AccordionHeader>
</template>
