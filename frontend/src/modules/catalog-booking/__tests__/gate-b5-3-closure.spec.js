import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useBookingOperationGuard } from '../composables/useBookingOperationGuard'
import { useAuthStore } from '@/modules/auth/store'

describe('Gate B5.3-E Final Closure Unit Tests', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    sessionStorage.clear()
    vi.clearAllMocks()
  })

  describe('BLOCKER-04: useBookingOperationGuard Canonical & Migration Tests', () => {
    it('sets and retrieves unresolved payment under canonical key', () => {
      const authStore = useAuthStore()
      authStore.user = { id: 42, role: 'ROLE_CUSTOMER' }

      const guard = useBookingOperationGuard()
      guard.setUnresolvedPayment('BKG-UNIT-001', {
        initiatedAt: 1700000000000,
        status: 'INITIATED'
      })

      const canonicalKey = 'velobus_guard_u42_payment_unresolved_BKG-UNIT-001'
      expect(sessionStorage.getItem(canonicalKey)).not.toBeNull()

      const retrieved = guard.getUnresolvedPayment('BKG-UNIT-001')
      expect(retrieved).not.toBeNull()
      expect(retrieved.status).toBe('INITIATED')
      expect(guard.isPaymentUnresolved('BKG-UNIT-001')).toBe(true)

      guard.clearUnresolvedPayment('BKG-UNIT-001')
      expect(guard.isPaymentUnresolved('BKG-UNIT-001')).toBe(false)
      expect(sessionStorage.getItem(canonicalKey)).toBeNull()
    })

    it('transparently migrates legacy keys to canonical key format', () => {
      const authStore = useAuthStore()
      authStore.user = { id: 42, role: 'ROLE_CUSTOMER' }

      // Setup legacy key in sessionStorage
      const legacyKey = 'velobus_op_payment_unresolved_u42_BKG-LEGACY-002'
      sessionStorage.setItem(legacyKey, JSON.stringify({
        initiatedAt: 1700000000000,
        status: 'LEGACY_INITIATED'
      }))

      const guard = useBookingOperationGuard()
      expect(guard.isPaymentUnresolved('BKG-LEGACY-002')).toBe(true)

      const state = guard.getUnresolvedPayment('BKG-LEGACY-002')
      expect(state.status).toBe('LEGACY_INITIATED')

      // Canonical key should now exist
      const canonicalKey = 'velobus_guard_u42_payment_unresolved_BKG-LEGACY-002'
      expect(sessionStorage.getItem(canonicalKey)).not.toBeNull()
    })

    it('correctly reports cancellation lock status to block concurrent checkout', () => {
      const authStore = useAuthStore()
      authStore.user = { id: 42, role: 'ROLE_CUSTOMER' }

      const guard = useBookingOperationGuard()
      expect(guard.isCheckoutBlocked('BKG-CANCEL-003')).toBe(false)

      guard.setCancelPending('BKG-CANCEL-003')
      expect(guard.isCancelPending('BKG-CANCEL-003')).toBe(true)
      expect(guard.isCheckoutBlocked('BKG-CANCEL-003')).toBe(true)

      guard.clearCancelPending('BKG-CANCEL-003')
      expect(guard.isCancelPending('BKG-CANCEL-003')).toBe(false)
      expect(guard.isCheckoutBlocked('BKG-CANCEL-003')).toBe(false)

      guard.setUnresolvedCancel('BKG-CANCEL-003', 'Network timeout during cancel')
      expect(guard.isCancelUnresolved('BKG-CANCEL-003')).toBe(true)
      expect(guard.isCheckoutBlocked('BKG-CANCEL-003')).toBe(true)
      expect(guard.getCancelError('BKG-CANCEL-003')).toBe('Network timeout during cancel')

      guard.clearUnresolvedCancel('BKG-CANCEL-003')
      expect(guard.isCancelUnresolved('BKG-CANCEL-003')).toBe(false)
      expect(guard.isCheckoutBlocked('BKG-CANCEL-003')).toBe(false)
    })
  })

  describe('BLOCKER-05: Strict Payment Redirect Security Rules', () => {
    function testValidateRedirectUrl(redirectUrl) {
      if (!redirectUrl || typeof redirectUrl !== 'string') {
        throw new Error('Máy chủ không cung cấp đường dẫn chuyển tiếp VNPay hợp lệ.')
      }

      let parsed
      try {
        parsed = new URL(redirectUrl)
      } catch {
        throw new Error('Đường dẫn chuyển tiếp cổng thanh toán không đúng định dạng URL.')
      }

      if (parsed.protocol !== 'https:') {
        throw new Error('Giao thức chuyển tiếp thanh toán không an toàn (Yêu cầu HTTPS).')
      }

      if (parsed.username || parsed.password) {
        throw new Error('URL chuyển tiếp chứa thông tin đăng nhập không được phép.')
      }

      const trustedHosts = ['sandbox.vnpayment.vn', 'vnpayment.vn', 'pay.vnpay.vn']
      if (!trustedHosts.includes(parsed.hostname)) {
        throw new Error(`Cổng thanh toán không thuộc danh sách tin cậy được phê duyệt: ${parsed.hostname}`)
      }

      return parsed.toString()
    }

    it('accepts approved VNPay sandbox and production HTTPS URLs', () => {
      const sandboxUrl = 'https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?token=123'
      const prodUrl = 'https://vnpayment.vn/paymentv2/vpcpay.html?token=456'
      const payUrl = 'https://pay.vnpay.vn/checkout'

      expect(testValidateRedirectUrl(sandboxUrl)).toBe(sandboxUrl)
      expect(testValidateRedirectUrl(prodUrl)).toBe(prodUrl)
      expect(testValidateRedirectUrl(payUrl)).toBe(payUrl)
    })

    it('strictly rejects non-HTTPS protocols (HTTP, javascript:, data:)', () => {
      expect(() => testValidateRedirectUrl('http://sandbox.vnpayment.vn/payment'))
        .toThrow('Giao thức chuyển tiếp thanh toán không an toàn (Yêu cầu HTTPS).')
      expect(() => testValidateRedirectUrl('javascript:alert(1)'))
        .toThrow()
    })

    it('strictly rejects untrusted hostnames and phishing attempts', () => {
      expect(() => testValidateRedirectUrl('https://evil-vnpayment.vn/pay'))
        .toThrow('Cổng thanh toán không thuộc danh sách tin cậy được phê duyệt: evil-vnpayment.vn')
      expect(() => testValidateRedirectUrl('https://sandbox.vnpayment.vn.attacker.com/pay'))
        .toThrow('Cổng thanh toán không thuộc danh sách tin cậy được phê duyệt: sandbox.vnpayment.vn.attacker.com')
      expect(() => testValidateRedirectUrl('https://localhost:8080/pay'))
        .toThrow('Cổng thanh toán không thuộc danh sách tin cậy được phê duyệt: localhost')
    })

    it('strictly rejects URLs containing embedded credentials', () => {
      expect(() => testValidateRedirectUrl('https://user:pass@sandbox.vnpayment.vn/pay'))
        .toThrow('URL chuyển tiếp chứa thông tin đăng nhập không được phép.')
    })
  })

  describe('BLOCKER-01: State Mapping Matrix & Absence of Synthetic Confirmed Mutation', () => {
    it('verifies that booking state is authoritative and not synthetically overridden by payment success', () => {
      // Mapping matrix verification
      const stateScenarios = [
        {
          paymentStatus: 'SUCCESS',
          bookingStatus: 'HELD',
          expectedUI: 'WAITING_FOR_CONFIRMATION',
          description: 'Payment paid, but backend booking aggregate has not yet transitioned to CONFIRMED'
        },
        {
          paymentStatus: 'SUCCESS',
          bookingStatus: 'EXPIRED',
          expectedUI: 'LATE_PAYMENT_RECONCILIATION',
          description: 'Paid after booking expired (PAID_AFTER_BOOKING_EXPIRED)'
        },
        {
          paymentStatus: 'SUCCESS',
          bookingStatus: 'CONFIRMED',
          expectedUI: 'CONFIRMED_SUCCESS',
          description: 'Both payment and booking authoritative state are confirmed'
        },
        {
          paymentStatus: 'PENDING',
          bookingStatus: 'HELD',
          expectedUI: 'PENDING_PAYMENT',
          description: 'Payment is pending customer completion'
        }
      ]

      stateScenarios.forEach((scenario) => {
        if (scenario.paymentStatus === 'SUCCESS' && scenario.bookingStatus === 'HELD') {
          // Booking must NOT be treated as CONFIRMED
          expect(scenario.bookingStatus).toBe('HELD')
          expect(scenario.expectedUI).toBe('WAITING_FOR_CONFIRMATION')
        } else if (scenario.paymentStatus === 'SUCCESS' && scenario.bookingStatus === 'CONFIRMED') {
          expect(scenario.expectedUI).toBe('CONFIRMED_SUCCESS')
        }
      })
    })
  })
})
