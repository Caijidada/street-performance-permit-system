import request from '@/utils/request'

export const statsApi = {
  overview: () => request.get('/admin/stats/overview'),
  heatmap: () => request.get('/admin/stats/heatmap'),
  venueRate: () => request.get('/admin/stats/venue-rate'),
  artistRank: () => request.get('/admin/stats/artist-rank'),
  performType: () => request.get('/admin/stats/perform-type')
}
