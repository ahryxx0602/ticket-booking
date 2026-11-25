<script setup lang="ts">
import { Toaster as Sonner, type ToasterProps } from 'vue-sonner'
import 'vue-sonner/style.css'
import { useTheme } from '@/shared/composables/useTheme'

const { resolvedMode } = useTheme()

const props = withDefaults(defineProps<ToasterProps>(), {
  richColors: true,
  position: 'top-right',
  closeButton: true,
})
</script>

<template>
  <Sonner
    class="toaster group"
    v-bind="props"
    :theme="resolvedMode"
    :toast-options="{
      classes: {
        toast:
          'group toast group-[.toaster]:border-border group-[.toaster]:shadow-lg group-[.toaster]:rounded-xl font-sans text-xs',
        description: 'group-[.toast]:text-muted-foreground',
        actionButton:
          'group-[.toast]:bg-primary group-[.toast]:text-primary-foreground font-medium',
        cancelButton:
          'group-[.toast]:bg-muted group-[.toast]:text-muted-foreground font-medium',
      },
    }"
  />
</template>
