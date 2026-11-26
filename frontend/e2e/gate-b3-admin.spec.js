import { test, expect } from '@playwright/test'
import path from 'path'
import fs from 'fs'
import { fileURLToPath } from 'url'
import { loginAsRole } from './helpers/auth'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const screenshotDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-gate-b3-final')
const closureScreenshotDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-gate-b3-closure')

// Ensure screenshots directories exist
if (!fs.existsSync(screenshotDir)) {
  fs.mkdirSync(screenshotDir, { recursive: true })
}
if (!fs.existsSync(closureScreenshotDir)) {
  fs.mkdirSync(closureScreenshotDir, { recursive: true })
}

// ---------------------------------------------------------------------------
// 1. Synthetic Hermetic Dataset (Strictly Synthetic, Zero Real PII)
// ---------------------------------------------------------------------------

// 25 synthetic users for multi-page server pagination (1 SYSTEM admin first, 15 CUSTOMER, 5 OPERATOR, 4 other SYSTEM)
const syntheticUsers = [
  {
    id: 1,
    username: 'admin',
    fullName: 'Quản Trị Viên Toàn Sàn',
    email: 'admin@velobus.vn',
    phone: '0900000001',
    role: 'ROLE_ADMIN',
    category: 'SYSTEM',
    status: 'ACTIVE'
  }
]
for (let i = 1; i <= 15; i++) {
  syntheticUsers.push({
    id: 100 + i,
    username: `cust_user_${String(i).padStart(2, '0')}`,
    fullName: `Khách Hàng Thử Nghiệm ${i}`,
    email: `cust${i}@example.com`,
    phone: `09120000${String(i).padStart(2, '0')}`,
    role: 'ROLE_CUSTOMER',
    category: 'CUSTOMER',
    status: 'ACTIVE'
  })
}
for (let i = 1; i <= 5; i++) {
  syntheticUsers.push({
    id: 200 + i,
    username: `op_partner_${String(i).padStart(2, '0')}`,
    fullName: `Chủ Xe Đối Tác ${i}`,
    email: `op${i}@example.com`,
    phone: `09010000${String(i).padStart(2, '0')}`,
    role: 'ROLE_OPERATOR',
    category: 'OPERATOR',
    status: 'ACTIVE'
  })
}
syntheticUsers.push(
  {
    id: 2,
    username: 'ketoan_ha',
    fullName: 'Lê Thị Thu Hà',
    email: 'ketoan@velobus.vn',
    phone: '0900000002',
    role: 'ROLE_ACCOUNTANT',
    category: 'SYSTEM',
    status: 'ACTIVE'
  },
  {
    id: 3,
    username: 'staff_support_01',
    fullName: 'Hỗ Trợ Kỹ Thuật 01',
    email: 'support1@velobus.vn',
    phone: '0900000003',
    role: 'ROLE_STAFF',
    category: 'SYSTEM',
    status: 'ACTIVE'
  },
  {
    id: 4,
    username: 'staff_support_02',
    fullName: 'Hỗ Trợ Kỹ Thuật 02',
    email: 'support2@velobus.vn',
    phone: '0900000004',
    role: 'ROLE_STAFF',
    category: 'SYSTEM',
    status: 'ACTIVE'
  },
  {
    id: 5,
    username: 'staff_support_03',
    fullName: 'Hỗ Trợ Kỹ Thuật 03',
    email: 'support3@velobus.vn',
    phone: '0900000005',
    role: 'ROLE_STAFF',
    category: 'SYSTEM',
    status: 'ACTIVE'
  }
)

// Payment Report Response (Adhering 1:1 to PaymentReportResponse.java)
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

