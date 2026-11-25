<script setup>
import { ref, computed, watch } from 'vue'
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
  Zap
} from 'lucide-vue-next'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import FormField from '@/shared/components/FormField.vue'
import AppButton from '@/shared/components/AppButton.vue'
import { ADMIN_PERMISSION_MODULES, ALL_ADMIN_PERMISSION_CODES } from '@/modules/auth/store'
import { useAdminPermissions } from '@/modules/admin/composables/useAdminPermissions'
import { adminApi } from '@/modules/admin/api'

const {
  isSuperAdmin,
  customRole,
  previewRole,
  hasPermission,
  startRolePreview,
  stopRolePreview
} = useAdminPermissions()

// Active main tab: 'USERS' | 'ROLES' | 'BOOKINGS' — FND-003: Tự động chọn Tab đầu tiên mà user có quyền xem
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

// Active user category filter inside USERS tab: 'ALL' | 'CUSTOMER' | 'OPERATOR' | 'SYSTEM'
const activeUserCategory = ref('ALL')
const userSearchKeyword = ref('')
const feedbackMessage = ref('')
const feedbackType = ref('success') // 'success' | 'error'

function notify(msg, type = 'success') {
  feedbackMessage.value = msg
  feedbackType.value = type
  if (type === 'error') {
    toast.error(msg)
  } else {
    toast.success(msg)
  }
  setTimeout(() => {
    if (feedbackMessage.value === msg) {
      feedbackMessage.value = ''
    }
  }, 4500)
}

// =========================================================================
// 1. DYNAMIC SYSTEM ROLES & 7 MODULES x 26 APIs MATRIX
// =========================================================================
const permissionCatalog = ref(ADMIN_PERMISSION_MODULES)

const systemRoles = ref([
  {
    id: 1,
    code: 'ROLE_ADMIN',
    name: 'Quản Trị Viên Toàn Sàn (Super Admin)',
    description: 'Toàn quyền điều hành hệ thống, quản lý 7 Modules và 26 APIs quản trị.',
    category: 'SYSTEM',
    permissions: [...ALL_ADMIN_PERMISSION_CODES],
    systemDefault: true
  },
  {
    id: 2,
    code: 'ROLE_OPERATOR',
    name: 'Chủ Xe / Đối Tác Vận Tải',
    description: 'Vai trò dành cho đối tác nhà xe (Quản lý đội xe, tài xế, chuyến xe và ví ký quỹ tại /operator).',
    category: 'OPERATOR',
    permissions: [],
    systemDefault: true
  },
  {
    id: 3,
    code: 'ROLE_CUSTOMER',
    name: 'Khách Hàng Đặt Vé',
    description: 'Vai trò dành cho hành khách tìm chuyến, chọn ghế, thanh toán và tra cứu vé.',
    category: 'CUSTOMER',
    permissions: [],
    systemDefault: true
  },
  {
    id: 4,
    code: 'ROLE_ACCOUNTANT',
    name: 'Kế Toán & Đối Soát Tài Chính',
    description: 'Chuyên trách báo cáo doanh thu, xử lý đối soát VNPay, giải ngân ví Escrow và hoa hồng nhà xe.',
    category: 'SYSTEM',
    permissions: [
      'PAYMENT_REPORT_VIEW',
      'PAYMENT_LIST_VIEW',
      'PAYMENT_RECONCILE_RESOLVE',
      'WITHDRAWAL_VIEW',
      'WITHDRAWAL_APPROVE',
      'WITHDRAWAL_REJECT',
      'OPERATOR_VIEW',
      'OPERATOR_COMMISSION_MANAGE',
      'BOOKING_VIEW',
      'BOOKING_FORCE_REFUND'
    ],
    systemDefault: false
  },
  {
    id: 5,
    code: 'ROLE_CSKH',
    name: 'CSKH & Điều Hành Vận Tải',
    description: 'Hỗ trợ khách hàng, tra cứu/hoàn vé, duyệt nhà xe, kiểm duyệt banner và quản lý bến/chuyến xe.',
    category: 'SYSTEM',
    permissions: [
      'OPERATOR_VIEW',
      'OPERATOR_APPROVE_REJECT',
      'BANNER_VIEW',
      'BANNER_CREATE_DELETE',
      'BANNER_APPROVE_REJECT',
      'USER_VIEW',
      'USER_STATUS_LOCK',
      'USER_PASSWORD_RESET',
      'BOOKING_VIEW',
      'BOOKING_FORCE_REFUND',
      'STATION_VIEW',
      'STATION_CREATE_UPDATE_DELETE',
      'TRIP_MANAGE_CRUD'
    ],
    systemDefault: false
  }
])

// =========================================================================
// 2. USERS LIST (3 CATEGORIES: CUSTOMER, OPERATOR, SYSTEM STAFF)
// =========================================================================
const users = ref([
  {
    id: 'USR-ADMIN-01',
    username: 'admin',
    fullName: 'Quản Trị Viên Toàn Sàn',
    email: 'admin@velobus.vn',
    phone: '0901000001',
    role: 'ROLE_ADMIN',
    category: 'SYSTEM',
    status: 'ACTIVE',
    isSelf: true
  },
  {
    id: 'USR-SYS-02',
    username: 'ketoan01',
    fullName: 'Lê Thị Thu Hà (Kế Toán)',
    email: 'ketoan@velobus.vn',
    phone: '0901000088',
    role: 'ROLE_ACCOUNTANT',
    category: 'SYSTEM',
    status: 'ACTIVE',
    isSelf: false
  },
  {
    id: 'USR-SYS-03',
    username: 'cskh01',
    fullName: 'Trần Quốc Bảo (Điều Hành & CSKH)',
    email: 'cskh@velobus.vn',
    phone: '0901000099',
    role: 'ROLE_CSKH',
    category: 'SYSTEM',
    status: 'ACTIVE',
    isSelf: false
  },
  {
    id: 'USR-OPER-01',
    username: 'operator1',
    fullName: 'Nguyễn Hoàng Long',
    email: 'hoanglong@velobus.vn',
    phone: '0901234567',
    role: 'ROLE_OPERATOR',
    category: 'OPERATOR',
    status: 'ACTIVE',
    isSelf: false
  },
  {
    id: 'USR-OPER-02',
    username: 'phuongtrang_op',
    fullName: 'Lê Minh Phương (Phương Trang Velo)',
    email: 'phuongtrang@velobus.vn',
    phone: '0918765432',
    role: 'ROLE_OPERATOR',
    category: 'OPERATOR',
    status: 'ACTIVE',
    isSelf: false
  },
  {
    id: 'USR-CUST-01',
    username: 'thanhpv',
    fullName: 'Phan Văn Thành',
    email: 'thanh.pv@gmail.com',
    phone: '0912345678',
    role: 'ROLE_CUSTOMER',
    category: 'CUSTOMER',
    status: 'ACTIVE',
    isSelf: false
  },
  {
    id: 'USR-CUST-02',
    username: 'quantm',
    fullName: 'Trần Minh Quân',
    email: 'quan.tm@gmail.com',
    phone: '0988776655',
    role: 'ROLE_CUSTOMER',
    category: 'CUSTOMER',
    status: 'ACTIVE',
    isSelf: false
  }
])

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

function getCategoryLabel(category) {
  if (category === 'CUSTOMER') return '1. Khách hàng (Customer)'
  if (category === 'OPERATOR') return '2. Chủ xe (Operator)'
  return '3. Phía hệ thống (Admin)'
}

function getCategoryBadgeClass(category) {
  if (category === 'CUSTOMER') return 'bg-sky-500/15 text-sky-700 dark:text-sky-300 border-sky-500/30'
  if (category === 'OPERATOR') return 'bg-emerald-500/15 text-emerald-700 dark:text-emerald-300 border-emerald-500/30'
  return 'bg-purple-500/15 text-purple-700 dark:text-purple-300 border-purple-500/30'
}

const categoryCounts = computed(() => {
  const counts = { ALL: users.value.length, CUSTOMER: 0, OPERATOR: 0, SYSTEM: 0 }
  for (const u of users.value) {
    const cat = u.category || getRoleInfo(u.role).category
    if (counts[cat] !== undefined) counts[cat]++
  }
  return counts
})

const filteredUsers = computed(() => {
  return users.value.filter(u => {
    const cat = u.category || getRoleInfo(u.role).category
    if (activeUserCategory.value !== 'ALL' && cat !== activeUserCategory.value) {
      return false
    }
    if (userSearchKeyword.value.trim()) {
      const kw = userSearchKeyword.value.trim().toLowerCase()
      return (
        u.username.toLowerCase().includes(kw) ||
        u.fullName.toLowerCase().includes(kw) ||
        u.email.toLowerCase().includes(kw) ||
        u.role.toLowerCase().includes(kw)
      )
    }
    return true
  })
})

