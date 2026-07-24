import { createApp } from 'vue'
import '@srm/config/tokens.css'
import './styles/app.css'
import App from './App.vue'
import router from './router'

createApp(App).use(router).mount('#app')
