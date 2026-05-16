import request from '../utils/request'

export const messageApi = {
  getOffline() { return request.get('/messages/offline') },
  getHistory(contactId, page = 0, size = 20) {
    return request.get(`/messages/history/${contactId}`, { params: { page, size } })
  },
  getGroupHistory(groupId, page = 0, size = 20) {
    return request.get(`/messages/group/${groupId}`, { params: { page, size } })
  },
  getConversations() { return request.get('/messages/conversations') },
  markAsRead(contactId) { return request.post('/messages/read', { contactId }) },
  getUnreadCount() { return request.get('/messages/unread-count') },
  getMessageStatuses(ids) {
    return request.get('/messages/status', { params: { ids: ids.join(',') } })
  }
}
