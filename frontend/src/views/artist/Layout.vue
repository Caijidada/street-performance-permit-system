<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">
        <el-icon class="brand-icon"><Location /></el-icon>
        <span class="brand-name">演出报备</span>
      </div>
      <nav class="nav">
        <router-link v-for="item in menu" :key="item.path" :to="item.path" class="nav-item" :class="{ active: $route.path === item.path }">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>
    </aside>

    <div class="main-wrap">
      <header class="topbar">
        <div class="page-title">{{ $route.meta.title }}</div>
        <div class="topbar-right">
          <el-tag v-if="userStore.userInfo?.qualifyStatus === 1" size="small" type="success">资质已认证</el-tag>
          <el-tag v-else size="small" type="warning">资质待认证</el-tag>

          <!-- 通知铃铛 -->
          <el-popover
            v-model:visible="noticeVisible"
            placement="bottom-end"
            :width="340"
            trigger="click"
            @show="openNotices"
          >
            <template #reference>
              <div class="notice-btn">
                <el-icon class="bell-icon"><Bell /></el-icon>
                <span v-if="unreadCount > 0" class="notice-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
              </div>
            </template>
            <div class="notice-panel">
              <div class="notice-head">
                <span class="notice-title">消息通知</span>
                <el-button size="small" text @click="readAll" v-if="unreadCount > 0">全部已读</el-button>
              </div>
              <div v-if="noticeLoading" class="notice-loading">
                <el-icon class="is-loading"><Loading /></el-icon>
              </div>
              <div v-else-if="notices.length === 0" class="notice-empty">暂无消息</div>
              <div v-else class="notice-list">
                <div
                  v-for="n in notices" :key="n.id"
                  class="notice-item"
                  :class="{ unread: n.isRead === 0 }"
                  @click="readNotice(n)"
                >
                  <div class="notice-item-title">
                    <span class="unread-dot" v-if="n.isRead === 0"></span>
                    {{ n.title }}
                  </div>
                  <div class="notice-item-content">{{ n.content }}</div>
                  <div class="notice-item-time">{{ n.createTime?.slice(0, 16) }}</div>
                </div>
              </div>
            </div>
          </el-popover>

          <!-- 用户信息 + 退出 -->
          <div class="user-info">
            <div class="avatar">{{ userStore.userInfo?.realName?.slice(0,1) }}</div>
            <div class="user-meta">
              <div class="user-name">{{ userStore.userInfo?.realName }}</div>
              <div class="credit-row">
                <el-icon style="font-size:11px;color:var(--warning)"><Star /></el-icon>
                <span class="credit-val">{{ userStore.userInfo?.creditScore ?? 100 }} 分</span>
              </div>
            </div>
          </div>
          <el-tooltip content="退出登录" placement="bottom">
            <el-icon class="logout-btn" @click="logout"><SwitchButton /></el-icon>
          </el-tooltip>
        </div>
      </header>
      <main class="content"><router-view /></main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { noticeApi } from '@/api/notice'

const router = useRouter()
const userStore = useUserStore()
const menu = [
  { path: '/artist/map',     icon: 'MapLocation', label: '地图选点报备' },
  { path: '/artist/reports', icon: 'Document', label: '我的报备' },
  { path: '/artist/credit',  icon: 'Medal',    label: '信用记录' },
  { path: '/artist/profile', icon: 'User',     label: '个人中心' },
]
function logout() { userStore.logout(); router.push('/login') }

// 通知
const unreadCount = ref(0)
const notices = ref([])
const noticeVisible = ref(false)
const noticeLoading = ref(false)

let _noticeTimer = null
onMounted(() => {
  fetchUnread()
  _noticeTimer = setInterval(fetchUnread, 30000)
})
onUnmounted(() => clearInterval(_noticeTimer))

async function fetchUnread() {
  try { unreadCount.value = await noticeApi.unreadCount() } catch {}
}

async function openNotices() {
  noticeVisible.value = true
  noticeLoading.value = true
  try {
    const res = await noticeApi.list(1, 20)
    notices.value = res.records || []
  } finally {
    noticeLoading.value = false
  }
}

async function readNotice(n) {
  if (n.isRead === 0) {
    await noticeApi.markRead(n.id)
    n.isRead = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }
}

async function readAll() {
  await noticeApi.readAll()
  notices.value.forEach(n => n.isRead = 1)
  unreadCount.value = 0
}
</script>

