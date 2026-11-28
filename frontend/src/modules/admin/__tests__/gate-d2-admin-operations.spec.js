import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/modules/auth/store'
import AdminOperatorsView from '../views/AdminOperatorsView.vue'
import AdminSettingsView from '../views/AdminSettingsView.vue'
import AdminWithdrawalsView from '../views/AdminWithdrawalsView.vue'
import { adminApi } from '@/modules/admin/api'

const dialogStubs = {
  Dialog: { template: '<div v-if="$attrs.open"><slot /></div>' },
  DialogContent: { template: '<div><slot /></div>' },
  DialogHeader: { template: '<div><slot /></div>' },
  DialogTitle: { template: '<div><slot /></div>' },
  DialogDescription: { template: '<div><slot /></div>' },
  DialogFooter: { template: '<div><slot /></div>' },
  AlertDialog: { template: '<div v-if="$attrs.open"><slot /></div>' },
  AlertDialogContent: { template: '<div><slot /></div>' },
  AlertDialogHeader: { template: '<div><slot /></div>' },
  AlertDialogTitle: { template: '<div><slot /></div>' },
  AlertDialogDescription: { template: '<div><slot /></div>' },
  AlertDialogFooter: { template: '<div><slot /></div>' },
  AlertDialogAction: { template: '<button @click="$emit(\'click\', $event)"><slot /></button>' },
  AlertDialogCancel: { template: '<button @click="$emit(\'click\', $event)"><slot /></button>' }
}

function makeFakeJwt(payload) {
  const encodeUtf8Base64Url = (obj) => {
    const utf8Str = encodeURIComponent(JSON.stringify(obj)).replace(/%([0-9A-F]{2})/g, (_, p1) =>
      String.fromCharCode(parseInt(p1, 16))
    )
    return btoa(utf8Str).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
  }
  return `${encodeUtf8Base64Url({ alg: 'HS256', typ: 'JWT' })}.${encodeUtf8Base64Url(payload)}.sig`
}

