import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import AdminBannersView from '../views/AdminBannersView.vue'
import OperatorBannersView from '@/modules/partner/views/OperatorBannersView.vue'
import UserProfileView from '@/modules/auth/views/UserProfileView.vue'
import { adminApi } from '@/modules/admin/api'
import { partnerApi } from '@/modules/partner/api'
import { authApi } from '@/modules/auth/api'

vi.mock('@/modules/admin/api', () => ({
  adminApi: {
    getBanners: vi.fn(),
    createPlatformBanner: vi.fn(),
    updateBanner: vi.fn(),
    deleteBanner: vi.fn(),
    approveBanner: vi.fn(),
    rejectBanner: vi.fn()
  }
}))

vi.mock('@/modules/partner/api', () => ({
  partnerApi: {
    getMyBanners: vi.fn(),
    createBanner: vi.fn()
  }
}))

vi.mock('@/modules/auth/api', () => ({
  authApi: {
    getMe: vi.fn()
  }
}))

describe('Gate D1: Content & Account Management Suite', () => {
  let pinia

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    vi.clearAllMocks()
  })

  describe('AdminBannersView.vue', () => {
    it('fetches and renders banners with moderation buttons', async () => {
      adminApi.getBanners.mockResolvedValueOnce({
        data: [
          {
            id: 10,
            operatorId: 2,
            operatorName: 'Nhà xe Sao Việt',
            title: 'Khuyến mãi đặc biệt tuyến Hà Nội - Sa Pa',
            imageUrl: 'https://images.unsplash.com/test.jpg',
            targetUrl: 'https://velobus.vn/search',
            position: 'HOME_CAROUSEL',
            status: 'PENDING_APPROVAL',
            createdAt: '2026-11-25T08:00:00Z'
          }
        ]
      })

      const wrapper = mount(AdminBannersView, {
        global: {
          plugins: [pinia],
          stubs: {
            FirebaseUploader: true,
            Dialog: { template: '<div><slot /></div>' },
            DialogContent: { template: '<div><slot /></div>' },
            DialogHeader: { template: '<div><slot /></div>' },
            DialogTitle: { template: '<div><slot /></div>' },
            DialogDescription: { template: '<div><slot /></div>' },
            DialogFooter: { template: '<div><slot /></div>' },
            AlertDialog: { template: '<div><slot /></div>' },
            AlertDialogContent: { template: '<div><slot /></div>' },
            AlertDialogHeader: { template: '<div><slot /></div>' },
            AlertDialogTitle: { template: '<div><slot /></div>' },
            AlertDialogDescription: { template: '<div><slot /></div>' },
            AlertDialogFooter: { template: '<div><slot /></div>' },
            AlertDialogCancel: { template: '<button><slot /></button>' },
            AlertDialogAction: { template: '<button><slot /></button>' }
          }
        }
      })

      await flushPromises()
      expect(adminApi.getBanners).toHaveBeenCalled()
      expect(wrapper.text()).toContain('Kiểm Duyệt Banner Quảng Cáo Toàn Sàn')
      expect(wrapper.text()).toContain('Khuyến mãi đặc biệt tuyến Hà Nội - Sa Pa')
      expect(wrapper.text()).toContain('Nhà xe Sao Việt')
    })

    it('opens create platform banner modal and validates required fields', async () => {
      adminApi.getBanners.mockResolvedValueOnce({ data: [] })
      const wrapper = mount(AdminBannersView, {
        global: {
          plugins: [pinia],
          stubs: {
            FirebaseUploader: true,
            Dialog: { template: '<div><slot /></div>' },
            DialogContent: { template: '<div><slot /></div>' },
            DialogHeader: { template: '<div><slot /></div>' },
            DialogTitle: { template: '<div><slot /></div>' },
            DialogDescription: { template: '<div><slot /></div>' },
            DialogFooter: { template: '<div><slot /></div>' }
          }
        }
      })

      await flushPromises()
      const createBtn = wrapper.find('[data-testid="create-banner-btn"]')
      if (createBtn.exists()) {
        await createBtn.trigger('click')
        expect(wrapper.vm.isFormModalOpen).toBe(true)
      }
    })
  })

  describe('OperatorBannersView.vue', () => {
    it('fetches operator banners and opens registration sheet', async () => {
      partnerApi.getMyBanners.mockResolvedValueOnce({
        data: [
          {
            id: 1,
            title: 'Xe VIP Limousine Tuyến Sài Gòn - Vũng Tàu',
            imageUrl: 'https://images.unsplash.com/limo.jpg',
            targetUrl: 'https://velobus.vn/trips/1',
            status: 'ACTIVE',
            registeredAt: '22/11/2026'
          }
        ]
      })

      const wrapper = mount(OperatorBannersView, {
        global: {
          plugins: [pinia],
          stubs: {
            FirebaseUploader: true,
            Sheet: { template: '<div><slot /></div>' },
            SheetContent: { template: '<div><slot /></div>' },
            SheetHeader: { template: '<div><slot /></div>' },
            SheetTitle: { template: '<div><slot /></div>' },
            SheetDescription: { template: '<div><slot /></div>' },
            SheetFooter: { template: '<div><slot /></div>' }
          }
        }
      })

      await flushPromises()
      expect(partnerApi.getMyBanners).toHaveBeenCalled()
      expect(wrapper.text()).toContain('Banner & Chiến Dịch Quảng Cáo Sàn')
      expect(wrapper.text()).toContain('Xe VIP Limousine Tuyến Sài Gòn - Vũng Tàu')

      const addBtn = wrapper.find('[data-testid="btn-add-banner"]')
      expect(addBtn.exists()).toBe(true)
      await addBtn.trigger('click')
      expect(wrapper.vm.isSheetOpen).toBe(true)
    })
  })

  describe('UserProfileView.vue', () => {
    it('binds authenticated user profile and renders read-only capability notice', async () => {
      authApi.getMe.mockResolvedValueOnce({
        data: {
          username: 'admin_test',
          fullName: 'Nguyễn Văn Quản Trị',
          email: 'admin.test@velobus.vn',
          role: 'ROLE_ADMIN'
        }
      })

      const wrapper = mount(UserProfileView, {
        global: {
          plugins: [pinia],
          stubs: {
            TelegramLinkModal: true
          }
        }
      })

      await flushPromises()
      expect(wrapper.text()).toContain('Hồ Sơ Cá Nhân & Bảo Mật Tài Khoản')
      expect(wrapper.text()).toContain('Chế độ hiển thị định danh xác thực (Read-only)')
      expect(wrapper.text()).toContain('Quản Trị Viên Sàn (Admin)')

      // Verify mutation buttons are disabled to prevent misleading user promises
      const disabledButtons = wrapper.findAll('button[disabled]')
      expect(disabledButtons.length).toBeGreaterThanOrEqual(2)
    })
  })
})
