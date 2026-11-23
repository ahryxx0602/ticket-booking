import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import StatusBadge from '../StatusBadge.vue'
import CurrencyText from '../CurrencyText.vue'
import AppButton from '../AppButton.vue'

describe('StatusBadge Component', () => {
  it('renders CONFIRMED status with emerald green style', () => {
    const wrapper = mount(StatusBadge, {
      props: { status: 'CONFIRMED', label: 'Đã xác nhận' }
    })
    expect(wrapper.text()).toContain('Đã xác nhận')
    expect(wrapper.classes()).toContain('bg-emerald-50')
  })

  it('renders HELD status with amber style', () => {
    const wrapper = mount(StatusBadge, {
      props: { status: 'HELD', label: 'Đang giữ' }
    })
    expect(wrapper.classes()).toContain('bg-amber-50')
  })
})

describe('CurrencyText Component', () => {
  it('formats number to Vietnamese currency format with tabular-nums', () => {
    const wrapper = mount(CurrencyText, {
      props: { value: 350000 }
    })
    expect(wrapper.text()).toContain('350.000')
    expect(wrapper.classes()).toContain('tabular-nums')
  })
})

describe('AppButton Component', () => {
  it('renders primary variant by default and emits click', async () => {
    const wrapper = mount(AppButton, {
      slots: { default: 'Bấm vào đây' }
    })
    expect(wrapper.text()).toContain('Bấm vào đây')
    expect(wrapper.classes()).toContain('bg-primary-600')

    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toHaveLength(1)
  })

  it('shows loading spinner and disables click when loading is true', async () => {
    const wrapper = mount(AppButton, {
      props: { loading: true },
      slots: { default: 'Đang xử lý' }
    })
    expect(wrapper.find('svg.animate-spin').exists()).toBe(true)
    expect(wrapper.attributes('disabled')).toBeDefined()

    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeUndefined()
  })
})
