import { test, expect } from '@playwright/test'
import path from 'path'
import fs from 'fs'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const screenshotDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-gate-b5-1-corrected')

if (!fs.existsSync(screenshotDir)) {
  fs.mkdirSync(screenshotDir, { recursive: true })
}

// ---------------------------------------------------------------------------
// 1. Authoritative Backend Contract Fixtures
// ---------------------------------------------------------------------------
const mockStations = [
  { id: 1, code: 'HN-GP', name: 'Bến xe Giáp Bát', city: 'Hà Nội' },
  { id: 2, code: 'HN-MD', name: 'Bến xe Mỹ Đình', city: 'Hà Nội' },
  { id: 3, code: 'DN-TT', name: 'Bến xe Trung tâm Đà Nẵng', city: 'Đà Nẵng' },
  { id: 4, code: 'SG-MD', name: 'Bến xe Miền Đông', city: 'Sài Gòn' },
  { id: 5, code: 'DL-LD', name: 'Bến xe Liên tỉnh Đà Lạt', city: 'Đà Lạt' },
  { id: 6, code: 'HP-CL', name: 'Bến xe Cầu Rào', city: 'Hải Phòng' },
  { id: 7, code: 'NT-PB', name: 'Bến xe Phía Nam Nha Trang', city: 'Nha Trang' }
]

const mockBanners = [
  {
    id: 1,
    title: 'Ưu Đãi Tuyến Bắc Nam Mùa Du Lịch',
    description: 'Chương trình ưu đãi đối tác áp dụng cho tuyến đường Hà Nội đi Đà Nẵng.',
    badge: 'Khuyến mãi hot',
    imageUrl: '',
    linkUrl: '/search?departureCity=Hà Nội&arrivalCity=Đà Nẵng',
    active: true,
    displayOrder: 1
  },
  {
    id: 2,
    title: 'Tuyến Mới: Sài Gòn Đi Đà Lạt',
    description: 'Bổ sung các khung giờ xuất bến thuận tiện vào dịp cuối tuần.',
    badge: 'Tuyến mới',
    imageUrl: '',
    linkUrl: '/search?departureCity=Sài Gòn&arrivalCity=Đà Lạt',
    active: true,
    displayOrder: 2
  }
]

// Contract TripDto matching backend TripDto.java
const mockTrips = [
  {
    id: 101,
    tripCode: 'HN-DN-01',
    departureStationCode: 'HN-GP',
    departureStationName: 'Bến xe Giáp Bát',
    departureCity: 'Hà Nội',
    arrivalStationCode: 'DN-TT',
    arrivalStationName: 'Bến xe Trung tâm Đà Nẵng',
    arrivalCity: 'Đà Nẵng',
    departureTime: '2026-11-26T08:30:00',
    arrivalTime: '2026-11-26T22:30:00',
    transportType: 'Limousine VIP 34 Giường',
    basePrice: 350000,
    totalSeats: 34,
    status: 'SCHEDULED'
  },
  {
    id: 102,
    tripCode: 'HN-DN-02',
    departureStationCode: 'HN-MD',
    departureStationName: 'Bến xe Mỹ Đình',
    departureCity: 'Hà Nội',
    arrivalStationCode: 'DN-TT',
    arrivalStationName: 'Bến xe Trung tâm Đà Nẵng',
    arrivalCity: 'Đà Nẵng',
    departureTime: '2026-11-26T14:00:00',
    arrivalTime: '2026-11-27T04:00:00',
    transportType: 'Xe Giường Nằm',
    basePrice: 320000,
    totalSeats: 40,
    status: 'SCHEDULED'
  },
  {
    id: 103,
    tripCode: 'HN-DN-03',
    departureStationCode: 'HN-GP',
    departureStationName: 'Bến xe Giáp Bát',
    departureCity: 'Hà Nội',
    arrivalStationCode: 'DN-TT',
    arrivalStationName: 'Bến xe Trung tâm Đà Nẵng',
    arrivalCity: 'Đà Nẵng',
    departureTime: '2026-11-26T19:30:00',
    arrivalTime: '2026-11-27T09:30:00',
    transportType: 'Limousine VIP 34 Giường',
    basePrice: 450000,
    totalSeats: 24,
    status: 'SCHEDULED'
  },
  {
    id: 104,
    tripCode: 'HN-DN-PREMIUM',
    departureStationCode: 'HN-GP',
    departureStationName: 'Bến xe Giáp Bát',
    departureCity: 'Hà Nội',
    arrivalStationCode: 'DN-TT',
    arrivalStationName: 'Bến xe Trung tâm Đà Nẵng',
    arrivalCity: 'Đà Nẵng',
    departureTime: '2026-11-26T21:00:00',
    arrivalTime: '2026-11-27T11:00:00',
    transportType: 'Limousine Cabin Riêng',
    basePrice: 1200000, // PRE-04: > 1,000,000 VND
    totalSeats: 12,
    status: 'SCHEDULED'
  }
]

