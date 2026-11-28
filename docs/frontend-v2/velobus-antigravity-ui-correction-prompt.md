# VeloBus — Gate A Visual Fidelity Recovery & Enforced UI Component Registry

## 1. Task and authority

Thực hiện trực tiếp trên repository VeloBus, branch `feature/frontend-v2-claude-plus`. Đọc file review đi kèm `velobus-showcase-ui-review.md` và tài liệu `docs/frontend-v2/FRONTEND_CODING_RULES.md`, Design System, Component Catalog, Quality Gates hiện tại. Tận dụng kết quả khảo sát cũ, không viết lại Master Plan.

Tôi chưa phê duyệt Gate A. Review độc lập đã mở đủ 8 ảnh trong `docs/frontend-v2/screenshots-showcase-hub/` và đối chiếu source code, phát hiện UI-01 đến UI-14. Kết quả test PASS trong các báo cáo cũ không phải là bằng chứng giao diện đã đúng. Mục tiêu là sửa source thực tế để giao diện trung thành với reference, không chỉ làm đẹp screenshot hoặc sửa báo cáo.

**Không migration production views. Không chuyển Gate B.**

## 2. Reference hierarchy — bắt buộc

- Claude+ canonical theme: https://tweakcn.com/themes/cmdght103000n04lh3e2ae93r ; registry https://tweakcn.com/r/themes/cmdght103000n04lh3e2ae93r
- Component source tương thích Tailwind v3: https://v3.shadcn-vue.com/
- Component inventory: https://www.shadcn-vue.com/docs/components
- Documentation/layout patterns: https://shadcn-vue-admin.netlify.app/ ; https://www.shadcn-vue.com/blocks
- Icons: https://lucide.dev/icons/ — `lucide-vue-next` theo dependency đang cài.

Claude+ quyết định canonical palette, fonts, base radius và shadow. Shadcn Vue quyết định anatomy, interaction, state, component composition. VeloBus chỉ thay đổi các effective semantic/accessibility và role-specific geometry có lý do, có preview và được duyệt.

Phải mở reference bằng browser, so sánh component cùng state/viewport. Nếu source reference không truy cập được, ghi blocker; không giả lập đã xem. Xác minh chính xác phiên bản Shadcn Vue/Reka/Tailwind/TanStack trong lockfile trước khi lấy source; không chạy CLI latest tùy tiện. Không lấy React code hoặc syntax Tailwind v4 để đưa vào Tailwind v3.

## 3. Preflight & execution order

Kiểm tra git status, branch, commit SHA, diff so với baseline phù hợp, package scripts và lockfile. Bảo toàn user changes. Xác định component source đã tồn tại, không tạo trùng. Chạy baseline tests và chụp hiện trạng mới nếu đã có thay đổi sau review. Đọc toàn bộ review; ghi trạng thái từng UI-01…UI-14 (confirmed / already fixed / not reproducible / pending). Nếu review không khớp source hiện tại, cung cấp bằng chứng trước khi quyết định sửa.

Thứ tự: (A) geometry + contrast + density + theme resolver → (B) calendar/time/form/table/icons/typography → (C) Showcase documentation shell + inventory + guardrails → (D) visual verification + regression testing. Không mở rộng sang nghiệp vụ.

## 4. Mandatory corrections

### A. Geometry and theme foundation

**UI-01:** Giữ canonical `--radius: 1rem` và canonical radius scale. Không áp radius 12–16px lên checkbox 16px. Thiết lập role-specific geometry qua semantic tokens/variants tại shared layer. Mặc định đề xuất: checkbox 4px, calendar day 6px, input/button 8px, menu 8–12px, table 8–12px, card/dialog 16px. Đây là geometry hiệu dụng đề xuất, không phải giá trị canonical bắt buộc của Shadcn; đối chiếu reference và ghi nhận khác biệt. Radius presets 6/8/12/16px phải có tác dụng hợp lý mà không biến checkbox thành radio hoặc làm biến dạng icon controls. Không sửa global Tailwind radius mapping một cách mù quáng ảnh hưởng legacy views.

