import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [
    vue(),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
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
        target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
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
        additionalData: `@import "${resolve(__dirname, 'src/assets/styles/dark.less').replace(/\\/g, '/')}";`
      }
    }
  }
})
