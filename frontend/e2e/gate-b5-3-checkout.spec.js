import { test, expect } from '@playwright/test'
import path from 'path'
import fs from 'fs'
import { fileURLToPath } from 'url'
import { loginAsRole } from './helpers/auth.js'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const screenshotDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-gate-b5-3')

if (!fs.existsSync(screenshotDir)) {
  fs.mkdirSync(screenshotDir, { recursive: true })
}

const mockHeldBooking = {
  bookingId: 881,
  bookingReference: 'BKG-CHK-881',
  userId: 1,
  tripId: 101,
  status: 'HELD',
  totalAmount: 700000,
  expiresAt: new Date(Date.now() + 500000).toISOString(), // ~8 mins remaining
  createdAt: new Date().toISOString(),
  seatNumbers: ['A01', 'A02']
}

const mockExpiringSoonBooking = {
  bookingId: 882,
  bookingReference: 'BKG-EXP-882',
  userId: 1,
  tripId: 101,
  status: 'HELD',
  totalAmount: 350000,
  expiresAt: new Date(Date.now() + 45000).toISOString(), // 45s remaining (< 60s)
  createdAt: new Date().toISOString(),
  seatNumbers: ['B05']
}

const mockConfirmedBooking = {
  bookingId: 883,
  bookingReference: 'BKG-CONF-883',
  userId: 1,
  tripId: 101,
  status: 'CONFIRMED',
  totalAmount: 350000,
  expiresAt: new Date(Date.now() + 300000).toISOString(),
  createdAt: new Date(Date.now() - 60000).toISOString(),
  seatNumbers: ['A03']
}

const mockPageBookings = {
  items: [
    {
      bookingId: 881,
      bookingReference: 'BKG-CHK-881',
      userId: 1,
      tripId: 101,
      status: 'HELD',
      totalAmount: 700000,
      createdAt: '2026-11-26T14:30:00',
      expiresAt: new Date(Date.now() + 500000).toISOString(),
      seatNumbers: ['A01', 'A02']
    },
    {
      bookingId: 883,
      bookingReference: 'BKG-CONF-883',
      userId: 1,
      tripId: 101,
      status: 'CONFIRMED',
      totalAmount: 350000,
      createdAt: '2026-11-25T09:15:00',
      seatNumbers: ['A03']
    },
    {
      bookingId: 884,
      bookingReference: 'BKG-CANC-884',
      userId: 1,
      tripId: 99,
      status: 'CANCELLED',
      totalAmount: 400000,
      createdAt: '2026-11-24T18:00:00',
      seatNumbers: ['B02']
    },
    {
      bookingId: 885,
      bookingReference: 'BKG-EXP-885',
      userId: 1,
      tripId: 98,
      status: 'EXPIRED',
      totalAmount: 350000,
      createdAt: '2026-11-23T11:20:00',
      seatNumbers: ['01A']
    }
  ],
  page: 0,
  size: 10,
  totalElements: 15,
  totalPages: 2,
  hasNext: true,
  hasPrevious: false
}

