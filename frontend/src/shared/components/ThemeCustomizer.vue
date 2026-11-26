<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import {
  useTheme,
  type ThemeMode,
  type DensityMode,
  type RadiusPreset
} from '@/shared/composables/useTheme'
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import {
  Sun,
  Moon,
  Laptop,
  Paintbrush,
  RotateCcw,
  Check,
  X,
  ShieldCheck,
  AlertTriangle
} from 'lucide-vue-next'
import { toast } from 'vue-sonner'

const props = withDefaults(
  defineProps<{
    modelValue?: boolean
    showFloatingTrigger?: boolean
  }>(),
  {
    modelValue: undefined,
    showFloatingTrigger: true
  }
)

const emits = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
}>()

const isOpen = ref(false)
const drawerRef = ref<HTMLElement | null>(null)
// REV-06 & B2-REV-05: Track element that opened the drawer for focus restoration
let lastFocusedElement: HTMLElement | null = null

watch(
  () => props.modelValue,
  (val) => {
    if (val !== undefined) {
      isOpen.value = val
    }
  },
  { immediate: true }
)

watch(isOpen, (val) => {
  emits('update:modelValue', val)
  // REV-06: Body scroll lock when drawer is open
  if (val) {
    lastFocusedElement = document.activeElement as HTMLElement
    document.body.classList.add('theme-customizer-open')
    nextTick(() => {
      const container = drawerRef.value
      if (container) {
        const closeBtn = container.querySelector<HTMLElement>('button[aria-label="Đóng tùy biến"]')
        if (closeBtn) {
          closeBtn.focus()
        } else {
          const first = container.querySelector<HTMLElement>('button, [href], input, select, textarea')
          first?.focus()
        }
      }
    })
  } else {
    document.body.classList.remove('theme-customizer-open')
    // Restore focus to trigger element after close
    if (lastFocusedElement) {
      lastFocusedElement.focus()
      lastFocusedElement = null
    }
  }
})

// B2-REV-05: Keyboard Escape & Tab/Shift+Tab Focus Trap
function handleKeydown(e: KeyboardEvent) {
  if (!isOpen.value) return

  if (e.key === 'Escape') {
    handleCancel()
    return
  }

  if (e.key === 'Tab') {
    const container = drawerRef.value
    if (!container) return

    const focusableSelectors = 'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
    const focusableElements = Array.from(container.querySelectorAll<HTMLElement>(focusableSelectors))
      .filter(el => el.offsetParent !== null)

    if (focusableElements.length === 0) return

    const firstElement = focusableElements[0]
    const lastElement = focusableElements[focusableElements.length - 1]

    if (e.shiftKey) {
      if (document.activeElement === firstElement || !container.contains(document.activeElement)) {
        e.preventDefault()
        lastElement.focus()
      }
    } else {
      if (document.activeElement === lastElement || !container.contains(document.activeElement)) {
        e.preventDefault()
        firstElement.focus()
      }
    }
  }
}
onMounted(() => document.addEventListener('keydown', handleKeydown))
onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeydown)
  document.body.classList.remove('theme-customizer-open')
})

const {
  draftTheme,
  currentTheme,
  isDark,
  resolvedMode,
  colorPresets,
  contrastVsWhite,
  contrastVsDark,
  effectiveTextColor,
  setThemeMode,
  setPrimaryColor,
  setRadius,
  setDensity,
  commitTheme,
  cancelDraftTheme,
  resetToClaudeDefaults
} = useTheme()

const radiusOptions: { label: string; value: RadiusPreset }[] = [
  { label: '6px', value: '0.375rem' },
  { label: '8px', value: '0.5rem' },
  { label: '12px', value: '0.75rem' },
  { label: '16px', value: '1rem' }
]

const densityOptions: { label: string; value: DensityMode; desc: string }[] = [
  { label: 'Compact', value: 'compact', desc: 'Cao 32px, tối ưu dashboard & bảng dữ liệu lớn' },
  { label: 'Default', value: 'default', desc: 'Cao 38px, chuẩn Claude+ Mobility SaaS' },
  { label: 'Comfortable', value: 'comfortable', desc: 'Cao 44px, thoáng đãng cho tablet & mobile' }
]

function handleSave() {
  commitTheme()
  isOpen.value = false
  toast.success('Đã lưu cấu hình giao diện thành công!')
}

function handleCancel() {
  cancelDraftTheme()
  isOpen.value = false
}

function handleReset() {
  resetToClaudeDefaults()
  toast.info('Đã khôi phục giao diện Claude+ mặc định.')
}
</script>

