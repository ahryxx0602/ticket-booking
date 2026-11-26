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
  ShieldCheck,
  Armchair
} from 'lucide-vue-next'

const props = defineProps({
  trip: {
    type: Object,
    required: true
  }
})

const router = useRouter()

// Support both TripDto from backend and legacy object shape
const displayRoute = computed(() => {
  if (props.trip.route) return props.trip.route
  const from = props.trip.departureCity || props.trip.departureStationName || 'Điểm đi'
  const to = props.trip.arrivalCity || props.trip.arrivalStationName || 'Điểm đến'
  return `${from} → ${to}`
})

const displayOperator = computed(() => {
  return props.trip.operator || props.trip.tripCode || 'VeloBus Partner'
})

const displayCoachType = computed(() => {
  return props.trip.transportType || props.trip.coachType || 'Xe Khách Giường Nằm'
})

const rawPrice = computed(() => {
  if (props.trip.basePrice !== undefined && props.trip.basePrice !== null) {
    return Number(props.trip.basePrice)
  }
  return Number(props.trip.price || 0)
})

const formattedPrice = computed(() => {
  return rawPrice.value.toLocaleString('vi-VN')
})

const availableSeats = computed(() => {
  if (props.trip.availableSeats !== undefined) return Number(props.trip.availableSeats)
  if (props.trip.totalSeats !== undefined) return Number(props.trip.totalSeats)
  return 0
})

const isSoldOut = computed(() => availableSeats.value <= 0)

function formatTime(val) {
  if (!val) return '—'
  // If already "HH:mm"
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

function onSelectTrip() {
  if (isSoldOut.value) return
  router.push(`/trips/${props.trip.id}/seats`)
}
</script>

<template>
  <Card
    class="border border-border bg-card shadow-xs hover:border-primary/40 hover:shadow-sm transition-all duration-200 overflow-hidden"
    data-testid="trip-card"
  >
    <CardContent class="p-4 sm:p-5 flex flex-col justify-between gap-4">
      <!-- Header row: Operator Badge, Coach Type & Available seats -->
      <div class="flex flex-wrap items-center justify-between gap-2 pb-3 border-b border-border">
        <div class="flex items-center gap-2 flex-wrap">
          <Badge variant="outline" class="text-xs font-semibold px-2.5 py-0.5 border-primary/30 text-primary bg-primary/5">
            {{ displayOperator }}
          </Badge>
          <span class="text-xs text-muted-foreground flex items-center gap-1">
            <Bus class="w-3.5 h-3.5 text-muted-foreground" />
            <span>{{ displayCoachType }}</span>
          </span>
          <span v-if="trip.plate" class="text-xs text-muted-foreground">
            • <strong class="text-foreground font-mono">{{ trip.plate }}</strong>
          </span>
        </div>

        <div class="flex items-center gap-1.5">
          <Armchair class="w-3.5 h-3.5" :class="!isSoldOut ? 'text-emerald-600 dark:text-emerald-400' : 'text-muted-foreground'" />
          <span
            class="text-xs font-semibold tabular-nums"
            :class="!isSoldOut ? 'text-emerald-600 dark:text-emerald-400' : 'text-muted-foreground'"
          >
            {{ !isSoldOut ? `Còn ${availableSeats} chỗ` : 'Hết chỗ' }}
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
              {{ trip.departureStationName || trip.departureCity || 'Bến xuất phát' }}
            </p>
          </div>

          <!-- Duration / Separator arrow -->
          <div class="flex flex-col items-center justify-center px-1">
            <span v-if="trip.duration" class="text-[10px] text-muted-foreground font-medium mb-0.5">{{ trip.duration }}</span>
            <ArrowRight class="w-4 h-4 text-primary/70" />
          </div>

          <!-- Arrival -->
          <div class="space-y-0.5">
            <div class="text-lg font-bold text-foreground font-mono tabular-nums leading-none">
              {{ arrivalTimeDisplay }}
            </div>
            <p class="text-[11px] text-muted-foreground line-clamp-1">
              {{ trip.arrivalStationName || trip.arrivalCity || 'Bến kết thúc' }}
            </p>
          </div>
        </div>
      </div>

      <!-- Footer row: Pricing & Selection Button -->
      <div class="pt-3 border-t border-border flex items-center justify-between gap-3">
        <div>
          <span class="text-[11px] text-muted-foreground block">Giá vé từ</span>
          <div class="text-lg sm:text-xl font-black text-primary tabular-nums font-mono">
            {{ formattedPrice }} <span class="text-xs font-bold text-foreground">đ</span>
          </div>
        </div>

        <Button
          type="button"
          size="sm"
          :disabled="isSoldOut"
          data-testid="btn-select-trip"
          class="h-9 px-4 text-xs font-semibold shadow-xs gap-1.5"
          @click="onSelectTrip"
        >
          <span>Chọn Chỗ</span>
          <ArrowRight class="w-3.5 h-3.5" />
        </Button>
      </div>
    </CardContent>
  </Card>
</template>
