<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  seats: {
    type: Array,
    default: () => []
  },
  selectedSeats: {
    type: Array,
    default: () => []
  },
  basePrice: {
    type: Number,
    default: 350000
  },
  maxSelectable: {
    type: Number,
    default: 4
  },
  readonly: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['toggle-seat'])

// Active Deck Tab: 'LOWER' or 'UPPER' (or 'BOTH' on wide screens)
const activeDeck = ref('LOWER')

// Fallback seed generator if backend seats not yet loaded
const defaultLowerSeats = [
  { code: 'A01', deck: 'LOWER', row: 1, col: 1, status: 'AVAILABLE', price: props.basePrice },
  { code: 'A02', deck: 'LOWER', row: 1, col: 2, status: 'AVAILABLE', price: props.basePrice },
  { code: 'A03', deck: 'LOWER', row: 1, col: 3, status: 'SOLD', price: props.basePrice },
  { code: 'A04', deck: 'LOWER', row: 2, col: 1, status: 'AVAILABLE', price: props.basePrice },
  { code: 'A05', deck: 'LOWER', row: 2, col: 2, status: 'HELD_BY_OTHER', price: props.basePrice },
  { code: 'A06', deck: 'LOWER', row: 2, col: 3, status: 'AVAILABLE', price: props.basePrice },
  { code: 'A07', deck: 'LOWER', row: 3, col: 1, status: 'AVAILABLE', price: props.basePrice },
  { code: 'A08', deck: 'LOWER', row: 3, col: 2, status: 'AVAILABLE', price: props.basePrice },
  { code: 'A09', deck: 'LOWER', row: 3, col: 3, status: 'AVAILABLE', price: props.basePrice },
  { code: 'A10', deck: 'LOWER', row: 4, col: 1, status: 'AVAILABLE', price: props.basePrice },
  { code: 'A11', deck: 'LOWER', row: 4, col: 2, status: 'AVAILABLE', price: props.basePrice },
  { code: 'A12', deck: 'LOWER', row: 4, col: 3, status: 'AVAILABLE', price: props.basePrice },
  { code: 'A13', deck: 'LOWER', row: 5, col: 1, status: 'AVAILABLE', price: props.basePrice },
  { code: 'A14', deck: 'LOWER', row: 5, col: 2, status: 'AVAILABLE', price: props.basePrice },
  { code: 'A15', deck: 'LOWER', row: 5, col: 3, status: 'AVAILABLE', price: props.basePrice },
  { code: 'A16', deck: 'LOWER', row: 6, col: 1, status: 'AVAILABLE', price: props.basePrice },
  { code: 'A17', deck: 'LOWER', row: 6, col: 2, status: 'AVAILABLE', price: props.basePrice },
  { code: 'A18', deck: 'LOWER', row: 6, col: 3, status: 'AVAILABLE', price: props.basePrice },
]

