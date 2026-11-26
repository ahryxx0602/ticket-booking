<script setup>
import { computed, ref } from 'vue'
import { Bus, Check, Lock, X } from 'lucide-vue-next'

const props = defineProps({
  seats: {
    type: Array,
    default: () => []
  },
  selectedSeats: {
    type: Array,
    default: () => []
  },
  heldSeats: {
    type: Array,
    default: () => []
  },
  basePrice: {
    type: Number,
    default: 0
  },
  maxSelectable: {
    type: Number,
    default: 10
  },
  readonly: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['toggle-seat'])

// Active Deck Tab: 'LOWER' or 'UPPER'
const activeDeck = ref('LOWER')

// Physical seat normalization from backend SeatDto:
// { id, tripId, seatNumber, seatType, priceMultiplier, finalPrice }
const normalizedSeats = computed(() => {
  return (props.seats || []).map(s => {
    const code = s.seatNumber || s.seatCode || s.code || ''
    const price = Number(s.finalPrice) || Number(s.price) || props.basePrice || 0
    let deck = s.deck
    if (!deck) {
      if (code.toUpperCase().startsWith('A')) deck = 'LOWER'
      else if (code.toUpperCase().startsWith('B')) deck = 'UPPER'
      else deck = 'SINGLE'
    }
    return {
      id: s.id,
      code,
      deck,
      seatType: s.seatType || 'STANDARD',
      priceMultiplier: s.priceMultiplier || 1.0,
      price,
      status: (s.status || '').toUpperCase()
    }
  })
})

const hasMultipleDecks = computed(() => {
  const hasLower = normalizedSeats.value.some(s => s.deck === 'LOWER' || s.code.startsWith('A'))
  const hasUpper = normalizedSeats.value.some(s => s.deck === 'UPPER' || s.code.startsWith('B'))
  return hasLower && hasUpper
})

const lowerDeckSeats = computed(() => {
  if (!hasMultipleDecks.value) return normalizedSeats.value
  return normalizedSeats.value.filter(s => s.deck === 'LOWER' || s.code.startsWith('A'))
})

const upperDeckSeats = computed(() => {
  return normalizedSeats.value.filter(s => s.deck === 'UPPER' || s.code.startsWith('B'))
})

function getSeatStatus(seat) {
  const code = seat.code
  if (props.selectedSeats.includes(code)) return 'SELECTED'
  if (props.heldSeats.includes(code)) return 'HELD_BY_ME'
  const rawStatus = (seat.status || '').toUpperCase()
  if (rawStatus === 'HELD' || rawStatus === 'HELD_BY_OTHER') return 'HELD_BY_OTHER'
  if (rawStatus === 'BOOKED' || rawStatus === 'SOLD' || rawStatus === 'CONFIRMED' || rawStatus === 'UNAVAILABLE') return 'SOLD'
  return 'AVAILABLE'
}

function handleSeatClick(seat) {
  if (props.readonly) return
  const status = getSeatStatus(seat)
  if (status === 'SOLD' || status === 'HELD_BY_OTHER') return
  emit('toggle-seat', seat.code)
}

function formatCurrency(val) {
  if (val === undefined || val === null) return '0'
  return Number(val).toLocaleString('vi-VN')
}
</script>

<template>
  <div class="bg-card text-card-foreground rounded-xl border border-border p-5 shadow-sm space-y-5">
    <!-- Header: Title & Deck Switcher Tabs -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-border gap-3">
      <div>
        <h3 class="text-base font-semibold text-foreground flex items-center gap-2">
          <Bus class="w-5 h-5 text-primary" />
          <span>Sơ đồ chỗ ngồi</span>
        </h3>
        <p class="text-xs text-muted-foreground mt-0.5">
          Chọn giường còn trống trên sơ đồ xe (Tối đa {{ maxSelectable }} giường/vé)
        </p>
      </div>

      <!-- Deck Switcher Tabs (Only if multiple decks exist) -->
      <div v-if="hasMultipleDecks" class="inline-flex rounded-lg border border-border p-1 bg-muted/40 self-start sm:self-auto">
        <button
          type="button"
          data-testid="deck-tab-lower"
          @click="activeDeck = 'LOWER'"
          :class="[
            'px-3.5 py-1.5 text-xs font-semibold rounded-md transition-all',
            activeDeck === 'LOWER'
              ? 'bg-primary text-primary-foreground shadow-sm'
              : 'text-muted-foreground hover:text-foreground'
          ]"
        >
          Tầng Dưới (A)
        </button>
        <button
          type="button"
          data-testid="deck-tab-upper"
          @click="activeDeck = 'UPPER'"
          :class="[
            'px-3.5 py-1.5 text-xs font-semibold rounded-md transition-all',
            activeDeck === 'UPPER'
              ? 'bg-primary text-primary-foreground shadow-sm'
              : 'text-muted-foreground hover:text-foreground'
          ]"
        >
          Tầng Trên (B)
        </button>
      </div>
    </div>

    <!-- Visual Status Legend Bar (Accessible with Symbols & Text Labels) -->
    <div class="py-3 px-3.5 bg-muted/30 rounded-lg border border-border flex flex-wrap items-center justify-between gap-3 text-xs">
      <div class="flex items-center gap-2">
        <div class="w-4 h-4 rounded border border-border bg-card shadow-xs"></div>
        <span class="text-muted-foreground font-medium">Trống</span>
      </div>
      <div class="flex items-center gap-2">
        <div class="w-4 h-4 rounded bg-primary text-primary-foreground flex items-center justify-center text-[10px] font-bold">
          <Check class="w-3 h-3 stroke-[3]" />
        </div>
        <span class="text-muted-foreground font-medium">Đang chọn</span>
      </div>
      <div class="flex items-center gap-2">
        <div class="w-4 h-4 rounded bg-amber-500/20 border border-amber-500/40 text-amber-600 dark:text-amber-400 flex items-center justify-center text-[10px]">
          <Lock class="w-2.5 h-2.5" />
        </div>
        <span class="text-muted-foreground font-medium">Đang giữ chỗ</span>
      </div>
      <div class="flex items-center gap-2">
        <div class="w-4 h-4 rounded bg-muted border border-border text-muted-foreground/60 flex items-center justify-center text-[10px]">
          <X class="w-2.5 h-2.5" />
        </div>
        <span class="text-muted-foreground font-medium">Đã bán / Khóa</span>
      </div>
    </div>

    <!-- Bus Cabin Visualization Body -->
    <div class="bg-muted/20 rounded-xl p-4 sm:p-6 border border-border flex flex-col items-center">
      <!-- Front Cab Header (Driver / Windshield) -->
      <div class="w-full max-w-md bg-card border border-border rounded-t-2xl p-3 mb-5 shadow-xs flex items-center justify-between text-xs text-muted-foreground">
        <div class="flex items-center gap-2 font-medium text-foreground">
          <svg class="w-4 h-4 text-primary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="9"/>
            <path d="M12 3v18M3 12h18"/>
          </svg>
          <span>Khu vực Tài xế</span>
        </div>
        <span class="text-[11px] bg-muted text-muted-foreground px-2 py-0.5 rounded font-medium border border-border">
          Cửa lên xuống
        </span>
      </div>

      <!-- Empty State if no seats in deck -->
      <div v-if="normalizedSeats.length === 0" class="py-12 text-center text-xs text-muted-foreground" data-testid="deck-empty">
        Không có dữ liệu ghế cho chuyến xe này.
      </div>

      <!-- Berth Grid: Lower Deck (or Single Deck) -->
      <div
        v-else-if="!hasMultipleDecks || activeDeck === 'LOWER'"
        class="w-full max-w-md space-y-3"
        data-testid="seat-grid-lower"
      >
        <div class="grid grid-cols-3 gap-3 sm:gap-4">
          <button
            v-for="seat in lowerDeckSeats"
            :key="seat.code"
            type="button"
            :data-testid="'seat-' + seat.code"
            :disabled="getSeatStatus(seat) === 'SOLD' || getSeatStatus(seat) === 'HELD_BY_OTHER' || readonly"
            @click="handleSeatClick(seat)"
            :class="[
              'relative rounded-lg p-3 transition-all select-none border text-center flex flex-col items-center justify-center h-20 w-full focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-1',
              getSeatStatus(seat) === 'AVAILABLE' && 'bg-card border-border hover:border-primary hover:shadow-xs cursor-pointer text-foreground',
              getSeatStatus(seat) === 'SELECTED' && 'bg-primary border-primary text-primary-foreground font-bold shadow-xs cursor-pointer',
              getSeatStatus(seat) === 'HELD_BY_ME' && 'bg-primary/10 border-primary text-primary font-bold shadow-xs cursor-pointer',
              getSeatStatus(seat) === 'HELD_BY_OTHER' && 'bg-amber-500/10 border-amber-500/30 text-amber-700 dark:text-amber-400 cursor-not-allowed opacity-90',
              getSeatStatus(seat) === 'SOLD' && 'bg-muted/60 border-border/60 text-muted-foreground/60 cursor-not-allowed opacity-60'
            ]"
            :aria-label="`Giường ${seat.code}, ${getSeatStatus(seat)}`"
          >
            <!-- Bed Silhouette / Icon -->
            <svg class="w-4 h-4 mb-1" :class="getSeatStatus(seat) === 'SELECTED' ? 'text-primary-foreground' : 'text-muted-foreground'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.75" d="M3 10h18M3 14h18M5 18h14M3 6h4v4H3z"/>
            </svg>
            <span class="text-sm font-semibold tabular-nums">{{ seat.code }}</span>
            <span class="text-[10px] tabular-nums mt-0.5" :class="getSeatStatus(seat) === 'SELECTED' ? 'text-primary-foreground/90' : 'text-muted-foreground'">
              {{ formatCurrency(seat.price) }}đ
            </span>

            <!-- Status Indicator Overlay -->
            <span v-if="getSeatStatus(seat) === 'HELD_BY_OTHER'" class="absolute top-1 right-1 text-[10px]" title="Đang trong phiên giữ chỗ">
              <Lock class="w-2.5 h-2.5 text-amber-600 dark:text-amber-400" />
            </span>
            <span v-else-if="getSeatStatus(seat) === 'HELD_BY_ME'" class="absolute top-1 right-1 text-[10px]" title="Ghế của bạn đã giữ">
              <Lock class="w-2.5 h-2.5 text-primary" />
            </span>
            <span v-else-if="getSeatStatus(seat) === 'SOLD'" class="absolute top-1 right-1 text-[10px]">
              <X class="w-2.5 h-2.5 text-muted-foreground/60" />
            </span>
            <span v-else-if="getSeatStatus(seat) === 'SELECTED'" class="absolute top-1 right-1 text-[10px]">
              <Check class="w-2.5 h-2.5 stroke-[3] text-primary-foreground" />
            </span>
          </button>
        </div>
      </div>

      <!-- Berth Grid: Upper Deck -->
      <div
        v-else-if="hasMultipleDecks && activeDeck === 'UPPER'"
        class="w-full max-w-md space-y-3"
        data-testid="seat-grid-upper"
      >
        <div class="grid grid-cols-3 gap-3 sm:gap-4">
          <button
            v-for="seat in upperDeckSeats"
            :key="seat.code"
            type="button"
            :data-testid="'seat-' + seat.code"
            :disabled="getSeatStatus(seat) === 'SOLD' || getSeatStatus(seat) === 'HELD_BY_OTHER' || readonly"
            @click="handleSeatClick(seat)"
            :class="[
              'relative rounded-lg p-3 transition-all select-none border text-center flex flex-col items-center justify-center h-20 w-full focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-1',
              getSeatStatus(seat) === 'AVAILABLE' && 'bg-card border-border hover:border-primary hover:shadow-xs cursor-pointer text-foreground',
              getSeatStatus(seat) === 'SELECTED' && 'bg-primary border-primary text-primary-foreground font-bold shadow-xs cursor-pointer',
              getSeatStatus(seat) === 'HELD_BY_ME' && 'bg-primary/10 border-primary text-primary font-bold shadow-xs cursor-pointer',
              getSeatStatus(seat) === 'HELD_BY_OTHER' && 'bg-amber-500/10 border-amber-500/30 text-amber-700 dark:text-amber-400 cursor-not-allowed opacity-90',
              getSeatStatus(seat) === 'SOLD' && 'bg-muted/60 border-border/60 text-muted-foreground/60 cursor-not-allowed opacity-60'
            ]"
            :aria-label="`Giường ${seat.code}, ${getSeatStatus(seat)}`"
          >
            <!-- Bed Silhouette / Icon -->
            <svg class="w-4 h-4 mb-1" :class="getSeatStatus(seat) === 'SELECTED' ? 'text-primary-foreground' : 'text-muted-foreground'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.75" d="M3 10h18M3 14h18M5 18h14M3 6h4v4H3z"/>
            </svg>
            <span class="text-sm font-semibold tabular-nums">{{ seat.code }}</span>
            <span class="text-[10px] tabular-nums mt-0.5" :class="getSeatStatus(seat) === 'SELECTED' ? 'text-primary-foreground/90' : 'text-muted-foreground'">
              {{ formatCurrency(seat.price) }}đ
            </span>

            <!-- Status Indicator Overlay -->
            <span v-if="getSeatStatus(seat) === 'HELD_BY_OTHER'" class="absolute top-1 right-1 text-[10px]" title="Đang trong phiên giữ chỗ">
              <Lock class="w-2.5 h-2.5 text-amber-600 dark:text-amber-400" />
            </span>
            <span v-else-if="getSeatStatus(seat) === 'HELD_BY_ME'" class="absolute top-1 right-1 text-[10px]" title="Ghế của bạn đã giữ">
              <Lock class="w-2.5 h-2.5 text-primary" />
            </span>
            <span v-else-if="getSeatStatus(seat) === 'SOLD'" class="absolute top-1 right-1 text-[10px]">
              <X class="w-2.5 h-2.5 text-muted-foreground/60" />
            </span>
            <span v-else-if="getSeatStatus(seat) === 'SELECTED'" class="absolute top-1 right-1 text-[10px]">
              <Check class="w-2.5 h-2.5 stroke-[3] text-primary-foreground" />
            </span>
          </button>
        </div>
      </div>

      <!-- Bus Rear Tail -->
      <div class="w-full max-w-md bg-card border border-border rounded-b-2xl p-2.5 mt-5 text-center text-[11px] text-muted-foreground shadow-xs">
        Đuôi xe khách · Khoang hành lý phía dưới
      </div>
    </div>
  </div>
</template>
