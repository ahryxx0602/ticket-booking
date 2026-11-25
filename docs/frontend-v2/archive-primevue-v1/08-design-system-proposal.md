# 08 – Đề Xuất Hệ Thống Thiết Kế VeloBus V2 (Design System Proposal)

> **Mã tài liệu:** `FE-V2-DS-08`  
> **Thời điểm xây dựng:** 25/11/2026  
> **Định hướng nhận diện:** **Premium Mobility SaaS + Modern Enterprise Dashboard**.

---

## 1. Định Hướng Trực Quan Cho 3 Phân Hệ (Portal Personas)

1. **Customer Portal (Trải Nghiệm Đặt Vé Cao Cấp - Premium Travel):**
   - Không gian thoáng đãng, tương phản rõ nét giữa nền trắng ngà `--canvas` (`#F8FAFC`) và sắc xanh biển sâu `--brand-navy` (`#142B4A`).
   - Sơ đồ ghế 2 tầng hiện đại, trực quan, các trạng thái ghế hiển thị rõ ràng bằng màu sắc mềm mại không chói gắt.
   - Các tương tác chọn chuyến xe có cảm giác mượt mà, tạo sự an tâm khi thanh toán.
2. **Operator Workspace (Không Gian Vận Hành Chuyên Nghiệp - Transport Operations):**
   - Thiết kế dạng bảng biểu mật độ dữ liệu cao (Data-dense), tập trung vào các thông số sống còn của nhà xe: Số dư ví ký quỹ, lịch trình sắp xuất bến, trạng thái bảo dưỡng phương tiện.
   - Thao tác nhanh, tạo chuyến xe và quản lý tài xế qua Right-Side Drawer không làm mất ngữ cảnh.
3. **Platform Governance Admin (Bảng Điều Khiển Quản Trị Sàn - Enterprise SaaS):**
   - Phong cách quản trị doanh nghiệp chuẩn mực lấy cảm hứng từ PrimeVue Sakai & Stripe Dashboard.
   - Phân cấp thông tin chặt chẽ theo Business Domains, hỗ trợ tìm kiếm tức thì, bộ lọc nâng cao và kiểm soát quyền hạn minh bạch.

---

## 2. Hệ Thống Design Tokens Chính Thức V2 (VeloBus Design Tokens V2)

### 2.1. Brand & Semantic Colors:
```css
:root {
  /* Brand Navy: Dành cho Sidebar, Footer, Hero tối */
  --brand-navy: #142B4A;
  --brand-navy-hover: #1E3A5F;
  --brand-navy-active: #0F1F35;

  /* Primary Blue: Single Source of Truth cho CTAs, Active States */
  --primary-50:  #EFF6FF;
  --primary-100: #DBEAFE;
  --primary-500: #3B82F6;
  --primary-600: #2563EB; /* Nút bấm chính, Ghế đang chọn */
  --primary-700: #1D4ED8; /* Hover */
  --primary-800: #1E40AF;

  /* Accent & Warning Amber: Đếm ngược giữ chỗ, Cảnh báo */
  --accent-500:  #F59E0B;
  --warning-50:  #FEF3C7;
  --warning-800: #92400E;

  /* Semantic Success Emerald: Đặt vé thành công, Lệnh rút hoàn tất */
  --success-50:  #DCFCE7;
  --success-600: #16A34A;
  --success-800: #166534;

  /* Semantic Danger Rose: Hủy vé, Hết hạn giữ chỗ, Khóa tài khoản */
  --danger-50:   #FEE2E2;
  --danger-600:  #DC2626;
  --danger-800:  #991B1B;

  /* Surface & Canvas: Nền ứng dụng và thẻ Card */
  --canvas:        #F8FAFC;
  --surface:       #FFFFFF;
  --surface-muted: #F1F5F9;
  --border-default:#E2E8F0;
  --border-strong: #CBD5E1;

  /* Typography Text Colors (WCAG AA Compliance) */
  --text-primary:   #0F172A; /* Tiêu đề, số liệu KPI */
  --text-secondary: #475569; /* Nội dung, nhãn trường nhập */
  --text-muted:     #64748B; /* Metadata, placeholder */
  --text-inverse:   #FFFFFF; /* Chữ trên nền Navy / Nút Primary */
}
```

### 2.2. Thang Đo Khoảng Cách (Spacing) & Bo Góc (Border Radius):
- **Spacing:** Hệ số 4px chuẩn mực (`4px`, `8px`, `12px`, `16px`, `20px`, `24px`, `32px`, `48px`).
- **Border Radius:**
  - `sm` (6px): Nhãn trạng thái (Status Badges), Chips.
  - `md` (8px): Ô nhập liệu (Input), Nút bấm (Button), Mục chọn (Select).
  - `lg` (12px): Thẻ nội dung (Cards), Khối KPI, Bảng dữ liệu (DataTable).
  - `xl` (16px): Khung trượt (Drawer), Hộp thoại (Dialog, Modal).
  - `2xl` (20px): Khung Hero quảng cáo (Hero Banner).

### 2.3. Bảng Phân Loại Trạng Thái Ghế Xe Khách 2 Tầng:
| Trạng thái ghế | Màu nền | Màu viền | Màu chữ | Icon phụ trợ | Ý nghĩa nghiệp vụ |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **Available (Trống)** | `#FFFFFF` | `#CBD5E1` | `#0F172A` | Không | Ghế có thể chọn đặt vé. |
| **Selected (Đang chọn)** | `#2563EB` | `#1D4ED8` | `#FFFFFF` | Check | Ghế do chính khách hàng đang bấm chọn. |
| **Held Other (Người khác giữ)**| `#FEF3C7` | `#F59E0B` | `#92400E` | Lock | Ghế đang bị khóa tạm thời 10 phút trên Redis. |
| **Sold (Đã bán)** | `#F1F5F9` | `#E2E8F0` | `#94A3B8` | X | Ghế đã thanh toán hoàn tất, không thể chọn. |
| **Own Hold (Chính mình giữ)** | `#EFF6FF` | `#2563EB` (2px) | `#1D4ED8` | Clock | Ghế của phiên giữ chỗ hiện tại đang đếm ngược. |

---

## 3. Chiến Lược Dark Mode (Light/Dark Strategy)

- **Giai đoạn hiện tại (V2 Initial):** Ưu tiên hoàn thiện **100% Light Theme** chuẩn mực, thanh lịch và không có lỗi tương phản màu sắc.
- **Kiến trúc mở:** Toàn bộ bảng màu đều được cấu hình qua CSS Variables và selector `.app-dark` của Tailwind CSS + PrimeVue. Hạ tầng sẵn sàng để mở rộng tính năng chuyển đổi giao diện Dark Mode ở giai đoạn tiếp theo mà không cần sửa đổi mã nguồn component.
