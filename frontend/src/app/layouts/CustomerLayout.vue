<template>
  <div class="min-h-screen flex flex-col bg-canvas text-text-primary">
    <!-- Header (Sticky, 72px desktop / 60px mobile, white surface with subtle border) -->
    <header class="bg-surface border-b border-border-default sticky top-0 z-30 shadow-sm">
      <div :class="['px-4 h-[72px] flex items-center justify-between', containerClass]">
        <div class="flex items-center gap-6">
          <router-link to="/" class="flex items-center gap-2 font-bold text-xl text-primary-600">
            <span class="w-8 h-8 rounded-lg bg-brand-navy flex items-center justify-center text-white font-black text-sm">
              VB
            </span>
            <span class="text-brand-navy tracking-tight">VeloBus</span>
            <span class="hidden sm:inline-block text-[11px] font-semibold px-2 py-0.5 rounded-full bg-blue-50 text-primary-600 border border-blue-200">
              Mobility SaaS
            </span>
          </router-link>

          <nav class="hidden md:flex items-center gap-1">
            <router-link
              to="/"
              class="px-3.5 py-2 rounded-lg text-xs font-semibold text-text-secondary hover:bg-canvas transition-colors"
              exact-active-class="bg-blue-50 text-primary-600"
            >
              Tìm & Đặt vé
            </router-link>
            <router-link
              v-if="!auth.isAuthenticated || auth.isCustomer"
              to="/my-bookings"
              class="px-3.5 py-2 rounded-lg text-xs font-semibold text-text-secondary hover:bg-canvas transition-colors"
              active-class="bg-blue-50 text-primary-600"
            >
              Vé của tôi
            </router-link>
          </nav>
        </div>

        <div class="flex items-center gap-3">
          <!-- Notification Center -->
          <NotificationCenter />

          <!-- Width Layout Mode Switcher (Hẹp / Chuẩn / Rộng) -->
          <div class="hidden sm:block">
            <LayoutWidthSwitcher />
          </div>

          <!-- Role-based Portal Links -->
          <router-link
            v-if="auth.isOperator"
            to="/operator"
            class="hidden sm:inline-flex text-xs font-semibold px-3 py-2 rounded-lg border border-emerald-300 bg-emerald-50 text-emerald-700 hover:bg-emerald-100 transition-colors items-center gap-1.5"
          >
            <i class="pi pi-car"></i>
            <span>Portal Nhà xe</span>
          </router-link>

          <router-link
            v-if="auth.isAdmin"
            to="/admin"
            class="hidden sm:inline-flex text-xs font-semibold px-3 py-2 rounded-lg border border-purple-300 bg-purple-50 text-purple-700 hover:bg-purple-100 transition-colors items-center gap-1.5"
          >
            <i class="pi pi-shield"></i>
            <span>Portal Admin</span>
          </router-link>

          <!-- Authentication buttons -->
          <router-link
            v-if="!auth.isAuthenticated"
            to="/login"
            class="text-xs font-semibold px-4 py-2 rounded-lg bg-primary-600 text-white hover:bg-primary-700 shadow-sm transition-all"
          >
            Đăng nhập
          </router-link>
          <div v-else class="flex items-center gap-2">
            <router-link
              to="/profile"
              class="text-xs font-semibold px-3 py-2 rounded-lg border border-border-default hover:bg-canvas transition-colors text-text-secondary flex items-center gap-1"
            >
              <i class="pi pi-user text-xs"></i>
              <span>{{ auth.user?.username || 'Hồ sơ' }}</span>
            </router-link>
            <button
              class="text-xs font-semibold px-3 py-2 rounded-lg bg-slate-100 text-text-primary hover:bg-slate-200 transition-colors"
              title="Đăng xuất"
              @click="handleLogout"
            >
              Thoát
            </button>
          </div>
        </div>
      </div>
    </header>

    <!-- Main Content Area with dynamic container class -->
    <main class="flex-1">
      <div :class="containerClass">
        <router-view />
      </div>
    </main>

    <!-- Footer (Brand Navy Background, 48-64px padding desktop as per guidelines) -->
    <footer class="bg-brand-navy text-white pt-12 pb-8 border-t border-slate-800">
      <div :class="['grid grid-cols-1 md:grid-cols-4 gap-8 pb-8 border-b border-slate-800 text-xs px-4', containerClass]">
        <div class="space-y-3 md:col-span-1">
          <div class="flex items-center gap-2 font-bold text-lg text-white">
            <span class="w-7 h-7 rounded-lg bg-primary-600 flex items-center justify-center text-white font-black text-xs">VB</span>
            <span>VeloBus</span>
          </div>
          <p class="text-slate-400 text-xs leading-relaxed">
            Nền tảng đặt vé xe khách liên tỉnh trực tuyến chuẩn công nghệ cao. Khóa ghế thời gian thực với Redis Distributed Lock và thanh toán VNPay an toàn.
          </p>
        </div>

        <div class="space-y-2">
          <h4 class="text-xs font-bold uppercase tracking-wider text-slate-200">Hành Khách</h4>
          <ul class="space-y-1.5 text-slate-400">
            <li><router-link to="/" class="hover:text-white">Tìm kiếm chuyến xe</router-link></li>
            <li v-if="!auth.isAuthenticated || auth.isCustomer">
              <router-link to="/my-bookings" class="hover:text-white">Kiểm tra vé điện tử</router-link>
            </li>
            <li><span class="hover:text-white cursor-pointer">Chính sách hủy & đổi trả</span></li>
          </ul>
        </div>

        <div class="space-y-2">
          <h4 class="text-xs font-bold uppercase tracking-wider text-slate-200">Đối Tác Nhà Xe</h4>
          <ul class="space-y-1.5 text-slate-400">
            <template v-if="auth.isOperator">
              <li><router-link to="/operator" class="hover:text-white font-medium text-emerald-400">Vào Portal Nhà xe</router-link></li>
              <li><router-link to="/operator/fleet" class="hover:text-white">Quản lý đội xe & tài xế</router-link></li>
              <li><router-link to="/operator" class="hover:text-white">Ví ký quỹ Escrow 4 số dư</router-link></li>
            </template>
            <template v-else-if="!auth.isAuthenticated">
              <li><router-link to="/login" class="hover:text-white">Cổng thông tin Đối tác</router-link></li>
              <li><span class="hover:text-white cursor-pointer">Chính sách hợp tác mở tuyến</span></li>
            </template>
            <template v-else>
              <li><span class="hover:text-white cursor-pointer">Hợp tác mở tuyến xe liên tỉnh</span></li>
            </template>
          </ul>
        </div>

        <div class="space-y-2">
          <h4 class="text-xs font-bold uppercase tracking-wider text-slate-200">Hỗ Trợ 24/7</h4>
          <p class="text-slate-400">Tổng đài chăm sóc khách hàng: <strong class="text-white">1900 6868</strong></p>
          <p class="text-slate-400">Email: hotro@velobus.vn</p>
          <p class="text-[11px] text-slate-500 mt-2">Hà Nội • Đà Nẵng • TP. Hồ Chí Minh</p>
        </div>
      </div>

      <div :class="['pt-6 flex flex-col sm:flex-row items-center justify-between text-[11px] text-slate-500 gap-3 px-4', containerClass]">
        <span>© 2026 VeloBus Platform. Bản quyền thuộc về Đồ Án Kỹ Thuật Tốt Nghiệp CNTT.</span>
        <span>Kiến trúc Monorepo • Java 21 Spring Boot + Redisson • Vue 3 PrimeVue + Tailwind</span>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/modules/auth/store'
import { useLayoutWidth } from '@/shared/composables/useLayoutWidth'
import LayoutWidthSwitcher from '@/shared/components/LayoutWidthSwitcher.vue'
import NotificationCenter from '@/shared/components/NotificationCenter.vue'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const { containerClass } = useLayoutWidth()

function handleLogout() {
  auth.logout()
  const requiresAuth = route.matched.some(r => r.meta?.requiresAuth)
  if (requiresAuth) {
    router.push('/login')
  }
}
</script>
