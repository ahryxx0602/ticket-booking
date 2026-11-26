<script setup>
import { ref, computed, watch, onMounted, h } from 'vue'
import { toast } from 'vue-sonner'
import {
  Users,
  Shield,
  Ticket,
  Plus,
  Pencil,
  KeyRound,
  Lock,
  Unlock,
  Trash2,
  Banknote,
  Search,
  Check,
  X,
  AlertTriangle,
  AlertCircle,
  Zap,
  RefreshCw,
  MoreHorizontal
} from 'lucide-vue-next'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import FormField from '@/shared/components/FormField.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { PasswordInput } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Checkbox } from '@/components/ui/checkbox'
import DataTable from '@/components/ui/table/DataTable.vue'
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
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator
} from '@/components/ui/dropdown-menu'
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert'
import { ADMIN_PERMISSION_MODULES, ALL_ADMIN_PERMISSION_CODES } from '@/modules/auth/store'
import { useAdminPermissions } from '@/modules/admin/composables/useAdminPermissions'
import { adminApi } from '@/modules/admin/api'

const {
  auth,
  isSuperAdmin,
  customRole,
  previewRole,
  hasPermission,
  startRolePreview,
  stopRolePreview
} = useAdminPermissions()

// Active main tab: 'USERS' | 'ROLES' | 'BOOKINGS'
const resolveInitialTab = () => {
  if (hasPermission('USER_VIEW')) return 'USERS'
  if (hasPermission('ROLE_MANAGE_CRUD')) return 'ROLES'
  return 'BOOKINGS'
}
const activeTab = ref(resolveInitialTab())

watch(previewRole, () => {
  if (activeTab.value === 'USERS' && !hasPermission('USER_VIEW')) {
    activeTab.value = resolveInitialTab()
  } else if (activeTab.value === 'ROLES' && !hasPermission('ROLE_MANAGE_CRUD')) {
    activeTab.value = resolveInitialTab()
  } else if (activeTab.value === 'BOOKINGS' && !hasPermission('BOOKING_VIEW')) {
    activeTab.value = resolveInitialTab()
  }
})

// Current user identity from auth store for invariant checks
const currentUsername = computed(() => {
  if (auth?.user?.username) return auth.user.username
  if (auth?.jwtPayload?.sub) return auth.jwtPayload.sub
  if (auth?.jwtPayload?.username) return auth.jwtPayload.username
  if (auth?.isSuperAdmin) return 'admin'
  return ''
})
const currentUserId = computed(() => auth?.user?.id || auth?.jwtPayload?.userId || null)

function isSelfUser(u) {
  if (!u) return false
  if (u.isSelf) return true
  if (currentUserId.value && u.id && String(currentUserId.value) === String(u.id)) return true
  if (currentUsername.value && u.username && currentUsername.value.toLowerCase() === u.username.toLowerCase()) return true
  return false
}

const feedbackMessage = ref('')
const feedbackType = ref('success')

function notify(msg, type = 'success') {
  feedbackMessage.value = msg
  feedbackType.value = type
  if (type === 'error') {
    toast.error(msg)
  } else if (type === 'warning') {
    toast.warning(msg)
  } else if (type === 'info') {
    toast.info(msg)
  } else {
    toast.success(msg)
  }
}

function formatVnd(val) {
  if (val === undefined || val === null) return '0'
  return Number(val).toLocaleString('vi-VN')
}

// =========================================================================
// 1. REAL API USERS STATE & SERVER-SIDE PAGINATION
// =========================================================================
const users = ref([])
const isLoadingUsers = ref(false)
const usersError = ref(null)
const userPage = ref(0)
const userPageSize = ref(10)
const userTotalElements = ref(0)
const userTotalPages = ref(1)

const activeUserCategory = ref('ALL')
const userSearchKeyword = ref('')

async function fetchUsers() {
  if (!hasPermission('USER_VIEW')) return
  isLoadingUsers.value = true
  usersError.value = null

  try {
    const params = {
      page: userPage.value,
      size: userPageSize.value
    }
    if (userSearchKeyword.value.trim()) {
      params.keyword = userSearchKeyword.value.trim()
    }
    if (activeUserCategory.value !== 'ALL') {
      params.role = activeUserCategory.value
    }

    const res = await adminApi.getUsers(params)
    const pageData = res?.data || res || {}
    const items = pageData.items || (Array.isArray(pageData) ? pageData : [])
    users.value = items.map(u => ({
      ...u,
      isSelf: isSelfUser(u)
    }))
    userTotalElements.value = pageData.totalElements ?? users.value.length
    userTotalPages.value = pageData.totalPages ?? Math.max(1, Math.ceil(userTotalElements.value / userPageSize.value))
  } catch (err) {
    usersError.value = err?.message || 'Không thể tải danh sách người dùng từ máy chủ Backend'
    users.value = []
    userTotalElements.value = 0
  } finally {
    isLoadingUsers.value = false
  }
}

function onUserPageChange(newPage) {
  userPage.value = newPage
  fetchUsers()
}

function handleSearchUsers() {
  userPage.value = 0
  fetchUsers()
}

function handleCategoryChange(cat) {
  activeUserCategory.value = cat
  userPage.value = 0
  fetchUsers()
}

// =========================================================================
// 2. REAL API SYSTEM ROLES & 7x26 PERMISSION MATRIX
// =========================================================================
// Canonical mapping for exact set equality (1:1 with AdminPermissionCatalog.java)
const CANONICAL_MODULE_PERMS = {
  MOD_DASHBOARD: ['PAYMENT_REPORT_VIEW', 'PAYMENT_LIST_VIEW', 'PAYMENT_RECONCILE_RESOLVE'],
  MOD_OPERATORS: ['OPERATOR_VIEW', 'OPERATOR_CREATE_UPDATE', 'OPERATOR_APPROVE_REJECT', 'OPERATOR_SUSPEND', 'OPERATOR_COMMISSION_MANAGE'],
  MOD_WITHDRAWALS: ['WITHDRAWAL_VIEW', 'WITHDRAWAL_APPROVE', 'WITHDRAWAL_REJECT'],
  MOD_BANNERS: ['BANNER_VIEW', 'BANNER_CREATE_DELETE', 'BANNER_APPROVE_REJECT'],
  MOD_IAM: ['USER_VIEW', 'USER_CREATE_UPDATE_DELETE', 'USER_STATUS_LOCK', 'USER_PASSWORD_RESET', 'USER_ROLE_ASSIGN', 'ROLE_MANAGE_CRUD'],
  MOD_BOOKINGS: ['BOOKING_VIEW', 'BOOKING_FORCE_REFUND'],
  MOD_SETTINGS: ['STATION_VIEW', 'STATION_CREATE_UPDATE_DELETE', 'TRIP_MANAGE_CRUD', 'SYSTEM_SETTINGS_MANAGE']
}

function validatePermissionCatalog(cat) {
  if (!Array.isArray(cat) || cat.length !== 7) return false
  const canonicalModules = Object.keys(CANONICAL_MODULE_PERMS)
  const incomingModules = new Set()
  const allCodes = new Set()

  for (const m of cat) {
    if (!m || typeof m.moduleCode !== 'string' || !m.moduleCode) return false
    if (typeof m.moduleName !== 'string' || !m.moduleName) return false
    if (!Array.isArray(m.permissions) || m.permissions.length === 0) return false

    if (!CANONICAL_MODULE_PERMS[m.moduleCode]) return false
    incomingModules.add(m.moduleCode)

    const expectedPerms = new Set(CANONICAL_MODULE_PERMS[m.moduleCode])
    const modulePerms = new Set()

    for (const p of m.permissions) {
      if (!p || typeof p.code !== 'string' || !p.code) return false
      if (typeof p.name !== 'string' || !p.name) return false
      if (typeof p.httpMethod !== 'string') return false
      if (typeof p.apiPattern !== 'string') return false

      if (allCodes.has(p.code)) return false // Duplicate code rejected
      allCodes.add(p.code)
      modulePerms.add(p.code)

      if (!expectedPerms.has(p.code)) return false // Must belong to this exact module
    }

    if (modulePerms.size !== expectedPerms.size) return false
  }

  return incomingModules.size === canonicalModules.length && allCodes.size === 26
}

