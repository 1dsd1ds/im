<template>
  <div class="chat-window">
    <!-- 顶部标题栏 -->
    <div class="chat-header">
      <span class="chat-title">
        <el-tag v-if="chatStore.currentChat?.type === 'group'" size="small" class="header-group-tag">群聊</el-tag>
        {{ chatStore.currentChat?.contactName || chatStore.currentChat?.name }}
      </span>
      <span v-if="chatStore.currentChat?.type === 'group'" class="status-text member-hint" style="cursor: pointer" @click="showMembers = true">
        {{ chatStore.currentChat?.contactStatus || '' }}
      </span>
      <span v-else class="status-text" :class="chatStore.currentChat?.contactStatus === 'ONLINE' ? 'online' : 'offline'">
        {{ chatStore.currentChat?.contactStatus === 'ONLINE' ? '在线' : '离线' }}
      </span>
    </div>

    <!-- 消息列表 -->
    <div class="message-area" ref="msgArea" @scroll="handleScroll">
      <div v-if="chatStore.hasMoreHistory" class="load-more" @click="chatStore.loadMoreHistory">
        加载更多消息
      </div>
      <div
        v-for="msg in chatStore.currentMessages"
        :key="msg.id"
        class="message-row"
        :class="{ self: msg.fromUserId === 'me' || msg.fromUserId === authStore.userId }"
      >
        <MessageBubble :msg="msg" />
      </div>
      <div ref="bottomRef" />
    </div>

    <!-- 输入区域 -->
    <ChatInput @send="handleSend" />

    <!-- 群成员弹窗 -->
    <el-dialog v-model="showMembers" :title="chatStore.currentChat?.contactName + ' - 群成员'" width="480px">
      <div v-loading="loadingMembers">
        <div v-if="members.length === 0 && !loadingMembers" class="empty-members">暂无成员数据</div>
        <div v-for="m in members" :key="m.userId" class="member-item">
          <div class="member-left">
            <el-avatar :size="36" :src="m.avatarUrl">{{ m.nickname?.charAt(0) }}</el-avatar>
            <div class="member-info">
              <span class="member-name">{{ m.nickname }}</span>
              <span class="member-status" :class="m.status === 'ONLINE' ? 'online' : ''">
                {{ m.status === 'ONLINE' ? '在线' : '离线' }}
              </span>
            </div>
          </div>
          <div class="member-right">
            <el-tag v-if="m.role === 'OWNER'" size="small" type="warning">群主</el-tag>
            <el-tag v-else size="small" type="info">成员</el-tag>
            <el-button
              v-if="isOwner && m.role !== 'OWNER'"
              size="small"
              type="danger"
              :loading="kickingId === m.userId"
              @click="handleKick(m)"
            >
              踢出
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, onUnmounted, computed } from 'vue'
import { useAuthStore } from '../../stores/auth'
import { useChatStore } from '../../stores/chat'
import { groupApi } from '../../api/group'
import MessageBubble from './MessageBubble.vue'
import ChatInput from './ChatInput.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const authStore = useAuthStore()
const chatStore = useChatStore()
const msgArea = ref(null)
const bottomRef = ref(null)

// 群成员
const showMembers = ref(false)
const members = ref([])
const loadingMembers = ref(false)
const kickingId = ref(null)
const isOwner = computed(() => {
  const g = chatStore.currentChat
  if (!g || g.type !== 'group') return false
  // ownerId is not always in currentChat, need to load from members
  return members.value.some(m => m.userId === authStore.userId && m.role === 'OWNER')
})

function scrollToBottom() {
  nextTick(() => {
    if (bottomRef.value) {
      bottomRef.value.scrollIntoView({ behavior: 'smooth' })
    }
  })
}

function handleScroll() {
  if (msgArea.value && msgArea.value.scrollTop === 0) {
    chatStore.loadMoreHistory()
  }
}

function handleSend(content, msgType) {
  chatStore.sendMessage(content, msgType || 'TEXT')
}

async function loadMembers() {
  if (!chatStore.currentChat || chatStore.currentChat.type !== 'group') return
  loadingMembers.value = true
  try {
    const res = await groupApi.getMembers(chatStore.currentChat.contactId)
    if (res.success) members.value = res.data || []
  } catch {
    // ignore
  } finally {
    loadingMembers.value = false
  }
}

async function handleKick(member) {
  try {
    await ElMessageBox.confirm(`确定要将 ${member.nickname} 踢出群聊吗？`, '确认踢人', {
      type: 'warning'
    })
  } catch {
    return
  }
  kickingId.value = member.userId
  try {
    const res = await groupApi.kickMember(chatStore.currentChat.contactId, member.userId)
    if (res.success) {
      ElMessage.success(`已将 ${member.nickname} 踢出群聊`)
      members.value = members.value.filter(m => m.userId !== member.userId)
      chatStore.currentChat.contactStatus = members.value.length + '人'
    } else {
      ElMessage.error(res.message)
    }
  } catch {
    ElMessage.error('操作失败')
  } finally {
    kickingId.value = null
  }
}

watch(() => chatStore.currentMessages.length, () => {
  scrollToBottom()
})

watch(showMembers, (val) => {
  if (val) loadMembers()
})

onMounted(() => {
  scrollToBottom()
  chatStore.startPolling()
})

onUnmounted(() => {
  chatStore.stopPolling()
})
</script>

<style scoped>
.chat-window {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100vh;
}
.chat-header {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: 14px var(--space-xl);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
}
.chat-title {
  font-family: var(--font-display);
  font-size: var(--text-title-size);
  font-weight: var(--text-title-weight);
  color: var(--color-text-primary);
}
.status-text {
  font-size: var(--text-caption-size);
  color: var(--color-text-secondary);
}
.member-hint:hover { color: var(--color-accent); }
.status-text.online { color: var(--color-online); }
.status-text.offline { color: var(--color-text-tertiary); }
.message-area {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-lg);
  background: var(--color-chat-area);
}
.load-more {
  text-align: center;
  padding: var(--space-sm);
  color: var(--color-accent);
  cursor: pointer;
  font-size: var(--text-caption-size);
  transition: color var(--duration-fast) var(--ease-standard);
}
.load-more:hover { color: var(--color-accent-hover); }
.message-row {
  margin-bottom: var(--space-lg);
  display: flex;
}
.message-row.self {
  justify-content: flex-end;
}
/* 群成员 */
.empty-members {
  text-align: center;
  padding: var(--space-2xl);
  color: var(--color-text-tertiary);
}
.member-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid var(--color-border-light);
}
.member-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.member-info {
  display: flex;
  flex-direction: column;
}
.member-name {
  font-weight: var(--text-title-weight);
  color: var(--color-text-primary);
}
.member-status {
  font-size: var(--text-caption-size);
  color: var(--color-text-secondary);
}
.member-status.online { color: var(--color-online); }
.member-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
