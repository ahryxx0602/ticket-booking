<script setup lang="ts">
import { computed, type HTMLAttributes } from 'vue'
import {
  SliderRoot,
  SliderTrack,
  SliderRange,
  SliderThumb,
  type SliderRootEmits,
  type SliderRootProps,
  useForwardPropsEmits
} from 'reka-ui'
import { cn } from '@/lib/utils'

const props = withDefaults(
  defineProps<SliderRootProps & { class?: HTMLAttributes['class'] }>(),
  {
    modelValue: () => [0]
  }
)
const emits = defineEmits<SliderRootEmits>()

const delegatedProps = computed(() => {
  const { class: _, ...delegated } = props
  return delegated
})

const forwarded = useForwardPropsEmits(delegatedProps, emits)
</script>

<template>
  <SliderRoot
    :class="
      cn(
        'relative flex w-full touch-none select-none items-center',
        props.class
      )
    "
    v-bind="forwarded"
  >
    <SliderTrack class="relative h-2 w-full grow overflow-hidden rounded-full bg-secondary">
      <SliderRange class="absolute h-full bg-primary" />
    </SliderTrack>
    <SliderThumb
      v-for="(_, key) in (modelValue || [0])"
      :key="key"
      class="block h-5 w-5 rounded-full border-2 border-primary bg-background ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 cursor-pointer shadow-sm hover:scale-105"
    />
  </SliderRoot>
</template>