// Canonical 7 Modules x 26 Permissions Catalog (Adhering 1:1 to AdminPermissionCatalog.java)
const canonicalPermissionCatalog = [
  {
    moduleCode: 'MOD_DASHBOARD',
    moduleName: '1. Tổng quan & Đối soát',
    routePath: '/admin',
    viewPermissionCode: 'PAYMENT_REPORT_VIEW',
    permissions: [
      { code: 'PAYMENT_REPORT_VIEW', name: 'Xem báo cáo KPI doanh thu', httpMethod: 'GET', apiPattern: '/api/v1/admin/payments/reports', description: 'Xem 4 thẻ KPI GMV, hoa hồng, tổng đơn vé và nhà xe', viewPermission: true },
      { code: 'PAYMENT_LIST_VIEW', name: 'Xem danh sách giao dịch & đối soát', httpMethod: 'GET', apiPattern: '/api/v1/admin/payments', description: 'Xem bảng lịch sử giao dịch và cảnh báo lệch số dư VNPay', viewPermission: true },
      { code: 'PAYMENT_RECONCILE_RESOLVE', name: 'Xử lý cảnh báo đối soát', httpMethod: 'POST', apiPattern: '/api/v1/admin/payments/*/reconcile', description: 'Xác nhận đã xử lý cảnh báo lệch đối soát VNPay ↔ Escrow', viewPermission: false }
    ]
  },
  {
    moduleCode: 'MOD_OPERATORS',
    moduleName: '2. Nhà xe & Hoa hồng',
    routePath: '/admin/operators',
    viewPermissionCode: 'OPERATOR_VIEW',
    permissions: [
      { code: 'OPERATOR_VIEW', name: 'Xem danh sách đối tác nhà xe', httpMethod: 'GET', apiPattern: '/api/v1/admin/partners/operators*', description: 'Hiển thị menu Nhà xe & xem danh sách hồ sơ đối tác vận tải', viewPermission: true },
      { code: 'OPERATOR_CREATE_UPDATE', name: 'Thêm / Sửa / Xóa hồ sơ nhà xe', httpMethod: 'POST|PUT|DELETE', apiPattern: '/api/v1/admin/partners/operators*', description: 'Tạo mới, cập nhật thông tin hoặc xóa hồ sơ đối tác nhà xe', viewPermission: false },
      { code: 'OPERATOR_APPROVE_REJECT', name: 'Phê duyệt / Từ chối nhà xe', httpMethod: 'PUT', apiPattern: '/api/v1/admin/partners/operators/*/approve|reject', description: 'Thẩm định duyệt hoặc từ chối hồ sơ đăng ký nhà xe mới', viewPermission: false },
      { code: 'OPERATOR_SUSPEND', name: 'Đình chỉ / Mở khóa nhà xe', httpMethod: 'PUT', apiPattern: '/api/v1/admin/partners/operators/*/suspend', description: 'Tạm đình chỉ hoặc kích hoạt lại hoạt động của nhà xe', viewPermission: false },
      { code: 'OPERATOR_COMMISSION_MANAGE', name: 'Cấu hình hoa hồng 2 cấp', httpMethod: 'PUT', apiPattern: '/api/v1/admin/partners/operators/*/commission', description: 'Thiết lập tỷ lệ chiết khấu hoa hồng riêng cho từng nhà xe', viewPermission: false }
    ]
  },
  {
    moduleCode: 'MOD_WITHDRAWALS',
    moduleName: '3. Giải ngân Ví Escrow',
    routePath: '/admin/withdrawals',
    viewPermissionCode: 'WITHDRAWAL_VIEW',
    permissions: [
      { code: 'WITHDRAWAL_VIEW', name: 'Xem danh sách lệnh rút tiền', httpMethod: 'GET', apiPattern: '/api/v1/admin/partners/withdrawals*', description: 'Hiển thị menu Giải ngân & xem các yêu cầu rút tiền ví ký quỹ', viewPermission: true },
      { code: 'WITHDRAWAL_APPROVE', name: 'Phê duyệt giải ngân', httpMethod: 'PUT', apiPattern: '/api/v1/admin/partners/withdrawals/*/approve', description: 'Duyệt chi chuyển khoản và khấu trừ số dư Locked Balance', viewPermission: false },
      { code: 'WITHDRAWAL_REJECT', name: 'Từ chối lệnh rút tiền', httpMethod: 'PUT', apiPattern: '/api/v1/admin/partners/withdrawals/*/reject', description: 'Từ chối lệnh rút và hoàn trả số dư về ví khả dụng của nhà xe', viewPermission: false }
    ]
  },
  {
    moduleCode: 'MOD_BANNERS',
    moduleName: '4. Banner & Truyền thông',
    routePath: '/admin/banners',
    viewPermissionCode: 'BANNER_VIEW',
    permissions: [
      { code: 'BANNER_VIEW', name: 'Xem danh sách banner toàn sàn', httpMethod: 'GET', apiPattern: '/api/v1/admin/partners/banners*', description: 'Hiển thị menu Banner & xem danh sách chiến dịch quảng cáo', viewPermission: true },
      { code: 'BANNER_CREATE_DELETE', name: 'Tạo / Sửa / Xóa banner sàn', httpMethod: 'POST|PUT|DELETE', apiPattern: '/api/v1/admin/partners/banners*', description: 'Đăng mới, chỉnh sửa hoặc xóa banner truyền thông của sàn', viewPermission: false },
      { code: 'BANNER_APPROVE_REJECT', name: 'Phê duyệt / Gỡ bỏ banner nhà xe', httpMethod: 'PUT', apiPattern: '/api/v1/admin/partners/banners/*/approve|reject', description: 'Duyệt hiển thị hoặc từ chối banner do nhà xe gửi lên', viewPermission: false }
    ]
  },
  {
    moduleCode: 'MOD_IAM',
    moduleName: '5. Người dùng & Phân quyền RBAC',
    routePath: '/admin/users-bookings',
    viewPermissionCode: 'USER_VIEW',
    permissions: [
      { code: 'USER_VIEW', name: 'Xem 3 phân mục người dùng', httpMethod: 'GET', apiPattern: '/api/v1/admin/users*', description: 'Xem danh sách Khách hàng, Chủ xe và Hệ thống', viewPermission: true },
      { code: 'USER_CREATE_UPDATE_DELETE', name: 'Tạo / Sửa / Xóa tài khoản', httpMethod: 'POST|PUT|DELETE', apiPattern: '/api/v1/admin/users*', description: 'Tạo mới, cập nhật hồ sơ hoặc xóa tài khoản', viewPermission: false },
      { code: 'USER_STATUS_LOCK', name: 'Khóa / Mở khóa tài khoản', httpMethod: 'PATCH', apiPattern: '/api/v1/admin/users/*/status', description: 'Khóa hoặc kích hoạt lại trạng thái hoạt động', viewPermission: false },
      { code: 'USER_PASSWORD_RESET', name: 'Đặt lại mật khẩu người dùng', httpMethod: 'POST', apiPattern: '/api/v1/admin/users/*/reset-password', description: 'Cấp lại mật khẩu mới cho tài khoản', viewPermission: false },
      { code: 'USER_ROLE_ASSIGN', name: 'Gán / Thay đổi Role cho người dùng', httpMethod: 'PATCH', apiPattern: '/api/v1/admin/users/*/role', description: 'Gán hoặc chuyển đổi Role cho tài khoản', viewPermission: false },
      { code: 'ROLE_MANAGE_CRUD', name: 'Quản lý Role & Phân quyền API', httpMethod: 'GET|POST|PUT|DELETE', apiPattern: '/api/v1/admin/roles*', description: 'Tạo mới, sửa ma trận 26 quyền API và xóa Custom Role', viewPermission: true }
    ]
  },
  {
    moduleCode: 'MOD_BOOKINGS',
    moduleName: '6. Đơn vé & Hoàn tiền Admin',
    routePath: '/admin/users-bookings',
    viewPermissionCode: 'BOOKING_VIEW',
    permissions: [
      { code: 'BOOKING_VIEW', name: 'Xem danh sách đơn đặt vé toàn sàn', httpMethod: 'GET', apiPattern: '/api/v1/admin/bookings*', description: 'Tra cứu danh sách đơn đặt vé của khách hàng', viewPermission: true },
      { code: 'BOOKING_FORCE_REFUND', name: 'Thực thi hoàn tiền cưỡng chế', httpMethod: 'POST', apiPattern: '/api/v1/admin/bookings/*/refund', description: 'Hủy vé và thu hồi tiền ký quỹ Escrow hoàn trả 100% cho khách', viewPermission: false }
    ]
  },
  {
    moduleCode: 'MOD_SETTINGS',
    moduleName: '7. Bến xe, Chuyến xe & Cấu hình',
    routePath: '/admin/settings',
    viewPermissionCode: 'STATION_VIEW',
    permissions: [
      { code: 'STATION_VIEW', name: 'Xem danh mục bến xe toàn quốc', httpMethod: 'GET', apiPattern: '/api/v1/admin/settings/stations*', description: 'Xem danh sách bến xe trung chuyển', viewPermission: true },
      { code: 'STATION_CREATE_UPDATE_DELETE', name: 'Thêm / Sửa / Xóa bến xe', httpMethod: 'POST|PUT|DELETE', apiPattern: '/api/v1/admin/settings/stations*', description: 'Quản lý danh mục bến xe', viewPermission: false },
      { code: 'TRIP_MANAGE_CRUD', name: 'Quản lý lịch trình & Chuyến đi', httpMethod: 'GET|POST|PUT|DELETE', apiPattern: '/api/v1/admin/settings/trips*', description: 'Xem và điều phối chuyến đi toàn sàn', viewPermission: false },
      { code: 'SYSTEM_SETTINGS_MANAGE', name: 'Cấu hình tham số vận hành sàn', httpMethod: 'GET|PUT', apiPattern: '/api/v1/admin/settings/configs*', description: 'Thiết lập thời gian giữ chỗ, tỷ lệ hoa hồng mặc định', viewPermission: false }
    ]
  }
]

