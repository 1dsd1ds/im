import request from '../utils/request'

export const authApi = {
  login(data) { return request.post('/auth/login', data) },
  register(data) { return request.post('/auth/register', data) },
  getMe() { return request.get('/auth/me') },
  validate(token) { return request.post('/auth/validate', { token }) },
  logout() { return request.post('/auth/logout') },
  changePassword(data) { return request.post('/auth/change-password', data) }
}
