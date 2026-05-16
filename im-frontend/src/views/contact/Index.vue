<template>
  <div class="contact-page">
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="$router.push('/chat')">返回聊天</el-button>
      <h3>通讯录</h3>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="我的好友" name="friends">
        <div v-if="friends.length === 0" class="empty">暂无好友</div>
        <div v-for="friend in friends" :key="friend.id" class="contact-item"
          @click="startChat(friend)">
          <el-avatar :size="44" :src="friend.avatarUrl">{{ friend.nickname?.charAt(0) }}</el-avatar>
          <div class="contact-info">
            <div class="contact-name">{{ friend.nickname }}</div>
            <div class="contact-status" :class="friend.status === 'ONLINE' ? 'online' : ''">
              {{ friend.status === 'ONLINE' ? '在线' : '离线' }}
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="好友申请" name="pending">
        <div v-if="pendingRequests.length === 0" class="empty">暂无待处理的好友申请</div>
        <div v-for="req in pendingRequests" :key="req.id" class="contact-item">
          <el-avatar :size="44" :src="req.avatarUrl">{{ req.nickname?.charAt(0) }}</el-avatar>
          <div class="contact-info">
            <div class="contact-name">{{ req.nickname }} 申请添加好友</div>
          </div>
          <div class="request-actions">
            <el-button type="primary" size="small" @click="handleAccept(req.id)">同意</el-button>
            <el-button size="small" @click="handleReject(req.id)">拒绝</el-button>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useChatStore } from '../../stores/chat'
import { contactApi } from '../../api/contact'
import { ElMessage } from 'element-plus'

const router = useRouter()
const chatStore = useChatStore()
const activeTab = ref('friends')
const friends = ref([])
const pendingRequests = ref([])

async function loadFriends() {
  try {
    const res = await contactApi.getList()
    if (res.success) friends.value = res.data || []
  } catch (e) { /* ignore */ }
}

async function loadPending() {
  try {
    const res = await contactApi.getPending()
    if (res.success) pendingRequests.value = res.data || []
  } catch (e) { /* ignore */ }
}

async function handleAccept(requestId) {
  try {
    const res = await contactApi.accept(requestId)
    ElMessage.success(res.message)
    loadPending()
    loadFriends()
  } catch (e) { ElMessage.error('操作失败') }
}

async function handleReject(requestId) {
  try {
    const res = await contactApi.reject(requestId)
    ElMessage.info(res.message)
    loadPending()
  } catch (e) { ElMessage.error('操作失败') }
}

function startChat(friend) {
  chatStore.openConversation({
    contactId: friend.id,
    contactName: friend.nickname,
    contactAvatar: friend.avatarUrl,
    contactStatus: friend.status,
    type: 'user'
  })
  router.push('/chat')
}

onMounted(() => {
  loadFriends()
  loadPending()
})
</script>

<style scoped>
.contact-page {
  max-width: 600px;
  margin: 0 auto;
  background: var(--color-surface);
  min-height: 100vh;
  padding: var(--space-lg);
}
.page-header {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
  margin-bottom: var(--space-lg);
}
.page-header h3 {
  font-family: var(--font-display);
  font-size: var(--text-title-size);
  font-weight: var(--text-title-weight);
  color: var(--color-text-primary);
}
.empty {
  text-align: center;
  padding: var(--space-3xl) 0;
  color: var(--color-text-secondary);
}
.contact-item {
  display: flex;
  align-items: center;
  padding: var(--space-md) 0;
  border-bottom: 1px solid var(--color-border-light);
  gap: var(--space-md);
  cursor: pointer;
  transition: background var(--duration-fast) var(--ease-standard);
}
.contact-item:hover { background: var(--color-page); }
.contact-info {
  flex: 1;
}
.contact-name {
  font-weight: var(--text-title-weight);
  color: var(--color-text-primary);
}
.contact-status {
  font-size: var(--text-caption-size);
  color: var(--color-text-secondary);
}
.contact-status.online { color: var(--color-online); }
.request-actions {
  display: flex;
  gap: var(--space-sm);
}
</style>
