<script setup>
import { ref, onMounted } from 'vue'
import QRCode from 'qrcode'
import { Send, X, ShieldCheck } from 'lucide-vue-next'
import { useNotificationStore } from '@/shared/stores/notificationStore'
import AppButton from '@/shared/components/AppButton.vue'

const emit = defineEmits(['close'])
const notiStore = useNotificationStore()

const deepLinkToken = ref(`VB-TG-${Math.random().toString(36).substring(2, 10).toUpperCase()}`)
const telegramUrl = ref(`https://t.me/velobus_bot?start=${deepLinkToken.value}`)
const qrDataUrl = ref('')
const isChecking = ref(false)

onMounted(async () => {
  try {
    qrDataUrl.value = await QRCode.toDataURL(telegramUrl.value, {
      width: 200,
      margin: 2,
      color: {
        dark: '#141413',
        light: '#FFFFFF'
      }
    })
  } catch (err) {
    console.error('Lỗi sinh mã QR Telegram:', err)
  }
})

function handleSimulateLink() {
  isChecking.value = true
  setTimeout(() => {
    notiStore.linkTelegram('@thanh_phan_dev')
    isChecking.value = false
    emit('close')
  }, 1000)
}
</script>

<template>
  <div class="fixed inset-0 bg-background/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
    <div class="bg-card rounded-2xl max-w-sm w-full p-6 shadow-2xl border border-border text-center space-y-4">
      <div class="flex items-center justify-between pb-2 border-b border-border">
        <h3 class="text-sm font-bold text-foreground flex items-center gap-1.5">
          <Send class="w-4 h-4 text-sky-600 dark:text-sky-400" />
          <span>Liên Kết Bot Telegram VeloBus</span>
        </h3>
        <button
          type="button"
          @click="$emit('close')"
          class="p-1 rounded-md text-muted-foreground hover:text-foreground hover:bg-muted transition-colors"
        >
          <X class="w-4 h-4" />
        </button>
      </div>

      <p class="text-xs text-muted-foreground leading-relaxed">
        Quét mã QR hoặc bấm nút bên dưới để mở Telegram Bot nhận biến động số dư ví, mã OTP xác thực và cảnh báo vận hành tức thì.
      </p>

      <!-- QR Code Container -->
      <div class="p-3 bg-muted/40 rounded-xl border border-border flex justify-center">
        <img
          v-if="qrDataUrl"
          :src="qrDataUrl"
          alt="QR Telegram Deep Link"
          data-testid="img-telegram-qr"
          class="w-48 h-48 rounded-lg shadow-xs bg-white p-1"
        />
        <div v-else class="w-48 h-48 flex items-center justify-center text-xs text-muted-foreground">
          Đang sinh mã QR kết nối...
        </div>
      </div>

      <div class="space-y-2 pt-1">
        <a
          :href="telegramUrl"
          target="_blank"
          rel="noopener noreferrer"
          data-testid="link-open-telegram"
          class="block w-full py-2.5 bg-[#229ED9] hover:bg-[#1e8ec3] text-white font-semibold rounded-lg text-xs shadow-sm transition-all"
        >
          Mở Telegram Bot Ngay
        </a>

        <AppButton
          variant="secondary"
          class="w-full h-10 text-xs font-semibold"
          data-testid="btn-verify-telegram"
          :loading="isChecking"
          @click="handleSimulateLink"
        >
          Xác Nhận Đã Kết Nối
        </AppButton>
      </div>

      <p class="text-[11px] text-muted-foreground flex items-center justify-center gap-1 italic">
        <ShieldCheck class="w-3.5 h-3.5" />
        <span>Mã kết nối an toàn (One-time Token có thời hạn trong 5 phút)</span>
      </p>
    </div>
  </div>
</template>
