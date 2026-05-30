import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  { path: '/', redirect: '/login' },

  { path: '/login', component: () => import('@/views/Login.vue'), meta: { title: '登录' } },
  { path: '/register', component: () => import('@/views/Register.vue'), meta: { title: '注册' } },
  { path: '/verify/:certCode', component: () => import('@/views/CertVerify.vue'), meta: { title: '证件核验' } },

  // 艺人端
  {
    path: '/artist',
    component: () => import('@/views/artist/Layout.vue'),
    meta: { requireAuth: true, role: 1 },
    children: [
      { path: '', redirect: '/artist/map' },
      { path: 'map', component: () => import('@/views/artist/MapReport.vue'), meta: { title: '地图报备' } },
      { path: 'reports', component: () => import('@/views/artist/MyReports.vue'), meta: { title: '我的报备' } },
      { path: 'credit', component: () => import('@/views/artist/MyCredit.vue'), meta: { title: '我的信用' } },
      { path: 'profile', component: () => import('@/views/artist/Profile.vue'), meta: { title: '个人资料' } }
    ]
  },

  // 管理端
  {
    path: '/admin',
    component: () => import('@/views/admin/Layout.vue'),
    meta: { requireAuth: true, role: 2 },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { title: '数据大屏' } },
      { path: 'venues', component: () => import('@/views/admin/VenueManage.vue'), meta: { title: '点位管理' } },
      { path: 'approvals', component: () => import('@/views/admin/ApprovalList.vue'), meta: { title: '报备审批' } },
      { path: 'users', component: () => import('@/views/admin/UserManage.vue'), meta: { title: '艺人管理' } },
      { path: 'credit', component: () => import('@/views/admin/CreditManage.vue'), meta: { title: '信用管理' } }
    ]
  },

  { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  document.title = (to.meta.title ? to.meta.title + ' - ' : '') + '街头演出点位报备系统'

  if (to.meta.requireAuth) {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) {
      return next('/login')
    }
    if (to.meta.role && userStore.userInfo?.role < to.meta.role) {
      return next('/login')
    }
  }
  next()
})

export default router
