import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { useNotificationStore } from '@/shared/stores/notificationStore'
import NotificationCenter from '../NotificationCenter.vue'
import TelegramLinkModal from '../TelegramLinkModal.vue'

// Mock qrcode
vi.mock('qrcode', () => ({
  default: {
    toDataURL: vi.fn().mockResolvedValue('data:image/png;base64,mockTelegramQr')
  }
}))

// REV-04: Helper to seed test notifications into an empty store
// (Production store initializes empty; tests seed their own fixtures)
function seedTestNotifications(store) {
  store.addNotification({
    id: 'NOTI-03',
    title: 'Cảnh Báo Đối Soát',
    message: 'Phát hiện sai lệch số dư.',
    type: 'RECONCILIATION_ALERT',
    icon: '⚠️',
    read: true,
    createdAt: 'Hôm qua'
  })
  store.addNotification({
    id: 'NOTI-02',
    title: 'Quyết Toán Ví Ký Quỹ Escrow',
    message: 'Chuyến xe TRIP-99 đã hoàn tất.',
    type: 'ESCROW_SETTLED',
    icon: '💰',
    read: false,
    createdAt: '1 giờ trước'
  })
  store.addNotification({
    id: 'NOTI-01',
    title: 'Xác Nhận Đặt Vé Thành Công',
    message: 'Đơn vé BKG-20261123-9988 tuyến Hà Nội → Đà Nẵng đã thanh toán thành công qua VNPay.',
    type: 'BOOKING_CONFIRMED',
    icon: '🎫',
    read: false,
    createdAt: '10 phút trước'
  })
}

describe('Phase FE-04: Real-Time Notifications & Telegram Bot Integration', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  describe('useNotificationStore', () => {
    it('calculates unreadCount accurately and marks notifications as read', () => {
      const store = useNotificationStore()
      seedTestNotifications(store)

      expect(store.unreadCount).toBe(2) // NOTI-01 & NOTI-02 are unread

      store.markAsRead('NOTI-01')
      expect(store.unreadCount).toBe(1)

      store.markAllAsRead()
      expect(store.unreadCount).toBe(0)
    })

    it('adds new notification and links telegram account successfully', () => {
      const store = useNotificationStore()
      seedTestNotifications(store)

      store.addNotification({
        title: 'Thanh Toán Thành Công',
        message: 'Vé đã được xác nhận.',
        type: 'PAYMENT_SUCCESS',
        icon: '✅'
      })

      expect(store.notifications[0].title).toBe('Thanh Toán Thành Công')
      expect(store.unreadCount).toBe(3)

      store.linkTelegram('@thanh_dev')
      expect(store.isTelegramLinked).toBe(true)
      expect(store.telegramUsername).toBe('@thanh_dev')
    })
  })

  describe('NotificationCenter.vue Component', () => {
    it('renders unread badge and toggles dropdown panel on click', async () => {
      // Seed store before mounting so component sees 2 unread
      const store = useNotificationStore()
      seedTestNotifications(store)

      const wrapper = mount(NotificationCenter, {
        global: {
          stubs: {
            TelegramLinkModal: true
          }
        }
      })

      // Check badge exists with 2 unread items
      const badge = wrapper.find('[data-testid="badge-unread-count"]')
      expect(badge.exists()).toBe(true)
      expect(badge.text()).toBe('2')

      // Dropdown initially closed
      expect(wrapper.find('[data-testid="notification-dropdown"]').exists()).toBe(false)

      // Click bell icon to open dropdown
      await wrapper.find('[data-testid="btn-notification-bell"]').trigger('click')
      expect(wrapper.find('[data-testid="notification-dropdown"]').exists()).toBe(true)
      expect(wrapper.text()).toContain('Xác Nhận Đặt Vé Thành Công')

      // Click "Đã đọc tất cả"
      const markAllBtn = wrapper.find('[data-testid="btn-mark-all-read"]')
      expect(markAllBtn.exists()).toBe(true)
      await markAllBtn.trigger('click')

      expect(wrapper.find('[data-testid="badge-unread-count"]').exists()).toBe(false)
    })
  })

  describe('TelegramLinkModal.vue Component', () => {
    it('renders telegram deep link and connects telegram on verify', async () => {
      const wrapper = mount(TelegramLinkModal, {
        global: {
          stubs: {
            AppButton: true
          }
        }
      })

      expect(wrapper.text()).toContain('Liên Kết Bot Telegram VeloBus')
      const telegramLink = wrapper.find('[data-testid="link-open-telegram"]')
      expect(telegramLink.exists()).toBe(true)
      expect(telegramLink.attributes('href')).toContain('https://t.me/velobus_bot?start=')
    })
  })
})
