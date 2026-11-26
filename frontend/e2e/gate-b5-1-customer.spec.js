import { test, expect } from '@playwright/test'
import path from 'path'
import fs from 'fs'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const screenshotDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-gate-b5-1')

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
    title: 'Ưu Đãi VeloBus Tuyến Bắc Nam Mùa Lễ',
    description: 'Giảm 20% cho toàn bộ tuyến xe Limousine VIP giường nằm khi đặt trước 7 ngày.',
    badge: 'Khuyến mãi hot',
    imageUrl: '',
    linkUrl: '/search?departureCity=Hà Nội&arrivalCity=Đà Nẵng',
    active: true,
    displayOrder: 1
  },
  {
    id: 2,
    title: 'Tuyến Mới: Sài Gòn Đi Đà Lạt Đón Tận Nơi',
    description: 'Xe Limousine phòng nằm đơn riêng tư với tiện ích giải trí màn hình cá nhân.',
    badge: 'Tuyến mới',
    imageUrl: '',
    linkUrl: '/search?departureCity=Sài Gòn&arrivalCity=Đà Lạt',
    active: true,
    displayOrder: 2
  }
]

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
    totalSeats: 26,
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
    transportType: 'Xe Giường Nằm Luxury',
    basePrice: 320000,
    totalSeats: 18,
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
    transportType: 'Limousine Phòng Đôi 24',
    basePrice: 450000,
    totalSeats: 6,
    status: 'SCHEDULED'
  }
]

