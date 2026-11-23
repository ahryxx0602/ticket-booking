import PrimeUI from 'tailwindcss-primeui'

/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ['selector', '[class*="app-dark"]'],
  content: [
    './index.html',
    './src/**/*.{vue,js,ts,jsx,tsx}',
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'Be Vietnam Pro', 'system-ui', '-apple-system', 'sans-serif'],
      },
      colors: {
        'brand-navy': '#142B4A',
        'brand-navy-hover': '#1E3A5F',
        'primary-600': '#2563EB',
        'primary-700': '#1D4ED8',
        'primary-100': '#DBEAFE',
        'accent-500': '#F59E0B',
        canvas: '#F8FAFC',
        surface: '#FFFFFF',
        'surface-muted': '#F1F5F9',
        'border-default': '#E2E8F0',
        'border-strong': '#CBD5E1',
        'text-primary': '#0F172A',
        'text-secondary': '#475569',
        'text-muted': '#64748B',
      },
      borderRadius: {
        sm: '6px',
        md: '8px',
        lg: '12px',
        xl: '16px',
        hero: '20px',
      },
      boxShadow: {
        card: '0 2px 8px rgba(15, 23, 42, .04)',
        dropdown: '0 8px 24px rgba(15, 23, 42, .10)',
        dialog: '0 20px 60px rgba(15, 23, 42, .18)',
      },
    },
  },
  plugins: [PrimeUI],
}
