import { test, expect } from '@playwright/test'
import path from 'path'
import { fileURLToPath } from 'url'
import { loginAsRole } from './helpers/auth'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const d3ScreenshotDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-gate-d3')

const mockVehicles = [
  {
    id: 1,
    operatorId: 10,
    plateNumber: '29B-888.99',
    modelName: 'Thaco Mobihome VIP 2024',
    vehicleType: 'SLEEPER',
    totalSeats: 36,
    seatRows: 9,
    seatsPerRow: 4,
    vipRows: 2,
    status: 'ACTIVE'
  },
  {
    id: 2,
    operatorId: 10,
    plateNumber: '51B-123.45',
    modelName: 'Hyundai Universe Express',
    vehicleType: 'SEAT',
    totalSeats: 28,
    seatRows: 7,
    seatsPerRow: 4,
    vipRows: 0,
    status: 'MAINTENANCE'
  }
]

const mockDrivers = [
  {
    id: 101,
    operatorId: 10,
    fullName: 'Trần Văn Đức',
    phone: '0987654321',
    licenseNumber: 'GPLX-790123456',
    status: 'ACTIVE'
  },
  {
    id: 102,
    operatorId: 10,
    fullName: 'Lê Hoàng Nam',
    phone: '0912987654',
    licenseNumber: 'GPLX-801234567',
    status: 'ACTIVE'
  }
]

const mockStations = [
  { id: 1, name: 'Bến xe Miền Đông Mới', city: 'TP. Hồ Chí Minh' },
  { id: 2, name: 'Bến xe Liên Tỉnh Đà Lạt', city: 'Lâm Đồng' },
  { id: 3, name: 'Bến xe Trung Tâm Đà Nẵng', city: 'Đà Nẵng' }
]

const mockProfile = {
  id: 10,
  userId: 5,
  businessName: 'CÔNG TY CP XE KHÁCH PHƯƠNG TRANG FUTA BUS LINES',
  brandName: 'Phương Trang FUTA Bus Lines',
  taxCode: '0301118888',
  contactPhone: '1900606700',
  contactEmail: 'hotro@futa.vn',
  logoUrl: 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=300&q=80',
  description: 'Đơn vị vận tải hành khách chất lượng cao hàng đầu Việt Nam.',
  commissionRate: 8.5,
  status: 'ACTIVE',
  rejectionReason: null,
  createdAt: '2026-01-01T00:00:00'
}

