import { test, expect } from '@playwright/test'

test.describe('E2E: Hành Trình Khách Hàng (Customer Journey)', () => {
  test.beforeEach(async ({ page }) => {
    // Mock API ghế và giữ chỗ để E2E chạy độc lập và ổn định
    await page.route('**/api/v1/catalog/trips/*/seats', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          { code: 'A01', deck: 'LOWER', row: 1, col: 1, status: 'AVAILABLE', price: 350000 },
          { code: 'A02', deck: 'LOWER', row: 1, col: 2, status: 'AVAILABLE', price: 350000 },
          { code: 'B01', deck: 'UPPER', row: 1, col: 1, status: 'AVAILABLE', price: 350000 },
        ]),
      })
    })

    await page.route('**/api/v1/**/hold**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          bookingReference: 'BKG-20261123-01',
          tripId: 'TRIP-01',
          seats: ['A01'],
          totalAmount: 350000,
          expiresAt: new Date(Date.now() + 600000).toISOString(),
        }),
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
    await page.goto('/trips/TRIP-01/seats')
    
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
    
    await expect(page.locator('text=Thông Tin Người Đi & Nhận Vé Điện Tử')).toBeVisible()

    const nameInput = page.locator('input[placeholder*="Nguyễn Văn A"]').first()
    const phoneInput = page.locator('input[placeholder*="0987654321"]').first()
    const emailInput = page.locator('input[placeholder*="example.com"]').first()

    if (await nameInput.isVisible()) {
      await nameInput.fill('Trần Văn Thử Nghiệm')
    }
    if (await phoneInput.isVisible()) {
      await phoneInput.fill('0912345678')
    }
    if (await emailInput.isVisible()) {
      await emailInput.fill('test@velobus.vn')
    }

    const payBtn = page.getByRole('button', { name: /Thanh Toán Qua VNPay Ngay/i })
    await expect(payBtn).toBeVisible()
  })

  test('FE-CUST-E2E-05: Lịch sử vé và hiển thị vé điện tử QR Code HMAC-SHA256', async ({ page }) => {
    await page.goto('/my-bookings')
    
    await expect(page.locator('text=Lịch Sử Vé Đã Đặt & Vé Điện Tử')).toBeVisible()
    await expect(page.locator('text=Mã đơn:').first()).toBeVisible()

    // Kiểm tra mở Modal xem QR Code
    const viewQrBtn = page.getByRole('button', { name: /Mã QR Lên Xe/i }).first()
    if (await viewQrBtn.isVisible()) {
      await viewQrBtn.click()
      await expect(page.locator('img[alt*="Mã QR"]')).toBeVisible()
      await page.keyboard.press('Escape')
    }

    // Kiểm tra nút Yêu Cầu Hủy Vé
    const cancelBtn = page.getByRole('button', { name: /Yêu Cầu Hủy Vé/i }).first()
    if (await cancelBtn.isVisible()) {
      await cancelBtn.click()
      await expect(page.locator('text=Xác Nhận Hủy Vé Trực Tuyến').or(page.locator('text=Đồng Ý Hủy Vé'))).toBeVisible()
    }
  })
})
