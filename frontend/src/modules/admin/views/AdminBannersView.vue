<script setup>
import { ref } from 'vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import FormField from '@/shared/components/FormField.vue'
import AppButton from '@/shared/components/AppButton.vue'
import { useAdminPermissions } from '@/modules/admin/composables/useAdminPermissions'
import { adminApi } from '@/modules/admin/api'

const { hasPermission } = useAdminPermissions()

// Banners list across operators (corresponds to API-ADM-PARTNER-04)
const banners = ref([
  {
    id: 'BAN-02',
    operatorName: 'Phương Trang Velo',
    title: 'Trải nghiệm Limousine VIP 24 Phòng Đôi sang trọng',
    imageUrl: 'https://images.unsplash.com/photo-1570125909232-eb263c188f7e?w=600&auto=format&fit=crop&q=60',
    targetUrl: 'https://velobus.vn/trips/TRIP-02/seats',
    submittedAt: '23/11/2026',
    status: 'PENDING_APPROVAL'
  },
  {
    id: 'BAN-01',
    operatorName: 'Hoàng Long Limousine',
    title: 'Khuyến mãi mùa hè: Giảm 15% chặng Hà Nội - Đà Nẵng',
    imageUrl: 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=600&auto=format&fit=crop&q=60',
    targetUrl: 'https://velobus.vn/search?from=HN-GP&to=DN-TT',
    submittedAt: '20/11/2026',
    status: 'APPROVED'
  }
])

// Create / Edit Banner Modal State
const isBannerModalOpen = ref(false)
const editingBanner = ref(null)
const bannerForm = ref({
  title: '',
  imageUrl: '',
  targetUrl: '',
  operatorName: 'VeloBus Platform (Banner Sàn)'
})
const bannerFormError = ref('')

function openCreateBannerModal() {
  editingBanner.value = null
  bannerFormError.value = ''
  bannerForm.value = {
    title: '',
    imageUrl: 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=600&auto=format&fit=crop&q=60',
    targetUrl: 'https://velobus.vn/search',
    operatorName: 'VeloBus Platform (Banner Sàn)'
  }
  isBannerModalOpen.value = true
}

function openEditBannerModal(b) {
  editingBanner.value = b
  bannerFormError.value = ''
  bannerForm.value = {
    title: b.title,
    imageUrl: b.imageUrl,
    targetUrl: b.targetUrl,
    operatorName: b.operatorName
  }
  isBannerModalOpen.value = true
}

function closeBannerModal() {
  isBannerModalOpen.value = false
  editingBanner.value = null
}

async function handleSaveBanner() {
  bannerFormError.value = ''
  if (!bannerForm.value.title.trim() || !bannerForm.value.imageUrl.trim()) {
    bannerFormError.value = 'Vui lòng nhập Tiêu đề chiến dịch và URL hình ảnh'
    return
  }

  if (!editingBanner.value) {
    const newBanner = {
      id: `BAN-${String(banners.value.length + 1).padStart(2, '0')}`,
      operatorName: bannerForm.value.operatorName || 'VeloBus Platform (Banner Sàn)',
      title: bannerForm.value.title.trim(),
      imageUrl: bannerForm.value.imageUrl.trim(),
      targetUrl: bannerForm.value.targetUrl.trim() || 'https://velobus.vn',
      submittedAt: '24/11/2026',
      status: 'APPROVED'
    }
    banners.value.unshift(newBanner)
    adminApi.createPlatformBanner({
      title: newBanner.title,
      imageUrl: newBanner.imageUrl,
      targetUrl: newBanner.targetUrl
    }).then(res => {
      if (res?.data?.id) newBanner.id = res.data.id
    }).catch(() => {})
  } else {
    editingBanner.value.title = bannerForm.value.title.trim()
    editingBanner.value.imageUrl = bannerForm.value.imageUrl.trim()
    editingBanner.value.targetUrl = bannerForm.value.targetUrl.trim()
    try {
      await adminApi.updateBanner(editingBanner.value.id, {
        title: editingBanner.value.title,
        imageUrl: editingBanner.value.imageUrl,
        targetUrl: editingBanner.value.targetUrl
      })
    } catch (_) {}
  }

  closeBannerModal()
}

async function handleDeleteBanner(banner) {
  try {
    await adminApi.deleteBanner(banner.id)
  } catch (_) {}
  banners.value = banners.value.filter(b => b.id !== banner.id)
}

