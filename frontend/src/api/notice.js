import request from '@/utils/request'

export const noticeApi = {
  list: (page = 1, size = 20) => request.get('/artist/notices', { params: { page, size } }),
  unreadCount: () => request.get('/artist/notices/unread-count'),
  markRead: (id) => request.post(`/artist/notices/${id}/read`),
  readAll: () => request.post('/artist/notices/read-all')
}
