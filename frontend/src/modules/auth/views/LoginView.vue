<template>
  <div class="max-w-md mx-auto my-12 bg-white p-8 rounded-2xl border border-slate-200 shadow-sm space-y-5">
    <div>
      <h1 class="text-2xl font-bold text-slate-900">Đăng nhập VeloBus</h1>
      <p class="text-xs text-slate-500 mt-1">Sử dụng tài khoản mẫu hoặc tài khoản đã đăng ký</p>
    </div>

    <div class="grid grid-cols-3 gap-2 text-xs">
      <button
        type="button"
        class="px-2.5 py-1.5 rounded-lg border border-slate-200 hover:bg-slate-50 font-medium"
        @click="fillSeed('customer1', 'customer_dev_password')"
      >
        Khách hàng
      </button>
      <button
        type="button"
        class="px-2.5 py-1.5 rounded-lg border border-slate-200 hover:bg-slate-50 font-medium"
        @click="fillSeed('operator1', 'operator_dev_password')"
      >
        Nhà xe
      </button>
      <button
        type="button"
        class="px-2.5 py-1.5 rounded-lg border border-slate-200 hover:bg-slate-50 font-medium"
        @click="fillSeed('admin', 'admin_dev_password')"
      >
        Admin Sàn
      </button>
    </div>

    <form class="space-y-4" @submit.prevent="onSubmit">
      <div>
        <label class="block text-xs font-semibold text-slate-700 mb-1">Tên đăng nhập</label>
        <input
          v-model="username"
          type="text"
          required
          class="w-full px-3.5 py-2 rounded-lg border border-slate-300 text-sm focus:outline-none focus:border-blue-600"
        />
      </div>
      <div>
        <label class="block text-xs font-semibold text-slate-700 mb-1">Mật khẩu</label>
        <input
          v-model="password"
          type="password"
          required
          class="w-full px-3.5 py-2 rounded-lg border border-slate-300 text-sm focus:outline-none focus:border-blue-600"
        />
      </div>
      <p v-if="errorMsg" class="text-xs text-rose-600 font-medium">{{ errorMsg }}</p>
      <button
        type="submit"
        :disabled="loading"
        class="w-full py-2.5 rounded-lg bg-blue-600 text-white font-semibold text-sm hover:bg-blue-700 disabled:opacity-50"
      >
        {{ loading ? 'Đang xác thực...' : 'Đăng nhập' }}
      </button>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/modules/auth/store'

const router = useRouter()
const auth = useAuthStore()

const username = ref('customer1')
const password = ref('customer_dev_password')
const loading = ref(false)
const errorMsg = ref('')

function fillSeed(u, p) {
  username.value = u
  password.value = p
}

async function onSubmit() {
  loading.value = true
  errorMsg.value = ''
  try {
    await auth.login(username.value, password.value)
    if (auth.isAdmin) router.push('/admin')
    else if (auth.isOperator) router.push('/operator')
    else router.push('/')
  } catch (err) {
    errorMsg.value = err?.message || 'Sai thông tin đăng nhập'
  } finally {
    loading.value = false
  }
}
</script>
