<script setup>
defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  searchPlaceholder: {
    type: String,
    default: 'Tìm kiếm nhanh...'
  },
  showSearch: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['update:modelValue', 'search', 'clear'])

function handleInput(e) {
  emit('update:modelValue', e.target.value)
  emit('search', e.target.value)
}

function handleClear() {
  emit('update:modelValue', '' )
  emit('clear')
}
</script>

<template>
  <div class="bg-surface rounded-xl border border-border-default p-4 mb-4 shadow-card flex flex-col lg:flex-row lg:items-center justify-between gap-4">
    <!-- Left: Search input & filter slots -->
    <div class="flex flex-1 flex-col sm:flex-row items-stretch sm:items-center gap-3">
      <!-- Search Input (Height 44px, border-strong, radius 8px) -->
      <div v-if="showSearch" class="relative flex-1 max-w-md">
        <svg class="w-4 h-4 text-text-muted absolute left-3.5 top-1/2 -translate-y-1/2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
        </svg>

        <input
          type="text"
          :value="modelValue"
          @input="handleInput"
          :placeholder="searchPlaceholder"
          class="w-full h-11 pl-10 pr-9 bg-white border border-border-strong rounded-md text-xs text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-primary-600 focus:border-primary-600 transition-all"
        />

        <button
          v-if="modelValue"
          type="button"
          @click="handleClear"
          class="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted hover:text-text-primary text-xs p-1"
          title="Xóa tìm kiếm"
        >
          ✕
        </button>
      </div>

      <!-- Filters Slot (Dropdowns, datepickers, chips) -->
      <div v-if="$slots.filters" class="flex flex-wrap items-center gap-2">
        <slot name="filters" />
      </div>
    </div>

    <!-- Right: Secondary & Primary Actions -->
    <div class="flex items-center gap-2.5 flex-shrink-0 self-end lg:self-auto">
      <slot name="secondary" />
      <slot name="primary" />
    </div>
  </div>
</template>
