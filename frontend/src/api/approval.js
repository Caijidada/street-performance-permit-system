import request from '@/utils/request'

export const approvalApi = {
  list: (page = 1, size = 10, status = null) => request.get('/admin/approval/list', {
    params: { page, size, ...(status !== null && status !== undefined ? { status } : {}) }
  }),
  doAction: (orderId, data) => request.post(`/admin/approval/${orderId}/action`, data),
  adminCancel: (orderId, reason) => request.post(`/admin/approval/${orderId}/cancel`, { reason })
}
