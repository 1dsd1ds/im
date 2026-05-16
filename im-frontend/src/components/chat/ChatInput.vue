<template>
  <div class="chat-input-area">
    <div class="toolbar">
      <input ref="fileInput" type="file" accept="image/*" style="display: none" @change="handleFileChange" />
      <el-icon class="tool-btn" :size="20" @click="$refs.fileInput.click()"><Picture /></el-icon>
    </div>
    <div v-if="uploadingImg" class="img-preview">
      <span class="uploading-text">图片上传中...</span>
    </div>
    <div class="input-wrapper">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="3"
        placeholder="输入消息，Enter 发送"
        resize="none"
        @keydown.enter.exact="handleSend"
      />
    </div>
    <div class="send-row">
      <span class="tip">Enter 发送</span>
      <el-button type="primary" size="small" @click="handleSend" :disabled="!inputText.trim()">
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Picture } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const emit = defineEmits(['send'])
const inputText = ref('')
const fileInput = ref(null)
const uploadingImg = ref(false)

async function handleFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return

  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过10MB')
    return
  }

  uploadingImg.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await request.post('/upload', formData)
    if (res.success) {
      emit('send', res.data.url, 'IMAGE')
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch {
    ElMessage.error('上传失败')
  } finally {
    uploadingImg.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}

function handleSend(e) {
  if (e && e.shiftKey) return
  if (e) e.preventDefault()
  const text = inputText.value.trim()
  if (!text) return
  emit('send', text)
  inputText.value = ''
}
</script>

<style scoped>
.chat-input-area {
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
  padding: var(--space-sm) var(--space-lg) var(--space-md);
}
.toolbar {
  padding: var(--space-xs) 0;
}
.tool-btn {
  cursor: pointer;
  color: var(--color-text-secondary);
  transition: color var(--duration-fast) var(--ease-standard);
}
.tool-btn:hover { color: var(--color-accent); }
.img-preview {
  padding: var(--space-xs) 0;
}
.uploading-text {
  font-size: var(--text-caption-size);
  color: var(--color-accent);
}
.input-wrapper {
  margin-bottom: var(--space-sm);
}
.send-row {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: var(--space-md);
}
.tip {
  font-size: var(--text-caption-size);
  color: var(--color-text-tertiary);
}
</style>
