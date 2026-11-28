import { test, expect } from '@playwright/test'
import path from 'path'
import fs from 'fs'
import { fileURLToPath } from 'url'
import { loginAsRole } from './helpers/auth'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const d2ScreenshotDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-gate-d2')

if (!fs.existsSync(d2ScreenshotDir)) {
  fs.mkdirSync(d2ScreenshotDir, { recursive: true })
}

const mockOperators = [
  {
    id: 1,
    brandName: 'Hoàng Long Limousine',
    businessName: 'Công ty TNHH Hoàng Long',
    taxCode: '0109876543',
    contactPhone: '0901234567',
    contactEmail: 'hoanglong@velobus.vn',
    representative: 'Nguyễn Hoàng Long',
    commissionRate: 8.5,
    status: 'ACTIVE'
  },
  {
    id: 2,
    brandName: 'Hải Vân Express',
    businessName: 'Công ty Vận Tải Hải Vân',
    taxCode: '0405566778',
    contactPhone: '0989112233',
    contactEmail: 'haivan@velobus.vn',
    representative: 'Trần Hải Vân',
    commissionRate: null,
    status: 'PENDING_APPROVAL'
  },
  {
    id: 3,
    brandName: 'Phương Trang Velo',
    businessName: 'Tập Đoàn Phương Trang',
    taxCode: '0301122334',
    contactPhone: '0918765432',
    contactEmail: 'phuongtrang@velobus.vn',
    representative: 'Lê Minh Phương',
    commissionRate: 10.0,
    status: 'SUSPENDED'
  }
]

const mockStations = [
  { id: 1, code: 'HN-GP', name: 'Bến xe Giáp Bát', city: 'Hà Nội', status: 'ACTIVE' },
  { id: 2, code: 'HN-MY', name: 'Bến xe Mỹ Đình', city: 'Hà Nội', status: 'ACTIVE' },
  { id: 3, code: 'DN-TT', name: 'Bến xe Trung Tâm Đà Nẵng', city: 'Đà Nẵng', status: 'ACTIVE' },
  { id: 4, code: 'SG-MD', name: 'Bến xe Miền Đông Mới', city: 'TP. Hồ Chí Minh', status: 'ACTIVE' },
  { id: 5, code: 'DL-LD', name: 'Bến xe Liên Tỉnh Đà Lạt', city: 'Lâm Đồng', status: 'ACTIVE' }
]

const mockTrips = [
  { id: 101, tripCode: 'VB-HN-DN-01', departureCity: 'Hà Nội', arrivalCity: 'Đà Nẵng', departureTime: '2026-12-01T20:00:00', basePrice: 450000, totalSeats: 34, status: 'SCHEDULED' },
  { id: 102, tripCode: 'VB-SG-DL-02', departureCity: 'TP. Hồ Chí Minh', arrivalCity: 'Đà Lạt', departureTime: '2026-12-02T22:30:00', basePrice: 380000, totalSeats: 24, status: 'SCHEDULED' }
]

const mockSettings = {
  platformName: 'VeloBus - Sàn Vé Xe Khách Cao Cấp',
  defaultCommissionRate: '10.0',
  seatHoldTtlMinutes: '10',
  turnaroundBufferMinutes: '30',
  escrowSettlementDelayHours: '2'
}

const mockWithdrawals = [
  {
    id: 'WDR-20261123-01',
    operatorId: 1,
    operatorName: 'Hoàng Long Limousine',
    amount: 15000000,
    bankName: 'Vietcombank',
    accountNumber: '0071001234567',
    accountHolderName: 'Hoàng Long Ltd',
    createdAt: '2026-11-23 10:15',
    status: 'PENDING'
  },
  {
    id: 'WDR-20261123-02',
    operatorId: 1,
    operatorName: 'Hoàng Long Limousine',
    amount: 20000000,
    bankName: 'Vietcombank',
    accountNumber: '0071001234567',
    accountHolderName: 'Hoàng Long Ltd',
    createdAt: '2026-11-23 11:00',
    status: 'PENDING'
  },
  {
    id: 'WDR-20261122-09',
    operatorId: 2,
    operatorName: 'Phương Trang Velo',
    amount: 35000000,
    bankName: 'Techcombank',
    accountNumber: '1903456789001',
    accountHolderName: 'Phương Trang Corp',
    createdAt: '2026-11-22 16:40',
    status: 'APPROVED'
  }
]

