<template>
  <div class="space-y-2">
    <div
      class="border-2 border-dashed border-slate-300 rounded-xl p-4 text-center hover:border-blue-500 transition cursor-pointer bg-white"
      @click="fileInput?.click()"
    >
      <input
        ref="fileInput"
        type="file"
        accept="image/jpeg,image/png,image/webp,image/gif"
        class="hidden"
        @change="onFileSelected"
      />
      <div v-if="uploading" class="py-3 text-sm text-blue-600 font-medium flex items-center justify-center gap-2">
        <i class="pi pi-spin pi-spinner"></i>
        <span>Đang tải ảnh lên Firebase Storage...</span>
      </div>
      <div v-else-if="modelValue" class="space-y-2">
        <img :src="modelValue" alt="Preview" class="max-h-36 mx-auto rounded-lg object-contain border border-slate-200" />
        <p class="text-xs text-emerald-600 font-medium">Đã tải lên Cloud CDN • Nhấn để thay đổi ảnh</p>
      </div>
      <div v-else class="py-3 space-y-1">
        <i class="pi pi-cloud-upload text-2xl text-slate-400"></i>
        <p class="text-sm font-medium text-slate-700">{{ label }}</p>
        <p class="text-xs text-slate-400">Hỗ trợ PNG, JPG, WebP (Tối đa 10MB • Lưu trữ trên Firebase Cloud)</p>
      </div>
    </div>
    <p v-if="errorMsg" class="text-xs text-rose-600">{{ errorMsg }}</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
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
    errorMsg.value = err?.message || 'Lỗi tải ảnh lên Firebase Storage'
  } finally {
    uploading.value = false
  }
}
</script>
