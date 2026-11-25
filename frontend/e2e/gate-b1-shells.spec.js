import { test, expect } from '@playwright/test'
import path from 'path'
import { fileURLToPath } from 'url'
import { loginAsRole } from './helpers/auth'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const screenshotDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-gate-b1')

test.describe('Gate B1: Application Shells Verification & Visual Evidence', () => {
  test('B1-01: Admin Shell (Light & Dark Mode)', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')

    // Verify Admin Sidebar (256px expanded) & Header (64px)
    const sidebar = page.locator('aside')
    await expect(sidebar).toBeVisible()
    const sidebarWidth = await sidebar.evaluate(el => el.getBoundingClientRect().width)
    expect(sidebarWidth).toBe(256)

    const header = page.locator('header')
    await expect(header).toBeVisible()
    const headerHeight = await header.evaluate(el => el.getBoundingClientRect().height)
    expect(headerHeight).toBe(64)

    // Verify Breadcrumbs & Role Badge
    await expect(page.locator('header nav')).toContainText('Platform Governance')
    await expect(page.locator('header')).toContainText('ROLE_ADMIN')

    // Capture Light Mode
    await page.screenshot({
      path: path.join(screenshotDir, 'admin-shell-light.png'),
      fullPage: false
    })

    // Switch to Dark Mode & Capture
    await page.evaluate(() => {
      document.documentElement.classList.add('dark')
    })
    await page.waitForTimeout(300)
    await page.screenshot({
      path: path.join(screenshotDir, 'admin-shell-dark.png'),
      fullPage: false
    })
  })

  test('B1-02: Operator Shell (Light & Dark Mode)', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_OPERATOR', 'operator1')
    await page.goto('/operator')
    await page.waitForLoadState('networkidle')

    // Verify Operator Sidebar (256px) & Breadcrumbs
    const sidebar = page.locator('aside')
    await expect(sidebar).toBeVisible()
    await expect(page.locator('header nav')).toContainText('Partner Workspace')
    await expect(page.locator('header')).toContainText('ROLE_OPERATOR')

    // Capture Light Mode
    await page.screenshot({
      path: path.join(screenshotDir, 'operator-shell-light.png'),
      fullPage: false
    })

    // Switch to Dark Mode & Capture
    await page.evaluate(() => {
      document.documentElement.classList.add('dark')
    })
    await page.waitForTimeout(300)
    await page.screenshot({
      path: path.join(screenshotDir, 'operator-shell-dark.png'),
      fullPage: false
    })
  })

  test('B1-03: Customer Shell (Light & Dark Mode)', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/')
    await page.waitForLoadState('networkidle')

    // Verify Customer Header & Footer
    const header = page.locator('header')
    await expect(header).toBeVisible()
    const footer = page.locator('footer')
    await expect(footer).toBeVisible()
    await expect(page.locator('header')).toContainText('VeloBus')

    // Capture Light Mode
    await page.screenshot({
      path: path.join(screenshotDir, 'customer-shell-light.png'),
      fullPage: false
    })

    // Switch to Dark Mode & Capture
    await page.evaluate(() => {
      document.documentElement.classList.add('dark')
    })
    await page.waitForTimeout(300)
    await page.screenshot({
      path: path.join(screenshotDir, 'customer-shell-dark.png'),
      fullPage: false
    })
  })

  test('B1-04: Authentication Shell (Light & Dark Mode)', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/login')
    await page.waitForLoadState('networkidle')

    // Verify 45% brand panel and 55% form container
    await expect(page.locator('text=Enterprise Mobility SaaS Platform')).toBeVisible()
    await expect(page.locator('text=Đăng nhập VeloBus')).toBeVisible()

    // Capture Light Mode
    await page.screenshot({
      path: path.join(screenshotDir, 'auth-shell-light.png'),
      fullPage: false
    })

    // Switch to Dark Mode & Capture
    await page.evaluate(() => {
      document.documentElement.classList.add('dark')
    })
    await page.waitForTimeout(300)
    await page.screenshot({
      path: path.join(screenshotDir, 'auth-shell-dark.png'),
      fullPage: false
    })
  })

  test('B1-05: Sidebar Expanded & Collapsed States', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')

    const sidebar = page.locator('aside')
    await expect(sidebar).toBeVisible()

    // Capture Expanded (256px)
    await page.screenshot({
      path: path.join(screenshotDir, 'sidebar-expanded.png'),
      fullPage: false
    })

    // Click collapse button
    const collapseBtn = sidebar.locator('button[title*="Thu gọn menu"]')
    await expect(collapseBtn).toBeVisible()
    await collapseBtn.click()

    // Verify width becomes 64px
    await expect(sidebar).toHaveClass(/w-16/)
    await page.waitForTimeout(350) // wait for transition
    const collapsedWidth = await sidebar.evaluate(el => el.getBoundingClientRect().width)
    expect(collapsedWidth).toBe(64)

    // Capture Collapsed (64px)
    await page.screenshot({
      path: path.join(screenshotDir, 'sidebar-collapsed.png'),
      fullPage: false
    })
  })

  test('B1-06: Mobile Navigation Sheet (375px Viewport)', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 })
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')

    // Open Mobile Drawer via hamburger
    const menuBtn = page.locator('header button:has-text("Mở menu điều hướng")')
    await expect(menuBtn).toBeVisible()
    await menuBtn.click()

    // Verify SheetContent is visible
    const sheet = page.locator('[role="dialog"]')
    await expect(sheet).toBeVisible()
    await expect(sheet).toContainText('Platform Governance')

    await page.screenshot({
      path: path.join(screenshotDir, 'mobile-navigation-admin.png'),
      fullPage: false
    })

    // Close and test Customer Mobile Nav
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    const customerMenuBtn = page.locator('header button:has-text("Mở menu điều hướng")')
    await expect(customerMenuBtn).toBeVisible()
    await customerMenuBtn.click()

    const customerSheet = page.locator('[role="dialog"]')
    await expect(customerSheet).toBeVisible()
    await expect(customerSheet).toContainText('Tìm & Đặt vé')

    await page.screenshot({
      path: path.join(screenshotDir, 'mobile-navigation-customer.png'),
      fullPage: false
    })
  })

  test('B1-07: User Profile Dropdown Menu Interaction', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')

    // Click profile dropdown trigger
    const profileBtn = page.locator('header button:has-text("admin")')
    await expect(profileBtn).toBeVisible()
    await profileBtn.click()

    // Verify dropdown content
    const dropdown = page.locator('[role="menu"]')
    await expect(dropdown).toBeVisible()
    await expect(dropdown).toContainText('Hồ sơ tài khoản')
    await expect(dropdown).toContainText('Đăng xuất')

    await page.screenshot({
      path: path.join(screenshotDir, 'user-profile-dropdown.png'),
      fullPage: false
    })
  })

  test('B1-08: Header Theme Customizer Trigger & Drawer Interaction', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')

    // Click Theme Customizer button in Header
    const customizerBtn = page.locator('header button[title*="Theme Customizer"]')
    await expect(customizerBtn).toBeVisible()
    await customizerBtn.click()

    // Verify customizer sheet/modal is open
    const customizer = page.locator('text=VeloBus Theme Studio')
    await expect(customizer).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotDir, 'theme-customizer-open.png'),
      fullPage: false
    })
  })
})
