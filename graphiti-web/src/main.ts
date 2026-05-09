import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import App from './App.vue'
import router from './router'
import './assets/styles/global.less'

console.log('[main.ts] 开始启动应用...')

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(Antd)

console.log('[main.ts] Vue 应用已创建，准备挂载...')

app.mount('#app')

console.log('[main.ts] 应用已挂载到 #app')
console.log('[main.ts] 当前环境:', import.meta.env.MODE)
console.log('[main.ts] 当前 URL:', window.location.href)
