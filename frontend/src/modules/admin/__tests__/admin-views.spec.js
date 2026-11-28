import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/modules/auth/store'
import { useAdminPermissions } from '../composables/useAdminPermissions'
import AdminDashboardView from '../views/AdminDashboardView.vue'
import AdminOperatorsView from '../views/AdminOperatorsView.vue'
import AdminWithdrawalsView from '../views/AdminWithdrawalsView.vue'
import AdminBannersView from '../views/AdminBannersView.vue'
import AdminUsersBookingsView from '../views/AdminUsersBookingsView.vue'
import AdminSettingsView from '../views/AdminSettingsView.vue'
import { adminApi } from '@/modules/admin/api'

function makeFakeJwt(payload) {
  const encodeUtf8Base64Url = (obj) => {
    const utf8Str = encodeURIComponent(JSON.stringify(obj)).replace(/%([0-9A-F]{2})/g, (_, p1) =>
      String.fromCharCode(parseInt(p1, 16))
    )
    return btoa(utf8Str)
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '')
  }
  return `${encodeUtf8Base64Url({ alg: 'HS256', typ: 'JWT' })}.${encodeUtf8Base64Url(payload)}.sig`
}

describe('Admin Backoffice Views & Platform Governance Tests (Phase FE-03)', () => {
  let pinia
  let auth

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    auth = useAuthStore()
    auth.token = makeFakeJwt({
      sub: 'admin',
      role: 'ROLE_ADMIN',
      customRole: 'ROLE_ADMIN',
      roleName: 'Quản Trị Viên Toàn Sàn (Super Admin)',
      exp: Math.floor(Date.now() / 1000) + 3600
    })

    // Setup network boundary mocks using authoritative Spring Boot DTO contracts (Section 4 & 15)
    vi.spyOn(adminApi, 'getPaymentReport').mockResolvedValue({
      success: true,
      message: 'Payment report retrieved successfully',
      data: {
        grossRevenue: 1450800000,
        netRevenue: 142500000,
        totalRefunded: 0,
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
            bookingId: 'BKG-20261123-7712',
            amount: 700000,
            currency: 'VND',
            paymentMethod: 'VNPAY',
            status: 'SUCCESS',
            reconciliationRequired: false,
            createdAt: '2026-11-23T14:20:00'
          },
          {
            id: 2,
            paymentReference: 'REC-20261122-04',
            bookingId: 'BKG-20261122-3319',
            amount: 380000,
            currency: 'VND',
            paymentMethod: 'VNPAY',
            status: 'SUCCESS',
            reconciliationRequired: true,
            createdAt: '2026-11-22T21:05:00'
          }
        ]
      }
    })

    vi.spyOn(adminApi, 'getPayments').mockResolvedValue({
      success: true,
      message: 'Payments retrieved',
      data: {
        items: [
          {
            id: 2,
            paymentReference: 'REC-20261122-04',
            bookingId: 'BKG-20261122-3319',
            amount: 380000,
            currency: 'VND',
            paymentMethod: 'VNPAY',
            status: 'SUCCESS',
            reconciliationRequired: true,
            createdAt: '2026-11-22T21:05:00'
          }
        ],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
        hasNext: false,
        hasPrevious: false
      }
    })

    vi.spyOn(adminApi, 'reconcilePayment').mockResolvedValue({
      success: true,
      message: 'Reconciled',
      data: {
        id: 2,
        paymentReference: 'REC-20261122-04',
        reconciliationRequired: false,
        status: 'SUCCESS'
      }
    })

    vi.spyOn(adminApi, 'getPermissionsCatalog').mockResolvedValue({
      success: true,
      data: []
    })

    const mockUsers = [
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
    ]

    vi.spyOn(adminApi, 'getUsers').mockImplementation(async (params) => {
      let filtered = [...mockUsers]
      if (params?.role && params.role !== 'ALL') {
        filtered = filtered.filter(u => u.category === params.role || u.role === params.role)
      }
      return {
        success: true,
        data: {
          items: filtered,
          page: 0,
          size: 10,
          totalElements: filtered.length,
          totalPages: 1,
          hasNext: false,
          hasPrevious: false
        }
      }
    })

    vi.spyOn(adminApi, 'getRoles').mockResolvedValue({
      success: true,
      data: [
        {
          id: 1,
          code: 'ROLE_ADMIN',
          name: 'Quản Trị Viên Toàn Sàn (Super Admin)',
          description: 'Toàn quyền điều hành',
          category: 'SYSTEM',
          systemDefault: true,
          permissions: ['PAYMENT_REPORT_VIEW', 'PAYMENT_LIST_VIEW', 'USER_VIEW', 'BOOKING_VIEW']
        },
        {
          id: 4,
          code: 'ROLE_ACCOUNTANT',
          name: 'Kế Toán & Đối Soát Tài Chính',
          description: 'Chuyên trách báo cáo doanh thu',
          category: 'SYSTEM',
          systemDefault: false,
          permissions: ['PAYMENT_REPORT_VIEW', 'PAYMENT_LIST_VIEW']
        }
      ]
    })

    vi.spyOn(adminApi, 'getBookings').mockResolvedValue({
      success: true,
      data: {
        items: [
          {
            id: 1,
            bookingId: 1,
            bookingReference: 'BKG-20261123-9988',
            customerName: 'Phan Văn Thành',
            customerPhone: '0912345678',
            userId: 3,
            tripId: 101,
            route: 'Hà Nội → Đà Nẵng',
            operatorName: 'Hoàng Long Limousine',
            seats: ['A05', 'A06'],
            totalAmount: 700000,
            status: 'CONFIRMED'
          }
        ],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
        hasNext: false,
        hasPrevious: false
      }
    })

    vi.spyOn(adminApi, 'refundBooking').mockResolvedValue({
      success: true,
      message: 'Booking refunded successfully',
      data: {
        bookingReference: 'BKG-20261123-9988',
        status: 'REFUNDED',
        refundPercentage: 100,
        refundAmount: 700000,
        paymentRefunded: true
      }
    })

    vi.spyOn(adminApi, 'createUser').mockResolvedValue({
      success: true,
      data: { id: 10, username: 'newuser' }
    })

    vi.spyOn(adminApi, 'createRole').mockResolvedValue({
      success: true,
      data: { id: 20, code: 'ROLE_CONTENT_MOD', name: 'Kiểm Duyệt Nội Dung & Banner', permissions: ['BANNER_VIEW'] }
    })

    vi.spyOn(adminApi, 'getOperators').mockResolvedValue({
      success: true,
      data: [
        { id: 'OP-01', brandName: 'Hoàng Long Limousine', businessName: 'Hoàng Long Limousine', representative: 'Nguyễn Hoàng Long', phone: '0901234567', email: 'hoanglong@velobus.vn', taxCode: '0109876543', customCommissionRate: 8.5, commissionRate: 8.5, vehicleCount: 18, status: 'ACTIVE' },
        { id: 'OP-02', brandName: 'Phương Trang Velo', businessName: 'Phương Trang Velo', representative: 'Lê Minh Phương', phone: '0918765432', email: 'phuongtrang@velobus.vn', taxCode: '0301122334', customCommissionRate: null, commissionRate: null, vehicleCount: 32, status: 'ACTIVE' },
        { id: 'OP-03', brandName: 'Hải Vân Express', businessName: 'Hải Vân Express', representative: 'Trần Hải Vân', phone: '0989112233', email: 'haivan@velobus.vn', taxCode: '0405566778', customCommissionRate: null, commissionRate: null, vehicleCount: 10, status: 'PENDING_APPROVAL' }
      ]
    })

    vi.spyOn(adminApi, 'getGlobalCommission').mockResolvedValue({
      success: true,
      data: { commissionRate: 10.0 }
    })

    vi.spyOn(adminApi, 'approveOperator').mockResolvedValue({
      success: true,
      data: { id: 'OP-03', status: 'ACTIVE' }
    })

    vi.spyOn(adminApi, 'updateOperatorCommission').mockResolvedValue({
      success: true,
      data: { id: 'OP-01', commissionRate: 7.5 }
    })

    vi.spyOn(adminApi, 'getWithdrawals').mockResolvedValue({
      success: true,
      data: [
        { id: 'WDR-20261123-01', operatorName: 'Hoàng Long Limousine', amount: 15000000, bankName: 'Vietcombank', accountNumber: '0071001234567', accountHolderName: 'Hoàng Long Ltd', requestedAt: '23/11/2026 10:15', status: 'PENDING' },
        { id: 'WDR-20261122-09', operatorName: 'Phương Trang Velo', amount: 35000000, bankName: 'Techcombank', accountNumber: '1903456789001', accountHolderName: 'Phương Trang Corp', requestedAt: '22/11/2026 16:40', status: 'APPROVED' }
      ]
    })

    vi.spyOn(adminApi, 'approveWithdrawal').mockResolvedValue({
      success: true,
      data: { id: 'WDR-20261123-01', status: 'APPROVED' }
    })

    vi.spyOn(adminApi, 'getStations').mockResolvedValue({
      success: true,
      data: [
        { id: 1, code: 'HN-GP', name: 'Bến xe Giáp Bát', city: 'Hà Nội', status: 'ACTIVE' },
        { id: 2, code: 'HN-MY', name: 'Bến xe Mỹ Đình', city: 'Hà Nội', status: 'ACTIVE' },
        { id: 3, code: 'DN-TT', name: 'Bến xe Trung Tâm Đà Nẵng', city: 'Đà Nẵng', status: 'ACTIVE' },
        { id: 4, code: 'SG-MD', name: 'Bến xe Miền Đông Mới', city: 'TP. Hồ Chí Minh', status: 'ACTIVE' },
        { id: 5, code: 'DL-LD', name: 'Bến xe Liên Tỉnh Đà Lạt', city: 'Lâm Đồng', status: 'ACTIVE' }
      ]
    })

    vi.spyOn(adminApi, 'createStation').mockResolvedValue({
      success: true,
      data: { id: 6, code: 'HP-TL', name: 'Bến xe Thượng Lý', city: 'Hải Phòng' }
    })

    vi.spyOn(adminApi, 'getTrips').mockResolvedValue({
      success: true,
      data: [
        { id: 101, tripCode: 'VB-HN-DN-01', departureCity: 'Hà Nội', arrivalCity: 'Đà Nẵng', departureTime: '2026-12-01T20:00:00', basePrice: 450000, totalSeats: 34, status: 'SCHEDULED' },
        { id: 102, tripCode: 'VB-SG-DL-02', departureCity: 'TP. Hồ Chí Minh', arrivalCity: 'Đà Lạt', departureTime: '2026-12-02T22:30:00', basePrice: 380000, totalSeats: 24, status: 'SCHEDULED' }
      ]
    })

    vi.spyOn(adminApi, 'createTrip').mockResolvedValue({
      success: true,
      data: { id: 103, tripCode: 'VB-HP-HN-09', departureCity: 'Hải Phòng', arrivalCity: 'Hà Nội', departureTime: '2026-12-05T21:00:00', basePrice: 400000, totalSeats: 34, status: 'SCHEDULED' }
    })

    vi.spyOn(adminApi, 'getSystemSettings').mockResolvedValue({
      success: true,
      data: {
        platformName: 'VeloBus - Sàn Vé Xe Khách Cao Cấp',
        defaultCommissionRate: '10.0',
        seatHoldTtlMinutes: '10',
        turnaroundBufferMinutes: '30',
        escrowSettlementDelayHours: '2'
      }
    })

    vi.spyOn(adminApi, 'updateSystemSettings').mockResolvedValue({
      success: true,
      data: {}
    })
  })

  describe('FE-SEC-001: useAdminPermissions Fail-Closed Security Invariant', () => {
    it('returns false for all permissions and isSuperAdmin when user is unauthenticated (GUEST)', () => {
      auth.logout()

      const perms = useAdminPermissions()
      expect(perms.isSuperAdmin.value).toBe(false)
      expect(perms.customRole.value).toBe('GUEST')
      expect(perms.permissions.value).toEqual([])
      expect(perms.hasPermission('PAYMENT_REPORT_VIEW')).toBe(false)
      expect(perms.hasPermission('')).toBe(false)
      expect(perms.hasPermission(null)).toBe(false)
      expect(perms.hasAnyPermission(['USER_VIEW', 'BOOKING_VIEW'])).toBe(false)
      expect(perms.hasAnyPermission([])).toBe(false)
    })
  })

  describe('AdminDashboardView.vue - KPI & Reconciliation', () => {
    it('renders 4 system KPI cards and resolves reconciliation discrepancy', async () => {
      const wrapper = mount(AdminDashboardView, { global: { plugins: [pinia] } })
      await flushPromises()

      expect(wrapper.find('[data-testid="kpi-gmv"]').text()).toContain('1.450.800.000')
      expect(wrapper.find('[data-testid="kpi-commission"]').text()).toContain('142.500.000')
      expect(wrapper.text()).toContain('1 cảnh báo cần xử lý')

      const resolveBtn = wrapper.find('[data-testid="btn-resolve-alert"]')
      expect(resolveBtn.exists()).toBe(true)
      await resolveBtn.trigger('click')
      await flushPromises()

      // Confirm mutation dialog
      const confirmAction = wrapper.findComponent({ name: 'AlertDialogAction' })
      if (confirmAction.exists()) {
        // Mock report update to 0 alerts
        vi.spyOn(adminApi, 'getPaymentReport').mockResolvedValueOnce({
          success: true,
          data: {
            grossRevenue: 1450800000,
            netRevenue: 142500000,
            totalTransactions: 3842,
            reconciliationCount: 0,
            reconciliationTransactions: []
          }
        })
        vi.spyOn(adminApi, 'getPayments').mockResolvedValueOnce({
          success: true,
          data: {
            items: [],
            page: 0,
            size: 10,
            totalElements: 0,
            totalPages: 1,
            hasNext: false,
            hasPrevious: false
          }
        })
        await confirmAction.trigger('click')
        await flushPromises()
      }

      expect(wrapper.text()).toContain('0 cảnh báo cần xử lý')
    })
  })

  describe('AdminOperatorsView.vue - 2-Tier Commission & Operator Approval', () => {
    it('approves pending operator and validates custom commission rate boundary (0% - 50%)', async () => {
      const wrapper = mount(AdminOperatorsView, { global: { plugins: [pinia] } })
      await flushPromises()

      // Approve pending operator OP-03
      const approveBtn = wrapper.find('[data-testid="btn-approve-OP-03"]')
      expect(approveBtn.exists()).toBe(true)
      await approveBtn.trigger('click')
      expect(wrapper.find('[data-testid="btn-approve-OP-03"]').exists()).toBe(false)

      // Open 2-tier commission dialog for OP-01
      const commBtn = wrapper.find('[data-testid="btn-commission-OP-01"]')
      await commBtn.trigger('click')

      const input = wrapper.find('[data-testid="input-custom-commission"]')
      expect(input.exists()).toBe(true)

      // Test boundary violation (> 50%)
      await input.setValue(65)
      await wrapper.find('[data-testid="btn-save-commission"]').trigger('click')
      expect(wrapper.text()).toContain('Tỷ lệ hoa hồng riêng phải nằm trong khoảng từ 0% đến 50%')

      // Valid custom rate (7.5%)
      await input.setValue(7.5)
      await wrapper.find('[data-testid="btn-save-commission"]').trigger('click')
      expect(wrapper.text()).toContain('7.5% (Riêng)')
    })
  })

  describe('AdminWithdrawalsView.vue - Escrow Payout Approval', () => {
    it('approves pending escrow withdrawal request', async () => {
      const wrapper = mount(AdminWithdrawalsView, { global: { plugins: [pinia] } })
      await flushPromises()

      const approveBtn = wrapper.find('[data-testid="btn-approve-withdraw-WDR-20261123-01"]')
      expect(approveBtn.exists()).toBe(true)
      await approveBtn.trigger('click')

      expect(wrapper.find('[data-testid="btn-approve-withdraw-WDR-20261123-01"]').exists()).toBe(false)
      expect(wrapper.text()).toContain('Đã đóng lệnh')
    })
  })

  describe('AdminUsersBookingsView.vue - RBAC Self-Lock Guard & Admin Forced Refund', () => {
    it('protects current admin account from self-lock and enforces refund audit reason', async () => {
      const wrapper = mount(AdminUsersBookingsView, { global: { plugins: [pinia] } })
      await flushPromises()

      // Invariant: ADMIN_CANNOT_LOCK_SELF
      const selfGuard = wrapper.find('[data-testid="self-admin-guard"]')
      expect(selfGuard.exists()).toBe(true)
      expect(selfGuard.text()).toContain('Không thể tự khóa')
      expect(wrapper.find('[data-testid="btn-lock-1"]').exists()).toBe(false)

      // Switch to Bookings tab and test Admin Forced Refund
      await wrapper.find('[data-testid="tab-admin-bookings"]').trigger('click')
      await flushPromises()

      const refundBtn = wrapper.find('[data-testid="btn-admin-refund-BKG-20261123-9988"]')
      expect(refundBtn.exists()).toBe(true)
      await refundBtn.trigger('click')
      await flushPromises()

      // Submit short reason (< 10 chars) -> blocked
      const reasonInput = wrapper.find('[data-testid="input-refund-reason"]')
      await reasonInput.setValue('Lỗi')
      await wrapper.find('[data-testid="btn-confirm-refund"]').trigger('click')
      await flushPromises()
      expect(wrapper.text()).toContain('tối thiểu 10 ký tự')

      // Valid audit reason
      await reasonInput.setValue('Nhà xe gặp sự cố kỹ thuật đột xuất, hoàn 100% giá vé cho hành khách.')
      await wrapper.find('[data-testid="btn-confirm-refund"]').trigger('click')
      await flushPromises()
      expect(wrapper.text()).toContain('Đã hoàn tiền')
    })

    it('supports 3 user categories filtering, user creation, role assignment and dynamic 7x26 API role creation', async () => {
      const wrapper = mount(AdminUsersBookingsView, { global: { plugins: [pinia] } })
      await flushPromises()

      // 1. Filter by 3 user categories: Customer, Operator, System Staff
      await wrapper.find('[data-testid="category-tab-CUSTOMER"]').trigger('click')
      await flushPromises()
      expect(wrapper.text()).toContain('Phan Văn Thành')
      expect(wrapper.text()).not.toContain('Nguyễn Hoàng Long')

      await wrapper.find('[data-testid="category-tab-OPERATOR"]').trigger('click')
      await flushPromises()
      expect(wrapper.text()).toContain('Nguyễn Hoàng Long')

      await wrapper.find('[data-testid="category-tab-SYSTEM"]').trigger('click')
      await flushPromises()
      expect(wrapper.text()).toContain('Quản Trị Viên Toàn Sàn')

      await wrapper.find('[data-testid="category-tab-ALL"]').trigger('click')
      await flushPromises()

      // 2. Switch to Roles tab and create a new Custom System Role with 7 Modules x 26 APIs matrix
      await wrapper.find('[data-testid="tab-admin-roles"]').trigger('click')
      await flushPromises()
      expect(wrapper.text()).toContain('Phân Quyền Động Theo Ma Trận 7 Modules × 26 APIs')
      expect(wrapper.find('[data-testid="role-card-ROLE_ACCOUNTANT"]').exists()).toBe(true)

      await wrapper.find('[data-testid="btn-create-role-in-tab"]').trigger('click')
      await flushPromises()
      await wrapper.find('[data-testid="input-role-code"]').setValue('content_mod')
      await wrapper.find('[data-testid="input-role-name"]').setValue('Kiểm Duyệt Nội Dung & Banner')

      // Toggle MOD_BANNERS module in matrix
      await wrapper.find('[data-testid="toggle-module-MOD_BANNERS"]').trigger('click')

      // Mock update to include new role
      vi.spyOn(adminApi, 'getRoles').mockResolvedValueOnce({
        success: true,
        data: [
          {
            id: 1,
            code: 'ROLE_ADMIN',
            name: 'Quản Trị Viên Toàn Sàn (Super Admin)',
            category: 'SYSTEM',
            systemDefault: true,
            permissions: []
          },
          {
            id: 20,
            code: 'ROLE_CONTENT_MOD',
            name: 'Kiểm Duyệt Nội Dung & Banner',
            category: 'SYSTEM',
            systemDefault: false,
            permissions: ['BANNER_VIEW']
          }
        ]
      })

      await wrapper.find('[data-testid="btn-save-role"]').trigger('click')
      await flushPromises()

      expect(wrapper.find('[data-testid="role-card-ROLE_CONTENT_MOD"]').exists()).toBe(true)
      expect(wrapper.text()).toContain('Kiểm Duyệt Nội Dung & Banner')
    })
  })

  describe('AdminSettingsView.vue - Global System Parameters, Stations & Trips CRUD', () => {
    it('renders global parameters, saves settings confirmation, and performs station/trip CRUD', async () => {
      const wrapper = mount(AdminSettingsView, { global: { plugins: [pinia] } })
      await flushPromises()

      expect(wrapper.text()).toContain('Danh Mục Bến Xe Liên Tỉnh (5)')
      await wrapper.find('[data-testid="btn-save-settings"]').trigger('click')

      const alert = wrapper.find('[data-testid="settings-saved-alert"]')
      expect(alert.exists()).toBe(true)
      expect(alert.text()).toContain('đồng bộ Redis Cache thành công')

      // Add new station
      await wrapper.find('[data-testid="input-station-code"]').setValue('HP-TL')
      await wrapper.find('[data-testid="input-station-name"]').setValue('Bến xe Thượng Lý')
      await wrapper.find('[data-testid="input-station-city"]').setValue('Hải Phòng')
      await wrapper.find('[data-testid="save-station-btn"]').trigger('click')
      expect(wrapper.text()).toContain('Danh Mục Bến Xe Liên Tỉnh (6)')
      expect(wrapper.text()).toContain('Bến xe Thượng Lý')

      // Add new trip
      await wrapper.find('[data-testid="input-trip-code"]').setValue('VB-HP-HN-09')
      await wrapper.find('[data-testid="input-trip-route"]').setValue('Hải Phòng → Hà Nội')
      await wrapper.find('[data-testid="save-trip-btn"]').trigger('click')
      expect(wrapper.text()).toContain('VB-HP-HN-09')
    })
  })

  describe('AdminBannersView.vue & Dynamic RBAC UI Permission Hiding (Phase 07)', () => {
    it('renders AdminBannersView and creates platform banner when permission granted', async () => {
      vi.spyOn(adminApi, 'getBanners').mockResolvedValue({ data: [] })
      vi.spyOn(adminApi, 'createPlatformBanner').mockResolvedValue({
        data: { id: 99, title: 'Banner Tết 2027', imageUrl: 'https://cdn.velobus.vn/banner.jpg', position: 'HOMEPAGE_HERO', status: 'ACTIVE' }
      })

      const wrapper = mount(AdminBannersView, { global: { plugins: [pinia] } })
      await flushPromises()
      expect(wrapper.text()).toContain('Kiểm Duyệt Banner Quảng Cáo Toàn Sàn')
      expect(wrapper.find('[data-testid="create-banner-btn"]').exists()).toBe(true)

      await wrapper.find('[data-testid="create-banner-btn"]').trigger('click')
      await wrapper.find('[data-testid="input-banner-title"]').setValue('Banner Tết 2027')
      await wrapper.find('[data-testid="input-banner-image"]').setValue('https://cdn.velobus.vn/banner.jpg')

      vi.spyOn(adminApi, 'getBanners').mockResolvedValue({
        data: [{ id: 99, title: 'Banner Tết 2027', imageUrl: 'https://cdn.velobus.vn/banner.jpg', position: 'HOMEPAGE_HERO', status: 'ACTIVE' }]
      })

      await wrapper.find('[data-testid="btn-save-banner"]').trigger('click')
      await flushPromises()
      expect(wrapper.text()).toContain('Banner Tết 2027')
    })

    it('hides CRUD action buttons when logged in as a Read-Only Custom System Role and supports Role Preview', async () => {
      // Login as ROLE_READONLY_AUDITOR with only *_VIEW permissions (no write/approve/refund permissions)
      auth.token = makeFakeJwt({
        sub: 'auditor01',
        role: 'ROLE_ADMIN',
        customRole: 'ROLE_AUDITOR',
        roleName: 'Kiểm Toán Nội Bộ (Read-Only)',
        permissions: [
          'PAYMENT_REPORT_VIEW',
          'PAYMENT_LIST_VIEW',
          'OPERATOR_VIEW',
          'WITHDRAWAL_VIEW',
          'BANNER_VIEW',
          'BOOKING_VIEW',
          'STATION_VIEW'
        ],
        exp: Math.floor(Date.now() / 1000) + 3600
      })

      // 1. AdminOperatorsView: create, approve, commission buttons must be hidden
      const opWrapper = mount(AdminOperatorsView, { global: { plugins: [pinia] } })
      await flushPromises()
      expect(opWrapper.find('[data-testid="open-create-operator-btn"]').exists()).toBe(false)
      expect(opWrapper.find('[data-testid="btn-approve-OP-03"]').exists()).toBe(false)
      expect(opWrapper.find('[data-testid="btn-commission-OP-01"]').exists()).toBe(false)

      // 2. AdminBannersView: create, approve, edit, delete buttons must be hidden
      const bannerWrapper = mount(AdminBannersView, { global: { plugins: [pinia] } })
      await flushPromises()
      expect(bannerWrapper.find('[data-testid="create-banner-btn"]').exists()).toBe(false)
      expect(bannerWrapper.find('[data-testid="btn-approve-banner-BAN-02"]').exists()).toBe(false)

      // 3. AdminUsersBookingsView: Auditor has BOOKING_VIEW but NOT USER_VIEW or BOOKING_FORCE_REFUND
      const iamWrapper = mount(AdminUsersBookingsView, { global: { plugins: [pinia] } })
      await flushPromises()
      expect(iamWrapper.find('[data-testid="tab-admin-users"]').exists()).toBe(false)
      expect(iamWrapper.find('[data-testid="tab-admin-roles"]').exists()).toBe(false)
      expect(iamWrapper.find('[data-testid="btn-admin-refund-BKG-20261123-9988"]').exists()).toBe(false)
      expect(iamWrapper.text()).toContain('Chỉ xem (Thiếu quyền hoàn vé)')

      // 4. Role Preview mode in store
      auth.startRolePreview({
        code: 'ROLE_ACCOUNTANT',
        name: 'Kế Toán & Đối Soát',
        permissions: ['WITHDRAWAL_VIEW', 'WITHDRAWAL_APPROVE']
      })
      expect(auth.customRole).toBe('ROLE_ACCOUNTANT')
      expect(auth.hasPermission('WITHDRAWAL_APPROVE')).toBe(true)
      expect(auth.hasPermission('WITHDRAWAL_REJECT')).toBe(false)
      auth.stopRolePreview()
    })
  })
})
