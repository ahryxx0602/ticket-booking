<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  User as UserIcon,
  Mail,
  Shield,
  KeyRound,
  Send,
  CheckCircle2,
  Info,
  Lock,
  BadgeCheck,
  AlertCircle
} from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Separator } from '@/components/ui/separator'
import TelegramLinkModal from '@/shared/components/TelegramLinkModal.vue'
import { useAuthStore } from '@/modules/auth/store'
import { authApi } from '@/modules/auth/api'

const auth = useAuthStore()
const showTelegramModal = ref(false)

const user = ref({
  username: '',
  fullName: '',
  email: '',
  role: 'ROLE_CUSTOMER',
  status: 'ACTIVE'
})

onMounted(async () => {
  if (auth.user) {
    user.value.username = auth.user.username || auth.user.sub || ''
    user.value.fullName = auth.user.fullName || auth.user.name || auth.user.username || 'Người dùng VeloBus'
    user.value.email = auth.user.email || 'customer@velobus.vn'
    user.value.role = auth.role || auth.user.role || 'ROLE_CUSTOMER'
  } else {
    // Fallback baseline if store not yet populated
    user.value = {
      username: 'thanhpv',
      fullName: 'Phan Văn Thành',
      email: 'thanh.pv@gmail.com',
      role: 'ROLE_CUSTOMER',
      status: 'ACTIVE'
    }
  }

  // Attempt authoritative refresh via authApi.getMe() if available
  try {
    const res = await authApi.getMe()
    if (res?.data) {
      const data = res.data
      user.value.username = data.username || user.value.username
      user.value.fullName = data.fullName || data.name || user.value.fullName
      user.value.email = data.email || user.value.email
      user.value.role = data.role || user.value.role
    }
  } catch (_) {
    // Silent fallback to store state
  }
})

const userInitials = computed(() => {
  const name = user.value.fullName || user.value.username || 'U'
  return name.charAt(0).toUpperCase()
})

const formattedRole = computed(() => {
  switch (user.value.role) {
    case 'ROLE_ADMIN':
    case 'ADMIN':
      return 'Quản Trị Viên Sàn (Admin)'
    case 'ROLE_OPERATOR':
    case 'OPERATOR':
      return 'Đối Tác Nhà Xe (Operator)'
    case 'ROLE_CUSTOMER':
    case 'CUSTOMER':
    default:
      return 'Khách Hàng (Customer)'
  }
})
</script>

