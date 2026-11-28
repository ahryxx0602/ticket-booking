<script setup>
import { ref, computed, onMounted } from 'vue'
import { toast } from 'vue-sonner'
import {
  Building2,
  Plus,
  Pencil,
  Trash2,
  CheckCircle2,
  XCircle,
  Ban,
  Percent,
  RefreshCw,
  Search,
  AlertCircle,
  Filter,
  ShieldAlert,
  Loader2,
  Phone,
  Mail,
  FileText
} from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter
} from '@/components/ui/dialog'
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogCancel,
  AlertDialogAction
} from '@/components/ui/alert-dialog'
import {
  Table,
  TableHeader,
  TableHead,
  TableBody,
  TableRow,
  TableCell
} from '@/components/ui/table'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import EmptyState from '@/shared/components/EmptyState.vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useAdminPermissions } from '@/modules/admin/composables/useAdminPermissions'
import { adminApi } from '@/modules/admin/api'

const { hasPermission } = useAdminPermissions()

const operators = ref([])
const globalDefaultRate = ref(null)
const globalCommissionLoading = ref(false)
const globalCommissionError = ref('')
const isLoading = ref(false)
const loadError = ref('')
const isMutating = ref(false)

// Filters
const searchQuery = ref('')
const selectedStatusFilter = ref('ALL')

const OPERATOR_STATUSES = [
  { value: 'ALL', label: 'Tất cả trạng thái' },
  { value: 'PENDING_APPROVAL', label: 'Chờ duyệt' },
  { value: 'ACTIVE', label: 'Đang hoạt động' },
  { value: 'SUSPENDED', label: 'Đình chỉ' },
  { value: 'REJECTED', label: 'Bị từ chối' }
]

// Data Fetching
async function fetchOperators() {
  if (!hasPermission('OPERATOR_VIEW')) {
    loadError.value = 'Bạn không có quyền xem danh sách đối tác nhà xe (Yêu cầu OPERATOR_VIEW).'
    return
  }

  isLoading.value = true
  loadError.value = ''
  try {
    const statusParam = selectedStatusFilter.value !== 'ALL' ? selectedStatusFilter.value : null
    const [opsRes, commRes] = await Promise.allSettled([
      adminApi.getOperators(statusParam),
      adminApi.getGlobalCommission()
    ])

    if (opsRes.status === 'fulfilled') {
      const data = opsRes.value?.data?.data !== undefined ? opsRes.value.data.data : opsRes.value?.data
      if (Array.isArray(data)) {
        operators.value = data
      } else {
        throw new Error('Dữ liệu đối tác từ máy chủ không đúng định dạng danh sách.')
      }
    } else {
      throw opsRes.reason
    }

    if (commRes.status === 'fulfilled') {
      const commData = commRes.value?.data?.data !== undefined ? commRes.value.data.data : commRes.value?.data
      if (commData?.commissionRate !== undefined && commData.commissionRate !== null) {
        globalDefaultRate.value = Number(commData.commissionRate)
        globalCommissionError.value = ''
      }
    } else {
      globalCommissionError.value = 'Không thể tải mức hoa hồng toàn sàn'
      globalDefaultRate.value = null
    }
  } catch (err) {
    const status = err?.response?.status
    if (status === 403 || status === 401) {
      loadError.value = 'Truy cập bị từ chối: Phiên làm việc hết hạn hoặc thiếu quyền OPERATOR_VIEW.'
    } else {
      loadError.value = err?.response?.data?.message || err?.message || 'Không thể kết nối đến máy chủ để tải danh sách nhà xe.'
    }
    operators.value = []
  } finally {
    isLoading.value = false
  }
}

const filteredOperators = computed(() => {
  let list = operators.value
  if (selectedStatusFilter.value !== 'ALL') {
    list = list.filter(op => op.status === selectedStatusFilter.value)
  }
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    list = list.filter(op => {
      const brand = (op.brandName || op.name || '').toLowerCase()
      const business = (op.businessName || '').toLowerCase()
      const tax = (op.taxCode || '').toLowerCase()
      const phone = (op.contactPhone || op.phone || '').toLowerCase()
      const id = String(op.id || '').toLowerCase()
      return brand.includes(q) || business.includes(q) || tax.includes(q) || phone.includes(q) || id.includes(q)
    })
  }
  return list
})

