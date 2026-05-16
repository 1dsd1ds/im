import request from '../utils/request'

export const userApi = {
  search(keyword) { return request.get('/users/search', { params: { keyword } }) },
  getById(id) { return request.get(`/users/${id}`) },
  getMe() { return request.get('/auth/me') },
  updateProfile(data) { return request.put('/users/me', data) }
}