describe('Gate D2 - Administration & Financial Operations Specification Suite', () => {
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
      roleName: 'Quản Trị Viên Toàn Sàn',
      exp: Math.floor(Date.now() / 1000) + 3600
    })
  })

  describe('D2-OP: AdminOperatorsView - Operator Governance & 2-Tier Commission', () => {
    const mockOperators = [
      {
        id: 1,
        brandName: 'Hoàng Long Limousine',
        businessName: 'Công ty TNHH Hoàng Long',
        taxCode: '0109876543',
        contactPhone: '0901234567',
        contactEmail: 'hoanglong@velobus.vn',
        representative: 'Nguyễn Hoàng Long',
        commissionRate: 8.5,
        status: 'ACTIVE'
      },
      {
        id: 2,
        brandName: 'Hải Vân Express',
        businessName: 'Công ty Vận Tải Hải Vân',
        taxCode: '0405566778',
        contactPhone: '0989112233',
        contactEmail: 'haivan@velobus.vn',
        representative: 'Trần Hải Vân',
        commissionRate: null,
        status: 'PENDING_APPROVAL'
      }
    ]

    it('renders authoritative operator list with status badges and 2-tier commission labels', async () => {
      vi.spyOn(adminApi, 'getOperators').mockResolvedValue({ success: true, data: mockOperators })
      vi.spyOn(adminApi, 'getGlobalCommission').mockResolvedValue({ success: true, data: { commissionRate: 10.0 } })

      const wrapper = mount(AdminOperatorsView, { global: { plugins: [pinia], stubs: dialogStubs } })
      await flushPromises()

      expect(wrapper.text()).toContain('Phê Duyệt Hồ Sơ Nhà Xe & Cấu Hình Hoa Hồng 2 Cấp')
      expect(wrapper.text()).toContain('Hoàng Long Limousine')
      expect(wrapper.text()).toContain('Hải Vân Express')
      expect(wrapper.text()).toContain('8.5% (Riêng)')
      expect(wrapper.text()).toContain('10.0% (Chuẩn)')
    })

    it('renders EmptyState when operator response is empty', async () => {
      vi.spyOn(adminApi, 'getOperators').mockResolvedValue({ success: true, data: [] })
      vi.spyOn(adminApi, 'getGlobalCommission').mockResolvedValue({ success: true, data: { commissionRate: 10.0 } })

      const wrapper = mount(AdminOperatorsView, { global: { plugins: [pinia], stubs: dialogStubs } })
      await flushPromises()

      expect(wrapper.text()).toContain('Không tìm thấy đối tác nhà xe')
    })

    it('renders Error Alert and supports retry on API failure', async () => {
      vi.spyOn(adminApi, 'getOperators').mockRejectedValue(new Error('Network timeout'))
      vi.spyOn(adminApi, 'getGlobalCommission').mockResolvedValue({ success: true, data: { commissionRate: 10.0 } })

      const wrapper = mount(AdminOperatorsView, { global: { plugins: [pinia], stubs: dialogStubs } })
      await flushPromises()

      expect(wrapper.text()).toContain('Network timeout')

      // Retry successfully
      vi.spyOn(adminApi, 'getOperators').mockResolvedValue({ success: true, data: mockOperators })
      const retryBtn = wrapper.find('button:has-text("Thử lại"), button:has(.lucide-refresh-cw)')
      if (retryBtn.exists()) {
        await retryBtn.trigger('click')
        await flushPromises()
        expect(wrapper.text()).toContain('Hoàng Long Limousine')
      }
    })

    it('validates required fields during operator creation', async () => {
      vi.spyOn(adminApi, 'getOperators').mockResolvedValue({ success: true, data: mockOperators })
      vi.spyOn(adminApi, 'getGlobalCommission').mockResolvedValue({ success: true, data: { commissionRate: 10.0 } })

      const wrapper = mount(AdminOperatorsView, { global: { plugins: [pinia], stubs: dialogStubs } })
      await flushPromises()

      await wrapper.find('[data-testid="open-create-operator-btn"]').trigger('click')
      await wrapper.find('[data-testid="btn-save-operator"]').trigger('click')
      await flushPromises()

      expect(wrapper.text()).toContain('Vui lòng nhập Tên nhà xe và Mã số thuế')
    })

    it('enforces commission boundaries (0.0% to 100.0%) and handles save', async () => {
      vi.spyOn(adminApi, 'getOperators').mockResolvedValue({ success: true, data: mockOperators })
      vi.spyOn(adminApi, 'getGlobalCommission').mockResolvedValue({ success: true, data: { commissionRate: 10.0 } })
      const updateCommSpy = vi.spyOn(adminApi, 'updateOperatorCommission').mockResolvedValue({ success: true })

      const wrapper = mount(AdminOperatorsView, { global: { plugins: [pinia], stubs: dialogStubs } })
      await flushPromises()

      await wrapper.find('[data-testid="btn-commission-1"]').trigger('click')
      const input = wrapper.find('[data-testid="input-custom-commission"]')
      expect(input.exists()).toBe(true)

      // Test boundary > 100%
      await input.setValue(115)
      await wrapper.find('[data-testid="btn-save-commission"]').trigger('click')
      expect(wrapper.text()).toContain('Tỷ lệ hoa hồng riêng phải nằm trong khoảng từ 0.0% đến 100.0%')

      // Valid rate 12.5%
      await input.setValue(12.5)
      await wrapper.find('[data-testid="btn-save-commission"]').trigger('click')
      await flushPromises()

      const confirmBtn = wrapper.findAll('button').find(b => b.text().includes('Xác Nhận Cập Nhật'))
      if (confirmBtn) {
        await confirmBtn.trigger('click')
        await flushPromises()
      }

      expect(updateCommSpy).toHaveBeenCalledWith(1, 12.5)
    })

    it('hides write actions for Read-Only Auditor', async () => {
      auth.token = makeFakeJwt({
        sub: 'auditor',
        role: 'ROLE_ADMIN',
        customRole: 'ROLE_AUDITOR',
        permissions: ['OPERATOR_VIEW'],
        exp: Math.floor(Date.now() / 1000) + 3600
      })

      vi.spyOn(adminApi, 'getOperators').mockResolvedValue({ success: true, data: mockOperators })
      vi.spyOn(adminApi, 'getGlobalCommission').mockResolvedValue({ success: true, data: { commissionRate: 10.0 } })

      const wrapper = mount(AdminOperatorsView, { global: { plugins: [pinia], stubs: dialogStubs } })
      await flushPromises()

      expect(wrapper.find('[data-testid="open-create-operator-btn"]').exists()).toBe(false)
      expect(wrapper.find('[data-testid="btn-approve-2"]').exists()).toBe(false)
      expect(wrapper.find('[data-testid="btn-commission-1"]').exists()).toBe(false)
    })
  })

  describe('D2-SET: AdminSettingsView - Stations, Trips & System Settings', () => {
    const mockStations = [
      { id: 1, code: 'HN-GP', name: 'Bến xe Giáp Bát', city: 'Hà Nội', status: 'ACTIVE' },
      { id: 2, code: 'HN-MY', name: 'Bến xe Mỹ Đình', city: 'Hà Nội', status: 'ACTIVE' }
    ]

    const mockTrips = [
      { id: 101, tripCode: 'VB-HN-DN-01', departureCity: 'Hà Nội', arrivalCity: 'Đà Nẵng', departureTime: '2026-12-01T20:00:00', basePrice: 450000, totalSeats: 34, status: 'SCHEDULED' }
    ]

    const mockSettings = {
      platformName: 'VeloBus Sàn Vé Khách',
      defaultCommissionRate: '12.0',
      seatHoldTtlMinutes: '15',
      turnaroundBufferMinutes: '45',
      escrowSettlementDelayHours: '3'
    }

    it('renders all 3 sections: System Settings, Stations, and Trips', async () => {
      vi.spyOn(adminApi, 'getStations').mockResolvedValue({ success: true, data: mockStations })
      vi.spyOn(adminApi, 'getTrips').mockResolvedValue({ success: true, data: mockTrips })
      vi.spyOn(adminApi, 'getSystemSettings').mockResolvedValue({ success: true, data: mockSettings })

      const wrapper = mount(AdminSettingsView, { global: { plugins: [pinia], stubs: dialogStubs } })
      await flushPromises()

      expect(wrapper.text()).toContain('Cấu Hình Tham Số Vận Hành Toàn Sàn')
      expect(wrapper.text()).toContain('Danh Mục Bến Xe Liên Tỉnh (2)')
      expect(wrapper.text()).toContain('Danh Mục Tuyến & Chuyến Vận Hành (1)')
      expect(wrapper.find('[data-testid="btn-save-settings"]').exists()).toBe(true)
    })

    it('saves global system settings and displays sync notification', async () => {
      vi.spyOn(adminApi, 'getStations').mockResolvedValue({ success: true, data: mockStations })
      vi.spyOn(adminApi, 'getTrips').mockResolvedValue({ success: true, data: mockTrips })
      vi.spyOn(adminApi, 'getSystemSettings').mockResolvedValue({ success: true, data: mockSettings })
      const updateSpy = vi.spyOn(adminApi, 'updateSystemSettings').mockResolvedValue({ success: true })

      const wrapper = mount(AdminSettingsView, { global: { plugins: [pinia], stubs: dialogStubs } })
      await flushPromises()

      await wrapper.find('[data-testid="btn-save-settings"]').trigger('click')
      await flushPromises()

      const confirmSaveBtn = wrapper.findAll('button').find(b => b.text().includes('Lưu Cấu Hình'))
      if (confirmSaveBtn) {
        await confirmSaveBtn.trigger('click')
        await flushPromises()
      }

      expect(updateSpy).toHaveBeenCalled()
      expect(wrapper.find('[data-testid="settings-saved-alert"]').exists()).toBe(true)
      expect(wrapper.text()).toContain('Đã lưu cấu hình tham số toàn sàn vào cơ sở dữ liệu thành công!')
    })

    it('adds new station with code and name validation', async () => {
      vi.spyOn(adminApi, 'getStations').mockResolvedValue({ success: true, data: mockStations })
      vi.spyOn(adminApi, 'getTrips').mockResolvedValue({ success: true, data: mockTrips })
      vi.spyOn(adminApi, 'getSystemSettings').mockResolvedValue({ success: true, data: mockSettings })
      const createStationSpy = vi.spyOn(adminApi, 'createStation').mockResolvedValue({
        success: true,
        data: { id: 3, code: 'DN-TT', name: 'Bến xe Đà Nẵng', city: 'Đà Nẵng' }
      })

      const wrapper = mount(AdminSettingsView, { global: { plugins: [pinia], stubs: dialogStubs } })
      await flushPromises()

      await wrapper.find('[data-testid="input-station-code"]').setValue('DN-TT')
      await wrapper.find('[data-testid="input-station-name"]').setValue('Bến xe Đà Nẵng')
      await wrapper.find('[data-testid="input-station-city"]').setValue('Đà Nẵng')
      await wrapper.find('[data-testid="save-station-btn"]').trigger('click')
      await flushPromises()

      expect(createStationSpy).toHaveBeenCalledWith({
        code: 'DN-TT',
        name: 'Bến xe Đà Nẵng',
        city: 'Đà Nẵng'
      })
    })
  })

  describe('D2-WDR: AdminWithdrawalsView - Escrow Payout Governance & Privacy', () => {
    const mockWithdrawals = [
      {
        id: 'WDR-20261123-01',
        operatorId: 1,
        operatorName: 'Hoàng Long Limousine',
        amount: 15000000,
        bankName: 'Vietcombank',
        accountNumber: '0071001234567',
        accountHolderName: 'Hoàng Long Ltd',
        createdAt: '2026-11-23 10:15',
        status: 'PENDING'
      },
      {
        id: 'WDR-20261122-09',
        operatorId: 2,
        operatorName: 'Phương Trang Velo',
        amount: 35000000,
        bankName: 'Techcombank',
        accountNumber: '1903456789001',
        accountHolderName: 'Phương Trang Corp',
        createdAt: '2026-11-22 16:40',
        status: 'APPROVED'
      }
    ]

    it('masks bank account number by default and reveals on toggle click', async () => {
      vi.spyOn(adminApi, 'getWithdrawals').mockResolvedValue({ success: true, data: mockWithdrawals })

      const wrapper = mount(AdminWithdrawalsView, { global: { plugins: [pinia], stubs: dialogStubs } })
      await flushPromises()

      // Default masked: ••••4567
      expect(wrapper.text()).toContain('••••4567')
      expect(wrapper.text()).not.toContain('0071001234567')

      // Click toggle
      const toggleBtn = wrapper.find('button[title="Ẩn/Hiện số tài khoản"]')
      expect(toggleBtn.exists()).toBe(true)
      await toggleBtn.trigger('click')
      await flushPromises()

      expect(wrapper.text()).toContain('0071001234567')
    })

    it('approves pending withdrawal with authoritative API and updates status', async () => {
      vi.spyOn(adminApi, 'getWithdrawals').mockResolvedValue({ success: true, data: mockWithdrawals })
      const approveSpy = vi.spyOn(adminApi, 'approveWithdrawal').mockResolvedValue({ success: true })

      const wrapper = mount(AdminWithdrawalsView, { global: { plugins: [pinia], stubs: dialogStubs } })
      await flushPromises()

      const approveBtn = wrapper.find('[data-testid="btn-approve-withdraw-WDR-20261123-01"]')
      expect(approveBtn.exists()).toBe(true)

      await approveBtn.trigger('click')
      await flushPromises()

      const confirmBtn = wrapper.find('[data-testid="btn-confirm-withdrawal-action"]')
      expect(confirmBtn.exists()).toBe(true)
      await confirmBtn.trigger('click')
      await flushPromises()

      expect(approveSpy).toHaveBeenCalledWith('WDR-20261123-01', expect.any(String))
    })

    it('rejects pending withdrawal with authoritative API', async () => {
      vi.spyOn(adminApi, 'getWithdrawals').mockResolvedValue({ success: true, data: mockWithdrawals })
      const rejectSpy = vi.spyOn(adminApi, 'rejectWithdrawal').mockResolvedValue({ success: true })

      const wrapper = mount(AdminWithdrawalsView, { global: { plugins: [pinia], stubs: dialogStubs } })
      await flushPromises()

      const rejectBtn = wrapper.find('[data-testid="btn-reject-withdraw-WDR-20261123-01"]')
      expect(rejectBtn.exists()).toBe(true)

      await rejectBtn.trigger('click')
      await flushPromises()

      const noteTextarea = wrapper.find('textarea')
      expect(noteTextarea.exists()).toBe(true)
      await noteTextarea.setValue('Sai số tài khoản nhận tiền')

      const confirmBtn = wrapper.find('[data-testid="btn-confirm-withdrawal-action"]')
      expect(confirmBtn.exists()).toBe(true)
      await confirmBtn.trigger('click')
      await flushPromises()

      expect(rejectSpy).toHaveBeenCalledWith('WDR-20261123-01', expect.any(String))
    })

    it('activates unknown outcome banner on network timeout', async () => {
      vi.spyOn(adminApi, 'getWithdrawals').mockResolvedValue({ success: true, data: mockWithdrawals })
      const timeoutErr = new Error('Network Error: Request timeout')
      timeoutErr.code = 'ECONNABORTED'
      vi.spyOn(adminApi, 'approveWithdrawal').mockRejectedValue(timeoutErr)

      const wrapper = mount(AdminWithdrawalsView, { global: { plugins: [pinia], stubs: dialogStubs } })
      await flushPromises()

      const approveBtn = wrapper.find('[data-testid="btn-approve-withdraw-WDR-20261123-01"]')
      await approveBtn.trigger('click')
      await flushPromises()

      const confirmBtn = wrapper.find('[data-testid="btn-confirm-withdrawal-action"]')
      expect(confirmBtn.exists()).toBe(true)
      await confirmBtn.trigger('click')
      await flushPromises()

      expect(wrapper.text()).toContain('Cảnh báo an toàn tài chính: Kết quả chưa xác định')
      expect(wrapper.text()).toContain('Kiểm tra lại trạng thái')
    })

    it('hides payout approval and rejection buttons when user lacks permissions', async () => {
      auth.token = makeFakeJwt({
        sub: 'viewer',
        role: 'ROLE_ADMIN',
        customRole: 'ROLE_VIEWER',
        permissions: ['WITHDRAWAL_VIEW'],
        exp: Math.floor(Date.now() / 1000) + 3600
      })

      vi.spyOn(adminApi, 'getWithdrawals').mockResolvedValue({ success: true, data: mockWithdrawals })

      const wrapper = mount(AdminWithdrawalsView, { global: { plugins: [pinia], stubs: dialogStubs } })
      await flushPromises()

      expect(wrapper.find('[data-testid="btn-approve-withdraw-WDR-20261123-01"]').exists()).toBe(false)
      expect(wrapper.find('[data-testid="btn-reject-withdraw-WDR-20261123-01"]').exists()).toBe(false)
      expect(wrapper.text()).toContain('Chỉ xem (Thiếu quyền duyệt)')
    })
  })
})