// =========================================================================
// OPERATOR CRUD (Create / Edit)
// =========================================================================
const isOperatorModalOpen = ref(false)
const editingOperator = ref(null)
const operatorForm = ref({
  businessName: '',
  brandName: '',
  taxCode: '',
  contactPhone: '',
  contactEmail: '',
  vehicleCount: 5,
  representative: ''
})
const operatorFormError = ref('')

function openCreateOperatorModal() {
  editingOperator.value = null
  operatorFormError.value = ''
  operatorForm.value = {
    businessName: '',
    brandName: '',
    taxCode: '',
    contactPhone: '',
    contactEmail: '',
    vehicleCount: 5,
    representative: ''
  }
  isOperatorModalOpen.value = true
}

function openEditOperatorModal(op) {
  editingOperator.value = op
  operatorFormError.value = ''
  operatorForm.value = {
    businessName: op.businessName || op.name || '',
    brandName: op.brandName || op.name || '',
    taxCode: op.taxCode || '',
    contactPhone: op.contactPhone || op.phone || '',
    contactEmail: op.contactEmail || op.email || '',
    vehicleCount: op.vehicleCount || 5,
    representative: op.representative || ''
  }
  isOperatorModalOpen.value = true
}

function closeOperatorModal() {
  if (isMutating.value) return
  isOperatorModalOpen.value = false
  editingOperator.value = null
}

async function handleSaveOperator() {
  operatorFormError.value = ''
  const brand = (operatorForm.value.brandName || operatorForm.value.name || '').trim()
  const tax = operatorForm.value.taxCode.trim()

  if (!brand || !tax) {
    operatorFormError.value = 'Vui lòng nhập Tên nhà xe và Mã số thuế'
    return
  }

  isMutating.value = true
  try {
    const payload = {
      businessName: operatorForm.value.businessName.trim() || brand,
      brandName: brand,
      taxCode: tax,
      contactPhone: operatorForm.value.contactPhone.trim() || '0901234567',
      contactEmail: operatorForm.value.contactEmail.trim() || 'partner@velobus.vn',
      logoUrl: '',
      description: `Đại diện: ${operatorForm.value.representative.trim() || 'Đại diện đối tác'}`
    }

    if (!editingOperator.value) {
      await adminApi.createOperator(payload)
      toast.success('Thêm đối tác nhà xe mới thành công')
    } else {
      await adminApi.updateOperator(editingOperator.value.id, payload)
      toast.success(`Cập nhật thông tin nhà xe #${editingOperator.value.id} thành công`)
    }
    closeOperatorModal()
    await fetchOperators()
  } catch (err) {
    operatorFormError.value = err?.response?.data?.message || err?.message || 'Có lỗi xảy ra khi lưu thông tin nhà xe.'
  } finally {
    isMutating.value = false
  }
}

// =========================================================================
// DELETE OPERATOR
// =========================================================================
const operatorToDelete = ref(null)

function confirmDeleteOperator(op) {
  operatorToDelete.value = op
}

async function executeDeleteOperator() {
  if (!operatorToDelete.value) return
  isMutating.value = true
  try {
    await adminApi.deleteOperator(operatorToDelete.value.id)
    toast.success(`Đã xóa đối tác nhà xe #${operatorToDelete.value.id}`)
    operatorToDelete.value = null
    await fetchOperators()
  } catch (err) {
    toast.error(err?.response?.data?.message || err?.message || 'Không thể xóa đối tác nhà xe.')
  } finally {
    isMutating.value = false
  }
}

// =========================================================================
// APPROVE / REJECT / SUSPEND MODERATION
// =========================================================================
const moderationDialog = ref({
  isOpen: false,
  operator: null,
  action: null, // 'APPROVE' | 'REJECT' | 'SUSPEND'
  note: '',
  error: ''
})

function openModerationDialog(op, action) {
  moderationDialog.value = {
    isOpen: true,
    operator: op,
    action,
    note: '',
    error: ''
  }
}

