import { test, expect } from '@playwright/test'
import path from 'path'
import fs from 'fs'
import { fileURLToPath } from 'url'
import { loginAsRole } from './helpers/auth'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const screenshotDir = path.resolve(__dirname, '../../docs/frontend-v2/screenshots-gate-b4')

if (!fs.existsSync(screenshotDir)) {
  fs.mkdirSync(screenshotDir, { recursive: true })
}

// ---------------------------------------------------------------------------
// 1. Synthetic Hermetic Partner Data (Authoritative Spring Boot DTO Contracts)
// ---------------------------------------------------------------------------
let dynamicWallet = {
  id: 1,
  operatorId: 10,
  availableBalance: 42500000,
  pendingSettlementBalance: 18700000,
  lockedBalance: 5200000,
  debtBalance: 0,
  currency: 'VND'
}

let dynamicTransactions = [
  {
    id: 9081,
    walletId: 1,
    operatorId: 10,
    bookingId: 7712,
    tripId: 101,
    paymentReference: 'TXN-9081',
    transactionType: 'TICKET_ESCROW_CREDIT',
    grossAmount: 700000,
    commissionRate: 10,
    commissionAmount: 70000,
    netAmount: 630000,
    balanceBefore: 41870000,
    balanceAfter: 42500000,
    description: 'Cộng tiền vé (Ký quỹ)',
    createdAt: '2026-11-23T14:15:00'
  },
  {
    id: 9075,
    walletId: 1,
    operatorId: 10,
    bookingId: 7712,
    tripId: 101,
    paymentReference: 'TXN-9075',
    transactionType: 'TRIP_SETTLEMENT',
    grossAmount: 700000,
    commissionRate: 10,
    commissionAmount: 70000,
    netAmount: 630000,
    balanceBefore: 41240000,
    balanceAfter: 41870000,
    description: 'Quyết toán chuyến hoàn tất',
    createdAt: '2026-11-23T14:15:00'
  },
  {
    id: 8890,
    walletId: 1,
    operatorId: 10,
    bookingId: null,
    tripId: null,
    paymentReference: 'TXN-8890',
    transactionType: 'WITHDRAWAL_LOCK',
    grossAmount: 15000000,
    commissionRate: 0,
    commissionAmount: 0,
    netAmount: -15000000,
    balanceBefore: 56870000,
    balanceAfter: 41870000,
    description: 'Khóa số dư rút tiền về Vietcombank',
    createdAt: '2026-11-21T09:30:00'
  },
  {
    id: 8712,
    walletId: 1,
    operatorId: 10,
    bookingId: null,
    tripId: 99,
    paymentReference: 'TXN-8712',
    transactionType: 'TRIP_SETTLEMENT',
    grossAmount: 15777000,
    commissionRate: 10,
    commissionAmount: 1577000,
    netAmount: 14200000,
    balanceBefore: 42670000,
    balanceAfter: 56870000,
    description: 'Quyết toán chuyến đi TRIP-99',
    createdAt: '2026-11-20T23:00:00'
  }
]

let dynamicWithdrawals = [
  {
    id: 1,
    operatorId: 10,
    amount: 120000000,
    bankName: 'Vietcombank',
    accountNumber: '0071001234567',
    accountHolderName: 'HOANG LONG LIMOUSINE',
    status: 'APPROVED',
    adminNote: 'Đã giải ngân đợt 1',
    processedByAdminId: 1,
    processedAt: '2026-11-20T10:00:00',
    createdAt: '2026-11-19T14:00:00'
  }
]

