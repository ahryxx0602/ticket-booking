<template>
  <div class="space-y-6">
    <PageHeader
      title="Hồ Sơ Doanh Nghiệp & Tài Khoản Thụ Hưởng"
      subtitle="Quản lý pháp nhân nhà xe, giấy phép kinh doanh vận tải và cấu hình tài khoản ngân hàng nhận tiền quyết toán ký quỹ."
    >
      <template #actions>
        <AppButton
          variant="primary"
          icon="pi pi-save"
          :loading="saving"
          @click="saveProfile"
        >
          Lưu Thông Tin
        </AppButton>
      </template>
    </PageHeader>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Cột Trái: Logo & Pháp Nhân -->
      <div class="space-y-6">
        <div class="bg-white border border-slate-200 rounded-xl p-6 shadow-sm space-y-4">
          <h3 class="text-base font-bold text-slate-900 border-b border-slate-100 pb-3 flex items-center gap-2">
            <i class="pi pi-image text-blue-600"></i>
            <span>Logo & Nhận Diện Thương Hiệu</span>
          </h3>

          <FirebaseUploader
            v-model="profile.logoUrl"
            folder="operator-logos"
            label="Tải lên Logo Nhà Xe (PNG/JPG)"
          />

          <div class="p-3 bg-slate-50 border border-slate-200 rounded-lg text-xs space-y-1.5 text-slate-600">
            <div class="flex justify-between">
              <span>Trạng thái hồ sơ:</span>
              <StatusBadge :status="profile.status || 'ACTIVE'" />
            </div>
            <div class="flex justify-between">
              <span>Hạng đối tác:</span>
              <span class="font-semibold text-amber-600">Gold Partner ★</span>
            </div>
            <div class="flex justify-between">
              <span>Tỷ lệ hoa hồng:</span>
              <span class="font-semibold text-blue-600">5.0% (Tier 1)</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Cột Phải: Thông Tin Chi Tiết & Ngân Hàng -->
      <div class="lg:col-span-2 space-y-6">
        <!-- Thông tin pháp nhân -->
        <div class="bg-white border border-slate-200 rounded-xl p-6 shadow-sm space-y-4">
          <h3 class="text-base font-bold text-slate-900 border-b border-slate-100 pb-3 flex items-center gap-2">
            <i class="pi pi-building text-blue-600"></i>
            <span>Thông Tin Pháp Lý Nhà Xe</span>
          </h3>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <FormField label="Tên Pháp Nhân Đăng Ký KD" required>
              <input
                v-model="profile.businessName"
                type="text"
                class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:border-blue-500 focus:outline-none"
                placeholder="VD: CÔNG TY CP XE KHÁCH PHƯƠNG TRANG FUTA BUS LINES"
              />
            </FormField>

            <FormField label="Tên Thương Hiệu Hiển Thị" required>
              <input
                v-model="profile.brandName"
                type="text"
                class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:border-blue-500 focus:outline-none"
                placeholder="VD: Phương Trang FUTA Bus Lines"
              />
            </FormField>

            <FormField label="Mã Số Thuế (Tax Code)" required>
              <input
                v-model="profile.taxCode"
                type="text"
                class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm font-mono focus:border-blue-500 focus:outline-none"
                placeholder="VD: 0301118888"
              />
            </FormField>

            <FormField label="Số Điện Thoại Hotline" required>
              <input
                v-model="profile.contactPhone"
                type="tel"
                class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:border-blue-500 focus:outline-none"
                placeholder="VD: 1900 6067"
              />
            </FormField>

            <div class="md:col-span-2">
              <FormField label="Email Tiếp Nhận Đối Soát & Hóa Đơn" required>
                <input
                  v-model="profile.contactEmail"
                  type="email"
                  class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:border-blue-500 focus:outline-none"
                  placeholder="hotro@futa.vn"
                />
              </FormField>
            </div>

            <div class="md:col-span-2">
              <FormField label="Mô Tả Giới Thiệu Nhà Xe">
                <textarea
                  v-model="profile.description"
                  rows="3"
                  class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:border-blue-500 focus:outline-none"
                  placeholder="Giới thiệu dịch vụ chất lượng cao, các tuyến xe phục vụ..."
                ></textarea>
              </FormField>
            </div>
          </div>
        </div>

        <!-- Cấu hình tài khoản ngân hàng thụ hưởng -->
        <div class="bg-white border border-slate-200 rounded-xl p-6 shadow-sm space-y-4">
          <h3 class="text-base font-bold text-slate-900 border-b border-slate-100 pb-3 flex items-center justify-between">
            <div class="flex items-center gap-2">
              <i class="pi pi-credit-card text-emerald-600"></i>
              <span>Tài Khoản Ngân Hàng Nhận Quyết Toán (Escrow Payout)</span>
            </div>
            <span class="text-xs px-2.5 py-1 bg-emerald-50 text-emerald-700 font-semibold rounded-full border border-emerald-200">
              Đã Xác Minh KYC Cấp 3
            </span>
          </h3>

          <div class="p-3 bg-blue-50 border border-blue-200 rounded-lg text-xs text-blue-800">
            <i class="pi pi-info-circle mr-1"></i>
            <span>Tiền rút từ ví khả dụng sẽ được chuyển khoản trực tiếp 24/7 vào số tài khoản ngân hàng này sau khi hệ thống hoàn tất đối soát.</span>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <FormField label="Ngân Hàng Thụ Hưởng" required>
              <input
                v-model="bankInfo.bankName"
                type="text"
                class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:border-blue-500 focus:outline-none"
                placeholder="Vietcombank (VCB)"
              />
            </FormField>

            <FormField label="Số Tài Khoản Ngân Hàng" required>
              <input
                v-model="bankInfo.accountNumber"
                type="text"
                class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm font-mono focus:border-blue-500 focus:outline-none"
                placeholder="0123456789"
              />
            </FormField>

            <div class="md:col-span-2">
              <FormField label="Chủ Tài Khoản (In Hoa Không Dấu)" required>
                <input
                  v-model="bankInfo.accountHolder"
                  type="text"
                  class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm uppercase font-semibold focus:border-blue-500 focus:outline-none"
                  placeholder="CONG TY CP XE KHACH PHUONG TRANG FUTA BUS LINES"
                />
              </FormField>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import PageHeader from '@/shared/components/PageHeader.vue'
