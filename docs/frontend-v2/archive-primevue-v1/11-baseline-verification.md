# 11 – Báo Cáo Kiểm Chứng Baseline Ban Đầu (Baseline Verification)

> **Mã tài liệu:** `FE-V2-VER-11`  
> **Thời điểm thực thi kiểm tra:** 25/11/2026 11:22:24 (Giờ địa phương)  
> **Môi trường kiểm tra:** Linux WSL2 (Kernel 5.15), Node.js v22.x, NPM v10.x.  
> **Baseline Commit:** `2659e1c` (HEAD -> main).

---

## 1. Kết Quả Chạy Kiểm Thử Đơn Vị (Vitest Unit Tests)

Lệnh thực thi:
```bash
cd frontend && npm run test:unit
```

- **Mã kết thúc (Exit Code):** `0` (SUCCESS)
- **Thời gian thực thi:** 1.52 giây
- **Tổng số tệp kiểm thử (Test Files):** **8 / 8 passed (100%)**
- **Tổng số ca kiểm thử (Tests):** **67 / 67 passed (100%)**

### Chi tiết các tệp kiểm thử:
| Tệp kiểm thử Unit Test | Số ca test | Thời gian | Trạng thái |
| :--- | :---: | :---: | :---: |
| `src/shared/composables/__tests__/composables.spec.js` | 5 | 9ms | **PASS** |
| `src/shared/components/__tests__/notification.spec.js` | 4 | 61ms | **PASS** |
| `src/shared/components/__tests__/components.spec.js` | 5 | 116ms | **PASS** |
| `src/modules/admin/__tests__/rbac-contract-parity.spec.js` | 6 | 25ms | **PASS** |
| `src/modules/catalog-booking/__tests__/booking-views.spec.js`| 5 | 100ms | **PASS** |
| `src/modules/partner/__tests__/operator-views.spec.js` | 5 | 132ms | **PASS** |
| `src/modules/admin/__tests__/admin-views.spec.js` | 9 | 148ms | **PASS** |
| `src/app/router/__tests__/navigation-guard.spec.js` | 28 | 185ms | **PASS** |

> **Cảnh báo ghi nhận:** Có cảnh báo `[Vue warn]: injection "Symbol(pinia)" not found` trong `admin-views.spec.js` do component test được mount độc lập không inject store Pinia mock đầy đủ. Cần bổ sung `createTestingPinia()` khi chuẩn hóa test V2.

---

## 2. Kết Quả Đóng Gói Ứng Dụng Sản Xuất (Vite Production Build)

Lệnh thực thi:
```bash
cd frontend && npm run build
```

- **Mã kết thúc (Exit Code):** `0` (SUCCESS)
- **Thời gian đóng gói:** 3.35 giây
- **Số lượng modules chuyển đổi:** 317 modules

### Phân tích dung lượng Bundle Chunks:
| Tên tệp đầu ra (Dist Asset) | Kích thước thô | Kích thước nén (Gzip) | Đánh giá |
| :--- | :---: | :---: | :--- |
| `dist/index.html` | 0.76 kB | 0.47 kB | Tối ưu |
| `dist/assets/index-19fQQsY9.css` | 56.30 kB | 10.99 kB | Rất nhẹ nhờ Tailwind Tree-shaking |
| `dist/assets/index-Dn505jpV.js` (Vendor Core) | 468.92 kB | 137.27 kB | Bình thường cho Vue + PrimeVue + Pinia |
| `dist/assets/AdminUsersBookingsView-fdHaz7g9.js` | **49.68 kB** | **13.02 kB** | **Quá lớn đối với 1 View duy nhất** |
| `dist/assets/SeatSelectionView-27zrFWlO.js` | 24.04 kB | 7.30 kB | Trung bình |
| `dist/assets/primeicons-Dr5RGzOO.svg` | **342.53 kB** | **105.26 kB** | **Lãng phí dung lượng** (Nên thay bằng Lucide) |

---

## 3. Kết Quả Kiểm Thử Đầu Cuối (Playwright E2E & Visual Tests)

Lệnh thực thi:
```bash
cd frontend && npm run test:e2e
```

- **Mã kết thúc (Exit Code):** `0` (SUCCESS)
- **Thời gian thực thi:** 7.6 giây (6 parallel workers)
- **Tổng số ca kiểm thử:** **22 / 22 passed (100%)**
  - Luồng Quản trị sàn (Admin Backoffice): 3/3 passed.
  - Luồng Khách hàng (Customer Booking): 5/5 passed.
  - Luồng Nhà xe (Operator Portal): 3/3 passed.
  - Kiểm tra Visual Responsive đa kích thước (375px, 768px, 1440px): 11/11 passed.

---

## 4. Kiểm Thử Lỗ Hổng Bảo Mật Phụ Thuộc (Security Audit)

Lệnh thực thi:
```bash
cd frontend && npm audit
```
- **Kết quả:** **0 vulnerabilities** (Đạt chuẩn an toàn tuyệt đối, các CVE `glob` và `minimatch` đã được xử lý qua overrides).
