import { test, expect } from '@playwright/test'
import path from 'path'
import fs from 'fs'
import { fileURLToPath } from 'url'
import { loginAsRole } from './helpers/auth.js'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const screenshotDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-gate-b5-3-enhancement')
const legacyScreenshotDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-gate-b5-3')

for (const dir of [screenshotDir, legacyScreenshotDir]) {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true })
  }
}

async function captureDual(page, filename, options = {}) {
  await page.screenshot({ path: path.join(screenshotDir, filename), ...options })
  await page.screenshot({ path: path.join(legacyScreenshotDir, filename), ...options })
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

const mockPageBookingsPage0 = {
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
    }
  ],
  page: 0,
  size: 2,
  totalElements: 4,
  totalPages: 2,
  hasNext: true,
  hasPrevious: false
}

const mockPageBookingsPage1 = {
  items: [
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
  page: 1,
  size: 2,
  totalElements: 4,
  totalPages: 2,
  hasNext: false,
  hasPrevious: true
}

const mockQrData = {
  bookingReference: 'BKG-CONF-883',
  tripId: 101,
  status: 'CONFIRMED',
  totalAmount: 350000,
  seatNumbers: ['A03'],
  qrImageBase64: 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
  qrPayload: 'v1|BKG-CONF-883|101|d8c47b5e40e6c88f8d6f51cb7e8913b8364cb6b0a1a0f9b3b0dfb2fca019d53c',
  issuedAt: new Date().toISOString()
}

test.describe('Gate B5.3-E: Payment Lifecycle Closure & Electronic Ticket Integration', () => {

  test.beforeEach(async ({ page }) => {
    await page.route('**/api/v1/public/settings', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: { brandName: 'VeloBus' } })
      })
    })
  })

  test('E2E-CHK-01: Active hold layout, light, dark, mobile & semantic CTA', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })

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
    const ctaButton = page.getByRole('button', { name: 'Thanh Toán Qua VNPay' })
    await expect(ctaButton).toBeVisible()

    // 1. checkout-desktop-light.png & checkout-semantic-cta.png
    await captureDual(page, 'checkout-desktop-light.png', { fullPage: true })
    await captureDual(page, 'checkout-semantic-cta.png', { fullPage: true })

    // 2. checkout-desktop-dark.png
    await page.evaluate(() => document.documentElement.classList.add('dark'))
    await page.waitForTimeout(200)
    await captureDual(page, 'checkout-desktop-dark.png', { fullPage: true })
    await page.evaluate(() => document.documentElement.classList.remove('dark'))

    // 3. checkout-mobile.png
    await page.setViewportSize({ width: 375, height: 812 })
    await page.waitForTimeout(200)
    await captureDual(page, 'checkout-mobile.png', { fullPage: true })
  })

  test('E2E-CHK-02: Unknown outcome & return error display', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })

    await page.route('**/api/v1/bookings/BKG-CHK-881', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockHeldBooking })
      })
    })

    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')
    // Simulate invalid signature return redirect
    await page.goto('/checkout/BKG-CHK-881?vnp_Error=INVALID_SIGNATURE')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('text=Phát hiện phản hồi không hợp lệ từ cổng thanh toán')).toBeVisible()

    // 4. checkout-unknown-outcome.png
    await captureDual(page, 'checkout-unknown-outcome.png', { fullPage: true })
  })

  test('E2E-CHK-03: Delayed confirmation polling & reconciliation banner', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })

    await page.route('**/api/v1/bookings/BKG-CHK-881', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockHeldBooking })
      })
    })

    // Status query returns reconciliation required
    await page.route('**/api/v1/payments/bookings/BKG-CHK-881/status', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            hasPaymentAttempt: true,
            paymentReference: 'PAY-88100',
            paymentStatus: 'PENDING',
            reconciliationRequired: true
          }
        })
      })
    })

    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')
    await page.goto('/checkout/BKG-CHK-881?vnp_ResponseCode=00&vnp_TxnRef=PAY-88100')
    await page.waitForLoadState('networkidle')

    // Check polling indicator
    await expect(page.locator('text=Đang đối soát kết quả giao dịch từ máy chủ VeloBus...')).toBeVisible()

    // 5. checkout-delayed-confirmation.png
    await captureDual(page, 'checkout-delayed-confirmation.png', { fullPage: true })

    // Check reconciliation alert banner
    await expect(page.locator('text=Đang Đối Soát Giao Dịch')).toBeVisible()

    // 6. checkout-reconciliation.png
    await captureDual(page, 'checkout-reconciliation.png', { fullPage: true })
  })

  test('E2E-CHK-04: Confirmed booking screen', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })

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

    // 7. checkout-confirmed.png
    await captureDual(page, 'checkout-confirmed.png', { fullPage: true })
  })

  test('E2E-BKG-01: Server-side status filter, pagination & destructive cancel modal', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })

    await page.route('**/api/v1/bookings/my-bookings*', async (route) => {
      const url = new URL(route.request().url())
      const pageParam = url.searchParams.get('page')
      if (pageParam === '1') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockPageBookingsPage1 })
        })
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockPageBookingsPage0 })
        })
      }
    })

    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')
    await page.goto('/my-bookings')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('text=Lịch Sử Đặt Vé & Vé Điện Tử')).toBeVisible()
    await expect(page.locator('text=BKG-CHK-881')).toBeVisible()

    // Click tab "Đang giữ chỗ"
    const heldTab = page.locator('button:has-text("Đang giữ chỗ")')
    await heldTab.click()
    await page.waitForTimeout(200)

    // 8. my-bookings-status-filter.png
    await captureDual(page, 'my-bookings-status-filter.png', { fullPage: true })

    // Return to "Tất cả" tab
    await page.locator('button:has-text("Tất cả")').click()
    await page.waitForTimeout(200)

    // Test second page
    const nextBtn = page.getByRole('button', { name: 'Sau' })
    await nextBtn.click()
    await page.waitForTimeout(200)
    await expect(page.locator('text=Trang 2 / 2')).toBeVisible()

    // 9. my-bookings-second-page.png
    await captureDual(page, 'my-bookings-second-page.png', { fullPage: true })

    // Back to page 1
    await page.getByRole('button', { name: 'Trước' }).click()
    await page.waitForTimeout(200)

    // Click cancel button for BKG-CHK-881
    const cancelBtn = page.locator('button:has-text("Hủy Giữ Chỗ")').first()
    await cancelBtn.click()
    await expect(page.locator('text=Xác Nhận Hủy Giữ Chỗ?')).toBeVisible()

    // 10. my-bookings-cancel-unresolved.png
    await captureDual(page, 'my-bookings-cancel-unresolved.png')

    // Close cancel dialog
    await page.locator('button:has-text("Bỏ Qua")').click()
  })

  test('E2E-BKG-02: Authorized electronic ticket dialog with signed QR code & unauthorized handling', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })

    await page.route('**/api/v1/bookings/my-bookings*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockPageBookingsPage0 })
      })
    })

    // Successful ticket retrieval
    await page.route('**/api/v1/bookings/BKG-CONF-883/ticket', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockQrData })
      })
    })

    // Ineligible / unauthorized ticket retrieval simulation
    await page.route('**/api/v1/bookings/BKG-CHK-881/ticket', async (route) => {
      await route.fulfill({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({
          success: false,
          code: 'BOOKING_NOT_ELIGIBLE_FOR_TICKET',
          message: 'Đơn đặt chỗ chưa ở trạng thái CONFIRMED để xuất vé điện tử'
        })
      })
    })

    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')
    await page.goto('/my-bookings')
    await page.waitForLoadState('networkidle')

    // Open electronic ticket for BKG-CONF-883
    const viewTicketBtn = page.locator('button:has-text("Xem Chi Tiết & Vé Điện Tử")').first()
    await viewTicketBtn.click()

    await expect(page.locator('text=Thông Tin Vé Điện Tử VeloBus')).toBeVisible()
    await expect(page.locator('text=Vé Điện Tử Hợp Lệ & Có Chữ Ký Số')).toBeVisible()
    await expect(page.locator('img[alt="Mã QR Vé Điện Tử"]')).toBeVisible()

    // 11. my-bookings-ticket-dialog-qr.png
    await captureDual(page, 'my-bookings-ticket-dialog-qr.png')

    // Close dialog
    await page.locator('button:has-text("Đóng")').first().click()

    // Test unauthorized / ineligible ticket error display
    // Manually trigger openTicketDetail on BKG-CHK-881 via client execution or component state
    await page.evaluate(async () => {
      const btn = document.createElement('button')
      btn.id = 'trigger-test-err'
      btn.onclick = async () => {
        const vue = window.__VUE_APP__
      }
      document.body.appendChild(btn)
    })
    // Simulate error dialog by visiting ticket endpoint failure directly in UI or testing ineligible state
    await page.route('**/api/v1/bookings/BKG-CONF-883/ticket', async (route) => {
      await route.fulfill({
        status: 403,
        contentType: 'application/json',
        body: JSON.stringify({
          success: false,
          code: 'FORBIDDEN',
          message: 'Quý khách không có quyền truy cập vé điện tử của đơn đặt chỗ này'
        })
      })
    })

    await viewTicketBtn.click()
    await expect(page.locator('text=Không Thể Cấp Vé Điện Tử')).toBeVisible()

    // 12. my-bookings-unauthorized-ticket.png
    await captureDual(page, 'my-bookings-unauthorized-ticket.png')
  })
})
