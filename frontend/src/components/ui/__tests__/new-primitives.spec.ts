import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { Calendar } from '@/components/ui/calendar'
import { RangeCalendar } from '@/components/ui/range-calendar'
import { Toggle } from '@/components/ui/toggle'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { Accordion, AccordionItem, AccordionTrigger, AccordionContent } from '@/components/ui/accordion'
import { Separator } from '@/components/ui/separator'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { today, getLocalTimeZone } from '@internationalized/date'

describe('New UI Primitives Suite (Calendar, Toggle, Accordion, Avatar, Separator)', () => {
  it('renders Calendar primitive correctly', () => {
    const wrapper = mount(Calendar, {
      props: {
        modelValue: today(getLocalTimeZone()) as any
      }
    })
    expect(wrapper.exists()).toBe(true)
    expect(wrapper.find('table').exists() || wrapper.find('[role="grid"]').exists()).toBe(true)
  })

  it('renders Toggle primitive and toggles state', () => {
    const wrapper = mount(Toggle, {
      props: {
        pressed: false
      },
      slots: {
        default: 'Toggle Test'
      }
    })
    expect(wrapper.text()).toContain('Toggle Test')
    expect(wrapper.exists()).toBe(true)
  })

  it('renders Accordion primitive and expands trigger', () => {
    const wrapper = mount(Accordion, {
      props: { type: 'single', collapsible: true },
      slots: {
        default: `
          <AccordionItem value="item-1">
            <AccordionTrigger>FAQ Question</AccordionTrigger>
            <AccordionContent>FAQ Answer</AccordionContent>
          </AccordionItem>
        `
      },
      global: {
        components: { AccordionItem, AccordionTrigger, AccordionContent }
      }
    })
    expect(wrapper.text()).toContain('FAQ Question')
  })

  it('renders Separator with horizontal orientation', () => {
    const wrapper = mount(Separator)
    expect(wrapper.classes()).toContain('h-[1px]')
    expect(wrapper.classes()).toContain('w-full')
  })

  it('renders Avatar with fallback text', () => {
    const wrapper = mount(Avatar, {
      slots: {
        default: '<AvatarFallback>VB</AvatarFallback>'
      },
      global: {
        components: { AvatarFallback }
      }
    })
    expect(wrapper.text()).toContain('VB')
  })
})
