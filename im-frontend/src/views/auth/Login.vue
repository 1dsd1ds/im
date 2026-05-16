<template>
  <div class="auth-container">
    <el-card class="auth-card">
      <h2 class="auth-title">登录即时通讯</h2>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码"
            :prefix-icon="Lock" @keyup.enter="handleLogin" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" block size="large" style="width: 100%" @click="handleLogin">
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="auth-footer">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await authStore.login({ username: form.username, password: form.password })
    if (res.success) {
      ElMessage.success('登录成功')
      router.push('/chat')
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {
    ElMessage.error('登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: var(--color-page);
}
.auth-card {
  width: 400px;
  padding: var(--space-3xl);
}
.auth-title {
  text-align: center;
  margin-bottom: var(--space-2xl);
  font-family: var(--font-display);
  font-size: var(--text-title-size);
  font-weight: var(--text-title-weight);
  color: var(--color-text-primary);
}
.auth-footer {
  text-align: center;
  margin-top: var(--space-lg);
  font-size: var(--text-caption-size);
  color: var(--color-text-secondary);
}
.auth-footer a {
  color: var(--color-accent);
  text-decoration: none;
}
</style>
