import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// REV-04: Mock fixtures are ONLY allowed in DEV, Showcase or automated tests.
// Production initializes with an empty list. Notifications will be populated
// from real backend API calls when that feature is available.
// mark-as-read only updates local state; server persistence is a Gate B3+ concern.

const IS_DEV_SHOWCASE = import.meta.env.DEV && import.meta.env.VITE_ENABLE_MOCK_NOTIFICATIONS === 'true'

const devMockNotifications = IS_DEV_SHOWCASE
  ? [
      {
        id: 'NOTI-01',
        title: 'Xác Nhận Đặt Vé Thành Công',
        message: 'Đơn vé BKG-20261123-9988 tuyến Hà Nội → Đà Nẵng đã thanh toán thành công qua VNPay.',
        type: 'BOOKING_CONFIRMED',
        icon: '🎫',
        read: false,
        createdAt: '10 phút trước'
      },
      {
        id: 'NOTI-02',
        title: 'Quyết Toán Ví Ký Quỹ Escrow',
        message: 'Chuyến xe TRIP-99 đã hoàn tất +2h. Số tiền 14.200.000 đ đã được chuyển vào số dư khả dụng.',
        type: 'ESCROW_SETTLED',
        icon: '💰',
        read: false,
        createdAt: '1 giờ trước'
      },
      {
        id: 'NOTI-03',
        title: 'Cảnh Báo Đối Soát Dòng Tiền',
        message: 'Hệ thống phát hiện sai lệch số dư tại đơn BKG-20261122-3319. Vui lòng kiểm tra đối soát.',
        type: 'RECONCILIATION_ALERT',
        icon: '⚠️',
        read: true,
        createdAt: 'Hôm qua'
      }
    ]
  : []

export const useNotificationStore = defineStore('notification', () => {
  // Production: empty list. DEV+MOCK flag: seed with fixtures for showcase/tests only.
  const notifications = ref([...devMockNotifications])

  const isTelegramLinked = ref(false)
  const telegramUsername = ref('')

  const unreadCount = computed(() => {
    return notifications.value.filter(n => !n.read).length
  })

  function markAsRead(id) {
    // NOTE: This only updates local state. Server-side persistence is deferred to Gate B3+.
    const item = notifications.value.find(n => n.id === id)
    if (item) {
      item.read = true
    }
  }

  function markAllAsRead() {
    // NOTE: Local state only — no server call.
    notifications.value.forEach(n => {
      n.read = true
    })
  }

  function addNotification(noti) {
    notifications.value.unshift({
      id: `NOTI-${Date.now()}`,
      read: false,
      createdAt: 'Vừa xong',
      ...noti
    })
  }

  function linkTelegram(username) {
    isTelegramLinked.value = true
    telegramUsername.value = username || '@velobus_user'
  }

  return {
    notifications,
    isTelegramLinked,
    telegramUsername,
    unreadCount,
    markAsRead,
    markAllAsRead,
    addNotification,
    linkTelegram
  }
})
