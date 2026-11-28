import { test, expect } from '@playwright/test'
import path from 'path'
import fs from 'fs'
import { fileURLToPath } from 'url'
import { loginAsRole } from './helpers/auth'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const closureScreenshotDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-gate-d1-closure')

if (!fs.existsSync(closureScreenshotDir)) {
  fs.mkdirSync(closureScreenshotDir, { recursive: true })
}

test.describe('Gate D1 Closure: Data Integrity & Error Recovery E2E Suite', () => {

  test('E2E-D1-01: Admin Banners Empty & Error States, Mutation Rejection & Position Enums', async ({ page }) => {
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')

    // Scenario 1: Empty list renders EmptyState without fallback fixtures
    await page.route('**/api/v1/admin/partners/banners*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'OK', data: [] })
      })
    })

    await page.goto('/admin/banners')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('h1')).toContainText('Kiểm Duyệt Banner Quảng Cáo Toàn Sàn')
    await expect(page.locator('text=Không có banner nào')).toBeVisible()
    await expect(page.locator('text=Phương Trang Velo')).not.toBeVisible()

    await page.screenshot({
      path: path.join(closureScreenshotDir, 'd1-closure-empty-banners.png'),
      fullPage: true
    })

    // Scenario 2: Error state with retry
    await page.route('**/api/v1/admin/partners/banners*', async route => {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'INTERNAL_ERROR', message: 'Lỗi cơ sở dữ liệu kết nối đối tác.' })
      })
    })

    await page.locator('button:has-text("Làm mới")').click()
    await page.waitForLoadState('networkidle')

    await expect(page.locator('text=Lỗi tải dữ liệu banner')).toBeVisible()
    await expect(page.locator('text=Lỗi cơ sở dữ liệu kết nối đối tác')).toBeVisible()

    await page.screenshot({
      path: path.join(closureScreenshotDir, 'd1-closure-error-banner-state.png'),
      fullPage: true
    })

    // Scenario 3: Backend rejection on create banner retains modal and user inputs
    const createBtn = page.locator('[data-testid="create-banner-btn"]')
    await createBtn.click()
    await expect(page.locator('text=Tạo Banner Truyền Thông Sàn')).toBeVisible()

    await page.locator('[data-testid="input-banner-title"]').fill('Chiến Dịch Lỗi Máy Chủ')
    await page.locator('[data-testid="input-banner-image"]').fill('https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=600')
    await page.locator('[data-testid="input-banner-target"]').fill('https://velobus.vn/search')

    await page.route('**/api/v1/admin/partners/banners/platform', async route => {
      await route.fulfill({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'INVALID_TITLE', message: 'Tiêu đề chiến dịch không được phép trùng lặp trong quý.' })
      })
    })

    await page.locator('[data-testid="btn-save-banner"]').click()
    await expect(page.locator('[role="alert"]').locator('text=Tiêu đề chiến dịch không được phép trùng lặp trong quý.')).toBeVisible()

    await page.screenshot({
      path: path.join(closureScreenshotDir, 'd1-closure-rejected-create-banner.png')
    })
    await page.keyboard.press('Escape')
  })

  test('E2E-D1-02: Image Upload Integrity & Preview Verification', async ({ page }) => {
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')

    await page.route('**/api/v1/admin/partners/banners*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'OK', data: [] })
      })
    })

    await page.goto('/admin/banners')
    await page.waitForLoadState('networkidle')
    await page.locator('[data-testid="create-banner-btn"]').click()

    // Mock upload failure
    await page.route('**/api/v1/public/upload', async route => {
      await route.fulfill({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'INVALID_IMAGE_CONTENT', message: 'Tệp tải lên không đúng chữ ký nhị phân hình ảnh hợp lệ.' })
      })
    })

    // Select synthetic dummy file
    const fileChooserPromise = page.waitForEvent('filechooser')
    await page.locator('text=Chọn hoặc kéo thả ảnh').click()
    const fileChooser = await fileChooserPromise
    await fileChooser.setFiles({
      name: 'corrupted.png',
      mimeType: 'image/png',
      buffer: Buffer.from('not-a-real-image-payload')
    })

    await expect(page.locator('[data-testid="uploader-error-msg"]')).toBeVisible()
    await page.screenshot({
      path: path.join(closureScreenshotDir, 'd1-closure-upload-failure.png')
    })

    // Mock upload success with deterministic synthetic image
    const validPngBuffer = Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
      'base64'
    )

    await page.route('https://storage.googleapis.com/**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'image/png',
        body: validPngBuffer
      })
    })

    await page.route('**/api/v1/public/upload', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'OK',
          data: {
            url: 'https://storage.googleapis.com/velobus/banners/valid.png',
            filename: 'valid.png'
          }
        })
      })
    })

    const fileChooserPromise2 = page.waitForEvent('filechooser')
    await page.locator('text=Chọn hoặc kéo thả ảnh').click()
    const fileChooser2 = await fileChooserPromise2
    await fileChooser2.setFiles({
      name: 'valid.png',
      mimeType: 'image/png',
      buffer: validPngBuffer
    })

    const previewImg = page.locator('[data-testid="img-uploader-preview"]')
    await expect(previewImg).toBeVisible()

    // Assert image.complete, naturalWidth > 0 and naturalHeight > 0
    const isImageValid = await previewImg.evaluate(img => {
      return img.complete && img.naturalWidth > 0 && img.naturalHeight > 0
    })
    expect(isImageValid).toBe(true)

    await page.screenshot({
      path: path.join(closureScreenshotDir, 'd1-closure-valid-image-preview.png')
    })
  })

  test('E2E-D1-03: User Profile Clean Synthetic Identity without Fabricated Contacts', async ({ page }) => {
    await loginAsRole(page, 'ROLE_CUSTOMER', 'synthetic_customer_42')

    await page.route('**/api/v1/auth/me', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'OK',
          data: {
            username: 'synthetic_customer_42',
            role: 'ROLE_CUSTOMER',
            customRole: null,
            roleName: null,
            permissions: ['BOOKING_VIEW', 'TICKET_DOWNLOAD']
          }
        })
      })
    })

    await page.goto('/profile')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('h1')).toContainText('Hồ Sơ Cá Nhân & Bảo Mật Tài Khoản')
    await expect(page.locator('text=@synthetic_customer_42')).toBeVisible()
    await expect(page.locator('text=ROLE_CUSTOMER').first()).toBeVisible()
    await expect(page.locator('text=Chưa liên kết trong phiên').first()).toBeVisible()

    // Assert no password inputs exist
    expect(await page.locator('input[type="password"]').count()).toBe(0)

    await page.screenshot({
      path: path.join(closureScreenshotDir, 'd1-closure-clean-profile-synthetic.png'),
      fullPage: true
    })
  })
})
