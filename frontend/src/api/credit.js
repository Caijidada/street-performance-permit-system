import request from '@/utils/request'

export const creditApi = {
  getCredit: (userId) => request.get(`/artist/${userId}/credit`),
  myLogs: (page = 1, size = 10) => request.get('/artist/credit/my-logs', { params: { page, size } }),
  operate: (data) => request.post('/admin/credit/operate', data),
  adminLogs: (userId, page = 1, size = 10) => request.get('/admin/credit/logs', {
    params: { ...(userId != null ? { userId } : {}), page, size }
  })
}
