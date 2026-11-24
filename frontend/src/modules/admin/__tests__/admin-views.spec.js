import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/modules/auth/store'
import AdminDashboardView from '../views/AdminDashboardView.vue'
import AdminOperatorsView from '../views/AdminOperatorsView.vue'
import AdminWithdrawalsView from '../views/AdminWithdrawalsView.vue'
import AdminBannersView from '../views/AdminBannersView.vue'
import AdminUsersBookingsView from '../views/AdminUsersBookingsView.vue'
import AdminSettingsView from '../views/AdminSettingsView.vue'

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
  describe('AdminDashboardView.vue - KPI & Reconciliation', () => {
    it('renders 4 system KPI cards and resolves reconciliation discrepancy', async () => {
      const wrapper = mount(AdminDashboardView)

      expect(wrapper.find('[data-testid="kpi-gmv"]').text()).toContain('1.450.800.000')
      expect(wrapper.find('[data-testid="kpi-commission"]').text()).toContain('142.500.000')
      expect(wrapper.text()).toContain('1 cảnh báo cần xử lý')

      const resolveBtn = wrapper.find('[data-testid="btn-resolve-alert"]')
      expect(resolveBtn.exists()).toBe(true)
      await resolveBtn.trigger('click')

      expect(wrapper.text()).toContain('0 cảnh báo cần xử lý')
    })
  })

  describe('AdminOperatorsView.vue - 2-Tier Commission & Operator Approval', () => {
    it('approves pending operator and validates custom commission rate boundary (0% - 50%)', async () => {
      const wrapper = mount(AdminOperatorsView)

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
      const wrapper = mount(AdminWithdrawalsView)

      const approveBtn = wrapper.find('[data-testid="btn-approve-withdraw-WDR-20261123-01"]')
      expect(approveBtn.exists()).toBe(true)
      await approveBtn.trigger('click')

      expect(wrapper.find('[data-testid="btn-approve-withdraw-WDR-20261123-01"]').exists()).toBe(false)
      expect(wrapper.text()).toContain('Đã đóng lệnh')
    })
  })

  describe('AdminUsersBookingsView.vue - RBAC Self-Lock Guard & Admin Forced Refund', () => {
    it('protects current admin account from self-lock and enforces refund audit reason', async () => {
      const wrapper = mount(AdminUsersBookingsView)

      // Invariant: ADMIN_CANNOT_LOCK_SELF
      const selfGuard = wrapper.find('[data-testid="self-admin-guard"]')
      expect(selfGuard.exists()).toBe(true)
      expect(selfGuard.text()).toContain('Không thể tự khóa')
      expect(wrapper.find('[data-testid="btn-lock-USR-ADMIN-01"]').exists()).toBe(false)

      // Switch to Bookings tab and test Admin Forced Refund
      await wrapper.find('[data-testid="tab-admin-bookings"]').trigger('click')
      const refundBtn = wrapper.find('[data-testid="btn-admin-refund-BKG-20261123-9988"]')
      expect(refundBtn.exists()).toBe(true)
      await refundBtn.trigger('click')

      // Submit short reason (< 10 chars) -> blocked
      const reasonInput = wrapper.find('[data-testid="input-refund-reason"]')
      await reasonInput.setValue('Lỗi')
      await wrapper.find('[data-testid="btn-confirm-refund"]').trigger('click')
      expect(wrapper.text()).toContain('tối thiểu 10 ký tự')

      // Valid audit reason
      await reasonInput.setValue('Nhà xe gặp sự cố kỹ thuật đột xuất, hoàn 100% giá vé cho hành khách.')
      await wrapper.find('[data-testid="btn-confirm-refund"]').trigger('click')
      expect(wrapper.text()).toContain('Đã hoàn tiền')
    })

    it('supports 3 user categories filtering, user creation, role assignment and dynamic 7x26 API role creation', async () => {
      const wrapper = mount(AdminUsersBookingsView)

      // 1. Filter by 3 user categories: Customer, Operator, System Staff
      await wrapper.find('[data-testid="category-tab-CUSTOMER"]').trigger('click')
      expect(wrapper.text()).toContain('Phan Văn Thành')
      expect(wrapper.text()).not.toContain('Nguyễn Hoàng Long')

      await wrapper.find('[data-testid="category-tab-OPERATOR"]').trigger('click')
      expect(wrapper.text()).toContain('Nguyễn Hoàng Long')

      await wrapper.find('[data-testid="category-tab-SYSTEM"]').trigger('click')
      expect(wrapper.text()).toContain('Quản Trị Viên Toàn Sàn')
      expect(wrapper.text()).toContain('ROLE_ACCOUNTANT')

      await wrapper.find('[data-testid="category-tab-ALL"]').trigger('click')

      // 2. Switch to Roles tab and create a new Custom System Role with 7 Modules x 26 APIs matrix
      await wrapper.find('[data-testid="tab-admin-roles"]').trigger('click')
      expect(wrapper.text()).toContain('Phân Quyền Động Theo Ma Trận 7 Modules × 26 APIs')
      expect(wrapper.find('[data-testid="role-card-ROLE_ACCOUNTANT"]').exists()).toBe(true)

      await wrapper.find('[data-testid="btn-create-role-in-tab"]').trigger('click')
      await wrapper.find('[data-testid="input-role-code"]').setValue('content_mod')
      await wrapper.find('[data-testid="input-role-name"]').setValue('Kiểm Duyệt Nội Dung & Banner')

      // Toggle MOD_BANNERS module in matrix
      await wrapper.find('[data-testid="toggle-module-MOD_BANNERS"]').trigger('click')
      await wrapper.find('[data-testid="btn-save-role"]').trigger('click')

      expect(wrapper.find('[data-testid="role-card-ROLE_CONTENT_MOD"]').exists()).toBe(true)
      expect(wrapper.text()).toContain('Kiểm Duyệt Nội Dung & Banner')
    })
  })

  describe('AdminSettingsView.vue - Global System Parameters, Stations & Trips CRUD', () => {
    it('renders global parameters, saves settings confirmation, and performs station/trip CRUD', async () => {
      const wrapper = mount(AdminSettingsView)

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
      const wrapper = mount(AdminBannersView)
      expect(wrapper.text()).toContain('Kiểm Duyệt Banner Quảng Cáo Toàn Sàn')
      expect(wrapper.find('[data-testid="create-banner-btn"]').exists()).toBe(true)

      await wrapper.find('[data-testid="create-banner-btn"]').trigger('click')
      await wrapper.find('[data-testid="input-banner-title"]').setValue('Banner Tết 2027')
      await wrapper.find('[data-testid="btn-save-banner"]').trigger('click')
      expect(wrapper.text()).toContain('Banner Tết 2027')
    })

    it('hides CRUD action buttons when logged in as a Read-Only Custom System Role and supports Role Preview', async () => {
      const pinia = createPinia()
      setActivePinia(pinia)
      const auth = useAuthStore()

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
      expect(opWrapper.find('[data-testid="open-create-operator-btn"]').exists()).toBe(false)
      expect(opWrapper.find('[data-testid="btn-approve-OP-03"]').exists()).toBe(false)
      expect(opWrapper.find('[data-testid="btn-commission-OP-01"]').exists()).toBe(false)

      // 2. AdminBannersView: create, approve, edit, delete buttons must be hidden
      const bannerWrapper = mount(AdminBannersView, { global: { plugins: [pinia] } })
      expect(bannerWrapper.find('[data-testid="create-banner-btn"]').exists()).toBe(false)
      expect(bannerWrapper.find('[data-testid="btn-approve-banner-BAN-02"]').exists()).toBe(false)

      // 3. AdminUsersBookingsView: Auditor has BOOKING_VIEW but NOT USER_VIEW or BOOKING_FORCE_REFUND
      const iamWrapper = mount(AdminUsersBookingsView, { global: { plugins: [pinia] } })
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
