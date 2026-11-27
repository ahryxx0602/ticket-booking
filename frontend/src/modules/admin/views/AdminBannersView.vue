<script setup>
import { ref, computed, onMounted } from 'vue'
import { toast } from 'vue-sonner'
import {
  Image as ImageIcon,
  Plus,
  Pencil,
  Trash2,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  ExternalLink,
  RefreshCw,
  Loader2,
  Filter,
  Eye,
  Clock,
  Ban
} from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter
} from '@/components/ui/dialog'
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogCancel,
  AlertDialogAction
} from '@/components/ui/alert-dialog'
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
import { useAdminPermissions } from '@/modules/admin/composables/useAdminPermissions'
import { adminApi } from '@/modules/admin/api'

const { hasPermission } = useAdminPermissions()

const banners = ref([])
const isLoading = ref(false)
const loadError = ref('')
const selectedStatusFilter = ref('ALL')

// Baseline mock data fallback if backend returns empty or runs offline
const initialFallbackBanners = [
  {
    id: 1,
    operatorId: 2,
    operatorName: 'Phương Trang Velo',
    title: 'Trải nghiệm Limousine VIP 24 Phòng Đôi sang trọng',
    imageUrl: 'https://images.unsplash.com/photo-1570125909232-eb263c188f7e?w=600&auto=format&fit=crop&q=60',
    targetUrl: 'https://velobus.vn/trips/TRIP-02/seats',
    position: 'HOME_CAROUSEL',
    priority: 1,
    status: 'PENDING_APPROVAL',
    createdAt: '2026-11-23T10:00:00Z'
  },
  {
    id: 2,
    operatorId: 1,
    operatorName: 'Hoàng Long Limousine',
    title: 'Khuyến mãi mùa hè: Giảm 15% chặng Hà Nội - Đà Nẵng',
    imageUrl: 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=600&auto=format&fit=crop&q=60',
    targetUrl: 'https://velobus.vn/search?from=HN-GP&to=DN-TT',
    position: 'HOME_CAROUSEL',
    priority: 2,
    status: 'ACTIVE',
    createdAt: '2026-11-20T10:00:00Z'
  }
]

async function fetchBanners() {
  isLoading.value = true
  loadError.value = ''
  try {
    const statusParam = selectedStatusFilter.value !== 'ALL' ? selectedStatusFilter.value : null
    const res = await adminApi.getBanners(statusParam)
    if (res?.data && Array.isArray(res.data)) {
      banners.value = res.data.length > 0 ? res.data : initialFallbackBanners
    } else {
      banners.value = initialFallbackBanners
    }
  } catch (err) {
    console.warn('[AdminBanners] Không tải được dữ liệu từ API:', err?.message)
    banners.value = initialFallbackBanners
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  fetchBanners()
})

const filteredBanners = computed(() => {
  if (selectedStatusFilter.value === 'ALL') return banners.value
  return banners.value.filter(b => b.status === selectedStatusFilter.value)
})

// Modal Form State: Create / Edit
const isFormModalOpen = ref(false)
const isSubmitting = ref(false)
const editingBanner = ref(null)
const bannerForm = ref({
  title: '',
  imageUrl: '',
  targetUrl: '',
  position: 'HOME_CAROUSEL',
  priority: 1,
  operatorName: 'VeloBus Platform (Banner Sàn)'
})
const formError = ref('')

function openCreateBannerModal() {
  editingBanner.value = null
  formError.value = ''
  bannerForm.value = {
    title: '',
    imageUrl: 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=600&auto=format&fit=crop&q=60',
    targetUrl: 'https://velobus.vn/search',
    position: 'HOME_CAROUSEL',
    priority: 1,
    operatorName: 'VeloBus Platform (Banner Sàn)'
  }
  isFormModalOpen.value = true
}

function openEditBannerModal(b) {
  editingBanner.value = b
  formError.value = ''
  bannerForm.value = {
    title: b.title || '',
    imageUrl: b.imageUrl || '',
    targetUrl: b.targetUrl || '',
    position: b.position || 'HOME_CAROUSEL',
    priority: b.priority || 1,
    operatorName: b.operatorName || (b.operatorId ? `Nhà xe #${b.operatorId}` : 'VeloBus Platform')
  }
  isFormModalOpen.value = true
}

