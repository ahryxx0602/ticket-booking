<script setup lang="ts">
import { ref } from 'vue'
import { Button } from '@/components/ui/button'
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Dialog,
  DialogTrigger,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
  DialogClose
} from '@/components/ui/dialog'
import {
  AlertDialog,
  AlertDialogTrigger,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogAction,
  AlertDialogCancel
} from '@/components/ui/alert-dialog'
import {
  Sheet,
  SheetTrigger,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
  SheetFooter,
  SheetClose
} from '@/components/ui/sheet'
import {
  CircleCheck,
  AlertTriangle,
  CircleX,
  Info,
  LoaderCircle,
  BellRing,
  Trash2
} from 'lucide-vue-next'
import { toast } from 'vue-sonner'

function triggerToast(type: 'success' | 'error' | 'warning' | 'info') {
  switch (type) {
    case 'success':
      toast.success('Thanh toán đơn vé thành công!', {
        description: 'Mã vé VLB-9988 đã được gửi vào SMS và Email của hành khách.'
      })
      break
    case 'error':
      toast.error('Không thể hoàn tiền cưỡng chế!', {
        description: 'Số dư ví ký quỹ của nhà xe không đủ để khấu trừ (tối thiểu 850.000 đ).'
      })
      break
    case 'warning':
      toast.warning('Phiên giữ chỗ sắp hết hạn!', {
        description: 'Bạn còn 2 phút để hoàn tất thông tin thanh toán trước khi ghế bị giải phóng.'
      })
      break
    case 'info':
      toast.info('Cập nhật lộ trình chuyến xe', {
        description: 'Xe xuất bến lúc 22:30 tại Bến xe Miền Đông mới.'
      })
      break
  }
}
</script>

