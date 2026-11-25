/**
 * VeloBus Catalog, Seat & Booking Contracts
 * Strictly mirrors Backend BookingResponse, HoldSeatCommand, CreatePaymentCommand, etc.
 */

export type BookingStatus = 'HELD' | 'CONFIRMED' | 'CANCELLED' | 'REFUNDED' | 'EXPIRED'

export type PaymentMethod = 'VNPAY' | 'MOMO' | 'ZALOPAY'

export type SeatStatus = 'AVAILABLE' | 'HELD_OTHER' | 'SOLD' | 'SELECTED'

export interface StationDto {
  id: number
  code: string
  name: string
  city: string
  address?: string
}

export interface TripSummaryDto {
  id: number
  tripCode: string
  routeId: number
  routeName: string
  departureCity: string
  arrivalCity: string
  departureStation: string
  arrivalStation: string
  departureTime: string
  arrivalTime: string
  operatorName: string
  vehiclePlate: string
  vehicleType: string
  basePrice: number
  availableSeats: number
  totalSeats: number
}

export interface SeatDto {
  id: number
  seatNumber: string
  deck: number // 1: Lower deck, 2: Upper deck
  status: SeatStatus
  price: number
}

export interface HoldSeatCommand {
  userId?: number
  tripId: number
  seatNumbers: string[]
}

export interface BookingItemDto {
  seatId: number
  seatNumber: string
  price: number
}

export interface BookingResponse {
  bookingId: number
  bookingReference: string
  userId: number
  tripId: number
  status: BookingStatus
  totalAmount: number
  expiresAt?: string // LocalDateTime ISO string
  createdAt?: string
  seatNumbers: string[]
  items?: BookingItemDto[]
  qrPayload?: string // Server-signed payload
}

export interface CreatePaymentCommand {
  bookingReference: string
  paymentMethod: PaymentMethod
}

export interface PaymentInitResponse {
  paymentReference: string
  redirectPaymentUrl: string
}

export interface CatalogTripSearchParams {
  departureCity?: string
  arrivalCity?: string
  departureDate?: string // YYYY-MM-DD
}
