<script setup>
import { ref, computed, onMounted } from 'vue'
import { toast } from 'vue-sonner'
import {
  Image as ImageIcon,
  Plus,
  ExternalLink,
  Clock,
  CheckCircle2,
  XCircle,
  AlertCircle,
  AlertTriangle,
  RefreshCw,
  Loader2,
  Calendar,
  Sparkles
} from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
  SheetFooter
} from '@/components/ui/sheet'
import {
  Table,
  TableHeader,
  TableHead,
  TableBody,
  TableRow,
  TableCell
} from '@/components/ui/table'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import FirebaseUploader from '@/shared/components/FirebaseUploader.vue'
import { partnerApi } from '@/modules/partner/api'

// Banners List state
const banners = ref([])
const isLoading = ref(false)
const isSubmitting = ref(false)
const bannerError = ref('')

const fallbackBanners = [
  {
    id: 1,
    title: 'Khuyến mãi mùa hè: Giảm 15% chặng Hà Nội - Đà Nẵng',
    imageUrl: 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=600&auto=format&fit=crop&q=60',
    targetUrl: 'https://velobus.vn/search?from=HN-GP&to=DN-TT',
    position: 'HOME_CAROUSEL',
    status: 'ACTIVE',
    createdAt: '2026-11-20T08:30:00Z',
    registeredAt: '20/11/2026'
  },
  {
    id: 2,
    title: 'Trải nghiệm Limousine VIP 24 Phòng Đôi sang trọng',
    imageUrl: 'https://images.unsplash.com/photo-1570125909232-eb263c188f7e?w=600&auto=format&fit=crop&q=60',
    targetUrl: 'https://velobus.vn/trips/TRIP-02/seats',
    position: 'HOME_CAROUSEL',
    status: 'PENDING_APPROVAL',
    createdAt: '2026-11-23T14:15:00Z',
    registeredAt: '23/11/2026'
  }
]

async function fetchMyBanners() {
  isLoading.value = true
  try {
    const res = await partnerApi.getMyBanners()
    if (res?.data && Array.isArray(res.data)) {
      banners.value = res.data.length > 0 ? res.data : fallbackBanners
    } else {
      banners.value = fallbackBanners
    }
  } catch (err) {
    console.warn('[OperatorBanners] Không tải được banner nhà xe:', err?.message)
    banners.value = fallbackBanners
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  fetchMyBanners()
})

// Registration Sheet State
const isSheetOpen = ref(false)
const bannerForm = ref({
  title: '',
  targetUrl: '',
  imageUrl: '',
  position: 'HOME_CAROUSEL'
})

function openRegisterSheet() {
  bannerError.value = ''
  bannerForm.value = {
    title: '',
    targetUrl: 'https://velobus.vn/search',
    imageUrl: 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=600&auto=format&fit=crop&q=60',
    position: 'HOME_CAROUSEL'
  }
  isSheetOpen.value = true
}

function closeRegisterSheet() {
  isSheetOpen.value = false
}

async function handleRegisterBanner() {
  bannerError.value = ''

  if (!bannerForm.value.title.trim()) {
    bannerError.value = 'Vui lòng nhập tiêu đề thông điệp banner'
    return
  }

  if (!bannerForm.value.imageUrl.trim()) {
    bannerError.value = 'Vui lòng tải lên ảnh hoặc nhập URL hình ảnh banner'
    return
  }

  if (!bannerForm.value.targetUrl.trim() || !bannerForm.value.targetUrl.startsWith('https://')) {
    bannerError.value = 'Đường dẫn liên kết phải bắt đầu bằng giao thức an toàn https://'
    return
  }

  isSubmitting.value = true
  try {
    const payload = {
      title: bannerForm.value.title.trim(),
      imageUrl: bannerForm.value.imageUrl.trim(),
      targetUrl: bannerForm.value.targetUrl.trim(),
      position: bannerForm.value.position
    }

    const res = await partnerApi.createBanner(payload)
    const newBanner = res?.data || {
      id: Date.now(),
      ...payload,
      status: 'PENDING_APPROVAL',
      registeredAt: 'Vừa xong',
      createdAt: new Date().toISOString()
    }

    banners.value.unshift(newBanner)
    toast.success('Đã gửi đăng ký banner quảng cáo lên Ban Quản Trị sàn thành công!')
    closeRegisterSheet()
  } catch (err) {
    bannerError.value = err?.message || 'Có lỗi khi gửi yêu cầu đăng ký banner'
    toast.error(bannerError.value)
  } finally {
    isSubmitting.value = false
  }
}

function getStatusBadgeClass(status) {
  switch (status) {
    case 'ACTIVE':
    case 'APPROVED':
      return 'bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/40 dark:text-emerald-300 dark:border-emerald-800'
    case 'PENDING_APPROVAL':
      return 'bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-950/40 dark:text-amber-300 dark:border-amber-800'
    case 'REJECTED':
      return 'bg-rose-50 text-rose-700 border-rose-200 dark:bg-rose-950/40 dark:text-rose-300 dark:border-rose-800'
    case 'EXPIRED':
    default:
      return 'bg-muted text-muted-foreground border-border'
  }
}

