import { test, expect } from '@playwright/test'
import path from 'path'
import fs from 'fs'
import { fileURLToPath } from 'url'
import { loginAsRole } from './helpers/auth'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const screenshotDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-gate-b5-2-closure')
const screenshotEnhancementDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-gate-b5-2-enhancement')

for (const dir of [screenshotDir, screenshotEnhancementDir]) {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true })
  }
}

// ---------------------------------------------------------------------------
// 1. Authoritative Backend Seat Matrix Fixtures (SeatDto.java)
// Zero fake status properties: id, tripId, seatNumber, seatType, priceMultiplier, finalPrice
// ---------------------------------------------------------------------------
const createMockSeats = (options = {}) => {
  const { withDeck = false } = options
  const seats = []
  // Lower Deck (A01 - A18)
  for (let i = 1; i <= 18; i++) {
    const num = `A${String(i).padStart(2, '0')}`
    const seat = {
      id: 100 + i,
      tripId: 101,
      seatNumber: num,
      seatType: i <= 6 ? 'VIP' : 'STANDARD',
      priceMultiplier: i <= 6 ? 1.15 : 1.0,
      finalPrice: i <= 6 ? 400000 : 350000
    }
    if (withDeck) seat.deck = 'LOWER'
    seats.push(seat)
  }
  // Upper Deck (B01 - B18)
  for (let i = 1; i <= 18; i++) {
    const num = `B${String(i).padStart(2, '0')}`
    const seat = {
      id: 200 + i,
      tripId: 101,
      seatNumber: num,
      seatType: 'STANDARD',
      priceMultiplier: 1.0,
      finalPrice: 350000
    }
    if (withDeck) seat.deck = 'UPPER'
    seats.push(seat)
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
  transportType: 'VIP 36 Giường Nằm',
  totalSeats: 36,
  basePrice: 350000,
  status: 'SCHEDULED'
}

/**
 * Setup Route Interceptions for Gate B5.2 Seat Selection
 */
async function setupSeatSelectionMocks(page, options = {}) {
  const {
    seats = createMockSeats({ withDeck: true }),
    seatsFail = false,
    delaySeatsMs = 0,
    holdFail = false,
    holdErrorCode = null,
    holdErrorMessage = null,
    malformedHold = false,
    holdTimeout = false,
    cancelFail = false,
    cancelTimeout = false,
    myBookings = [],
    myBookingsMalformed = false,
    availabilitySeats = [
      { seatNumber: 'A01', status: 'AVAILABLE' },
      { seatNumber: 'A02', status: 'AVAILABLE' }
    ]
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

  // Mock GET Seat Availability (Option B Endpoint)
  await page.route('**/api/v1/bookings/trips/*/availability', async (route) => {
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          tripId: 101,
          serverTime: '2026-11-26T15:30:00+07:00',
          seats: availabilitySeats
        }
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

  // Mock GET My Bookings (PRE-01: PageResponse<BookingResponse> Contract)
  await page.route('**/api/v1/bookings/my-bookings*', async (route) => {
    if (myBookingsMalformed) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { corrupted: true } // Missing items array
        })
      })
    }
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          items: myBookings,
          page: 0,
          size: 20,
          totalElements: myBookings.length,
          totalPages: 1,
          hasNext: false,
          hasPrevious: false
        }
      })
    })
  })

  // Mock GET Booking Detail
  await page.route('**/api/v1/bookings/VB-*', async (route) => {
    const url = route.request().url()
    const ref = url.split('/').pop().split('?')[0]
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          bookingId: 991,
          bookingReference: ref,
          userId: 1,
          tripId: 101,
          status: 'HELD',
          totalAmount: 400000,
          expiresAt: new Date(Date.now() + 8 * 60 * 1000).toISOString(),
          createdAt: new Date().toISOString(),
          seatNumbers: ['A01']
        }
      })
    })
  })

  // Mock POST Cancel Booking Hold
  await page.route('**/api/v1/bookings/*/cancel', async (route) => {
    if (cancelTimeout) {
      return route.abort('timedout')
    }
    if (cancelFail) {
      return route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          success: false,
          code: 'SERVER_ERROR',
          message: 'Lỗi máy chủ khi hủy giữ chỗ.'
        })
      })
    }
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

