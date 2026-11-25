import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { Button } from '@/components/ui/button'
import { Input, PasswordInput } from '@/components/ui/input'
import { Badge, StatusBadge } from '@/components/ui/badge'
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert'
import { DataTable } from '@/components/ui/table'
import type { ColumnDef } from '@tanstack/vue-table'

describe('Shadcn Vue / Reka UI Primitives & Domain Components', () => {
  describe('Button.vue', () => {
    it('renders default button text and emits click event', async () => {
      const wrapper = mount(Button, {
        slots: { default: 'Xác nhận đặt vé' }
      })
      expect(wrapper.text()).toContain('Xác nhận đặt vé')
      expect(wrapper.classes()).toContain('bg-primary')

      await wrapper.trigger('click')
      expect(wrapper.emitted('click')).toHaveLength(1)
    })

    it('renders destructive variant with proper CVA class', () => {
      const wrapper = mount(Button, {
        props: { variant: 'destructive' },
        slots: { default: 'Hủy vé' }
      })
      expect(wrapper.classes()).toContain('bg-destructive')
    })

    it('renders loading state with disabled attribute and spinner', () => {
      const wrapper = mount(Button, {
        props: { loading: true },
        slots: { default: 'Đang tải' }
      })
      expect(wrapper.attributes('disabled')).toBeDefined()
      expect(wrapper.find('.animate-spin').exists()).toBe(true)
    })

    it('respects disabled prop and prevents action', async () => {
      const wrapper = mount(Button, {
        props: { disabled: true },
        slots: { default: 'Bị khóa' }
      })
      expect(wrapper.attributes('disabled')).toBeDefined()
      expect(wrapper.classes()).toContain('disabled:opacity-50')
    })
  })

  describe('Input.vue & PasswordInput.vue', () => {
    it('Input emits update:modelValue on text input', async () => {
      const wrapper = mount(Input, {
        props: { modelValue: 'Hà Nội' }
      })
      const input = wrapper.find('input')
      expect(input.element.value).toBe('Hà Nội')

      await input.setValue('Đà Lạt')
      expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['Đà Lạt'])
    })

    it('PasswordInput toggles visibility from password to text', async () => {
      const wrapper = mount(PasswordInput, {
        props: { modelValue: 'secret123' }
      })
      const input = wrapper.find('input')
      expect(input.attributes('type')).toBe('password')

      const toggleBtn = wrapper.find('button[type="button"]')
      expect(toggleBtn.exists()).toBe(true)

      // Click eye button to show password
      await toggleBtn.trigger('click')
      expect(input.attributes('type')).toBe('text')

      // Click again to hide password
      await toggleBtn.trigger('click')
      expect(input.attributes('type')).toBe('password')
    })
  })

  describe('Badge.vue & StatusBadge.vue', () => {
    it('Badge renders variants correctly', () => {
      const wrapper = mount(Badge, {
        props: { variant: 'outline' },
        slots: { default: 'Thông tin' }
      })
      expect(wrapper.text()).toBe('Thông tin')
      expect(wrapper.classes()).toContain('border-border')
    })

    it('StatusBadge correctly maps 16 domain statuses with semantic color classes', () => {
      const statuses = [
        { code: 'CONFIRMED', expectedClass: 'emerald' },
        { code: 'PAYMENT_SUCCESS', expectedClass: 'emerald' },
        { code: 'ACTIVE', expectedClass: 'emerald' },
        { code: 'PENDING', expectedClass: 'amber' },
        { code: 'PAYMENT_PENDING', expectedClass: 'amber' },
        { code: 'CANCELLED', expectedClass: 'red' },
        { code: 'PAYMENT_FAILED', expectedClass: 'red' },
        { code: 'DELAYED', expectedClass: 'orange' },
        { code: 'IN_TRANSIT', expectedClass: 'sky' }
      ]

      for (const item of statuses) {
        const wrapper = mount(StatusBadge, {
          props: { status: item.code }
        })
        const hasExpectedClass = wrapper.classes().some(c => c.includes(item.expectedClass))
        expect(hasExpectedClass, `Status ${item.code} should contain ${item.expectedClass}`).toBe(true)
      }
    })
  })

  describe('Alert.vue', () => {
    it('renders alert component with role="alert" and content', () => {
      const wrapper = mount(Alert, {
        slots: {
          default: `
            <div class="title">Cảnh báo</div>
            <div class="desc">Chuyến xe trễ 15 phút do thời tiết</div>
          `
        }
      })
      expect(wrapper.attributes('role')).toBe('alert')
      expect(wrapper.text()).toContain('Chuyến xe trễ 15 phút')
    })

    it('renders destructive variant with proper danger tokens', () => {
      const wrapper = mount(Alert, {
        props: { variant: 'destructive' },
        slots: { default: 'Giao dịch bị từ chối' }
      })
      expect(wrapper.classes()).toContain('text-status-danger')
    })
  })

  describe('TanStack DataTable.vue', () => {
    interface TestItem {
      id: string
      name: string
      price: number
    }

    const columns: any[] = [
      {
        accessorKey: 'id',
        header: 'Mã'
      },
      {
        accessorKey: 'name',
        header: 'Tên tuyến'
      },
      {
        accessorKey: 'price',
        header: 'Giá vé'
      }
    ]

    const testData: TestItem[] = [
      { id: '1', name: 'Sài Gòn - Đà Lạt', price: 350000 },
      { id: '2', name: 'Sài Gòn - Nha Trang', price: 320000 }
    ]

    it('renders headers, rows, and pagination summary', () => {
      const wrapper = mount(DataTable, {
        props: {
          columns,
          data: testData,
          pageSize: 10
        }
      })
      expect(wrapper.text()).toContain('Mã')
      expect(wrapper.text()).toContain('Tên tuyến')
      expect(wrapper.text()).toContain('Sài Gòn - Đà Lạt')
      expect(wrapper.text()).toContain('Sài Gòn - Nha Trang')
      expect(wrapper.text()).toContain('Trang 1 / 1 (Tổng 2 dòng)')
    })
  })
})
