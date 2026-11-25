<script setup lang="ts">
import { ref, computed } from 'vue'
import { Button } from '@/components/ui/button'
import { Input, PasswordInput } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Badge, StatusBadge } from '@/components/ui/badge'
import { Switch } from '@/components/ui/switch'
import { Checkbox } from '@/components/ui/checkbox'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from '@/components/ui/select'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { Calendar } from '@/components/ui/calendar'
import { RangeCalendar } from '@/components/ui/range-calendar'
import { Toggle } from '@/components/ui/toggle'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { Accordion, AccordionItem, AccordionTrigger, AccordionContent } from '@/components/ui/accordion'
import { Separator } from '@/components/ui/separator'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { TimePicker } from '@/components/ui/time-picker'
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from '@/components/ui/sheet'
import DatePickerDemo from '@/components/ui/calendar/DatePickerDemo.vue'
import DateRangePickerDemo from '@/components/ui/calendar/DateRangePickerDemo.vue'
import IconRegistryDemo from './IconRegistryDemo.vue'
import DataTableDemo from './DataTableDemo.vue'
import FeedbackDemo from './FeedbackDemo.vue'
import DropdownDemo from './DropdownDemo.vue'
import ThemeCustomizer from '@/shared/components/ThemeCustomizer.vue'
import { useTheme } from '@/shared/composables/useTheme'
import {
  Bus,
  Layers,
  Palette,
  Type,
  LayoutGrid,
  ShieldCheck,
  BellRing,
  CalendarDays,
  Sun,
  Moon,
  Search,
  BookOpen,
  CheckCheck,
  Menu,
  Clock,
  Paintbrush,
  Sparkles,
  ExternalLink,
  SlidersHorizontal,
  Code2
} from 'lucide-vue-next'
import { today, getLocalTimeZone } from '@internationalized/date'

const { isDark, setThemeMode } = useTheme()
const isCustomizerOpen = ref(false)
const isMobileMenuOpen = ref(false)
const filterKeyword = ref('')
const activeSection = ref('foundations-colors')

// Form sample states
const sampleText = ref('')
const samplePassword = ref('')
const sampleTextarea = ref('')
const sampleSelect = ref('express')
const sampleSwitch = ref(true)
const sampleCheckbox = ref(true)
const sampleRadio = ref('option-1')
const sampleToggle = ref(false)
const sampleToggleGroup = ref('day')
const sampleTime = ref('08:30')
const singleDate = ref<any>(today(getLocalTimeZone()))

// Navigation Groups
const navGroups = [
  {
    title: 'Nền Tảng (Foundations)',
    items: [
      { id: 'foundations-colors', label: 'Bảng Màu OKLCH Claude+', icon: Palette },
      { id: 'foundations-typography', label: 'Quy Chuẩn Typography', icon: Type },
      { id: 'foundations-radius', label: 'Bo Góc & Density Scoped', icon: SlidersHorizontal },
      { id: 'foundations-icons', label: 'Icon Registry (Lucide)', icon: ShieldCheck }
    ]
  },
  {
    title: 'Form & Nút Bấm',
    items: [
      { id: 'controls-buttons', label: 'Buttons & Biến Thể', icon: LayoutGrid },
      { id: 'controls-inputs', label: 'Input & Textarea', icon: Code2 },
      { id: 'controls-selection', label: 'Checkbox, Radio & Switch', icon: CheckCheck },
      { id: 'controls-select', label: 'Select & ToggleGroup', icon: Layers }
    ]
  },
  {
    title: 'Lịch & Thời Gian',
    items: [
      { id: 'calendar-inline', label: 'Calendar vi-VN (Thứ 2 Đầu Tuần)', icon: CalendarDays },
      { id: 'calendar-datepicker', label: 'DatePicker & TimePicker 24H', icon: Clock },
      { id: 'calendar-range', label: 'DateRangePicker & Quick Presets', icon: CalendarDays }
    ]
  },
  {
    title: 'Dữ Liệu & Bảng Biểu',
    items: [
      { id: 'data-table', label: 'TanStack DataTable & Density', icon: Layers },
      { id: 'data-primitives', label: 'Badges, Avatar & Separator', icon: Sparkles }
    ]
  },
  {
    title: 'Điều Hướng & Menu',
    items: [
      { id: 'nav-dropdown', label: 'Dropdown Menu System', icon: LayoutGrid },
      { id: 'nav-accordion', label: 'Accordion & Tabs', icon: BookOpen }
    ]
  },
  {
    title: 'Lớp Phủ & Phản Hồi',
    items: [
      { id: 'feedback-modals', label: 'Dialogs & Alerts', icon: BellRing }
    ]
  }
]

