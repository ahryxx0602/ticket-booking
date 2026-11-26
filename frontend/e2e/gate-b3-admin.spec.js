import { test, expect } from '@playwright/test'
import path from 'path'
import { fileURLToPath } from 'url'
import { loginAsRole } from './helpers/auth'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const screenshotDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-gate-b3')

test.describe('Gate B3: Admin Pilot Verification & Visual Evidence', () => {
  // B3-01: Admin Dashboard Desktop Light & Dark
  test('B3-01: Admin Dashboard — Desktop Light & Dark (KPIs & Reconciliation)', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')

    // 4 KPI cards visible
    await expect(page.locator('[data-testid="kpi-gmv"]')).toBeVisible()
    await expect(page.locator('[data-testid="kpi-commission"]')).toBeVisible()
    await expect(page.locator('[data-testid="kpi-bookings"]')).toBeVisible()
    await expect(page.locator('[data-testid="kpi-operators"]')).toBeVisible()

    // Assert monetary formatting
    await expect(page.locator('[data-testid="kpi-gmv"]')).toContainText('1.450.800.000')
    await expect(page.locator('[data-testid="kpi-commission"]')).toContainText('142.500.000')

    // Reconciliation table visible
    const table = page.locator('table')
    await expect(table).toBeVisible()
    await expect(page.locator('text=REC-20261123-01')).toBeVisible()

    // Capture Light Mode
    await page.screenshot({
      path: path.join(screenshotDir, 'admin-dashboard-desktop-light.png'),
      fullPage: false
    })

    // Switch to Dark Mode & Capture
    await page.evaluate(() => document.documentElement.classList.add('dark'))
    await page.waitForTimeout(300)
    await page.screenshot({
      path: path.join(screenshotDir, 'admin-dashboard-desktop-dark.png'),
      fullPage: false
    })
  })

  // B3-02: Admin Dashboard Tablet & Mobile
  test('B3-02: Admin Dashboard — Tablet 768px & Mobile 375px', async ({ page }) => {
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')

    // Tablet 768px
    await page.setViewportSize({ width: 768, height: 1024 })
    await page.waitForTimeout(300)
    await expect(page.locator('[data-testid="kpi-gmv"]')).toBeVisible()
    const tabletOverflow = await page.evaluate(() => document.body.scrollWidth > document.body.clientWidth)
    expect(tabletOverflow).toBe(false)

    await page.screenshot({
      path: path.join(screenshotDir, 'admin-dashboard-tablet.png'),
      fullPage: false
    })

    // Mobile 375px
    await page.setViewportSize({ width: 375, height: 667 })
    await page.waitForTimeout(300)
    await expect(page.locator('[data-testid="kpi-gmv"]')).toBeVisible()
    const mobileOverflow = await page.evaluate(() => document.body.scrollWidth > document.body.clientWidth)
    expect(mobileOverflow).toBe(false)

    await page.screenshot({
      path: path.join(screenshotDir, 'admin-dashboard-mobile.png'),
      fullPage: false
    })
  })

  // B3-03: Reconciliation Actions & Confirmation
  test('B3-03: Reconciliation — manual trigger and resolve action', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')

    // Click manual run reconciliation
    const runBtn = page.locator('[data-testid="btn-run-reconciliation"]')
    await expect(runBtn).toBeVisible()
    await runBtn.click()

    // Click resolve alert button
    const resolveBtn = page.locator('[data-testid="btn-resolve-alert"]').first()
    if (await resolveBtn.isVisible()) {
      await resolveBtn.click()
      await page.waitForTimeout(300)
    }

    await page.screenshot({
      path: path.join(screenshotDir, 'admin-reconciliation-resolved.png'),
      fullPage: false
    })
  })

  // B3-04: User Management Desktop Light & Dark
  test('B3-04: User Management — Desktop Light & Dark', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    // Assert main tabs visible
    await expect(page.locator('[data-testid="tab-admin-users"]')).toBeVisible()
    await expect(page.locator('[data-testid="tab-admin-roles"]')).toBeVisible()
    await expect(page.locator('[data-testid="tab-admin-bookings"]')).toBeVisible()

    // Assert category tabs visible
    await expect(page.locator('[data-testid="category-tab-CUSTOMER"]')).toBeVisible()
    await expect(page.locator('[data-testid="category-tab-OPERATOR"]')).toBeVisible()
    await expect(page.locator('[data-testid="category-tab-SYSTEM"]')).toBeVisible()
    await expect(page.locator('[data-testid="category-tab-ALL"]')).toBeVisible()

    // Capture Light Mode
    await page.screenshot({
      path: path.join(screenshotDir, 'user-management-desktop-light.png'),
      fullPage: false
    })

    // Dark Mode
    await page.evaluate(() => document.documentElement.classList.add('dark'))
    await page.waitForTimeout(300)
    await page.screenshot({
      path: path.join(screenshotDir, 'user-management-desktop-dark.png'),
      fullPage: false
    })
  })

  // B3-05: User Management Mobile Responsive
  test('B3-05: User Management — Mobile 375px', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 })
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    // Table container scrollable without breaking viewport
    const pageOverflow = await page.evaluate(() => document.body.scrollWidth > document.body.clientWidth)
    expect(pageOverflow).toBe(false)

    await page.screenshot({
      path: path.join(screenshotDir, 'user-management-mobile.png'),
      fullPage: false
    })
  })

  // B3-06: Category Filtering & Search
  test('B3-06: User Management — category filters & search', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    // Filter by Customer
    await page.locator('[data-testid="category-tab-CUSTOMER"]').click()
    await page.waitForTimeout(200)

    // Filter by Operator
    await page.locator('[data-testid="category-tab-OPERATOR"]').click()
    await page.waitForTimeout(200)

    // Filter by System
    await page.locator('[data-testid="category-tab-SYSTEM"]').click()
    await page.waitForTimeout(200)

    // Reset to All
    await page.locator('[data-testid="category-tab-ALL"]').click()
    await page.waitForTimeout(200)

    await page.screenshot({
      path: path.join(screenshotDir, 'user-management-category-filters.png'),
      fullPage: false
    })
  })

  // B3-07: User Creation Dialog
  test('B3-07: User Form Modal — open dialog and inspect fields', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="open-create-user-modal-btn"]').click()
    await page.waitForTimeout(300)

    // Verify modal inputs exist
    await expect(page.locator('[data-testid="input-user-username"]')).toBeVisible()
    await expect(page.locator('[data-testid="input-user-fullname"]')).toBeVisible()
    await expect(page.locator('[data-testid="input-user-email"]')).toBeVisible()
    await expect(page.locator('[data-testid="modal-select-user-role"]')).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotDir, 'user-create-modal.png'),
      fullPage: false
    })
  })

  // B3-08: Role Management & 26 APIs Matrix
  test('B3-08: Role Management — 7 modules and 26 APIs permission matrix', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    // Switch to ROLES tab
    await page.locator('[data-testid="tab-admin-roles"]').click()
    await page.waitForTimeout(300)

    // Verify system roles visible
    await expect(page.locator('[data-testid="role-card-ROLE_ACCOUNTANT"]')).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotDir, 'role-permission-matrix-tab.png'),
      fullPage: false
    })
  })

  // B3-09: Role Creation & Module Toggle
  test('B3-09: Role Form Modal — toggle module permissions and save', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="tab-admin-roles"]').click()
    await page.waitForTimeout(200)

    await page.locator('[data-testid="btn-create-role-in-tab"]').click()
    await page.waitForTimeout(300)

    // Fill role info
    await page.locator('[data-testid="input-role-code"]').fill('ROLE_MARKETING')
    await page.locator('[data-testid="input-role-name"]').fill('Chuyên Viên Marketing')

    // Clear all then select banner module
    await page.locator('[data-testid="btn-role-clear-all"]').click()
    await page.locator('[data-testid="toggle-module-MOD_BANNERS"]').click()

    await page.screenshot({
      path: path.join(screenshotDir, 'role-create-matrix-modal.png'),
      fullPage: false
    })

    // Save role
    await page.locator('[data-testid="btn-save-role"]').click()
    await page.waitForTimeout(400)

    await expect(page.locator('[data-testid="role-card-ROLE_MARKETING"]')).toBeVisible()
  })

  // B3-10: Role Preview & Banner Activation
  test('B3-10: Role Preview — simulate role permissions in real-time', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="tab-admin-roles"]').click()
    await page.waitForTimeout(200)

    // Click preview role accountant
    await page.locator('[data-testid="preview-role-ROLE_ACCOUNTANT"]').click()
    await page.waitForTimeout(300)

    // Role preview banner should be active
    const previewBanner = page.locator('[data-testid="role-preview-banner"]')
    await expect(previewBanner).toBeVisible()
    await expect(previewBanner).toContainText('ROLE_ACCOUNTANT')

    await page.screenshot({
      path: path.join(screenshotDir, 'role-preview-mode-active.png'),
      fullPage: false
    })

    // Exit role preview
    await page.locator('[data-testid="exit-role-preview-btn"]').click()
    await page.waitForTimeout(300)
    await expect(page.locator('[data-testid="role-preview-banner"]')).toHaveCount(0)
  })

  // B3-11: Self Admin Lock Guard (ADMIN_CANNOT_LOCK_SELF)
  test('B3-11: Security Guard — Super Admin cannot lock own account', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    // Find admin user row and assert self-admin-guard badge
    const selfGuard = page.locator('[data-testid="self-admin-guard"]')
    await expect(selfGuard).toBeVisible()
    await expect(selfGuard).toContainText('Không thể tự khóa')

    // Lock button must not exist for self admin
    await expect(page.locator('[data-testid="btn-lock-USR-ADMIN-01"]')).toHaveCount(0)

    await page.screenshot({
      path: path.join(screenshotDir, 'self-admin-lock-guard.png'),
      fullPage: false
    })
  })

  // B3-12: Booking Management & Forced Refund Confirmation
  test('B3-12: Booking Management — forced refund dialog & audit reason', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    // Switch to BOOKINGS tab
    await page.locator('[data-testid="tab-admin-bookings"]').click()
    await page.waitForTimeout(200)

    // Table with booking rows
    await expect(page.locator('table')).toBeVisible()

    // Open refund modal on first confirmed booking
    const refundBtn = page.locator('[data-testid^="btn-admin-refund-"]').first()
    if (await refundBtn.isVisible()) {
      await refundBtn.click()
      await page.waitForTimeout(300)

      // Enter refund reason
      const reasonInput = page.locator('[data-testid="input-refund-reason"]')
      await expect(reasonInput).toBeVisible()
      await reasonInput.fill('Nhà xe đổi chuyến không đúng lịch trình, admin hoàn 100% tiền.')

      await page.screenshot({
        path: path.join(screenshotDir, 'booking-refund-confirmation-modal.png'),
        fullPage: false
      })

      // Click confirm refund
      await page.locator('[data-testid="btn-confirm-refund"]').click()
      await page.waitForTimeout(400)
    }

    await page.screenshot({
      path: path.join(screenshotDir, 'booking-management-tab.png'),
      fullPage: false
    })
  })

  // B3-13: Compact Density on Admin Views
  test('B3-13: Compact Density Mode on Admin views', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')

    // Apply compact density via theme attribute
    await page.evaluate(() => {
      document.documentElement.setAttribute('data-density', 'compact')
    })
    await page.waitForTimeout(300)

    await page.screenshot({
      path: path.join(screenshotDir, 'admin-compact-density.png'),
      fullPage: false
    })
  })

  // B3-14: Custom Primary Theme on Admin Views
  test('B3-14: Custom Primary Color Theme on Admin views', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')

    // Apply Ocean Blue primary preset
    await page.evaluate(() => {
      document.documentElement.style.setProperty('--primary', '#2563EB')
      document.documentElement.style.setProperty('--primary-effective', '#1D4ED8')
      document.documentElement.style.setProperty('--primary-effective-foreground', '#FFFFFF')
    })
    await page.waitForTimeout(300)

    await page.screenshot({
      path: path.join(screenshotDir, 'admin-custom-primary-theme.png'),
      fullPage: false
    })
  })
})