function countUsersWithRole(roleCode) {
  return users.value.filter(u => u.role === roleCode).length
}

// =========================================================================
// 3. USER CRUD (CREATE, EDIT, ASSIGN ROLE, LOCK/UNLOCK, RESET PASSWORD, DELETE)
// =========================================================================
const isUserModalOpen = ref(false)
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

  // Chốt chặn PC-02: Bảo vệ Super Admin
  if (userForm.value.role === 'ROLE_ADMIN' && !isSuperAdmin.value) {
    userFormError.value = 'Chỉ Super Admin (ROLE_ADMIN) mới có quyền tạo hoặc gán quyền ROLE_ADMIN'
    return
  }

  const roleMeta = getRoleInfo(userForm.value.role)

  if (!editingUser.value) {
    if (!userForm.value.username.trim() || userForm.value.username.trim().length < 3) {
      userFormError.value = 'Tên đăng nhập (Username) phải từ 3 ký tự trở lên'
      return
    }
    if (!userForm.value.password || userForm.value.password.length < 6) {
      userFormError.value = 'Mật khẩu khởi tạo phải tối thiểu 6 ký tự'
      return
    }
    if (users.value.some(u => u.username.toLowerCase() === userForm.value.username.trim().toLowerCase())) {
      userFormError.value = 'Tên đăng nhập này đã tồn tại trên hệ thống'
      return
    }

    const prefix = roleMeta.category === 'CUSTOMER' ? 'USR-CUST-' : roleMeta.category === 'OPERATOR' ? 'USR-OPER-' : 'USR-SYS-'
    const newUser = {
      id: `${prefix}${String(users.value.length + 1).padStart(2, '0')}`,
      username: userForm.value.username.trim(),
      fullName: userForm.value.fullName.trim(),
      email: userForm.value.email.trim(),
      phone: userForm.value.phone.trim(),
      role: roleMeta.code,
      category: roleMeta.category,
      status: 'ACTIVE',
      isSelf: false
    }
    users.value.unshift(newUser)
    notify(`Đã tạo tài khoản @${newUser.username} với Role ${roleMeta.name} (${roleMeta.code})`)
    adminApi.createUser({
      username: newUser.username,
      fullName: newUser.fullName,
      email: newUser.email,
      phone: newUser.phone,
      password: userForm.value.password,
      role: newUser.role
    }).then(res => {
      if (res?.data?.id) newUser.id = res.data.id
    }).catch(() => {})
  } else {
    if (editingUser.value.isSelf && userForm.value.role !== editingUser.value.role) {
      userFormError.value = 'Không thể tự hạ quyền hoặc đổi Role của chính tài khoản đang đăng nhập (ADMIN_CANNOT_DEMOTE_SELF)'
      return
    }
    editingUser.value.fullName = userForm.value.fullName.trim()
    editingUser.value.email = userForm.value.email.trim()
    editingUser.value.phone = userForm.value.phone.trim()
    editingUser.value.role = roleMeta.code
    editingUser.value.category = roleMeta.category
    adminApi.updateUser(editingUser.value.id, {
      fullName: editingUser.value.fullName,
      email: editingUser.value.email,
      phone: editingUser.value.phone,
      role: editingUser.value.role
    }).catch(() => {})
    notify(`Đã cập nhật thông tin tài khoản @${editingUser.value.username}`)
  }

  closeUserModal()
}

async function handleQuickRoleChange(user, newRoleCode) {
  if (user.isSelf) {
    notify('Không thể tự thay đổi Role của tài khoản đang đăng nhập (ADMIN_CANNOT_DEMOTE_SELF)', 'error')
    return
  }
  if ((user.role === 'ROLE_ADMIN' || newRoleCode === 'ROLE_ADMIN') && !isSuperAdmin.value) {
    notify('Nhân sự mang Custom Role không được phép cấp hoặc thu hồi quyền ROLE_ADMIN (SUPER_ADMIN_PROTECTED)', 'error')
    return
  }
  const roleMeta = getRoleInfo(newRoleCode)
  user.role = roleMeta.code
  user.category = roleMeta.category
  try {
    await adminApi.changeUserRole(user.id, roleMeta.code)
  } catch (_) {}
  notify(`Đã gán Role "${roleMeta.name} (${roleMeta.code})" cho tài khoản @${user.username}`)
}

async function toggleUserLock(user) {
  if (user.isSelf) return // Invariant: ADMIN_CANNOT_LOCK_SELF
  if (user.role === 'ROLE_ADMIN' && !isSuperAdmin.value) {
    notify('Không được phép khóa tài khoản Super Admin (SUPER_ADMIN_PROTECTED)', 'error')
    return
  }
  user.status = user.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE'
  try {
    await adminApi.updateUserStatus(user.id, user.status === 'ACTIVE' ? 'ACTIVE' : 'LOCKED')
  } catch (_) {}
  notify(
    user.status === 'ACTIVE'
      ? `Đã mở khóa tài khoản @${user.username}`
      : `Đã khóa tài khoản @${user.username}`
  )
}

async function handleDeleteUser(user) {
  if (user.isSelf) {
    notify('Không thể tự xóa tài khoản quản trị đang đăng nhập (ADMIN_CANNOT_DELETE_SELF)', 'error')
    return
  }
  if (user.role === 'ROLE_ADMIN' && !isSuperAdmin.value) {
    notify('Không được phép xóa tài khoản Super Admin (SUPER_ADMIN_PROTECTED)', 'error')
    return
  }
  try {
    await adminApi.deleteUser(user.id)
  } catch (_) {}
  users.value = users.value.filter(u => u.id !== user.id)
  notify(`Đã xóa tài khoản @${user.username} khỏi hệ thống`)
}

// Reset Password Modal
const userForPasswordReset = ref(null)
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
}

async function confirmResetPassword() {
  passwordResetError.value = ''
  if (!newPasswordInput.value || newPasswordInput.value.length < 6) {
    passwordResetError.value = 'Mật khẩu mới phải có ít nhất 6 ký tự'
    return
  }
  try {
    await adminApi.resetUserPassword(userForPasswordReset.value.id, newPasswordInput.value)
  } catch (_) {}
  notify(`Đã đặt lại mật khẩu mới cho tài khoản @${userForPasswordReset.value.username}`)
  userForPasswordReset.value = null
}

// =========================================================================
// 4. ROLE CRUD & 7 MODULES x 26 APIs PERMISSION MATRIX MODAL
// =========================================================================
const isRoleModalOpen = ref(false)
const editingRole = ref(null)
const roleForm = ref({
  code: '',
  name: '',
  description: '',
  permissions: []
})
const roleFormError = ref('')

function normalizeRoleCodeInput(raw) {
  if (!raw) return ''
  let clean = raw.trim().toUpperCase().replace(/[\s-]+/g, '_').replace(/[^A-Z0-9_]/g, '')
  if (clean && !clean.startsWith('ROLE_')) {
    clean = 'ROLE_' + clean
  }
  return clean
}

function openCreateRoleModal() {
  editingRole.value = null
  roleFormError.value = ''
  roleForm.value = {
    code: 'ROLE_',
    name: '',
    description: '',
    permissions: ['PAYMENT_REPORT_VIEW', 'PAYMENT_LIST_VIEW']
  }
  isRoleModalOpen.value = true
}

function openEditRoleModal(roleItem) {
  if (roleItem.systemDefault) {
    notify(`Role mặc định gốc (${roleItem.code}) được bảo vệ bởi hệ thống, không thể sửa mã quyền.`, 'error')
    return
  }
  if (!isSuperAdmin.value && customRole.value === roleItem.code) {
    notify('Bạn không được phép tự chỉnh sửa quyền hạn của chính Role mình đang mang (CANNOT_MODIFY_OWN_ROLE)', 'error')
    return
  }
  editingRole.value = roleItem
  roleFormError.value = ''
  roleForm.value = {
    code: roleItem.code,
    name: roleItem.name,
    description: roleItem.description || '',
    permissions: [...roleItem.permissions]
  }
  isRoleModalOpen.value = true
}

function closeRoleModal() {
  isRoleModalOpen.value = false
  editingRole.value = null
}

/**
 * PC-07: Khi Admin tick một quyền thao tác trong Module (VD: OPERATOR_APPROVE_REJECT),
 * tự động tick luôn quyền xem tương ứng của Module đó (OPERATOR_VIEW) để tránh tạo Role không thấy giao diện.
 */
function togglePermissionInMatrix(moduleItem, permItem) {
  const list = roleForm.value.permissions
  const idx = list.indexOf(permItem.code)
  if (idx >= 0) {
    list.splice(idx, 1)
  } else {
    list.push(permItem.code)
    if (!permItem.viewPermission && moduleItem.viewPermissionCode && !list.includes(moduleItem.viewPermissionCode)) {
      list.push(moduleItem.viewPermissionCode)
    }
  }
}

