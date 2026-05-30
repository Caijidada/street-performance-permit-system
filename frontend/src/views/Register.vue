<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <h1>艺人注册</h1>
        <p>加入街头演出报备平台</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请设置登录用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请设置密码（至少6位）" show-password />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" placeholder="请输入身份证号" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" style="width:100%" @click="handleRegister">
            注册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        已有账号？
        <el-link type="primary" @click="$router.push('/login')">立即登录</el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = ref({ username: '', password: '', realName: '', idCard: '', phone: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }, { min: 3, max: 20, message: '长度3-20位', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  idCard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }, { len: 18, message: '身份证号为18位', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

async function handleRegister() {
  await formRef.value.validate()
  loading.value = true
  try {
    await authApi.register(form.value)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-base);
}
.login-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 36px 40px;
  width: 460px;
  box-shadow: var(--shadow);
}
.login-header {
  text-align: center;
  margin-bottom: 28px;
}
.login-header h1 { font-size: 20px; font-weight: 700; color: var(--text-primary); margin: 0 0 6px; }
.login-header p { color: var(--text-muted); font-size: 13px; margin: 0; }
.login-footer { text-align: center; margin-top: 16px; color: var(--text-muted); font-size: 13px; }
</style>
