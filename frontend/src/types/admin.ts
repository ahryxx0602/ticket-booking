/**
 * VeloBus Admin Governance Types
 */
import type { AdminPermissionCode, UserBaseRole } from './rbac'

export type AdminUserCategory = 'CUSTOMER' | 'OPERATOR' | 'SYSTEM'

export interface AdminUserDto {
  id: number
  username: string
  email?: string
  fullName?: string
  phone?: string
  role: UserBaseRole | string
  customRoleCode?: string | null
  customRoleName?: string | null
  status: 'ACTIVE' | 'LOCKED'
  createdAt?: string
}

export interface SystemRoleDto {
  id: number
  roleCode: string
  roleName: string
  description?: string
  permissions: AdminPermissionCode[]
  userCount?: number
}

export interface SystemSettingsDto {
  defaultCommissionRate: number
  seatHoldTtlMinutes: number
  maxSeatsPerBooking: number
  maintenanceMode: boolean
  updatedAt?: string
}

export interface PaymentReportDto {
  totalRevenue: number
  totalPlatformCommission: number
  totalBookings: number
  totalOperators: number
  reconciliationDiscrepancies?: number
}
