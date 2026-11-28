import { describe, it, expect } from 'vitest'
import { scanContent, forbiddenPatterns } from '../lint-design-system.ts'

describe('Design System Guardrail Linter (REV-08)', () => {
  it('detects forbidden emojis in component code', () => {
    const badCode = `<template><Button>Click Me 🚀</Button></template>`
    const violations = scanContent(badCode, 'test.vue')
    expect(violations.some((v) => v.id === 'forbidden-emoji')).toBe(true)
  })

  it('detects forbidden PrimeIcons usage', () => {
    const badCode = `<i class="pi pi-check"></i>`
    const violations = scanContent(badCode, 'test.vue')
    expect(violations.some((v) => v.id === 'forbidden-primeicons')).toBe(true)
  })

  it('detects forbidden hardcoded tailwind colors (e.g. bg-blue-500, bg-red-600)', () => {
    const badCode = `<div class="bg-blue-500 text-white">Hello</div>`
    const violations = scanContent(badCode, 'test.vue')
    expect(violations.some((v) => v.id === 'forbidden-hardcoded-color')).toBe(true)
  })

  it('passes valid Claude+ semantic classes without violations', () => {
    const goodCode = `
      <div class="bg-primary text-primary-foreground border-border rounded-md p-4">
        <Button variant="default">
          <Check class="h-4 w-4" />
          <span>Xác nhận</span>
        </Button>
      </div>
    `
    const violations = scanContent(goodCode, 'valid.vue')
    expect(violations.length).toBe(0)
  })

  it('guarantees regex statefulness is eliminated across multiple sequential calls', () => {
    const lineWithViolation = `<div class="bg-red-500">Error</div>`
    // If regex has /g flag, second call might return false due to lastIndex retention
    const firstRun = scanContent(lineWithViolation, 'test.vue')
    const secondRun = scanContent(lineWithViolation, 'test.vue')
    const thirdRun = scanContent(lineWithViolation, 'test.vue')

    expect(firstRun.length).toBe(1)
    expect(secondRun.length).toBe(1)
    expect(thirdRun.length).toBe(1)
  })
})