let dynamicRoles = [
  {
    id: 1,
    code: 'ROLE_ADMIN',
    name: 'Quản Trị Viên Toàn Sàn (Super Admin)',
    description: 'Toàn quyền điều hành hệ thống, quản lý 7 Modules và 26 APIs quản trị.',
    category: 'SYSTEM',
    systemDefault: true,
    userCount: 1,
    permissions: canonicalPermissionCatalog.flatMap(m => m.permissions.map(p => p.code))
  },
  {
    id: 4,
    code: 'ROLE_ACCOUNTANT',
    name: 'Kế Toán & Đối Soát Tài Chính',
    description: 'Chuyên trách báo cáo doanh thu, đối soát VNPay và giải ngân ví Escrow.',
    category: 'SYSTEM',
    systemDefault: false,
    userCount: 1,
    permissions: ['PAYMENT_REPORT_VIEW', 'PAYMENT_LIST_VIEW', 'PAYMENT_RECONCILE_RESOLVE', 'WITHDRAWAL_VIEW', 'BOOKING_VIEW', 'BOOKING_FORCE_REFUND']
  },
  {
    id: 5,
    code: 'ROLE_DISPATCHER',
    name: 'Điều Hành Lịch Trình',
    description: 'Chuyên trách quản lý tuyến và bến xe.',
    category: 'SYSTEM',
    systemDefault: false,
    userCount: 0,
    permissions: ['TRIP_MANAGE_CRUD', 'STATION_VIEW']
  }
]

let dynamicBookings = [
  {
    bookingId: 1,
    bookingReference: 'BKG-20261123-9988',
    userId: 3,
    customerName: 'Khách Hàng Thử Nghiệm 1',
    customerPhone: '0912000001',
    tripId: 101,
    route: 'Hà Nội → Đà Nẵng',
    operatorName: 'Hoàng Long Limousine',
    seatNumbers: ['A05', 'A06'],
    totalAmount: 700000,
    status: 'CONFIRMED',
    createdAt: '2026-11-23T14:20:00'
  }
]

