<template>
  <div class="reports-page">
    <div class="toolbar">
      <span class="total-tip">我的报备 · 共 {{ total }} 条</span>
      <el-button size="small" @click="loadData">刷新</el-button>
    </div>

    <el-table :data="list" v-loading="loading">
      <el-table-column label="报备单号" width="190">
        <template #default="{ row }"><span class="mono">{{ row.orderNo }}</span></template>
      </el-table-column>
      <el-table-column label="演出信息" width="200">
        <template #default="{ row }">
          <div class="cell-primary">{{ row.performDate }}</div>
          <div class="cell-sub">{{ row.timeSlotStart?.slice(0,5) }} ~ {{ row.timeSlotEnd?.slice(0,5) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag type="info" size="small">{{ row.performType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="REPORT_STATUS[row.status]?.type">{{ REPORT_STATUS[row.status]?.label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="提交时间" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click="viewCert(row)" v-if="row.status === 2">查看证件</el-button>
          <el-popconfirm title="确认撤销该报备？" @confirm="cancelReport(row.id)" v-if="row.status < 2">
            <template #reference>
              <el-button size="small" type="danger" plain>撤销</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-model:current-page="page" :page-size="10" :total="total"
      layout="total, prev, pager, next" class="pagination"
      @current-change="loadData" />

    <!-- 电子准演证弹窗 -->
    <el-dialog v-model="certVisible" title="电子准演证" width="380px" align-center>
      <div v-if="certData" class="cert-card">
        <div class="cert-row"><label>证件编号</label><span class="mono">{{ certData.certCode }}</span></div>
        <div class="cert-row"><label>演出日期</label><span>{{ certData.performDate }}</span></div>
        <div class="cert-row"><label>演出时段</label><span>{{ certData.timeSlotStart }} ~ {{ certData.timeSlotEnd }}</span></div>
        <img v-if="certData.certUrl" :src="certData.certUrl" alt="二维码" class="cert-qr"
          ref="certQrImg"
          crossorigin="anonymous"
          @error="e => e.target.style.display='none'" />
        <el-tag type="success" style="margin-top:12px;display:block;text-align:center">证件有效</el-tag>
        <el-button
          v-if="certData.certUrl"
          type="primary" plain size="small"
          style="margin-top:10px;width:100%"
          @click="downloadCert"
        >
          <el-icon style="margin-right:4px"><Download /></el-icon>保存证件图片
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { reportApi } from '@/api/report'
import { REPORT_STATUS } from '@/utils/constants'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const certVisible = ref(false)
const certData = ref(null)
const certQrImg = ref(null)

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const res = await reportApi.myList(page.value, 10)
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function cancelReport(id) {
  await reportApi.cancel(id)
  ElMessage.success('已撤销')
  loadData()
}

async function viewCert(row) {
  const cert = await request.get(`/certificate/my/${row.id}`)
  certData.value = cert
  certVisible.value = true
}

function downloadCert() {
  const img = certQrImg.value
  if (!img || !img.complete) { ElMessage.warning('图片尚未加载完成'); return }
  const canvas = document.createElement('canvas')
  canvas.width = img.naturalWidth || img.width
  canvas.height = img.naturalHeight || img.height
  const ctx = canvas.getContext('2d')
  ctx.drawImage(img, 0, 0)
  canvas.toBlob(blob => {
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `准演证_${certData.value.certCode}.png`
    a.click()
    URL.revokeObjectURL(url)
  }, 'image/png')
}
</script>

<style scoped>
.reports-page { display: flex; flex-direction: column; }
.toolbar { display: flex; align-items: center; justify-content: space-between; padding-bottom: 14px; }
.total-tip { font-size: 12px; color: var(--text-muted); }
.cell-primary { font-size: 13px; color: var(--text-primary); }
.cell-sub { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
.mono { font-family: monospace; font-size: 12px; color: var(--text-secondary); }
.pagination { margin-top: 14px; justify-content: flex-end; display: flex; }
.cert-card { display: flex; flex-direction: column; gap: 8px; }
.cert-row { display: flex; align-items: center; gap: 12px; font-size: 13px; }
.cert-row label { width: 70px; color: var(--text-muted); flex-shrink: 0; }
.cert-row span { color: var(--text-primary); }
.cert-qr { width: 180px; height: 180px; margin: 12px auto 0; display: block; border-radius: var(--radius-sm); }
</style>
