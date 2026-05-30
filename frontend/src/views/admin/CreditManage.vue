<template>
  <div class="credit-page">
    <!-- 扫码跳转高亮提示 -->
    <div v-if="highlightUserId" class="scan-alert">
      <el-icon><Warning /></el-icon>
      <span>已定位到艺人 <strong>{{ highlightName || `ID: ${highlightUserId}` }}</strong>，请选择事件类型后执行评分操作</span>
      <el-button size="small" text @click="clearHighlight">关闭</el-button>
    </div>

    <!-- 信用操作面板 -->
    <div class="op-block" :class="{ 'highlight-block': highlightUserId }">
      <div class="block-header">
        <span class="block-title">手动信用操作</span>
        <el-button size="small" plain @click="manageVisible = true">
          <el-icon style="margin-right:4px"><Setting /></el-icon>管理事件类型
        </el-button>
      </div>
      <el-form :model="form" inline>
        <el-form-item label="选择艺人">
          <el-select
            v-model="form.userId"
            filterable
            placeholder="输入姓名搜索"
            style="width:200px"
            :filter-method="filterArtist"
          >
            <el-option
              v-for="u in filteredArtists"
              :key="u.id"
              :value="u.id"
              :label="u.realName"
            >
              <span>{{ u.realName }}</span>
              <span style="color:var(--text-muted);font-size:12px;margin-left:8px">{{ u.phone }}</span>
              <span :style="{ color: u.creditScore >= 80 ? '#67c23a' : u.creditScore >= 60 ? '#e6a23c' : '#f56c6c', marginLeft:'8px', fontSize:'12px', fontWeight:600 }">{{ u.creditScore }}分</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="事件类型">
          <el-select v-model="form.eventType" placeholder="选择事件" style="width:220px" @change="onEventTypeChange">
            <el-option v-for="(v, k) in allEventTypes" :key="k" :value="Number(k)" :label="v.label">
              <span>{{ v.label }}</span>
              <span :style="{ color: v.delta > 0 ? '#67c23a' : '#f56c6c', marginLeft: '8px', fontWeight: 600 }">
                {{ v.delta > 0 ? '+' : '' }}{{ v.delta }} 分
              </span>
            </el-option>
            <el-option :value="0" label="自定义">
              <span>自定义</span>
              <span style="color:#909399;margin-left:8px;font-size:12px">手动输入</span>
            </el-option>
          </el-select>
        </el-form-item>
        <!-- 自定义事件额外字段 -->
        <template v-if="form.eventType === 0">
          <el-form-item label="事件名称">
            <el-input v-model="form.customLabel" placeholder="如：优秀表演奖励" style="width:150px" />
          </el-form-item>
          <el-form-item label="分值">
            <el-input-number v-model="form.customDelta" :min="-100" :max="100" style="width:120px" />
          </el-form-item>
        </template>
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="备注说明" style="width:180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="operating" @click="doOperate">执行操作</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 信用日志 -->
    <div class="log-header">
      <span class="block-title" style="margin:0">信用变动日志</span>
      <el-select
        v-model="filterUserId"
        filterable
        clearable
        placeholder="按艺人筛选"
        style="width:180px"
        :filter-method="filterArtist"
        @change="page = 1; loadLogs()"
        @clear="page = 1; loadLogs()"
      >
        <el-option v-for="u in filteredArtists" :key="u.id" :value="u.id" :label="u.realName" />
      </el-select>
    </div>

    <el-table :data="logs" v-loading="loading">
      <el-table-column label="艺人" width="130">
        <template #default="{ row }">
          <span class="artist-name">{{ userMap[row.userId]?.realName || '—' }}</span>
          <span class="artist-sub">ID: {{ row.userId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="事件" width="140">
        <template #default="{ row }">
          <el-tag :type="getEventMeta(row.eventType).type" size="small">
            {{ row.eventType === 0 ? (row.remark?.match(/^\[(.+?)\]/) ? row.remark.match(/^\[(.+?)\]/)[1] : '自定义') : getEventMeta(row.eventType).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="分值变化" width="90" align="center">
        <template #default="{ row }">
          <span :class="row.delta > 0 ? 'delta-up' : 'delta-down'">
            {{ row.delta > 0 ? '+' : '' }}{{ row.delta }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="scoreAfter" label="变化后" width="80" align="center" />
      <el-table-column label="备注" show-overflow-tooltip>
        <template #default="{ row }">{{ row.eventType === 0 ? (row.remark?.replace(/^\[.+?\]\s*/, '') || '') : (row.remark || '') }}</template>
      </el-table-column>
      <el-table-column label="操作人" width="90">
        <template #default="{ row }">{{ userMap[row.operatorId]?.realName || row.operatorId }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" width="170" />
    </el-table>

    <el-pagination v-model:current-page="page" :page-size="10" :total="total"
      layout="total, prev, pager, next" class="pagination"
      @current-change="loadLogs" />

    <!-- 事件类型管理弹窗 -->
    <el-dialog v-model="manageVisible" title="管理事件类型" width="560px">
      <div class="manage-hint">内置类型不可修改（分值以后端为准）；自定义类型可自由增删</div>

      <!-- 内置类型只读 -->
      <div class="type-section-title">内置事件</div>
      <div class="type-list">
        <div v-for="(v, k) in BASE_CREDIT_EVENT_TYPE" :key="k" class="type-row readonly">
          <span class="type-label">{{ v.label }}</span>
          <span :style="{ color: v.delta > 0 ? '#67c23a' : '#f56c6c', fontWeight:600 }">
            {{ v.delta > 0 ? '+' : '' }}{{ v.delta }} 分
          </span>
          <span class="type-lock">内置</span>
        </div>
      </div>

      <!-- 自定义类型可编辑 -->
      <div class="type-section-title" style="margin-top:16px">
        自定义事件
        <el-button size="small" type="primary" plain @click="addCustomType" style="margin-left:8px">+ 新增</el-button>
      </div>
      <div class="type-list">
        <div v-if="customTypes.length === 0" class="type-empty">暂无自定义事件类型</div>
        <div v-for="(item, idx) in customTypes" :key="item.id" class="type-row">
          <el-input v-model="item.label" size="small" style="width:140px" placeholder="事件名称" />
          <el-input-number v-model="item.delta" size="small" :min="-100" :max="100" style="width:120px" />
          <span style="font-size:12px;color:var(--text-muted)">分</span>
          <el-button size="small" type="danger" plain @click="removeCustomType(idx)">删除</el-button>
        </div>
      </div>

      <template #footer>
        <el-button @click="manageVisible = false">取消</el-button>
        <el-button type="primary" @click="saveManage">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { creditApi } from '@/api/credit'
import { certificateApi } from '@/api/certificate'
import { BASE_CREDIT_EVENT_TYPE, getMergedEventTypes, saveCustomEventTypes, loadCustomEventTypes } from '@/utils/constants'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const operating = ref(false)
const logs = ref([])
const total = ref(0)
const page = ref(1)
const filterUserId = ref(null)
const form = ref({ userId: null, eventType: null, customLabel: '', customDelta: 0, remark: '' })
const highlightUserId = ref(null)
const highlightName = ref('')
const manageVisible = ref(false)

// 艺人列表
const artists = ref([])
const artistKeyword = ref('')
const userMap = ref({})  // id → user

// 事件类型
const allEventTypes = ref(getMergedEventTypes())
const customTypes = ref(loadCustomEventTypes())

const filteredArtists = computed(() => {
  if (!artistKeyword.value) return artists.value
  const kw = artistKeyword.value.toLowerCase()
  return artists.value.filter(u => u.realName?.includes(kw) || u.phone?.includes(kw))
})

function filterArtist(kw) { artistKeyword.value = kw }

onMounted(async () => {
  await loadArtists()
  const uid = route.query.highlightUserId
  if (uid) {
    highlightUserId.value = uid
    const uidNum = Number(uid)
    form.value.userId = uidNum
    filterUserId.value = uidNum
    highlightName.value = userMap.value[uidNum]?.realName || ''
  }
  loadLogs()
})

async function loadArtists() {
  try {
    const res = await request.get('/admin/user/list', { params: { page: 1, size: 200 } })
    artists.value = res.records || []
    artists.value.forEach(u => { userMap.value[u.id] = u })
  } catch {}
}

function clearHighlight() {
  highlightUserId.value = null
  highlightName.value = ''
  router.replace({ query: {} })
}

function onEventTypeChange(val) {
  form.value.customLabel = ''
  form.value.customDelta = 0
  // 选中自定义类型（时间戳 key，不在内置 1-7 内），自动填入 label/delta 并映射为 eventType=0
  if (val && !BASE_CREDIT_EVENT_TYPE[val] && val !== 0) {
    const custom = customTypes.value.find(t => t.id === val)
    if (custom) {
      form.value.customLabel = custom.label
      form.value.customDelta = custom.delta
    }
    form.value.eventType = 0
  }
}

function getEventMeta(eventType) {
  const all = allEventTypes.value
  if (all[eventType]) return { label: all[eventType].label, type: all[eventType].type || 'info' }
  if (eventType === 0) return { label: '自定义', type: 'info' }
  return { label: `类型${eventType}`, type: 'info' }
}

async function loadLogs() {
  loading.value = true
  try {
    const res = await creditApi.adminLogs(filterUserId.value || undefined, page.value, 10)
    logs.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function doOperate() {
  if (!form.value.userId) { ElMessage.warning('请选择艺人'); return }
  if (form.value.eventType === null || form.value.eventType === undefined) { ElMessage.warning('请选择事件类型'); return }
  if (form.value.eventType === 0) {
    if (!form.value.customLabel?.trim()) { ElMessage.warning('请输入自定义事件名称'); return }
    if (form.value.customDelta === 0) { ElMessage.warning('自定义分值不能为 0'); return }
  }
  operating.value = true
  try {
    await creditApi.operate({
      userId: form.value.userId,
      eventType: form.value.eventType,
      remark: form.value.remark,
      customDelta: form.value.eventType === 0 ? form.value.customDelta : undefined,
      customLabel: form.value.eventType === 0 ? form.value.customLabel : undefined,
    })
    ElMessage.success('操作成功')
    await loadArtists()  // 刷新信用分
    loadLogs()
  } finally {
    operating.value = false
  }
}

// 事件类型管理
function addCustomType() {
  customTypes.value.push({ id: Date.now(), label: '', delta: -5, color: '#909399', type: 'info' })
}

function removeCustomType(idx) {
  customTypes.value.splice(idx, 1)
}

function saveManage() {
  for (const t of customTypes.value) {
    if (!t.label?.trim()) { ElMessage.warning('请填写所有自定义事件的名称'); return }
    if (t.delta === 0) { ElMessage.warning(`"${t.label}" 的分值不能为 0`); return }
  }
  saveCustomEventTypes(customTypes.value)
  allEventTypes.value = getMergedEventTypes()
  manageVisible.value = false
  ElMessage.success('事件类型已保存')
}
</script>

<style scoped>
.credit-page { display: flex; flex-direction: column; gap: 0; }
.op-block {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 16px 20px;
  margin-bottom: 20px;
  transition: border-color 0.3s, box-shadow 0.3s;
}
.block-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.block-title { font-size: 13px; font-weight: 600; color: var(--text-primary); }
.log-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }

.artist-name { display: block; font-size: 13px; color: var(--text-primary); font-weight: 500; }
.artist-sub { display: block; font-size: 11px; color: var(--text-muted); }

.delta-up { color: var(--success); font-weight: 600; font-size: 13px; }
.delta-down { color: var(--danger); font-weight: 600; font-size: 13px; }
.pagination { margin-top: 14px; justify-content: flex-end; display: flex; }

.scan-alert {
  display: flex; align-items: center; gap: 8px;
  background: #fff3e0; border: 1px solid #e6a23c;
  border-radius: var(--radius-sm); padding: 10px 14px;
  margin-bottom: 14px; font-size: 13px; color: #b45309;
}
.scan-alert .el-icon { color: #e6a23c; font-size: 16px; flex-shrink: 0; }
.scan-alert strong { color: #92400e; }
.scan-alert .el-button { margin-left: auto; }

.highlight-block {
  border-color: #f56c6c !important;
  box-shadow: 0 0 0 3px rgba(245,108,108,0.15);
  animation: pulse-red 1.5s ease-in-out 2;
}
@keyframes pulse-red {
  0%, 100% { box-shadow: 0 0 0 3px rgba(245,108,108,0.15); }
  50%       { box-shadow: 0 0 0 6px rgba(245,108,108,0.25); }
}

/* 事件类型管理 */
.manage-hint { font-size: 12px; color: var(--text-muted); margin-bottom: 12px; }
.type-section-title { font-size: 12px; font-weight: 600; color: var(--text-secondary); margin-bottom: 8px; display: flex; align-items: center; }
.type-list { display: flex; flex-direction: column; gap: 6px; }
.type-row { display: flex; align-items: center; gap: 10px; padding: 6px 10px; border-radius: var(--radius-sm); background: var(--bg-base); }
.type-row.readonly { opacity: 0.8; }
.type-label { flex: 1; font-size: 13px; color: var(--text-primary); }
.type-lock { font-size: 11px; color: var(--text-muted); background: #f5f7fa; padding: 1px 6px; border-radius: 4px; }
.type-empty { font-size: 12px; color: var(--text-muted); padding: 12px 10px; }
</style>
