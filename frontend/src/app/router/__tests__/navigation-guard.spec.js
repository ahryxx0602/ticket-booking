import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import router from '@/app/router/index.js'
import { useAuthStore } from '@/modules/auth/store'
import { resolveSafeRedirect } from '@/shared/utils/redirect'
import ForbiddenView from '@/shared/views/ForbiddenView.vue'
import UserProfileView from '@/modules/auth/views/UserProfileView.vue'

const storageMap = new Map()
const localStorageMock = {
  getItem: (key) => (storageMap.has(key) ? storageMap.get(key) : null),
  setItem: (key, val) => storageMap.set(key, String(val)),
  removeItem: (key) => storageMap.delete(key),
  clear: () => storageMap.clear(),
}

Object.defineProperty(globalThis, 'localStorage', {
  value: localStorageMock,
  writable: true,
})

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
  const header = encodeUtf8Base64Url({ alg: 'HS256', typ: 'JWT' })
  const body = encodeUtf8Base64Url(payload)
  return `${header}.${body}.mock_signature`
}

describe('Vue Router Navigation Guard & RBAC Suite (Phase FE-06)', () => {
  let authStore

  beforeEach(async () => {
    localStorage.clear()
    setActivePinia(createPinia())
    authStore = useAuthStore()
    await router.push('/')
  })

  describe('Kịch bản 1: Khách vãng lai (Guest) chưa đăng nhập', () => {
    it('cho phép truy cập tự do các route công khai (Home, Search, Seats, 403, 404)', async () => {
      await router.push('/')
      expect(router.currentRoute.value.path).toBe('/')

      await router.push('/search')
      expect(router.currentRoute.value.path).toBe('/search')

      await router.push('/403')
      expect(router.currentRoute.value.name).toBe('forbidden')
    })

    it('chặn truy cập Portal Quản trị (/admin/**) và điều hướng sang /login kèm redirect query', async () => {
      await router.push('/admin')
      expect(router.currentRoute.value.name).toBe('auth-login')
      expect(router.currentRoute.value.query.redirect).toBe('/admin')

      await router.push('/admin/operators')
      expect(router.currentRoute.value.name).toBe('auth-login')
      expect(router.currentRoute.value.query.redirect).toBe('/admin/operators')
    })

    it('chặn truy cập Portal Nhà xe (/operator/**) và điều hướng sang /login kèm redirect query', async () => {
      await router.push('/operator')
      expect(router.currentRoute.value.name).toBe('auth-login')
      expect(router.currentRoute.value.query.redirect).toBe('/operator')

      await router.push('/operator/fleet')
      expect(router.currentRoute.value.name).toBe('auth-login')
      expect(router.currentRoute.value.query.redirect).toBe('/operator/fleet')
    })

    it('chặn truy cập các trang khách hàng yêu cầu xác thực (/my-bookings, /checkout/123)', async () => {
      await router.push('/my-bookings')
      expect(router.currentRoute.value.name).toBe('auth-login')
      expect(router.currentRoute.value.query.redirect).toBe('/my-bookings')

      await router.push('/checkout/BKG-TEST-01')
      expect(router.currentRoute.value.name).toBe('auth-login')
      expect(router.currentRoute.value.query.redirect).toBe('/checkout/BKG-TEST-01')
    })

    it('chống Route Enumeration: Guest truy cập route nội bộ không tồn tại (/admin/secret-xyz) bị redirect /login chứ không lộ 404', async () => {
      await router.push('/admin/secret-xyz')
      expect(router.currentRoute.value.name).toBe('auth-login')
      expect(router.currentRoute.value.query.redirect).toBe('/admin/secret-xyz')

      await router.push('/ADMIN/secret-uppercase')
      expect(router.currentRoute.value.name).toBe('auth-login')
      expect(router.currentRoute.value.query.redirect).toBe('/ADMIN/secret-uppercase')

      await router.push('/operator/nonexistent')
      expect(router.currentRoute.value.name).toBe('auth-login')
      expect(router.currentRoute.value.query.redirect).toBe('/operator/nonexistent')

      await router.push('/OPERATOR/nonexistent-upper')
      expect(router.currentRoute.value.name).toBe('auth-login')
      expect(router.currentRoute.value.query.redirect).toBe('/OPERATOR/nonexistent-upper')
    })
  })

  describe('Kịch bản 2: Phân quyền vai trò Khách hàng (ROLE_CUSTOMER)', () => {
    beforeEach(() => {
      const token = makeFakeJwt({
        sub: 'customer1',
        role: 'ROLE_CUSTOMER',
        exp: Math.floor(Date.now() / 1000) + 3600,
      })
      authStore.token = token
      authStore.user = { username: 'customer1', role: 'ROLE_CUSTOMER' }
    })

    it('cho phép Customer truy cập trang cá nhân, lịch sử vé và checkout', async () => {
      await router.push('/my-bookings')
      expect(router.currentRoute.value.path).toBe('/my-bookings')

      await router.push('/checkout/BKG-TEST-01')
      expect(router.currentRoute.value.path).toBe('/checkout/BKG-TEST-01')

      await router.push('/profile')
      expect(router.currentRoute.value.path).toBe('/profile')
    })

    it('CHẶN Customer truy cập /admin và chuyển hướng sang /403', async () => {
      await router.push('/admin')
      expect(router.currentRoute.value.name).toBe('forbidden')
      expect(router.currentRoute.value.path).toBe('/403')
    })

    it('CHẶN Customer truy cập /admin nội bộ không tồn tại và chuyển hướng sang /403 (chống enumeration)', async () => {
      await router.push('/admin/unknown-panel')
      expect(router.currentRoute.value.name).toBe('forbidden')
      expect(router.currentRoute.value.path).toBe('/403')
    })

    it('CHẶN Customer truy cập /operator và chuyển hướng sang /403', async () => {
      await router.push('/operator')
      expect(router.currentRoute.value.name).toBe('forbidden')
      expect(router.currentRoute.value.path).toBe('/403')
    })

    it('tự động chuyển hướng về trang chủ (/) nếu Customer cố vào lại trang /login', async () => {
      await router.push('/login')
      expect(router.currentRoute.value.path).toBe('/')
    })

    it('chống Bypass bằng sửa đổi localStorage: sửa velobus_user thành ROLE_ADMIN không làm tăng quyền', async () => {
      localStorage.setItem('velobus_user', JSON.stringify({ username: 'customer1', role: 'ROLE_ADMIN' }))
      authStore.user = { username: 'customer1', role: 'ROLE_ADMIN' }

      // Role phải luôn lấy từ JWT payload (Single Source of Truth)
      expect(authStore.role).toBe('ROLE_CUSTOMER')
      expect(authStore.isAdmin).toBe(false)

      await router.push('/admin')
      expect(router.currentRoute.value.name).toBe('forbidden')
    })
  })

  describe('Kịch bản 3: Phân quyền vai trò Đối tác Nhà xe (ROLE_OPERATOR)', () => {
    beforeEach(() => {
      const token = makeFakeJwt({
        sub: 'operator1',
        role: 'ROLE_OPERATOR',
        exp: Math.floor(Date.now() / 1000) + 3600,
      })
      authStore.token = token
      authStore.user = { username: 'operator1', role: 'ROLE_OPERATOR' }
    })

    it('cho phép Operator truy cập các phân hệ trong /operator/**', async () => {
      await router.push('/operator')
      expect(router.currentRoute.value.path).toBe('/operator')

      await router.push('/operator/fleet')
      expect(router.currentRoute.value.path).toBe('/operator/fleet')
    })

    it('CHẶN Operator truy cập /admin và chuyển hướng sang /403', async () => {
      await router.push('/admin')
      expect(router.currentRoute.value.name).toBe('forbidden')
    })

    it('CHẶN Operator truy cập /my-bookings của khách hàng và chuyển hướng sang /403', async () => {
      await router.push('/my-bookings')
      expect(router.currentRoute.value.name).toBe('forbidden')
    })

    it('CHẶN Operator truy cập /checkout/:id và chuyển hướng sang /403', async () => {
      await router.push('/checkout/BKG-TEST-01')
      expect(router.currentRoute.value.name).toBe('forbidden')
    })

    it('tự động chuyển hướng về /operator nếu Operator cố vào lại trang /login', async () => {
      await router.push('/login')
      expect(router.currentRoute.value.path).toBe('/operator')
    })
  })

  describe('Kịch bản 4: Phân quyền vai trò Quản trị viên (ROLE_ADMIN)', () => {
    beforeEach(() => {
      const token = makeFakeJwt({
        sub: 'admin',
        role: 'ROLE_ADMIN',
        exp: Math.floor(Date.now() / 1000) + 3600,
      })
      authStore.token = token
      authStore.user = { username: 'admin', role: 'ROLE_ADMIN' }
    })

    it('cho phép Admin truy cập toàn quyền trong /admin/**', async () => {
      await router.push('/admin')
      expect(router.currentRoute.value.path).toBe('/admin')

      await router.push('/admin/operators')
      expect(router.currentRoute.value.path).toBe('/admin/operators')
    })

    it('CHẶN Admin can thiệp vào /operator workspace và chuyển hướng sang /403', async () => {
      await router.push('/operator')
      expect(router.currentRoute.value.name).toBe('forbidden')
    })

    it('CHẶN Admin truy cập /my-bookings khách hàng và chuyển hướng sang /403', async () => {
      await router.push('/my-bookings')
      expect(router.currentRoute.value.name).toBe('forbidden')
    })

    it('tự động chuyển hướng về /admin nếu Admin cố vào lại trang /login', async () => {
      await router.push('/login')
      expect(router.currentRoute.value.path).toBe('/admin')
    })
  })

  describe('Kịch bản 4B: Phân quyền Động theo Ma trận 7 Modules x 26 APIs (Phase 07 Custom System Roles)', () => {
    it('nhân sự mang ROLE_ACCOUNTANT được vào /admin và /admin/withdrawals nhưng bị chặn /403 khi vào /admin/banners hoặc /admin/settings', async () => {
      const token = makeFakeJwt({
        sub: 'ketoan01',
        role: 'ROLE_ADMIN',
        customRole: 'ROLE_ACCOUNTANT',
        roleName: 'Kế Toán & Đối Soát',
        permissions: [
          'PAYMENT_REPORT_VIEW',
          'PAYMENT_LIST_VIEW',
          'WITHDRAWAL_VIEW',
          'WITHDRAWAL_APPROVE'
        ],
        exp: Math.floor(Date.now() / 1000) + 3600,
      })
      authStore.token = token

      expect(authStore.isAdmin).toBe(true)
      expect(authStore.isSuperAdmin).toBe(false)
      expect(authStore.customRole).toBe('ROLE_ACCOUNTANT')

      await router.push('/admin')
      expect(router.currentRoute.value.path).toBe('/admin')

      await router.push('/admin/withdrawals')
      expect(router.currentRoute.value.path).toBe('/admin/withdrawals')

      // Không có quyền BANNER_VIEW -> bị chặn sang /403
      await router.push('/admin/banners')
      expect(router.currentRoute.value.name).toBe('forbidden')

      // Không có quyền STATION_VIEW / SYSTEM_SETTINGS_MANAGE -> bị chặn sang /403
      await router.push('/admin/settings')
      expect(router.currentRoute.value.name).toBe('forbidden')
    })

    it('getHomeRoute() tự động tìm trang /admin/* đầu tiên mà Custom Role có quyền xem', async () => {
      const token = makeFakeJwt({
        sub: 'banner_mod',
        role: 'ROLE_ADMIN',
        customRole: 'ROLE_BANNER_MOD',
        roleName: 'Kiểm Duyệt Banner',
        permissions: ['BANNER_VIEW', 'BANNER_APPROVE_REJECT'],
        exp: Math.floor(Date.now() / 1000) + 3600,
      })
      authStore.token = token

      expect(authStore.getHomeRoute()).toBe('/admin/banners')

      // Nếu cố vào /login khi đã đăng nhập -> tự động đẩy về /admin/banners
      await router.push('/login')
      expect(router.currentRoute.value.path).toBe('/admin/banners')
    })

    it('PC-01: Custom Role có permissions rỗng [] tuyệt đối KHÔNG trở thành Super Admin và bị chặn /403', async () => {
      const token = makeFakeJwt({
        sub: 'intern01',
        role: 'ROLE_ADMIN',
        customRole: 'ROLE_INTERN',
        roleName: 'Thực Tập Sinh',
        permissions: [],
        exp: Math.floor(Date.now() / 1000) + 3600,
      })
      authStore.token = token

      expect(authStore.isSuperAdmin).toBe(false)
      expect(authStore.getHomeRoute()).toBe('/403')

      await router.push('/admin')
      expect(router.currentRoute.value.name).toBe('forbidden')
    })
  })

  describe('Kịch bản 5: Token bất thường, Hết hạn & Chống Open Redirect', () => {
    it('coi token đã hết hạn (exp quá khứ) là Guest và chuyển hướng sang /login', async () => {
      const expiredToken = makeFakeJwt({
        sub: 'customer1',
        role: 'ROLE_CUSTOMER',
        exp: Math.floor(Date.now() / 1000) - 3600,
      })
      authStore.token = expiredToken

      expect(authStore.isAuthenticated).toBe(false)
      expect(authStore.role).toBe('GUEST')

      await router.push('/admin')
      expect(router.currentRoute.value.name).toBe('auth-login')
    })

    it('coi token có exp = 0 hoặc exp chuỗi không hợp lệ là Guest', () => {
      const zeroExpToken = makeFakeJwt({ sub: 'c', role: 'ROLE_CUSTOMER', exp: 0 })
      authStore.token = zeroExpToken
      expect(authStore.isAuthenticated).toBe(false)

      const stringExpToken = makeFakeJwt({ sub: 'c', role: 'ROLE_CUSTOMER', exp: 'invalid' })
      authStore.token = stringExpToken
      expect(authStore.isAuthenticated).toBe(false)
    })

    it('resolveSafeRedirect lọc bỏ URL ngoài, Open Redirect và ký tự điều khiển', () => {
      // Open redirect
      expect(resolveSafeRedirect('https://malicious-site.com', 'ROLE_CUSTOMER', router)).toBeNull()
      expect(resolveSafeRedirect('//malicious-site.com', 'ROLE_CUSTOMER', router)).toBeNull()
      expect(resolveSafeRedirect('/login', 'ROLE_CUSTOMER', router)).toBeNull()
      expect(resolveSafeRedirect('/login?foo=bar', 'ROLE_CUSTOMER', router)).toBeNull()
      expect(resolveSafeRedirect('/login#section', 'ROLE_CUSTOMER', router)).toBeNull()
      expect(resolveSafeRedirect('/403', 'ROLE_CUSTOMER', router)).toBeNull()
      expect(resolveSafeRedirect('/path\x00evil', 'ROLE_CUSTOMER', router)).toBeNull()

      // Redirect hợp lệ cho Customer
      expect(resolveSafeRedirect('/my-bookings', 'ROLE_CUSTOMER', router)).toBe('/my-bookings')

      // Redirect yêu cầu Admin nhưng vai trò là Customer -> bị từ chối
      expect(resolveSafeRedirect('/admin', 'ROLE_CUSTOMER', router)).toBeNull()

      // Redirect yêu cầu Admin cho Admin -> hợp lệ
      expect(resolveSafeRedirect('/admin/operators', 'ROLE_ADMIN', router)).toBe('/admin/operators')
    })
  })

  describe('Kịch bản 6: Giao diện ForbiddenView.vue (Mã lỗi 403)', () => {
    it('render chính xác thông tin lỗi 403 và nút điều hướng theo vai trò', () => {
      const token = makeFakeJwt({
        sub: 'customer1',
        role: 'ROLE_CUSTOMER',
        exp: Math.floor(Date.now() / 1000) + 3600,
      })
      authStore.token = token
      authStore.user = { username: 'customer1', role: 'ROLE_CUSTOMER' }

      const wrapper = mount(ForbiddenView, {
        global: {
          plugins: [router],
        },
      })

      expect(wrapper.text()).toContain('403')
      expect(wrapper.text()).toContain('Không Có Quyền Truy Cập')
      expect(wrapper.text()).toContain('ROLE_CUSTOMER')
      expect(wrapper.text()).toContain('Về Trang Chủ Đặt Vé')
    })

    it('render UserProfileView thành công không bị crash auth undefined', () => {
      const token = makeFakeJwt({
        sub: 'customer1',
        role: 'ROLE_CUSTOMER',
        exp: Math.floor(Date.now() / 1000) + 3600,
      })
      authStore.token = token
      authStore.user = { username: 'customer1', role: 'ROLE_CUSTOMER' }

      const wrapper = mount(UserProfileView)
      expect(wrapper.text()).toContain('Hồ Sơ Cá Nhân & Bảo Mật Tài Khoản')
      expect(wrapper.text()).toContain('ROLE_CUSTOMER')
    })
  })
})
