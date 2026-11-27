import { describe, it, expect } from 'vitest'
import { loadRegistry, scanContent } from '../../../scripts/lint-design-system.js'

describe('D0-PRE-05: Central Migration Registry & Linter Unit Tests', () => {
  const registry = loadRegistry()

  it('validates migration-registry.json structure and counts', () => {
    expect(registry).toBeDefined()
    expect(Array.isArray(registry.views)).toBe(true)
    expect(registry.views.length).toBe(18)

    const migrated = registry.views.filter((v) => v.migrationStatus === 'MIGRATED')
    const pendingD1 = registry.views.filter((v) => v.migrationStatus === 'PENDING_MIGRATION')
    const unmigrated = registry.views.filter((v) => v.migrationStatus === 'UNMIGRATED')

    expect(migrated.length).toBe(12)
    expect(pendingD1.length).toBe(0)
    expect(unmigrated.length).toBe(6)

    // Verify all views have required fields
    registry.views.forEach((v) => {
      expect(v.viewPath).toBeDefined()
      expect(v.route).toBeDefined()
      expect(v.featureModule).toBeDefined()
      expect(v.migrationGate).toBeDefined()
      expect(v.migrationStatus).toBeDefined()
      expect(typeof v.strictLint).toBe('boolean')
      expect(Array.isArray(v.exceptions)).toBe(true)
    })
  })

  it('detects legacy AppButton import violation in migrated view', () => {
    const fakeContent = `
      <script setup>
      import AppButton from '@/shared/components/AppButton.vue'
      </script>
    `
    const violations = scanContent(fakeContent, 'src/modules/catalog-booking/views/CheckoutView.vue')
    expect(violations.some((v) => v.id === 'forbidden-legacy-components')).toBe(true)
  })

  it('detects PrimeVue imports in migrated view', () => {
    const fakeContent = `
      <script setup>
      import DataTable from 'primevue/datatable'
      </script>
    `
    const violations = scanContent(fakeContent, 'src/modules/catalog-booking/views/CheckoutView.vue')
    expect(violations.some((v) => v.id === 'forbidden-primevue-imports')).toBe(true)
  })

  it('detects PrimeIcons in any scanned component', () => {
    const fakeContent = `
      <template>
        <i class="pi pi-check"></i>
      </template>
    `
    const violations = scanContent(fakeContent, 'src/components/ui/button/Button.vue')
    expect(violations.some((v) => v.id === 'forbidden-primeicons')).toBe(true)
  })

  it('detects hardcoded palette color classes', () => {
    const fakeContent = `
      <template>
        <div class="bg-blue-600 text-white">Warning</div>
      </template>
    `
    const violations = scanContent(fakeContent, 'src/modules/catalog-booking/views/CheckoutView.vue')
    expect(violations.some((v) => v.id === 'forbidden-hardcoded-color')).toBe(true)
  })

  it('detects unicode emoji characters in UI components', () => {
    const fakeContent = `
      <template>
        <button>Click me 🚀</button>
      </template>
    `
    const violations = scanContent(fakeContent, 'src/modules/catalog-booking/views/CheckoutView.vue')
    expect(violations.some((v) => v.id === 'forbidden-emoji')).toBe(true)
  })

  it('detects unapproved native <select> in strict migrated view', () => {
    const fakeContent = `
      <template>
        <select v-model="selected">
          <option value="1">Option 1</option>
        </select>
      </template>
    `
    const violations = scanContent(fakeContent, 'src/modules/catalog-booking/views/CheckoutView.vue')
    expect(violations.some((v) => v.id === 'forbidden-native-select')).toBe(true)
  })

  it('respects documented exceptions for specific legacy components', () => {
    // AdminUsersBookingsView has documented exception 'legacy-formfield'
    const fakeAdminContent = `
      <script setup>
      import FormField from '@/shared/components/FormField.vue'
      </script>
    `
    const violations = scanContent(fakeAdminContent, 'src/modules/admin/views/AdminUsersBookingsView.vue')
    expect(violations.some((v) => v.id === 'forbidden-legacy-components')).toBe(false)
  })
})
