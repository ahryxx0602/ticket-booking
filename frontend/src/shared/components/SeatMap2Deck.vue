<script setup>
import { computed, ref } from 'vue'
import { Bus, Check, Lock, X, AlertCircle } from 'lucide-vue-next'

const props = defineProps({
  seats: {
    type: Array,
    default: () => []
  },
  availabilityMap: {
    type: Object,
    default: () => ({})
  },
  selectedSeats: {
    type: Array,
    default: () => []
  },
  heldSeats: {
    type: Array,
    default: () => []
  },
  unavailableSeats: {
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

// Active Deck Tab: 'LOWER' or 'UPPER' (only used when explicit decks exist)
const activeDeck = ref('LOWER')

// Physical seat normalization from backend SeatDto:
// { id, tripId, seatNumber, seatType, priceMultiplier, finalPrice }
// PRE-05: Remove A/B floor inference unless explicitly provided in seat metadata.
const normalizedSeats = computed(() => {
  return (props.seats || []).map(s => {
    const code = s.seatNumber || s.seatCode || s.code || ''
    const price = Number(s.finalPrice) || Number(s.price) || props.basePrice || 0
    let deck = null
    if (s.deck) {
      const d = String(s.deck).toUpperCase()
      if (d === 'LOWER' || d === 'UPPER' || d === 'SINGLE') {
        deck = d
      }
    }
    return {
      id: s.id,
      code,
      deck,
      seatType: s.seatType || 'STANDARD',
      priceMultiplier: s.priceMultiplier || 1.0,
      price,
      rawStatus: (s.status || '').toUpperCase()
    }
  })
})

const hasMultipleDecks = computed(() => {
  const hasLower = normalizedSeats.value.some(s => s.deck === 'LOWER')
  const hasUpper = normalizedSeats.value.some(s => s.deck === 'UPPER')
  return hasLower && hasUpper
})

const lowerDeckSeats = computed(() => {
  if (!hasMultipleDecks.value) return normalizedSeats.value
  return normalizedSeats.value.filter(s => s.deck === 'LOWER')
})

const upperDeckSeats = computed(() => {
  return normalizedSeats.value.filter(s => s.deck === 'UPPER')
})

/**
 * Seat State Priority Rules:
 * 1. HELD_BY_ME: In active booking held by current passenger
 * 2. CONFIRMED_UNAVAILABLE: In unavailableSeats (conflict), OR backend availability is SOLD/HELD, OR raw status
 * 3. SELECTED: Current passenger booking intent
 * 4. AVAILABLE: Backend availability confirms AVAILABLE
 * 5. UNVERIFIED: Catalog seat existence (availability verified upon hold)
 */
function getSeatStatus(seat) {
  const code = seat.code
  if (props.heldSeats.includes(code)) return 'HELD_BY_ME'
  if (props.unavailableSeats.includes(code)) return 'CONFIRMED_UNAVAILABLE'

  const avail = props.availabilityMap?.[code]
  if (avail === 'SOLD' || avail === 'HELD') {
    return 'CONFIRMED_UNAVAILABLE'
  }
  if (['BOOKED', 'SOLD', 'CONFIRMED', 'UNAVAILABLE', 'HELD_BY_OTHER'].includes(seat.rawStatus)) {
    return 'CONFIRMED_UNAVAILABLE'
  }
  if (props.selectedSeats.includes(code)) return 'SELECTED'
  if (avail === 'AVAILABLE') return 'AVAILABLE'
  return 'UNVERIFIED'
}

function handleSeatClick(seat) {
  if (props.readonly) return
  const status = getSeatStatus(seat)
  if (status === 'CONFIRMED_UNAVAILABLE') return
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
          Chọn chỗ ngồi danh mục trên sơ đồ xe (Tối đa {{ maxSelectable }} chỗ/lần đặt)
        </p>
      </div>

      <!-- Deck Switcher Tabs (Only if multiple decks explicitly provided in seat metadata) -->
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
          Tầng Dưới
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
          Tầng Trên
        </button>
      </div>
    </div>

    <!-- Informational Verification Alert (PRE-05: Customer-Friendly Vietnamese Wording) -->
    <div class="p-3 bg-muted/40 border border-border rounded-lg flex items-start gap-2.5 text-xs text-muted-foreground" data-testid="seat-contract-notice">
      <AlertCircle class="w-4 h-4 text-primary shrink-0 mt-0.5" />
      <div>
        <span class="font-medium text-foreground">Xác thực tính khả dụng: </span>
        Tình trạng chỗ thực tế sẽ được hệ thống kiểm tra và xác nhận khi quý khách bấm 'Giữ Chỗ'.
      </div>
    </div>

    <!-- Visual Status Legend Bar (Accessible with Symbols & Text Labels) -->
    <div class="py-3 px-3.5 bg-muted/30 rounded-lg border border-border flex flex-wrap items-center justify-between gap-3 text-xs">
      <div class="flex items-center gap-2">
        <div class="w-4 h-4 rounded border border-border bg-card shadow-xs"></div>
        <span class="text-muted-foreground font-medium">Chỗ trống (Có thể chọn)</span>
      </div>
      <div class="flex items-center gap-2">
        <div class="w-4 h-4 rounded bg-primary text-primary-foreground flex items-center justify-center text-[10px] font-bold">
          <Check class="w-3 h-3 stroke-[3]" />
        </div>
        <span class="text-muted-foreground font-medium">Đang chọn (Ý định giữ)</span>
      </div>
      <div class="flex items-center gap-2">
        <div class="w-4 h-4 rounded bg-primary/10 border border-primary text-primary flex items-center justify-center text-[10px]">
          <Lock class="w-2.5 h-2.5" />
        </div>
        <span class="text-muted-foreground font-medium">Đang giữ chỗ (Của bạn)</span>
      </div>
      <div class="flex items-center gap-2">
        <div class="w-4 h-4 rounded bg-muted border border-border text-muted-foreground/60 flex items-center justify-center text-[10px]">
          <X class="w-2.5 h-2.5" />
        </div>
        <span class="text-muted-foreground font-medium">Không khả dụng (Đã bán / Đang giữ)</span>
      </div>
    </div>

    <!-- Cabin Grid Container (Neutral Grid or Explicit Multi-Deck) -->
    <div class="bg-muted/10 rounded-xl p-4 sm:p-6 border border-border flex flex-col items-center">
      <!-- Empty State if no seats in deck -->
      <div v-if="normalizedSeats.length === 0" class="py-12 text-center text-xs text-muted-foreground" data-testid="deck-empty">
        Không có dữ liệu chỗ ngồi cho chuyến xe này.
      </div>

      <!-- Berth Grid: Lower Deck (or Unified Neutral Grid when no explicit deck metadata) -->
      <div
        v-else-if="!hasMultipleDecks || activeDeck === 'LOWER'"
        class="w-full max-w-lg space-y-3"
        data-testid="seat-grid-lower"
      >
        <div class="grid grid-cols-3 sm:grid-cols-4 gap-3 sm:gap-4">
          <button
            v-for="seat in lowerDeckSeats"
            :key="seat.code"
            type="button"
            :data-testid="'seat-' + seat.code"
            :disabled="getSeatStatus(seat) === 'CONFIRMED_UNAVAILABLE' || readonly"
            @click="handleSeatClick(seat)"
            :class="[
              'relative rounded-lg p-3 transition-all select-none border text-center flex flex-col items-center justify-center h-20 w-full focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-1',
              (getSeatStatus(seat) === 'AVAILABLE' || getSeatStatus(seat) === 'UNVERIFIED') && 'bg-card border-border hover:border-primary hover:shadow-xs cursor-pointer text-foreground',
              getSeatStatus(seat) === 'SELECTED' && 'bg-primary border-primary text-primary-foreground font-bold shadow-xs cursor-pointer',
              getSeatStatus(seat) === 'HELD_BY_ME' && 'bg-primary/10 border-primary text-primary font-bold shadow-xs cursor-pointer',
              getSeatStatus(seat) === 'CONFIRMED_UNAVAILABLE' && 'bg-muted/60 border-border/60 text-muted-foreground/60 cursor-not-allowed opacity-60'
            ]"
            :aria-label="`Chỗ ${seat.code}, ${getSeatStatus(seat)}`"
          >
            <!-- Seat Icon -->
            <Bus class="w-4 h-4 mb-1" :class="getSeatStatus(seat) === 'SELECTED' ? 'text-primary-foreground' : 'text-muted-foreground'" />
            <span class="text-sm font-semibold tabular-nums">{{ seat.code }}</span>
            <span class="text-[10px] tabular-nums mt-0.5" :class="getSeatStatus(seat) === 'SELECTED' ? 'text-primary-foreground/90' : 'text-muted-foreground'">
              {{ formatCurrency(seat.price) }} đ
            </span>

            <!-- Status Indicator Overlay -->
            <span v-if="getSeatStatus(seat) === 'HELD_BY_ME'" class="absolute top-1 right-1 text-[10px]" title="Chỗ của bạn đã được giữ">
              <Lock class="w-2.5 h-2.5 text-primary" />
            </span>
            <span v-else-if="getSeatStatus(seat) === 'CONFIRMED_UNAVAILABLE'" class="absolute top-1 right-1 text-[10px]" title="Không khả dụng">
              <X class="w-2.5 h-2.5 text-muted-foreground/60" />
            </span>
            <span v-else-if="getSeatStatus(seat) === 'SELECTED'" class="absolute top-1 right-1 text-[10px]">
              <Check class="w-2.5 h-2.5 stroke-[3] text-primary-foreground" />
            </span>
          </button>
        </div>
      </div>

      <!-- Berth Grid: Upper Deck (when explicit deck metadata exists) -->
      <div
        v-else-if="hasMultipleDecks && activeDeck === 'UPPER'"
        class="w-full max-w-lg space-y-3"
        data-testid="seat-grid-upper"
      >
        <div class="grid grid-cols-3 sm:grid-cols-4 gap-3 sm:gap-4">
          <button
            v-for="seat in upperDeckSeats"
            :key="seat.code"
            type="button"
            :data-testid="'seat-' + seat.code"
            :disabled="getSeatStatus(seat) === 'CONFIRMED_UNAVAILABLE' || readonly"
            @click="handleSeatClick(seat)"
            :class="[
              'relative rounded-lg p-3 transition-all select-none border text-center flex flex-col items-center justify-center h-20 w-full focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-1',
              (getSeatStatus(seat) === 'AVAILABLE' || getSeatStatus(seat) === 'UNVERIFIED') && 'bg-card border-border hover:border-primary hover:shadow-xs cursor-pointer text-foreground',
              getSeatStatus(seat) === 'SELECTED' && 'bg-primary border-primary text-primary-foreground font-bold shadow-xs cursor-pointer',
              getSeatStatus(seat) === 'HELD_BY_ME' && 'bg-primary/10 border-primary text-primary font-bold shadow-xs cursor-pointer',
              getSeatStatus(seat) === 'CONFIRMED_UNAVAILABLE' && 'bg-muted/60 border-border/60 text-muted-foreground/60 cursor-not-allowed opacity-60'
            ]"
            :aria-label="`Chỗ ${seat.code}, ${getSeatStatus(seat)}`"
          >
            <!-- Seat Icon -->
            <Bus class="w-4 h-4 mb-1" :class="getSeatStatus(seat) === 'SELECTED' ? 'text-primary-foreground' : 'text-muted-foreground'" />
            <span class="text-sm font-semibold tabular-nums">{{ seat.code }}</span>
            <span class="text-[10px] tabular-nums mt-0.5" :class="getSeatStatus(seat) === 'SELECTED' ? 'text-primary-foreground/90' : 'text-muted-foreground'">
              {{ formatCurrency(seat.price) }} đ
            </span>

            <!-- Status Indicator Overlay -->
            <span v-if="getSeatStatus(seat) === 'HELD_BY_ME'" class="absolute top-1 right-1 text-[10px]" title="Chỗ của bạn đã được giữ">
              <Lock class="w-2.5 h-2.5 text-primary" />
            </span>
            <span v-else-if="getSeatStatus(seat) === 'CONFIRMED_UNAVAILABLE'" class="absolute top-1 right-1 text-[10px]" title="Không khả dụng">
              <X class="w-2.5 h-2.5 text-muted-foreground/60" />
            </span>
            <span v-else-if="getSeatStatus(seat) === 'SELECTED'" class="absolute top-1 right-1 text-[10px]">
              <Check class="w-2.5 h-2.5 stroke-[3] text-primary-foreground" />
            </span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
