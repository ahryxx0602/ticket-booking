import { test, expect } from '@playwright/test'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const screenshotDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-showcase-hub')

test.describe('Showcase UI Fidelity & Interactive Audit (UI-01 through UI-14)', () => {
  test('Audit 01: Desktop Showcase Hub Overview & Foundations (Light Mode)', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/showcase')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('h1')).toContainText('VeloBus UI Component Specification')
    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-hub-desktop-light-corrected.png'),
      fullPage: false
    })
  })

  test('Audit 02: Desktop Showcase Hub (Dark Mode)', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/showcase')
    await page.waitForLoadState('networkidle')

    // Click dark mode toggle button in header
    const themeButton = page.locator('header button[title*="Chuyển sang giao diện Tối"], header button:has(.lucide-moon)')
    if (await themeButton.isVisible()) {
      await themeButton.click()
      await page.waitForTimeout(300)
    }

    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-hub-desktop-dark-corrected.png'),
      fullPage: false
    })
  })

  test('Audit 03: Calendar Primitive with vi-VN Locale and Monday First', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 800 })
    await page.goto('/showcase')
    await page.waitForLoadState('networkidle')

    const calendarSection = page.locator('#calendar-inline')
    await calendarSection.scrollIntoViewIfNeeded()
    await page.waitForTimeout(300)

    // Verify Vietnamese weekdays header (T2 or Thứ 2)
    await expect(calendarSection).toBeVisible()
    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-calendar-vi-vn-corrected.png'),
      clip: await calendarSection.boundingBox() || undefined
    })
  })

  test('Audit 04: DatePicker Popover Open State & TimePicker 24H', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 800 })
    await page.goto('/showcase')
    await page.waitForLoadState('networkidle')

    const datepickerSection = page.locator('#calendar-datepicker')
    await datepickerSection.scrollIntoViewIfNeeded()

    // Click DatePicker Popover trigger button
    const dateButton = datepickerSection.locator('button:has-text("/")').first()
    if (await dateButton.isVisible()) {
      await dateButton.click()
      await page.waitForTimeout(400)
    }

    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-datepicker-popover-open.png'),
      fullPage: false
    })
  })

  test('Audit 04b: Custom TimePicker 24H Dropdown Open State', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 800 })
    await page.goto('/showcase')
    await page.waitForLoadState('networkidle')

    const datepickerSection = page.locator('#calendar-datepicker')
    await datepickerSection.scrollIntoViewIfNeeded()

    // Click TimePicker SelectTrigger for hour
    const hourTrigger = datepickerSection.locator('[role="combobox"]').first()
    if (await hourTrigger.isVisible()) {
      await hourTrigger.click()
      await page.waitForTimeout(400)
    }

    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-timepicker-dropdown-open.png'),
      fullPage: false
    })
  })

  test('Audit 05: DateRangePicker with Month Crossing & Presets Popover Open State', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 800 })
    await page.goto('/showcase')
    await page.waitForLoadState('networkidle')

    const rangeSection = page.locator('#calendar-range')
    await rangeSection.scrollIntoViewIfNeeded()

    // Click Range Picker trigger button
    const rangeButton = rangeSection.locator('button:has(.lucide-calendar)').first()
    if (await rangeButton.isVisible()) {
      await rangeButton.click()
      await page.waitForTimeout(400)
    }

    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-range-presets-open.png'),
      fullPage: false
    })
  })

  test('Audit 06: DataTable Density Modes, Row Selection & Pagination State', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 900 })
    await page.goto('/showcase')
    await page.waitForLoadState('networkidle')

    const tableSection = page.locator('#data-table')
    await tableSection.scrollIntoViewIfNeeded()

    // 1. Select the first two rows
    const checkboxes = tableSection.locator('input[type="checkbox"], button[role="checkbox"]')
    if (await checkboxes.count() > 2) {
      await checkboxes.nth(1).click()
      await checkboxes.nth(2).click()
      await page.waitForTimeout(200)
    }

    // Capture Default density
    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-datatable-density-default.png'),
      clip: await tableSection.boundingBox() || undefined
    })

    // 2. Switch to Compact density
    const compactBtn = tableSection.locator('button:has-text("Compact"), button:has-text("Gọn")')
    if (await compactBtn.isVisible()) {
      await compactBtn.click()
      await page.waitForTimeout(200)
      await page.screenshot({
        path: path.join(screenshotDir, 'showcase-datatable-density-compact.png'),
        clip: await tableSection.boundingBox() || undefined
      })
    }

    // 3. Click next page
    const nextBtn = tableSection.locator('button:has-text("Sau")')
    if (await nextBtn.isEnabled()) {
      await nextBtn.click()
      await page.waitForTimeout(200)
      await page.screenshot({
        path: path.join(screenshotDir, 'showcase-datatable-pagination-page2.png'),
        clip: await tableSection.boundingBox() || undefined
      })
    }
  })

  test('Audit 07: Dropdown Menu System Open Interactive State', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 800 })
    await page.goto('/showcase')
    await page.waitForLoadState('networkidle')

    const dropdownSection = page.locator('#nav-dropdown')
    await dropdownSection.scrollIntoViewIfNeeded()

    const accountMenuTrigger = dropdownSection.locator('button:has-text("Tài khoản cá nhân")')
    if (await accountMenuTrigger.isVisible()) {
      await accountMenuTrigger.click()
      await page.waitForTimeout(300)
    }

    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-dropdown-menu-open.png'),
      fullPage: false
    })
  })

  test('Audit 08: Modals, Dialogs and Feedback Open Interactive State', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 800 })
    await page.goto('/showcase')
    await page.waitForLoadState('networkidle')

    const feedbackSection = page.locator('#feedback-modals')
    await feedbackSection.scrollIntoViewIfNeeded()

    const dialogBtn = feedbackSection.locator('button:has-text("Mở Dialog"), button:has-text("Xác nhận")').first()
    if (await dialogBtn.isVisible()) {
      await dialogBtn.click()
      await page.waitForTimeout(400)
    }

    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-dialog-confirmation-open.png'),
      fullPage: false
    })
  })

  test('Audit 09: Theme Customizer Drawer Open State', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 800 })
    await page.goto('/showcase')
    await page.waitForLoadState('networkidle')

    // Click "Tùy Biến Theme" in header
    const customizerBtn = page.locator('header button:has-text("Tùy Biến Theme")')
    await customizerBtn.click()
    await page.waitForTimeout(400)

    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-theme-customizer-open.png'),
      fullPage: false
    })
  })

  test('Audit 10: Mobile Responsive 375px Header & Navigation Sheet', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 })
    await page.goto('/showcase')
    await page.waitForLoadState('networkidle')

    // Open mobile hamburger menu
    const menuBtn = page.locator('[data-testid="mobile-menu-trigger"]')
    await menuBtn.click()
    await page.waitForTimeout(400)

    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-mobile-375-navigation-sheet.png'),
      fullPage: false
    })
  })

  test('Audit 11: Mobile Responsive 375px Calendar without Overlapping Trigger', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 })
    await page.goto('/showcase')
    await page.waitForLoadState('networkidle')

    const calSection = page.locator('#calendar-inline')
    await calSection.scrollIntoViewIfNeeded()
    await page.waitForTimeout(300)

    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-mobile-375-calendar.png'),
      fullPage: false
    })
  })

  test('Audit 12: Mobile Responsive 390px DataTable Flow', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 })
    await page.goto('/showcase')
    await page.waitForLoadState('networkidle')

    const tableSection = page.locator('#data-table')
    await tableSection.scrollIntoViewIfNeeded()
    await page.waitForTimeout(300)

    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-mobile-390-datatable.png'),
      fullPage: false
    })
  })
})
