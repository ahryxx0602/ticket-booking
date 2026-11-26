/**
 * BLOCKER-04: Canonical Shared Booking Operation Guard Composable
 * Unified mutation recovery metadata for SeatSelectionView, CheckoutView, and MyBookingsView.
 * Scoped by authenticated user, bookingReference (or tripId for holds).
 * Prevents checkout when cancellation is pending/unresolved, and retains failure states across reloads.
 */
import { ref, computed } from 'vue'
import { useAuthStore } from '@/modules/auth/store'

// Global reactive revision counter so all composable instances react to storage mutations
const storageRevision = ref(0)

export function useBookingOperationGuard() {
  const authStore = useAuthStore()
  const userId = computed(() => authStore.user?.id || 'guest')

  function getCanonicalKey(scope, identifier) {
    return `velobus_guard_u${userId.value}_${scope}_${String(identifier || '').trim()}`
  }

  function getLegacyKeys(scope, identifier) {
    const id = String(identifier || '').trim()
    const u = userId.value
    switch (scope) {
      case 'hold_unknown':
        return [`velobus_hold_unknown_${u}_${id}`, `velobus_op_hold_unresolved_u${u}_${id}`]
      case 'hold_request':
        return [`velobus_hold_request_${u}_${id}`]
      case 'active_hold':
        return [`velobus_active_hold_${u}_${id}`]
      case 'cancel_pending':
        return [`velobus_op_cancel_pending_u${u}_${id}`]
      case 'cancel_unresolved':
        return [`velobus_cancel_unresolved_${u}_${id}`, `velobus_op_cancel_unresolved_u${u}_${id}`]
      case 'payment_unresolved':
        return [`velobus_op_payment_unresolved_u${u}_${id}`]
      default:
        return []
    }
  }

  function readState(scope, identifier) {
    // Read reactive revision to register Vue reactivity dependency
    void storageRevision.value

    if (!identifier) return null
    try {
      // 1. Try canonical key
      const canonicalKey = getCanonicalKey(scope, identifier)
      const raw = sessionStorage.getItem(canonicalKey)
      if (raw) return JSON.parse(raw)

      // 2. Try legacy migration keys
      const legacyKeys = getLegacyKeys(scope, identifier)
      for (const legacyKey of legacyKeys) {
        const legacyRaw = sessionStorage.getItem(legacyKey)
        if (legacyRaw) {
          const parsed = JSON.parse(legacyRaw)
          // Migrate to canonical key
          sessionStorage.setItem(canonicalKey, legacyRaw)
          sessionStorage.removeItem(legacyKey)
          return parsed
        }
      }
      return null
    } catch {
      return null
    }
  }

  function writeState(scope, identifier, data) {
    if (!identifier) return
    try {
      const canonicalKey = getCanonicalKey(scope, identifier)
      // Sanitize payload: strip any raw Axios response / headers / credentials
      const sanitized = sanitizeData(data)
      sessionStorage.setItem(canonicalKey, JSON.stringify({
        ...sanitized,
        updatedAt: Date.now()
      }))
      storageRevision.value++
    } catch (e) {
      console.warn('[BookingGuard] Failed to save operation state', e)
    }
  }

  function removeState(scope, identifier) {
    if (!identifier) return
    try {
      const canonicalKey = getCanonicalKey(scope, identifier)
      sessionStorage.removeItem(canonicalKey)
      const legacyKeys = getLegacyKeys(scope, identifier)
      for (const legacyKey of legacyKeys) {
        sessionStorage.removeItem(legacyKey)
      }
      storageRevision.value++
    } catch {}
  }

  function sanitizeData(data) {
    if (!data) return {}
    if (typeof data !== 'object') return { value: String(data) }
    const clean = {}
    for (const [k, v] of Object.entries(data)) {
      if (k === 'config' || k === 'request' || k === 'headers') continue
      if (v instanceof Error) {
        clean[k] = v.message
      } else if (typeof v === 'string' || typeof v === 'number' || typeof v === 'boolean' || Array.isArray(v)) {
        clean[k] = v
      } else if (v && typeof v === 'object') {
        clean[k] = v.message || v.code || String(v)
      }
    }
    return clean
  }

  // --- 1. HOLD MUTATIONS (Scoped by tripId) ---
  function getHoldUnknown(tripId) {
    return readState('hold_unknown', tripId)
  }

  function isHoldUnknown(tripId) {
    return Boolean(getHoldUnknown(tripId))
  }

  function setHoldUnknown(tripId, data) {
    writeState('hold_unknown', tripId, data)
  }

  function clearHoldUnknown(tripId) {
    removeState('hold_unknown', tripId)
  }

  function getHoldRequest(tripId) {
    return readState('hold_request', tripId)
  }

  function setHoldRequest(tripId, data) {
    writeState('hold_request', tripId, data)
  }

  function clearHoldRequest(tripId) {
    removeState('hold_request', tripId)
  }

  function getActiveHold(tripId) {
    return readState('active_hold', tripId)
  }

  function setActiveHold(tripId, data) {
    writeState('active_hold', tripId, data)
  }

  function clearActiveHold(tripId) {
    removeState('active_hold', tripId)
  }

  // --- 2. CANCELLATION MUTATIONS (Scoped by bookingReference) ---
  function isCancelPending(bookingReference) {
    return Boolean(readState('cancel_pending', bookingReference))
  }

  function setCancelPending(bookingReference, data = {}) {
    writeState('cancel_pending', bookingReference, data)
  }

  function clearCancelPending(bookingReference) {
    removeState('cancel_pending', bookingReference)
  }

  function getUnresolvedCancel(bookingReference) {
    return readState('cancel_unresolved', bookingReference)
  }

  function isCancelUnresolved(bookingReference) {
    return Boolean(getUnresolvedCancel(bookingReference))
  }

  function getCancelError(bookingReference) {
    const data = getUnresolvedCancel(bookingReference)
    if (!data) return ''
    if (typeof data === 'string') return data
    return data.message || data.error || ''
  }

  function setUnresolvedCancel(bookingReference, errorInfo) {
    const message = typeof errorInfo === 'string'
      ? errorInfo
      : errorInfo?.response?.data?.message || errorInfo?.message || 'Lỗi thao tác hủy đơn đặt chỗ'
    writeState('cancel_unresolved', bookingReference, { message })
  }

  function clearUnresolvedCancel(bookingReference) {
    removeState('cancel_unresolved', bookingReference)
  }

  // --- 3. PAYMENT MUTATIONS (Scoped by bookingReference) ---
  function getUnresolvedPayment(bookingReference) {
    return readState('payment_unresolved', bookingReference)
  }

  function isPaymentUnresolved(bookingReference) {
    return Boolean(getUnresolvedPayment(bookingReference))
  }

  function setUnresolvedPayment(bookingReference, data) {
    writeState('payment_unresolved', bookingReference, data)
  }

  function clearUnresolvedPayment(bookingReference) {
    removeState('payment_unresolved', bookingReference)
  }

  // --- 4. CHECKOUT ELIGIBILITY GUARD ---
  function isCheckoutBlocked(bookingReference) {
    if (!bookingReference) return false
    if (isCancelPending(bookingReference)) return true
    if (isCancelUnresolved(bookingReference)) return true
    return false
  }

  return {
    // Hold operations
    getHoldUnknown,
    isHoldUnknown,
    setHoldUnknown,
    clearHoldUnknown,
    getHoldRequest,
    setHoldRequest,
    clearHoldRequest,
    getActiveHold,
    setActiveHold,
    clearActiveHold,

    // Cancel operations
    isCancelPending,
    setCancelPending,
    clearCancelPending,
    getUnresolvedCancel,
    isCancelUnresolved,
    getCancelError,
    setUnresolvedCancel,
    clearUnresolvedCancel,

    // Payment operations
    getUnresolvedPayment,
    isPaymentUnresolved,
    setUnresolvedPayment,
    clearUnresolvedPayment,

    // Guards
    isCheckoutBlocked
  }
}
