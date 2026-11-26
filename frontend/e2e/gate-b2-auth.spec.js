import { test, expect } from '@playwright/test'
import path from 'path'
import { fileURLToPath } from 'url'
import { makeE2eJwt } from './helpers/auth'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const screenshotDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-gate-b2-corrected')

test.describe('Gate B2 Corrected: Realistic Authentication Pilot & Verification', () => {
  // B2-01: Login desktop light — two-panel layout, no marketing fluff
  test('B2-01: Login desktop light — two-panel layout, form components', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/login')
    await page.waitForLoadState('networkidle')

    // Brand panel (left, lg:flex)
    const brandPanel = page.locator('.hidden.lg\\:flex').first()
    await expect(brandPanel).toBeVisible()

    // Form container (right panel)
    const formPanel = page.locator('form')
    await expect(formPanel).toBeVisible()

    // VeloBus brand visible
    await expect(brandPanel).toContainText('VeloBus')

    // B2-REV-07: Verified neutral content, no unsupported marketing claims
    await expect(brandPanel).not.toContainText('Hàng nghìn')
    await expect(brandPanel).not.toContainText('Hàng trăm')
    await expect(brandPanel).not.toContainText('vài giây')
    await expect(brandPanel).not.toContainText('Redis')
    await expect(brandPanel).not.toContainText('HMAC-SHA256')

    // Input components with labels
    await expect(page.locator('label[for="username"]')).toContainText('Tên đăng nhập')
    await expect(page.locator('label[for="password"]')).toContainText('Mật khẩu')
    await expect(page.locator('#username')).toBeVisible()
    await expect(page.locator('#password')).toBeVisible()

    // Submit button
    await expect(page.locator('button[type="submit"]')).toContainText('Đăng nhập')

    // No demo credentials in production preview mode
    const demoText = page.locator('text=Tài khoản mẫu')
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

  // B2-03: Login tablet 768px
  test('B2-03: Login tablet 768px — single-column form', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 })
    await page.goto('/login')
    await page.waitForLoadState('networkidle')

    // Brand panel hidden on tablet
    const brandPanel = page.locator('.hidden.lg\\:flex').first()
    await expect(brandPanel).not.toBeVisible()

    // Mobile brand mark visible
    await expect(page.locator('.lg\\:hidden').filter({ hasText: 'VeloBus' })).toBeVisible()

    // No horizontal overflow
    const hasOverflow = await page.evaluate(() => document.body.scrollWidth > document.body.clientWidth)
    expect(hasOverflow).toBe(false)

    await page.screenshot({
      path: path.join(screenshotDir, 'login-tablet.png'),
      fullPage: false
    })
  })

  // B2-04: Login mobile 375px light & dark
  test('B2-04: Login mobile 375px — touch targets & responsive layout', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 })
    await page.goto('/login')
    await page.waitForLoadState('networkidle')

    // No horizontal overflow
    const hasOverflow = await page.evaluate(() => document.body.scrollWidth > document.body.clientWidth)
    expect(hasOverflow).toBe(false)

    // Submit button touch target height >= 40px
    const btnHeight = await page.locator('button[type="submit"]').evaluate(el => el.getBoundingClientRect().height)
    expect(btnHeight).toBeGreaterThanOrEqual(40)

    await page.screenshot({
      path: path.join(screenshotDir, 'login-mobile-light.png'),
      fullPage: false
    })

    // Dark mode
    await page.evaluate(() => document.documentElement.classList.add('dark'))
    await page.waitForTimeout(300)

    await page.screenshot({
      path: path.join(screenshotDir, 'login-mobile-dark.png'),
      fullPage: false
    })
  })

  // B2-05: Inline Vietnamese validation (B2-REV-02)
  test('B2-05: Inline Vietnamese validation — no native tooltips, no API calls', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/login')
    await page.waitForLoadState('networkidle')

    let apiCalled = false
    await page.route('**/api/v1/auth/login', route => {
      apiCalled = true
      route.abort()
    })

    // Click submit with empty form
    await page.locator('button[type="submit"]').click()

    // Assert inline Vietnamese validation messages
    await expect(page.locator('#username-error')).toContainText('Vui lòng nhập tên đăng nhập')
    await expect(page.locator('#password-error')).toContainText('Vui lòng nhập mật khẩu')

    // Assert aria-invalid and aria-describedby
    await expect(page.locator('#username')).toHaveAttribute('aria-invalid', 'true')
    await expect(page.locator('#username')).toHaveAttribute('aria-describedby', 'username-error')

    // Assert NO API call was made
    expect(apiCalled).toBe(false)

    await page.screenshot({
      path: path.join(screenshotDir, 'login-inline-validation-errors.png'),
      fullPage: false
    })
  })

  // B2-06: PasswordInput component keyboard accessibility & toggle (B2-REV-03)
  test('B2-06: PasswordInput — keyboard accessible toggle button', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/login')
    await page.waitForLoadState('networkidle')

    const passwordInput = page.locator('#password')
    await passwordInput.fill('SecretPassword123')

    const toggleBtn = page.locator('button[aria-label="Hiện mật khẩu"]')
    await expect(toggleBtn).toBeVisible()

    // Click toggle button
    await toggleBtn.click()
    await expect(passwordInput).toHaveAttribute('type', 'text')
    await expect(page.locator('button[aria-label="Ẩn mật khẩu"]')).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotDir, 'login-password-visible.png'),
      fullPage: false
    })

    // Click again to hide
    await page.locator('button[aria-label="Ẩn mật khẩu"]').click()
    await expect(passwordInput).toHaveAttribute('type', 'password')
  })

  // B2-07: Realistic Admin Login Submission with API Interception & Auto Redirect (B2-REV-04)
  test('B2-07: Admin login submission — payload assert, loading state, redirect to /admin', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/login')
    await page.waitForLoadState('networkidle')

    let requestBody = null
    const adminToken = makeE2eJwt('ROLE_ADMIN', 'admin')

    await page.route('**/api/v1/auth/login', async route => {
      requestBody = route.request().postDataJSON()
      await new Promise(r => setTimeout(r, 200)) // slight delay to verify loading state
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          message: 'Đăng nhập thành công',
          data: {
            accessToken: adminToken,
            username: 'admin',
            role: 'ROLE_ADMIN'
          }
        })
      })
    })

    // Fill form
    await page.locator('#username').fill('admin')
    await page.locator('#password').fill('AdminPassword123')

    // Submit form
    const submitBtn = page.locator('button[type="submit"]')
    await submitBtn.click()

    // Assert loading state & disabled
    await expect(submitBtn).toBeDisabled()

    // Wait for redirect to /admin
    await page.waitForURL('**/admin', { timeout: 5000 })
    expect(page.url()).toContain('/admin')

    // Assert request payload matched
    expect(requestBody).toEqual({
      username: 'admin',
      password: 'AdminPassword123'
    })

    // Assert auth state stored
    const storedToken = await page.evaluate(() => localStorage.getItem('velobus_access_token'))
    expect(storedToken).toBe(adminToken)

    await page.screenshot({
      path: path.join(screenshotDir, 'login-admin-redirect-success.png'),
      fullPage: false
    })
  })

  // B2-08: Realistic Operator Login Submission (B2-REV-04)
  test('B2-08: Operator login submission — redirect to /operator', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/login')
    await page.waitForLoadState('networkidle')

    const operatorToken = makeE2eJwt('ROLE_OPERATOR', 'operator1')

    await page.route('**/api/v1/auth/login', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          message: 'Đăng nhập thành công',
          data: {
            accessToken: operatorToken,
            username: 'operator1',
            role: 'ROLE_OPERATOR'
          }
        })
      })
    })

    await page.locator('#username').fill('operator1')
    await page.locator('#password').fill('OpPassword123')
    await page.locator('button[type="submit"]').click()

    await page.waitForURL('**/operator', { timeout: 5000 })
    expect(page.url()).toContain('/operator')
  })

  // B2-09: Single-Flight Duplicate Submission Prevention (B2-REV-04)
  test('B2-09: Duplicate submission prevention — only 1 API call on double click', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/login')
    await page.waitForLoadState('networkidle')

    let callCount = 0
    await page.route('**/api/v1/auth/login', async route => {
      callCount++
      await new Promise(r => setTimeout(r, 400))
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'Bad credentials' })
      })
    })

    await page.locator('#username').fill('admin')
    await page.locator('#password').fill('wrongpassword')

    const submitBtn = page.locator('button[type="submit"]')
    // Double click quickly
    await submitBtn.click()
    await submitBtn.click({ force: true }).catch(() => {})

    await page.waitForTimeout(600)
    expect(callCount).toBe(1)
  })

  // B2-10: Invalid Credentials Error (B2-REV-02 & B2-REV-04)
  test('B2-10: Invalid credentials error — displays clear Vietnamese error alert', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/login')
    await page.waitForLoadState('networkidle')

    await page.route('**/api/v1/auth/login', async route => {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'Bad credentials' })
      })
    })

    await page.locator('#username').fill('admin')
    await page.locator('#password').fill('wrongpassword')
    await page.locator('button[type="submit"]').click()

    const errorAlert = page.locator('[role="alert"]')
    await expect(errorAlert).toBeVisible()
    await expect(errorAlert).toContainText('Tên đăng nhập hoặc mật khẩu không chính xác')

    await page.screenshot({
      path: path.join(screenshotDir, 'login-invalid-credentials-alert.png'),
      fullPage: false
    })
  })

  // B2-11: Network Error handling
  test('B2-11: Network error — displays safe network failure alert', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/login')
    await page.waitForLoadState('networkidle')

    await page.route('**/api/v1/auth/login', async route => {
      await route.abort('failed')
    })

    await page.locator('#username').fill('admin')
    await page.locator('#password').fill('anypassword')
    await page.locator('button[type="submit"]').click()

    const errorAlert = page.locator('[role="alert"]')
    await expect(errorAlert).toBeVisible()
    await expect(errorAlert).toContainText('Không thể kết nối đến máy chủ')
  })

  // B2-12: Session Expiration reason handling
  test('B2-12: Session expired reason — displays session expiration notification', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/login?reason=session_expired')
    await page.waitForLoadState('networkidle')

    const reasonAlert = page.locator('[role="alert"]')
    await expect(reasonAlert).toBeVisible()
    await expect(reasonAlert).toContainText('Phiên đăng nhập đã hết hạn')

    await page.screenshot({
      path: path.join(screenshotDir, 'login-session-expired-alert.png'),
      fullPage: false
    })
  })

  // B2-13: Theme Studio Focus Trap & Accessibility (B2-REV-05)
  test('B2-13: Theme Studio — keyboard focus trap, Escape, scroll lock', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/login')
    await page.waitForLoadState('networkidle')

    const triggerBtn = page.locator('button[title="Tùy biến giao diện (Theme Customizer)"]')
    await triggerBtn.click()
    await page.waitForTimeout(300)

    const drawer = page.locator('[data-testid="theme-customizer-drawer"]')
    await expect(drawer).toBeVisible()
    await expect(drawer).toHaveAttribute('aria-modal', 'true')

    // Assert body scroll lock
    const isLocked = await page.evaluate(() => document.body.classList.contains('theme-customizer-open'))
    expect(isLocked).toBe(true)

    // Test Tab cycles inside drawer
    await page.keyboard.press('Tab')
    const activeElInDrawer = await page.evaluate(() => {
      const drawer = document.querySelector('[data-testid="theme-customizer-drawer"]')
      return drawer?.contains(document.activeElement)
    })
    expect(activeElInDrawer).toBe(true)

    await page.screenshot({
      path: path.join(screenshotDir, 'theme-studio-focus-trap.png'),
      fullPage: false
    })

    // Escape closes drawer
    await page.keyboard.press('Escape')
    await page.waitForTimeout(300)
    await expect(drawer).not.toBeVisible()

    // Assert scroll lock removed
    const isLockRemoved = await page.evaluate(() => !document.body.classList.contains('theme-customizer-open'))
    expect(isLockRemoved).toBe(true)
  })
})
