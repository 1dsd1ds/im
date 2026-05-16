import { defineStore } from 'pinia'
import { ref, computed, nextTick } from 'vue'
import { messageApi } from '../api/message'
import { wsClient } from '../utils/websocket'
import { useAuthStore } from './auth'
import { ElMessage, ElNotification } from 'element-plus'

export const useChatStore = defineStore('chat', () => {
  const conversations = ref([])
  const currentChat = ref(null)
  const currentMessages = ref([])
  const wsConnected = ref(false)
  const historyPage = ref(0)
  const hasMoreHistory = ref(true)
  const pendingMessages = ref([])   // 待确认消息队列
  const pendingReads = ref(new Set())  // 提前到达的已读回执 msgId
  const pendingRequestCount = ref(0)   // 待处理好友申请数

  const unreadTotal = computed(() =>
    conversations.value.reduce((s, c) => s + c.unreadCount, 0)
  )

  // 拉取会话列表
  async function loadConversations() {
    try {
      const res = await messageApi.getConversations()
      if (res.success) {
        conversations.value = res.data || []
      }
    } catch (e) {
      console.error('加载会话列表失败', e)
    }
  }

  // 拉取离线消息
  async function pullOfflineMessages() {
    try {
      const res = await messageApi.getOffline()
      if (res.success && res.data?.length > 0) {
        ElMessage.success(`收到 ${res.data.length} 条离线消息`)
        await loadConversations()
      }
    } catch (e) {
      console.error('拉取离线消息失败', e)
    }
  }

  // 打开某个会话
  async function openConversation(contact) {
    currentChat.value = contact
    currentMessages.value = []
    historyPage.value = 0
    hasMoreHistory.value = true
    await loadHistory()

    if (contact.type !== 'group') {
      try {
        await messageApi.markAsRead(contact.contactId)
        const conv = conversations.value.find(c => c.contactId === contact.contactId && c.type !== 'group')
        if (conv) conv.unreadCount = 0
        // 对所有对方发来的消息发送已读回执，而非仅最后一条
        const authStore = useAuthStore()
        const otherMessages = currentMessages.value.filter(
          m => m.fromUserId !== 'me' && m.fromUserId !== authStore.userId
        )
        for (const msg of otherMessages) {
          wsClient.send('MSG_READ', { msgId: msg.id, readerId: authStore.userId })
        }
      } catch (e) {
        // ignore
      }
    }
  }

  // 加载历史消息
  async function loadHistory() {
    if (!currentChat.value || !hasMoreHistory.value) return
    try {
      const isGroup = currentChat.value.type === 'group'
      const res = isGroup
        ? await messageApi.getGroupHistory(currentChat.value.contactId, historyPage.value)
        : await messageApi.getHistory(currentChat.value.contactId, historyPage.value)
      if (res.success && res.data?.content) {
        const newMsgs = res.data.content.reverse()
        currentMessages.value = [...newMsgs, ...currentMessages.value]
        hasMoreHistory.value = !res.data.last
        historyPage.value++
      }
    } catch (e) {
      console.error('加载历史消息失败', e)
    }
  }

  // 上拉加载更多
  async function loadMoreHistory() {
    await loadHistory()
  }

  // 发送消息
  function sendMessage(content, msgType) {
    msgType = msgType || 'TEXT'
    if (!currentChat.value || !content.trim()) return
    if (!wsClient.connected) {
      ElMessage.warning('正在连接消息服务器，请稍后再试...')
      wsClient.connect()
      return
    }

    const tempId = 'temp_' + Date.now() + '_' + Math.random().toString(36).slice(2, 6)
    const msgData = currentChat.value.type === 'group'
      ? { toId: null, groupId: currentChat.value.contactId, content, msgType }
      : { toId: currentChat.value.contactId, content, msgType }

    // 乐观更新
    currentMessages.value.push({
      id: tempId,
      fromUserId: 'me',
      toUserId: currentChat.value.contactId,
      content,
      msgType,
      status: 'SENDING',
      createdAt: new Date().toISOString()
    })
    pendingMessages.value.push(tempId)

    // 5秒超时自动标记失败
    setTimeout(() => {
      const idx = pendingMessages.value.indexOf(tempId)
      if (idx !== -1) {
        pendingMessages.value.splice(idx, 1)
        const msg = currentMessages.value.find(m => m.id === tempId)
        if (msg && msg.status === 'SENDING') {
          msg.status = 'FAILED'
        }
      }
    }, 5000)

    // 更新对应会话的侧边栏预览
    const convKey = currentChat.value.type === 'group' ? 'groupId' : 'contactId'
    const conv = conversations.value.find(c =>
      c.type === currentChat.value.type && c[convKey] === currentChat.value.contactId
    )
    if (conv) {
      conv.lastMsgContent = content
      conv.lastMsgType = msgType
      conv.lastMsgTime = new Date().toISOString()
    }

    wsClient.send(currentChat.value.type === 'group' ? 'GROUP_MSG' : 'CHAT_MSG', msgData)
    nextTick(scrollToBottom)
  }

  // 处理收到的单聊消息
  function handleIncomingMessage(data) {
    const fromId = data.fromId
    if (currentChat.value && currentChat.value.contactId === fromId && currentChat.value.type !== 'group') {
      currentMessages.value.push({
        id: data.msgId,
        fromUserId: fromId,
        toUserId: data.toId,
        content: data.content,
        msgType: data.msgType,
        status: 'UNREAD',
        createdAt: new Date(data.timestamp).toISOString()
      })
      nextTick(scrollToBottom)
      messageApi.markAsRead(fromId).catch(() => {})
      console.log('[INCOMING] 发送已读回执:', data.msgId, 'readerId:', data.toId)
      wsClient.send('MSG_READ', { msgId: data.msgId, readerId: data.toId })
    } else {
      const conv = conversations.value.find(c => c.contactId === fromId && c.type !== 'group')
      if (conv) {
        conv.unreadCount = (conv.unreadCount || 0) + 1
        conv.lastMsgContent = data.content
        conv.lastMsgType = data.msgType
        conv.lastMsgTime = new Date(data.timestamp).toISOString()
      } else {
        loadConversations()
      }
    }
  }

  // 处理收到的群聊消息
  function handleGroupMessage(data) {
    const groupId = data.groupId
    const fromId = data.fromId
    const authStore = useAuthStore()

    // 忽略自己发的消息
    if (fromId === authStore.userId) return

    if (currentChat.value && currentChat.value.type === 'group' && currentChat.value.contactId === groupId) {
      currentMessages.value.push({
        id: data.msgId,
        fromUserId: fromId,
        content: data.content,
        msgType: data.msgType,
        status: 'UNREAD',
        createdAt: new Date(data.timestamp).toISOString()
      })
      nextTick(scrollToBottom)
    }
    // 更新群会话
    const conv = conversations.value.find(c => c.type === 'group' && c.groupId === groupId)
    if (conv) {
      conv.lastMsgContent = data.content
      conv.lastMsgType = data.msgType
      conv.lastMsgTime = new Date(data.timestamp).toISOString()
    } else {
      loadConversations()
    }
  }

  // 处理消息回执（匹配最早一条 SENDING 消息，避免 shift() 竞态）
  function handleAck(data) {
    const idx = currentMessages.value.findIndex(m => m.status === 'SENDING')
    if (idx !== -1) {
      const msg = currentMessages.value[idx]
      // 清理 pendingMessages 中对应的 tempId
      const pendingIdx = pendingMessages.value.indexOf(msg.id)
      if (pendingIdx !== -1) pendingMessages.value.splice(pendingIdx, 1)

      msg.id = data.msgId
      msg.status = pendingReads.value.has(data.msgId) ? 'READ' : 'SENT'
      if (data.status === 'READ') msg.status = 'READ'
      pendingReads.value.delete(data.msgId)
      console.log('[ACK] 消息已确认:', data.msgId, 'status:', msg.status)
    } else {
      console.warn('[ACK] 未找到 SENDING 状态的消息:', data)
    }
  }

  // 处理好友上/下线通知
  function handleUserOnline(data) {
    const userId = data.userId
    const conv = conversations.value.find(c => c.type === 'user' && c.contactId === userId)
    if (conv) conv.contactStatus = 'ONLINE'
    if (currentChat.value && currentChat.value.type === 'user' && currentChat.value.contactId === userId) {
      currentChat.value.contactStatus = 'ONLINE'
    }
  }

  function handleUserOffline(data) {
    const userId = data.userId
    const conv = conversations.value.find(c => c.type === 'user' && c.contactId === userId)
    if (conv) conv.contactStatus = 'OFFLINE'
    if (currentChat.value && currentChat.value.type === 'user' && currentChat.value.contactId === userId) {
      currentChat.value.contactStatus = 'OFFLINE'
    }
  }

  // 处理好友申请通知
  function handleFriendRequest(data) {
    pendingRequestCount.value++
    ElNotification({
      title: '新的好友申请',
      message: `${data.nickname || '未知用户'} 申请添加你为好友`,
      type: 'info',
      duration: 5000,
      onClick: () => {
        window.location.href = '/contacts'
      }
    })
  }

  // 处理已读回执
  function handleReadReceipt(data) {
    console.log('[MSG_READ] 收到已读回执:', data.msgId, '当前消息数:', currentMessages.value.length)
    const msg = currentMessages.value.find(m => m.id === data.msgId)
    if (msg) {
      console.log('[MSG_READ] 找到消息，更新为已读:', data.msgId)
      msg.status = 'READ'
    } else {
      console.log('[MSG_READ] 消息未找到，暂存等待ACK:', data.msgId)
      pendingReads.value.add(data.msgId)
    }
  }

  function scrollToBottom() {
    // 由 ChatWindow 组件实现
  }

  // 轮询检查已发送消息的已读状态
  let pollTimer = null
  async function checkReadStatus() {
    const sentMsgs = currentMessages.value.filter(m => m.status === 'SENT')
    if (sentMsgs.length === 0) return
    try {
      const ids = sentMsgs.map(m => m.id).filter(id => !id.startsWith('temp_'))
      if (ids.length === 0) return
      const res = await messageApi.getMessageStatuses(ids)
      if (res.success && res.data) {
        for (const msg of sentMsgs) {
          if (res.data[msg.id] === 'READ') {
            msg.status = 'READ'
            console.log('[POLL] 轮询发现消息已读:', msg.id)
          }
        }
      }
    } catch (e) {
      // 忽略轮询错误
    }
  }

  function startPolling() {
    stopPolling()
    pollTimer = setInterval(checkReadStatus, 5000)
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  function reset() {
    conversations.value = []
    currentChat.value = null
    currentMessages.value = []
    historyPage.value = 0
    hasMoreHistory.value = true
    pendingMessages.value = []
    pendingReads.value = new Set()
    pendingRequestCount.value = 0
    stopPolling()
  }

  return {
    conversations, currentChat, currentMessages, wsConnected,
    unreadTotal, historyPage, hasMoreHistory,
    loadConversations, pullOfflineMessages, openConversation,
    loadHistory, loadMoreHistory, sendMessage,
    handleIncomingMessage, handleGroupMessage, handleAck, handleReadReceipt,
    handleUserOnline, handleUserOffline, handleFriendRequest, reset,
    startPolling, stopPolling,
    pendingRequestCount
  }
})
