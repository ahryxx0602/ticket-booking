/**
 * VeloBus RBAC Contract & Permission Catalog
 * Strictly mirrors com.ticket.identity.domain.model.AdminPermissionCatalog (7 Modules x 26 API Permissions)
 */

export type UserBaseRole = 'ROLE_CUSTOMER' | 'ROLE_OPERATOR' | 'ROLE_ADMIN'

export type AdminPermissionCode =
  // Module 1: MOD_DASHBOARD (1. Tổng quan & Đối soát)
  | 'PAYMENT_REPORT_VIEW'
  | 'PAYMENT_LIST_VIEW'
  | 'PAYMENT_RECONCILE_RESOLVE'

  // Module 2: MOD_OPERATORS (2. Nhà xe & Hoa hồng)
  | 'OPERATOR_VIEW'
  | 'OPERATOR_CREATE_UPDATE'
  | 'OPERATOR_APPROVE_REJECT'
  | 'OPERATOR_SUSPEND'
  | 'OPERATOR_COMMISSION_MANAGE'

  // Module 3: MOD_WITHDRAWALS (3. Giải ngân Ví Escrow)
  | 'WITHDRAWAL_VIEW'
  | 'WITHDRAWAL_APPROVE'
  | 'WITHDRAWAL_REJECT'

  // Module 4: MOD_BANNERS (4. Banner & Truyền thông)
  | 'BANNER_VIEW'
  | 'BANNER_CREATE_DELETE'
  | 'BANNER_APPROVE_REJECT'

  // Module 5: MOD_IAM (5. Người dùng & Phân quyền RBAC)
  | 'USER_VIEW'
  | 'USER_CREATE_UPDATE_DELETE'
  | 'USER_STATUS_LOCK'
  | 'USER_PASSWORD_RESET'
  | 'USER_ROLE_ASSIGN'
  | 'ROLE_MANAGE_CRUD'

  // Module 6: MOD_BOOKINGS (6. Đơn vé & Hoàn tiền Admin)
  | 'BOOKING_VIEW'
  | 'BOOKING_FORCE_REFUND'

  // Module 7: MOD_SETTINGS (7. Bến xe, Chuyến xe & Cấu hình)
  | 'STATION_VIEW'
  | 'STATION_CREATE_UPDATE_DELETE'
  | 'TRIP_MANAGE_CRUD'
  | 'SYSTEM_SETTINGS_MANAGE'

export interface PermissionItem {
  code: AdminPermissionCode
  name: string
  httpMethod: string
  apiPattern: string
  description: string
  viewPermission: boolean
}

export interface ModuleCatalog {
  moduleCode: string
  moduleName: string
  routePath: string
  viewPermissionCode: AdminPermissionCode
  permissions: PermissionItem[]
}

