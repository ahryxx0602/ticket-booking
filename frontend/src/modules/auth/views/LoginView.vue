<template>
  <div class="max-w-md mx-auto my-12 bg-white p-8 rounded-2xl border border-slate-200 shadow-sm space-y-6">
    <!-- Header -->
    <div>
      <h1 class="text-2xl font-bold text-slate-900">Đăng nhập VeloBus</h1>
      <p class="text-xs text-slate-500 mt-1">Hệ thống nền tảng vé xe công nghệ & vận tải liên tỉnh</p>
    </div>

    <!-- Reason notification alert -->
    <div
      v-if="reasonMessage"
      class="p-3 rounded-xl bg-amber-50 border border-amber-200 flex items-start gap-2.5 text-xs text-amber-800"
    >
      <i class="pi pi-exclamation-triangle text-amber-600 mt-0.5 text-sm"></i>
      <span>{{ reasonMessage }}</span>
    </div>

    <!-- Main Login Form -->
    <form class="space-y-4" @submit.prevent="onSubmit">
      <div>
        <label class="block text-xs font-semibold text-slate-700 mb-1">Tên đăng nhập</label>
        <input
          v-model="username"
          type="text"
          placeholder="Nhập tên đăng nhập của bạn"
          required
          autocomplete="username"
          class="w-full px-3.5 py-2.5 rounded-lg border border-slate-300 text-sm focus:outline-none focus:border-blue-600 focus:ring-1 focus:ring-blue-600 transition-all"
        />
      </div>

      <div>
        <label class="block text-xs font-semibold text-slate-700 mb-1">Mật khẩu</label>
        <input
          v-model="password"
          type="password"
          placeholder="••••••••"
          required
          autocomplete="current-password"
          class="w-full px-3.5 py-2.5 rounded-lg border border-slate-300 text-sm focus:outline-none focus:border-blue-600 focus:ring-1 focus:ring-blue-600 transition-all"
        />
      </div>

      <div v-if="errorMsg" class="p-2.5 rounded-lg bg-rose-50 border border-rose-200 text-xs text-rose-600 font-medium flex items-center gap-2">
        <i class="pi pi-times-circle text-rose-500"></i>
        <span>{{ errorMsg }}</span>
      </div>

      <button
        type="submit"
        :disabled="loading"
        class="w-full py-2.5 rounded-lg bg-blue-600 text-white font-semibold text-sm hover:bg-blue-700 active:bg-blue-800 transition-all disabled:opacity-50 shadow-sm flex items-center justify-center gap-2"
      >
        <i v-if="loading" class="pi pi-spin pi-spinner text-sm"></i>
        <span>{{ loading ? 'Đang xác thực hệ thống...' : 'Đăng nhập' }}</span>
      </button>
    </form>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/modules/auth/store'
import { resolveSafeRedirect } from '@/shared/utils/redirect'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMsg = ref('')

const reasonMessage = computed(() => {
  if (route.query.reason === 'session_expired') {
    return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại để tiếp tục.'
  }
  if (route.query.reason === 'auth_required') {
    return 'Vui lòng đăng nhập để tiếp tục thực hiện tác vụ này.'
  }
  return ''
})

async function onSubmit() {
  loading.value = true
  errorMsg.value = ''
  try {
    await auth.login(username.value, password.value)
    
    // Tự động kiểm tra và chuyển hướng an toàn
    const rawRedirect = route.query.redirect
    const safeTarget = resolveSafeRedirect(rawRedirect, auth.role, router)
    if (safeTarget) {
      await router.push(safeTarget)
    } else {
      await router.push(auth.getHomeRoute())
    }
  } catch (err) {
    errorMsg.value = err?.message || 'Tên đăng nhập hoặc mật khẩu không chính xác'
  } finally {
    loading.value = false
  }
}
</script>
