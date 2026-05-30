<template>
  <div class="verify-page">
    <div class="verify-card">
      <div class="card-header">电子准演证核验</div>

      <div v-if="loading" class="state-box">
        <el-icon class="is-loading" size="40"><Loading /></el-icon>
        <p class="state-tip">核验中...</p>
      </div>

      <div v-else-if="cert" class="state-box">
        <div class="result-icon success">
          <el-icon size="40"><CircleCheckFilled /></el-icon>
        </div>
        <div class="result-title success">证件有效</div>
        <div class="info-list">
          <div class="info-row">
            <span class="info-label">证件编号</span>
            <span class="info-value mono">{{ cert.certCode }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">演出日期</span>
            <span class="info-value">{{ cert.performDate }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">演出时段</span>
            <span class="info-value">{{ cert.timeSlotStart?.slice(0,5) }} ~ {{ cert.timeSlotEnd?.slice(0,5) }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">签发时间</span>
            <span class="info-value">{{ cert.issueTime }}</span>
          </div>
        </div>
      </div>

      <div v-else-if="error" class="state-box">
        <div class="result-icon danger">
          <el-icon size="40"><CircleCloseFilled /></el-icon>
        </div>
        <div class="result-title danger">{{ error }}</div>
        <p class="state-tip">该证件不存在或已失效，请联系管理员</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { CircleCheckFilled, CircleCloseFilled, Loading } from '@element-plus/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const loading = ref(true)
const cert = ref(null)
const error = ref('')

onMounted(async () => {
  try {
    cert.value = await request.get(`/certificate/verify/${route.params.certCode}`)
  } catch (e) {
    error.value = '证件核验失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.verify-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f4f8;
  padding: 16px;
  box-sizing: border-box;
}
.verify-card {
  width: 100%;
  max-width: 480px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.10);
  overflow: hidden;
}
.card-header {
  text-align: center;
  font-size: 17px;
  font-weight: 700;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #ebeef5;
  color: #303133;
  letter-spacing: 0.5px;
}
.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 24px 36px;
  gap: 8px;
}
.result-icon { margin-bottom: 4px; }
.result-icon.success { color: #67c23a; }
.result-icon.danger  { color: #f56c6c; }
.result-title { font-size: 20px; font-weight: 700; }
.result-title.success { color: #67c23a; }
.result-title.danger  { color: #f56c6c; }
.state-tip { font-size: 13px; color: #909399; margin: 0; }
.info-list {
  width: 100%;
  margin-top: 16px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  overflow: hidden;
}
.info-row {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f2f4f7;
  gap: 12px;
}
.info-row:last-child { border-bottom: none; }
.info-label { font-size: 13px; color: #909399; width: 72px; flex-shrink: 0; }
.info-value { font-size: 13px; color: #303133; font-weight: 500; }
.mono { font-family: monospace; font-size: 12px; word-break: break-all; }
</style>
