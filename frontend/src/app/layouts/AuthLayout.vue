<script setup>
</script>

<template>
  <div class="min-h-screen flex bg-canvas text-text-primary">
    <!-- Left Brand Panel (Desktop: 45% width, Brand Navy background, rich mobility aesthetics) -->
    <div class="hidden lg:flex lg:w-[45%] bg-brand-navy text-white flex-col justify-between p-12 relative overflow-hidden select-none">
      <!-- Background subtle gradient & glow -->
      <div class="absolute inset-0 bg-gradient-to-br from-[#142B4A] via-[#1E3A5F] to-[#0D1E35] opacity-95"></div>
      
      <!-- Top Brand Logo -->
      <div class="relative z-10 flex items-center gap-3">
        <div class="w-10 h-10 rounded-xl bg-primary-600 flex items-center justify-center font-black text-white text-lg shadow-md">
          VB
        </div>
        <div>
          <h1 class="text-xl font-bold tracking-tight">VeloBus</h1>
          <p class="text-xs text-blue-200">Premium Mobility SaaS Platform</p>
        </div>
      </div>

      <!-- Center Hero Message & Metrics -->
      <div class="relative z-10 space-y-6 max-w-md">
        <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/10 border border-white/20 text-xs text-blue-100">
          <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
          Hệ thống Đặt vé & Vận hành Chuyên nghiệp
        </div>

        <h2 class="text-3xl font-extrabold leading-snug tracking-tight">
          Nền tảng đặt vé xe khách và quản trị đội xe thông minh
        </h2>

        <p class="text-sm text-slate-300 leading-relaxed">
          Giải pháp toàn diện cho hành khách và đối tác vận tải: Giữ chỗ phân tán thời gian thực, quản lý ví ký quỹ Escrow 4 số dư và kiểm soát hành trình liên tỉnh.
        </p>

        <!-- Feature Pill Indicators -->
        <div class="grid grid-cols-2 gap-3 pt-2 text-xs text-slate-300">
          <div class="flex items-center gap-2">
            <span class="text-emerald-400 font-bold">✓</span> Khóa ghế phân tán Redis
          </div>
          <div class="flex items-center gap-2">
            <span class="text-emerald-400 font-bold">✓</span> Thanh toán VNPay Sandbox
          </div>
          <div class="flex items-center gap-2">
            <span class="text-emerald-400 font-bold">✓</span> Vé điện tử QR HMAC-SHA256
          </div>
          <div class="flex items-center gap-2">
            <span class="text-emerald-400 font-bold">✓</span> Ví ký quỹ Escrow minh bạch
          </div>
        </div>
      </div>

      <!-- Bottom Copyright & Security note -->
      <div class="relative z-10 text-xs text-slate-400 flex items-center justify-between border-t border-slate-700/60 pt-6">
        <span>© 2026 VeloBus Ecosystem</span>
        <span class="flex items-center gap-1.5">
          <svg class="w-3.5 h-3.5 text-emerald-400" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
          </svg>
          Mã hóa bảo mật 256-bit
        </span>
      </div>
    </div>

    <!-- Right Form Container (Desktop: 55% width, Form container max-width 420px, vertically centered) -->
    <div class="flex-1 flex flex-col justify-center items-center p-6 sm:p-12">
      <div class="w-full max-w-[420px]">
        <!-- Mobile Logo Header -->
        <div class="lg:hidden flex items-center justify-center gap-2.5 mb-8">
          <div class="w-9 h-9 rounded-xl bg-brand-navy flex items-center justify-center font-bold text-white text-base">
            VB
          </div>
          <span class="text-2xl font-bold text-brand-navy">VeloBus</span>
        </div>

        <!-- Form Slot (e.g. LoginView.vue) -->
        <router-view />

        <!-- Footer link back to Home -->
        <div class="mt-8 pt-6 border-t border-border-default text-center text-xs text-text-muted">
          <router-link to="/" class="hover:text-primary-600 font-medium">
            ← Quay về Trang chủ tìm vé
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>
