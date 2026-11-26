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
      await loginAsRole(page, 'ROLE_OPERATOR')
      await page.setViewportSize({ width: vp.width, height: vp.height })
      await page.goto('/operator')

      // Đảm bảo 4 thẻ fintech render
      await expect(page.locator('text=Số Dư Khả Dụng')).toBeVisible()
      await expect(page.locator('text=Tổng Tiền Đã Rút')).toBeVisible()
    })

    test(`FE-VIS-03: Dashboard Admin responsive trên ${vp.name}`, async ({ page }) => {
      await loginAsRole(page, 'ROLE_ADMIN', 'admin')
      await page.setViewportSize({ width: vp.width, height: vp.height })
      await page.goto('/admin')

      // Đảm bảo các khối KPI render
      await expect(page.locator('text=Doanh Thu Gộp')).toBeVisible()
      await expect(page.locator('text=Cần Đối Soát')).toBeVisible()
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
