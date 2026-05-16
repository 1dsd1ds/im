<template>
  <div class="profile-page">
    <div class="page-header">
      <el-button text :icon="ArrowLeft" @click="$router.push('/chat')">返回</el-button>
    </div>

    <div class="profile-card" v-loading="loading">
      <div class="avatar-section">
        <el-avatar :size="80" :src="form.avatarUrl" class="avatar">{{ form.nickname?.charAt(0) }}</el-avatar>
        <input ref="avatarInput" type="file" accept="image/*" style="display: none" @change="handleAvatarUpload" />
        <div class="avatar-actions">
          <el-button size="small" :loading="avatarUploading" @click="$refs.avatarInput.click()">上传头像</el-button>
          <el-button v-if="form.avatarUrl" size="small" @click="form.avatarUrl = ''">移除</el-button>
        </div>
      </div>

      <el-form :model="form" label-position="top" class="profile-form" @submit.prevent>
        <el-form-item label="用户名">
          <el-input :model-value="form.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" maxlength="50" />
        </el-form-item>
        <el-form-item label="状态">
          <el-tag :type="form.status === 'ONLINE' ? 'success' : 'info'">
            {{ form.status === 'ONLINE' ? '在线' : '离线' }}
          </el-tag>
        </el-form-item>
        <el-form-item v-if="form.createdAt" label="注册时间">
          <span class="text-secondary">{{ formatDate(form.createdAt) }}</span>
        </el-form-item>
        <el-form-item v-if="form.lastOnlineAt" label="最近上线">
          <span class="text-secondary">{{ formatDate(form.lastOnlineAt) }}</span>
        </el-form-item>
        <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
      </el-form>

      <el-divider />

      <div class="password-section">
        <h4>修改密码</h4>
        <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-position="top" @submit.prevent>
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="pwdForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="pwdForm.newPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
          </el-form-item>
          <el-button type="warning" :loading="changingPwd" @click="handleChangePassword">修改密码</el-button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { userApi } from '../../api/user'
import { authApi } from '../../api/auth'
import request from '../../utils/request'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const saving = ref(false)
const avatarUploading = ref(false)
const avatarInput = ref(null)

const form = reactive({
  username: '',
  nickname: '',
  avatarUrl: '',
  status: 'OFFLINE',
  createdAt: null,
  lastOnlineAt: null
})

async function loadProfile() {
  loading.value = true
  try {
    const res = await userApi.getMe()
    if (res.success) {
      Object.assign(form, res.data)
      authStore.nickname = res.data.nickname
      authStore.avatarUrl = res.data.avatarUrl || ''
    }
  } finally {
    loading.value = false
  }
}

async function handleAvatarUpload(e) {
  const file = e.target.files?.[0]
  if (!file) return

  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过10MB')
    return
  }

  avatarUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await request.post('/upload', formData)
    if (res.success) {
      form.avatarUrl = res.data.url
      ElMessage.success('头像上传成功')
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch {
    ElMessage.error('上传失败')
  } finally {
    avatarUploading.value = false
    if (avatarInput.value) avatarInput.value.value = ''
  }
}

async function handleSave() {
  saving.value = true
  try {
    const res = await userApi.updateProfile({
      nickname: form.nickname,
      avatarUrl: form.avatarUrl
    })
    if (res.success) {
      ElMessage.success('保存成功')
      authStore.nickname = form.nickname
      authStore.avatarUrl = form.avatarUrl
      localStorage.setItem('nickname', form.nickname)
      localStorage.setItem('avatarUrl', form.avatarUrl || '')
    } else {
      ElMessage.error(res.message)
    }
  } catch {
    // handled by interceptor
  } finally {
    saving.value = false
  }
}

// 修改密码
const pwdFormRef = ref(null)
const changingPwd = ref(false)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const validateConfirmPwd = (rule, value, callback) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPwd, trigger: 'blur' }
  ]
}
async function handleChangePassword() {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  changingPwd.value = true
  try {
    const res = await authApi.changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    if (res.success) {
      ElMessage.success('密码修改成功，请重新登录')
      pwdForm.oldPassword = ''
      pwdForm.newPassword = ''
      pwdForm.confirmPassword = ''
    } else {
      ElMessage.error(res.message)
    }
  } catch {
    ElMessage.error('修改失败')
  } finally {
    changingPwd.value = false
  }
}

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleString('zh-CN')
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: var(--color-page);
  padding: var(--space-xl);
  max-width: 560px;
  margin: 0 auto;
}
.page-header {
  margin-bottom: var(--space-2xl);
}
.profile-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: var(--space-3xl);
}
.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: var(--space-2xl);
}
.avatar { margin-bottom: var(--space-md); }
.avatar-actions { display: flex; gap: var(--space-sm); justify-content: center; }
.profile-form {
  max-width: 360px;
  margin: 0 auto;
}
.text-secondary {
  color: var(--color-text-secondary);
  font-size: var(--text-caption-size);
}
.password-section {
  max-width: 360px;
  margin: 0 auto;
}
.password-section h4 {
  font-size: 16px;
  font-weight: var(--text-title-weight);
  color: var(--color-text-primary);
  margin-bottom: var(--space-lg);
}
</style>
