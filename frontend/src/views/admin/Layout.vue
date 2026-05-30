<template>
  <div class="shell">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="brand">
        <el-icon class="brand-icon"><MapLocation /></el-icon>
        <span class="brand-name">演出报备管理台</span>
      </div>

      <nav class="nav">
        <router-link
          v-for="item in menu" :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: $route.path === item.path }"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

    </aside>

    <!-- 主体 -->
    <div class="main-wrap">
      <header class="topbar">
        <div class="page-title">{{ $route.meta.title }}</div>
        <div class="topbar-right">
          <el-tag size="small" type="success">系统运行中</el-tag>
          <el-tooltip content="扫码核验证件" placement="bottom">
            <el-button size="small" plain @click="openScan">
              <el-icon style="margin-right:4px"><Crop /></el-icon>扫码核验
            </el-button>
          </el-tooltip>
          <div class="user-info">
            <div class="avatar">{{ userStore.userInfo?.realName?.slice(0,1) }}</div>
            <div class="user-meta">
              <div class="user-name">{{ userStore.userInfo?.realName }}</div>
              <div class="user-role">管理员</div>
            </div>
          </div>
          <el-tooltip content="退出登录" placement="bottom">
            <el-icon class="logout-btn" @click="logout"><SwitchButton /></el-icon>
          </el-tooltip>
        </div>
      </header>
      <main class="content">
        <router-view />
      </main>
    </div>

    <!-- 扫码核验弹窗 -->
    <el-dialog v-model="scanVisible" title="扫码核验证件" width="440px" :close-on-click-modal="!scanning">
      <div class="scan-body">
        <div v-if="!scanResult" class="upload-area" @click="triggerFileSelect">
          <el-icon size="40" style="color:var(--text-muted)"><Picture /></el-icon>
          <p class="upload-tip">点击上传二维码图片</p>
          <p class="upload-sub">支持艺人准演证上的二维码截图</p>
          <el-button type="primary" plain size="small" :loading="scanning" style="margin-top:8px">
            {{ scanning ? '识别中...' : '选择图片' }}
          </el-button>
        </div>

        <div v-else class="cert-result">
          <div class="result-header">
            <el-icon style="color:#67c23a;font-size:22px"><CircleCheckFilled /></el-icon>
            <span class="result-ok">证件有效</span>
          </div>
          <div class="result-grid">
            <div class="rg-item">
              <span class="rg-label">艺人姓名</span>
              <span class="rg-val bold">{{ scanResult.user?.realName || '—' }}</span>
            </div>
            <div class="rg-item">
              <span class="rg-label">手机号码</span>
              <span class="rg-val">{{ scanResult.user?.phone || '—' }}</span>
            </div>
            <div class="rg-item">
              <span class="rg-label">当前信用</span>
              <span class="rg-val" :style="{ color: (scanResult.user?.creditScore ?? 100) >= 80 ? '#67c23a' : '#f56c6c', fontWeight:600 }">
                {{ scanResult.user?.creditScore ?? '—' }} 分
              </span>
            </div>
            <div class="rg-item">
              <span class="rg-label">演出日期</span>
              <span class="rg-val">{{ scanResult.cert?.performDate }}</span>
            </div>
            <div class="rg-item">
              <span class="rg-label">演出时段</span>
              <span class="rg-val">{{ scanResult.cert?.timeSlotStart?.slice(0,5) }} ~ {{ scanResult.cert?.timeSlotEnd?.slice(0,5) }}</span>
            </div>
            <div class="rg-item full">
              <span class="rg-label">证件编号</span>
              <span class="rg-val mono">{{ scanResult.cert?.certCode }}</span>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <template v-if="!scanResult">
          <el-button @click="scanVisible = false">取消</el-button>
        </template>
        <template v-else>
          <el-button @click="scanResult = null">重新扫描</el-button>
          <el-button type="primary" @click="goCredit">前往评分</el-button>
        </template>
      </template>
    </el-dialog>

    <!-- 隐藏文件选择 -->
    <input ref="fileInputRef" type="file" accept="image/*" style="display:none" @change="onFileChange" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import jsQR from 'jsqr'
import { certificateApi } from '@/api/certificate'

const router = useRouter()
const userStore = useUserStore()

const menu = [
  { path: '/admin/dashboard', icon: 'DataAnalysis', label: '数据大屏' },
  { path: '/admin/approvals', icon: 'Stamp',        label: '报备审批' },
  { path: '/admin/venues',    icon: 'MapLocation',  label: '点位管理' },
  { path: '/admin/users',     icon: 'UserFilled',   label: '艺人管理' },
  { path: '/admin/credit',    icon: 'Medal',        label: '信用管理' },
]

function logout() { userStore.logout(); router.push('/login') }

// 扫码核验
const scanVisible = ref(false)
const scanning = ref(false)
const scanResult = ref(null)   // { cert, user }
const fileInputRef = ref(null)

