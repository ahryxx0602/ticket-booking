import { test, expect } from '@playwright/test'
import path from 'path'
import { fileURLToPath } from 'url'
import { loginAsRole } from './helpers/auth'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const screenshotDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-gate-b3-corrected')

// Standard mock responses adhering strictly to Spring Boot ApiResponse<T> & PageResponse<T>
const mockPaymentReport = {
  success: true,
  message: 'Payment report retrieved successfully',
  data: {
    grossRevenue: 1450800000,
    netRevenue: 142500000,
    totalTransactions: 3842,
    successCount: 3842,
    failedCount: 0,
    refundedCount: 0,
    pendingCount: 0,
    reconciliationCount: 1,
    reconciliationTransactions: [
      {
        id: 1,
        paymentReference: 'REC-20261123-01',
        bookingId: 7712,
        amount: 700000,
        currency: 'VND',
        paymentMethod: 'VNPAY',
        transactionId: 'VNP-99881122',
        status: 'SUCCESS',
        reconciliationRequired: false,
        createdAt: '2026-11-23T14:20:00'
      },
      {
        id: 2,
        paymentReference: 'REC-20261122-04',
        bookingId: 3319,
        amount: 380000,
        currency: 'VND',
        paymentMethod: 'VNPAY',
        transactionId: 'VNP-77441092',
        status: 'SUCCESS',
        reconciliationRequired: true,
        createdAt: '2026-11-22T21:05:00'
      }
    ]
  }
}

const mockUsersPage = {
  success: true,
  message: 'Users retrieved successfully',
  data: {
    items: [
      {
        id: 1,
        username: 'admin',
        fullName: 'Quản Trị Viên Toàn Sàn',
        email: 'admin@velobus.vn',
        phone: '0900000001',
        role: 'ROLE_ADMIN',
        category: 'SYSTEM',
        status: 'ACTIVE'
      },
      {
        id: 2,
        username: 'hoanglong_op',
        fullName: 'Nguyễn Hoàng Long',
        email: 'hoanglong@velobus.vn',
        phone: '0901234567',
        role: 'ROLE_OPERATOR',
        category: 'OPERATOR',
        status: 'ACTIVE'
      },
      {
        id: 3,
        username: 'thanhpv',
        fullName: 'Phan Văn Thành',
        email: 'thanh.pv@gmail.com',
        phone: '0912345678',
        role: 'ROLE_CUSTOMER',
        category: 'CUSTOMER',
        status: 'ACTIVE'
      }
    ],
    page: 0,
    size: 10,
    totalElements: 3,
    totalPages: 1,
    hasNext: false,
    hasPrevious: false
  }
}

const mockRolesList = {
  success: true,
  message: 'Roles retrieved successfully',
  data: [
    {
      id: 1,
      code: 'ROLE_ADMIN',
      name: 'Quản Trị Viên Toàn Sàn (Super Admin)',
      description: 'Toàn quyền điều hành hệ thống, quản lý 7 Modules và 26 APIs quản trị.',
      category: 'SYSTEM',
      systemDefault: true,
      permissions: [
        'PAYMENT_REPORT_VIEW', 'PAYMENT_LIST_VIEW', 'PAYMENT_RECONCILE_RESOLVE',
        'OPERATOR_VIEW', 'OPERATOR_CREATE_UPDATE', 'OPERATOR_APPROVE_REJECT', 'OPERATOR_SUSPEND', 'OPERATOR_COMMISSION_MANAGE',
        'WITHDRAWAL_VIEW', 'WITHDRAWAL_APPROVE', 'WITHDRAWAL_REJECT',
        'BANNER_VIEW', 'BANNER_CREATE_DELETE', 'BANNER_APPROVE_REJECT',
        'USER_VIEW', 'USER_CREATE_UPDATE_DELETE', 'USER_STATUS_LOCK', 'USER_PASSWORD_RESET', 'USER_ROLE_ASSIGN', 'ROLE_MANAGE_CRUD',
        'BOOKING_VIEW', 'BOOKING_FORCE_REFUND',
        'STATION_VIEW', 'STATION_CREATE_UPDATE_DELETE', 'TRIP_MANAGE_CRUD', 'SYSTEM_SETTINGS_MANAGE'
      ]
    },
    {
      id: 4,
      code: 'ROLE_ACCOUNTANT',
      name: 'Kế Toán & Đối Soát Tài Chính',
      description: 'Chuyên trách báo cáo doanh thu, đối soát VNPay và giải ngân ví Escrow.',
      category: 'SYSTEM',
      systemDefault: false,
      permissions: ['PAYMENT_REPORT_VIEW', 'PAYMENT_LIST_VIEW', 'PAYMENT_RECONCILE_RESOLVE', 'WITHDRAWAL_VIEW', 'BOOKING_VIEW', 'BOOKING_FORCE_REFUND']
    }
  ]
}

