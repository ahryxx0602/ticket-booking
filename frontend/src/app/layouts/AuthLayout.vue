<script setup>
import ThemeCustomizer from '@/shared/components/ThemeCustomizer.vue'
import { ShieldCheck, Bus, CheckCircle2 } from 'lucide-vue-next'
</script>

<template>
  <div class="min-h-screen flex bg-background text-foreground transition-colors duration-200">
    <!-- Left Brand Panel (Desktop: 45% width, Claude+ Warm Minimalist Architecture) -->
    <div class="hidden lg:flex lg:w-[45%] bg-card border-r border-border text-foreground flex-col justify-between p-12 relative select-none">
      <!-- Top Brand Identity -->
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-xl bg-primary flex items-center justify-center font-black text-primary-foreground text-base shadow-xs">
          VB
        </div>
        <div>
          <h1 class="text-lg font-bold tracking-tight text-foreground">VeloBus</h1>
          <p class="text-xs text-muted-foreground">Enterprise Mobility SaaS Platform</p>
        </div>
      </div>

      <!-- Center Hero Message & Technical Pillars -->
      <div class="space-y-6 max-w-md">
        <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary/10 border border-primary/20 text-xs font-semibold text-primary">
          <span class="w-2 h-2 rounded-full bg-primary animate-pulse"></span>
          Hệ thống Đặt vé & Vận hành Chuyên nghiệp
        </div>

        <h2 class="text-2xl sm:text-3xl font-extrabold leading-snug tracking-tight text-foreground">
          Nền tảng đặt vé xe khách và quản trị đội xe thông minh
        </h2>

        <p class="text-sm text-muted-foreground leading-relaxed">
          Giải pháp toàn diện cho hành khách và đối tác vận tải: Khóa giữ chỗ phân tán thời gian thực, quản lý ví ký quỹ Escrow 4 số dư và kiểm soát hành trình liên tỉnh.
        </p>

        <!-- Feature Pill Indicators -->
        <div class="grid grid-cols-2 gap-3 pt-2 text-xs text-muted-foreground">
          <div class="flex items-center gap-2">
            <CheckCircle2 class="w-4 h-4 text-primary font-bold flex-shrink-0" />
            <span>Khóa ghế phân tán Redis</span>
          </div>
          <div class="flex items-center gap-2">
            <CheckCircle2 class="w-4 h-4 text-primary font-bold flex-shrink-0" />
            <span>Thanh toán VNPay Sandbox</span>
          </div>
          <div class="flex items-center gap-2">
            <CheckCircle2 class="w-4 h-4 text-primary font-bold flex-shrink-0" />
            <span>Vé điện tử QR HMAC-SHA256</span>
          </div>
          <div class="flex items-center gap-2">
            <CheckCircle2 class="w-4 h-4 text-primary font-bold flex-shrink-0" />
            <span>Ví ký quỹ Escrow minh bạch</span>
          </div>
        </div>
      </div>

      <!-- Bottom Security & Copyright note -->
      <div class="text-xs text-muted-foreground flex items-center justify-between border-t border-border pt-6">
        <span>© 2026 VeloBus Ecosystem</span>
        <span class="flex items-center gap-1.5 font-medium">
          <ShieldCheck class="w-4 h-4 text-primary" />
          Bảo mật cấp độ Enterprise
        </span>
      </div>
    </div>

    <!-- Right Form Container (Desktop: 55% width, Form container max-width 420px, vertically centered) -->
    <div class="flex-1 flex flex-col justify-center items-center p-6 sm:p-12">
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

    <!-- Runtime Theme Customizer -->
    <ThemeCustomizer />
  </div>
</template>
