<script setup>
defineProps({
  label: {
    type: String,
    default: ''
  },
  id: {
    type: String,
    default: () => `field-${Math.random().toString(36).substring(2, 9)}`
  },
  required: {
    type: Boolean,
    default: false
  },
  error: {
    type: String,
    default: ''
  },
  helperText: {
    type: String,
    default: ''
  }
})
</script>

<template>
  <div class="space-y-2">
    <!-- Label -->
    <label
      v-if="label"
      :for="id"
      class="block text-sm font-medium text-text-primary"
    >
      {{ label }}
      <span v-if="required" class="text-red-500 font-bold ml-0.5">*</span>
    </label>

    <!-- Field Control Slot -->
    <div>
      <slot :id="id" :has-error="Boolean(error)" />
    </div>

    <!-- Error Message or Helper Text -->
    <p
      v-if="error"
      :id="`${id}-error`"
      class="text-xs text-red-600 flex items-center gap-1 font-medium mt-1"
    >
      <svg class="w-3.5 h-3.5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
        <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/>
      </svg>
      <span>{{ error }}</span>
    </p>
    <p
      v-else-if="helperText"
      :id="`${id}-helper`"
      class="text-xs text-text-muted mt-1"
    >
      {{ helperText }}
    </p>
  </div>
</template>
