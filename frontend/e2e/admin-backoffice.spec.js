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

  test('FE-ADM-E2E-04: Phân mục 3 nhóm User CRUD, Tạo Role theo Ma trận 7 Modules x 26 APIs và Xem thử giao diện theo Role', async ({ page }) => {
    await page.goto('/admin/users-bookings')

    // 1. Kiểm tra 3 phân mục người dùng (1. Khách hàng, 2. Chủ xe, 3. Phía hệ thống)
    await page.locator('[data-testid="category-tab-CUSTOMER"]').click()
    await expect(page.locator('text=Phan Văn Thành')).toBeVisible()

    await page.locator('[data-testid="category-tab-OPERATOR"]').click()
    await expect(page.locator('text=Nguyễn Hoàng Long')).toBeVisible()

    await page.locator('[data-testid="category-tab-SYSTEM"]').click()
    await expect(page.locator('text=Lê Thị Thu Hà (Kế Toán)')).toBeVisible()

    // 2. Chuyển sang Tab Quản lý Role & Phân quyền 26 APIs
    await page.locator('[data-testid="tab-admin-roles"]').click()
    await expect(page.locator('text=Phân Quyền Động Theo Ma Trận 7 Modules × 26 APIs')).toBeVisible()

    // 3. Tạo Role mới ROLE_MOD_BANNER với Module 4 (Banner & Truyền thông)
    await page.locator('[data-testid="btn-create-role-in-tab"]').click()
    await page.locator('[data-testid="input-role-code"]').fill('ROLE_MOD_BANNER')
    await page.locator('[data-testid="input-role-name"]').fill('Điều Hành Chiến Dịch Banner')
    await page.locator('[data-testid="btn-role-clear-all"]').click()
    await page.locator('[data-testid="toggle-module-MOD_BANNERS"]').click()
    await page.locator('[data-testid="btn-save-role"]').click()

    await expect(page.locator('[data-testid="role-card-ROLE_MOD_BANNER"]')).toBeVisible()

    // 4. Bấm "⚡ Xem Thử Giao Diện Với Role Này" trên ROLE_ACCOUNTANT và kiểm tra Banner Preview xuất hiện
    await page.locator('[data-testid="preview-role-ROLE_ACCOUNTANT"]').click()
    await expect(page.locator('[data-testid="role-preview-banner"]')).toBeVisible()
    await expect(page.locator('[data-testid="exit-role-preview-btn"]')).toBeVisible()

    // Thoát chế độ Xem thử
    await page.locator('[data-testid="exit-role-preview-btn"]').click()
    await expect(page.locator('[data-testid="role-preview-banner"]')).toHaveCount(0)
  })
})
