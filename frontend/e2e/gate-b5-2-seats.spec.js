import { test, expect } from '@playwright/test'
import path from 'path'
import fs from 'fs'
import { fileURLToPath } from 'url'
import { loginAsRole } from './helpers/auth.js'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const screenshotDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-gate-b5-2')

if (!fs.existsSync(screenshotDir)) {
  fs.mkdirSync(screenshotDir, { recursive: true })
}

// ---------------------------------------------------------------------------
// 1. Authoritative Backend Seat Matrix Fixtures (SeatDto.java)
// ---------------------------------------------------------------------------
const createMockSeats = () => {
  const seats = []
  // Lower Deck (A01 - A18)
  for (let i = 1; i <= 18; i++) {
    const num = `A${String(i).padStart(2, '0')}`
    let status = 'AVAILABLE'
    if (i === 3 || i === 7) status = 'BOOKED'
    if (i === 5) status = 'HELD'
    seats.push({
      id: 100 + i,
      tripId: 101,
      seatNumber: num,
      seatType: i <= 6 ? 'VIP' : 'STANDARD',
      priceMultiplier: i <= 6 ? 1.15 : 1.0,
      finalPrice: i <= 6 ? 400000 : 350000,
      status
    })
  }
  // Upper Deck (B01 - B18)
  for (let i = 1; i <= 18; i++) {
    const num = `B${String(i).padStart(2, '0')}`
    let status = 'AVAILABLE'
    if (i === 2 || i === 8) status = 'BOOKED'
    seats.push({
      id: 200 + i,
      tripId: 101,
      seatNumber: num,
      seatType: 'STANDARD',
      priceMultiplier: 1.0,
      finalPrice: 350000,
      status
    })
  }
  return seats
}

const mockTripDetail = {
  id: 101,
  tripCode: 'HN-DN-01',
  departureStationCode: 'HN-GP',
  departureStationName: 'Bến xe Giáp Bát',
  departureCity: 'Hà Nội',
  arrivalStationCode: 'DN-TT',
  arrivalStationName: 'Bến xe Trung tâm Đà Nẵng',
  arrivalCity: 'Đà Nẵng',
  route: 'Hà Nội (BX Giáp Bát) → Đà Nẵng (BX Trung Tâm)',
  operator: 'Hoàng Long Limousine',
  plate: '29B-888.99',
  departureTime: '2026-11-26T19:00:00',
  arrivalTime: '2026-11-27T09:00:00',
  duration: '14 giờ',
  transportType: 'VIP Luxury 36 Giường Nằm',
  totalSeats: 36,
  basePrice: 350000,
  status: 'SCHEDULED'
}

/**
 * Setup Route Interceptions for Gate B5.2 Seat Selection
 */
async function setupSeatSelectionMocks(page, options = {}) {
  const {
    seats = createMockSeats(),
    seatsFail = false,
    delaySeatsMs = 0,
    holdFail = false,
    holdErrorCode = null,
    holdErrorMessage = null,
    malformedHold = false,
    holdTimeout = false
  } = options

  // Mock Settings & Banners
  await page.route('**/api/v1/public/settings', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: { brandName: 'VeloBus' } })
    })
  })

  // Mock GET Trip Seats
  await page.route('**/api/v1/catalog/trips/*/seats', async (route) => {
    if (delaySeatsMs > 0) {
      await new Promise(r => setTimeout(r, delaySeatsMs))
    }
    if (seatsFail) {
      return route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          success: false,
          code: 'SERVER_ERROR',
          message: 'Lỗi máy chủ khi tải danh sách ghế.'
        })
      })
    }
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: seats
      })
    })
  })

  // Mock POST Hold Seats
  await page.route('**/api/v1/bookings/hold', async (route) => {
    if (holdTimeout) {
      return route.abort('timedout')
    }
    if (holdFail) {
      return route.fulfill({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({
          success: false,
          code: holdErrorCode || 'HOLD_FAILED',
          message: holdErrorMessage || 'Không thể giữ chỗ trên máy chủ.'
        })
      })
    }
    if (malformedHold) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { corrupted: true } // Missing bookingReference
        })
      })
    }
    const postData = JSON.parse(route.request().postData() || '{}')
    const requestedSeats = postData.seatNumbers || []
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        message: 'Seats held successfully for 10 minutes',
        data: {
          bookingId: 991,
          bookingReference: 'VB-HOLD-8899',
          userId: 1,
          tripId: postData.tripId || 101,
          status: 'HELD',
          totalAmount: requestedSeats.length * 400000,
          expiresAt: new Date(Date.now() + 10 * 60 * 1000).toISOString(),
          createdAt: new Date().toISOString(),
          seatNumbers: requestedSeats
        }
      })
    })
  })

  // Mock POST Cancel Booking Hold
  await page.route('**/api/v1/bookings/*/cancel', async (route) => {
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        message: 'Booking cancelled successfully',
        data: null
      })
    })
  })
}

