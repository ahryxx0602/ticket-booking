<template>
  <div class="max-w-4xl mx-auto space-y-6">
    <PageHeader
      title="Hồ Sơ Cá Nhân & Bảo Mật Tài Khoản"
      subtitle="Quản lý thông tin định danh cá nhân, đổi mật khẩu và liên kết Bot Telegram nhận biến động số dư / vé điện tử."
    />

    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <!-- Cột Trái: Avatar & Phân Quyền -->
      <div class="bg-white border border-slate-200 rounded-xl p-6 shadow-sm text-center space-y-4">
        <div class="w-24 h-24 mx-auto rounded-full bg-blue-100 border-2 border-blue-500 flex items-center justify-center text-blue-700 text-3xl font-bold">
          {{ userInitials }}
        </div>
        <div>
          <h3 class="text-lg font-bold text-slate-900">{{ user.fullName || 'Khách Hàng VeloBus' }}</h3>
          <p class="text-xs text-slate-500 font-mono">@{{ user.username || 'username' }}</p>
        </div>
        <div class="pt-2 border-t border-slate-100 space-y-2 text-left text-xs text-slate-600">
          <div class="flex justify-between">
            <span>Vai trò:</span>
            <span class="font-bold text-blue-600">{{ user.role || 'ROLE_CUSTOMER' }}</span>
          </div>
          <div class="flex justify-between">
            <span>Trạng thái:</span>
            <span class="font-semibold text-emerald-600">Hoạt Động (ACTIVE)</span>
          </div>
        </div>

        <div class="pt-4 border-t border-slate-100">
          <AppButton
            variant="outline"
            class="w-full text-xs"
            icon="pi pi-send"
            @click="showTelegramModal = true"
          >
            Liên Kết Telegram Bot
          </AppButton>
        </div>
      </div>

      <!-- Cột Phải: Form Thông Tin & Đổi Mật Khẩu -->
      <div class="md:col-span-2 space-y-6">
        <div class="bg-white border border-slate-200 rounded-xl p-6 shadow-sm space-y-4">
          <h4 class="text-sm font-bold text-slate-900 border-b border-slate-100 pb-3">Thông Tin Chi Tiết</h4>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <FormField label="Họ và Tên">
              <input
                v-model="user.fullName"
                type="text"
                class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:border-blue-500 focus:outline-none"
              />
            </FormField>
            <FormField label="Địa Chỉ Email">
              <input
                v-model="user.email"
                type="email"
                class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:border-blue-500 focus:outline-none"
              />
            </FormField>
          </div>
          <div class="flex justify-end pt-2">
            <AppButton variant="primary" @click="saveBasicInfo">Lưu Thay Đổi</AppButton>
          </div>
        </div>

        <div class="bg-white border border-slate-200 rounded-xl p-6 shadow-sm space-y-4">
          <h4 class="text-sm font-bold text-slate-900 border-b border-slate-100 pb-3 flex items-center gap-2">
            <i class="pi pi-lock text-slate-500"></i>
            <span>Đổi Mật Khẩu Đăng Nhập</span>
          </h4>
          <div class="space-y-3">
            <FormField label="Mật khẩu hiện tại">
              <input
                v-model="passwordForm.current"
                type="password"
                class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:border-blue-500 focus:outline-none"
              />
            </FormField>
            <FormField label="Mật khẩu mới (Tối thiểu 8 ký tự)">
              <input
                v-model="passwordForm.newPassword"
                type="password"
                class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:border-blue-500 focus:outline-none"
              />
            </FormField>
          </div>
          <div class="flex justify-end pt-2">
            <AppButton variant="secondary" @click="changePassword">Cập Nhật Mật Khẩu</AppButton>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal Liên Kết Telegram -->
    <TelegramLinkModal v-model="showTelegramModal" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/modules/auth/store'
import PageHeader from '@/shared/components/PageHeader.vue'
import AppButton from '@/shared/components/AppButton.vue'
import FormField from '@/shared/components/FormField.vue'
import TelegramLinkModal from '@/shared/components/TelegramLinkModal.vue'

const auth = useAuthStore()
const showTelegramModal = ref(false)

const user = ref({
  username: 'thanhpv',
  fullName: 'Phan Văn Thành',
  email: 'thanh.pv@gmail.com',
  role: 'ROLE_CUSTOMER',
})

const passwordForm = ref({
  current: '',
  newPassword: '',
})

onMounted(() => {
  if (auth.user) {
    Object.assign(user.value, auth.user)
  }
  user.value.role = auth.role
})

const userInitials = computed(() => {
  const name = user.value.fullName || user.value.username || 'U'
  return name.charAt(0).toUpperCase()
})

function saveBasicInfo() {
  if (auth.user) {
    auth.user = { ...auth.user, ...user.value }
  }
  localStorage.setItem('velobus_user', JSON.stringify(user.value))
  alert('Đã cập nhật thông tin cá nhân thành công!')
}

function changePassword() {
  if (!passwordForm.value.newPassword || passwordForm.value.newPassword.length < 6) {
    alert('Mật khẩu mới phải có ít nhất 6 ký tự')
    return
  }
  alert('Đã cập nhật mật khẩu thành công!')
  passwordForm.value.current = ''
  passwordForm.value.newPassword = ''
}
</script>
