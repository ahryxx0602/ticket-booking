<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Bus,
  Clock,
  MapPin,
  ArrowRight,
  Armchair,
  Moon
} from 'lucide-vue-next'

const props = defineProps({
  trip: {
    type: Object,
    required: true
  }
})

const router = useRouter()

// PRE-02: Support authentic TripDto from backend without fake semantics
const displayRoute = computed(() => {
  if (props.trip.route) return props.trip.route
  const from = props.trip.departureCity || props.trip.departureStationName || 'Điểm đi'
  const to = props.trip.arrivalCity || props.trip.arrivalStationName || 'Điểm đến'
  return `${from} → ${to}`
})

const hasOperator = computed(() => !!(props.trip.operator && props.trip.operator.trim()))
const operatorName = computed(() => props.trip.operator?.trim() || '')
const tripCode = computed(() => props.trip.tripCode || (props.trip.id ? `#${props.trip.id}` : ''))

const displayCoachType = computed(() => {
  return props.trip.transportType || props.trip.coachType || 'Tiêu chuẩn'
})

const rawPrice = computed(() => {
  if (props.trip.basePrice !== undefined && props.trip.basePrice !== null) {
    const num = Number(props.trip.basePrice)
    return isNaN(num) || num <= 0 ? null : num
  }
  if (props.trip.price !== undefined && props.trip.price !== null) {
    const num = Number(props.trip.price)
    return isNaN(num) || num <= 0 ? null : num
  }
  return null
})

const formattedPrice = computed(() => {
  if (rawPrice.value !== null) {
    return rawPrice.value.toLocaleString('vi-VN')
  }
  return null
})

// PRE-02: Do NOT equate totalSeats to availableSeats or claim "Còn N chỗ"
const totalSeats = computed(() => {
  if (props.trip.totalSeats !== undefined && props.trip.totalSeats !== null) {
    const num = Number(props.trip.totalSeats)
    return isNaN(num) || num <= 0 ? null : num
  }
  return null
})

// Trip status check (SCHEDULED is open for booking)
const isScheduled = computed(() => {
  if (!props.trip.status) return true
  return props.trip.status === 'SCHEDULED'
})

function formatTime(val) {
  if (!val) return '—'
  if (typeof val === 'string' && val.length === 5 && val.includes(':')) {
    return val
  }
  try {
    const d = new Date(val)
    if (isNaN(d.getTime())) return String(val)
    return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false })
  } catch (_) {
    return String(val)
  }
}

const departureTimeDisplay = computed(() => formatTime(props.trip.departureTime))
const arrivalTimeDisplay = computed(() => formatTime(props.trip.arrivalTime))

// PRE-02: Overnight arrival indicator based on actual timestamps
const isOvernight = computed(() => {
  if (!props.trip.departureTime || !props.trip.arrivalTime) return false
  try {
    const dep = new Date(props.trip.departureTime)
    const arr = new Date(props.trip.arrivalTime)
    if (isNaN(dep.getTime()) || isNaN(arr.getTime())) return false
    return arr.getDate() !== dep.getDate() || (arr.getTime() - dep.getTime() >= 20 * 3600 * 1000)
  } catch (_) {
    return false
  }
})

function onSelectTrip() {
  if (!isScheduled.value) return
  router.push(`/trips/${props.trip.id}/seats`)
}
</script>

