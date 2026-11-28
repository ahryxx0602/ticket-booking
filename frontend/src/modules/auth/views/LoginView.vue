<script setup>
import { ref, computed, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/modules/auth/store'
import { resolveSafeRedirect } from '@/shared/utils/redirect'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card'
import { Input, PasswordInput } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { AlertCircle, AlertTriangle } from 'lucide-vue-next'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMsg = ref('')

const errors = ref({
  username: '',
  password: ''
})

const usernameInput = ref(null)
const passwordInput = ref(null)

// B2: Demo credentials only shown in DEV builds — never in production
const isDev = import.meta.env.DEV

const reasonMessage = computed(() => {
  if (route.query.reason === 'session_expired') {
    return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại để tiếp tục.'
  }
  if (route.query.reason === 'auth_required') {
    return 'Vui lòng đăng nhập để tiếp tục thực hiện tác vụ này.'
  }
  return ''
})

function validateForm() {
  errors.value.username = ''
  errors.value.password = ''
  let isValid = true

  if (!username.value || !username.value.trim()) {
    errors.value.username = 'Vui lòng nhập tên đăng nhập'
    isValid = false
  }

  if (!password.value || !password.value.trim()) {
    errors.value.password = 'Vui lòng nhập mật khẩu'
    isValid = false
  }

  if (!isValid) {
    nextTick(() => {
      if (errors.value.username) {
        // Focus native input inside Input component or DOM element
        const el = document.getElementById('username')
        el?.focus()
      } else if (errors.value.password) {
        const el = document.getElementById('password')
        el?.focus()
      }
    })
  }

  return isValid
}

async function onSubmit() {
  if (loading.value) return

  errorMsg.value = ''
  if (!validateForm()) {
    return
  }

  loading.value = true
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
    const status = err?.response?.status
    const message = err?.response?.data?.message || err?.message || ''

    if (status === 401 || message.toLowerCase().includes('bad credentials') || message.toLowerCase().includes('không chính xác')) {
      errorMsg.value = 'Tên đăng nhập hoặc mật khẩu không chính xác.'
    } else if (err?.code === 'ERR_NETWORK' || !err?.response) {
      errorMsg.value = 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra đường truyền mạng.'
    } else if (status >= 500) {
      errorMsg.value = 'Đã có lỗi xảy ra từ máy chủ hệ thống. Vui lòng thử lại sau.'
    } else {
      errorMsg.value = message || 'Đăng nhập không thành công. Vui lòng thử lại.'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <Card class="w-full max-w-md mx-auto shadow-md border-border bg-card">
    <!-- Header -->
    <CardHeader class="space-y-1.5 pb-4">
      <CardTitle class="text-xl sm:text-2xl font-bold tracking-tight text-foreground">
        Đăng nhập VeloBus
      </CardTitle>
      <CardDescription class="text-xs text-muted-foreground">
        Hệ thống nền tảng vé xe công nghệ & vận tải liên tỉnh
      </CardDescription>
    </CardHeader>

    <CardContent class="space-y-4">
      <!-- Reason notification alert -->
      <Alert
        v-if="reasonMessage"
        class="border-amber-500/30 bg-amber-500/10 text-amber-800 dark:text-amber-300 py-2.5 px-3"
      >
        <AlertTriangle class="h-4 w-4 text-amber-600 dark:text-amber-400" />
        <AlertDescription class="text-xs">
          {{ reasonMessage }}
        </AlertDescription>
      </Alert>

      <!-- Main Login Form (novalidate to suppress native browser tooltip in favor of inline Vietnamese feedback) -->
      <form class="space-y-4" novalidate @submit.prevent="onSubmit">
        <div class="space-y-1.5">
          <label for="username" class="block text-xs font-semibold text-foreground">
            Tên đăng nhập <span class="text-status-danger">*</span>
          </label>
          <Input
            id="username"
            name="username"
            v-model="username"
            type="text"
            placeholder="Nhập tên đăng nhập của bạn"
            autocomplete="username"
            class="h-10 text-xs"
            :aria-invalid="!!errors.username"
            :aria-describedby="errors.username ? 'username-error' : undefined"
            :class="errors.username ? 'border-status-danger focus-visible:ring-status-danger' : ''"
          />
          <p v-if="errors.username" id="username-error" class="text-[11px] text-status-danger font-medium mt-1">
            {{ errors.username }}
          </p>
        </div>

        <div class="space-y-1.5">
          <label for="password" class="block text-xs font-semibold text-foreground">
            Mật khẩu <span class="text-status-danger">*</span>
          </label>
          <PasswordInput
            id="password"
            name="password"
            v-model="password"
            placeholder="••••••••"
            autocomplete="current-password"
            class="h-10 text-xs"
            :aria-invalid="!!errors.password"
            :aria-describedby="errors.password ? 'password-error' : undefined"
            :class="errors.password ? 'border-status-danger focus-visible:ring-status-danger' : ''"
          />
          <p v-if="errors.password" id="password-error" class="text-[11px] text-status-danger font-medium mt-1">
            {{ errors.password }}
          </p>
        </div>

        <!-- Error alert -->
        <Alert
          v-if="errorMsg"
          variant="destructive"
          class="py-2.5 px-3"
        >
          <AlertCircle class="h-4 w-4 text-status-danger" />
          <AlertDescription class="text-xs text-status-danger font-medium">
            {{ errorMsg }}
          </AlertDescription>
        </Alert>

        <Button
          type="submit"
          :disabled="loading"
          :loading="loading"
          class="w-full h-10 text-xs font-semibold shadow-xs"
        >
          <span>{{ loading ? 'Đang xác thực hệ thống...' : 'Đăng nhập' }}</span>
        </Button>
      </form>
    </CardContent>

    <CardFooter class="pt-0 pb-4 text-center justify-center">
      <!-- B2: Demo account info only shown in DEV environment -->
      <p v-if="isDev" class="text-[11px] text-muted-foreground">
        Tài khoản mẫu: <span class="font-mono font-medium text-foreground">admin</span> (Admin) hoặc <span class="font-mono font-medium text-foreground">operator1</span> (Nhà xe)
      </p>
    </CardFooter>
  </Card>
</template>
