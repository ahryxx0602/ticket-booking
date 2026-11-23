import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import AdminDashboardView from '../views/AdminDashboardView.vue'
import AdminOperatorsView from '../views/AdminOperatorsView.vue'
import AdminWithdrawalsView from '../views/AdminWithdrawalsView.vue'
import AdminUsersBookingsView from '../views/AdminUsersBookingsView.vue'
import AdminSettingsView from '../views/AdminSettingsView.vue'

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
  })

  describe('AdminSettingsView.vue - Global System Parameters', () => {
    it('renders global parameters and saves settings confirmation', async () => {
      const wrapper = mount(AdminSettingsView)

      expect(wrapper.text()).toContain('Danh Mục Bến Xe Liên Tỉnh (5)')
      await wrapper.find('[data-testid="btn-save-settings"]').trigger('click')

      const alert = wrapper.find('[data-testid="settings-saved-alert"]')
      expect(alert.exists()).toBe(true)
      expect(alert.text()).toContain('đồng bộ Redis Cache thành công')
    })
  })
})
