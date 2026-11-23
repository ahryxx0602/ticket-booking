# 🖥️ VeloBus Frontend — Premium Mobility SaaS Interface

<p align="center">
  🌐 <b><a href="README.md">English</a></b> | <b><a href="README.vi.md">Tiếng Việt</a></b>
</p>

[![Vue 3](https://img.shields.io/badge/Vue-3.5.13-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white)](https://vuejs.org/)
[![Vite](https://img.shields.io/badge/Vite-6.0.5-646CFF?style=for-the-badge&logo=vite&logoColor=white)](https://vitejs.dev/)
[![PrimeVue 4](https://img.shields.io/badge/PrimeVue-4.2.5-41B883?style=for-the-badge&logo=primevue&logoColor=white)](https://primevue.org/)
[![Tailwind CSS](https://img.shields.io/badge/TailwindCSS-3.4.17-38B2AC?style=for-the-badge&logo=tailwind-css&logoColor=white)](https://tailwindcss.com/)
[![Pinia](https://img.shields.io/badge/Pinia-2.3.0-FFE57F?style=for-the-badge&logo=pinia&logoColor=black)](https://pinia.vuejs.org/)
[![Playwright](https://img.shields.io/badge/Playwright-E2E%20Tests%20(21%20Passed)-2EAD33?style=for-the-badge&logo=playwright&logoColor=white)](https://playwright.dev/)
[![Vitest](https://img.shields.io/badge/Vitest-Unit%20Tests%20(29%20Passed)-729B1B?style=for-the-badge&logo=vitest&logoColor=white)](https://vitest.dev/)

A modern, high-performance web frontend for the **VeloBus** multi-tenant bus ticketing and logistics platform. Built on **Vue 3 (Composition API)**, **PrimeVue 4**, and **Tailwind CSS**, the interface translates rigorous enterprise business rules into an ultra-clean **Kinetic Executive** design system designed in collaboration with **Google Stitch**.

---

## 🎨 UI/UX Design System & Google Stitch Cloud Integration

The interface implements the **Kinetic Executive** design system, engineered for clarity, readability, and data-dense mobility operations:
- **Brand Navy (`#142B4A`):** Core brand anchors, Admin/Operator navigation sidebars, and high-impact structural panels.
- **Primary Blue (`#2563EB`):** Main interactive CTAs, seat selection active states, and checkout progressions.
- **Accent Amber (`#F59E0B`):** Urgency states, boarding warnings, and turnaround buffer indicators.
- **Canvas (`#F8FAFC`):** Glare-free, calm surface background.
- **Typography:** Inter with strict OpenType `font-variant-numeric: tabular-nums` for currency, countdown timers, and schedule matrices.

### Official Google Stitch Cloud Project
All core screens have been generated and synchronized with **Google Stitch Cloud**:
- **Project URL:** [https://stitch.withgoogle.com/projects/7031998889146609699](https://stitch.withgoogle.com/projects/7031998889146609699)
- **Design System Asset:** `assets/a83bf80db97045319502dd4fa3a461f3` (*Kinetic Executive*)
- **Generated High-Resolution Screens:**
  1. `d5b0031050ff4ae4a8382fbfca348063`: *VeloBus - Chọn chỗ & Đặt vé* (2-Deck Seat Map, 10m countdown, VNPay summary)
  2. `74bd918ef9e741dd9624de8f1b7165e8`: *Fintech Escrow Dashboard & Payout Management* (4 balances, Vietcombank KYC, ledger)
  3. `4e909110885f4aa4b302b413417bcfa7`: *Platform Governance & VNPay Reconciliation Dashboard* (IPN discrepancy alerts, 2-tier commission)
  4. `a1c763d98f854dc4a5859dbcac5698b1`: *Fleet & Trip Schedule Management* (30m turnaround buffer, conflict detection drawer)
  5. `0c0c51cfabda47c08fb8b230245acd33`: *Customer Digital Boarding Pass & E-Ticket* (HMAC-SHA256 encrypted QR Code)

---

## 🏛️ Feature-Sliced Modular Structure

```
frontend/src/
├── app/
│   ├── layouts/
│   │   ├── CustomerLayout.vue             # Header 72px sticky, max-w container, Navy footer
│   │   ├── OperatorLayout.vue             # Partner Hub shell (Navy sidebar 256px / 76px)
│   │   ├── AdminLayout.vue                # Governance shell with emergency action triggers
│   │   └── AuthLayout.vue                 # Split-screen 45% Navy / 55% Form
│   └── router/index.js                    # Vue Router 4 route catalog
├── modules/
│   ├── auth/                              # Login, Register, Profile, Telegram link
│   ├── catalog-booking/                   # Search, 2-Deck SeatMap, Checkout, E-Tickets
│   ├── partner/                           # Escrow Wallet, Fleet & Drivers, Trips, Profile
│   └── admin/                             # Platform KPI, Reconciliation, Operators, Users
└── shared/
    ├── api/httpClient.js                  # Axios instance with Bearer token interceptor
    ├── components/                        # SeatMap2Deck, FirebaseUploader, StatusBadge, etc.
    ├── composables/                       # useCountdown, useLayoutWidth
    └── stores/notificationStore.js        # Real-time notifications & Telegram linking
```

---

## 🚀 Key Portal Features

### 1. Customer Portal (`CustomerLayout`)
- **Interactive 2-Deck Seat Map:** Real-time visual representation of Lower Deck (`Tầng 1 - A01..A20`) and Upper Deck (`Tầng 2 - B01..B20`), 5 status colors, and zero-shift tabular pricing.
- **10-Minute Hold Countdown:** Client-side synchronized timer (`useCountdown.js`) with visual amber warnings under 2 minutes.
- **Digital Boarding Pass:** Perforated airline-style ticket with an `HMAC-SHA256` encrypted verification QR Code.

### 2. Operator Workspace (`OperatorLayout`)
- **4-Balance Escrow Ledger:** Instant tracking of `AVAILABLE`, `HOLDING_ESCROW`, `SETTLING`, and `WITHDRAWN` funds.
- **Fleet & Driver Roster:** Right-side Slide-over Drawer (520px) for rapid data entry and Firebase document upload.
- **Turnaround Buffer Validation:** Automatic frontend warning preventing trip creation if vehicle turnaround buffer (< 30m) is violated.

### 3. Admin Backoffice (`AdminLayout`)
- **VNPay IPN Reconciliation Discrepancy Alerts:** Prominent visual alerts for mismatch between gateway records and platform escrow.
- **Two-Tier Commission Setting:** Global platform default rate and individual operator contract overrides (5.0% vs 7.0%).
- **Invariant `ADMIN_CANNOT_LOCK_SELF`:** Safeguard disabling administrative self-demotion or account lockouts.

---

## 🧪 Testing Suite & Quality Verification

### Unit & Composable Tests (Vitest)
```bash
npm run test:unit
# Output: 29/29 tests passed across 6 test suites (1.03s)
```

### End-to-End & Visual Regression Tests (Playwright)
```bash
npm run test:e2e
# Output: 21/21 tests passed across Chromium, Mobile (375x667), Tablet (768x1024), Desktop (1440x900)
```

---

## 📦 Containerization & Production Build

The frontend is packaged using a multi-stage **Dockerfile** yielding an ultra-lightweight Nginx container (< 25MB):
- **Stage 1 (Builder):** Node 22 Alpine executes `npm ci && npm run build` (build completes in ~2.5s).
- **Stage 2 (Runtime):** Nginx 1.27 Alpine with Gzip compression, SPA fallback routing, and `/api/` reverse proxy pass.

```bash
# Build production bundle locally
npm run build

# Start preview server
npm run preview -- --port 4173
```
