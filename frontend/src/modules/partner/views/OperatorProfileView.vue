<script setup>
import { ref, computed, onMounted } from 'vue'
import { partnerApi } from '@/modules/partner/api'
import { toast } from 'vue-sonner'
import {
  Building2,
  Mail,
  Phone,
  FileText,
  Image as ImageIcon,
  ShieldCheck,
  CheckCircle,
  AlertTriangle,
  RefreshCw,
  Save,
  Lock,
  Info,
  CreditCard,
  Percent
} from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert'
import FirebaseUploader from '@/shared/components/FirebaseUploader.vue'

// State
const isLoading = ref(false)
const isSaving = ref(false)
const loadError = ref('')
const formError = ref('')

// Authoritative Operator Profile from Backend
const profile = ref(null)

// Mutable form values
const form = ref({
  brandName: '',
  contactPhone: '',
  contactEmail: '',
  logoUrl: '',
  description: ''
})

async function fetchProfile() {
  isLoading.value = true
  loadError.value = ''
  try {
    const res = await partnerApi.getProfile()
    const data = res?.data?.data || res?.data
    if (!data) {
      throw new Error('Dữ liệu hồ sơ nhà xe trả về rỗng từ máy chủ')
    }
    profile.value = data
    form.value = {
      brandName: data.brandName || '',
      contactPhone: data.contactPhone || '',
      contactEmail: data.contactEmail || '',
      logoUrl: data.logoUrl || '',
      description: data.description || ''
    }
  } catch (err) {
    loadError.value = err?.response?.data?.message || err?.message || 'Không thể tải hồ sơ đối tác từ máy chủ'
    toast.error('Lỗi khi tải hồ sơ đối tác: ' + loadError.value)
  } finally {
    isLoading.value = false
  }
}

async function handleSaveProfile() {
  formError.value = ''

  const brand = form.value.brandName?.trim()
  if (!brand) {
    formError.value = 'Tên thương hiệu nhà xe không được để trống'
    return
  }

  const phone = form.value.contactPhone?.trim()
  const phoneRegex = /(84|0[3|5|7|8|9])+([0-9]{8})\b/
  if (!phone || !phoneRegex.test(phone)) {
    formError.value = 'Số điện thoại liên hệ không đúng định dạng (10 số VN)'
    return
  }

  const email = form.value.contactEmail?.trim()
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!email || !emailRegex.test(email)) {
    formError.value = 'Email liên hệ không đúng định dạng hợp lệ'
    return
  }

  const payload = {
    brandName: brand,
    contactPhone: phone,
    contactEmail: email,
    logoUrl: form.value.logoUrl || '',
    description: form.value.description?.trim() || ''
  }

  isSaving.value = true
  try {
    const res = await partnerApi.updateProfile(payload)
    const updated = res?.data?.data || res?.data || payload
    
    // Update local profile state
    if (profile.value) {
      Object.assign(profile.value, updated)
    }

    toast.success('Cập nhật hồ sơ nhà xe thành công')
  } catch (err) {
    formError.value = err?.response?.data?.message || err?.message || 'Lỗi khi lưu thông tin hồ sơ nhà xe'
    toast.error(formError.value)
  } finally {
    isSaving.value = false
  }
}

function formatCommission(rate) {
  if (rate == null) return 'Mặc định toàn sàn (10.0%)'
  return `${Number(rate).toFixed(1)}%`
}

function getStatusBadge(status) {
  switch (status) {
    case 'ACTIVE':
      return { text: 'Đang Hoạt Động', class: 'bg-emerald-500/10 text-emerald-600 border-emerald-500/20' }
    case 'PENDING_APPROVAL':
      return { text: 'Chờ Duyệt Hồ Sơ', class: 'bg-amber-500/10 text-amber-600 border-amber-500/20' }
    case 'SUSPENDED':
      return { text: 'Tạm Ngưng', class: 'bg-destructive/10 text-destructive border-destructive/20' }
    case 'REJECTED':
      return { text: 'Từ Chối Hồ Sơ', class: 'bg-destructive/10 text-destructive border-destructive/20' }
    default:
      return { text: status || 'Chưa rõ', class: 'bg-muted text-muted-foreground' }
  }
}

