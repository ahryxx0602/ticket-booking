<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  User as UserIcon,
  Shield,
  Send,
  Info,
  Loader2,
  AlertTriangle,
  RefreshCw,
  KeyRound
} from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Separator } from '@/components/ui/separator'
import TelegramLinkModal from '@/shared/components/TelegramLinkModal.vue'
import { useAuthStore } from '@/modules/auth/store'
import { authApi } from '@/modules/auth/api'

const auth = useAuthStore()
const showTelegramModal = ref(false)

const isLoading = ref(false)
const loadError = ref('')

const profileData = ref({
  username: '',
  role: '',
  customRole: null,
  roleName: '',
  permissions: []
})

// D1-PRE-05: Fetch actual authenticated identity from GET /api/v1/auth/me
async function fetchUserProfile() {
  isLoading.value = true
  loadError.value = ''
  try {
    const res = await authApi.getMe()
    const data = res?.data?.data !== undefined ? res.data.data : res?.data
    if (!data || !data.username) {
      throw new Error('Không nhận được thông tin phiên đăng nhập hợp lệ.')
    }
    profileData.value = {
      username: data.username,
      role: data.role || auth.role || 'ROLE_CUSTOMER',
      customRole: data.customRole || null,
      roleName: data.roleName || '',
      permissions: Array.isArray(data.permissions) ? data.permissions : []
    }
  } catch (err) {
    const status = err?.response?.status
    if (status === 401) {
      loadError.value = 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
    } else {
      loadError.value = err?.response?.data?.message || err?.message || 'Không thể tải thông tin hồ sơ tài khoản.'
    }
    // Fallback to store values if available, without fabricated personal info
    if (auth.user) {
      profileData.value = {
        username: auth.user.username || auth.user.sub || 'authenticated_user',
        role: auth.role || auth.user.role || 'ROLE_CUSTOMER',
        customRole: null,
        roleName: '',
        permissions: []
      }
    }
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  fetchUserProfile()
})

const userInitials = computed(() => {
  const name = profileData.value.username || 'U'
  return name.charAt(0).toUpperCase()
})

const formattedRole = computed(() => {
  const role = profileData.value.role
  switch (role) {
    case 'ROLE_ADMIN':
    case 'ADMIN':
      return 'Quản Trị Viên Sàn (Admin)'
    case 'ROLE_OPERATOR':
    case 'OPERATOR':
      return 'Đối Tác Nhà Xe (Operator)'
    case 'ROLE_CUSTOMER':
    case 'CUSTOMER':
      return 'Khách Hàng (Customer)'
    default:
      return profileData.value.roleName || role || 'Người Dùng'
  }
})
</script>