test.describe('Gate D2: Administration and Financial Operations E2E Suite', () => {

  test('E2E-D2-01: Admin Operators Management & Responsive Screenshots', async ({ page }) => {
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')

    await page.route('**/api/v1/admin/partners/operators*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'OK', data: mockOperators })
      })
    })

    await page.route('**/api/v1/admin/partners/commission/global*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'OK', data: { commissionRate: 10.0 } })
      })
    })

    await page.goto('/admin/operators')
    await page.waitForLoadState('networkidle')

    // 1. Light Mode View
    await expect(page.locator('h1')).toContainText('Phê Duyệt Hồ Sơ Nhà Xe & Cấu Hình Hoa Hồng 2 Cấp')
    await expect(page.locator('text=Hoàng Long Limousine')).toBeVisible()
    await expect(page.locator('text=Hải Vân Express')).toBeVisible()

    await page.screenshot({
      path: path.join(d2ScreenshotDir, 'd2-admin-operators-light.png'),
      fullPage: true
    })

    // 2. Dark Mode View
    await page.evaluate(() => {
      document.documentElement.classList.add('dark')
    })
    await page.waitForTimeout(300)
    await page.screenshot({
      path: path.join(d2ScreenshotDir, 'd2-admin-operators-dark.png'),
      fullPage: true
    })

    // Restore Light Mode
    await page.evaluate(() => {
      document.documentElement.classList.remove('dark')
    })

    // 3. Mobile View
    await page.setViewportSize({ width: 375, height: 667 })
    await page.waitForTimeout(300)
    await page.screenshot({
      path: path.join(d2ScreenshotDir, 'd2-admin-operators-mobile.png'),
      fullPage: true
    })
    await page.setViewportSize({ width: 1280, height: 800 })

    // 4. Operator Create Modal
    await page.locator('[data-testid="open-create-operator-btn"]').click()
    await expect(page.locator('text=Thêm Đối Tác Nhà Xe Mới')).toBeVisible()
    await page.screenshot({
      path: path.join(d2ScreenshotDir, 'd2-operator-create-edit.png')
    })
    await page.keyboard.press('Escape')

    // 5. Operator Moderation Note Confirmation Dialog
    const rejectBtn = page.locator('[data-testid="btn-reject-2"]')
    if (await rejectBtn.isVisible()) {
      await rejectBtn.click()
      await expect(page.locator('text=Từ Chối Hồ Sơ Đối Tác')).toBeVisible()
      await page.screenshot({
        path: path.join(d2ScreenshotDir, 'd2-operator-moderation-confirmation.png')
      })
      await page.keyboard.press('Escape')
    }

    // 6. 2-Tier Commission Modal
    await page.locator('[data-testid="btn-commission-1"]').click()
    await expect(page.locator('text=Cấu Hình Hoa Hồng — Hoàng Long Limousine')).toBeVisible()
    await page.screenshot({
      path: path.join(d2ScreenshotDir, 'd2-operator-commission-confirmation.png')
    })
    await page.keyboard.press('Escape')
  })

  test('E2E-D2-02: Admin Settings - Stations, Trips, System Parameters & Theme Density', async ({ page }) => {
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')

    await page.route('**/api/v1/admin/stations*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'OK', data: mockStations })
      })
    })

    await page.route('**/api/v1/admin/trips*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'OK', data: mockTrips })
      })
    })

    await page.route('**/api/v1/admin/settings*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'OK', data: mockSettings })
      })
    })

    await page.goto('/admin/settings')
    await page.waitForLoadState('networkidle')

    // 1. System Settings Section
    await expect(page.locator('text=Cấu Hình Tham Số Vận Hành Toàn Sàn')).toBeVisible()
    await page.locator('[data-testid="btn-save-settings"]').click()
    const confirmSaveBtn = page.locator('button:has-text("Lưu Cấu Hình")')
    if (await confirmSaveBtn.isVisible()) {
      await confirmSaveBtn.click()
    }
    await expect(page.locator('[data-testid="settings-saved-alert"]')).toBeVisible()
    await page.screenshot({
      path: path.join(d2ScreenshotDir, 'd2-settings-system.png')
    })

    // 2. Stations Section
    await expect(page.locator('text=Danh Mục Bến Xe Liên Tỉnh (5)')).toBeVisible()
    await page.screenshot({
      path: path.join(d2ScreenshotDir, 'd2-settings-stations.png')
    })

    // 3. Trips Section
    await expect(page.locator('text=Danh Mục Tuyến & Chuyến Vận Hành (2)')).toBeVisible()
    await page.screenshot({
      path: path.join(d2ScreenshotDir, 'd2-settings-trips.png')
    })

    // 4. Custom Theme & Density Propagation
    await page.evaluate(() => {
      document.documentElement.setAttribute('data-theme', 'emerald')
      document.documentElement.setAttribute('data-density', 'compact')
    })
    await page.waitForTimeout(300)
    await page.screenshot({
      path: path.join(d2ScreenshotDir, 'd2-custom-theme-density.png'),
      fullPage: true
    })
  })

  test('E2E-D2-03: Admin Withdrawals - Escrow Payout, Account Privacy & Unknown Outcome Guard', async ({ page }) => {
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')

    await page.route('**/api/v1/admin/partners/withdrawals*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'OK', data: mockWithdrawals })
      })
    })

    await page.goto('/admin/withdrawals')
    await page.waitForLoadState('networkidle')

    // 1. Withdrawal List with Bank Account Masking (••••4567)
    await expect(page.locator('h1')).toContainText('Kiểm Duyệt Lệnh Rút Tiền Ví Ký Quỹ Escrow')
    await expect(page.locator('text=••••4567').first()).toBeVisible()
    await page.screenshot({
      path: path.join(d2ScreenshotDir, 'd2-withdrawal-list.png'),
      fullPage: true
    })

    // 2. Pending filter
    await page.locator('button:has-text("Chờ giải ngân")').click()
    await page.waitForTimeout(200)
    await page.screenshot({
      path: path.join(d2ScreenshotDir, 'd2-withdrawal-pending.png')
    })

    // Switch to All before approving so the closed item remains visible in the table
    await page.locator('button:has-text("Tất cả lệnh")').click()
    await page.waitForTimeout(200)

    // 3. Approve Withdrawal Action
    await page.route('**/api/v1/admin/partners/withdrawals/*/approve', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'OK',
          data: {
            id: 'WDR-20261123-01',
            operatorId: 1,
            amount: 15000000,
            status: 'APPROVED'
          }
        })
      })
    })

    const approveBtn = page.locator('[data-testid="btn-approve-withdraw-WDR-20261123-01"]').first()
    if (await approveBtn.isVisible()) {
      await approveBtn.click()
      const confirmActionBtn = page.locator('[data-testid="btn-confirm-withdrawal-action"]')
      if (await confirmActionBtn.isVisible()) {
        await confirmActionBtn.click()
      }
      await expect(page.locator('text=Đã đóng lệnh').first()).toBeVisible()
      await page.screenshot({
        path: path.join(d2ScreenshotDir, 'd2-withdrawal-approval-confirmation.png')
      })
    }

    // 4. Unknown Outcome Guard Alert on Network Timeout
    await page.route('**/api/v1/admin/partners/withdrawals*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'OK', data: mockWithdrawals })
      })
    })
    await page.locator('button:has-text("Tất cả lệnh")').click()
    await page.waitForTimeout(200)

    await page.route('**/api/v1/admin/partners/withdrawals/*/reject', async route => {
      await route.abort('timedout')
    })

    const rejectBtn = page.locator('[data-testid="btn-reject-withdraw-WDR-20261123-02"]').first()
    await expect(rejectBtn).toBeVisible()
    await rejectBtn.click()

    const reasonBox = page.locator('textarea')
    await expect(reasonBox).toBeVisible()
    await reasonBox.fill('Thông tin tài khoản thụ hưởng không chính xác')

    const confirmActionBtn = page.locator('[data-testid="btn-confirm-withdrawal-action"]')
    await expect(confirmActionBtn).toBeVisible()
    await confirmActionBtn.click()

    await expect(page.locator('text=Cảnh báo an toàn tài chính: Kết quả chưa xác định')).toBeVisible()
    await page.screenshot({
      path: path.join(d2ScreenshotDir, 'd2-withdrawal-unknown-outcome.png')
    })

    // 5. Error & Retry State
    await page.route('**/api/v1/admin/partners/withdrawals*', async route => {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'DB_ERROR', message: 'Lỗi đồng bộ ví Escrow từ cơ sở dữ liệu.' })
      })
    })

    await page.locator('button:has-text("Làm mới")').first().click()
    await page.waitForTimeout(300)
    await expect(page.locator('text=Lỗi tải danh sách lệnh rút tiền')).toBeVisible()
    await page.screenshot({
      path: path.join(d2ScreenshotDir, 'd2-error-and-retry.png')
    })
  })
})