<template>
  <div>
    <!-- Trigger Floating Button (Nút nổi tròn kinh điển ở góc dưới phải z-40) -->
    <Button
      v-if="showFloatingTrigger"
      variant="outline"
      size="sm"
      class="fixed bottom-5 right-5 z-40 flex items-center gap-2 rounded-full shadow-lg border-primary/30 bg-background/95 backdrop-blur hover:bg-accent cursor-pointer transition-transform duration-150 hover:scale-105 active:scale-95"
      @click="isOpen = !isOpen"
    >
      <Paintbrush class="h-4 w-4 text-primary" />
      <span class="font-medium text-xs">Tùy biến Theme</span>
    </Button>

    <!-- Slide-over Drawer / Panel (Teleport thẳng ra body z-50, không bị che khuất hay ảnh hưởng bởi layout) -->
    <Teleport to="body">
      <div
        v-if="isOpen"
        class="fixed inset-0 z-50 flex justify-end bg-black/50 backdrop-blur-xs transition-opacity duration-300"
        @click.self="handleCancel"
      >
        <div
          ref="drawerRef"
          role="dialog"
          aria-modal="true"
          aria-labelledby="theme-customizer-title"
          data-testid="theme-customizer-drawer"
          class="flex flex-col h-full w-full max-w-md bg-background border-l shadow-2xl p-6 overflow-y-auto animate-in slide-in-from-right duration-300"
        >
        <div class="flex items-center justify-between pb-4 border-b">
          <div class="flex items-center gap-2">
            <div class="p-2 rounded-lg bg-primary/10 text-primary">
              <Paintbrush class="h-5 w-5" />
            </div>
            <div>
              <h2 id="theme-customizer-title" class="text-base font-semibold">VeloBus Theme Studio</h2>
              <p class="text-xs text-muted-foreground">Claude+ Warm Minimalist Customizer</p>
            </div>
          </div>
          <Button variant="ghost" size="icon" class="h-8 w-8" aria-label="Đóng tùy biến" @click="handleCancel">
            <X class="h-4 w-4" />
          </Button>
        </div>

        <div class="py-5 space-y-6 flex-1">
          <!-- 1. Appearance Mode -->
          <div class="space-y-2">
            <div class="flex items-center justify-between">
              <label class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Chế độ hiển thị</label>
              <Badge variant="outline" class="text-[10px]">
                Đang dùng: {{ resolvedMode === 'dark' ? 'Tối' : 'Sáng' }}
              </Badge>
            </div>
            <div class="grid grid-cols-3 gap-2">
              <button
                type="button"
                :class="[
                  'flex flex-col items-center justify-center p-3 rounded-lg border text-xs font-medium gap-2 transition-all',
                  draftTheme.mode === 'light'
                    ? 'border-primary bg-primary/10 text-primary ring-2 ring-primary/20'
                    : 'border-border hover:bg-muted text-muted-foreground'
                ]"
                @click="setThemeMode('light')"
              >
                <Sun class="h-4 w-4" />
                <span>Sáng</span>
              </button>

              <button
                type="button"
                :class="[
                  'flex flex-col items-center justify-center p-3 rounded-lg border text-xs font-medium gap-2 transition-all',
                  draftTheme.mode === 'dark'
                    ? 'border-primary bg-primary/10 text-primary ring-2 ring-primary/20'
                    : 'border-border hover:bg-muted text-muted-foreground'
                ]"
                @click="setThemeMode('dark')"
              >
                <Moon class="h-4 w-4" />
                <span>Tối</span>
              </button>

              <button
                type="button"
                :class="[
                  'flex flex-col items-center justify-center p-3 rounded-lg border text-xs font-medium gap-2 transition-all',
                  draftTheme.mode === 'system'
                    ? 'border-primary bg-primary/10 text-primary ring-2 ring-primary/20'
                    : 'border-border hover:bg-muted text-muted-foreground'
                ]"
                @click="setThemeMode('system')"
              >
                <Laptop class="h-4 w-4" />
                <span>Hệ thống</span>
              </button>
            </div>
          </div>

          <!-- 2. Primary Color Palette & Contrast Validation -->
          <div class="space-y-2">
            <div class="flex items-center justify-between">
              <label class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Màu chủ đạo</label>
              <span class="text-xs font-mono text-muted-foreground">{{ draftTheme.primaryColor }}</span>
            </div>
            <div class="grid grid-cols-5 gap-2">
              <button
                v-for="preset in colorPresets"
                :key="preset.value"
                type="button"
                :style="{ backgroundColor: preset.value }"
                :title="preset.name"
                class="h-9 w-full rounded-md flex items-center justify-center text-white shadow-xs transition-transform hover:scale-105 relative"
                @click="setPrimaryColor(preset.value)"
              >
                <Check v-if="draftTheme.primaryColor === preset.value" class="h-4 w-4 stroke-[3]" />
              </button>
            </div>
            <div class="pt-2 flex items-center gap-2">
              <input
                type="color"
                :value="draftTheme.primaryColor"
                class="h-8 w-12 rounded border cursor-pointer bg-transparent"
                @input="(e: any) => setPrimaryColor(e.target.value)"
              />
              <span class="text-xs text-muted-foreground">Chọn màu tự do (Custom Color Picker)</span>
            </div>

            <!-- Live WCAG 2.1 Contrast Validator -->
            <div class="mt-2 p-2.5 rounded-lg bg-muted/60 border text-xs space-y-1.5">
              <div class="flex items-center justify-between">
                <span class="font-medium text-muted-foreground">WCAG 2.1 AA Contrast:</span>
                <span class="font-mono font-semibold">{{ contrastVsWhite }}:1 (vs White) | {{ contrastVsDark }}:1 (vs Dark)</span>
              </div>
              <div class="flex items-center gap-1.5">
                <ShieldCheck v-if="contrastVsWhite >= 4.5 || contrastVsDark >= 4.5" class="h-3.5 w-3.5 text-emerald-600 shrink-0" />
                <AlertTriangle v-else class="h-3.5 w-3.5 text-amber-600 shrink-0" />
                <span class="text-[11px] text-muted-foreground">
                  Text tự động thích ứng:
                  <strong class="font-mono text-foreground">{{ effectiveTextColor === '#FFFFFF' ? 'Trắng (#FFFFFF)' : 'Than đậm (#141413)' }}</strong>
                  đảm bảo tương phản đạt chuẩn AA.
                </span>
              </div>
            </div>
          </div>

          <!-- 3. Border Radius -->
          <div class="space-y-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Độ bo góc (Radius)</label>
            <div class="grid grid-cols-4 gap-2">
              <button
                v-for="rad in radiusOptions"
                :key="rad.value"
                type="button"
                :class="[
                  'p-2 text-center rounded border text-xs font-medium transition-all',
                  draftTheme.radius === rad.value
                    ? 'border-primary bg-primary/10 text-primary font-semibold'
                    : 'border-border hover:bg-muted text-muted-foreground'
                ]"
                @click="setRadius(rad.value)"
              >
                {{ rad.label }}
              </button>
            </div>
          </div>

          <!-- 4. Density -->
          <div class="space-y-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Mật độ hiển thị (Density)</label>
            <div class="space-y-1.5">
              <div
                v-for="den in densityOptions"
                :key="den.value"
                :class="[
                  'p-2.5 rounded-lg border text-xs cursor-pointer transition-all flex items-start gap-3',
                  draftTheme.density === den.value
                    ? 'border-primary bg-primary/5 ring-1 ring-primary/30'
                    : 'border-border hover:bg-muted/50'
                ]"
                @click="setDensity(den.value)"
              >
                <div
                  :class="[
                    'mt-0.5 h-3.5 w-3.5 rounded-full border flex items-center justify-center',
                    draftTheme.density === den.value ? 'border-primary bg-primary text-primary-foreground' : 'border-muted-foreground'
                  ]"
                >
                  <span v-if="draftTheme.density === den.value" class="h-1.5 w-1.5 rounded-full bg-white" />
                </div>
                <div>
                  <div class="font-medium text-foreground">{{ den.label }}</div>
                  <div class="text-[11px] text-muted-foreground">{{ den.desc }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 5. Live Preview Demo Card -->
          <div class="space-y-2 pt-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Xem trước tức thì</label>
            <Card class="border shadow-xs">
              <CardHeader class="p-3 pb-1">
                <div class="flex items-center justify-between">
                  <CardTitle class="text-sm">Chuyến xe VeloBus Express</CardTitle>
                  <Badge variant="default">Đang chạy</Badge>
                </div>
                <CardDescription class="text-xs">Sài Gòn → Đà Lạt (Giường nằm 34 chỗ)</CardDescription>
              </CardHeader>
              <CardContent class="p-3 text-xs space-y-1">
                <div class="flex justify-between text-muted-foreground">
                  <span>Khởi hành: 22:30 hôm nay</span>
                  <span class="font-semibold text-primary font-mono">350.000 đ</span>
                </div>
              </CardContent>
              <CardFooter class="p-3 pt-0 flex justify-end gap-2">
                <Button size="sm" variant="outline" class="h-7 text-xs">Chi tiết</Button>
                <Button size="sm" class="h-7 text-xs">Đặt vé ngay</Button>
              </CardFooter>
            </Card>
          </div>
        </div>

        <!-- Footer Actions -->
        <div class="pt-4 border-t flex items-center justify-between gap-2">
          <Button variant="ghost" size="sm" class="text-xs gap-1 text-muted-foreground" @click="handleReset">
            <RotateCcw class="h-3.5 w-3.5" />
            Mặc định
          </Button>
          <div class="flex items-center gap-2">
            <Button variant="outline" size="sm" class="text-xs" @click="handleCancel">
              Hủy
            </Button>
            <Button size="sm" class="text-xs gap-1" @click="handleSave">
              <Check class="h-3.5 w-3.5" />
              Lưu thay đổi
            </Button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</div>
</template>
