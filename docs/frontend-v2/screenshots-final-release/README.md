# VeloBus — Final Release Candidate Screenshots (v1.0.0-rc.1)

Tập hợp bằng chứng thị giác đại diện (Representative Visual Evidence) cho bản phát hành Release Candidate, được chụp từ production build chính thức (`npm run build && vite preview`) trên nền tảng Claude+ Warm Minimalist Design System (Reka UI, Tailwind CSS, TanStack Table, Lucide).

Tất cả dữ liệu được hiển thị là dữ liệu giả lập chuẩn hóa (Synthetic Canonical Data), không chứa thông tin cá nhân thực hoặc tài khoản ngân hàng nhạy cảm.

---

## 1. Xác thực & Đăng nhập (Authentication)
| Giao diện sáng (Light Mode - 1440x900) | Giao diện tối (Dark Mode - 1440x900) |
|---|---|
| ![Login Light](./01-login-desktop-light.png) | ![Login Dark](./02-login-desktop-dark.png) |

---

## 2. Phân Hệ Khách Hàng (Customer Booking Journey)
### 2.1 Trang chủ & Form Tra cứu (Home)
| Giao diện sáng (Light Mode) | Giao diện tối (Dark Mode) |
|---|---|
| ![Home Light](./03-customer-home-light.png) | ![Home Dark](./04-customer-home-dark.png) |

### 2.2 Danh sách Chuyến đi & Sơ đồ Ghế (Search & Seat Selection)
| Kết quả Tra cứu Chuyến xe | Sơ đồ Chọn Ghế 2 Tầng |
|---|---|
| ![Trips Search](./05-trip-search-results.png) | ![Seat Selection](./06-seat-selection-view.png) |

### 2.3 Thanh toán & Vé Điện Tử QR Code (Checkout & Bookings)
| Màn hình Checkout VNPay | Lịch sử Vé & Vé Điện Tử QR HMAC-SHA256 |
|---|---|
| ![Checkout](./07-checkout-payment-view.png) | ![My Bookings](./08-my-bookings-qr-ticket.png) |

---

## 3. Phân Hệ Ban Quản Trị (Admin Backoffice)
### 3.1 Dashboard & Báo cáo Tài chính GMV
| Admin Dashboard (Light Mode) | Admin Dashboard (Dark Mode) |
|---|---|
| ![Admin Light](./09-admin-dashboard-light.png) | ![Admin Dark](./10-admin-dashboard-dark.png) |

### 3.2 Quản lý Giải ngân Ví Escrow (Withdrawals Payout)
| Danh sách Lệnh Rút Tiền & Chốt Chặn Fail-Closed |
|---|
| ![Admin Withdrawals](./11-admin-withdrawals-escrow.png) |

---

## 4. Phân Hệ Đối Tác Nhà Xe (Operator Portal)
### 4.1 Dashboard Ví Ký Quỹ & Doanh thu
| Operator Dashboard (Light Mode) | Operator Dashboard (Dark Mode) |
|---|---|
| ![Operator Light](./12-operator-dashboard-light.png) | ![Operator Dark](./13-operator-dashboard-dark.png) |

### 4.2 Lập Lịch Chuyến Xe & Chốt Chặn Turnaround Buffer 30 Phút
| Lập lịch Chuyến xe & Điều độ Vận hành |
|---|
| ![Operator Trips](./14-operator-trips-scheduling.png) |

---

## 5. Trải Nghiệm Thiết Bị Di Động (Mobile Responsive - 375x812)
| Trang chủ Khách hàng (Mobile) | Admin Dashboard (Mobile) |
|---|---|
| ![Mobile Home](./15-mobile-customer-home-375.png) | ![Mobile Admin](./16-mobile-admin-dashboard-375.png) |
