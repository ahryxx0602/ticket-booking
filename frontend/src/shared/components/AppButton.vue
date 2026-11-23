<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: {
    type: String,
    default: 'primary',
    validator: (v) => ['primary', 'secondary', 'destructive', 'danger', 'ghost', 'link'].includes(v)
  },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg'].includes(v)
  },
  loading: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  type: {
    type: String,
    default: 'button'
  },
  fullWidth: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['click'])

const isDisabled = computed(() => props.disabled || props.loading)

const sizeClasses = computed(() => {
  switch (props.size) {
    case 'sm':
      return 'h-8 px-3 text-xs gap-1.5'
    case 'lg':
      return 'h-12 px-5 text-sm gap-2.5 font-semibold'
    case 'md':
    default:
      return 'h-10 px-4 text-xs gap-2 font-medium'
  }
})

const variantClasses = computed(() => {
  if (isDisabled.value) {
    return 'bg-slate-100 text-slate-400 border-border-default cursor-not-allowed opacity-75'
  }

  switch (props.variant) {
    case 'secondary':
      return 'bg-surface text-text-primary border border-border-default hover:bg-canvas hover:border-border-strong shadow-sm'
    case 'destructive':
      return 'bg-amber-500 hover:bg-amber-600 text-white shadow-sm ring-amber-200'
    case 'danger':
      return 'bg-red-600 hover:bg-red-700 text-white shadow-sm ring-red-200'
    case 'ghost':
      return 'bg-transparent text-text-secondary hover:bg-canvas hover:text-text-primary'
    case 'link':
      return 'bg-transparent text-primary-600 hover:underline p-0 h-auto'
    case 'primary':
    default:
      return 'bg-primary-600 hover:bg-primary-700 text-white shadow-sm ring-primary-100'
  }
})

function handleClick(e) {
  if (!isDisabled.value) {
    emit('click', e)
  }
}
</script>

<template>
  <button
    :type="type"
    :disabled="isDisabled"
    @click="handleClick"
    :class="[
      'inline-flex items-center justify-center rounded-md font-sans select-none transition-all outline-none focus-visible:ring-2 focus-visible:ring-offset-2',
      sizeClasses,
      variantClasses,
      fullWidth && 'w-full'
    ]"
  >
    <!-- Loading Spinner -->
    <svg
      v-if="loading"
      class="animate-spin -ml-0.5 h-4 w-4"
      :class="variant === 'secondary' || variant === 'ghost' ? 'text-text-secondary' : 'text-white'"
      fill="none"
      viewBox="0 0 24 24"
    >
      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
    </svg>

    <!-- Prepend Icon Slot -->
    <span v-if="$slots.prepend && !loading" class="inline-flex items-center">
      <slot name="prepend" />
    </span>

    <!-- Content -->
    <span>
      <slot />
    </span>

    <!-- Append Icon Slot -->
    <span v-if="$slots.append && !loading" class="inline-flex items-center">
      <slot name="append" />
    </span>
  </button>
</template>
