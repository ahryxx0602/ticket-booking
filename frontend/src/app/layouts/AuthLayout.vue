<script setup>
import { ref } from 'vue'
import ThemeCustomizer from '@/shared/components/ThemeCustomizer.vue'
import { Button } from '@/components/ui/button'
import { ShieldCheck, MapPin, Ticket, CalendarCheck, Paintbrush } from 'lucide-vue-next'

const isThemeCustomizerOpen = ref(false)
</script>

<template>
  <div class="min-h-screen flex bg-background text-foreground transition-colors duration-200 relative">
    <!-- Left Brand Panel (Desktop: 45% width, Claude+ Warm Minimalist) -->
    <div class="hidden lg:flex lg:w-[45%] bg-card border-r border-border text-foreground flex-col justify-between p-12 relative select-none">
      <!-- Top Brand Identity -->
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-xl bg-primary flex items-center justify-center font-black text-primary-foreground text-base shadow-xs">
          VB
        </div>
        <div>
          <h1 class="text-lg font-bold tracking-tight text-foreground">VeloBus</h1>
          <p class="text-xs text-muted-foreground">Nền tảng vé xe khách liên tỉnh</p>
        </div>
      </div>

      <!-- Center Hero Message & Feature Pillars -->
      <div class="space-y-6 max-w-md">
        <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary/10 border border-primary/20 text-xs font-semibold text-primary">
          <span class="w-2 h-2 rounded-full bg-primary animate-pulse"></span>
          Đặt vé nhanh chóng & tiện lợi
        </div>

        <h2 class="text-2xl sm:text-3xl font-extrabold leading-snug tracking-tight text-foreground">
          Tìm chuyến xe, chọn chỗ ngồi và quản lý vé của bạn
        </h2>

        <p class="text-sm text-muted-foreground leading-relaxed">
          Nền tảng kết nối hành khách và đối tác vận tải: Tra cứu tuyến xe liên tỉnh, nhận vé điện tử, theo dõi hành trình và đối soát minh bạch.
        </p>

        <!-- Feature Pill Indicators -->
        <div class="grid grid-cols-2 gap-3 pt-2 text-xs text-muted-foreground">
          <div class="flex items-center gap-2">
            <MapPin class="w-4 h-4 text-primary flex-shrink-0" />
            <span>Tra cứu tuyến xe trực tuyến</span>
          </div>
          <div class="flex items-center gap-2">
            <Ticket class="w-4 h-4 text-primary flex-shrink-0" />
            <span>Vé điện tử QR xác thực</span>
          </div>
          <div class="flex items-center gap-2">
            <CalendarCheck class="w-4 h-4 text-primary flex-shrink-0" />
            <span>Quản lý lịch trình và hoàn vé</span>
          </div>
          <div class="flex items-center gap-2">
            <ShieldCheck class="w-4 h-4 text-primary flex-shrink-0" />
            <span>Thanh toán an toàn</span>
          </div>
        </div>
      </div>

      <!-- Bottom Copyright -->
      <div class="text-xs text-muted-foreground flex items-center justify-between border-t border-border pt-6">
        <span>© 2026 VeloBus</span>
        <span class="flex items-center gap-1.5 font-medium">
          <ShieldCheck class="w-4 h-4 text-primary" />
          Thanh toán bảo mật
        </span>
      </div>
    </div>

    <!-- Right Form Container (Desktop: 55% width, Form max-width 420px, vertically centered) -->
    <div class="flex-1 flex flex-col justify-center items-center p-6 sm:p-12 relative">
      <!-- Top Right Theme Customizer Toggle -->
      <div class="absolute top-4 right-4 sm:top-6 sm:right-6">
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
      </div>

      <div class="w-full max-w-[420px]">
        <!-- Mobile Logo Header -->
        <div class="lg:hidden flex items-center justify-center gap-2.5 mb-8">
          <div class="w-9 h-9 rounded-xl bg-primary flex items-center justify-center font-bold text-primary-foreground text-base shadow-xs">
            VB
          </div>
          <span class="text-2xl font-bold tracking-tight text-foreground">VeloBus</span>
        </div>

        <!-- Form Slot (e.g. LoginView.vue) -->
        <router-view />

        <!-- Footer link back to Home -->
        <div class="mt-8 pt-6 border-t border-border text-center text-xs text-muted-foreground">
          <router-link to="/" class="hover:text-primary font-medium transition-colors">
            ← Quay về Trang chủ tìm vé
          </router-link>
        </div>
      </div>
    </div>

    <!-- Runtime Theme Customizer Modal/Drawer -->
    <ThemeCustomizer v-model="isThemeCustomizerOpen" :show-floating-trigger="false" />
  </div>
</template>
