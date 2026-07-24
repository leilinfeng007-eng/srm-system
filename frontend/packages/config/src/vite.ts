import vue from '@vitejs/plugin-vue'
import { defineConfig, type UserConfig } from 'vite'

export const createSrmViteConfig = (port: number, base = '/'): UserConfig => {
  const proxy = {
    '/api': { target: process.env.SRM_DEV_BACKEND_URL ?? 'http://localhost:8080', changeOrigin: true },
  }
  return defineConfig({
    base,
    plugins: [vue()],
    server: { port, strictPort: true, proxy },
    preview: { port, strictPort: true, proxy },
    build: { sourcemap: false, target: 'es2022' },
  })
}
