import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    coverage: {
      provider: 'v8',
      include: [
        'src/**/*.vue',
        'src/services/**/*.js',
        'src/stores/**/*.js',
        'src/router/**/*.js'
      ],
      exclude: [
        'src/main.js',
        '**/*.test.js'
      ]
    }
  },
  server: {
    port: 3000,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8084',
        changeOrigin: true
      }
    }
  }
})
