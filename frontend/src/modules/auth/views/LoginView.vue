<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/modules/auth/store'
import { resolveSafeRedirect } from '@/shared/utils/redirect'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card'
import { Input, PasswordInput } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { AlertCircle, AlertTriangle, Lock, User, LogIn } from 'lucide-vue-next'

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

      <!-- Main Login Form -->
      <form class="space-y-4" @submit.prevent="onSubmit">
        <div class="space-y-1.5">
          <label class="block text-xs font-semibold text-foreground">Tên đăng nhập</label>
          <Input
            v-model="username"
            type="text"
            placeholder="Nhập tên đăng nhập của bạn"
            required
            autocomplete="username"
            class="h-10 text-xs"
          />
        </div>

        <div class="space-y-1.5">
          <label class="block text-xs font-semibold text-foreground">Mật khẩu</label>
          <PasswordInput
            v-model="password"
            placeholder="••••••••"
            required
            autocomplete="current-password"
            class="h-10 text-xs"
          />
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
      <p class="text-[11px] text-muted-foreground">
        Tài khoản mẫu: <span class="font-mono font-medium text-foreground">admin</span> (Admin) hoặc <span class="font-mono font-medium text-foreground">operator1</span> (Nhà xe)
      </p>
    </CardFooter>
  </Card>
</template>