**UI-02:** Tách canonical, semantic/accessibility effective và runtime overrides. Nối effective tokens vào Button, Calendar, Checkbox, selected/focus states, sidebar, link và các component thực tế. Không chỉ khai báo `--primary-effective` rồi không dùng. Đảm bảo default/reset vẫn có foreground đạt contrast. Tính contrast trên giá trị thực, không làm tròn trước khi so ngưỡng. `#C96442/#FFFFFF` = 3.90 fail; `#C96442/#141413` = 4.73 pass; `#D97757/#FFFFFF` = 3.12 fail; `#D97757/#141413` = 5.90 pass. Với `#777777`, cả white (4.48) và charcoal (4.12) fail nhưng black (4.69) pass. Sửa helper để kiểm tra tất cả candidate và fallback hợp lệ. Kiểm tra normal text >=4.5, large text và relevant non-text >=3 khi áp dụng; test hover/composited background, focus, selected, status. Không tuyên bố WCAG hoàn chỉnh chỉ từ contrast.

**UI-08:** Xóa/thay density overrides rộng kiểu `[data-density] .p-6`, `table td`, `button.h-9` và `!important` bằng component-specific tokens/variants. Kiểm tra h-10 controls hiện tại. Density Compact/Default/Comfortable chỉ thay đổi component được quy định; calendar geometry, radio, checkbox, icon controls không bị bóp méo. Touch targets ưu tiên >=44px với thao tác thông thường trên mobile; với date grid chật, xác minh yêu cầu target size/spacing phù hợp thay vì ép grid tràn.

**UI-09:** Một theme resolver nhất quán cho light/dark/system, presets, custom primary, effective foreground, ring, sidebar accents, selected controls. Giữ semantic status colors độc lập, giữ mode-specific canonical default. Storage key `velobus_theme_config_v2`; normalize HEX hợp lệ (3 hoặc 6 chữ số, chuẩn hóa thành 6), schema validation, fallback, save/cancel/reset, cross-tab, OS theme change. Đồng bộ prepaint `index.html` với runtime resolver, kể cả first visit không có storage. Không tạo backend theme API. Kiểm thử mọi radius/density, custom color và Reset ở dark/light.

### B. Component correction and completion

**UI-03:** Calendar và RangeCalendar dựa trên official compatible source, Vietnamese locale, Monday-first, dd/MM/yyyy và month/year navigation. Kiểm tra props thực tế của Reka UI. Hiển thị đầy đủ today/selected/disabled/outside-month/range/focus states. Single date, range crossing months, min/max, keyboard, popover, inline, responsive. Bảo toàn date-only values; không tự UTC shift. Business constraints lấy từ caller/domain.

**UI-04:** Sửa TimePicker hai icon: chọn một affordance, giữ native interaction nếu dùng native hoặc compose reusable component chính thức. 24 giờ, label association, dark mode, touch/keyboard, v-model và validation. Tách demo fixtures/state ra Showcase; chỉ export production primitives/composed controls cần thiết. DatePicker, DateRangePicker, TimePicker, DateTimePicker có API rõ ràng; không gọi demo là component production nếu chưa đúng.

**UI-05:** DataTable default row mục tiêu 52px, compact 44px, comfortable 60px; multiline có thể tăng chiều cao. Giảm cell padding dư thừa, căn tiền bên phải, tabular nums, status label dễ đọc và accessible, action icons 16px với hit target phù hợp. Đối chiếu demo đang dùng wrapper hay table tự dựng. Kiểm tra package-lock và TanStack v9 `stockFeatures`, `getState()` polyfill, các `any`; ưu tiên API chính thức có type. Không tự upgrade/downgrade; báo blocker nếu không tương thích. Showcase dùng đúng shared DataTable và chứng minh sorting, search/filter, pagination, row selection, column visibility, loading/empty/error, long data, responsive toolbar, internal scroll/mobile card khi phù hợp. Kiểm tra server/client mode theo API thực tế; không double paginate/filter; đánh dấu PARTIAL cho chức năng chưa có.

**UI-06:** Giảm card lồng nhau, chỉ một border chính cho table, section header/toolbar hợp lý, preview frame không làm sai geometry. Không gradient, glassmorphism, shadow quá mạnh, decorative badges hoặc hiệu ứng tùy ý. Tạo information hierarchy từ typography, spacing và semantic surfaces.