const permissionCatalog = ref(ADMIN_PERMISSION_MODULES)
const systemRoles = ref([])
const isLoadingRoles = ref(false)
const rolesError = ref(null)

async function fetchRoles() {
  isLoadingRoles.value = true
  rolesError.value = null

  // 1. Fetch catalog with contract validation
  try {
    const catRes = await adminApi.getPermissionsCatalog()
    const cat = catRes?.data || catRes
    if (validatePermissionCatalog(cat)) {
      permissionCatalog.value = cat
    } else {
      // Validated canonical fallback strictly adhering to AdminPermissionCatalog.java
      permissionCatalog.value = ADMIN_PERMISSION_MODULES
    }
  } catch (_) {
    permissionCatalog.value = ADMIN_PERMISSION_MODULES
  }

  // 2. Fetch roles
  try {
    const res = await adminApi.getRoles()
    const roles = res?.data || res || []
    if (Array.isArray(roles)) {
      systemRoles.value = roles
    }
  } catch (err) {
    rolesError.value = err?.message || 'Không thể tải danh sách vai trò từ máy chủ Backend'
    systemRoles.value = []
  } finally {
    isLoadingRoles.value = false
  }
}

function getRoleInfo(roleCode) {
  return (
    systemRoles.value.find(r => r.code === roleCode) || {
      code: roleCode,
      name: roleCode,
      category: roleCode === 'ROLE_CUSTOMER' ? 'CUSTOMER' : roleCode === 'ROLE_OPERATOR' ? 'OPERATOR' : 'SYSTEM',
      permissions: []
    }
  )
}

// =========================================================================
// 3. REAL API BOOKINGS & ADMIN FORCED REFUND
// =========================================================================
const allBookings = ref([])
const isLoadingBookings = ref(false)
const bookingsError = ref(null)
const bookingPage = ref(0)
const bookingPageSize = ref(10)
const bookingTotalElements = ref(0)
const bookingTotalPages = ref(1)

async function fetchBookings() {
  if (!hasPermission('BOOKING_VIEW')) return
  isLoadingBookings.value = true
  bookingsError.value = null

  try {
    const params = {
      page: bookingPage.value,
      size: bookingPageSize.value
    }
    const res = await adminApi.getBookings(params)
    const pageData = res?.data || res || {}
    allBookings.value = pageData.items || (Array.isArray(pageData) ? pageData : [])
    bookingTotalElements.value = pageData.totalElements ?? allBookings.value.length
    bookingTotalPages.value = pageData.totalPages ?? Math.max(1, Math.ceil(bookingTotalElements.value / bookingPageSize.value))
  } catch (err) {
    bookingsError.value = err?.message || 'Không thể tải danh sách đơn vé từ máy chủ Backend'
    allBookings.value = []
    bookingTotalElements.value = 0
  } finally {
    isLoadingBookings.value = false
  }
}

function onBookingPageChange(newPage) {
  bookingPage.value = newPage
  fetchBookings()
}

onMounted(() => {
  fetchUsers()
  fetchRoles()
  fetchBookings()
})

// =========================================================================
// 4. USER MUTATIONS (CREATE, EDIT, ROLE, LOCK, RESET PWD, DELETE)
// =========================================================================
const isUserModalOpen = ref(false)
const isSavingUser = ref(false)
const editingUser = ref(null)
const userForm = ref({
  username: '',
  fullName: '',
  email: '',
  phone: '',
  password: '',
  category: 'SYSTEM',
  role: 'ROLE_ACCOUNTANT'
})
const userFormError = ref('')

const availableRolesForSelectedCategory = computed(() => {
  const cat = userForm.value.category
  return systemRoles.value.filter(r => r.category === cat)
})

function onUserFormCategoryChange() {
  const matching = availableRolesForSelectedCategory.value
  if (matching.length > 0) {
    userForm.value.role = matching[0].code
  }
}

function openCreateUserModal(defaultCat = null) {
  editingUser.value = null
  userFormError.value = ''
  const cat = defaultCat && defaultCat !== 'ALL' ? defaultCat : (activeUserCategory.value !== 'ALL' ? activeUserCategory.value : 'SYSTEM')
  const firstRole = systemRoles.value.find(r => r.category === cat)?.code || 'ROLE_CUSTOMER'
  userForm.value = {
    username: '',
    fullName: '',
    email: '',
    phone: '',
    password: '',
    category: cat,
    role: firstRole
  }
  isUserModalOpen.value = true
}

function openEditUserModal(user) {
  if (user.role === 'ROLE_ADMIN' && !isSuperAdmin.value) {
    notify('Nhân sự mang Custom Role không được phép chỉnh sửa tài khoản Super Admin (SUPER_ADMIN_PROTECTED)', 'error')
    return
  }
  editingUser.value = user
  userFormError.value = ''
  const cat = user.category || getRoleInfo(user.role).category
  userForm.value = {
    username: user.username,
    fullName: user.fullName,
    email: user.email,
    phone: user.phone || '',
    password: '',
    category: cat,
    role: user.role
  }
  isUserModalOpen.value = true
}

function closeUserModal() {
  isUserModalOpen.value = false
  editingUser.value = null
}

async function handleSaveUser() {
  userFormError.value = ''
  if (!userForm.value.fullName.trim() || !userForm.value.email.trim()) {
    userFormError.value = 'Vui lòng nhập đầy đủ Họ tên và Email hợp lệ'
    return
  }

  // Chốt chặn bảo vệ Super Admin
  if (userForm.value.role === 'ROLE_ADMIN' && !isSuperAdmin.value) {
    userFormError.value = 'Chỉ Super Admin (ROLE_ADMIN) mới có quyền tạo hoặc gán quyền ROLE_ADMIN'
    return
  }

  isSavingUser.value = true
  try {
    if (!editingUser.value) {
      // Create user
      if (!userForm.value.username.trim() || userForm.value.username.trim().length < 3) {
        throw new Error('Tên đăng nhập (Username) phải từ 3 ký tự trở lên')
      }
      if (!userForm.value.password || userForm.value.password.length < 6) {
        throw new Error('Mật khẩu khởi tạo phải tối thiểu 6 ký tự')
      }

      await adminApi.createUser({
        username: userForm.value.username.trim(),
        fullName: userForm.value.fullName.trim(),
        email: userForm.value.email.trim(),
        phone: userForm.value.phone.trim(),
        password: userForm.value.password,
        role: userForm.value.role
      })

      notify(`Đã tạo tài khoản @${userForm.value.username.trim()} thành công!`)
      closeUserModal()
      await fetchUsers()
    } else {
      // Edit user
      if (isSelfUser(editingUser.value) && userForm.value.role !== editingUser.value.role) {
        throw new Error('Không thể tự hạ quyền hoặc đổi Role của chính tài khoản đang đăng nhập (ADMIN_CANNOT_DEMOTE_SELF)')
      }

      await adminApi.updateUser(editingUser.value.id, {
        fullName: userForm.value.fullName.trim(),
        email: userForm.value.email.trim(),
        phone: userForm.value.phone.trim(),
        role: userForm.value.role
      })

      notify(`Đã cập nhật thông tin tài khoản @${editingUser.value.username}`)
      closeUserModal()
      await fetchUsers()
    }
  } catch (err) {
    userFormError.value = err?.message || 'Lỗi khi lưu thông tin người dùng'
  } finally {
    isSavingUser.value = false
  }
}

async function handleQuickRoleChange(user, newRoleCode) {
  if (isSelfUser(user)) {
    notify('Không thể tự thay đổi Role của tài khoản đang đăng nhập (ADMIN_CANNOT_DEMOTE_SELF)', 'error')
    return
  }
  if ((user.role === 'ROLE_ADMIN' || newRoleCode === 'ROLE_ADMIN') && !isSuperAdmin.value) {
    notify('Nhân sự mang Custom Role không được phép cấp hoặc thu hồi quyền ROLE_ADMIN (SUPER_ADMIN_PROTECTED)', 'error')
    return
  }
  try {
    await adminApi.changeUserRole(user.id, newRoleCode)
    notify(`Đã gán Role "${newRoleCode}" cho tài khoản @${user.username}`)
    await fetchUsers()
  } catch (err) {
    notify(err?.message || 'Lỗi khi thay đổi vai trò người dùng', 'error')
  }
}

