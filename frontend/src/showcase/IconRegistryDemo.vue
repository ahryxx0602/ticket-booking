<script setup lang="ts">
import { ref, computed } from 'vue'
import { Input } from '@/components/ui/input'
import {
  LayoutDashboard,
  Route,
  BusFront,
  ContactRound,
  CalendarDays,
  Ticket,
  Users,
  ShieldCheck,
  Wallet,
  CreditCard,
  Banknote,
  Settings,
  Plus,
  Pencil,
  Trash2,
  Eye,
  Search,
  Filter,
  Ellipsis,
  Download,
  Upload,
  CircleCheck,
  CircleX,
  TriangleAlert,
  Info,
  LoaderCircle,
  Calendar,
  Clock,
  MapPin,
  ArrowLeftRight,
  Armchair,
  Sun,
  Moon,
  Monitor,
  Palette
} from 'lucide-vue-next'

const icons = [
  { name: 'LayoutDashboard', component: LayoutDashboard, category: 'Navigation', usage: 'Dashboard quản trị / đối tác' },
  { name: 'Route', component: Route, category: 'Mobility', usage: 'Tuyến đường liên tỉnh' },
  { name: 'BusFront', component: BusFront, category: 'Mobility', usage: 'Đội xe vận tải' },
  { name: 'ContactRound', component: ContactRound, category: 'Management', usage: 'Tài xế & phụ xe' },
  { name: 'CalendarDays', component: CalendarDays, category: 'Schedule', usage: 'Lịch xuất bến & phân ca' },
  { name: 'Ticket', component: Ticket, category: 'Booking', usage: 'Đơn vé & đặt chỗ' },
  { name: 'Users', component: Users, category: 'Management', usage: 'Quản lý tài khoản' },
  { name: 'ShieldCheck', component: ShieldCheck, category: 'Security', usage: 'Phân quyền Role & bảo mật' },
  { name: 'Wallet', component: Wallet, category: 'Fintech', usage: 'Ví ký quỹ Escrow nhà xe' },
  { name: 'CreditCard', component: CreditCard, category: 'Fintech', usage: 'Cổng thanh toán VNPay' },
  { name: 'Banknote', component: Banknote, category: 'Fintech', usage: 'Lệnh rút tiền & đối soát' },
  { name: 'Settings', component: Settings, category: 'System', usage: 'Tham số cấu hình sàn' },
  { name: 'Plus', component: Plus, category: 'Actions', usage: 'Thêm mới dữ liệu' },
  { name: 'Pencil', component: Pencil, category: 'Actions', usage: 'Chỉnh sửa bản ghi' },
  { name: 'Trash2', component: Trash2, category: 'Actions', usage: 'Xóa bản ghi' },
  { name: 'Eye', component: Eye, category: 'Actions', usage: 'Xem chi tiết' },
  { name: 'Search', component: Search, category: 'Actions', usage: 'Tra cứu & tìm kiếm' },
  { name: 'Filter', component: Filter, category: 'Actions', usage: 'Lọc dữ liệu bảng' },
  { name: 'Ellipsis', component: Ellipsis, category: 'Actions', usage: 'Menu thao tác phụ' },
  { name: 'Download', component: Download, category: 'Data', usage: 'Xuất file báo cáo Excel/CSV' },
  { name: 'Upload', component: Upload, category: 'Data', usage: 'Tải tài liệu, hình ảnh' },
  { name: 'CircleCheck', component: CircleCheck, category: 'Feedback', usage: 'Thông báo thành công' },
  { name: 'CircleX', component: CircleX, category: 'Feedback', usage: 'Thông báo lỗi thất bại' },
  { name: 'TriangleAlert', component: TriangleAlert, category: 'Feedback', usage: 'Cảnh báo rủi ro' },
  { name: 'Info', component: Info, category: 'Feedback', usage: 'Thông tin hướng dẫn' },
  { name: 'LoaderCircle', component: LoaderCircle, category: 'Feedback', usage: 'Trạng thái đang tải (Loading)' },
  { name: 'Calendar', component: Calendar, category: 'Input', usage: 'Date picker' },
  { name: 'Clock', component: Clock, category: 'Input', usage: 'Time picker' },
  { name: 'MapPin', component: MapPin, category: 'Mobility', usage: 'Điểm đón / điểm trả' },
  { name: 'ArrowLeftRight', component: ArrowLeftRight, category: 'Mobility', usage: 'Hoán đổi điểm đi / đến' },
  { name: 'Armchair', component: Armchair, category: 'Booking', usage: 'Sơ đồ ghế ngồi' },
  { name: 'Sun', component: Sun, category: 'Theme', usage: 'Giao diện sáng (Light)' },
  { name: 'Moon', component: Moon, category: 'Theme', usage: 'Giao diện tối (Dark)' },
  { name: 'Monitor', component: Monitor, category: 'Theme', usage: 'Theo hệ điều hành (System)' },
  { name: 'Palette', component: Palette, category: 'Theme', usage: 'Tùy biến theme' }
]

const searchQuery = ref('')
const selectedCategory = ref('ALL')

const filteredIcons = computed(() => {
  return icons.filter(item => {
    const matchesSearch = item.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
                          item.usage.toLowerCase().includes(searchQuery.value.toLowerCase())
    const matchesCategory = selectedCategory.value === 'ALL' || item.category === selectedCategory.value
    return matchesSearch && matchesCategory
  })
})
</script>

<template>
  <div class="space-y-4">
    <!-- Search Bar & Category Filter -->
    <div class="flex flex-col sm:flex-row items-center gap-3">
      <div class="relative w-full sm:w-72">
        <Search class="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground pointer-events-none" />
        <Input
          v-model="searchQuery"
          placeholder="Tìm kiếm icon (tên hoặc mục đích)..."
          class="pl-9 h-9 text-xs"
        />
      </div>

      <div class="flex items-center gap-1.5 overflow-x-auto w-full pb-1 sm:pb-0">
        <button
          v-for="cat in ['ALL', 'Mobility', 'Fintech', 'Management', 'Actions', 'Feedback']"
          :key="cat"
          type="button"
          @click="selectedCategory = cat"
          class="px-2.5 py-1 rounded-md text-[11px] font-semibold border transition-all whitespace-nowrap cursor-pointer"
          :class="selectedCategory === cat ? 'bg-primary text-primary-foreground border-primary' : 'bg-card text-muted-foreground border-border hover:bg-muted'"
        >
          {{ cat }}
        </button>
      </div>
    </div>

    <!-- Icons Grid -->
    <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
      <div
        v-for="icon in filteredIcons"
        :key="icon.name"
        class="p-3 rounded-xl border border-border bg-card hover:border-primary/50 transition-all flex flex-col items-center text-center space-y-2 group shadow-xs"
      >
        <div class="h-10 w-10 rounded-lg bg-muted flex items-center justify-center text-foreground group-hover:text-primary transition-colors">
          <component :is="icon.component" class="h-5 w-5" />
        </div>
        <div class="space-y-0.5 w-full">
          <div class="font-mono text-xs font-bold text-foreground truncate" :title="icon.name">
            {{ icon.name }}
          </div>
          <div class="text-[10px] text-muted-foreground line-clamp-1" :title="icon.usage">
            {{ icon.usage }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
