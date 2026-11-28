import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import FleetAndDriversView from '../views/FleetAndDriversView.vue'
import OperatorTripsView from '../views/OperatorTripsView.vue'
import OperatorProfileView from '../views/OperatorProfileView.vue'
import { partnerApi } from '@/modules/partner/api'
import { catalogBookingApi } from '@/modules/catalog-booking/api'

// Common Stubs for Reka UI / Lucide / UI components to keep virtual DOM queries inline
const commonStubs = {
  Dialog: { template: '<div v-if="$attrs.open"><slot /></div>' },
  DialogContent: { template: '<div><slot /></div>' },
  DialogHeader: { template: '<div><slot /></div>' },
  DialogTitle: { template: '<div><slot /></div>' },
  DialogDescription: { template: '<div><slot /></div>' },
  DialogFooter: { template: '<div><slot /></div>' },
  AlertDialog: { template: '<div v-if="$attrs.open"><slot /></div>' },
  AlertDialogContent: { template: '<div><slot /></div>' },
  AlertDialogHeader: { template: '<div><slot /></div>' },
  AlertDialogTitle: { template: '<div><slot /></div>' },
  AlertDialogDescription: { template: '<div><slot /></div>' },
  AlertDialogFooter: { template: '<div><slot /></div>' },
  AlertDialogAction: { template: '<button @click="$emit(\'click\', $event)"><slot /></button>' },
  AlertDialogCancel: { template: '<button @click="$emit(\'click\', $event)"><slot /></button>' },
  Tabs: { template: '<div><slot /></div>' },
  TabsList: { template: '<div><slot /></div>' },
  TabsTrigger: { template: '<button @click="$emit(\'click\', $event)"><slot /></button>' },
  TabsContent: { template: '<div><slot /></div>' },
  Alert: { template: '<div><slot /></div>' },
  AlertTitle: { template: '<div><slot /></div>' },
  AlertDescription: { template: '<div><slot /></div>' },
  Badge: { template: '<span><slot /></span>' },
  FirebaseUploader: { template: '<div data-testid="firebase-uploader"><slot /></div>' }
}

