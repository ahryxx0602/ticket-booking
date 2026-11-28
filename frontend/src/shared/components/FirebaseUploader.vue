<template>
  <div class="space-y-2">
    <div
      class="border-2 border-dashed rounded-xl p-4 text-center transition cursor-pointer bg-card"
      :class="[
        uploading ? 'border-primary/60 bg-muted/20 cursor-wait' : 'border-border hover:border-primary',
        previewFailed ? 'border-destructive/60' : ''
      ]"
      @click="handleContainerClick"
    >
      <input
        ref="fileInput"
        type="file"
        accept="image/jpeg,image/png,image/webp,image/gif"
        class="hidden"
        :disabled="uploading"
        @change="onFileSelected"
      />

      <!-- State 1: Uploading in progress -->
      <div v-if="uploading" class="py-3 text-sm text-primary font-medium flex items-center justify-center gap-2">
        <Loader2 class="w-5 h-5 animate-spin" />
        <span>Đang xác thực và tải ảnh lên máy chủ lưu trữ Cloud...</span>
      </div>

      <!-- State 2: Has Image (Preview mode) -->
      <div v-else-if="modelValue && !previewFailed" class="space-y-2">
        <div class="relative max-h-40 mx-auto overflow-hidden rounded-lg border border-border bg-muted/20 flex items-center justify-center">
          <Loader2 v-if="previewLoading" class="w-6 h-6 animate-spin text-muted-foreground my-6" />
          <img
            :src="modelValue"
            alt="Xem trước hình ảnh banner"
            class="max-h-36 mx-auto rounded-lg object-contain transition-opacity duration-200"
            data-testid="img-uploader-preview"
            @load="onImageLoad"
            @error="onImageError"
          />
        </div>

        <!-- Honest Status Badge: Distinguish actual cloud upload vs externally entered URL -->
        <p v-if="isCloudUploaded" class="text-xs text-emerald-600 dark:text-emerald-400 font-medium flex items-center justify-center gap-1">
          <CheckCircle2 class="w-3.5 h-3.5 shrink-0" />
          <span>Tệp ảnh hợp lệ đã tải lên Cloud CDN • Nhấn để thay đổi</span>
        </p>
        <p v-else class="text-xs text-muted-foreground font-medium flex items-center justify-center gap-1">
          <ImageIcon class="w-3.5 h-3.5 shrink-0" />
          <span>Đường dẫn ảnh đang hoạt động • Nhấn để tải tệp mới lên Cloud</span>
        </p>
      </div>

      <!-- State 3: Broken image / Preview Failed -->
      <div v-else-if="modelValue && previewFailed" class="py-3 space-y-1 text-destructive">
        <AlertTriangle class="w-7 h-7 mx-auto text-destructive" />
        <p class="text-xs font-semibold">Không thể hiển thị hình ảnh</p>
        <p class="text-[11px] text-muted-foreground">URL không tồn tại, sai định dạng hoặc bị chặn truy cập. Nhấn để chọn tệp khác.</p>
      </div>

      <!-- State 4: Empty Initial State -->
      <div v-else class="py-3 space-y-1">
        <UploadCloud class="w-8 h-8 mx-auto text-muted-foreground" />
        <p class="text-sm font-medium text-foreground">{{ label }}</p>
        <p class="text-xs text-muted-foreground">Hỗ trợ PNG, JPG, WebP, GIF (Tối đa 10MB • Kiểm tra chữ ký nhị phân)</p>
      </div>
    </div>

    <!-- Error message display -->
    <p v-if="errorMsg" class="text-xs text-destructive font-medium flex items-center gap-1" data-testid="uploader-error-msg">
      <AlertCircle class="w-3.5 h-3.5 shrink-0" />
      <span>{{ errorMsg }}</span>
    </p>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Loader2, UploadCloud, CheckCircle2, AlertTriangle, AlertCircle, Image as ImageIcon } from 'lucide-vue-next'
import httpClient from '@/shared/api/httpClient'

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  folder: {
    type: String,
    default: 'banners',
  },
  label: {
    type: String,
    default: 'Nhấn để chọn ảnh tải lên Cloud',
  },
})

const emit = defineEmits(['update:modelValue', 'uploaded', 'upload-start', 'upload-end'])

const fileInput = ref(null)
const uploading = ref(false)
const errorMsg = ref('')
const isCloudUploaded = ref(false)
const previewLoading = ref(false)
const previewFailed = ref(false)

const ALLOWED_MIME_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024 // 10 MB

watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    previewLoading.value = true
    previewFailed.value = false
  } else {
    previewLoading.value = false
    previewFailed.value = false
    isCloudUploaded.value = false
  }
})

function handleContainerClick() {
  if (!uploading.value) {
    fileInput.value?.click()
  }
}

function onImageLoad(e) {
  previewLoading.value = false
  const img = e.target
  if (img && (img.naturalWidth === 0 || img.naturalHeight === 0)) {
    previewFailed.value = true
  } else {
    previewFailed.value = false
  }
}

function onImageError() {
  previewLoading.value = false
  previewFailed.value = true
}

async function onFileSelected(event) {
  const file = event.target.files?.[0]
  // Reset input immediately so selecting the exact same file again re-triggers @change
  event.target.value = ''

  if (!file) return

  errorMsg.value = ''
  previewFailed.value = false

  // Client-side MIME validation matching PublicUploadController
  if (!ALLOWED_MIME_TYPES.includes(file.type.toLowerCase())) {
    errorMsg.value = 'Chỉ chấp nhận các định dạng tệp ảnh JPEG, PNG, WebP hoặc GIF.'
    return
  }

  // Client-side 10MB file size validation
  if (file.size > MAX_FILE_SIZE_BYTES) {
    errorMsg.value = 'Dung lượng tệp vượt quá giới hạn tối đa cho phép (10MB).'
    return
  }

  uploading.value = true
  emit('upload-start')

  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('folder', props.folder)

    const res = await httpClient.post('/public/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })

    const payload = res?.data?.data || res?.data
    const uploadedUrl = payload?.url || ''

    if (!uploadedUrl || typeof uploadedUrl !== 'string' || (!uploadedUrl.startsWith('http') && !uploadedUrl.startsWith('data:image/'))) {
      throw new Error('Máy chủ lưu trữ không trả về đường dẫn URL tệp hợp lệ.')
    }

    isCloudUploaded.value = true
    previewFailed.value = false
    previewLoading.value = true

    emit('update:modelValue', uploadedUrl)
    emit('uploaded', uploadedUrl)
  } catch (err) {
    errorMsg.value = err?.response?.data?.message || err?.message || 'Lỗi tải ảnh lên dịch vụ lưu trữ đám mây.'
    isCloudUploaded.value = false
  } finally {
    uploading.value = false
    emit('upload-end')
  }
}

defineExpose({
  uploading,
  previewFailed,
  isCloudUploaded
})
</script>
