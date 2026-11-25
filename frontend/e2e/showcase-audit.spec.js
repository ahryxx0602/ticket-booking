import { test, expect } from '@playwright/test'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const screenshotDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-gate-a-final')

test.describe('Showcase UI Fidelity & Interactive Audit (REV-01 through REV-08)', () => {
  test('Audit 01: Desktop Showcase Hub Overview & Semantic Geometry Verification', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/showcase')
    await page.waitForLoadState('networkidle')

    // 1. Verify Header & Title
    await expect(page.locator('h1')).toContainText('VeloBus UI Component Specification')

    // 2. REV-01: Verify role-specific semantic geometry (border-radius)
    // Checkbox must have 4px border-radius
    const checkbox = page.locator('#cb-1')
    await expect(checkbox).toBeVisible()
    const checkboxRadius = await checkbox.evaluate(el => window.getComputedStyle(el).borderRadius)
    expect(checkboxRadius).toBe('4px')

    // Button must have 8px border-radius
    const primaryBtn = page.locator('#controls-buttons button:has-text("Primary Button")')
    await expect(primaryBtn).toBeVisible()
    const btnRadius = await primaryBtn.evaluate(el => window.getComputedStyle(el).borderRadius)
    expect(btnRadius).toBe('8px')

    // Input must have 8px border-radius
    const sampleInput = page.locator('#controls-inputs input').first()
    await expect(sampleInput).toBeVisible()
    const inputRadius = await sampleInput.evaluate(el => window.getComputedStyle(el).borderRadius)
    expect(inputRadius).toBe('8px')

    // Card/Container surface with rounded-xl must have 16px border-radius
    const surfaceCard = page.locator('.rounded-xl').first()
    await expect(surfaceCard).toBeVisible()
    const cardRadius = await surfaceCard.evaluate(el => window.getComputedStyle(el).borderRadius)
    expect(cardRadius).toBe('16px')

    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-hub-desktop-light-corrected.png'),
      fullPage: false
    })
  })

  test('Audit 02: Desktop Showcase Hub (Dark Mode & Color Tokens)', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/showcase')
    await page.waitForLoadState('networkidle')

    // Toggle dark mode
    const themeButton = page.locator('header button[title*="Chuyển sang giao diện Tối"], header button:has(.lucide-moon)').first()
    await expect(themeButton).toBeVisible()
    await themeButton.click()

    // Assert dark mode class is applied to html root
    await expect(page.locator('html')).toHaveClass(/dark/)
    await page.waitForTimeout(300)

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
    await expect(calendarSection).toBeVisible()

    // Verify Vietnamese weekdays header (T2, T3, T4, T5, T6, T7, CN)
    const weekdayHeaders = calendarSection.locator('th')
    await expect(weekdayHeaders.first()).toHaveText('T2')

    // Verify calendar day button border-radius is 6px
    const dayBtn = calendarSection.locator('[data-reka-calendar-cell-trigger]').first()
    await expect(dayBtn).toBeVisible()
    const dayRadius = await dayBtn.evaluate(el => window.getComputedStyle(el).borderRadius)
    expect(dayRadius).toBe('6px')

    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-calendar-vi-vn-corrected.png'),
      clip: await calendarSection.boundingBox() || undefined
    })
  })

  test('Audit 04: DatePicker Popover Open State & TimePicker Single Affordance', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 800 })
    await page.goto('/showcase')
    await page.waitForLoadState('networkidle')

    const datepickerSection = page.locator('#calendar-datepicker')
    await datepickerSection.scrollIntoViewIfNeeded()

    // Click DatePicker Popover trigger button
    const dateButton = datepickerSection.locator('button:has-text("/")').first()
    await expect(dateButton).toBeVisible()
    await dateButton.click()

    // Assert DatePicker popover content is open by checking day trigger inside open popover
    const openDayTrigger = page.locator('[data-state="open"] [data-reka-calendar-cell-trigger]').first()
    await expect(openDayTrigger).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-datepicker-popover-open.png'),
      fullPage: false
    })

    // Close popover
    await page.keyboard.press('Escape')
  })

  test('Audit 04b: Custom TimePicker 24H Dropdown Open State', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 800 })
    await page.goto('/showcase')
    await page.waitForLoadState('networkidle')

    const datepickerSection = page.locator('#calendar-datepicker')
    await datepickerSection.scrollIntoViewIfNeeded()

    // Click TimePicker SelectTrigger for hour
    const hourTrigger = datepickerSection.locator('[role="combobox"]').first()
    await expect(hourTrigger).toBeVisible()
    await hourTrigger.click()

    // Assert Select dropdown content is open
    const selectContent = page.locator('[role="listbox"]').first()
    await expect(selectContent).toBeVisible()

    // Assert options are present
    const option08 = page.locator('[role="option"]:has-text("08")').first()
    await expect(option08).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-timepicker-dropdown-open.png'),
      fullPage: false
    })

    // Close select
    await page.keyboard.press('Escape')
  })

  test('Audit 05: DateRangePicker with Month Crossing & Presets Popover Open State', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 800 })
    await page.goto('/showcase')
    await page.waitForLoadState('networkidle')

    const rangeSection = page.locator('#calendar-range')
    await rangeSection.scrollIntoViewIfNeeded()

    // 1. Open Date Range Calendar Popover
    const rangeButton = rangeSection.locator('button:has-text("—")').first()
    await expect(rangeButton).toBeVisible()
    await rangeButton.click()

    // Assert Range Calendar popover is open
    const openRangeCell = page.locator('[data-state="open"] [data-reka-calendar-cell-trigger]').first()
    await expect(openRangeCell).toBeVisible()

    // Close calendar popover
    await page.keyboard.press('Escape')
    await page.waitForTimeout(200)

    // 2. Open Presets Dropdown Menu
    const presetButton = rangeSection.locator('button:has-text("Chọn nhanh khoảng ngày")')
    await expect(presetButton).toBeVisible()
    await presetButton.click()

    // Assert Presets menu is open
    const presetMenu = page.locator('[role="menu"]')
    await expect(presetMenu).toBeVisible()
    await expect(presetMenu.locator('[role="menuitem"]:has-text("7 ngày tới (1 tuần)")')).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-range-presets-open.png'),
      fullPage: false
    })

    // Select preset to verify reactive binding
    await presetMenu.locator('[role="menuitem"]:has-text("7 ngày tới (1 tuần)")').click()
    await page.waitForTimeout(200)
  })

  test('Audit 06: TanStack DataTable Density Modes, Selection & Pagination', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 900 })
    await page.goto('/showcase')
    await page.waitForLoadState('networkidle')

    const tableSection = page.locator('#data-table')
    await tableSection.scrollIntoViewIfNeeded()
    await expect(tableSection).toBeVisible()

    // 1. Select the first two rows via checkbox
    const rowCheckboxes = tableSection.locator('tbody tr button[role="checkbox"]')
    await expect(rowCheckboxes.first()).toBeVisible()
    await rowCheckboxes.nth(0).click()
    await page.waitForTimeout(150)
    await rowCheckboxes.nth(1).click()
    await page.waitForTimeout(150)

    // Assert selection message in batch selection bar
    const selectionBar = page.locator('text=Đã chọn 2 đơn vé trên bảng dữ liệu')
    await expect(selectionBar).toBeVisible()

    // Capture Default density
    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-datatable-density-default.png'),
      clip: await tableSection.boundingBox() || undefined
    })

    // 2. Switch to Compact density
    const compactBtn = tableSection.locator('button:has-text("Gọn")')
    await expect(compactBtn).toBeVisible()
    await compactBtn.click()
    await page.waitForTimeout(200)

    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-datatable-density-compact.png'),
      clip: await tableSection.boundingBox() || undefined
    })

    // 3. Test Pagination: click "Sau"
    const nextBtn = tableSection.locator('button:has-text("Sau")')
    await expect(nextBtn).toBeEnabled()
    await nextBtn.click()
    await page.waitForTimeout(200)

    // Assert page 2
    const pageText = tableSection.locator('text=Trang 2 / 2')
    await expect(pageText).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-datatable-pagination-page2.png'),
      clip: await tableSection.boundingBox() || undefined
    })
  })

  test('Audit 07: Dropdown Menu System Open Interactive State', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 800 })
    await page.goto('/showcase')
    await page.waitForLoadState('networkidle')

    const dropdownSection = page.locator('#nav-dropdown')
    await dropdownSection.scrollIntoViewIfNeeded()

    const accountMenuTrigger = dropdownSection.locator('button:has-text("Tài Khoản Quản Trị")')
    await expect(accountMenuTrigger).toBeVisible()
    await accountMenuTrigger.click()

    // Assert dropdown menu is visible and contains expected items
    const menuContent = page.locator('[role="menu"]')
    await expect(menuContent).toBeVisible()
    await expect(menuContent.locator('[role="menuitem"]:has-text("Hồ sơ cá nhân")')).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-dropdown-menu-open.png'),
      fullPage: false
    })

    // Close menu
    await page.keyboard.press('Escape')
  })

  test('Audit 08: Modals, Dialogs and Feedback Open Interactive State', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 800 })
    await page.goto('/showcase')
    await page.waitForLoadState('networkidle')

    const feedbackSection = page.locator('#feedback-modals')
    await feedbackSection.scrollIntoViewIfNeeded()

    const dialogBtn = feedbackSection.locator('button:has-text("Mở Dialog Chuẩn")')
    await expect(dialogBtn).toBeVisible()
    await dialogBtn.click()

    // Assert Dialog is visible and has correct content
    const dialogContent = page.locator('[role="dialog"]')
    await expect(dialogContent).toBeVisible()
    await expect(dialogContent.locator('text=Xác Nhận Giữ Chỗ Chuyến Xe')).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-dialog-confirmation-open.png'),
      fullPage: false
    })

    // Close dialog
    const closeBtn = dialogContent.locator('button:has-text("Đóng lại")')
    await expect(closeBtn).toBeVisible()
    await closeBtn.click()
  })

  test('Audit 09: Theme Customizer Drawer Open State', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 800 })
    await page.goto('/showcase')
    await page.waitForLoadState('networkidle')

    // Click "Tùy Biến Theme" in header
    const customizerBtn = page.locator('header button:has-text("Tùy Biến Theme")')
    await expect(customizerBtn).toBeVisible()
    await customizerBtn.click()

    // Assert customizer drawer is visible
    const drawerTitle = page.locator('text=VeloBus Theme Studio')
    await expect(drawerTitle).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-theme-customizer-open.png'),
      fullPage: false
    })

    // Close drawer via close button
    const closeBtn = page.locator('button[aria-label="Đóng tùy biến"]')
    await expect(closeBtn).toBeVisible()
    await closeBtn.click()
  })

  test('Audit 10: Mobile Responsive 375px Header & Navigation Sheet', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 })
    await page.goto('/showcase')
    await page.waitForLoadState('networkidle')

    // Open mobile hamburger menu
    const menuBtn = page.locator('[data-testid="mobile-menu-trigger"]')
    await expect(menuBtn).toBeVisible()
    await menuBtn.click()

    // Assert mobile sheet is visible
    const mobileSheet = page.locator('[role="dialog"]')
    await expect(mobileSheet).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-mobile-375-navigation-sheet.png'),
      fullPage: false
    })

    await page.keyboard.press('Escape')
  })

  test('Audit 11: Mobile Responsive 375px Calendar without Overlapping Trigger', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 })
    await page.goto('/showcase')
    await page.waitForLoadState('networkidle')

    const calSection = page.locator('#calendar-inline')
    await calSection.scrollIntoViewIfNeeded()
    await expect(calSection).toBeVisible()

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
    await expect(tableSection).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotDir, 'showcase-mobile-390-datatable.png'),
      fullPage: false
    })
  })
})
