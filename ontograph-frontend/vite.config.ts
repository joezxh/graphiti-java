import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'url'

export default defineConfig({
  plugins: [
    vue(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    target: 'es2015',
    cssTarget: 'chrome61'
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:9090',
        changeOrigin: true
      }
    }
  },
  css: {
    preprocessorOptions: {
      less: {
        modifyVars: {
          'primary-color': '#5e6ad2',
          'component-background': '#0f1011',
          'border-radius-base': '8px'
        },
        javascriptEnabled: true,
        additionalData: `@import "${fileURLToPath(new URL('./src/assets/styles/dark.less', import.meta.url)).replace(/\\/g, '/')}";`
      }
    }
  }
})
