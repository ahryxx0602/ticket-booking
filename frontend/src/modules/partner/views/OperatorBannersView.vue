<script setup>
import { ref } from 'vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import FormField from '@/shared/components/FormField.vue'
import AppButton from '@/shared/components/AppButton.vue'

// Banners List (corresponds to API-PARTNER-07)
const banners = ref([
  {
    id: 'BAN-01',
    title: 'Khuyến mãi mùa hè: Giảm 15% chặng Hà Nội - Đà Nẵng',
    imageUrl: 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=600&auto=format&fit=crop&q=60',
    targetUrl: 'https://velobus.vn/search?from=HN-GP&to=DN-TT',
    position: 'HOME_CAROUSEL',
    status: 'APPROVED',
    registeredAt: '20/11/2026'
  },
  {
    id: 'BAN-02',
    title: 'Trải nghiệm Limousine VIP 24 Phòng Đôi sang trọng',
    imageUrl: 'https://images.unsplash.com/photo-1570125909232-eb263c188f7e?w=600&auto=format&fit=crop&q=60',
    targetUrl: 'https://velobus.vn/trips/TRIP-02/seats',
    position: 'HOME_CAROUSEL',
    status: 'PENDING_APPROVAL',
    registeredAt: '23/11/2026'
  }
])

// Banner Drawer State (520px Right Drawer)
const showBannerDrawer = ref(false)
const bannerForm = ref({
  title: '',
  targetUrl: '',
  imageUrl: ''
})
const bannerError = ref('')
const isSubmittingBanner = ref(false)

function openBannerDrawer() {
  bannerError.value = ''
  bannerForm.value = {
    title: '',
    targetUrl: '',
    imageUrl: 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=600&auto=format&fit=crop&q=60'
  }
  showBannerDrawer.value = true
}

function closeBannerDrawer() {
  showBannerDrawer.value = false
}

