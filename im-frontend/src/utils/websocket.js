import { useAuthStore } from '../stores/auth'
import { useChatStore } from '../stores/chat'
import { ElMessage } from 'element-plus'

const HEARTBEAT_INTERVAL = 30000
const RECONNECT_BASE = 1000
const RECONNECT_MAX = 30000

class WsClient {
  constructor() {
    this.ws = null
    this.reconnectAttempts = 0
    this.heartbeatTimer = null
    this.reconnectTimer = null
    this.connected = false
    this.manualClose = false
  }

  connect() {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) return

    const authStore = useAuthStore()
    if (!authStore.token) return

    const url = `ws://localhost:8090/ws?token=${authStore.token}`
    this.ws = new WebSocket(url)

    this.ws.onopen = () => {
      this.connected = true
      this.reconnectAttempts = 0
      this.startHeartbeat()

      const chatStore = useChatStore()
      chatStore.wsConnected = true

      // 拉取离线消息
      setTimeout(() => chatStore.pullOfflineMessages(), 500)
    }

    this.ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data)
        this.handleMessage(msg)
      } catch (e) {
        console.error('[WS] 消息解析失败', e)
      }
    }

    this.ws.onclose = (e) => {
      this.connected = false
      this.stopHeartbeat()

      const chatStore = useChatStore()
      chatStore.wsConnected = false

      if (!this.manualClose) {
        ElMessage.warning('消息服务连接断开，正在重连...')
        this.reconnect()
      }
    }

    this.ws.onerror = (e) => {
      console.error('[WS] 连接错误，请确认后端是否启动且 8090 端口可访问')
      if (this.reconnectAttempts === 0) {
        setTimeout(() => {
          ElMessage.error('无法连接到消息服务器(ws://localhost:8090)，请确认后端已启动')
        }, 300)
      }
    }
  }

  handleMessage(msg) {
    const chatStore = useChatStore()
    switch (msg.type) {
      case 'CHAT_MSG':
        chatStore.handleIncomingMessage(msg.data)
        break
      case 'GROUP_MSG':
        chatStore.handleGroupMessage(msg.data)
        break
      case 'CHAT_ACK':
      case 'GROUP_ACK':
        chatStore.handleAck(msg.data)
        break
      case 'MSG_READ':
        chatStore.handleReadReceipt(msg.data)
        break
      case 'USER_ONLINE':
        chatStore.handleUserOnline(msg.data)
        break
      case 'USER_OFFLINE':
        chatStore.handleUserOffline(msg.data)
        break
      case 'FRIEND_REQUEST':
        chatStore.handleFriendRequest(msg.data)
        break
      case 'HEARTBEAT_ACK':
        break
      case 'ERROR':
        console.error('[WS] 服务器错误:', msg.data)
        break
    }
  }

  send(type, data) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      const msg = { type, data, timestamp: Date.now() }
      this.ws.send(JSON.stringify(msg))
    } else {
      console.warn('[WS] 消息未发送(连接未就绪):', type, data)
    }
  }

  startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      this.send('HEARTBEAT', {})
    }, HEARTBEAT_INTERVAL)
  }

  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  reconnect() {
    const delay = Math.min(RECONNECT_BASE * Math.pow(2, this.reconnectAttempts), RECONNECT_MAX)
    this.reconnectAttempts++
    this.reconnectTimer = setTimeout(() => this.connect(), delay)
  }

  disconnect() {
    this.manualClose = true
    this.stopHeartbeat()
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
    }
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.connected = false
  }
}

export const wsClient = new WsClient()
