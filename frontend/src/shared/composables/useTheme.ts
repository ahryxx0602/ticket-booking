import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'

export type ThemeMode = 'light' | 'dark' | 'system'
export type DensityMode = 'compact' | 'default' | 'comfortable'
export type RadiusPreset = '0.375rem' | '0.5rem' | '0.75rem' | '1rem'

export interface ThemeConfig {
  mode: ThemeMode
  primaryColor: string
  radius: RadiusPreset
  density: DensityMode
}

export const THEME_COLOR_PRESETS = [
  { name: 'Claude+ Terracotta', value: '#C96442', oklch: '0.6171 0.1375 39.0427' },
  { name: 'Ocean Blue', value: '#2563EB', oklch: '0.546 0.245 262.881' },
  { name: 'Forest Green', value: '#16A34A', oklch: '0.627 0.194 149.214' },
  { name: 'Royal Violet', value: '#7C3AED', oklch: '0.541 0.281 293.009' },
  { name: 'Sunset Orange', value: '#EA580C', oklch: '0.646 0.222 41.116' }
]

export const STORAGE_KEY = 'velobus_theme_config_v2'

export const DEFAULT_THEME: ThemeConfig = {
  mode: 'system',
  primaryColor: '#C96442',
  radius: '1rem',
  density: 'default'
}

/**
 * WCAG 2.1 AA Relative Luminance & Contrast Calculation Utilities
 */
export function hexToRgb(hex: string): [number, number, number] | null {
  if (!hex || typeof hex !== 'string') return null
  const clean = hex.replace('#', '').trim()
  if (clean.length === 3) {
    const r = parseInt(clean[0] + clean[0], 16)
    const g = parseInt(clean[1] + clean[1], 16)
    const b = parseInt(clean[2] + clean[2], 16)
    return [r, g, b]
  }
  if (clean.length === 6) {
    const r = parseInt(clean.substring(0, 2), 16)
    const g = parseInt(clean.substring(2, 4), 16)
    const b = parseInt(clean.substring(4, 6), 16)
    return [r, g, b]
  }
  return null
}

export function getLuminance([r, g, b]: [number, number, number]): number {
  const a = [r, g, b].map(v => {
    v /= 255
    return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4)
  })
  return a[0] * 0.2126 + a[1] * 0.7152 + a[2] * 0.0722
}

export interface ContrastResult {
  ratio: number
  rawRatio: number
  isAA: boolean
  isAAA: boolean
}

export function getContrastInfo(hex1: string, hex2: string): ContrastResult {
  const rgb1 = hexToRgb(hex1)
  const rgb2 = hexToRgb(hex2)
  if (!rgb1 || !rgb2) return { ratio: 1, rawRatio: 1, isAA: false, isAAA: false }
  const l1 = getLuminance(rgb1)
  const l2 = getLuminance(rgb2)
  const lighter = Math.max(l1, l2)
  const darker = Math.min(l1, l2)
  const rawRatio = (lighter + 0.05) / (darker + 0.05)
  return {
    ratio: parseFloat(rawRatio.toFixed(2)),
    rawRatio,
    isAA: rawRatio >= 4.5,
    isAAA: rawRatio >= 7.0
  }
}

export function getContrastRatio(hex1: string, hex2: string): number {
  return getContrastInfo(hex1, hex2).ratio
}

export function normalizeHex(hex: string): string | null {
  if (!hex || typeof hex !== 'string') return null
  const clean = hex.replace('#', '').trim()
  if (/^[0-9a-fA-F]{3}$/.test(clean)) {
    return '#' + clean[0].toUpperCase() + clean[0].toUpperCase() +
                 clean[1].toUpperCase() + clean[1].toUpperCase() +
                 clean[2].toUpperCase() + clean[2].toUpperCase()
  }
  if (/^[0-9a-fA-F]{6}$/.test(clean)) {
    return '#' + clean.toUpperCase()
  }
  return null
}

export function getAccessibleTextColor(bgHex: string): string {
  const norm = normalizeHex(bgHex) || bgHex
  if (!hexToRgb(norm)) return '#141413'
  const contrastWithWhite = getContrastRatio(norm, '#FFFFFF')
  // WCAG AA for normal text requires minimum 4.5:1
  if (contrastWithWhite >= 4.5) {
    return '#FFFFFF'
  }
  const contrastWithCharcoal = getContrastRatio(norm, '#141413')
  if (contrastWithCharcoal >= 4.5) {
    return '#141413' // Dark charcoal high-contrast text
  }
  const contrastWithBlack = getContrastRatio(norm, '#000000')
  if (contrastWithBlack >= 4.5) {
    return '#000000' // True black for midtones like #777777 (contrast 4.69:1)
  }
  // Fallback to whichever candidate offers highest contrast
  const highest = Math.max(contrastWithWhite, contrastWithCharcoal, contrastWithBlack)
  if (highest === contrastWithBlack) return '#000000'
  if (highest === contrastWithCharcoal) return '#141413'
  return '#FFFFFF'
}

// Global singleton reactive state
const savedConfig = loadFromStorage()
const currentTheme = reactive<ThemeConfig>({ ...savedConfig })
const draftTheme = reactive<ThemeConfig>({ ...savedConfig })
const isDark = ref(false)

