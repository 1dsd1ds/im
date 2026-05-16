<template>
  <div class="chat-layout">
    <!-- 左侧面板 -->
    <div class="left-panel">
      <div class="panel-header">
        <el-dropdown trigger="click" @command="handleUserMenu">
          <div class="user-info">
            <el-avatar :size="36" :src="authStore.avatarUrl">{{ authStore.nickname?.charAt(0) }}</el-avatar>
            <span class="nickname">{{ authStore.nickname }}</span>
            <el-icon :size="14"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人信息</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <div class="header-actions">
          <el-tooltip :content="chatStore.wsConnected ? '已连接' : '未连接'" placement="bottom">
            <span class="conn-dot" :class="{ online: chatStore.wsConnected }" />
          </el-tooltip>
          <el-badge :value="unreadTotal" :hidden="unreadTotal === 0" :max="99">
            <el-icon :size="20"><ChatDotRound /></el-icon>
          </el-badge>
          <el-badge :value="chatStore.pendingRequestCount" :hidden="chatStore.pendingRequestCount === 0" :max="99">
            <el-icon :size="20" style="cursor: pointer" @click="$router.push('/contacts')"><Bell /></el-icon>
          </el-badge>
          <el-dropdown trigger="click" @command="handleMenu">
            <el-icon :size="20"><Plus /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="addContact">添加好友</el-dropdown-item>
                <el-dropdown-item command="groups">群聊</el-dropdown-item>
                <el-dropdown-item command="contacts">通讯录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
      <ConversationList />
    </div>

    <!-- 右侧聊天区域 -->
    <div class="right-panel">
      <ChatWindow v-if="chatStore.currentChat" />
      <div v-else class="empty-chat">
        <el-icon :size="80" color="#dcdfe6"><ChatLineSquare /></el-icon>
        <p>选择一个会话开始聊天</p>
      </div>
    </div>

    <!-- 添加好友弹窗 -->
    <AddContactDialog v-model="showAddDialog" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { useChatStore } from '../../stores/chat'
import { wsClient } from '../../utils/websocket'
import { ChatDotRound, Plus, ChatLineSquare, ArrowDown, Bell } from '@element-plus/icons-vue'
import ConversationList from '../../components/chat/ConversationList.vue'
import ChatWindow from '../../components/chat/ChatWindow.vue'
import AddContactDialog from '../../components/contact/AddContactDialog.vue'

const router = useRouter()
const authStore = useAuthStore()
const chatStore = useChatStore()
const showAddDialog = ref(false)

const unreadTotal = computed(() => chatStore.unreadTotal)

function handleMenu(command) {
  if (command === 'addContact') {
    showAddDialog.value = true
  } else if (command === 'groups') {
    router.push('/groups')
  } else if (command === 'contacts') {
    router.push('/contacts')
  }
}

async function handleUserMenu(command) {
  if (command === 'logout') {
    wsClient.disconnect()
    chatStore.reset()
    await authStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  }
}

onMounted(() => {
  if (!authStore.token) {
    router.push('/login')
    return
  }
  chatStore.reset()
  chatStore.loadConversations()
  wsClient.connect()
})
</script>

<style scoped>
.chat-layout {
  display: flex;
  height: 100vh;
  background: var(--color-page);
}
.left-panel {
  width: 320px;
  background: var(--color-surface);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-md) var(--space-lg);
  border-bottom: 1px solid var(--color-border-light);
}
.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  cursor: pointer;
}
.nickname {
  font-weight: var(--text-title-weight);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.header-actions {
  display: flex;
  gap: var(--space-md);
  align-items: center;
  cursor: pointer;
}
.conn-dot {
  width: 10px;
  height: 10px;
  border-radius: var(--radius-full);
  background: var(--color-error);
}
.conn-dot.online {
  background: var(--color-online);
}
.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.empty-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: var(--color-text-secondary);
  gap: var(--space-lg);
}
.empty-chat p {
  font-size: var(--text-body-size);
  color: var(--color-text-tertiary);
}
</style>
