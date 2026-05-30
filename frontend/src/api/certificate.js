import request from '@/utils/request'

export const certificateApi = {
  verify: (certCode) => request.get(`/certificate/verify/${certCode}`),
  getUserById: (userId) => request.get(`/admin/user/${userId}`)
}
