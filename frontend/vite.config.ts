import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// En développement, les appels /api sont relayés vers le backend Spring :
// le navigateur ne voit qu'une seule origine, donc pas de problème de CORS.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
