<script setup lang="ts">
import { ref } from 'vue'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuCheckboxItem,
  DropdownMenuShortcut
} from '@/components/ui/dropdown-menu'
import {
  MoreHorizontal,
  User,
  CreditCard,
  Settings,
  LogOut,
  SlidersHorizontal,
  Download,
  Trash2,
  FileSpreadsheet
} from 'lucide-vue-next'

const showPhone = ref(true)
const showEmail = ref(true)
const showStatus = ref(false)
</script>

<template>
  <div class="space-y-4">
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <!-- 1. User Profile Dropdown -->
      <div class="p-4 rounded-xl border border-border bg-card space-y-2">
        <div class="text-xs font-bold text-foreground">User Menu Dropdown:</div>
        <DropdownMenu>
          <DropdownMenuTrigger as-child>
            <Button variant="outline" size="sm" class="w-full justify-between">
              <span class="flex items-center gap-2">
                <User class="h-4 w-4 text-primary" />
                <span>Tài Khoản Quản Trị</span>
              </span>
              <MoreHorizontal class="h-4 w-4 text-muted-foreground" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent class="w-56" align="start">
            <DropdownMenuLabel>Tài khoản của tôi</DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem>
              <User class="mr-2 h-4 w-4" />
              <span>Hồ sơ cá nhân</span>
              <DropdownMenuShortcut>⇧⌘P</DropdownMenuShortcut>
            </DropdownMenuItem>
            <DropdownMenuItem>
              <CreditCard class="mr-2 h-4 w-4" />
              <span>Ví ký quỹ & Doanh thu</span>
              <DropdownMenuShortcut>⌘B</DropdownMenuShortcut>
            </DropdownMenuItem>
            <DropdownMenuItem>
              <Settings class="mr-2 h-4 w-4" />
              <span>Cài đặt hệ thống</span>
              <DropdownMenuShortcut>⌘S</DropdownMenuShortcut>
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem class="text-destructive focus:text-destructive">
              <LogOut class="mr-2 h-4 w-4" />
              <span>Đăng xuất</span>
              <DropdownMenuShortcut>⇧⌘Q</DropdownMenuShortcut>
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      <!-- 2. Column Visibility Dropdown -->
      <div class="p-4 rounded-xl border border-border bg-card space-y-2">
        <div class="text-xs font-bold text-foreground">Column Visibility Dropdown:</div>
        <DropdownMenu>
          <DropdownMenuTrigger as-child>
            <Button variant="outline" size="sm" class="w-full justify-between">
              <span class="flex items-center gap-2">
                <SlidersHorizontal class="h-4 w-4 text-primary" />
                <span>Ẩn/Hiện cột bảng dữ liệu</span>
              </span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent class="w-52" align="start">
            <DropdownMenuLabel>Tùy chỉnh cột hiển thị</DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuCheckboxItem v-model:checked="showPhone">
              Số điện thoại khách
            </DropdownMenuCheckboxItem>
            <DropdownMenuCheckboxItem v-model:checked="showEmail">
              Địa chỉ Email
            </DropdownMenuCheckboxItem>
            <DropdownMenuCheckboxItem v-model:checked="showStatus">
              Trạng thái đối soát
            </DropdownMenuCheckboxItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      <!-- 3. Actions / Export Dropdown -->
      <div class="p-4 rounded-xl border border-border bg-card space-y-2">
        <div class="text-xs font-bold text-foreground">Table Actions Dropdown:</div>
        <DropdownMenu>
          <DropdownMenuTrigger as-child>
            <Button variant="default" size="sm" class="w-full justify-between">
              <span class="flex items-center gap-2">
                <Download class="h-4 w-4" />
                <span>Thao Tác Dữ Liệu</span>
              </span>
              <MoreHorizontal class="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent class="w-52" align="end">
            <DropdownMenuLabel>Xuất dữ liệu & Tác vụ</DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem>
              <FileSpreadsheet class="mr-2 h-4 w-4 text-emerald-600" />
              <span>Xuất file Excel (.xlsx)</span>
            </DropdownMenuItem>
            <DropdownMenuItem>
              <Download class="mr-2 h-4 w-4 text-sky-600" />
              <span>Tải báo cáo PDF</span>
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem class="text-destructive focus:text-destructive">
              <Trash2 class="mr-2 h-4 w-4" />
              <span>Hủy hàng loạt đơn</span>
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </div>
  </div>
</template>
