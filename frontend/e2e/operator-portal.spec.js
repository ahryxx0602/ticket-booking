import { test, expect } from '@playwright/test'
import { loginAsRole } from './helpers/auth'

test.describe('E2E: Phân Hệ Đối Tác Nhà Xe (Operator Portal)', () => {
  test.beforeEach(async ({ page }) => {
    await page.route('**/api/v1/partners/wallet/transactions*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([])
      })
    })
    await page.route('**/api/v1/partners/wallet/withdrawals*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([])
      })
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
    await loginAsRole(page, 'ROLE_OPERATOR', 'operator1')
  })
  test('FE-OPER-E2E-01: Dashboard ví ký quỹ hiển thị 4 số dư Fintech và bảng giao dịch', async ({ page }) => {
    await page.goto('/operator')
    
    // Kiểm tra tiêu đề trang
    await expect(page.locator('text=Tổng Quan Ví Ký Quỹ Escrow')).toBeVisible()

    // 4 thẻ Fintech
    await expect(page.locator('text=Số Dư Khả Dụng')).toBeVisible()
    await expect(page.locator('text=Đang Ký Quỹ Chuyến')).toBeVisible()
    await expect(page.locator('text=Đang Xử Lý Rút')).toBeVisible()
    await expect(page.locator('text=Tổng Tiền Đã Rút')).toBeVisible()

    // Nút Yêu Cầu Rút Tiền
    const withdrawBtn = page.getByRole('button', { name: /Yêu Cầu Rút Tiền/i })
    await expect(withdrawBtn).toBeVisible()
    await withdrawBtn.click()

    // Modal rút tiền hiển thị
    await expect(page.locator('text=Yêu Cầu Rút Tiền Về Ngân Hàng')).toBeVisible()
    
    // Nhập số tiền vượt quá số dư khả dụng
    const amountInput = page.locator('[data-testid="input-withdraw-amount"]')
    await amountInput.fill('999999999')
    
    // Click gửi yêu cầu rút
    const submitBtn = page.locator('[data-testid="btn-submit-withdraw"]')
    await submitBtn.click()

    // Chốt chặn hiển thị lỗi vượt số dư
    await expect(page.locator('text=vượt quá số dư khả dụng')).toBeVisible()
  })

  test('FE-OPER-E2E-02: Quản lý đội xe và tài xế với Drawer 520px', async ({ page }) => {
    await page.goto('/operator/fleet')
    
    await expect(page.locator('text=Quản Trị Đội Xe & Danh Sách Tài Xế')).toBeVisible()

    // Mở Drawer thêm xe mới
    const addVehicleBtn = page.locator('[data-testid="btn-add-vehicle"]')
    await expect(addVehicleBtn).toBeVisible()
    await addVehicleBtn.click()

    // Drawer xuất hiện
    await expect(page.locator('text=Thêm Phương Tiện Mới Vào Đội Xe')).toBeVisible()
    
    // Thử nhập biển số sai định dạng
    const plateInput = page.locator('input[placeholder*="29B-"]').first()
    await plateInput.fill('BIEN_SO_SAI')
    
    const saveBtn = page.getByRole('button', { name: /Lưu Phương Tiện/i })
    await saveBtn.click()

    // Kiểm tra thông báo lỗi validation
    await expect(page.locator('text=không đúng định dạng')).toBeVisible()

    // Nhập biển số đúng định dạng
    await plateInput.fill('29B-999.99')
    await saveBtn.click()

    // Xe mới hiển thị trong bảng
    await expect(page.locator('text=29B-999.99')).toBeVisible()
  })

  test('FE-OPER-E2E-03: Lập chuyến xe và chốt chặn Turnaround Buffer 30 phút', async ({ page }) => {
    await page.goto('/operator/trips')
    
    await expect(page.locator('text=Lập Lịch Chuyến Xe & Quản Lý Giờ Chạy')).toBeVisible()

    // Mở Drawer 680px
    const addTripBtn = page.locator('[data-testid="btn-open-schedule"]')
    await expect(addTripBtn).toBeVisible()
    await addTripBtn.click()

    // Drawer hiển thị
    await expect(page.locator('text=Lập Lịch Chuyến Xe Mới (30-Min Buffer Check)')).toBeVisible()

    // Nhập giờ vi phạm Buffer 30 phút
    const depInput = page.locator('input[type="time"]').first()
    if (await depInput.isVisible()) {
      await depInput.fill('11:45')
      
      // Cảnh báo vi phạm xuất hiện
      await expect(page.locator('text=Vi Phạm Turnaround Buffer').or(page.locator('text=Thời gian xoay vòng'))).toBeVisible()
      
      // Nút xác nhận bị khóa
      const confirmTripBtn = page.getByRole('button', { name: /Xác Nhận Lập Chuyến/i })
      await expect(confirmTripBtn).toBeDisabled()
    }
  })
})
