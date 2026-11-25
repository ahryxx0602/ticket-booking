# 21 – Danh Mục Quyết Định Cần Phê Duyệt Tinh Chỉnh (Revised Approval Checklist)

> **Mã tài liệu:** `FE-V2-APP-21`  
> **Thời điểm lập:** 25/11/2026  
> **Trạng thái:** **CONDITIONAL APPROVAL GATE — CHỜ DUYỆT 2 CỔNG ĐIỀU KIỆN (UI DESIGN & BACKEND GAPS)**

---

Kính gửi Quý người dùng (Tech Lead / Product Owner),

Căn cứ vào chỉ đạo tại văn bản **Independent Plan Review & Conditional Approval**, đội ngũ kỹ thuật đã hoàn thành tinh chỉnh toàn diện kế hoạch hiện đại hóa Frontend VeloBus V2. 

Để đảm bảo việc triển khai diễn ra chính xác, không gây lãng phí công sức và đạt độ hài lòng cao nhất về mặt thẩm mỹ, chúng tôi trân trọng trình **Hai Cổng Phê Duyệt Điều Kiện (Conditional Approval Gates)**:

---

### CỔNG 1: PHÊ DUYỆT HƯỚNG THIẾT KẾ GIAO DIỆN (UI DESIGN GATE)

Vui lòng lựa chọn **1 trong 3 Hướng Thiết Kế Trực Quan** được đặc tả tại [19-ui-design-direction-proposal.md](file:///home/vanthanh/Ind-Project/Java/ticket-booking/docs/frontend-v2/19-ui-design-direction-proposal.md):

| Hướng thiết kế | Phong cách chủ đạo | Đặc trưng nổi bật | Trạng thái đề xuất | Lựa chọn |
| :--- | :--- | :--- | :---: | :---: |
| **Hướng 1** | **Modern Enterprise SaaS** (Stripe & Linear) | Mật độ dữ liệu cao, viền mỏng 1px tinh tế, Slate/Zinc surfaces, Glassmorphism header, compact table (44px row). | **KHUYẾN NGHỊ** | [ ] CHỌN HƯỚNG 1 |
| **Hướng 2** | **Premium Mobility & Travel** (Airbnb Luxe) | Sắc Navy biển sâu kết hợp Amber hoàng kim, bo góc lớn (16-20px), spacing thoáng đãng, ấm cúng và sang trọng. | Khả thi | [ ] CHỌN HƯỚNG 2 |
| **Hướng 3** | **Minimal Monochromatic Dashboard** (Vercel) | Đơn sắc tương phản cao (Black/White/Gray), đường nét kỹ thuật sắc sảo, monospace font accents, cảm giác siêu tốc. | Khả thi | [ ] CHỌN HƯỚNG 3 |

> **Cam kết:** Sau khi bạn chọn Hướng thiết kế, đội ngũ kỹ thuật sẽ hoàn thiện **Bộ Trình Diễn UI Showcase (9 màn hình mẫu)** để bạn kiểm duyệt trực quan trước khi áp dụng hàng loạt lên toàn bộ codebase!

---

### CỔNG 2: PHÊ DUYỆT XỬ LÝ KHOẢNG CÁCH API BACKEND (API GAP GATE)

Căn cứ theo [17-backend-api-gap-register.md](file:///home/vanthanh/Ind-Project/Java/ticket-booking/docs/frontend-v2/17-backend-api-gap-register.md), phê duyệt phương án xử lý Frontend cho các khoảng cách:

| Mã Gap | Vấn đề API | Phương án xử lý đề xuất tại Frontend V2 | Lựa chọn |
| :---: | :--- | :--- | :---: |
| **GAP-01** | Backend thiếu endpoint `GET /api/v1/partners/trips`. | Operator tạm thời xem danh sách chuyến qua `GET /catalog/trips` hoặc các chuyến do chính mình vừa tạo trong phiên làm việc; **không tự ý sửa Backend**. Đưa vào Backlog Backend cho phase sau. | [ ] ĐỒNG Ý |
| **GAP-02** | Contract Mismatch `GET /catalog/trips` (City vs Station Code). | Frontend Client Composable tự động ánh xạ cặp mã bến (`HN-GP`) sang tên thành phố (`Hà Nội`) trước khi gửi request. | [ ] ĐỒNG Ý |
| **GAP-03** | `CheckoutView` thiếu gọi API VNPay thật. | Xóa bỏ 100% `setTimeout` giả lập. Gọi API thật `POST /api/v1/payments/checkout`, nhận `redirectPaymentUrl` và redirect trình duyệt sang cổng thanh toán VNPay thật. | [ ] ĐỒNG Ý |
| **GAP-04** | Dữ liệu mock tĩnh tại các View quản trị và vé của tôi. | Loại bỏ mảng tĩnh `ref([...])`, nạp dữ liệu thật trên `onMounted` kèm `TableSkeleton` và `ErrorState` có nút Thử lại (Retry). | [ ] ĐỒNG Ý |

---

### CÂU LỆNH XÁC NHẬN PHÊ DUYỆT ĐỀ XUẤT:
Bạn có thể phản hồi bằng cú pháp:
> **"Chọn Hướng Thiết Kế [1/2/3], đồng ý phương án xử lý API Gaps, tiến hành xây dựng UI Showcase và TypeScript Foundation."**