function loadFromStorage(): ThemeConfig {
  if (typeof window === 'undefined') return { ...DEFAULT_THEME }
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      return {
        mode: ['light', 'dark', 'system'].includes(parsed.mode) ? parsed.mode : DEFAULT_THEME.mode,
        primaryColor: typeof parsed.primaryColor === 'string' && normalizeHex(parsed.primaryColor)
          ? normalizeHex(parsed.primaryColor)!
          : DEFAULT_THEME.primaryColor,
        radius: ['0.375rem', '0.5rem', '0.75rem', '1rem'].includes(parsed.radius) ? parsed.radius : DEFAULT_THEME.radius,
        density: ['compact', 'default', 'comfortable'].includes(parsed.density) ? parsed.density : DEFAULT_THEME.density
      }
    }
  } catch (e) {
    console.warn('Failed to read theme config from localStorage:', e)
  }
  return { ...DEFAULT_THEME }
}

function applyTheme(cfg: ThemeConfig) {
  if (typeof document === 'undefined') return
  const root = document.documentElement

  // 1. Resolve Mode
  let resolvedDark = false
  if (cfg.mode === 'dark') {
    resolvedDark = true
  } else if (cfg.mode === 'light') {
    resolvedDark = false
  } else {
    // system
    resolvedDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
  }
  isDark.value = resolvedDark

  if (resolvedDark) {
    root.classList.add('dark')
  } else {
    root.classList.remove('dark')
  }

  // 2. Radius & Role-Specific Geometry (REV-01)
  const numRadius = parseFloat(cfg.radius) || 16
  const px = cfg.radius.includes('rem') ? numRadius * 16 : numRadius
  root.style.setProperty('--radius', cfg.radius)
  root.style.setProperty('--radius-surface', `${px}px`)
  root.style.setProperty('--radius-control', `${Math.round(px * 0.5)}px`)
  root.style.setProperty('--radius-checkbox', '4px')
  root.style.setProperty('--radius-calendar', '6px')
  root.style.setProperty('--radius-badge', `${Math.min(6, Math.round(px * 0.375))}px`)

  // 3. Density
  root.setAttribute('data-density', cfg.density)

  // 4. Primary color override & Accessible Foreground text (FINAL-02 Synchronization)
  if (cfg.primaryColor && cfg.primaryColor !== DEFAULT_THEME.primaryColor) {
    root.style.setProperty('--primary', cfg.primaryColor)
    root.style.setProperty('--ring', cfg.primaryColor)
    const accessibleText = getAccessibleTextColor(cfg.primaryColor)
    root.style.setProperty('--primary-foreground', accessibleText)
    root.style.setProperty('--primary-effective', cfg.primaryColor)
    root.style.setProperty('--primary-effective-foreground', accessibleText)
  } else {
    root.style.removeProperty('--primary')
    root.style.removeProperty('--ring')
    root.style.removeProperty('--primary-foreground')
    root.style.removeProperty('--primary-effective')
    root.style.removeProperty('--primary-effective-foreground')
  }
}

// Initial apply
if (typeof window !== 'undefined') {
  applyTheme(currentTheme)
}

export function useTheme() {
  const resolvedMode = computed(() => {
    if (draftTheme.mode === 'system') {
      return isDark.value ? 'dark' : 'light'
    }
    return draftTheme.mode
  })

  const contrastVsWhite = computed(() => getContrastRatio(draftTheme.primaryColor, '#FFFFFF'))
  const contrastVsDark = computed(() => getContrastRatio(draftTheme.primaryColor, '#141413'))
  const effectiveTextColor = computed(() => getAccessibleTextColor(draftTheme.primaryColor))

  function setThemeMode(mode: ThemeMode) {
    draftTheme.mode = mode
    applyTheme(draftTheme)
  }

  function setPrimaryColor(color: string) {
    draftTheme.primaryColor = color
    applyTheme(draftTheme)
  }

  function setRadius(radius: RadiusPreset) {
    draftTheme.radius = radius
    applyTheme(draftTheme)
  }

  function setDensity(density: DensityMode) {
    draftTheme.density = density
    applyTheme(draftTheme)
  }

  function commitTheme() {
    Object.assign(currentTheme, draftTheme)
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(currentTheme))
    } catch (e) {
      console.error('Failed to save theme to localStorage:', e)
    }
    applyTheme(currentTheme)
  }

  function cancelDraftTheme() {
    Object.assign(draftTheme, currentTheme)
    applyTheme(currentTheme)
  }

  function resetToClaudeDefaults() {
    Object.assign(draftTheme, DEFAULT_THEME)
    commitTheme()
  }

  // Media query listener for system dark mode changes
  const handleMediaChange = (e: MediaQueryListEvent) => {
    if (draftTheme.mode === 'system') {
      applyTheme(draftTheme)
    }
  }

  // Storage listener for cross-tab sync
  const handleStorageChange = (e: StorageEvent) => {
    if (e.key === STORAGE_KEY && e.newValue) {
      try {
        const parsed = JSON.parse(e.newValue)
        Object.assign(currentTheme, parsed)
        Object.assign(draftTheme, parsed)
        applyTheme(currentTheme)
      } catch (err) {
        console.warn('Storage sync failed:', err)
      }
    }
  }

  onMounted(() => {
    const mql = window.matchMedia('(prefers-color-scheme: dark)')
    mql.addEventListener('change', handleMediaChange)
    window.addEventListener('storage', handleStorageChange)
  })

  onUnmounted(() => {
    const mql = window.matchMedia('(prefers-color-scheme: dark)')
    mql.removeEventListener('change', handleMediaChange)
    window.removeEventListener('storage', handleStorageChange)
  })

  return {
    currentTheme,
    draftTheme,
    isDark,
    resolvedMode,
    colorPresets: THEME_COLOR_PRESETS,
    contrastVsWhite,
    contrastVsDark,
    effectiveTextColor,
    getContrastInfo,
    setThemeMode,
    setPrimaryColor,
    setRadius,
    setDensity,
    commitTheme,
    cancelDraftTheme,
    resetToClaudeDefaults
  }
}