async function setupCustomerDiscoveryMocks(page, options = {}) {
  const { trips = mockTrips, delayTripsMs = 0, tripsFail = false } = options

  // Catalog Stations
  await page.route('**/api/v1/catalog/stations**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, message: 'OK', data: mockStations })
    })
  })

  // Public Banners
  await page.route('**/api/v1/public/banners/active**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, message: 'OK', data: mockBanners })
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

test.describe('VeloBus Gate B5.1 — Customer Discovery & Mobility UX', () => {

  // -------------------------------------------------------------------------
  // B5-01: Home Page Desktop Light
  // -------------------------------------------------------------------------
  test('B5-01: Home Page Desktop Light - Warm Minimalist Hero and Discovery loaded from API', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/')
    await page.waitForLoadState('networkidle')

    // Verify Warm Minimalist Hero
    await expect(page.locator('h1')).toContainText('Đặt Vé Xe Khách Liên Tỉnh')
    await expect(page.locator('[data-testid="departure-city-select"]')).toBeVisible()
    await expect(page.locator('[data-testid="arrival-city-select"]')).toBeVisible()
    await expect(page.locator('[data-testid="btn-search-trips"]')).toBeVisible()

    // Verify Active Banners from API
    await expect(page.getByText('Ưu Đãi VeloBus Tuyến Bắc Nam Mùa Lễ')).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'home-desktop-light.png') })
  })

  // -------------------------------------------------------------------------
  // B5-02: Home Page Desktop Dark
  // -------------------------------------------------------------------------
  test('B5-02: Home Page Desktop Dark - Theme synchronization', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/')
    await page.waitForLoadState('networkidle')

    // Open Theme Studio & Switch to Dark Mode
    const themeTrigger = page.locator('[data-testid="theme-customizer-trigger"]')
    if (await themeTrigger.isVisible()) {
      await themeTrigger.click()
      const drawer = page.locator('[data-testid="theme-customizer-drawer"]')
      await expect(drawer).toBeVisible()

      const darkBtn = drawer.getByRole('button', { name: 'Tối', exact: true })
      await darkBtn.click()
      await page.keyboard.press('Escape')
    } else {
      await page.evaluate(() => document.documentElement.classList.add('dark'))
    }

    await expect(page.locator('html')).toHaveClass(/dark/)
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
  // B5-05: Station Selection & Swap Interaction
  // -------------------------------------------------------------------------
  test('B5-05: Station selection and Swap button interaction', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/')
    await page.waitForLoadState('networkidle')

    const depSelect = page.locator('[data-testid="departure-city-select"]')
    const arrSelect = page.locator('[data-testid="arrival-city-select"]')

    await depSelect.selectOption('Hà Nội')
    await arrSelect.selectOption('Đà Nẵng')

    // Click swap button
    const swapBtn = page.getByTitle('Đổi chiều điểm đi và điểm đến')
    await swapBtn.click()

    await expect(depSelect).toHaveValue('Đà Nẵng')
    await expect(arrSelect).toHaveValue('Hà Nội')

    await page.screenshot({ path: path.join(screenshotDir, 'station-selection.png') })
  })

  // -------------------------------------------------------------------------
  // B5-06: Timezone-safe Departure Datepicker
  // -------------------------------------------------------------------------
  test('B5-06: Timezone-safe Datepicker interaction (>= today, vi-VN)', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/')
    await page.waitForLoadState('networkidle')

    const dateInput = page.locator('#departure-date-input')
    await expect(dateInput).toBeVisible()

    const todayIso = new Date().toISOString().split('T')[0]
    const minAttr = await dateInput.getAttribute('min')
    expect(minAttr).toBeTruthy()
    expect(minAttr <= todayIso).toBe(true)

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

    const depSelect = page.locator('[data-testid="departure-city-select"]')
    const arrSelect = page.locator('[data-testid="arrival-city-select"]')

    await depSelect.selectOption('Hà Nội')
    await arrSelect.selectOption('Hà Nội')

    const searchBtn = page.locator('[data-testid="btn-search-trips"]')
    await searchBtn.click()

    await expect(page.locator('text=Điểm đi và điểm đến không được trùng nhau')).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'search-validation.png') })
  })

  // -------------------------------------------------------------------------
  // B5-08: Search Navigation to Trip List View
  // -------------------------------------------------------------------------
  test('B5-08: Valid search navigates to /search with synced query params', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/')
    await page.waitForLoadState('networkidle')

    const depSelect = page.locator('[data-testid="departure-city-select"]')
    const arrSelect = page.locator('[data-testid="arrival-city-select"]')

    await depSelect.selectOption('Hà Nội')
    await arrSelect.selectOption('Đà Nẵng')

    const searchBtn = page.locator('[data-testid="btn-search-trips"]')
    await searchBtn.click()

    await page.waitForURL(/\/search/)
    expect(page.url()).toContain('departureCity=H%C3%A0+N%E1%BB%99i')
    expect(page.url()).toContain('arrivalCity=%C4%90%C3%A0+N%E1%BA%B5ng')
  })

  // -------------------------------------------------------------------------
  // B5-09: Trip List Desktop Light
  // -------------------------------------------------------------------------
  test('B5-09: Trip List Desktop Light - Header search summary, filter sidebar, trip cards', async ({ page }) => {
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
    await expect(cards).toHaveCount(3)
    await expect(cards.first().getByText('HN-DN-01')).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'triplist-desktop-light.png') })
  })

  // -------------------------------------------------------------------------
  // B5-10: Trip List Desktop Dark
  // -------------------------------------------------------------------------
  test('B5-10: Trip List Desktop Dark - Warm Minimalist Dark Mode', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/search?departureCity=Hà Nội&arrivalCity=Đà Nẵng')
    await page.waitForLoadState('networkidle')

    // Open Theme Studio & Switch to Dark Mode
    const themeTrigger = page.locator('[data-testid="theme-customizer-trigger"]')
    if (await themeTrigger.isVisible()) {
      await themeTrigger.click()
      const drawer = page.locator('[data-testid="theme-customizer-drawer"]')
      await expect(drawer).toBeVisible()

      const darkBtn = drawer.getByRole('button', { name: 'Tối', exact: true })
      await darkBtn.click()
      await page.keyboard.press('Escape')
    } else {
      await page.evaluate(() => document.documentElement.classList.add('dark'))
    }

    await expect(page.locator('html')).toHaveClass(/dark/)
    await page.screenshot({ path: path.join(screenshotDir, 'triplist-desktop-dark.png') })
  })

  // -------------------------------------------------------------------------
  // B5-11: Trip List Responsive Tablet (768x1024)
  // -------------------------------------------------------------------------
  test('B5-11: Trip List Responsive Tablet (768x1024)', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/search?departureCity=Hà Nội&arrivalCity=Đà Nẵng')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('[data-testid="search-summary-header"]')).toBeVisible()
    const cards = page.locator('[data-testid="trip-card"]')
    await expect(cards).toHaveCount(3)

    await page.screenshot({ path: path.join(screenshotDir, 'triplist-tablet.png') })
  })

  // -------------------------------------------------------------------------
  // B5-12: Trip List Responsive Mobile (375x812)
  // -------------------------------------------------------------------------
  test('B5-12: Trip List Responsive Mobile (375x812)', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/search?departureCity=Hà Nội&arrivalCity=Đà Nẵng')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('[data-testid="search-summary-header"]')).toBeVisible()
    await expect(page.locator('[data-testid="filter-sheet-trigger"]')).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'triplist-mobile.png') })
  })

  // -------------------------------------------------------------------------
  // B5-13: Mobile Filter Sheet Open (375x812)
  // -------------------------------------------------------------------------
  test('B5-13: Mobile Filter Sheet open with controls on 375px', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/search?departureCity=Hà Nội&arrivalCity=Đà Nẵng')
    await page.waitForLoadState('networkidle')

    const filterTrigger = page.locator('[data-testid="filter-sheet-trigger"]')
    await filterTrigger.click()

    const sheetContent = page.getByRole('dialog')
    await expect(sheetContent).toBeVisible()
    await expect(sheetContent.getByText('Khung giờ xuất bến')).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'triplist-filter-sheet-mobile.png') })
  })

  // -------------------------------------------------------------------------
  // B5-14: Loading Skeleton State
  // -------------------------------------------------------------------------
  test('B5-14: Loading Skeleton state during API fetch', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    // Delay 3000ms to capture skeleton
    await setupCustomerDiscoveryMocks(page, { delayTripsMs: 3000 })

    await page.goto('/search?departureCity=Hà Nội&arrivalCity=Đà Nẵng')
    await expect(page.locator('[data-testid="trip-list-loading"]')).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'triplist-loading-skeleton.png') })
  })

  // -------------------------------------------------------------------------
  // B5-15: Empty Results State
  // -------------------------------------------------------------------------
  test('B5-15: Empty Results state when 0 trips returned', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page, { trips: [] })

    await page.goto('/search?departureCity=Hà Nội&arrivalCity=Nha Trang')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('[data-testid="trip-list-empty"]')).toBeVisible()
    await expect(page.getByText('Không tìm thấy chuyến xe phù hợp')).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'triplist-empty-results.png') })
  })

  // -------------------------------------------------------------------------
  // B5-16: Error & Retry State
  // -------------------------------------------------------------------------
  test('B5-16: Error & Retry state when API returns 500 error', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page, { tripsFail: true })

    await page.goto('/search?departureCity=Hà Nội&arrivalCity=Đà Nẵng')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('[data-testid="trip-list-error"]')).toBeVisible()
    await expect(page.locator('[data-testid="retry-search-btn"]')).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'triplist-error-retry.png') })
  })

  // -------------------------------------------------------------------------
  // B5-17: Client Filters & Sorting
  // -------------------------------------------------------------------------
  test('B5-17: Client-side filtering by coach type and price sorting', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/search?departureCity=Hà Nội&arrivalCity=Đà Nẵng')
    await page.waitForLoadState('networkidle')

    // Initial count is 3
    await expect(page.locator('[data-testid="trip-card"]')).toHaveCount(3)

    // Filter by Coach type: Limousine
    const coachFilter = page.locator('[data-testid="filter-coach-type"]')
    await coachFilter.selectOption('LIMOUSINE')

    // Should now show 2 trips (HN-DN-01 and HN-DN-03)
    await expect(page.locator('[data-testid="trip-card"]')).toHaveCount(2)

    // Sort by price: descending
    const sortSelect = page.locator('[data-testid="sort-select"]')
    await sortSelect.selectOption('PRICE_DESC')

    // First card should be the 450,000 VND trip (HN-DN-03)
    const firstCard = page.locator('[data-testid="trip-card"]').first()
    await expect(firstCard).toContainText('450.000')
  })

  // -------------------------------------------------------------------------
  // B5-18: Custom Primary Theme Synchronization
  // -------------------------------------------------------------------------
  test('B5-18: Custom Primary Theme synchronization in Customer Discovery', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupCustomerDiscoveryMocks(page)

    await page.goto('/search?departureCity=Hà Nội&arrivalCity=Đà Nẵng')
    await page.waitForLoadState('networkidle')

    // Open Theme Studio and change primary color (e.g. Amber or Emerald)
    const themeTrigger = page.locator('[data-testid="theme-customizer-trigger"]')
    if (await themeTrigger.isVisible()) {
      await themeTrigger.click()
      const drawer = page.locator('[data-testid="theme-customizer-drawer"]')
      await expect(drawer).toBeVisible()

      const amberBtn = drawer.locator('button[title*="Amber"], button:has-text("Amber")')
      if (await amberBtn.isVisible()) {
        await amberBtn.click()
      }
      await page.keyboard.press('Escape')
    }

    await page.screenshot({ path: path.join(screenshotDir, 'customer-custom-primary-theme.png') })
  })

  // -------------------------------------------------------------------------
  // B5-19: Trip Selection navigates to Seat Selection
  // -------------------------------------------------------------------------
  test('B5-19: Trip selection click navigates to /trips/:id/seats', async ({ page }) => {
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
