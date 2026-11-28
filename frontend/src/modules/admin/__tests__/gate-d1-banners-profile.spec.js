import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import AdminBannersView from '../views/AdminBannersView.vue'
import OperatorBannersView from '@/modules/partner/views/OperatorBannersView.vue'
import UserProfileView from '@/modules/auth/views/UserProfileView.vue'
import FirebaseUploader from '@/shared/components/FirebaseUploader.vue'
import { useAuthStore } from '@/modules/auth/store'
import { adminApi } from '@/modules/admin/api'
import { partnerApi } from '@/modules/partner/api'
import { authApi } from '@/modules/auth/api'
import httpClient from '@/shared/api/httpClient'

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

vi.mock('@/shared/api/httpClient', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn()
  },
  setHttpClientPreviewRole: vi.fn(),
  getHttpClientPreviewRole: vi.fn()
}))

describe('Gate D1 Data Integrity & Contract Conformance Suite', () => {
  let pinia
  let auth

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    auth = useAuthStore()
    auth.token = makeFakeJwt({ sub: 'admin', role: 'ROLE_ADMIN' })
    vi.clearAllMocks()
  })

  describe('AdminBannersView.vue', () => {
    it('D1-PRE-01: renders EmptyState when API returns empty list without falling back to mock fixtures', async () => {
      adminApi.getBanners.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(AdminBannersView, {
        global: {
          plugins: [pinia],
          stubs: { FirebaseUploader: true }
        }
      })

      await flushPromises()
      expect(adminApi.getBanners).toHaveBeenCalled()
      expect(wrapper.text()).toContain('Không có banner nào')
      expect(wrapper.text()).not.toContain('Phương Trang Velo')
      expect(wrapper.text()).not.toContain('Hoàng Long Limousine')
    })

    it('D1-PRE-01: renders ErrorState and Retry on network or API failure', async () => {
      adminApi.getBanners.mockRejectedValueOnce(new Error('Network connection timeout'))

      const wrapper = mount(AdminBannersView, {
        global: {
          plugins: [pinia],
          stubs: { FirebaseUploader: true }
        }
      })

      await flushPromises()
      expect(wrapper.text()).toContain('Lỗi tải dữ liệu banner')
      expect(wrapper.text()).toContain('Network connection timeout')
      expect(wrapper.findAll('button').some(b => b.text().includes('Thử Lại'))).toBe(true)
    })

    it('D1-PRE-01: handles malformed response safely without fabricating records', async () => {
      adminApi.getBanners.mockResolvedValueOnce({ data: 'invalid_string' })

      const wrapper = mount(AdminBannersView, {
        global: {
          plugins: [pinia],
          stubs: { FirebaseUploader: true }
        }
      })

      await flushPromises()
      expect(wrapper.text()).toContain('Lỗi tải dữ liệu banner')
      expect(wrapper.text()).toContain('sai cấu trúc')
    })

    it('D1-PRE-03: renders exact BannerPosition and honest operator display', async () => {
      adminApi.getBanners.mockResolvedValueOnce({
        data: [
          {
            id: 101,
            operatorId: 45,
            title: 'Chiến dịch mùa hè',
            imageUrl: 'https://images.unsplash.com/test.jpg',
            targetUrl: 'https://velobus.vn/search',
            position: 'HOMEPAGE_HERO',
            status: 'ACTIVE',
            createdAt: '2026-11-25T08:00:00Z'
          },
          {
            id: 102,
            operatorId: null,
            title: 'Khuyến mãi sàn VeloBus',
            imageUrl: 'https://images.unsplash.com/test2.jpg',
            targetUrl: 'https://velobus.vn/deals',
            position: 'SEARCH_SIDEBAR',
            status: 'PENDING_APPROVAL',
            createdAt: '2026-11-25T09:00:00Z'
          }
        ]
      })

      const wrapper = mount(AdminBannersView, {
        global: {
          plugins: [pinia],
          stubs: { FirebaseUploader: true }
        }
      })

      await flushPromises()
      // Operator with ID displays honestly without fabricated company name
      expect(wrapper.text()).toContain('Nhà xe #45')
      expect(wrapper.text()).toContain('VeloBus Platform (Banner Sàn)')
      expect(wrapper.text()).toContain('Hero Carousel (Đầu trang chủ)')
      expect(wrapper.text()).toContain('Cột Bên Tìm Kiếm (Trang tìm vé)')
    })

    it('D1-PRE-02: create banner awaits API and refetches authoritative list on success', async () => {
      adminApi.getBanners.mockResolvedValueOnce({ data: [] })
      adminApi.createPlatformBanner.mockResolvedValueOnce({ data: { id: 201 } })

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
      await wrapper.find('[data-testid="create-banner-btn"]').trigger('click')

      await wrapper.find('[data-testid="input-banner-title"]').setValue('Khuyến mãi tết 2027')
      await wrapper.find('[data-testid="input-banner-image"]').setValue('https://cdn.velobus.vn/banner.png')
      await wrapper.find('[data-testid="input-banner-target"]').setValue('https://velobus.vn/search?from=HN')

      await wrapper.find('[data-testid="btn-save-banner"]').trigger('click')
      await flushPromises()

      expect(adminApi.createPlatformBanner).toHaveBeenCalledWith({
        title: 'Khuyến mãi tết 2027',
        imageUrl: 'https://cdn.velobus.vn/banner.png',
        targetUrl: 'https://velobus.vn/search?from=HN',
        position: 'HOMEPAGE_HERO',
        priority: 1
      })
      // Refetches list authoritatively
      expect(adminApi.getBanners).toHaveBeenCalledTimes(2)
      expect(wrapper.vm.isFormModalOpen).toBe(false)
    })

    it('D1-PRE-02: create banner retains modal and user inputs when backend rejects', async () => {
      adminApi.getBanners.mockResolvedValueOnce({ data: [] })
      adminApi.createPlatformBanner.mockRejectedValueOnce({
        response: { data: { message: 'Tiêu đề banner đã tồn tại trong khoảng thời gian này.' } }
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
            DialogFooter: { template: '<div><slot /></div>' }
          }
        }
      })

      await flushPromises()
      await wrapper.find('[data-testid="create-banner-btn"]').trigger('click')

      await wrapper.find('[data-testid="input-banner-title"]').setValue('Banner Trùng Lặp')
      await wrapper.find('[data-testid="input-banner-image"]').setValue('https://cdn.velobus.vn/dup.png')

      await wrapper.find('[data-testid="btn-save-banner"]').trigger('click')
      await flushPromises()

      expect(wrapper.vm.isFormModalOpen).toBe(true)
      expect(wrapper.text()).toContain('Tiêu đề banner đã tồn tại')
      expect(wrapper.vm.bannerForm.title).toBe('Banner Trùng Lặp')
    })
  })

  describe('OperatorBannersView.vue', () => {
    it('D1-PRE-01: renders empty state when operator has no banners registered', async () => {
      partnerApi.getMyBanners.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(OperatorBannersView, {
        global: {
          plugins: [pinia],
          stubs: { FirebaseUploader: true }
        }
      })

      await flushPromises()
      expect(partnerApi.getMyBanners).toHaveBeenCalled()
      expect(wrapper.text()).toContain('Chưa có banner quảng cáo nào')
    })

    it('D1-PRE-02: handles registration failure without fabricating created record', async () => {
      partnerApi.getMyBanners.mockResolvedValueOnce({ data: [] })
      partnerApi.createBanner.mockRejectedValueOnce(new Error('Hạn mức đăng ký banner của nhà xe đã đầy.'))

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
      await wrapper.find('[data-testid="btn-add-banner"]').trigger('click')

      await wrapper.find('[data-testid="input-banner-title"]').setValue('Khuyến Mãi Quá Tải')
      await wrapper.find('[data-testid="input-banner-image"]').setValue('https://cdn.velobus.vn/banner.jpg')
      await wrapper.find('[data-testid="input-banner-target"]').setValue('https://velobus.vn/trips/1')

      await wrapper.find('[data-testid="btn-submit-banner"]').trigger('click')
      await flushPromises()

      expect(partnerApi.createBanner).toHaveBeenCalled()
      expect(wrapper.vm.isSheetOpen).toBe(true)
      expect(wrapper.text()).toContain('Hạn mức đăng ký banner của nhà xe đã đầy.')
      // No record was fabricated into banners list
      expect(wrapper.vm.banners.length).toBe(0)
    })
  })

  describe('FirebaseUploader.vue', () => {
    it('D1-PRE-04: rejects unsupported file types before sending network request', async () => {
      const wrapper = mount(FirebaseUploader)

      const badFile = new File(['fake content'], 'document.pdf', { type: 'application/pdf' })
      const input = wrapper.find('input[type="file"]')

      Object.defineProperty(input.element, 'files', {
        value: [badFile],
        writable: true
      })
      await input.trigger('change')

      expect(httpClient.post).not.toHaveBeenCalled()
      expect(wrapper.text()).toContain('Chỉ chấp nhận các định dạng tệp ảnh JPEG, PNG, WebP hoặc GIF')
    })

    it('D1-PRE-04: rejects file exceeding 10MB limit', async () => {
      const wrapper = mount(FirebaseUploader)

      const hugeFile = new File([''], 'huge.png', { type: 'image/png' })
      Object.defineProperty(hugeFile, 'size', { value: 11 * 1024 * 1024 })

      const input = wrapper.find('input[type="file"]')
      Object.defineProperty(input.element, 'files', {
        value: [hugeFile],
        writable: true
      })
      await input.trigger('change')

      expect(httpClient.post).not.toHaveBeenCalled()
      expect(wrapper.text()).toContain('vượt quá giới hạn tối đa cho phép (10MB)')
    })

    it('D1-PRE-04: validates canonical response URL and emits uploaded', async () => {
      httpClient.post.mockResolvedValueOnce({
        data: {
          data: {
            url: 'https://storage.googleapis.com/velobus/banners/valid.jpg',
            filename: 'valid.jpg'
          }
        }
      })

      const wrapper = mount(FirebaseUploader)

      const validFile = new File(['image-bytes'], 'valid.jpg', { type: 'image/jpeg' })
      const input = wrapper.find('input[type="file"]')
      Object.defineProperty(input.element, 'files', {
        value: [validFile],
        writable: true
      })
      await input.trigger('change')
      await flushPromises()

      expect(httpClient.post).toHaveBeenCalledWith(
        '/public/upload',
        expect.any(FormData),
        expect.objectContaining({ headers: { 'Content-Type': 'multipart/form-data' } })
      )
      expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['https://storage.googleapis.com/velobus/banners/valid.jpg'])
      expect(wrapper.emitted('uploaded')?.[0]).toEqual(['https://storage.googleapis.com/velobus/banners/valid.jpg'])
    })

    it('D1-PRE-04: handles image load error with accessible fallback', async () => {
      const wrapper = mount(FirebaseUploader, {
        props: {
          modelValue: 'https://broken-url.com/non-existent.png'
        }
      })

      const img = wrapper.find('img[data-testid="img-uploader-preview"]')
      expect(img.exists()).toBe(true)

      await img.trigger('error')
      await wrapper.vm.$nextTick()

      expect(wrapper.text()).toContain('Không thể hiển thị hình ảnh')
      expect(wrapper.text()).toContain('URL không tồn tại, sai định dạng hoặc bị chặn truy cập')
    })
  })

  describe('UserProfileView.vue', () => {
    it('D1-PRE-05: extracts actual authenticated identity from AuthResponse without fabricating personal data', async () => {
      authApi.getMe.mockResolvedValueOnce({
        data: {
          username: 'operator_admin_01',
          role: 'ROLE_OPERATOR',
          customRole: 'CHIEF_DISPATCHER',
          roleName: 'Trưởng Điều Hành Tuyến',
          permissions: ['TRIP_VIEW', 'TRIP_CREATE', 'TRIP_UPDATE']
        }
      })

      const wrapper = mount(UserProfileView, {
        global: {
          plugins: [pinia],
          stubs: { TelegramLinkModal: true }
        }
      })

      await flushPromises()

      expect(wrapper.text()).toContain('@operator_admin_01')
      expect(wrapper.text()).toContain('ROLE_OPERATOR')
      expect(wrapper.text()).toContain('Trưởng Điều Hành Tuyến')
      expect(wrapper.text()).toContain('TRIP_VIEW')
      // Honest unavailable fields
      expect(wrapper.text()).toContain('Chưa liên kết trong phiên')
      // No misleading fake password form
      expect(wrapper.find('input[type="password"]').exists()).toBe(false)
    })
  })
})
