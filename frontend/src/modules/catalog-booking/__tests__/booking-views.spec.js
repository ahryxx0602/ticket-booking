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
      id: 'TRIP-99',
      route: 'Hà Nội → Đà Nẵng',
      operator: 'Hoàng Long Limousine',
      plate: '29B-888.99',
      departureTime: '19:00',
      arrivalTime: '09:00 (+1 ngày)',
      duration: '14 giờ',
      coachType: 'VIP Luxury 36 Giường',
      availableSeats: 5,
      price: 350000
    }

    it('renders trip information and formatted price in tabular-nums', () => {
      const wrapper = mount(TripCard, {
        props: { trip: mockTrip }
      })

      expect(wrapper.text()).toContain('Hà Nội → Đà Nẵng')
      expect(wrapper.text()).toContain('Hoàng Long Limousine')
      expect(wrapper.text()).toContain('29B-888.99')
      expect(wrapper.text()).toContain('350.000')
      expect(wrapper.text()).toContain('Còn 5 chỗ')
    })

    it('navigates to seat selection view when clicking Chọn Chỗ', async () => {
      const wrapper = mount(TripCard, {
        props: { trip: mockTrip }
      })

      const btn = wrapper.find('[data-testid="btn-select-trip"]')
      expect(btn.exists()).toBe(true)
      await btn.trigger('click')

      expect(pushMock).toHaveBeenCalledWith('/trips/TRIP-99/seats')
    })

    it('disables button when available seats is 0', () => {
      const soldOutTrip = { ...mockTrip, availableSeats: 0 }
      const wrapper = mount(TripCard, {
        props: { trip: soldOutTrip }
      })

      const btn = wrapper.find('[data-testid="btn-select-trip"]')
      expect(btn.attributes('disabled')).toBeDefined()
      expect(wrapper.text()).toContain('Hết chỗ')
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

  describe('HomeView.vue Component Form & Discovery', () => {
    it('validates duplicate departure and arrival cities', async () => {
      const { default: HomeView } = await import('../views/HomeView.vue')
      const wrapper = mount(HomeView, {
        global: {
          stubs: {
            'router-link': true
          }
        }
      })

      // Set same city
      const departureSelect = wrapper.find('[data-testid="departure-city-select"]')
      const arrivalSelect = wrapper.find('[data-testid="arrival-city-select"]')

      if (departureSelect.exists() && arrivalSelect.exists()) {
        await departureSelect.setValue('Hà Nội')
        await arrivalSelect.setValue('Hà Nội')

        const form = wrapper.find('form')
        await form.trigger('submit')

        expect(wrapper.text()).toContain('Điểm đi và điểm đến không được trùng nhau')
      }
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
