import { ref, computed } from 'vue'

const STORAGE_KEY = 'velobus_layout_width_mode'

function getInitialMode() {
  try {
    if (typeof window !== 'undefined' && window.localStorage && typeof window.localStorage.getItem === 'function') {
      return window.localStorage.getItem(STORAGE_KEY) || 'normal'
    }
  } catch (e) {
    // fallback
  }
  return 'normal'
}

const currentMode = ref(getInitialMode())

export function useLayoutWidth() {
  const containerClass = computed(() => {
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

  function setMode(mode) {
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
