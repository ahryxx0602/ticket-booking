import PrimeUI from 'tailwindcss-primeui'

/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ['selector', ':where(.dark, .app-dark)'],
  content: [
    './index.html',
    './src/**/*.{vue,js,ts,jsx,tsx}',
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Outfit', 'Inter', 'Be Vietnam Pro', 'system-ui', '-apple-system', 'sans-serif'],
        mono: ['Geist Mono', 'ui-monospace', 'SFMono-Regular', 'Menlo', 'Monaco', 'Consolas', 'monospace'],
      },
      colors: {
        // Shadcn & Claude+ Semantic CSS Variables
        border: 'var(--border)',
        input: 'var(--input)',
        ring: 'var(--ring)',
        background: 'var(--background)',
        foreground: 'var(--foreground)',
        primary: {
          DEFAULT: 'var(--primary)',
          foreground: 'var(--primary-foreground)',
          effective: 'var(--primary-effective)',
        },
        secondary: {
          DEFAULT: 'var(--secondary)',
          foreground: 'var(--secondary-foreground)',
        },
        destructive: {
          DEFAULT: 'var(--destructive)',
          foreground: 'var(--destructive-foreground)',
          effective: 'var(--destructive-effective)',
        },
        muted: {
          DEFAULT: 'var(--muted)',
          foreground: 'var(--muted-foreground)',
        },
        accent: {
          DEFAULT: 'var(--accent)',
          foreground: 'var(--accent-foreground)',
        },
        popover: {
          DEFAULT: 'var(--popover)',
          foreground: 'var(--popover-foreground)',
        },
        card: {
          DEFAULT: 'var(--card)',
          foreground: 'var(--card-foreground)',
        },
        sidebar: {
          DEFAULT: 'var(--sidebar)',
          foreground: 'var(--sidebar-foreground)',
          primary: 'var(--sidebar-primary)',
          'primary-foreground': 'var(--sidebar-primary-foreground)',
          accent: 'var(--sidebar-accent)',
          'accent-foreground': 'var(--sidebar-accent-foreground)',
          border: 'var(--sidebar-border)',
          ring: 'var(--sidebar-ring)',
        },
        status: {
          success: 'var(--status-success)',
          'success-bg': 'var(--status-success-bg)',
          warning: 'var(--status-warning)',
          'warning-bg': 'var(--status-warning-bg)',
          danger: 'var(--status-danger)',
          'danger-bg': 'var(--status-danger-bg)',
          info: 'var(--status-info)',
          'info-bg': 'var(--status-info-bg)',
        },

        // Legacy VeloBus tokens (Retained for existing production views)
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
        DEFAULT: 'var(--radius)',
        sm: 'var(--radius-checkbox, 4px)',
        md: 'var(--radius-control, 8px)',
        lg: 'calc(var(--radius) - 4px)',
        xl: 'var(--radius-surface, 16px)',
        hero: '20px',
        // Semantic role-specific geometry tokens (REV-01)
        checkbox: 'var(--radius-checkbox, 4px)',
        calendar: 'var(--radius-calendar, 6px)',
        control: 'var(--radius-control, 8px)',
        badge: 'var(--radius-badge, 6px)',
        surface: 'var(--radius-surface, 16px)',
        pill: 'var(--radius-pill, 9999px)',
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
