import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/modules/auth/store'
import CustomerLayout from '@/app/layouts/CustomerLayout.vue'
import OperatorLayout from '@/app/layouts/OperatorLayout.vue'
import AdminLayout from '@/app/layouts/AdminLayout.vue'

const routes = [
  {
    path: '/',
    component: CustomerLayout,
    children: [
      {
        path: '',
        name: 'customer-home',
        component: () => import('@/modules/catalog-booking/views/HomeView.vue'),
        meta: { requiresAuth: false },
      },
      {
        path: 'search',
        name: 'customer-trip-search',
        component: () => import('@/modules/catalog-booking/views/TripListView.vue'),
        meta: { requiresAuth: false },
      },
      {
        path: 'trips/:id/seats',
        name: 'trip-seat-selection',
        component: () => import('@/modules/catalog-booking/views/SeatSelectionView.vue'),
        meta: { requiresAuth: false },
      },
      {
        path: 'checkout/:bookingId',
        name: 'customer-checkout',
        component: () => import('@/modules/catalog-booking/views/CheckoutView.vue'),
        meta: { requiresAuth: true, roles: ['ROLE_CUSTOMER'] },
      },
      {
        path: 'my-bookings',
        name: 'customer-my-bookings',
        component: () => import('@/modules/catalog-booking/views/MyBookingsView.vue'),
        meta: { requiresAuth: true, roles: ['ROLE_CUSTOMER'] },
      },
      {
        path: 'profile',
        name: 'customer-profile',
        component: () => import('@/modules/auth/views/UserProfileView.vue'),
        meta: { requiresAuth: true, roles: ['ROLE_CUSTOMER', 'ROLE_OPERATOR', 'ROLE_ADMIN'] },
      },
    ],
  },
  {
    path: '/login',
    component: () => import('@/app/layouts/AuthLayout.vue'),
    children: [
      {
        path: '',
        name: 'auth-login',
        component: () => import('@/modules/auth/views/LoginView.vue'),
        meta: { requiresAuth: false },
      },
    ],
  },
  {
    path: '/operator',
    component: OperatorLayout,
    meta: { requiresAuth: true, roles: ['ROLE_OPERATOR'] },
    children: [
      {
        path: '',
        name: 'operator-dashboard',
        component: () => import('@/modules/partner/views/OperatorDashboardView.vue'),
        meta: { requiresAuth: true, roles: ['ROLE_OPERATOR'] },
      },
      {
        path: 'fleet',
        name: 'operator-fleet',
        component: () => import('@/modules/partner/views/FleetAndDriversView.vue'),
        meta: { requiresAuth: true, roles: ['ROLE_OPERATOR'] },
      },
      {
        path: 'trips',
        name: 'operator-trips',
        component: () => import('@/modules/partner/views/OperatorTripsView.vue'),
        meta: { requiresAuth: true, roles: ['ROLE_OPERATOR'] },
      },
      {
        path: 'banners',
        name: 'operator-banners',
        component: () => import('@/modules/partner/views/OperatorBannersView.vue'),
        meta: { requiresAuth: true, roles: ['ROLE_OPERATOR'] },
      },
      {
        path: 'profile',
        name: 'operator-profile',
        component: () => import('@/modules/partner/views/OperatorProfileView.vue'),
        meta: { requiresAuth: true, roles: ['ROLE_OPERATOR'] },
      },
    ],
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] },
    children: [
      {
        path: '',
        name: 'admin-dashboard',
        component: () => import('@/modules/admin/views/AdminDashboardView.vue'),
        meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] },
      },
      {
        path: 'operators',
        name: 'admin-operators',
        component: () => import('@/modules/admin/views/AdminOperatorsView.vue'),
        meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] },
      },
      {
        path: 'withdrawals',
        name: 'admin-withdrawals',
        component: () => import('@/modules/admin/views/AdminWithdrawalsView.vue'),
        meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] },
      },
      {
        path: 'banners',
        name: 'admin-banners',
        component: () => import('@/modules/admin/views/AdminBannersView.vue'),
        meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] },
      },
      {
        path: 'users-bookings',
        name: 'admin-users-bookings',
        component: () => import('@/modules/admin/views/AdminUsersBookingsView.vue'),
        meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] },
      },
      {
        path: 'settings',
        name: 'admin-settings',
        component: () => import('@/modules/admin/views/AdminSettingsView.vue'),
        meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] },
      },
    ],
  },
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('@/shared/views/ForbiddenView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/shared/views/NotFoundView.vue'),
    meta: { requiresAuth: false },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

/**
 * Vue Router Navigation Guard - Cưỡng chế bảo mật RBAC & điều hướng động
 */
router.beforeEach((to, from) => {
  // useAuthStore được gọi an toàn bên trong callback sau khi Pinia đã khởi tạo
  const auth = useAuthStore()

  // 1. Whitelist duy nhất route trạng thái 403 Forbidden (chống loop)
  if (to.name === 'forbidden') {
    return true
  }

  // 2. Chống lộ route nội bộ (Route Enumeration) cho khách vãng lai và vai trò trái phép
  // Bắt đầu kiểm tra segment /admin(/|$) hoặc /operator(/|$) trước not-found (không phân biệt chữ hoa/thường)
  const isInternalPath = /^\/(admin|operator)(\/|$)/i.test(to.path)
  if (isInternalPath) {
    if (!auth.isAuthenticated) {
      return {
        name: 'auth-login',
        query: { redirect: to.fullPath },
      }
    }
    const lowerPath = to.path.toLowerCase()
    const requiredRole = lowerPath.startsWith('/admin') ? 'ROLE_ADMIN' : 'ROLE_OPERATOR'
    if (auth.role !== requiredRole) {
      return {
        name: 'forbidden',
        replace: true,
      }
    }
  }

  // 3. Sau khi đã chốt chặn internal path, cho phép render not-found cho các route công khai không tồn tại
  if (to.name === 'not-found') {
    return true
  }

  // 4. Nếu đã đăng nhập mà truy cập lại trang Login -> Chuyển về trang chủ theo vai trò
  if (to.name === 'auth-login' && auth.isAuthenticated) {
    return auth.getHomeRoute()
  }

  // 5. Kiểm tra requiresAuth cho các route bảo vệ khác (khách hàng / profile)
  const requiresAuth = to.matched.some(record => record.meta?.requiresAuth)
  if (requiresAuth && !auth.isAuthenticated) {
    return {
      name: 'auth-login',
      query: { redirect: to.fullPath },
    }
  }

  // 6. Kiểm tra phân quyền vai trò (RBAC) cho route con cụ thể
  const requiredRolesRecord = [...to.matched].reverse().find(r => r.meta?.roles)
  const allowedRoles = requiredRolesRecord?.meta?.roles

  if (allowedRoles && Array.isArray(allowedRoles) && allowedRoles.length > 0) {
    if (auth.isAuthenticated && !allowedRoles.includes(auth.role)) {
      return {
        name: 'forbidden',
        replace: true,
      }
    }
  }

  return true
})

export default router
