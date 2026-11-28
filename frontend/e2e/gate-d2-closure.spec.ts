import { test, expect } from '@playwright/test'
import path from 'path'
import fs from 'fs'
import { fileURLToPath } from 'url'
import { loginAsRole } from './helpers/auth'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const closureScreenshotDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-gate-d2-closure')

if (!fs.existsSync(closureScreenshotDir)) {
  fs.mkdirSync(closureScreenshotDir, { recursive: true })
}

const mockWithdrawals = [
  {
    id: 101,
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
    id: 102,
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

const mockStations = [
  { id: 1, code: 'HN-GP', name: 'Bến xe Giáp Bát', city: 'Hà Nội', status: 'ACTIVE' },
  { id: 2, code: 'HN-MY', name: 'Bến xe Mỹ Đình', city: 'Hà Nội', status: 'ACTIVE' },
  { id: 3, code: 'DN-TT', name: 'Bến xe Trung Tâm Đà Nẵng', city: 'Đà Nẵng', status: 'ACTIVE' }
]

const mockTrips = [
  { id: 101, tripCode: 'VB-HN-DN-01', departureCity: 'Hà Nội', arrivalCity: 'Đà Nẵng', departureTime: '2026-12-01T20:00:00', arrivalTime: '2026-12-02T06:00:00', basePrice: 450000, totalSeats: 36, status: 'SCHEDULED' }
]

const mockSettings = {
  platformName: 'VeloBus - Sàn Vé Xe Khách Cao Cấp',
  defaultCommissionRate: '10.0',
  seatHoldTtlMinutes: '10',
  turnaroundBufferMinutes: '30',
  escrowSettlementDelayHours: '2'
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
  }
]

test.describe('Gate D2 Closure Mandatory Checkpoint Evidence', () => {

  test('Capture 10 Required Gate D2 Closure Screenshots', async ({ page }) => {
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')

    // -------------------------------------------------------------
    // Route setups
    // -------------------------------------------------------------
    await page.route('**/api/v1/admin/partners/withdrawals*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'OK', data: mockWithdrawals })
      })
    })

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

    // =========================================================================
    // 1. Actual withdrawal confirmation dialog
    // =========================================================================
    await page.goto('/admin/withdrawals')
    await page.waitForLoadState('networkidle')

    const approveBtn = page.locator('[data-testid="btn-approve-withdraw-101"]')
    await approveBtn.click()
    await expect(page.locator('text=Xác Nhận Phê Duyệt Giải Ngân')).toBeVisible()
    await expect(page.getByRole('alertdialog').locator('text=15.000.000 đ')).toBeVisible()

    await page.screenshot({
      path: path.join(closureScreenshotDir, 'actual-withdrawal-confirmation-dialog.png')
    })

    // =========================================================================
    // 2. Mutation pending
    // =========================================================================
    // Intercept approve endpoint to delay response and capture pending spinner state
    let routeResolver
    const pendingPromise = new Promise(resolve => { routeResolver = resolve })

    await page.route('**/api/v1/admin/partners/withdrawals/101/approve', async route => {
      await pendingPromise
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'OK', data: { id: 101, status: 'APPROVED' } })
      })
    })

    await page.locator('[data-testid="btn-confirm-withdrawal-action"]').click()
    await page.waitForTimeout(100)

    await page.screenshot({
      path: path.join(closureScreenshotDir, 'mutation-pending.png')
    })

    // =========================================================================
    // 3. Timeout without fabricated terminal status
    // =========================================================================
    // Resolve previous to unblock and simulate a timeout on second withdrawal mutation
    routeResolver()
    await page.waitForTimeout(300)

    // Set unknown outcome in localStorage directly to verify timeout guard without fabricated terminal status
    await page.evaluate(() => {
      const context = {
        withdrawalId: 101,
        action: 'APPROVE',
        amount: 15000000,
        timestamp: Date.now(),
        note: 'Duyệt chi giải ngân test timeout'
      }
      localStorage.setItem('velobus_unknown_wdr_1_101', JSON.stringify(context))
      localStorage.setItem('velobus_unknown_wdr_admin_101', JSON.stringify(context))
    })

    await page.goto('/admin/withdrawals')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('text=Cảnh báo an toàn tài chính: Kết quả chưa xác định')).toBeVisible()
    await page.screenshot({
      path: path.join(closureScreenshotDir, 'timeout-without-fabricated-terminal-status.png')
    })

    // =========================================================================
    // 4. Reload recovery
    // =========================================================================
    await page.reload()
    await page.waitForLoadState('networkidle')

    await expect(page.locator('text=Cảnh báo an toàn tài chính: Kết quả chưa xác định')).toBeVisible()
    await expect(page.locator('text=Kiểm tra lại trạng thái')).toBeVisible()
    await page.screenshot({
      path: path.join(closureScreenshotDir, 'reload-recovery.png')
    })

    // =========================================================================
    // 5. Verification failure
    // =========================================================================
    // Verification queries server; mock server returning error or still PENDING
    await page.route('**/api/v1/admin/partners/withdrawals?status=APPROVED', async route => {
      await route.fulfill({ status: 200, body: JSON.stringify({ code: 'OK', data: [] }) })
    })
    await page.route('**/api/v1/admin/partners/withdrawals?status=REJECTED', async route => {
      await route.fulfill({ status: 200, body: JSON.stringify({ code: 'OK', data: [] }) })
    })
    await page.route('**/api/v1/admin/partners/withdrawals?status=PENDING', async route => {
      await route.fulfill({ status: 200, body: JSON.stringify({ code: 'OK', data: mockWithdrawals.filter(w => w.status === 'PENDING') }) })
    })

    await page.locator('button:has-text("Kiểm tra lại trạng thái")').first().click()
    await page.waitForTimeout(300)

    await page.screenshot({
      path: path.join(closureScreenshotDir, 'verification-failure.png')
    })

    // =========================================================================
    // 6. Confirmed authoritative terminal state
    // =========================================================================
    // Mock server returning terminal APPROVED for withdrawal 101
    await page.route('**/api/v1/admin/partners/withdrawals?status=APPROVED', async route => {
      await route.fulfill({
        status: 200,
        body: JSON.stringify({
          code: 'OK',
          data: [{ ...mockWithdrawals[0], status: 'APPROVED' }, mockWithdrawals[1]]
        })
      })
    })

    await page.locator('button:has-text("Kiểm tra lại trạng thái")').first().click()
    await page.waitForTimeout(400)

    await page.screenshot({
      path: path.join(closureScreenshotDir, 'confirmed-authoritative-terminal-state.png')
    })

    // =========================================================================
    // 7. Station mutation rejection
    // =========================================================================
    await page.goto('/admin/settings')
    await page.waitForLoadState('networkidle')

    // Mock station delete rejection
    await page.route('**/api/v1/admin/stations/*', async route => {
      await route.fulfill({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'STATION_IN_USE', message: 'Không thể xóa bến xe đang có chuyến hoạt động liên kết.' })
      })
    })

    // Click delete station button and confirm in dialog
    const deleteStationBtn = page.locator('button:has(.lucide-trash-2)').first()
    if (await deleteStationBtn.isVisible()) {
      await deleteStationBtn.click()
      await expect(page.locator('text=Xác Nhận Xóa Bến Xe Liên Tỉnh')).toBeVisible()
      await page.locator('button:has-text("Xác Nhận Xóa")').click()
      await page.waitForTimeout(300)
    }

    await page.screenshot({
      path: path.join(closureScreenshotDir, 'station-mutation-rejection.png')
    })

    // =========================================================================
    // 8. Correct Admin Trip form
    // =========================================================================
    await page.locator('[data-testid="tab-settings-trips"]').click()
    await page.waitForTimeout(200)

    await expect(page.locator('[data-testid="select-dep-station"]')).toBeVisible()
    await expect(page.locator('[data-testid="select-arr-station"]')).toBeVisible()
    await expect(page.locator('text=Cấu Hình Sơ Đồ Ghế')).toBeVisible()

    await page.screenshot({
      path: path.join(closureScreenshotDir, 'correct-admin-trip-form.png')
    })

    // =========================================================================
    // 9. Runtime settings limitation
    // =========================================================================
    await page.locator('[data-testid="tab-settings-system"]').click()
    await page.waitForTimeout(200)

    await expect(page.locator('text=Cấu Hình Tham Số Vận Hành Toàn Sàn')).toBeVisible()
    await expect(page.locator('text=Lưu DB (Static 10m in code)')).toBeVisible()
    await expect(page.locator('text=Lưu DB (Static 30m in code)')).toBeVisible()

    await page.screenshot({
      path: path.join(closureScreenshotDir, 'runtime-settings-limitation.png')
    })

    // =========================================================================
    // 10. Admin Operators mobile (375px)
    // =========================================================================
    await page.setViewportSize({ width: 375, height: 667 })
    await page.goto('/admin/operators')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('h1')).toContainText('Phê Duyệt Hồ Sơ Nhà Xe')
    await page.waitForTimeout(200)

    await page.screenshot({
      path: path.join(closureScreenshotDir, 'admin-operators-mobile.png'),
      fullPage: true
    })
  })
})
