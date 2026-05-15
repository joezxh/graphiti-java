import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import App from './App.vue'
import router from './router'
import { i18n } from './i18n'
import './assets/styles/global.less'

console.log('[main.ts] 开始启动应用...')

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(Antd)
app.use(i18n)

console.log('[main.ts] Vue 应用已创建，准备挂载...')

app.mount('#app')

console.log('[main.ts] 应用已挂载到 #app')
console.log('[main.ts] 当前环境:', import.meta.env.MODE)
console.log('[main.ts] 当前 URL:', window.location.href)
console.log('[main.ts] 当前语言:', i18n.global.locale.value)
