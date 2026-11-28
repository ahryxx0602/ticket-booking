import { chromium } from '@playwright/test'
import path from 'path'
import fs from 'fs'
import { fileURLToPath } from 'url'
import { makeE2eJwt } from '../e2e/helpers/auth'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const TARGET_DIR = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-final-release')

if (!fs.existsSync(TARGET_DIR)) {
  fs.mkdirSync(TARGET_DIR, { recursive: true })
}

const BASE_URL = 'http://127.0.0.1:4173'

async function setupSyntheticMocks(page) {
  // Mock auth/me
  await page.route('**/api/v1/auth/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          id: 1,
          username: 'admin',
          role: 'ROLE_ADMIN',
          customRole: 'ROLE_ADMIN',
          roleName: 'Quản trị viên tối cao',
          permissions: [
            'PAYMENT_REPORT_VIEW', 'PAYMENT_LIST_VIEW', 'PAYMENT_RECONCILE_RESOLVE',
            'OPERATOR_VIEW', 'OPERATOR_CREATE_UPDATE', 'OPERATOR_APPROVE_REJECT', 'OPERATOR_SUSPEND', 'OPERATOR_COMMISSION_MANAGE',
            'WITHDRAWAL_VIEW', 'WITHDRAWAL_APPROVE', 'WITHDRAWAL_REJECT',
            'BANNER_VIEW', 'BANNER_CREATE_DELETE', 'BANNER_APPROVE_REJECT',
            'USER_VIEW', 'USER_CREATE_UPDATE', 'USER_ROLE_CHANGE', 'USER_STATUS_CHANGE', 'USER_PASSWORD_RESET', 'BOOKING_LOOKUP', 'BOOKING_REFUND',
            'ROLE_VIEW', 'ROLE_CREATE_UPDATE', 'ROLE_DELETE',
            'TRIP_SYSTEM_VIEW', 'SYSTEM_CONFIG_MANAGE'
          ]
        }
      })
    })
  })

  // Mock catalog stations
  await page.route('**/api/v1/catalog/stations*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 'OK',
        data: [
          { id: 1, name: 'Bến xe Miền Đông Mới', city: 'TP. Hồ Chí Minh' },
          { id: 2, name: 'Bến xe Liên Tỉnh Đà Lạt', city: 'Lâm Đồng' },
          { id: 3, name: 'Bến xe Trung Tâm Đà Nẵng', city: 'Đà Nẵng' },
          { id: 4, name: 'Bến xe Mỹ Đình', city: 'Hà Nội' }
        ]
      })
    })
  })

  // Mock catalog trips search
  await page.route('**/api/v1/catalog/trips*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 'OK',
        data: {
          items: [
            {
              id: 1,
              tripCode: 'SG-DL-0800',
              operatorName: 'Phương Trang FUTA Bus Lines',
              vehiclePlate: '51B-888.99',
              vehicleType: 'SLEEPER',
              departureStation: 'Bến xe Miền Đông Mới',
              arrivalStation: 'Bến xe Liên Tỉnh Đà Lạt',
              departureTime: '2026-12-01T08:00:00',
              arrivalTime: '2026-12-01T14:30:00',
              basePrice: 320000,
              availableSeats: 18,
              status: 'SCHEDULED'
            },
            {
              id: 2,
              tripCode: 'SG-DL-1030',
              operatorName: 'Thành Bưởi Limousine',
              vehiclePlate: '51B-999.88',
              vehicleType: 'VIP_LIMOUSINE',
              departureStation: 'Bến xe Miền Đông Mới',
              arrivalStation: 'Bến xe Liên Tỉnh Đà Lạt',
              departureTime: '2026-12-01T10:30:00',
              arrivalTime: '2026-12-01T16:45:00',
              basePrice: 450000,
              availableSeats: 6,
              status: 'SCHEDULED'
            }
          ],
          totalElements: 2,
          totalPages: 1,
          page: 0,
          size: 10
        }
      })
    })
  })

  // Mock seat selection
  await page.route('**/api/v1/catalog/trips/1/seats*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 'OK',
        data: {
          tripId: 1,
          tripCode: 'SG-DL-0800',
          vehiclePlate: '51B-888.99',
          seatRows: 9,
          seatsPerRow: 4,
          seats: [
            { id: 101, seatNumber: 'A01', deck: 1, status: 'AVAILABLE', seatType: 'VIP', priceMultiplier: 1.2 },
            { id: 102, seatNumber: 'A02', deck: 1, status: 'HELD', seatType: 'VIP', priceMultiplier: 1.2 },
            { id: 103, seatNumber: 'A03', deck: 1, status: 'AVAILABLE', seatType: 'STANDARD', priceMultiplier: 1.0 },
            { id: 104, seatNumber: 'A04', deck: 1, status: 'SOLD', seatType: 'STANDARD', priceMultiplier: 1.0 },
            { id: 105, seatNumber: 'B01', deck: 2, status: 'AVAILABLE', seatType: 'STANDARD', priceMultiplier: 1.0 },
            { id: 106, seatNumber: 'B02', deck: 2, status: 'AVAILABLE', seatType: 'STANDARD', priceMultiplier: 1.0 }
          ]
        }
      })
    })
  })

  // Mock checkout booking
  await page.route('**/api/v1/bookings/1*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 'OK',
        data: {
          id: 1,
          bookingCode: 'VB-BK-20261128-001',
          tripId: 1,
          tripCode: 'SG-DL-0800',
          operatorName: 'Phương Trang FUTA Bus Lines',
          departureStation: 'Bến xe Miền Đông Mới',
          arrivalStation: 'Bến xe Liên Tỉnh Đà Lạt',
          departureTime: '2026-12-01T08:00:00',
          seatNumbers: ['A01'],
          totalAmount: 384000,
          customerName: 'Nguyễn Văn Thành',
          customerPhone: '0901234567',
          status: 'PENDING_PAYMENT',
          holdExpiresAt: new Date(Date.now() + 600000).toISOString()
        }
      })
    })
  })

  // Mock customer bookings
  await page.route('**/api/v1/bookings?*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 'OK',
        data: {
          items: [
            {
              id: 1,
              bookingCode: 'VB-BK-20261128-001',
              tripCode: 'SG-DL-0800',
              operatorName: 'Phương Trang FUTA Bus Lines',
              departureStation: 'Bến xe Miền Đông Mới',
              arrivalStation: 'Bến xe Liên Tỉnh Đà Lạt',
              departureTime: '2026-12-01T08:00:00',
              seatNumbers: ['A01'],
              totalAmount: 384000,
              status: 'CONFIRMED',
              qrSignature: 'HMAC_SHA256_SYNTHETIC_VERIFIED_SIGNATURE'
            }
          ],
          totalElements: 1,
          totalPages: 1,
          page: 0,
          size: 10
        }
      })
    })
  })

  // Mock admin payment report
  await page.route('**/api/v1/admin/payments/reports*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 'OK',
        data: {
          grossRevenue: 850000000,
          netRevenue: 85000000,
          successCount: 1420,
          failedCount: 15,
          pendingCount: 22,
          refundCount: 8,
          reconciliationCount: 5,
          commissionRate: 10
        }
      })
    })
  })

  // Mock admin payments list
  await page.route('**/api/v1/admin/payments?*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 'OK',
        data: {
          items: [
            {
              id: 1,
              txnRef: 'VNP_20261128_001',
              bookingCode: 'VB-BK-001',
              amount: 384000,
              vnpayStatus: '00',
              internalStatus: 'CONFIRMED',
              discrepancyType: 'NONE',
              createdAt: '2026-11-28T10:00:00'
            }
          ],
          totalElements: 1,
          totalPages: 1,
          page: 0,
          size: 10
        }
      })
    })
  })

  // Mock admin withdrawals
  await page.route('**/api/v1/admin/partners/withdrawals*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 'OK',
        data: [
          {
            id: 1,
            operatorId: 10,
            operatorName: 'Phương Trang FUTA Bus Lines',
            bankName: 'Vietcombank',
            bankAccountNumber: '0071001234567',
            bankAccountHolder: 'CONG TY CP XE KHACH PHUONG TRANG',
            amount: 50000000,
            status: 'PENDING',
            createdAt: '2026-11-28T09:30:00'
          }
        ]
      })
    })
  })

  // Mock operator wallet
  await page.route('**/api/v1/partners/wallet*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 'OK',
        data: {
          id: 1,
          operatorId: 10,
          availableBalance: 42500000,
          pendingSettlementBalance: 18700000,
          lockedBalance: 5200000,
          debtBalance: 0,
          currency: 'VND'
        }
      })
    })
  })

  // Mock operator trips
  await page.route('**/api/v1/partners/trips*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 'OK',
        data: {
          items: [
            {
              id: 101,
              tripCode: 'SG-DL-0800',
              vehicleId: 1,
              departureStationId: 1,
              arrivalStationId: 2,
              departureTime: '2026-12-01T08:00:00',
              arrivalTime: '2026-12-01T14:30:00',
              basePrice: 320000,
              status: 'SCHEDULED'
            }
          ],
          totalElements: 1,
          totalPages: 1,
          page: 0,
          size: 10
        }
      })
    })
  })

  // Mock operator vehicles
  await page.route('**/api/v1/partners/vehicles*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        { id: 1, plateNumber: '51B-888.99', modelName: 'Thaco Mobihome VIP', vehicleType: 'SLEEPER', totalSeats: 36, status: 'ACTIVE' }
      ])
    })
  })

  // Mock operator drivers
  await page.route('**/api/v1/partners/drivers*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        { id: 1, fullName: 'Trần Văn Đức', phone: '0987654321', licenseNumber: 'GPLX-790123456', status: 'ACTIVE' }
      ])
    })
  })
}