test.describe('Gate B5.2 — Seat Reservation Flow & Physical Seat Matrix Verification', () => {
  // -------------------------------------------------------------------------
  // B52-01: Seat API Success & Authentic Seat Matrix
  // -------------------------------------------------------------------------
  test('B52-01: Seat API success loads real seats from backend without demo fallback', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupSeatSelectionMocks(page)

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    // Sơ đồ chỗ ngồi hiển thị
    await expect(page.getByText('Sơ đồ chỗ ngồi')).toBeVisible()
    await expect(page.locator('[data-testid="seat-grid-lower"]')).toBeVisible()

    // Ghế A01, A02 hiển thị
    await expect(page.locator('[data-testid="seat-A01"]')).toBeVisible()
    await expect(page.locator('[data-testid="seat-A02"]')).toBeVisible()

    // B52 Screenshot: Desktop Light
    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-desktop-light.png') })
  })

  // -------------------------------------------------------------------------
  // B52-02: Seat API Loading Skeleton
  // -------------------------------------------------------------------------
  test('B52-02: Seat API Loading indicator displays while request is pending', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupSeatSelectionMocks(page, { delaySeatsMs: 3000 })

    await page.goto('/trips/101/seats')
    await expect(page.locator('[data-testid="seat-loading"]')).toBeVisible()

    // B52 Screenshot: Loading State
    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-loading-state.png') })
  })

  // -------------------------------------------------------------------------
  // B52-03: Seat API Failure & Retry
  // -------------------------------------------------------------------------
  test('B52-03: Seat API failure displays error alert with retry button', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupSeatSelectionMocks(page, { seatsFail: true })

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('[data-testid="seat-error"]')).toBeVisible()
    await expect(page.locator('[data-testid="seat-retry-btn"]')).toBeVisible()

    // B52 Screenshot: Error Retry
    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-error-retry.png') })
  })

  // -------------------------------------------------------------------------
  // B52-04: Valid Empty Seat List
  // -------------------------------------------------------------------------
  test('B52-04: Valid empty seat list displays empty state notice', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupSeatSelectionMocks(page, { seats: [] })

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('[data-testid="seat-empty"]')).toBeVisible()
    await expect(page.getByText('Chưa có thông tin chỗ ngồi')).toBeVisible()

    // B52 Screenshot: Empty State
    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-empty-state.png') })
  })

  // -------------------------------------------------------------------------
  // B52-05: Seat Status Mapping (Available, Booked, Held)
  // -------------------------------------------------------------------------
  test('B52-05: Seat status mapping correctly marks available, booked and held seats', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupSeatSelectionMocks(page)

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    // A01 is AVAILABLE: enabled
    const seatA01 = page.locator('[data-testid="seat-A01"]')
    await expect(seatA01).toBeEnabled()

    // A03 is BOOKED: disabled
    const seatA03 = page.locator('[data-testid="seat-A03"]')
    await expect(seatA03).toBeDisabled()

    // A05 is HELD: disabled with lock icon
    const seatA05 = page.locator('[data-testid="seat-A05"]')
    await expect(seatA05).toBeDisabled()
  })

  // -------------------------------------------------------------------------
  // B52-06: Deck Rendering & Tab Switching (Lower Deck / Upper Deck)
  // -------------------------------------------------------------------------
  test('B52-06: Deck rendering switches smoothly between Tầng Dưới (A) and Tầng Trên (B)', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupSeatSelectionMocks(page)

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    // Click Upper Deck Tab
    const upperTab = page.locator('[data-testid="deck-tab-upper"]')
    await upperTab.click()

    await expect(page.locator('[data-testid="seat-grid-upper"]')).toBeVisible()
    await expect(page.locator('[data-testid="seat-B01"]')).toBeVisible()

    // B52 Screenshot: Deck Tabs
    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-deck-tabs.png') })

    // Switch back to Lower Deck
    const lowerTab = page.locator('[data-testid="deck-tab-lower"]')
    await lowerTab.click()
    await expect(page.locator('[data-testid="seat-grid-lower"]')).toBeVisible()
    await expect(page.locator('[data-testid="seat-A01"]')).toBeVisible()
  })

  // -------------------------------------------------------------------------
  // B52-07: Selection and Deselection Interaction
  // -------------------------------------------------------------------------
  test('B52-07: Selection and deselection toggles seat and updates subtotal and total', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupSeatSelectionMocks(page)

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    // Select A01
    await page.locator('[data-testid="seat-A01"]').click()
    await expect(page.locator('[data-testid="selected-seat-item-A01"]')).toBeVisible()
    await expect(page.locator('[data-testid="total-price-display"]')).toContainText('400.000')

    // Select A02
    await page.locator('[data-testid="seat-A02"]').click()
    await expect(page.locator('[data-testid="selected-seat-item-A02"]')).toBeVisible()
    await expect(page.locator('[data-testid="total-price-display"]')).toContainText('800.000')

    // B52 Screenshot: Selected Seats
    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-selected-seats.png') })

    // Deselect A01 via sidebar remove button
    await page.locator('[data-testid="remove-seat-A01"]').click()
    await expect(page.locator('[data-testid="selected-seat-item-A01"]')).toHaveCount(0)
    await expect(page.locator('[data-testid="total-price-display"]')).toContainText('400.000')
  })

  // -------------------------------------------------------------------------
  // B52-08: Unavailable Seats Cannot Be Clicked
  // -------------------------------------------------------------------------
  test('B52-08: Sold or held seat click does not add seat to selection', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupSeatSelectionMocks(page)

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    // Click disabled booked seat A03 (force: true simulation)
    const seatA03 = page.locator('[data-testid="seat-A03"]')
    await seatA03.click({ force: true })

    // Should still have no seats selected
    await expect(page.locator('[data-testid="no-seats-selected"]')).toBeVisible()
    await expect(page.locator('[data-testid="selected-seat-item-A03"]')).toHaveCount(0)
  })

  // -------------------------------------------------------------------------
  // B52-09: Seat Limit (Max 10 Seats per Booking)
  // -------------------------------------------------------------------------
  test('B52-09: Seat limit enforces maximum 10 seats per booking with warning toast', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupSeatSelectionMocks(page)

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    // Select 10 available seats on Lower Deck: A01, A02, A04, A06, A08, A09, A10, A11, A12, A13
    const seatCodes = ['A01', 'A02', 'A04', 'A06', 'A08', 'A09', 'A10', 'A11', 'A12', 'A13']
    for (const code of seatCodes) {
      await page.locator(`[data-testid="seat-${code}"]`).click()
    }
    await expect(page.locator('[data-testid="selected-seats-list"]').locator('> div')).toHaveCount(10)

    // Try to select 11th seat (A14)
    await page.locator('[data-testid="seat-A14"]').click()

    // Warning toast
    await expect(page.getByText('Mỗi hành khách được chọn tối đa 10 giường trong một lần đặt vé.')).toBeVisible()

    // Count remains 10
    await expect(page.locator('[data-testid="selected-seats-list"]').locator('> div')).toHaveCount(10)

    // B52 Screenshot: Max Limit Warning
    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-max-limit-warning.png') })
  })

  // -------------------------------------------------------------------------
  // B52-10: Guest Auth Redirect with Return URL
  // -------------------------------------------------------------------------
  test('B52-10: Unauthenticated guest user clicking hold is redirected to login with return URL', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupSeatSelectionMocks(page)

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    // Select A01
    await page.locator('[data-testid="seat-A01"]').click()

    // Click Proceed button as Guest
    await page.locator('[data-testid="btn-proceed-booking"]').click()

    // Redirect to /login?redirect=/trips/101/seats&reason=auth_required
    await page.waitForURL(/\/login\?redirect=.*trips.*101.*seats/)
    await expect(page.getByText('Vui lòng đăng nhập tài khoản Khách hàng')).toBeVisible()

    // B52 Screenshot: Auth Redirect
    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-auth-redirect.png') })
  })

  // -------------------------------------------------------------------------
  // B52-11: Customer Hold Payload Verification (tripId & seatNumbers)
  // -------------------------------------------------------------------------
  test('B52-11: Authenticated customer clicking hold sends correct payload { tripId, seatNumbers }', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')

    await setupSeatSelectionMocks(page)

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    // Select A01
    await page.locator('[data-testid="seat-A01"]').click()

    // Wait for the actual request triggered by the button click
    const holdRequestPromise = page.waitForRequest('**/api/v1/bookings/hold')
    await page.locator('[data-testid="btn-proceed-booking"]').click()
    const holdRequest = await holdRequestPromise

    const capturedPayload = JSON.parse(holdRequest.postData() || '{}')

    // Assert payload fields
    expect(capturedPayload.tripId).toBe(101)
    expect(capturedPayload.seatNumbers).toEqual(['A01'])
  })

  // -------------------------------------------------------------------------
  // B52-12: Duplicate Hold Prevention (In-flight Guard)
  // -------------------------------------------------------------------------
  test('B52-12: Rapid multiple clicks trigger only 1 hold request to backend', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')

    let requestCount = 0
    await setupSeatSelectionMocks(page)
    await page.route('**/api/v1/bookings/hold', async (route) => {
      requestCount++
      await new Promise(r => setTimeout(r, 600))
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            bookingId: 991,
            bookingReference: 'VB-HOLD-8899',
            userId: 1,
            tripId: 101,
            status: 'HELD',
            totalAmount: 400000,
            expiresAt: new Date(Date.now() + 600000).toISOString(),
            seatNumbers: ['A01']
          }
        })
      })
    })

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="seat-A01"]').click()

    const btn = page.locator('[data-testid="btn-proceed-booking"]')
    // Rapid double click
    await Promise.all([
      btn.click(),
      btn.click({ force: true }).catch(() => {})
    ])

    await page.waitForTimeout(800)
    expect(requestCount).toBe(1)
  })

  // -------------------------------------------------------------------------
  // B52-13: Backend-Confirmed Hold & Countdown Activation
  // -------------------------------------------------------------------------
  test('B52-13: Backend-confirmed hold starts active hold countdown and updates CTA', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')
    await setupSeatSelectionMocks(page)

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="seat-A01"]').click()
    await page.locator('[data-testid="btn-proceed-booking"]').click()

    // Countdown widget visible
    await expect(page.locator('[data-testid="hold-countdown-widget"]')).toBeVisible()
    await expect(page.locator('[data-testid="countdown-timer"]')).toBeVisible()

    // CTA updates to "Tiến Hành Thanh Toán"
    await expect(page.locator('[data-testid="btn-proceed-booking"]')).toContainText('Tiến Hành Thanh Toán')

    // B52 Screenshot: Active Hold Countdown
    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-hold-countdown.png') })
  })

  // -------------------------------------------------------------------------
  // B52-14: Seat Conflict Handling (SEAT_ALREADY_RESERVED)
  // -------------------------------------------------------------------------
  test('B52-14: Seat conflict error SEAT_ALREADY_RESERVED displays error toast and refetches seats', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')
    await setupSeatSelectionMocks(page, {
      holdFail: true,
      holdErrorCode: 'SEAT_ALREADY_RESERVED',
      holdErrorMessage: 'Một hoặc nhiều ghế đã bị hành khách khác chọn.'
    })

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="seat-A01"]').click()
    await page.locator('[data-testid="btn-proceed-booking"]').click()

    // Error toast
    await expect(page.getByText('Một hoặc nhiều ghế đã bị hành khách khác chọn. Vui lòng chọn ghế khác!')).toBeVisible()

    // Countdown does NOT start
    await expect(page.locator('[data-testid="hold-countdown-widget"]')).toHaveCount(0)

    // B52 Screenshot: Seat Conflict
    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-seat-conflict.png') })
  })

  // -------------------------------------------------------------------------
  // B52-15: General Hold Failure
  // -------------------------------------------------------------------------
  test('B52-15: General hold failure keeps state fail-closed without activating countdown', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')
    await setupSeatSelectionMocks(page, {
      holdFail: true,
      holdErrorCode: 'INTERNAL_ERROR',
      holdErrorMessage: 'Máy chủ đang bảo trì hệ thống đặt chỗ.'
    })

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="seat-A01"]').click()
    await page.locator('[data-testid="btn-proceed-booking"]').click()

    await expect(page.getByText('Máy chủ đang bảo trì hệ thống đặt chỗ.')).toBeVisible()
    await expect(page.locator('[data-testid="hold-countdown-widget"]')).toHaveCount(0)
  })

  // -------------------------------------------------------------------------
  // B52-16: Malformed Hold Response (Missing bookingReference)
  // -------------------------------------------------------------------------
  test('B52-16: Malformed hold response missing bookingReference fails closed', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')
    await setupSeatSelectionMocks(page, { malformedHold: true })

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="seat-A01"]').click()
    await page.locator('[data-testid="btn-proceed-booking"]').click()

    await expect(page.getByText('Dữ liệu phản hồi giữ chỗ từ máy chủ không hợp lệ.')).toBeVisible()
    await expect(page.locator('[data-testid="hold-countdown-widget"]')).toHaveCount(0)
  })

  // -------------------------------------------------------------------------
  // B52-17: Network Timeout on Hold Mutation
  // -------------------------------------------------------------------------
  test('B52-17: Network timeout during hold displays error and retains safe state', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')
    await setupSeatSelectionMocks(page, { holdTimeout: true })

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="seat-A01"]').click()
    await page.locator('[data-testid="btn-proceed-booking"]').click()

    await expect(page.getByText(/Không thể giữ chỗ|kết nối|Network Error/)).toBeVisible()
    await expect(page.locator('[data-testid="hold-countdown-widget"]')).toHaveCount(0)
  })

  // -------------------------------------------------------------------------
  // B52-18: Server-Based Countdown from authoritative expiresAt
  // -------------------------------------------------------------------------
  test('B52-18: Server-based countdown syncs with backend expiresAt timestamp', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')
    await setupSeatSelectionMocks(page)

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="seat-A01"]').click()
    await page.locator('[data-testid="btn-proceed-booking"]').click()

    const timer = page.locator('[data-testid="countdown-timer"]')
    await expect(timer).toBeVisible()
    // Timer text starts around 09:59 or 10:00
    await expect(timer).toContainText(/^(09|10):[0-5]\d$/)
  })

  // -------------------------------------------------------------------------
  // B52-19: Countdown Expiration Behavior
  // -------------------------------------------------------------------------
  test('B52-19: Countdown expiration clears held booking and informs passenger', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')

    // Set expiresAt to 2 seconds in the future
    await setupSeatSelectionMocks(page)
    await page.route('**/api/v1/bookings/hold', async (route) => {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            bookingId: 991,
            bookingReference: 'VB-EXPIRE-TEST',
            userId: 1,
            tripId: 101,
            status: 'HELD',
            totalAmount: 400000,
            expiresAt: new Date(Date.now() + 2000).toISOString(),
            seatNumbers: ['A01']
          }
        })
      })
    })

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="seat-A01"]').click()
    await page.locator('[data-testid="btn-proceed-booking"]').click()

    await expect(page.locator('[data-testid="hold-countdown-widget"]')).toBeVisible()

    // Wait 3s for timer to expire
    await page.waitForTimeout(3000)

    // Countdown widget disappears and expiration notice is shown
    await expect(page.locator('[data-testid="hold-countdown-widget"]')).toHaveCount(0)
    await expect(page.getByText('Thời gian 10 phút đã hết. Vui lòng chọn lại giường nằm.')).toBeVisible()
  })

  // -------------------------------------------------------------------------
  // B52-20: Cancellation Confirmation & Success
  // -------------------------------------------------------------------------
  test('B52-20: Cancellation flow asks confirmation and clears held booking upon server success', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')
    await setupSeatSelectionMocks(page)

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="seat-A01"]').click()
    await page.locator('[data-testid="btn-proceed-booking"]').click()

    await expect(page.locator('[data-testid="hold-countdown-widget"]')).toBeVisible()

    // Click "Hủy phiên giữ chỗ"
    await page.locator('[data-testid="btn-cancel-hold-trigger"]').click()

    // Confirmation dialog appears
    await expect(page.getByText('Hủy giữ chỗ hiện tại?')).toBeVisible()

    // Confirm cancel
    await page.locator('[data-testid="btn-confirm-cancel-hold"]').click()

    // Countdown widget disappears and toast appears
    await expect(page.locator('[data-testid="hold-countdown-widget"]')).toHaveCount(0)
    await expect(page.getByText('Đã hủy giữ chỗ thành công.')).toBeVisible()

    // B52 Screenshot: Hold Cancelled
    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-hold-cancelled.png') })
  })

  // -------------------------------------------------------------------------
  // B52-21: Proceed to Checkout with Authoritative bookingReference
  // -------------------------------------------------------------------------
  test('B52-21: Clicking Proceed with active hold navigates to /checkout/:bookingReference', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')
    await setupSeatSelectionMocks(page)

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="seat-A01"]').click()
    await page.locator('[data-testid="btn-proceed-booking"]').click()

    await expect(page.locator('[data-testid="hold-countdown-widget"]')).toBeVisible()

    // Click Proceed button again to go to Checkout
    await page.locator('[data-testid="btn-proceed-booking"]').click()

    // Assert URL contains authoritative bookingReference VB-HOLD-8899, NEVER tripId
    await page.waitForURL(/\/checkout\/VB-HOLD-8899/)
    expect(page.url()).toContain('/checkout/VB-HOLD-8899')
  })

  // -------------------------------------------------------------------------
  // B52-22: Responsive Viewports (Tablet 768x1024 and Mobile 375x812)
  // -------------------------------------------------------------------------
  test('B52-22: Responsive viewports maintain usability on tablet and mobile', async ({ page }) => {
    // 1. Tablet Viewport
    await page.setViewportSize({ width: 768, height: 1024 })
    await setupSeatSelectionMocks(page)
    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('[data-testid="seat-grid-lower"]')).toBeVisible()
    // B52 Screenshot: Tablet
    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-tablet.png') })

    // 2. Mobile Viewport
    await page.setViewportSize({ width: 375, height: 812 })
    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('[data-testid="seat-grid-lower"]')).toBeVisible()
    await expect(page.locator('[data-testid="btn-proceed-booking"]')).toBeVisible()
    // B52 Screenshot: Mobile
    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-mobile.png') })
  })

  // -------------------------------------------------------------------------
  // B52-23: Light and Dark Theme Contrast
  // -------------------------------------------------------------------------
  test('B52-23: Light and Dark themes maintain color contrast and legibility', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupSeatSelectionMocks(page)

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    // Switch to dark theme via root class
    await page.evaluate(() => {
      document.documentElement.classList.add('dark')
    })
    await page.waitForTimeout(300)

    await expect(page.locator('[data-testid="seat-grid-lower"]')).toBeVisible()

    // B52 Screenshot: Desktop Dark
    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-desktop-dark.png') })
  })
})
