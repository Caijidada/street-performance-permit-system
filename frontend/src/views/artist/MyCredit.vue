<template>
  <div class="credit-page">
    <div class="score-panel">
      <div class="score-ring">
        <svg viewBox="0 0 120 120" class="ring-svg">
          <circle cx="60" cy="60" r="50" class="ring-track" />
          <circle cx="60" cy="60" r="50" class="ring-fill"
            :stroke="creditColor"
            :stroke-dasharray="`${creditScore * 3.14} 314`"
          />
        </svg>
        <div class="score-center">
          <div class="score-num" :style="{ color: creditColor }">{{ creditScore }}</div>
          <div class="score-unit">分</div>
        </div>
      </div>
      <div class="score-info">
        <div class="score-title">当前信用分</div>
        <div class="score-level">{{ creditLevel }}</div>
        <div class="score-hints">
          <div class="hint-item"><span class="hint-dot" style="background:var(--success)"></span>90+ 优先排期权</div>
          <div class="hint-item"><span class="hint-dot" style="background:var(--warning)"></span>60-89 标准使用</div>
          <div class="hint-item"><span class="hint-dot" style="background:var(--danger)"></span>&lt;60 热门点位受限</div>
        </div>
      </div>
    </div>

    <div class="log-section">
      <div class="log-header">信用变动记录</div>
      <el-table :data="logs" v-loading="loading">
        <el-table-column label="事件">
          <template #default="{ row }">
            <el-tag :type="row.delta > 0 ? 'success' : 'danger'">
              {{ CREDIT_EVENT_TYPE[row.eventType]?.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分值变化" width="100" align="center">
          <template #default="{ row }">
            <span :class="row.delta > 0 ? 'delta-up' : 'delta-down'">
              {{ row.delta > 0 ? '+' : '' }}{{ row.delta }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="scoreAfter" label="变化后" width="90" align="center" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="10" :total="total"
        layout="total, prev, pager, next" class="pagination"
        @current-change="loadLogs" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { creditApi } from '@/api/credit'
import { CREDIT_EVENT_TYPE } from '@/utils/constants'

const userStore = useUserStore()
const loading = ref(false)
const logs = ref([])
const total = ref(0)
const page = ref(1)

const creditScore = computed(() => Math.min(userStore.userInfo?.creditScore ?? 100, 100))
const creditColor = computed(() => {
  if (creditScore.value >= 80) return '#67c23a'
  if (creditScore.value >= 60) return '#e6a23c'
  return '#f56c6c'
})
const creditLevel = computed(() => {
  if (creditScore.value >= 90) return '信誉优秀，享有优先排期权'
  if (creditScore.value >= 70) return '信誉良好'
  if (creditScore.value >= 60) return '信誉一般，无法预约热门点位'
  return '信誉较差，账号受限'
})

onMounted(() => { userStore.refreshUserInfo(); loadLogs() })

async function loadLogs() {
  loading.value = true
  try {
    const res = await creditApi.myLogs(page.value, 10)
    logs.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.credit-page { display: flex; flex-direction: column; gap: 20px; }

.score-panel {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 24px 28px;
  display: flex;
  align-items: center;
  gap: 32px;
}
.score-ring { position: relative; width: 120px; height: 120px; flex-shrink: 0; }
.ring-svg { transform: rotate(-90deg); }
.ring-track { fill: none; stroke: var(--bg-hover); stroke-width: 8; }
.ring-fill { fill: none; stroke-width: 8; stroke-linecap: round; transition: stroke-dasharray 0.6s ease; }
.score-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.score-num { font-size: 28px; font-weight: 700; line-height: 1; }
.score-unit { font-size: 12px; color: var(--text-muted); }
.score-info { flex: 1; }
.score-title { font-size: 15px; font-weight: 600; color: var(--text-primary); margin-bottom: 4px; }
.score-level { font-size: 13px; color: var(--text-secondary); margin-bottom: 16px; }
.score-hints { display: flex; flex-direction: column; gap: 6px; }
.hint-item { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--text-muted); }
.hint-dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }

.log-section { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); overflow: hidden; }
.log-header { padding: 10px 16px; border-bottom: 1px solid var(--border); font-size: 13px; font-weight: 600; color: var(--text-primary); }
.delta-up { color: var(--success); font-weight: 600; }
.delta-down { color: var(--danger); font-weight: 600; }
.pagination { padding: 10px 0; justify-content: flex-end; display: flex; margin: 0 16px; }
</style>