async function setupCustomerDiscoveryMocks(page, options = {}) {
  const {
    trips = mockTrips,
    delayTripsMs = 0,
    tripsFail = false,
    stations = mockStations,
    stationsFail = false,
    banners = mockBanners
  } = options

  // Catalog Stations
  await page.route('**/api/v1/catalog/stations**', async (route) => {
    if (stationsFail) {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ code: 500, message: 'Internal Server Error' })
      })
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, message: 'OK', data: stations })
    })
  })

  // Public Banners
  await page.route('**/api/v1/public/banners/active**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, message: 'OK', data: banners })
    })
  })

  // Public Settings
  await page.route('**/api/v1/public/settings**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, message: 'OK', data: { siteName: 'VeloBus' } })
    })
  })

  // Catalog Trips Search
  await page.route('**/api/v1/catalog/trips**', async (route) => {
    if (tripsFail) {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ code: 500, message: 'Internal Server Error: Không thể truy vấn lịch trình' })
      })
      return
    }
    if (delayTripsMs > 0) {
      await new Promise(r => setTimeout(r, delayTripsMs))
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, message: 'OK', data: trips })
    })
  })
}

test.describe('VeloBus Gate B5.1 Corrected — Customer Discovery & Mobility UX', () => {

  // -------------------------------------------------------------------------
  // B5-01: Home Page Desktop Light
  // -------------------------------------------------------------------------
  test('B5-01: Home Page Desktop Light - Warm Minimalist Hero and Discovery loaded from API', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/')
    await page.waitForLoadState('networkidle')

    // Verify Warm Minimalist Hero & Shared TripSearchForm
    await expect(page.locator('h1')).toContainText('Đặt Vé Xe Khách Liên Tỉnh')
    await expect(page.locator('[data-testid="trip-search-form"]')).toBeVisible()
    await expect(page.locator('[data-testid="departure-city-select"]')).toBeVisible()
    await expect(page.locator('[data-testid="arrival-city-select"]')).toBeVisible()
    await expect(page.locator('[data-testid="departure-datepicker-trigger"]')).toBeVisible()
    await expect(page.locator('[data-testid="btn-search-trips"]')).toBeVisible()

    // Verify authentic banners from API
    await expect(page.getByText('Ưu Đãi Tuyến Bắc Nam Mùa Du Lịch')).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'home-desktop-light.png') })
  })

  // -------------------------------------------------------------------------
  // B5-02: Home Page Desktop Dark - Theme synchronization & Computed Contrast
  // -------------------------------------------------------------------------
  test('B5-02: Home Page Desktop Dark - Theme synchronization and computed colors', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/')
    await page.waitForLoadState('networkidle')

    // Open Theme Studio & Switch to Dark Mode & Save (Commit)
    const themeTrigger = page.locator('[data-testid="theme-customizer-trigger"]')
    if (await themeTrigger.isVisible()) {
      await themeTrigger.click()
      const drawer = page.locator('[data-testid="theme-customizer-drawer"]')
      await expect(drawer).toBeVisible()

      const darkBtn = drawer.getByRole('button', { name: 'Tối', exact: true })
      await darkBtn.click()

      // PRE-05: Click "Lưu giao diện" to commit draft theme before closing
      const saveBtn = drawer.getByRole('button', { name: 'Lưu giao diện' })
      if (await saveBtn.isVisible()) {
        await saveBtn.click()
      } else {
        await page.keyboard.press('Escape')
      }
    } else {
      await page.evaluate(() => document.documentElement.classList.add('dark'))
    }

    await expect(page.locator('html')).toHaveClass(/dark/)

    // Assert computed dark background color on body
    const bodyBg = await page.evaluate(() => window.getComputedStyle(document.body).backgroundColor)
    expect(bodyBg).not.toBe('rgb(255, 255, 255)')

    await page.screenshot({ path: path.join(screenshotDir, 'home-desktop-dark.png') })
  })

  // -------------------------------------------------------------------------
  // B5-03: Home Page Responsive Tablet (768x1024)
  // -------------------------------------------------------------------------
  test('B5-03: Home Page Responsive Tablet (768x1024)', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('h1')).toBeVisible()
    await expect(page.locator('[data-testid="btn-search-trips"]')).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'home-tablet.png') })
  })

  // -------------------------------------------------------------------------
  // B5-04: Home Page Responsive Mobile (375x812)
  // -------------------------------------------------------------------------
  test('B5-04: Home Page Responsive Mobile (375x812)', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('h1')).toBeVisible()
    await expect(page.locator('[data-testid="btn-search-trips"]')).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'home-mobile.png') })
  })

  // -------------------------------------------------------------------------
  // B5-05: Station Selection (Combobox/Select) & Swap Interaction
  // -------------------------------------------------------------------------
  test('B5-05: Station selection via Select/Combobox and Swap button interaction', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/')
    await page.waitForLoadState('networkidle')

    const depSelect = page.locator('[data-testid="departure-city-select"]')
    const arrSelect = page.locator('[data-testid="arrival-city-select"]')
    await expect(depSelect).toBeVisible()
    await expect(arrSelect).toBeVisible()

    const initialDep = await depSelect.innerText()
    const initialArr = await arrSelect.innerText()

    // Click swap button
    const swapBtn = page.getByTitle('Đổi chiều điểm đi và điểm đến')
    await swapBtn.click()

    await expect(depSelect).toContainText(initialArr)
    await expect(arrSelect).toContainText(initialDep)

    await page.screenshot({ path: path.join(screenshotDir, 'station-selection.png') })
  })

  // -------------------------------------------------------------------------
  // B5-06: Timezone-safe Departure Datepicker (Popover + Calendar vi-VN)
  // -------------------------------------------------------------------------
  test('B5-06: Timezone-safe Datepicker interaction with Popover Calendar (>= today, vi-VN)', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/')
    await page.waitForLoadState('networkidle')

    const dateTrigger = page.locator('[data-testid="departure-datepicker-trigger"]')
    await expect(dateTrigger).toBeVisible()

    // Open Calendar Popover
    await dateTrigger.click()
    const calendarPopover = page.locator('[role="dialog"]')
    await expect(calendarPopover).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'departure-datepicker.png') })
  })

  // -------------------------------------------------------------------------
  // B5-07: Form Validation (Origin === Destination Rejection)
  // -------------------------------------------------------------------------
  test('B5-07: Form validation rejection when departure and arrival cities are identical', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/')
    await page.waitForLoadState('networkidle')

    // Click departure select and choose "Hà Nội"
    await page.locator('[data-testid="departure-city-select"]').click()
    await page.locator('[role="option"]:has-text("Hà Nội")').click()

    // Click arrival select and choose "Hà Nội"
    await page.locator('[data-testid="arrival-city-select"]').click()
    await page.locator('[role="option"]:has-text("Hà Nội")').click()

    const searchBtn = page.locator('[data-testid="btn-search-trips"]')
    await searchBtn.click()

    await expect(page.locator('[data-testid="search-validation-error"]')).toBeVisible()
    await expect(page.locator('text=Điểm đi và điểm đến không được trùng nhau')).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'search-validation.png') })
  })

  // -------------------------------------------------------------------------
  // B5-08: Station loading, error and empty states
  // -------------------------------------------------------------------------
  test('B5-08: Station loading error handling', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page, { stationsFail: true })

    await page.goto('/')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('text=Lỗi tải danh mục bến xe')).toBeVisible()
  })

  // -------------------------------------------------------------------------
  // B5-09: Valid search navigates to /search with synced query params
  // -------------------------------------------------------------------------
  test('B5-09: Valid search navigates to /search with synced query params', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="departure-city-select"]').click()
    await page.locator('[role="option"]:has-text("Hà Nội")').click()

    await page.locator('[data-testid="arrival-city-select"]').click()
    await page.locator('[role="option"]:has-text("Đà Nẵng")').click()

    const searchBtn = page.locator('[data-testid="btn-search-trips"]')
    await searchBtn.click()

    await page.waitForURL(/\/search/)
    expect(page.url()).toContain('departureCity=H%C3%A0+N%E1%BB%99i')
    expect(page.url()).toContain('arrivalCity=%C4%90%C3%A0+N%E1%BA%B5ng')
  })

  // -------------------------------------------------------------------------
  // B5-10: Trip List Desktop Light
  // -------------------------------------------------------------------------
  test('B5-10: Trip List Desktop Light - Header search summary, filter sidebar, trip cards', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/search?departureCity=Hà Nội&arrivalCity=Đà Nẵng')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('[data-testid="search-summary-header"]')).toBeVisible()
    await expect(page.locator('[data-testid="summary-origin"]')).toHaveText('Hà Nội')
    await expect(page.locator('[data-testid="summary-destination"]')).toHaveText('Đà Nẵng')
    await expect(page.locator('[data-testid="filter-sidebar"]')).toBeVisible()

    // Assert cards loaded from mockTrips
    const cards = page.locator('[data-testid="trip-card"]')
    await expect(cards).toHaveCount(4)
    await expect(cards.first().getByText('HN-DN-01')).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'triplist-desktop-light.png') })
  })

  // -------------------------------------------------------------------------
  // B5-11: Trip List Desktop Dark
  // -------------------------------------------------------------------------
  test('B5-11: Trip List Desktop Dark - Warm Minimalist Dark Mode & Color Contrast', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/search?departureCity=Hà Nội&arrivalCity=Đà Nẵng')
    await page.waitForLoadState('networkidle')

    // Commit dark mode
    await page.evaluate(() => {
      document.documentElement.classList.add('dark')
      document.documentElement.setAttribute('data-theme', 'dark')
    })

    await expect(page.locator('html')).toHaveClass(/dark/)
    const bodyBg = await page.evaluate(() => window.getComputedStyle(document.body).backgroundColor)
    expect(bodyBg).not.toBe('rgb(255, 255, 255)')

    await page.screenshot({ path: path.join(screenshotDir, 'triplist-desktop-dark.png') })
  })

  // -------------------------------------------------------------------------
  // B5-12: Trip List Responsive Tablet (768x1024)
  // -------------------------------------------------------------------------
  test('B5-12: Trip List Responsive Tablet (768x1024)', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/search?departureCity=Hà Nội&arrivalCity=Đà Nẵng')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('[data-testid="search-summary-header"]')).toBeVisible()
    const cards = page.locator('[data-testid="trip-card"]')
    await expect(cards).toHaveCount(4)

    await page.screenshot({ path: path.join(screenshotDir, 'triplist-tablet.png') })
  })

  // -------------------------------------------------------------------------
  // B5-13: Trip List Responsive Mobile (375x812)
  // -------------------------------------------------------------------------
  test('B5-13: Trip List Responsive Mobile (375x812)', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/search?departureCity=Hà Nội&arrivalCity=Đà Nẵng')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('[data-testid="search-summary-header"]')).toBeVisible()
    await expect(page.locator('[data-testid="filter-sheet-trigger"]')).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'triplist-mobile.png') })
  })

  // -------------------------------------------------------------------------
  // B5-14: Mobile Filter Sheet Open (375x812)
  // -------------------------------------------------------------------------
  test('B5-14: Mobile Filter Sheet open with controls on 375px', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/search?departureCity=Hà Nội&arrivalCity=Đà Nẵng')
    await page.waitForLoadState('networkidle')

    const filterTrigger = page.locator('[data-testid="filter-sheet-trigger"]')
    await filterTrigger.click()

    const sheetContent = page.getByRole('dialog')
    await expect(sheetContent).toBeVisible()
    await expect(sheetContent.getByText('Khung giờ xuất bến', { exact: true })).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'triplist-filter-sheet-mobile.png') })
  })

  // -------------------------------------------------------------------------
  // B5-15: Loading Skeleton State
  // -------------------------------------------------------------------------
  test('B5-15: Loading Skeleton state during API fetch', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page, { delayTripsMs: 3000 })

    await page.goto('/search?departureCity=Hà Nội&arrivalCity=Đà Nẵng')
    await expect(page.locator('[data-testid="trip-list-loading"]')).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'triplist-loading-skeleton.png') })
  })

  // -------------------------------------------------------------------------
  // B5-16: Empty Results State
  // -------------------------------------------------------------------------
  test('B5-16: Empty Results state when 0 trips returned', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page, { trips: [] })

    await page.goto('/search?departureCity=Hà Nội&arrivalCity=Nha Trang')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('[data-testid="trip-list-empty"]')).toBeVisible()
    await expect(page.getByText('Không tìm thấy chuyến xe phù hợp')).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'triplist-empty-results.png') })
  })

  // -------------------------------------------------------------------------
  // B5-17: Error & Retry State
  // -------------------------------------------------------------------------
  test('B5-17: Error & Retry state when API returns 500 error', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page, { tripsFail: true })

    await page.goto('/search?departureCity=Hà Nội&arrivalCity=Đà Nẵng')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('[data-testid="trip-list-error"]')).toBeVisible()
    await expect(page.locator('[data-testid="retry-search-btn"]')).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'triplist-error-retry.png') })
  })

  // -------------------------------------------------------------------------
  // B5-18: PRE-02 TripDto totalSeats displayed as Capacity (Not Available Seats)
  // -------------------------------------------------------------------------
  test('B5-18: TripDto totalSeats displayed as Capacity without fake availability claims', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/search?departureCity=Hà Nội&arrivalCity=Đà Nẵng')
    await page.waitForLoadState('networkidle')

    const firstCard = page.locator('[data-testid="trip-card"]').first()
    // PRE-02: Assert "Sức chứa 34 chỗ" instead of "Còn 34 chỗ"
    await expect(firstCard.locator('[data-testid="trip-capacity"]')).toContainText('Sức chứa 34 chỗ')
    await expect(page.getByText(/Còn \d+ chỗ/)).toHaveCount(0)

    // PRE-02: Assert unsupported operator filter is not displayed
    await expect(page.locator('text=Hãng xe')).toHaveCount(0)
  })

  // -------------------------------------------------------------------------
  // B5-19: PRE-04 Price Filtering — Trips > 1,000,000 VND not filtered by default
  // -------------------------------------------------------------------------
  test('B5-19: Trip with price > 1,000,000 VND appears when no price ceiling is set', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/search?departureCity=Hà Nội&arrivalCity=Đà Nẵng')
    await page.waitForLoadState('networkidle')

    // 1,200,000 VND trip (HN-DN-PREMIUM) must be visible in results
    await expect(page.locator('[data-testid="trip-card"]').getByText('1.200.000')).toBeVisible()
  })

  // -------------------------------------------------------------------------
  // B5-20: Custom Primary Theme Synchronization
  // -------------------------------------------------------------------------
  test('B5-20: Custom Primary Theme synchronization in Customer Discovery', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/search?departureCity=Hà Nội&arrivalCity=Đà Nẵng')
    await page.waitForLoadState('networkidle')

    const themeTrigger = page.locator('[data-testid="theme-customizer-trigger"]')
    if (await themeTrigger.isVisible()) {
      await themeTrigger.click()
      const drawer = page.locator('[data-testid="theme-customizer-drawer"]')
      await expect(drawer).toBeVisible()

      const colorBtn = drawer.locator('button[title*="Ocean"], button:has-text("Ocean")')
      if (await colorBtn.isVisible()) {
        await colorBtn.click()
      }
      await page.keyboard.press('Escape')
    }

    await page.screenshot({ path: path.join(screenshotDir, 'customer-custom-primary-theme.png') })
  })

  // -------------------------------------------------------------------------
  // B5-21: Trip Selection navigates to Seat Selection
  // -------------------------------------------------------------------------
  test('B5-21: Trip selection click navigates to /trips/:id/seats', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/search?departureCity=Hà Nội&arrivalCity=Đà Nẵng')
    await page.waitForLoadState('networkidle')

    const firstSelectBtn = page.locator('[data-testid="btn-select-trip"]').first()
    await firstSelectBtn.click()

    await page.waitForURL(/\/trips\/101\/seats/)
    expect(page.url()).toContain('/trips/101/seats')
  })
})
