<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import StatusBadge from '@/shared/components/StatusBadge.vue'

const props = defineProps({
  trip: {
    type: Object,
    required: true
  }
})

const router = useRouter()

const formattedPrice = computed(() => {
  return (props.trip.price || 0).toLocaleString('vi-VN')
})

function onSelectTrip() {
  router.push(`/trips/${props.trip.id}/seats`)
}
</script>

<template>
  <div
    class="bg-surface rounded-xl border border-border-default p-5 shadow-card hover:shadow-dropdown transition-all flex flex-col justify-between"
    data-testid="trip-card"
  >
    <div class="space-y-3">
      <!-- Top Badges -->
      <div class="flex items-center justify-between">
        <span class="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-blue-50 text-primary-600 border border-blue-200">
          {{ trip.operator }}
        </span>
        <span
          class="text-xs font-semibold tabular-nums"
          :class="trip.availableSeats > 5 ? 'text-emerald-600' : 'text-amber-600'"
        >
          {{ trip.availableSeats > 0 ? `Còn ${trip.availableSeats} chỗ` : 'Hết chỗ' }}
        </span>
      </div>

      <!-- Route & Times -->
      <div>
        <h3 class="text-base font-bold text-text-primary">{{ trip.route }}</h3>
        <p class="text-xs text-text-secondary flex items-center gap-2 mt-1">
          <span class="font-medium text-text-primary">🕒 {{ trip.departureTime }}</span>
          <span>→</span>
          <span class="font-medium text-text-primary">{{ trip.arrivalTime }}</span>
          <span class="text-text-muted">({{ trip.duration }})</span>
        </p>
      </div>

      <!-- Coach Info -->
      <div class="text-xs text-text-muted flex items-center gap-2 pt-1 border-t border-border-default/60">
        <span>🚌 {{ trip.coachType }}</span>
        <span>•</span>
        <span>Biển số: <strong class="text-text-primary">{{ trip.plate }}</strong></span>
      </div>
    </div>

    <!-- Pricing & Action CTA -->
    <div class="pt-4 mt-4 border-t border-border-default flex items-center justify-between">
      <div>
        <span class="text-[11px] text-text-muted block">Giá vé từ</span>
        <span class="text-lg font-bold text-primary-600 tabular-nums">
          {{ formattedPrice }} <span class="text-xs font-semibold">đ</span>
        </span>
      </div>

      <button
        type="button"
        @click="onSelectTrip"
        :disabled="trip.availableSeats <= 0"
        data-testid="btn-select-trip"
        class="px-4 py-2 bg-primary-600 hover:bg-primary-700 disabled:bg-slate-300 disabled:cursor-not-allowed text-white font-semibold rounded-lg text-xs shadow-sm transition-all flex items-center gap-1.5"
      >
        <span>Chọn Chỗ</span>
        <span>→</span>
      </button>
    </div>
  </div>
</template>