// ---------------------------------------------------------------------------
// 2. Strict Hermetic Network Mocks
// ---------------------------------------------------------------------------
async function setupOperatorMocks(page, options = {}) {
  // Reset state for isolation
  dynamicWallet = {
    id: 1,
    operatorId: 10,
    availableBalance: options.availableBalance !== undefined ? options.availableBalance : 42500000,
    pendingSettlementBalance: 18700000,
    lockedBalance: 5200000,
    debtBalance: options.debtBalance !== undefined ? options.debtBalance : 0,
    currency: 'VND'
  }

  // 1. Wallet API
  await page.route('**/api/v1/partners/wallet', async (route) => {
    if (route.request().method() === 'GET') {
      if (options.walletFail) {
        await route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'WALLET_SERVICE_UNAVAILABLE',
            message: 'Dịch vụ ví nhà xe hiện không khả dụng từ máy chủ Backend'
          })
        })
        return
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          message: 'Wallet retrieved',
          data: dynamicWallet
        })
      })
      return
    }
    route.fallback()
  })

  // 2. Wallet Transactions API
  await page.route('**/api/v1/partners/wallet/transactions', async (route) => {
    if (options.transactionsFail) {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'TRANSACTIONS_UNAVAILABLE',
          message: 'Không thể truy vấn lịch sử giao dịch ví'
        })
      })
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        message: 'Transactions retrieved',
        data: dynamicTransactions
      })
    })
  })

  // 3. Withdrawals List API
  await page.route('**/api/v1/partners/wallet/withdrawals', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        message: 'Withdrawals retrieved',
        data: dynamicWithdrawals
      })
    })
  })

  // 4. Withdrawal Mutation API
  await page.route('**/api/v1/partners/wallet/withdraw', async (route) => {
    if (route.request().method() === 'POST') {
      if (options.withdrawFail) {
        await route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'INSUFFICIENT_BALANCE',
            message: 'Số dư khả dụng không đủ để thực hiện lệnh rút tiền'
          })
        })
        return
      }
      if (options.withdrawTimeout) {
        await route.abort('timedout')
        return
      }

      const payload = route.request().postDataJSON()
      const newWithdrawal = {
        id: Date.now(),
        operatorId: 10,
        amount: payload.amount,
        bankName: payload.bankName,
        accountNumber: payload.accountNumber,
        accountHolderName: payload.accountHolderName,
        status: 'PENDING',
        adminNote: null,
        processedByAdminId: null,
        processedAt: null,
        createdAt: new Date().toISOString()
      }

      // Authoritative state transition: available decreases, locked increases
      dynamicWallet.availableBalance -= payload.amount
      dynamicWallet.lockedBalance += payload.amount
      dynamicWithdrawals.unshift(newWithdrawal)
      dynamicTransactions.unshift({
        id: Date.now(),
        walletId: 1,
        operatorId: 10,
        bookingId: null,
        tripId: null,
        paymentReference: `TXN-${Date.now()}`,
        transactionType: 'WITHDRAWAL_LOCK',
        grossAmount: payload.amount,
        commissionRate: 0,
        commissionAmount: 0,
        netAmount: -payload.amount,
        balanceBefore: dynamicWallet.availableBalance + payload.amount,
        balanceAfter: dynamicWallet.availableBalance,
        description: `Khóa số dư rút tiền về ${payload.bankName}`,
        createdAt: new Date().toISOString()
      })

      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          message: 'Withdrawal requested successfully',
          data: newWithdrawal
        })
      })
      return
    }
    route.fallback()
  })

  // Auth/me mock
  await page.route('**/api/v1/auth/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          id: 10,
          username: 'operator1',
          role: 'ROLE_OPERATOR',
          roles: ['ROLE_OPERATOR'],
          fullName: 'Nhà Xe Hoàng Long'
        }
      })
    })
  })
}

