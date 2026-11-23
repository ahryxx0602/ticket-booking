import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import OperatorDashboardView from '../views/OperatorDashboardView.vue'
import FleetAndDriversView from '../views/FleetAndDriversView.vue'
import OperatorTripsView from '../views/OperatorTripsView.vue'

describe('Operator Portal Views & Business Logic Tests (Phase FE-02)', () => {
  describe('OperatorDashboardView.vue', () => {
    it('renders 4 fintech balance cards with formatted VND amounts', () => {
      const wrapper = mount(OperatorDashboardView, {
        global: {
          stubs: {
            StatusBadge: true
          }
        }
      })

      expect(wrapper.text()).toContain('Tổng Quan Ví Ký Quỹ Escrow (4 Số Dư)')
      expect(wrapper.text()).toContain('Số Dư Khả Dụng')
      expect(wrapper.text()).toContain('Đang Ký Quỹ Chuyến')
      expect(wrapper.text()).toContain('Đang Xử Lý Rút')
      expect(wrapper.text()).toContain('Tổng Tiền Đã Rút')

      const availableCard = wrapper.find('[data-testid="available-balance"]')
      expect(availableCard.exists()).toBe(true)
      expect(availableCard.text()).toContain('42.500.000')
    })

    it('opens withdrawal modal and blocks amount greater than available balance', async () => {
      const wrapper = mount(OperatorDashboardView, {
        global: {
          stubs: {
            StatusBadge: true
          }
        }
      })

      const openBtn = wrapper.find('[data-testid="btn-open-withdraw"]')
      await openBtn.trigger('click')

      expect(wrapper.text()).toContain('Yêu Cầu Rút Tiền Về Ngân Hàng')

      // Set withdraw amount greater than 42,500,000 đ
      const amountInput = wrapper.find('[data-testid="input-withdraw-amount"]')
      expect(amountInput.exists()).toBe(true)
      await amountInput.setValue(50000000)

      const submitBtn = wrapper.find('[data-testid="btn-submit-withdraw"]')
      await submitBtn.trigger('click')

      expect(wrapper.text()).toContain('vượt quá số dư khả dụng')
    })
  })

  describe('FleetAndDriversView.vue', () => {
    it('switches between vehicles tab and drivers tab smoothly', async () => {
      const wrapper = mount(FleetAndDriversView, {
        global: {
          stubs: {
            StatusBadge: true
          }
        }
      })

      // Default tab: Vehicles
      expect(wrapper.text()).toContain('Danh Sách Phương Tiện')
      expect(wrapper.text()).toContain('29B-888.99')

      // Click Drivers tab
      const driversTab = wrapper.find('[data-testid="tab-drivers"]')
      await driversTab.trigger('click')

      expect(wrapper.text()).toContain('Đội Ngũ Tài Xế')
      expect(wrapper.text()).toContain('Trần Văn Đức')
      expect(wrapper.text()).toContain('0987654321')
    })

    it('opens vehicle drawer on click of add vehicle button', async () => {
      const wrapper = mount(FleetAndDriversView, {
        global: {
          stubs: {
            StatusBadge: true
          }
        }
      })

      const addBtn = wrapper.find('[data-testid="btn-add-vehicle"]')
      await addBtn.trigger('click')

      expect(wrapper.text()).toContain('Thêm Phương Tiện Mới Vào Đội Xe')
      expect(wrapper.text()).toContain('Biển số đăng ký xe')
    })
  })

  describe('OperatorTripsView.vue - Turnaround Buffer 30-min Invariant', () => {
    it('detects turnaround buffer violation and disables submit button when interval < 30 mins', async () => {
      const wrapper = mount(OperatorTripsView, {
        global: {
          stubs: {
            StatusBadge: true
          }
        }
      })

      const openScheduleBtn = wrapper.find('[data-testid="btn-open-schedule"]')
      await openScheduleBtn.trigger('click')

      expect(wrapper.text()).toContain('Lập Lịch Chuyến Xe Mới (30-Min Buffer Check)')

      // TripForm has departureTime at 10:00 (safe, after 09:00 + 30m) -> No violation
      let alert = wrapper.find('[data-testid="buffer-violation-alert"]')
      expect(alert.exists()).toBe(false)

      // Change departure time to 09:15 (violation: previous trip arrives at 09:00, buffer is only 15 mins)
      const depInput = wrapper.find('[data-testid="input-trip-dep-time"]')
      expect(depInput.exists()).toBe(true)
      await depInput.setValue('2026-11-24 09:15')

      alert = wrapper.find('[data-testid="buffer-violation-alert"]')
      expect(alert.exists()).toBe(true)
      expect(alert.text()).toContain('Vi Phạm Quy Chuẩn Đệm Quay Đầu 30 Phút')
      expect(alert.text()).toContain('Cần thời gian đệm bảo dưỡng/vệ sinh tối thiểu 30 phút')
    })
  })
})
