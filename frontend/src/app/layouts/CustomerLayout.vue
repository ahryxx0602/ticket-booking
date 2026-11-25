<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
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
  Bus,
  Ticket,
  User,
  LogOut,
  Building2,
  Shield,
  Menu,
  Sparkles,
  PhoneCall,
  ShieldCheck,
  Paintbrush
} from 'lucide-vue-next'

const router = useRouter()
const auth = useAuthStore()
const { containerClass } = useLayoutWidth()
const isMobileOpen = ref(false)
const isThemeCustomizerOpen = ref(false)

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="min-h-screen flex flex-col bg-background text-foreground transition-colors duration-200">
    <!-- Header (Sticky, 64px desktop / mobile, Claude+ surface with subtle border) -->
    <header class="bg-background/95 backdrop-blur-md border-b border-border sticky top-0 z-30 shadow-xs">
      <div :class="['px-4 h-16 flex items-center justify-between', containerClass]">
        <!-- Brand & Desktop Nav -->
        <div class="flex items-center gap-6">
          <router-link to="/" class="flex items-center gap-2.5 font-bold text-lg text-foreground">
            <span class="w-8 h-8 rounded-lg bg-primary flex items-center justify-center text-primary-foreground font-black text-sm shadow-xs">
              VB
            </span>
            <span class="tracking-tight text-foreground font-bold">VeloBus</span>
            <span class="hidden sm:inline-block text-[11px] font-semibold px-2.5 py-0.5 rounded-full bg-primary/10 text-primary border border-primary/20">
              Mobility SaaS
            </span>
          </router-link>

          <nav class="hidden md:flex items-center gap-1.5">
            <router-link
              to="/"
              class="px-3 py-2 rounded-lg text-xs font-semibold text-muted-foreground hover:bg-muted/60 hover:text-foreground transition-colors"
              exact-active-class="bg-primary/10 text-primary"
            >
              Tìm & Đặt vé
            </router-link>
            <router-link
              v-if="!auth.isAuthenticated || auth.isCustomer"
              to="/my-bookings"
              class="px-3 py-2 rounded-lg text-xs font-semibold text-muted-foreground hover:bg-muted/60 hover:text-foreground transition-colors"
              active-class="bg-primary/10 text-primary"
            >
              Vé của tôi
            </router-link>
          </nav>
        </div>

        <!-- Right Side Nav Actions & Dropdowns -->
        <div class="flex items-center gap-2 sm:gap-3">
          <!-- Notification Center -->
          <NotificationCenter />

          <!-- Layout Width Switcher -->
          <div class="hidden sm:block">
            <LayoutWidthSwitcher />
          </div>

          <!-- Theme Customizer Trigger Button -->
          <Button
            variant="ghost"
            size="sm"
            class="h-9 w-9 p-0 text-muted-foreground hover:text-foreground"
            @click="isThemeCustomizerOpen = true"
            title="Tùy biến giao diện (Theme Customizer)"
          >
            <Paintbrush class="w-4 h-4" />
            <span class="sr-only">Tùy biến giao diện</span>
          </Button>

          <!-- Role-based Portal Quick Links -->
          <router-link
            v-if="auth.isOperator"
            to="/operator"
            class="hidden sm:inline-flex text-xs font-semibold px-3 py-1.5 rounded-lg border border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 hover:bg-emerald-500/20 transition-colors items-center gap-1.5"
          >
            <Building2 class="w-3.5 h-3.5" />
            <span>Portal Nhà xe</span>
          </router-link>

          <router-link
            v-if="auth.isAdmin"
            to="/admin"
            class="hidden sm:inline-flex text-xs font-semibold px-3 py-1.5 rounded-lg border border-primary/30 bg-primary/10 text-primary hover:bg-primary/20 transition-colors items-center gap-1.5"
          >
            <Shield class="w-3.5 h-3.5" />
            <span>Portal Admin</span>
          </router-link>

          <!-- Auth Login / Profile Menu -->
          <router-link
            v-if="!auth.isAuthenticated"
            to="/login"
            class="text-xs font-semibold px-3.5 py-2 rounded-lg bg-primary text-primary-foreground hover:bg-primary/90 shadow-xs transition-all"
          >
            Đăng nhập
          </router-link>

          <DropdownMenu v-else>
            <DropdownMenuTrigger as-child>
              <Button variant="outline" size="sm" class="flex items-center gap-2 h-9 pl-2 pr-2.5">
                <div class="w-6 h-6 rounded-full bg-primary/10 text-primary flex items-center justify-center font-bold text-xs">
                  {{ (auth.user?.username || 'U')[0].toUpperCase() }}
                </div>
                <span class="text-xs font-semibold hidden sm:inline-block max-w-[120px] truncate">
                  {{ auth.user?.username || 'Hồ sơ' }}
                </span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" class="w-56">
              <div class="px-3 py-2">
                <p class="text-xs font-semibold text-foreground">{{ auth.user?.username || 'Hành khách' }}</p>
                <p class="text-[11px] text-muted-foreground truncate">{{ auth.user?.email || 'user@velobus.vn' }}</p>
              </div>
              <DropdownMenuSeparator />
              <DropdownMenuItem as-child>
                <router-link to="/profile" class="flex items-center gap-2 cursor-pointer">
                  <User class="w-4 h-4" />
                  <span>Thông tin tài khoản</span>
                </router-link>
              </DropdownMenuItem>
              <DropdownMenuItem as-child>
                <router-link to="/my-bookings" class="flex items-center gap-2 cursor-pointer">
                  <Ticket class="w-4 h-4" />
                  <span>Vé xe của tôi</span>
                </router-link>
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem class="text-destructive focus:bg-destructive/10 cursor-pointer" @click="handleLogout">
                <LogOut class="w-4 h-4 mr-2" />
                <span>Đăng xuất</span>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>

          <!-- Mobile Hamburger Drawer Button -->
          <Sheet v-model:open="isMobileOpen">
            <SheetTrigger as-child>
              <Button variant="ghost" size="sm" class="md:hidden p-1.5 h-9 w-9">
                <Menu class="h-5 w-5" />
                <span class="sr-only">Mở menu điều hướng</span>
              </Button>
            </SheetTrigger>
            <SheetContent side="right" class="w-72 p-0 flex flex-col bg-card">
              <SheetHeader class="h-16 px-5 border-b border-border flex flex-row items-center justify-between">
                <div class="flex items-center gap-2.5">
                  <div class="w-8 h-8 rounded-lg bg-primary flex items-center justify-center font-black text-primary-foreground text-sm">
                    VB
                  </div>
                  <SheetTitle class="text-sm font-bold text-foreground">VeloBus</SheetTitle>
                </div>
              </SheetHeader>
              <nav class="p-4 space-y-2 flex-1 overflow-y-auto">
                <router-link
                  to="/"
                  @click="isMobileOpen = false"
                  class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-xs font-medium text-foreground hover:bg-muted/60"
                >
                  <Bus class="w-4 h-4 text-primary" />
                  <span>Tìm & Đặt vé</span>
                </router-link>
                <router-link
                  to="/my-bookings"
                  @click="isMobileOpen = false"
                  class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-xs font-medium text-foreground hover:bg-muted/60"
                >
                  <Ticket class="w-4 h-4 text-primary" />
                  <span>Vé của tôi</span>
                </router-link>

                <div v-if="auth.isOperator" class="pt-2 border-t border-border">
                  <router-link
                    to="/operator"
                    @click="isMobileOpen = false"
                    class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-xs font-medium text-emerald-700 dark:text-emerald-400 bg-emerald-500/10"
                  >
                    <Building2 class="w-4 h-4" />
                    <span>Portal Nhà xe</span>
                  </router-link>
                </div>

                <div v-if="auth.isAdmin" class="pt-2 border-t border-border">
                  <router-link
                    to="/admin"
                    @click="isMobileOpen = false"
                    class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-xs font-medium text-primary bg-primary/10"
                  >
                    <Shield class="w-4 h-4" />
                    <span>Portal Quản trị Admin</span>
                  </router-link>
                </div>
              </nav>
            </SheetContent>
          </Sheet>
        </div>
      </div>
    </header>

    <!-- Main Content Area with dynamic container class -->
    <main class="flex-1">
      <div :class="containerClass">
        <router-view />
      </div>
    </main>

    <!-- Footer (Claude+ Warm Minimalist Clean Footer) -->
    <footer class="bg-card text-card-foreground pt-10 pb-8 border-t border-border">
      <div :class="['px-4', containerClass]">
        <div class="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8 text-xs">
          <div class="space-y-3">
            <div class="flex items-center gap-2">
              <span class="w-7 h-7 rounded-lg bg-primary flex items-center justify-center text-primary-foreground font-black text-xs">
                VB
              </span>
              <span class="text-base font-bold tracking-tight text-foreground">VeloBus</span>
            </div>
            <p class="text-muted-foreground leading-relaxed">
              Sàn công nghệ vé xe khách liên tỉnh & giải pháp quản trị đội xe thông minh hàng đầu.
            </p>
          </div>
          <div>
            <h4 class="font-semibold text-foreground mb-3 uppercase tracking-wider text-[11px]">Hành khách</h4>
            <ul class="space-y-2 text-muted-foreground">
              <li><router-link to="/" class="hover:text-primary transition-colors">Tìm chuyến xe</router-link></li>
              <li><router-link to="/my-bookings" class="hover:text-primary transition-colors">Tra cứu vé đã đặt</router-link></li>
              <li><router-link to="/login" class="hover:text-primary transition-colors">Đăng nhập tài khoản</router-link></li>
            </ul>
          </div>
          <div>
            <h4 class="font-semibold text-foreground mb-3 uppercase tracking-wider text-[11px]">Đối tác nhà xe</h4>
            <ul class="space-y-2 text-muted-foreground">
              <li><router-link to="/operator" class="hover:text-primary transition-colors">Portal quản trị đội xe</router-link></li>
              <li><router-link to="/operator/trips" class="hover:text-primary transition-colors">Lập lịch chuyến xe</router-link></li>
              <li><router-link to="/operator" class="hover:text-primary transition-colors">Ví ký quỹ Escrow</router-link></li>
            </ul>
          </div>
          <div>
            <h4 class="font-semibold text-foreground mb-3 uppercase tracking-wider text-[11px]">Bảo mật & Chuẩn</h4>
            <p class="text-muted-foreground leading-relaxed mb-3">
              Áp dụng thuật toán mã hóa HMAC-SHA256 trên vé điện tử QR Code và khóa phân tán Redis 10 phút.
            </p>
            <div class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded bg-muted text-[11px] font-medium text-foreground">
              <ShieldCheck class="w-3.5 h-3.5 text-primary" />
              <span>Fintech Escrow Secured</span>
            </div>
          </div>
        </div>

        <div class="border-t border-border pt-6 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-muted-foreground">
          <p>© 2026 VeloBus Platform. Chuẩn hóa kiến trúc Claude+ Design System.</p>
          <div class="flex items-center gap-4">
            <span class="hover:text-foreground cursor-pointer">Điều khoản dịch vụ</span>
            <span class="hover:text-foreground cursor-pointer">Chính sách bảo mật</span>
          </div>
        </div>
      </div>
    </footer>

    <!-- Runtime Theme Customizer Modal/Drawer -->
    <ThemeCustomizer v-model="isThemeCustomizerOpen" :show-floating-trigger="false" />
  </div>
</template>
