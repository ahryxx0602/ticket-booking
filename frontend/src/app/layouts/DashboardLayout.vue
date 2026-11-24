<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/modules/auth/store'
import { useLayoutWidth } from '@/shared/composables/useLayoutWidth'
import LayoutWidthSwitcher from '@/shared/components/LayoutWidthSwitcher.vue'
import NotificationCenter from '@/shared/components/NotificationCenter.vue'

const props = defineProps({
  portalType: {
    type: String,
    default: 'operator',
    validator: (v) => ['operator', 'admin'].includes(v)
  }
})

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const { containerClass } = useLayoutWidth()

// Collapsible sidebar state
const isCollapsed = ref(false)

const isAdmin = computed(() => props.portalType === 'admin')

const portalTitle = computed(() => {
  return isAdmin.value ? 'Platform Governance' : 'Partner Workspace'
})

const roleBadge = computed(() => {
  if (isAdmin.value) {
    const code = auth.customRole || 'ROLE_ADMIN'
    const isCustom = code !== 'ROLE_ADMIN'
    return {
      label: isCustom ? `${code} • ${auth.roleName}` : 'ROLE_ADMIN • Super Admin',
      class: isCustom
        ? 'bg-amber-50 text-amber-800 border-amber-300'
        : 'bg-purple-50 text-purple-700 border-purple-200'
    }
  }
  return { label: 'ROLE_OPERATOR', class: 'bg-emerald-50 text-emerald-700 border-emerald-200' }
})

const operatorMenuItems = [
  {
    label: 'Tổng quan & Ví Ký Quỹ',
    to: '/operator',
    exact: true,
    icon: 'M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z'
  },
  {
    label: 'Đội xe & Tài xế',
    to: '/operator/fleet',
    exact: false,
    icon: 'M9 17a2 2 0 11-4 0 2 2 0 014 0zM19 17a2 2 0 11-4 0 2 2 0 014 0z M13 16V6a1 1 0 00-1-1H4a1 1 0 00-1 1v10a1 1 0 001 1h1m8-1a1 1 0 01-1 1H9m4-1V8a1 1 0 011-1h2.586a1 1 0 01.707.293l3.414 3.414a1 1 0 01.293.707V16a1 1 0 01-1 1h-1m-6-1a1 1 0 001 1h1M5 17a2 2 0 104 0m-4 0a2 2 0 114 0m6 0a2 2 0 104 0m-4 0a2 2 0 114 0'
  },
  {
    label: 'Lập lịch Chuyến xe',
    to: '/operator/trips',
    exact: false,
    icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z'
  },
  {
    label: 'Banner & Thương hiệu',
    to: '/operator/banners',
    exact: false,
    icon: 'M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z'
  }
]

const adminMenuItems = [
  {
    label: 'Tổng quan & Đối soát',
    to: '/admin',
    exact: true,
    permissions: ['PAYMENT_REPORT_VIEW', 'PAYMENT_LIST_VIEW'],
    icon: 'M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z'
  },
  {
    label: 'Duyệt Nhà xe & Hoa hồng',
    to: '/admin/operators',
    exact: false,
    permissions: ['OPERATOR_VIEW'],
    icon: 'M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4'
  },
  {
    label: 'Duyệt Rút tiền Escrow',
    to: '/admin/withdrawals',
    exact: false,
    permissions: ['WITHDRAWAL_VIEW'],
    icon: 'M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z'
  },
  {
    label: 'Kiểm duyệt Banner',
    to: '/admin/banners',
    exact: false,
    permissions: ['BANNER_VIEW'],
    icon: 'M11 5.882V19.24a1.76 1.76 0 01-3.417.592l-2.147-6.15M18 13a3 3 0 100-6M5.436 13.683A4.001 4.001 0 017 6h1.832c4.1 0 7.625-1.234 9.168-3v14c-1.543-1.766-5.067-3-9.168-3H7a3.988 3.988 0 01-1.564-.317z'
  },
  {
    label: 'User, Role & Hoàn vé Admin',
    to: '/admin/users-bookings',
    exact: false,
    permissions: ['USER_VIEW', 'ROLE_MANAGE_CRUD', 'BOOKING_VIEW'],
    icon: 'M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z'
  },
  {
    label: 'Bến xe, Chuyến & Cấu hình',
    to: '/admin/settings',
    exact: false,
    permissions: ['STATION_VIEW', 'TRIP_MANAGE_CRUD', 'SYSTEM_SETTINGS_MANAGE'],
    icon: 'M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z'
  }
]