<style scoped>
.shell { display:flex;height:100vh;background:var(--bg-base);overflow:hidden; }
.sidebar { width:200px;flex-shrink:0;background:var(--bg-sidebar);border-right:1px solid var(--border);display:flex;flex-direction:column;box-shadow:2px 0 8px rgba(0,0,0,0.04); }
.brand { display:flex;align-items:center;gap:10px;padding:20px 16px 16px;border-bottom:1px solid var(--border); }
.brand-icon { font-size:18px;color:var(--accent); }
.brand-name { font-size:14px;font-weight:700;color:var(--text-primary); }
.nav { flex:1;padding:12px 8px;display:flex;flex-direction:column;gap:2px; }
.nav-item { display:flex;align-items:center;gap:10px;padding:0 12px;height:38px;border-radius:var(--radius-sm);color:var(--text-secondary);font-size:13px;font-weight:500;text-decoration:none;transition:all 0.15s; }
.nav-item:hover { background:var(--bg-hover);color:var(--text-primary); }
.nav-item.active { background:var(--accent-light);color:var(--accent);font-weight:600; }
.nav-item .el-icon { font-size:15px; }

.main-wrap { flex:1;display:flex;flex-direction:column;overflow:hidden; }
.topbar { height:56px;flex-shrink:0;background:var(--bg-header);border-bottom:1px solid var(--border);display:flex;align-items:center;justify-content:space-between;padding:0 20px;box-shadow:0 1px 4px rgba(0,0,0,0.04); }
.page-title { font-size:15px;font-weight:600;color:var(--text-primary); }
.topbar-right { display:flex;align-items:center;gap:12px; }

.user-info { display:flex;align-items:center;gap:8px; border-left:1px solid var(--border); padding-left:12px; }
.avatar { width:30px;height:30px;border-radius:50%;background:var(--accent-light);border:1px solid var(--accent);color:var(--accent);font-size:12px;font-weight:700;display:flex;align-items:center;justify-content:center;flex-shrink:0; }
.user-name { font-size:12px;font-weight:600;color:var(--text-primary);line-height:1.3; }
.credit-row { display:flex;align-items:center;gap:3px; }
.credit-val { font-size:11px;color:var(--warning); }
.logout-btn { font-size:18px;color:var(--text-muted);cursor:pointer;transition:color 0.15s; }
.logout-btn:hover { color:var(--danger); }

.content { flex:1;overflow-y:auto;padding:20px 24px;background:var(--bg-base); }

/* 通知铃铛 */
.notice-btn { position:relative;cursor:pointer;display:flex;align-items:center;justify-content:center;width:32px;height:32px;border-radius:50%;transition:background 0.15s; }
.notice-btn:hover { background:var(--bg-hover); }
.bell-icon { font-size:18px;color:var(--text-muted); }
.notice-badge { position:absolute;top:-2px;right:-4px;background:#f56c6c;color:#fff;font-size:10px;font-weight:700;min-width:16px;height:16px;border-radius:8px;display:flex;align-items:center;justify-content:center;padding:0 3px;line-height:1; }

/* 通知面板 */
.notice-panel { display:flex;flex-direction:column; }
.notice-head { display:flex;align-items:center;justify-content:space-between;padding:0 0 10px;border-bottom:1px solid var(--border); }
.notice-title { font-size:13px;font-weight:600;color:var(--text-primary); }
.notice-loading { display:flex;justify-content:center;padding:24px;color:var(--text-muted); }
.notice-empty { text-align:center;padding:28px;font-size:13px;color:var(--text-muted); }
.notice-list { max-height:360px;overflow-y:auto;margin:0 -16px;padding:0 16px; }
.notice-item { padding:10px 0;border-bottom:1px solid var(--border-light);cursor:pointer;transition:background 0.1s; }
.notice-item:last-child { border-bottom:none; }
.notice-item:hover { background:var(--bg-hover);margin:0 -16px;padding:10px 16px; }
.notice-item.unread { background:rgba(64,158,255,0.04); }
.notice-item-title { display:flex;align-items:center;gap:6px;font-size:13px;font-weight:600;color:var(--text-primary);margin-bottom:3px; }
.unread-dot { width:6px;height:6px;border-radius:50%;background:#409eff;flex-shrink:0; }
.notice-item-content { font-size:12px;color:var(--text-secondary);line-height:1.5;margin-bottom:4px; }
.notice-item-time { font-size:11px;color:var(--text-muted); }
</style>
