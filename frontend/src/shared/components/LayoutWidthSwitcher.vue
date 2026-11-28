<script setup>
import { useLayoutWidth } from '@/shared/composables/useLayoutWidth'

const { mode, setMode } = useLayoutWidth()

const modes = [
  {
    key: 'narrow',
    label: 'Hẹp',
    desc: 'Bố cục thu hẹp (Max 1024px) - Tập trung cao độ',
    icon: 'M9 4v16m6-16v16'
  },
  {
    key: 'normal',
    label: 'Chuẩn',
    desc: 'Bố cục mặc định (Max 1280px) - Chuẩn SaaS',
    icon: 'M4 4h16v16H4z'
  },
  {
    key: 'wide',
    label: 'Rộng',
    desc: 'Bố cục toàn màn hình (Fluid Width) - Cho bảng biểu lớn',
    icon: 'M4 8h16M4 16h16M4 4h16v16H4z'
  }
]
</script>

<template>
  <div
    class="inline-flex items-center rounded-lg border border-border bg-muted/60 p-0.5 text-xs select-none shadow-xs"
    role="group"
    aria-label="Tùy chọn độ rộng bố cục"
  >
    <button
      v-for="item in modes"
      :key="item.key"
      type="button"
      @click="setMode(item.key)"
      :title="item.desc"
      :class="[
        'px-2.5 py-1 text-[11px] rounded-md transition-all flex items-center gap-1.5',
        mode === item.key
          ? 'bg-card text-foreground font-semibold shadow-xs border border-border'
          : 'text-muted-foreground hover:text-foreground hover:bg-card/50'
      ]"
      :aria-pressed="mode === item.key"
    >
      <svg class="w-3.5 h-3.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.75" :d="item.icon"/>
      </svg>
      <span class="hidden sm:inline">{{ item.label }}</span>
    </button>
  </div>
</template>
