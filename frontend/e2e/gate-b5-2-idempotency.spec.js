import { test, expect } from '@playwright/test'
import path from 'path'
import fs from 'fs'
import { fileURLToPath } from 'url'
import { loginAsRole } from './helpers/auth.js'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const screenshotDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-gate-b5-2-idempotency')

if (!fs.existsSync(screenshotDir)) {
  fs.mkdirSync(screenshotDir, { recursive: true })
}

const createMockSeats = () => {
  const seats = []
  // Lower Deck (A01 - A18)
  for (let i = 1; i <= 18; i++) {
    const num = `A${String(i).padStart(2, '0')}`
    seats.push({
      id: 100 + i,
      tripId: 101,
      seatNumber: num,
      seatType: i <= 6 ? 'VIP' : 'STANDARD',
      priceMultiplier: i <= 6 ? 1.15 : 1.0,
      finalPrice: i <= 6 ? 400000 : 350000,
      deck: 'LOWER'
    })
  }
  // Upper Deck (B01 - B18)
  for (let i = 1; i <= 18; i++) {
    const num = `B${String(i).padStart(2, '0')}`
    seats.push({
      id: 200 + i,
      tripId: 101,
      seatNumber: num,
      seatType: 'STANDARD',
      priceMultiplier: 1.0,
      finalPrice: 350000,
      deck: 'UPPER'
    })
  }
  return seats
}