const menuItems = computed(() => {
  if (!isAdmin.value) return operatorMenuItems
  if (!auth.isAuthenticated) return adminMenuItems
  return adminMenuItems.filter(item => auth.hasAnyPermission(item.permissions))
})

function handleExitPreview() {
  auth.stopRolePreview()
  router.push('/admin/users-bookings')
}

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="min-h-screen flex bg-canvas text-text-primary">
    <!-- Sidebar (Single Source of Truth: 256px desktop, Brand Navy #142B4A background) -->
    <aside
      :class="[
        'bg-brand-navy text-white flex flex-col transition-all duration-200 z-40 flex-shrink-0',
        isCollapsed ? 'w-[76px]' : 'w-64',
        'hidden md:flex'
      ]"
    >
      <!-- Top Brand Logo -->
      <div class="h-[72px] px-5 flex items-center justify-between border-b border-slate-700/60 flex-shrink-0">
        <router-link to="/" class="flex items-center gap-2.5 overflow-hidden font-bold">
          <div class="w-8 h-8 rounded-lg bg-primary-600 flex items-center justify-center font-black text-white text-sm flex-shrink-0 shadow-sm">
            VB
          </div>
          <div v-show="!isCollapsed" class="whitespace-nowrap transition-opacity">
            <span class="text-base text-white font-bold">VeloBus</span>
            <span class="block text-[10px] text-blue-200 font-normal uppercase tracking-wider">{{ portalTitle }}</span>
          </div>
        </router-link>

        <button
          type="button"
          @click="isCollapsed = !isCollapsed"
          class="text-slate-400 hover:text-white p-1 rounded transition-colors"
          :title="isCollapsed ? 'Mở rộng menu' : 'Thu gọn menu'"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="isCollapsed ? 'M13 5l7 7-7 7M5 5l7 7-7 7' : 'M11 19l-7-7 7-7m8 14l-7-7 7-7'"/>
          </svg>
        </button>
      </div>

      <!-- Navigation Menu Items -->
      <nav class="p-3 space-y-1.5 flex-1 overflow-y-auto">
        <div v-show="!isCollapsed" class="px-3 py-1.5 text-[11px] font-semibold uppercase tracking-wider text-slate-400 flex items-center justify-between">
          <span>Chức năng nghiệp vụ</span>
          <span v-if="isAdmin" class="text-[10px] px-1.5 py-0.5 rounded bg-slate-800 text-blue-300 font-bold">
            {{ menuItems.length }}/6
          </span>
        </div>

        <router-link
          v-for="item in menuItems"
          :key="item.to"
          :to="item.to"
          :class="[
            'flex items-center gap-3 px-3 py-2.5 rounded-lg text-xs font-semibold transition-all group',
            route.path === item.to || (!item.exact && route.path.startsWith(item.to) && item.to !== '/operator' && item.to !== '/admin')
              ? 'bg-primary-600 text-white shadow-sm'
              : 'text-slate-300 hover:bg-brand-navy-hover hover:text-white'
          ]"
          :title="isCollapsed ? item.label : ''"
        >
          <svg class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.75" :d="item.icon"/>
          </svg>
          <span v-show="!isCollapsed" class="whitespace-nowrap">{{ item.label }}</span>
        </router-link>
      </nav>

      <!-- Bottom User & Exit Panel -->
      <div class="p-3 border-t border-slate-700/60 space-y-2 flex-shrink-0">
        <router-link
          to="/"
          class="flex items-center gap-2 px-3 py-2 text-xs text-slate-400 hover:text-white transition-colors rounded-lg hover:bg-brand-navy-hover"
        >
          <svg class="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"/>
          </svg>
          <span v-show="!isCollapsed" class="whitespace-nowrap">Trang chủ đặt vé</span>
        </router-link>
      </div>
    </aside>

    <!-- Right Side: Header (72px) & Main Content -->
    <div class="flex-1 flex flex-col min-w-0">
      <!-- Role Preview Active Banner (PA-05) -->
      <div
        v-if="isAdmin && auth.previewRole"
        data-testid="role-preview-banner"
        class="bg-amber-500 text-slate-950 px-4 sm:px-6 py-2.5 flex flex-wrap items-center justify-between gap-3 text-xs font-bold shadow-sm z-40"
      >
        <div class="flex items-center gap-2">
          <span class="px-2 py-0.5 rounded bg-slate-950 text-amber-300 text-[11px] uppercase tracking-wider">⚡ Chế Độ Xem Thử Role</span>
          <span>
            Giao diện & Sidebar đang giới hạn theo Role:
            <strong class="underline">{{ auth.previewRole.name }} ({{ auth.previewRole.code }})</strong>
            — {{ auth.previewRole.permissions?.length || 0 }}/26 quyền API được cấp
          </span>
        </div>
        <button
          type="button"
          data-testid="exit-role-preview-btn"
          @click="handleExitPreview"
          class="px-3 py-1 rounded-md bg-slate-950 text-white hover:bg-slate-800 text-xs font-bold transition-colors"
        >
          ✕ Thoát Xem Thử (Về Super Admin)
        </button>
      </div>

      <!-- Fixed Header (72px height, Surface White #FFFFFF, Border #E2E8F0) -->
      <header class="h-[72px] bg-surface border-b border-border-default px-4 sm:px-6 flex items-center justify-between sticky top-0 z-30 shadow-card flex-shrink-0">
        <!-- Left: Workspace breadcrumb & status -->
        <div class="flex items-center gap-2 sm:gap-3">
          <div class="flex items-center gap-2">
            <h2 class="text-xs sm:text-sm font-bold text-text-primary tracking-tight">{{ portalTitle }}</h2>
            <span class="text-[10px] sm:text-[11px] px-2.5 py-0.5 rounded-full font-semibold border" :class="roleBadge.class">
              {{ roleBadge.label }}
            </span>
          </div>
        </div>

        <!-- Right: Actions, Layout Width Switcher & User Profile -->
        <div class="flex items-center gap-2 sm:gap-4">
          <!-- Width Layout Mode Switcher (Hẹp / Chuẩn / Rộng) -->
          <div class="hidden sm:block">
            <LayoutWidthSwitcher />
          </div>

          <!-- Notification Center -->
          <NotificationCenter />

          <!-- User Profile & Logout -->
          <div class="flex items-center gap-3 pl-2 border-l border-border-default">
            <div class="text-right hidden sm:block">
              <p class="text-xs font-bold text-text-primary leading-tight">{{ auth.user?.username || 'VeloBus User' }}</p>
              <p class="text-[11px] text-text-muted">{{ auth.roleName || auth.user?.email || 'user@velobus.vn' }}</p>
            </div>

            <button
              type="button"
              @click="handleLogout"
              class="px-3 py-1.5 text-xs font-semibold rounded-md border border-border-default text-text-secondary hover:bg-canvas hover:text-red-600 transition-colors"
              title="Đăng xuất"
            >
              Thoát
            </button>
          </div>
        </div>
      </header>

      <!-- Main Content Canvas with dynamic width mode -->
      <main class="flex-1 bg-canvas p-6 overflow-y-auto">
        <div :class="containerClass">
          <router-view />
        </div>
      </main>
    </div>
  </div>
</template>