function isModuleFullySelected(moduleItem) {
  return moduleItem.permissions.every(p => roleForm.value.permissions.includes(p.code))
}

function toggleWholeModule(moduleItem) {
  const allCodes = moduleItem.permissions.map(p => p.code)
  if (isModuleFullySelected(moduleItem)) {
    roleForm.value.permissions = roleForm.value.permissions.filter(c => !allCodes.includes(c))
  } else {
    for (const c of allCodes) {
      if (!roleForm.value.permissions.includes(c)) {
        roleForm.value.permissions.push(c)
      }
    }
  }
}

function selectAll26Permissions() {
  roleForm.value.permissions = [...ALL_ADMIN_PERMISSION_CODES]
}

function selectReadOnlyPermissions() {
  const viewCodes = permissionCatalog.value.flatMap(m =>
    m.permissions.filter(p => p.viewPermission || p.httpMethod === 'GET').map(p => p.code)
  )
  roleForm.value.permissions = [...new Set(viewCodes)]
}

function clearAllPermissions() {
  roleForm.value.permissions = []
}

async function handleSaveRole() {
  roleFormError.value = ''
  const normalizedCode = normalizeRoleCodeInput(roleForm.value.code)
  if (!normalizedCode || !/^ROLE_[A-Z0-9_]{2,45}$/.test(normalizedCode)) {
    roleFormError.value = 'Mã Role không hợp lệ (Ví dụ hợp lệ: ROLE_ACCOUNTANT, ROLE_CSKH, ROLE_CONTENT_MOD)'
    return
  }
  if (!roleForm.value.name.trim()) {
    roleFormError.value = 'Vui lòng nhập Tên hiển thị của Role'
    return
  }

  if (!editingRole.value) {
    if (systemRoles.value.some(r => r.code === normalizedCode)) {
      roleFormError.value = `Mã Role ${normalizedCode} đã tồn tại trên hệ thống`
      return
    }
    const newRole = {
      id: Date.now(),
      code: normalizedCode,
      name: roleForm.value.name.trim(),
      description: roleForm.value.description.trim() || 'Role tùy chỉnh phía hệ thống quản trị',
      category: 'SYSTEM',
      permissions: [...roleForm.value.permissions],
      systemDefault: false
    }
    systemRoles.value.push(newRole)
    notify(`Đã tạo Role mới "${newRole.name} (${newRole.code})" với ${newRole.permissions.length}/26 quyền API!`)
    adminApi.createRole(newRole).then(res => {
      if (res?.data?.id) newRole.id = res.data.id
    }).catch(() => {})
  } else {
    editingRole.value.name = roleForm.value.name.trim()
    editingRole.value.description = roleForm.value.description.trim()
    editingRole.value.permissions = [...roleForm.value.permissions]
    adminApi.updateRole(editingRole.value.id, {
      code: editingRole.value.code,
      name: editingRole.value.name,
      description: editingRole.value.description,
      category: 'SYSTEM',
      permissions: editingRole.value.permissions
    }).catch(() => {})

    // Nếu đang ở chế độ Preview chính Role này thì cập nhật tức thì
    if (previewRole.value?.code === editingRole.value.code) {
      startRolePreview(editingRole.value)
    }
    notify(`Đã cập nhật ma trận phân quyền cho Role "${editingRole.value.name}" (${editingRole.value.permissions.length}/26 APIs)`)
  }

  closeRoleModal()
}

async function handleDeleteRole(roleItem) {
  if (roleItem.systemDefault) {
    notify('Không thể xóa 3 Role mặc định gốc của hệ thống (SYSTEM_ROLE_PROTECTED)', 'error')
    return
  }
  if (!isSuperAdmin.value && customRole.value === roleItem.code) {
    notify('Bạn không được phép tự xóa chính Role mà tài khoản của bạn đang sử dụng (CANNOT_MODIFY_OWN_ROLE)', 'error')
    return
  }
  const userCount = countUsersWithRole(roleItem.code)
  if (userCount > 0) {
    notify(
      `Không thể xóa Role ${roleItem.code} vì đang có ${userCount} tài khoản được gán Role này (ROLE_IN_USE). Vui lòng đổi Role cho người dùng trước.`,
      'error'
    )
    return
  }
  adminApi.deleteRole(roleItem.id).catch(() => {})
  if (previewRole.value?.code === roleItem.code) {
    stopRolePreview()
  }
  systemRoles.value = systemRoles.value.filter(r => r.code !== roleItem.code)
  notify(`Đã xóa Role tùy chỉnh ${roleItem.code}`)
}

function handlePreviewRole(roleItem) {
  if (roleItem.category !== 'SYSTEM') {
    notify('Chế độ Xem thử giao diện Admin chỉ áp dụng cho các Role thuộc nhóm 3: Phía hệ thống (Admin)', 'error')
    return
  }
  startRolePreview(roleItem)
  notify(`Đang bật chế độ Xem thử giao diện với Role: ${roleItem.name} (${roleItem.permissions.length}/26 quyền API)`)
}

function getHttpMethodBadgeClass(method) {
  if (method.includes('DELETE')) return 'bg-rose-500/15 text-rose-700 dark:text-rose-300 border-rose-500/30'
  if (method.includes('POST')) return 'bg-emerald-500/15 text-emerald-700 dark:text-emerald-300 border-emerald-500/30'
  if (method.includes('PUT') || method.includes('PATCH')) return 'bg-amber-500/15 text-amber-700 dark:text-amber-300 border-amber-500/30'
  return 'bg-sky-500/15 text-sky-700 dark:text-sky-300 border-sky-500/30'
}

