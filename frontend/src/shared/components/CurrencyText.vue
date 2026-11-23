<template>
  <span class="font-semibold tabular-nums">{{ formatted }}</span>
</template>

<script setup>
import { computed } from 'vue'
import { formatVnd } from '@/shared/utils/formatters'

const props = defineProps({
  value: {
    type: [Number, String],
    default: 0,
  },
})

const formatted = computed(() => formatVnd(props.value))
</script>