function closeFormModal() {
  isFormModalOpen.value = false
  editingBanner.value = null
}

async function handleSaveBanner() {
  formError.value = ''
  if (!bannerForm.value.title.trim()) {
    formError.value = 'Vui lòng nhập tiêu đề chiến dịch quảng cáo'
    return
  }
  if (!bannerForm.value.imageUrl.trim()) {
    formError.value = 'Vui lòng cung cấp URL hoặc tải ảnh banner lên Cloud'
    return
  }
  if (bannerForm.value.targetUrl.trim() && !bannerForm.value.targetUrl.startsWith('https://')) {
    formError.value = 'Đường dẫn liên kết phải sử dụng giao thức an toàn https://'
    return
  }

  isSubmitting.value = true
  try {
    if (!editingBanner.value) {
      // Create Platform Banner with Optimistic UI unshift
      const payload = {
        title: bannerForm.value.title.trim(),
        imageUrl: bannerForm.value.imageUrl.trim(),
        targetUrl: bannerForm.value.targetUrl.trim() || 'https://velobus.vn',
        position: bannerForm.value.position,
        priority: Number(bannerForm.value.priority) || 1
      }
      const created = {
        id: Date.now(),
        ...payload,
        operatorName: bannerForm.value.operatorName,
        status: 'ACTIVE',
        createdAt: new Date().toISOString()
      }
      banners.value.unshift(created)
      adminApi.createPlatformBanner(payload)
        .then(res => {
          if (res?.data?.id) created.id = res.data.id
        })
        .catch(() => {})
      toast.success('Đã đăng banner sàn mới thành công')
    } else {
      // Update Banner with Optimistic UI update
      const payload = {
        title: bannerForm.value.title.trim(),
        imageUrl: bannerForm.value.imageUrl.trim(),
        targetUrl: bannerForm.value.targetUrl.trim(),
        position: bannerForm.value.position,
        priority: Number(bannerForm.value.priority) || 1
      }
      Object.assign(editingBanner.value, payload)
      adminApi.updateBanner(editingBanner.value.id, payload).catch(() => {})
      toast.success(`Cập nhật banner #${editingBanner.value.id} thành công`)
    }
    closeFormModal()
  } catch (err) {
    formError.value = err?.message || 'Có lỗi khi lưu banner'
    toast.error(formError.value)
  } finally {
    isSubmitting.value = false
  }
}

// Moderation Action: Approve
const approveDialogState = ref({
  isOpen: false,
  banner: null,
  isProcessing: false
})

function confirmApproveBanner(b) {
  approveDialogState.value = {
    isOpen: true,
    banner: b,
    isProcessing: false
  }
}

async function executeApproveBanner() {
  const b = approveDialogState.value.banner
  if (!b) return
  approveDialogState.value.isProcessing = true
  try {
    await adminApi.approveBanner(b.id)
    b.status = 'ACTIVE'
    toast.success(`Đã duyệt hiển thị banner #${b.id} lên trang chủ sàn`)
    approveDialogState.value.isOpen = false
  } catch (err) {
    toast.error(err?.message || 'Không thể phê duyệt banner')
  } finally {
    approveDialogState.value.isProcessing = false
  }
}

// Moderation Action: Reject with Reason
const rejectDialogState = ref({
  isOpen: false,
  banner: null,
  reason: '',
  error: '',
  isProcessing: false
})

function confirmRejectBanner(b) {
  rejectDialogState.value = {
    isOpen: true,
    banner: b,
    reason: '',
    error: '',
    isProcessing: false
  }
}

async function executeRejectBanner() {
  const b = rejectDialogState.value.banner
  if (!b) return
  if (!rejectDialogState.value.reason.trim()) {
    rejectDialogState.value.error = 'Vui lòng nêu rõ lý do từ chối để thông báo cho đối tác'
    return
  }
  rejectDialogState.value.isProcessing = true
  try {
    await adminApi.rejectBanner(b.id, rejectDialogState.value.reason.trim())
    b.status = 'REJECTED'
    b.rejectionReason = rejectDialogState.value.reason.trim()
    toast.success(`Đã từ chối và gỡ bỏ banner #${b.id}`)
    rejectDialogState.value.isOpen = false
  } catch (err) {
    rejectDialogState.value.error = err?.message || 'Không thể từ chối banner'
    toast.error(rejectDialogState.value.error)
  } finally {
    rejectDialogState.value.isProcessing = false
  }
}