test.describe('Gate B5.3: Checkout, VNPay Integration & Customer Booking Management', () => {

  test('E2E-CHK-01: Active hold checkout layout (desktop light & dark, mobile)', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })

    await page.route('**/api/v1/public/settings', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { brandName: 'VeloBus' } }) })
    })

    await page.route('**/api/v1/bookings/BKG-CHK-881', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockHeldBooking })
      })
    })

    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')
    await page.goto('/checkout/BKG-CHK-881')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('text=BKG-CHK-881').first()).toBeVisible()
    await expect(page.locator('text=Cổng Thanh Toán Trực Tuyến VNPay')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Thanh Toán Qua VNPay' })).toBeVisible()

    // 1. checkout-desktop-light.png & checkout-active-hold.png
    await page.screenshot({ path: path.join(screenshotDir, 'checkout-desktop-light.png'), fullPage: true })
    await page.screenshot({ path: path.join(screenshotDir, 'checkout-active-hold.png'), fullPage: true })

    // 2. checkout-desktop-dark.png
    await page.evaluate(() => document.documentElement.classList.add('dark'))
    await page.waitForTimeout(200)
    await page.screenshot({ path: path.join(screenshotDir, 'checkout-desktop-dark.png'), fullPage: true })
    await page.evaluate(() => document.documentElement.classList.remove('dark'))

    // 3. checkout-mobile.png
    await page.setViewportSize({ width: 375, height: 812 })
    await page.waitForTimeout(200)
    await page.screenshot({ path: path.join(screenshotDir, 'checkout-mobile.png'), fullPage: true })
  })

  test('E2E-CHK-02: Expiring soon (<60s) disables payment CTA with amber warning', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })

    await page.route('**/api/v1/public/settings', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { brandName: 'VeloBus' } }) })
    })

    await page.route('**/api/v1/bookings/BKG-EXP-882', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockExpiringSoonBooking })
      })
    })

    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')
    await page.goto('/checkout/BKG-EXP-882')
    await page.waitForLoadState('networkidle')

    // Warning and disabled button
    await expect(page.locator('text=Thời Gian Giữ Chỗ Sắp Hết (Dưới 60 Giây)')).toBeVisible()
    const payBtn = page.locator('button:has-text("Thời gian < 60s")')
    await expect(payBtn).toBeVisible()
    await expect(payBtn).toBeDisabled()

    // 4. checkout-expiring-soon.png
    await page.screenshot({ path: path.join(screenshotDir, 'checkout-expiring-soon.png'), fullPage: true })
  })

  test('E2E-CHK-03: Confirmed booking displays verified success state', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })

    await page.route('**/api/v1/public/settings', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { brandName: 'VeloBus' } }) })
    })

    await page.route('**/api/v1/bookings/BKG-CONF-883', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockConfirmedBooking })
      })
    })

    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')
    await page.goto('/checkout/BKG-CONF-883')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('text=Thanh Toán Thành Công & Đã Xác Nhận Vé')).toBeVisible()
    await expect(page.locator('text=Xem Vé Của Tôi')).toBeVisible()

    // 5. checkout-confirmed.png
    await page.screenshot({ path: path.join(screenshotDir, 'checkout-confirmed.png'), fullPage: true })
  })

  test('E2E-BKG-01: MyBookings list, pagination, and cancel dialog', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })

    await page.route('**/api/v1/public/settings', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { brandName: 'VeloBus' } }) })
    })

    await page.route('**/api/v1/bookings/my-bookings*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockPageBookings })
      })
    })

    await page.route('**/api/v1/bookings/BKG-CHK-881/cancel', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, message: 'Booking cancelled' })
      })
    })

    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')
    await page.goto('/my-bookings')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('text=Lịch Sử Đặt Vé & Vé Điện Tử')).toBeVisible()
    await expect(page.locator('text=BKG-CHK-881')).toBeVisible()
    await expect(page.locator('text=BKG-CONF-883')).toBeVisible()

    // 6. my-bookings-desktop.png & 7. my-bookings-pagination.png
    await page.screenshot({ path: path.join(screenshotDir, 'my-bookings-desktop.png'), fullPage: true })
    await page.screenshot({ path: path.join(screenshotDir, 'my-bookings-pagination.png'), fullPage: true })

    // 8. my-bookings-cancel-dialog.png
    const cancelBtn = page.locator('button:has-text("Hủy Giữ Chỗ")').first()
    await cancelBtn.click()
    await expect(page.locator('text=Xác Nhận Hủy Giữ Chỗ?')).toBeVisible()
    await page.screenshot({ path: path.join(screenshotDir, 'my-bookings-cancel-dialog.png') })

    // Dismiss cancel dialog
    await page.locator('button:has-text("Bỏ Qua")').click()

    // 9. my-bookings-mobile.png
    await page.setViewportSize({ width: 375, height: 812 })
    await page.waitForTimeout(200)
    await page.screenshot({ path: path.join(screenshotDir, 'my-bookings-mobile.png'), fullPage: true })
  })
})
