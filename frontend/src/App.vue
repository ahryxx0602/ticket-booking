<template>
  <ConfigProvider :scroll-body="false">
    <Toast />
    <Sonner position="top-right" richColors />
    <router-view />
  </ConfigProvider>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ConfigProvider } from 'reka-ui'
import Toast from 'primevue/toast'
import { Sonner } from '@/components/ui/sonner'
import { useAuthStore } from '@/modules/auth/store'

const router = useRouter()
const authStore = useAuthStore()

function handleUnauthorized() {
  authStore.logout()
  if (router && router.currentRoute?.value?.path !== '/login') {
    router.push({
      path: '/login',
      query: {
        redirect: router.currentRoute.value.fullPath,
        reason: 'session_expired',
      },
    })
  }
}

onMounted(() => {
  if (typeof window !== 'undefined') {
    window.addEventListener('velobus:unauthorized', handleUnauthorized)
  }
})

onUnmounted(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('velobus:unauthorized', handleUnauthorized)
  }
})
</script>