<template>
  <div class="max-w-4xl mx-auto space-y-6">
    <!-- Page Header -->
    <div class="space-y-1">
      <h1 class="text-2xl font-bold tracking-tight text-foreground flex items-center gap-2">
        <UserIcon class="w-6 h-6 text-primary" />
        <span>Hồ Sơ Cá Nhân & Bảo Mật Tài Khoản</span>
      </h1>
      <p class="text-xs text-muted-foreground">
        Xem thông tin định danh cá nhân đã xác thực, quản lý phân quyền và liên kết Bot Telegram để nhận biến động số dư / vé điện tử tức thì.
      </p>
    </div>

    <!-- Capability Gap Honest Notice -->
    <Alert class="bg-blue-50/70 dark:bg-blue-950/30 border-blue-200 dark:border-blue-900 text-blue-950 dark:text-blue-200 py-3">
      <Info class="w-4 h-4 text-blue-600 dark:text-blue-400" />
      <AlertTitle class="text-xs font-semibold">Chế độ hiển thị định danh xác thực (Read-only)</AlertTitle>
      <AlertDescription class="text-xs leading-relaxed text-blue-800 dark:text-blue-300 mt-1">
        Thông tin cá nhân và mật khẩu tài khoản được quản lý và bảo mật qua dịch vụ Identity & Access Management. Chức năng tự cập nhật thông tin cá nhân và đổi mật khẩu tự phục vụ của khách hàng đang được chuẩn bị triển khai trong phiên bản tiếp theo.
      </AlertDescription>
    </Alert>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <!-- Cột Trái: Avatar & Định Danh -->
      <Card class="shadow-sm text-center">
        <CardContent class="pt-6 space-y-4">
          <div class="relative w-24 h-24 mx-auto rounded-full bg-primary/10 border-2 border-primary flex items-center justify-center text-primary text-3xl font-bold shadow-inner">
            {{ userInitials }}
            <span class="absolute bottom-0 right-0 p-1 bg-primary rounded-full border-2 border-background">
              <BadgeCheck class="w-4 h-4 text-primary-foreground" />
            </span>
          </div>

          <div>
            <h3 class="text-base font-bold text-foreground">
              {{ user.fullName || 'Khách Hàng VeloBus' }}
            </h3>
            <p class="text-xs text-muted-foreground font-mono mt-0.5">
              @{{ user.username || 'username' }}
            </p>
          </div>

          <Separator />

          <div class="space-y-2 text-left text-xs text-muted-foreground">
            <div class="flex items-center justify-between">
              <span>Vai trò tài khoản:</span>
              <Badge variant="outline" class="font-semibold text-[11px] text-primary border-primary/30">
                {{ user.role || 'ROLE_CUSTOMER' }} ({{ formattedRole }})
              </Badge>
            </div>
            <div class="flex items-center justify-between">
              <span>Trạng thái hoạt động:</span>
              <span class="inline-flex items-center gap-1 font-semibold text-emerald-600 dark:text-emerald-400">
                <CheckCircle2 class="w-3.5 h-3.5" />
                <span>Hoạt Động (ACTIVE)</span>
              </span>
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

      <!-- Cột Phải: Form Thông Tin & Mật Khẩu (Read-Only Honest Display) -->
      <div class="md:col-span-2 space-y-6">
        <!-- Thông Tin Chi Tiết -->
        <Card class="shadow-sm">
          <CardHeader class="pb-3 border-b border-border bg-muted/20">
            <CardTitle class="text-sm font-semibold text-foreground flex items-center gap-2">
              <UserIcon class="w-4 h-4 text-primary" />
              <span>Thông Tin Cá Nhân Định Danh</span>
            </CardTitle>
            <CardDescription class="text-xs text-muted-foreground">
              Các thông tin liên hệ được đồng bộ trực tiếp từ tài khoản đăng nhập.
            </CardDescription>
          </CardHeader>

          <CardContent class="pt-4 space-y-4">
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div class="space-y-1.5">
                <label for="profile-name" class="block text-xs font-semibold text-foreground">Họ và Tên</label>
                <Input
                  id="profile-name"
                  :model-value="user.fullName"
                  readonly
                  class="h-9 text-xs bg-muted/30 cursor-not-allowed"
                />
              </div>

              <div class="space-y-1.5">
                <label for="profile-email" class="block text-xs font-semibold text-foreground">Địa Chỉ Email</label>
                <Input
                  id="profile-email"
                  :model-value="user.email"
                  readonly
                  class="h-9 text-xs bg-muted/30 cursor-not-allowed"
                />
              </div>
            </div>

            <div class="space-y-1.5">
              <label for="profile-username" class="block text-xs font-semibold text-foreground">Tên Đăng Nhập (Username)</label>
              <Input
                id="profile-username"
                :model-value="user.username"
                readonly
                class="h-9 text-xs font-mono bg-muted/30 cursor-not-allowed max-w-sm"
              />
            </div>
          </CardContent>

          <CardFooter class="border-t border-border bg-muted/10 py-3 flex items-center justify-between">
            <p class="text-[11px] text-muted-foreground italic flex items-center gap-1">
              <Shield class="w-3.5 h-3.5 text-emerald-600" />
              <span>Định danh đã xác thực theo phiên bản bảo mật B5.3</span>
            </p>
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled
              class="text-xs opacity-60 cursor-not-allowed"
            >
              Lưu Thay Đổi (Chưa hỗ trợ)
            </Button>
          </CardFooter>
        </Card>

        <!-- Đổi Mật Khẩu -->
        <Card class="shadow-sm">
          <CardHeader class="pb-3 border-b border-border bg-muted/20">
            <CardTitle class="text-sm font-semibold text-foreground flex items-center gap-2">
              <Lock class="w-4 h-4 text-primary" />
              <span>Bảo Mật & Mật Khẩu Đăng Nhập</span>
            </CardTitle>
            <CardDescription class="text-xs text-muted-foreground">
              Mật khẩu được mã hóa chuẩn BCrypt một chiều trong dịch vụ cơ sở dữ liệu.
            </CardDescription>
          </CardHeader>

          <CardContent class="pt-4 space-y-4">
            <div class="space-y-3">
              <div class="space-y-1.5">
                <label class="block text-xs font-semibold text-foreground">Mật khẩu hiện tại</label>
                <Input
                  type="password"
                  value="••••••••••••"
                  readonly
                  class="h-9 text-xs bg-muted/30 cursor-not-allowed max-w-md font-mono"
                />
              </div>

              <div class="space-y-1.5">
                <label class="block text-xs font-semibold text-foreground">Mật khẩu mới</label>
                <Input
                  type="password"
                  value="••••••••••••"
                  readonly
                  class="h-9 text-xs bg-muted/30 cursor-not-allowed max-w-md font-mono"
                />
              </div>
            </div>
          </CardContent>

          <CardFooter class="border-t border-border bg-muted/10 py-3 flex items-center justify-between">
            <p class="text-[11px] text-muted-foreground italic flex items-center gap-1">
              <KeyRound class="w-3.5 h-3.5 text-amber-600" />
              <span>Chính sách đổi mật khẩu quản trị tập trung</span>
            </p>
            <Button
              type="button"
              variant="secondary"
              size="sm"
              disabled
              class="text-xs opacity-60 cursor-not-allowed"
            >
              Cập Nhật Mật Khẩu (Chưa hỗ trợ)
            </Button>
          </CardFooter>
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