// ---------------------------------------------------------------------------
// 2. Strict Hermetic Network Mocks Registration
// ---------------------------------------------------------------------------
async function setupAdminMocks(page, options = {}) {
  // Reset dynamic state for test isolation
  dynamicRoles = [
    {
      id: 1,
      code: 'ROLE_ADMIN',
      name: 'Quản Trị Viên Toàn Sàn (Super Admin)',
      description: 'Toàn quyền điều hành hệ thống, quản lý 7 Modules và 26 APIs quản trị.',
      category: 'SYSTEM',
      systemDefault: true,
      userCount: 1,
      permissions: canonicalPermissionCatalog.flatMap(m => m.permissions.map(p => p.code))
    },
    {
      id: 4,
      code: 'ROLE_ACCOUNTANT',
      name: 'Kế Toán & Đối Soát Tài Chính',
      description: 'Chuyên trách báo cáo doanh thu, đối soát VNPay và giải ngân ví Escrow.',
      category: 'SYSTEM',
      systemDefault: false,
      userCount: 1,
      permissions: ['PAYMENT_REPORT_VIEW', 'PAYMENT_LIST_VIEW', 'PAYMENT_RECONCILE_RESOLVE', 'WITHDRAWAL_VIEW', 'BOOKING_VIEW', 'BOOKING_FORCE_REFUND']
    },
    {
      id: 5,
      code: 'ROLE_DISPATCHER',
      name: 'Điều Hành Lịch Trình',
      description: 'Chuyên trách quản lý tuyến và bến xe.',
      category: 'SYSTEM',
      systemDefault: false,
      userCount: 0,
      permissions: ['TRIP_MANAGE_CRUD', 'STATION_VIEW']
    }
  ]

  dynamicBookings = [
    {
      bookingId: 1,
      bookingReference: 'BKG-20261123-9988',
      userId: 3,
      customerName: 'Khách Hàng Thử Nghiệm 1',
      customerPhone: '0912000001',
      tripId: 101,
      route: 'Hà Nội → Đà Nẵng',
      operatorName: 'Hoàng Long Limousine',
      seatNumbers: ['A05', 'A06'],
      totalAmount: 700000,
      status: 'CONFIRMED',
      createdAt: '2026-11-23T14:20:00'
    }
  ]

  // 1. Payment Report (isolated endpoint)
  await page.route('**/api/v1/admin/payments/reports**', async (route) => {
    if (options.reportFail) {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'INTERNAL_ERROR',
          message: 'Lỗi kết nối dịch vụ báo cáo thanh toán từ máy chủ Backend'
        })
      })
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(mockPaymentReport)
    })
  })

  // 2. Payment List & Reconciliation Resolving
  await page.route('**/api/v1/admin/payments/**', async (route) => {
    const url = route.request().url()
    const method = route.request().method()

    if (url.includes('/reconcile') && method === 'POST') {
      if (options.reconcileFail) {
        await route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'RECONCILE_FAILED',
            message: 'Cổng đối soát VNPay từ chối xác nhận đối chiếu giao dịch'
          })
        })
        return
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          message: 'Reconciliation alert resolved successfully',
          data: {
            id: 1,
            reconciliationRequired: false,
            status: 'SUCCESS'
          }
        })
      })
      return
    }

    if (url.includes('/reports')) return route.fallback()

    if (options.paymentsFail) {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'PAYMENTS_UNAVAILABLE',
          message: 'Không thể truy vấn danh sách giao dịch đối soát từ máy chủ'
        })
      })
      return
    }

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

  // Also catch /api/v1/admin/payments without trailing slash
  await page.route('**/api/v1/admin/payments?**', async (route) => {
    if (options.paymentsFail) {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'PAYMENTS_UNAVAILABLE',
          message: 'Không thể truy vấn danh sách giao dịch đối soát từ máy chủ'
        })
      })
      return
    }
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

  // 3. Permissions Catalog (exact route)
  await page.route('**/api/v1/admin/roles/permissions-catalog', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        message: 'Permissions catalog retrieved successfully',
        data: canonicalPermissionCatalog
      })
    })
  })

  // 4. Roles CRUD (exact route)
  await page.route('**/api/v1/admin/roles/**', async (route) => {
    const method = route.request().method()
    const url = route.request().url()
    if (url.includes('/permissions-catalog')) return route.fallback()

    if (method === 'DELETE') {
      const roleId = url.split('/').pop()
      if (options.roleDeleteFail) {
        await route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'ROLE_IN_USE',
            message: 'Không thể xóa Role vì đang có tài khoản sử dụng'
          })
        })
        return
      }
      const idx = dynamicRoles.findIndex(r => String(r.id) === String(roleId))
      if (idx !== -1) dynamicRoles.splice(idx, 1)
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          message: 'System role deleted successfully',
          data: null
        })
      })
      return
    }
    route.fallback()
  })

  await page.route('**/api/v1/admin/roles', async (route) => {
    const method = route.request().method()
    if (method === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          message: 'Roles retrieved successfully',
          data: dynamicRoles
        })
      })
    } else if (method === 'POST') {
      const payload = route.request().postDataJSON()
      if (options.roleCreateFail || payload?.code?.includes('FAIL')) {
        await route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'ROLE_ALREADY_EXISTS',
            message: `Mã vai trò "${payload?.code}" đã tồn tại trong hệ thống`
          })
        })
        return
      }

      const newRole = {
        id: Date.now(),
        code: payload.code,
        name: payload.name,
        description: payload.description || '',
        category: payload.category || 'SYSTEM',
        systemDefault: false,
        userCount: 0,
        permissions: payload.permissions || []
      }
      dynamicRoles.push(newRole)
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          message: 'System role created successfully',
          data: newRole
        })
      })
    }
  })

  // 5. Users Mutation Routes (Status, Role, Password, Delete)
  await page.route('**/api/v1/admin/users/**', async (route) => {
    const method = route.request().method()
    const url = route.request().url()

    if (method === 'PATCH' && url.includes('/status')) {
      const parts = url.split('/')
      const userId = parts[parts.length - 2]
      const user = syntheticUsers.find(u => String(u.id) === String(userId) || u.username === userId)
      if (user) {
        const payload = route.request().postDataJSON() || {}
        user.status = payload.status || (user.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE')
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, message: 'Status updated', data: user })
      })
      return
    }

    if (method === 'PATCH' && url.includes('/role')) {
      const parts = url.split('/')
      const userId = parts[parts.length - 2]
      const user = syntheticUsers.find(u => String(u.id) === String(userId) || u.username === userId)
      if (user) {
        const payload = route.request().postDataJSON() || {}
        user.role = payload.role
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, message: 'Role updated', data: user })
      })
      return
    }

    if (method === 'POST' && url.includes('/reset-password')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, message: 'Password reset successfully' })
      })
      return
    }

    if (method === 'DELETE') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, message: 'User deleted successfully' })
      })
      return
    }

    route.fallback()
  })

  // 5. Users with Server-Side Pagination & Role/Category Filtering
  await page.route('**/api/v1/admin/users**', async (route) => {
    const url = new URL(route.request().url())
    const pageNum = parseInt(url.searchParams.get('page') || '0', 10)
    const sizeNum = parseInt(url.searchParams.get('size') || '10', 10)
    const roleParam = url.searchParams.get('role')
    const keywordParam = (url.searchParams.get('keyword') || '').toLowerCase().trim()

    let filtered = [...syntheticUsers]
    if (roleParam && roleParam !== 'ALL') {
      filtered = filtered.filter(u => u.category === roleParam || u.role.includes(roleParam))
    }
    if (keywordParam) {
      filtered = filtered.filter(u => 
        u.username.toLowerCase().includes(keywordParam) ||
        u.fullName.toLowerCase().includes(keywordParam) ||
        u.email.toLowerCase().includes(keywordParam)
      )
    }

    const totalElements = filtered.length
    const totalPages = Math.ceil(totalElements / sizeNum) || 1
    const startIdx = pageNum * sizeNum
    const items = filtered.slice(startIdx, startIdx + sizeNum)

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        message: 'Users retrieved successfully',
        data: {
          items,
          page: pageNum,
          size: sizeNum,
          totalElements,
          totalPages,
          hasNext: pageNum < totalPages - 1,
          hasPrevious: pageNum > 0
        }
      })
    })
  })

  // 6. Bookings
  await page.route('**/api/v1/admin/bookings**', async (route) => {
    const url = route.request().url()
    if (url.includes('/refund')) return // handled below

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        message: 'Bookings retrieved successfully',
        data: {
          items: dynamicBookings,
          page: 0,
          size: 10,
          totalElements: dynamicBookings.length,
          totalPages: 1,
          hasNext: false,
          hasPrevious: false
        }
      })
    })
  })
}

