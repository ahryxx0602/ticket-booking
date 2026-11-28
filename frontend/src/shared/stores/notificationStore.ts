import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface AppNotification {
  id: string
  title: string
  message: string
  type: string
  icon?: string
  read: boolean
  createdAt: string
  [key: string]: any
}

const IS_DEV_SHOWCASE = import.meta.env.DEV && import.meta.env.VITE_ENABLE_MOCK_NOTIFICATIONS === 'true'

const devMockNotifications: AppNotification[] = IS_DEV_SHOWCASE
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
  const notifications = ref<AppNotification[]>([...devMockNotifications])

  const isTelegramLinked = ref<boolean>(false)
  const telegramUsername = ref<string>('')

  const unreadCount = computed<number>(() => {
    return notifications.value.filter(n => !n.read).length
  })

  function markAsRead(id: string): void {
    const item = notifications.value.find(n => n.id === id)
    if (item) {
      item.read = true
    }
  }

  function markAllAsRead(): void {
    notifications.value.forEach(n => {
      n.read = true
    })
  }

  function addNotification(noti: Partial<AppNotification>): void {
    notifications.value.unshift({
      id: `NOTI-${Date.now()}`,
      title: noti.title || 'Thông báo mới',
      message: noti.message || '',
      type: noti.type || 'SYSTEM',
      read: false,
      createdAt: 'Vừa xong',
      ...noti
    })
  }

  function linkTelegram(username?: string): void {
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
