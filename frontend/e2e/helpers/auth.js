/**
 * E2E Auth Helper - Sinh token hợp lệ và inject vào localStorage của Playwright Browser context
 */
export function makeE2eJwt(role = 'ROLE_CUSTOMER', username = 'test_user') {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
  const payload = btoa(
    JSON.stringify({
      sub: username,
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
  const token = makeE2eJwt(role, username)
  const user = { username: username || (role === 'ROLE_ADMIN' ? 'admin' : role === 'ROLE_OPERATOR' ? 'operator1' : 'customer1'), role }
  await page.addInitScript(({ token, user }) => {
    window.localStorage.setItem('velobus_access_token', token)
    window.localStorage.setItem('velobus_user', JSON.stringify(user))
  }, { token, user })
}