onMounted(() => {
  // Purge any legacy unsupported persistent bank profile keys (FQA-04)
  try {
    for (let i = localStorage.length - 1; i >= 0; i--) {
      const key = localStorage.key(i)
      if (key && key.startsWith('velobus_partner_bank_')) {
        localStorage.removeItem(key)
      }
    }
  } catch (_) {}

  fetchProfile()
})
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-border">
      <div>
        <h1 class="text-2xl font-bold tracking-tight text-foreground flex items-center gap-2">
          <Building2 class="w-6 h-6 text-primary" />
          <span>Hồ Sơ Doanh Nghiệp & Định Danh Nhà Xe</span>
        </h1>
        <p class="text-xs text-muted-foreground mt-1">
          Quản lý thông tin pháp nhân đăng ký kinh doanh vận tải, thương hiệu hiển thị và kênh liên lạc chính thức.
        </p>
      </div>

      <div class="flex items-center gap-2">
        <Button
          variant="outline"
          size="sm"
          class="h-9 text-xs"
          @click="fetchProfile"
          :disabled="isLoading || isSaving"
        >
          <RefreshCw class="w-3.5 h-3.5 mr-1" /> Làm mới
        </Button>
        <Button
          size="sm"
          data-testid="btn-save-profile"
          class="bg-primary hover:bg-primary/90 text-primary-foreground text-xs font-semibold h-9"
          :disabled="isLoading || isSaving || !profile"
          @click="handleSaveProfile"
        >
          <RefreshCw v-if="isSaving" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
          <Save v-else class="w-3.5 h-3.5 mr-1.5" />
          Lưu Thông Tin
        </Button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="p-16 text-center border rounded-xl bg-card">
      <RefreshCw class="w-6 h-6 mx-auto animate-spin mb-3 text-primary" />
      <p class="text-xs text-muted-foreground">Đang tải hồ sơ doanh nghiệp từ máy chủ...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="loadError" class="p-8 text-center border border-destructive/30 rounded-xl bg-destructive/5 space-y-3">
      <AlertTriangle class="w-8 h-8 mx-auto text-destructive" />
      <h3 class="text-sm font-bold text-destructive">Không thể nạp hồ sơ đối tác</h3>
      <p class="text-xs text-muted-foreground max-w-md mx-auto">{{ loadError }}</p>
      <Button variant="outline" size="sm" class="text-xs" data-testid="btn-retry" @click="fetchProfile">
        <RefreshCw class="w-3.5 h-3.5 mr-1" /> Thử lại
      </Button>
    </div>

    <!-- Profile Content -->
    <div v-else-if="profile" class="space-y-6">
      <div v-if="formError" class="p-3 bg-destructive/10 border border-destructive/20 rounded-lg text-xs text-destructive flex items-center gap-2">
        <AlertTriangle class="w-4 h-4 shrink-0" />
        <span>{{ formError }}</span>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Cột Trái: Logo, Nhận diện & Trạng thái pháp lý -->
        <div class="space-y-6">
          <div class="bg-card border border-border rounded-xl p-5 shadow-sm space-y-4">
            <h3 class="text-xs font-bold uppercase tracking-wider text-foreground pb-2 border-b border-border flex items-center gap-2">
              <ImageIcon class="w-4 h-4 text-primary" />
              <span>Logo & Nhận Diện Thương Hiệu</span>
            </h3>

            <div class="flex flex-col items-center gap-3">
              <div v-if="form.logoUrl" class="w-28 h-28 rounded-xl border border-border overflow-hidden bg-muted flex items-center justify-center p-1">
                <img :src="form.logoUrl" alt="Logo nhà xe" class="w-full h-full object-contain rounded-lg" />
              </div>
              <div v-else class="w-28 h-28 rounded-xl border border-dashed border-border flex flex-col items-center justify-center text-muted-foreground/60 text-xs gap-1">
                <ImageIcon class="w-8 h-8" />
                <span>Chưa có logo</span>
              </div>

              <div class="w-full">
                <FirebaseUploader
                  v-model="form.logoUrl"
                  folder="operator-logos"
                  label="Tải lên Logo Mới (PNG/JPG)"
                />
              </div>
            </div>

            <!-- Thẻ tóm tắt đối tác -->
            <div class="p-3 bg-muted/30 border border-border rounded-lg text-xs space-y-2">
              <div class="flex items-center justify-between">
                <span class="text-muted-foreground">Trạng thái hồ sơ:</span>
                <Badge :class="getStatusBadge(profile.status).class" class="text-[10px] font-semibold">
                  {{ getStatusBadge(profile.status).text }}
                </Badge>
              </div>

              <div class="flex items-center justify-between">
                <span class="text-muted-foreground">Mã đối tác (ID):</span>
                <span class="font-mono font-bold text-foreground">#{{ profile.id }}</span>
              </div>

              <div class="flex items-center justify-between">
                <span class="text-muted-foreground">Tỷ lệ hoa hồng sàn:</span>
                <span class="font-mono font-bold text-primary">
                  {{ formatCommission(profile.commissionRate) }}
                </span>
              </div>
            </div>

            <div v-if="profile.rejectionReason" class="p-3 bg-destructive/10 border border-destructive/20 rounded-lg text-xs text-destructive space-y-1">
              <span class="font-bold">Lý do từ chối:</span>
              <p>{{ profile.rejectionReason }}</p>
            </div>
          </div>
        </div>

        <!-- Cột Phải: Thông Tin Chi Tiết & Ngân Hàng -->
        <div class="lg:col-span-2 space-y-6">
          <!-- Khối 1: Thông tin pháp lý & Liên hệ -->
          <div class="bg-card border border-border rounded-xl p-5 shadow-sm space-y-4">
            <h3 class="text-xs font-bold uppercase tracking-wider text-foreground pb-2 border-b border-border flex items-center justify-between">
              <div class="flex items-center gap-2">
                <FileText class="w-4 h-4 text-primary" />
                <span>Thông Tin Pháp Lý & Liên Hệ Vận Hành</span>
              </div>
              <Badge variant="outline" class="text-[10px] font-normal text-muted-foreground">
                Chuẩn Bộ GTVT
              </Badge>
            </h3>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <!-- IMMUTABLE: Tên pháp nhân -->
              <div>
                <div class="flex items-center justify-between">
                  <label class="text-xs font-semibold text-foreground">Tên pháp nhân đăng ký KD</label>
                  <span class="text-[10px] text-muted-foreground flex items-center gap-1 font-mono">
                    <Lock class="w-3 h-3 text-muted-foreground" /> Bất biến theo ĐKKD
                  </span>
                </div>
                <Input
                  :model-value="profile.businessName"
                  disabled
                  class="mt-1 bg-muted/40 font-semibold text-xs cursor-not-allowed"
                />
              </div>

              <!-- MUTABLE: Tên thương hiệu -->
              <div>
                <label class="text-xs font-semibold text-foreground">Tên thương hiệu hiển thị <span class="text-destructive">*</span></label>
                <Input
                  v-model="form.brandName"
                  placeholder="VD: Phương Trang FUTA Bus Lines"
                  class="mt-1 text-xs font-semibold"
                  :disabled="isSaving"
                />
              </div>

              <!-- IMMUTABLE: Mã số thuế -->
              <div>
                <div class="flex items-center justify-between">
                  <label class="text-xs font-semibold text-foreground">Mã số thuế (Tax Code)</label>
                  <span class="text-[10px] text-muted-foreground flex items-center gap-1 font-mono">
                    <Lock class="w-3 h-3 text-muted-foreground" /> Bất biến
                  </span>
                </div>
                <Input
                  :model-value="profile.taxCode"
                  disabled
                  class="mt-1 bg-muted/40 font-mono font-bold text-xs cursor-not-allowed"
                />
              </div>

              <!-- MUTABLE: Số điện thoại hotline -->
              <div>
                <label class="text-xs font-semibold text-foreground">Số điện thoại hotline <span class="text-destructive">*</span></label>
                <Input
                  v-model="form.contactPhone"
                  type="tel"
                  placeholder="VD: 1900 6067"
                  class="mt-1 font-mono text-xs"
                  :disabled="isSaving"
                />
              </div>

              <!-- MUTABLE: Email tiếp nhận đối soát -->
              <div class="md:col-span-2">
                <label class="text-xs font-semibold text-foreground">Email tiếp nhận đối soát & thông báo vé <span class="text-destructive">*</span></label>
                <Input
                  v-model="form.contactEmail"
                  type="email"
                  placeholder="VD: hotro@velobus.vn"
                  class="mt-1 text-xs font-mono"
                  :disabled="isSaving"
                />
              </div>

              <!-- MUTABLE: Giới thiệu nhà xe -->
              <div class="md:col-span-2">
                <label class="text-xs font-semibold text-foreground">Mô tả giới thiệu dịch vụ nhà xe</label>
                <Textarea
                  v-model="form.description"
                  :rows="3"
                  placeholder="Mô tả tiêu chuẩn an toàn, các tuyến phục vụ, tiện ích trên xe..."
                  class="mt-1 text-xs"
                  :disabled="isSaving"
                />
              </div>
            </div>
          </div>

          <!-- Khối 2: Cơ chế tài khoản thụ hưởng giải ngân (FQA-04) -->
          <div class="bg-card border border-border rounded-xl p-5 shadow-sm space-y-4">
            <h3 class="text-xs font-bold uppercase tracking-wider text-foreground pb-2 border-b border-border flex items-center justify-between">
              <div class="flex items-center gap-2">
                <CreditCard class="w-4 h-4 text-primary" />
                <span>Quy Trình Giải Ngân Ví Escrow & Tài Khoản Thụ Hưởng</span>
              </div>
              <Badge variant="outline" class="text-[10px] text-muted-foreground">
                Chuẩn Đối Soát
              </Badge>
            </h3>

            <Alert class="border-border bg-muted/20">
              <Info class="h-4 w-4 text-primary" />
              <AlertTitle class="text-xs font-semibold text-foreground">Chính sách bảo mật tài chính</AlertTitle>
              <AlertDescription class="text-xs text-muted-foreground leading-relaxed mt-1">
                Theo quy chuẩn vận hành an toàn tài chính, thông tin tài khoản ngân hàng thụ hưởng không lưu trữ cố định trong hồ sơ doanh nghiệp. Thông tin tài khoản được nhập và xác thực độc lập trên từng yêu cầu rút tiền tại màn hình <strong>Ví Đối Tác</strong> (<code>POST /partners/wallet/withdraw</code>) nhằm bảo đảm tính toàn vẹn kiểm toán và đối soát giao dịch giải ngân.
              </AlertDescription>
            </Alert>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
