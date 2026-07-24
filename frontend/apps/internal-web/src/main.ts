import { createPinia } from 'pinia'
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import '@srm/config/tokens.css'
import './styles/app.css'
import App from './App.vue'
import { createInternalRouter, installRouterGuards } from './router'

const app = createApp(App)
const pinia = createPinia()
const router = createInternalRouter()

app.use(pinia)
installRouterGuards(router, pinia)
app.use(router)
app.use(ElementPlus)
app.mount('#app')