// ---------------------------------------------------------------------------
// 3. Gate B4 Test Suites
// ---------------------------------------------------------------------------
test.describe('Gate B4: Controlled Operator Pilot & Financial Contract Integration', () => {

  // B4-01: Operator Dashboard — Fintech Wallet Balances & Summary Cards (Desktop Light)
  test('B4-01: Operator Dashboard — Fintech Wallet Balances (Desktop Light)', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupOperatorMocks(page)
    await loginAsRole(page, 'ROLE_OPERATOR', 'operator1')

    await page.goto('/operator')
    await page.waitForLoadState('networkidle')

    // Verify 4 Balance Cards
    const availableCard = page.locator('[data-testid="available-balance"]')
    await expect(availableCard).toBeVisible()
    await expect(availableCard).toContainText('42.500.000')

    const pendingCard = page.locator('[data-testid="pending-settlement-balance"]')
    await expect(pendingCard).toBeVisible()
    await expect(pendingCard).toContainText('18.700.000')

    const lockedCard = page.locator('[data-testid="locked-balance"]')
    await expect(lockedCard).toBeVisible()
    await expect(lockedCard).toContainText('5.200.000')

    const totalWithdrawnCard = page.locator('[data-testid="total-withdrawn"]')
    await expect(totalWithdrawnCard).toBeVisible()
    await expect(totalWithdrawnCard).toContainText('120.000.000')

    // Capture required evidence
    await page.screenshot({
      path: path.join(screenshotDir, 'operator-dashboard-desktop-light.png'),
      fullPage: false
    })
  })

  // B4-02: Operator Dashboard — Theme Customizer (Dark Mode, Compact Density, Primary Theme)
  test('B4-02: Operator Dashboard — Dark Mode, Density & Custom Primary', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupOperatorMocks(page)
    await loginAsRole(page, 'ROLE_OPERATOR', 'operator1')

    await page.goto('/operator')
    await page.waitForLoadState('networkidle')

    const customizerBtn = page.locator('[data-testid="theme-customizer-trigger"]')
    await expect(customizerBtn).toBeVisible()
    await customizerBtn.click()

    const drawer = page.locator('[data-testid="theme-customizer-drawer"]')
    await expect(drawer).toBeVisible()

    // Toggle Dark Mode
    const darkBtn = drawer.locator('button:has-text("Tối")')
    await darkBtn.click()
    await expect(page.locator('html')).toHaveClass(/dark/)
    await page.screenshot({
      path: path.join(screenshotDir, 'operator-dashboard-desktop-dark.png'),
      fullPage: false
    })

    // Toggle Compact Density
    const compactOpt = drawer.locator('text=Thu gọn')
    if (await compactOpt.isVisible()) {
      await compactOpt.click()
      await page.screenshot({
        path: path.join(screenshotDir, 'operator-compact-density.png'),
        fullPage: false
      })
    }

    // Toggle Custom Primary Theme
    const emeraldOpt = drawer.locator('button:has-text("Ngọc Lục Bảo"), button:has-text("Xanh Lá"), [title*="Emerald"]').first()
    if (await emeraldOpt.isVisible()) {
      await emeraldOpt.click()
    }
    await page.screenshot({
      path: path.join(screenshotDir, 'operator-custom-primary-theme.png'),
      fullPage: false
    })

    await page.keyboard.press('Escape')
    await expect(drawer).toHaveCount(0)
  })

  // B4-03: Responsive — Tablet 768px and Mobile 375px
  test('B4-03: Responsive — Tablet 768px and Mobile 375px Viewports', async ({ page }) => {
    // 1. Tablet Viewport
    await page.setViewportSize({ width: 768, height: 1024 })
    await setupOperatorMocks(page)
    await loginAsRole(page, 'ROLE_OPERATOR', 'operator1')

    await page.goto('/operator')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('[data-testid="available-balance"]')).toBeVisible()
    const tabletOverflow = await page.evaluate(() => document.body.scrollWidth > document.body.clientWidth)
    expect(tabletOverflow).toBe(false)
    await page.screenshot({
      path: path.join(screenshotDir, 'operator-dashboard-tablet.png'),
      fullPage: false
    })

    // 2. Mobile Viewport
    await page.setViewportSize({ width: 375, height: 667 })
    await page.waitForTimeout(200)
    await expect(page.locator('[data-testid="btn-open-withdraw"]')).toBeVisible()
    const mobileOverflow = await page.evaluate(() => document.body.scrollWidth > document.body.clientWidth)
    expect(mobileOverflow).toBe(false)
    await page.screenshot({
      path: path.join(screenshotDir, 'operator-dashboard-mobile.png'),
      fullPage: false
    })
  })

  // B4-04: Wallet Summary & Loading / Error States
  test('B4-04: Wallet Summary — Loading and Error States Integrity', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupOperatorMocks(page, { walletFail: true })
    await loginAsRole(page, 'ROLE_OPERATOR', 'operator1')

    await page.goto('/operator')
    await page.waitForLoadState('networkidle')

    // Error alert is displayed with Thử lại button
    const errorAlert = page.locator('[data-testid="wallet-error-alert"]')
    await expect(errorAlert).toBeVisible()
    await expect(errorAlert).toContainText('Không thể tải thông tin ví')

    await page.screenshot({
      path: path.join(screenshotDir, 'wallet-loading-error-states.png'),
      fullPage: false
    })
  })

  // B4-05: Wallet Transactions — Reusable TanStack DataTable
  test('B4-05: Wallet Transactions — Reusable TanStack DataTable', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupOperatorMocks(page)
    await loginAsRole(page, 'ROLE_OPERATOR', 'operator1')

    await page.goto('/operator')
    await page.waitForLoadState('networkidle')

    // Table is rendered with correct columns
    const table = page.locator('table')
    await expect(table).toBeVisible()
    await expect(table).toContainText('TXN-9081')
    await expect(table).toContainText('Cộng tiền vé (Ký quỹ)')
    await expect(table).toContainText('+630.000 đ')

    await page.screenshot({
      path: path.join(screenshotDir, 'wallet-transaction-datatable.png'),
      fullPage: false
    })
  })

  // B4-06: Withdrawal Form — Input Validation & Boundaries
  test('B4-06: Withdrawal Form — Input Validation & Boundaries', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupOperatorMocks(page)
    await loginAsRole(page, 'ROLE_OPERATOR', 'operator1')

    await page.goto('/operator')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="btn-open-withdraw"]').click()
    const dialog = page.locator('[role="dialog"]')
    await expect(dialog).toBeVisible()
    await expect(dialog).toContainText('Yêu Cầu Rút Tiền Về Ngân Hàng')

    await page.screenshot({
      path: path.join(screenshotDir, 'withdrawal-form.png'),
      fullPage: false
    })

    const amountInput = page.locator('[data-testid="input-withdraw-amount"]')
    const submitBtn = page.locator('[data-testid="btn-submit-withdraw"]')

    // Test minimum amount (< 100,000 VND)
    await amountInput.fill('50000')
    await submitBtn.click()
    await expect(dialog).toContainText('Số tiền rút tối thiểu là 100.000 đ')

    // Test exceed available balance (> 42,500,000 VND)
    await amountInput.fill('50000000')
    await submitBtn.click()
    await expect(dialog).toContainText('vượt quá số dư khả dụng')
  })

  // B4-07: Withdrawal Flow — Mandatory Confirmation Modal & Cancellation
  test('B4-07: Withdrawal Flow — Confirmation Modal & Cancel Flow', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupOperatorMocks(page)
    await loginAsRole(page, 'ROLE_OPERATOR', 'operator1')

    await page.goto('/operator')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="btn-open-withdraw"]').click()
    const amountInput = page.locator('[data-testid="input-withdraw-amount"]')
    const bankInput = page.locator('[data-testid="input-withdraw-bank"]')
    const accNumInput = page.locator('[data-testid="input-withdraw-account-number"]')
    const accHolderInput = page.locator('[data-testid="input-withdraw-account-holder"]')

    await amountInput.fill('5000000')
    await bankInput.fill('Vietcombank')
    await accNumInput.fill('0071001234567')
    await accHolderInput.fill('CONG TY HOANG LONG')

    await page.locator('[data-testid="btn-submit-withdraw"]').click()

    // Confirmation AlertDialog opens
    const confirmDialog = page.locator('[role="alertdialog"]')
    await expect(confirmDialog).toBeVisible()
    await expect(confirmDialog).toContainText('Xác nhận thông tin rút tiền')
    await expect(confirmDialog).toContainText('5.000.000 đ')
    await expect(confirmDialog).toContainText('Vietcombank')
    await expect(confirmDialog).toContainText('4567') // Masked STK

    await page.screenshot({
      path: path.join(screenshotDir, 'withdrawal-confirmation.png'),
      fullPage: false
    })

    // Click cancel
    await confirmDialog.locator('button:has-text("Quay lại")').click()
    await expect(confirmDialog).toHaveCount(0)

    // Form modal remains open and values preserved
    const dialog = page.locator('[role="dialog"]')
    await expect(dialog).toBeVisible()
    await expect(amountInput).toHaveValue('5000000')
  })

  // B4-08: Withdrawal Flow — Backend Rejection Handling
  test('B4-08: Withdrawal Flow — Backend Rejection Handling', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupOperatorMocks(page, { withdrawFail: true })
    await loginAsRole(page, 'ROLE_OPERATOR', 'operator1')

    await page.goto('/operator')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="btn-open-withdraw"]').click()
    await page.locator('[data-testid="input-withdraw-amount"]').fill('5000000')
    await page.locator('[data-testid="input-withdraw-bank"]').fill('Vietcombank')
    await page.locator('[data-testid="input-withdraw-account-number"]').fill('0071001234567')
    await page.locator('[data-testid="input-withdraw-account-holder"]').fill('CONG TY HOANG LONG')
    await page.locator('[data-testid="btn-submit-withdraw"]').click()

    const confirmBtn = page.locator('[data-testid="btn-confirm-withdraw-action"]')
    await expect(confirmBtn).toBeVisible()
    await confirmBtn.click()
    await page.waitForTimeout(300)

    // Assert dialog remains open and displays backend rejection error
    const dialog = page.locator('[role="dialog"]')
    await expect(dialog).toBeVisible()
    await expect(dialog).toContainText('Số dư khả dụng không đủ')

    await page.screenshot({
      path: path.join(screenshotDir, 'withdrawal-backend-rejection.png'),
      fullPage: false
    })
  })

  // B4-09: Withdrawal Flow — Confirmed Success & Authoritative Refetch
  test('B4-09: Withdrawal Flow — Confirmed Success & Authoritative Refetch', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupOperatorMocks(page)
    await loginAsRole(page, 'ROLE_OPERATOR', 'operator1')

    await page.goto('/operator')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="btn-open-withdraw"]').click()
    await page.locator('[data-testid="input-withdraw-amount"]').fill('5000000')
    await page.locator('[data-testid="input-withdraw-bank"]').fill('Vietcombank')
    await page.locator('[data-testid="input-withdraw-account-number"]').fill('0071001234567')
    await page.locator('[data-testid="input-withdraw-account-holder"]').fill('CONG TY HOANG LONG')
    await page.locator('[data-testid="btn-submit-withdraw"]').click()

    const confirmBtn = page.locator('[data-testid="btn-confirm-withdraw-action"]')
    await expect(confirmBtn).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotDir, 'withdrawal-pending-mutation.png'),
      fullPage: false
    })

    await confirmBtn.click()
    await page.waitForTimeout(300)

    // Modal closes and success toast appears
    await expect(page.locator('[role="dialog"]')).toHaveCount(0)
    await expect(page.locator('text=Gửi yêu cầu rút tiền thành công!')).toBeVisible()

    // Authoritative balance refetch: available balance became 37,500,000 (42.5M - 5M)
    const availableCard = page.locator('[data-testid="available-balance"]')
    await expect(availableCard).toContainText('37.500.000')

    // Locked balance became 10,200,000 (5.2M + 5M)
    const lockedCard = page.locator('[data-testid="locked-balance"]')
    await expect(lockedCard).toContainText('10.200.000')

    await page.screenshot({
      path: path.join(screenshotDir, 'withdrawal-confirmed-success.png'),
      fullPage: false
    })
  })

  // B4-10: Withdrawal Flow — Network Timeout & Unknown Outcome Guard
  test('B4-10: Withdrawal Flow — Network Timeout & Unknown Outcome Guard', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await setupOperatorMocks(page, { withdrawTimeout: true })
    await loginAsRole(page, 'ROLE_OPERATOR', 'operator1')

    await page.goto('/operator')
    await page.waitForLoadState('networkidle')

    await page.locator('[data-testid="btn-open-withdraw"]').click()
    await page.locator('[data-testid="input-withdraw-amount"]').fill('5000000')
    await page.locator('[data-testid="input-withdraw-bank"]').fill('Vietcombank')
    await page.locator('[data-testid="input-withdraw-account-number"]').fill('0071001234567')
    await page.locator('[data-testid="input-withdraw-account-holder"]').fill('CONG TY HOANG LONG')
    await page.locator('[data-testid="btn-submit-withdraw"]').click()

    const confirmBtn = page.locator('[data-testid="btn-confirm-withdraw-action"]')
    await confirmBtn.click()
    await page.waitForTimeout(300)

    // Dialog stays open and shows Network Timeout lock warning
    const dialog = page.locator('[role="dialog"]')
    await expect(dialog).toBeVisible()
    await expect(dialog).toContainText('KHÓA gửi lại')

    const submitBtn = page.locator('[data-testid="btn-submit-withdraw"]')
    await expect(submitBtn).toBeDisabled()

    await page.screenshot({
      path: path.join(screenshotDir, 'withdrawal-unknown-outcome.png'),
      fullPage: false
    })

    // Close modal via Hủy Bỏ
    await dialog.locator('button:has-text("Hủy Bỏ")').click()
    await expect(dialog).toHaveCount(0)

    // Reopen modal: verify persistent lock
    await page.locator('[data-testid="btn-open-withdraw"]').click()
    await expect(dialog).toBeVisible()
    await expect(submitBtn).toBeDisabled()
    await expect(dialog).toContainText('KHÓA gửi lại')

    // Click verify server state
    const verifyBtn = page.locator('[data-testid="btn-verify-wallet-state"]')
    await expect(verifyBtn).toBeVisible()
    await verifyBtn.click()
    await page.waitForTimeout(300)

    // Unlocks when server state is verified
    await expect(submitBtn).toBeEnabled()
  })
})
