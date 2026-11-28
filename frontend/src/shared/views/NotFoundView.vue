<template>
  <div class="min-h-[70vh] flex items-center justify-center p-6 text-center">
    <div class="max-w-md space-y-5 bg-card border border-border rounded-2xl p-8 shadow-sm">
      <div class="w-16 h-16 mx-auto rounded-2xl bg-primary/10 border border-primary/20 flex items-center justify-center text-primary text-3xl font-extrabold">
        404
      </div>
      <div class="space-y-2">
        <h2 class="text-xl font-bold text-foreground">Không Tìm Thấy Trang Yêu Cầu</h2>
        <p class="text-sm text-muted-foreground">
          Đường dẫn bạn vừa truy cập không tồn tại, đã bị xóa hoặc bạn không có đủ thẩm quyền truy cập.
        </p>
      </div>
      <div class="pt-2 flex flex-col sm:flex-row items-center justify-center gap-3">
        <AppButton variant="primary" @click="$router.push('/')">
          <Home class="w-4 h-4 mr-1 inline" />
          <span>Về Trang Chủ</span>
        </AppButton>
        <AppButton variant="secondary" @click="$router.back()">
          <ArrowLeft class="w-4 h-4 mr-1 inline" />
          <span>Quay Lại</span>
        </AppButton>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Home, ArrowLeft } from 'lucide-vue-next'
import AppButton from '@/shared/components/AppButton.vue'
</script>
