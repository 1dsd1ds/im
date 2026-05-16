import request from '../utils/request'

export const contactApi = {
  getList() { return request.get('/contacts') },
  add(contactId) { return request.post('/contacts/add', { contactId }) },
  accept(requestId) { return request.post(`/contacts/accept/${requestId}`) },
  reject(requestId) { return request.post(`/contacts/reject/${requestId}`) },
  remove(contactId) { return request.delete(`/contacts/${contactId}`) },
  getPending() { return request.get('/contacts/pending') }
}
