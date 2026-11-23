import { createRouter, createWebHistory } from 'vue-router'
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
      },
      {
        path: 'search',
        name: 'customer-trip-search',
        component: () => import('@/modules/catalog-booking/views/TripListView.vue'),
      },
      {
        path: 'trips/:id/seats',
        name: 'trip-seat-selection',
        component: () => import('@/modules/catalog-booking/views/SeatSelectionView.vue'),
      },
      {
        path: 'checkout/:bookingId',
        name: 'customer-checkout',
        component: () => import('@/modules/catalog-booking/views/CheckoutView.vue'),
      },
      {
        path: 'my-bookings',
        name: 'customer-my-bookings',
        component: () => import('@/modules/catalog-booking/views/MyBookingsView.vue'),
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
      },
    ],
  },
  {
    path: '/operator',
    component: OperatorLayout,
    children: [
      {
        path: '',
        name: 'operator-dashboard',
        component: () => import('@/modules/partner/views/OperatorDashboardView.vue'),
      },
      {
        path: 'fleet',
        name: 'operator-fleet',
        component: () => import('@/modules/partner/views/FleetAndDriversView.vue'),
      },
      {
        path: 'trips',
        name: 'operator-trips',
        component: () => import('@/modules/partner/views/OperatorTripsView.vue'),
      },
      {
        path: 'banners',
        name: 'operator-banners',
        component: () => import('@/modules/partner/views/OperatorBannersView.vue'),
      },
    ],
  },
  {
    path: '/admin',
    component: AdminLayout,
    children: [
      {
        path: '',
        name: 'admin-dashboard',
        component: () => import('@/modules/admin/views/AdminDashboardView.vue'),
      },
      {
        path: 'operators',
        name: 'admin-operators',
        component: () => import('@/modules/admin/views/AdminOperatorsView.vue'),
      },
      {
        path: 'withdrawals',
        name: 'admin-withdrawals',
        component: () => import('@/modules/admin/views/AdminWithdrawalsView.vue'),
      },
      {
        path: 'banners',
        name: 'admin-banners',
        component: () => import('@/modules/admin/views/AdminBannersView.vue'),
      },
      {
        path: 'users-bookings',
        name: 'admin-users-bookings',
        component: () => import('@/modules/admin/views/AdminUsersBookingsView.vue'),
      },
      {
        path: 'settings',
        name: 'admin-settings',
        component: () => import('@/modules/admin/views/AdminSettingsView.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
