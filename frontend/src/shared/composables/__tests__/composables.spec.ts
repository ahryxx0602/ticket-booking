import { describe, it, expect } from 'vitest'
import { useLayoutWidth } from '../useLayoutWidth'
import { useCountdown } from '../useCountdown'

describe('useLayoutWidth Composable', () => {
  it('has mode value and containerClass', () => {
    const { mode, containerClass } = useLayoutWidth()
    expect(['narrow', 'normal', 'wide']).toContain(mode.value)
    expect(containerClass.value).toBeDefined()
  })

  it('updates to narrow mode', () => {
    const { mode, containerClass, setMode } = useLayoutWidth()
    setMode('narrow')
    expect(mode.value).toBe('narrow')
    expect(containerClass.value).toContain('max-w-5xl')
  })

  it('updates to wide mode', () => {
    const { mode, containerClass, setMode } = useLayoutWidth()
    setMode('wide')
    expect(mode.value).toBe('wide')
    expect(containerClass.value).toContain('max-w-none')
  })

  it('updates to normal mode', () => {
    const { mode, containerClass, setMode } = useLayoutWidth()
    setMode('normal')
    expect(mode.value).toBe('normal')
    expect(containerClass.value).toContain('max-w-7xl')
  })
})

describe('useCountdown Composable', () => {
  it('formats remaining time correctly into mm:ss', () => {
    const { formattedTime, start, stop } = useCountdown()
    const target = new Date(Date.now() + 65 * 1000).toISOString()
    start(target)
    expect(formattedTime.value).toBe('01:05')
    stop()
  })
})