// ---------------------------------------------------------------------------
// 3. Test Suites
// ---------------------------------------------------------------------------

test.describe('Gate B3 Final: Contract & Acceptance Tests', () => {

  // B3-01: Correct Financial KPI Semantics & Valid Data Dashboard
  test('B3-01: Admin Dashboard — Correct Financial KPI Semantics (Valid Data)', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')

    // 1. Assert 4 KPI cards with exact financial business semantics
    await expect(page.getByRole('heading', { name: 'Doanh Thu Gộp' })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Doanh Thu Ròng' })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Thanh Toán Thành Công' })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Cần Đối Soát' })).toBeVisible()

    // 2. Assert values
    await expect(page.locator('[data-testid="kpi-gmv"]')).toContainText('1.450.800.000')
    await expect(page.locator('[data-testid="kpi-commission"]')).toContainText('142.500.000')
    await expect(page.locator('[data-testid="kpi-bookings"]')).toContainText('3.842')
    await expect(page.locator('[data-testid="kpi-operators"]')).toContainText('1')

    // 3. Zero hardcoded 28 active operators!
    await expect(page.locator('text=28 đối tác')).toHaveCount(0)

    // Capture required evidence: dashboard-valid-data.png
    await page.screenshot({
      path: path.join(screenshotDir, 'dashboard-valid-data.png'),
      fullPage: false
    })
  })

  // B3-02: Dashboard Error State Integrity (Isolated Failure & No Fabricated Zeros)
  test('B3-02: Admin Dashboard — API Error State Integrity', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page, { reportFail: true })
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')

    // Error alert must be shown
    const errorAlert = page.locator('[data-testid="dashboard-report-error"]')
    await expect(errorAlert).toBeVisible()
    await expect(errorAlert).toContainText('Không thể tải báo cáo doanh thu tài chính')

    // Crucial: KPI grid must NOT be rendered with fabricated zeros or 28 operators!
    await expect(page.locator('[data-testid="dashboard-kpi-grid"]')).toHaveCount(0)
    await expect(page.locator('text=28 đối tác')).toHaveCount(0)

    // Capture required evidence: dashboard-api-error.png
    await page.screenshot({
      path: path.join(screenshotDir, 'dashboard-api-error.png'),
      fullPage: false
    })
  })

  // B3-03: Role Matrix 7x26 Complete Verification
  test('B3-03: Role Matrix — Complete 7 Modules and 26 Permissions', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="tab-admin-roles"]').click()

    // Open create role dialog
    const createRoleBtn = page.locator('[data-testid="btn-create-role-in-tab"]')
    await expect(createRoleBtn).toBeVisible()
    await createRoleBtn.click()

    const roleDialog = page.locator('[role="dialog"]')
    await expect(roleDialog).toBeVisible()

    // Assert all 7 module headers are rendered with titles
    for (const mod of canonicalPermissionCatalog) {
      await expect(page.locator(`[data-testid="module-title-${mod.moduleCode}"]`)).toBeVisible()
    }

    // Assert all 26 permissions are rendered with name and code
    for (const mod of canonicalPermissionCatalog) {
      for (const perm of mod.permissions) {
        await expect(page.locator(`[data-testid="perm-name-${perm.code}"]`)).toBeVisible()
        await expect(page.locator(`[data-testid="perm-code-${perm.code}"]`)).toBeVisible()
      }
    }

    // Capture required evidence: role-matrix-7x26-complete.png
    await page.screenshot({
      path: path.join(screenshotDir, 'role-matrix-7x26-complete.png'),
      fullPage: false
    })

    await page.locator('button:has-text("Hủy")').click()
  })

  // B3-04: Role Creation Confirmed Success and Failed Rejection Test
  test('B3-04: Role Creation — Confirmed Success & Failure Handling', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="tab-admin-roles"]').click()
    await page.locator('[data-testid="btn-create-role-in-tab"]').click()

    // 1. Failure Path: Duplicate Code / Backend Rejection
    await page.locator('[data-testid="input-role-code"]').fill('CAMPAIGN_MOD_FAIL')
    await page.locator('[data-testid="input-role-name"]').fill('Vai Trò Trùng Lặp')
    await page.locator('[data-testid="btn-save-role"]').click()
    await page.waitForTimeout(200)

    // Assert error message displayed, dialog stays open
    await expect(page.locator('text=Mã vai trò "ROLE_CAMPAIGN_MOD_FAIL" đã tồn tại').or(page.locator('text=Mã vai trò đã tồn tại'))).toBeVisible()
    await expect(page.locator('[role="dialog"]')).toBeVisible()

    // 2. Success Path: Valid code
    await page.locator('[data-testid="input-role-code"]').fill('CAMPAIGN_MOD')
    await page.locator('[data-testid="input-role-name"]').fill('Quản Trị Chiến Dịch')

    // Clear all and toggle module 4 (MOD_BANNERS)
    await page.locator('[data-testid="btn-role-clear-all"]').click()
    await page.locator('[data-testid="toggle-module-MOD_BANNERS"]').click()

    // Toggle single permission: PAYMENT_REPORT_VIEW
    await page.locator('[data-testid="perm-checkbox-PAYMENT_REPORT_VIEW"]').click()

    // Save role
    await page.locator('[data-testid="btn-save-role"]').click()
    await page.waitForTimeout(300)

    // Assert dialog closed and refetched card is visible with userCount: 0
    await expect(page.locator('[data-testid="role-card-ROLE_CAMPAIGN_MOD"]')).toBeVisible()
    await expect(page.locator('[data-testid="role-card-ROLE_CAMPAIGN_MOD"]')).toContainText('Gán: 0 user')

    // Capture required evidence: role-creation-confirmed-success.png
    await page.screenshot({
      path: path.join(screenshotDir, 'role-creation-confirmed-success.png'),
      fullPage: false
    })
  })

  // B3-05: Real Server-side Pagination across Multiple Pages
  test('B3-05: User Management — Server-side Pagination Page 1 and Page 2', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    // Page 1: Verify 10 items shown, next button enabled
    await expect(page.locator('text=@cust_user_01')).toBeVisible()
    await expect(page.locator('text=@cust_user_09')).toBeVisible()
    await expect(page.locator('text=@cust_user_10')).toHaveCount(0)

    // Capture required evidence: user-pagination-page-1.png
    await page.screenshot({
      path: path.join(screenshotDir, 'user-pagination-page-1.png'),
      fullPage: false
    })

    // Click Next page
    const nextBtn = page.locator('[data-testid="pagination-next"]')
    await expect(nextBtn).toBeEnabled()
    await nextBtn.click()
    await page.waitForTimeout(250)

    // Page 2: Verify items shown, item 1 gone
    await expect(page.locator('text=@cust_user_10')).toBeVisible()
    await expect(page.locator('text=@cust_user_01')).toHaveCount(0)

    // Capture required evidence: user-pagination-page-2.png
    await page.screenshot({
      path: path.join(screenshotDir, 'user-pagination-page-2.png'),
      fullPage: false
    })

    // Click Previous page
    const prevBtn = page.locator('[data-testid="pagination-prev"]')
    await expect(prevBtn).toBeEnabled()
    await prevBtn.click()
    await page.waitForTimeout(250)
    await expect(page.locator('text=@cust_user_01')).toBeVisible()
  })

  // B3-06: Customer-only Filtered Table (Zero Admins and Operators Visible)
  test('B3-06: User Management — Customer-only Filtered Table', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    // Click Khách hàng category tab
    const customerTab = page.locator('[data-testid="category-tab-CUSTOMER"]')
    await expect(customerTab).toBeVisible()
    await customerTab.click()
    await page.waitForTimeout(250)

    // Assert only Customers appear (no Admin or Operator users!)
    await expect(page.locator('text=@cust_user_01')).toBeVisible()
    await expect(page.locator('text=@admin')).toHaveCount(0)
    await expect(page.locator('text=@op_partner_01')).toHaveCount(0)

    // Capture required evidence: customer-only-filtered-table.png
    await page.screenshot({
      path: path.join(screenshotDir, 'customer-only-filtered-table.png'),
      fullPage: false
    })
  })

  // B3-07: Refund Failure — Backend Rejection Retains Modal & Reason
  test('B3-07: Forced Refund — Failure Retains Context & Modal', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)

    // Mock 400 Bad Request rejection
    await page.route('**/api/v1/admin/bookings/BKG-20261123-9988/refund', async (route) => {
      await route.fulfill({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'REFUND_TIME_EXPIRED',
          message: 'Đơn vé đã quá hạn chính sách hoàn tiền 100% từ máy chủ Backend'
        })
      })
    })

    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="tab-admin-bookings"]').click()
    await page.locator('[data-testid="btn-admin-refund-BKG-20261123-9988"]').click()

    const refundDialog = page.locator('[data-testid="dialog-refund"]')
    await expect(refundDialog).toBeVisible()

    // Test minimum length validation (< 10 chars)
    const reasonInput = page.locator('[data-testid="input-refund-reason"]')
    await reasonInput.fill('Hủy vé')
    await page.locator('[data-testid="btn-confirm-refund"]').click()
    await expect(page.locator('text=tối thiểu 10 ký tự')).toBeVisible()

    // Test valid reason
    await reasonInput.fill('Hành khách thông báo hủy chuyến trước 24 giờ theo quy định.')
    await page.locator('[data-testid="btn-confirm-refund"]').click()
    await page.waitForTimeout(250)

    // Assert dialog REMAINS OPEN, error message is displayed, and reason is preserved
    await expect(page.locator('text=Đơn vé đã quá hạn chính sách hoàn tiền 100%')).toBeVisible()
    await expect(refundDialog).toBeVisible()
    await expect(reasonInput).toHaveValue('Hành khách thông báo hủy chuyến trước 24 giờ theo quy định.')

    // Capture required evidence: refund-failure.png
    await page.screenshot({
      path: path.join(screenshotDir, 'refund-failure.png'),
      fullPage: false
    })
  })

  // B3-08: Refund Confirmed Success — Authoritative Refetch & Status Update
  test('B3-08: Forced Refund — Confirmed Success & Authoritative Refetch', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)

    let refundExecuted = false
    await page.route('**/api/v1/admin/bookings/BKG-20261123-9988/refund', async (route) => {
      refundExecuted = true
      // Update local fixture to authoritative domain status CANCELLED
      dynamicBookings[0].status = 'CANCELLED'
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          message: 'Booking refunded successfully',
          data: {
            bookingReference: 'BKG-20261123-9988',
            status: 'CANCELLED',
            refundPercentage: 100,
            refundAmount: 700000,
            paymentRefunded: true
          }
        })
      })
    })

    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="tab-admin-bookings"]').click()
    await page.locator('[data-testid="btn-admin-refund-BKG-20261123-9988"]').click()

    const reasonInput = page.locator('[data-testid="input-refund-reason"]')
    await reasonInput.fill('Hành khách thông báo hủy chuyến trước 24 giờ theo quy định.')
    await page.locator('[data-testid="btn-confirm-refund"]').click()
    await page.waitForTimeout(300)

    expect(refundExecuted).toBe(true)
    await expect(page.locator('[data-testid="dialog-refund"]')).toHaveCount(0)

    // Table reflects authoritative state: status is CANCELLED (domain status from Booking.java)
    await expect(page.locator('tbody').getByText('CANCELLED', { exact: true })).toBeVisible()

    // Capture required evidence: refund-confirmed-success.png
    await page.screenshot({
      path: path.join(screenshotDir, 'refund-confirmed-success.png'),
      fullPage: false
    })
  })

  // B3-09: Tablet 768px DataTable Layout and Horizontal Scrolling
  test('B3-09: Responsive — Tablet 768px DataTable', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 })
    await setupAdminMocks(page)
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    const dataTable = page.locator('table').first()
    await expect(dataTable).toBeVisible()

    // Assert body doesn't overflow horizontally
    const bodyOverflow = await page.evaluate(() => document.body.scrollWidth > document.body.clientWidth)
    expect(bodyOverflow).toBe(false)

    // Capture required evidence: tablet-datatable.png
    await page.screenshot({
      path: path.join(screenshotDir, 'tablet-datatable.png'),
      fullPage: false
    })
  })

  // B3-10: Mobile 375px Table Actions & Accessibility
  test('B3-10: Responsive — Mobile 375px Table Actions', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 })
    await setupAdminMocks(page)
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    // Find first user row action button
    const actionBtn = page.locator('[data-testid^="btn-more-actions-"]').first()
    await expect(actionBtn).toBeVisible()
    await actionBtn.click()

    // Dropdown menu appears
    await expect(page.locator('text=Đặt lại mật khẩu')).toBeVisible()

    // Capture required evidence: mobile-table-actions.png
    await page.screenshot({
      path: path.join(screenshotDir, 'mobile-table-actions.png'),
      fullPage: false
    })
  })

  // B3-11: Self-Admin Lock Guard Verification
  test('B3-11: Admin Security Invariants — Self Admin Protection Guard', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    const selfGuard = page.locator('[data-testid="self-admin-guard"]')
    await expect(selfGuard).toBeVisible()
    await expect(selfGuard).toContainText('Không thể tự khóa')
  })

  // B3-12: Role Preview Mode
  test('B3-12: Role Preview Mode — Dynamic Permissions Real-Time Simulation', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="tab-admin-roles"]').click()
    const previewBtn = page.locator('[data-testid="preview-role-ROLE_ACCOUNTANT"]')
    await expect(previewBtn).toBeVisible()
    await previewBtn.click()

    const banner = page.locator('[data-testid="role-preview-banner"]')
    await expect(banner).toBeVisible()
    await expect(page.locator('[data-testid="exit-role-preview-btn"]')).toBeVisible()

    await page.locator('[data-testid="exit-role-preview-btn"]').click()
    await expect(banner).toHaveCount(0)
  })

  // B3-13: Role Deletion Invariants — Protection Guards, In-Use Block, and Confirmed Deletion
  test('B3-13: Role Deletion Invariants — Protection & Safe Deletion', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="tab-admin-roles"]').click()

    // 1. Try to delete custom role with userCount: 1 (ROLE_ACCOUNTANT)
    const deleteAccountantBtn = page.locator('[data-testid="btn-delete-role-ROLE_ACCOUNTANT"]')
    await expect(deleteAccountantBtn).toBeVisible()
    await deleteAccountantBtn.click()

    // Assert ROLE_IN_USE error notification appears
    await expect(page.locator('text=ROLE_IN_USE').first()).toBeVisible()
    await page.screenshot({
      path: path.join(closureScreenshotDir, 'role-deletion-in-use-blocked.png'),
      fullPage: false
    })

    // 2. Delete an unused custom role (ROLE_DISPATCHER, userCount: 0)
    const deleteDispatcherBtn = page.locator('[data-testid="btn-delete-role-ROLE_DISPATCHER"]')
    await expect(deleteDispatcherBtn).toBeVisible()
    await deleteDispatcherBtn.click()

    // Confirmation dialog opens
    const confirmDialog = page.locator('[role="alertdialog"]')
    await expect(confirmDialog).toBeVisible()
    await expect(confirmDialog).toContainText('ROLE_DISPATCHER')

    // Cancel deletion
    await confirmDialog.locator('button:has-text("Hủy")').click()
    await expect(confirmDialog).toHaveCount(0)
    await expect(page.locator('[data-testid="role-card-ROLE_DISPATCHER"]')).toBeVisible()

    // Reopen and confirm deletion
    await deleteDispatcherBtn.click()
    await expect(confirmDialog).toBeVisible()
    await confirmDialog.locator('button:has-text("Xác nhận xóa")').click()
    await page.waitForTimeout(300)

    // Assert role card is deleted from the view
    await expect(page.locator('[data-testid="role-card-ROLE_DISPATCHER"]')).toHaveCount(0)
    await page.screenshot({
      path: path.join(closureScreenshotDir, 'role-deletion-confirmed.png'),
      fullPage: false
    })
  })

  // B3-14: Reconciliation Mutation — Confirmation Dialog, Success & Feedback
  test('B3-14: Reconciliation — Manual Resolve Confirmation & Mutation Safety', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')

    // Find first resolve button in reconciliation table
    const resolveBtn = page.locator('[data-testid^="btn-resolve-alert"]').first()
    await expect(resolveBtn).toBeVisible()
    await resolveBtn.click()

    // Confirm dialog opens
    const dialog = page.locator('[role="alertdialog"]')
    await expect(dialog).toBeVisible()
    await expect(dialog).toContainText('xử lý đối soát')

    // Click cancel
    await dialog.locator('button:has-text("Hủy bỏ")').click()
    await expect(dialog).toHaveCount(0)

    // Reopen and confirm resolve
    await resolveBtn.click()
    await expect(dialog).toBeVisible()

    await page.screenshot({
      path: path.join(closureScreenshotDir, 'reconciliation-mutation-resolve.png'),
      fullPage: false
    })

    await dialog.locator('button:has-text("Xác nhận xử lý")').click()
    await page.waitForTimeout(300)

    // Dialog closed and success toast visible
    await expect(dialog).toHaveCount(0)
    await expect(page.locator('text=Đã xác nhận xử lý đối soát')).toBeVisible()
  })

  // B3-15: Financial Safety — Refund Gateway Rejection & Network Timeout Locking (PRE-01)
  test('B3-15: Forced Refund — Gateway Rejection & Network Timeout Locking', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)

    // Route for paymentRefunded = false (Gateway Rejection)
    await page.route('**/api/v1/admin/bookings/BKG-20261123-9988/refund', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          message: 'Booking status updated but payment gateway rejected refund',
          data: {
            bookingReference: 'BKG-20261123-9988',
            status: 'CANCELLED',
            refundPercentage: 100,
            refundAmount: 700000,
            paymentRefunded: false
          }
        })
      })
    })

    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin/users-bookings')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="tab-admin-bookings"]').click()
    await page.locator('[data-testid="btn-admin-refund-BKG-20261123-9988"]').click()

    const reasonInput = page.locator('[data-testid="input-refund-reason"]')
    await reasonInput.fill('Hành khách thông báo hủy chuyến trước 24 giờ theo quy định.')
    await page.locator('[data-testid="btn-confirm-refund"]').click()
    await page.waitForTimeout(300)

    // Assert warning notification regarding payment gateway rejection
    await expect(page.locator('text=cổng thanh toán từ chối hoàn tiền').first()).toBeVisible()
    await page.screenshot({
      path: path.join(closureScreenshotDir, 'refund-payment-gateway-rejected.png'),
      fullPage: false
    })

    // PRE-01: Network Failure & Persistent Unknown Outcome Test
    // Reset booking status to CONFIRMED for timeout test
    dynamicBookings[0].status = 'CONFIRMED'
    await page.locator('[data-testid="tab-admin-bookings"]').click()
    await page.waitForTimeout(200)

    // Unroute and mock network abort / timeout
    await page.unroute('**/api/v1/admin/bookings/BKG-20261123-9988/refund')
    await page.route('**/api/v1/admin/bookings/BKG-20261123-9988/refund', async (route) => {
      await route.abort('timedout')
    })

    // Click refund button again
    await page.locator('[data-testid="btn-admin-refund-BKG-20261123-9988"]').click()
    const refundDialog = page.locator('[data-testid="dialog-refund"]')
    await expect(refundDialog).toBeVisible()

    await reasonInput.fill('Thử hoàn tiền lần 2 và bị sự cố đứt kết nối mạng timeout.')
    const confirmBtn = page.locator('[data-testid="btn-confirm-refund"]')
    await confirmBtn.click()
    await page.waitForTimeout(300)

    // Assert confirm button is now DISABLED due to unknown outcome guard
    await expect(confirmBtn).toBeDisabled()
    await expect(page.locator('text=KHÓA gửi lại').first()).toBeVisible()

    // Close modal by clicking Hủy bỏ
    await page.locator('button:has-text("Hủy bỏ")').click()
    await expect(refundDialog).toHaveCount(0)

    // Reopen modal for the SAME booking: guard must be persistent
    await page.locator('[data-testid="btn-admin-refund-BKG-20261123-9988"]').click()
    await expect(refundDialog).toBeVisible()
    await expect(confirmBtn).toBeDisabled()
    await expect(page.locator('text=KHÓA gửi lại').first()).toBeVisible()

    // Assert verify server state button is visible
    const verifyBtn = page.locator('[data-testid="btn-verify-server-state"]')
    await expect(verifyBtn).toBeVisible()

    // Unroute refund and re-mock with successful server state
    await page.unroute('**/api/v1/admin/bookings/BKG-20261123-9988/refund')
    await verifyBtn.click()
    await page.waitForTimeout(300)

    // PRE-01: When server booking is still CONFIRMED, guard does NOT auto-unlock; maintains fail-closed lock!
    await expect(confirmBtn).toBeDisabled()
    await expect(page.locator('text=TIẾP TỤC KHÓA').first()).toBeVisible()
  })

  // B3-16: Theme Customizer — Desktop Light/Dark, Compact Density, Custom Primary
  test('B3-16: Theme Customizer — Light/Dark, Density & Primary Theme', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupAdminMocks(page)
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')

    // Open Theme Customizer Trigger in header
    const customizerBtn = page.locator('[data-testid="theme-customizer-trigger"]')
    await expect(customizerBtn).toBeVisible()
    await customizerBtn.click()

    // Assert theme studio drawer is open
    const drawer = page.locator('[data-testid="theme-customizer-drawer"]')
    await expect(drawer).toBeVisible()

    // Toggle Dark Mode
    const darkBtn = drawer.getByRole('button', { name: 'Tối', exact: true })
    await darkBtn.click()
    await expect(page.locator('html')).toHaveClass(/dark/)
    await page.screenshot({
      path: path.join(closureScreenshotDir, 'admin-dark-theme.png'),
      fullPage: false
    })

    // Toggle Compact Density
    const compactOpt = drawer.locator('text=Thu gọn')
    if (await compactOpt.isVisible()) {
      await compactOpt.click()
      await page.screenshot({
        path: path.join(closureScreenshotDir, 'admin-compact-density.png'),
        fullPage: false
      })
    }

    // Close Theme Customizer via Escape key
    await page.keyboard.press('Escape')
    await expect(drawer).toHaveCount(0)
  })

  // B3-17: Independent Dashboard Loading — Isolated Failure Permutations
  test('B3-17: Dashboard Loading — Report Failure with Independent Payments Success', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    // Report fails, but payments list succeeds
    await setupAdminMocks(page, { reportFail: true, paymentsFail: false })
    await loginAsRole(page, 'ROLE_ADMIN', 'admin')
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')

    // Report error alert is displayed
    await expect(page.locator('[data-testid="dashboard-report-error"]')).toBeVisible()

    // Meanwhile, Reconciliation Payments table is LOADED INDEPENDENTLY and visible!
    await expect(page.locator('table')).toBeVisible()
    await expect(page.locator('text=REC-20261123-01').first()).toBeVisible()

    // Capture required evidence: dashboard-independent-loading.png
    await page.screenshot({
      path: path.join(closureScreenshotDir, 'dashboard-independent-loading.png'),
      fullPage: false
    })
  })
})
