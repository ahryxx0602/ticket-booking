<script setup lang="ts">
import { AlertDialogTitle, type AlertDialogTitleProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { cn } from '@/lib/utils'

const props = defineProps<AlertDialogTitleProps & { class?: HTMLAttributes['class'] }>()
</script>

<template>
  <AlertDialogTitle v-bind="props" :class="cn('text-lg font-semibold text-foreground', props.class)">
    <slot />
  </AlertDialogTitle>
</template>
