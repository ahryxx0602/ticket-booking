import { test, expect } from '@playwright/test'
import { loginAsRole } from './helpers/auth'

test.describe('E2E: Visual Regression & Layout Responsive Check', () => {
  const viewports = [
    { name: 'Mobile', width: 375, height: 667 },
    { name: 'Tablet', width: 768, height: 1024 },
    { name: 'Desktop', width: 1440, height: 900 },
  ]

  for (const vp of viewports) {
    test(`FE-VIS-01: Layout trang chủ responsive trên màn hình ${vp.name} (${vp.width}x${vp.height})`, async ({ page }) => {
      await page.setViewportSize({ width: vp.width, height: vp.height })
      await page.goto('/')

      // Đảm bảo phần tử chính của trang chủ render hoàn chỉnh
      await expect(page.locator('text=VeloBus').first()).toBeVisible()
      await expect(page.getByRole('button', { name: /Tìm Chuyến Xe/i })).toBeVisible()
    })

    test(`FE-VIS-02: Dashboard Operator responsive trên ${vp.name}`, async ({ page }) => {
      await page.route('**/api/v1/partners/wallet/transactions*', async (route) => {
        await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' })
      })
      await page.route('**/api/v1/partners/wallet/withdrawals*', async (route) => {
        await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' })
      })
      await page.route('**/api/v1/partners/wallet*', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 1,
            operatorId: 10,
            availableBalance: 42500000,
            pendingSettlementBalance: 18700000,
            lockedBalance: 5200000,
            debtBalance: 0,
            currency: 'VND'
          })
        })
      })
      await loginAsRole(page, 'ROLE_OPERATOR')
      await page.setViewportSize({ width: vp.width, height: vp.height })
      await page.goto('/operator')

      // Đảm bảo 4 thẻ fintech render
      await expect(page.locator('text=Số Dư Khả Dụng')).toBeVisible()
      await expect(page.locator('text=Tổng Tiền Đã Rút')).toBeVisible()
    })

    test(`FE-VIS-03: Dashboard Admin responsive trên ${vp.name}`, async ({ page }) => {
      await page.route('**/admin/payments/reports*', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            grossRevenue: 850000000,
            netRevenue: 85000000,
            successCount: 1420,
            failedCount: 15,
            pendingCount: 22,
            refundCount: 8,
            reconciliationCount: 5,
            commissionRate: 10
          })
        })
      })
      await page.route('**/admin/payments?*', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            items: [],
            totalElements: 0,
            totalPages: 0,
            size: 10,
            page: 0
          })
        })
      })
      await page.route('**/admin/payments', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            items: [],
            totalElements: 0,
            totalPages: 0,
            size: 10,
            page: 0
          })
        })
      })
      await loginAsRole(page, 'ROLE_ADMIN', 'admin')
      await page.setViewportSize({ width: vp.width, height: vp.height })
      await page.goto('/admin')

      // Đảm bảo các khối KPI render
      await expect(page.getByRole('heading', { name: 'Doanh Thu Gộp' })).toBeVisible()
      await expect(page.getByRole('heading', { name: 'Cần Đối Soát' })).toBeVisible()
    })
  }

  test('FE-VIS-04: Drawer thích ứng độ rộng chuẩn UX trên Mobile', async ({ page }) => {
    await loginAsRole(page, 'ROLE_OPERATOR')
    await page.setViewportSize({ width: 375, height: 667 })
    await page.goto('/operator/fleet')

    const addVehicleBtn = page.locator('[data-testid="btn-add-vehicle"]')
    await expect(addVehicleBtn).toBeVisible()
    await addVehicleBtn.click()
    
    // Kiểm tra Drawer hiển thị
    await expect(page.locator('text=Thêm Phương Tiện Mới Vào Đội Xe')).toBeVisible()
  })
})
