<script setup lang="ts">
import { ref } from 'vue'
import { Button } from '@/components/ui/button'
import { Input, PasswordInput } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Badge, StatusBadge } from '@/components/ui/badge'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card'
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
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'
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
  CheckCheck
} from 'lucide-vue-next'
import { today, getLocalTimeZone } from '@internationalized/date'
import type { DateValue } from 'reka-ui'

const activeTab = ref('foundations')
const { isDark, setThemeMode } = useTheme()

// Form controls states
const sampleText = ref('')
const samplePassword = ref('')
const sampleTextarea = ref('')
const sampleSelect = ref('express')
const sampleSwitch = ref(true)
const sampleCheckbox = ref(true)
const sampleRadio = ref('option-1')
const sampleToggle = ref(false)
const sampleToggleGroup = ref('day')

// Calendar sample state
const singleDate = ref<any>(today(getLocalTimeZone()))
</script>

<template>
  <div class="min-h-screen bg-background text-foreground transition-colors duration-200">
    <!-- Showcase Top Header -->
    <header class="border-b bg-card/80 backdrop-blur sticky top-0 z-30">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="h-9 w-9 rounded-lg bg-primary flex items-center justify-center text-primary-foreground shadow-sm">
            <Bus class="h-5 w-5" />
          </div>
          <div>
            <div class="flex items-center gap-2">
              <span class="font-bold text-base tracking-tight">VeloBus Design System</span>
              <Badge variant="outline" class="text-[11px] font-mono border-primary/40 text-primary">
                Claude+ V2.1
              </Badge>
            </div>
            <p class="text-xs text-muted-foreground hidden sm:block">
              Warm Minimalist Enterprise Mobility SaaS Documentation & Component Showcase
            </p>
          </div>
        </div>

        <div class="flex items-center gap-2">
          <Button
            variant="ghost"
            size="icon"
            class="h-8 w-8 text-muted-foreground hover:text-foreground"
            :title="isDark ? 'Chuyển chế độ sáng' : 'Chuyển chế độ tối'"
            @click="setThemeMode(isDark ? 'light' : 'dark')"
          >
            <Sun v-if="isDark" class="h-4 w-4" />
            <Moon v-else class="h-4 w-4" />
          </Button>
          <Badge variant="secondary" class="font-mono text-xs">
            Gate A Review
          </Badge>
        </div>
      </div>
    </header>

    <!-- Main Content Container with Sidebar Layout -->
    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- Breadcrumb & Page Header -->
      <div class="mb-6 space-y-1">
        <div class="flex items-center gap-2 text-xs text-muted-foreground">
          <span>Tài Liệu Thiết Kế</span>
          <span>/</span>
          <span class="font-semibold text-foreground">Component Showcase</span>
        </div>
        <h1 class="text-2xl font-bold tracking-tight">Thư Viện Giao Diện & Component Showcase</h1>
        <p class="text-xs text-muted-foreground">
          Toàn bộ các component chính thức tương thích Shadcn Vue, Reka UI và bảng màu Warm Minimalist Claude+ được bảo toàn 100%.
        </p>
      </div>

      <!-- Navigation Tabs by Component Groups -->
      <Tabs v-model="activeTab" class="w-full space-y-6">
        <TabsList class="flex flex-wrap items-center gap-1.5 w-full h-auto p-1.5 bg-muted/60 rounded-xl overflow-x-auto">
          <TabsTrigger value="foundations" class="text-xs py-2 px-3 gap-1.5 rounded-lg">
            <Palette class="h-3.5 w-3.5" />
            <span>Foundations & Tokens</span>
          </TabsTrigger>
          <TabsTrigger value="icons" class="text-xs py-2 px-3 gap-1.5 rounded-lg">
            <ShieldCheck class="h-3.5 w-3.5" />
            <span>Icon Registry</span>
          </TabsTrigger>
          <TabsTrigger value="controls" class="text-xs py-2 px-3 gap-1.5 rounded-lg">
            <LayoutGrid class="h-3.5 w-3.5" />
            <span>Form Controls</span>
          </TabsTrigger>
          <TabsTrigger value="calendar" class="text-xs py-2 px-3 gap-1.5 rounded-lg">
            <CalendarDays class="h-3.5 w-3.5" />
            <span>Calendar & Date/Time</span>
          </TabsTrigger>
          <TabsTrigger value="tables" class="text-xs py-2 px-3 gap-1.5 rounded-lg">
            <Layers class="h-3.5 w-3.5" />
            <span>Data Display & Tables</span>
          </TabsTrigger>
          <TabsTrigger value="feedback" class="text-xs py-2 px-3 gap-1.5 rounded-lg">
            <BellRing class="h-3.5 w-3.5" />
            <span>Feedback & Overlays</span>
          </TabsTrigger>
        </TabsList>

        <!-- ================= GROUP 01: FOUNDATIONS & TOKENS ================= -->
        <TabsContent value="foundations" class="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle class="text-base flex items-center gap-2">
                <Palette class="h-4 w-4 text-primary" />
                <span>Bảng Màu Chuẩn Claude+ (Canonical OKLCH Token System)</span>
              </CardTitle>
              <CardDescription>
                Bảo toàn 100% token gốc: Terracotta Primary oklch(0.6171 0.1375 39.0427) và Warm Light Background oklch(0.9818 0.0054 95.0986).
              </CardDescription>
            </CardHeader>
            <CardContent class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              <!-- Swatch 1 -->
              <div class="p-4 rounded-xl border border-border bg-card space-y-2 shadow-xs">
                <div class="h-16 rounded-lg bg-primary flex items-center justify-center text-primary-foreground font-mono text-xs font-bold shadow-inner">
                  Primary #C96442
                </div>
                <div class="text-xs space-y-0.5">
                  <div class="font-bold text-foreground">Canonical Primary</div>
                  <div class="text-[11px] font-mono text-muted-foreground">oklch(0.6171 0.1375 39.0427)</div>
                </div>
              </div>

              <!-- Swatch 2 -->
              <div class="p-4 rounded-xl border border-border bg-card space-y-2 shadow-xs">
                <div class="h-16 rounded-lg bg-secondary flex items-center justify-center text-secondary-foreground font-mono text-xs font-bold shadow-inner border border-border">
                  Secondary
                </div>
                <div class="text-xs space-y-0.5">
                  <div class="font-bold text-foreground">Neutral Secondary</div>
                  <div class="text-[11px] font-mono text-muted-foreground">Light 98% / Dark 32% Lightness</div>
                </div>
              </div>

              <!-- Swatch 3 -->
              <div class="p-4 rounded-xl border border-border bg-card space-y-2 shadow-xs">
                <div class="h-16 rounded-lg bg-muted flex items-center justify-center text-muted-foreground font-mono text-xs font-bold shadow-inner border border-border">
                  Muted
                </div>
                <div class="text-xs space-y-0.5">
                  <div class="font-bold text-foreground">Muted Surface</div>
                  <div class="text-[11px] font-mono text-muted-foreground">Nền thứ cấp cho toolbar & table header</div>
                </div>
              </div>

              <!-- Swatch 4 -->
              <div class="p-4 rounded-xl border border-border bg-card space-y-2 shadow-xs">
                <div class="h-16 rounded-lg bg-destructive flex items-center justify-center text-destructive-foreground font-mono text-xs font-bold shadow-inner">
                  Destructive
                </div>
                <div class="text-xs space-y-0.5">
                  <div class="font-bold text-foreground">Destructive / Error</div>
                  <div class="text-[11px] font-mono text-muted-foreground">oklch(0.577 0.245 27.325)</div>
                </div>
              </div>
            </CardContent>
          </Card>

          <!-- Typography & Geometry Card -->
          <Card>
            <CardHeader>
              <CardTitle class="text-base flex items-center gap-2">
                <Type class="h-4 w-4 text-primary" />
                <span>Quy Chuẩn Typography & Border Radius</span>
              </CardTitle>
              <CardDescription>Hệ thống phân cấp kiểu chữ chuẩn SaaS di động và bo góc ấm áp (Radius 0.75rem / 12px).</CardDescription>
            </CardHeader>
            <CardContent class="grid grid-cols-1 md:grid-cols-2 gap-6 text-xs">
              <div class="space-y-3">
                <div class="text-2xl font-black text-foreground">Heading 1 — 24px Bold (Title)</div>
                <div class="text-xl font-bold text-foreground">Heading 2 — 20px Bold (Section)</div>
                <div class="text-base font-bold text-foreground">Heading 3 — 16px Bold (Card Title)</div>
                <div class="text-sm font-semibold text-foreground">Body Regular — 14px Medium (Default Text)</div>
                <div class="text-xs text-muted-foreground">Caption / Subtitle — 12px Regular (Helper / Timestamps)</div>
                <div class="text-[11px] font-mono text-primary font-bold">Monospace Tabular — 11px (Mã vé & Tiền tệ)</div>
              </div>

              <div class="space-y-3">
                <h4 class="font-bold text-foreground">Avatar & Separator Primitives:</h4>
                <div class="flex items-center gap-3">
                  <Avatar size="lg">
                    <AvatarFallback>AD</AvatarFallback>
                  </Avatar>
                  <Avatar>
                    <AvatarFallback>OP</AvatarFallback>
                  </Avatar>
                  <Avatar size="sm">
                    <AvatarFallback>CU</AvatarFallback>
                  </Avatar>
                </div>
                <Separator class="my-3" />
                <p class="text-muted-foreground leading-relaxed">
                  Đường phân cách (Separator) dùng token <code>bg-border</code> mảnh 1px phân định rõ ràng giữa các phân vùng nội dung mà không gây rối mắt.
                </p>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <!-- ================= GROUP 02: ICON REGISTRY ================= -->
        <TabsContent value="icons" class="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle class="text-base flex items-center gap-2">
                <ShieldCheck class="h-4 w-4 text-primary" />
                <span>Bảng Tra Cứu Biểu Tượng Chuẩn (Lucide Icon Registry)</span>
              </CardTitle>
              <CardDescription>
                Duy nhất sử dụng <code>lucide-vue-next</code>. Tuyệt đối không dùng emoji, PrimeIcons hay SVG tự vẽ trong giao diện.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <IconRegistryDemo />
            </CardContent>
          </Card>
        </TabsContent>

        <!-- ================= GROUP 03: FORM CONTROLS & DROPDOWNS ================= -->
        <TabsContent value="controls" class="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle class="text-base flex items-center gap-2">
                <LayoutGrid class="h-4 w-4 text-primary" />
                <span>Form Controls & Interactive Toggles</span>
              </CardTitle>
              <CardDescription>Hệ thống input, select, switches, toggles và accordion chính thức từ Reka UI.</CardDescription>
            </CardHeader>
            <CardContent class="grid grid-cols-1 md:grid-cols-2 gap-6 text-xs">
              <!-- Column 1: Inputs & Select -->
              <div class="space-y-4">
                <div class="space-y-1.5">
                  <label class="font-bold text-foreground">Họ và tên hành khách:</label>
                  <Input v-model="sampleText" placeholder="VD: Nguyễn Văn A..." />
                </div>

                <div class="space-y-1.5">
                  <label class="font-bold text-foreground">Mật khẩu bảo mật:</label>
                  <PasswordInput v-model="samplePassword" placeholder="Tối thiểu 6 ký tự..." />
                </div>

                <div class="space-y-1.5">
                  <label class="font-bold text-foreground">Ghi chú chuyến xe:</label>
                  <Textarea v-model="sampleTextarea" :rows="3" placeholder="Nhập yêu cầu đón trả tận nơi..." />
                </div>

                <div class="space-y-1.5">
                  <label class="font-bold text-foreground">Hạng xe đăng ký:</label>
                  <Select v-model="sampleSelect">
                    <SelectTrigger>
                      <SelectValue placeholder="Chọn hạng xe" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="express">Limousine 22 phòng VIP</SelectItem>
                      <SelectItem value="sleeper">Giường nằm cao cấp 34 chỗ</SelectItem>
                      <SelectItem value="seat">Ghế ngồi 29 chỗ</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <!-- Column 2: Checkbox, Radio, Toggles, Accordion -->
              <div class="space-y-4">
                <div class="space-y-2">
                  <label class="font-bold text-foreground">Tùy chọn bổ sung & Switch:</label>
                  <div class="flex items-center space-x-2">
                    <Switch v-model:checked="sampleSwitch" id="sw-showcase" />
                    <label for="sw-showcase" class="cursor-pointer text-muted-foreground font-medium">
                      Nhận thông báo biến động vé qua Telegram Bot
                    </label>
                  </div>
                  <div class="flex items-center space-x-2 pt-1">
                    <Checkbox v-model:checked="sampleCheckbox" id="cb-showcase" />
                    <label for="cb-showcase" class="cursor-pointer text-muted-foreground font-medium">
                      Tôi đồng ý với điều khoản vận chuyển hành khách VeloBus
                    </label>
                  </div>
                </div>

                <div class="space-y-2 pt-1">
                  <label class="font-bold text-foreground">Toggle Group (Xem theo ca / ngày):</label>
                  <ToggleGroup v-model="sampleToggleGroup" type="single">
                    <ToggleGroupItem value="day">Hôm nay</ToggleGroupItem>
                    <ToggleGroupItem value="week">Tuần này</ToggleGroupItem>
                    <ToggleGroupItem value="month">Tháng này</ToggleGroupItem>
                  </ToggleGroup>
                </div>

                <div class="pt-2">
                  <Accordion type="single" collapsible class="w-full">
                    <AccordionItem value="item-1">
                      <AccordionTrigger>Quy định về thời gian giữ chỗ 10 phút?</AccordionTrigger>
                      <AccordionContent>
                        Hệ thống tự động kích hoạt đếm ngược 10 phút trên Redis khi bạn chọn ghế. Nếu quá thời hạn mà chưa hoàn tất thanh toán, ghế sẽ tự động giải phóng.
                      </AccordionContent>
                    </AccordionItem>
                    <AccordionItem value="item-2">
                      <AccordionTrigger>Chính sách hoàn tiền cưỡng chế của Admin?</AccordionTrigger>
                      <AccordionContent>
                        Admin Backoffice có quyền thực thi lệnh hoàn vé 100% trong trường hợp nhà xe hủy chuyến đột xuất, khấu trừ tự động từ ví ký quỹ đối tác.
                      </AccordionContent>
                    </AccordionItem>
                  </Accordion>
                </div>
              </div>
            </CardContent>
          </Card>

          <!-- Dropdown Menus Card -->
          <Card>
            <CardHeader>
              <CardTitle class="text-base flex items-center gap-2">
                <LayoutGrid class="h-4 w-4 text-primary" />
                <span>Dropdown Menu System (Menu cá nhân, Ẩn/Hiện cột & Thao tác dữ liệu)</span>
              </CardTitle>
              <CardDescription>
                Dropdown menu chuẩn Shadcn Vue hỗ trợ phím tắt, checkbox items và phân cách nhóm chức năng.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <DropdownDemo />
            </CardContent>
          </Card>
        </TabsContent>

        <!-- ================= GROUP 04: CALENDAR & DATE/TIME ================= -->
        <TabsContent value="calendar" class="space-y-6">
          <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <!-- Inline Calendar Primitive -->
            <Card>
              <CardHeader>
                <CardTitle class="text-base flex items-center gap-2">
                  <CalendarDays class="h-4 w-4 text-primary" />
                  <span>Calendar Primitive (Inline Mode)</span>
                </CardTitle>
                <CardDescription>
                  Dựa trên <code>reka-ui</code> và <code>@internationalized/date</code>, tuần bắt đầu từ Thứ Hai, định dạng ngày Việt Nam chuẩn.
                </CardDescription>
              </CardHeader>
              <CardContent class="flex flex-col items-center justify-center p-4">
                <Calendar
                  v-model="singleDate"
                  :min-value="today(getLocalTimeZone())"
                />
                <div class="mt-4 text-xs text-muted-foreground">
                  Ngày đang chọn: <strong class="font-mono text-primary">{{ singleDate.toString() }}</strong>
                </div>
              </CardContent>
            </Card>

            <!-- Date Picker & Time Picker Composition -->
            <Card>
              <CardHeader>
                <CardTitle class="text-base flex items-center gap-2">
                  <CalendarDays class="h-4 w-4 text-primary" />
                  <span>DatePicker & DateRangePicker (Popover Mode)</span>
                </CardTitle>
                <CardDescription>
                  Ứng dụng trong bộ lọc tìm kiếm chuyến xe, lịch báo cáo và thời gian xuất bến.
                </CardDescription>
              </CardHeader>
              <CardContent class="space-y-6">
                <DatePickerDemo />
                <Separator />
                <DateRangePickerDemo />
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <!-- ================= GROUP 05: TABLES & DATA DISPLAY ================= -->
        <TabsContent value="tables" class="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle class="text-base flex items-center gap-2">
                <Layers class="h-4 w-4 text-primary" />
                <span>TanStack / Responsive DataTable System</span>
              </CardTitle>
              <CardDescription>
                Bảng dữ liệu responsive với tìm kiếm, lọc trạng thái, sắp xếp số tiền và chọn hàng loạt (Row Selection).
              </CardDescription>
            </CardHeader>
            <CardContent>
              <DataTableDemo />
            </CardContent>
          </Card>
        </TabsContent>

        <!-- ================= GROUP 06: FEEDBACK & OVERLAYS ================= -->
        <TabsContent value="feedback" class="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle class="text-base flex items-center gap-2">
                <BellRing class="h-4 w-4 text-primary" />
                <span>Centralized Feedback System & Modals</span>
              </CardTitle>
              <CardDescription>
                Vue Sonner Toasts, Dialog xác nhận, AlertDialog thu hồi vé và Sheet Drawer 520px.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <FeedbackDemo />
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </main>

    <!-- Floating Theme Customizer Component -->
    <ThemeCustomizer />
  </div>
</template>
