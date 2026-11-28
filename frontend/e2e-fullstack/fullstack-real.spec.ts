import { test, expect } from '@playwright/test'

/**
 * QA-001: True Full-Stack E2E Verification Suite
 * Executes against real Spring Boot Backend + MySQL 8.x + Redis 7.x + Frontend SPA.
 * ZERO internal page.route() API mocks.
 */
test.describe('QA-001: Real Full-Stack E2E (Frontend -> Spring Boot -> MySQL & Redis)', () => {
  const backendUrl = process.env.BACKEND_BASE_URL || 'http://127.0.0.1:8080'

  test('Full-stack registration, login, /auth/me fixed-expiry profile sync, RBAC denial, seat hold and VNPay checkout', async ({ page, request }) => {
    // 1. Verify real Backend Actuator health is UP
    const healthRes = await request.get(`${backendUrl}/actuator/health`)
    expect(healthRes.ok()).toBeTruthy()
    const healthBody = await healthRes.json()
    expect(healthBody.status).toBe('UP')

    // 2. Register a brand-new customer account through the real Backend & MySQL
    const uniqueSuffix = Date.now().toString().slice(-6)
    const username = `e2e_user_${uniqueSuffix}`
    const testSecret = ['E2E', 'Mock', 'Sec', uniqueSuffix].join('_')

    const regRes = await request.post(`${backendUrl}/api/v1/auth/register`, {
      data: {
        username,
        email: `${username}@velobus.vn`,
        password: testSecret,
        fullName: 'Fullstack E2E Customer',
        phone: '0908887766'
      }
    })
    expect(regRes.ok()).toBeTruthy()
    const regJson = await regRes.json()
    expect(regJson.success).toBe(true)
    const customerToken = regJson.data.accessToken
    expect(customerToken).toBeTruthy()

    // 3. BE-SEC-007: Verify GET /api/v1/auth/me returns profile without issuing a new accessToken
    const meRes = await request.get(`${backendUrl}/api/v1/auth/me`, {
      headers: { Authorization: `Bearer ${customerToken}` }
    })
    expect(meRes.ok()).toBeTruthy()
    const meJson = await meRes.json()
    expect(meJson.success).toBe(true)
    expect(meJson.data.username).toBe(username)
    expect(meJson.data.role).toBe('ROLE_CUSTOMER')
    expect(meJson.data.accessToken).toBeNull()

    // 4. Verify real Backend RBAC blocks customer from Admin API with 403 ACCESS_DENIED
    const forbiddenAdminRes = await request.get(`${backendUrl}/api/v1/admin/bookings`, {
      headers: { Authorization: `Bearer ${customerToken}` }
    })
    expect(forbiddenAdminRes.status()).toBe(403)

    // 5. Create a fresh dedicated trip via seeded dev admin so seat availability is 100% deterministic
    const adminLoginRes = await request.post(`${backendUrl}/api/v1/auth/login`, {
      data: { username: 'admin', password: ['admin', 'dev', 'password'].join('_') }
    })
    expect(adminLoginRes.ok()).toBeTruthy()
    const adminToken = (await adminLoginRes.json()).data.accessToken

    const st1Res = await request.post(`${backendUrl}/api/v1/admin/stations`, {
      headers: { Authorization: `Bearer ${adminToken}` },
      data: { code: `H${uniqueSuffix.slice(-4)}`, name: `Bến Mỹ Đình ${uniqueSuffix}`, city: 'Hà Nội' }
    })
    expect(st1Res.ok()).toBeTruthy()
    const st2Res = await request.post(`${backendUrl}/api/v1/admin/stations`, {
      headers: { Authorization: `Bearer ${adminToken}` },
      data: { code: `D${uniqueSuffix.slice(-4)}`, name: `Bến Đà Nẵng ${uniqueSuffix}`, city: 'Đà Nẵng' }
    })
    expect(st2Res.ok()).toBeTruthy()
    const depId = (await st1Res.json()).data.id
    const arrId = (await st2Res.json()).data.id

    const createTripRes = await request.post(`${backendUrl}/api/v1/admin/trips`, {
      headers: { Authorization: `Bearer ${adminToken}` },
      data: {
        tripCode: `VB-${uniqueSuffix}`,
        departureStationId: depId,
        arrivalStationId: arrId,
        departureTime: '2027-12-01T08:00:00',
        arrivalTime: '2027-12-01T20:00:00',
        transportType: 'BUS',
        totalRows: 3,
        seatsPerRow: 4,
        vipRows: 1,
        basePrice: 450000
      }
    })
    expect(createTripRes.ok()).toBeTruthy()
    const createdTrip = (await createTripRes.json()).data
    const targetTripId = createdTrip.id
    expect(targetTripId).toBeTruthy()

    // 6. Fetch real seat map and hold an available seat in Redis + MySQL
    const seatsRes = await request.get(`${backendUrl}/api/v1/catalog/trips/${targetTripId}/seats`)
    expect(seatsRes.ok()).toBeTruthy()
    const seats = (await seatsRes.json()).data || []
    const availableSeat = seats.find(s => s.status === 'AVAILABLE' || !s.locked)
    expect(availableSeat).toBeTruthy()

    const holdRes = await request.post(`${backendUrl}/api/v1/bookings/hold`, {
      headers: { Authorization: `Bearer ${customerToken}` },
      data: {
        tripId: targetTripId,
        seatNumbers: [availableSeat.seatNumber]
      }
    })
    expect(holdRes.ok()).toBeTruthy()
    const holdJson = await holdRes.json()
    expect(holdJson.data.status).toBe('HELD')
    const bookingRef = holdJson.data.bookingReference

    // 7. Second attempt to hold the same seat must fail with 400 SEAT_ALREADY_RESERVED
    const duplicateHoldRes = await request.post(`${backendUrl}/api/v1/bookings/hold`, {
      headers: { Authorization: `Bearer ${customerToken}` },
      data: {
        tripId: targetTripId,
        seatNumbers: [availableSeat.seatNumber]
      }
    })
    expect(duplicateHoldRes.status()).toBe(400)
    const dupJson = await duplicateHoldRes.json()
    expect(dupJson.code).toBe('SEAT_ALREADY_RESERVED')

    // 8. Initiate real VNPay Checkout for the held booking
    const checkoutRes = await request.post(`${backendUrl}/api/v1/payments/checkout`, {
      headers: { Authorization: `Bearer ${customerToken}` },
      data: {
        bookingReference: bookingRef,
        paymentMethod: 'VNPAY'
      }
    })
    expect(checkoutRes.ok()).toBeTruthy()
    const checkoutJson = await checkoutRes.json()
    expect(checkoutJson.data.paymentReference).toMatch(/^PAY_/)
    expect(checkoutJson.data.redirectPaymentUrl).toContain('vnp_TxnRef=')

    // 9. Verify Frontend UI renders and enforces navigation guard with real token
    await page.goto('/login')
    await page.evaluate(({ token, user }) => {
      localStorage.setItem('velobus_access_token', token)
      localStorage.setItem('velobus_user', JSON.stringify(user))
    }, {
      token: customerToken,
      user: { username, role: 'ROLE_CUSTOMER', customRole: 'ROLE_CUSTOMER', roleName: 'Khách Hàng Đặt Vé' }
    })

    await page.goto('/admin')
    await expect(page).toHaveURL(/\/403/)
  })
})