test.describe('Gate B5.2-I: Durable Hold Idempotency & Concurrency Verification', () => {

  test('E2E-IDEM-01: Sends Idempotency-Key header on hold request and captures screenshot', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    let capturedIdempotencyHeader = null

    await page.route('**/api/v1/public/settings', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { brandName: 'VeloBus' } }) })
    })

    await page.route('**/api/v1/catalog/trips/*/seats', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: createMockSeats() }) })
    })

    await page.route('**/api/v1/bookings/trips/*/availability', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            tripId: 101,
            serverTime: '2026-11-26T15:30:00+07:00',
            seats: [{ seatNumber: 'A01', status: 'AVAILABLE' }]
          }
        })
      })
    })

    await page.route('**/api/v1/bookings/hold', async (route) => {
      capturedIdempotencyHeader = route.request().headers()['idempotency-key']
      const postData = JSON.parse(route.request().postData() || '{}')
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            bookingId: 991,
            bookingReference: 'VB-IDEM-001',
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

    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')
    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    // Click seat A01
    const seatBtn = page.locator('[data-testid="seat-A01"]')
    await expect(seatBtn).toBeVisible({ timeout: 10000 })
    await seatBtn.click()

    // Click Proceed / Hold button
    const holdBtn = page.locator('[data-testid="btn-proceed-booking"]')
    await expect(holdBtn).toBeVisible()
    await holdBtn.click()

    // Verify hold success
    await expect(page.getByText(/giữ chỗ thành công/i)).toBeVisible({ timeout: 5000 })
    expect(capturedIdempotencyHeader).toBeTruthy()
    expect(capturedIdempotencyHeader.length).toBeGreaterThan(10)

    await page.screenshot({ path: path.join(screenshotDir, '01-idempotency-key-sent.png'), fullPage: true })
  })

  test('E2E-IDEM-02: Timeout preserves exact Idempotency-Key across retry and reload', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    let holdAttempt = 0
    const sentKeys = []

    await page.route('**/api/v1/public/settings', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { brandName: 'VeloBus' } }) })
    })

    await page.route('**/api/v1/catalog/trips/*/seats', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: createMockSeats() }) })
    })

    await page.route('**/api/v1/bookings/trips/*/availability', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            tripId: 101,
            serverTime: '2026-11-26T15:30:00+07:00',
            seats: [{ seatNumber: 'A01', status: 'AVAILABLE' }]
          }
        })
      })
    })

    await page.route('**/api/v1/bookings/hold', async (route) => {
      holdAttempt++
      const key = route.request().headers()['idempotency-key']
      sentKeys.push(key)

      if (holdAttempt === 1) {
        // First attempt: network timeout
        return route.abort('timedout')
      }

      // Subsequent attempt / recovery replay: success
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            bookingId: 992,
            bookingReference: 'VB-IDEM-REPLAY-002',
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

    await page.route('**/api/v1/bookings/my-bookings*', async (route) => {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            items: [{
              bookingId: 992,
              bookingReference: 'VB-IDEM-REPLAY-002',
              userId: 1,
              tripId: 101,
              status: 'HELD',
              totalAmount: 400000,
              expiresAt: new Date(Date.now() + 600000).toISOString(),
              seatNumbers: ['A01']
            }],
            page: 0,
            size: 20,
            totalElements: 1,
            totalPages: 1,
            hasNext: false,
            hasPrevious: false
          }
        })
      })
    })

    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')
    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    // Select seat A01
    const seatBtn = page.locator('[data-testid="seat-A01"]')
    await expect(seatBtn).toBeVisible({ timeout: 10000 })
    await seatBtn.click()

    // Trigger hold -> times out
    const holdBtn = page.locator('[data-testid="btn-proceed-booking"]')
    await holdBtn.click()

    // Alert for unknown outcome must appear
    const alert = page.locator('[data-testid="hold-unknown-alert"]')
    await expect(alert).toBeVisible({ timeout: 5000 })
    await page.screenshot({ path: path.join(screenshotDir, '02-timeout-fail-closed-alert.png'), fullPage: true })

    // Reload page: fail-closed alert should still be present from recovery context
    await page.reload()
    await page.waitForLoadState('networkidle')
    await expect(page.locator('[data-testid="hold-unknown-alert"]')).toBeVisible({ timeout: 5000 })
    await page.screenshot({ path: path.join(screenshotDir, '03-reload-preserved-alert.png'), fullPage: true })

    // Click verify hold state button
    const verifyBtn = page.locator('[data-testid="btn-verify-hold-state"]')
    await expect(verifyBtn).toBeVisible()
    await verifyBtn.click()

    // Should resolve with replay
    await expect(page.getByText(/máy chủ đã xác nhận phiên giữ chỗ/i)).toBeVisible({ timeout: 5000 })
    await page.screenshot({ path: path.join(screenshotDir, '04-idempotent-recovery-success.png'), fullPage: true })

    // Verify both attempts used identical Idempotency-Key
    expect(sentKeys.length).toBeGreaterThanOrEqual(2)
    expect(sentKeys[1]).toBe(sentKeys[0])
  })

  test('E2E-IDEM-03: Cancellation timeout lock persists across page reload', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })

    await page.route('**/api/v1/public/settings', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { brandName: 'VeloBus' } }) })
    })

    await page.route('**/api/v1/catalog/trips/*/seats', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: createMockSeats() }) })
    })

    await page.route('**/api/v1/bookings/trips/*/availability', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            tripId: 101,
            serverTime: '2026-11-26T15:30:00+07:00',
            seats: [{ seatNumber: 'A01', status: 'AVAILABLE' }]
          }
        })
      })
    })

    await page.route('**/api/v1/bookings/hold', async (route) => {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            bookingId: 993,
            bookingReference: 'VB-IDEM-CANCEL-003',
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

    await page.route('**/api/v1/bookings/*/cancel', async (route) => {
      // Abort cancel request with timeout
      return route.abort('timedout')
    })

    await page.route('**/api/v1/bookings/VB-IDEM-CANCEL-003', async (route) => {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            bookingId: 993,
            bookingReference: 'VB-IDEM-CANCEL-003',
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

    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')
    await page.goto('/trips/101/seats')
    await page.waitForLoadState('networkidle')

    // Select seat & hold
    const seatBtn = page.locator('[data-testid="seat-A01"]')
    await expect(seatBtn).toBeVisible({ timeout: 10000 })
    await seatBtn.click()

    const holdBtn = page.locator('[data-testid="btn-proceed-booking"]')
    await holdBtn.click()

    // Active hold active: click "Hủy giữ chỗ"
    const cancelBtn = page.locator('[data-testid="btn-cancel-hold-trigger"]')
    await expect(cancelBtn).toBeVisible({ timeout: 5000 })
    await cancelBtn.click()

    // Confirm cancel modal
    const confirmCancelBtn = page.locator('[data-testid="btn-confirm-cancel-hold"]')
    await expect(confirmCancelBtn).toBeVisible()
    await confirmCancelBtn.click()

    // Cancellation timeout error should appear
    await expect(page.getByText(/gặp sự cố mạng hoặc timeout/i).first()).toBeVisible({ timeout: 5000 })
    await page.screenshot({ path: path.join(screenshotDir, '05-cancel-timeout-guard.png'), fullPage: true })

    // Reload page: cancellation unresolved lock must persist and block checkout
    await page.reload()
    await page.waitForLoadState('networkidle')
    const cancelAlert = page.locator('[data-testid="cancel-unresolved-alert"]')
    await expect(cancelAlert).toBeVisible({ timeout: 5000 })
    const checkoutBtn = page.locator('[data-testid="btn-proceed-booking"]')
    await expect(checkoutBtn).toBeDisabled()
    await page.screenshot({ path: path.join(screenshotDir, '06-reload-cancel-lock-persisted.png'), fullPage: true })
  })
})
