import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import '@/styles/tokens.css'
import '@/styles/animations.css'
import '@/styles/global.css'

// 应用已保存的主题（深色模式），需在渲染前设置
if (localStorage.getItem('theme') === 'dark') {
  document.documentElement.dataset.theme = 'dark'
}

const app = createApp(App)
app.use(router)
app.use(ElementPlus)
app.mount('#app')