/**
 * E2E Auth Helper - Sinh token hợp lệ và inject vào localStorage của Playwright Browser context
 */
export function makeE2eJwt(role = 'ROLE_CUSTOMER', username) {
  const defaultUser = role === 'ROLE_ADMIN' ? 'admin' : role === 'ROLE_OPERATOR' ? 'operator1' : 'customer1'
  const sub = username || defaultUser
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
  const payload = btoa(
    JSON.stringify({
      sub,
      role: role,
      exp: Math.floor(Date.now() / 1000) + 86400, // Còn hạn 24 giờ
    })
  )
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
  return `${header}.${payload}.mock_e2e_signature`
}

export async function loginAsRole(page, role, username) {
  const defaultUser = role === 'ROLE_ADMIN' ? 'admin' : role === 'ROLE_OPERATOR' ? 'operator1' : 'customer1'
  const resolvedUsername = username || defaultUser
  const token = makeE2eJwt(role, resolvedUsername)
  const user = { id: 1, username: resolvedUsername, role }
  await page.addInitScript(({ token, user }) => {
    window.localStorage.setItem('velobus_access_token', token)
    window.localStorage.setItem('velobus_user', JSON.stringify(user))
  }, { token, user })

  await page.route('**/api/v1/auth/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          id: 1,
          username: resolvedUsername,
          role,
          customRole: role,
          roleName: role === 'ROLE_ADMIN' ? 'Quản trị viên tối cao' : role === 'ROLE_OPERATOR' ? 'Đối tác nhà xe' : 'Hành khách',
          permissions: role === 'ROLE_ADMIN' ? [
            'PAYMENT_REPORT_VIEW', 'PAYMENT_LIST_VIEW', 'PAYMENT_RECONCILE_RESOLVE',
            'OPERATOR_VIEW', 'OPERATOR_CREATE_UPDATE', 'OPERATOR_APPROVE_REJECT', 'OPERATOR_SUSPEND', 'OPERATOR_COMMISSION_MANAGE',
            'WITHDRAWAL_VIEW', 'WITHDRAWAL_APPROVE', 'WITHDRAWAL_REJECT',
            'BANNER_VIEW', 'BANNER_CREATE_DELETE', 'BANNER_APPROVE_REJECT',
            'USER_VIEW', 'USER_CREATE_UPDATE', 'USER_ROLE_CHANGE', 'USER_STATUS_CHANGE', 'USER_PASSWORD_RESET', 'BOOKING_LOOKUP', 'BOOKING_REFUND',
            'ROLE_VIEW', 'ROLE_CREATE_UPDATE', 'ROLE_DELETE',
            'TRIP_SYSTEM_VIEW', 'SYSTEM_CONFIG_MANAGE'
          ] : []
        }
      })
    })
  })
}
