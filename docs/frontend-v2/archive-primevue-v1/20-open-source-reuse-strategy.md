# 20 – Chiến Lược Tái Sử Dụng Tài Nguyên Mở (Open-Source Resource Reuse Strategy)

> **Mã tài liệu:** `FE-V2-OSS-20`  
> **Thời điểm xác lập:** 25/11/2026  
> **Mục tiêu:** Tận dụng hợp pháp các tinh hoa thiết kế của cộng đồng mã nguồn mở, bảo toàn bản sắc VeloBus, tuân thủ nghiêm ngặt quyền tác giả và kiểm soát kích thước bundle.

---

## 1. Nguyên Tắc Quản Lý Tài Nguyên Mở (Non-Invasive Strategy)

1. **Tuyệt Đối Không Fork/Clone Trực Tiếp Vào Repository Chính:**
   - Không clone toàn bộ source code của các template bên ngoài (như Sakai, Volt) vào cây thư mục của VeloBus để tránh rác mã nguồn, xung đột dependencies và mất khả năng kiểm soát kiến trúc.
2. **Khai Thác Theo Mô Hình Workspace Tham Chiếu Tách Biệt:**
   - Nếu cần tải về để nghiên cứu, các kho mã tham khảo sẽ được đặt tại thư mục nằm ngoài git repo chính:
     ```
     workspace/
     ├── ticket-booking/              # Kho mã nguồn chính thức VeloBus
     └── .references/                  # Thư mục tham khảo độc lập (nằm trong .gitignore)
         ├── sakai-vue/               # Tham khảo cấu trúc App Shell, Responsive Topbar
         └── primeblocks-samples/     # Tham khảo cấu trúc Tailwind PT
     ```
3. **Chọn Lọc & Viết Lại Theo Chuẩn VeloBus (Learn & Re-implement):**
   - Chỉ học hỏi các pattern layout, giải thuật responsive drawer, và kỹ thuật kết hợp Tailwind CSS với PrimeVue Pass Through.
   - Toàn bộ component tạo mới đều mang tên tiền tố của dự án (`App*`, `Velo*`), sử dụng đúng Design Tokens `--brand-navy`, `--primary-600` và tuân thủ TypeScript nghiêm ngặt.

---

## 2. Bảng Theo Dõi & Thuộc Tính Bản Quyền (Open-Source Attribution Matrix)

| Tên tài nguyên tham khảo | Nguồn URL chính thức | Phiên bản / Commit tham chiếu | Giấy phép (License) | Phạm vi tái sử dụng cụ thể | Yêu cầu pháp lý (Attribution) |
| :--- | :--- | :---: | :---: | :--- | :--- |
| **Sakai Vue** | `github.com/primefaces/sakai-vue` | Tag `v4.2.0` | **MIT License** | Giải thuật đóng/mở Sidebar Desktop (256px $\to$ 76px) và Mobile Sidebar Overlay trong `DashboardLayout.vue`. | Ghi nhận trong `THIRD_PARTY_NOTICES.md` của dự án. |
| **PrimeVue Volt** | `primevue.org/volt` | Release 2026 | **MIT License** | Kỹ thuật cấu hình `pt` (Pass Through) cho `DataTable` và `InputText`. | Không yêu cầu đặc biệt ngoài MIT notice. |
| **PrimeBlocks Free**| `primeblocks.org` | Free Samples | **MIT License** | Bố cục dạng thẻ Summary KPI Cards và Toolbar bộ lọc. | Ghi nhận bản quyền MIT của PrimeTek. |
| **Lucide Icons** | `github.com/lucide-icons/lucide` | `v0.469.0` | **ISC License** | Thay thế 100% hệ thống icon trong toàn bộ ứng dụng. | Tự do sử dụng thương mại và cá nhân. |

---

## 3. Lộ Trình Gỡ Bỏ PrimeIcons & Thu Hẹp Bundle Size

- **Hiện trạng:** Tệp `primeicons.svg` trong `dist/assets/` nặng **342.53 kB** (nén gzip 105.26 kB), kèm các file font `.woff2`, `.ttf`, `.eot`.
- **Rủi ro khi gỡ ngay:** Một số component nội bộ của PrimeVue (như icon mũi tên sort của `DataTable`, icon đóng của `Dialog`, icon lịch của `DatePicker`) có thể dùng class `pi pi-*` mặc định.
- **Quy trình 3 bước an toàn:**
  1. *Bước 1 (Phase 3-7):* Thay thế toàn bộ icon trong mã nguồn ứng dụng của chúng ta sang `<component :is="LucideIcon" />`.
  2. *Bước 2 (Phase 8):* Sử dụng PrimeVue Pass Through hoặc icon slots (`#sorticon`, `#closeicon`, `#previcon`) để gán Lucide Icons cho các controls nội bộ của PrimeVue.
  3. *Bước 3 (Phase 8 Verification):* Chạy `npm run test:visual` trên Playwright để đảm bảo không có icon nào bị mất hoặc vỡ trước khi xóa dòng `import 'primeicons/primeicons.css'` khỏi `main.ts` và gỡ `primeicons` khỏi `package.json`.