describe('Gate D3 Operator Operations Unit Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // -------------------------------------------------------------------------
  // 1. FleetAndDriversView
  // -------------------------------------------------------------------------
  describe('FleetAndDriversView.vue (D3-01)', () => {
    const mockVehicles = [
      {
        id: 1,
        operatorId: 10,
        plateNumber: '29B-888.99',
        modelName: 'Thaco Mobihome 2024',
        vehicleType: 'SLEEPER',
        totalSeats: 36,
        seatRows: 9,
        seatsPerRow: 4,
        vipRows: 2,
        status: 'ACTIVE'
      },
      {
        id: 2,
        operatorId: 10,
        plateNumber: '51B-123.45',
        modelName: 'Hyundai Universe',
        vehicleType: 'SEAT',
        totalSeats: 28,
        seatRows: 7,
        seatsPerRow: 4,
        vipRows: 0,
        status: 'MAINTENANCE'
      }
    ]

    const mockDrivers = [
      {
        id: 101,
        operatorId: 10,
        fullName: 'Nguyễn Văn Tuấn',
        phone: '0987654321',
        licenseNumber: 'GPLX-790123456',
        status: 'ACTIVE'
      }
    ]

    it('fetches vehicles and drivers on mount from backend API', async () => {
      vi.spyOn(partnerApi, 'getVehicles').mockResolvedValue({ data: { code: 'OK', data: mockVehicles } })
      vi.spyOn(partnerApi, 'getDrivers').mockResolvedValue({ data: { code: 'OK', data: mockDrivers } })

      const wrapper = mount(FleetAndDriversView, { global: { stubs: commonStubs } })
      await flushPromises()

      expect(partnerApi.getVehicles).toHaveBeenCalledTimes(1)
      expect(partnerApi.getDrivers).toHaveBeenCalledTimes(1)
      expect(wrapper.text()).toContain('29B-888.99')
      expect(wrapper.text()).toContain('Thaco Mobihome 2024')
      expect(wrapper.text()).toContain('51B-123.45')
    })

    it('creates vehicle with exact VehicleCommand contract', async () => {
      vi.spyOn(partnerApi, 'getVehicles').mockResolvedValue({ data: { code: 'OK', data: [] } })
      vi.spyOn(partnerApi, 'getDrivers').mockResolvedValue({ data: { code: 'OK', data: [] } })
      vi.spyOn(partnerApi, 'createVehicle').mockResolvedValue({ data: { code: 'OK', data: { id: 3 } } })

      const wrapper = mount(FleetAndDriversView, { global: { stubs: commonStubs } })
      await flushPromises()

      // Open vehicle modal
      wrapper.vm.openAddVehicleModal()
      await flushPromises()
      expect(wrapper.vm.isVehicleModalOpen).toBe(true)

      // Fill form
      wrapper.vm.vehicleForm.plateNumber = '43B-999.88'
      wrapper.vm.vehicleForm.modelName = 'Tracomeco Limousine'
      wrapper.vm.vehicleForm.vehicleType = 'LIMOUSINE'
      wrapper.vm.vehicleForm.seatRows = 8
      wrapper.vm.vehicleForm.seatsPerRow = 3
      wrapper.vm.vehicleForm.totalSeats = 24
      wrapper.vm.vehicleForm.vipRows = 2

      await wrapper.vm.handleSaveVehicle()
      await flushPromises()

      expect(partnerApi.createVehicle).toHaveBeenCalledWith({
        plateNumber: '43B-999.88',
        modelName: 'Tracomeco Limousine',
        vehicleType: 'LIMOUSINE',
        totalSeats: 24,
        seatRows: 8,
        seatsPerRow: 3,
        vipRows: 2
      })
      expect(wrapper.vm.isVehicleModalOpen).toBe(false)
    })

    it('toggles vehicle maintenance/active with confirmation dialog', async () => {
      vi.spyOn(partnerApi, 'getVehicles').mockResolvedValue({ data: { code: 'OK', data: mockVehicles } })
      vi.spyOn(partnerApi, 'getDrivers').mockResolvedValue({ data: { code: 'OK', data: [] } })
      vi.spyOn(partnerApi, 'setVehicleMaintenance').mockResolvedValue({ data: { code: 'OK' } })

      const wrapper = mount(FleetAndDriversView, { global: { stubs: commonStubs } })
      await flushPromises()

      // Open maintenance confirmation for vehicle 1 (ACTIVE)
      wrapper.vm.openStatusConfirm(mockVehicles[0])
      expect(wrapper.vm.isStatusConfirmOpen).toBe(true)
      expect(wrapper.vm.selectedVehicleForStatus.plateNumber).toBe('29B-888.99')

      await wrapper.vm.handleToggleVehicleStatus()
      await flushPromises()

      expect(partnerApi.setVehicleMaintenance).toHaveBeenCalledWith(1)
      expect(wrapper.vm.isStatusConfirmOpen).toBe(false)
    })

    it('creates driver with exact DriverCommand contract', async () => {
      vi.spyOn(partnerApi, 'getVehicles').mockResolvedValue({ data: { code: 'OK', data: [] } })
      vi.spyOn(partnerApi, 'getDrivers').mockResolvedValue({ data: { code: 'OK', data: [] } })
      vi.spyOn(partnerApi, 'createDriver').mockResolvedValue({ data: { code: 'OK', data: { id: 102 } } })

      const wrapper = mount(FleetAndDriversView, { global: { stubs: commonStubs } })
      await flushPromises()

      wrapper.vm.openAddDriverModal()
      wrapper.vm.driverForm.fullName = 'Lê Hoàng Nam'
      wrapper.vm.driverForm.phone = '0912987654'
      wrapper.vm.driverForm.licenseNumber = 'GPLX-801234567'

      await wrapper.vm.handleSaveDriver()
      await flushPromises()

      expect(partnerApi.createDriver).toHaveBeenCalledWith({
        fullName: 'Lê Hoàng Nam',
        phone: '0912987654',
        licenseNumber: 'GPLX-801234567'
      })
      expect(wrapper.vm.isDriverModalOpen).toBe(false)
    })
  })

  // -------------------------------------------------------------------------
  // 2. OperatorTripsView
  // -------------------------------------------------------------------------
  describe('OperatorTripsView.vue (D3-02)', () => {
    const mockVehicles = [
      { id: 1, plateNumber: '29B-888.99', modelName: 'Thaco', totalSeats: 36, status: 'ACTIVE' },
      { id: 2, plateNumber: '51B-123.45', modelName: 'Hyundai', totalSeats: 28, status: 'MAINTENANCE' }
    ]

    const mockStations = [
      { id: 1, name: 'Bến xe Miền Đông', city: 'TP. Hồ Chí Minh' },
      { id: 2, name: 'Bến xe Liên Tỉnh Đà Lạt', city: 'Lâm Đồng' }
    ]

    it('populates vehicle and station dropdowns from backend APIs', async () => {
      vi.spyOn(partnerApi, 'getVehicles').mockResolvedValue({ data: { code: 'OK', data: mockVehicles } })
      vi.spyOn(catalogBookingApi, 'getStations').mockResolvedValue({ data: { code: 'OK', data: mockStations } })

      const wrapper = mount(OperatorTripsView, { global: { stubs: commonStubs } })
      await flushPromises()

      expect(partnerApi.getVehicles).toHaveBeenCalledTimes(1)
      expect(catalogBookingApi.getStations).toHaveBeenCalledTimes(1)
      // Only ACTIVE vehicles should be available for scheduling
      expect(wrapper.vm.activeVehicles.length).toBe(1)
      expect(wrapper.vm.activeVehicles[0].id).toBe(1)
      expect(wrapper.vm.stations.length).toBe(2)
    })

    it('enforces 30-minute turnaround buffer between trips of same vehicle', async () => {
      vi.spyOn(partnerApi, 'getVehicles').mockResolvedValue({ data: { code: 'OK', data: mockVehicles } })
      vi.spyOn(catalogBookingApi, 'getStations').mockResolvedValue({ data: { code: 'OK', data: mockStations } })

      const wrapper = mount(OperatorTripsView, { global: { stubs: commonStubs } })
      await flushPromises()

      // Seed an existing trip arriving at 09:00
      wrapper.vm.sessionTrips = [
        {
          id: 'TRIP-01',
          tripCode: 'SG-DL-01',
          vehicleId: 1,
          departureStationId: 1,
          arrivalStationId: 2,
          departureTime: '2026-11-24T05:00:00',
          arrivalTime: '2026-11-24T09:00:00',
          basePrice: 300000,
          status: 'SCHEDULED'
        }
      ]

      wrapper.vm.openScheduleModal()
      wrapper.vm.tripForm.vehicleId = '1'

      // Set departure at 09:20 (interval 20 mins < 30 mins -> violation)
      wrapper.vm.tripForm.departureTime = '2026-11-24T09:20:00'
      expect(wrapper.vm.turnaroundBufferViolation).not.toBeNull()
      expect(wrapper.vm.turnaroundBufferViolation).toContain('tối thiểu 30 phút')

      // Set departure at 09:30 (interval 30 mins == 30 mins -> valid)
      wrapper.vm.tripForm.departureTime = '2026-11-24T09:30:00'
      expect(wrapper.vm.turnaroundBufferViolation).toBeNull()
    })

    it('submits CreatePartnerTripCommand with exact backend parameters', async () => {
      vi.spyOn(partnerApi, 'getVehicles').mockResolvedValue({ data: { code: 'OK', data: mockVehicles } })
      vi.spyOn(catalogBookingApi, 'getStations').mockResolvedValue({ data: { code: 'OK', data: mockStations } })
      vi.spyOn(partnerApi, 'createPartnerTrip').mockResolvedValue({
        data: {
          code: 'OK',
          data: {
            id: 501,
            tripCode: 'SG-DL-02',
            vehicleId: 1,
            departureStationId: 1,
            arrivalStationId: 2,
            departureTime: '2026-11-24T10:00:00',
            arrivalTime: '2026-11-24T16:00:00',
            basePrice: 320000,
            status: 'SCHEDULED'
          }
        }
      })

      const wrapper = mount(OperatorTripsView, { global: { stubs: commonStubs } })
      await flushPromises()

      wrapper.vm.openScheduleModal()
      wrapper.vm.tripForm.tripCode = 'SG-DL-02'
      wrapper.vm.tripForm.vehicleId = '1'
      wrapper.vm.tripForm.departureStationId = '1'
      wrapper.vm.tripForm.arrivalStationId = '2'
      wrapper.vm.tripForm.departureTime = '2026-11-24T10:00:00'
      wrapper.vm.tripForm.arrivalTime = '2026-11-24T16:00:00'
      wrapper.vm.tripForm.basePrice = 320000

      await wrapper.vm.handleScheduleTrip()
      await flushPromises()

      expect(partnerApi.createPartnerTrip).toHaveBeenCalledWith({
        tripCode: 'SG-DL-02',
        vehicleId: 1,
        departureStationId: 1,
        arrivalStationId: 2,
        departureTime: '2026-11-24T10:00:00',
        arrivalTime: '2026-11-24T16:00:00',
        basePrice: 320000
      })
      expect(wrapper.vm.sessionTrips.length).toBe(1)
      expect(wrapper.vm.sessionTrips[0].tripCode).toBe('SG-DL-02')
    })

    it('rejects unsupported trip status filter with clear error message', async () => {
      vi.spyOn(partnerApi, 'getVehicles').mockResolvedValue({ data: { code: 'OK', data: mockVehicles } })
      vi.spyOn(catalogBookingApi, 'getStations').mockResolvedValue({ data: { code: 'OK', data: mockStations } })
      const getMyTripsSpy = vi.spyOn(partnerApi, 'getMyTrips')

      const wrapper = mount(OperatorTripsView, { global: { stubs: commonStubs } })
      await flushPromises()

      wrapper.vm.filterStatus = 'RUNNING' // No longer supported in backend TripStatus
      await wrapper.vm.fetchTrips(0)

      expect(wrapper.vm.tripsError).toContain('không được hỗ trợ')
      // Did NOT dispatch API request with invalid filter
      expect(getMyTripsSpy).not.toHaveBeenCalledWith(expect.objectContaining({ status: 'RUNNING' }))
    })

    it('strictly parses and validates pagination metadata and populates valid trips', async () => {
      vi.spyOn(partnerApi, 'getVehicles').mockResolvedValue({ data: { code: 'OK', data: mockVehicles } })
      vi.spyOn(catalogBookingApi, 'getStations').mockResolvedValue({ data: { code: 'OK', data: mockStations } })
      vi.spyOn(partnerApi, 'getMyTrips').mockResolvedValue({
        data: {
          code: 'OK',
          data: {
            items: [
              { id: 991, tripCode: 'TRIP-VALID', departureStationId: 1, arrivalStationId: 2, status: 'SCHEDULED' }
            ],
            totalPages: 5,
            totalElements: 12,
            page: 2
          }
        }
      })

      const wrapper = mount(OperatorTripsView, { global: { stubs: commonStubs } })
      await flushPromises()

      await wrapper.vm.fetchTrips(2)

      expect(wrapper.vm.sessionTrips.length).toBe(1)
      expect(wrapper.vm.sessionTrips[0].tripCode).toBe('TRIP-VALID')
      expect(wrapper.vm.totalPages).toBe(5)
      expect(wrapper.vm.totalElements).toBe(12)
      expect(wrapper.vm.currentPage).toBe(2)
    })

    it('rejects responses with malformed items or invalid pagination metadata', async () => {
      vi.spyOn(partnerApi, 'getVehicles').mockResolvedValue({ data: { code: 'OK', data: mockVehicles } })
      vi.spyOn(catalogBookingApi, 'getStations').mockResolvedValue({ data: { code: 'OK', data: mockStations } })
      vi.spyOn(partnerApi, 'getMyTrips').mockResolvedValue({
        data: {
          code: 'OK',
          data: {
            items: [{ id: null }],
            totalPages: 1,
            totalElements: 1,
            page: 0
          }
        }
      })

      const wrapper = mount(OperatorTripsView, { global: { stubs: commonStubs } })
      await flushPromises()

      await wrapper.vm.fetchTrips(0)
      expect(wrapper.vm.tripsError).toContain('chứa bản ghi không hợp lệ')
    })
  })

  // -------------------------------------------------------------------------
  // 3. OperatorProfileView
  // -------------------------------------------------------------------------
  describe('OperatorProfileView.vue (D3-03)', () => {
    const mockProfile = {
      id: 10,
      userId: 5,
      businessName: 'CÔNG TY CP XE KHÁCH PHƯƠNG TRANG FUTA BUS LINES',
      brandName: 'Phương Trang FUTA Bus Lines',
      taxCode: '0301118888',
      contactPhone: '1900606700',
      contactEmail: 'hotro@futa.vn',
      logoUrl: 'https://velobus.vn/logos/futa.png',
      description: 'Đơn vị vận tải hành khách chất lượng cao hàng đầu Việt Nam.',
      commissionRate: 8.5,
      status: 'ACTIVE',
      rejectionReason: null,
      createdAt: '2026-01-01T00:00:00'
    }

    it('fetches profile and displays immutable legal fields as read-only', async () => {
      vi.spyOn(partnerApi, 'getProfile').mockResolvedValue({ data: { code: 'OK', data: mockProfile } })

      const wrapper = mount(OperatorProfileView, { global: { stubs: commonStubs } })
      await flushPromises()

      expect(partnerApi.getProfile).toHaveBeenCalledTimes(1)
      expect(wrapper.vm.profile.businessName).toBe('CÔNG TY CP XE KHÁCH PHƯƠNG TRANG FUTA BUS LINES')
      expect(wrapper.vm.profile.taxCode).toBe('0301118888')
      expect(wrapper.text()).toContain('8.5%')
      expect(wrapper.text()).toContain('Đang Hoạt Động')
    })

    it('updates profile sending only mutable fields in UpdateOperatorProfileCommand', async () => {
      vi.spyOn(partnerApi, 'getProfile').mockResolvedValue({ data: { code: 'OK', data: mockProfile } })
      vi.spyOn(partnerApi, 'updateProfile').mockResolvedValue({
        data: {
          code: 'OK',
          data: {
            ...mockProfile,
            brandName: 'FUTA Bus Lines Updated',
            contactPhone: '0988776655'
          }
        }
      })

      const wrapper = mount(OperatorProfileView, { global: { stubs: commonStubs } })
      await flushPromises()

      wrapper.vm.form.brandName = 'FUTA Bus Lines Updated'
      wrapper.vm.form.contactPhone = '0988776655'
      wrapper.vm.form.contactEmail = 'contact@futa.vn'
      wrapper.vm.form.description = 'Dịch vụ nâng cấp 2026'

      await wrapper.vm.handleSaveProfile()
      await flushPromises()

      // Must NOT include businessName or taxCode
      expect(partnerApi.updateProfile).toHaveBeenCalledWith({
        brandName: 'FUTA Bus Lines Updated',
        contactPhone: '0988776655',
        contactEmail: 'contact@futa.vn',
        logoUrl: 'https://velobus.vn/logos/futa.png',
        description: 'Dịch vụ nâng cấp 2026'
      })
    })

    it('renders error state and retry button when profile fetch fails without mock fallbacks', async () => {
      vi.spyOn(partnerApi, 'getProfile').mockRejectedValue(new Error('Network error 500'))

      const wrapper = mount(OperatorProfileView, { global: { stubs: commonStubs } })
      await flushPromises()

      expect(wrapper.text()).toContain('Không thể nạp hồ sơ đối tác')
      expect(wrapper.vm.profile).toBeNull()
      const retryBtn = wrapper.find('[data-testid="btn-retry"]')
      expect(retryBtn.exists()).toBe(true)
      expect(retryBtn.text()).toContain('Thử lại')
    })

    it('blocks profile save when logo is actively uploading or save is already in progress', async () => {
      vi.spyOn(partnerApi, 'getProfile').mockResolvedValue({ data: { code: 'OK', data: mockProfile } })
      const updateSpy = vi.spyOn(partnerApi, 'updateProfile')

      const wrapper = mount(OperatorProfileView, { global: { stubs: commonStubs } })
      await flushPromises()

      // When logo is uploading
      wrapper.vm.isUploadingLogo = true
      wrapper.vm.form.brandName = 'New Brand'
      wrapper.vm.form.contactPhone = '0988776655'
      wrapper.vm.form.contactEmail = 'contact@futa.vn'

      await wrapper.vm.handleSaveProfile()
      expect(updateSpy).not.toHaveBeenCalled()
      expect(wrapper.vm.formError).toContain('Tệp logo đang được tải lên')

      // When isSaving is already true
      wrapper.vm.isUploadingLogo = false
      wrapper.vm.isSaving = true
      await wrapper.vm.handleSaveProfile()
      expect(updateSpy).not.toHaveBeenCalled()
    })
  })
})
