<script setup>
import { ref } from 'vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import FormField from '@/shared/components/FormField.vue'
import AppButton from '@/shared/components/AppButton.vue'

// Active tab: 'USERS' | 'BOOKINGS'
const activeTab = ref('USERS')

// Users list (corresponds to API-ADM-USR-01..05)
// Core invariant: Admin cannot lock or demote their own account (isSelf: true)
const users = ref([
  {
    id: 'USR-ADMIN-01',
    username: 'admin',
    fullName: 'Quản Trị Viên Toàn Sàn',
    email: 'admin@velobus.vn',
    role: 'ROLE_ADMIN',
    status: 'ACTIVE',
    isSelf: true
  },
  {
    id: 'USR-OPER-01',
    username: 'operator1',
    fullName: 'Nguyễn Hoàng Long',
    email: 'hoanglong@velobus.vn',
    role: 'ROLE_OPERATOR',
    status: 'ACTIVE',
    isSelf: false
  },
  {
    id: 'USR-CUST-01',
    username: 'thanhpv',
    fullName: 'Phan Văn Thành',
    email: 'thanh.pv@gmail.com',
    role: 'ROLE_CUSTOMER',
    status: 'ACTIVE',
    isSelf: false
  }
])

function toggleUserLock(user) {
  if (user.isSelf) return // Invariant: ADMIN_CANNOT_LOCK_SELF
  user.status = user.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE'
}

// All Bookings list for Admin Refund (corresponds to API-ADM-BKG-01)
const allBookings = ref([
  {
    id: 'BKG-20261123-9988',
    customerName: 'Phan Văn Thành',
    customerPhone: '0912345678',
    route: 'Hà Nội → Đà Nẵng',
    operatorName: 'Hoàng Long Limousine',
    seats: ['A05', 'A06'],
    totalAmount: 700000,
    status: 'CONFIRMED',
    refundReason: ''
  },
  {
    id: 'BKG-20261120-4411',
    customerName: 'Trần Minh Quân',
    customerPhone: '0988776655',
    route: 'Sài Gòn → Đà Lạt',
    operatorName: 'Phương Trang Velo',
    seats: ['B01'],
    totalAmount: 380000,
    status: 'CANCELLED',
    refundReason: ''
  }
])

// Admin Forced Refund Modal State
const selectedBookingForRefund = ref(null)
const refundReasonInput = ref('')
const refundError = ref('')

function openRefundModal(booking) {
  selectedBookingForRefund.value = booking
  refundReasonInput.value = ''
  refundError.value = ''
}

function closeRefundModal() {
  selectedBookingForRefund.value = null
}