// =========================================================================
// 5. ALL BOOKINGS LIST FOR ADMIN FORCED REFUND
// =========================================================================
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

  const targetBooking = selectedBookingForRefund.value
  const reason = refundReasonInput.value.trim()
  targetBooking.status = 'REFUNDED'
  targetBooking.refundReason = reason
  adminApi.refundBooking(targetBooking.id, { reason }).catch(() => {})
  notify(`Đã hoàn tiền cưỡng chế 100% cho đơn vé ${targetBooking.id}`)
  closeRefundModal()
}
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
      <div>
        <h1 class="text-xl sm:text-2xl font-bold text-foreground tracking-tight">Quản Trị Người Dùng & Hoàn Tiền Cưỡng Chế (Admin Refund)</h1>
        <p class="text-xs text-muted-foreground mt-1">
          Quản lý người dùng theo 3 phân mục (1. Khách hàng • 2. Chủ xe • 3. Phía hệ thống), tạo Role tùy chỉnh theo ma trận 7 Modules × 26 APIs và thực thi lệnh hoàn tiền vé.
        </p>
      </div>

      <div class="flex flex-wrap items-center gap-2">
        <button
          v-if="hasPermission('ROLE_MANAGE_CRUD')"
          type="button"
          data-testid="open-create-role-modal-btn"
          @click="activeTab = 'ROLES'; openCreateRoleModal()"
          class="px-3 py-2 rounded-lg border border-border bg-card hover:bg-muted text-foreground font-semibold text-xs transition-all shadow-xs flex items-center gap-1.5"
        >
          <Shield class="w-4 h-4 text-primary" />
          <span>Tạo Role Mới</span>
        </button>

        <button
          v-if="hasPermission('USER_CREATE_UPDATE_DELETE')"
          type="button"
          data-testid="open-create-user-modal-btn"
          @click="activeTab = 'USERS'; openCreateUserModal()"
          class="px-3.5 py-2 rounded-lg bg-primary hover:bg-primary/90 text-primary-foreground font-semibold text-xs transition-all shadow-xs flex items-center gap-1.5"
        >
          <Users class="w-4 h-4" />
          <span>Tạo Tài Khoản</span>
        </button>
      </div>
    </div>

    <!-- Feedback Toast Banner -->
    <div
      v-if="feedbackMessage"
      data-testid="iam-feedback-banner"
      class="px-4 py-3 rounded-xl border text-xs font-semibold flex items-center justify-between shadow-xs"
      :class="feedbackType === 'error' ? 'bg-destructive/10 border-destructive/20 text-destructive' : 'bg-emerald-500/10 border-emerald-500/30 text-emerald-700 dark:text-emerald-300'"
    >
      <div class="flex items-center gap-2">
        <AlertTriangle v-if="feedbackType === 'error'" class="w-4 h-4 text-destructive flex-shrink-0" />
        <Check v-else class="w-4 h-4 text-emerald-600 dark:text-emerald-400 flex-shrink-0" />
        <span>{{ feedbackMessage }}</span>
      </div>
      <button type="button" @click="feedbackMessage = ''" class="text-muted-foreground hover:text-foreground">
        <X class="w-4 h-4" />
      </button>
    </div>

    <!-- Top Main Tabs: USERS | ROLES | BOOKINGS -->
    <div class="flex overflow-x-auto gap-2 border-b border-border pb-2 text-xs font-semibold">
      <button
        v-if="hasPermission('USER_VIEW')"
        type="button"
        @click="activeTab = 'USERS'"
        data-testid="tab-admin-users"
        class="px-4 py-2.5 rounded-lg transition-all flex items-center gap-2 font-medium whitespace-nowrap shrink-0"
        :class="activeTab === 'USERS' ? 'bg-primary text-primary-foreground shadow-sm' : 'text-muted-foreground hover:bg-muted/60'"
      >
        <Users class="w-4 h-4" />
        <span>Quản Lý Người Dùng (3 Phân Mục)</span>
        <span class="px-1.5 py-0.5 rounded-full text-[10px]" :class="activeTab === 'USERS' ? 'bg-primary-foreground/20 text-primary-foreground' : 'bg-muted text-muted-foreground'">
          {{ users.length }}
        </span>
      </button>

      <button
        v-if="hasPermission('ROLE_MANAGE_CRUD')"
        type="button"
        @click="activeTab = 'ROLES'"
        data-testid="tab-admin-roles"
        class="px-4 py-2.5 rounded-lg transition-all flex items-center gap-2 font-medium whitespace-nowrap shrink-0"
        :class="activeTab === 'ROLES' ? 'bg-primary text-primary-foreground shadow-sm' : 'text-muted-foreground hover:bg-muted/60'"
      >
        <Shield class="w-4 h-4" />
        <span>Quản Lý Role & Phân Quyền 26 APIs</span>
        <span class="px-1.5 py-0.5 rounded-full text-[10px]" :class="activeTab === 'ROLES' ? 'bg-primary-foreground/20 text-primary-foreground' : 'bg-purple-500/15 text-purple-700 dark:text-purple-300'">
          {{ systemRoles.length }} Roles
        </span>
      </button>

      <button
        v-if="hasPermission('BOOKING_VIEW')"
        type="button"
        @click="activeTab = 'BOOKINGS'"
        data-testid="tab-admin-bookings"
        class="px-4 py-2.5 rounded-lg transition-all flex items-center gap-2 font-medium whitespace-nowrap shrink-0"
        :class="activeTab === 'BOOKINGS' ? 'bg-primary text-primary-foreground shadow-sm' : 'text-muted-foreground hover:bg-muted/60'"
      >
        <Ticket class="w-4 h-4" />
        <span>Đơn Vé Toàn Sàn & Hoàn Tiền</span>
        <span class="px-1.5 py-0.5 rounded-full text-[10px]" :class="activeTab === 'BOOKINGS' ? 'bg-primary-foreground/20 text-primary-foreground' : 'bg-muted text-muted-foreground'">
          {{ allBookings.length }}
        </span>
      </button>
    </div>

    <!-- =========================================================================
         TAB 1: QUẢN LÝ NGƯỜI DÙNG (3 PHÂN MỤC: CUSTOMER, OPERATOR, SYSTEM STAFF)
         ========================================================================= -->
    <div v-if="activeTab === 'USERS' && hasPermission('USER_VIEW')" class="space-y-4">
      <!-- 3 Category Cards & Filter Bar -->
      <div class="grid grid-cols-2 lg:grid-cols-4 gap-2.5 sm:gap-3">
        <button
          type="button"
          data-testid="category-tab-ALL"
          @click="activeUserCategory = 'ALL'"
          class="p-3 sm:p-4 rounded-xl border text-left transition-all"
          :class="activeUserCategory === 'ALL' ? 'bg-primary/10 text-primary border-primary ring-1 ring-primary/30 shadow-xs' : 'bg-card border-border hover:border-primary/40 text-card-foreground'"
        >
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold uppercase tracking-wider" :class="activeUserCategory === 'ALL' ? 'text-primary' : 'text-muted-foreground'">Tất Cả Tài Khoản</span>
            <span class="text-base sm:text-lg font-black tabular-nums">{{ categoryCounts.ALL }}</span>
          </div>
          <p class="text-[11px] mt-1 text-muted-foreground line-clamp-1 sm:line-clamp-none">
            Tổng hợp cả 3 phân mục người dùng toàn sàn
          </p>
        </button>

        <button
          type="button"
          data-testid="category-tab-CUSTOMER"
          @click="activeUserCategory = 'CUSTOMER'"
          class="p-3 sm:p-4 rounded-xl border text-left transition-all"
          :class="activeUserCategory === 'CUSTOMER' ? 'bg-sky-500/15 text-sky-800 dark:text-sky-200 border-sky-500 ring-1 ring-sky-500/30 shadow-xs' : 'bg-card border-border hover:border-sky-400 text-card-foreground'"
        >
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold uppercase tracking-wider" :class="activeUserCategory === 'CUSTOMER' ? 'text-sky-700 dark:text-sky-300' : 'text-sky-600 dark:text-sky-400'">1. Khách Hàng (Customer)</span>
            <span class="text-base sm:text-lg font-black tabular-nums">{{ categoryCounts.CUSTOMER }}</span>
          </div>
          <p class="text-[11px] mt-1 text-muted-foreground line-clamp-1 sm:line-clamp-none">
            Hành khách đặt vé (`ROLE_CUSTOMER`)
          </p>
        </button>

        <button
          type="button"
          data-testid="category-tab-OPERATOR"
          @click="activeUserCategory = 'OPERATOR'"
          class="p-3 sm:p-4 rounded-xl border text-left transition-all"
          :class="activeUserCategory === 'OPERATOR' ? 'bg-emerald-500/15 text-emerald-800 dark:text-emerald-200 border-emerald-500 ring-1 ring-emerald-500/30 shadow-xs' : 'bg-card border-border hover:border-emerald-400 text-card-foreground'"
        >
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold uppercase tracking-wider" :class="activeUserCategory === 'OPERATOR' ? 'text-emerald-700 dark:text-emerald-300' : 'text-emerald-600 dark:text-emerald-400'">2. Chủ Xe (Operator)</span>
            <span class="text-base sm:text-lg font-black tabular-nums">{{ categoryCounts.OPERATOR }}</span>
          </div>
          <p class="text-[11px] mt-1 text-muted-foreground line-clamp-1 sm:line-clamp-none">
            Đối tác vận tải / Nhà xe (`ROLE_OPERATOR`)
          </p>
        </button>

        <button
          type="button"
          data-testid="category-tab-SYSTEM"
          @click="activeUserCategory = 'SYSTEM'"
          class="p-3 sm:p-4 rounded-xl border text-left transition-all"
          :class="activeUserCategory === 'SYSTEM' ? 'bg-purple-500/15 text-purple-800 dark:text-purple-200 border-purple-500 ring-1 ring-purple-500/30 shadow-xs' : 'bg-card border-border hover:border-purple-400 text-card-foreground'"
        >
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold uppercase tracking-wider" :class="activeUserCategory === 'SYSTEM' ? 'text-purple-700 dark:text-purple-300' : 'text-purple-600 dark:text-purple-400'">3. Phía Hệ Thống (Admin)</span>
            <span class="text-base sm:text-lg font-black tabular-nums">{{ categoryCounts.SYSTEM }}</span>
          </div>
          <p class="text-[11px] mt-1 text-muted-foreground line-clamp-1 sm:line-clamp-none">
            Super Admin & Nhân sự gắn Custom Role
          </p>
        </button>
      </div>

      <!-- Search & Action Toolbar -->
      <div class="bg-card p-4 rounded-xl border border-border flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-sm">
        <div class="flex-1 max-w-md">
          <input
            v-model="userSearchKeyword"
            type="text"
            data-testid="input-search-users"
            placeholder="Tìm theo username, họ tên, email hoặc mã Role..."
            class="w-full h-10 px-3.5 rounded-lg border border-input bg-background text-xs text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
          />
        </div>

        <div class="flex items-center gap-2 text-xs">
          <span class="text-muted-foreground">Đang hiển thị: <strong class="text-foreground">{{ filteredUsers.length }}</strong> tài khoản</span>
          <button
            v-if="hasPermission('USER_CREATE_UPDATE_DELETE') && activeUserCategory !== 'ALL'"
            type="button"
            @click="openCreateUserModal(activeUserCategory)"
            class="px-3 py-2 rounded-lg bg-primary hover:bg-primary/90 text-primary-foreground font-semibold text-xs transition-all shadow-sm flex items-center gap-1.5"
          >
            <Plus class="w-3.5 h-3.5" />
            <span>Thêm vào {{ getCategoryLabel(activeUserCategory) }}</span>
          </button>
        </div>
      </div>

      <!-- Users Table -->
      <div class="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full min-w-[850px] text-left border-collapse text-xs">
            <thead>
              <tr class="bg-muted/50 border-b border-border text-muted-foreground uppercase tracking-wider text-[11px]">
                <th class="py-3 px-4 font-semibold">Mã User</th>
                <th class="py-3 px-4 font-semibold">Tài Khoản / Họ Tên</th>
                <th class="py-3 px-4 font-semibold">Phân Mục</th>
                <th class="py-3 px-4 font-semibold text-center w-56">Vai Trò RBAC (Gán / Đổi Role)</th>
                <th class="py-3 px-4 font-semibold text-center">Trạng Thái</th>
                <th class="py-3 px-4 font-semibold text-right">Thao Tác CRUD</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-border">
              <tr v-for="u in filteredUsers" :key="u.id" class="hover:bg-muted/30 transition-colors">
                <td class="py-3.5 px-4 font-bold text-primary tabular-nums">{{ u.id }}</td>
                <td class="py-3.5 px-4">
                  <span class="font-bold text-foreground block">{{ u.fullName }}</span>
                  <span class="text-[11px] text-muted-foreground">@{{ u.username }} • {{ u.email }}</span>
                </td>
                <td class="py-3.5 px-4">
                  <span
                    class="px-2.5 py-0.5 rounded-full text-[11px] font-semibold border"
                    :class="getCategoryBadgeClass(u.category || getRoleInfo(u.role).category)"
                  >
                    {{ getCategoryLabel(u.category || getRoleInfo(u.role).category) }}
                  </span>
                </td>
                <td class="py-3.5 px-4 text-center">
                  <div class="inline-flex flex-col items-center gap-1 max-w-[200px]">
                    <select
                      v-if="hasPermission('USER_ROLE_ASSIGN') && !u.isSelf && (isSuperAdmin || u.role !== 'ROLE_ADMIN')"
                      :value="u.role"
                      :data-testid="`select-role-${u.id}`"
                      @change="handleQuickRoleChange(u, $event.target.value)"
                      class="w-full px-2 py-1 rounded-md border border-input text-xs font-semibold bg-background text-foreground focus:ring-2 focus:ring-ring cursor-pointer truncate"
                    >
                      <optgroup label="3. Phía hệ thống">
                        <option
                          v-for="r in systemRoles.filter(sr => sr.category === 'SYSTEM' && (isSuperAdmin || sr.code !== 'ROLE_ADMIN'))"
                          :key="r.code"
                          :value="r.code"
                        >
                          {{ r.name }} ({{ r.code }})
                        </option>
                      </optgroup>
                      <optgroup label="2. Chủ xe">
                        <option
                          v-for="r in systemRoles.filter(sr => sr.category === 'OPERATOR')"
                          :key="r.code"
                          :value="r.code"
                        >
                          {{ r.name }} ({{ r.code }})
                        </option>
                      </optgroup>
                      <optgroup label="1. Khách hàng">
                        <option
                          v-for="r in systemRoles.filter(sr => sr.category === 'CUSTOMER')"
                          :key="r.code"
                          :value="r.code"
                        >
                          {{ r.name }} ({{ r.code }})
                        </option>
                      </optgroup>
                    </select>
                    <span
                      v-else
                      class="px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-muted text-muted-foreground"
                    >
                      {{ u.role }}
                    </span>
                    <span class="text-[10px] text-muted-foreground truncate max-w-[180px]">{{ getRoleInfo(u.role).name }}</span>
                  </div>
                </td>
                <td class="py-3.5 px-4 text-center">
                  <StatusBadge :status="u.status" />
                </td>
                <td class="py-3.5 px-4 text-right space-x-1.5 whitespace-nowrap">
                  <span
                    v-if="u.isSelf"
                    data-testid="self-admin-guard"
                    class="text-[11px] font-semibold text-amber-700 bg-amber-50 dark:bg-amber-950/20 dark:text-amber-300 px-2.5 py-1 rounded border border-amber-200 dark:border-amber-800 inline-flex items-center gap-1"
                  >
                    <Shield class="w-3.5 h-3.5" />
                    <span>Tài khoản hiện tại (Không thể tự khóa)</span>
                  </span>
                  <template v-else>
                    <button
                      v-if="hasPermission('USER_CREATE_UPDATE_DELETE')"
                      type="button"
                      :data-testid="`btn-edit-user-${u.id}`"
                      @click="openEditUserModal(u)"
                      class="px-2.5 py-1 rounded-md border border-border bg-background hover:bg-muted text-foreground font-semibold text-xs transition-all shadow-xs inline-flex items-center gap-1"
                    >
                      <Pencil class="w-3.5 h-3.5 text-muted-foreground" />
                      <span>Sửa</span>
                    </button>

                    <button
                      v-if="hasPermission('USER_PASSWORD_RESET')"
                      type="button"
                      :data-testid="`reset-pwd-${u.id}`"
                      @click="openResetPasswordModal(u)"
                      class="px-2.5 py-1 rounded-md bg-sky-500/10 text-sky-700 dark:text-sky-300 hover:bg-sky-500/20 font-semibold text-xs transition-all border border-sky-500/30 shadow-xs inline-flex items-center gap-1"
                    >
                      <KeyRound class="w-3.5 h-3.5" />
                      <span>Reset MK</span>
                    </button>

                    <button
                      v-if="hasPermission('USER_STATUS_LOCK')"
                      type="button"
                      @click="toggleUserLock(u)"
                      :data-testid="`btn-lock-${u.id}`"
                      class="px-2.5 py-1 rounded-md font-semibold text-xs transition-all border shadow-xs inline-flex items-center gap-1"
                      :class="u.status === 'ACTIVE' ? 'bg-rose-500/10 text-rose-700 dark:text-rose-300 border-rose-500/30 hover:bg-rose-500/20' : 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-300 border-emerald-500/30 hover:bg-emerald-500/20'"
                    >
                      <Lock v-if="u.status === 'ACTIVE'" class="w-3.5 h-3.5" />
                      <Unlock v-else class="w-3.5 h-3.5" />
                      <span>{{ u.status === 'ACTIVE' ? 'Khóa Tài Khoản' : 'Mở Khóa' }}</span>
                    </button>

                    <button
                      v-if="hasPermission('USER_CREATE_UPDATE_DELETE')"
                      type="button"
                      :data-testid="`btn-delete-user-${u.id}`"
                      @click="handleDeleteUser(u)"
                      class="px-2.5 py-1 rounded-md bg-destructive/10 hover:bg-destructive/20 text-destructive font-semibold text-xs transition-all border border-destructive/30 shadow-xs inline-flex items-center gap-1"
                    >
                      <Trash2 class="w-3.5 h-3.5" />
                      <span>Xóa</span>
                    </button>
                  </template>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- =========================================================================
         TAB 2: QUẢN LÝ ROLE HỆ THỐNG & MA TRẬN PHÂN QUYỀN 7 MODULES x 26 APIs
         ========================================================================= -->
    <div v-else-if="activeTab === 'ROLES' && hasPermission('ROLE_MANAGE_CRUD')" class="space-y-4">
      <div class="bg-purple-500/10 border border-purple-500/20 rounded-xl p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-sm">
        <div class="space-y-1">
          <h2 class="text-sm font-bold text-foreground flex items-center gap-2">
            <Shield class="w-4 h-4 text-purple-600 dark:text-purple-400" />
            <span>Phân Quyền Động Theo Ma Trận 7 Modules × 26 APIs (Dynamic Admin RBAC)</span>
          </h2>
          <p class="text-xs text-muted-foreground">
            Ngoài 3 Role mặc định (`ROLE_CUSTOMER`, `ROLE_OPERATOR`, `ROLE_ADMIN`), bạn có thể tạo thêm các Role tùy chỉnh cho nhân sự hệ thống và bấm <strong>"Xem thử giao diện với Role này"</strong> để kiểm chứng Sidebar & nút bấm co giãn tức thì.
          </p>
        </div>

        <button
          type="button"
          data-testid="btn-create-role-in-tab"
          @click="openCreateRoleModal()"
          class="self-start sm:self-auto px-4 py-2 rounded-lg bg-primary hover:bg-primary/90 text-primary-foreground font-bold text-xs transition-all shadow-sm whitespace-nowrap flex items-center gap-1.5"
        >
          <Plus class="w-3.5 h-3.5" />
          <span>Tạo Role Hệ Thống Mới</span>
        </button>
      </div>

      <!-- Roles Cards Grid -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div
          v-for="roleItem in systemRoles"
          :key="roleItem.code"
          :data-testid="`role-card-${roleItem.code}`"
          class="bg-card rounded-xl border p-5 shadow-sm flex flex-col justify-between space-y-4 transition-all"
          :class="previewRole?.code === roleItem.code ? 'border-2 border-amber-500 ring-2 ring-amber-200 dark:ring-amber-900/40' : 'border-border'"
        >
          <div class="space-y-2">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <div class="flex items-center gap-2">
                <span class="px-2.5 py-1 rounded-md font-mono font-extrabold text-xs bg-foreground text-background">
                  {{ roleItem.code }}
                </span>
                <span
                  class="px-2 py-0.5 rounded-full text-[10px] font-bold border"
                  :class="getCategoryBadgeClass(roleItem.category)"
                >
                  {{ getCategoryLabel(roleItem.category) }}
                </span>
                <span
                  v-if="roleItem.systemDefault"
                  class="px-2 py-0.5 rounded-full text-[10px] font-bold bg-muted text-muted-foreground border border-border"
                >
                  Mặc định gốc
                </span>
              </div>

              <span class="text-xs font-bold text-muted-foreground flex items-center gap-1">
                <Users class="w-3.5 h-3.5" />
                <span>{{ countUsersWithRole(roleItem.code) }} tài khoản</span>
              </span>
            </div>

            <h3 class="text-base font-bold text-foreground">{{ roleItem.name }}</h3>
            <p class="text-xs text-muted-foreground leading-relaxed">{{ roleItem.description }}</p>

            <!-- Permissions summary for SYSTEM roles -->
            <div v-if="roleItem.category === 'SYSTEM'" class="pt-2 space-y-2">
              <div class="flex items-center justify-between text-xs">
                <span class="font-semibold text-muted-foreground">Quyền hạn API Hệ thống (/admin):</span>
                <span class="font-extrabold text-primary tabular-nums">
                  {{ roleItem.permissions.length }} / 26 APIs
                </span>
              </div>

              <div class="flex flex-wrap gap-1.5 max-h-24 overflow-y-auto p-2 rounded-lg bg-muted/40 border border-border">
                <span
                  v-for="perm in roleItem.permissions.slice(0, 10)"
                  :key="perm"
                  class="px-2 py-0.5 rounded text-[10px] font-mono font-semibold bg-background border border-border text-foreground"
                >
                  {{ perm }}
                </span>
                <span
                  v-if="roleItem.permissions.length > 10"
                  class="px-2 py-0.5 rounded text-[10px] font-bold bg-primary/10 text-primary"
                >
                  +{{ roleItem.permissions.length - 10 }} API khác
                </span>
                <span v-if="roleItem.permissions.length === 0" class="text-[11px] text-destructive font-semibold">
                  Chưa cấp quyền API nào (0/26)
                </span>
              </div>
            </div>
          </div>

          <!-- Role Card Footer Actions -->
          <div class="pt-3 border-t border-border flex flex-wrap items-center justify-between gap-2">
            <div>
              <button
                v-if="roleItem.category === 'SYSTEM'"
                type="button"
                :data-testid="`preview-role-${roleItem.code}`"
                @click="previewRole?.code === roleItem.code ? stopRolePreview() : handlePreviewRole(roleItem)"
                class="px-3 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5"
                :class="previewRole?.code === roleItem.code
                  ? 'bg-foreground text-background hover:bg-foreground/90'
                  : 'bg-amber-500/15 text-amber-700 dark:text-amber-300 border border-amber-500/30 hover:bg-amber-500/25'"
              >
                <Zap class="w-3.5 h-3.5" />
                <span>{{ previewRole?.code === roleItem.code ? 'Thoát Xem Thử Role Này' : 'Xem Thử Giao Diện Với Role Này' }}</span>
              </button>
            </div>

            <div class="flex items-center gap-2">
              <button
                v-if="!roleItem.systemDefault"
                type="button"
                :data-testid="`edit-role-${roleItem.code}`"
                @click="openEditRoleModal(roleItem)"
                class="px-3 py-1.5 rounded-lg border border-border hover:bg-muted text-foreground font-semibold text-xs transition-all flex items-center gap-1"
              >
                <Pencil class="w-3.5 h-3.5" />
                <span>Sửa Quyền API</span>
              </button>
              <button
                v-if="!roleItem.systemDefault && (isSuperAdmin || customRole !== roleItem.code)"
                type="button"
                :data-testid="`delete-role-${roleItem.code}`"
                @click="handleDeleteRole(roleItem)"
                class="px-3 py-1.5 rounded-lg bg-rose-500/15 text-rose-700 dark:text-rose-300 hover:bg-rose-500/25 font-semibold text-xs transition-all border border-rose-500/30 flex items-center gap-1"
              >
                <Trash2 class="w-3.5 h-3.5" />
                <span>Xóa Role</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- =========================================================================
         TAB 3: ĐƠN VÉ TOÀN SÀN & HOÀN TIỀN CƯỠNG CHẾ (ADMIN REFUND)
         ========================================================================= -->
    <div v-else-if="hasPermission('BOOKING_VIEW')" class="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full min-w-[850px] text-left border-collapse text-xs">
          <thead>
            <tr class="bg-muted/50 border-b border-border text-muted-foreground uppercase tracking-wider text-[11px]">
              <th class="py-3 px-4 font-semibold">Mã Đơn Vé</th>
              <th class="py-3 px-4 font-semibold">Hành Khách</th>
              <th class="py-3 px-4 font-semibold">Chuyến Xe / Nhà Xe</th>
              <th class="py-3 px-4 font-semibold text-center">Ghế</th>
              <th class="py-3 px-4 font-semibold text-right">Tổng Tiền</th>
              <th class="py-3 px-4 font-semibold text-center">Trạng Thái</th>
              <th class="py-3 px-4 font-semibold text-right">Can Thiệp Quản Trị</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border">
            <tr v-for="b in allBookings" :key="b.id" class="hover:bg-muted/30 transition-colors">
              <td class="py-3.5 px-4 font-bold text-primary tabular-nums">{{ b.id }}</td>
              <td class="py-3.5 px-4">
                <span class="font-bold text-foreground block">{{ b.customerName }}</span>
                <span class="text-[11px] text-muted-foreground tabular-nums">{{ b.customerPhone }}</span>
              </td>
              <td class="py-3.5 px-4">
                <span class="font-bold text-foreground block">{{ b.route }}</span>
                <span class="text-[11px] text-muted-foreground">{{ b.operatorName }}</span>
              </td>
              <td class="py-3.5 px-4 text-center font-bold text-primary tabular-nums">{{ b.seats.join(', ') }}</td>
              <td class="py-3.5 px-4 text-right font-extrabold text-foreground tabular-nums">
                {{ b.totalAmount.toLocaleString('vi-VN') }} đ
              </td>
              <td class="py-3.5 px-4 text-center">
                <StatusBadge :status="b.status" />
              </td>
              <td class="py-3.5 px-4 text-right">
                <button
                  v-if="b.status === 'CONFIRMED' && hasPermission('BOOKING_FORCE_REFUND')"
                  type="button"
                  @click="openRefundModal(b)"
                  :data-testid="`btn-admin-refund-${b.id}`"
                  class="px-3 py-1.5 bg-destructive hover:bg-destructive/90 text-destructive-foreground font-semibold rounded-lg text-xs transition-all shadow-sm inline-flex items-center gap-1.5"
                >
                  <Banknote class="w-3.5 h-3.5" />
                  <span>Hoàn Tiền Cưỡng Chế</span>
                </button>
                <span v-else class="text-muted-foreground text-[11px]">
                  {{ b.status === 'REFUNDED' ? 'Đã hoàn tiền' : (!hasPermission('BOOKING_FORCE_REFUND') ? 'Chỉ xem (Thiếu quyền hoàn vé)' : 'Không khả dụng') }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- =========================================================================
         MODAL 1: TẠO / SỬA NGƯỜI DÙNG (3 PHÂN MỤC & GÁN ROLE)
         ========================================================================= -->
    <div
      v-if="isUserModalOpen"
      class="fixed inset-0 bg-background/80 backdrop-blur-sm z-50 flex items-center justify-center p-4"
    >
      <div class="bg-card rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-border space-y-4">
        <div class="flex items-center justify-between pb-2 border-b border-border">
          <h3 class="text-base font-bold text-foreground">
            {{ editingUser ? `Chỉnh Sửa Tài Khoản — @${editingUser.username}` : 'Tạo Tài Khoản Người Dùng Mới' }}
          </h3>
          <button type="button" @click="closeUserModal" class="p-1 rounded-md text-muted-foreground hover:text-foreground hover:bg-muted transition-colors">
            <X class="w-4 h-4" />
          </button>
        </div>

        <div v-if="userFormError" class="p-3 rounded-lg bg-destructive/10 border border-destructive/20 text-destructive text-xs font-semibold flex items-center gap-2">
          <AlertTriangle class="w-4 h-4 flex-shrink-0" />
          <span>{{ userFormError }}</span>
        </div>

        <!-- Bước 1: Chọn 1 trong 3 Phân mục -->
        <div class="space-y-1.5">
          <label class="block text-xs font-bold text-foreground">Phân mục người dùng (3 Nhóm):</label>
          <div class="grid grid-cols-3 gap-2">
            <button
              type="button"
              @click="userForm.category = 'CUSTOMER'; onUserFormCategoryChange()"
              class="py-2 px-2.5 rounded-lg border text-xs font-bold transition-all"
              :class="userForm.category === 'CUSTOMER' ? 'bg-sky-700 text-white border-sky-700' : 'bg-muted/50 text-muted-foreground border-border'"
            >
              1. Khách hàng
            </button>
            <button
              type="button"
              @click="userForm.category = 'OPERATOR'; onUserFormCategoryChange()"
              class="py-2 px-2.5 rounded-lg border text-xs font-bold transition-all"
              :class="userForm.category === 'OPERATOR' ? 'bg-emerald-700 text-white border-emerald-700' : 'bg-muted/50 text-muted-foreground border-border'"
            >
              2. Chủ xe
            </button>
            <button
              type="button"
              @click="userForm.category = 'SYSTEM'; onUserFormCategoryChange()"
              class="py-2 px-2.5 rounded-lg border text-xs font-bold transition-all"
              :class="userForm.category === 'SYSTEM' ? 'bg-purple-800 text-white border-purple-800' : 'bg-muted/50 text-muted-foreground border-border'"
            >
              3. Phía hệ thống
            </button>
          </div>
        </div>

        <!-- Bước 2: Chọn Role cụ thể trong phân mục -->
        <FormField label="Vai trò (Role) được gán" required>
          <select
            v-model="userForm.role"
            data-testid="modal-select-user-role"
            class="w-full h-10 px-3 rounded-lg border border-input text-xs font-bold text-foreground bg-background focus:ring-2 focus:ring-ring"
          >
            <option
              v-for="r in availableRolesForSelectedCategory.filter(sr => isSuperAdmin || sr.code !== 'ROLE_ADMIN')"
              :key="r.code"
              :value="r.code"
            >
              {{ r.code }} — {{ r.name }} {{ r.category === 'SYSTEM' ? `(${r.permissions.length}/26 APIs)` : '' }}
            </option>
          </select>
        </FormField>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <FormField label="Tên đăng nhập (Username)" required>
            <input
              v-model="userForm.username"
              type="text"
              :disabled="Boolean(editingUser)"
              data-testid="input-user-username"
              placeholder="VD: ketoan02"
              class="w-full h-10 px-3 rounded-lg border border-input text-xs text-foreground bg-background disabled:bg-muted focus:ring-2 focus:ring-ring"
            />
          </FormField>

          <FormField label="Họ và tên" required>
            <input
              v-model="userForm.fullName"
              type="text"
              data-testid="input-user-fullname"
              placeholder="VD: Nguyễn Văn A"
              class="w-full h-10 px-3 rounded-lg border border-input text-xs text-foreground bg-background focus:ring-2 focus:ring-ring"
            />
          </FormField>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <FormField label="Địa chỉ Email" required>
            <input
              v-model="userForm.email"
              type="email"
              data-testid="input-user-email"
              placeholder="user@velobus.vn"
              class="w-full h-10 px-3 rounded-lg border border-input text-xs text-foreground bg-background focus:ring-2 focus:ring-ring"
            />
          </FormField>

          <FormField label="Số điện thoại">
            <input
              v-model="userForm.phone"
              type="text"
              data-testid="input-user-phone"
              placeholder="0901234567"
              class="w-full h-10 px-3 rounded-lg border border-input text-xs text-foreground bg-background focus:ring-2 focus:ring-ring"
            />
          </FormField>
        </div>

        <FormField v-if="!editingUser" label="Mật khẩu khởi tạo" required>
          <input
            v-model="userForm.password"
            type="password"
            data-testid="input-user-password"
            placeholder="Tối thiểu 6 ký tự..."
            class="w-full h-10 px-3 rounded-lg border border-input text-xs text-foreground bg-background focus:ring-2 focus:ring-ring"
          />
        </FormField>

        <div class="flex justify-end gap-3 pt-3 border-t border-border">
          <button
            type="button"
            @click="closeUserModal"
            class="px-4 py-2 border border-border hover:bg-muted text-muted-foreground text-xs font-semibold rounded-lg"
          >
            Hủy Bỏ
          </button>
          <AppButton
            variant="primary"
            class="px-4 py-2 text-xs font-bold"
            data-testid="btn-save-user"
            @click="handleSaveUser"
          >
            {{ editingUser ? 'Lưu Cập Nhật' : 'Tạo Tài Khoản' }}
          </AppButton>
        </div>
      </div>
    </div>

    <!-- =========================================================================
         MODAL 2: ĐẶT LẠI MẬT KHẨU (RESET PASSWORD)
         ========================================================================= -->
    <div
      v-if="userForPasswordReset"
      class="fixed inset-0 bg-background/80 backdrop-blur-sm z-50 flex items-center justify-center p-4"
    >
      <div class="bg-card rounded-2xl max-w-md w-full p-6 shadow-2xl border border-border space-y-4">
        <h3 class="text-base font-bold text-foreground flex items-center gap-2">
          <KeyRound class="w-4 h-4 text-primary" />
          <span>Đặt Lại Mật Khẩu — @{{ userForPasswordReset.username }}</span>
        </h3>
        <FormField label="Mật khẩu mới" :error="passwordResetError" required>
          <input
            v-model="newPasswordInput"
            type="password"
            data-testid="input-reset-password"
            placeholder="Nhập mật khẩu mới (tối thiểu 6 ký tự)..."
            class="w-full h-10 px-3 rounded-lg border border-input text-xs text-foreground bg-background focus:ring-2 focus:ring-ring"
          />
        </FormField>
        <div class="flex justify-end gap-3 pt-2 border-t border-border">
          <button
            type="button"
            @click="userForPasswordReset = null"
            class="px-4 py-2 border border-border hover:bg-muted text-muted-foreground text-xs font-semibold rounded-lg"
          >
            Hủy Bỏ
          </button>
          <AppButton
            variant="primary"
            class="px-4 py-2 text-xs font-bold"
            data-testid="btn-confirm-reset-password"
            @click="confirmResetPassword"
          >
            Xác Nhận Đổi Mật Khẩu
          </AppButton>
        </div>
      </div>
    </div>

    <!-- =========================================================================
         MODAL 3: TẠO / SỬA ROLE HỆ THỐNG & MA TRẬN 7 MODULES x 26 APIs (KIỂU 2)
         ========================================================================= -->
    <div
      v-if="isRoleModalOpen"
      class="fixed inset-0 bg-background/80 backdrop-blur-sm z-50 flex items-center justify-center p-4 overflow-y-auto"
    >
      <div class="bg-card rounded-2xl max-w-5xl w-full max-h-[92vh] flex flex-col shadow-2xl border border-border overflow-hidden">
        <!-- Modal Header -->
        <div class="px-6 py-4 bg-muted/80 border-b border-border text-foreground flex items-center justify-between flex-shrink-0">
          <div>
            <h3 class="text-base font-bold">
              {{ editingRole ? `Cấu Hình Ma Trận Phân Quyền API — ${editingRole.code}` : 'Tạo Role Hệ Thống Mới & Phân Quyền Ma Trận 7 Modules × 26 APIs' }}
            </h3>
            <p class="text-[11px] text-muted-foreground mt-0.5">
              Tick chọn từng API endpoint trong từng Module mà bạn muốn cấp quyền cho Role này. Giao diện Sidebar và các nút CRUD sẽ tự động thay đổi theo các API đã chọn.
            </p>
          </div>
          <button type="button" @click="closeRoleModal" class="p-1 rounded-md text-muted-foreground hover:text-foreground hover:bg-muted transition-colors">
            <X class="w-4 h-4" />
          </button>
        </div>

        <!-- Modal Scrollable Body -->
        <div class="p-6 overflow-y-auto space-y-5 flex-1">
          <div v-if="roleFormError" class="p-3 rounded-lg bg-destructive/10 border border-destructive/20 text-destructive text-xs font-semibold flex items-center gap-2">
            <AlertTriangle class="w-4 h-4 flex-shrink-0" />
            <span>{{ roleFormError }}</span>
          </div>

          <!-- Role Basic Info -->
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4 bg-muted/40 p-4 rounded-xl border border-border">
            <FormField label="Mã Role (Tự động chuẩn hóa ROLE_*)" required>
              <input
                v-model="roleForm.code"
                type="text"
                :disabled="Boolean(editingRole)"
                data-testid="input-role-code"
                placeholder="VD: ROLE_CONTENT_MOD"
                class="w-full h-10 px-3 rounded-lg border border-input text-xs font-mono font-bold uppercase text-foreground bg-background disabled:bg-muted focus:ring-2 focus:ring-ring"
              />
            </FormField>

            <FormField label="Tên hiển thị của Role" required>
              <input
                v-model="roleForm.name"
                type="text"
                data-testid="input-role-name"
                placeholder="VD: Kiểm Duyệt Nội Dung & Banner"
                class="w-full h-10 px-3 rounded-lg border border-input text-xs font-bold text-foreground bg-background focus:ring-2 focus:ring-ring"
              />
            </FormField>

            <FormField label="Mô tả chức năng nhiệm vụ">
              <input
                v-model="roleForm.description"
                type="text"
                data-testid="input-role-description"
                placeholder="Mô tả phạm vi công việc của Role..."
                class="w-full h-10 px-3 rounded-lg border border-input text-xs text-foreground bg-background focus:ring-2 focus:ring-ring"
              />
            </FormField>
          </div>

          <!-- Quick Selection Toolbar -->
          <div class="flex flex-wrap items-center justify-between gap-2 pb-2 border-b border-border">
            <div class="text-xs font-bold text-foreground">
              Đã chọn: <span class="text-primary text-sm font-black tabular-nums">{{ roleForm.permissions.length }} / 26</span> quyền API
            </div>

            <div class="flex flex-wrap items-center gap-2">
              <button
                type="button"
                data-testid="btn-role-select-all"
                @click="selectAll26Permissions"
                class="px-3 py-1.5 rounded-lg bg-primary/10 hover:bg-primary/20 text-primary font-bold text-xs transition-all border border-primary/20 flex items-center gap-1.5"
              >
                <Check class="w-3.5 h-3.5" />
                <span>Chọn Toàn Bộ 26 APIs</span>
              </button>
              <button
                type="button"
                data-testid="btn-role-select-readonly"
                @click="selectReadOnlyPermissions"
                class="px-3 py-1.5 rounded-lg bg-sky-500/15 hover:bg-sky-500/25 text-sky-700 dark:text-sky-300 font-bold text-xs transition-all border border-sky-500/30 flex items-center gap-1.5"
              >
                <Shield class="w-3.5 h-3.5" />
                <span>Chỉ Chọn Quyền Xem (Read-Only GET)</span>
              </button>
              <button
                type="button"
                data-testid="btn-role-clear-all"
                @click="clearAllPermissions"
                class="px-3 py-1.5 rounded-lg bg-muted hover:bg-muted/80 text-muted-foreground font-semibold text-xs transition-all border border-border flex items-center gap-1.5"
              >
                <X class="w-3.5 h-3.5" />
                <span>Bỏ Chọn Tất Cả</span>
              </button>
            </div>
          </div>

          <!-- 7 Modules x 26 APIs Matrix -->
          <div class="space-y-4">
            <div
              v-for="mod in permissionCatalog"
              :key="mod.moduleCode"
              class="rounded-xl border border-border overflow-hidden bg-card shadow-sm"
            >
              <!-- Module Header -->
              <div class="px-4 py-3 bg-muted/40 border-b border-border flex flex-wrap items-center justify-between gap-2">
                <div class="flex items-center gap-2.5">
                  <span class="font-bold text-xs text-foreground">{{ mod.moduleName }}</span>
                  <span class="text-[11px] font-mono text-primary bg-primary/10 px-2 py-0.5 rounded border border-primary/20">
                    Route: {{ mod.routePath }}
                  </span>
                </div>

                <button
                  type="button"
                  :data-testid="`toggle-module-${mod.moduleCode}`"
                  @click="toggleWholeModule(mod)"
                  class="px-2.5 py-1 rounded text-[11px] font-bold transition-all"
                  :class="isModuleFullySelected(mod)
                    ? 'bg-foreground text-background'
                    : 'bg-background border border-border text-foreground hover:bg-muted'"
                >
                  {{ isModuleFullySelected(mod) ? 'Bỏ chọn Module này' : '+ Chọn cả Module (' + mod.permissions.length + ' APIs)' }}
                </button>
              </div>

              <!-- Module APIs Table -->
              <div class="divide-y divide-border">
                <label
                  v-for="perm in mod.permissions"
                  :key="perm.code"
                  class="px-4 py-3 flex flex-col sm:flex-row sm:items-center justify-between gap-2 hover:bg-muted/30 cursor-pointer transition-colors"
                >
                  <div class="flex items-start gap-3">
                    <input
                      type="checkbox"
                      :checked="roleForm.permissions.includes(perm.code)"
                      :data-testid="`checkbox-perm-${perm.code}`"
                      @change="togglePermissionInMatrix(mod, perm)"
                      class="mt-0.5 w-4 h-4 rounded text-primary focus:ring-ring"
                    />
                    <div>
                      <div class="flex flex-wrap items-center gap-2">
                        <span class="font-bold text-xs text-foreground">{{ perm.name }}</span>
                        <span class="font-mono text-[10px] font-bold px-1.5 py-0.5 rounded bg-muted text-muted-foreground">
                          {{ perm.code }}
                        </span>
                        <span
                          v-if="perm.viewPermission"
                          class="text-[10px] font-bold px-1.5 py-0.5 rounded bg-purple-500/15 text-purple-700 dark:text-purple-300 border border-purple-500/30"
                        >
                          Quyền Mở Menu Sidebar
                        </span>
                      </div>
                      <p class="text-[11px] text-muted-foreground mt-0.5">{{ perm.description }}</p>
                    </div>
                  </div>

                  <div class="flex items-center gap-2 sm:justify-end pl-7 sm:pl-0 flex-shrink-0">
                    <span
                      class="px-2 py-0.5 rounded text-[10px] font-mono font-extrabold border"
                      :class="getHttpMethodBadgeClass(perm.httpMethod)"
                    >
                      {{ perm.httpMethod }}
                    </span>
                    <code class="text-[11px] font-mono text-foreground bg-muted px-2 py-0.5 rounded">
                      {{ perm.apiPattern }}
                    </code>
                  </div>
                </label>
              </div>
            </div>
          </div>
        </div>

        <!-- Modal Footer -->
        <div class="px-6 py-4 bg-muted/40 border-t border-border flex items-center justify-between flex-shrink-0">
          <span class="text-xs text-muted-foreground">
            Khi lưu, cả Backend (`AdminApiPermissionFilter`) và Frontend sẽ áp dụng ngay ma trận quyền này.
          </span>
          <div class="flex items-center gap-3">
            <button
              type="button"
              @click="closeRoleModal"
              class="px-4 py-2 border border-border hover:bg-muted text-muted-foreground text-xs font-semibold rounded-lg"
            >
              Hủy Bỏ
            </button>
            <AppButton
              variant="primary"
              class="px-5 py-2 text-xs font-bold"
              data-testid="btn-save-role"
              @click="handleSaveRole"
            >
              {{ editingRole ? 'Lưu Phân Quyền Role' : 'Tạo Role & Lưu Ma Trận API' }}
            </AppButton>
          </div>
        </div>
      </div>
    </div>

    <!-- =========================================================================
         MODAL 4: LỆNH HOÀN TIỀN CƯỠNG CHẾ (ADMIN FORCED REFUND)
         ========================================================================= -->
    <div
      v-if="selectedBookingForRefund"
      class="fixed inset-0 bg-background/80 backdrop-blur-sm z-50 flex items-center justify-center p-4"
    >
      <div class="bg-card rounded-2xl max-w-md w-full p-6 shadow-2xl border border-border space-y-4">
        <h3 class="text-base font-bold text-destructive flex items-center gap-2">
          <AlertTriangle class="w-4 h-4 flex-shrink-0" />
          <span>Lệnh Hoàn Tiền Cưỡng Chế — {{ selectedBookingForRefund.id }}</span>
        </h3>

        <div class="bg-muted/40 p-3 rounded-lg border border-border text-xs text-muted-foreground space-y-1">
          <p>Hành khách: <strong class="text-foreground">{{ selectedBookingForRefund.customerName }}</strong></p>
          <p>Số tiền hoàn trả 100%: <strong class="text-destructive tabular-nums">{{ selectedBookingForRefund.totalAmount.toLocaleString('vi-VN') }} đ</strong></p>
          <p class="text-[11px] text-muted-foreground">Hệ thống sẽ khấu trừ lại từ ví ký quỹ của nhà xe {{ selectedBookingForRefund.operatorName }} và giải phóng ghế trên Redis.</p>
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
            class="w-full p-3 rounded-lg border border-input text-xs text-foreground bg-background placeholder:text-muted-foreground focus:ring-2 focus:ring-ring resize-none"
            :class="{ 'border-destructive': refundError }"
          ></textarea>
        </FormField>

        <div class="flex justify-end gap-3 pt-2 border-t border-border">
          <button
            type="button"
            @click="closeRefundModal"
            class="px-4 py-2 border border-border hover:bg-muted text-muted-foreground text-xs font-semibold rounded-lg"
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
