import { test, expect } from '@playwright/test'
import { loginAsRole } from './helpers/auth'

const defaultUsers = [
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
  },
  {
    id: 4,
    username: 'ketoan_ha',
    fullName: 'Lê Thị Thu Hà (Kế Toán)',
    email: 'ketoan.ha@velobus.vn',
    phone: '0903334444',
    role: 'ROLE_ACCOUNTANT',
    category: 'SYSTEM',
    status: 'ACTIVE'
  }
]

const defaultPaymentReport = {
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

const defaultBookings = [
  {
    id: 1,
    bookingReference: 'VB-20261123-001',
    customerName: 'Nguyễn Văn A',
    customerPhone: '0912345678',
    tripId: 101,
    route: 'Hà Nội → Đà Nẵng',
    operatorName: 'Hoàng Long Limousine',
    seatNumbers: ['A05', 'A06'],
    totalAmount: 700000,
    status: 'CONFIRMED',
    createdAt: '2026-11-23T14:20:00'
  }
]

test.describe('E2E: Phân Hệ Ban Quản Trị (Admin Backoffice)', () => {
  let rolesList = []

  test.beforeEach(async ({ page }) => {
    rolesList = [
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

    await page.route('**/api/v1/admin/payments/reports**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(defaultPaymentReport)
      })
    })

    await page.route('**/api/v1/admin/payments?**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            items: defaultPaymentReport.data.reconciliationTransactions,
            page: 0,
            size: 10,
            totalElements: 2,
            totalPages: 1
          }
        })
      })
    })

    await page.route('**/api/v1/admin/users**', async (route) => {
      const url = new URL(route.request().url())
      const roleParam = url.searchParams.get('role')
      let items = defaultUsers
      if (roleParam && roleParam !== 'ALL') {
        items = defaultUsers.filter(u => u.category === roleParam || u.role.includes(roleParam))
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          message: 'Users retrieved successfully',
          data: {
            items,
            page: 0,
            size: 10,
            totalElements: items.length,
            totalPages: 1,
            hasNext: false,
            hasPrevious: false
          }
        })
      })
    })

    await page.route('**/api/v1/admin/roles**', async (route) => {
      const url = route.request().url()
      if (url.includes('/permissions-catalog')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: [
              {
                moduleCode: 'MOD_DASHBOARD',
                moduleName: '1. Tổng quan & Đối soát',
                permissions: [
                  { code: 'PAYMENT_REPORT_VIEW', name: 'Xem báo cáo KPI doanh thu' },
                  { code: 'PAYMENT_LIST_VIEW', name: 'Xem danh sách giao dịch & đối soát' },
                  { code: 'PAYMENT_RECONCILE_RESOLVE', name: 'Xử lý cảnh báo đối soát' }
                ]
              },
              {
                moduleCode: 'MOD_OPERATORS',
                moduleName: '2. Nhà xe & Hoa hồng',
                permissions: [
                  { code: 'OPERATOR_VIEW', name: 'Xem danh sách đối tác nhà xe' },
                  { code: 'OPERATOR_CREATE_UPDATE', name: 'Thêm / Sửa / Xóa hồ sơ nhà xe' },
                  { code: 'OPERATOR_APPROVE_REJECT', name: 'Phê duyệt / Từ chối nhà xe' },
                  { code: 'OPERATOR_SUSPEND', name: 'Đình chỉ / Mở khóa nhà xe' },
                  { code: 'OPERATOR_COMMISSION_MANAGE', name: 'Cấu hình hoa hồng 2 cấp' }
                ]
              },
              {
                moduleCode: 'MOD_WITHDRAWALS',
                moduleName: '3. Giải ngân Ví Escrow',
                permissions: [
                  { code: 'WITHDRAWAL_VIEW', name: 'Xem danh sách lệnh rút tiền' },
                  { code: 'WITHDRAWAL_APPROVE', name: 'Phê duyệt giải ngân' },
                  { code: 'WITHDRAWAL_REJECT', name: 'Từ chối lệnh rút tiền' }
                ]
              },
              {
                moduleCode: 'MOD_BANNERS',
                moduleName: '4. Banner & Truyền thông',
                permissions: [
                  { code: 'BANNER_VIEW', name: 'Xem danh sách banner toàn sàn' },
                  { code: 'BANNER_CREATE_DELETE', name: 'Tạo / Sửa / Xóa banner sàn' },
                  { code: 'BANNER_APPROVE_REJECT', name: 'Phê duyệt / Gỡ bỏ banner nhà xe' }
                ]
              },
              {
                moduleCode: 'MOD_IAM',
                moduleName: '5. Người dùng & Phân quyền RBAC',
                permissions: [
                  { code: 'USER_VIEW', name: 'Xem danh sách người dùng' },
                  { code: 'USER_CREATE_UPDATE_DELETE', name: 'Thêm / Sửa / Xóa người dùng' },
                  { code: 'USER_STATUS_LOCK', name: 'Khóa / Mở khóa tài khoản' },
                  { code: 'USER_PASSWORD_RESET', name: 'Đặt lại mật khẩu' },
                  { code: 'USER_ROLE_ASSIGN', name: 'Gán vai trò cho người dùng' },
                  { code: 'ROLE_MANAGE_CRUD', name: 'Quản lý Role & Ma trận quyền' }
                ]
              },
              {
                moduleCode: 'MOD_BOOKINGS',
                moduleName: '6. Đơn vé & Hoàn tiền',
                permissions: [
                  { code: 'BOOKING_VIEW', name: 'Xem danh sách đơn đặt vé toàn sàn' },
                  { code: 'BOOKING_FORCE_REFUND', name: 'Hoàn tiền cưỡng chế có kiểm toán' }
                ]
              },
              {
                moduleCode: 'MOD_TRIPS_SETTINGS',
                moduleName: '7. Tuyến bến & Cấu hình sàn',
                permissions: [
                  { code: 'STATION_VIEW', name: 'Xem danh mục bến xe' },
                  { code: 'STATION_CREATE_UPDATE_DELETE', name: 'Thêm / Sửa / Xóa bến xe' },
                  { code: 'TRIP_MANAGE_CRUD', name: 'Quản lý lịch trình & Chuyến đi' },
                  { code: 'SYSTEM_SETTINGS_MANAGE', name: 'Cấu hình tham số vận hành sàn' }
                ]
              }
            ]
          })
        })
        return
      }

      if (route.request().method() === 'POST') {
        const body = JSON.parse(route.request().postData() || '{}')
        const newRole = {
          id: Date.now(),
          code: body.code,
          name: body.name,
          description: body.description || '',
          category: body.category || 'SYSTEM',
          systemDefault: false,
          permissions: body.permissions || []
        }
        rolesList.push(newRole)
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: newRole })
        })
        return
      }

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: rolesList })
      })
    })

    await page.route('**/api/v1/admin/bookings**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            items: defaultBookings,
            page: 0,
            size: 10,
            totalElements: defaultBookings.length,
            totalPages: 1,
            hasNext: false,
            hasPrevious: false
          }
        })
      })
    })

    await page.route('**/api/v1/admin/bookings/**/refund', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          message: 'Hoàn tiền cưỡng chế thành công',
          data: { status: 'REFUNDED' }
        })
      })
    })

    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
  })
  test('FE-ADM-E2E-01: Dashboard quản trị hiển thị KPI toàn sàn và cảnh báo đối soát VNPay', async ({ page }) => {
    await page.goto('/admin')
    
    // Tiêu đề trang
    await expect(page.locator('text=Tổng Quan Toàn Sàn & Báo Cáo Đối Soát')).toBeVisible()

    // 4 thẻ KPI toàn sàn
    await expect(page.getByRole('heading', { name: 'Doanh Thu Gộp' })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Doanh Thu Ròng' })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Thanh Toán Thành Công' })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Cần Đối Soát' })).toBeVisible()

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
    await page.route('**/admin/bookings/*/refund*', async (route) => {
      const url = route.request().url()
      const match = url.match(/\/admin\/bookings\/([^/]+)\/refund/)
      const ref = match ? match[1] : 'BK-12345'
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          refundId: 'RF-999',
          bookingReference: ref,
          refundAmount: 500000,
          refundPercentage: 100,
          status: 'CANCELLED',
          paymentRefunded: true
        })
      })
    })
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