export const ADMIN_PERMISSION_MODULES: readonly ModuleCatalog[] = [
  {
    moduleCode: 'MOD_DASHBOARD',
    moduleName: '1. Tổng quan & Đối soát',
    routePath: '/admin',
    viewPermissionCode: 'PAYMENT_REPORT_VIEW',
    permissions: [
      { code: 'PAYMENT_REPORT_VIEW', name: 'Xem báo cáo KPI doanh thu', httpMethod: 'GET', apiPattern: '/api/v1/admin/payments/reports', description: 'Xem 4 thẻ KPI GMV, hoa hồng, tổng đơn vé và nhà xe', viewPermission: true },
      { code: 'PAYMENT_LIST_VIEW', name: 'Xem danh sách giao dịch & đối soát', httpMethod: 'GET', apiPattern: '/api/v1/admin/payments', description: 'Xem bảng lịch sử giao dịch và cảnh báo lệch số dư VNPay', viewPermission: true },
      { code: 'PAYMENT_RECONCILE_RESOLVE', name: 'Xử lý cảnh báo đối soát', httpMethod: 'POST', apiPattern: '/api/v1/admin/payments/*/reconcile', description: 'Xác nhận đã xử lý cảnh báo lệch đối soát VNPay ↔ Escrow', viewPermission: false },
    ],
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
      { code: 'OPERATOR_COMMISSION_MANAGE', name: 'Cấu hình hoa hồng 2 cấp', httpMethod: 'PUT', apiPattern: '/api/v1/admin/partners/operators/*/commission', description: 'Thiết lập tỷ lệ chiết khấu hoa hồng riêng cho từng nhà xe', viewPermission: false },
    ],
  },
  {
    moduleCode: 'MOD_WITHDRAWALS',
    moduleName: '3. Giải ngân Ví Escrow',
    routePath: '/admin/withdrawals',
    viewPermissionCode: 'WITHDRAWAL_VIEW',
    permissions: [
      { code: 'WITHDRAWAL_VIEW', name: 'Xem danh sách lệnh rút tiền', httpMethod: 'GET', apiPattern: '/api/v1/admin/partners/withdrawals*', description: 'Hiển thị menu Giải ngân & xem các yêu cầu rút tiền ví ký quỹ', viewPermission: true },
      { code: 'WITHDRAWAL_APPROVE', name: 'Phê duyệt giải ngân', httpMethod: 'PUT', apiPattern: '/api/v1/admin/partners/withdrawals/*/approve', description: 'Duyệt chi chuyển khoản và khấu trừ số dư Locked Balance', viewPermission: false },
      { code: 'WITHDRAWAL_REJECT', name: 'Từ chối lệnh rút tiền', httpMethod: 'PUT', apiPattern: '/api/v1/admin/partners/withdrawals/*/reject', description: 'Từ chối lệnh rút và hoàn trả số dư về ví khả dụng của nhà xe', viewPermission: false },
    ],
  },
  {
    moduleCode: 'MOD_BANNERS',
    moduleName: '4. Banner & Truyền thông',
    routePath: '/admin/banners',
    viewPermissionCode: 'BANNER_VIEW',
    permissions: [
      { code: 'BANNER_VIEW', name: 'Xem danh sách banner toàn sàn', httpMethod: 'GET', apiPattern: '/api/v1/admin/partners/banners*', description: 'Hiển thị menu Banner & xem danh sách chiến dịch quảng cáo', viewPermission: true },
      { code: 'BANNER_CREATE_DELETE', name: 'Tạo / Sửa / Xóa banner sàn', httpMethod: 'POST|PUT|DELETE', apiPattern: '/api/v1/admin/partners/banners*', description: 'Đăng mới, chỉnh sửa hoặc xóa banner truyền thông của sàn', viewPermission: false },
      { code: 'BANNER_APPROVE_REJECT', name: 'Phê duyệt / Gỡ bỏ banner nhà xe', httpMethod: 'PUT', apiPattern: '/api/v1/admin/partners/banners/*/approve|reject', description: 'Duyệt hiển thị hoặc từ chối banner do nhà xe gửi lên', viewPermission: false },
    ],
  },
  {
    moduleCode: 'MOD_IAM',
    moduleName: '5. Người dùng & Phân quyền RBAC',
    routePath: '/admin/users-bookings',
    viewPermissionCode: 'USER_VIEW',
    permissions: [
      { code: 'USER_VIEW', name: 'Xem 3 phân mục người dùng', httpMethod: 'GET', apiPattern: '/api/v1/admin/users*', description: 'Xem danh sách Khách hàng, Chủ xe và Quản trị viên', viewPermission: true },
      { code: 'USER_CREATE_UPDATE_DELETE', name: 'Tạo / Sửa / Xóa tài khoản', httpMethod: 'POST|PUT|DELETE', apiPattern: '/api/v1/admin/users*', description: 'Tạo mới, cập nhật hồ sơ hoặc xóa tài khoản người dùng', viewPermission: false },
      { code: 'USER_STATUS_LOCK', name: 'Khóa / Mở khóa tài khoản', httpMethod: 'PATCH', apiPattern: '/api/v1/admin/users/*/status', description: 'Khóa hoặc kích hoạt lại trạng thái hoạt động của tài khoản', viewPermission: false },
      { code: 'USER_PASSWORD_RESET', name: 'Đặt lại mật khẩu người dùng', httpMethod: 'POST', apiPattern: '/api/v1/admin/users/*/reset-password', description: 'Cấp lại mật khẩu mới cho tài khoản người dùng', viewPermission: false },
      { code: 'USER_ROLE_ASSIGN', name: 'Gán / Thay đổi Role cho người dùng', httpMethod: 'PATCH', apiPattern: '/api/v1/admin/users/*/role', description: 'Gán hoặc chuyển đổi Role cho tài khoản người dùng', viewPermission: false },
      { code: 'ROLE_MANAGE_CRUD', name: 'Quản lý Role & Phân quyền API', httpMethod: 'GET|POST|PUT|DELETE', apiPattern: '/api/v1/admin/roles*', description: 'Tạo mới, sửa ma trận 26 quyền API và xóa Custom Role', viewPermission: true },
    ],
  },
  {
    moduleCode: 'MOD_BOOKINGS',
    moduleName: '6. Đơn vé & Hoàn tiền Admin',
    routePath: '/admin/users-bookings',
    viewPermissionCode: 'BOOKING_VIEW',
    permissions: [
      { code: 'BOOKING_VIEW', name: 'Xem danh sách đơn vé toàn sàn', httpMethod: 'GET', apiPattern: '/api/v1/admin/bookings*', description: 'Tra cứu toàn bộ đơn đặt vé của hành khách trên hệ thống', viewPermission: true },
      { code: 'BOOKING_FORCE_REFUND', name: 'Hoàn tiền cưỡng chế (Admin Refund)', httpMethod: 'POST', apiPattern: '/api/v1/admin/bookings/*/refund', description: 'Thực thi lệnh hoàn tiền 100% và thu hồi ví ký quỹ khi có khiếu nại', viewPermission: false },
    ],
  },
  {
    moduleCode: 'MOD_SETTINGS',
    moduleName: '7. Bến xe, Chuyến xe & Cấu hình',
    routePath: '/admin/settings',
    viewPermissionCode: 'STATION_VIEW',
    permissions: [
      { code: 'STATION_VIEW', name: 'Xem danh mục bến xe liên tỉnh', httpMethod: 'GET', apiPattern: '/api/v1/admin/stations*', description: 'Hiển thị menu Cấu hình & xem danh sách nhà ga, bến xe', viewPermission: true },
      { code: 'STATION_CREATE_UPDATE_DELETE', name: 'Thêm / Sửa / Xóa bến xe', httpMethod: 'POST|PUT|DELETE', apiPattern: '/api/v1/admin/stations*', description: 'Thêm mới, cập nhật hoặc xóa bến xe liên tỉnh', viewPermission: false },
      { code: 'TRIP_MANAGE_CRUD', name: 'Quản lý chuyến xe (Tạo/Sửa/Hủy/Xóa)', httpMethod: 'GET|POST|PUT|PATCH|DELETE', apiPattern: '/api/v1/admin/trips*', description: 'Tạo chuyến xe, cập nhật giá/lịch trình, hủy hoặc xóa chuyến xe', viewPermission: true },
      { code: 'SYSTEM_SETTINGS_MANAGE', name: 'Quản lý cấu hình tham số toàn sàn', httpMethod: 'GET|PUT', apiPattern: '/api/v1/admin/settings & /commission/global', description: 'Cập nhật hoa hồng mặc định, TTL khóa ghế Redis và tham số vận hành', viewPermission: true },
    ],
  },
] as const
