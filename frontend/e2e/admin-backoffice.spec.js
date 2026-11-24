import { test, expect } from '@playwright/test'
import { loginAsRole } from './helpers/auth'

test.describe('E2E: Phân Hệ Ban Quản Trị (Admin Backoffice)', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
  })
  test('FE-ADM-E2E-01: Dashboard quản trị hiển thị KPI toàn sàn và cảnh báo đối soát VNPay', async ({ page }) => {
    await page.goto('/admin')
    
    // Tiêu đề trang
    await expect(page.locator('text=Tổng Quan Toàn Sàn & Báo Cáo Đối Soát')).toBeVisible()

    // 4 thẻ KPI toàn sàn
    await expect(page.locator('text=Tổng GMV Toàn Sàn')).toBeVisible()
    await expect(page.locator('text=Hoa Hồng Thực Thu')).toBeVisible()
    await expect(page.locator('text=Đơn Vé Thành Công')).toBeVisible()
    await expect(page.locator('text=Nhà Xe Hoạt Động')).toBeVisible()

    // Khối Cảnh báo Đối Soát VNPay
    await expect(page.locator('text=Nhật Ký Cảnh Báo Đối Soát Tự Động')).toBeVisible()
  })

  test('FE-ADM-E2E-02: Quản lý đối tác và cấu hình hoa hồng 2 cấp', async ({ page }) => {
    await page.goto('/admin/operators')
    
    await expect(page.locator('text=Phê Duyệt Hồ Sơ Nhà Xe & Cấu Hình Hoa Hồng 2 Cấp')).toBeVisible()

    // Mở Dialog cấu hình hoa hồng
    const configBtn = page.locator('button:has-text("Hoa Hồng")').first()
    await expect(configBtn).toBeVisible()
    await configBtn.click()

    // Modal cấu hình hoa hồng mở ra (dùng h3 để tránh strict mode violation)
    await expect(page.locator('h3:has-text("Cấu Hình Hoa Hồng 2 Cấp")')).toBeVisible()

    // Thử nhập tỷ lệ > 50%
    const rateInput = page.locator('[data-testid="input-custom-commission"]')
    if (await rateInput.isVisible()) {
      await rateInput.fill('75')
      const saveBtn = page.locator('[data-testid="btn-save-commission"]')
      await saveBtn.click()
      
      // Chốt chặn hiển thị lỗi validate biên 0% - 50%
      await expect(page.locator('text=phải nằm trong khoảng từ 0% đến 50%')).toBeVisible()
    }
  })

  test('FE-ADM-E2E-03: Chốt chặn ADMIN_CANNOT_LOCK_SELF và Hoàn tiền cưỡng chế Admin Refund', async ({ page }) => {
    await page.goto('/admin/users-bookings')
    
    await expect(page.locator('text=Quản Trị Người Dùng & Hoàn Tiền Cưỡng Chế (Admin Refund)')).toBeVisible()

    // Chốt chặn ADMIN_CANNOT_LOCK_SELF: Kiểm tra tài khoản admin hiện tại có badge bảo vệ
    const adminUserRow = page.locator('tr:has-text("admin@velobus.vn")')
    await expect(adminUserRow.locator('[data-testid="self-admin-guard"]')).toBeVisible()
    await expect(adminUserRow.locator('button:has-text("Khóa")')).toHaveCount(0)

    // Chuyển sang Tab 2: Bookings & Admin Refund bằng data-testid
    const tabBookingsBtn = page.locator('[data-testid="tab-admin-bookings"]')
    await expect(tabBookingsBtn).toBeVisible()
    await tabBookingsBtn.click()

    // Mở Dialog hoàn tiền trên đơn CONFIRMED
    const refundBtn = page.locator('[data-testid^="btn-admin-refund-"]').first()
    await expect(refundBtn).toBeVisible()
    await refundBtn.click()

    // Modal Hoàn Tiền Cưỡng Chế xuất hiện
    await expect(page.locator('text=Lệnh Hoàn Tiền Cưỡng Chế')).toBeVisible()

    // Validate lý do hoàn tiền >= 10 ký tự
    const reasonInput = page.locator('textarea').first()
    const confirmRefundBtn = page.locator('[data-testid="btn-confirm-refund"]')
    
    await reasonInput.fill('Lý do')
    await confirmRefundBtn.click()
    await expect(page.locator('text=tối thiểu 10 ký tự')).toBeVisible()

    await reasonInput.fill('Hành khách yêu cầu hủy chuyến trước 24h theo chính sách')
    await confirmRefundBtn.click()
    await expect(page.locator('text=Lệnh Hoàn Tiền Cưỡng Chế')).toHaveCount(0)
  })
})
