import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

/**
 * Configuración de Vite (servidor de desarrollo y empaquetado) y de Vitest (pruebas).
 *
 * proxy: en desarrollo, las llamadas a /api se reenvían al backend en :8080.
 * Así el navegador cree que frontend y API están en el mismo origen y no hay problemas de CORS.
 */
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_API_PROXY ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    restoreMocks: true,
  },
});