function closeModerationDialog() {
  if (isMutating.value) return
  moderationDialog.value.isOpen = false
  moderationDialog.value.operator = null
}

async function executeModeration() {
  const { operator, action, note } = moderationDialog.value
  if (!operator) return

  if ((action === 'REJECT' || action === 'SUSPEND') && !note.trim()) {
    moderationDialog.value.error = 'Vui lòng nhập lý do để đối tác nhận thông báo.'
    return
  }

  isMutating.value = true
  moderationDialog.value.error = ''
  try {
    if (action === 'APPROVE') {
      await adminApi.approveOperator(operator.id)
      toast.success(`Đã phê duyệt hồ sơ nhà xe #${operator.id}`)
    } else if (action === 'REJECT') {
      await adminApi.rejectOperator(operator.id, note.trim())
      toast.success(`Đã từ chối hồ sơ nhà xe #${operator.id}`)
    } else if (action === 'SUSPEND') {
      await adminApi.suspendOperator(operator.id, note.trim())
      toast.success(`Đã thay đổi trạng thái đình chỉ nhà xe #${operator.id}`)
    }
    closeModerationDialog()
    await fetchOperators()
  } catch (err) {
    moderationDialog.value.error = err?.response?.data?.message || err?.message || 'Thao tác không thành công.'
  } finally {
    isMutating.value = false
  }
}

// =========================================================================
// 2-TIER COMMISSION MODAL (0.0% to 100.0% range, null inheritance)
// =========================================================================
const selectedOperatorForCommission = ref(null)
const commissionInput = ref(10.0)
const useGlobalFallback = ref(false)
const commissionError = ref('')
const isConfirmCommissionOpen = ref(false)

function openCommissionModal(op) {
  selectedOperatorForCommission.value = op
  commissionError.value = ''
  const currentRate = op.commissionRate !== undefined && op.commissionRate !== null
    ? op.commissionRate
    : op.customCommissionRate

  if (currentRate === null || currentRate === undefined) {
    useGlobalFallback.value = true
    commissionInput.value = globalDefaultRate.value !== null ? globalDefaultRate.value : 10.0
  } else {
    useGlobalFallback.value = false
    commissionInput.value = Number(currentRate)
  }
}

function closeCommissionModal() {
  if (isMutating.value) return
  selectedOperatorForCommission.value = null
  isConfirmCommissionOpen.value = false
}

function handleOpenConfirmCommission() {
  commissionError.value = ''
  if (!useGlobalFallback.value) {
    const val = Number(commissionInput.value)
    if (commissionInput.value === '' || isNaN(val) || val < 0 || val > 100) {
      commissionError.value = 'Tỷ lệ hoa hồng riêng phải nằm trong khoảng từ 0.0% đến 100.0%'
      return
    }
  }
  isConfirmCommissionOpen.value = true
}

async function executeSaveCommission() {
  commissionError.value = ''
  isConfirmCommissionOpen.value = false

  let rateToSave = null
  if (!useGlobalFallback.value) {
    rateToSave = Number(commissionInput.value)
  }

  isMutating.value = true
  try {
    await adminApi.updateOperatorCommission(selectedOperatorForCommission.value.id, rateToSave)
    toast.success(`Đã cập nhật hoa hồng cho nhà xe #${selectedOperatorForCommission.value.id}`)
    closeCommissionModal()
    await fetchOperators()
  } catch (err) {
    commissionError.value = err?.response?.data?.message || err?.message || 'Không thể lưu tỷ lệ hoa hồng.'
  } finally {
    isMutating.value = false
  }
}

onMounted(() => {
  fetchOperators()
})
</script>

