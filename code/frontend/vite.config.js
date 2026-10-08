import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // ให้เรียก /api/... จาก frontend ได้ตรง ๆ แล้ว Vite dev server proxy ไปหา backend ให้เอง
      // แก้ปัญหา CORS ตอน dev โดยไม่ต้องเพิ่ม CORS config ฝั่ง Spring Boot เลย
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