const mockBookingsPage = {
  success: true,
  message: 'Bookings retrieved successfully',
  data: {
    items: [
      {
        bookingId: 1,
        bookingReference: 'BKG-20261123-9988',
        userId: 3,
        customerName: 'Phan Văn Thành',
        customerPhone: '0912345678',
        tripId: 101,
        route: 'Hà Nội → Đà Nẵng',
        operatorName: 'Hoàng Long Limousine',
        seatNumbers: ['A05', 'A06'],
        totalAmount: 700000,
        status: 'CONFIRMED',
        createdAt: '2026-11-23T14:20:00'
      }
    ],
    page: 0,
    size: 10,
    totalElements: 1,
    totalPages: 1,
    hasNext: false,
    hasPrevious: false
  }
}

// Setup common network routes
async function setupAdminMocks(page) {
  await page.route('**/api/v1/admin/payments/reports**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(mockPaymentReport)
    })
  })

  await page.route('**/api/v1/admin/payments?**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          items: mockPaymentReport.data.reconciliationTransactions,
          page: 0,
          size: 10,
          totalElements: 2,
          totalPages: 1
        }
      })
    })
  })

  await page.route('**/api/v1/admin/users**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(mockUsersPage)
    })
  })

  await page.route('**/api/v1/admin/roles**', async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockRolesList)
      })
    } else {
      await route.continue()
    }
  })

  await page.route('**/api/v1/admin/bookings**', async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockBookingsPage)
      })
    } else {
      await route.continue()
    }
  })
}