const filteredNavGroups = computed(() => {
  if (!filterKeyword.value.trim()) return navGroups
  const q = filterKeyword.value.toLowerCase().trim()
  return navGroups.map(group => ({
    ...group,
    items: group.items.filter(item =>
      item.label.toLowerCase().includes(q) || item.id.toLowerCase().includes(q)
    )
  })).filter(group => group.items.length > 0)
})

function scrollTo(id: string) {
  activeSection.value = id
  isMobileMenuOpen.value = false
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}
</script>

<template>
  <div class="min-h-screen bg-background text-foreground transition-colors duration-200">
    <!-- Showcase Documentation Top Header -->
    <header class="border-b bg-card/90 backdrop-blur sticky top-0 z-40 border-border">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-4">
        <!-- Logo & Brand Identity -->
        <div class="flex items-center gap-3 shrink-0">
          <!-- Mobile Hamburger Menu Button (UI-10) -->
          <Button
            variant="ghost"
            size="icon"
            data-testid="mobile-menu-trigger"
            class="md:hidden h-9 w-9 text-foreground"
            @click="isMobileMenuOpen = true"
          >
            <Menu class="h-5 w-5" />
          </Button>

          <div class="h-9 w-9 rounded-md bg-primary flex items-center justify-center text-primary-foreground shadow-sm">
            <Bus class="h-5 w-5" />
          </div>
          <div>
            <div class="flex items-center gap-2">
              <span class="font-bold text-base tracking-tight">VeloBus Design System</span>
              <Badge variant="outline" class="text-[10px] font-mono border-primary/40 text-primary px-1.5 py-0">
                Claude+ V2.1
              </Badge>
            </div>
            <p class="text-[11px] text-muted-foreground hidden sm:block">
              Tài liệu thư viện UI Component chuẩn Shadcn Vue & Bảng màu Claude+
            </p>
          </div>
        </div>

        <!-- Global Search in Header -->
        <div class="hidden md:flex items-center flex-1 max-w-xs relative">
          <Search class="absolute left-3 top-2.5 h-3.5 w-3.5 text-muted-foreground pointer-events-none" />
          <Input
            v-model="filterKeyword"
            placeholder="Tìm kiếm component..."
            class="pl-8.5 h-8.5 text-xs bg-muted/30"
          />
        </div>

        <!-- Header Actions: Theme Mode + Theme Customizer Drawer Trigger -->
        <div class="flex items-center gap-2 shrink-0">
          <!-- Integrated Theme Customizer Trigger Button (UI-10: replaces overlapping floating button) -->
          <Button
            variant="outline"
            size="sm"
            class="h-8.5 px-2.5 text-xs gap-1.5 border-primary/30 text-primary hover:bg-primary/10"
            @click="isCustomizerOpen = true"
          >
            <Paintbrush class="h-3.5 w-3.5" />
            <span class="hidden sm:inline">Tùy Biến Theme</span>
          </Button>

          <!-- Light / Dark Switcher -->
          <Button
            variant="ghost"
            size="icon"
            class="h-8.5 w-8.5 text-muted-foreground hover:text-foreground hover:bg-muted"
            :title="isDark ? 'Chuyển sang giao diện Sáng' : 'Chuyển sang giao diện Tối'"
            @click="setThemeMode(isDark ? 'light' : 'dark')"
          >
            <Sun v-if="isDark" class="h-4 w-4" />
            <Moon v-else class="h-4 w-4" />
          </Button>

          <Badge variant="secondary" class="font-mono text-[11px] hidden lg:inline-flex">
            Gate A Verified
          </Badge>
        </div>
      </div>
    </header>

    <!-- Mobile Navigation Sheet (UI-10) -->
    <Sheet v-model:open="isMobileMenuOpen">
      <SheetContent side="left" class="w-72 p-4 overflow-y-auto">
        <SheetHeader class="text-left pb-4 border-b border-border">
          <SheetTitle class="text-sm font-bold flex items-center gap-2">
            <Bus class="h-4 w-4 text-primary" />
            <span>Mục Lục Component</span>
          </SheetTitle>
        </SheetHeader>
        <div class="py-4 space-y-6">
          <div v-for="group in navGroups" :key="group.title" class="space-y-1.5">
            <div class="text-[11px] font-bold uppercase tracking-wider text-muted-foreground px-2">
              {{ group.title }}
            </div>
            <div class="space-y-0.5">
              <button
                v-for="item in group.items"
                :key="item.id"
                type="button"
                class="w-full flex items-center gap-2 px-2.5 py-1.5 rounded-md text-xs font-medium transition-colors text-left"
                :class="activeSection === item.id ? 'bg-primary text-primary-foreground font-semibold' : 'text-foreground hover:bg-muted'"
                @click="scrollTo(item.id)"
              >
                <component :is="item.icon" class="h-3.5 w-3.5 shrink-0" />
                <span class="truncate">{{ item.label }}</span>
              </button>
            </div>
          </div>
        </div>
      </SheetContent>
    </Sheet>

    <!-- Main Documentation Shell (Sidebar + Main Content) -->
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex items-start gap-8">
      <!-- Desktop Sticky Sidebar (UI-07) -->
      <aside class="hidden md:block w-64 shrink-0 sticky top-24 self-start max-h-[calc(100vh-7rem)] overflow-y-auto pr-2 space-y-6">
        <div v-for="group in filteredNavGroups" :key="group.title" class="space-y-1.5">
          <div class="text-[11px] font-bold uppercase tracking-wider text-muted-foreground px-2">
            {{ group.title }}
          </div>
          <div class="space-y-0.5">
            <button
              v-for="item in group.items"
              :key="item.id"
              type="button"
              class="w-full flex items-center gap-2 px-2.5 py-1.5 rounded-md text-xs font-medium transition-colors text-left cursor-pointer"
              :class="activeSection === item.id ? 'bg-primary text-primary-foreground font-semibold shadow-xs' : 'text-foreground/80 hover:bg-muted hover:text-foreground'"
              @click="scrollTo(item.id)"
            >
              <component :is="item.icon" class="h-3.5 w-3.5 shrink-0" />
              <span class="truncate">{{ item.label }}</span>
            </button>
          </div>
        </div>
      </aside>

      <!-- Main Content Documentation Workspace -->
      <main class="flex-1 min-w-0 space-y-12">
        <!-- Breadcrumb & Page Overview -->
        <div class="space-y-2 border-b border-border pb-6">
          <div class="flex items-center gap-2 text-xs text-muted-foreground">
            <span>Tài Liệu Kỹ Thuật Frontend V2</span>
            <span>/</span>
            <span class="font-semibold text-foreground">Design System Documentation Hub</span>
          </div>
          <h1 class="text-2xl font-bold tracking-tight text-foreground">
            VeloBus UI Component Specification & Design System
          </h1>
          <p class="text-sm text-muted-foreground max-w-3xl leading-relaxed">
            Hệ thống giao diện chuẩn cho nền tảng VeloBus xây dựng trên nền <strong>Shadcn Vue</strong>, <strong>Reka UI</strong>, bảng màu canonical <strong>Claude+ (Warm Minimalist)</strong> và bộ icon duy nhất <strong>Lucide</strong>.
          </p>
        </div>

        <!-- ================= SECTION 01: FOUNDATIONS - COLORS ================= -->
        <section id="foundations-colors" class="space-y-4 scroll-mt-24">
          <div class="flex items-center justify-between border-b border-border pb-2">
            <div>
              <h2 class="text-lg font-bold flex items-center gap-2">
                <Palette class="h-4 w-4 text-primary" />
                <span>Bảng Màu OKLCH Canonical & Effective Tokens (UI-02, UI-13)</span>
              </h2>
              <p class="text-xs text-muted-foreground">
                Tách biệt rõ ràng giữa Canonical Palette và Semantic/Effective Accessibility Overrides đạt chuẩn WCAG AA.
              </p>
            </div>
            <Badge variant="outline" class="text-xs font-mono">OKLCH / WCAG AA</Badge>
          </div>

          <!-- Color Swatches Grid -->
          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <!-- Swatch 1: Canonical Primary -->
            <div class="p-3.5 rounded-lg border border-border bg-card space-y-2 shadow-xs">
              <div class="h-14 rounded-md bg-primary flex items-center justify-center text-primary-foreground font-mono text-xs font-bold shadow-inner">
                Primary #C96442
              </div>
              <div class="text-xs space-y-0.5">
                <div class="font-bold text-foreground">Canonical Primary</div>
                <div class="text-[11px] font-mono text-muted-foreground">oklch(0.6171 0.1375 39.0427)</div>
                <div class="text-[11px] text-emerald-600 dark:text-emerald-400 font-medium">Text #141413 (Contrast 4.73:1 AA)</div>
              </div>
            </div>

            <!-- Swatch 2: Effective Primary -->
            <div class="p-3.5 rounded-lg border border-border bg-card space-y-2 shadow-xs">
              <div class="h-14 rounded-md bg-primary-effective flex items-center justify-center text-white font-mono text-xs font-bold shadow-inner">
                Effective #BC5937
              </div>
              <div class="text-xs space-y-0.5">
                <div class="font-bold text-foreground">Effective Primary (White Text)</div>
                <div class="text-[11px] font-mono text-muted-foreground">oklch(0.58 0.1375 39.0427)</div>
                <div class="text-[11px] text-emerald-600 dark:text-emerald-400 font-medium">Text #FFFFFF (Contrast 4.54:1 AA)</div>
              </div>
            </div>

            <!-- Swatch 3: Canonical Destructive (Near-black) -->
            <div class="p-3.5 rounded-lg border border-border bg-card space-y-2 shadow-xs">
              <div class="h-14 rounded-md bg-destructive flex items-center justify-center text-destructive-foreground font-mono text-xs font-bold shadow-inner">
                Destructive #141413
              </div>
              <div class="text-xs space-y-0.5">
                <div class="font-bold text-foreground">Canonical Destructive (UI-13)</div>
                <div class="text-[11px] font-mono text-muted-foreground">oklch(0.1908 0.0020 106.5859)</div>
                <div class="text-[11px] text-muted-foreground">Màu tối đặc trưng Claude+</div>
              </div>
            </div>

            <!-- Swatch 4: Semantic Error / Action Danger -->
            <div class="p-3.5 rounded-lg border border-border bg-card space-y-2 shadow-xs">
              <div class="h-14 rounded-md bg-status-danger flex items-center justify-center text-white font-mono text-xs font-bold shadow-inner">
                Danger / Red #D9372F
              </div>
              <div class="text-xs space-y-0.5">
                <div class="font-bold text-foreground">Semantic Destructive Action</div>
                <div class="text-[11px] font-mono text-muted-foreground">oklch(0.585 0.20 28.0)</div>
                <div class="text-[11px] text-emerald-600 dark:text-emerald-400 font-medium">Nút xóa/hủy (Contrast 4.63:1 AA)</div>
              </div>
            </div>
          </div>
        </section>

        <!-- ================= SECTION 02: FOUNDATIONS - TYPOGRAPHY ================= -->
        <section id="foundations-typography" class="space-y-4 scroll-mt-24">
          <div class="border-b border-border pb-2">
            <h2 class="text-lg font-bold flex items-center gap-2">
              <Type class="h-4 w-4 text-primary" />
              <span>Quy Chuẩn Typography & Phông Chữ (UI-11)</span>
            </h2>
            <p class="text-xs text-muted-foreground">
              Phông Outfit cho sans-serif và Geist Mono cho mã vé/tiền tệ. Trọng số thực tế tải từ Google Fonts (400, 500, 600, 700).
            </p>
          </div>

          <div class="p-5 rounded-lg border border-border bg-card space-y-4 shadow-xs">
            <div class="space-y-3">
              <div class="text-2xl font-semibold text-foreground">
                Heading 1 — 24px / SemiBold (600) — Tiêu đề trang chính
              </div>
              <div class="text-lg font-semibold text-foreground">
                Heading 2 — 18px / SemiBold (600) — Tiêu đề phân vùng nghiệp vụ
              </div>
              <div class="text-base font-semibold text-foreground">
                Heading 3 — 16px / SemiBold (600) — Tiêu đề card / modal header
              </div>
              <div class="text-sm font-normal text-foreground">
                Body Regular — 14px / Regular (400) — Nội dung thông tin người dùng, bảng dữ liệu mặc định
              </div>
              <div class="text-xs font-medium text-muted-foreground">
                Table Header / Helper — 13px / Medium (500) — Tiêu đề cột bảng & văn bản hướng dẫn
              </div>
              <div class="text-xs text-muted-foreground">
                Caption / Timestamp — 12px / Regular (400) — Thời gian xuất bến & ghi chú phụ
              </div>
              <div class="text-xs font-mono font-medium text-primary tabular-nums">
                Monospace Tabular — 12px / Medium (500) — Mã vé: BKG-20261125-001 | 850.000 đ
              </div>
            </div>
          </div>
        </section>

        <!-- ================= SECTION 03: FOUNDATIONS - RADIUS & DENSITY ================= -->
        <section id="foundations-radius" class="space-y-4 scroll-mt-24">
          <div class="border-b border-border pb-2">
            <h2 class="text-lg font-bold flex items-center gap-2">
              <SlidersHorizontal class="h-4 w-4 text-primary" />
              <span>Quy Chuẩn Bo Góc (Radius) & Scoped Density (UI-01, UI-08, UI-12)</span>
            </h2>
            <p class="text-xs text-muted-foreground">
              Base Radius chuẩn 1rem (16px), đi kèm Role-Specific Geometry: Checkbox 4px, Calendar cell 6px, Input/Button 8px, Card 16px.
            </p>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="p-4 rounded-lg border border-border bg-card space-y-3 text-xs shadow-xs">
              <div class="font-bold text-foreground">Role-Specific Geometry (Bảo toàn hình học điều khiển):</div>
              <ul class="space-y-2 list-disc list-inside text-muted-foreground">
                <li><strong class="text-foreground">Checkbox:</strong> <code>rounded-[4px]</code> (Không bao giờ bị biến dạng thành hình tròn như Radio).</li>
                <li><strong class="text-foreground">Calendar Day Cell:</strong> <code>rounded-[6px]</code> (Bo nhẹ tinh tế, không bo tròn 16px).</li>
                <li><strong class="text-foreground">Input & Button:</strong> <code>rounded-md</code> (8px chuẩn công thái học SaaS).</li>
                <li><strong class="text-foreground">Card & Dialog:</strong> <code>rounded-xl</code> (16px surface).</li>
              </ul>
            </div>

            <div class="p-4 rounded-lg border border-border bg-card space-y-3 text-xs shadow-xs">
              <div class="font-bold text-foreground">Scoped Density System (UI-08):</div>
              <p class="text-muted-foreground leading-relaxed">
                Đã loại bỏ hoàn toàn các quy tắc ép <code>!important</code> diện rộng làm hỏng ô lịch hoặc dialog. Density chỉ tác động có kiểm soát lên độ cao bảng dữ liệu:
              </p>
              <div class="flex items-center gap-2 pt-1 font-mono text-[11px]">
                <Badge variant="outline">Compact: 44px</Badge>
                <Badge variant="outline">Default: 52px</Badge>
                <Badge variant="outline">Comfortable: 60px</Badge>
              </div>
            </div>
          </div>
        </section>

        <!-- ================= SECTION 04: FOUNDATIONS - ICONS ================= -->
        <section id="foundations-icons" class="space-y-4 scroll-mt-24">
          <div class="border-b border-border pb-2">
            <h2 class="text-lg font-bold flex items-center gap-2">
              <ShieldCheck class="h-4 w-4 text-primary" />
              <span>Lucide Icon Registry (UI-11)</span>
            </h2>
            <p class="text-xs text-muted-foreground">
              Bộ icon duy nhất <code>lucide-vue-next</code> được chuẩn hóa theo kích thước và ngữ nghĩa miền vận tải VeloBus.
            </p>
          </div>

          <IconRegistryDemo />
        </section>

        <!-- ================= SECTION 05: FORM CONTROLS - BUTTONS ================= -->
        <section id="controls-buttons" class="space-y-4 scroll-mt-24">
          <div class="border-b border-border pb-2">
            <h2 class="text-lg font-bold flex items-center gap-2">
              <LayoutGrid class="h-4 w-4 text-primary" />
              <span>Buttons & Biến Thể (UI-01, UI-02)</span>
            </h2>
            <p class="text-xs text-muted-foreground">
              Các biến thể nút bấm chuẩn Shadcn Vue với góc bo 8px (<code>rounded-md</code>) và màu chữ đạt tương phản WCAG AA.
            </p>
          </div>

          <div class="p-5 rounded-lg border border-border bg-card space-y-4 shadow-xs">
            <div class="flex flex-wrap items-center gap-3">
              <Button variant="default">Primary Button</Button>
              <Button variant="secondary">Secondary Button</Button>
              <Button variant="outline">Outline Button</Button>
              <Button variant="ghost">Ghost Button</Button>
              <Button variant="destructive">Destructive Action</Button>
              <Button variant="link">Link Button</Button>
              <Button variant="default" disabled>Disabled State</Button>
            </div>
            <div class="flex flex-wrap items-center gap-3 pt-2 border-t border-border/60">
              <span class="text-xs text-muted-foreground">Kích thước:</span>
              <Button size="sm" variant="outline">Small (h-9)</Button>
              <Button size="default" variant="outline">Default (h-10)</Button>
              <Button size="lg" variant="outline">Large (h-11)</Button>
            </div>
          </div>
        </section>

        <!-- ================= SECTION 06: FORM CONTROLS - INPUTS ================= -->
        <section id="controls-inputs" class="space-y-4 scroll-mt-24">
          <div class="border-b border-border pb-2">
            <h2 class="text-lg font-bold flex items-center gap-2">
              <Code2 class="h-4 w-4 text-primary" />
              <span>Inputs & Textarea (UI-01)</span>
            </h2>
            <p class="text-xs text-muted-foreground">
              Ô nhập liệu tiêu chuẩn với chiều cao h-10, bo góc 8px (<code>rounded-md</code>), hỗ trợ Password toggle và placeholder.
            </p>
          </div>

          <div class="p-5 rounded-lg border border-border bg-card grid grid-cols-1 md:grid-cols-2 gap-4 shadow-xs">
            <div class="space-y-1.5">
              <label class="text-xs font-semibold text-foreground">Họ và tên hành khách:</label>
              <Input v-model="sampleText" placeholder="Ví dụ: Nguyễn Văn An" />
            </div>
            <div class="space-y-1.5">
              <label class="text-xs font-semibold text-foreground">Mật khẩu tài khoản:</label>
              <PasswordInput v-model="samplePassword" placeholder="Nhập tối thiểu 8 ký tự" />
            </div>
            <div class="space-y-1.5 md:col-span-2">
              <label class="text-xs font-semibold text-foreground">Ghi chú chuyến xe:</label>
              <Textarea v-model="sampleTextarea" :rows="3" placeholder="Yêu cầu đón tại điểm hẹn, hành lý cồng kềnh..." />
            </div>
          </div>
        </section>

        <!-- ================= SECTION 07: FORM CONTROLS - SELECTION ================= -->
        <section id="controls-selection" class="space-y-4 scroll-mt-24">
          <div class="border-b border-border pb-2">
            <h2 class="text-lg font-bold flex items-center gap-2">
              <CheckCheck class="h-4 w-4 text-primary" />
              <span>Checkbox, Radio & Switch (UI-01: Geometry Preservation)</span>
            </h2>
            <p class="text-xs text-muted-foreground">
              Phân biệt rõ ràng giữa Checkbox vuông bo 4px (<code>rounded-[4px]</code>) và Radio tròn tuyệt đối (<code>rounded-full</code>).
            </p>
          </div>

          <div class="p-5 rounded-lg border border-border bg-card grid grid-cols-1 md:grid-cols-3 gap-6 shadow-xs">
            <!-- Checkbox -->
            <div class="space-y-3">
              <div class="font-bold text-xs text-foreground">Checkbox (Bo góc 4px):</div>
              <div class="flex items-center space-x-2">
                <Checkbox id="cb-1" v-model:checked="sampleCheckbox" />
                <label for="cb-1" class="text-xs text-foreground font-medium cursor-pointer">
                  Đồng ý điều khoản vận chuyển
                </label>
              </div>
              <div class="flex items-center space-x-2 opacity-50">
                <Checkbox id="cb-disabled" disabled />
                <label for="cb-disabled" class="text-xs text-muted-foreground">Vô hiệu hóa</label>
              </div>
            </div>

            <!-- Radio Group -->
            <div class="space-y-3">
              <div class="font-bold text-xs text-foreground">Radio Group (Tròn hoàn toàn):</div>
              <RadioGroup v-model="sampleRadio" class="space-y-2">
                <div class="flex items-center space-x-2">
                  <RadioGroupItem id="r-1" value="option-1" />
                  <label for="r-1" class="text-xs text-foreground font-medium cursor-pointer">Thanh toán qua Ví ký quỹ</label>
                </div>
                <div class="flex items-center space-x-2">
                  <RadioGroupItem id="r-2" value="option-2" />
                  <label for="r-2" class="text-xs text-foreground font-medium cursor-pointer">Cổng VNPay QR</label>
                </div>
              </RadioGroup>
            </div>

            <!-- Switch -->
            <div class="space-y-3">
              <div class="font-bold text-xs text-foreground">Switch (Pill shape):</div>
              <div class="flex items-center space-x-2">
                <Switch id="sw-1" v-model:checked="sampleSwitch" />
                <label for="sw-1" class="text-xs text-foreground font-medium cursor-pointer">
                  Nhận tin nhắn biến động vé qua Bot Telegram
                </label>
              </div>
            </div>
          </div>
        </section>

        <!-- ================= SECTION 08: CALENDAR INLINE ================= -->
        <section id="calendar-inline" class="space-y-4 scroll-mt-24">
          <div class="border-b border-border pb-2">
            <h2 class="text-lg font-bold flex items-center gap-2">
              <CalendarDays class="h-4 w-4 text-primary" />
              <span>Calendar Inline Primitive (UI-03: Locale vi-VN, Monday-first)</span>
            </h2>
            <p class="text-xs text-muted-foreground">
              Định dạng tiếng Việt chuẩn: Tuần bắt đầu từ <strong>Thứ Hai</strong> (T2), tiêu đề tháng tiếng Việt, bo góc ô ngày 6px (<code>rounded-[6px]</code>).
            </p>
          </div>

          <div class="p-6 rounded-lg border border-border bg-card flex flex-col items-center justify-center shadow-xs">
            <Calendar
              v-model="singleDate"
              :min-value="today(getLocalTimeZone())"
            />
            <div class="mt-4 text-xs text-muted-foreground">
              Ngày khởi hành đang chọn: <strong class="font-mono text-primary font-bold">{{ singleDate.toString() }}</strong>
            </div>
          </div>
        </section>

        <!-- ================= SECTION 09: DATEPICKER & TIMEPICKER ================= -->
        <section id="calendar-datepicker" class="space-y-4 scroll-mt-24">
          <div class="border-b border-border pb-2">
            <h2 class="text-lg font-bold flex items-center gap-2">
              <Clock class="h-4 w-4 text-primary" />
              <span>DatePicker Popover & Reusable TimePicker 24H (UI-04)</span>
            </h2>
            <p class="text-xs text-muted-foreground">
              TimePicker tái sử dụng chuẩn hóa: 1 icon Clock duy nhất (không lặp icon), định dạng 24H với quick presets xuất bến.
            </p>
          </div>

          <div class="p-6 rounded-lg border border-border bg-card shadow-xs">
            <DatePickerDemo />
          </div>
        </section>

        <!-- ================= SECTION 10: DATERANGEPICKER ================= -->
        <section id="calendar-range" class="space-y-4 scroll-mt-24">
          <div class="border-b border-border pb-2">
            <h2 class="text-lg font-bold flex items-center gap-2">
              <CalendarDays class="h-4 w-4 text-primary" />
              <span>DateRangePicker & Quick Presets (UI-14: Interactive Range)</span>
            </h2>
            <p class="text-xs text-muted-foreground">
              Lọc theo khoảng ngày (Hôm nay, 7 ngày tới, 30 ngày tới, Tháng này) với popover tương tác thực tế.
            </p>
          </div>

          <div class="p-6 rounded-lg border border-border bg-card shadow-xs">
            <DateRangePickerDemo />
          </div>
        </section>

        <!-- ================= SECTION 11: DATA TABLE ================= -->
        <section id="data-table" class="space-y-4 scroll-mt-24">
          <div class="border-b border-border pb-2">
            <h2 class="text-lg font-bold flex items-center gap-2">
              <Layers class="h-4 w-4 text-primary" />
              <span>TanStack Responsive DataTable & Row Density (UI-05, UI-06, UI-08)</span>
            </h2>
            <p class="text-xs text-muted-foreground">
              Bảng dữ liệu một đường viền chính (no nested cards), căn phải cột tiền tệ, số liệu tabular numbers và 3 mức mật độ (Compact 44px, Default 52px, Comfortable 60px).
            </p>
          </div>

          <DataTableDemo />
        </section>

        <!-- ================= SECTION 12: DATA PRIMITIVES ================= -->
        <section id="data-primitives" class="space-y-4 scroll-mt-24">
          <div class="border-b border-border pb-2">
            <h2 class="text-lg font-bold flex items-center gap-2">
              <Sparkles class="h-4 w-4 text-primary" />
              <span>Badges, Avatar & Separator Primitives</span>
            </h2>
            <p class="text-xs text-muted-foreground">
              Huy hiệu trạng thái vé xe (CONFIRMED, PENDING, REFUNDED, CANCELLED) và avatar định danh.
            </p>
          </div>

          <div class="p-5 rounded-lg border border-border bg-card space-y-4 shadow-xs">
            <div class="flex flex-wrap items-center gap-3">
              <StatusBadge status="CONFIRMED" />
              <StatusBadge status="PENDING" />
              <StatusBadge status="REFUNDED" />
              <StatusBadge status="CANCELLED" />
              <Badge variant="default">Default</Badge>
              <Badge variant="secondary">Secondary</Badge>
              <Badge variant="outline">Outline</Badge>
              <Badge variant="destructive">Destructive</Badge>
            </div>

            <Separator />

            <div class="flex items-center gap-4">
              <Avatar size="lg">
                <AvatarFallback>AD</AvatarFallback>
              </Avatar>
              <Avatar>
                <AvatarFallback>OP</AvatarFallback>
              </Avatar>
              <Avatar size="sm">
                <AvatarFallback>CU</AvatarFallback>
              </Avatar>
              <div class="text-xs text-muted-foreground">
                Avatar cho 3 phân vùng chính: <strong>Admin (AD)</strong>, <strong>Operator (OP)</strong>, <strong>Customer (CU)</strong>.
              </div>
            </div>
          </div>
        </section>

        <!-- ================= SECTION 13: NAVIGATION & DROPDOWNS ================= -->
        <section id="nav-dropdown" class="space-y-4 scroll-mt-24">
          <div class="border-b border-border pb-2">
            <h2 class="text-lg font-bold flex items-center gap-2">
              <LayoutGrid class="h-4 w-4 text-primary" />
              <span>Dropdown Menu System (UI-14: Interactive Menu States)</span>
            </h2>
            <p class="text-xs text-muted-foreground">
              Menu hồ sơ cá nhân, thao tác bản ghi và menu chọn cột bảng dữ liệu.
            </p>
          </div>

          <div class="p-6 rounded-lg border border-border bg-card shadow-xs">
            <DropdownDemo />
          </div>
        </section>

        <!-- ================= SECTION 14: ACCORDION & TABS ================= -->
        <section id="nav-accordion" class="space-y-4 scroll-mt-24">
          <div class="border-b border-border pb-2">
            <h2 class="text-lg font-bold flex items-center gap-2">
              <BookOpen class="h-4 w-4 text-primary" />
              <span>Accordion & Tabs Primitives</span>
            </h2>
            <p class="text-xs text-muted-foreground">
              Khối nội dung đóng/mở chuẩn FAQ chính sách hủy vé và chuyển tab danh mục.
            </p>
          </div>

          <div class="p-5 rounded-lg border border-border bg-card space-y-4 shadow-xs">
            <Accordion type="single" collapsible class="w-full">
              <AccordionItem value="item-1">
                <AccordionTrigger class="text-xs font-semibold">Chính sách hoàn tiền vé xe trước giờ khởi hành?</AccordionTrigger>
                <AccordionContent class="text-xs text-muted-foreground">
                  Hành khách được hoàn 100% tiền vé nếu hủy trước 24 giờ. Hủy trong vòng 12-24 giờ khấu trừ 20% phí sàn.
                </AccordionContent>
              </AccordionItem>
              <AccordionItem value="item-2">
                <AccordionTrigger class="text-xs font-semibold">Cơ chế bảo chứng thanh toán qua Ví ký quỹ Escrow?</AccordionTrigger>
                <AccordionContent class="text-xs text-muted-foreground">
                  Doanh thu vé xe được tạm khóa tại ví Escrow và chỉ giải ngân cho Nhà xe sau khi chuyến đi hoàn thành 60 phút.
                </AccordionContent>
              </AccordionItem>
            </Accordion>
          </div>
        </section>

        <!-- ================= SECTION 15: OVERLAYS & FEEDBACK ================= -->
        <section id="feedback-modals" class="space-y-4 scroll-mt-24">
          <div class="border-b border-border pb-2">
            <h2 class="text-lg font-bold flex items-center gap-2">
              <BellRing class="h-4 w-4 text-primary" />
              <span>Feedback Modals, Alerts & Drawer (UI-14: Open Dialog States)</span>
            </h2>
            <p class="text-xs text-muted-foreground">
              Vue Sonner Toasts, Dialog xác nhận, AlertDialog thu hồi vé và Sheet Drawer 520px tương tác thực tế.
            </p>
          </div>

          <div class="p-6 rounded-lg border border-border bg-card shadow-xs">
            <FeedbackDemo />
          </div>
        </section>
      </main>
    </div>

    <!-- Theme Customizer Drawer (Controlled from Header or custom trigger) -->
    <ThemeCustomizer
      v-model="isCustomizerOpen"
      :show-floating-trigger="false"
    />
  </div>
</template>
