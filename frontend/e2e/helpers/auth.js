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
}
