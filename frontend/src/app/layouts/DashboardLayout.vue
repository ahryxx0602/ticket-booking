<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/modules/auth/store'
import { useLayoutWidth } from '@/shared/composables/useLayoutWidth'
import LayoutWidthSwitcher from '@/shared/components/LayoutWidthSwitcher.vue'
import NotificationCenter from '@/shared/components/NotificationCenter.vue'
import ThemeCustomizer from '@/shared/components/ThemeCustomizer.vue'
import { Button } from '@/components/ui/button'
import {
  Sheet,
  SheetTrigger,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetClose
} from '@/components/ui/sheet'
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator
} from '@/components/ui/dropdown-menu'
import {
  LayoutDashboard,
  Bus,
  Calendar,
  Image as ImageIcon,
  Users,
  Settings,
  CreditCard,
  Building2,
  Receipt,
  LogOut,
  Menu,
  ChevronLeft,
  ChevronRight,
  Shield,
  ArrowLeft,
  User
} from 'lucide-vue-next'

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

// Collapsible sidebar state (Desktop)
const isCollapsed = ref(false)
// Mobile off-canvas sheet state
const isMobileOpen = ref(false)

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
        ? 'bg-amber-500/10 text-amber-700 dark:text-amber-400 border-amber-500/30'
        : 'bg-primary/10 text-primary border-primary/20'
    }
  }
  return { label: 'ROLE_OPERATOR', class: 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 border-emerald-500/30' }
})

const operatorMenuItems = [
  {
    label: 'Tổng quan & Ví Ký Quỹ',
    to: '/operator',
    exact: true,
    icon: CreditCard
  },
  {
    label: 'Đội xe & Tài xế',
    to: '/operator/fleet',
    exact: false,
    icon: Bus
  },
  {
    label: 'Lập lịch Chuyến xe',
    to: '/operator/trips',
    exact: false,
    icon: Calendar
  },
  {
    label: 'Banner & Thương hiệu',
    to: '/operator/banners',
    exact: false,
    icon: ImageIcon
  }
]

const adminMenuItems = [
  {
    label: 'Tổng quan & Đối soát',
    to: '/admin',
    exact: true,
    permissions: ['PAYMENT_REPORT_VIEW', 'PAYMENT_LIST_VIEW'],
    icon: LayoutDashboard
  },
  {
    label: 'Duyệt Nhà xe & Hoa hồng',
    to: '/admin/operators',
    exact: false,
    permissions: ['OPERATOR_VIEW'],
    icon: Building2
  },
  {
    label: 'Duyệt Rút tiền Escrow',
    to: '/admin/withdrawals',
    exact: false,
    permissions: ['WITHDRAWAL_VIEW'],
    icon: Receipt
  },
  {
    label: 'Kiểm duyệt Banner',
    to: '/admin/banners',
    exact: false,
    permissions: ['BANNER_VIEW'],
    icon: ImageIcon
  },
  {
    label: 'User, Role & Hoàn vé Admin',
    to: '/admin/users-bookings',
    exact: false,
    permissions: ['USER_VIEW', 'ROLE_MANAGE_CRUD', 'BOOKING_VIEW'],
    icon: Users
  },
  {
    label: 'Bến xe, Chuyến & Cấu hình',
    to: '/admin/settings',
    exact: false,
    permissions: ['STATION_VIEW', 'TRIP_MANAGE_CRUD', 'SYSTEM_SETTINGS_MANAGE'],
    icon: Settings
  }
]

const menuItems = computed(() => {
  if (!isAdmin.value) return operatorMenuItems
  // FE-SEC-001: Fail-Closed sidebar menu when unauthenticated
  if (!auth.isAuthenticated) return []
  return adminMenuItems.filter(item => auth.hasAnyPermission(item.permissions))
})