<template>
  <div class="space-y-6">
    <!-- Page Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold tracking-tight text-foreground">Phê Duyệt Hồ Sơ Nhà Xe & Cấu Hình Hoa Hồng 2 Cấp</h1>
        <p class="text-xs text-muted-foreground mt-1">
          Thẩm định pháp lý đối tác vận tải, quản lý hồ sơ nhà xe và thiết lập mức chiết khấu hoa hồng riêng (Custom Tier 0-100%) hoặc mặc định toàn sàn.
        </p>
      </div>

      <div class="flex flex-wrap items-center gap-2">
        <Badge
          variant="outline"
          class="px-3 py-1.5 text-xs font-semibold tabular-nums border-primary/30 text-primary bg-primary/5"
        >
          <Percent class="w-3.5 h-3.5 mr-1 text-primary inline" />
          <span v-if="globalDefaultRate !== null">Mặc định toàn sàn: {{ globalDefaultRate.toFixed(1) }}%</span>
          <span v-else-if="globalCommissionLoading">Đang tải hoa hồng sàn...</span>
          <span v-else class="text-muted-foreground">Mặc định sàn: Chưa tải được</span>
        </Badge>

        <Button
          v-if="hasPermission('OPERATOR_CREATE_UPDATE')"
          type="button"
          data-testid="open-create-operator-btn"
          @click="openCreateOperatorModal"
          class="bg-primary text-primary-foreground hover:bg-primary/90 font-semibold text-xs shadow-sm"
        >
          <Plus class="w-4 h-4 mr-1.5" /> Thêm Nhà Xe Mới
        </Button>
      </div>
    </div>

    <!-- Error State -->
    <Alert v-if="loadError" variant="destructive" class="border-destructive/50 bg-destructive/5">
      <AlertCircle class="h-4 w-4" />
      <AlertTitle>Lỗi tải danh sách đối tác</AlertTitle>
      <AlertDescription class="flex items-center justify-between">
        <span>{{ loadError }}</span>
        <Button variant="outline" size="sm" @click="fetchOperators" class="ml-4 h-7 text-xs">
          <RefreshCw class="w-3.5 h-3.5 mr-1" /> Thử lại
        </Button>
      </AlertDescription>
    </Alert>

    <!-- Filter & Search Toolbar -->
    <Card class="border-border">
      <CardContent class="p-4 flex flex-col sm:flex-row items-center justify-between gap-3">
        <div class="relative w-full sm:w-80">
          <Search class="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            v-model="searchQuery"
            placeholder="Tìm theo tên nhà xe, MST, SĐT..."
            class="pl-9 h-9 text-xs"
          />
        </div>

        <div class="flex items-center gap-2 w-full sm:w-auto">
          <Filter class="w-4 h-4 text-muted-foreground hidden sm:inline" />
          <div class="flex flex-wrap gap-1">
            <Button
              v-for="st in OPERATOR_STATUSES"
              :key="st.value"
              variant="ghost"
              size="sm"
              :class="[
                'h-8 text-xs font-medium',
                selectedStatusFilter === st.value
                  ? 'bg-primary text-primary-foreground hover:bg-primary/90'
                  : 'text-muted-foreground hover:text-foreground'
              ]"
              @click="selectedStatusFilter = st.value; fetchOperators()"
            >
              {{ st.label }}
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- Table Card with Responsive Mobile Handling (D2-PRE-10) -->
    <Card class="border-border shadow-sm overflow-hidden">
      <!-- Loading State -->
      <div v-if="isLoading" class="p-12 text-center text-muted-foreground text-xs flex flex-col items-center justify-center space-y-3">
        <Loader2 class="w-8 h-8 animate-spin text-primary" />
        <p>Đang tải danh sách nhà xe từ máy chủ...</p>
      </div>

      <!-- Empty State -->
      <div v-else-if="filteredOperators.length === 0" class="p-8">
        <EmptyState
          title="Không tìm thấy đối tác nhà xe"
          description="Hiện chưa có đối tác nhà xe nào phù hợp với điều kiện tìm kiếm hoặc trạng thái đã chọn."
        >
          <template #actions>
            <Button
              v-if="hasPermission('OPERATOR_CREATE_UPDATE')"
              @click="openCreateOperatorModal"
              size="sm"
              class="text-xs"
            >
              <Plus class="w-4 h-4 mr-1.5" /> Tạo nhà xe mới
            </Button>
          </template>
        </EmptyState>
      </div>

      <!-- Real Authoritative Operators Table -->
      <div v-else class="overflow-x-auto min-w-full">
        <Table class="min-w-[700px]">
          <TableHeader>
            <TableRow class="bg-muted/40 hover:bg-muted/40">
              <TableHead class="font-semibold text-xs py-3 w-20">Mã Đối Tác</TableHead>
              <TableHead class="font-semibold text-xs py-3">Tên Nhà Xe & MST</TableHead>
              <TableHead class="font-semibold text-xs py-3">Liên Hệ</TableHead>
              <TableHead class="font-semibold text-xs py-3 text-center">Hoa Hồng Áp Dụng</TableHead>
              <TableHead class="font-semibold text-xs py-3 text-center">Trạng Thái</TableHead>
              <TableHead class="font-semibold text-xs py-3 text-right">Thao Tác Quản Trị</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow
              v-for="op in filteredOperators"
              :key="op.id"
              class="hover:bg-muted/30 transition-colors"
            >
              <TableCell class="font-bold text-primary tabular-nums text-xs">
                #{{ op.id }}
              </TableCell>

              <TableCell class="text-xs">
                <span class="font-bold text-foreground block">{{ op.brandName || op.businessName || op.name }}</span>
                <span class="text-[11px] text-muted-foreground tabular-nums">MST: {{ op.taxCode }}</span>
              </TableCell>

              <TableCell class="text-xs text-muted-foreground">
                <div class="space-y-0.5">
                  <div class="flex items-center gap-1.5 text-foreground font-medium">
                    <span>{{ op.representative || op.businessName || 'Đại diện đối tác' }}</span>
                  </div>
                  <div class="flex items-center gap-1 text-[11px] text-muted-foreground tabular-nums">
                    <Phone class="w-3 h-3" />
                    <span>{{ op.contactPhone || op.phone || 'Chưa cập nhật' }}</span>
                  </div>
                </div>
              </TableCell>

              <TableCell class="text-xs text-center">
                <Badge
                  variant="outline"
                  class="font-bold tabular-nums text-[11px]"
                  :class="[
                    op.commissionRate !== null && op.commissionRate !== undefined
                      ? 'border-amber-500/30 text-amber-600 bg-amber-500/10'
                      : 'border-muted text-muted-foreground bg-muted/20'
                  ]"
                >
                  {{
                    op.commissionRate !== null && op.commissionRate !== undefined
                      ? `${Number(op.commissionRate).toFixed(1)}% (Riêng)`
                      : (globalDefaultRate !== null ? `${globalDefaultRate.toFixed(1)}% (Chuẩn)` : 'Mặc định sàn')
                  }}
                </Badge>
              </TableCell>

              <TableCell class="text-center text-xs">
                <StatusBadge :status="op.status" />
              </TableCell>

              <TableCell class="text-right space-x-1.5 whitespace-nowrap text-xs">
                <!-- Commission Button -->
                <Button
                  v-if="hasPermission('OPERATOR_COMMISSION_MANAGE')"
                  type="button"
                  variant="outline"
                  size="sm"
                  @click="openCommissionModal(op)"
                  :data-testid="`btn-commission-${op.id}`"
                  class="h-8 text-xs font-semibold"
                >
                  <Percent class="w-3.5 h-3.5 mr-1" /> Hoa Hồng
                </Button>

                <!-- Moderation: Approve Pending -->
                <Button
                  v-if="op.status === 'PENDING_APPROVAL' && hasPermission('OPERATOR_APPROVE_REJECT')"
                  type="button"
                  size="sm"
                  @click="openModerationDialog(op, 'APPROVE')"
                  :data-testid="`btn-approve-${op.id}`"
                  class="h-8 text-xs font-semibold text-emerald-600 border-emerald-500/30 hover:bg-emerald-500/10"
                  variant="outline"
                >
                  <CheckCircle2 class="w-3.5 h-3.5 mr-1" /> Duyệt
                </Button>

                <!-- Moderation: Reject Pending -->
                <Button
                  v-if="op.status === 'PENDING_APPROVAL' && hasPermission('OPERATOR_APPROVE_REJECT')"
                  type="button"
                  size="sm"
                  variant="destructive"
                  @click="openModerationDialog(op, 'REJECT')"
                  :data-testid="`btn-reject-${op.id}`"
                  class="h-8 text-xs font-semibold"
                >
                  <XCircle class="w-3.5 h-3.5 mr-1" /> Từ Chối
                </Button>

                <!-- Moderation: Suspend / Reactivate Active -->
                <Button
                  v-if="op.status !== 'PENDING_APPROVAL' && hasPermission('OPERATOR_SUSPEND')"
                  type="button"
                  size="sm"
                  variant="outline"
                  @click="openModerationDialog(op, op.status === 'ACTIVE' ? 'SUSPEND' : 'APPROVE')"
                  :data-testid="`btn-suspend-${op.id}`"
                  :class="[
                    'h-8 text-xs font-semibold',
                    op.status === 'ACTIVE'
                      ? 'text-amber-600 hover:bg-amber-50 border-amber-200'
                      : 'text-emerald-600 hover:bg-emerald-50 border-emerald-200'
                  ]"
                >
                  <Ban v-if="op.status === 'ACTIVE'" class="w-3.5 h-3.5 mr-1" />
                  <CheckCircle2 v-else class="w-3.5 h-3.5 mr-1" />
                  {{ op.status === 'ACTIVE' ? 'Đình Chỉ' : 'Kích Hoạt' }}
                </Button>

                <!-- Edit Operator -->
                <Button
                  v-if="hasPermission('OPERATOR_CREATE_UPDATE')"
                  type="button"
                  variant="outline"
                  size="sm"
                  @click="openEditOperatorModal(op)"
                  :data-testid="`btn-edit-operator-${op.id}`"
                  class="h-8 text-xs font-semibold"
                >
                  <Pencil class="w-3.5 h-3.5 mr-1" /> Sửa
                </Button>

                <!-- Delete Operator -->
                <Button
                  v-if="hasPermission('OPERATOR_CREATE_UPDATE')"
                  type="button"
                  variant="ghost"
                  size="sm"
                  @click="confirmDeleteOperator(op)"
                  :data-testid="`btn-delete-operator-${op.id}`"
                  class="h-8 text-xs font-semibold text-destructive hover:bg-destructive/10"
                >
                  <Trash2 class="w-3.5 h-3.5" />
                </Button>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </div>
    </Card>

    <!-- Modal 1: Create / Edit Operator -->
    <Dialog :open="isOperatorModalOpen" @update:open="(v) => !v && closeOperatorModal()">
      <DialogContent class="max-w-md">
        <DialogHeader>
          <DialogTitle class="text-base font-bold">
            {{ editingOperator ? `Chỉnh Sửa Nhà Xe — #${editingOperator.id}` : 'Thêm Đối Tác Nhà Xe Mới' }}
          </DialogTitle>
          <DialogDescription class="text-xs">
            Nhập thông tin pháp nhân và liên hệ của nhà xe đối tác trên sàn VeloBus.
          </DialogDescription>
        </DialogHeader>

        <Alert v-if="operatorFormError" variant="destructive" class="py-2 text-xs">
          <AlertCircle class="h-4 w-4" />
          <AlertDescription>{{ operatorFormError }}</AlertDescription>
        </Alert>

        <div class="space-y-3 py-2 text-xs">
          <div class="space-y-1">
            <label class="block font-semibold text-foreground">
              Tên thương hiệu nhà xe <span class="text-destructive">*</span>
            </label>
            <Input
              v-model="operatorForm.brandName"
              data-testid="input-operator-name"
              placeholder="VD: Sao Việt Limousine"
              class="h-9 text-xs"
            />
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div class="space-y-1">
              <label class="block font-semibold text-foreground">
                Mã số thuế <span class="text-destructive">*</span>
              </label>
              <Input
                v-model="operatorForm.taxCode"
                data-testid="input-operator-tax"
                placeholder="0109988776"
                class="h-9 text-xs tabular-nums"
              />
            </div>

            <div class="space-y-1">
              <label class="block font-semibold text-foreground">Số điện thoại</label>
              <Input
                v-model="operatorForm.contactPhone"
                placeholder="0901234567"
                class="h-9 text-xs tabular-nums"
              />
            </div>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div class="space-y-1">
              <label class="block font-semibold text-foreground">Email liên hệ</label>
              <Input
                v-model="operatorForm.contactEmail"
                type="email"
                placeholder="partner@velobus.vn"
                class="h-9 text-xs"
              />
            </div>

            <div class="space-y-1">
              <label class="block font-semibold text-foreground">Người đại diện</label>
              <Input
                v-model="operatorForm.representative"
                placeholder="Nguyễn Văn A"
                class="h-9 text-xs"
              />
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" size="sm" @click="closeOperatorModal" :disabled="isMutating" class="text-xs">
            Hủy
          </Button>
          <Button
            size="sm"
            data-testid="btn-save-operator"
            @click="handleSaveOperator"
            :disabled="isMutating"
            class="text-xs"
          >
            <Loader2 v-if="isMutating" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
            {{ editingOperator ? 'Lưu Thay Đổi' : 'Thêm Nhà Xe' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- Modal 2: 2-Tier Commission Modal -->
    <Dialog :open="!!selectedOperatorForCommission" @update:open="(v) => !v && closeCommissionModal()">
      <DialogContent class="max-w-sm">
        <DialogHeader>
          <DialogTitle class="text-base font-bold">
            Cấu Hình Hoa Hồng — {{ selectedOperatorForCommission?.brandName || selectedOperatorForCommission?.name }}
          </DialogTitle>
          <DialogDescription class="text-xs">
            Thiết lập tỷ lệ hoa hồng riêng (Custom Tier 0.0% – 100.0%) hoặc sử dụng tỷ lệ mặc định toàn sàn.
          </DialogDescription>
        </DialogHeader>

        <Alert v-if="commissionError" variant="destructive" class="py-2 text-xs">
          <AlertCircle class="h-4 w-4" />
          <AlertDescription>{{ commissionError }}</AlertDescription>
        </Alert>

        <div class="space-y-4 py-2 text-xs">
          <div class="p-3 rounded-lg bg-muted/40 border border-border space-y-2">
            <div class="flex items-center justify-between">
              <span class="text-muted-foreground">Tỷ lệ sàn mặc định:</span>
              <span class="font-bold text-foreground tabular-nums">
                {{ globalDefaultRate !== null ? `${globalDefaultRate.toFixed(1)}%` : 'Chưa tải được' }}
              </span>
            </div>
            <label class="flex items-center gap-2 cursor-pointer pt-1">
              <input
                type="checkbox"
                v-model="useGlobalFallback"
                class="rounded border-border text-primary focus:ring-primary w-4 h-4"
              />
              <span class="font-medium text-foreground">Sử dụng mức mặc định toàn sàn (Kế thừa null)</span>
            </label>
          </div>

          <div v-if="!useGlobalFallback" class="space-y-1">
            <label class="block font-semibold text-foreground">
              Tỷ lệ hoa hồng riêng (%) <span class="text-destructive">*</span>
            </label>
            <div class="relative">
              <Input
                v-model.number="commissionInput"
                type="number"
                step="0.1"
                min="0"
                max="100"
                data-testid="input-custom-commission"
                class="h-9 text-xs pr-8 tabular-nums font-bold"
              />
              <span class="absolute right-3 top-2 text-muted-foreground text-xs">%</span>
            </div>
            <p class="text-[11px] text-muted-foreground">Hạn mức miền hợp lệ: từ 0.0% đến 100.0% (cho phép 0%)</p>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" size="sm" @click="closeCommissionModal" :disabled="isMutating" class="text-xs">
            Hủy
          </Button>
          <Button
            size="sm"
            data-testid="btn-save-commission"
            @click="handleOpenConfirmCommission"
            :disabled="isMutating"
            class="text-xs"
          >
            <Loader2 v-if="isMutating" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
            Lưu Cấu Hình
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- Modal 3: Moderation Note Dialog (Reject / Suspend / Approve) -->
    <Dialog :open="moderationDialog.isOpen" @update:open="(v) => !v && closeModerationDialog()">
      <DialogContent class="max-w-md">
        <DialogHeader>
          <DialogTitle class="text-base font-bold flex items-center gap-2">
            <ShieldAlert v-if="moderationDialog.action !== 'APPROVE'" class="w-5 h-5 text-amber-500" />
            <CheckCircle2 v-else class="w-5 h-5 text-emerald-500" />
            <span>
              {{
                moderationDialog.action === 'APPROVE'
                  ? 'Phê Duyệt Hồ Sơ Nhà Xe'
                  : moderationDialog.action === 'REJECT'
                    ? 'Từ Chối Hồ Sơ Đối Tác'
                    : 'Tạm Đình Chỉ Nhà Xe'
              }}
            </span>
          </DialogTitle>
          <DialogDescription class="text-xs">
            Hành động này sẽ cập nhật trạng thái vận hành của đối tác (#{{ moderationDialog.operator?.id }}) trên toàn sàn.
          </DialogDescription>
        </DialogHeader>

        <Alert v-if="moderationDialog.error" variant="destructive" class="py-2 text-xs">
          <AlertCircle class="h-4 w-4" />
          <AlertDescription>{{ moderationDialog.error }}</AlertDescription>
        </Alert>

        <div v-if="moderationDialog.action !== 'APPROVE'" class="space-y-2 py-2 text-xs">
          <label class="block font-semibold text-foreground">
            Lý do xử lý <span class="text-destructive">*</span>
          </label>
          <textarea
            v-model="moderationDialog.note"
            rows="3"
            placeholder="Nhập lý do chi tiết để gửi thông báo cho đối tác..."
            class="w-full p-2.5 rounded-md border border-input bg-background text-xs text-foreground focus:outline-none focus:ring-1 focus:ring-ring"
          ></textarea>
        </div>
        <div v-else class="py-2 text-xs text-muted-foreground">
          Xác nhận phê duyệt đối tác nhà xe <strong>{{ moderationDialog.operator?.brandName || moderationDialog.operator?.name }}</strong>. Nhà xe sẽ được kích hoạt trạng thái ACTIVE để vận hành chuyến xe.
        </div>

        <DialogFooter>
          <Button variant="outline" size="sm" @click="closeModerationDialog" :disabled="isMutating" class="text-xs">
            Hủy
          </Button>
          <Button
            size="sm"
            :variant="moderationDialog.action === 'APPROVE' ? 'default' : 'destructive'"
            @click="executeModeration"
            :disabled="isMutating"
            class="text-xs"
          >
            <Loader2 v-if="isMutating" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
            Xác Nhận
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- Commission Confirmation AlertDialog -->
    <AlertDialog :open="isConfirmCommissionOpen" @update:open="(v) => !v && (isConfirmCommissionOpen = false)">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle class="text-base font-bold">Xác nhận thay đổi hoa hồng đối tác?</AlertDialogTitle>
          <AlertDialogDescription class="text-xs space-y-1">
            <p>
              Áp dụng mức hoa hồng đối với nhà xe <strong>{{ selectedOperatorForCommission?.brandName || selectedOperatorForCommission?.name }}</strong>:
            </p>
            <p class="font-bold text-foreground">
              {{ useGlobalFallback ? 'Kế thừa mức mặc định toàn sàn (null)' : `${Number(commissionInput).toFixed(1)}%` }}
            </p>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel :disabled="isMutating" class="text-xs">Hủy</AlertDialogCancel>
          <AlertDialogAction
            @click="executeSaveCommission"
            :disabled="isMutating"
            class="text-xs font-semibold"
          >
            <Loader2 v-if="isMutating" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
            Xác Nhận Cập Nhật
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <!-- Delete Confirmation AlertDialog -->
    <AlertDialog :open="!!operatorToDelete" @update:open="(v) => !v && (operatorToDelete = null)">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle class="text-base font-bold">Xác nhận xóa đối tác nhà xe?</AlertDialogTitle>
          <AlertDialogDescription class="text-xs">
            Bạn có chắc chắn muốn xóa nhà xe <strong class="text-foreground">{{ operatorToDelete?.brandName || operatorToDelete?.name }}</strong>? Thao tác này sẽ xóa hồ sơ và không thể hoàn tác nếu nhà xe chưa có ràng buộc chuyến.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel :disabled="isMutating" class="text-xs">Hủy</AlertDialogCancel>
          <AlertDialogAction
            @click="executeDeleteOperator"
            :disabled="isMutating"
            class="text-xs bg-destructive text-destructive-foreground hover:bg-destructive/90"
          >
            <Loader2 v-if="isMutating" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
            Xác Nhận Xóa
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>