function getStatusLabel(status) {
  switch (status) {
    case 'ACTIVE':
    case 'APPROVED':
      return 'Đã Phê Duyệt'
    case 'PENDING_APPROVAL':
      return 'Chờ Duyệt'
    case 'REJECTED':
      return 'Bị Từ Chối'
    case 'EXPIRED':
      return 'Hết Hạn'
    default:
      return status || 'Không rõ'
  }
}

function formatDate(isoStr) {
  if (!isoStr) return '—'
  if (isoStr.includes('/')) return isoStr
  try {
    const d = new Date(isoStr)
    return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
  } catch {
    return isoStr
  }
}
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold tracking-tight text-foreground flex items-center gap-2">
          <ImageIcon class="w-6 h-6 text-primary" />
          <span>Banner & Chiến Dịch Quảng Cáo Sàn</span>
        </h1>
        <p class="text-xs text-muted-foreground mt-1">
          Đăng ký banner quảng cáo của nhà xe để hiển thị nổi bật trên Hero Carousel của trang chủ sàn VeloBus.
        </p>
      </div>

      <div class="flex items-center gap-2">
        <Button
          variant="outline"
          size="sm"
          class="h-9 gap-1.5"
          :disabled="isLoading"
          @click="fetchMyBanners"
        >
          <RefreshCw class="w-3.5 h-3.5" :class="{ 'animate-spin': isLoading }" />
          <span>Làm mới</span>
        </Button>

        <Button
          type="button"
          data-testid="btn-add-banner"
          size="sm"
          class="h-9 gap-1.5 font-semibold"
          @click="openRegisterSheet"
        >
          <Plus class="w-4 h-4" />
          <span>Đăng Ký Banner Mới</span>
        </Button>
      </div>
    </div>

    <!-- Table Card -->
    <Card class="shadow-sm overflow-hidden">
      <CardHeader class="px-6 py-4 border-b border-border bg-muted/20">
        <div class="flex items-center justify-between">
          <CardTitle class="text-sm font-semibold text-foreground flex items-center gap-2">
            <span>Danh Sách Banner Của Nhà Xe ({{ banners.length }})</span>
          </CardTitle>
          <Badge variant="outline" class="text-[11px] font-mono">
            Vị trí hiển thị: Hero Carousel (Trang Chủ)
          </Badge>
        </div>
      </CardHeader>

      <CardContent class="p-0">
        <div class="overflow-x-auto">
          <Table>
            <TableHeader class="bg-muted/30">
              <TableRow>
                <TableHead class="w-[120px] font-semibold text-xs">Ảnh Xem Trước</TableHead>
                <TableHead class="font-semibold text-xs">Tiêu Đề Banner</TableHead>
                <TableHead class="font-semibold text-xs">Vị Trí Hiển Thị</TableHead>
                <TableHead class="font-semibold text-xs">Đường Dẫn Đích</TableHead>
                <TableHead class="w-[130px] font-semibold text-xs">Ngày Gửi Duyệt</TableHead>
                <TableHead class="w-[120px] text-center font-semibold text-xs">Trạng Thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-if="banners.length === 0">
                <TableCell colspan="6" class="h-32 text-center text-muted-foreground text-xs">
                  Chưa có banner quảng cáo nào được đăng ký. Bấm "Đăng Ký Banner Mới" để bắt đầu.
                </TableCell>
              </TableRow>

              <TableRow
                v-for="b in banners"
                :key="b.id"
                class="hover:bg-muted/40 transition-colors"
              >
                <!-- Thumbnail -->
                <TableCell class="py-3">
                  <div class="relative w-24 h-14 rounded-lg overflow-hidden border border-border bg-muted/40 shadow-xs">
                    <img
                      :src="b.imageUrl"
                      alt="Banner preview"
                      class="w-full h-full object-cover"
                      loading="lazy"
                    />
                  </div>
                </TableCell>

                <!-- Title -->
                <TableCell class="py-3">
                  <div class="font-semibold text-xs text-foreground line-clamp-2 max-w-sm">
                    {{ b.title }}
                  </div>
                  <div v-if="b.rejectionReason" class="text-[11px] text-rose-600 dark:text-rose-400 mt-1 flex items-center gap-1 font-medium">
                    <AlertCircle class="w-3 h-3 shrink-0" />
                    <span>Lý do từ chối: {{ b.rejectionReason }}</span>
                  </div>
                </TableCell>

                <!-- Position -->
                <TableCell class="py-3 text-xs text-muted-foreground">
                  Trang Chủ (Hero Carousel)
                </TableCell>

                <!-- Target URL -->
                <TableCell class="py-3">
                  <a
                    :href="b.targetUrl"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="inline-flex items-center gap-1 text-[11px] font-mono text-primary hover:underline max-w-[220px] truncate"
                  >
                    <span class="truncate">{{ b.targetUrl }}</span>
                    <ExternalLink class="w-3 h-3 shrink-0" />
                  </a>
                </TableCell>

                <!-- Date -->
                <TableCell class="py-3 text-xs text-muted-foreground">
                  {{ formatDate(b.createdAt || b.registeredAt) }}
                </TableCell>

                <!-- Status Badge -->
                <TableCell class="py-3 text-center">
                  <span
                    class="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold border"
                    :class="getStatusBadgeClass(b.status)"
                  >
                    {{ getStatusLabel(b.status) }}
                  </span>
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </div>
      </CardContent>
    </Card>

    <!-- Right-Side Sheet (520px) Đăng Ký Banner Mới -->
    <Sheet :open="isSheetOpen" @update:open="val => { if (!val) closeRegisterSheet() }">
      <SheetContent side="right" class="w-full sm:max-w-[520px] flex flex-col justify-between p-6">
        <div>
          <SheetHeader class="space-y-1 pb-4 border-b border-border text-left">
            <SheetTitle class="text-base font-bold text-foreground flex items-center gap-2">
              <ImageIcon class="w-5 h-5 text-primary" />
              <span>Đăng Ký Banner Quảng Cáo Mới</span>
            </SheetTitle>
            <SheetDescription class="text-xs text-muted-foreground">
              Yêu cầu hình ảnh tỷ lệ chuẩn 16:9, độ phân giải sắc nét và không chứa nội dung vi phạm bản quyền hay quy định sàn.
            </SheetDescription>
          </SheetHeader>

          <Alert v-if="bannerError" variant="destructive" class="my-4 py-2.5">
            <AlertTriangle class="w-4 h-4" />
            <AlertTitle class="text-xs font-semibold">Lỗi dữ liệu</AlertTitle>
            <AlertDescription class="text-xs">{{ bannerError }}</AlertDescription>
          </Alert>

          <div class="space-y-4 py-4 text-left">
            <!-- Title -->
            <div class="space-y-1.5">
              <label for="op-banner-title" class="block text-xs font-semibold text-foreground">
                Tiêu đề thông điệp banner <span class="text-destructive">*</span>
              </label>
              <Input
                id="op-banner-title"
                v-model="bannerForm.title"
                data-testid="input-banner-title"
                placeholder="Ví dụ: Khuyến mãi vé xe Tết 2027..."
                class="h-9 text-xs"
              />
            </div>

            <!-- Target URL -->
            <div class="space-y-1.5">
              <label for="op-banner-target" class="block text-xs font-semibold text-foreground">
                Đường dẫn đích khi khách bấm vào banner <span class="text-destructive">*</span>
              </label>
              <Input
                id="op-banner-target"
                v-model="bannerForm.targetUrl"
                data-testid="input-banner-target"
                placeholder="https://velobus.vn/search?from=HN-GP..."
                class="h-9 text-xs font-mono"
              />
            </div>

            <!-- Image Upload -->
            <div class="space-y-1.5">
              <label class="block text-xs font-semibold text-foreground">
                URL Hình ảnh banner (Tỷ lệ 16:9) <span class="text-destructive">*</span>
              </label>
              <FirebaseUploader
                v-model="bannerForm.imageUrl"
                folder="banners"
                label="Chọn hoặc kéo thả ảnh tải lên Cloud CDN"
              />
              <div class="pt-1.5">
                <label for="op-banner-image" class="block text-[11px] text-muted-foreground font-medium">Hoặc dán trực tiếp URL ảnh:</label>
                <Input
                  id="op-banner-image"
                  v-model="bannerForm.imageUrl"
                  data-testid="input-banner-image"
                  placeholder="https://images.unsplash.com/..."
                  class="h-8 text-xs font-mono mt-1"
                />
              </div>
            </div>

            <!-- Image Preview Box -->
            <div v-if="bannerForm.imageUrl" class="space-y-1.5 pt-2">
              <span class="text-xs text-muted-foreground font-semibold flex items-center gap-1">
                <Sparkles class="w-3.5 h-3.5 text-primary" />
                <span>Xem trước hiển thị:</span>
              </span>
              <div class="w-full h-36 rounded-xl overflow-hidden border border-border bg-muted/40 shadow-xs">
                <img
                  :src="bannerForm.imageUrl"
                  alt="Banner preview"
                  class="w-full h-full object-cover"
                />
              </div>
            </div>
          </div>
        </div>

        <SheetFooter class="border-t border-border pt-4 flex-row items-center justify-end gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            class="text-xs"
            @click="closeRegisterSheet"
          >
            Hủy Bỏ
          </Button>
          <Button
            type="button"
            size="sm"
            data-testid="btn-submit-banner"
            class="text-xs font-semibold"
            :disabled="isSubmitting"
            @click="handleRegisterBanner"
          >
            <Loader2 v-if="isSubmitting" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
            <span>Gửi Ban Quản Trị Duyệt</span>
          </Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  </div>
</template>