// FE-SEC-002: Tự động đồng bộ lại role & permissions từ GET /api/v1/auth/me khi khởi tạo/reload trang quản trị
onMounted(async () => {
  if (isAdmin.value && auth.isAuthenticated) {
    const updated = await auth.refreshProfile()
    if (updated && !auth.isAdmin) {
      router.replace(auth.getHomeRoute())
    }
  }
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
  <div class="min-h-screen flex bg-background text-foreground transition-colors duration-200">
    <!-- Desktop Sidebar (256px expanded / 64px collapsed, Claude+ Clean Minimalist Enterprise) -->
    <aside
      :class="[
        'bg-card border-r border-border flex flex-col transition-all duration-300 z-40 flex-shrink-0 select-none',
        isCollapsed ? 'w-16' : 'w-64',
        'hidden md:flex'
      ]"
    >
      <!-- Top Brand Logo -->
      <div class="h-16 px-4 flex items-center justify-between border-b border-border flex-shrink-0">
        <router-link to="/" class="flex items-center gap-3 overflow-hidden font-bold">
          <div class="w-8 h-8 rounded-lg bg-primary flex items-center justify-center font-black text-primary-foreground text-sm flex-shrink-0 shadow-xs">
            VB
          </div>
          <div v-show="!isCollapsed" class="whitespace-nowrap transition-opacity">
            <span class="text-sm font-bold tracking-tight text-foreground">VeloBus</span>
            <span class="block text-[10px] text-muted-foreground uppercase tracking-wider font-medium">{{ portalTitle }}</span>
          </div>
        </router-link>

        <button
          type="button"
          @click="isCollapsed = !isCollapsed"
          class="text-muted-foreground hover:text-foreground p-1.5 rounded-md hover:bg-muted/60 transition-colors"
          :title="isCollapsed ? 'Mở rộng menu (256px)' : 'Thu gọn menu (64px)'"
        >
          <ChevronRight v-if="isCollapsed" class="w-4 h-4" />
          <ChevronLeft v-else class="w-4 h-4" />
        </button>
      </div>

      <!-- Navigation Menu Items -->
      <nav class="p-3 space-y-1.5 flex-1 overflow-y-auto">
        <div v-show="!isCollapsed" class="px-3 py-1.5 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground flex items-center justify-between">
          <span>Chức năng</span>
          <span v-if="isAdmin" class="text-[10px] px-1.5 py-0.5 rounded bg-muted text-muted-foreground font-bold">
            {{ menuItems.length }}/6
          </span>
        </div>

        <router-link
          v-for="item in menuItems"
          :key="item.to"
          :to="item.to"
          :class="[
            'flex items-center gap-3 px-3 py-2.5 rounded-lg text-xs font-medium transition-all group',
            route.path === item.to || (!item.exact && route.path.startsWith(item.to) && item.to !== '/operator' && item.to !== '/admin')
              ? 'bg-primary text-primary-foreground shadow-xs font-semibold'
              : 'text-muted-foreground hover:bg-muted/60 hover:text-foreground'
          ]"
          :title="isCollapsed ? item.label : ''"
        >
          <component :is="item.icon" class="w-4 h-4 flex-shrink-0" />
          <span v-show="!isCollapsed" class="whitespace-nowrap">{{ item.label }}</span>
        </router-link>
      </nav>

      <!-- Bottom User & Exit Panel -->
      <div class="p-3 border-t border-border space-y-1 flex-shrink-0">
        <router-link
          to="/"
          class="flex items-center gap-2.5 px-3 py-2 text-xs text-muted-foreground hover:text-foreground transition-colors rounded-lg hover:bg-muted/60"
        >
          <ArrowLeft class="w-4 h-4 flex-shrink-0" />
          <span v-show="!isCollapsed" class="whitespace-nowrap">Trang chủ đặt vé</span>
        </router-link>
      </div>
    </aside>

    <!-- Right Side: Header (64px) & Main Content -->
    <div class="flex-1 flex flex-col min-w-0">
      <!-- Role Preview Active Banner (PA-05) -->
      <div
        v-if="isAdmin && auth.previewRole"
        data-testid="role-preview-banner"
        class="bg-amber-500 text-slate-950 px-4 sm:px-6 py-2.5 flex flex-wrap items-center justify-between gap-3 text-xs font-bold shadow-xs z-40"
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
          class="px-3 py-1 rounded-md bg-slate-950 text-white hover:bg-slate-800 text-xs font-bold transition-colors cursor-pointer"
        >
          ✕ Thoát Xem Thử (Về Super Admin)
        </button>
      </div>

      <!-- Fixed Header (64px height, Claude+ Sticky Navigation with Backdrop Blur) -->
      <header class="h-16 bg-background/95 backdrop-blur-md border-b border-border px-4 sm:px-6 flex items-center justify-between sticky top-0 z-30 shadow-xs flex-shrink-0">
        <!-- Left: Mobile Menu Trigger & Portal Title / Breadcrumbs -->
        <div class="flex items-center gap-3">
          <!-- Mobile Drawer Trigger (< md) -->
          <Sheet v-model:open="isMobileOpen">
            <SheetTrigger as-child>
              <Button variant="ghost" size="sm" class="md:hidden p-1.5 h-9 w-9">
                <Menu class="h-5 w-5" />
                <span class="sr-only">Mở menu điều hướng</span>
              </Button>
            </SheetTrigger>
            <SheetContent side="left" class="w-72 p-0 flex flex-col bg-card">
              <SheetHeader class="h-16 px-5 border-b border-border flex flex-row items-center justify-between">
                <div class="flex items-center gap-2.5">
                  <div class="w-8 h-8 rounded-lg bg-primary flex items-center justify-center font-black text-primary-foreground text-sm">
                    VB
                  </div>
                  <div>
                    <SheetTitle class="text-sm font-bold text-foreground">VeloBus</SheetTitle>
                    <p class="text-[10px] text-muted-foreground uppercase">{{ portalTitle }}</p>
                  </div>
                </div>
              </SheetHeader>
              <nav class="p-4 space-y-1.5 flex-1 overflow-y-auto">
                <router-link
                  v-for="item in menuItems"
                  :key="item.to"
                  :to="item.to"
                  @click="isMobileOpen = false"
                  :class="[
                    'flex items-center gap-3 px-3 py-2.5 rounded-lg text-xs font-medium transition-colors',
                    route.path === item.to || (!item.exact && route.path.startsWith(item.to) && item.to !== '/operator' && item.to !== '/admin')
                      ? 'bg-primary text-primary-foreground font-semibold'
                      : 'text-muted-foreground hover:bg-muted/60 hover:text-foreground'
                  ]"
                >
                  <component :is="item.icon" class="w-4 h-4 flex-shrink-0" />
                  <span>{{ item.label }}</span>
                </router-link>
              </nav>
              <div class="p-4 border-t border-border">
                <router-link
                  to="/"
                  @click="isMobileOpen = false"
                  class="flex items-center gap-2 px-3 py-2 text-xs text-muted-foreground hover:text-foreground rounded-lg hover:bg-muted/60"
                >
                  <ArrowLeft class="w-4 h-4" />
                  <span>Quay về trang chủ đặt vé</span>
                </router-link>
              </div>
            </SheetContent>
          </Sheet>

          <div class="flex items-center gap-2.5">
            <h2 class="text-xs sm:text-sm font-bold text-foreground tracking-tight">{{ portalTitle }}</h2>
            <span class="text-[10px] sm:text-[11px] px-2.5 py-0.5 rounded-full font-semibold border" :class="roleBadge.class">
              {{ roleBadge.label }}
            </span>
          </div>
        </div>

        <!-- Right: Actions, Layout Width Switcher & User Profile -->
        <div class="flex items-center gap-2 sm:gap-3">
          <!-- Width Layout Mode Switcher (Hẹp / Chuẩn / Rộng) -->
          <div class="hidden sm:block">
            <LayoutWidthSwitcher />
          </div>

          <!-- Notification Center -->
          <NotificationCenter />

          <!-- User Profile Dropdown Menu -->
          <DropdownMenu>
            <DropdownMenuTrigger as-child>
              <Button variant="ghost" size="sm" class="flex items-center gap-2.5 pl-2 pr-2 h-9 border border-border/60">
                <div class="w-6 h-6 rounded-full bg-primary/10 text-primary flex items-center justify-center font-bold text-xs">
                  {{ (auth.user?.username || 'U')[0].toUpperCase() }}
                </div>
                <span class="text-xs font-semibold text-foreground hidden sm:inline-block max-w-[120px] truncate">
                  {{ auth.user?.username || 'VeloBus User' }}
                </span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" class="w-56">
              <div class="px-3 py-2">
                <p class="text-xs font-semibold text-foreground">{{ auth.user?.username || 'VeloBus User' }}</p>
                <p class="text-[11px] text-muted-foreground truncate">{{ auth.roleName || auth.user?.email || 'user@velobus.vn' }}</p>
              </div>
              <DropdownMenuSeparator />
              <DropdownMenuItem as-child>
                <router-link to="/profile" class="flex items-center gap-2 cursor-pointer">
                  <User class="w-4 h-4" />
                  <span>Hồ sơ tài khoản</span>
                </router-link>
              </DropdownMenuItem>
              <DropdownMenuItem as-child>
                <router-link to="/" class="flex items-center gap-2 cursor-pointer">
                  <Bus class="w-4 h-4" />
                  <span>Trang đặt vé khách hàng</span>
                </router-link>
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem class="text-destructive focus:bg-destructive/10 cursor-pointer" @click="handleLogout">
                <LogOut class="w-4 h-4 mr-2" />
                <span>Đăng xuất</span>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </header>

      <!-- Main Content Canvas with dynamic width mode -->
      <main class="flex-1 bg-background p-4 sm:p-6 overflow-y-auto">
        <div :class="containerClass">
          <router-view />
        </div>
      </main>
    </div>

    <!-- Floating Runtime Theme Customizer Component -->
    <ThemeCustomizer />
  </div>
</template>
