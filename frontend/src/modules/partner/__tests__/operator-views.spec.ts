import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import OperatorDashboardView from '../views/OperatorDashboardView.vue'
import FleetAndDriversView from '../views/FleetAndDriversView.vue'
import OperatorTripsView from '../views/OperatorTripsView.vue'
import { partnerApi } from '@/modules/partner/api'

describe('Operator Portal Views & Business Logic Tests (Phase FE-02)', () => {
  beforeEach(() => {
    vi.spyOn(partnerApi, 'getWallet').mockResolvedValue({
      success: true,
      message: 'Wallet retrieved',
      data: {
        id: 1,
        operatorId: 10,
        availableBalance: 42500000,
        pendingSettlementBalance: 18700000,
        lockedBalance: 5200000,
        debtBalance: 0,
        currency: 'VND'
      }
    })
    vi.spyOn(partnerApi, 'getWalletTransactions').mockResolvedValue({
      success: true,
      message: 'Transactions retrieved',
      data: [
        {
          id: 9081,
          walletId: 1,
          operatorId: 10,
          bookingId: 7712,
          tripId: 101,
          paymentReference: 'TXN-9081',
          transactionType: 'TICKET_ESCROW_CREDIT',
          grossAmount: 700000,
          commissionRate: 10,
          commissionAmount: 70000,
          netAmount: 630000,
          balanceBefore: 41870000,
          balanceAfter: 42500000,
          description: 'Cộng tiền vé (Ký quỹ)',
          createdAt: '2026-11-23T14:15:00'
        }
      ]
    })
    vi.spyOn(partnerApi, 'getWithdrawals').mockResolvedValue({
      success: true,
      message: 'Withdrawals retrieved',
      data: [
        {
          id: 1,
          operatorId: 10,
          amount: 120000000,
          bankName: 'Vietcombank',
          accountNumber: '0071001234567',
          accountHolderName: 'HOANG LONG',
          status: 'APPROVED',
          createdAt: '2026-11-20T10:00:00'
        }
      ]
    })
  })

  describe('OperatorDashboardView.vue', () => {
    it('renders 4 fintech balance cards with formatted VND amounts', async () => {
      const wrapper = mount(OperatorDashboardView, {
        global: {
          stubs: {
            StatusBadge: true
          }
        }
      })
      await flushPromises()

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
      await flushPromises()

      const openBtn = wrapper.find('[data-testid="btn-open-withdraw"]')
      await openBtn.trigger('click')

      expect(wrapper.text()).toContain('Yêu Cầu Rút Tiền Về Ngân Hàng')

      // Set withdraw amount greater than 42,500,000 đ
      const amountInput = wrapper.find('[data-testid="input-withdraw-amount"]')
      expect(amountInput.exists()).toBe(true)
      await amountInput.setValue(50000000)

      const submitBtn = wrapper.find('[data-testid="btn-submit-withdraw"]')
      await submitBtn.trigger('click')
      await flushPromises()

      expect(wrapper.text()).toContain('vượt quá số dư khả dụng')
    })

    it('renders dash instead of fabricated zero when withdrawals API fails', async () => {
      vi.spyOn(partnerApi, 'getWithdrawals').mockRejectedValue(new Error('Withdrawal service down'))
      const wrapper = mount(OperatorDashboardView, {
        global: {
          stubs: {
            StatusBadge: true
          }
        }
      })
      await flushPromises()

      const totalWithdrawnCard = wrapper.find('[data-testid="total-withdrawn"]')
      expect(totalWithdrawnCard.exists()).toBe(true)
      expect(totalWithdrawnCard.text()).toBe('—')
    })

    it('blocks withdrawal when operator has an active debt balance', async () => {
      vi.spyOn(partnerApi, 'getWallet').mockResolvedValue({
        success: true,
        message: 'Wallet with debt',
        data: {
          id: 1,
          operatorId: 10,
          availableBalance: 42500000,
          pendingSettlementBalance: 0,
          lockedBalance: 0,
          debtBalance: 1500000,
          currency: 'VND'
        }
      })
      const wrapper = mount(OperatorDashboardView, {
        global: {
          stubs: {
            StatusBadge: true
          }
        }
      })
      await flushPromises()

      const openBtn = wrapper.find('[data-testid="btn-open-withdraw"]')
      await openBtn.trigger('click')

      const submitBtn = wrapper.find('[data-testid="btn-submit-withdraw"]')
      await submitBtn.trigger('click')
      await flushPromises()

      expect(wrapper.text()).toContain('Không thể tạo lệnh rút khi đang có khoản dư nợ đối soát')
    })
  })

  describe('FleetAndDriversView.vue', () => {
    it('switches between vehicles tab and drivers tab smoothly', async () => {
      vi.spyOn(partnerApi, 'getVehicles').mockResolvedValue({
        data: {
          code: 'OK',
          data: [{ id: 1, plateNumber: '29B-888.99', modelName: 'Thaco', status: 'ACTIVE' }]
        }
      })
      vi.spyOn(partnerApi, 'getDrivers').mockResolvedValue({
        data: {
          code: 'OK',
          data: [{ id: 1, fullName: 'Trần Văn Đức', phone: '0987654321', licenseNumber: 'GPLX-01', status: 'ACTIVE' }]
        }
      })
      const wrapper = mount(FleetAndDriversView, {
        global: {
          stubs: {
            Dialog: true,
            AlertDialog: true
          }
        }
      })
      await flushPromises()

      expect(wrapper.text()).toContain('Quản Trị Đội Xe & Danh Sách Tài Xế')
      expect(wrapper.text()).toContain('29B-888.99')

      wrapper.vm.activeTab = 'DRIVERS'
      await flushPromises()

      expect(wrapper.text()).toContain('Trần Văn Đức')
      expect(wrapper.text()).toContain('0987654321')
    })

    it('opens vehicle modal on click of add vehicle button', async () => {
      vi.spyOn(partnerApi, 'getVehicles').mockResolvedValue({ data: { code: 'OK', data: [] } })
      vi.spyOn(partnerApi, 'getDrivers').mockResolvedValue({ data: { code: 'OK', data: [] } })
      const wrapper = mount(FleetAndDriversView)
      await flushPromises()

      const addBtn = wrapper.find('[data-testid="btn-add-vehicle"]')
      await addBtn.trigger('click')
      await flushPromises()

      expect(wrapper.text()).toContain('Thêm Phương Tiện Mới')
      expect(wrapper.text()).toContain('Biển số xe đăng ký')
    })
  })

  describe('OperatorTripsView.vue - Turnaround Buffer 30-min Invariant', () => {
    it('detects turnaround buffer violation when interval < 30 mins', async () => {
      vi.spyOn(partnerApi, 'getVehicles').mockResolvedValue({
        data: {
          code: 'OK',
          data: [{ id: 1, plateNumber: '29B-888.99', modelName: 'Thaco', totalSeats: 36, status: 'ACTIVE' }]
        }
      })
      const wrapper = mount(OperatorTripsView)
      await flushPromises()

      wrapper.vm.sessionTrips = [
        {
          id: 'TRIP-PREV',
          tripCode: 'HN-DN-01',
          vehicleId: 1,
          departureStationId: 1,
          arrivalStationId: 2,
          departureTime: '2026-11-24T05:00:00',
          arrivalTime: '2026-11-24T09:00:00',
          basePrice: 350000,
          status: 'SCHEDULED'
        }
      ]

      const openScheduleBtn = wrapper.find('[data-testid="btn-open-schedule"]')
      await openScheduleBtn.trigger('click')
      await flushPromises()

      expect(wrapper.text()).toContain('Lập Lịch Chuyến Xe Mới')

      // Violation interval < 30m
      wrapper.vm.tripForm.vehicleId = '1'
      wrapper.vm.tripForm.departureTime = '2026-11-24T09:15:00'
      await flushPromises()

      expect(wrapper.vm.turnaroundBufferViolation).not.toBeNull()
      expect(wrapper.vm.turnaroundBufferViolation).toContain('tối thiểu 30 phút')

      // Compliant interval >= 30m
      wrapper.vm.tripForm.departureTime = '2026-11-24T09:35:00'
      await flushPromises()
      expect(wrapper.vm.turnaroundBufferViolation).toBeNull()
    })
  })
})