async function toggleUserLock(user) {
  if (isSelfUser(user)) {
    notify('Không thể tự khóa tài khoản của chính mình (ADMIN_CANNOT_LOCK_SELF)', 'error')
    return
  }
  if (user.role === 'ROLE_ADMIN' && !isSuperAdmin.value) {
    notify('Không được phép khóa tài khoản Super Admin (SUPER_ADMIN_PROTECTED)', 'error')
    return
  }

  const targetStatus = user.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE'
  try {
    await adminApi.updateUserStatus(user.id, targetStatus)
    notify(
      targetStatus === 'ACTIVE'
        ? `Đã mở khóa tài khoản @${user.username}`
        : `Đã khóa tài khoản @${user.username}`
    )
    await fetchUsers()
  } catch (err) {
    notify(err?.message || 'Lỗi khi cập nhật trạng thái tài khoản', 'error')
  }
}

const userToDelete = ref(null)
const isConfirmDeleteUserOpen = ref(false)
const isDeletingUser = ref(false)

function openDeleteUserModal(user) {
  if (isSelfUser(user)) {
    notify('Không thể tự xóa tài khoản quản trị đang đăng nhập (ADMIN_CANNOT_DELETE_SELF)', 'error')
    return
  }
  if (user.role === 'ROLE_ADMIN' && !isSuperAdmin.value) {
    notify('Không được phép xóa tài khoản Super Admin (SUPER_ADMIN_PROTECTED)', 'error')
    return
  }
  userToDelete.value = user
  isConfirmDeleteUserOpen.value = true
}

async function confirmDeleteUser() {
  if (!userToDelete.value) return
  isDeletingUser.value = true
  try {
    await adminApi.deleteUser(userToDelete.value.id)
    notify(`Đã xóa tài khoản @${userToDelete.value.username} khỏi hệ thống`)
    isConfirmDeleteUserOpen.value = false
    userToDelete.value = null
    await fetchUsers()
  } catch (err) {
    notify(err?.message || 'Lỗi khi xóa tài khoản', 'error')
  } finally {
    isDeletingUser.value = false
  }
}

// Reset Password Modal
const userForPasswordReset = ref(null)
const isResetPasswordOpen = ref(false)
const isResettingPassword = ref(false)
const newPasswordInput = ref('')
const passwordResetError = ref('')

function openResetPasswordModal(user) {
  if (user.role === 'ROLE_ADMIN' && !isSuperAdmin.value) {
    notify('Không được phép đặt lại mật khẩu của tài khoản Super Admin (SUPER_ADMIN_PROTECTED)', 'error')
    return
  }
  userForPasswordReset.value = user
  newPasswordInput.value = ''
  passwordResetError.value = ''
  isResetPasswordOpen.value = true
}

async function handleConfirmResetPassword() {
  passwordResetError.value = ''
  if (!newPasswordInput.value || newPasswordInput.value.length < 6) {
    passwordResetError.value = 'Mật khẩu mới phải tối thiểu 6 ký tự'
    return
  }

  isResettingPassword.value = true
  try {
    await adminApi.resetUserPassword(userForPasswordReset.value.id, newPasswordInput.value)
    notify(`Đã đặt lại mật khẩu thành công cho tài khoản @${userForPasswordReset.value.username}`)
    isResetPasswordOpen.value = false
    userForPasswordReset.value = null
  } catch (err) {
    passwordResetError.value = err?.message || 'Lỗi khi đặt lại mật khẩu'
  } finally {
    isResettingPassword.value = false
  }
}

// =========================================================================
// 5. ROLE CRUD & MATRIX
// =========================================================================
const isRoleModalOpen = ref(false)
const isSavingRole = ref(false)
const editingRole = ref(null)
const roleForm = ref({
  code: '',
  name: '',
  description: '',
  category: 'SYSTEM',
  permissions: []
})
const roleFormError = ref('')

function openCreateRoleModal() {
  editingRole.value = null
  roleFormError.value = ''
  roleForm.value = {
    code: '',
    name: '',
    description: '',
    category: 'SYSTEM',
    permissions: []
  }
  isRoleModalOpen.value = true
}

function openEditRoleModal(role) {
  if (role.systemDefault) {
    notify('Không thể chỉnh sửa 3 Role mặc định gốc của hệ thống (SYSTEM_ROLE_PROTECTED)', 'error')
    return
  }
  editingRole.value = role
  roleFormError.value = ''
  roleForm.value = {
    code: role.code,
    name: role.name,
    description: role.description || '',
    category: role.category || 'SYSTEM',
    permissions: [...(role.permissions || [])]
  }
  isRoleModalOpen.value = true
}

function togglePermission(permCode) {
  const perms = roleForm.value.permissions
  const idx = perms.indexOf(permCode)
  if (idx >= 0) {
    perms.splice(idx, 1)
  } else {
    perms.push(permCode)
  }
}

function isModuleFullySelected(module) {
  const codes = module.permissions.map(p => p.code)
  return codes.every(c => roleForm.value.permissions.includes(c))
}

function toggleModulePermissions(module) {
  const codes = module.permissions.map(p => p.code)
  const allSelected = isModuleFullySelected(module)
  if (allSelected) {
    roleForm.value.permissions = roleForm.value.permissions.filter(p => !codes.includes(p))
  } else {
    const toAdd = codes.filter(c => !roleForm.value.permissions.includes(c))
    roleForm.value.permissions.push(...toAdd)
  }
}

async function handleSaveRole() {
  roleFormError.value = ''
  if (!roleForm.value.name.trim()) {
    roleFormError.value = 'Vui lòng nhập tên hiển thị cho Role'
    return
  }

  isSavingRole.value = true
  try {
    if (!editingRole.value) {
      if (!roleForm.value.code.trim()) {
        throw new Error('Vui lòng nhập mã định danh Role (code)')
      }
      let cleanCode = roleForm.value.code.trim().toUpperCase()
      if (!cleanCode.startsWith('ROLE_')) {
        cleanCode = `ROLE_${cleanCode}`
      }

      await adminApi.createRole({
        code: cleanCode,
        name: roleForm.value.name.trim(),
        description: roleForm.value.description.trim(),
        category: 'SYSTEM',
        permissions: roleForm.value.permissions
      })

      notify(`Đã tạo Role mới "${cleanCode}" với ${roleForm.value.permissions.length}/26 quyền API!`)
      isRoleModalOpen.value = false
      await fetchRoles()
    } else {
      await adminApi.updateRole(editingRole.value.id, {
        code: editingRole.value.code,
        name: roleForm.value.name.trim(),
        description: roleForm.value.description.trim(),
        category: 'SYSTEM',
        permissions: roleForm.value.permissions
      })

      if (previewRole.value?.code === editingRole.value.code) {
        startRolePreview({ ...editingRole.value, permissions: roleForm.value.permissions })
      }

      notify(`Đã cập nhật Role "${editingRole.value.name}"`)
      isRoleModalOpen.value = false
      await fetchRoles()
    }
  } catch (err) {
    roleFormError.value = err?.message || 'Lỗi khi lưu vai trò'
  } finally {
    isSavingRole.value = false
  }
}

const roleToDelete = ref(null)
const isConfirmDeleteRoleOpen = ref(false)
const isDeletingRole = ref(false)

function openDeleteRoleModal(roleItem) {
  if (!roleItem) return
  if (roleItem.systemDefault) {
    notify('Không thể xóa các Role mặc định gốc của hệ thống (SYSTEM_ROLE_PROTECTED)', 'error')
    return
  }
  if (!isSuperAdmin.value && customRole.value === roleItem.code) {
    notify('Bạn không được phép tự xóa chính Role mà tài khoản của bạn đang sử dụng (CANNOT_MODIFY_OWN_ROLE)', 'error')
    return
  }
  // Authoritative roleItem.userCount check (No fallback to 0 if undefined)
  if (typeof roleItem.userCount === 'number') {
    if (roleItem.userCount > 0) {
      notify(
        `Không thể xóa Role ${roleItem.code} vì đang có ${roleItem.userCount} tài khoản được gán Role này (ROLE_IN_USE). Vui lòng đổi Role cho người dùng trước.`,
        'error'
      )
      return
    }
  } else if (roleItem.userCount === undefined || roleItem.userCount === null) {
    notify('Chưa có thông tin số lượng người dùng từ máy chủ cho Role này. Vui lòng làm mới danh sách vai trò trước khi xóa.', 'warning')
    return
  }
  roleToDelete.value = roleItem
  isConfirmDeleteRoleOpen.value = true
}

