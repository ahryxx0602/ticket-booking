import { test, expect } from '@playwright/test'
import path from 'path'
import fs from 'fs'
import { fileURLToPath } from 'url'
import { loginAsRole } from './helpers/auth'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const screenshotDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-gate-d1')

if (!fs.existsSync(screenshotDir)) {
  fs.mkdirSync(screenshotDir, { recursive: true })
}

test.describe('Gate D1: Content & Account Management E2E Suite', () => {

  test('E2E-D1-01: Admin Banners View & Moderation Lifecycle', async ({ page }) => {
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')

    const mockBanners = [
      {
        id: 1,
        operatorId: 2,
        operatorName: 'Phương Trang Velo',
        title: 'Trải nghiệm Limousine VIP 24 Phòng Đôi sang trọng',
        imageUrl: 'https://images.unsplash.com/photo-1570125909232-eb263c188f7e?w=600&auto=format&fit=crop&q=60',
        targetUrl: 'https://velobus.vn/trips/TRIP-02/seats',
        position: 'HOME_CAROUSEL',
        status: 'PENDING_APPROVAL',
        priority: 1,
        createdAt: '2026-11-23T10:00:00Z'
      },
      {
        id: 2,
        operatorId: 1,
        operatorName: 'Hoàng Long Limousine',
        title: 'Khuyến mãi mùa hè: Giảm 15% chặng Hà Nội - Đà Nẵng',
        imageUrl: 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=600&auto=format&fit=crop&q=60',
        targetUrl: 'https://velobus.vn/search?from=HN-GP&to=DN-TT',
        position: 'HOME_CAROUSEL',
        status: 'ACTIVE',
        priority: 2,
        createdAt: '2026-11-20T10:00:00Z'
      }
    ]

    await page.route('**/api/v1/admin/partners/banners*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'OK', data: mockBanners })
      })
    })

    await page.goto('/admin/banners')
    await page.waitForLoadState('networkidle')

    // Expect header & table
    await expect(page.locator('h1')).toContainText('Kiểm Duyệt Banner Quảng Cáo Toàn Sàn')
    await expect(page.locator('text=Trải nghiệm Limousine VIP 24 Phòng Đôi')).toBeVisible()

    // 1. Capture Table Screenshot
    await page.screenshot({
      path: path.join(screenshotDir, 'd1-admin-banners-table.png'),
      fullPage: true
    })

    // 2. Open Create Banner Modal
    const createBtn = page.locator('[data-testid="create-banner-btn"]')
    await createBtn.click()
    await expect(page.locator('text=Tạo Banner Truyền Thông Sàn')).toBeVisible()

    await page.locator('[data-testid="input-banner-title"]').fill('Ưu Đãi Đặc Biệt Chào Xuân 2027')
    await page.screenshot({
      path: path.join(screenshotDir, 'd1-admin-create-banner-modal.png')
    })
    // Close modal
    await page.keyboard.press('Escape')

    // 3. Open Reject Reason Dialog
    const rejectBtn = page.locator('button:has-text("Gỡ")').first()
    if (await rejectBtn.isVisible()) {
      await rejectBtn.click()
      await expect(page.locator('text=Từ Chối & Gỡ Bỏ Banner')).toBeVisible()
      await page.locator('#reject-reason').fill('Hình ảnh mờ, không đạt tỷ lệ 16:9 tiêu chuẩn sàn')
      await page.screenshot({
        path: path.join(screenshotDir, 'd1-admin-reject-dialog.png')
      })
      await page.keyboard.press('Escape')
    }
  })

  test('E2E-D1-02: Operator Banners View & Registration Sheet', async ({ page }) => {
    await loginAsRole(page, 'ROLE_OPERATOR', 'operator1')

    const mockOpBanners = [
      {
        id: 1,
        title: 'Khuyến mãi mùa hè: Giảm 15% chặng Hà Nội - Đà Nẵng',
        imageUrl: 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=600&auto=format&fit=crop&q=60',
        targetUrl: 'https://velobus.vn/search?from=HN-GP&to=DN-TT',
        position: 'HOME_CAROUSEL',
        status: 'ACTIVE',
        registeredAt: '20/11/2026'
      },
      {
        id: 2,
        title: 'Trải nghiệm Limousine VIP 24 Phòng Đôi sang trọng',
        imageUrl: 'https://images.unsplash.com/photo-1570125909232-eb263c188f7e?w=600&auto=format&fit=crop&q=60',
        targetUrl: 'https://velobus.vn/trips/TRIP-02/seats',
        position: 'HOME_CAROUSEL',
        status: 'PENDING_APPROVAL',
        registeredAt: '23/11/2026'
      }
    ]

    await page.route('**/api/v1/partners/banners', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'OK', data: mockOpBanners })
      })
    })

    await page.goto('/operator/banners')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('h1')).toContainText('Banner & Chiến Dịch Quảng Cáo Sàn')
    await expect(page.locator('text=Trang Chủ (Hero Carousel)').first()).toBeVisible()

    // 4. Capture Operator Banners Table
    await page.screenshot({
      path: path.join(screenshotDir, 'd1-operator-banners-table.png'),
      fullPage: true
    })

    // 5. Open Right-Side Sheet (520px)
    await page.locator('[data-testid="btn-add-banner"]').click()
    await expect(page.locator('text=Đăng Ký Banner Quảng Cáo Mới')).toBeVisible()

    await page.locator('[data-testid="input-banner-title"]').fill('Giảm 20% Vé Xe Giường Nằm Nha Trang')
    await page.screenshot({
      path: path.join(screenshotDir, 'd1-operator-register-sheet.png')
    })
  })

  test('E2E-D1-03: User Profile & Security with Read-Only Notice and Telegram Modal', async ({ page }) => {
    await loginAsRole(page, 'ROLE_CUSTOMER', 'thanhpv')

    await page.route('**/api/v1/auth/me', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'OK',
          data: {
            username: 'thanhpv',
            fullName: 'Phan Văn Thành',
            email: 'thanh.pv@gmail.com',
            role: 'ROLE_CUSTOMER'
          }
        })
      })
    })

    await page.goto('/profile')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('h1')).toContainText('Hồ Sơ Cá Nhân & Bảo Mật Tài Khoản')
    await expect(page.locator('text=Chế độ hiển thị định danh xác thực (Read-only)')).toBeVisible()
    await expect(page.locator('text=ROLE_CUSTOMER')).toBeVisible()

    // 6. Capture Profile View Screenshot
    await page.screenshot({
      path: path.join(screenshotDir, 'd1-user-profile-readonly.png'),
      fullPage: true
    })

    // 7. Open Telegram Link Modal
    const tgBtn = page.locator('button:has-text("Liên Kết Telegram Bot")')
    await tgBtn.click()
    await expect(page.locator('text=Liên Kết Bot Telegram VeloBus')).toBeVisible()
    await expect(page.locator('[data-testid="img-telegram-qr"]')).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotDir, 'd1-telegram-modal.png')
    })
  })
})