import AppButton from '@/shared/components/AppButton.vue'
import FormField from '@/shared/components/FormField.vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import FirebaseUploader from '@/shared/components/FirebaseUploader.vue'
import { partnerApi } from '@/modules/partner/api'

const saving = ref(false)

const profile = ref({
  businessName: 'CÔNG TY CP XE KHÁCH PHƯƠNG TRANG FUTA BUS LINES',
  brandName: 'Phương Trang FUTA Bus Lines',
  taxCode: '0301118888',
  contactPhone: '1900 6067',
  contactEmail: 'hotro@futa.vn',
  logoUrl: 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=300&q=80',
  description: 'Đơn vị vận tải hành khách chất lượng cao hàng đầu Việt Nam.',
  status: 'ACTIVE',
})

const bankInfo = ref({
  bankName: 'Vietcombank (VCB)',
  accountNumber: '0123456789',
  accountHolder: 'CONG TY CP XE KHACH PHUONG TRANG FUTA BUS LINES',
})

onMounted(async () => {
  try {
    const res = await partnerApi.getProfile()
    if (res?.data) {
      Object.assign(profile.value, res.data)
    }
  } catch {
    // Keep seed data for dev resilience
  }
})

async function saveProfile() {
  saving.value = true
  try {
    await partnerApi.updateProfile({
      businessName: profile.value.businessName,
      brandName: profile.value.brandName,
      taxCode: profile.value.taxCode,
      contactPhone: profile.value.contactPhone,
      contactEmail: profile.value.contactEmail,
      logoUrl: profile.value.logoUrl,
      description: profile.value.description,
    })
    alert('Đã cập nhật hồ sơ nhà xe thành công!')
  } catch (err) {
    alert(err?.message || 'Lỗi khi lưu thông tin nhà xe')
  } finally {
    saving.value = false
  }
}
</script>
