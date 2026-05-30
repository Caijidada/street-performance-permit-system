import request from '@/utils/request'

export const authApi = {
  register: (data) => request.post('/auth/register', data),
  login: (data) => request.post('/auth/login', data),
  logout: () => request.post('/auth/logout'),
  getUserInfo: () => request.get('/auth/info'),
  uploadQualification: (formData) => request.post('/auth/qualification', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  changePassword: (data) => request.post('/auth/change-password', data)
}
