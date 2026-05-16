<template>
  <div class="group-page">
    <div class="page-header">
      <el-button text :icon="ArrowLeft" @click="$router.push('/chat')">返回</el-button>
      <h3>我的群聊</h3>
      <div class="header-right">
        <el-button type="primary" :icon="Plus" @click="showCreate = true">创建群聊</el-button>
        <el-button :icon="Search" @click="showJoin = true">加入群聊</el-button>
      </div>
    </div>

    <div class="group-list" v-loading="loading">
      <div v-if="groups.length === 0" class="empty">暂无群聊，去创建一个吧</div>
      <div v-for="g in groups" :key="g.id" class="group-card" @click="openGroup(g)">
        <el-avatar :size="48" :src="g.avatarUrl">{{ g.name.charAt(0) }}</el-avatar>
        <div class="group-info">
          <div class="group-name">{{ g.name }}</div>
          <div class="group-meta">群号: {{ g.id }} · {{ g.memberCount }} 人 · 群主: {{ g.ownerName }}</div>
        </div>
        <el-tag v-if="g.ownerId === authStore.userId" size="small" type="info">群主</el-tag>
      </div>
    </div>

    <!-- 创建群聊弹窗 -->
    <el-dialog v-model="showCreate" title="创建群聊" width="480px" :close-on-click-modal="false">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-position="top">
        <el-form-item label="群名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入群名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="邀请好友（可选）">
          <el-select v-model="createForm.memberIds" multiple filterable placeholder="搜索并选择好友" style="width: 100%">
            <el-option v-for="f in friends" :key="f.id" :label="f.nickname || f.username" :value="f.id">
              <span>{{ f.nickname || f.username }}</span>
              <span :class="f.status === 'ONLINE' ? 'online-tag' : 'offline-tag'">{{ f.status === 'ONLINE' ? '在线' : '离线' }}</span>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 加入群聊弹窗 -->
    <el-dialog v-model="showJoin" title="加入群聊" width="400px">
      <el-form :model="joinForm" label-position="top">
        <el-form-item label="群ID">
          <el-input v-model="joinForm.groupId" placeholder="请输入群ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showJoin = false">取消</el-button>
        <el-button type="primary" :loading="joining" @click="handleJoin">加入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { useChatStore } from '../../stores/chat'
import { groupApi } from '../../api/group'
import { contactApi } from '../../api/contact'
import { ArrowLeft, Plus, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const chatStore = useChatStore()

const loading = ref(false)
const creating = ref(false)
const joining = ref(false)
const showCreate = ref(false)
const showJoin = ref(false)
const groups = ref([])
const friends = ref([])
const createFormRef = ref(null)

const createForm = reactive({ name: '', memberIds: [] })
const createRules = {
  name: [{ required: true, message: '请输入群名称', trigger: 'blur' }]
}
const joinForm = reactive({ groupId: '' })

async function loadGroups() {
  loading.value = true
  try {
    const res = await groupApi.getMyGroups()
    if (res.success) groups.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function loadFriends() {
  try {
    const res = await contactApi.getList()
    if (res.success) {
      friends.value = res.data || []
    }
  } catch {
    // ignore
  }
}

function openGroup(group) {
  chatStore.openConversation({ type: 'group', contactId: group.id, contactName: group.name, contactAvatar: group.avatarUrl, contactStatus: group.memberCount + '人' })
  router.push('/chat')
}

async function handleCreate() {
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  creating.value = true
  try {
    const res = await groupApi.create({ name: createForm.name, memberIds: createForm.memberIds })
    if (res.success) {
      ElMessage.success('群聊创建成功')
      showCreate.value = false
      createForm.name = ''
      createForm.memberIds = []
      await loadGroups()
    } else {
      ElMessage.error(res.message)
    }
  } catch {
    // handled by interceptor
  } finally {
    creating.value = false
  }
}

async function handleJoin() {
  if (!joinForm.groupId) {
    ElMessage.warning('请输入群ID')
    return
  }
  joining.value = true
  try {
    const res = await groupApi.join(Number(joinForm.groupId))
    if (res.success) {
      ElMessage.success('加入成功')
      showJoin.value = false
      joinForm.groupId = ''
      await loadGroups()
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    joining.value = false
  }
}

onMounted(() => {
  loadGroups()
  loadFriends()
})
</script>

<style scoped>
.group-page {
  min-height: 100vh;
  background: var(--color-page);
  padding: var(--space-xl);
  max-width: 720px;
  margin: 0 auto;
}
.page-header {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-2xl);
}
.page-header h3 {
  flex: 1;
  font-family: var(--font-display);
  font-size: var(--text-title-size);
  font-weight: var(--text-title-weight);
  color: var(--color-text-primary);
  margin: 0;
}
.header-right {
  display: flex;
  gap: var(--space-sm);
}
.group-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.group-card {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
  padding: var(--space-lg);
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: background var(--motion-base);
}
.group-card:hover {
  background: var(--color-chat-area);
}
.group-info {
  flex: 1;
}
.group-name {
  font-weight: var(--text-title-weight);
  color: var(--color-text-primary);
}
.group-meta {
  font-size: var(--text-caption-size);
  color: var(--color-text-tertiary);
  margin-top: 2px;
}
.empty {
  text-align: center;
  padding: var(--space-4xl);
  color: var(--color-text-tertiary);
}
.online-tag { color: var(--color-online); font-size: 12px; margin-left: 8px; }
.offline-tag { color: var(--color-text-tertiary); font-size: 12px; margin-left: 8px; }
</style>
