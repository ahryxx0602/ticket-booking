<script setup>
import { ref } from 'vue'
import { Bell, Send, CheckCheck, X } from 'lucide-vue-next'
import { useNotificationStore } from '@/shared/stores/notificationStore'
import TelegramLinkModal from './TelegramLinkModal.vue'

const notiStore = useNotificationStore()

const isOpen = ref(false)
const showTelegramModal = ref(false)

function toggleDropdown() {
  isOpen.value = !isOpen.value
}

function closeDropdown() {
  isOpen.value = false
}

function handleItemClick(noti) {
  notiStore.markAsRead(noti.id)
}
</script>

<template>
  <div class="relative">
    <!-- Bell Button -->
    <button
      type="button"
      @click="toggleDropdown"
      data-testid="btn-notification-bell"
      class="w-9 h-9 rounded-lg border border-border bg-card hover:bg-muted flex items-center justify-center text-foreground relative transition-colors shadow-xs"
      title="Thông báo hệ thống"
      aria-label="Thông báo hệ thống"
    >
      <Bell class="w-4 h-4 text-foreground" />

      <!-- Unread Badge -->
      <span
        v-if="notiStore.unreadCount > 0"
        data-testid="badge-unread-count"
        class="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 rounded-full bg-destructive text-destructive-foreground font-extrabold text-[10px] flex items-center justify-center ring-2 ring-background tabular-nums"
      >
        {{ notiStore.unreadCount > 9 ? '9+' : notiStore.unreadCount }}
      </span>
    </button>

    <!-- Dropdown Panel -->
    <div
      v-if="isOpen"
      data-testid="notification-dropdown"
      class="absolute right-0 mt-2 w-80 sm:w-96 bg-card rounded-xl shadow-xl border border-border z-50 overflow-hidden flex flex-col"
    >
      <!-- Header -->
      <div class="p-3.5 border-b border-border flex items-center justify-between bg-muted/40">
        <div class="flex items-center gap-2">
          <span class="text-sm font-bold text-foreground">Thông Báo</span>
          <span
            v-if="notiStore.unreadCount > 0"
            class="px-2 py-0.5 rounded-full bg-primary/15 text-primary font-bold text-[10px] tabular-nums"
          >
            {{ notiStore.unreadCount }} mới
          </span>
        </div>

        <button
          v-if="notiStore.unreadCount > 0"
          type="button"
          @click="notiStore.markAllAsRead"
          data-testid="btn-mark-all-read"
          class="text-[11px] text-primary hover:underline font-semibold flex items-center gap-1"
        >
          <CheckCheck class="w-3.5 h-3.5" />
          <span>Đã đọc tất cả</span>
        </button>
      </div>

      <!-- Telegram Link Banner Bar -->
      <div class="p-3 bg-sky-500/10 border-b border-sky-500/20 flex items-center justify-between text-xs">
        <div class="flex items-center gap-2 text-foreground">
          <Send class="w-3.5 h-3.5 text-sky-600 dark:text-sky-400 flex-shrink-0" />
          <span class="text-muted-foreground text-[11px]">
            {{ notiStore.isTelegramLinked ? `Đã nối Telegram (${notiStore.telegramUsername})` : 'Nhận thông báo qua Telegram' }}
          </span>
        </div>
        <button
          type="button"
          @click="showTelegramModal = true"
          data-testid="btn-open-telegram-modal"
          class="text-[11px] font-bold text-sky-600 dark:text-sky-400 hover:underline flex-shrink-0"
        >
          {{ notiStore.isTelegramLinked ? 'Đổi tài khoản' : 'Kết nối ngay' }}
        </button>
      </div>

      <!-- Notifications List -->
      <div class="max-h-80 overflow-y-auto divide-y divide-border">
        <div
          v-for="item in notiStore.notifications"
          :key="item.id"
          @click="handleItemClick(item)"
          class="p-3.5 hover:bg-muted/50 cursor-pointer transition-colors flex items-start gap-3"
          :class="{ 'bg-primary/5': !item.read }"
        >
          <span class="text-base flex-shrink-0 p-2 rounded-lg bg-muted text-foreground flex items-center justify-center">{{ item.icon }}</span>
          <div class="space-y-1 flex-1 min-w-0">
            <div class="flex items-center justify-between gap-1">
              <h4 class="text-xs font-bold text-foreground truncate" :class="{ 'text-primary': !item.read }">
                {{ item.title }}
              </h4>
              <span class="text-[10px] text-muted-foreground flex-shrink-0">{{ item.createdAt }}</span>
            </div>
            <p class="text-[11px] text-muted-foreground leading-snug line-clamp-2">
              {{ item.message }}
            </p>
          </div>
          <span v-if="!item.read" class="w-2 h-2 rounded-full bg-primary flex-shrink-0 mt-1.5"></span>
        </div>

        <div v-if="notiStore.notifications.length === 0" class="p-8 text-center text-xs text-muted-foreground">
          Bạn chưa có thông báo mới nào.
        </div>
      </div>
    </div>

    <!-- Telegram Link Modal -->
    <TelegramLinkModal
      v-if="showTelegramModal"
      @close="showTelegramModal = false"
    />
  </div>
</template>