async function confirmDeleteRole() {
  if (!roleToDelete.value) return
  isDeletingRole.value = true
  try {
    await adminApi.deleteRole(roleToDelete.value.id)
    if (previewRole.value?.code === roleToDelete.value.code) {
      stopRolePreview()
    }
    notify(`Đã xóa Role tùy chỉnh ${roleToDelete.value.code}`, 'success')
    isConfirmDeleteRoleOpen.value = false
    roleToDelete.value = null
    await fetchRoles()
  } catch (err) {
    notify(err?.message || 'Lỗi khi xóa vai trò từ máy chủ Backend', 'error')
    // Keep dialog open on rejection so user sees error and can cancel or retry
  } finally {
    isDeletingRole.value = false
  }
}

function handlePreviewRole(roleItem) {
  if (roleItem.category !== 'SYSTEM') {
    notify('Chế độ Xem thử giao diện Admin chỉ áp dụng cho các Role thuộc nhóm 3: Phía hệ thống (Admin)', 'error')
    return
  }
  startRolePreview(roleItem)
  notify(`Đang bật chế độ Xem thử giao diện với Role: ${roleItem.name} (${roleItem.permissions?.length || 0}/26 quyền API)`)
}

// =========================================================================
// 6. FINANCIAL FORCED REFUND MUTATION
// =========================================================================
const selectedBookingForRefund = ref(null)
const isRefundModalOpen = ref(false)
const isRefunding = ref(false)
const isUnknownOutcome = ref(false)
const refundReasonInput = ref('')
const refundError = ref('')

function openRefundModal(booking) {
  selectedBookingForRefund.value = booking
  refundReasonInput.value = ''
  refundError.value = ''
  isUnknownOutcome.value = false
  isRefundModalOpen.value = true
}

async function confirmAdminRefund() {
  refundError.value = ''
  if (!hasPermission('BOOKING_FORCE_REFUND')) {
    refundError.value = 'Bạn không có quyền thực hiện thao tác hoàn tiền cưỡng chế (BOOKING_FORCE_REFUND)'
    return
  }

  if (isRefunding.value || isUnknownOutcome.value) return

  const target = selectedBookingForRefund.value
  if (!target || !target.bookingReference) {
    refundError.value = 'Định danh đơn vé (bookingReference) không hợp lệ'
    return
  }
  const bookingRef = target.bookingReference

  const trimmedReason = (refundReasonInput.value || '').trim()
  if (!trimmedReason || trimmedReason.length < 10) {
    refundError.value = 'Lý do hoàn tiền cưỡng chế phải rõ ràng (tối thiểu 10 ký tự) để lưu Audit Log'
    return
  }
  if (trimmedReason.length > 255) {
    refundError.value = 'Lý do hoàn tiền không được vượt quá 255 ký tự'
    return
  }

  isRefunding.value = true
  try {
    // Contract: reason (max 255), refundPercentage: 100
    const res = await adminApi.refundBooking(bookingRef, {
      reason: trimmedReason,
      refundPercentage: 100
    })

    const data = res?.data || res
    if (!data || typeof data !== 'object') {
      throw new Error('Máy chủ trả về phản hồi không hợp lệ cho lệnh hoàn tiền (Malformed RefundResponse)')
    }

    if (!data.bookingReference || data.bookingReference !== bookingRef) {
      throw new Error('Dữ liệu hoàn tiền không khớp mã đơn vé yêu cầu')
    }

    if (!data.status) {
      throw new Error('Phản hồi hoàn tiền thiếu trạng thái đơn vé từ máy chủ')
    }

    // Explicitly validate refundAmount, supporting valid zero (0đ)
    const hasValidAmount = data.refundAmount !== undefined && data.refundAmount !== null && !isNaN(Number(data.refundAmount))
    const amountStr = hasValidAmount ? `${formatVnd(data.refundAmount)} đ` : '— đ'

    // paymentRefunded validation according to DTO (distinguishing booking status transition from payment outcome)
    if (typeof data.paymentRefunded !== 'boolean') {
      notify(`Đơn vé #${bookingRef} đã chuyển sang trạng thái ${data.status}, nhưng kết quả hoàn tiền cổng thanh toán chưa được xác nhận. Vui lòng đối soát thủ công.`, 'warning')
    } else if (data.paymentRefunded === false) {
      notify(`Đã cập nhật đơn #${bookingRef} sang ${data.status}, nhưng cổng thanh toán từ chối hoàn tiền (${amountStr}). Cần đối soát bổ sung.`, 'warning')
    } else {
      notify(`Đã hoàn tiền thành công ${amountStr} cho đơn vé #${bookingRef} (Trạng thái: ${data.status})!`, 'success')
    }

    isRefundModalOpen.value = false
    selectedBookingForRefund.value = null

    // Authoritative state refetch
    try {
      await fetchBookings()
    } catch (_) {
      notify('Hoàn tiền thành công trên máy chủ nhưng làm mới danh sách đơn vé thất bại. Vui lòng bấm làm mới.', 'warning')
    }
  } catch (err) {
    const errMsg = err?.message || ''
    const isTimeout = err?.code === 'ECONNABORTED' || errMsg.toLowerCase().includes('timeout') || err?.name === 'TimeoutError'
    if (isTimeout) {
      isUnknownOutcome.value = true
      refundError.value = 'Yêu cầu hoàn tiền bị quá hạn phản hồi (Network Timeout). Trạng thái giao dịch tại máy chủ chưa xác định, hệ thống đã KHÓA gửi lại để chống hoàn tiền trùng lặp. Vui lòng bấm Hủy và kiểm tra lại danh sách đơn vé hoặc đối soát thủ công.'
    } else {
      refundError.value = errMsg || 'Máy chủ từ chối yêu cầu hoàn tiền cưỡng chế'
    }
  } finally {
    isRefunding.value = false
  }
}