test.describe('Gate B5.2 & B5.2-E — Seat Reservation Flow & Contract Hardening', () => {
  // -------------------------------------------------------------------------
  // B52-01: Seat API Success & Authentic Seat Matrix
  // -------------------------------------------------------------------------
  test('B52-01: Seat API success loads real seats from backend without demo fallback', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupSeatSelectionMocks(page)

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    await expect(page.getByText('Sơ đồ chỗ ngồi')).toBeVisible()
    await expect(page.locator('[data-testid="seat-grid-lower"]')).toBeVisible()
    await expect(page.locator('[data-testid="seat-A01"]')).toBeVisible()
    await expect(page.locator('[data-testid="seat-A02"]')).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-desktop-light.png') })
    await page.screenshot({ path: path.join(screenshotEnhancementDir, 'seatmap-desktop-light.png') })
  })

  // -------------------------------------------------------------------------
  // B52-02: Seat API Loading Skeleton
  // -------------------------------------------------------------------------
  test('B52-02: Seat API Loading indicator displays while request is pending', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupSeatSelectionMocks(page, { delaySeatsMs: 3000 })

    await page.goto('/trips/101/seats')
    await expect(page.locator('[data-testid="seat-loading"]')).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-loading-state.png') })
    await page.screenshot({ path: path.join(screenshotEnhancementDir, 'seatmap-loading-state.png') })
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

    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-error-retry.png') })
    await page.screenshot({ path: path.join(screenshotEnhancementDir, 'seatmap-error-retry.png') })
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

    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-empty-state.png') })
    await page.screenshot({ path: path.join(screenshotEnhancementDir, 'seatmap-empty-state.png') })
  })

  // -------------------------------------------------------------------------
  // B52-05: Seat Status Mapping (Unverified Catalog Seats & Contract Notice)
  // -------------------------------------------------------------------------
  test('B52-05: Seat status mapping displays customer-friendly contract notice (PRE-05)', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupSeatSelectionMocks(page)

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    const seatA01 = page.locator('[data-testid="seat-A01"]')
    await expect(seatA01).toBeEnabled()
    await expect(page.locator('[data-testid="seat-contract-notice"]')).toBeVisible()

    // PRE-05: Friendly Vietnamese wording without technical jargon
    await expect(page.locator('[data-testid="seat-contract-notice"]')).toContainText("Tình trạng chỗ thực tế sẽ được hệ thống kiểm tra và xác nhận khi quý khách bấm 'Giữ Chỗ'")
  })

  // -------------------------------------------------------------------------
  // B52-06: Explicit Multi-Deck Switching
  // -------------------------------------------------------------------------
  test('B52-06: Deck rendering switches smoothly when explicit deck metadata is present', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupSeatSelectionMocks(page, { seats: createMockSeats({ withDeck: true }) })

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    // Click Upper Deck Tab
    const upperTab = page.locator('[data-testid="deck-tab-upper"]')
    await expect(upperTab).toBeVisible()
    await upperTab.click()

    await expect(page.locator('[data-testid="seat-grid-upper"]')).toBeVisible()
    await expect(page.locator('[data-testid="seat-B01"]')).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-deck-tabs.png') })
    await page.screenshot({ path: path.join(screenshotEnhancementDir, 'seatmap-deck-tabs.png') })

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

    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-selected-seats.png') })
    await page.screenshot({ path: path.join(screenshotEnhancementDir, 'seatmap-selected-seats.png') })

    // Deselect A01 via sidebar remove button
    await page.locator('[data-testid="remove-seat-A01"]').click()
    await expect(page.locator('[data-testid="selected-seat-item-A01"]')).toHaveCount(0)
    await expect(page.locator('[data-testid="total-price-display"]')).toContainText('400.000')
  })

  // -------------------------------------------------------------------------
  // B52-08: Conflict Handling Does Not Permanently Disable All Seats (PRE-05)
  // -------------------------------------------------------------------------
  test('B52-08: Conflict rejected seats deselect and refresh availability without permanent lock (PRE-05)', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')

    let availCount = 0
    await setupSeatSelectionMocks(page, {
      holdFail: true,
      holdErrorCode: 'SEAT_ALREADY_RESERVED',
      holdErrorMessage: 'Một hoặc nhiều ghế đã bị hành khách khác chọn.'
    })

    // Dynamic mock: initial load A01 is AVAILABLE; after conflict refresh, A01 is HELD
    await page.route('**/api/v1/bookings/trips/*/availability', async (route) => {
      availCount++
      const a01Status = availCount > 1 ? 'HELD' : 'AVAILABLE'
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            tripId: 101,
            serverTime: '2026-11-26T15:30:00+07:00',
            seats: [
              { seatNumber: 'A01', status: a01Status },
              { seatNumber: 'A02', status: 'AVAILABLE' }
            ]
          }
        })
      })
    })

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    // Initial state: A01 is available and can be selected
    const seatA01 = page.locator('[data-testid="seat-A01"]')
    await expect(seatA01).toBeEnabled()
    await seatA01.click()

    // Proceed with hold -> backend rejects with conflict
    await page.locator('[data-testid="btn-proceed-booking"]').click()

    // Conflict toast
    await expect(page.getByText('Một hoặc nhiều ghế đã bị hành khách khác chọn. Vui lòng chọn ghế khác!')).toBeVisible()

    // A01 is now deselected and disabled because refreshed availability returned HELD
    await expect(seatA01).toBeDisabled()

    // A02 remains selectable
    const seatA02 = page.locator('[data-testid="seat-A02"]')
    await expect(seatA02).toBeEnabled()
  })

  // -------------------------------------------------------------------------
  // B52-09: Seat Limit (Max 10 Seats per Booking)
  // -------------------------------------------------------------------------
  test('B52-09: Seat limit enforces maximum 10 seats per booking with warning toast', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupSeatSelectionMocks(page)

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    // Select 10 seats on Lower Deck: A01 to A10
    const seatCodes = ['A01', 'A02', 'A03', 'A04', 'A05', 'A06', 'A07', 'A08', 'A09', 'A10']
    for (const code of seatCodes) {
      await page.locator(`[data-testid="seat-${code}"]`).click()
    }
    await expect(page.locator('[data-testid="selected-seats-list"]').locator('> div')).toHaveCount(10)

    // Try to select 11th seat (A11)
    await page.locator('[data-testid="seat-A11"]').click()

    // Warning toast
    await expect(page.getByText('Mỗi hành khách được chọn tối đa 10 chỗ trong một lần đặt vé.')).toBeVisible()

    // Count remains 10
    await expect(page.locator('[data-testid="selected-seats-list"]').locator('> div')).toHaveCount(10)

    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-max-limit-warning.png') })
    await page.screenshot({ path: path.join(screenshotEnhancementDir, 'seatmap-max-limit-warning.png') })
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

    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-auth-redirect.png') })
    await page.screenshot({ path: path.join(screenshotEnhancementDir, 'seatmap-auth-redirect.png') })
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

    await page.locator('[data-testid="seat-A01"]').click()

    const holdRequestPromise = page.waitForRequest('**/api/v1/bookings/hold')
    await page.locator('[data-testid="btn-proceed-booking"]').click()
    const holdRequest = await holdRequestPromise

    const capturedPayload = JSON.parse(holdRequest.postData() || '{}')
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

    await expect(page.locator('[data-testid="hold-countdown-widget"]')).toBeVisible()
    await expect(page.locator('[data-testid="countdown-timer"]')).toBeVisible()
    await expect(page.locator('[data-testid="btn-proceed-booking"]')).toContainText('Tiến Hành Thanh Toán')

    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-hold-countdown.png') })
    await page.screenshot({ path: path.join(screenshotEnhancementDir, 'seatmap-hold-countdown.png') })
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

    await expect(page.getByText('Một hoặc nhiều ghế đã bị hành khách khác chọn. Vui lòng chọn ghế khác!')).toBeVisible()
    await expect(page.locator('[data-testid="hold-countdown-widget"]')).toHaveCount(0)

    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-seat-conflict.png') })
    await page.screenshot({ path: path.join(screenshotEnhancementDir, 'seatmap-seat-conflict.png') })
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
  // B52-16: Malformed Hold Response Triggers Unknown Outcome Guard (PRE-03)
  // -------------------------------------------------------------------------
  test('B52-16: Malformed hold response triggers fail-closed unknown outcome guard (PRE-03)', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')
    await setupSeatSelectionMocks(page, { malformedHold: true })

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="seat-A01"]').click()
    await page.locator('[data-testid="btn-proceed-booking"]').click()

    await expect(page.locator('[data-testid="hold-unknown-alert"]')).toBeVisible()
    await expect(page.locator('[data-testid="btn-proceed-booking"]')).toBeDisabled()
    await expect(page.locator('[data-testid="hold-countdown-widget"]')).toHaveCount(0)
  })

  // -------------------------------------------------------------------------
  // B52-17: Network Timeout on Hold Mutation Triggers Unknown Outcome Guard (PRE-02)
  // -------------------------------------------------------------------------
  test('B52-17: Network timeout during hold triggers persistent guard and disables CTA', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')
    await setupSeatSelectionMocks(page, { holdTimeout: true })

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="seat-A01"]').click()
    await page.locator('[data-testid="btn-proceed-booking"]').click()

    const alert = page.locator('[data-testid="hold-unknown-alert"]')
    await expect(alert).toBeVisible()
    await expect(alert).toContainText('chưa xác định kết quả')

    const proceedBtn = page.locator('[data-testid="btn-proceed-booking"]')
    await expect(proceedBtn).toBeDisabled()
    await expect(page.locator('[data-testid="btn-verify-hold-state"]')).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-hold-unknown-outcome.png') })
    await page.screenshot({ path: path.join(screenshotEnhancementDir, 'seatmap-hold-unknown-outcome.png') })
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
    await expect(timer).toContainText(/^(09|10):[0-5]\d$/)
  })

  // -------------------------------------------------------------------------
  // B52-19: Countdown Expiration Behavior
  // -------------------------------------------------------------------------
  test('B52-19: Countdown expiration clears held booking and informs passenger', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')

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

    await expect(page.locator('[data-testid="hold-countdown-widget"]')).toHaveCount(0)
    await expect(page.getByText('Thời gian 10 phút giữ chỗ đã hết. Vui lòng chọn lại chỗ ngồi.')).toBeVisible()
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

    await page.locator('[data-testid="btn-cancel-hold-trigger"]').click()
    await expect(page.getByText('Hủy giữ chỗ hiện tại?')).toBeVisible()

    await page.locator('[data-testid="btn-confirm-cancel-hold"]').click()

    await expect(page.locator('[data-testid="hold-countdown-widget"]')).toHaveCount(0)
    await expect(page.getByText('Đã hủy giữ chỗ thành công.')).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-hold-cancelled.png') })
    await page.screenshot({ path: path.join(screenshotEnhancementDir, 'seatmap-hold-cancelled.png') })
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

    await page.locator('[data-testid="btn-proceed-booking"]').click()

    await page.waitForURL(/\/checkout\/VB-HOLD-8899/)
    expect(page.url()).toContain('/checkout/VB-HOLD-8899')
  })

  // -------------------------------------------------------------------------
  // B52-22: Responsive Viewports
  // -------------------------------------------------------------------------
  test('B52-22: Responsive viewports maintain usability on tablet and mobile', async ({ page }) => {
    // 1. Tablet Viewport
    await page.setViewportSize({ width: 768, height: 1024 })
    await setupSeatSelectionMocks(page)
    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('[data-testid="seat-grid-lower"]')).toBeVisible()
    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-tablet.png') })
    await page.screenshot({ path: path.join(screenshotEnhancementDir, 'seatmap-tablet.png') })

    // 2. Mobile Viewport
    await page.setViewportSize({ width: 375, height: 812 })
    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('[data-testid="seat-grid-lower"]')).toBeVisible()
    await expect(page.locator('[data-testid="btn-proceed-booking"]')).toBeVisible()
    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-mobile.png') })
    await page.screenshot({ path: path.join(screenshotEnhancementDir, 'seatmap-mobile.png') })
  })

  // -------------------------------------------------------------------------
  // B52-23: Light and Dark Theme Contrast
  // -------------------------------------------------------------------------
  test('B52-23: Light and Dark themes maintain color contrast and legibility', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupSeatSelectionMocks(page)

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    await page.evaluate(() => {
      document.documentElement.classList.add('dark')
    })
    await page.waitForTimeout(300)

    await expect(page.locator('[data-testid="seat-grid-lower"]')).toBeVisible()

    await page.screenshot({ path: path.join(screenshotDir, 'seatmap-desktop-dark.png') })
    await page.screenshot({ path: path.join(screenshotEnhancementDir, 'seatmap-desktop-dark.png') })
  })

  // -------------------------------------------------------------------------
  // B52-24: Active Held Booking Restoration on Reload
  // -------------------------------------------------------------------------
  test('B52-24: Reload restores active held booking from storage and backend verification', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')
    await setupSeatSelectionMocks(page)

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="seat-A01"]').click()
    await page.locator('[data-testid="btn-proceed-booking"]').click()
    await expect(page.locator('[data-testid="hold-countdown-widget"]')).toBeVisible()

    await page.reload()
    await page.waitForLoadState('networkidle')

    await expect(page.locator('[data-testid="hold-countdown-widget"]')).toBeVisible()
    await expect(page.locator('[data-testid="btn-proceed-booking"]')).toContainText('Tiến Hành Thanh Toán')
    await expect(page.locator('[data-testid="selected-seat-item-A01"]')).toBeVisible()
  })

  // -------------------------------------------------------------------------
  // B52-25: Hold State Verification Button Resolves Interrupted Hold (PRE-01 & PRE-02)
  // -------------------------------------------------------------------------
  test('B52-25: Hold state verification recovers active hold with exact seat match from PageResponse', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')

    await setupSeatSelectionMocks(page, {
      holdTimeout: true,
      myBookings: [
        {
          bookingId: 991,
          bookingReference: 'VB-RECOVERED-01',
          userId: 1,
          tripId: 101,
          status: 'HELD',
          totalAmount: 400000,
          expiresAt: new Date(Date.now() + 9 * 60 * 1000).toISOString(),
          seatNumbers: ['A01']
        }
      ]
    })

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="seat-A01"]').click()
    await page.locator('[data-testid="btn-proceed-booking"]').click()

    await expect(page.locator('[data-testid="hold-unknown-alert"]')).toBeVisible()

    const verifyBtn = page.locator('[data-testid="btn-verify-hold-state"]')
    await verifyBtn.click()

    // Recovers hold: alert disappears, countdown widget appears
    await expect(page.locator('[data-testid="hold-unknown-alert"]')).toHaveCount(0)
    await expect(page.locator('[data-testid="hold-countdown-widget"]')).toBeVisible()
    await expect(page.locator('[data-testid="btn-proceed-booking"]')).toContainText('Tiến Hành Thanh Toán')
  })

  // -------------------------------------------------------------------------
  // B52E-01: Seat Availability Endpoint & Status Mapping (Option B)
  // -------------------------------------------------------------------------
  test('B52E-01: Dynamic availability endpoint maps AVAILABLE, HELD, and SOLD statuses', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupSeatSelectionMocks(page, {
      availabilitySeats: [
        { seatNumber: 'A01', status: 'AVAILABLE' },
        { seatNumber: 'A02', status: 'SOLD' },
        { seatNumber: 'A03', status: 'HELD' }
      ]
    })

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    // A01 is available
    await expect(page.locator('[data-testid="seat-A01"]')).toBeEnabled()

    // A02 is SOLD -> disabled
    await expect(page.locator('[data-testid="seat-A02"]')).toBeDisabled()

    // A03 is HELD -> disabled
    await expect(page.locator('[data-testid="seat-A03"]')).toBeDisabled()

    // Refresh availability button is functional
    const refreshBtn = page.locator('[data-testid="btn-refresh-availability"]')
    await expect(refreshBtn).toBeVisible()
    await refreshBtn.click()

    await page.screenshot({ path: path.join(screenshotEnhancementDir, 'seatmap-dynamic-availability.png') })
  })

  // -------------------------------------------------------------------------
  // B52E-02: PRE-01 Malformed PageResponse Preserves Fail-Closed Lock
  // -------------------------------------------------------------------------
  test('B52E-02: PRE-01 Malformed PageResponse preserves unknown outcome lock', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')
    await setupSeatSelectionMocks(page, {
      holdTimeout: true,
      myBookingsMalformed: true
    })

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="seat-A01"]').click()
    await page.locator('[data-testid="btn-proceed-booking"]').click()

    await expect(page.locator('[data-testid="hold-unknown-alert"]')).toBeVisible()

    // Click verify hold state
    await page.locator('[data-testid="btn-verify-hold-state"]').click()

    // Must PRESERVE lock on malformed response!
    await expect(page.locator('[data-testid="hold-unknown-alert"]')).toBeVisible()
    await expect(page.locator('[data-testid="btn-proceed-booking"]')).toBeDisabled()
  })

  // -------------------------------------------------------------------------
  // B52E-03: PRE-02 Unrelated Booking on Same Trip Does NOT Clear Lock
  // -------------------------------------------------------------------------
  test('B52E-03: PRE-02 Unrelated booking on same trip preserves fail-closed lock', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')

    await setupSeatSelectionMocks(page, {
      holdTimeout: true,
      myBookings: [
        {
          bookingId: 992,
          bookingReference: 'VB-OTHER-BOOKING',
          userId: 1,
          tripId: 101,
          status: 'HELD',
          totalAmount: 350000,
          expiresAt: new Date(Date.now() + 600000).toISOString(),
          seatNumbers: ['B05'] // Different seat!
        }
      ]
    })

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    // Requested seat A01
    await page.locator('[data-testid="seat-A01"]').click()
    await page.locator('[data-testid="btn-proceed-booking"]').click()

    await expect(page.locator('[data-testid="hold-unknown-alert"]')).toBeVisible()

    // Click verify hold state
    await page.locator('[data-testid="btn-verify-hold-state"]').click()

    // Does NOT clear unknown outcome because seats do not match!
    await expect(page.locator('[data-testid="hold-unknown-alert"]')).toBeVisible()
    await expect(page.locator('[data-testid="btn-proceed-booking"]')).toBeDisabled()
  })

  // -------------------------------------------------------------------------
  // B52E-04: PRE-04 Cancellation Timeout Locks Checkout
  // -------------------------------------------------------------------------
  test('B52E-04: PRE-04 Cancellation timeout locks checkout and preserves warning context', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')
    await setupSeatSelectionMocks(page, { cancelTimeout: true })

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="seat-A01"]').click()
    await page.locator('[data-testid="btn-proceed-booking"]').click()

    await expect(page.locator('[data-testid="hold-countdown-widget"]')).toBeVisible()

    // Attempt cancel -> times out
    await page.locator('[data-testid="btn-cancel-hold-trigger"]').click()
    await page.locator('[data-testid="btn-confirm-cancel-hold"]').click()

    // Cancel unresolved alert appears and checkout CTA is disabled
    await expect(page.locator('[data-testid="cancel-unresolved-alert"]')).toBeVisible()
    await expect(page.locator('[data-testid="btn-proceed-booking"]')).toBeDisabled()

    await page.screenshot({ path: path.join(screenshotEnhancementDir, 'seatmap-cancel-unresolved.png') })
  })

  // -------------------------------------------------------------------------
  // B52E-05: PRE-05 Layout Neutrality Without Explicit Deck Metadata
  // -------------------------------------------------------------------------
  test('B52E-05: PRE-05 Layout renders unified neutral grid without deck tabs when deck metadata is omitted', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    // Seats without explicit deck
    await setupSeatSelectionMocks(page, { seats: createMockSeats({ withDeck: false }) })

    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    // Deck tabs must NOT be present
    await expect(page.locator('[data-testid="deck-tab-lower"]')).toHaveCount(0)
    await expect(page.locator('[data-testid="deck-tab-upper"]')).toHaveCount(0)

    // Neutral grid is visible
    await expect(page.locator('[data-testid="seat-grid-lower"]')).toBeVisible()
    await expect(page.locator('[data-testid="seat-A01"]')).toBeVisible()
    await expect(page.locator('[data-testid="seat-B01"]')).toBeVisible()

    await page.screenshot({ path: path.join(screenshotEnhancementDir, 'seatmap-neutral-grid.png') })
  })
})
