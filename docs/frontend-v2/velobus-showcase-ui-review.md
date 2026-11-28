# VeloBus — Independent Showcase UI/UX & Source Code Review

**Review date:** 25/11/2026  
**Decision:** CHANGES REQUIRED — Gate A visual approval not granted  
**Repository:** [velobus-ticketing-platform](https://github.com/ahryxx0602/velobus-ticketing-platform/tree/feature/frontend-v2-claude-plus)  
**Branch:** `feature/frontend-v2-claude-plus`  
**Scope:** Showcase, shared UI primitives, theme integration and visual coding rules. No production migration authorized.

## 1. Executive summary

The Claude+ palette is recognizable and the Lucide icon registry is a useful foundation. However, the current implementation does not consistently preserve component geometry, accessible color pairings, information density or the requested documentation-site composition. The main issue is not the choice of Claude+ or Shadcn Vue; it is their integration into the existing Tailwind v3 application.

All eight screenshots in the branch's `docs/frontend-v2/screenshots-showcase-hub/` directory were opened and visually inspected. Relevant source files were read in the preceding review. The qualitative assessment was approximately 65/100 for UI/UX; this is a reviewer judgement, not a measured test score. Earlier automated test results (88/88 unit, 22/22 E2E) were reported by Antigravity, not independently executed here. No live browser interaction, computed-style inspection, full source audit, or official reference screenshot comparison was performed in this review. The branch is mutable; the precise reviewed commit SHA was not recorded.

### Decision

- Retain Claude+, Vue 3, TypeScript, Tailwind v3, Shadcn Vue/Reka UI and Lucide. Do not restart the technology selection.
- Fix the shared component/theme problems at their source, not individually in production views.
- Finish the Showcase as a real, interactive component registry and documentation workspace.
- Require visual approval before Gate B. Passing tests alone is insufficient.

## 2. Evidence inventory

[Open screenshot directory](https://github.com/ahryxx0602/velobus-ticketing-platform/tree/feature/frontend-v2-claude-plus/docs/frontend-v2/screenshots-showcase-hub)

| ID | Screenshot | Observations |
|---|---|---|
| SS-01 | `showcase-calendar-desktop-light.png` | English month/weekdays, Sunday-first, circular selected day, white text on terracotta, duplicate clock icons. |
| SS-02 | `showcase-calendar-desktop-dark.png` | Dark palette recognizable; same locale issue; native time icon has weak visibility. |
| SS-03 | `showcase-calendar-mobile-light.png` | Header badges wrap, navigation consumes three rows, floating theme button overlaps calendar content. |
| SS-04 | `showcase-datatable-desktop-light.png` | Circular checkboxes, tall rows (roughly 73px), nested bordered surfaces, small action icons; pagination/column visibility not demonstrated in capture. |
| SS-05 | `showcase-dropdown-controls-desktop-light.png` | Very rounded inputs, checkbox resembles radio, dense bold labels; dropdowns shown closed, so menu behavior cannot be certified. |
| SS-06 | `showcase-foundations-desktop-light.png` | Palette swatches and typography; destructive swatch black with a red OKLCH label; radius text still says 12px. |
| SS-07 | `showcase-icons-desktop-light.png` | Lucide names and semantic grouping visible; icon cards use considerable space. |
| SS-08 | `showcase-range-presets-desktop-light.png` | Quick range selector visible but closed; no evidence of range selection behavior or open popover. |

## 3. Findings & corrective acceptance criteria

Priority: P0 = blocks visual approval because of design-system-wide or accessibility regression; P1 = required before reuse in pilot screens; P2 = documentation/evidence quality issue. These are proposed priorities, not automated severity scores.

### UI-01 — P0: Radius mapping distorts control anatomy

**Evidence:** [tailwind.config.js:90–97](https://github.com/ahryxx0602/velobus-ticketing-platform/blob/feature/frontend-v2-claude-plus/frontend/tailwind.config.js#L90-L97), [Checkbox.vue:13–24](https://github.com/ahryxx0602/velobus-ticketing-platform/blob/feature/frontend-v2-claude-plus/frontend/src/components/ui/checkbox/Checkbox.vue#L13-L24), SS-04, SS-05.

`--radius: 1rem` yields `rounded-sm: 12px`, `rounded-md: 14px`, `rounded-lg: 16px`, `rounded-xl: 20px`. A 16×16 checkbox using `rounded-sm` therefore looks circular. The canonical theme radius formula is not inherently wrong; reusing the surface radius scale for small controls is the integration error.

**Correction:** Preserve the canonical 16px base and define role-specific geometry for controls, surfaces, indicators and calendar days. Do not blindly change global Tailwind utilities that legacy views also consume. Suggested effective defaults: checkbox 4px, calendar day 6px, input/button 8px, menu 8–12px, table container 8–12px, card/dialog 16px. Radio, avatar and switch may remain circular/pill-shaped where appropriate. These are proposed VeloBus geometry values, not official Shadcn Vue defaults; verify against the compatible official reference and document approved deviations.

**Acceptance:** Checkboxes remain visually distinct from radio at every radius preset. Focus and hit areas remain usable. Compare default geometry with official component previews. Test mobile and legacy view impact.

### UI-02 — P0: Accessibility tokens are not consistently consumed

**Evidence:** [main.css:22–23](https://github.com/ahryxx0602/velobus-ticketing-platform/blob/feature/frontend-v2-claude-plus/frontend/src/assets/main.css#L22-L23), [buttonVariants.ts](https://github.com/ahryxx0602/velobus-ticketing-platform/blob/feature/frontend-v2-claude-plus/frontend/src/components/ui/button/buttonVariants.ts), [useTheme.ts:135–145](https://github.com/ahryxx0602/velobus-ticketing-platform/blob/feature/frontend-v2-claude-plus/frontend/src/shared/composables/useTheme.ts#L135-L145), SS-01, SS-06.

The light canonical primary foreground is white. The default Button uses `bg-primary text-primary-foreground`; the reset path removes the inline foreground override. A separate `--primary-effective` declaration does not help components that never consume it. The screenshot shows white text on selected terracotta. The helper chooses white when its ratio passes, otherwise returns `#141413` without checking that dark color actually passes.

Independent calculation from reported sRGB HEX values (WCAG relative luminance):

| Background | Foreground | Ratio | Normal text AA |
|---|---|---:|---|
| #C96442 | #FFFFFF | 3.90 | Fail |
| #C96442 | #141413 | 4.73 | Pass |
| #D97757 | #FFFFFF | 3.12 | Fail |
| #D97757 | #141413 | 5.90 | Pass |
| #BC5937 | #FFFFFF | 4.54 | Pass |
| #777777 | #FFFFFF | 4.48 | Fail |
| #777777 | #141413 | 4.12 | Fail |
| #777777 | #000000 | 4.69 | Pass |

**Correction:** Keep immutable canonical tokens; route component styles through accessible effective foreground/background pairs. Use a dark or black fallback only after calculation; validate exact color syntax and reject invalid inputs. Do not round before pass/fail. Verify hover opacity/composited backgrounds, focus, selected states, text links and status labels, not just base button colors. Check computed styles for actual browser colors, including OKLCH conversion.

**Acceptance:** Normal text >=4.5:1; large text >=3:1 as applicable; relevant non-text component boundaries/indicators >=3:1 where WCAG requires it. Disabled/inactive exceptions handled separately. No claim of full WCAG conformance solely from contrast tests.

### UI-03 — P1: Calendar locale and week start mismatch

**Evidence:** [Calendar.vue:27–33](https://github.com/ahryxx0602/velobus-ticketing-platform/blob/feature/frontend-v2-claude-plus/frontend/src/components/ui/calendar/Calendar.vue#L27-L33), [ShowcaseHub.vue:373–415](https://github.com/ahryxx0602/velobus-ticketing-platform/blob/feature/frontend-v2-claude-plus/frontend/src/showcase/ShowcaseHub.vue#L373-L415), SS-01–03.

Description promises Monday-first Vietnamese calendar, but screenshots show `November 2026` and `Sun Mon Tue...`. Calendar default props set weekday format and fixedWeeks, not locale or week start.

**Correction:** Verify supported props for installed Reka UI; configure Vietnamese locale (`vi-VN`) and Monday-first consistently across Calendar, RangeCalendar and composed date pickers. Preserve overrideability for other locales. Date-only values must not be shifted via UTC conversion. Separate today, selected, disabled, outside-month, range-start/end/middle and keyboard focus styles.

**Acceptance:** Vietnamese month/year and weekday labels, Monday first, `dd/MM/yyyy` display, actual selected value correct. Show inline and popover, range crossing months, min/max dates, month/year navigation, keyboard and mobile. 24-hour time for scheduling; business date limits from actual domain rules.

### UI-04 — P1: TimePicker has duplicate icons and is a demo composition

**Evidence:** [DatePickerDemo.vue](https://github.com/ahryxx0602/velobus-ticketing-platform/blob/feature/frontend-v2-claude-plus/frontend/src/components/ui/calendar/DatePickerDemo.vue), SS-01–02.

Lucide `Clock` is rendered next to native `input type="time"`, whose browser also renders an indicator. This duplicates meaning and is especially weak in dark mode. Demo state and native control are not proof of a reusable TimePicker API.

**Correction:** Choose one visible time affordance. Preserve native picker keyboard/accessibility or compose a reusable tested control; do not hide essential affordances blindly. Move demo-only state/fixtures into Showcase; provide production-ready DatePicker, DateRangePicker and TimePicker interfaces where required. Labels must be associated with controls.

**Acceptance:** One time icon, clear dark/light appearance, 24-hour values, keyboard and touch operation, typed v-model/validation/disabled support, no demo dependencies in production primitives.

### UI-05 — P1: DataTable density and wrapper maturity

**Evidence:** [TableCell.vue](https://github.com/ahryxx0602/velobus-ticketing-platform/blob/feature/frontend-v2-claude-plus/frontend/src/components/ui/table/TableCell.vue), [DataTable.vue:25–86](https://github.com/ahryxx0602/velobus-ticketing-platform/blob/feature/frontend-v2-claude-plus/frontend/src/components/ui/table/DataTable.vue#L25-L86), SS-04.

Cells use `p-4`; screenshot shows roughly 73px rows. The shared wrapper uses `any[]`, `as any`, client-side models and a `getState()` compatibility polyfill. The exact table rendered in the demo should be verified against the wrapper; the screenshot does not prove full shared DataTable feature parity. Source does not expose explicit server pagination or controlled row-selection/visibility contracts.

**Correction:** Use scoped cell padding and row density (proposed default 52px, compact 44px, comfortable 60px; multiline content may grow). Align numeric columns right, use tabular numbers, make action hit areas adequate, localize display labels without changing domain enums. Verify installed TanStack API and package-lock; prefer typed stable contracts. Do not silently change dependency version. Support server/client modes without double pagination/filtering only when needed by mapped screens.

**Acceptance:** Use the actual reusable wrapper in Showcase. Demonstrate search, filters, sorting, pagination, selection, visibility, actions, loading/empty/error, long data and mobile overflow/card alternatives. If unsupported, mark partial. No false feature claims.

### UI-06 — P1: Nested surfaces and weak hierarchy

**Evidence:** SS-01, SS-04, SS-06, SS-07. Large outer cards contain additional bordered/rounded panels with similar background. The result is flat in color but visually over-framed.

**Correction:** Use one primary table boundary, unboxed section heading and toolbar where appropriate. Preview frames should not alter the component's real appearance. Use spacing and type hierarchy rather than multiple nested cards. Reserve strong emphasis for actions/data requiring it.

**Acceptance:** Compare reference and implementation side by side; remove unnecessary frames without losing grouping or accessibility. No gradients, excessive shadows, decorative icon badges or glass effects unless a documented reference requires them.

### UI-07 — P1: Showcase is still tabs, not documentation registry

**Evidence:** [ShowcaseHub.vue:104–146](https://github.com/ahryxx0602/velobus-ticketing-platform/blob/feature/frontend-v2-claude-plus/frontend/src/showcase/ShowcaseHub.vue#L104-L146), all screenshots. Code uses six horizontal tabs despite the sidebar comment.

**Correction:** Implement a single Showcase documentation shell with grouped desktop sidebar, search, current component navigation, responsive mobile Sheet, Preview/Code/States/API where relevant, source links and real examples. Deep-linkable component IDs; no duplicate demo app. Split large page into registry metadata and modular examples. All previews import real production components.

**Acceptance:** Search and navigation work; each required component is reachable; snippets match real exports; mobile navigation does not occupy three rows. Existing dev-only route protection and mock isolation retained.

### UI-08 — P1: Global density overrides leak across component boundaries

**Evidence:** [main.css:86–153](https://github.com/ahryxx0602/velobus-ticketing-platform/blob/feature/frontend-v2-claude-plus/frontend/src/assets/main.css#L86-L153).

Rules target generic `table td`, `button.h-9`, `input.h-9`, `.p-6` and use `!important`. These can affect Calendar grid cells, Dialogs, legacy layouts and unrelated components. Default Input and Button use `h-10`, which is not covered by several rules. Hence density is both overbroad and incomplete.

**Correction:** Use named semantic dimensions or scoped component variants/data slots. Density may affect row height/control spacing, not calendar cell geometry or arbitrary layout padding. Keep input/button sizes consistent with approved component specs; maintain adequate touch targets, with documented constraints for dense date grids.

**Acceptance:** Test every density × radius preset in light/dark; only intended components change. Demonstrate legacy views unaffected. No global utility-targeting `!important` rules added to solve design issues.

### UI-09 — P1: Theme customizer dependent tokens and initialization

**Evidence:** [useTheme.ts:87–145](https://github.com/ahryxx0602/velobus-ticketing-platform/blob/feature/frontend-v2-claude-plus/frontend/src/shared/composables/useTheme.ts#L87-L145), [index.html](https://github.com/ahryxx0602/velobus-ticketing-platform/blob/feature/frontend-v2-claude-plus/frontend/index.html).

Runtime overrides only primary/ring/foreground, not the effective or sidebar variants. Default branch removes inline overrides. Storage accepts hex lengths 3 through 8, although parser supports only 3 or 6. Early-init script applies stored values differently from runtime, and no stored config means no OS dark-mode initialization in that script. These create risk of inconsistent color or flash; actual runtime behavior requires browser testing.

**Correction:** One deterministic theme resolver for canonical, semantic/accessibility and runtime layers; mode-aware presets; normalize valid opaque HEX; validate schema; derive all intended dependent tokens. Preserve status colors, intentional dark sidebar neutral behavior unless approved otherwise, and canonical mode-specific values on Reset. Align prepaint initialization with runtime. Keep draft/committed state, save/cancel/reset, cross-tab sync and system change.

**Acceptance:** Custom primary affects relevant controls, focus, selected states and sidebar accents per mapping; computed colors pass contrast; both mode defaults restore correctly; reload, invalid storage, cancel, cross-tab and first paint tested.

### UI-10 — P1: Mobile header/navigation and floating overlay

**Evidence:** SS-03; [ShowcaseHub.vue:456–457](https://github.com/ahryxx0602/velobus-ticketing-platform/blob/feature/frontend-v2-claude-plus/frontend/src/showcase/ShowcaseHub.vue#L456-L457).

Header badges wrap; horizontal tabs become three rows; floating theme trigger covers calendar content.

**Correction:** Compact responsive header, mobile Sheet navigation, theme trigger inside header/settings, internal scroll for overlay, safe-area handling and focus restoration.

**Acceptance:** At 320/375/390/430px all essential content remains reachable; no unwanted horizontal scroll, overlay clipping or floating control covering date cells. Test keyboard and touch.

### UI-11 — P1: Typography and icon consistency

**Evidence:** SS-05–07, [ShowcaseHub.vue:218–223](https://github.com/ahryxx0602/velobus-ticketing-platform/blob/feature/frontend-v2-claude-plus/frontend/src/showcase/ShowcaseHub.vue#L218-L223), [index.html](https://github.com/ahryxx0602/velobus-ticketing-platform/blob/feature/frontend-v2-claude-plus/frontend/index.html).

Typography demo labels `font-black` as Bold and `font-semibold` as Body Regular/Medium; Google Fonts requests only weights 400–700. Screenshot appearance suggests a possible fallback, but screenshot alone cannot identify the rendered font. Icon registry is promising but does not prove all usages are consistent.

**Correction:** Verify actual rendered fonts via browser font inspection, including Vietnamese glyphs and fallback coverage. Use accurate labels and approved scale: proposed title 24/600, section 18/600, body 14/400, table header 13/500, caption 12/400; line heights 1.3–1.5 as applicable. Use Geist Mono selectively for IDs/code, tabular numerals for amounts. Use Lucide only in new/migrated UI; icon registry maps action, export, size and accessible name. No decorative icon inflation.

**Acceptance:** Inspect loaded fonts and computed weights, icons at 16px actions/18px sidebar/20px header, consistent stroke 2, icon-only accessible names, no PrimeIcons in new UI. Validate Reka checkbox/switch v-model against installed version (not infer behavior from static screenshot).

### UI-12 — P2: Stale radius documentation

**Evidence:** [ShowcaseHub.vue:214](https://github.com/ahryxx0602/velobus-ticketing-platform/blob/feature/frontend-v2-claude-plus/frontend/src/showcase/ShowcaseHub.vue#L214), SS-06. Text still says 0.75rem/12px while canonical base is 1rem/16px.

**Acceptance:** Derive displayed values from actual tokens or registry metadata; distinguish base radius and effective component radius; update docs and examples together.

### UI-13 — P2: Destructive swatch mismatch

**Evidence:** [ShowcaseHub.vue:195–201](https://github.com/ahryxx0602/velobus-ticketing-platform/blob/feature/frontend-v2-claude-plus/frontend/src/showcase/ShowcaseHub.vue#L195-L201), SS-06. Black swatch labelled with a red OKLCH value. Canonical Claude+ light destructive is near-black; semantic destructive action can use an accessible red override, but the two must not be conflated.

**Acceptance:** Show canonical and effective swatches separately, use correct label/value, verify destructive action and error color contrast. Do not overwrite canonical values to make the label match.

### UI-14 — P2: Evidence does not demonstrate open/interactive states

**Evidence:** SS-05 and SS-08 show closed triggers; SS-04 does not show a pagination example. No screenshot in this directory demonstrates opened confirmation dialog, theme customizer, keyboard focus, loading/empty/error or mobile tables.

**Acceptance:** Capture reference/current/corrected screenshots under matching viewport/state; demonstrate actual interactions and retain failed cases. Add screenshot assertions only after user approves baseline. Do not claim missing evidence means feature absent.

## 4. Required Showcase inventory and scope

Audit the current catalog against actual business routes and compatible official Shadcn Vue sources. Classify each entry as verified, visually incorrect, partial, missing-required, optional, or not required. Prioritize these groups:

| Group | Required demonstrations (as applicable to VeloBus) |
|---|---|
| Foundation | All canonical/effective tokens, typography, geometry, density, spacing, shadows, Lucide registry |
| Buttons/forms | Button/IconButton, Input/PasswordInput, Textarea, Label/FormField, Select/Combobox, Checkbox/Radio/Switch, ToggleGroup, validation |
| Date/time | Calendar, RangeCalendar, DatePicker, DateRangePicker/presets, month/year, TimePicker, DateTimePicker; scheduling preview if useful |
| Data | Table/DataTable, Pagination, filters/selection/visibility, Card, Badge/StatusBadge, Avatar, Separator |
| Navigation | Sidebar, Breadcrumb, Tabs, DropdownMenu, Accordion, Collapsible, ScrollArea, mobile Sheet |
| Overlays/feedback | Dialog, AlertDialog, Sheet, Popover, Tooltip, Alert, Sonner, Progress, Spinner, Skeleton, Empty/Error, pending confirmation |
| Domain/composition | Existing seat map, trip card, ticket summary, role matrix, wallets, auth/admin/operator/customer layout examples; isolated fixture data only |
| Optional audit | Upload, image preview, charts, command palette, multiselect, stepper; implement only if justified |

## 5. Verification and approval gates

1. Record exact current commit, working tree and baseline commands; preserve uncommitted work.
2. Review official Tailwind v3-compatible component sources and screenshots. Record reference versions/URLs; do not substitute React code or blindly run latest CLI.
3. Fix theme geometry, effective contrast and density first; then Calendar/time, table, typography/icons; then documentation shell and inventory.
4. Verify actual screenshots and computed styles at 320, 375, 390, 430, 768, 1024, 1280, 1440, 1920px as appropriate. Include light/dark, overlays, interaction states and long Vietnamese content. Test selected critical interactions with Playwright and unit tests.
5. Run current package scripts for type check, unit, E2E, build and design lint. Do not weaken test assertions; preserve existing auth mocks and API envelope. Inspect diff vs baseline and identify any scope violations.
6. Document unresolved blockers, before/after evidence and changed files; no fabricated test/visual PASS. Set up screenshot baseline only after user-approved appearance.

**Prohibited:** production view migration, backend/DTO/auth/RBAC changes, blind dependency upgrade, PrimeVue removal, Tailwind v4 upgrade, automatic commit/push, destructive rollback of user changes.

**Gate A final status:** `GATE A — VISUAL FIDELITY RECOVERY COMPLETE, AWAITING USER APPROVAL` only after all required corrections have evidence. Otherwise `GATE A — CHANGES REQUIRED`. Gate B remains blocked until the user approves actual Showcase.

## 6. References and limits

- [Claude+ theme](https://tweakcn.com/themes/cmdght103000n04lh3e2ae93r) — canonical visual identity (community theme license status remains unverified in prior reports).
- [Shadcn Vue v3](https://v3.shadcn-vue.com/) — proposed compatibility reference for Tailwind v3; verify exact source/dependency versions.
- [Shadcn Vue Components](https://www.shadcn-vue.com/docs/components) — inventory and interaction patterns, not automatic code compatibility.
- [Shadcn Vue Admin](https://shadcn-vue-admin.netlify.app/) — layout reference.
- [Lucide](https://lucide.dev/icons/) — icon reference.

The review does not claim that every component is wrong or that all functionality is missing. Static screenshots and inspected excerpts support the specific findings above; runtime behavior and reference fidelity must be verified by Antigravity in a browser. Proposed geometry values and severity classifications are reviewer decisions for user approval.
