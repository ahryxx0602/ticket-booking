/**
 * PRE-03: Shared Booking Operation Guard Composable
 * Scopes stored mutation recovery metadata by authenticated user and bookingReference (or tripId).
 * Prevents checkout when cancellation is pending/unresolved, and retains failure states across reloads.
 */
import { ref, computed } from 'vue'
import { useAuthStore } from '@/modules/auth/store'

export function useBookingOperationGuard() {
  const authStore = useAuthStore()
  const userId = computed(() => authStore.user?.id || 'guest')

  function getStorageKey(prefix, identifier) {
    return `velobus_op_${prefix}_u${userId.value}_${String(identifier || '').trim()}`
  }

  function readState(prefix, identifier) {
    try {
      const raw = sessionStorage.getItem(getStorageKey(prefix, identifier))
      return raw ? JSON.parse(raw) : null
    } catch {
      return null
    }
  }

  function writeState(prefix, identifier, data) {
    try {
      if (!identifier) return
      sessionStorage.setItem(getStorageKey(prefix, identifier), JSON.stringify({
        ...data,
        updatedAt: Date.now()
      }))
    } catch (e) {
      console.warn('[BookingGuard] Failed to save operation state', e)
    }
  }

  function removeState(prefix, identifier) {
    try {
      if (!identifier) return
      sessionStorage.removeItem(getStorageKey(prefix, identifier))
    } catch {}
  }

  // --- HOLD MUTATIONS ---
  function getUnresolvedHold(tripId) {
    return readState('hold_unresolved', tripId)
  }

  function setUnresolvedHold(tripId, data) {
    writeState('hold_unresolved', tripId, data)
  }

  function clearUnresolvedHold(tripId) {
    removeState('hold_unresolved', tripId)
  }

  // --- CANCELLATION MUTATIONS ---
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
    const payload = typeof errorInfo === 'string'
      ? { message: errorInfo }
      : { message: errorInfo?.message || 'Lỗi thao tác hủy', ...errorInfo }
    writeState('cancel_unresolved', bookingReference, payload)
  }

  function clearUnresolvedCancel(bookingReference) {
    removeState('cancel_unresolved', bookingReference)
  }

  // --- PAYMENT MUTATIONS ---
  function getUnresolvedPayment(bookingReference) {
    return readState('payment_unresolved', bookingReference)
  }

  function setUnresolvedPayment(bookingReference, data) {
    writeState('payment_unresolved', bookingReference, data)
  }

  function clearUnresolvedPayment(bookingReference) {
    removeState('payment_unresolved', bookingReference)
  }

  // --- CHECKOUT ELIGIBILITY GUARD ---
  function isCheckoutBlocked(bookingReference) {
    if (!bookingReference) return false
    // Blocked if cancellation is pending or in unresolved outcome state
    if (isCancelPending(bookingReference)) return true
    if (getUnresolvedCancel(bookingReference)) return true
    return false
  }

  return {
    getUnresolvedHold,
    setUnresolvedHold,
    clearUnresolvedHold,

    isCancelPending,
    setCancelPending,
    clearCancelPending,
    getUnresolvedCancel,
    isCancelUnresolved,
    getCancelError,
    setUnresolvedCancel,
    clearUnresolvedCancel,

    getUnresolvedPayment,
    setUnresolvedPayment,
    clearUnresolvedPayment,

    isCheckoutBlocked
  }
}
