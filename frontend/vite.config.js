import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const gateway = globalThis.process?.env?.VITE_GATEWAY_URL ?? 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // api-gateway (enruta /api/v1/auth y /api/votos)
        changeOrigin: true,
      },
      '/ws': {
        target: gateway,
        ws: true,
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.js',
  },
})
