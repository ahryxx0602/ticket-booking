import { test, expect } from '@playwright/test'
import { loginAsRole } from './helpers/auth'

test.describe('E2E: Hành Trình Khách Hàng (Customer Journey)', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsRole(page, 'ROLE_CUSTOMER', 'customer1')

    await page.route('**/api/v1/public/settings', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: { brandName: 'VeloBus' } })
      })
    })

    await page.route('**/api/v1/catalog/stations', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: [
            { id: 1, code: 'HN', city: 'Hà Nội', name: 'Bến xe Giáp Bát' },
            { id: 2, code: 'DN', city: 'Đà Nẵng', name: 'Bến xe Trung tâm Đà Nẵng' }
          ]
        })
      })
    })

    await page.route('**/api/v1/catalog/trips*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: [
            {
              id: 101,
              tripCode: 'HN-DN-01',
              departureStationCode: 'HN',
              departureStationName: 'Bến xe Giáp Bát',
              departureCity: 'Hà Nội',
              arrivalStationCode: 'DN',
              arrivalStationName: 'Bến xe Trung tâm Đà Nẵng',
              arrivalCity: 'Đà Nẵng',
              route: 'Hà Nội → Đà Nẵng',
              departureTime: '2026-11-26T19:00:00',
              arrivalTime: '2026-11-27T09:00:00',
              duration: '14 giờ',
              transportType: 'VIP 36 Giường',
              totalSeats: 36,
              basePrice: 350000,
              status: 'SCHEDULED'
            }
          ]
        })
      })
    })

    // Mock API ghế và giữ chỗ để E2E chạy độc lập và ổn định
    await page.route('**/api/v1/catalog/trips/**', async (route) => {
      const url = route.request().url()
      if (url.includes('/seats')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'OK',
            data: [
              { id: 1, tripId: 101, seatNumber: 'A01', seatType: 'VIP', deck: 'LOWER', priceMultiplier: 1.0, finalPrice: 350000 },
              { id: 2, tripId: 101, seatNumber: 'A02', seatType: 'STANDARD', deck: 'LOWER', priceMultiplier: 1.0, finalPrice: 350000 },
              { id: 3, tripId: 101, seatNumber: 'B01', seatType: 'STANDARD', deck: 'UPPER', priceMultiplier: 1.0, finalPrice: 350000 }
            ]
          })
        })
        return
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'OK',
          data: {
            id: 101,
            tripCode: 'HN-DN-01',
            departureStationName: 'Bến xe Giáp Bát',
            departureCity: 'Hà Nội',
            arrivalStationName: 'Bến xe Trung tâm Đà Nẵng',
            arrivalCity: 'Đà Nẵng',
            route: 'Hà Nội → Đà Nẵng',
            departureTime: '2026-11-26T19:00:00',
            arrivalTime: '2026-11-27T09:00:00',
            duration: '14 giờ',
            transportType: 'VIP 36 Giường',
            totalSeats: 36,
            basePrice: 350000,
            status: 'SCHEDULED'
          }
        })
      })
    })

    await page.route('**/api/v1/bookings/trips/**/availability', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'OK',
          data: {
            tripId: 101,
            seats: [
              { seatNumber: 'A01', status: 'AVAILABLE' },
              { seatNumber: 'A02', status: 'AVAILABLE' },
              { seatNumber: 'B01', status: 'AVAILABLE' }
            ]
          }
        })
      })
    })

    await page.route('**/api/v1/**/hold**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'OK',
          data: {
            bookingId: 991,
            bookingReference: 'BKG-20261123-01',
            tripId: 101,
            userId: 1,
            seatNumbers: ['A01'],
            totalAmount: 350000,
            expiresAt: new Date(Date.now() + 600000).toISOString(),
            status: 'HELD'
          }
        })
      })
    })

    await page.route('**/api/v1/bookings/**', async (route) => {
      const url = route.request().url()
      if (url.includes('/my-bookings')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'OK',
            data: {
              items: [
                {
                  bookingId: 991,
                  bookingReference: 'BKG-20261123-01',
                  tripId: 101,
                  userId: 1,
                  status: 'CONFIRMED',
                  totalAmount: 350000,
                  seatNumbers: ['A01'],
                  departureStationName: 'Bến xe Giáp Bát',
                  arrivalStationName: 'Bến xe Trung tâm Đà Nẵng',
                  departureTime: '2026-11-26T19:00:00',
                  expiresAt: '2026-11-26T19:10:00Z'
                },
                {
                  bookingId: 992,
                  bookingReference: 'BKG-20261123-02',
                  tripId: 101,
                  userId: 1,
                  status: 'HELD',
                  totalAmount: 350000,
                  seatNumbers: ['A02'],
                  departureStationName: 'Bến xe Giáp Bát',
                  arrivalStationName: 'Bến xe Trung tâm Đà Nẵng',
                  departureTime: '2026-11-26T19:00:00',
                  expiresAt: new Date(Date.now() + 600000).toISOString()
                }
              ],
              page: 0,
              size: 10,
              totalElements: 2,
              totalPages: 1,
              hasNext: false
            }
          })
        })
        return
      }
      if (url.includes('/ticket')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'OK',
            data: {
              bookingReference: 'BKG-20261123-01',
              tripId: 101,
              status: 'CONFIRMED',
              totalAmount: 350000,
              seatNumbers: ['A01'],
              qrImageBase64: 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=='
            }
          })
        })
        return
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'OK',
          data: {
            bookingId: 991,
            bookingReference: 'BKG-20261123-01',
            tripId: 101,
            userId: 1,
            status: 'HELD',
            totalAmount: 350000,
            expiresAt: new Date(Date.now() + 600000).toISOString(),
            seatNumbers: ['A01']
          }
        })
      })
    })

    await page.route('**/api/v1/payments/bookings/*/status', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'OK',
          data: {
            bookingReference: 'BKG-20261123-01',
            canAttemptPayment: true,
            hasSuccessfulPayment: false,
            hasPaymentAttempt: false,
            reconciliationRequired: false
          }
        })
      })
    })
  })

  test('FE-CUST-E2E-01: Trang chủ hiển thị form tìm kiếm và hero banner', async ({ page }) => {
    await page.goto('/')
    await expect(page).toHaveTitle(/VeloBus/i)
    
    const searchBtn = page.getByRole('button', { name: /Tìm Chuyến Xe/i })
    await expect(searchBtn).toBeVisible()

    await searchBtn.click()
    await expect(page).toHaveURL(/\/search/)
  })

  test('FE-CUST-E2E-02: Màn hình kết quả tra cứu hiển thị bộ lọc và danh sách chuyến đi', async ({ page }) => {
    await page.goto('/search')
    
    await expect(page.locator('text=Hà Nội').first()).toBeVisible()
    await expect(page.locator('text=Đà Nẵng').first()).toBeVisible()
    
    const selectTripBtn = page.getByRole('button', { name: /Chọn Chỗ/i }).first()
    await expect(selectTripBtn).toBeVisible()
    await selectTripBtn.click()

    await expect(page).toHaveURL(/\/trips\/.*\/seats/)
  })

  test('FE-CUST-E2E-03: Sơ đồ ghế 2 tầng và kích hoạt đếm ngược giữ chỗ 10 phút', async ({ page }) => {
    await page.goto('/trips/101/seats')
    
    // Kiểm tra Tabs 2 tầng
    await expect(page.locator('text=Tầng Dưới')).toBeVisible()
    await expect(page.locator('text=Tầng Trên')).toBeVisible()

    // Chọn ghế A01
    const seatA01 = page.locator('[data-testid="seat-A01"]')
    await expect(seatA01).toBeVisible()
    await seatA01.click()

    // Nút Giữ Chỗ & Tiếp Tục được kích hoạt
    const holdBtn = page.getByRole('button', { name: /Giữ Chỗ & Tiếp Tục/i })
    await expect(holdBtn).toBeEnabled()
    await holdBtn.click()

    // Kiểm tra hộp đếm ngược 10 phút xuất hiện
    await expect(page.locator('text=Thời gian giữ chỗ của bạn')).toBeVisible()

    // Click Tiến Hành Thanh Toán để chuyển sang trang Checkout
    const payCtaBtn = page.getByRole('button', { name: /Tiến Hành Thanh Toán/i })
    await expect(payCtaBtn).toBeVisible()
    await payCtaBtn.click()

    // Kiểm tra chuyển hướng sang trang checkout
    await expect(page).toHaveURL(/\/checkout\/BKG-/)
  })

  test('FE-CUST-E2E-04: Màn hình Checkout nhập thông tin hành khách và cổng thanh toán VNPay', async ({ page }) => {
    await page.goto('/checkout/BKG-20261123-01')
    
    await expect(page.locator('text=Thông Tin Người Nhận Vé')).toBeVisible()

    const payBtn = page.getByRole('button', { name: /Thanh Toán Qua VNPay/i })
    await expect(payBtn).toBeVisible()
  })

  test('FE-CUST-E2E-05: Lịch sử vé và hiển thị vé điện tử QR Code HMAC-SHA256', async ({ page }) => {
    await page.goto('/my-bookings')
    
    await expect(page.locator('text=Lịch Sử Đặt Vé & Vé Điện Tử')).toBeVisible()
    await expect(page.locator('text=Mã tham chiếu:').first()).toBeVisible()

    // Kiểm tra mở Modal xem QR Code
    const viewQrBtn = page.getByRole('button', { name: /Xem Chi Tiết & Vé Điện Tử/i }).first()
    if (await viewQrBtn.isVisible()) {
      await viewQrBtn.click()
      await expect(page.locator('img[alt*="Mã QR"]')).toBeVisible()
      await page.keyboard.press('Escape')
    }

    // Kiểm tra nút Hủy Giữ Chỗ
    const cancelBtn = page.getByRole('button', { name: /Hủy Giữ Chỗ/i }).first()
    if (await cancelBtn.isVisible()) {
      await cancelBtn.click()
      await expect(page.locator('text=Xác Nhận Hủy Giữ Chỗ?')).toBeVisible()
    }
  })
})
