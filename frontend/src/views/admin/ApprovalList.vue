<template>
  <div class="approval-page">
    <!-- Tab 筛选栏 -->
    <div class="filter-bar">
      <div class="tab-group">
        <button
          v-for="tab in tabs" :key="tab.value"
          class="tab-btn"
          :class="{ active: filterStatus === tab.value }"
          @click="switchTab(tab.value)"
        >
          {{ tab.label }}
          <span v-if="tab.value === null" class="tab-badge total">{{ total }}</span>
        </button>
      </div>
      <div class="bar-right">
        <el-button size="small" @click="loadData">刷新</el-button>
      </div>
    </div>

    <el-table :data="list" v-loading="loading" row-key="order.id">
      <el-table-column label="报备单号" width="190">
        <template #default="{ row }">
          <span class="mono">{{ row.order.orderNo }}</span>
        </template>
      </el-table-column>
      <el-table-column label="艺人" width="140">
        <template #default="{ row }">
          <div class="cell-primary">{{ row.artistName }}</div>
          <div class="cell-sub">{{ row.artistPhone }}</div>
          <el-tag size="small" :type="row.creditScore >= 80 ? 'success' : row.creditScore >= 60 ? 'warning' : 'danger'">
            信用 {{ row.creditScore }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="点位" width="170">
        <template #default="{ row }">
          <div class="cell-primary">{{ row.venueName }}</div>
          <div class="cell-sub">{{ row.venueDistrict }}</div>
        </template>
      </el-table-column>
      <el-table-column label="演出日期" width="110">
        <template #default="{ row }">{{ row.order.performDate }}</template>
      </el-table-column>
      <el-table-column label="演出时段" width="140">
        <template #default="{ row }">{{ row.order.timeSlotStart?.slice(0,5) }} ~ {{ row.order.timeSlotEnd?.slice(0,5) }}</template>
      </el-table-column>
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag type="info" size="small">{{ row.order.performType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="演出内容" show-overflow-tooltip>
        <template #default="{ row }"><span class="cell-sub">{{ row.order.performContent }}</span></template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="REPORT_STATUS[row.order.status]?.type">{{ REPORT_STATUS[row.order.status]?.label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <template v-if="row.order.status === 0">
            <el-button size="small" type="success" @click="approve(row, 1)">一审通过</el-button>
            <el-button size="small" type="danger" plain @click="openReject(row, 2)">驳回</el-button>
          </template>
          <template v-else-if="row.order.status === 1">
            <el-button size="small" type="primary" @click="approve(row, 3)">终审通过</el-button>
            <el-button size="small" type="danger" plain @click="openReject(row, 4)">驳回</el-button>
          </template>
          <template v-else-if="row.order.status === 2">
            <el-popconfirm title="确认撤销该报备？需填写原因" @confirm="openCancel(row)">
              <template #reference>
                <el-button size="small" type="warning" plain>撤销</el-button>
              </template>
            </el-popconfirm>
          </template>
          <span v-else class="cell-sub">—</span>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total"
      layout="total, prev, pager, next" class="pagination"
      @current-change="loadData" />

    <!-- 驳回对话框 -->
    <el-dialog v-model="rejectVisible" title="填写驳回原因" width="420px">
      <el-input v-model="rejectComment" type="textarea" :rows="4" placeholder="请输入驳回原因（将通知艺人）" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="acting" @click="doReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { approvalApi } from '@/api/approval'
import { REPORT_STATUS } from '@/utils/constants'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const acting = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const filterStatus = ref(null)   // null = 全部
const rejectVisible = ref(false)
const rejectComment = ref('')
const currentRow = ref(null)
const currentAction = ref(null)

const tabs = [
  { label: '全部', value: null },
  { label: '待一审', value: 0 },
  { label: '待终审', value: 1 },
  { label: '已通过', value: 2 },
  { label: '已驳回', value: 3 },
  { label: '已撤销', value: 4 },
]

onMounted(loadData)

function switchTab(val) {
  filterStatus.value = val
  page.value = 1
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const res = await approvalApi.list(page.value, pageSize, filterStatus.value)
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function approve(row, action) {
  acting.value = true
  try {
    await approvalApi.doAction(row.order.id, { action, comment: '审批通过' })
    ElMessage.success('操作成功')
    loadData()
  } finally {
    acting.value = false
  }
}

function openReject(row, action) {
  currentRow.value = row
  currentAction.value = action
  rejectComment.value = ''
  rejectVisible.value = true
}

async function doReject() {
  if (!rejectComment.value.trim()) { ElMessage.warning('请填写驳回原因'); return }
  acting.value = true
  try {
    await approvalApi.doAction(currentRow.value.order.id, { action: currentAction.value, comment: rejectComment.value })
    ElMessage.success('已驳回')
    rejectVisible.value = false
    loadData()
  } finally {
    acting.value = false
  }
}

async function openCancel(row) {
  try {
    await approvalApi.adminCancel(row.order.id, '管理员撤销')
    ElMessage.success('已撤销')
    loadData()
  } catch (e) {}
}
</script>

<style scoped>
.approval-page { display: flex; flex-direction: column; gap: 0; }

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 14px;
}
.tab-group { display: flex; gap: 4px; }
.tab-btn {
  padding: 5px 14px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: #fff;
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
  display: flex;
  align-items: center;
  gap: 4px;
}
.tab-btn:hover { border-color: var(--accent); color: var(--accent); }
.tab-btn.active { background: var(--accent); border-color: var(--accent); color: #fff; }
.tab-badge {
  font-size: 11px;
  background: rgba(255,255,255,0.25);
  border-radius: 8px;
  padding: 0 5px;
  min-width: 18px;
  text-align: center;
  line-height: 16px;
}
.tab-btn:not(.active) .tab-badge { background: #f0f2f5; color: var(--text-muted); }

.bar-right { display: flex; align-items: center; gap: 8px; }
.cell-primary { font-size: 13px; color: var(--text-primary); font-weight: 500; }
.cell-sub { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
.mono { font-family: monospace; font-size: 12px; color: var(--text-secondary); }
.pagination { margin-top: 14px; justify-content: flex-end; display: flex; }
</style>