<template>
  <div class="space-y-6">
    <!-- Sonner Toasts Grid -->
    <div class="p-4 rounded-xl border border-border bg-card space-y-3">
      <div class="space-y-1">
        <h3 class="text-sm font-bold text-foreground flex items-center gap-2">
          <BellRing class="h-4 w-4 text-primary" />
          <span>Centralized Toast Notifications (Vue Sonner)</span>
        </h3>
        <p class="text-xs text-muted-foreground">
          Bấm các nút dưới đây để kích hoạt toast thông báo chuẩn hệ thống:
        </p>
      </div>

      <div class="flex flex-wrap gap-2.5">
        <Button variant="default" size="sm" class="gap-1.5" @click="triggerToast('success')">
          <CircleCheck class="h-4 w-4" />
          <span>Toast Thành Công</span>
        </Button>
        <Button variant="destructive" size="sm" class="gap-1.5" @click="triggerToast('error')">
          <CircleX class="h-4 w-4" />
          <span>Toast Lỗi Hệ Thống</span>
        </Button>
        <Button variant="outline" size="sm" class="gap-1.5" @click="triggerToast('warning')">
          <AlertTriangle class="h-4 w-4 text-amber-500" />
          <span>Toast Cảnh Báo</span>
        </Button>
        <Button variant="secondary" size="sm" class="gap-1.5" @click="triggerToast('info')">
          <Info class="h-4 w-4 text-sky-500" />
          <span>Toast Thông Tin</span>
        </Button>
      </div>
    </div>

    <!-- Modals & Overlays -->
    <div class="p-4 rounded-xl border border-border bg-card space-y-3">
      <div class="space-y-1">
        <h3 class="text-sm font-bold text-foreground">Hộp Thoại & Ngăn Kéo (Dialog, AlertDialog, Sheet)</h3>
        <p class="text-xs text-muted-foreground">Các mẫu overlay tiêu chuẩn theo Shadcn Vue & Reka UI:</p>
      </div>

      <div class="flex flex-wrap gap-3">
        <!-- Standard Dialog -->
        <Dialog>
          <DialogTrigger as-child>
            <Button variant="outline" size="sm">Mở Dialog Chuẩn</Button>
          </DialogTrigger>
          <DialogContent class="max-w-md">
            <DialogHeader>
              <DialogTitle>Xác Nhận Giữ Chỗ Chuyến Xe</DialogTitle>
              <DialogDescription>
                Hệ thống sẽ giữ chỗ 2 ghế của bạn trong vòng 10 phút trên cụm Redis cache.
              </DialogDescription>
            </DialogHeader>
            <div class="py-3 text-xs text-muted-foreground">
              Vui lòng không đóng trình duyệt trong khi giao dịch đang được xử lý.
            </div>
            <DialogFooter>
              <DialogClose as-child>
                <Button variant="outline" size="sm">Đóng lại</Button>
              </DialogClose>
              <Button size="sm">Tiếp tục thanh toán</Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        <!-- Alert Dialog -->
        <AlertDialog>
          <AlertDialogTrigger as-child>
            <Button variant="destructive" size="sm" class="gap-1.5">
              <Trash2 class="h-4 w-4" />
              <span>Mở AlertDialog Hủy Đơn</span>
            </Button>
          </AlertDialogTrigger>
          <AlertDialogContent class="max-w-md">
            <AlertDialogHeader>
              <AlertDialogTitle class="text-destructive">Bạn có chắc chắn muốn hủy đơn vé này?</AlertDialogTitle>
              <AlertDialogDescription>
                Hành động này không thể hoàn tác. Vé sẽ bị thu hồi và hoàn trả vào kho vé trống.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>Giữ lại vé</AlertDialogCancel>
              <AlertDialogAction class="bg-destructive text-destructive-foreground hover:bg-destructive/90">
                Xác nhận hủy vé
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>

        <!-- Sheet Drawer -->
        <Sheet>
          <SheetTrigger as-child>
            <Button variant="secondary" size="sm">Mở Sheet Drawer (520px)</Button>
          </SheetTrigger>
          <SheetContent side="right">
            <SheetHeader>
              <SheetTitle>Chi Tiết Hồ Sơ Đối Tác</SheetTitle>
              <SheetDescription>
                Xem và chỉnh sửa thông tin nhà xe, tỷ lệ hoa hồng và tài khoản ngân hàng.
              </SheetDescription>
            </SheetHeader>
            <div class="py-4 space-y-3 text-xs">
              <div class="p-3 bg-muted/40 rounded-lg space-y-2 border border-border">
                <div class="flex justify-between">
                  <span class="text-muted-foreground">Tên nhà xe:</span>
                  <span class="font-bold text-foreground">Phương Trang FUTA Bus Lines</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-muted-foreground">Tỷ lệ hoa hồng:</span>
                  <span class="font-mono font-bold text-primary">8.5%</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-muted-foreground">Số dư ví ký quỹ:</span>
                  <span class="font-mono font-bold text-foreground">45.000.000 đ</span>
                </div>
              </div>
            </div>
            <SheetFooter>
              <SheetClose as-child>
                <Button size="sm">Đóng ngăn kéo</Button>
              </SheetClose>
            </SheetFooter>
          </SheetContent>
        </Sheet>
      </div>
    </div>

    <!-- Inline Alerts & Skeletons -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div class="p-4 rounded-xl border border-border bg-card space-y-3">
        <h4 class="text-xs font-bold text-foreground">Inline Alerts</h4>
        <Alert>
          <Info class="h-4 w-4 text-primary" />
          <AlertTitle>Lưu ý thời gian xuất bến</AlertTitle>
          <AlertDescription class="text-xs">
            Hành khách vui lòng có mặt trước 30 phút để hoàn tất thủ tục lên xe.
          </AlertDescription>
        </Alert>

        <Alert variant="destructive">
          <AlertTriangle class="h-4 w-4 text-destructive" />
          <AlertTitle>Chuyến xe đã bị hủy</AlertTitle>
          <AlertDescription class="text-xs">
            Chuyến xe này đã bị hủy do điều kiện thời tiết xấu. Toàn bộ tiền vé đã được hoàn.
          </AlertDescription>
        </Alert>
      </div>

      <div class="p-4 rounded-xl border border-border bg-card space-y-3">
        <h4 class="text-xs font-bold text-foreground">Loading Skeletons</h4>
        <div class="space-y-3 pt-1">
          <div class="flex items-center space-x-3">
            <Skeleton class="h-10 w-10 rounded-full" />
            <div class="space-y-1.5 flex-1">
              <Skeleton class="h-3.5 w-3/4" />
              <Skeleton class="h-3 w-1/2" />
            </div>
          </div>
          <Skeleton class="h-16 w-full rounded-xl" />
        </div>
      </div>
    </div>
  </div>
</template>