**UI-11:** Xác minh Outfit/Geist Mono được load và thực sự render, bao gồm glyph tiếng Việt; không đoán font từ screenshot. Sửa font weight label và weight không được load. Scale đề xuất: title 24/600, section 18/600, body 14/400, table header 13/500, caption 12/400. Icon system dùng `lucide-vue-next` duy nhất cho new/migrated UI, stroke 2, currentColor; badge 12–14px, action/input/button 16px, sidebar 18px, header/dialog 20px, empty state 32px. Mapping chuẩn: Plus/add, Pencil/edit, Trash2/delete, Eye/view, Search/search, Filter/filter, Ellipsis/more, Calendar/date, Clock/time, BusFront/vehicle, Route/trips, Ticket/booking, Users/users, Wallet/wallet, ShieldCheck/role, CircleCheck/success, CircleX/error, TriangleAlert/warning, Info/info. Xác minh export thực tế. Icon-only button có accessible name và tooltip khi phù hợp. Kiểm tra đúng API v-model của checkbox/switch theo dependency hiện tại; không suy đoán từ hình.

### C. Showcase documentation website

**UI-07/UI-10:** Thay 6 tabs ngang bằng documentation shell thực sự, giữ route `/showcase` và DEV-only protection. Desktop sidebar nhóm component, search, active navigation, deep links, breadcrumb, page title, Preview/Code/States/API tùy component. Mobile dùng Sheet navigation, header gọn, theme action trong header (không floating button đè calendar). Không tạo app demo thứ hai. Tách ShowcaseHub lớn thành registry metadata và component examples dễ bảo trì. Mỗi preview import chính component production; usage snippet khớp export, props/events thực tế; không dựng HTML đẹp riêng để đánh lừa review.

**UI-12/UI-13:** Sửa mô tả radius 12px lỗi thời; phân biệt base 16px và effective geometry. Swatch canonical destructive near-black và effective destructive red phải tách riêng, label/token/computed colors đúng. Hiển thị canonical và effective color pair, contrast ratio lấy từ dữ liệu thực tế.

**UI-14:** Thêm interactive examples và bằng chứng mở thật cho dropdown, DatePicker/range presets, tooltip, dialog, alert dialog, sheet, theme customizer, selection, pagination, loading/empty/error và focus. Không coi trigger đóng hoặc ảnh tồn tại là đã verified.

### Component inventory (khảo sát đầy đủ, không cài bừa)

Phân loại `VERIFIED`, `VISUALLY INCORRECT`, `PARTIAL`, `MISSING REQUIRED`, `OPTIONAL`, `NOT REQUIRED`. Ưu tiên actual route/actions mapping, official source tương thích. Tối thiểu có các nhóm:

- Foundations: canonical/effective colors, typography, spacing, radius, shadows, density, icons.
- Buttons/forms: Button, IconButton, Input, PasswordInput, Textarea, Label/FormField, NumberField nếu cần, Select, Combobox, Checkbox, RadioGroup, Switch, ToggleGroup, validation.
- Calendar & Time: Calendar, RangeCalendar, DatePicker, DateRangePicker, quick presets, month/year, TimePicker, DateTimePicker; Schedule Calendar mock preview nếu phù hợp.
- Data: Table, reusable DataTable, Pagination, Card, Badge/StatusBadge, Avatar, Separator, ScrollArea.
- Navigation: Sidebar, Breadcrumb, Tabs, DropdownMenu, Accordion, Collapsible, mobile Sheet; Command/ContextMenu nếu cần.
- Feedback/overlay: Alert, Sonner, Dialog, AlertDialog, Sheet, Popover, Tooltip, Skeleton, Spinner, Progress, EmptyState, ErrorState, unsaved confirmation.
- Domain/layout: tái sử dụng SeatMap, TripCard, BookingSummary, TicketCard, RoleMatrix và shell examples nếu đã có; mock data cô lập trong Showcase. Không tạo API mới.
- Audit tùy nhu cầu: Upload/ImagePreview, charts, MultiSelect, Stepper. Không thêm dependency chỉ để tăng số lượng component.

## 5. Frontend coding enforcement

Cập nhật instruction files đang có (AGENTS.md, Antigravity workspace rules nếu được hỗ trợ, FRONTEND_CODING_RULES.md) để agent phải đọc registry trước khi code. Một nguồn quy tắc, không tạo nhiều file lặp.