async function updateBannerStatus(banner, newStatus) {
  banner.status = newStatus
  try {
    if (newStatus === 'APPROVED') await adminApi.approveBanner(banner.id)
    else await adminApi.rejectBanner(banner.id, 'Rejected by admin')
  } catch (_) {}
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-text-primary">Kiểm Duyệt Banner Quảng Cáo Toàn Sàn</h1>
        <p class="text-xs text-text-muted mt-1">
          Phê duyệt nội dung hình ảnh, đăng mới/sửa/xóa Banner truyền thông của sàn và kiểm tra an toàn đường dẫn trên Hero Carousel.
        </p>
      </div>

      <button
        v-if="hasPermission('BANNER_CREATE_DELETE')"
        type="button"
        data-testid="create-banner-btn"
        @click="openCreateBannerModal"
        class="self-start sm:self-auto px-3.5 py-2 rounded-lg bg-brand-navy hover:bg-slate-800 text-white font-bold text-xs transition-all shadow-sm"
      >
        🖼️ + Tạo Banner Sàn Mới
      </button>
    </div>

    <div class="bg-surface rounded-xl border border-border-default shadow-card overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse text-xs">
          <thead>
            <tr class="bg-slate-50/80 border-b border-border-default text-text-secondary uppercase tracking-wider text-[11px]">
              <th class="py-3 px-4 font-semibold">Ảnh Preview</th>
              <th class="py-3 px-4 font-semibold">Nhà Xe Đăng Ký</th>
              <th class="py-3 px-4 font-semibold">Tiêu Đề Chiến Dịch</th>
              <th class="py-3 px-4 font-semibold">URL Điều Hướng</th>
              <th class="py-3 px-4 font-semibold text-center">Trạng Thái</th>
              <th class="py-3 px-4 font-semibold text-right">Thao Tác CRUD</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border-default">
            <tr v-for="b in banners" :key="b.id" class="hover:bg-slate-50/60 transition-colors">
              <td class="py-3 px-4">
                <img
                  :src="b.imageUrl"
                  alt="Banner preview"
                  class="w-24 h-14 object-cover rounded-lg border border-border-default"
                />
              </td>
              <td class="py-3 px-4 font-bold text-text-primary">{{ b.operatorName }}</td>
              <td class="py-3 px-4 font-medium text-text-secondary">{{ b.title }}</td>
              <td class="py-3 px-4 font-mono text-[11px] text-primary-600">{{ b.targetUrl }}</td>
              <td class="py-3 px-4 text-center">
                <StatusBadge :status="b.status" />
              </td>
              <td class="py-3 px-4 text-right space-x-1.5 whitespace-nowrap">
                <button
                  v-if="b.status !== 'APPROVED' && hasPermission('BANNER_APPROVE_REJECT')"
                  type="button"
                  @click="updateBannerStatus(b, 'APPROVED')"
                  :data-testid="`btn-approve-banner-${b.id}`"
                  class="px-2.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold rounded-lg text-xs transition-all"
                >
                  ✓ Duyệt Hiển Thị
                </button>
                <button
                  v-if="b.status !== 'REJECTED' && hasPermission('BANNER_APPROVE_REJECT')"
                  type="button"
                  @click="updateBannerStatus(b, 'REJECTED')"
                  class="px-2.5 py-1.5 bg-rose-600 hover:bg-rose-700 text-white font-semibold rounded-lg text-xs transition-all"
                >
                  ✕ Gỡ Bỏ
                </button>
                <button
                  v-if="hasPermission('BANNER_CREATE_DELETE')"
                  type="button"
                  :data-testid="`btn-edit-banner-${b.id}`"
                  @click="openEditBannerModal(b)"
                  class="px-2.5 py-1.5 border border-border-strong hover:bg-slate-100 text-text-primary font-semibold rounded-lg text-xs transition-all"
                >
                  ✏️ Sửa
                </button>
                <button
                  v-if="hasPermission('BANNER_CREATE_DELETE')"
                  type="button"
                  :data-testid="`btn-delete-banner-${b.id}`"
                  @click="handleDeleteBanner(b)"
                  class="px-2.5 py-1.5 bg-rose-50 text-rose-700 hover:bg-rose-100 font-semibold rounded-lg text-xs transition-all"
                >
                  🗑️ Xóa
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modal: Create / Edit Banner -->
    <div
      v-if="isBannerModalOpen"
      class="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4"
    >
      <div class="bg-surface rounded-xl max-w-md w-full p-6 shadow-dialog border border-border-default space-y-4">
        <div class="flex items-center justify-between pb-2 border-b border-border-default">
          <h3 class="text-base font-bold text-text-primary">
            {{ editingBanner ? `Chỉnh Sửa Banner — ${editingBanner.id}` : 'Tạo Banner Truyền Thông Toàn Sàn' }}
          </h3>
          <button type="button" @click="closeBannerModal" class="text-slate-400 hover:text-slate-600 text-lg">✕</button>
        </div>

        <div v-if="bannerFormError" class="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs font-semibold">
          ⚠️ {{ bannerFormError }}
        </div>

        <FormField label="Tiêu đề chiến dịch quảng cáo" required>
          <input
            v-model="bannerForm.title"
            type="text"
            data-testid="input-banner-title"
            placeholder="VD: Ưu đãi đặt vé Tết Nguyên Đán 2027..."
            class="w-full h-10 px-3 rounded-lg border border-border-strong text-xs text-text-primary"
          />
        </FormField>

        <FormField label="Đường dẫn ảnh Banner (Image URL)" required>
          <input
            v-model="bannerForm.imageUrl"
            type="text"
            data-testid="input-banner-image"
            placeholder="https://..."
            class="w-full h-10 px-3 rounded-lg border border-border-strong text-xs text-text-primary"
          />
        </FormField>

        <FormField label="URL đích khi khách bấm vào Banner">
          <input
            v-model="bannerForm.targetUrl"
            type="text"
            data-testid="input-banner-target"
            placeholder="https://velobus.vn/search"
            class="w-full h-10 px-3 rounded-lg border border-border-strong text-xs text-text-primary"
          />
        </FormField>

        <div class="flex justify-end gap-3 pt-2 border-t border-border-default">
          <button
            type="button"
            @click="closeBannerModal"
            class="px-4 py-2 border border-border-strong hover:bg-slate-50 text-text-secondary text-xs font-semibold rounded-lg"
          >
            Hủy Bỏ
          </button>
          <AppButton
            variant="primary"
            class="px-4 py-2 text-xs font-bold"
            data-testid="btn-save-banner"
            @click="handleSaveBanner"
          >
            {{ editingBanner ? 'Lưu Cập Nhật' : 'Đăng Banner Sàn' }}
          </AppButton>
        </div>
      </div>
    </div>
  </div>
</template>
