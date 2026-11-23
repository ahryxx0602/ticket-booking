<script setup>
import { ref } from 'vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'

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

function updateBannerStatus(banner, newStatus) {
  banner.status = newStatus
}
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-text-primary">Kiểm Duyệt Banner Quảng Cáo Toàn Sàn</h1>
      <p class="text-xs text-text-muted mt-1">
        Phê duyệt nội dung hình ảnh và kiểm tra an toàn đường dẫn trước khi cho phép hiển thị trên Hero Carousel trang chủ.
      </p>
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
              <th class="py-3 px-4 font-semibold text-right">Thao Tác</th>
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
              <td class="py-3 px-4 text-right space-x-2">
                <button
                  v-if="b.status !== 'APPROVED'"
                  type="button"
                  @click="updateBannerStatus(b, 'APPROVED')"
                  :data-testid="`btn-approve-banner-${b.id}`"
                  class="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold rounded-lg text-xs transition-all"
                >
                  ✓ Duyệt Hiển Thị
                </button>
                <button
                  v-if="b.status !== 'REJECTED'"
                  type="button"
                  @click="updateBannerStatus(b, 'REJECTED')"
                  class="px-3 py-1.5 bg-rose-600 hover:bg-rose-700 text-white font-semibold rounded-lg text-xs transition-all"
                >
                  ✕ Gỡ Bỏ
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
