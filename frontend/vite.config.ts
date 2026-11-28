import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    host: '127.0.0.1',
    port: Number(process.env.FRONTEND_PORT || 5173),
    strictPort: true,
    proxy: {
      '/api': {
        target: process.env.VITE_BACKEND_PROXY_TARGET || 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'happy-dom',
    globals: true,
    include: ['src/**/*.spec.ts', 'scripts/**/*.spec.ts'],
    exclude: ['**/e2e/**', '**/node_modules/**'],
  },
})
