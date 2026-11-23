<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  },
  breadcrumbs: {
    type: Array,
    default: () => []
  }
})
</script>

<template>
  <div class="mb-6 space-y-2">
    <!-- Breadcrumbs Navigation -->
    <nav v-if="breadcrumbs && breadcrumbs.length > 0" class="flex items-center gap-1.5 text-xs text-text-muted">
      <template v-for="(crumb, idx) in breadcrumbs" :key="crumb.label">
        <router-link
          v-if="crumb.to"
          :to="crumb.to"
          class="hover:text-primary-600 transition-colors"
        >
          {{ crumb.label }}
        </router-link>
        <span v-else class="text-text-secondary font-medium">{{ crumb.label }}</span>
        <span v-if="idx < breadcrumbs.length - 1" class="text-slate-400">/</span>
      </template>
    </nav>

    <!-- Main Title & Action Bar -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <div class="flex items-center gap-3">
          <h1 class="text-xl sm:text-2xl font-bold text-text-primary tracking-tight">
            {{ title }}
          </h1>
          <slot name="badge" />
        </div>
        <p v-if="subtitle" class="text-xs text-text-muted mt-1 leading-relaxed">
          {{ subtitle }}
        </p>
      </div>

      <!-- Action Buttons Slot -->
      <div v-if="$slots.actions" class="flex items-center gap-3 flex-shrink-0">
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>
