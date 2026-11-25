/**
 * VeloBus Partner & Escrow Types
 */

export interface PartnerWalletDto {
  availableBalance: number
  holdingBalance: number
  settlingBalance: number
  withdrawnTotal: number
  currency: string
}

export interface PartnerWalletTransactionDto {
  id: number
  transactionReference: string
  amount: number
  type: 'BOOKING_CREDIT' | 'COMMISSION_DEBIT' | 'WITHDRAWAL_LOCK' | 'WITHDRAWAL_SUCCESS' | 'REFUND_DEBIT'
  status: 'PENDING' | 'SUCCESS' | 'FAILED'
  createdAt: string
  note?: string
}

export interface VehicleDto {
  id: number
  plateNumber: string
  vehicleType: string
  seatCapacity: number
  status: 'ACTIVE' | 'MAINTENANCE' | 'INACTIVE'
}

export interface DriverDto {
  id: number
  fullName: string
  licenseNumber: string
  phone: string
  status: 'ACTIVE' | 'OFF_DUTY'
}

export interface WithdrawalRequestDto {
  id: number
  amount: number
  bankCode: string
  accountNumber: string
  accountName: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  createdAt: string
  reviewedAt?: string
  adminNote?: string
}

export interface PartnerTripResponse {
  tripId: number
  tripCode: string
  routeId: number
  departureTime: string
  arrivalTime: string
  basePrice: number
  status: string
}
