# FRONTEND UI/UX DESIGN GUIDELINES

> **Project:** High-Concurrency Ticket Booking System (Modular Monolith)  
> **Design direction:** A — Premium Mobility SaaS  
> **Version:** 1.0  
> **Status:** Approved visual direction; global design specification  
> **Scope:** Authentication · Customer · Operator · Platform Admin  
> **Language:** Vietnamese (technical tokens in English)

---

## 1. Mục đích và nguyên tắc

Tài liệu này là **Single Source of Truth** cho ngôn ngữ thiết kế frontend. Mọi màn hình và component phải tuân theo tài liệu này, bất kể vai trò người dùng. Đây **không phải** prompt, tài liệu API, danh sách chức năng cần xây dựng hay kế hoạch triển khai.

### 1.1. Tính cách thương hiệu

- **Premium:** cao cấp nhưng không phô trương; ưu tiên khoảng trắng, tỷ lệ và chi tiết tinh tế.
- **Mobility:** cảm giác di chuyển nhanh, rõ ràng, tin cậy.
- **SaaS:** hiệu quả, có cấu trúc, dễ thao tác trên dữ liệu lớn.
- **Accessible:** tương phản tốt, keyboard-friendly, thông báo dễ hiểu.
- **Consistent:** cùng một loại thành phần phải có cùng hành vi, kích thước, trạng thái.

### 1.2. Nguyên tắc bắt buộc

1. Một hệ thống **design tokens** cho toàn bộ ứng dụng; không hardcode màu/khoảng cách tùy tiện.
2. Dùng **light theme** làm mặc định. Dark theme không nằm trong phạm vi v1.
3. **Customer** ưu tiên hành trình và chuyển đổi; **Operator/Admin** ưu tiên dữ liệu và hiệu suất thao tác.
4. Tất cả tương tác phải có trạng thái **default / hover / focus / active / disabled / loading / error / success** khi phù hợp.
5. Không phụ thuộc riêng vào màu để biểu thị ý nghĩa; luôn có text/icon/label bổ sung.
6. Dữ liệu quan trọng (ghế, số tiền, thời gian, trạng thái) cần rõ ràng hơn yếu tố trang trí.
7. Không sử dụng animation chỉ để gây ấn tượng; chuyển động phải phục vụ nhận biết trạng thái.
8. Không lồng nhiều card, sidebar hoặc drawer không cần thiết.

## 2. Visual references

> Đây là **liên kết tìm kiếm hình ảnh tham khảo**, không phải screenshot chính thức hay thiết kế đã được phê duyệt. Chỉ tham khảo bố cục, mật độ và cảm giác; **không sao chép màu/token** từ hình khác.