Layer 1 `src/components/ui/`: anatomy, variants, tokens, generic interaction, accessibility. Layer 2 shared composed components: toolbar, page header, data table composition, feedback, theme. Layer 3 business components: domain UI. Layer 4 feature views: data, events, permission checks và composition. Feature view không tái tạo primitive nếu shared component tồn tại.

Bắt buộc trước khi dùng component mới: official reference → kiểm tra dependency/source hiện tại → reuse/approved adaptation → Showcase preview → tests + visual review → user-approved registry. Không tự thêm variant trong view; nếu cần thì bổ sung ở shared layer, documented exception.

Thêm design checks có thể chạy lại, ưu tiên ESLint/script hiện tại. Scope new/migrated UI; legacy được phân biệt. Phát hiện forbidden icon packages/PrimeIcons mới, hardcoded palette, raw duplicate controls, arbitrary geometry/effects và unauthorized variants. Không blanket-ban HTML semantic tags hoặc valid exceptions; không dùng regex như bằng chứng tuyệt đối cho visual fidelity. Có tests cho chính rule/checker, ghi rõ exception list, đường dẫn vi phạm. Không sửa legacy hàng loạt để pass lint.

## 6. Browser verification and tests

So sánh reference/current/corrected với cùng viewport, component, state. Chụp ảnh sau sửa và trực tiếp xem ảnh. 9 widths: 320,375,390,430,768,1024,1280,1440,1920px tùy component. Kiểm tra light/dark, radius/density, typography tiếng Việt, computed colors, keyboard/focus trap/restore, touch, long text, internal scroll. Không che overflow bằng `overflow-hidden`. Không tạo approved screenshot baseline trước user sign-off; không auto-update baseline để che regression.

Test thêm các case quan trọng: default/custom color contrast (#777777), invalid storage, reset light/dark, density không đổi Calendar, checkbox model/geometry, locale Monday-first, date-only/range, TimePicker, DataTable state/reactivity và pagination, confirm/cancel/pending/error, mobile navigation, Showcase guard. Phân biệt unit/interaction/E2E/screenshot comparison. Chạy current scripts (xác minh tên thực tế trước): `npm run type-check`, `npm run test:unit`, `npm run test:e2e`, `npm run build`, design checks. Baseline trước đó được báo cáo 88/88 unit, 22/22 E2E; không giả định vẫn đúng. Không skip/xóa assertion, không tăng timeout để che lỗi. Preserve auth fixture consistency.

## 7. Safety of scope and deliverables

Giữ nguyên API envelope (interceptor trả `response.data`, không unwrap thêm), Jackson NON_NULL handling, auth, router guards, 26 RBAC permissions, backend Java, payment/booking/business logic. Không thay đổi production views, không remove PrimeVue, không Tailwind v4 migration, không tự đổi package versions, không commit/push. Bảo toàn user changes, rollback theo diff có kiểm soát, không dùng lệnh destructive đại trà.

Báo cáo cuối ngắn gọn nhưng có bằng chứng:

1. Branch/commit baseline và scope diff.
2. Bảng UI-01…UI-14: finding, root cause, changed file, test/visual evidence, PASS/PARTIAL/BLOCKED.
3. Component inventory và các component mới/thiếu; official reference và version tương ứng.
4. Geometry/effective token/contrast và Icon Registry đã áp dụng.
5. Before/After screenshots và reference links, gồm desktop/mobile/light/dark và các open states.
6. Design guardrails, instruction files, cách chạy checks.
7. Kết quả lệnh test thực tế (passed/failed/skipped/retries), responsive/accessibility evidence, known limitations.
8. Showcase URL và hướng dẫn user review.

Lưu screenshot trong `docs/frontend-v2/screenshots-showcase-hub/` theo cấu trúc rõ ràng (before/after hoặc tên không ghi đè evidence cũ). Không tạo thêm hệ thống tài liệu trùng lặp. Không tuyên bố 100% nếu còn hạng mục chưa kiểm chứng.

Kết thúc bằng một trong hai trạng thái:

- `GATE A — VISUAL FIDELITY RECOVERY COMPLETE, AWAITING USER APPROVAL` nếu tất cả mandatory corrections có evidence.
- `GATE A — CHANGES REQUIRED` nếu còn lỗi, blocker hoặc thiếu evidence bắt buộc.

**Dừng tại Gate A. Không tự động chuyển Gate B, không tự phê duyệt thay tôi.**
