import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8082', // temporal: votacion-service (luego http://localhost:8080 gateway)
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8083', // resultados-service (WebSocket)
        ws: true,
        changeOrigin: true,
      },
    },
  },
})
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const gateway = process.env.VITE_GATEWAY_URL ?? 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: gateway,
        changeOrigin: true
      },
      '/ws': {
        target: gateway,
        ws: true,
        changeOrigin: true
      }
    }
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.js'
  }
});
