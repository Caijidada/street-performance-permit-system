<template>
  <div class="profile-page">

    <!-- 顶部横幅 -->
    <div class="profile-banner">
      <div class="banner-avatar">{{ (user.realName || user.username || '?').slice(0, 1) }}</div>
      <div class="banner-info">
        <div class="banner-name">{{ user.realName || '—' }}</div>
        <div class="banner-sub">@{{ user.username }}</div>
      </div>
      <div class="banner-right">
        <div class="credit-ring" :class="creditClass">
          <span class="credit-num">{{ user.creditScore ?? 100 }}</span>
          <span class="credit-unit">信用分</span>
        </div>
        <el-tag v-if="user.qualifyStatus === 1" type="success" size="small" class="qualify-tag">资质已认证</el-tag>
        <el-tag v-else-if="user.qualifyStatus === 0" type="warning" size="small" class="qualify-tag">审核中</el-tag>
        <el-tag v-else type="danger" size="small" class="qualify-tag">未认证</el-tag>
      </div>
    </div>

    <div class="cards-row">
      <!-- 个人信息卡 -->
      <div class="card info-card">
        <div class="card-title">
          <el-icon><User /></el-icon>基本信息
        </div>

        <div class="field-list">
          <div class="field-row">
            <span class="field-label">用户名</span>
            <span class="field-val">{{ user.username }}</span>
          </div>
          <div class="field-row">
            <span class="field-label">真实姓名</span>
            <span class="field-val">{{ user.realName }}</span>
          </div>
          <div class="field-row">
            <span class="field-label">手机号</span>
            <span class="field-val">{{ user.phone }}</span>
          </div>
          <div class="field-row">
            <span class="field-label">演艺资质</span>
            <span class="field-val" style="display:flex;align-items:center;gap:8px;flex-wrap:wrap">
              <el-tag v-if="user.qualifyStatus === 1" type="success" size="small">已认证</el-tag>
              <el-tag v-else-if="user.qualifyStatus === 0" type="warning" size="small">审核中</el-tag>
              <el-tag v-else type="danger" size="small">{{ user.qualification ? '未通过，请重新上传' : '未上传' }}</el-tag>
              <el-link v-if="user.qualification" type="primary" :href="user.qualification" target="_blank" style="font-size:12px">查看文件</el-link>
            </span>
          </div>
        </div>

        <template v-if="user.qualifyStatus !== 1">
          <div class="divider" />
          <el-alert title="完成演艺资质认证后才能提交报备申请" type="info" show-icon :closable="false" style="margin-bottom:12px" />
          <div class="upload-label">上传演艺资质证明（JPG / PNG / PDF，不超过 10MB）</div>
          <el-upload action="#" :auto-upload="false" :on-change="handleFileChange"
            accept=".jpg,.jpeg,.png,.pdf" :limit="1" :file-list="fileList">
            <el-button size="small">选择文件</el-button>
          </el-upload>
          <el-button type="primary" size="small" style="margin-top:10px" :loading="uploading" @click="uploadQualification">
            提交资质审核
          </el-button>
        </template>
      </div>

      <!-- 修改密码卡 -->
      <div class="card pwd-card">
        <div class="card-title">
          <el-icon><Lock /></el-icon>修改密码
        </div>

        <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-position="top">
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少6位" />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
          </el-form-item>
          <el-button type="primary" style="width:100%" :loading="changingPwd" @click="handleChangePwd">修改密码</el-button>
        </el-form>

        <el-alert type="warning" show-icon :closable="false" title="修改密码后将自动退出，需重新登录" style="margin-top:16px" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const user = ref({})
const uploading = ref(false)
const changingPwd = ref(false)
const selectedFile = ref(null)
const fileList = ref([])
const pwdFormRef = ref()

const creditClass = computed(() => {
  const s = user.value.creditScore ?? 100
  return s >= 80 ? 'credit-good' : s >= 60 ? 'credit-warn' : 'credit-bad'
})

