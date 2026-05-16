import request from '../utils/request'

export const groupApi = {
  create(data) { return request.post('/groups', data) },
  join(groupId) { return request.post(`/groups/${groupId}/join`) },
  leave(groupId) { return request.post(`/groups/${groupId}/leave`) },
  dismiss(groupId) { return request.post(`/groups/${groupId}/dismiss`) },
  getMyGroups() { return request.get('/groups') },
  getMembers(groupId) { return request.get(`/groups/${groupId}/members`) },
  kickMember(groupId, userId) { return request.delete(`/groups/${groupId}/members/${userId}`) }
}
