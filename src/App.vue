<template>
  <div class="app-container">
    <Login v-if="!user" @login-success="handleLogin" />
    <Dashboard v-else :user="user" @logout="handleLogout" @update-user="handleUserUpdate" />
  </div>
</template>

<script setup>
import { ref, onMounted, defineAsyncComponent } from 'vue'
import Login from './components/Login.vue'
const Dashboard = defineAsyncComponent(() => import('./components/Dashboard.vue'))

const user = ref(null)

onMounted(() => {
  const savedUser = localStorage.getItem('user')
  if (savedUser) {
    user.value = JSON.parse(savedUser)
  } else {
    // 如果用户未登录，延迟预加载 Dashboard 组件
    // 利用用户输入账号密码的时间（假设3秒后）开始静默下载
    setTimeout(() => {
      import('./components/Dashboard.vue')
    }, 3000)
  }
})

const handleLogin = (userData) => {
  user.value = userData
  localStorage.setItem('user', JSON.stringify(userData))
}

const handleUserUpdate = (userData) => {
  user.value = userData
  localStorage.setItem('user', JSON.stringify(userData))
}

const handleLogout = () => {
  user.value = null
  localStorage.removeItem('user')
}
</script>

<style>
body {
  margin: 0;
  padding: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
  background-color: #f5f7fa;
}
</style>