function handleRegisterBanner() {
  bannerError.value = ''

  if (!bannerForm.value.title.trim()) {
    bannerError.value = 'Vui lòng nhập tiêu đề banner'
    return
  }

  // URL sanity check
  if (!bannerForm.value.targetUrl.trim() || !bannerForm.value.targetUrl.startsWith('https://')) {
    bannerError.value = 'Đường dẫn liên kết phải bắt đầu bằng https://'
    return
  }

  isSubmittingBanner.value = true

  setTimeout(() => {
    banners.value.unshift({
      id: `BAN-${Math.floor(10 + Math.random() * 90)}`,
      title: bannerForm.value.title,
      imageUrl: bannerForm.value.imageUrl,
      targetUrl: bannerForm.value.targetUrl,
      position: 'HOME_CAROUSEL',
      status: 'PENDING_APPROVAL',
      registeredAt: 'Vừa xong'
    })

    isSubmittingBanner.value = false
    closeBannerDrawer()
  }, 800)
}
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-text-primary">Banner & Chiến Dịch Quảng Cáo Sàn</h1>
        <p class="text-xs text-text-muted mt-1">
          Đăng ký banner quảng cáo của nhà xe để hiển thị nổi bật trên Hero Carousel của trang chủ sàn VeloBus.
        </p>
      </div>

      <button
        type="button"
        @click="openBannerDrawer"
        data-testid="btn-add-banner"
        class="self-start sm:self-auto px-4 py-2.5 bg-primary-600 hover:bg-primary-700 text-white font-semibold rounded-lg text-xs shadow-sm flex items-center gap-1.5 transition-all"
      >
        <span>🖼️</span>
        <span>+ Đăng Ký Banner</span>
      </button>
    </div>

    <!-- Banners DataTable -->
    <div class="bg-surface rounded-xl border border-border-default shadow-card overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse text-xs">
          <thead>
            <tr class="bg-slate-50/80 border-b border-border-default text-text-secondary uppercase tracking-wider text-[11px]">
              <th class="py-3 px-4 font-semibold">Ảnh Xem Trước</th>
              <th class="py-3 px-4 font-semibold">Tiêu Đề Banner</th>
              <th class="py-3 px-4 font-semibold">Vị Trí Hiển Thị</th>
              <th class="py-3 px-4 font-semibold">Đường Dẫn Đích</th>
              <th class="py-3 px-4 font-semibold">Ngày Gửi Duyệt</th>
              <th class="py-3 px-4 font-semibold text-center">Trạng Thái</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border-default">
            <tr v-for="b in banners" :key="b.id" class="hover:bg-slate-50/60 transition-colors">
              <td class="py-3 px-4">
                <img
                  :src="b.imageUrl"
                  alt="Banner preview"
                  class="w-24 h-14 object-cover rounded-lg border border-border-default shadow-xs"
                />
              </td>
              <td class="py-3 px-4 font-bold text-text-primary">{{ b.title }}</td>
              <td class="py-3 px-4 text-text-muted">Trang Chủ (Hero Carousel)</td>
              <td class="py-3 px-4 text-primary-600 font-mono text-[11px] hover:underline cursor-pointer">
                {{ b.targetUrl }}
              </td>
              <td class="py-3 px-4 text-text-muted">{{ b.registeredAt }}</td>
              <td class="py-3 px-4 text-center">
                <StatusBadge :status="b.status" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Right-Side Drawer (520px) Đăng Ký Banner -->
    <div
      v-if="showBannerDrawer"
      class="fixed inset-0 bg-black/50 z-50 flex justify-end"
    >
      <div class="bg-surface w-full max-w-[520px] h-full shadow-2xl flex flex-col justify-between">
        <div class="p-5 border-b border-border-default flex items-center justify-between bg-slate-50">
          <div>
            <h2 class="text-base font-bold text-text-primary flex items-center gap-2">
              <span>🖼️</span>
              <span>Đăng Ký Banner Quảng Cáo Mới</span>
            </h2>
            <p class="text-[11px] text-text-muted mt-0.5">Yêu cầu tỷ lệ chuẩn 16:9, hình ảnh sắc nét và không chứa nội dung phản cảm</p>
          </div>
          <button @click="closeBannerDrawer" class="text-slate-400 hover:text-slate-600 text-lg">✕</button>
        </div>

        <div class="p-6 overflow-y-auto space-y-4 flex-1">
          <FormField
            label="Tiêu đề thông điệp banner"
            :error="bannerError"
            required
          >
            <input
              v-model="bannerForm.title"
              type="text"
              placeholder="Ví dụ: Khuyến mãi vé xe Tết 2027..."
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600"
              :class="{ 'border-rose-500': bannerError }"
            />
          </FormField>

          <FormField label="Đường dẫn đích khi khách bấm vào banner" required>
            <input
              v-model="bannerForm.targetUrl"
              type="url"
              placeholder="https://velobus.vn/search?from=HN-GP..."
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 font-mono"
            />
          </FormField>

          <FormField label="URL Hình ảnh banner (Tỷ lệ 16:9)" required>
            <input
              v-model="bannerForm.imageUrl"
              type="url"
              placeholder="https://images.unsplash.com/..."
              class="w-full h-11 px-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 font-mono"
            />
          </FormField>

          <div v-if="bannerForm.imageUrl" class="space-y-1.5 pt-2">
            <span class="text-xs text-text-secondary font-semibold">Xem trước hiển thị:</span>
            <img
              :src="bannerForm.imageUrl"
              alt="Banner preview"
              class="w-full h-40 object-cover rounded-xl border border-border-default shadow-xs"
            />
          </div>
        </div>

        <div class="p-5 border-t border-border-default bg-slate-50 flex items-center justify-end gap-3">
          <button
            type="button"
            @click="closeBannerDrawer"
            class="px-4 py-2.5 border border-border-strong hover:bg-slate-100 text-text-secondary text-xs font-semibold rounded-lg transition-all"
          >
            Hủy Bỏ
          </button>

          <AppButton
            variant="primary"
            class="px-5 py-2.5 text-xs font-bold"
            :loading="isSubmittingBanner"
            @click="handleRegisterBanner"
          >
            Gửi Ban Quản Trị Duyệt
          </AppButton>
        </div>
      </div>
    </div>
  </div>
</template>