| Hạng mục | Hình ảnh tham khảo |
|---|---|
| Tổng thể Premium Mobility SaaS | [Mobility SaaS UI](https://www.google.com/search?tbm=isch&q=premium+mobility+saas+dashboard+ui) |
| Login / Register split screen | [Split-screen authentication](https://www.google.com/search?tbm=isch&q=premium+split+screen+login+page+travel+ui) |
| Customer booking | [Travel booking website UI](https://www.google.com/search?tbm=isch&q=modern+travel+booking+website+ui+design) |
| Seat selection | [Seat selection UI](https://www.google.com/search?tbm=isch&q=bus+seat+selection+booking+ui) |
| Operator workspace | [Fleet management SaaS](https://www.google.com/search?tbm=isch&q=fleet+management+saas+dashboard+ui) |
| Admin workspace | [Enterprise dashboard](https://www.google.com/search?tbm=isch&q=modern+enterprise+admin+dashboard+light+ui) |
| Data tables | [SaaS data table](https://www.google.com/search?tbm=isch&q=modern+saas+data+table+ui) |
| Drawers / forms | [Right side form drawer](https://www.google.com/search?tbm=isch&q=saas+right+side+drawer+form+ui) |
| Finance cards | [Fintech balance dashboard](https://www.google.com/search?tbm=isch&q=fintech+wallet+balance+dashboard+ui) |
| Design system | [UI component design system](https://www.google.com/search?tbm=isch&q=modern+saas+design+system+components) |

## 3. Color system

### 3.1. Brand and neutral tokens

| Token | HEX | Mục đích |
|---|---|---|
| `--brand-navy` | `#142B4A` | Logo, sidebar, footer, hero tối |
| `--brand-navy-hover` | `#1E3A5F` | Hover trên nền navy |
| `--primary-600` | `#2563EB` | CTA, active state, link |
| `--primary-700` | `#1D4ED8` | Hover primary |
| `--primary-100` | `#DBEAFE` | Soft highlight |
| `--accent-500` | `#F59E0B` | Điểm nhấn thương mại, thông tin đặc biệt |
| `--canvas` | `#F8FAFC` | Nền ứng dụng |
| `--surface` | `#FFFFFF` | Card, form, table, dialog |
| `--surface-muted` | `#F1F5F9` | Header bảng, secondary surface |
| `--border-default` | `#E2E8F0` | Border thông thường |
| `--border-strong` | `#CBD5E1` | Input border |
| `--text-primary` | `#0F172A` | Nội dung chính |
| `--text-secondary` | `#475569` | Nội dung phụ |
| `--text-muted` | `#64748B` | Metadata/placeholder có tương phản tốt |
| `--text-inverse` | `#FFFFFF` | Chữ trên nền tối |

### 3.2. Semantic colors

| Trạng thái | Foreground | Background |
|---|---|---|
| Success | `#166534` | `#DCFCE7` |
| Warning | `#92400E` | `#FEF3C7` |
| Danger | `#991B1B` | `#FEE2E2` |
| Info | `#1D4ED8` | `#DBEAFE` |
| Neutral | `#475569` | `#F1F5F9` |

### 3.3. Quy tắc màu

- White/neutral là nền chủ đạo; navy định hình thương hiệu; blue dành cho tương tác chính.
- Amber chỉ dùng làm accent, không dùng thay thế warning nếu gây hiểu nhầm.
- Mỗi vùng tương tác có tối đa **một primary CTA**.
- Không tô mỗi KPI card một màu nền đậm.
- Không dùng màu đỏ cho ghế đã bán chỉ vì “không khả dụng”; dùng neutral và nhãn trạng thái.
- Trạng thái phải có độ tương phản tối thiểu theo WCAG AA; khi token chưa đạt, điều chỉnh cặp màu cụ thể trước khi dùng.

## 4. Typography

**Font:** `Inter`, fallback `Be Vietnam Pro`, `system-ui`, `sans-serif`. Hỗ trợ dấu tiếng Việt đầy đủ.

| Kiểu | Desktop | Mobile | Weight | Line height |
|---|---:|---:|---:|---:|
| Display / Hero | 48px | 32px | 700 | 1.15 |
| Page title / H1 | 30px | 24px | 700 | 1.25 |
| Section / H2 | 20px | 18px | 600 | 1.35 |
| Card title / H3 | 16px | 16px | 600 | 1.4 |
| Body | 14–15px | 14–16px | 400 | 1.5 |
| Label | 14px | 14px | 500 | 1.4 |
| Helper text | 13px | 13px | 400 | 1.45 |
| Metadata | 12px | 12px | 400 | 1.4 |
| KPI | 30px | 26px | 700 | 1.2 |

- Chỉ dùng `400 / 500 / 600 / 700`.
- Không dùng toàn bộ chữ hoa cho đoạn dài; uppercase chỉ cho label nhóm ngắn.
- Giá, số lượng, countdown và KPI sử dụng `font-variant-numeric: tabular-nums`.
- Tiếng Việt dùng dấu câu, chữ hoa và thuật ngữ nhất quán; ưu tiên ngôn ngữ người dùng dễ hiểu.

## 5. Spacing, grid và kích thước

### 5.1. Spacing scale

`4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 / 48 / 64 / 80 / 96px`.

- Khoảng cách giữa label và input: **8px**.
- Giữa các field: **16–20px**.
- Giữa các section: **24–32px**.
- Card padding: **20–24px** desktop, **16px** mobile.
- Main content padding: **32px** desktop, **24px** tablet, **16px** mobile.
- Không tạo khoảng cách tùy ý như 17px hoặc 29px nếu không có lý do thiết kế rõ ràng.

### 5.2. Breakpoints

| Tên | Khoảng viewport | Quy tắc |
|---|---|---|
| Mobile | `< 640px` | Một cột, full-width actions |
| Tablet | `640–1023px` | 1–2 cột, nav compact |
| Laptop | `1024–1439px` | Desktop shell, content linh hoạt |
| Wide | `>= 1440px` | Giới hạn content width phù hợp |

- Customer max content width **1280px**.
- Admin/Operator content chiếm chiều rộng còn lại, không bị khóa cứng 1280px nếu có bảng dữ liệu lớn.
- Grid KPI: 4 cột wide, 2 cột tablet, 1–2 cột mobile tùy độ dài nhãn.
- Touch target tối thiểu **44×44px** ở mobile.

## 6. Radius, border, elevation

| Thành phần | Radius |
|---|---:|
| Badge | 6px |
| Input / Button | 8px |
| Card | 12px |
| Drawer / Dialog | 16px |
| Hero / promotional panel | 20px |
| Avatar | 50% |

- Border mặc định `1px solid #E2E8F0`.
- Card ưu tiên border nhẹ hơn shadow.
- Shadow card: `0 2px 8px rgba(15,23,42,.04)`.
- Shadow dropdown: `0 8px 24px rgba(15,23,42,.10)`.
- Shadow modal: `0 20px 60px rgba(15,23,42,.18)`.
- Không áp dụng shadow dày cho mọi card hoặc bảng.

## 7. App shells và bố cục theo vai trò

### 7.1. Authentication shell

- Desktop: split-screen **45% brand / 55% form**, hoặc đảo chiều nhất quán giữa Login/Register.
- Brand panel navy, ảnh du lịch có overlay đủ tương phản; form panel trắng.
- Form container tối đa **420px**; căn giữa theo chiều dọc.
- Mobile: ẩn/thu gọn brand panel, giữ logo + heading + form; không ép split-screen.
- Footer ngắn: bản quyền và liên kết pháp lý, không dùng marketing footer lớn.

### 7.2. Customer shell

- Header trắng sticky cao **72px**, mobile **60px**.
- Không dùng sidebar toàn cục.
- Main content max **1280px**; checkout có thể hẹp hơn để tập trung.
- Footer đầy đủ trên landing/search; checkout dùng footer tối giản.
- Search và booking CTA được ưu tiên hơn các khối marketing.

### 7.3. Operator shell

- Sidebar navy **256px**, collapsed **76px**.
- Header trắng **72px**; breadcrumb, page actions và avatar.
- Dashboard density trung bình: rõ ràng nhưng không quá nhiều khoảng trắng.
- Các bảng nghiệp vụ dùng chung table, filter, drawer và dialog.
- Menu chỉ hiển thị mục phù hợp với vai trò và trạng thái tài khoản.

### 7.4. Admin shell

- Dùng **cùng shell và component** với Operator; khác menu, tên workspace và quyền.
- Không thiết kế một bộ dashboard khác màu hoặc kiểu card khác Operator.
- Dữ liệu tài chính và quản trị dùng layout gọn, số liệu tabular.
- Không có marketing footer; có thể dùng footer metadata rất ngắn nếu cần.

## 8. Header

### Customer header

- Trái: logo; giữa: navigation; phải: support / login / avatar.
- Active link: blue, có dấu hiệu bổ sung ngoài màu nếu cần.
- Sticky với nền trắng; khi scroll có thể thêm shadow cực nhẹ.
- Mobile: logo + hành động quan trọng + menu toggle.
- Header không được chiếm quá nhiều chiều cao màn hình mobile.

### Operator/Admin header

- Trái: sidebar toggle và breadcrumb.
- Phải: thông báo, các công cụ toàn cục đã hỗ trợ, avatar menu.
- Không đặt nhiều nút CRUD không liên quan trong global header.
- Page title và primary page action nằm ở page header bên dưới.

## 9. Sidebar

- Nền `--brand-navy`, chữ trắng / slate sáng; icon Lucide 18–20px.
- Menu item cao **44px**, radius **8px**, padding ngang **12px**.
- Active: nền blue, chữ trắng; hover: navy-hover.
- Có group label ngắn; mỗi group cách nhau **16–24px**.
- Logo cố định đầu sidebar, profile/utility ở cuối nếu phù hợp.
- Collapsed: icon + tooltip; không để text bị cắt khó hiểu.
- Mobile: overlay drawer, đóng khi chọn navigation; hỗ trợ Escape và focus management.
- Không tạo sidebar lồng sidebar; các tab trong trang dùng Tabs.

## 10. Footer

- Customer: navy background, brand + nhóm link + thông tin hỗ trợ + pháp lý; padding 48–64px desktop.
- Mobile footer: stack một cột hoặc accordion nhóm link.
- Auth: footer tối giản.
- Operator/Admin: không dùng footer marketing.
- Không hiển thị liên kết hoặc thông tin pháp lý giả.

## 11. Buttons

| Variant | Mục đích | Visual |
|---|---|---|
| Primary | Hành động chính | Blue fill, white text |
| Secondary | Hành động phụ | White, slate border |
| Tertiary / Ghost | Hành động nhẹ | Transparent, subtle hover |
| Danger | Hành động phá hủy | Red fill hoặc red outline |
| Link | Điều hướng inline | Blue text + underline on hover |
| Icon-only | Action gọn | Tooltip + accessible label |

- Heights: **32px small / 40px default / 48px large**.
- Gap icon/text **8px**; icon **16–18px**.
- Loading giữ nguyên width; vô hiệu hóa double submit.
- Disabled phải khác biệt rõ, không chỉ giảm opacity quá thấp khiến label khó đọc.
- Hành động nguy hiểm phải có confirmation khi không thể hoàn tác.
- Một action bar không nên có hai nút primary cạnh nhau.

## 12. Form & field controls

### Form structure

1. Section title và mô tả nếu cần.
2. Label nằm trên input; dấu `*` cho required.
3. Field control.
4. Helper text hoặc validation error bên dưới.
5. Footer actions nhất quán.

- Input height **44px** desktop, **48px** mobile.
- Border `--border-strong`, radius 8px.
- Focus ring blue rõ ràng, không bỏ outline mà không có thay thế.
- Placeholder không thay thế label.
- Validation khi blur và submit; lỗi từ server map về field nếu có thể.
- Không xóa dữ liệu khi submit lỗi.
- Form ngắn một cột; hai cột chỉ khi field có quan hệ hợp lý và đủ không gian.
- Password có show/hide; autocomplete và inputmode đúng ngữ nghĩa.
- Date/Time hiển thị theo locale, lưu/gửi theo contract; không tự đổi múi giờ tùy tiện.
- Monetary fields căn phải và format rõ đơn vị; không tự tin cậy số tiền do frontend tính.

### Field types

| Loại | Quy định |
|---|---|
| InputText | Label + focus + error thống nhất |
| Password | Toggle visibility, không hiển thị password trong toast/log |
| Select | Searchable nếu danh sách dài; clear khi phù hợp |
| DatePicker | Không cho chọn ngày không hợp lệ theo quy tắc màn hình |
| Checkbox / Radio | Label click được, keyboard accessible |
| Textarea | Có giới hạn độ dài khi cần, resize hợp lý |
| FileUpload | Preview, format/size hints, error rõ ràng |
| Switch | Chỉ cho trạng thái bật/tắt có hiệu lực ngay; nếu rủi ro cần confirm |

## 13. Drawer, Dialog, Popover

### Drawer

- Dùng cho create/edit/view detail và advanced filters.
- Mở từ bên phải; **520px default / 680px large**; mobile fullscreen.
- **Header cố định:** title + mô tả + close.
- **Body scroll riêng:** các form section.
- **Footer cố định:** Cancel (trái) + Submit/Save (phải).
- Có unsaved-changes confirmation.
- Không đóng drawer khi đang submit quan trọng nếu có thể làm mất trạng thái.
- Focus trap, Escape và focus return phải đúng.

### Dialog

- Dùng cho confirm, cảnh báo và quyết định ngắn.
- Width **400px small / 560px medium / 720px large**.
- Title cụ thể, mô tả hậu quả, action labels rõ ràng.
- Destructive dialog: Cancel + destructive action; không đặt nút destructive làm mặc định focus nếu dễ thao tác nhầm.
- Không chứa form dài trong dialog.

### Popover / Menu

- Dùng cho contextual actions, filter nhỏ, avatar menu.
- Không chứa workflow phức tạp hoặc nhiều section dài.
- Click outside và Escape đóng được; không mất dữ liệu quan trọng âm thầm.

## 14. DataTable, filter, pagination

- Header table nền `--surface-muted`, text weight 600.
- Row height **56–64px**; hover `#F1F5F9`.
- Số tiền căn phải; ID, mã tham chiếu dễ sao chép; ngày giờ format thống nhất.
- Action column cố định cuối bảng khi cần, icon menu `…`.
- Toolbar: search → filters → clear filters → secondary actions → primary action.
- Filter chips hiển thị bộ lọc đang áp dụng.
- Pagination mặc định **10/20/50** nếu backend hỗ trợ; không gửi query params mà backend không nhận.
- Mobile: dùng responsive card hoặc horizontal scroll có chủ đích; không thu chữ xuống quá nhỏ.
- Empty state khác nhau giữa **chưa có dữ liệu** và **không có kết quả lọc**.
- Skeleton table giữ bố cục để tránh layout shift.

## 15. Cards, KPIs, Charts

### Cards

- Padding **20–24px**; radius 12px; border nhẹ.
- Card title 16px/600, metadata 12–13px.
- Không lồng card nhiều cấp; dùng divider/section nếu cần.
- Clickable card phải có hover/focus và affordance rõ.

### KPI

- Label nhỏ, value lớn, trend chỉ hiển thị khi có số liệu thật.
- Không dùng biểu tượng/đồ thị giả làm số liệu.
- Một hàng desktop tối đa 4 KPI; giảm số cột khi viewport hẹp.

### Charts

- Line/area: xu hướng theo thời gian.
- Bar: so sánh nhóm.
- Donut: cơ cấu với ít nhóm.
- Tooltip đầy đủ giá trị, đơn vị và thời điểm.
- Legend và axis labels phải rõ; không phụ thuộc duy nhất vào màu.
- Chart cần loading, empty và error states.
- Không đặt nhiều biểu đồ chỉ để lấp đầy dashboard.

## 16. Status, badge, progress, feedback

### Badge

- Height khoảng **22–26px**, radius 6px, font 12px/600.
- Dùng semantic palette, không tùy ý chọn màu theo từng màn hình.
- Các status nghiệp vụ phải map qua một registry thống nhất (status → label, tone, icon).
- Không tự tạo trạng thái backend không tồn tại; visual state như `selected` và `loading` phải tách khỏi business state.

### Toast

- Desktop top-right; mobile top-center hoặc top với safe-area.
- Success 3–4s; info 4–5s; error có thể lâu hơn và cho phép đóng.
- Toast không thay thế inline form error hoặc error state lớn.
- Không hiện raw stack trace, secret, token hay thông tin nhạy cảm.

### Loading

- Skeleton cho trang/bảng/card; spinner cho button/action nhỏ.
- Giữ chiều cao bố cục ổn định.
- Khi cập nhật dữ liệu, ưu tiên loading tại khu vực bị ảnh hưởng thay vì khóa cả trang.

### Empty / Error / Permission

- Empty: icon đơn giản + tiêu đề + mô tả + CTA nếu có hành động hợp lệ.
- Error: mô tả dễ hiểu + retry khi thích hợp.
- 403: không đủ quyền, có đường quay lại an toàn.
- 404: không tìm thấy nội dung, có navigation rõ ràng.
- Offline: giải thích trạng thái kết nối, không giả định mutation thành công.

## 17. Booking-specific visual consistency

> Mục này chỉ quy định **ngôn ngữ hiển thị**, không thay thế backend state machine.

### Seat visual states

| Visual state | Thiết kế |
|---|---|
| Available | White surface, slate border |
| Selected locally | Primary blue fill, white label |
| Unavailable / sold | Slate-muted fill, disabled cursor |
| Held by another user (nếu API phân biệt) | Amber soft fill + lock icon |
| Confirmed own hold (nếu API trả về) | Blue outline + timer association |
| Focused | Blue focus ring rõ ràng |

- Mỗi ghế có label đọc được (ví dụ `01A`), trạng thái qua text/ARIA.
- Legend nằm gần seat map; không dùng chỉ màu.
- Ghế không khả dụng không thể click.
- Seat map responsive, có thể scroll/zoom nếu cần; không co nhỏ tới mức khó chạm.
- Summary giá vé và ghế chọn phải đồng bộ về mặt hiển thị; giá authoritative vẫn do backend quyết định.
- Timer hiển thị dạng `mm:ss`, tabular numerals; cảnh báo khi gần hết hạn nhưng không gây nhấp nháy quá mức.
- Khi conflict/expiry, thông báo rõ và cho người dùng hướng xử lý.

### Checkout

- Desktop: form chính + sticky summary; mobile một cột với summary rõ ràng.
- Giá cuối cùng, đơn vị tiền và điều kiện thanh toán phải dễ thấy.
- Không dùng hiệu ứng “thành công” trước khi xác nhận trạng thái thực tế.
- Không chèn banner marketing vào giữa checkout.

## 18. Navigation, breadcrumbs, tabs

- Breadcrumb cho workspace nhiều cấp; không dùng trên landing page nếu không cần.
- Tabs cho các nhóm nội dung cùng cấp, không dùng để che nhiều workflow độc lập.
- Active tab: blue indicator + font weight, không chỉ đổi màu.
- Trở về từ detail phải giữ filter/search/page khi hợp lý.
- Các liên kết điều hướng dùng semantic link; button dành cho action.

## 19. Icons và imagery

- **Icon system:** Lucide, stroke 1.75–2, kích thước 16/18/20/24px.
- Không trộn nhiều bộ icon hoặc dùng emoji làm icon nghiệp vụ chính.
- Ảnh Customer: phương tiện, hành trình, bến xe, điểm đến có liên quan; sắc độ tự nhiên, tránh ảnh stock không đúng ngữ cảnh.
- Operator/Admin: ít ảnh, ưu tiên bảng, KPI, biểu đồ, icon và typography.
- Hero ảnh cần overlay để chữ đủ tương phản.
- Ảnh phải có alt text phù hợp; ảnh trang trí dùng alt rỗng.
- Không sử dụng ảnh hoặc logo đối tác không có quyền sử dụng.

## 20. Motion & interaction

| Chuyển động | Thời lượng |
|---|---:|
| Hover / focus color | 120–160ms |
| Button feedback | 120–180ms |
| Dropdown / popover | 140–200ms |
| Drawer / dialog | 180–240ms |
| Page content transition | 160–220ms (nếu cần) |

- Easing mặc định `ease-out` cho enter, `ease-in` cho exit.
- Không animate các con số tài chính gây hiểu nhầm.
- Tôn trọng `prefers-reduced-motion`.
- Không dùng parallax, bounce hoặc auto-carousel mặc định.

## 21. Responsive & accessibility

- Thiết kế mobile-first cho Customer; desktop-first nhưng responsive đầy đủ cho Operator/Admin.
- Không có horizontal page overflow ngoài vùng bảng/seat map có chủ đích.
- Input, menu, dialog dùng keyboard được.
- Focus visible rõ; modal/drawer trap focus và trả focus về trigger.
- Semantic heading hierarchy và landmark hợp lý.
- Form error liên kết với input qua `aria-describedby`.
- Text contrast mục tiêu WCAG AA (4.5:1 cho text thường, 3:1 cho text lớn); control/focus indicator cũng cần đủ tương phản.
- Screen reader nhận biết loading, status và thay đổi quan trọng qua live region khi phù hợp.
- Không dùng tooltip làm cách duy nhất để cung cấp thông tin thiết yếu.

## 22. Component ownership — PrimeVue và custom

| Component | Quy tắc |
|---|---|
| Button, InputText, Password, Select | PrimeVue, style bằng tokens |
| DatePicker, Checkbox, RadioButton | PrimeVue |
| Dialog, Drawer, Toast, ConfirmDialog | PrimeVue |
| DataTable, Paginator, Tabs | PrimeVue |
| Badge, Tag, Skeleton, ProgressBar | PrimeVue |
| Charts | PrimeVue Chart / Chart.js |
| AppHeader, AppSidebar, AppFooter | Custom layout component dùng chung |
| TripCard, BookingSummary, SeatMap | Custom domain component |
| EmptyState, PageHeader, StatusBadge | Shared wrapper nhất quán |

- Một loại UI dùng **một** component chuẩn, không tự tạo biến thể tùy hứng ở từng feature.
- Wrapper chỉ tạo khi có logic/style lặp lại thực sự.
- Dùng theme/preset của PrimeVue và Tailwind cùng hệ token; tránh CSS override theo selector sâu tràn lan.

## 23. CSS design tokens — tham chiếu

```css
:root {
  --brand-navy: #142B4A;
  --brand-navy-hover: #1E3A5F;
  --primary-600: #2563EB;
  --primary-700: #1D4ED8;
  --primary-100: #DBEAFE;
  --accent-500: #F59E0B;

  --canvas: #F8FAFC;
  --surface: #FFFFFF;
  --surface-muted: #F1F5F9;
  --border-default: #E2E8F0;
  --border-strong: #CBD5E1;
  --text-primary: #0F172A;
  --text-secondary: #475569;
  --text-muted: #64748B;
  --text-inverse: #FFFFFF;

  --success-fg: #166534;
  --success-bg: #DCFCE7;
  --warning-fg: #92400E;
  --warning-bg: #FEF3C7;
  --danger-fg: #991B1B;
  --danger-bg: #FEE2E2;
  --info-fg: #1D4ED8;
  --info-bg: #DBEAFE;

  --radius-sm: 6px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-xl: 16px;
  --radius-hero: 20px;

  --shadow-card: 0 2px 8px rgba(15, 23, 42, .04);
  --shadow-dropdown: 0 8px 24px rgba(15, 23, 42, .10);
  --shadow-dialog: 0 20px 60px rgba(15, 23, 42, .18);

  --header-height: 72px;
  --sidebar-width: 256px;
  --sidebar-collapsed-width: 76px;
  --customer-max-width: 1280px;
  --font-sans: 'Inter', 'Be Vietnam Pro', system-ui, sans-serif;
}
```

> Các token trên là **quy chuẩn thiết kế**, không phải cấu hình PrimeVue/Tailwind đã được triển khai. Khi tích hợp phải ánh xạ qua theme API phù hợp với phiên bản đang dùng.

## 24. Quy tắc nhất quán và ngoại lệ

| Thành phần | Bắt buộc thống nhất | Khác biệt được phép |
|---|---|---|
| Colors | Cùng tokens và semantic meaning | Navy nhiều hơn ở Auth/Sidebar |
| Typography | Cùng font, scale, weight | Customer có hero display lớn |
| Buttons/Forms | Cùng sizes, validation, states | Customer CTA có thể large |
| Dialog/Drawer | Cùng interaction, focus, footer | Drawer rộng hơn cho form phức tạp |
| Cards | Cùng radius, border, shadow | TripCard bố cục riêng |
| Tables | Cùng toolbar, rows, pagination | Customer dùng card list thay bảng |
| Header | Cùng height/tokens | Navigation theo vai trò |
| Sidebar | Cùng kiểu cho Operator/Admin | Menu theo role |
| Footer | Cùng brand tokens | Customer đầy đủ, dashboard tối giản |
| Feedback | Cùng Toast, error, empty patterns | Copywriting theo nghiệp vụ |

### Các điều cấm (anti-patterns)

- Mỗi trang dùng một màu primary khác nhau.
- Có hai kiểu Drawer cho cùng một tác vụ.
- Dùng modal dài thay cho full-page/drawer.
- Dùng icon từ nhiều thư viện khác nhau.
- Dùng font 11px cho dữ liệu quan trọng.
- Dùng badge chỉ có màu, không có text.
- Bỏ focus ring hoặc ẩn lỗi validation.
- Tạo footer marketing trong dashboard.
- Dùng số liệu giả trông như dữ liệu thực.
- Dùng animation loading giả thay vì phản ánh trạng thái thật.
- Tạo các control không có hành động hợp lệ.

## 25. Checklist review giao diện

### Visual

- [ ] Màu lấy từ design tokens, không tự thêm HEX.
- [ ] Typography theo scale và phân cấp rõ.
- [ ] Spacing đúng hệ 4px.
- [ ] Radius, border, shadow nhất quán.
- [ ] Icon cùng một hệ.

### Layout

- [ ] Đúng shell theo role.
- [ ] Header/sidebar/footer đúng quy định.
- [ ] Không tràn viewport ở mobile.
- [ ] Form, table, card dùng đúng pattern.

### Interaction

- [ ] Có hover/focus/disabled/loading/error khi phù hợp.
- [ ] Form validation rõ ràng.
- [ ] Dialog/Drawer keyboard-accessible.
- [ ] Destructive actions có confirmation.
- [ ] Empty/error states có hướng xử lý.

### Product consistency

- [ ] Trạng thái nghiệp vụ dùng registry thống nhất.
- [ ] Số tiền/ngày giờ/countdown format thống nhất.
- [ ] Không hiển thị dữ liệu giả như dữ liệu thật.
- [ ] Không tự bổ sung chức năng/API chỉ vì giao diện có chỗ trống.
- [ ] Customer, Operator và Admin nhìn như **cùng một sản phẩm**.

---

**Kết luận:** Premium Mobility SaaS = **một hệ token, một bộ component rules, nhiều app shells theo vai trò**. Khác biệt nằm ở bố cục và mật độ thông tin, không nằm ở việc tạo các ngôn ngữ thiết kế riêng lẻ.