const defaultUpperSeats = [
  { code: 'B01', deck: 'UPPER', row: 1, col: 1, status: 'AVAILABLE', price: props.basePrice },
  { code: 'B02', deck: 'UPPER', row: 1, col: 2, status: 'AVAILABLE', price: props.basePrice },
  { code: 'B03', deck: 'UPPER', row: 1, col: 3, status: 'SOLD', price: props.basePrice },
  { code: 'B04', deck: 'UPPER', row: 2, col: 1, status: 'AVAILABLE', price: props.basePrice },
  { code: 'B05', deck: 'UPPER', row: 2, col: 2, status: 'AVAILABLE', price: props.basePrice },
  { code: 'B06', deck: 'UPPER', row: 2, col: 3, status: 'SOLD', price: props.basePrice },
  { code: 'B07', deck: 'UPPER', row: 3, col: 1, status: 'AVAILABLE', price: props.basePrice },
  { code: 'B08', deck: 'UPPER', row: 3, col: 2, status: 'AVAILABLE', price: props.basePrice },
  { code: 'B09', deck: 'UPPER', row: 3, col: 3, status: 'AVAILABLE', price: props.basePrice },
  { code: 'B10', deck: 'UPPER', row: 4, col: 1, status: 'AVAILABLE', price: props.basePrice },
  { code: 'B11', deck: 'UPPER', row: 4, col: 2, status: 'AVAILABLE', price: props.basePrice },
  { code: 'B12', deck: 'UPPER', row: 4, col: 3, status: 'AVAILABLE', price: props.basePrice },
  { code: 'B13', deck: 'UPPER', row: 5, col: 1, status: 'AVAILABLE', price: props.basePrice },
  { code: 'B14', deck: 'UPPER', row: 5, col: 2, status: 'AVAILABLE', price: props.basePrice },
  { code: 'B15', deck: 'UPPER', row: 5, col: 3, status: 'AVAILABLE', price: props.basePrice },
  { code: 'B16', deck: 'UPPER', row: 6, col: 1, status: 'AVAILABLE', price: props.basePrice },
  { code: 'B17', deck: 'UPPER', row: 6, col: 2, status: 'AVAILABLE', price: props.basePrice },
  { code: 'B18', deck: 'UPPER', row: 6, col: 3, status: 'AVAILABLE', price: props.basePrice },
]

const effectiveSeats = computed(() => {
  if (props.seats && props.seats.length > 0) return props.seats
  return [...defaultLowerSeats, ...defaultUpperSeats]
})

const lowerDeckSeats = computed(() => {
  return effectiveSeats.value.filter(s => {
    const code = s.code || s.seatCode || ''
    return s.deck === 'LOWER' || s.deck === 1 || code.startsWith('A')
  }).map(s => ({ ...s, code: s.code || s.seatCode }))
})

const upperDeckSeats = computed(() => {
  return effectiveSeats.value.filter(s => {
    const code = s.code || s.seatCode || ''
    return s.deck === 'UPPER' || s.deck === 2 || code.startsWith('B')
  }).map(s => ({ ...s, code: s.code || s.seatCode }))
})

function getSeatStatus(seat) {
  if (props.selectedSeats.includes(seat.code)) return 'SELECTED'
  const rawStatus = (seat.status || '').toUpperCase()
  if (rawStatus === 'HELD' || rawStatus === 'HELD_BY_OTHER') return 'HELD_BY_OTHER'
  if (rawStatus === 'BOOKED' || rawStatus === 'SOLD' || rawStatus === 'CONFIRMED') return 'SOLD'
  return 'AVAILABLE'
}

function handleSeatClick(seat) {
  if (props.readonly) return
  const status = getSeatStatus(seat)
  if (status === 'SOLD' || status === 'HELD_BY_OTHER') return
  emit('toggle-seat', seat.code)
}
</script>