<template>
  <div class="max-w-4xl mx-auto space-y-6">
    <!-- Page Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold tracking-tight text-foreground flex items-center gap-2">
          <UserIcon class="w-6 h-6 text-primary" />
          <span>Hồ Sơ Cá Nhân & Bảo Mật Tài Khoản</span>
        </h1>
        <p class="text-xs text-muted-foreground mt-1">
          Xem thông tin định danh tài khoản đăng nhập hiện tại và kết nối thông báo tự động qua Bot Telegram.
        </p>
      </div>

      <Button
        variant="outline"
        size="sm"
        class="h-9 gap-1.5 self-start sm:self-auto"
        :disabled="isLoading"
        @click="fetchUserProfile"
      >
        <RefreshCw class="w-3.5 h-3.5" :class="{ 'animate-spin': isLoading }" />
        <span>Tải lại</span>
      </Button>
    </div>

    <!-- Error State -->
    <Alert v-if="loadError" variant="destructive" class="py-3">
      <AlertTriangle class="w-4 h-4" />
      <AlertTitle class="text-xs font-semibold">Lỗi xác thực hồ sơ</AlertTitle>
      <AlertDescription class="text-xs flex items-center justify-between gap-4 mt-1">
        <span>{{ loadError }}</span>
        <Button variant="outline" size="sm" class="h-7 text-xs bg-background shrink-0" @click="fetchUserProfile">
          Thử Lại
        </Button>
      </AlertDescription>
    </Alert>

    <!-- Loading State -->
    <div v-if="isLoading && !profileData.username" class="h-64 flex flex-col items-center justify-center gap-2">
      <Loader2 class="w-8 h-8 animate-spin text-primary" />
      <p class="text-xs text-muted-foreground">Đang xác thực thông tin tài khoản...</p>
    </div>

    <!-- Honest Read-Only Notice (D1-PRE-05: Concise, no technical jargon) -->
    <Alert class="border-border bg-muted/30 py-3">
      <Info class="w-4 h-4 text-primary" />
      <AlertTitle class="text-xs font-semibold text-foreground">Thông tin tài khoản xác thực (Chế độ chỉ đọc)</AlertTitle>
      <AlertDescription class="text-xs text-muted-foreground mt-0.5 leading-relaxed">
        Thông tin định danh và phân quyền được cấp phát trực tiếp theo phiên đăng nhập. Chức năng cập nhật thông tin cá nhân bổ sung đang được hoàn thiện.
      </AlertDescription>
    </Alert>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <!-- Cột Trái: Avatar & Định Danh Thực Tế -->
      <Card class="shadow-sm text-center">
        <CardContent class="pt-6 space-y-4">
          <div class="w-20 h-20 mx-auto rounded-full bg-primary/10 border-2 border-primary flex items-center justify-center text-primary text-2xl font-bold">
            {{ userInitials }}
          </div>

          <div>
            <h3 class="text-base font-bold text-foreground font-mono">
              @{{ profileData.username || '...' }}
            </h3>
            <p class="text-xs text-muted-foreground mt-0.5">
              {{ formattedRole }}
            </p>
          </div>

          <Separator />

          <div class="space-y-2.5 text-left text-xs">
            <div class="flex items-center justify-between">
              <span class="text-muted-foreground">Mã quyền hạn:</span>
              <Badge variant="outline" class="font-mono text-[11px] text-primary border-primary/30">
                {{ profileData.role || 'ROLE_CUSTOMER' }}
              </Badge>
            </div>

            <div v-if="profileData.customRole" class="flex items-center justify-between">
              <span class="text-muted-foreground">Vai trò tùy biến:</span>
              <span class="font-semibold text-foreground">{{ profileData.roleName || profileData.customRole }}</span>
            </div>

            <div v-if="profileData.permissions.length > 0" class="pt-1">
              <span class="text-muted-foreground block mb-1">Quyền tác vụ ({{ profileData.permissions.length }}):</span>
              <div class="flex flex-wrap gap-1 max-h-24 overflow-y-auto">
                <Badge
                  v-for="p in profileData.permissions.slice(0, 6)"
                  :key="p"
                  variant="secondary"
                  class="text-[10px] font-mono px-1.5 py-0"
                >
                  {{ p }}
                </Badge>
                <span v-if="profileData.permissions.length > 6" class="text-[10px] text-muted-foreground">
                  +{{ profileData.permissions.length - 6 }} quyền khác
                </span>
              </div>
            </div>
          </div>

          <Separator />

          <div class="pt-1">
            <Button
              type="button"
              variant="outline"
              size="sm"
              class="w-full text-xs gap-1.5 font-semibold text-sky-600 dark:text-sky-400 border-sky-300 dark:border-sky-800 hover:bg-sky-50 dark:hover:bg-sky-950/30"
              @click="showTelegramModal = true"
            >
              <Send class="w-3.5 h-3.5" />
              <span>Liên Kết Telegram Bot</span>
            </Button>
          </div>
        </CardContent>
      </Card>

      <!-- Cột Phải: Thông tin định danh chi tiết (Read-only, no misleading forms) -->
      <div class="md:col-span-2 space-y-6">
        <Card class="shadow-sm">
          <CardHeader class="pb-3 border-b border-border bg-muted/20">
            <CardTitle class="text-sm font-semibold text-foreground flex items-center gap-2">
              <Shield class="w-4 h-4 text-primary" />
              <span>Chi Tiết Tài Khoản Đăng Nhập</span>
            </CardTitle>
            <CardDescription class="text-xs text-muted-foreground">
              Dữ liệu thuộc tính người dùng được trích xuất từ phiên xác thực hiện thời.
            </CardDescription>
          </CardHeader>

          <CardContent class="pt-4 space-y-4">
            <div class="space-y-3">
              <div class="space-y-1">
                <span class="text-xs font-semibold text-foreground">Tên đăng nhập (Username)</span>
                <div class="px-3 py-2 rounded-lg border border-input bg-muted/30 text-xs font-mono text-foreground">
                  {{ profileData.username || '—' }}
                </div>
              </div>

              <div class="space-y-1">
                <span class="text-xs font-semibold text-foreground">Vai trò hệ thống</span>
                <div class="px-3 py-2 rounded-lg border border-input bg-muted/30 text-xs text-foreground flex items-center justify-between">
                  <span>{{ formattedRole }}</span>
                  <span class="text-[11px] font-mono text-muted-foreground">{{ profileData.role }}</span>
                </div>
              </div>

              <!-- D1-PRE-05: Honest handling of unavailable contact fields -->
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-1">
                <div class="space-y-1">
                  <span class="text-xs font-semibold text-foreground">Họ và tên</span>
                  <div class="px-3 py-2 rounded-lg border border-input bg-muted/20 text-xs text-muted-foreground italic">
                    Chưa liên kết trong phiên
                  </div>
                </div>

                <div class="space-y-1">
                  <span class="text-xs font-semibold text-foreground">Địa chỉ email</span>
                  <div class="px-3 py-2 rounded-lg border border-input bg-muted/20 text-xs text-muted-foreground italic">
                    Chưa liên kết trong phiên
                  </div>
                </div>
              </div>
            </div>

            <!-- Bảo mật tài khoản thông báo ngắn gọn -->
            <div class="pt-3 border-t border-border space-y-2">
              <div class="flex items-center gap-2 text-xs font-semibold text-foreground">
                <KeyRound class="w-3.5 h-3.5 text-primary" />
                <span>Bảo Mật Mật Khẩu</span>
              </div>
              <p class="text-xs text-muted-foreground leading-relaxed">
                Mật khẩu đăng nhập được bảo vệ an toàn trên hệ thống máy chủ. Để đảm bảo an ninh, tính năng thay đổi mật khẩu trực tiếp qua giao diện web sẽ được bổ sung sau khi kích hoạt xác thực hai yếu tố.
              </p>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>

    <!-- Modal Liên Kết Telegram -->
    <TelegramLinkModal
      v-if="showTelegramModal"
      @close="showTelegramModal = false"
    />
  </div>
</template>