// =========================================================================
// 7. COLUMNS FOR USERS & BOOKINGS DATATABLE
// =========================================================================
const userColumns = [
  {
    accessorKey: 'username',
    header: 'Người Dùng',
    cell: ({ row }) => {
      const u = row.original
      const self = isSelfUser(u)
      return h('div', { class: 'space-y-0.5' }, [
        h('div', { class: 'flex items-center gap-1.5' }, [
          h('span', { class: 'font-mono font-bold text-foreground text-xs' }, `@${u.username}`),
          self ? h(Badge, { variant: 'outline', class: 'border-primary/40 bg-primary/10 text-primary text-[10px] px-1 py-0 font-bold whitespace-nowrap shrink-0', 'data-testid': 'self-admin-guard' }, () => 'Tài khoản hiện tại (Không thể tự khóa)') : null
        ]),
        h('span', { class: 'text-[11px] text-muted-foreground block' }, u.email)
      ])
    }
  },
  {
    accessorKey: 'fullName',
    header: 'Họ Tên & Liên Hệ',
    cell: ({ row }) => {
      const u = row.original
      return h('div', { class: 'space-y-0.5' }, [
        h('span', { class: 'font-semibold text-foreground text-xs block' }, u.fullName),
        h('span', { class: 'font-mono text-[11px] text-muted-foreground block' }, u.phone || '—')
      ])
    }
  },
  {
    accessorKey: 'category',
    header: 'Phân Mục',
    cell: ({ row }) => {
      const cat = row.original.category || 'SYSTEM'
      const label = cat === 'CUSTOMER' ? 'Khách hàng' : cat === 'OPERATOR' ? 'Chủ xe' : 'Hệ thống'
      const variantClass = cat === 'CUSTOMER'
        ? 'bg-sky-500/10 text-sky-700 dark:text-sky-300 border-sky-500/30'
        : cat === 'OPERATOR'
        ? 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-300 border-emerald-500/30'
        : 'bg-purple-500/10 text-purple-700 dark:text-purple-300 border-purple-500/30'

      return h(Badge, { variant: 'outline', class: `${variantClass} font-semibold text-[11px] whitespace-nowrap shrink-0` }, () => label)
    }
  },
  {
    accessorKey: 'role',
    header: 'Vai Trò (Role)',
    cell: ({ row }) => {
      const roleCode = row.original.role
      const meta = getRoleInfo(roleCode)
      return h('div', { class: 'space-y-0.5' }, [
        h('span', { class: 'font-mono text-[11px] font-bold text-foreground block' }, roleCode),
        h('span', { class: 'text-[11px] text-muted-foreground block' }, meta.name)
      ])
    }
  },
  {
    accessorKey: 'status',
    header: 'Trạng Thái',
    cell: ({ row }) => {
      const status = row.original.status || 'ACTIVE'
      return h(StatusBadge, { status })
    }
  },
  {
    id: 'actions',
    header: () => h('div', { class: 'text-right' }, 'Thao Tác'),
    cell: ({ row }) => {
      const u = row.original
      const self = isSelfUser(u)

      return h('div', { class: 'flex items-center justify-end gap-1.5' }, [
        // Edit button
        h(
          Button,
          {
            variant: 'ghost',
            size: 'sm',
            class: 'h-8 w-8 p-0',
            'data-testid': `btn-edit-user-${u.username}`,
            onClick: () => openEditUserModal(u)
          },
          () => h(Pencil, { class: 'h-3.5 w-3.5' })
        ),
        // Toggle Lock
        h(
          Button,
          {
            variant: 'ghost',
            size: 'sm',
            class: 'h-8 w-8 p-0',
            disabled: self,
            'data-testid': `btn-toggle-lock-${u.username}`,
            onClick: () => toggleUserLock(u)
          },
          () => h(u.status === 'LOCKED' ? Unlock : Lock, { class: 'h-3.5 w-3.5' })
        ),
        // Dropdown for more actions (Reset password, delete)
        h(
          DropdownMenu,
          {},
          {
            default: () => [
              h(DropdownMenuTrigger, { asChild: true }, () =>
                h(Button, { variant: 'ghost', size: 'sm', class: 'h-8 w-8 p-0', 'data-testid': `btn-more-actions-${u.username}` }, () =>
                  h(MoreHorizontal, { class: 'h-3.5 w-3.5' })
                )
              ),
              h(DropdownMenuContent, { align: 'end', class: 'w-44' }, () => [
                h(
                  DropdownMenuItem,
                  {
                    onClick: () => openResetPasswordModal(u),
                    class: 'cursor-pointer'
                  },
                  () => [h(KeyRound, { class: 'h-3.5 w-3.5 mr-2' }), 'Đặt lại mật khẩu']
                ),
                h(DropdownMenuSeparator),
                h(
                  DropdownMenuItem,
                  {
                    disabled: self,
                    onClick: () => openDeleteUserModal(u),
                    class: 'text-destructive focus:bg-destructive/10 cursor-pointer'
                  },
                  () => [h(Trash2, { class: 'h-3.5 w-3.5 mr-2' }), 'Xóa người dùng']
                )
              ])
            ]
          }
        )
      ])
    }
  }
]