test.describe('Gate D3 Operator Operations Evidence', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsRole(page, 'ROLE_OPERATOR', 'operator1')

    // Routes mock
    await page.route('**/api/v1/partners/vehicles*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'OK', data: mockVehicles })
      })
    })

    await page.route('**/api/v1/partners/drivers*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'OK', data: mockDrivers })
      })
    })

    await page.route('**/api/v1/catalog/stations*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'OK', data: mockStations })
      })
    })

    await page.route('**/api/v1/partners/me*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'OK', data: mockProfile })
      })
    })

    await page.route('**/api/v1/partners/trips*', async route => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'OK',
            data: {
              items: [],
              totalElements: 0,
              totalPages: 1,
              page: 0,
              size: 10
            }
          })
        })
      } else {
        await route.fallback()
      }
    })
  })

  test('D3-01: Capture FleetAndDriversView Evidence', async ({ page }) => {
    await page.goto('/operator/fleet')
    await page.waitForLoadState('networkidle')

    // 1. Fleet Vehicles Table
    await expect(page.locator('text=Quản Trị Đội Xe & Danh Sách Tài Xế')).toBeVisible()
    await expect(page.locator('text=29B-888.99')).toBeVisible()
    await expect(page.locator('text=51B-123.45')).toBeVisible()
    await page.screenshot({ path: path.join(d3ScreenshotDir, 'fleet-vehicles-list.png') })

    // 2. Open Add Vehicle Modal
    await page.locator('[data-testid="btn-add-vehicle"]').click()
    await expect(page.getByRole('dialog').locator('text=Thêm Phương Tiện Mới')).toBeVisible()
    await page.screenshot({ path: path.join(d3ScreenshotDir, 'vehicle-modal-form.png') })
    await page.keyboard.press('Escape')

    // 3. Open Maintenance Confirmation Dialog
    await page.getByRole('button', { name: 'Bảo Dưỡng' }).first().click()
    await expect(page.getByRole('alertdialog').locator('text=Xác nhận thay đổi trạng thái phương tiện')).toBeVisible()
    await page.screenshot({ path: path.join(d3ScreenshotDir, 'vehicle-maintenance-dialog.png') })
    await page.getByRole('button', { name: 'Hủy' }).click()

    // 4. Switch to Drivers Tab
    await page.locator('[data-testid="tab-drivers"]').click()
    await expect(page.locator('text=Trần Văn Đức')).toBeVisible()
    await expect(page.locator('text=Lê Hoàng Nam')).toBeVisible()
    await page.screenshot({ path: path.join(d3ScreenshotDir, 'fleet-drivers-list.png') })
  })

  test('D3-02: Capture OperatorTripsView Evidence & Turnaround Buffer Check', async ({ page }) => {
    await page.goto('/operator/trips')
    await page.waitForLoadState('networkidle')

    // 1. Initial Trips View with capability gap info
    await expect(page.locator('text=Lập Lịch Chuyến Xe & Điều Độ Vận Hành')).toBeVisible()
    await page.screenshot({ path: path.join(d3ScreenshotDir, 'trips-scheduling-view.png') })

    // 2. Open Schedule Trip Modal
    await page.locator('[data-testid="btn-open-schedule"]').click()
    await expect(page.getByRole('dialog').locator('text=Lập Lịch Chuyến Xe Mới')).toBeVisible()
    await page.screenshot({ path: path.join(d3ScreenshotDir, 'trip-schedule-modal.png') })

    // 3. Create a first trip to establish baseline schedule
    await page.route('**/api/v1/partners/trips*', async route => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'OK', data: { id: 101, tripCode: 'SG-DL-01', status: 'SCHEDULED' } })
        })
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'OK',
            data: {
              items: [
                {
                  id: 101,
                  tripCode: 'SG-DL-01',
                  vehicleId: 1,
                  departureTime: '2026-11-25T04:00',
                  arrivalTime: '2026-11-25T09:00',
                  status: 'SCHEDULED'
                }
              ],
              totalElements: 1,
              totalPages: 1,
              page: 0,
              size: 10
            }
          })
        })
      }
    })

    const selects = page.locator('select')
    await selects.nth(0).selectOption('1')
    await selects.nth(1).selectOption('1')
    await selects.nth(2).selectOption('2')
    await page.locator('input[placeholder="VD: SG-DL-0800"]').fill('SG-DL-01')
    await page.locator('input[type="datetime-local"]').first().fill('2026-11-25T04:00')
    await page.locator('input[type="datetime-local"]').nth(1).fill('2026-11-25T09:00')
    await page.getByRole('button', { name: 'Lập Lịch Chuyến Xe' }).click()

    // 4. Try scheduling a second trip for the same vehicle violating the 30-min buffer (at 09:15)
    await page.locator('[data-testid="btn-open-schedule"]').click()
    const selects2 = page.locator('select')
    await selects2.nth(0).selectOption('1')
    await selects2.nth(1).selectOption('1')
    await selects2.nth(2).selectOption('2')
    await page.locator('input[placeholder="VD: SG-DL-0800"]').fill('SG-DL-02')
    await page.locator('input[type="datetime-local"]').first().fill('2026-11-25T09:15')
    await page.locator('input[type="datetime-local"]').nth(1).fill('2026-11-25T15:00')

    await expect(page.locator('text=Yêu cầu thời gian đệm bảo dưỡng/vệ sinh tối thiểu 30 phút')).toBeVisible()
    await page.screenshot({ path: path.join(d3ScreenshotDir, 'trip-buffer-violation.png') })
  })

  test('D3-03: Capture OperatorProfileView Evidence & Mobile Responsive', async ({ page }) => {
    await page.goto('/operator/profile')
    await page.waitForLoadState('networkidle')

    // 1. Desktop Profile View with immutable lock badges
    await expect(page.locator('text=Hồ Sơ Doanh Nghiệp & Định Danh Nhà Xe')).toBeVisible()
    await expect(page.locator('text=Bất biến theo ĐKKD')).toBeVisible()
    await expect(page.locator('input[disabled]').first()).toHaveValue('CÔNG TY CP XE KHÁCH PHƯƠNG TRANG FUTA BUS LINES')
    await expect(page.locator('text=8.5%')).toBeVisible()
    await page.screenshot({ path: path.join(d3ScreenshotDir, 'operator-profile-view.png') })

    // 2. Mobile Viewport Profile
    await page.setViewportSize({ width: 375, height: 667 })
    await page.waitForTimeout(300)
    await page.screenshot({ path: path.join(d3ScreenshotDir, 'operator-profile-mobile.png') })
  })
})
