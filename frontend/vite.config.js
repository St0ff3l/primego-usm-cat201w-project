import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const legacyTarget = env.VITE_LEGACY_API_TARGET || 'http://localhost:8081'

  return {
    plugins: [vue()],
    server: {
      host: '0.0.0.0',
      proxy: {
        '/assets/images/products': legacyTarget,
        '/legacy': {
          target: legacyTarget,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/legacy/, ''),
        },
      },
    },
  }
})
