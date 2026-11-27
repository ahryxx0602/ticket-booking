<template>
  <div class="space-y-2">
    <div
      class="border-2 border-dashed border-border rounded-xl p-4 text-center hover:border-primary transition cursor-pointer bg-card"
      @click="fileInput?.click()"
    >
      <input
        ref="fileInput"
        type="file"
        accept="image/jpeg,image/png,image/webp,image/gif"
        class="hidden"
        @change="onFileSelected"
      />
      <div v-if="uploading" class="py-3 text-sm text-primary font-medium flex items-center justify-center gap-2">
        <Loader2 class="w-5 h-5 animate-spin" />
        <span>Đang tải ảnh lên Cloud CDN...</span>
      </div>
      <div v-else-if="modelValue" class="space-y-2">
        <img :src="modelValue" alt="Preview" class="max-h-36 mx-auto rounded-lg object-contain border border-border" />
        <p class="text-xs text-emerald-600 dark:text-emerald-400 font-medium flex items-center justify-center gap-1">
          <CheckCircle2 class="w-3.5 h-3.5" />
          <span>Đã tải lên Cloud CDN • Nhấn để thay đổi ảnh</span>
        </p>
      </div>
      <div v-else class="py-3 space-y-1">
        <UploadCloud class="w-8 h-8 mx-auto text-muted-foreground" />
        <p class="text-sm font-medium text-foreground">{{ label }}</p>
        <p class="text-xs text-muted-foreground">Hỗ trợ PNG, JPG, WebP, GIF (Tối đa 10MB • Lưu trữ Cloud Storage)</p>
      </div>
    </div>
    <p v-if="errorMsg" class="text-xs text-destructive font-medium">{{ errorMsg }}</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Loader2, UploadCloud, CheckCircle2 } from 'lucide-vue-next'
import httpClient from '@/shared/api/httpClient'

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  folder: {
    type: String,
    default: 'general',
  },
  label: {
    type: String,
    default: 'Nhấn để chọn ảnh tải lên Cloud',
  },
})

const emit = defineEmits(['update:modelValue', 'uploaded'])

const fileInput = ref(null)
const uploading = ref(false)
const errorMsg = ref('')

async function onFileSelected(event) {
  const file = event.target.files?.[0]
  if (!file) return

  errorMsg.value = ''
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('folder', props.folder)

    const res = await httpClient.post('/public/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })

    const uploadedUrl = res?.data?.url || ''
    emit('update:modelValue', uploadedUrl)
    emit('uploaded', uploadedUrl)
  } catch (err) {
    errorMsg.value = err?.message || 'Lỗi tải ảnh lên Cloud CDN'
  } finally {
    uploading.value = false
  }
}
</script>
