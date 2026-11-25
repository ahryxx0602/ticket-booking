import { describe, it, expect, beforeEach, vi } from 'vitest'
import {
  useTheme,
  DEFAULT_THEME,
  STORAGE_KEY,
  getContrastRatio,
  getAccessibleTextColor,
  hexToRgb
} from '../useTheme'

// Setup robust localStorage mock for happy-dom environment
const storageMap = new Map<string, string>()
const localStorageMock = {
  getItem: (key: string) => (storageMap.has(key) ? storageMap.get(key)! : null),
  setItem: (key: string, val: string) => storageMap.set(key, String(val)),
  removeItem: (key: string) => storageMap.delete(key),
  clear: () => storageMap.clear(),
}

Object.defineProperty(globalThis, 'localStorage', {
  value: localStorageMock,
  writable: true,
})

describe('useTheme Composable & Claude+ Theme Hardening', () => {
  beforeEach(() => {
    localStorageMock.clear()
    vi.restoreAllMocks()
    const { resetToClaudeDefaults } = useTheme()
    resetToClaudeDefaults()
  })

  describe('WCAG 2.1 AA Contrast Calculations', () => {
    it('correctly calculates relative luminance and contrast ratio', () => {
      // Pure White vs Pure Black is 21:1
      const whiteBlack = getContrastRatio('#FFFFFF', '#000000')
      expect(whiteBlack).toBe(21)

      // Identical colors have contrast ratio of 1:1
      const whiteWhite = getContrastRatio('#FFFFFF', '#FFFFFF')
      expect(whiteWhite).toBe(1)

      // Canonical Claude+ Light Primary #C96442 vs White is 3.90:1 (< 4.5:1)
      const terracottaWhite = getContrastRatio('#C96442', '#FFFFFF')
      expect(terracottaWhite).toBe(3.90)

      // Canonical Claude+ Light Primary #C96442 vs Dark Charcoal #141413 is 4.73:1 (Pass AA >= 4.5:1)
      const terracottaDark = getContrastRatio('#C96442', '#141413')
      expect(terracottaDark).toBe(4.73)

      // Canonical Claude+ Dark Primary #D97757 vs White is 3.12:1 (< 4.5:1)
      const darkPrimaryWhite = getContrastRatio('#D97757', '#FFFFFF')
      expect(darkPrimaryWhite).toBe(3.12)

      // Canonical Claude+ Dark Primary #D97757 vs Dark Charcoal #141413 is 5.90:1 (Pass AA >= 4.5:1)
      const darkPrimaryDark = getContrastRatio('#D97757', '#141413')
      expect(darkPrimaryDark).toBe(5.90)

      // Darkened Terracotta #BC5937 vs White is 4.54:1 (Pass AA >= 4.5:1)
      const darkenedTerracottaWhite = getContrastRatio('#BC5937', '#FFFFFF')
      expect(darkenedTerracottaWhite).toBe(4.54)

      // Darkened Terracotta #BC5937 vs Dark Charcoal #141413 is 4.06:1 (< 4.5:1)
      const darkenedTerracottaDark = getContrastRatio('#BC5937', '#141413')
      expect(darkenedTerracottaDark).toBe(4.06)
    })

    it('getAccessibleTextColor automatically selects high-contrast text', () => {
      // #C96442 (Light Primary): contrast with white is 3.90:1, contrast with #141413 is 4.73:1 -> #141413
      expect(getAccessibleTextColor('#C96442')).toBe('#141413')

      // #D97757 (Dark Primary): contrast with white is 3.12:1, contrast with #141413 is 5.90:1 -> #141413
      expect(getAccessibleTextColor('#D97757')).toBe('#141413')

      // #BC5937: contrast with white is 4.54:1 (>= 4.5:1) -> #FFFFFF
      expect(getAccessibleTextColor('#BC5937')).toBe('#FFFFFF')

      // For bright yellow, selects dark text #141413
      expect(getAccessibleTextColor('#FEF08A')).toBe('#141413')

      // For deep blue / navy, selects white #FFFFFF
      expect(getAccessibleTextColor('#1E40AF')).toBe('#FFFFFF')
      expect(getAccessibleTextColor('#0F172A')).toBe('#FFFFFF')
    })

    it('gracefully handles malformed hex codes in contrast functions', () => {
      expect(hexToRgb('invalid')).toBeNull()
      expect(getContrastRatio('invalid', '#FFFFFF')).toBe(1)
      expect(getAccessibleTextColor('not-a-color')).toBe('#141413')
    })
  })

  describe('Theme Customizer State & Actions', () => {
    it('initializes with default Claude+ settings', () => {
      const { draftTheme, currentTheme } = useTheme()
      expect(draftTheme.primaryColor).toBe(DEFAULT_THEME.primaryColor)
      expect(draftTheme.radius).toBe('1rem')
      expect(draftTheme.density).toBe('default')
      expect(currentTheme.mode).toBe(DEFAULT_THEME.mode)
    })

    it('setThemeMode, setPrimaryColor, setRadius and setDensity modify draft state and DOM', () => {
      const { draftTheme, setThemeMode, setPrimaryColor, setRadius, setDensity } = useTheme()

      setThemeMode('dark')
      expect(draftTheme.mode).toBe('dark')
      expect(document.documentElement.classList.contains('dark')).toBe(true)

      setPrimaryColor('#2563EB')
      expect(draftTheme.primaryColor).toBe('#2563EB')

      setRadius('0.5rem')
      expect(draftTheme.radius).toBe('0.5rem')
      expect(document.documentElement.style.getPropertyValue('--radius')).toBe('0.5rem')

      setDensity('compact')
      expect(draftTheme.density).toBe('compact')
      expect(document.documentElement.getAttribute('data-density')).toBe('compact')
    })

    it('commitTheme saves draft state to localStorage under velobus_theme_config_v2', () => {
      const { setThemeMode, setPrimaryColor, commitTheme, currentTheme } = useTheme()

      setThemeMode('light')
      setPrimaryColor('#16A34A')
      commitTheme()

      expect(currentTheme.mode).toBe('light')
      expect(currentTheme.primaryColor).toBe('#16A34A')

      const storedRaw = localStorage.getItem(STORAGE_KEY)
      expect(storedRaw).toBeTruthy()
      const parsed = JSON.parse(storedRaw!)
      expect(parsed.mode).toBe('light')
      expect(parsed.primaryColor).toBe('#16A34A')
    })

    it('cancelDraftTheme reverts draft state back to committed theme', () => {
      const { setThemeMode, setPrimaryColor, commitTheme, cancelDraftTheme, draftTheme } = useTheme()

      setThemeMode('dark')
      setPrimaryColor('#7C3AED')
      commitTheme()

      // User changes draft without saving
      setThemeMode('light')
      setPrimaryColor('#EA580C')
      expect(draftTheme.primaryColor).toBe('#EA580C')

      // Cancel
      cancelDraftTheme()
      expect(draftTheme.mode).toBe('dark')
      expect(draftTheme.primaryColor).toBe('#7C3AED')
    })

    it('resetToClaudeDefaults restores canonical defaults immediately', () => {
      const { setRadius, setDensity, resetToClaudeDefaults, draftTheme, currentTheme } = useTheme()

      setRadius('1rem')
      setDensity('comfortable')
      expect(draftTheme.radius).toBe('1rem')

      resetToClaudeDefaults()
      expect(draftTheme.radius).toBe(DEFAULT_THEME.radius)
      expect(draftTheme.density).toBe(DEFAULT_THEME.density)
      expect(currentTheme.primaryColor).toBe(DEFAULT_THEME.primaryColor)
    })
  })

  describe('Storage Validation & Error Recovery', () => {
    it('recovers gracefully from corrupted JSON in localStorage', () => {
      localStorage.setItem(STORAGE_KEY, '{invalid json}')
      const { resetToClaudeDefaults, draftTheme } = useTheme()
      resetToClaudeDefaults()
      expect(draftTheme.mode).toBe(DEFAULT_THEME.mode)
    })

    it('recovers gracefully from invalid enum values in storage schema', () => {
      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({
          mode: 'invalid_mode',
          primaryColor: 'not_a_hex',
          radius: '999px',
          density: 'ultra_wide'
        })
      )
      const { resetToClaudeDefaults, draftTheme } = useTheme()
      resetToClaudeDefaults()
      expect(draftTheme.primaryColor).toBe(DEFAULT_THEME.primaryColor)
      expect(draftTheme.radius).toBe(DEFAULT_THEME.radius)
    })
  })
})
