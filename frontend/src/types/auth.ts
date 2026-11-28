/**
 * VeloBus Authentication & Session Types
 * Strictly mirrors AuthResponse.java, AuthRequest.java, RegisterRequest.java
 */
import type { UserBaseRole } from './rbac'

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  username: string
  role: UserBaseRole | string
  customRole?: string | null
  roleName?: string | null
  permissions: string[]
}

export interface AuthUserSession {
  username: string
  role: string
  customRole?: string | null
  roleName?: string | null
  permissions: string[]
}

export interface LoginCredentials {
  username: string
  password?: string
}

export interface RegisterPayload {
  username: string
  password?: string
  email?: string
  fullName?: string
  phone?: string
}
