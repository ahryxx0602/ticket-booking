<script setup>
import { computed, watch, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  },
  size: {
    type: String,
    default: 'default',
    validator: (v) => ['default', 'large'].includes(v)
  },
  submitText: {
    type: String,
    default: 'Lưu thay đổi'
  },
  cancelText: {
    type: String,
    default: 'Hủy'
  },
  submitting: {
    type: Boolean,
    default: false
  },
  hideFooter: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible', 'submit', 'cancel', 'close'])

const widthClass = computed(() => {
  return props.size === 'large' ? 'max-w-[680px]' : 'max-w-[520px]'
})

function close() {
  if (props.submitting) return
  emit('update:visible', false)
  emit('close')
}

function handleCancel() {
  if (props.submitting) return
  emit('cancel')
  close()
}

function handleSubmit() {
  emit('submit')
}

function onKeyDown(e) {
  if (e.key === 'Escape' && props.visible && !props.submitting) {
    close()
  }
}

watch(() => props.visible, (isOpen) => {
  if (isOpen) {
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
})

onMounted(() => {
  window.addEventListener('keydown', onKeyDown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeyDown)
  document.body.style.overflow = ''
})
</script>

<template>
  <teleport to="body">
    <transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="visible"
        class="fixed inset-0 z-50 bg-[#142B4A]/40 backdrop-blur-sm"
        @click="close"
      />
    </transition>

    <transition
      enter-active-class="transition duration-200 ease-out transform"
      enter-from-class="translate-x-full"
      enter-to-class="translate-x-0"
      leave-active-class="transition duration-150 ease-in transform"
      leave-from-class="translate-x-0"
      leave-to-class="translate-x-full"
    >
      <aside
        v-if="visible"
        :class="[
          'fixed inset-y-0 right-0 z-50 w-full bg-surface shadow-dialog flex flex-col border-l border-border-default',
          widthClass
        ]"
        role="dialog"
        aria-modal="true"
        :aria-label="title"
      >
        <!-- Fixed Header -->
        <div class="px-6 py-4.5 border-b border-border-default flex items-center justify-between flex-shrink-0 bg-surface">
          <div>
            <h2 class="text-base font-bold text-text-primary">{{ title }}</h2>
            <p v-if="subtitle" class="text-xs text-text-muted mt-0.5">{{ subtitle }}</p>
          </div>

          <button
            type="button"
            @click="close"
            :disabled="submitting"
            class="w-8 h-8 rounded-lg flex items-center justify-center text-text-muted hover:text-text-primary hover:bg-canvas transition-colors"
            title="Đóng (Escape)"
          >
            ✕
          </button>
        </div>

        <!-- Scrollable Body -->
        <div class="flex-1 overflow-y-auto p-6 space-y-5">
          <slot />
        </div>

        <!-- Fixed Footer -->
        <div
          v-if="!hideFooter"
          class="px-6 py-4 border-t border-border-default flex items-center justify-between flex-shrink-0 bg-canvas"
        >
          <button
            type="button"
            @click="handleCancel"
            :disabled="submitting"
            class="px-4 py-2 text-xs font-semibold rounded-md border border-border-default text-text-secondary hover:bg-surface transition-all"
          >
            {{ cancelText }}
          </button>

          <button
            type="button"
            @click="handleSubmit"
            :disabled="submitting"
            class="px-5 py-2 text-xs font-semibold rounded-md bg-primary-600 hover:bg-primary-700 disabled:bg-slate-200 disabled:text-slate-400 text-white shadow-sm flex items-center gap-2 transition-all"
          >
            <svg v-if="submitting" class="animate-spin w-3.5 h-3.5 text-white" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
            </svg>
            <span>{{ submitText }}</span>
          </button>
        </div>
      </aside>
    </transition>
  </teleport>
</template>
