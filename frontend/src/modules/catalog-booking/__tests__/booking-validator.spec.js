import { describe, it, expect } from 'vitest'
import {
  parseIsoDate,
  normalizeSeatSet,
  validateBookingResponse
} from '../utils/booking-validator'

describe('booking-validator.js unit tests', () => {
  describe('parseIsoDate', () => {
    it('parses valid ISO string and number', () => {
      const iso = '2026-11-26T15:30:00+07:00'
      const t = parseIsoDate(iso)
      expect(Number.isFinite(t)).toBe(true)

      expect(parseIsoDate(1732600000000)).toBe(1732600000000)
      expect(parseIsoDate(new Date(1732600000000))).toBe(1732600000000)
    })

    it('rejects invalid or empty inputs', () => {
      expect(Number.isNaN(parseIsoDate(''))).toBe(true)
      expect(Number.isNaN(parseIsoDate('invalid-date'))).toBe(true)
      expect(Number.isNaN(parseIsoDate(null))).toBe(true)
      expect(Number.isNaN(parseIsoDate(undefined))).toBe(true)
      expect(Number.isNaN(parseIsoDate({}))).toBe(true)
    })
  })

  describe('normalizeSeatSet', () => {
    it('normalizes, trims, uppercases and sorts seat numbers', () => {
      expect(normalizeSeatSet([' b02 ', 'A01', 'a01', 'B01'])).toEqual(['A01', 'B01', 'B02'])
      expect(normalizeSeatSet(null)).toEqual([])
      expect(normalizeSeatSet(undefined)).toEqual([])
    })
  })

  describe('validateBookingResponse (PRE-03)', () => {
    const validBooking = {
      bookingReference: 'VB-HOLD-1234',
      status: 'HELD',
      tripId: 101,
      userId: 42,
      totalAmount: 350000,
      seatNumbers: ['A01', 'A02'],
      expiresAt: new Date(Date.now() + 600000).toISOString()
    }

    it('passes for a fully valid booking response', () => {
      const res = validateBookingResponse(validBooking, {
        expectedTripId: 101,
        expectedUserId: 42,
        expectedSeats: ['A01', 'A02']
      })
      expect(res.valid).toBe(true)
      expect(res.expiresAtMs).toBeGreaterThan(Date.now())
    })

    it('preserves valid 0 totalAmount (free or voucher booking)', () => {
      const zeroBooking = { ...validBooking, totalAmount: 0 }
      const res = validateBookingResponse(zeroBooking)
      expect(res.valid).toBe(true)
    })

    it('rejects non-object or null input', () => {
      expect(validateBookingResponse(null).valid).toBe(false)
      expect(validateBookingResponse('string').valid).toBe(false)
    })

    it('rejects missing or empty bookingReference', () => {
      expect(validateBookingResponse({ ...validBooking, bookingReference: '' }).valid).toBe(false)
      expect(validateBookingResponse({ ...validBooking, bookingReference: null }).valid).toBe(false)
    })

    it('rejects non-HELD status', () => {
      expect(validateBookingResponse({ ...validBooking, status: 'CONFIRMED' }).valid).toBe(false)
      expect(validateBookingResponse({ ...validBooking, status: 'PENDING' }).valid).toBe(false)
      expect(validateBookingResponse({ ...validBooking, status: null }).valid).toBe(false)
    })

    it('rejects mismatched tripId', () => {
      const res = validateBookingResponse(validBooking, { expectedTripId: 999 })
      expect(res.valid).toBe(false)
      expect(res.error).toContain('Mã chuyến xe không khớp')
    })

    it('rejects mismatched userId or missing userId when expectedUserId is set', () => {
      const res = validateBookingResponse(validBooking, { expectedUserId: 999 })
      expect(res.valid).toBe(false)
      expect(res.error).toContain('người dùng khác')

      const noUserBooking = { ...validBooking, userId: null }
      const res2 = validateBookingResponse(noUserBooking, { expectedUserId: 42 })
      expect(res2.valid).toBe(false)
      expect(res2.error).toContain('thiếu thông tin người dùng')
    })

    it('rejects invalid or negative totalAmount, including boolean and empty string', () => {
      expect(validateBookingResponse({ ...validBooking, totalAmount: -100 }).valid).toBe(false)
      expect(validateBookingResponse({ ...validBooking, totalAmount: 'not-a-number' }).valid).toBe(false)
      expect(validateBookingResponse({ ...validBooking, totalAmount: null }).valid).toBe(false)
      expect(validateBookingResponse({ ...validBooking, totalAmount: false }).valid).toBe(false)
      expect(validateBookingResponse({ ...validBooking, totalAmount: true }).valid).toBe(false)
      expect(validateBookingResponse({ ...validBooking, totalAmount: '  ' }).valid).toBe(false)
    })

    it('supports allowTerminalReplay option for idempotency recovery', () => {
      const confirmedBooking = { ...validBooking, status: 'CONFIRMED' }
      expect(validateBookingResponse(confirmedBooking).valid).toBe(false)
      expect(validateBookingResponse(confirmedBooking, { allowTerminalReplay: true }).valid).toBe(true)

      const cancelledBooking = { ...validBooking, status: 'CANCELLED' }
      expect(validateBookingResponse(cancelledBooking, { allowTerminalReplay: true }).valid).toBe(true)

      const expiredBooking = {
        ...validBooking,
        status: 'EXPIRED',
        expiresAt: new Date(Date.now() - 60000).toISOString()
      }
      expect(validateBookingResponse(expiredBooking, { allowTerminalReplay: true }).valid).toBe(true)
    })

    it('enforces exact clientRequestId match when expectedClientRequestId is specified (PRE-03)', () => {
      const bookingWithKey = { ...validBooking, clientRequestId: 'req-uuid-1234' }
      expect(validateBookingResponse(bookingWithKey, { expectedClientRequestId: 'req-uuid-1234' }).valid).toBe(true)

      const mismatchRes = validateBookingResponse(bookingWithKey, { expectedClientRequestId: 'req-other-5678' })
      expect(mismatchRes.valid).toBe(false)
      expect(mismatchRes.error).toContain('mismatch')

      const missingKeyRes = validateBookingResponse(validBooking, { expectedClientRequestId: 'req-uuid-1234' })
      expect(missingKeyRes.valid).toBe(false)
      expect(missingKeyRes.error).toContain('clientRequestId')
    })

    it('rejects empty or corrupt seatNumbers', () => {
      expect(validateBookingResponse({ ...validBooking, seatNumbers: [] }).valid).toBe(false)
      expect(validateBookingResponse({ ...validBooking, seatNumbers: null }).valid).toBe(false)
    })

    it('strictly checks exact seat set match (PRE-03 requirement)', () => {
      // Missing a seat
      const res1 = validateBookingResponse(validBooking, { expectedSeats: ['A01', 'A02', 'A03'] })
      expect(res1.valid).toBe(false)
      expect(res1.error).toContain('không khớp chính xác')

      // Different seat
      const res2 = validateBookingResponse(validBooking, { expectedSeats: ['A01', 'B01'] })
      expect(res2.valid).toBe(false)

      // Same seats in different order passes
      const res3 = validateBookingResponse(validBooking, { expectedSeats: ['A02', 'A01'] })
      expect(res3.valid).toBe(true)
    })

    it('rejects invalid or expired timestamps', () => {
      const invalidTimestamp = { ...validBooking, expiresAt: 'invalid' }
      expect(validateBookingResponse(invalidTimestamp).valid).toBe(false)

      const expiredBooking = { ...validBooking, expiresAt: new Date(Date.now() - 5000).toISOString() }
      expect(validateBookingResponse(expiredBooking).valid).toBe(false)

      // If allowPastExpiry is true (for historical audit)
      expect(validateBookingResponse(expiredBooking, { allowPastExpiry: true }).valid).toBe(true)
    })
  })
})
