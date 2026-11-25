/**
 * VeloBus API Envelope and Core Response Contracts
 * Strictly mirrors Spring Boot shared-kernel ApiResponse and PageResponse.
 */

export interface ApiResponse<T = unknown> {
  success: boolean
  message: string
  code?: string | null
  data?: T
  timestamp: string
}

export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}

export interface ApiErrorPayload {
  code?: string
  message: string
  timestamp?: string
  fieldErrors?: Record<string, string>
}