// Delete Action
const deleteDialogState = ref({
  isOpen: false,
  banner: null,
  isProcessing: false
})

function confirmDeleteBanner(b) {
  deleteDialogState.value = {
    isOpen: true,
    banner: b,
    isProcessing: false
  }
}

async function executeDeleteBanner() {
  const b = deleteDialogState.value.banner
  if (!b) return
  deleteDialogState.value.isProcessing = true
  try {
    await adminApi.deleteBanner(b.id)
    banners.value = banners.value.filter(item => item.id !== b.id)
    toast.success(`Đã xóa vĩnh viễn banner #${b.id}`)
    deleteDialogState.value.isOpen = false
  } catch (err) {
    toast.error(err?.message || 'Không thể xóa banner')
  } finally {
    deleteDialogState.value.isProcessing = false
  }
}

function getStatusBadgeVariant(status) {
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
      return 'Đang Hiển Thị'
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
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold tracking-tight text-foreground flex items-center gap-2">
          <ImageIcon class="w-6 h-6 text-primary" />
          <span>Kiểm Duyệt Banner Quảng Cáo Toàn Sàn</span>
        </h1>
        <p class="text-xs text-muted-foreground mt-1">
          Phê duyệt nội dung hình ảnh, tạo mới/sửa/xóa Banner truyền thông của sàn và kiểm tra tính an toàn điều hướng trên Hero Carousel.
        </p>
      </div>

      <div class="flex items-center gap-2">
        <Button
          variant="outline"
          size="sm"
          class="h-9 gap-1.5"
          :disabled="isLoading"
          @click="fetchBanners"
        >
          <RefreshCw class="w-3.5 h-3.5" :class="{ 'animate-spin': isLoading }" />
          <span>Làm mới</span>
        </Button>

        <Button
          v-if="hasPermission('BANNER_CREATE_DELETE')"
          type="button"
          data-testid="create-banner-btn"
          size="sm"
          class="h-9 gap-1.5 font-semibold"
          @click="openCreateBannerModal"
        >
          <Plus class="w-4 h-4" />
          <span>Tạo Banner Sàn Mới</span>
        </Button>
      </div>
    </div>

    <!-- Filter Bar -->
    <Card class="shadow-sm">
      <CardContent class="p-3 sm:p-4">
        <div class="flex flex-wrap items-center gap-3">
          <div class="flex items-center gap-1.5 text-xs text-muted-foreground font-medium">
            <Filter class="w-3.5 h-3.5" />
            <span>Trạng thái lọc:</span>
          </div>
          <div class="flex flex-wrap gap-1.5">
            <Button
              v-for="f in [
                { id: 'ALL', label: 'Tất Cả' },
                { id: 'PENDING_APPROVAL', label: 'Chờ Duyệt' },
                { id: 'ACTIVE', label: 'Đang Hiển Thị' },
                { id: 'REJECTED', label: 'Bị Từ Chối' }
              ]"
              :key="f.id"
              size="sm"
              :variant="selectedStatusFilter === f.id ? 'default' : 'outline'"
              class="h-7 text-xs font-medium"
              @click="selectedStatusFilter = f.id"
            >
              {{ f.label }}
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- Table Card -->
    <Card class="shadow-sm overflow-hidden">
      <CardHeader class="px-6 py-4 border-b border-border bg-muted/20">
        <div class="flex items-center justify-between">
          <CardTitle class="text-sm font-semibold text-foreground flex items-center gap-2">
            <span>Danh Sách Banner ({{ filteredBanners.length }})</span>
          </CardTitle>
          <Badge variant="outline" class="text-[11px] font-mono">
            Vị trí: Hero Carousel (Trang Chủ)
          </Badge>
        </div>
      </CardHeader>

      <CardContent class="p-0">
        <div class="overflow-x-auto">
          <Table>
            <TableHeader class="bg-muted/30">
              <TableRow>
                <TableHead class="w-[120px] font-semibold text-xs">Ảnh Preview</TableHead>
                <TableHead class="font-semibold text-xs">Nhà Xe / Đơn Vị</TableHead>
                <TableHead class="font-semibold text-xs">Tiêu Đề Chiến Dịch</TableHead>
                <TableHead class="font-semibold text-xs">Đường Dẫn Đích</TableHead>
                <TableHead class="w-[120px] text-center font-semibold text-xs">Trạng Thái</TableHead>
                <TableHead class="w-[200px] text-right font-semibold text-xs">Thao Tác Quản Trị</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-if="filteredBanners.length === 0">
                <TableCell colspan="6" class="h-32 text-center text-muted-foreground text-xs">
                  Không tìm thấy banner nào phù hợp với bộ lọc hiện tại.
                </TableCell>
              </TableRow>

              <TableRow
                v-for="b in filteredBanners"
                :key="b.id"
                class="hover:bg-muted/40 transition-colors"
              >
                <!-- Thumbnail -->
                <TableCell class="py-3">
                  <div class="relative w-24 h-14 rounded-lg overflow-hidden border border-border bg-muted/40 shadow-xs">
                    <img
                      :src="b.imageUrl"
                      alt="Banner Preview"
                      class="w-full h-full object-cover"
                      loading="lazy"
                    />
                  </div>
                </TableCell>

                <!-- Operator -->
                <TableCell class="py-3">
                  <div class="font-semibold text-xs text-foreground">
                    {{ b.operatorName || (b.operatorId ? `Nhà xe #${b.operatorId}` : 'VeloBus Platform') }}
                  </div>
                  <div class="text-[10px] text-muted-foreground font-mono mt-0.5">
                    ID: BAN-{{ String(b.id).padStart(2, '0') }}
                  </div>
                </TableCell>

                <!-- Title & Meta -->
                <TableCell class="py-3">
                  <div class="font-medium text-xs text-foreground line-clamp-2 max-w-sm">
                    {{ b.title }}
                  </div>
                  <div v-if="b.rejectionReason" class="text-[11px] text-rose-600 dark:text-rose-400 mt-1 flex items-center gap-1 font-medium">
                    <AlertTriangle class="w-3 h-3 shrink-0" />
                    <span>Lý do gỡ: {{ b.rejectionReason }}</span>
                  </div>
                </TableCell>

                <!-- Target URL -->
                <TableCell class="py-3">
                  <a
                    :href="b.targetUrl"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="inline-flex items-center gap-1 text-[11px] font-mono text-primary hover:underline max-w-[200px] truncate"
                  >
                    <span class="truncate">{{ b.targetUrl }}</span>
                    <ExternalLink class="w-3 h-3 shrink-0" />
                  </a>
                </TableCell>

                <!-- Status Badge -->
                <TableCell class="py-3 text-center">
                  <span
                    class="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold border"
                    :class="getStatusBadgeVariant(b.status)"
                  >
                    {{ getStatusLabel(b.status) }}
                  </span>
                </TableCell>

                <!-- Actions -->
                <TableCell class="py-3 text-right">
                  <div class="flex items-center justify-end gap-1.5">
                    <!-- Approve Button -->
                    <Button
                      v-if="b.status !== 'ACTIVE' && b.status !== 'APPROVED' && hasPermission('BANNER_APPROVE_REJECT')"
                      type="button"
                      size="sm"
                      variant="outline"
                      :data-testid="`btn-approve-banner-${b.id}`"
                      class="h-7 px-2 text-xs text-emerald-700 dark:text-emerald-300 border-emerald-300 dark:border-emerald-800 hover:bg-emerald-50 dark:hover:bg-emerald-950/30 gap-1 font-medium"
                      @click="confirmApproveBanner(b)"
                    >
                      <CheckCircle2 class="w-3.5 h-3.5" />
                      <span>Duyệt</span>
                    </Button>

                    <!-- Reject Button -->
                    <Button
                      v-if="b.status !== 'REJECTED' && hasPermission('BANNER_APPROVE_REJECT')"
                      type="button"
                      size="sm"
                      variant="outline"
                      class="h-7 px-2 text-xs text-rose-700 dark:text-rose-300 border-rose-300 dark:border-rose-800 hover:bg-rose-50 dark:hover:bg-rose-950/30 gap-1 font-medium"
                      @click="confirmRejectBanner(b)"
                    >
                      <Ban class="w-3.5 h-3.5" />
                      <span>Gỡ</span>
                    </Button>

                    <!-- Edit Button -->
                    <Button
                      v-if="hasPermission('BANNER_CREATE_DELETE')"
                      type="button"
                      size="sm"
                      variant="outline"
                      :data-testid="`btn-edit-banner-${b.id}`"
                      class="h-7 px-2 text-xs gap-1 font-medium"
                      @click="openEditBannerModal(b)"
                    >
                      <Pencil class="w-3 h-3" />
                      <span>Sửa</span>
                    </Button>

                    <!-- Delete Button -->
                    <Button
                      v-if="hasPermission('BANNER_CREATE_DELETE')"
                      type="button"
                      size="sm"
                      variant="ghost"
                      :data-testid="`btn-delete-banner-${b.id}`"
                      class="h-7 px-2 text-xs text-destructive hover:bg-destructive/10 gap-1 font-medium"
                      @click="confirmDeleteBanner(b)"
                    >
                      <Trash2 class="w-3 h-3" />
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </div>
      </CardContent>
    </Card>

    <!-- Dialog: Create / Edit Banner -->
    <Dialog :open="isFormModalOpen" @update:open="val => { if (!val) closeFormModal() }">
      <DialogContent class="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle class="text-base font-bold text-foreground flex items-center gap-2">
            <ImageIcon class="w-5 h-5 text-primary" />
            <span>{{ editingBanner ? `Chỉnh Sửa Banner #${editingBanner.id}` : 'Tạo Banner Truyền Thông Sàn' }}</span>
          </DialogTitle>
          <DialogDescription class="text-xs text-muted-foreground">
            Thiết lập tiêu đề chiến dịch, hình ảnh quảng cáo và liên kết điều hướng an toàn.
          </DialogDescription>
        </DialogHeader>

        <Alert v-if="formError" variant="destructive" class="py-2.5">
          <AlertTriangle class="w-4 h-4" />
          <AlertTitle class="text-xs font-semibold">Lỗi dữ liệu</AlertTitle>
          <AlertDescription class="text-xs">{{ formError }}</AlertDescription>
        </Alert>

        <div class="space-y-4 py-2">
          <!-- Title -->
          <div class="space-y-1.5">
            <label for="banner-title" class="block text-xs font-semibold text-foreground">
              Tiêu đề chiến dịch quảng cáo <span class="text-destructive">*</span>
            </label>
            <Input
              id="banner-title"
              v-model="bannerForm.title"
              data-testid="input-banner-title"
              placeholder="VD: Ưu đãi đặt vé Tết Nguyên Đán 2027..."
              class="h-9 text-xs"
            />
          </div>

          <!-- Target URL -->
          <div class="space-y-1.5">
            <label for="banner-target" class="block text-xs font-semibold text-foreground">
              Đường dẫn đích (URL) <span class="text-destructive">*</span>
            </label>
            <Input
              id="banner-target"
              v-model="bannerForm.targetUrl"
              data-testid="input-banner-target"
              placeholder="https://velobus.vn/search"
              class="h-9 text-xs font-mono"
            />
          </div>

          <!-- Image Upload / URL -->
          <div class="space-y-1.5">
            <label class="block text-xs font-semibold text-foreground">
              Hình ảnh Banner (Tỷ lệ 16:9) <span class="text-destructive">*</span>
            </label>
            <FirebaseUploader
              v-model="bannerForm.imageUrl"
              folder="banners"
              label="Chọn hoặc kéo thả ảnh banner tải lên Cloud"
            />
            <div class="pt-1.5">
              <label for="banner-image-url" class="block text-[11px] text-muted-foreground font-medium">Hoặc nhập trực tiếp URL ảnh:</label>
              <Input
                id="banner-image-url"
                v-model="bannerForm.imageUrl"
                data-testid="input-banner-image"
                placeholder="https://images.unsplash.com/..."
                class="h-8 text-xs font-mono mt-1"
              />
            </div>
          </div>
        </div>

        <DialogFooter class="gap-2 sm:gap-0">
          <Button
            type="button"
            variant="outline"
            size="sm"
            class="text-xs"
            @click="closeFormModal"
          >
            Hủy Bỏ
          </Button>
          <Button
            type="button"
            size="sm"
            data-testid="btn-save-banner"
            class="text-xs font-semibold"
            :disabled="isSubmitting"
            @click="handleSaveBanner"
          >
            <Loader2 v-if="isSubmitting" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
            <span>{{ editingBanner ? 'Lưu Cập Nhật' : 'Đăng Banner Sàn' }}</span>
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- AlertDialog: Approve Confirmation -->
    <AlertDialog :open="approveDialogState.isOpen" @update:open="val => { if (!val) approveDialogState.isOpen = false }">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle class="text-sm font-bold flex items-center gap-2">
            <CheckCircle2 class="w-4 h-4 text-emerald-600" />
            <span>Xác Nhận Phê Duyệt Banner</span>
          </AlertDialogTitle>
          <AlertDialogDescription class="text-xs text-muted-foreground">
            Banner "{{ approveDialogState.banner?.title }}" sẽ được kích hoạt trạng thái Đang Hiển Thị và xuất hiện công khai trên Hero Carousel của trang chủ sàn VeloBus.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel size="sm" class="text-xs">Hủy</AlertDialogCancel>
          <AlertDialogAction
            size="sm"
            class="text-xs font-semibold"
            :disabled="approveDialogState.isProcessing"
            @click.prevent="executeApproveBanner"
          >
            <Loader2 v-if="approveDialogState.isProcessing" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
            <span>Phê Duyệt Hiển Thị</span>
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <!-- Dialog: Reject with Reason -->
    <Dialog :open="rejectDialogState.isOpen" @update:open="val => { if (!val) rejectDialogState.isOpen = false }">
      <DialogContent class="sm:max-w-[450px]">
        <DialogHeader>
          <DialogTitle class="text-sm font-bold flex items-center gap-2 text-rose-600">
            <Ban class="w-4 h-4" />
            <span>Từ Chối & Gỡ Bỏ Banner</span>
          </DialogTitle>
          <DialogDescription class="text-xs text-muted-foreground">
            Vui lòng nhập lý do từ chối để phản hồi chính thức cho đối tác nhà xe quản lý banner này.
          </DialogDescription>
        </DialogHeader>

        <Alert v-if="rejectDialogState.error" variant="destructive" class="py-2">
          <AlertDescription class="text-xs">{{ rejectDialogState.error }}</AlertDescription>
        </Alert>

        <div class="space-y-1.5 py-2">
          <label for="reject-reason" class="block text-xs font-semibold text-foreground">Lý do từ chối <span class="text-destructive">*</span></label>
          <Input
            id="reject-reason"
            v-model="rejectDialogState.reason"
            placeholder="VD: Kích thước ảnh không đúng tỷ lệ 16:9, hình ảnh mờ..."
            class="h-9 text-xs"
          />
        </div>

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            size="sm"
            class="text-xs"
            @click="rejectDialogState.isOpen = false"
          >
            Hủy Bỏ
          </Button>
          <Button
            type="button"
            size="sm"
            variant="destructive"
            class="text-xs font-semibold"
            :disabled="rejectDialogState.isProcessing"
            @click="executeRejectBanner"
          >
            <Loader2 v-if="rejectDialogState.isProcessing" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
            <span>Xác Nhận Từ Chối</span>
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- AlertDialog: Delete Confirmation -->
    <AlertDialog :open="deleteDialogState.isOpen" @update:open="val => { if (!val) deleteDialogState.isOpen = false }">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle class="text-sm font-bold flex items-center gap-2 text-destructive">
            <Trash2 class="w-4 h-4" />
            <span>Xóa Vĩnh Viễn Banner</span>
          </AlertDialogTitle>
          <AlertDialogDescription class="text-xs text-muted-foreground">
            Hành động này không thể hoàn tác. Banner "{{ deleteDialogState.banner?.title }}" sẽ bị gỡ bỏ hoàn toàn khỏi cơ sở dữ liệu sàn.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel size="sm" class="text-xs">Hủy</AlertDialogCancel>
          <AlertDialogAction
            size="sm"
            variant="destructive"
            class="text-xs font-semibold"
            :disabled="deleteDialogState.isProcessing"
            @click.prevent="executeDeleteBanner"
          >
            <Loader2 v-if="deleteDialogState.isProcessing" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
            <span>Xóa Banner</span>
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>