async function loginAs(page, role, username) {
  const token = makeE2eJwt(role, username)
  const user = { id: 1, username, role }
  await page.evaluate(({ token, user }) => {
    localStorage.setItem('velobus_access_token', token)
    localStorage.setItem('velobus_user', JSON.stringify(user))
  }, { token, user })
}

async function setDarkMode(page, enabled) {
  await page.evaluate((isDark) => {
    if (isDark) {
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.classList.remove('dark')
    }
  }, enabled)
  await page.waitForTimeout(300)
}

async function main() {
  console.log('Starting screenshot capture against ' + BASE_URL)
  const browser = await chromium.launch({ headless: true })

  // 1. Desktop Context (1440x900)
  const desktopCtx = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const desktopPage = await desktopCtx.newPage()
  await setupSyntheticMocks(desktopPage)

  // 1.1 Login Light & Dark
  await desktopPage.goto(BASE_URL + '/login')
  await desktopPage.waitForLoadState('networkidle')
  await setDarkMode(desktopPage, false)
  await desktopPage.screenshot({ path: path.join(TARGET_DIR, '01-login-desktop-light.png') })
  await setDarkMode(desktopPage, true)
  await desktopPage.screenshot({ path: path.join(TARGET_DIR, '02-login-desktop-dark.png') })
  await setDarkMode(desktopPage, false)

  // 1.2 Customer Home
  await desktopPage.goto(BASE_URL + '/')
  await desktopPage.waitForLoadState('networkidle')
  await desktopPage.screenshot({ path: path.join(TARGET_DIR, '03-customer-home-light.png') })
  await setDarkMode(desktopPage, true)
  await desktopPage.screenshot({ path: path.join(TARGET_DIR, '04-customer-home-dark.png') })
  await setDarkMode(desktopPage, false)

  // 1.3 Trip Search Results
  await desktopPage.goto(BASE_URL + '/search?dep=1&arr=2&date=2026-12-01')
  await desktopPage.waitForLoadState('networkidle')
  await desktopPage.screenshot({ path: path.join(TARGET_DIR, '05-trip-search-results.png') })

  // 1.4 Seat Selection
  await desktopPage.goto(BASE_URL + '/trips/1/seats')
  await desktopPage.waitForLoadState('networkidle')
  await desktopPage.screenshot({ path: path.join(TARGET_DIR, '06-seat-selection-view.png') })

  // 1.5 Checkout View
  await loginAs(desktopPage, 'ROLE_CUSTOMER', 'customer1')
  await desktopPage.goto(BASE_URL + '/checkout/1')
  await desktopPage.waitForLoadState('networkidle')
  await desktopPage.screenshot({ path: path.join(TARGET_DIR, '07-checkout-payment-view.png') })

  // 1.6 My Bookings
  await desktopPage.goto(BASE_URL + '/my-bookings')
  await desktopPage.waitForLoadState('networkidle')
  await desktopPage.screenshot({ path: path.join(TARGET_DIR, '08-my-bookings-qr-ticket.png') })

  // 1.7 Admin Dashboard
  await loginAs(desktopPage, 'ROLE_ADMIN', 'admin')
  await desktopPage.goto(BASE_URL + '/admin')
  await desktopPage.waitForLoadState('networkidle')
  await desktopPage.screenshot({ path: path.join(TARGET_DIR, '09-admin-dashboard-light.png') })
  await setDarkMode(desktopPage, true)
  await desktopPage.screenshot({ path: path.join(TARGET_DIR, '10-admin-dashboard-dark.png') })
  await setDarkMode(desktopPage, false)

  // 1.8 Admin Withdrawals
  await desktopPage.goto(BASE_URL + '/admin/withdrawals')
  await desktopPage.waitForLoadState('networkidle')
  await desktopPage.screenshot({ path: path.join(TARGET_DIR, '11-admin-withdrawals-escrow.png') })

  // 1.9 Operator Dashboard
  await loginAs(desktopPage, 'ROLE_OPERATOR', 'operator1')
  await desktopPage.goto(BASE_URL + '/operator')
  await desktopPage.waitForLoadState('networkidle')
  await desktopPage.screenshot({ path: path.join(TARGET_DIR, '12-operator-dashboard-light.png') })
  await setDarkMode(desktopPage, true)
  await desktopPage.screenshot({ path: path.join(TARGET_DIR, '13-operator-dashboard-dark.png') })
  await setDarkMode(desktopPage, false)

  // 1.10 Operator Trips Scheduling
  await desktopPage.goto(BASE_URL + '/operator/trips')
  await desktopPage.waitForLoadState('networkidle')
  await desktopPage.screenshot({ path: path.join(TARGET_DIR, '14-operator-trips-scheduling.png') })

  await desktopCtx.close()

  // 2. Mobile Context (375x812 - iPhone 13/14/15)
  const mobileCtx = await browser.newContext({ viewport: { width: 375, height: 812 } })
  const mobilePage = await mobileCtx.newPage()
  await setupSyntheticMocks(mobilePage)

  await mobilePage.goto(BASE_URL + '/')
  await mobilePage.waitForLoadState('networkidle')
  await mobilePage.screenshot({ path: path.join(TARGET_DIR, '15-mobile-customer-home-375.png') })

  await loginAs(mobilePage, 'ROLE_ADMIN', 'admin')
  await mobilePage.goto(BASE_URL + '/admin')
  await mobilePage.waitForLoadState('networkidle')
  await mobilePage.screenshot({ path: path.join(TARGET_DIR, '16-mobile-admin-dashboard-375.png') })

  await mobileCtx.close()
  await browser.close()
  console.log('Successfully captured all 16 release screenshots!')
}

main().catch((err) => {
  console.error('Screenshot capture failed:', err)
  process.exit(1)
})
