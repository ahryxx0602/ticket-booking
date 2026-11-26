/**
 * BookingResponse Validator & Timezone-Safe Timestamp Utility
 * PRE-03: Shared validator for Hold response, Booking detail, Reload recovery, and Checkout eligibility.
 */

/**
 * Parse ISO date string to millisecond timestamp.
 * Rejects invalid, non-finite or empty timestamps.
 * Does not assume LocalDateTime is UTC; parses in accordance with ISO string specification.
 */
export function parseIsoDate(dateInput) {
  if (!dateInput) return NaN
  if (dateInput instanceof Date) {
    const t = dateInput.getTime()
    return Number.isFinite(t) ? t : NaN
  }
  if (typeof dateInput === 'number') {
    return Number.isFinite(dateInput) ? dateInput : NaN
  }
  if (typeof dateInput !== 'string') return NaN

  const trimmed = dateInput.trim()
  if (!trimmed) return NaN

  const timestamp = new Date(trimmed).getTime()
  return Number.isFinite(timestamp) ? timestamp : NaN
}

/**
 * Normalizes an array of seat numbers for exact set comparison:
 * trims, uppercases, removes duplicates, and sorts alphabetically.
 */
export function normalizeSeatSet(seatList) {
  if (!Array.isArray(seatList)) return []
  return [...new Set(seatList.map(s => String(s || '').trim().toUpperCase()).filter(Boolean))].sort()
}

/**
 * Strictly validates a BookingResponse object against expected parameters.
 *
 * Requirements:
 * - bookingReference: non-empty string
 * - status: strictly 'HELD'
 * - tripId: matches expectedTripId (if provided)
 * - userId: matches expectedUserId (if provided)
 * - totalAmount: finite number >= 0 (preserves valid 0)
 * - seatNumbers: non-empty array of strings. If expectedSeats provided, MUST EXACTLY match.
 *   DO NOT fallback missing confirmed seats to requestedSeats!
 * - expiresAt: valid finite timestamp in the future.
 *
 * @param {object} booking The booking object from API
 * @param {object} options { expectedTripId, expectedUserId, expectedSeats, allowPastExpiry }
 * @returns {{ valid: boolean, error?: string, expiresAtMs?: number }}
 */
export function validateBookingResponse(booking, options = {}) {
  const {
    expectedTripId,
    expectedUserId,
    expectedSeats,
    allowPastExpiry = false,
    allowTerminalReplay = false
  } = options

  if (!booking || typeof booking !== 'object') {
    return { valid: false, error: 'Dữ liệu phản hồi đặt chỗ không phải là đối tượng hợp lệ' }
  }

  // 1. bookingReference
  const ref = booking.bookingReference || booking.reference
  if (!ref || typeof ref !== 'string' || ref.trim().length === 0) {
    return { valid: false, error: 'Thiếu mã tham chiếu đặt chỗ (bookingReference)' }
  }

  // 2. status check: strictly HELD unless terminal replay is permitted
  const allowedStatuses = allowTerminalReplay
    ? ['HELD', 'CONFIRMED', 'CANCELLED', 'EXPIRED']
    : ['HELD']
  if (!allowedStatuses.includes(booking.status)) {
    return {
      valid: false,
      error: `Trạng thái giữ chỗ không hợp lệ: ${booking.status || 'UNDEFINED'} (Yêu cầu: ${allowedStatuses.join('/')})`
    }
  }

  // 3. tripId match
  if (expectedTripId !== undefined && expectedTripId !== null) {
    if (booking.tripId === undefined || booking.tripId === null) {
      return { valid: false, error: 'Phản hồi thiếu thông tin chuyến xe (tripId)' }
    }
    if (String(booking.tripId).trim() !== String(expectedTripId).trim()) {
      return { valid: false, error: `Mã chuyến xe không khớp (Nhận được: ${booking.tripId}, Yêu cầu: ${expectedTripId})` }
    }
  }

  // 4. userId match: if expectedUserId provided, booking.userId MUST be present and match
  if (expectedUserId !== undefined && expectedUserId !== null) {
    if (booking.userId === undefined || booking.userId === null) {
      return { valid: false, error: 'Phản hồi thiếu thông tin người dùng (userId)' }
    }
    if (String(booking.userId).trim() !== String(expectedUserId).trim()) {
      return { valid: false, error: 'Phiên giữ chỗ thuộc về người dùng khác' }
    }
  }

  // 5. totalAmount: finite number >= 0; explicitly reject boolean, empty string or null/undefined
  if (
    booking.totalAmount === undefined ||
    booking.totalAmount === null ||
    typeof booking.totalAmount === 'boolean' ||
    (typeof booking.totalAmount === 'string' && booking.totalAmount.trim() === '')
  ) {
    return { valid: false, error: 'Thiếu tổng tiền thanh toán hoặc định dạng không hợp lệ' }
  }
  const totalNum = Number(booking.totalAmount)
  if (!Number.isFinite(totalNum) || totalNum < 0) {
    return { valid: false, error: `Tổng tiền thanh toán không hợp lệ: ${booking.totalAmount}` }
  }

  // 6. seatNumbers: non-empty array of strings
  const seats = booking.seatNumbers || booking.seats
  if (!Array.isArray(seats) || seats.length === 0) {
    return { valid: false, error: 'Danh sách chỗ ngồi được xác nhận từ máy chủ rỗng hoặc không hợp lệ' }
  }

  const normalizedReturnedSeats = normalizeSeatSet(seats)
  if (normalizedReturnedSeats.length === 0) {
    return { valid: false, error: 'Không có chỗ ngồi hợp lệ trong phản hồi máy chủ' }
  }

  // Exact seat set matching if expectedSeats provided
  if (expectedSeats) {
    const normalizedExpected = normalizeSeatSet(expectedSeats)
    const match = normalizedReturnedSeats.length === normalizedExpected.length &&
      normalizedReturnedSeats.every((val, idx) => val === normalizedExpected[idx])
    if (!match) {
      return {
        valid: false,
        error: `Danh sách chỗ ngồi được máy chủ xác nhận (${normalizedReturnedSeats.join(', ')}) không khớp chính xác với yêu cầu (${normalizedExpected.join(', ')})`
      }
    }
  }

  // 7. expiresAt: finite timestamp in future (for HELD status)
  const expiresAtMs = parseIsoDate(booking.expiresAt)
  if (!Number.isFinite(expiresAtMs)) {
    return { valid: false, error: `Thời gian hết hạn giữ chỗ (expiresAt) không hợp lệ: ${booking.expiresAt}` }
  }

  const isTerminalReplay = allowTerminalReplay && booking.status !== 'HELD'
  if (!allowPastExpiry && !isTerminalReplay && expiresAtMs <= Date.now()) {
    return { valid: false, error: 'Phiên giữ chỗ từ máy chủ đã quá hạn thời gian thanh toán' }
  }

  return {
    valid: true,
    expiresAtMs
  }
}
