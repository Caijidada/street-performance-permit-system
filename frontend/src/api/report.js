import request from '@/utils/request'

export const reportApi = {
  preCheck: (data) => request.post('/report/pre-check', data),
  submit: (data) => request.post('/report/submit', data),
  myList: (page = 1, size = 10) => request.get('/report/my-list', { params: { page, size } }),
  detail: (id) => request.get(`/report/${id}`),
  cancel: (id) => request.delete(`/report/${id}`),
  // 查询点位指定日期已占用时段
  getSlots: (venueId, date) => request.get('/report/slots', { params: { venueId, date } })
}
