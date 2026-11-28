import { test, expect } from '@playwright/test'
import path from 'path'
import { fileURLToPath } from 'url'
import { loginAsRole } from './helpers/auth'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const screenshotDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-gate-b1-corrected')

test.describe('Gate B1 Corrected: B1-REV-01~06 Targeted Fix Verification', () => {
  // B1-REV-01: Collapsed sidebar — logo not clipped, trigger in header
  test('B1C-01: Sidebar collapsed — logo visible, expand trigger in header', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')

    const sidebar = page.locator('aside')
    await expect(sidebar).toBeVisible()

    // Collapse the sidebar
    await page.locator('aside button[title="Thu gọn menu (64px)"]').click()
    await page.waitForTimeout(400)

    // Sidebar should now be 64px
    const sidebarWidth = await sidebar.evaluate(el => el.getBoundingClientRect().width)
    expect(sidebarWidth).toBe(64)

    // Logo VB box must still be visible (not clipped)
    const logoVisible = await page.locator('aside a > div').first().isVisible()
    expect(logoVisible).toBe(true)

    // Expand trigger must now be in header
    const headerExpandBtn = page.locator('header button[aria-label="Mở rộng menu"]')
    await expect(headerExpandBtn).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotDir, 'sidebar-collapsed-logo-visible.png'),
      fullPage: false
    })

    // Click header expand trigger — sidebar should return to 256px
    await headerExpandBtn.click()
    await page.waitForTimeout(400)
    const expandedWidth = await sidebar.evaluate(el => el.getBoundingClientRect().width)
    expect(expandedWidth).toBe(256)

    await page.screenshot({
      path: path.join(screenshotDir, 'sidebar-expanded-after-header-trigger.png'),
      fullPage: false
    })
  })

  // B1-REV-02: Active nav contrast — verify CSS token is applied in both light & dark
  test('B1C-02: Active nav contrast — primary-effective tokens in light and dark', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')

    // Find active nav link
    const activeLink = page.locator('aside nav a').filter({ hasText: 'Tổng quan' }).first()
    await expect(activeLink).toBeVisible()

    // Verify active link background is set (not transparent)
    const computedBg = await activeLink.evaluate(el => getComputedStyle(el).backgroundColor)
    expect(computedBg).not.toBe('rgba(0, 0, 0, 0)')
    expect(computedBg).not.toMatch(/rgba\(0,\s*0,\s*0,\s*0\)/)

    // primary-effective-foreground is white — check color is white in any browser format
    const computedColor = await activeLink.evaluate(el => getComputedStyle(el).color)
    // Chromium may return oklch(1 0 0) or rgb(255, 255, 255) for white
    const isWhite = computedColor.includes('255, 255, 255') ||
                    computedColor.includes('oklch(1 0 0)') ||
                    computedColor.includes('oklch(1,') ||
                    computedColor === 'white'
    expect(isWhite).toBe(true)

    await page.screenshot({
      path: path.join(screenshotDir, 'active-nav-light-contrast.png'),
      fullPage: false
    })

    // Dark mode
    await page.evaluate(() => document.documentElement.classList.add('dark'))
    await page.waitForTimeout(300)
    const darkComputedColor = await activeLink.evaluate(el => getComputedStyle(el).color)
    const isDarkWhite = darkComputedColor.includes('255, 255, 255') ||
                        darkComputedColor.includes('oklch(1 0 0)') ||
                        darkComputedColor.includes('oklch(1,') ||
                        darkComputedColor === 'white'
    expect(isDarkWhite).toBe(true)

    await page.screenshot({
      path: path.join(screenshotDir, 'active-nav-dark-contrast.png'),
      fullPage: false
    })
  })

  // B1-REV-03: Customer header — no LayoutWidthSwitcher, correct "Vé của tôi" visibility
  test('B1C-03: Customer header — no admin tools, Vé của tôi hidden for guest', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/')
    await page.waitForLoadState('networkidle')

    // As guest: "Vé của tôi" link should NOT appear in desktop nav
    const myBookingsLink = page.locator('header nav a[href="/my-bookings"]')
    await expect(myBookingsLink).not.toBeVisible()

    // LayoutWidthSwitcher should NOT be in customer header
    const widthSwitcher = page.locator('header [data-testid="layout-width-switcher"]')
    expect(await widthSwitcher.count()).toBe(0)

    await page.screenshot({
      path: path.join(screenshotDir, 'customer-header-guest-clean.png'),
      fullPage: false
    })
  })

  // B1-REV-04: Notification store — no mock data shown in production build
  test('B1C-04: Notification store — empty in production build (no mock data)', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/')
    await page.waitForLoadState('networkidle')

    // Notification badge should not appear (no unread notifications in production)
    const badge = page.locator('[data-testid="badge-unread-count"]')
    const badgeCount = await badge.count()
    if (badgeCount > 0) {
      const text = await badge.textContent()
      expect(Number(text)).toBe(0)
    }

    await page.screenshot({
      path: path.join(screenshotDir, 'notification-empty-production.png'),
      fullPage: false
    })
  })

  // B1-REV-06: Theme Customizer — Escape closes, body scroll locked, aria-modal
  test('B1C-05: ThemeCustomizer — Escape key closes, aria-modal set, scroll locked', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/login')
    await page.waitForLoadState('networkidle')

    // Open theme customizer
    await page.locator('button[title="Tùy biến giao diện (Theme Customizer)"]').click()
    await page.waitForTimeout(400)

    const drawer = page.locator('[data-testid="theme-customizer-drawer"]')
    await expect(drawer).toBeVisible()

    // Verify aria-modal attribute
    await expect(drawer).toHaveAttribute('aria-modal', 'true')

    // Verify body has scroll-lock class
    const hasLockClass = await page.evaluate(() => document.body.classList.contains('theme-customizer-open'))
    expect(hasLockClass).toBe(true)

    await page.screenshot({
      path: path.join(screenshotDir, 'theme-customizer-open-auth.png'),
      fullPage: false
    })

    // Press Escape to close
    await page.keyboard.press('Escape')
    await page.waitForTimeout(400)
    await expect(drawer).not.toBeVisible()

    // Verify scroll-lock class removed
    const lockRemoved = await page.evaluate(() => !document.body.classList.contains('theme-customizer-open'))
    expect(lockRemoved).toBe(true)

    await page.screenshot({
      path: path.join(screenshotDir, 'theme-customizer-closed-after-escape.png'),
      fullPage: false
    })
  })

  // B1-REV-07: Responsive verification at key breakpoints
  test('B1C-06: Admin shell responsive — 375px mobile to 1440px desktop', async ({ page }) => {
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')

    // Mobile 375x667
    await page.setViewportSize({ width: 375, height: 667 })
    await page.waitForTimeout(300)
    const headerMobile = page.locator('header')
    await expect(headerMobile).toBeVisible()
    // Header should not overflow
    const headerWidthMobile = await headerMobile.evaluate(el => el.scrollWidth <= el.clientWidth)
    expect(headerWidthMobile).toBe(true)
    // Desktop sidebar hidden on mobile
    await expect(page.locator('aside')).not.toBeVisible()
    await page.screenshot({
      path: path.join(screenshotDir, 'admin-shell-375-mobile.png'),
      fullPage: false
    })

    // Tablet 768x1024 — B2-REV-01: <1024px uses mobile/tablet sheet navigation, desktop aside is hidden
    await page.setViewportSize({ width: 768, height: 1024 })
    await page.waitForTimeout(300)
    await expect(page.locator('aside')).not.toBeVisible()
    const headerTablet = page.locator('header')
    await expect(headerTablet).toBeVisible()
    const headerWidthTablet = await headerTablet.evaluate(el => el.scrollWidth <= el.clientWidth)
    expect(headerWidthTablet).toBe(true)
    await page.screenshot({
      path: path.join(screenshotDir, 'admin-shell-768-tablet.png'),
      fullPage: false
    })

    // Small Laptop 1024x768 — B2-REV-01: 1024-1279px defaults to collapsed (64px)
    await page.setViewportSize({ width: 1024, height: 768 })
    await page.waitForTimeout(300)
    const aside1024 = page.locator('aside')
    await expect(aside1024).toBeVisible()
    const width1024 = await aside1024.evaluate(el => el.getBoundingClientRect().width)
    expect(width1024).toBe(64)

    // Desktop 1440x900 — B2-REV-01: >=1280px defaults to expanded (256px)
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.waitForTimeout(300)
    // Expand if needed
    const aside1440 = page.locator('aside')
    await expect(aside1440).toBeVisible()
    await page.screenshot({
      path: path.join(screenshotDir, 'admin-shell-1440-desktop.png'),
      fullPage: false
    })
  })
})