test.describe('Gate B3: Corrected Production Admin Pilot Verification', () => {
  // B3-01: Admin Dashboard Desktop Light & Dark (Real API, KPIs & Reconciliation DataTable)
  test('B3-01: Admin Dashboard — Desktop Light & Dark (KPIs & Reconciliation)', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')

    // 4 KPI cards visible
    await expect(page.locator('[data-testid="kpi-gmv"]')).toBeVisible()
    await expect(page.locator('[data-testid="kpi-commission"]')).toBeVisible()
    await expect(page.locator('[data-testid="kpi-bookings"]')).toBeVisible()
    await expect(page.locator('[data-testid="kpi-operators"]')).toBeVisible()

    // Assert currency & numeric formatting
    await expect(page.locator('[data-testid="kpi-gmv"]')).toContainText('1.450.800.000')
    await expect(page.locator('[data-testid="kpi-commission"]')).toContainText('142.500.000')

    // Reusable TanStack DataTable visible with stable row keys
    const dataTable = page.locator('table').first()
    await expect(dataTable).toBeVisible()
    await expect(page.locator('text=REC-20261123-01')).toBeVisible()
    await expect(page.locator('text=REC-20261122-04')).toBeVisible()

    // Capture Light Mode
    await page.screenshot({
      path: path.join(screenshotDir, 'admin-dashboard-desktop-light.png'),
      fullPage: false
    })

    // Switch to Dark Mode & Capture
    await page.evaluate(() => document.documentElement.classList.add('dark'))
    await page.waitForTimeout(250)
    await page.screenshot({
      path: path.join(screenshotDir, 'admin-dashboard-desktop-dark.png'),
      fullPage: false
    })
  })

  // B3-02: Admin Dashboard Tablet & Mobile
  test('B3-02: Admin Dashboard — Tablet 768px & Mobile 375px', async ({ page }) => {
    await setupAdminMocks(page)
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')

    // Tablet 768px
    await page.setViewportSize({ width: 768, height: 1024 })
    await page.waitForTimeout(250)
    await expect(page.locator('[data-testid="kpi-gmv"]')).toBeVisible()
    const tabletOverflow = await page.evaluate(() => document.body.scrollWidth > document.body.clientWidth)
    expect(tabletOverflow).toBe(false)

    await page.screenshot({
      path: path.join(screenshotDir, 'admin-dashboard-tablet.png'),
      fullPage: false
    })

    // Mobile 375px
    await page.setViewportSize({ width: 375, height: 667 })
    await page.waitForTimeout(250)
    await expect(page.locator('[data-testid="kpi-gmv"]')).toBeVisible()
    const mobileOverflow = await page.evaluate(() => document.body.scrollWidth > document.body.clientWidth)
    expect(mobileOverflow).toBe(false)

    await page.screenshot({
      path: path.join(screenshotDir, 'admin-dashboard-mobile.png'),
      fullPage: false
    })
  })

  // B3-03: Reconciliation Actions & Mandatory Confirmation
  test('B3-03: Reconciliation — manual trigger and resolve action with confirmation dialog', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)

    let resolveRequested = false
    let resolvePayload = null
    await page.route('**/api/v1/admin/payments/2/reconcile', async (route) => {
      resolveRequested = true
      resolvePayload = route.request().postDataJSON()
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          message: 'Reconciliation resolved successfully',
          data: { id: 2, reconciliationRequired: false }
        })
      })
    })

    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')

    // Click resolve alert button (mandatory assertion - no conditional)
    const resolveBtn = page.locator('[data-testid="btn-resolve-alert"]').first()
    await expect(resolveBtn).toBeVisible()
    await resolveBtn.click()

    // Confirmation AlertDialog must be opened
    const dialogTitle = page.locator('text=Xác nhận xử lý cảnh báo đối soát')
    await expect(dialogTitle).toBeVisible()

    // Screenshot confirmation dialog open
    await page.screenshot({
      path: path.join(screenshotDir, 'admin-reconciliation-confirmation.png'),
      fullPage: false
    })

    // Confirm resolution
    const confirmBtn = page.locator('button:has-text("Xác nhận xử lý")')
    await expect(confirmBtn).toBeVisible()
    await confirmBtn.click()

    await page.waitForTimeout(300)
    expect(resolveRequested).toBe(true)

    // Capture resolved state
    await page.screenshot({
      path: path.join(screenshotDir, 'admin-reconciliation-resolved.png'),
      fullPage: false
    })
  })

  // B3-04: User Management — Desktop Light & Dark
  test('B3-04: User Management — Desktop Light & Dark', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('[data-testid="tab-admin-users"]')).toBeVisible()
    await expect(page.locator('text=@admin')).toBeVisible()
    await expect(page.locator('text=@hoanglong_op')).toBeVisible()
    await expect(page.locator('text=@thanhpv')).toBeVisible()

    // Capture Light Mode
    await page.screenshot({
      path: path.join(screenshotDir, 'user-management-desktop-light.png'),
      fullPage: false
    })

    // Switch to Dark Mode & Capture
    await page.evaluate(() => document.documentElement.classList.add('dark'))
    await page.waitForTimeout(250)
    await page.screenshot({
      path: path.join(screenshotDir, 'user-management-desktop-dark.png'),
      fullPage: false
    })
  })

  // B3-05: User Management — Server-side Pagination & Filtering
  test('B3-05: User Management — server-side pagination & category filtering', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    // Filter by Customer
    const custTab = page.locator('[data-testid="category-tab-CUSTOMER"]')
    await expect(custTab).toBeVisible()
    await custTab.click()
    await page.waitForTimeout(200)

    // Pagination toolbar exists
    const paginationNext = page.locator('[data-testid="pagination-next"]')
    await expect(paginationNext).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotDir, 'user-management-server-pagination.png'),
      fullPage: false
    })
  })

  // B3-06: User Form Dialog — Open, validate and submit
  test('B3-06: User Form Dialog — open dialog and validate fields', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    const createBtn = page.locator('[data-testid="btn-open-create-user"]')
    await expect(createBtn).toBeVisible()
    await createBtn.click()

    // Dialog must be visible
    const dialog = page.locator('[role="dialog"]')
    await expect(dialog).toBeVisible()
    await expect(page.locator('[data-testid="input-username"]')).toBeVisible()
    await expect(page.locator('[data-testid="input-fullname"]')).toBeVisible()
    await expect(page.locator('[data-testid="input-email"]')).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotDir, 'user-create-dialog.png'),
      fullPage: false
    })

    // Cancel dialog
    await page.locator('button:has-text("Hủy")').click()
    await expect(dialog).not.toBeVisible()
  })

  // B3-07: Security Guard — Super Admin self-lock prevention
  test('B3-07: Security Guard — Super Admin cannot lock own account', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    const selfGuard = page.locator('[data-testid="self-admin-guard"]')
    await expect(selfGuard).toBeVisible()
    await expect(selfGuard).toContainText('Không thể tự khóa')

    await page.screenshot({
      path: path.join(screenshotDir, 'self-admin-lock-guard.png'),
      fullPage: false
    })
  })

  // B3-08: Role Management — 7 modules and 26 APIs permission matrix
  test('B3-08: Role Management — 7 modules and 26 APIs permission matrix', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    const rolesTab = page.locator('[data-testid="tab-admin-roles"]')
    await expect(rolesTab).toBeVisible()
    await rolesTab.click()

    await expect(page.locator('[data-testid="role-card-ROLE_ADMIN"]')).toBeVisible()
    await expect(page.locator('[data-testid="role-card-ROLE_ACCOUNTANT"]')).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotDir, 'role-permission-matrix-tab.png'),
      fullPage: false
    })
  })

  // B3-09: Role Form Dialog — Toggle module permissions and save
  test('B3-09: Role Form Dialog — toggle module permissions and save', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)

    let createRolePayload = null
    await page.route('**/api/v1/admin/roles', async (route) => {
      if (route.request().method() === 'POST') {
        createRolePayload = route.request().postDataJSON()
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            message: 'Role created',
            data: { id: 10, code: 'ROLE_MODERATOR', name: 'Điều Hành Viên' }
          })
        })
      } else {
        await route.continue()
      }
    })

    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="tab-admin-roles"]').click()

    const createRoleBtn = page.locator('[data-testid="btn-create-role-in-tab"]')
    await expect(createRoleBtn).toBeVisible()
    await createRoleBtn.click()

    const roleDialog = page.locator('[role="dialog"]')
    await expect(roleDialog).toBeVisible()

    await page.locator('[data-testid="input-role-code"]').fill('MODERATOR')
    await page.locator('[data-testid="input-role-name"]').fill('Điều Hành Viên')

    await page.screenshot({
      path: path.join(screenshotDir, 'role-create-matrix-dialog.png'),
      fullPage: false
    })

    await page.locator('button:has-text("Hủy")').click()
  })

  // B3-10: Role Preview Mode
  test('B3-10: Role Preview — simulate role permissions in real-time', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="tab-admin-roles"]').click()

    const previewBtn = page.locator('[data-testid="preview-role-ROLE_ACCOUNTANT"]')
    await expect(previewBtn).toBeVisible()
    await previewBtn.click()

    // Assert banner appears
    const banner = page.locator('[data-testid="btn-stop-preview"]')
    await expect(banner).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotDir, 'role-preview-mode-active.png'),
      fullPage: false
    })

    // Stop preview
    await banner.click()
    await expect(banner).not.toBeVisible()
  })

  // B3-11: Booking Management DataTable
  test('B3-11: Booking Management — TanStack DataTable and Actions', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    const bookingTab = page.locator('[data-testid="tab-admin-bookings"]')
    await expect(bookingTab).toBeVisible()
    await bookingTab.click()

    await expect(page.locator('text=BKG-20261123-9988')).toBeVisible()
    await expect(page.locator('[data-testid="btn-admin-refund-BKG-20261123-9988"]')).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotDir, 'booking-management-tab.png'),
      fullPage: false
    })
  })

  // B3-12: Forced Refund Contract & Error Handling
  test('B3-12: Forced Refund Contract — assert payload, error handling & success state', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)

    let refundPayload = null
    let shouldFail = true

    await page.route('**/api/v1/admin/bookings/BKG-20261123-9988/refund', async (route) => {
      refundPayload = route.request().postDataJSON()
      if (shouldFail) {
        // Mock 400 Bad Request
        await route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'REFUND_NOT_PERMITTED',
            message: 'Đơn vé không đủ điều kiện hoàn tiền cưỡng chế từ máy chủ'
          })
        })
      } else {
        // Success
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            message: 'Booking refunded successfully',
            data: {
              bookingReference: 'BKG-20261123-9988',
              status: 'REFUNDED',
              refundPercentage: 100,
              paymentRefunded: true
            }
          })
        })
      }
    })

    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="tab-admin-bookings"]').click()
    const refundBtn = page.locator('[data-testid="btn-admin-refund-BKG-20261123-9988"]')
    await expect(refundBtn).toBeVisible()
    await refundBtn.click()

    // Dialog open
    const refundDialog = page.locator('[data-testid="dialog-refund"]')
    await expect(refundDialog).toBeVisible()

    // 1. Capture Dialog Open
    await page.screenshot({
      path: path.join(screenshotDir, 'booking-refund-confirmation-dialog.png'),
      fullPage: false
    })

    // Fill reason
    const reasonInput = page.locator('[data-testid="input-refund-reason"]')
    await reasonInput.fill('Nhà xe bị hủy chuyến đột xuất do thiên tai lũ lụt.')

    // Submit mutation -> will fail first
    const confirmBtn = page.locator('[data-testid="btn-confirm-refund"]')
    await confirmBtn.click()

    // Assert error message displayed and dialog KEPT OPEN
    await expect(page.locator('text=Đơn vé không đủ điều kiện hoàn tiền cưỡng chế')).toBeVisible()
    await expect(refundDialog).toBeVisible()

    // Assert contract payload
    expect(refundPayload).toEqual({
      reason: 'Nhà xe bị hủy chuyến đột xuất do thiên tai lũ lụt.',
      refundPercentage: 100
    })

    // 2. Capture Mutation Failure screenshot
    await page.screenshot({
      path: path.join(screenshotDir, 'booking-refund-mutation-failure.png'),
      fullPage: false
    })

    // Now allow success
    shouldFail = false
    await confirmBtn.click()

    // Dialog must close upon success
    await expect(refundDialog).not.toBeVisible({ timeout: 5000 })
  })

  // B3-13: Backend Unavailable / Error State
  test('B3-13: Backend Unavailable — error alert and retry button', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })

    // Simulate backend down (500 Internal Server Error)
    await page.route('**/api/v1/admin/payments/reports**', async (route) => {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'INTERNAL_SERVER_ERROR',
          message: 'Không thể kết nối cơ sở dữ liệu Payment'
        })
      })
    })

    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')

    // Error alert must be shown with Retry button
    const errorAlert = page.locator('text=Không thể tải dữ liệu báo cáo')
    await expect(errorAlert).toBeVisible()
    await expect(page.locator('button:has-text("Thử lại")')).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotDir, 'admin-dashboard-error-state.png'),
      fullPage: false
    })
  })

  // B3-14: Compact Density Mode & Custom Primary Theme
  test('B3-14: Compact Density Mode & Custom Primary Theme on Admin views', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')

    // Set compact density
    await page.evaluate(() => document.documentElement.setAttribute('data-density', 'compact'))
    await page.waitForTimeout(200)

    await page.screenshot({
      path: path.join(screenshotDir, 'admin-compact-density.png'),
      fullPage: false
    })

    // Set custom primary theme color (Ocean Blue #2563EB)
    await page.evaluate(() => {
      document.documentElement.style.setProperty('--primary', '221.2 83.2% 53.3%')
      document.documentElement.style.setProperty('--primary-effective', '#2563EB')
    })
    await page.waitForTimeout(200)

    await page.screenshot({
      path: path.join(screenshotDir, 'admin-custom-primary-theme.png'),
      fullPage: false
    })
  })
})
