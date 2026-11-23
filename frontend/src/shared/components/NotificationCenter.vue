<script setup>
import { ref } from 'vue'
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
      class="w-9 h-9 rounded-lg border border-border-default bg-surface hover:bg-canvas flex items-center justify-center text-text-secondary relative transition-colors"
      title="Thông báo hệ thống"
    >
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/>
      </svg>

      <!-- Unread Badge -->
      <span
        v-if="notiStore.unreadCount > 0"
        data-testid="badge-unread-count"
        class="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 rounded-full bg-rose-500 text-white font-extrabold text-[10px] flex items-center justify-center ring-2 ring-white tabular-nums"
      >
        {{ notiStore.unreadCount > 9 ? '9+' : notiStore.unreadCount }}
      </span>
    </button>

    <!-- Dropdown Panel -->
    <div
      v-if="isOpen"
      data-testid="notification-dropdown"
      class="absolute right-0 mt-2 w-80 sm:w-96 bg-surface rounded-xl shadow-dialog border border-border-default z-50 overflow-hidden flex flex-col"
    >
      <!-- Header -->
      <div class="p-4 border-b border-border-default flex items-center justify-between bg-slate-50">
        <div class="flex items-center gap-2">
          <span class="text-sm font-bold text-text-primary">Thông Báo</span>
          <span
            v-if="notiStore.unreadCount > 0"
            class="px-2 py-0.5 rounded-full bg-blue-100 text-primary-700 font-bold text-[10px] tabular-nums"
          >
            {{ notiStore.unreadCount }} mới
          </span>
        </div>

        <button
          v-if="notiStore.unreadCount > 0"
          type="button"
          @click="notiStore.markAllAsRead"
          data-testid="btn-mark-all-read"
          class="text-[11px] text-primary-600 hover:underline font-semibold"
        >
          Đã đọc tất cả
        </button>
      </div>

      <!-- Telegram Link Banner Bar -->
      <div class="p-3 bg-blue-50/70 border-b border-blue-200/60 flex items-center justify-between text-xs">
        <div class="flex items-center gap-2">
          <span class="text-base">✈️</span>
          <span class="text-text-secondary text-[11px]">
            {{ notiStore.isTelegramLinked ? `Đã nối Telegram (${notiStore.telegramUsername})` : 'Nhận thông báo qua Telegram' }}
          </span>
        </div>
        <button
          type="button"
          @click="showTelegramModal = true"
          data-testid="btn-open-telegram-modal"
          class="text-[11px] font-bold text-[#229ED9] hover:underline"
        >
          {{ notiStore.isTelegramLinked ? 'Đổi tài khoản' : 'Kết nối ngay' }}
        </button>
      </div>

      <!-- Notifications List -->
      <div class="max-h-80 overflow-y-auto divide-y divide-border-default">
        <div
          v-for="item in notiStore.notifications"
          :key="item.id"
          @click="handleItemClick(item)"
          class="p-3.5 hover:bg-slate-50/80 cursor-pointer transition-colors flex items-start gap-3"
          :class="{ 'bg-blue-50/30': !item.read }"
        >
          <span class="text-xl flex-shrink-0 p-1.5 rounded-lg bg-slate-100">{{ item.icon }}</span>
          <div class="space-y-1 flex-1 min-w-0">
            <div class="flex items-center justify-between gap-1">
              <h4 class="text-xs font-bold text-text-primary truncate" :class="{ 'text-primary-700': !item.read }">
                {{ item.title }}
              </h4>
              <span class="text-[10px] text-text-muted flex-shrink-0">{{ item.createdAt }}</span>
            </div>
            <p class="text-[11px] text-text-secondary leading-snug line-clamp-2">
              {{ item.message }}
            </p>
          </div>
          <span v-if="!item.read" class="w-2 h-2 rounded-full bg-primary-600 flex-shrink-0 mt-1.5"></span>
        </div>

        <div v-if="notiStore.notifications.length === 0" class="p-8 text-center text-xs text-text-muted">
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