const bookingColumns = [
  {
    accessorKey: 'bookingReference',
    header: 'Mã Đơn Vé',
    cell: ({ row }) => {
      const b = row.original
      const ref = b.bookingReference || `BKG-${b.bookingId || b.id}`
      return h('span', { class: 'font-mono font-bold text-foreground text-xs whitespace-nowrap' }, ref)
    }
  },
  {
    accessorKey: 'userId',
    header: 'Khách Hàng / User ID',
    cell: ({ row }) => {
      const b = row.original
      return h('div', { class: 'space-y-0.5' }, [
        h('span', { class: 'font-semibold text-foreground text-xs block' }, b.customerName || `User #${b.userId}`),
        h('span', { class: 'font-mono text-[11px] text-muted-foreground block' }, b.customerPhone || '—')
      ])
    }
  },
  {
    accessorKey: 'tripId',
    header: 'Chuyến Xe / Tuyến',
    cell: ({ row }) => {
      const b = row.original
      return h('div', { class: 'space-y-0.5' }, [
        h('span', { class: 'text-xs text-foreground block font-medium' }, b.route || `Chuyến #${b.tripId}`),
        h('span', { class: 'text-[11px] text-muted-foreground block' }, b.operatorName || '—')
      ])
    }
  },
  {
    accessorKey: 'seatNumbers',
    header: 'Ghế',
    cell: ({ row }) => {
      const seats = row.original.seatNumbers || []
      if (!seats.length) return h('span', { class: 'text-muted-foreground text-xs' }, '—')
      return h('div', { class: 'flex flex-wrap gap-1' }, seats.map(s =>
        h(Badge, { variant: 'outline', class: 'font-mono text-[10px] px-1 py-0' }, () => s)
      ))
    }
  },
  {
    accessorKey: 'totalAmount',
    header: () => h('div', { class: 'text-right' }, 'Tổng Tiền'),
    cell: ({ row }) => {
      const amt = row.original.totalAmount
      return h('div', { class: 'text-right font-mono font-bold tabular-nums whitespace-nowrap text-xs' }, [
        formatVnd(amt),
        h('span', { class: 'text-muted-foreground ml-1 font-normal text-[11px]' }, 'đ')
      ])
    }
  },
  {
    accessorKey: 'status',
    header: 'Trạng Thái',
    cell: ({ row }) => {
      const s = row.original.status || 'CONFIRMED'
      return h(StatusBadge, { status: s })
    }
  },
  {
    id: 'actions',
    header: () => h('div', { class: 'text-right' }, 'Thao Tác'),
    cell: ({ row }) => {
      const b = row.original
      const ref = b.bookingReference || b.id
      const canRefund = hasPermission('BOOKING_FORCE_REFUND')
      const isEligible = b.status === 'CONFIRMED'

      if (!canRefund) {
        return h('div', { class: 'text-right text-[11px] text-muted-foreground' }, 'Chỉ xem (Thiếu quyền hoàn vé)')
      }

      if (!isEligible) {
        return h('div', { class: 'text-right text-[11px] text-muted-foreground' }, b.status === 'REFUNDED' ? 'Đã hoàn tiền' : 'Không khả dụng')
      }

      return h(
        'div',
        { class: 'text-right' },
        h(
          Button,
          {
            variant: 'destructive',
            size: 'sm',
            class: 'h-8 px-2.5 text-xs font-semibold shadow-xs gap-1',
            'data-testid': `btn-admin-refund-${ref}`,
            onClick: () => openRefundModal(b)
          },
          () => [h(Banknote, { class: 'h-3.5 w-3.5' }), 'Hoàn Tiền 100%']
        )
      )
    }
  }
]
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
      <div>
        <h1 class="text-xl sm:text-2xl font-bold text-foreground tracking-tight">
          Quản Trị Người Dùng & Hoàn Tiền Cưỡng Chế (Admin Refund)
        </h1>
        <p class="text-xs text-muted-foreground mt-1">
          Quản lý tài khoản 3 phân mục, ma trận phân quyền 7 Modules × 26 APIs và thực thi hoàn tiền cưỡng chế có kiểm toán.
        </p>
      </div>

      <!-- Preview Mode Banner -->
      <div v-if="previewRole" class="flex items-center gap-2 p-2 rounded-lg bg-amber-500/10 border border-amber-500/30 text-xs">
        <Zap class="w-4 h-4 text-amber-500 flex-shrink-0" />
        <span class="text-amber-800 dark:text-amber-300 font-medium">
          Đang xem thử với: <strong>{{ previewRole.name }}</strong>
        </span>
        <Button size="sm" variant="outline" class="h-6 px-2 text-[11px]" data-testid="btn-stop-preview" @click="stopRolePreview">
          Tắt xem thử
        </Button>
      </div>
    </div>

    <!-- Inline Feedback Notification -->
    <Alert v-if="feedbackMessage" :variant="feedbackType === 'error' ? 'destructive' : 'default'" class="py-2.5 text-xs border" data-testid="admin-feedback-msg">
      <AlertDescription>{{ feedbackMessage }}</AlertDescription>
    </Alert>

    <!-- Main Navigation Tabs -->
    <div class="flex items-center border-b border-border space-x-2">
      <button
        v-if="hasPermission('USER_VIEW')"
        class="px-4 py-2 text-xs font-semibold border-b-2 transition-colors flex items-center gap-1.5"
        :class="activeTab === 'USERS' ? 'border-primary text-primary font-bold' : 'border-transparent text-muted-foreground hover:text-foreground'"
        data-testid="tab-admin-users"
        @click="activeTab = 'USERS'"
      >
        <Users class="w-4 h-4" />
        <span>Người Dùng</span>
      </button>

      <button
        v-if="hasPermission('ROLE_MANAGE_CRUD')"
        class="px-4 py-2 text-xs font-semibold border-b-2 transition-colors flex items-center gap-1.5"
        :class="activeTab === 'ROLES' ? 'border-primary text-primary font-bold' : 'border-transparent text-muted-foreground hover:text-foreground'"
        data-testid="tab-admin-roles"
        @click="activeTab = 'ROLES'"
      >
        <Shield class="w-4 h-4" />
        <span>Vai Trò<span class="hidden sm:inline"> & Phân Quyền (7x26)</span></span>
      </button>

      <button
        v-if="hasPermission('BOOKING_VIEW')"
        class="px-4 py-2 text-xs font-semibold border-b-2 transition-colors flex items-center gap-1.5"
        :class="activeTab === 'BOOKINGS' ? 'border-primary text-primary font-bold' : 'border-transparent text-muted-foreground hover:text-foreground'"
        data-testid="tab-admin-bookings"
        @click="activeTab = 'BOOKINGS'"
      >
        <Ticket class="w-4 h-4" />
        <span>Đơn Vé<span class="hidden sm:inline"> & Hoàn Tiền</span></span>
      </button>
    </div>

    <!-- ================================================================= -->
    <!-- TAB 1: USERS MANAGEMENT                                           -->
    <!-- ================================================================= -->
    <div v-if="activeTab === 'USERS'" class="space-y-4">
      <!-- Toolbar & Category filters -->
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div class="flex items-center gap-1.5 flex-wrap">
          <Button
            size="sm"
            :variant="activeUserCategory === 'ALL' ? 'default' : 'outline'"
            class="h-8 text-xs font-semibold"
            data-testid="category-tab-ALL"
            @click="handleCategoryChange('ALL')"
          >
            Tất cả
          </Button>
          <Button
            size="sm"
            :variant="activeUserCategory === 'CUSTOMER' ? 'default' : 'outline'"
            class="h-8 text-xs font-semibold"
            data-testid="category-tab-CUSTOMER"
            @click="handleCategoryChange('CUSTOMER')"
          >
            Khách hàng
          </Button>
          <Button
            size="sm"
            :variant="activeUserCategory === 'OPERATOR' ? 'default' : 'outline'"
            class="h-8 text-xs font-semibold"
            data-testid="category-tab-OPERATOR"
            @click="handleCategoryChange('OPERATOR')"
          >
            Chủ xe
          </Button>
          <Button
            size="sm"
            :variant="activeUserCategory === 'SYSTEM' ? 'default' : 'outline'"
            class="h-8 text-xs font-semibold"
            data-testid="category-tab-SYSTEM"
            @click="handleCategoryChange('SYSTEM')"
          >
            Phía hệ thống
          </Button>
        </div>

        <div class="flex items-center gap-2">
          <div class="relative w-full sm:w-64">
            <Search class="w-3.5 h-3.5 absolute left-2.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
            <Input
              v-model="userSearchKeyword"
              placeholder="Tìm username, email, họ tên..."
              class="h-8 pl-8 text-xs"
              data-testid="input-user-search"
              @keydown.enter="handleSearchUsers"
            />
          </div>
          <Button
            v-if="hasPermission('USER_CREATE_UPDATE_DELETE')"
            size="sm"
            class="h-8 px-3 text-xs font-semibold gap-1 shadow-xs"
            data-testid="btn-open-create-user"
            @click="openCreateUserModal(null)"
          >
            <Plus class="w-3.5 h-3.5" />
            <span>Thêm</span>
          </Button>
        </div>
      </div>

      <!-- Error State -->
      <Alert v-if="usersError" variant="destructive">
        <AlertCircle class="h-4 w-4" />
        <AlertTitle>Lỗi tải danh sách người dùng</AlertTitle>
        <AlertDescription class="flex items-center justify-between text-xs mt-1">
          <span>{{ usersError }}</span>
          <Button size="sm" variant="outline" class="h-6 px-2 text-xs bg-background text-foreground" @click="fetchUsers">
            Thử lại
          </Button>
        </AlertDescription>
      </Alert>

      <!-- Users Reusable TanStack DataTable -->
      <DataTable
        :columns="userColumns"
        :data="users"
        :loading="isLoadingUsers"
        :page-size="userPageSize"
        :manual-pagination="true"
        :page-index="userPage"
        :page-count="userTotalPages"
        :total-elements="userTotalElements"
        :get-row-id="(row) => String(row.id || row.username)"
        empty-text="Không tìm thấy người dùng nào phù hợp."
        @update:page-index="onUserPageChange"
      />
    </div>

    <!-- ================================================================= -->
    <!-- TAB 2: SYSTEM ROLES & 7x26 PERMISSION MATRIX                      -->
    <!-- ================================================================= -->
    <div v-if="activeTab === 'ROLES'" class="space-y-4">
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 class="text-sm font-bold text-foreground">
            Phân Quyền Động Theo Ma Trận 7 Modules × 26 APIs
          </h2>
          <p class="text-xs text-muted-foreground">
            Định nghĩa các Custom Role cho nhân viên hệ thống và cấu hình danh mục API truy cập an toàn.
          </p>
        </div>

        <Button
          v-if="hasPermission('ROLE_MANAGE_CRUD')"
          size="sm"
          class="h-8 px-3 text-xs font-semibold gap-1 shadow-xs self-start sm:self-auto"
          data-testid="btn-create-role-in-tab"
          @click="openCreateRoleModal"
        >
          <Plus class="w-3.5 h-3.5" />
          <span>Tạo Role Mới</span>
        </Button>
      </div>

      <!-- Error Alert -->
      <Alert v-if="rolesError" variant="destructive">
        <AlertCircle class="h-4 w-4" />
        <AlertTitle>Lỗi tải danh sách vai trò</AlertTitle>
        <AlertDescription class="flex items-center justify-between text-xs mt-1">
          <span>{{ rolesError }}</span>
          <Button size="sm" variant="outline" class="h-6 px-2 text-xs bg-background text-foreground" @click="fetchRoles">
            Thử lại
          </Button>
        </AlertDescription>
      </Alert>

      <!-- Roles Grid -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <Card
          v-for="role in systemRoles"
          :key="role.code"
          class="border-border bg-card shadow-xs flex flex-col justify-between"
          :data-testid="`role-card-${role.code}`"
        >
          <CardHeader class="pb-2">
            <div class="flex items-start justify-between gap-2">
              <div>
                <CardTitle class="text-xs font-bold text-foreground font-mono">
                  {{ role.code }}
                </CardTitle>
                <CardDescription class="text-xs font-semibold text-primary mt-0.5">
                  {{ role.name }}
                </CardDescription>
              </div>
              <Badge
                variant="outline"
                :class="role.systemDefault ? 'border-primary/30 bg-primary/10 text-primary' : 'border-purple-500/30 bg-purple-500/10 text-purple-700 dark:text-purple-300'"
                class="text-[10px] font-bold"
              >
                {{ role.systemDefault ? 'Mặc định' : 'Custom Role' }}
              </Badge>
            </div>
          </CardHeader>

          <CardContent class="space-y-3 pt-0">
            <p class="text-xs text-muted-foreground leading-relaxed line-clamp-2">
              {{ role.description || 'Chưa có mô tả chi tiết cho vai trò này.' }}
            </p>

            <div class="text-[11px] text-muted-foreground flex items-center justify-between border-t border-border/50 pt-2">
              <span>Quyền API: <strong>{{ role.permissions?.length || 0 }}/26</strong></span>
              <span>Gán: <strong>{{ role.userCount ?? 0 }}</strong> user</span>
            </div>

            <!-- Role Action Toolbar -->
            <div class="flex items-center justify-between pt-1">
              <Button
                v-if="role.category === 'SYSTEM'"
                variant="outline"
                size="sm"
                class="h-7 px-2 text-[11px] gap-1"
                :data-testid="`preview-role-${role.code}`"
                @click="handlePreviewRole(role)"
              >
                <Zap class="w-3 h-3 text-amber-500" />
                <span>Xem thử</span>
              </Button>
              <div v-else />

              <div v-if="!role.systemDefault" class="flex items-center gap-1">
                <Button
                  variant="ghost"
                  size="sm"
                  class="h-7 w-7 p-0"
                  :data-testid="`btn-edit-role-${role.code}`"
                  @click="openEditRoleModal(role)"
                >
                  <Pencil class="h-3 w-3" />
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  class="h-7 w-7 p-0 text-destructive hover:bg-destructive/10"
                  :data-testid="`btn-delete-role-${role.code}`"
                  @click="openDeleteRoleModal(role)"
                >
                  <Trash2 class="h-3 w-3" />
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>

    <!-- ================================================================= -->
    <!-- TAB 3: BOOKINGS MANAGEMENT & ADMIN FORCED REFUND                  -->
    <!-- ================================================================= -->
    <div v-if="activeTab === 'BOOKINGS'" class="space-y-4">
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 class="text-sm font-bold text-foreground">
            Danh Sách Đơn Vé Toàn Sàn & Hoàn Tiền Cưỡng Chế
          </h2>
          <p class="text-xs text-muted-foreground">
            Tra cứu đơn vé của hành khách và thực thi lệnh hoàn tiền 100% khi có tranh chấp hoặc khiếu nại.
          </p>
        </div>

        <Button
          variant="outline"
          size="sm"
          class="h-8 px-3 text-xs font-semibold gap-1 shadow-xs self-start sm:self-auto"
          :disabled="isLoadingBookings"
          @click="fetchBookings"
        >
          <RefreshCw class="w-3.5 h-3.5" :class="{ 'animate-spin': isLoadingBookings }" />
          <span>Làm Mới</span>
        </Button>
      </div>

      <!-- Error State -->
      <Alert v-if="bookingsError" variant="destructive">
        <AlertCircle class="h-4 w-4" />
        <AlertTitle>Lỗi tải danh sách đơn vé</AlertTitle>
        <AlertDescription class="flex items-center justify-between text-xs mt-1">
          <span>{{ bookingsError }}</span>
          <Button size="sm" variant="outline" class="h-6 px-2 text-xs bg-background text-foreground" @click="fetchBookings">
            Thử lại
          </Button>
        </AlertDescription>
      </Alert>

      <!-- Bookings Reusable TanStack DataTable -->
      <DataTable
        :columns="bookingColumns"
        :data="allBookings"
        :loading="isLoadingBookings"
        :page-size="bookingPageSize"
        :manual-pagination="true"
        :page-index="bookingPage"
        :page-count="bookingTotalPages"
        :total-elements="bookingTotalElements"
        :get-row-id="(row) => String(row.bookingReference || row.bookingId || row.id)"
        empty-text="Hiện không có đơn vé nào trong hệ thống."
        @update:page-index="onBookingPageChange"
      />
    </div>

    <!-- ================================================================= -->
    <!-- DIALOGS & ALERT DIALOGS                                            -->
    <!-- ================================================================= -->

    <!-- 1. Create/Edit User Dialog -->
    <Dialog v-model:open="isUserModalOpen">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{{ editingUser ? 'Chỉnh Sửa Người Dùng' : 'Tạo Tài Khoản Mới' }}</DialogTitle>
          <DialogDescription class="text-xs">
            {{ editingUser ? 'Cập nhật thông tin hồ sơ và vai trò của người dùng.' : 'Điền đầy đủ thông tin để khởi tạo tài khoản trong hệ thống.' }}
          </DialogDescription>
        </DialogHeader>

        <Alert v-if="userFormError" variant="destructive" class="py-2 text-xs">
          <AlertCircle class="h-4 w-4" />
          <AlertDescription>{{ userFormError }}</AlertDescription>
        </Alert>

        <form @submit.prevent="handleSaveUser" class="space-y-3 text-xs">
          <FormField label="Tên đăng nhập (Username)" required>
            <Input
              v-model="userForm.username"
              :disabled="Boolean(editingUser) || isSavingUser"
              placeholder="VD: nguyenan"
              data-testid="input-username"
            />
          </FormField>

          <FormField label="Họ và tên" required>
            <Input
              v-model="userForm.fullName"
              :disabled="isSavingUser"
              placeholder="VD: Nguyễn Văn An"
              data-testid="input-fullname"
            />
          </FormField>

          <div class="grid grid-cols-2 gap-2">
            <FormField label="Email" required>
              <Input
                v-model="userForm.email"
                type="email"
                :disabled="isSavingUser"
                placeholder="an@example.com"
                data-testid="input-email"
              />
            </FormField>
            <FormField label="Số điện thoại">
              <Input
                v-model="userForm.phone"
                :disabled="isSavingUser"
                placeholder="0912345678"
                data-testid="input-phone"
              />
            </FormField>
          </div>

          <FormField v-if="!editingUser" label="Mật khẩu khởi tạo" required>
            <PasswordInput
              v-model="userForm.password"
              :disabled="isSavingUser"
              placeholder="Tối thiểu 6 ký tự"
              data-testid="input-password"
            />
          </FormField>

          <div class="grid grid-cols-2 gap-2">
            <FormField label="Phân mục">
              <select
                v-model="userForm.category"
                :disabled="isSavingUser"
                class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-xs ring-offset-background"
                @change="onUserFormCategoryChange"
              >
                <option value="CUSTOMER">Khách hàng</option>
                <option value="OPERATOR">Chủ xe</option>
                <option value="SYSTEM">Phía hệ thống</option>
              </select>
            </FormField>

            <FormField label="Vai trò (Role)">
              <select
                v-model="userForm.role"
                :disabled="isSavingUser"
                class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-xs ring-offset-background"
                data-testid="select-user-role"
              >
                <option v-for="r in availableRolesForSelectedCategory" :key="r.code" :value="r.code">
                  {{ r.name }} ({{ r.code }})
                </option>
              </select>
            </FormField>
          </div>

          <DialogFooter class="pt-3">
            <Button type="button" variant="outline" :disabled="isSavingUser" @click="closeUserModal">
              Hủy
            </Button>
            <Button type="submit" :disabled="isSavingUser" data-testid="btn-save-user" @click.prevent="handleSaveUser">
              <RefreshCw v-if="isSavingUser" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
              <span>{{ isSavingUser ? 'Đang lưu...' : (editingUser ? 'Cập nhật' : 'Tạo mới') }}</span>
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>

    <!-- 2. Reset Password Dialog -->
    <Dialog v-model:open="isResetPasswordOpen">
      <DialogContent class="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>Đặt Lại Mật Khẩu</DialogTitle>
          <DialogDescription class="text-xs">
            Cấp mật khẩu mới cho tài khoản <strong class="text-foreground">@{{ userForPasswordReset?.username }}</strong>.
          </DialogDescription>
        </DialogHeader>

        <Alert v-if="passwordResetError" variant="destructive" class="py-2 text-xs">
          <AlertCircle class="h-4 w-4" />
          <AlertDescription>{{ passwordResetError }}</AlertDescription>
        </Alert>

        <form @submit.prevent="handleConfirmResetPassword" class="space-y-3 text-xs">
          <FormField label="Mật khẩu mới" required>
            <PasswordInput
              v-model="newPasswordInput"
              :disabled="isResettingPassword"
              placeholder="Tối thiểu 6 ký tự"
              data-testid="input-new-password"
            />
          </FormField>

          <DialogFooter class="pt-2">
            <Button type="button" variant="outline" :disabled="isResettingPassword" @click="isResetPasswordOpen = false">
              Hủy
            </Button>
            <Button type="submit" :disabled="isResettingPassword" data-testid="btn-confirm-reset-pwd" @click.prevent="handleConfirmResetPassword">
              <RefreshCw v-if="isResettingPassword" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
              <span>Xác nhận</span>
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>

    <!-- 3. Delete User AlertDialog -->
    <AlertDialog v-model:open="isConfirmDeleteUserOpen">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle class="flex items-center gap-2">
            <AlertTriangle class="w-5 h-5 text-destructive" />
            <span>Xác nhận xóa người dùng</span>
          </AlertDialogTitle>
          <AlertDialogDescription class="text-xs mt-2">
            Bạn có chắc chắn muốn xóa tài khoản <strong class="text-foreground">@{{ userToDelete?.username }}</strong>? Hành động này không thể hoàn tác.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel :disabled="isDeletingUser" @click="isConfirmDeleteUserOpen = false">Hủy</AlertDialogCancel>
          <Button
            :disabled="isDeletingUser"
            variant="destructive"
            @click="confirmDeleteUser"
          >
            {{ isDeletingUser ? 'Đang xóa...' : 'Xác nhận xóa' }}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <!-- 4. Create/Edit Role Dialog & Permission Matrix -->
    <Dialog v-model:open="isRoleModalOpen">
      <DialogContent class="sm:max-w-2xl max-h-[90vh] flex flex-col p-6">
        <DialogHeader>
          <DialogTitle>{{ editingRole ? `Sửa Vai Trò: ${editingRole.name}` : 'Tạo Role Mới' }}</DialogTitle>
          <DialogDescription class="text-xs">
            Cấu hình mã quyền và gán quyền chi tiết theo ma trận 7 Modules × 26 APIs.
          </DialogDescription>
        </DialogHeader>

        <Alert v-if="roleFormError" variant="destructive" class="py-2 text-xs shrink-0 my-2">
          <AlertCircle class="h-4 w-4" />
          <AlertDescription>{{ roleFormError }}</AlertDescription>
        </Alert>

        <form @submit.prevent="handleSaveRole" class="flex flex-col flex-1 min-h-0 space-y-4 text-xs">
          <div class="overflow-y-auto max-h-[58vh] pr-2 space-y-4">
            <div class="grid grid-cols-2 gap-3">
              <FormField label="Mã Role (code)" required>
                <Input
                  v-model="roleForm.code"
                  :disabled="Boolean(editingRole) || isSavingRole"
                  placeholder="VD: CONTENT_MOD"
                  data-testid="input-role-code"
                />
              </FormField>

              <FormField label="Tên hiển thị" required>
                <Input
                  v-model="roleForm.name"
                  :disabled="isSavingRole"
                  placeholder="VD: Kiểm Duyệt Nội Dung"
                  data-testid="input-role-name"
                />
              </FormField>
            </div>

            <FormField label="Mô tả chức năng">
              <Input
                v-model="roleForm.description"
                :disabled="isSavingRole"
                placeholder="Mô tả phạm vi trách nhiệm của vai trò này"
              />
            </FormField>

            <!-- 7 Modules x 26 Permissions Matrix -->
            <div class="space-y-3 border-t border-border pt-3">
              <div class="flex items-center justify-between">
                <span class="font-bold text-foreground">
                  Ma Trận Phân Quyền (Đã chọn: {{ roleForm.permissions.length }}/26 APIs)
                </span>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  class="h-6 px-2 text-[11px]"
                  data-testid="btn-role-clear-all"
                  @click="roleForm.permissions = []"
                >
                  Bỏ chọn tất cả
                </Button>
              </div>

              <div class="space-y-3">
                <div
                  v-for="mod in permissionCatalog"
                  :key="mod.moduleCode"
                  class="rounded-lg border border-border p-3 space-y-2 bg-muted/20"
                >
                  <div class="flex items-center justify-between border-b border-border/50 pb-2">
                    <span class="font-bold text-foreground text-xs" :data-testid="`module-title-${mod.moduleCode}`">{{ mod.moduleName }}</span>
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      class="h-6 px-2 text-[11px]"
                      :data-testid="`toggle-module-${mod.moduleCode}`"
                      @click="toggleModulePermissions(mod)"
                    >
                      {{ isModuleFullySelected(mod) ? 'Bỏ chọn cả nhóm' : 'Chọn cả nhóm' }}
                    </Button>
                  </div>

                  <div class="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1">
                    <label
                      v-for="perm in mod.permissions"
                      :key="perm.code"
                      class="flex items-start gap-2 p-1.5 rounded hover:bg-muted/40 cursor-pointer"
                    >
                      <Checkbox
                        :checked="roleForm.permissions.includes(perm.code)"
                        :data-testid="`perm-checkbox-${perm.code}`"
                        @update:checked="togglePermission(perm.code)"
                      />
                      <div class="space-y-0.5">
                        <span class="font-medium text-foreground block text-xs" :data-testid="`perm-name-${perm.code}`">{{ perm.name }}</span>
                        <span class="font-mono text-[10px] text-muted-foreground block" :data-testid="`perm-code-${perm.code}`">{{ perm.code }}</span>
                      </div>
                    </label>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <DialogFooter class="border-t border-border pt-3 shrink-0">
            <Button type="button" variant="outline" :disabled="isSavingRole" @click="isRoleModalOpen = false">
              Hủy
            </Button>
            <Button type="submit" :disabled="isSavingRole" data-testid="btn-save-role" @click.prevent="handleSaveRole">
              <RefreshCw v-if="isSavingRole" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
              <span>{{ isSavingRole ? 'Đang lưu...' : 'Lưu Thay Đổi' }}</span>
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>

    <!-- 5. Delete Role AlertDialog -->
    <AlertDialog v-model:open="isConfirmDeleteRoleOpen">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle class="flex items-center gap-2">
            <AlertTriangle class="w-5 h-5 text-destructive" />
            <span>Xác nhận xóa vai trò</span>
          </AlertDialogTitle>
          <AlertDialogDescription class="text-xs mt-2">
            Bạn có chắc chắn muốn xóa Role <strong class="text-foreground font-mono">{{ roleToDelete?.code }}</strong>? Thao tác này sẽ xóa vĩnh viễn cấu hình phân quyền của Role này.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel :disabled="isDeletingRole" @click="isConfirmDeleteRoleOpen = false">Hủy</AlertDialogCancel>
          <Button
            :disabled="isDeletingRole"
            variant="destructive"
            @click="confirmDeleteRole"
          >
            {{ isDeletingRole ? 'Đang xóa...' : 'Xác nhận xóa' }}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <!-- 6. Admin Forced Refund Dialog (Section 5) -->
    <Dialog v-model:open="isRefundModalOpen">
      <DialogContent class="sm:max-w-md">
        <div data-testid="dialog-refund" class="space-y-4">
        <DialogHeader>
          <DialogTitle class="flex items-center gap-2 text-destructive">
            <Banknote class="w-5 h-5" />
            <span>Lệnh Hoàn Tiền Cưỡng Chế 100% (Admin Force Refund)</span>
          </DialogTitle>
          <DialogDescription class="text-xs space-y-2 mt-2">
            <p>
              Đơn vé: <strong class="text-foreground font-mono">{{ selectedBookingForRefund?.bookingReference }}</strong>
            </p>
            <p>
              Số tiền hoàn: <strong class="text-destructive font-mono text-sm">{{ formatVnd(selectedBookingForRefund?.totalAmount) }} đ (100%)</strong>
            </p>
            <p class="text-muted-foreground">
              Lệnh này sẽ hoàn tiền 100% về tài khoản của hành khách, thu hồi số dư tương ứng từ ví ký quỹ Escrow của nhà xe, và ghi nhận kiểm toán bắt buộc.
            </p>
          </DialogDescription>
        </DialogHeader>

        <Alert v-if="refundError" variant="destructive" class="py-2 text-xs">
          <AlertCircle class="h-4 w-4" />
          <AlertDescription>{{ refundError }}</AlertDescription>
        </Alert>

        <form @submit.prevent="confirmAdminRefund" class="space-y-3 text-xs">
          <FormField label="Lý do hoàn tiền kiểm toán (Audit Reason)" required>
            <Textarea
              v-model="refundReasonInput"
              :disabled="isRefunding"
              placeholder="Nhập lý do chi tiết (tối thiểu 10 ký tự, tối đa 255 ký tự)..."
              :rows="3"
              data-testid="input-refund-reason"
            />
          </FormField>

          <DialogFooter class="pt-2">
            <Button type="button" variant="outline" :disabled="isRefunding" @click="isRefundModalOpen = false">
              Hủy bỏ
            </Button>
            <Button
              type="submit"
              variant="destructive"
              :disabled="isRefunding || isUnknownOutcome"
              data-testid="btn-confirm-refund"
              @click.prevent="confirmAdminRefund"
            >
              <RefreshCw v-if="isRefunding" class="w-3.5 h-3.5 mr-1.5 animate-spin" />
              <span>{{ isRefunding ? 'Đang thực thi...' : 'Xác Nhận Hoàn Tiền' }}</span>
            </Button>
          </DialogFooter>
        </form>
        </div>
      </DialogContent>
    </Dialog>
  </div>
</template>
