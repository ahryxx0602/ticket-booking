<template>
  <div class="min-h-[70vh] flex items-center justify-center p-6 text-center">
    <div class="max-w-md space-y-5 bg-white border border-slate-200 rounded-2xl p-8 shadow-sm">
      <div class="w-16 h-16 mx-auto rounded-2xl bg-blue-50 border border-blue-200 flex items-center justify-center text-blue-600 text-3xl font-extrabold">
        404
      </div>
      <div class="space-y-2">
        <h2 class="text-xl font-bold text-slate-900">Không Tìm Thấy Trang Yêu Cầu</h2>
        <p class="text-sm text-slate-500">
          Đường dẫn bạn vừa truy cập không tồn tại, đã bị xóa hoặc bạn không có đủ thẩm quyền truy cập.
        </p>
      </div>
      <div class="pt-2 flex flex-col sm:flex-row items-center justify-center gap-3">
        <AppButton variant="primary" @click="$router.push('/')">
          <i class="pi pi-home mr-1"></i>
          <span>Về Trang Chủ</span>
        </AppButton>
        <AppButton variant="secondary" @click="$router.back()">
          <i class="pi pi-arrow-left mr-1"></i>
          <span>Quay Lại</span>
        </AppButton>
      </div>
    </div>
  </div>
</template>

<script setup>
import AppButton from '@/shared/components/AppButton.vue'
</script>
