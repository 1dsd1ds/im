<template>
  <el-dialog v-model="visible" title="添加好友" width="420px" :close-on-click-modal="false">
    <el-input v-model="keyword" placeholder="搜索用户名或昵称" :prefix-icon="Search" @keyup.enter="handleSearch" />
    <div v-if="searchResults.length > 0" class="search-results">
      <div v-for="user in searchResults" :key="user.id" class="user-item">
        <div class="user-info">
          <el-avatar :size="40" :src="user.avatarUrl">{{ user.nickname?.charAt(0) }}</el-avatar>
          <div>
            <div class="user-name">{{ user.nickname }}</div>
            <div class="user-status" :class="user.status === 'ONLINE' ? 'online' : ''">
              {{ user.status === 'ONLINE' ? '在线' : '离线' }}
            </div>
          </div>
        </div>
        <el-button type="primary" size="small" :loading="addingId === user.id" @click="addContact(user.id)">
          添加
        </el-button>
      </div>
    </div>
    <div v-else-if="searched" class="no-result">未找到用户</div>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { userApi } from '../../api/user'
import { contactApi } from '../../api/contact'
import { ElMessage } from 'element-plus'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const keyword = ref('')
const searched = ref(false)
const searchResults = ref([])
const addingId = ref(null)

async function handleSearch() {
  if (!keyword.value.trim()) return
  searched.value = true
  try {
    const res = await userApi.search(keyword.value.trim())
    if (res.success) {
      searchResults.value = res.data || []
    }
  } catch (e) {
    ElMessage.error('搜索失败')
  }
}

async function addContact(contactId) {
  addingId.value = contactId
  try {
    const res = await contactApi.add(contactId)
    ElMessage.info(res.message)
  } catch (e) {
    ElMessage.error('添加失败')
  } finally {
    addingId.value = null
  }
}
</script>

<style scoped>
.search-results {
  margin-top: var(--space-lg);
  max-height: 300px;
  overflow-y: auto;
}
.user-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--color-border-light);
}
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.user-name {
  font-weight: var(--text-title-weight);
  color: var(--color-text-primary);
}
.user-status {
  font-size: var(--text-caption-size);
  color: var(--color-text-secondary);
}
.user-status.online { color: var(--color-online); }
.no-result {
  text-align: center;
  padding: var(--space-2xl);
  color: var(--color-text-secondary);
}
</style>
