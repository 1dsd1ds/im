<template>
  <div class="msg-bubble" :class="{ self: isSelf }">
    <el-avatar v-if="!isSelf" :size="32" class="avatar">{{ msg.fromNickname?.charAt(0) || '?' }}</el-avatar>
    <div class="bubble-content">
      <div class="bubble" :class="{ self: isSelf }">
        <img v-if="msg.msgType === 'IMAGE'" :src="msg.content" class="msg-image" @click="previewImage(msg.content)" />
        <template v-else>{{ msg.content }}</template>
      </div>
      <div class="msg-meta" :class="{ self: isSelf }">
        <span class="msg-time">{{ formatTime(msg.createdAt) }}</span>
        <span v-if="isSelf" class="msg-status">
          <span v-if="msg.status === 'SENDING'" class="sending">发送中...</span>
          <span v-else-if="msg.status === 'FAILED'" class="failed">发送失败</span>
          <span v-else-if="msg.status === 'READ'" class="read">已读</span>
          <span v-else class="sent">已发送</span>
        </span>
      </div>
    </div>
    <el-avatar v-if="isSelf" :size="32" class="avatar" :src="authStore.avatarUrl">
      {{ authStore.nickname?.charAt(0) }}
    </el-avatar>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()
const props = defineProps({ msg: Object })

const isSelf = computed(() => {
  return props.msg.fromUserId === 'me' || props.msg.fromUserId === authStore.userId
})

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const msgDay = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  const timeStr = d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })

  if (msgDay.getTime() === today.getTime()) {
    return timeStr
  }
  const yesterday = new Date(today.getTime() - 86400000)
  if (msgDay.getTime() === yesterday.getTime()) {
    return `昨天 ${timeStr}`
  }
  if (d.getFullYear() === now.getFullYear()) {
    return `${d.getMonth() + 1}/${d.getDate()} ${timeStr}`
  }
  return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()} ${timeStr}`
}

function previewImage(url) {
  window.open(url, '_blank')
}
</script>

<style scoped>
.msg-bubble {
  display: flex;
  align-items: flex-start;
  gap: var(--space-sm);
  max-width: 70%;
}
.msg-bubble.self {
  margin-left: auto;
}
.bubble-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}
.bubble {
  padding: 10px 14px;
  border-radius: var(--radius-md);
  word-break: break-word;
  font-size: var(--text-body-size);
  line-height: var(--text-body-line);
  color: var(--color-text-primary);
}
.bubble:not(.self) {
  background: var(--color-surface);
}
.bubble.self {
  background: var(--color-bubble-self);
}
.msg-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 3px;
}
.msg-meta.self {
  justify-content: flex-end;
}
.msg-time {
  font-size: 11px;
  color: var(--color-text-tertiary);
}
.msg-status {
  font-size: var(--text-label-size);
  font-weight: var(--text-label-weight);
  letter-spacing: var(--text-label-spacing);
}
.msg-status .sending { color: var(--color-text-tertiary); }
.msg-status .sent { color: var(--color-text-secondary); }
.msg-status .read { color: var(--color-accent); }
.msg-status .failed { color: var(--color-error); cursor: pointer; }
.msg-image {
  max-width: 240px;
  max-height: 320px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  display: block;
}
.bubble:has(.msg-image) {
  padding: 4px;
  background: transparent;
}
</style>
