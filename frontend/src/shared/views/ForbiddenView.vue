<template>
  <div class="min-h-[70vh] flex items-center justify-center p-6 text-center">
    <div class="max-w-md w-full space-y-5 bg-white border border-rose-200 rounded-2xl p-8 shadow-sm">
      <div class="w-16 h-16 mx-auto rounded-2xl bg-rose-50 border border-rose-200 flex items-center justify-center text-rose-600 text-2xl font-black">
        <i class="pi pi-shield text-3xl"></i>
      </div>
      <div class="space-y-2">
        <span class="inline-block px-3 py-1 text-xs font-bold uppercase tracking-wider rounded-full bg-rose-100 text-rose-700">
          Mã Lỗi 403 • Quyền Hạn Bị Từ Chối
        </span>
        <h2 class="text-xl font-bold text-slate-900">Không Có Quyền Truy Cập</h2>
        <p class="text-sm text-slate-600 leading-relaxed">
          Tài khoản hiện tại của bạn (<span class="font-semibold text-slate-900">{{ roleDisplay }}</span>) không đủ thẩm quyền để xem hoặc thao tác trên phân hệ này.
        </p>
      </div>
      <div class="pt-2 flex flex-col sm:flex-row items-center justify-center gap-3">
        <AppButton variant="primary" @click="goHome">
          <i class="pi pi-home mr-1"></i>
          <span>{{ homeBtnLabel }}</span>
        </AppButton>
        <AppButton variant="secondary" @click="switchAccount">
          <i class="pi pi-sign-out mr-1"></i>
          <span>Đăng Nhập Tài Khoản Khác</span>
        </AppButton>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/modules/auth/store'
import AppButton from '@/shared/components/AppButton.vue'

const router = useRouter()
const auth = useAuthStore()

const roleDisplay = computed(() => {
  if (auth.isAdmin) return 'Quản Trị Viên (ROLE_ADMIN)'
  if (auth.isOperator) return 'Đối Tác Nhà Xe (ROLE_OPERATOR)'
  if (auth.isCustomer) return 'Khách Hàng (ROLE_CUSTOMER)'
  return 'Khách Vãng Lai (Chưa đăng nhập)'
})

const homeBtnLabel = computed(() => {
  if (auth.isAdmin) return 'Về Portal Quản Trị'
  if (auth.isOperator) return 'Về Portal Nhà Xe'
  return 'Về Trang Chủ Đặt Vé'
})

function goHome() {
  if (auth.isAdmin) router.push('/admin')
  else if (auth.isOperator) router.push('/operator')
  else router.push('/')
}

function switchAccount() {
  auth.logout()
  router.push('/login')
}
</script>
