import request from '@/utils/request'

export const venueApi = {
  getMapList: () => request.get('/venue/map-list'),
  getDetail: (id) => request.get(`/venue/${id}`),
  // 管理员接口
  adminList: () => request.get('/admin/venue/list'),
  add: (data) => request.post('/admin/venue', data),
  update: (id, data) => request.put(`/admin/venue/${id}`, data),
  remove: (id) => request.delete(`/admin/venue/${id}`)
}
