<template>
  <div class="login-bg">
    <div class="login-panel">
      <div class="login-left">
        <div class="left-content">
          <el-icon class="logo-icon"><MapLocation /></el-icon>
          <h1 class="sys-title">街头演出<br>点位报备系统</h1>
          <p class="sys-desc">Street Performance Venue Registration Platform</p>
          <div class="features">
            <div class="feat" v-for="f in features" :key="f.text">
              <el-icon><component :is="f.icon" /></el-icon>
              <span>{{ f.text }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="login-right">
        <div class="form-wrap">
          <h2 class="form-title">欢迎回来</h2>
          <p class="form-sub">登录以继续操作</p>
          <el-form :model="form" @submit.prevent="handleLogin" style="margin-top:28px">
            <el-form-item>
              <el-input v-model="form.username" placeholder="用户名" size="large" :prefix-icon="User" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="form.password" type="password" placeholder="密码" size="large" :prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
            </el-form-item>
            <el-button type="primary" size="large" style="width:100%;margin-top:8px;height:42px;font-size:14px" :loading="loading" @click="handleLogin">
              登录
            </el-button>
          </el-form>
          <div class="divider"><span>还没有账号？</span></div>
          <el-button style="width:100%" size="large" @click="router.push('/register')">注册新账号</el-button>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = ref({ username: '', password: '' })
const features = [
  { icon: 'MapLocation', text: '地图可视化点位管理' },
  { icon: 'Calendar',    text: '智能冲突检测与调度' },
  { icon: 'Stamp',       text: '双级审批工作流' },
  { icon: 'Medal',       text: '艺人信用评分体系' },
]
async function handleLogin() {
  if (!form.value.username || !form.value.password) { ElMessage.warning('请填写用户名和密码'); return }
  loading.value = true
  try {
    await userStore.login(form.value)
    const role = userStore.userInfo?.role
    router.push(role >= 2 ? '/admin/dashboard' : '/artist/map')
  } finally {
    loading.value = false
  }
}
</script>
<style scoped>
.login-bg {
  min-height: 100vh;
  background: var(--bg-base);
  display: flex; align-items: center; justify-content: center;
  padding: 20px;
}
.login-panel {
  display: flex;
  width: 880px;
  max-width: 100%;
  min-height: 520px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border);
  overflow: hidden;
  box-shadow: var(--shadow);
}
.login-left {
  flex: 1;
  background: linear-gradient(135deg, #1e3a5f 0%, #2d6db5 100%);
  border-right: 1px solid var(--border);
  display: flex; align-items: center; justify-content: center;
  padding: 40px;
}
.left-content { max-width: 280px; }
.logo-icon { font-size: 40px; color: #7ec8ff; margin-bottom: 20px; }
.sys-title { font-size: 28px; font-weight: 700; color: #ffffff; line-height: 1.3; margin-bottom: 10px; }
.sys-desc { font-size: 12px; color: rgba(255,255,255,0.6); margin-bottom: 36px; letter-spacing: 0.5px; }
.features { display: flex; flex-direction: column; gap: 14px; }
.feat { display: flex; align-items: center; gap: 10px; color: rgba(255,255,255,0.85); font-size: 13px; }
.feat .el-icon { font-size: 15px; color: #7ec8ff; flex-shrink: 0; }
.login-right {
  width: 360px; flex-shrink: 0;
  background: var(--bg-card);
  display: flex; align-items: center; justify-content: center;
  padding: 48px 40px;
}
.form-wrap { width: 100%; }
.form-title { font-size: 22px; font-weight: 700; color: var(--text-primary); margin-bottom: 6px; }
.form-sub { font-size: 13px; color: var(--text-secondary); }
.divider { text-align: center; margin: 20px 0 16px; position: relative; }
.divider::before { content:''; position:absolute; top:50%; left:0; right:0; height:1px; background:var(--border); }
.divider span { background: var(--bg-card); padding: 0 12px; position: relative; font-size: 12px; color: var(--text-muted); }
</style>
