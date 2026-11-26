import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import TripCard from '../components/TripCard.vue'
import MyBookingsView from '../views/MyBookingsView.vue'

// Mock vue-router
const pushMock = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: pushMock
  }),
  useRoute: () => ({
    params: { bookingId: 'BKG-TEST-123' },
    query: {}
  }),
  RouterLink: {
    template: '<a><slot /></a>'
  }
}))

// Mock qrcode library
vi.mock('qrcode', () => ({
  default: {
    toDataURL: vi.fn().mockResolvedValue('data:image/png;base64,mockQrCodeDataUrl')
  }
}))

// Mock catalogBookingApi
vi.mock('@/modules/catalog-booking/api', () => ({
  catalogBookingApi: {
    getPublicBanners: vi.fn().mockResolvedValue({ data: [] }),
    getPublicSettings: vi.fn().mockResolvedValue({ data: {} }),
    getStations: vi.fn().mockResolvedValue({
      data: [
        { id: 1, code: 'HN-GP', name: 'Giáp Bát', city: 'Hà Nội' },
        { id: 2, code: 'DN-TT', name: 'Đà Nẵng Trung Tâm', city: 'Đà Nẵng' }
      ]
    }),
    searchTrips: vi.fn().mockResolvedValue({ data: [] })
  }
}))

describe('Customer Booking Flow Tests', () => {
  beforeEach(() => {
    pushMock.mockClear()
  })

  describe('TripCard.vue Component', () => {
    const mockTrip = {
      id: 99,
      tripCode: 'HN-DN-99',
      route: 'Hà Nội → Đà Nẵng',
      operator: 'Hoàng Long Limousine',
      plate: '29B-888.99',
      departureTime: '2026-11-26T19:00:00',
      arrivalTime: '2026-11-27T09:00:00',
      duration: '14 giờ',
      transportType: 'VIP Luxury 36 Giường',
      totalSeats: 36,
      basePrice: 350000,
      status: 'SCHEDULED'
    }

    it('renders authentic trip information, totalSeats capacity and formatted price', () => {
      const wrapper = mount(TripCard, {
        props: { trip: mockTrip }
      })

      expect(wrapper.text()).toContain('Hà Nội → Đà Nẵng')
      expect(wrapper.text()).toContain('Hoàng Long Limousine')
      expect(wrapper.text()).toContain('29B-888.99')
      expect(wrapper.text()).toContain('350.000')
      expect(wrapper.text()).toContain('Sức chứa 36 chỗ')
      expect(wrapper.text()).toContain('+1 ngày')
    })

    it('navigates to seat selection view when clicking Chọn Chỗ', async () => {
      const wrapper = mount(TripCard, {
        props: { trip: mockTrip }
      })

      const btn = wrapper.find('[data-testid="btn-select-trip"]')
      expect(btn.exists()).toBe(true)
      await btn.trigger('click')

      expect(pushMock).toHaveBeenCalledWith('/trips/99/seats')
    })

    it('disables button when trip status is not SCHEDULED', () => {
      const cancelledTrip = { ...mockTrip, status: 'CANCELLED' }
      const wrapper = mount(TripCard, {
        props: { trip: cancelledTrip }
      })

      const btn = wrapper.find('[data-testid="btn-select-trip"]')
      expect(btn.attributes('disabled')).toBeDefined()
      expect(wrapper.text()).toContain('Không khả dụng')
    })
  })

  describe('MyBookingsView.vue Component', () => {
    it('renders initial bookings list with confirmed and completed tickets', () => {
      const wrapper = mount(MyBookingsView, {
        global: {
          stubs: {
            'router-link': true,
            'StatusBadge': true,
            'EmptyState': true,
            'AppButton': true
          }
        }
      })

      expect(wrapper.text()).toContain('Lịch Sử Vé Đã Đặt & Vé Điện Tử')
      expect(wrapper.text()).toContain('BKG-20261123-9988')
      expect(wrapper.text()).toContain('Hoàng Long Limousine')
    })

    it('switches tabs and filters bookings list correctly', async () => {
      const wrapper = mount(MyBookingsView, {
        global: {
          stubs: {
            'router-link': true,
            'StatusBadge': true,
            'EmptyState': true,
            'AppButton': true
          }
        }
      })

      // Click "Đã hủy" tab
      const buttons = wrapper.findAll('button')
      const cancelledBtn = buttons.find(b => b.text().includes('Đã hủy'))
      expect(cancelledBtn).toBeDefined()
      await cancelledBtn.trigger('click')

      expect(wrapper.text()).toContain('BKG-20261115-4455')
      expect(wrapper.text()).not.toContain('BKG-20261123-9988')
    })
  })

  describe('TripSearchForm.vue Component Form & Discovery', () => {
    it('validates duplicate departure and arrival cities', async () => {
      const { default: TripSearchForm } = await import('../components/TripSearchForm.vue')
      const wrapper = mount(TripSearchForm, {
        props: {
          initialDepartureCity: 'Hà Nội',
          initialArrivalCity: 'Hà Nội',
          stations: [
            { id: 1, code: 'HN', city: 'Hà Nội' },
            { id: 2, code: 'DN', city: 'Đà Nẵng' }
          ]
        }
      })

      const form = wrapper.find('form')
      await form.trigger('submit')

      expect(wrapper.text()).toContain('Điểm đi và điểm đến không được trùng nhau')
    })

    it('validates past departure date', async () => {
      const { default: TripSearchForm } = await import('../components/TripSearchForm.vue')
      const wrapper = mount(TripSearchForm, {
        props: {
          initialDepartureCity: 'Hà Nội',
          initialArrivalCity: 'Đà Nẵng',
          initialDepartureDate: '2020-01-01',
          stations: [
            { id: 1, code: 'HN', city: 'Hà Nội' },
            { id: 2, code: 'DN', city: 'Đà Nẵng' }
          ]
        }
      })

      const form = wrapper.find('form')
      await form.trigger('submit')

      expect(wrapper.text()).toContain('Ngày khởi hành không được ở trong quá khứ')
    })
  })

  describe('TripListView.vue Component Search & Filter', () => {
    it('renders search summary and handles empty state and filters', async () => {
      const { default: TripListView } = await import('../views/TripListView.vue')
      const wrapper = mount(TripListView, {
        global: {
          stubs: {
            'router-link': true,
            'Sheet': true,
            'SheetTrigger': true,
            'SheetContent': true,
            'SheetHeader': true,
            'SheetTitle': true,
            'SheetDescription': true,
            'SheetFooter': true,
            'SheetClose': true
          }
        }
      })

      expect(wrapper.find('[data-testid="search-summary-header"]').exists()).toBe(true)
      expect(wrapper.find('[data-testid="filter-sidebar"]').exists()).toBe(true)
    })
  })
})
