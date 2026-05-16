<template>
  <div class="conversation-list">
    <div v-if="chatStore.conversations.length === 0" class="empty-list">
      <p>暂无会话</p>
      <p class="hint">添加好友或创建群聊后开始聊天</p>
    </div>
    <div
      v-for="conv in chatStore.conversations"
      :key="(conv.type === 'group' ? 'g-' : 'u-') + conv.contactId"
      class="conversation-item"
      :class="{ active: isActive(conv) }"
      @click="chatStore.openConversation(conv)"
    >
      <el-badge :value="conv.unreadCount" :hidden="conv.unreadCount === 0" :max="99">
        <el-avatar :size="44" :src="conv.contactAvatar">{{ conv.contactName?.charAt(0) }}</el-avatar>
      </el-badge>
      <div class="conv-info">
        <div class="conv-top">
          <span class="conv-name">
            <el-tag v-if="conv.type === 'group'" size="small" type="primary" class="group-tag">群</el-tag>
            {{ conv.contactName }}
          </span>
          <span class="conv-time">{{ formatTime(conv.lastMsgTime) }}</span>
        </div>
        <div class="conv-bottom">
          <span class="conv-last-msg">
            <template v-if="conv.lastMsgType === 'IMAGE'">[图片]</template>
            <template v-else>{{ conv.lastMsgContent || '暂无消息' }}</template>
          </span>
          <template v-if="conv.type === 'group'">
            <span class="member-count">{{ conv.contactStatus }}</span>
          </template>
          <template v-else>
            <span class="online-dot" :class="conv.contactStatus === 'ONLINE' ? 'online' : 'offline'" />
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useChatStore } from '../../stores/chat'

const chatStore = useChatStore()

function isActive(conv) {
  if (!chatStore.currentChat) return false
  return chatStore.currentChat.type === conv.type && chatStore.currentChat.contactId === conv.contactId
}

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}
</script>

<style scoped>
.conversation-list {
  flex: 1;
  overflow-y: auto;
}
.empty-list {
  text-align: center;
  padding: var(--space-3xl) 0;
  color: var(--color-text-secondary);
}
.hint {
  font-size: var(--text-caption-size);
  color: var(--color-text-tertiary);
}
.conversation-item {
  display: flex;
  align-items: center;
  padding: var(--space-md) var(--space-lg);
  gap: var(--space-md);
  cursor: pointer;
  border-bottom: 1px solid var(--color-border-light);
  border-left: 3px solid transparent;
  transition: background var(--duration-fast) var(--ease-standard);
}
.conversation-item:hover { background: var(--color-page); }
.conversation-item.active {
  background: var(--color-accent-bg);
  border-left: 3px solid var(--color-accent);
  border-radius: 0 3px 3px 0;
}
.conv-info {
  flex: 1;
  min-width: 0;
}
.conv-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.conv-name {
  font-weight: var(--text-title-weight);
  font-size: 15px;
  color: var(--color-text-primary);
}
.conv-time {
  font-size: var(--text-caption-size);
  color: var(--color-text-secondary);
}
.conv-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: var(--space-xs);
}
.conv-last-msg {
  font-size: var(--text-caption-size);
  color: var(--color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
}
.online-dot {
  width: 8px;
  height: 8px;
  border-radius: var(--radius-full);
  flex-shrink: 0;
}
.online-dot.online { background: var(--color-online); }
.online-dot.offline { background: var(--color-border); }
.group-tag {
  margin-right: 4px;
  vertical-align: middle;
}
.member-count {
  font-size: var(--text-caption-size);
  color: var(--color-text-tertiary);
}
</style>