<template>
  <div class="bg-surface rounded-xl border border-border-default p-5 shadow-card">
    <!-- Top Deck Toggle & Coach Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-border-default gap-3">
      <div>
        <h3 class="text-base font-semibold text-text-primary flex items-center gap-2">
          <svg class="w-5 h-5 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <rect x="3" y="4" width="18" height="16" rx="2" stroke-width="2"/>
            <path d="M7 8h10M7 12h10M7 16h4" stroke-width="2" stroke-linecap="round"/>
          </svg>
          Sơ đồ xe Limousine VIP (36 Giường nằm 2 Tầng)
        </h3>
        <p class="text-xs text-text-muted mt-0.5">Nhấp vào giường còn trống để chọn chỗ (Tối đa {{ maxSelectable }} vé)</p>
      </div>

      <!-- Deck Switcher Tabs -->
      <div class="inline-flex rounded-lg border border-border-default p-1 bg-canvas">
        <button
          type="button"
          @click="activeDeck = 'LOWER'"
          :class="[
            'px-3.5 py-1.5 text-xs font-semibold rounded-md transition-all',
            activeDeck === 'LOWER'
              ? 'bg-primary-600 text-white shadow-sm'
              : 'text-text-secondary hover:text-text-primary'
          ]"
        >
          Tầng Dưới (A01 - A18)
        </button>
        <button
          type="button"
          @click="activeDeck = 'UPPER'"
          :class="[
            'px-3.5 py-1.5 text-xs font-semibold rounded-md transition-all',
            activeDeck === 'UPPER'
              ? 'bg-primary-600 text-white shadow-sm'
              : 'text-text-secondary hover:text-text-primary'
          ]"
        >
          Tầng Trên (B01 - B18)
        </button>
      </div>
    </div>

    <!-- Visual Status Legend Bar (Single Source of Truth) -->
    <div class="py-3 px-3.5 my-4 bg-canvas rounded-lg border border-border-default flex flex-wrap items-center justify-between gap-2.5 text-xs">
      <div class="flex items-center gap-1.5">
        <div class="w-4 h-4 rounded border border-slate-300 bg-white shadow-sm"></div>
        <span class="text-text-secondary font-medium">Trống</span>
      </div>
      <div class="flex items-center gap-1.5">
        <div class="w-4 h-4 rounded bg-primary-600 ring-2 ring-primary-100 flex items-center justify-center text-white text-[10px] font-bold">✓</div>
        <span class="text-text-secondary font-medium">Đang chọn</span>
      </div>
      <div class="flex items-center gap-1.5">
        <div class="w-4 h-4 rounded bg-amber-100 border border-amber-300 flex items-center justify-center text-amber-800 text-[10px]">🔒</div>
        <span class="text-text-secondary font-medium">Đang giữ chỗ</span>
      </div>
      <div class="flex items-center gap-1.5">
        <div class="w-4 h-4 rounded bg-surface-muted border border-border-default flex items-center justify-center text-slate-400 text-[10px]">✕</div>
        <span class="text-text-secondary font-medium">Đã bán</span>
      </div>
    </div>

    <!-- Bus Cabin Visualization Body -->
    <div class="bg-canvas rounded-xl p-4 sm:p-6 border border-border-default flex flex-col items-center">
      <!-- Front Cab Header (Steering Wheel / Windshield) -->
      <div class="w-full max-w-md bg-white border border-border-default rounded-t-2xl p-3 mb-4 shadow-sm flex items-center justify-between text-xs text-text-muted">
        <div class="flex items-center gap-2">
          <!-- Steering wheel icon -->
          <svg class="w-5 h-5 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <circle cx="12" cy="12" r="9" stroke-width="2"/>
            <path d="M12 3v18M3 12h18" stroke-width="2"/>
          </svg>
          <span class="font-medium text-text-secondary">Khu vực Tài xế</span>
        </div>
        <span class="text-[11px] bg-slate-100 text-slate-600 px-2 py-0.5 rounded font-medium">Cửa lên xuống</span>
      </div>

      <!-- Berth Grid: Lower Deck -->
      <div v-show="activeDeck === 'LOWER'" class="w-full max-w-md space-y-3">
        <div class="grid grid-cols-3 gap-3 sm:gap-4">
          <div
            v-for="seat in lowerDeckSeats"
            :key="seat.code"
            :data-testid="'seat-' + seat.code"
            @click="handleSeatClick(seat)"
            :class="[
              'relative rounded-lg p-3 transition-all select-none border text-center flex flex-col items-center justify-center h-20',
              getSeatStatus(seat) === 'AVAILABLE' && 'bg-white border-border-strong hover:border-primary-600 hover:shadow-md cursor-pointer text-text-primary',
              getSeatStatus(seat) === 'SELECTED' && 'bg-primary-600 border-primary-600 text-white font-bold ring-2 ring-primary-100 shadow-sm cursor-pointer',
              getSeatStatus(seat) === 'HELD_BY_OTHER' && 'bg-amber-50 border-amber-300 text-amber-800 cursor-not-allowed',
              getSeatStatus(seat) === 'SOLD' && 'bg-surface-muted border-border-default text-text-muted cursor-not-allowed opacity-75'
            ]"
          >
            <!-- Bed Silhouette / Icon -->
            <svg class="w-4 h-4 mb-1" :class="getSeatStatus(seat) === 'SELECTED' ? 'text-white' : 'text-slate-400'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.75" d="M3 10h18M3 14h18M5 18h14M3 6h4v4H3z"/>
            </svg>
            <span class="text-sm font-semibold tabular-nums">{{ seat.code }}</span>
            <span class="text-[10px] tabular-nums mt-0.5" :class="getSeatStatus(seat) === 'SELECTED' ? 'text-blue-100' : 'text-text-muted'">
              {{ (seat.price || basePrice).toLocaleString('vi-VN') }}đ
            </span>

            <!-- Status Indicator Overlay -->
            <span v-if="getSeatStatus(seat) === 'HELD_BY_OTHER'" class="absolute top-1 right-1 text-[10px]" title="Đang trong phiên giữ chỗ 10 phút của người khác">🔒</span>
            <span v-if="getSeatStatus(seat) === 'SOLD'" class="absolute top-1 right-1 text-[10px] text-slate-400">✕</span>
            <span v-if="getSeatStatus(seat) === 'SELECTED'" class="absolute top-1 right-1 text-[10px] text-white">✓</span>
          </div>
        </div>
      </div>

      <!-- Berth Grid: Upper Deck -->
      <div v-show="activeDeck === 'UPPER'" class="w-full max-w-md space-y-3">
        <div class="grid grid-cols-3 gap-3 sm:gap-4">
          <div
            v-for="seat in upperDeckSeats"
            :key="seat.code"
            :data-testid="'seat-' + seat.code"
            @click="handleSeatClick(seat)"
            :class="[
              'relative rounded-lg p-3 transition-all select-none border text-center flex flex-col items-center justify-center h-20',
              getSeatStatus(seat) === 'AVAILABLE' && 'bg-white border-border-strong hover:border-primary-600 hover:shadow-md cursor-pointer text-text-primary',
              getSeatStatus(seat) === 'SELECTED' && 'bg-primary-600 border-primary-600 text-white font-bold ring-2 ring-primary-100 shadow-sm cursor-pointer',
              getSeatStatus(seat) === 'HELD_BY_OTHER' && 'bg-amber-50 border-amber-300 text-amber-800 cursor-not-allowed',
              getSeatStatus(seat) === 'SOLD' && 'bg-surface-muted border-border-default text-text-muted cursor-not-allowed opacity-75'
            ]"
          >
            <!-- Bed Silhouette / Icon -->
            <svg class="w-4 h-4 mb-1" :class="getSeatStatus(seat) === 'SELECTED' ? 'text-white' : 'text-slate-400'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.75" d="M3 10h18M3 14h18M5 18h14M3 6h4v4H3z"/>
            </svg>
            <span class="text-sm font-semibold tabular-nums">{{ seat.code }}</span>
            <span class="text-[10px] tabular-nums mt-0.5" :class="getSeatStatus(seat) === 'SELECTED' ? 'text-blue-100' : 'text-text-muted'">
              {{ (seat.price || basePrice).toLocaleString('vi-VN') }}đ
            </span>

            <!-- Status Indicator Overlay -->
            <span v-if="getSeatStatus(seat) === 'HELD_BY_OTHER'" class="absolute top-1 right-1 text-[10px]">🔒</span>
            <span v-if="getSeatStatus(seat) === 'SOLD'" class="absolute top-1 right-1 text-[10px] text-slate-400">✕</span>
            <span v-if="getSeatStatus(seat) === 'SELECTED'" class="absolute top-1 right-1 text-[10px] text-white">✓</span>
          </div>
        </div>
      </div>

      <!-- Bus Rear Tail -->
      <div class="w-full max-w-md bg-white border border-border-default rounded-b-2xl p-2.5 mt-4 text-center text-[11px] text-text-muted shadow-sm">
        Đuôi xe khách · Khoang hành lý phía dưới
      </div>
    </div>
  </div>
</template>
