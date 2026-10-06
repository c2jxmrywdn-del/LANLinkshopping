import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import './styles/theme.css'
import App from './App.vue'
import router from './router'
import { initRipple } from './utils/ripple'
import { initI18nDom } from './i18n'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(Antd)
app.mount('#app')
initI18nDom()

// 全局按钮点击波纹（事件委托，任何 .ant-btn 生效）
initRipple()