const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (value !== pwdForm.value.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

onMounted(async () => {
  await userStore.refreshUserInfo()
  user.value = { ...userStore.userInfo }
})

function handleFileChange(file) { selectedFile.value = file.raw }

async function uploadQualification() {
  if (!selectedFile.value) { ElMessage.warning('请先选择文件'); return }
  uploading.value = true
  const formData = new FormData()
  formData.append('file', selectedFile.value)
  try {
    await authApi.uploadQualification(formData)
    ElMessage.success('资质已提交，等待管理员审核')
    fileList.value = []
    selectedFile.value = null
    await userStore.refreshUserInfo()
    user.value = { ...userStore.userInfo }
  } finally {
    uploading.value = false
  }
}

async function handleChangePwd() {
  await pwdFormRef.value.validate()
  changingPwd.value = true
  try {
    await authApi.changePassword({
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    await userStore.logout()
    router.push('/login')
  } finally {
    changingPwd.value = false
  }
}
</script>

<style scoped>
.profile-page { display: flex; flex-direction: column; gap: 20px; }

/* 顶部横幅 */
.profile-banner {
  background: linear-gradient(135deg, #409eff 0%, #6abfff 100%);
  border-radius: 14px;
  padding: 28px 32px;
  display: flex;
  align-items: center;
  gap: 20px;
  color: #fff;
}
.banner-avatar {
  width: 64px; height: 64px;
  border-radius: 50%;
  background: rgba(255,255,255,0.25);
  display: flex; align-items: center; justify-content: center;
  font-size: 26px; font-weight: 700;
  flex-shrink: 0;
  border: 2px solid rgba(255,255,255,0.5);
}
.banner-info { flex: 1; }
.banner-name { font-size: 20px; font-weight: 700; line-height: 1.3; }
.banner-sub  { font-size: 13px; opacity: 0.8; margin-top: 2px; }
.banner-right { display: flex; flex-direction: column; align-items: flex-end; gap: 8px; }
.credit-ring {
  display: flex; flex-direction: column; align-items: center;
  background: rgba(255,255,255,0.2);
  border: 2px solid rgba(255,255,255,0.5);
  border-radius: 50%;
  width: 64px; height: 64px;
  justify-content: center;
}
.credit-num  { font-size: 18px; font-weight: 700; line-height: 1; }
.credit-unit { font-size: 10px; opacity: 0.85; margin-top: 1px; }
.credit-good { border-color: rgba(255,255,255,0.7); }
.credit-warn { border-color: rgba(230,162,60,0.8); }
.credit-bad  { border-color: rgba(245,108,108,0.8); }
.qualify-tag { border-color: rgba(255,255,255,0.6) !important; background: rgba(255,255,255,0.25) !important; color: #fff !important; font-weight: 600; }

/* 卡片行 */
.cards-row { display: grid; grid-template-columns: 3fr 2fr; gap: 20px; align-items: stretch; }

.card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  border: 1px solid #ebeef5;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.card-title {
  font-size: 14px; font-weight: 700; color: #303133;
  margin-bottom: 20px; padding-bottom: 14px;
  border-bottom: 1px solid #f0f2f5;
  display: flex; align-items: center; gap: 6px;
}

/* 信息字段 */
.field-list { display: flex; flex-direction: column; }
.field-row {
  display: flex; align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px solid #f5f7fa;
  gap: 16px;
}
.field-row:last-child { border-bottom: none; }
.field-label { width: 72px; flex-shrink: 0; font-size: 13px; color: #909399; padding-top: 1px; }
.field-val   { flex: 1; font-size: 14px; color: #303133; font-weight: 500; }

.divider { border-top: 1px solid #f0f2f5; margin: 16px 0; }
.upload-label { font-size: 12px; color: #909399; margin-bottom: 8px; }

/* 密码卡表单 */
.pwd-card :deep(.el-form-item__label) { font-size: 13px; color: #606266; font-weight: 500; padding-bottom: 4px; }
</style>