function openScan() {
  scanResult.value = null
  scanVisible.value = true
}

function triggerFileSelect() {
  fileInputRef.value?.click()
}

async function onFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  e.target.value = ''
  scanning.value = true
  scanResult.value = null
  try {
    // 读取图片 → canvas → jsQR 解码
    const url = URL.createObjectURL(file)
    const img = await loadImage(url)
    URL.revokeObjectURL(url)

    const canvas = document.createElement('canvas')
    canvas.width = img.width
    canvas.height = img.height
    const ctx = canvas.getContext('2d')
    ctx.drawImage(img, 0, 0)
    const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height)
    const code = jsQR(imageData.data, imageData.width, imageData.height)

    if (!code) { ElMessage.warning('未识别到二维码，请确认图片清晰'); return }

    // 从 URL 中提取 certCode（格式：.../verify/CERT-xxx）
    const match = code.data.match(/\/verify\/([A-Z0-9\-]+)$/)
    if (!match) { ElMessage.warning('二维码内容无效，不是本系统证件'); return }

    const certCode = match[1]
    const [cert, user] = await Promise.all([
      certificateApi.verify(certCode),
      null  // 先拿 cert 取 userId
    ])
    const userInfo = await certificateApi.getUserById(cert.userId)
    scanResult.value = { cert, user: userInfo }
  } catch (e) {
    ElMessage.error(e?.message || '核验失败，证件可能已失效')
  } finally {
    scanning.value = false
  }
}

function loadImage(src) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = reject
    img.src = src
  })
}

function goCredit() {
  const userId = scanResult.value?.cert?.userId
  scanVisible.value = false
  router.push({ path: '/admin/credit', query: { highlightUserId: userId } })
}
</script>

<style scoped>
.shell {
  display: flex;
  height: 100vh;
  background: var(--bg-base);
  overflow: hidden;
}

/* 侧边栏 */
.sidebar {
  width: 220px;
  flex-shrink: 0;
  background: var(--bg-sidebar);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  height: 100vh;
  box-shadow: 2px 0 8px rgba(0,0,0,0.04);
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 16px 16px;
  border-bottom: 1px solid var(--border);
}
.brand-icon { font-size: 20px; color: var(--accent); }
.brand-name { font-size: 15px; font-weight: 700; color: var(--text-primary); letter-spacing: 0.5px; }

.nav { flex: 1; padding: 12px 8px; display: flex; flex-direction: column; gap: 2px; }

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  height: 38px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.15s;
}
.nav-item:hover { background: var(--bg-hover); color: var(--text-primary); }
.nav-item.active { background: var(--accent-light); color: var(--accent); font-weight: 600; }
.nav-item .el-icon { font-size: 15px; }

.user-info { display: flex; align-items: center; gap: 8px; border-left: 1px solid var(--border); padding-left: 12px; }
.avatar {
  width: 30px; height: 30px;
  border-radius: 50%;
  background: var(--accent-light);
  border: 1px solid var(--accent);
  color: var(--accent);
  font-size: 12px;
  font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.user-name { font-size: 12px; font-weight: 600; color: var(--text-primary); line-height: 1.3; }
.user-role { font-size: 11px; color: var(--text-muted); }
.logout-btn { font-size: 18px; color: var(--text-muted); cursor: pointer; transition: color 0.15s; }
.logout-btn:hover { color: var(--danger); }

/* 主体 */
.main-wrap { flex: 1; display: flex; flex-direction: column; overflow: hidden; }

.topbar {
  height: 52px;
  flex-shrink: 0;
  background: var(--bg-header);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.page-title { font-size: 15px; font-weight: 600; color: var(--text-primary); }
.topbar-right { display: flex; align-items: center; gap: 12px; }

.content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  background: var(--bg-base);
}

/* 扫码弹窗 */
.scan-body { min-height: 200px; }
.upload-area {
  border: 2px dashed var(--border);
  border-radius: var(--radius);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  cursor: pointer;
  transition: border-color 0.15s;
  gap: 6px;
}
.upload-area:hover { border-color: var(--accent); }
.upload-tip { font-size: 14px; color: var(--text-primary); font-weight: 500; margin: 0; }
.upload-sub { font-size: 12px; color: var(--text-muted); margin: 0; }
.cert-result { display: flex; flex-direction: column; gap: 14px; }
.result-header { display: flex; align-items: center; gap: 8px; }
.result-ok { font-size: 16px; font-weight: 700; color: #67c23a; }
.result-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.rg-item { display: flex; flex-direction: column; gap: 2px; background: var(--bg-base); border-radius: var(--radius-sm); padding: 8px 12px; }
.rg-item.full { grid-column: 1 / -1; }
.rg-label { font-size: 11px; color: var(--text-muted); }
.rg-val { font-size: 13px; color: var(--text-primary); }
.rg-val.bold { font-weight: 600; }
.rg-val.mono { font-family: monospace; font-size: 12px; word-break: break-all; }
</style>
