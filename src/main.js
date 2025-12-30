import { createApp } from 'vue'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import axios from 'axios'

// Add Axios interceptor to inject token
axios.interceptors.request.use(config => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      if (user.token) {
        config.headers.Authorization = `Bearer ${user.token}`
      }
    } catch (e) {
      // ignore invalid json
    }
  }
  return config
}, error => {
  return Promise.reject(error)
})

// Handle 401/403 errors globally
axios.interceptors.response.use(response => {
  return response
}, error => {
  if (error.response && (error.response.status === 401 || error.response.status === 403)) {
    // Clear user and redirect to login
    localStorage.removeItem('user')
    window.location.reload()
  }
  return Promise.reject(error)
})

const app = createApp(App)
app.use(ElementPlus, {
  locale: zhCn,
})
app.mount('#app')
