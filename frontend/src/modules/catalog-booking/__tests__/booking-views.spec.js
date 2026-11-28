import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import TripCard from '../components/TripCard.vue'
import MyBookingsView from '../views/MyBookingsView.vue'
import CheckoutView from '../views/CheckoutView.vue'

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
    searchTrips: vi.fn().mockResolvedValue({ data: [] }),
    getMyBookings: vi.fn().mockResolvedValue({
      data: {
        data: {
          items: [
            {
              bookingId: 101,
              bookingReference: 'BKG-20261123-9988',
              userId: 1,
              tripId: 99,
              status: 'CONFIRMED',
              totalAmount: 700000,
              createdAt: '2026-11-23T10:00:00',
              seatNumbers: ['A05', 'A06']
            },
            {
              bookingId: 102,
              bookingReference: 'BKG-20261115-4455',
              userId: 1,
              tripId: 88,
              status: 'CANCELLED',
              totalAmount: 180000,
              createdAt: '2026-11-15T08:00:00',
              seatNumbers: ['B01']
            }
          ],
          page: 0,
          size: 10,
          totalElements: 2,
          totalPages: 1,
          hasNext: false,
          hasPrevious: false
        }
      }
    }),
    getBookingDetail: vi.fn().mockResolvedValue({
      data: {
        data: {
          bookingId: 201,
          bookingReference: 'BKG-CHECKOUT-123',
          userId: 1,
          tripId: 99,
          status: 'HELD',
          totalAmount: 350000,
          expiresAt: new Date(Date.now() + 600000).toISOString(),
          createdAt: new Date().toISOString(),
          seatNumbers: ['01A']
        }
      }
    }),
    cancelBooking: vi.fn().mockResolvedValue({ data: { success: true } }),
    createVnPayCheckout: vi.fn().mockResolvedValue({
      data: {
        data: {
          paymentReference: 'PAY-12345',
          redirectPaymentUrl: 'https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?token=test'
        }
      }
    }),
    getPaymentStatus: vi.fn().mockResolvedValue({
      data: {
        data: {
          bookingReference: 'BKG-CHECKOUT-123',
          canAttemptPayment: true,
          hasPaymentAttempt: true,
          paymentReference: 'PAY-12345',
          paymentStatus: 'COMPLETED',
          paymentMethod: 'VNPAY',
          amount: 350000,
          reconciliationRequired: false,
          verifiedAt: '2026-11-26T20:00:00'
        }
      }
    }),
    getElectronicTicket: vi.fn().mockResolvedValue({
      data: {
        data: {
          bookingReference: 'BKG-20261123-9988',
          tripId: 99,
          status: 'CONFIRMED',
          totalAmount: 700000,
          seatNumbers: ['A05', 'A06'],
          qrImageBase64: 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
          qrPayload: 'v1|BKG-20261123-9988|99|mock-signature',
          issuedAt: '2026-11-26T20:00:00'
        }
      }
    })
  }
}))

describe('Customer Booking Flow Tests', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
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
    it('renders initial bookings list with confirmed and completed tickets', async () => {
      const wrapper = mount(MyBookingsView, {
        global: {
          stubs: {
            'router-link': true,
            'StatusBadge': true,
            'EmptyState': true,
            'Button': true
          }
        }
      })
      await flushPromises()

      expect(wrapper.text()).toContain('Lịch Sử')
      expect(wrapper.text()).toContain('BKG-20261123-9988')
    })

    it('switches tabs and triggers server-side query with status filter', async () => {
      const wrapper = mount(MyBookingsView, {
        global: {
          stubs: {
            'router-link': true,
            'StatusBadge': true,
            'EmptyState': true,
            'Button': true
          }
        }
      })
      await flushPromises()

      // Click "Đã hủy" tab
      const buttons = wrapper.findAll('button')
      const cancelledBtn = buttons.find(b => b.text().includes('Đã hủy'))
      expect(cancelledBtn).toBeDefined()
      await cancelledBtn.trigger('click')
      await flushPromises()

      expect(wrapper.text()).toContain('BKG-20261115-4455')
      expect(wrapper.text()).not.toContain('BKG-20261123-9988')
    })
  })

  describe('CheckoutView.vue Component', () => {
    it('loads and validates booking detail, displaying countdown and payment options', async () => {
      const wrapper = mount(CheckoutView, {
        global: {
          stubs: {
            'router-link': true,
            'StatusBadge': true,
            'Button': true
          }
        }
      })
      await flushPromises()

      expect(wrapper.text()).toContain('BKG-CHECKOUT-123')
      expect(wrapper.text()).toContain('01A')
      expect(wrapper.text()).toContain('VNPAY')
    })

    it('rejects payment eligibility when paymentStatus has mismatched or missing bookingReference', async () => {
      const { catalogBookingApi } = await import('@/modules/catalog-booking/api')
      catalogBookingApi.getPaymentStatus.mockResolvedValueOnce({
        data: {
          data: {
            bookingReference: 'BKG-MISMATCH-999',
            canAttemptPayment: true
          }
        }
      })

      const wrapper = mount(CheckoutView, {
        global: {
          stubs: {
            'router-link': true,
            'StatusBadge': true,
            'Button': true
          }
        }
      })
      await flushPromises()

      expect(wrapper.vm.canAttemptPayment).toBe(false)
      expect(wrapper.vm.paymentStatusError).toContain('không hợp lệ')
    })

    it('rejects payment eligibility when canAttemptPayment is false', async () => {
      const { catalogBookingApi } = await import('@/modules/catalog-booking/api')
      catalogBookingApi.getPaymentStatus.mockResolvedValueOnce({
        data: {
          data: {
            bookingReference: 'BKG-CHECKOUT-123',
            canAttemptPayment: false
          }
        }
      })

      const wrapper = mount(CheckoutView, {
        global: {
          stubs: {
            'router-link': true,
            'StatusBadge': true,
            'Button': true
          }
        }
      })
      await flushPromises()

      expect(wrapper.vm.canAttemptPayment).toBe(false)
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
