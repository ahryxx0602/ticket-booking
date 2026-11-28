import { ref, computed } from 'vue'

const STORAGE_KEY = 'velobus_layout_width_mode'

export type LayoutWidthMode = 'narrow' | 'normal' | 'wide'

function getInitialMode(): LayoutWidthMode {
  try {
    if (typeof window !== 'undefined' && window.localStorage && typeof window.localStorage.getItem === 'function') {
      const val = window.localStorage.getItem(STORAGE_KEY) as LayoutWidthMode
      if (val === 'narrow' || val === 'normal' || val === 'wide') return val
    }
  } catch (e) {
    // fallback
  }
  return 'normal'
}

const currentMode = ref<LayoutWidthMode>(getInitialMode())

export function useLayoutWidth() {
  const containerClass = computed<string>(() => {
    switch (currentMode.value) {
      case 'narrow':
        return 'max-w-5xl mx-auto w-full transition-all duration-200'
      case 'wide':
        return 'max-w-none w-full px-2 sm:px-4 lg:px-6 transition-all duration-200'
      case 'normal':
      default:
        return 'max-w-7xl mx-auto w-full transition-all duration-200'
    }
  })

  function setMode(mode: LayoutWidthMode): void {
    if (['narrow', 'normal', 'wide'].includes(mode)) {
      currentMode.value = mode
      try {
        if (typeof window !== 'undefined' && window.localStorage && typeof window.localStorage.setItem === 'function') {
          window.localStorage.setItem(STORAGE_KEY, mode)
        }
      } catch (e) {
        // fallback
      }
    }
  }

  return {
    mode: computed(() => currentMode.value),
    containerClass,
    setMode,
  }
}
