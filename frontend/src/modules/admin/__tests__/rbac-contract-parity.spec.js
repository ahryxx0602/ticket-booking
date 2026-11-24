import { describe, it, expect, beforeEach } from 'vitest'
import fs from 'node:fs'
import path from 'node:path'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore, ADMIN_PERMISSION_MODULES, ALL_ADMIN_PERMISSION_CODES } from '@/modules/auth/store'
import httpClient, { isJwtTokenExpired } from '@/shared/api/httpClient'

function makeFakeJwt(payload) {
  const encodeUtf8Base64Url = (obj) => {
    const utf8Str = encodeURIComponent(JSON.stringify(obj)).replace(/%([0-9A-F]{2})/g, (_, p1) =>
      String.fromCharCode(parseInt(p1, 16))
    )
    return btoa(utf8Str)
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '')
  }
  return `${encodeUtf8Base64Url({ alg: 'HS256', typ: 'JWT' })}.${encodeUtf8Base64Url(payload)}.sig`
}

describe('Phase 3 & Phase 4 Remediation: RBAC Contract Parity & Auth/HttpClient Hardening', () => {
  beforeEach(() => {
    const pinia = createPinia()
    setActivePinia(pinia)
  })

  it('QA-001 & FE-SEC-003: Frontend ADMIN_PERMISSION_MODULES matches Backend AdminPermissionCatalog.java 1:1 (7 Modules x 26 Permissions)', () => {
    const backendCatalogPath = path.resolve(
      process.cwd(),
      '../backend/context-identity/identity-domain/src/main/java/com/ticket/identity/domain/model/AdminPermissionCatalog.java'
    )
    const javaSource = fs.readFileSync(backendCatalogPath, 'utf-8')

    // Extract all new PermissionItem("CODE", ...) from Java source
    const javaPermMatches = [...javaSource.matchAll(/new PermissionItem\("([A-Z0-9_]+)"/g)].map(m => m[1])
    // Extract all new ModuleCatalog(\n "MOD_...", ...) from Java source
    const javaModuleMatches = [...javaSource.matchAll(/new ModuleCatalog\(\s*"([A-Z0-9_]+)"/g)].map(m => m[1])

    expect(ADMIN_PERMISSION_MODULES).toHaveLength(7)
    expect(ALL_ADMIN_PERMISSION_CODES).toHaveLength(26)

    const frontendModules = ADMIN_PERMISSION_MODULES.map(m => m.moduleCode)
    expect(frontendModules).toEqual(javaModuleMatches)
    expect(ALL_ADMIN_PERMISSION_CODES).toEqual(javaPermMatches)
  })

  it('FE-SEC-002: useAuthStore prioritizes liveProfile permissions from /api/v1/auth/me over stale JWT claims', () => {
    const auth = useAuthStore()
    auth.token = makeFakeJwt({
      sub: 'accountant01',
      role: 'ROLE_ADMIN',
      customRole: 'ROLE_ACCOUNTANT',
      permissions: ['PAYMENT_REPORT_VIEW', 'WITHDRAWAL_APPROVE'],
      exp: Math.floor(Date.now() / 1000) + 3600
    })

    expect(auth.hasPermission('WITHDRAWAL_APPROVE')).toBe(true)

    // Simulate live sync from GET /api/v1/auth/me where Super Admin revoked WITHDRAWAL_APPROVE
    auth.liveProfile = {
      role: 'ROLE_ADMIN',
      customRole: 'ROLE_ACCOUNTANT',
      roleName: 'Kế Toán (Đã thu hồi quyền duyệt chi)',
      permissions: ['PAYMENT_REPORT_VIEW']
    }

    expect(auth.hasPermission('WITHDRAWAL_APPROVE')).toBe(false)
    expect(auth.hasPermission('PAYMENT_REPORT_VIEW')).toBe(true)
    expect(auth.roleName).toBe('Kế Toán (Đã thu hồi quyền duyệt chi)')
  })

  it('FE-SEC-004: isJwtTokenExpired detects expired tokens and unauthorized event logs out store immediately', () => {
    const expiredJwt = makeFakeJwt({
      sub: 'user1',
      role: 'ROLE_CUSTOMER',
      exp: Math.floor(Date.now() / 1000) - 60
    })
    const validJwt = makeFakeJwt({
      sub: 'user1',
      role: 'ROLE_CUSTOMER',
      exp: Math.floor(Date.now() / 1000) + 3600
    })

    expect(isJwtTokenExpired(expiredJwt)).toBe(true)
    expect(isJwtTokenExpired(validJwt)).toBe(false)
    expect(isJwtTokenExpired('malformed')).toBe(true)

    const auth = useAuthStore()
    auth.token = validJwt
    expect(auth.isAuthenticated).toBe(true)

    window.dispatchEvent(new CustomEvent('velobus:unauthorized'))
    expect(auth.isAuthenticated).toBe(false)
    expect(auth.role).toBe('GUEST')
  })

  it('FE-SEC-005: httpClient blocks mutating admin requests (POST/PUT/PATCH/DELETE) when Role Preview mode is active', async () => {
    const auth = useAuthStore()
    auth.token = makeFakeJwt({
      sub: 'admin',
      role: 'ROLE_ADMIN',
      customRole: 'ROLE_ADMIN',
      exp: Math.floor(Date.now() / 1000) + 3600
    })

    auth.startRolePreview({
      code: 'ROLE_CSKH',
      name: 'CSKH',
      permissions: ['BOOKING_VIEW', 'BOOKING_FORCE_REFUND']
    })

    // Run request interceptor directly with a mutating POST /admin/bookings/1/refund config
    const reqInterceptor = httpClient.interceptors.request.handlers[0].fulfilled
    await expect(
      reqInterceptor({
        method: 'post',
        url: '/admin/bookings/BKG-1/refund',
        headers: {}
      })
    ).rejects.toMatchObject({
      code: 'PREVIEW_MODE_READONLY'
    })

    auth.stopRolePreview()
  })
})
