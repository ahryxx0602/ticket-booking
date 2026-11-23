import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import Aura from '@primeuix/themes/aura'
import { definePreset } from '@primeuix/themes'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'

import App from './App.vue'
import router from './app/router'

import 'primeicons/primeicons.css'
import './assets/main.css'

// VeloBus Design System Preset (Direction A: Premium Mobility SaaS)
const VeloBusPreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#eff6ff',
      100: '#dbeafe',
      200: '#bfdbfe',
      300: '#93c5fd',
      400: '#60a5fa',
      500: '#3b82f6',
      600: '#2563eb', // --primary-600 (Single Source of Truth)
      700: '#1d4ed8', // --primary-700 hover
      800: '#1e40af',
      900: '#1e3a8a',
      950: '#172554',
    },
    colorScheme: {
      light: {
        surface: {
          0: '#ffffff',
          50: '#f8fafc',  // --canvas
          100: '#f1f5f9', // --surface-muted
          200: '#e2e8f0', // --border-default
          300: '#cbd5e1', // --border-strong
          400: '#94a3b8',
          500: '#64748b', // --text-muted
          600: '#475569', // --text-secondary
          700: '#334155',
          800: '#1e293b',
          900: '#0f172a', // --text-primary
          950: '#020617',
        },
      },
    },
  },
})

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(PrimeVue, {
  theme: {
    preset: VeloBusPreset,
    options: {
      darkModeSelector: '.app-dark',
    },
  },
})
app.use(ToastService)
app.use(ConfirmationService)

app.mount('#app')