<template>
  <Card
    class="border border-border bg-card shadow-xs hover:border-primary/40 hover:shadow-sm transition-all duration-200 overflow-hidden"
    data-testid="trip-card"
  >
    <CardContent class="p-4 sm:p-5 flex flex-col justify-between gap-4">
      <!-- Header row: Operator Badge (if available), Trip Code, Coach Type & Total Capacity -->
      <div class="flex flex-wrap items-center justify-between gap-2 pb-3 border-b border-border">
        <div class="flex items-center gap-2 flex-wrap">
          <Badge v-if="hasOperator" variant="outline" class="text-xs font-semibold px-2.5 py-0.5 border-primary/30 text-primary bg-primary/5">
            {{ operatorName }}
          </Badge>
          <Badge variant="secondary" class="text-xs font-mono font-medium px-2 py-0.5 text-muted-foreground">
            {{ tripCode }}
          </Badge>
          <span class="text-xs text-muted-foreground flex items-center gap-1">
            <Bus class="w-3.5 h-3.5 text-muted-foreground" />
            <span>{{ displayCoachType }}</span>
          </span>
          <span v-if="trip.plate" class="text-xs text-muted-foreground">
            • <strong class="text-foreground font-mono">{{ trip.plate }}</strong>
          </span>
        </div>

        <!-- PRE-02: Capacity label instead of fabricated availability -->
        <div class="flex items-center gap-1.5">
          <Armchair class="w-3.5 h-3.5 text-muted-foreground" />
          <span class="text-xs font-medium text-muted-foreground tabular-nums font-mono" data-testid="trip-capacity">
            {{ totalSeats ? `Sức chứa ${totalSeats} chỗ` : 'Kiểm tra chỗ trống' }}
          </span>
        </div>
      </div>

      <!-- Main info: Route and Times -->
      <div class="space-y-2">
        <h3 class="text-base font-bold text-foreground tracking-tight">
          {{ displayRoute }}
        </h3>

        <!-- Time schedule block -->
        <div class="flex flex-wrap items-center gap-3 sm:gap-6 text-xs text-muted-foreground pt-1">
          <!-- Departure -->
          <div class="space-y-0.5">
            <div class="text-lg font-bold text-foreground font-mono tabular-nums leading-none flex items-center gap-1">
              <Clock class="w-3.5 h-3.5 text-primary" />
              <span>{{ departureTimeDisplay }}</span>
            </div>
            <p class="text-[11px] text-muted-foreground line-clamp-1">
              {{ trip.departureStationName || trip.departureCity || 'Điểm xuất phát' }}
            </p>
          </div>

          <!-- Duration / Separator arrow -->
          <div class="flex flex-col items-center justify-center px-1">
            <span v-if="trip.duration" class="text-[10px] text-muted-foreground font-medium mb-0.5">{{ trip.duration }}</span>
            <ArrowRight class="w-4 h-4 text-primary/70" />
          </div>

          <!-- Arrival -->
          <div class="space-y-0.5">
            <div class="text-lg font-bold text-foreground font-mono tabular-nums leading-none flex items-center gap-1.5">
              <span>{{ arrivalTimeDisplay }}</span>
              <Badge v-if="isOvernight" variant="secondary" class="text-[10px] px-1.5 py-0 h-4 font-semibold text-amber-600 dark:text-amber-400 bg-amber-500/10 border-amber-500/20 gap-0.5">
                <Moon class="w-2.5 h-2.5" />
                <span>+1 ngày</span>
              </Badge>
            </div>
            <p class="text-[11px] text-muted-foreground line-clamp-1">
              {{ trip.arrivalStationName || trip.arrivalCity || 'Điểm kết thúc' }}
            </p>
          </div>
        </div>
      </div>

      <!-- Footer row: Pricing & Selection Button -->
      <div class="pt-3 border-t border-border flex items-center justify-between gap-3">
        <div>
          <span class="text-[11px] text-muted-foreground block">Giá vé từ</span>
          <div v-if="formattedPrice" class="text-lg sm:text-xl font-black text-primary tabular-nums font-mono">
            {{ formattedPrice }} <span class="text-xs font-bold text-foreground">đ</span>
          </div>
          <div v-else class="text-sm font-semibold text-muted-foreground font-mono">
            Liên hệ
          </div>
        </div>

        <Button
          type="button"
          size="sm"
          :disabled="!isScheduled"
          data-testid="btn-select-trip"
          class="h-9 px-4 text-xs font-semibold shadow-xs gap-1.5"
          @click="onSelectTrip"
        >
          <span>{{ isScheduled ? 'Chọn Chỗ' : 'Không khả dụng' }}</span>
          <ArrowRight v-if="isScheduled" class="w-3.5 h-3.5" />
        </Button>
      </div>
    </CardContent>
  </Card>
</template>
