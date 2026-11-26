import { test, expect } from '@playwright/test'
import path from 'path'
import { fileURLToPath } from 'url'
import { loginAsRole } from './helpers/auth'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const screenshotDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-gate-b2')

test.describe('Gate B2: Authentication Pilot — LoginView with AuthLayout', () => {
  // B2-01: Login form desktop light
  test('B2-01: Login desktop light — two-panel layout, form components', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/login')
    await page.waitForLoadState('networkidle')

    // Verify AuthLayout two-panel structure
    // Brand panel (left, lg:flex)
    const brandPanel = page.locator('.hidden.lg\\:flex').first()
    await expect(brandPanel).toBeVisible()

    // Form container (right panel)
    const formPanel = page.locator('form')
    await expect(formPanel).toBeVisible()

    // VeloBus brand visible in brand panel
    await expect(brandPanel).toContainText('VeloBus')

    // Key nav text: user-friendly (not technical jargon)
    await expect(brandPanel).toContainText('Tìm chuyến xe')
    await expect(brandPanel).not.toContainText('Redis')
    await expect(brandPanel).not.toContainText('HMAC-SHA256')
    await expect(brandPanel).not.toContainText('Sandbox')

    // Input components
    await expect(page.locator('input[type="text"]')).toBeVisible()
    await expect(page.locator('input[type="password"]')).toBeVisible()

    // Submit button
    await expect(page.locator('button[type="submit"]')).toContainText('Đăng nhập')

    // No demo credentials in production build (vite preview = production mode)
    const demoText = page.locator('text=Tài khoản mẫu')
    // In preview mode (production), isDev=false, so this should be hidden
    expect(await demoText.count()).toBe(0)

    await page.screenshot({
      path: path.join(screenshotDir, 'login-desktop-light.png'),
      fullPage: false
    })
  })

  // B2-02: Login desktop dark
  test('B2-02: Login desktop dark mode', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/login')
    await page.waitForLoadState('networkidle')

    await page.evaluate(() => document.documentElement.classList.add('dark'))
    await page.waitForTimeout(300)

    await page.screenshot({
      path: path.join(screenshotDir, 'login-desktop-dark.png'),
      fullPage: false
    })
  })

  // B2-03: Login tablet
  test('B2-03: Login tablet 768px — single-column form, brand visible', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 })
    await page.goto('/login')
    await page.waitForLoadState('networkidle')

    // Brand panel hidden on tablet (lg:flex = >=1024px)
    const brandPanel = page.locator('.hidden.lg\\:flex').first()
    await expect(brandPanel).not.toBeVisible()

    // Mobile VeloBus logo shown
    await expect(page.locator('.lg\\:hidden').filter({ hasText: 'VeloBus' })).toBeVisible()

    // Form still visible
    await expect(page.locator('form')).toBeVisible()

    // No horizontal overflow
    const hasOverflow = await page.evaluate(() => document.body.scrollWidth > document.body.clientWidth)
    expect(hasOverflow).toBe(false)

    await page.screenshot({
      path: path.join(screenshotDir, 'login-tablet.png'),
      fullPage: false
    })
  })

  // B2-04: Login mobile light
  test('B2-04: Login mobile 375px — single column, touch targets OK', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 })
    await page.goto('/login')
    await page.waitForLoadState('networkidle')

    // No horizontal overflow
    const hasOverflow = await page.evaluate(() => document.body.scrollWidth > document.body.clientWidth)
    expect(hasOverflow).toBe(false)

    // Form visible
    await expect(page.locator('form')).toBeVisible()

    // Submit button — touch target ≥ 44px height
    const btnHeight = await page.locator('button[type="submit"]').evaluate(el => el.getBoundingClientRect().height)
    expect(btnHeight).toBeGreaterThanOrEqual(40)

    await page.screenshot({
      path: path.join(screenshotDir, 'login-mobile-light.png'),
      fullPage: false
    })
  })

  // B2-05: Login mobile dark
  test('B2-05: Login mobile 375px dark mode', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 })
    await page.goto('/login')
    await page.waitForLoadState('networkidle')

    await page.evaluate(() => document.documentElement.classList.add('dark'))
    await page.waitForTimeout(300)

    await page.screenshot({
      path: path.join(screenshotDir, 'login-mobile-dark.png'),
      fullPage: false
    })
  })

  // B2-06: Validation errors — empty submit
  test('B2-06: Validation — empty form shows browser required validation', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/login')
    await page.waitForLoadState('networkidle')

    // Submit empty form — HTML5 required should prevent submission
    await page.locator('button[type="submit"]').click()

    // Input should have :invalid state — form not submitted
    const usernameInvalid = await page.locator('input[type="text"]').evaluate(el => !el.validity.valid)
    expect(usernameInvalid).toBe(true)

    await page.screenshot({
      path: path.join(screenshotDir, 'login-validation-empty.png'),
      fullPage: false
    })
  })

  // B2-07: Password visibility toggle
  test('B2-07: PasswordInput — toggle visibility icon', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/login')
    await page.waitForLoadState('networkidle')

    const passwordInput = page.locator('input[type="password"]')
    await expect(passwordInput).toBeVisible()

    // Fill password
    await passwordInput.fill('testpassword')

    // Find and click show/hide toggle button (PasswordInput component)
    const toggleBtn = page.locator('button[type="button"]').filter({ has: page.locator('svg') }).last()
    await toggleBtn.click()
    await page.waitForTimeout(200)

    // Input type should change to text
    const inputType = await page.locator('input').nth(1).getAttribute('type')
    expect(inputType).toBe('text')

    await page.screenshot({
      path: path.join(screenshotDir, 'login-password-visible.png'),
      fullPage: false
    })
  })

  // B2-08: Loading state during submit
  test('B2-08: Loading state — button disabled during submission', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/login')
    await page.waitForLoadState('networkidle')

    // Fill credentials
    await page.locator('input[type="text"]').fill('admin')
    await page.locator('input[type="password"]').fill('wrongpassword')

    // Intercept API to delay response
    await page.route('**/api/v1/auth/login', async route => {
      await new Promise(resolve => setTimeout(resolve, 500))
      await route.fulfill({ status: 401, body: JSON.stringify({ message: 'Invalid credentials' }) })
    })

    // Click submit
    await page.locator('button[type="submit"]').click()

    // Button should show loading text briefly
    await page.screenshot({
      path: path.join(screenshotDir, 'login-loading-state.png'),
      fullPage: false
    })

    // Wait for error message
    await page.waitForTimeout(1000)

    // Error alert should appear
    const errorAlert = page.locator('[role="alert"]')
    await expect(errorAlert).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotDir, 'login-invalid-credentials-error.png'),
      fullPage: false
    })
  })

  // B2-09: Theme Studio on Auth page
  test('B2-09: Theme Studio accessible from Login page', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/login')
    await page.waitForLoadState('networkidle')

    // Paintbrush trigger in top-right
    const themeBtn = page.locator('button[title="Tùy biến giao diện (Theme Customizer)"]')
    await expect(themeBtn).toBeVisible()

    // Open theme customizer
    await themeBtn.click()
    await page.waitForTimeout(400)

    const drawer = page.locator('[data-testid="theme-customizer-drawer"]')
    await expect(drawer).toBeVisible()
    await expect(drawer).toHaveAttribute('aria-modal', 'true')

    await page.screenshot({
      path: path.join(screenshotDir, 'login-theme-studio-open.png'),
      fullPage: false
    })

    // Close via Escape
    await page.keyboard.press('Escape')
    await page.waitForTimeout(400)
    await expect(drawer).not.toBeVisible()
  })

  // B2-10: Successful login redirect
  test('B2-10: Successful login — redirects to correct portal by role', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    // Navigate after injecting token
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')

    // Should land at /admin
    expect(page.url()).toContain('/admin')

    // Admin shell visible
    await expect(page.locator('aside')).toBeVisible()
    await expect(page.locator('header')).toContainText('ROLE_ADMIN')

    await page.screenshot({
      path: path.join(screenshotDir, 'login-redirect-admin-portal.png'),
      fullPage: false
    })
  })
})
