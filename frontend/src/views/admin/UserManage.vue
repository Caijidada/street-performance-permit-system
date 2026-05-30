<template>
  <div class="user-page">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="searchName" placeholder="搜索姓名/手机号" style="width:200px" clearable @keyup.enter="loadData" />
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
      <div class="toolbar-right">
        <span class="total-tip">共 {{ total }} 位艺人</span>
      </div>
    </div>

    <el-table :data="list" v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="艺人" width="160">
        <template #default="{ row }">
          <div class="cell-primary">{{ row.realName }}</div>
          <div class="cell-sub">{{ row.username }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column label="信用分" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.creditScore >= 80 ? 'success' : row.creditScore >= 60 ? 'warning' : 'danger'">
            {{ row.creditScore }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="演艺资质" width="220">
        <template #default="{ row }">
          <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap">
            <el-tag v-if="row.qualifyStatus === 1" type="success" size="small">已认证</el-tag>
            <el-tag v-else-if="row.qualifyStatus === 0" type="warning" size="small">待审核</el-tag>
            <el-tag v-else type="danger" size="small">未通过</el-tag>
            <el-link v-if="row.qualification" type="primary" :href="row.qualification" target="_blank" style="font-size:12px">查看文件</el-link>
          </div>
          <div v-if="row.qualifyStatus === 0" style="margin-top:6px;display:flex;gap:6px">
            <el-button size="small" type="success" @click="auditQualify(row, 1)">通过</el-button>
            <el-button size="small" type="danger" plain @click="auditQualify(row, 2)">拒绝</el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="账号状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="180" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <div style="display:flex;flex-direction:column;gap:4px">
            <el-button size="small" type="warning" plain @click="openCreditOp(row)">信用操作</el-button>
            <el-popconfirm
              :title="row.status === 1 ? '确认禁用该账号？' : '确认启用该账号？'"
              @confirm="toggleStatus(row)"
            >
              <template #reference>
                <el-button size="small" :type="row.status === 1 ? 'danger' : 'success'" plain>
                  {{ row.status === 1 ? '禁用' : '启用' }}
                </el-button>
              </template>
            </el-popconfirm>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-model:current-page="page" :page-size="size" :total="total"
      layout="total, prev, pager, next" class="pagination"
      @current-change="loadData" />

    <!-- 信用操作弹窗 -->
    <el-dialog v-model="creditVisible" :title="`信用操作 · ${creditTarget?.realName}`" width="420px">
      <el-form label-width="90px">
        <el-form-item label="当前信用分">
          <el-tag :type="creditTarget?.creditScore >= 80 ? 'success' : creditTarget?.creditScore >= 60 ? 'warning' : 'danger'">
            {{ creditTarget?.creditScore }} 分
          </el-tag>
        </el-form-item>
        <el-form-item label="事件类型">
          <el-select v-model="creditForm.eventType" placeholder="选择事件" style="width:100%">
            <el-option v-for="(v, k) in CREDIT_EVENT_TYPE" :key="k" :value="Number(k)" :label="v.label">
              <span>{{ v.label }}</span>
              <span :style="{ color: v.delta > 0 ? '#67c23a' : '#f56c6c', marginLeft: '8px', fontWeight: 600 }">
                {{ v.delta > 0 ? '+' : '' }}{{ v.delta }} 分
              </span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="creditForm.remark" placeholder="输入备注说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="creditVisible = false">取消</el-button>
        <el-button type="primary" :loading="creditLoading" @click="doCredit">确认操作</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { creditApi } from '@/api/credit'
import { CREDIT_EVENT_TYPE } from '@/utils/constants'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const creditLoading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const searchName = ref('')
const creditVisible = ref(false)
const creditTarget = ref(null)
const creditForm = ref({ eventType: null, remark: '' })

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const res = await request.get('/admin/user/list', { params: { page: page.value, size, keyword: searchName.value || undefined } })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function auditQualify(row, status) {
  await request.post(`/admin/user/${row.id}/qualify`, { status })
  ElMessage.success(status === 1 ? '已通过资质审核' : '已拒绝资质申请')
  loadData()
}

async function toggleStatus(row) {
  await request.post(`/admin/user/${row.id}/toggle-status`)
  ElMessage.success('操作成功')
  loadData()
}

function openCreditOp(row) {
  creditTarget.value = row
  creditForm.value = { eventType: null, remark: '' }
  creditVisible.value = true
}

async function doCredit() {
  if (!creditForm.value.eventType) { ElMessage.warning('请选择事件类型'); return }
  creditLoading.value = true
  try {
    await creditApi.operate({
      userId: creditTarget.value.id,
      eventType: creditForm.value.eventType,
      remark: creditForm.value.remark
    })
    ElMessage.success('信用操作成功')
    creditVisible.value = false
    loadData()
  } finally {
    creditLoading.value = false
  }
}
</script>

<style scoped>
.user-page { display: flex; flex-direction: column; }
.toolbar { display: flex; align-items: center; justify-content: space-between; padding-bottom: 14px; }
.toolbar-left { display: flex; gap: 8px; }
.toolbar-right { display: flex; align-items: center; gap: 8px; }
.total-tip { font-size: 12px; color: var(--text-muted); }
.cell-primary { font-size: 13px; color: var(--text-primary); font-weight: 500; }
.cell-sub { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
.pagination { margin-top: 14px; justify-content: flex-end; display: flex; }
</style>
