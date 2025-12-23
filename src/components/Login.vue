<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <span>个人学习状态助手</span>
        </div>
      </template>
      <el-form :model="form" label-width="0">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" :loading="loading" style="width: 100%">登录</el-button>
        </el-form-item>
        <el-form-item>
          <el-button @click="handleRegister" :loading="loading" style="width: 100%">注册</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'LoginComponent'
}
</script>

<script setup>
import { ref, defineEmits } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { API_BASE_URL } from '../config'

const emit = defineEmits(['login-success'])
const form = ref({
  username: '',
  password: ''
})
const loading = ref(false)
const API_URL = `${API_BASE_URL}/auth`

const handleLogin = async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await axios.post(`${API_URL}/login`, form.value)
    ElMessage.success('登录成功')
    emit('login-success', res.data)
  } catch (error) {
    ElMessage.error('登录失败: ' + (error.response?.data || error.message))
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    // eslint-disable-next-line
    const res = await axios.post(`${API_URL}/register`, form.value)
    ElMessage.success('注册成功，请登录')
  } catch (error) {
    ElMessage.error('注册失败: ' + (error.response?.data || error.message))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
}
.login-card {
  width: 400px;
}
.card-header {
  text-align: center;
  font-size: 20px;
  font-weight: bold;
}
</style>
