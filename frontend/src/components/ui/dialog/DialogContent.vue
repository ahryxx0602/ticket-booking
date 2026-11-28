<script setup lang="ts">
import {
  DialogClose,
  DialogContent,
  type DialogContentEmits,
  type DialogContentProps,
  DialogOverlay,
  DialogPortal,
  useForwardPropsEmits,
} from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { cn } from '@/lib/utils'
import { X } from 'lucide-vue-next'

const props = withDefaults(
  defineProps<DialogContentProps & { class?: HTMLAttributes['class']; portal?: boolean }>(),
  { portal: undefined }
)
const emits = defineEmits<DialogContentEmits>()

const forwarded = useForwardPropsEmits(props, emits)

const isTest = Boolean(import.meta.env?.MODE === 'test' || (globalThis as Record<string, any>)?.__VITEST__)
const isPortalDisabled = props.portal === false || (props.portal === undefined && isTest)
</script>

<template>
  <DialogPortal :disabled="isPortalDisabled">
    <DialogOverlay
      class="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0"
    />
    <DialogContent
      v-bind="forwarded"
      :class="
        cn(
          'fixed left-[50%] top-[50%] z-50 grid w-full max-w-lg translate-x-[-50%] translate-y-[-50%] gap-4 border border-border bg-card p-6 shadow-lg duration-200 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[state=closed]:slide-out-to-left-1/2 data-[state=closed]:slide-out-to-top-[48%] data-[state=open]:slide-in-from-left-1/2 data-[state=open]:slide-in-from-top-[48%] rounded-xl',
          props.class
        )
      "
    >
      <slot />

      <DialogClose
        class="absolute right-4 top-4 rounded-md p-1 opacity-70 ring-offset-background transition-opacity hover:opacity-100 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:pointer-events-none"
      >
        <X class="w-4 h-4 text-muted-foreground" />
        <span class="sr-only">Đóng</span>
      </DialogClose>
    </DialogContent>
  </DialogPortal>
</template>