function confirmAdminRefund() {
  refundError.value = ''
  if (!refundReasonInput.value || refundReasonInput.value.trim().length < 10) {
    refundError.value = 'Lý do hoàn tiền cưỡng chế phải rõ ràng (tối thiểu 10 ký tự) để lưu Audit Log'
    return
  }

  selectedBookingForRefund.value.status = 'REFUNDED'
  selectedBookingForRefund.value.refundReason = refundReasonInput.value.trim()
  closeRefundModal()
}
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div>
      <h1 class="text-2xl font-bold text-text-primary">Quản Trị Người Dùng & Hoàn Tiền Cưỡng Chế (Admin Refund)</h1>
      <p class="text-xs text-text-muted mt-1">
        Kiểm soát quyền truy cập RBAC (Chốt chặn chống tự khóa chính mình) và thực thi lệnh hoàn tiền vé khi có khiếu nại vận hành.
      </p>
    </div>

    <!-- Tabs -->
    <div class="flex gap-2 border-b border-border-default pb-2 text-xs font-semibold">
      <button
        type="button"
        @click="activeTab = 'USERS'"
        data-testid="tab-admin-users"
        class="px-4 py-2 rounded-lg transition-all"
        :class="activeTab === 'USERS' ? 'bg-brand-navy text-white' : 'text-text-secondary hover:bg-slate-100'"
      >
        Danh Sách Tài Khoản ({{ users.length }})
      </button>

      <button
        type="button"
        @click="activeTab = 'BOOKINGS'"
        data-testid="tab-admin-bookings"
        class="px-4 py-2 rounded-lg transition-all"
        :class="activeTab === 'BOOKINGS' ? 'bg-brand-navy text-white' : 'text-text-secondary hover:bg-slate-100'"
      >
        Đơn Vé Toàn Sàn & Hoàn Tiền ({{ allBookings.length }})
      </button>
    </div>

    <!-- Tab 1: Users Table -->
    <div v-if="activeTab === 'USERS'" class="bg-surface rounded-xl border border-border-default shadow-card overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse text-xs">
          <thead>
            <tr class="bg-slate-50/80 border-b border-border-default text-text-secondary uppercase tracking-wider text-[11px]">
              <th class="py-3 px-4 font-semibold">Mã User</th>
              <th class="py-3 px-4 font-semibold">Tài Khoản / Họ Tên</th>
              <th class="py-3 px-4 font-semibold">Email</th>
              <th class="py-3 px-4 font-semibold text-center">Vai Trò RBAC</th>
              <th class="py-3 px-4 font-semibold text-center">Trạng Thái</th>
              <th class="py-3 px-4 font-semibold text-right">Thao Tác</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border-default">
            <tr v-for="u in users" :key="u.id" class="hover:bg-slate-50/60 transition-colors">
              <td class="py-3.5 px-4 font-bold text-primary-600 tabular-nums">{{ u.id }}</td>
              <td class="py-3.5 px-4">
                <span class="font-bold text-text-primary block">{{ u.fullName }}</span>
                <span class="text-[11px] text-text-muted">@{{ u.username }}</span>
              </td>
              <td class="py-3.5 px-4 text-text-secondary">{{ u.email }}</td>
              <td class="py-3.5 px-4 text-center">
                <span class="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-slate-100 text-slate-800">
                  {{ u.role }}
                </span>
              </td>
              <td class="py-3.5 px-4 text-center">
                <StatusBadge :status="u.status" />
              </td>
              <td class="py-3.5 px-4 text-right">
                <span
                  v-if="u.isSelf"
                  data-testid="self-admin-guard"
                  class="text-[11px] font-semibold text-amber-700 bg-amber-50 px-2.5 py-1 rounded border border-amber-200"
                >
                  🛡️ Tài khoản hiện tại (Không thể tự khóa)
                </span>
                <button
                  v-else
                  type="button"
                  @click="toggleUserLock(u)"
                  :data-testid="`btn-lock-${u.id}`"
                  class="px-3 py-1.5 rounded-lg font-semibold text-xs transition-all"
                  :class="u.status === 'ACTIVE' ? 'bg-rose-50 text-rose-700 hover:bg-rose-100' : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100'"
                >
                  {{ u.status === 'ACTIVE' ? '🔒 Khóa Tài Khoản' : '🔓 Mở Khóa' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Tab 2: Bookings & Admin Refund Table -->
    <div v-else class="bg-surface rounded-xl border border-border-default shadow-card overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse text-xs">
          <thead>
            <tr class="bg-slate-50/80 border-b border-border-default text-text-secondary uppercase tracking-wider text-[11px]">
              <th class="py-3 px-4 font-semibold">Mã Đơn Vé</th>
              <th class="py-3 px-4 font-semibold">Hành Khách</th>
              <th class="py-3 px-4 font-semibold">Chuyến Xe / Nhà Xe</th>
              <th class="py-3 px-4 font-semibold text-center">Ghế</th>
              <th class="py-3 px-4 font-semibold text-right">Tổng Tiền</th>
              <th class="py-3 px-4 font-semibold text-center">Trạng Thái</th>
              <th class="py-3 px-4 font-semibold text-right">Can Thiệp Quản Trị</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border-default">
            <tr v-for="b in allBookings" :key="b.id" class="hover:bg-slate-50/60 transition-colors">
              <td class="py-3.5 px-4 font-bold text-primary-600 tabular-nums">{{ b.id }}</td>
              <td class="py-3.5 px-4">
                <span class="font-bold text-text-primary block">{{ b.customerName }}</span>
                <span class="text-[11px] text-text-muted tabular-nums">{{ b.customerPhone }}</span>
              </td>
              <td class="py-3.5 px-4">
                <span class="font-bold text-text-primary block">{{ b.route }}</span>
                <span class="text-[11px] text-text-muted">{{ b.operatorName }}</span>
              </td>
              <td class="py-3.5 px-4 text-center font-bold text-primary-600 tabular-nums">{{ b.seats.join(', ') }}</td>
              <td class="py-3.5 px-4 text-right font-extrabold text-text-primary tabular-nums">
                {{ b.totalAmount.toLocaleString('vi-VN') }} đ
              </td>
              <td class="py-3.5 px-4 text-center">
                <StatusBadge :status="b.status" />
              </td>
              <td class="py-3.5 px-4 text-right">
                <button
                  v-if="b.status === 'CONFIRMED'"
                  type="button"
                  @click="openRefundModal(b)"
                  :data-testid="`btn-admin-refund-${b.id}`"
                  class="px-3 py-1.5 bg-rose-600 hover:bg-rose-700 text-white font-semibold rounded-lg text-xs transition-all"
                >
                  💸 Hoàn Tiền Cưỡng Chế
                </button>
                <span v-else class="text-text-muted text-[11px]">
                  {{ b.status === 'REFUNDED' ? 'Đã hoàn tiền' : 'Không khả dụng' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Admin Forced Refund Modal -->
    <div
      v-if="selectedBookingForRefund"
      class="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4"
    >
      <div class="bg-surface rounded-xl max-w-md w-full p-6 shadow-dialog border border-border-default space-y-4">
        <h3 class="text-base font-bold text-rose-600 flex items-center gap-2">
          <span>⚠️</span>
          <span>Lệnh Hoàn Tiền Cưỡng Chế — {{ selectedBookingForRefund.id }}</span>
        </h3>

        <div class="bg-slate-50 p-3 rounded-lg border border-border-default text-xs text-text-secondary space-y-1">
          <p>Hành khách: <strong class="text-text-primary">{{ selectedBookingForRefund.customerName }}</strong></p>
          <p>Số tiền hoàn trả 100%: <strong class="text-rose-600 tabular-nums">{{ selectedBookingForRefund.totalAmount.toLocaleString('vi-VN') }} đ</strong></p>
          <p class="text-[11px] text-text-muted">Hệ thống sẽ khấu trừ lại từ ví ký quỹ của nhà xe {{ selectedBookingForRefund.operatorName }} và giải phóng ghế trên Redis.</p>
        </div>

        <FormField
          label="Lý do hoàn tiền cưỡng chế (Ghi nhận vào Audit Log)"
          :error="refundError"
          required
        >
          <textarea
            v-model="refundReasonInput"
            rows="3"
            data-testid="input-refund-reason"
            placeholder="Ví dụ: Nhà xe hủy chuyến đột xuất do sự cố kỹ thuật, hoàn 100% cho khách..."
            class="w-full p-3 rounded-lg border border-border-strong text-xs text-text-primary focus:ring-2 focus:ring-primary-600 resize-none"
            :class="{ 'border-rose-500': refundError }"
          ></textarea>
        </FormField>

        <div class="flex justify-end gap-3 pt-2 border-t border-border-default">
          <button
            type="button"
            @click="closeRefundModal"
            class="px-4 py-2 border border-border-strong hover:bg-slate-50 text-text-secondary text-xs font-semibold rounded-lg"
          >
            Hủy Bỏ
          </button>

          <AppButton
            variant="danger"
            class="px-4 py-2 text-xs font-bold"
            data-testid="btn-confirm-refund"
            @click="confirmAdminRefund"
          >
            Xác Nhận Hoàn Tiền
          </AppButton>
        </div>
      </div>
    </div>
  </div>
</template>
