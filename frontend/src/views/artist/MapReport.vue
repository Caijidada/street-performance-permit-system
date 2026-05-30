<template>
  <div class="map-report">
    <!-- 资质未通过提示 -->
    <el-alert
      v-if="qualifyStatus !== 1"
      :type="qualifyStatus === 0 ? 'warning' : 'error'"
      :title="qualifyStatus === 0 ? '您的演艺资质正在审核中，审核通过后才能提交报备' : '您的演艺资质未通过审核，请重新上传'"
      show-icon
      :closable="false"
      style="position:absolute;top:0;left:0;right:0;z-index:10"
    >
      <template #default>
        <el-link type="primary" @click="$router.push('/artist/profile')">去上传资质</el-link>
      </template>
    </el-alert>

    <div class="map-container" ref="mapContainer"></div>

    <!-- 点位信息 + 报备表单 侧边面板 -->
    <el-drawer v-model="drawerVisible" title="选点报备" size="500px" direction="rtl" :before-close="handleDrawerClose">
      <div v-if="selectedVenue">
        <!-- 点位信息卡 -->
        <el-card class="venue-card" shadow="never">
          <div class="venue-name">{{ selectedVenue.name }}</div>
          <div class="venue-meta">
            <el-icon><Location /></el-icon> {{ selectedVenue.address }}
          </div>
          <el-row :gutter="16" style="margin-top:12px">
            <el-col :span="12">
              <div class="meta-item"><label>开放时段</label><span>{{ selectedVenue.openTimeStart }} ~ {{ selectedVenue.openTimeEnd }}</span></div>
            </el-col>
            <el-col :span="12">
              <div class="meta-item"><label>最大音量</label><span>{{ selectedVenue.maxDecibel }} dB</span></div>
            </el-col>
            <el-col :span="12">
              <div class="meta-item"><label>最大人数</label><span>{{ selectedVenue.maxAudience }} 人</span></div>
            </el-col>
            <el-col :span="12">
              <div class="meta-item"><label>允许类型</label><span>{{ selectedVenue.allowTypes }}</span></div>
            </el-col>
          </el-row>
        </el-card>

        <el-divider>选择演出日期查看时段</el-divider>

        <!-- 日期选择 + 已占用时段 -->
        <el-date-picker
          v-model="calendarDate"
          type="date"
          placeholder="选择日期查看已占用时段"
          :disabled-date="disabledDate"
          style="width:100%;margin-bottom:12px"
          value-format="YYYY-MM-DD"
          @change="loadSlots"
        />

        <div v-if="occupiedSlots.length > 0" style="margin-bottom:12px">
          <div style="font-size:13px;color:var(--text-secondary);margin-bottom:6px">已占用时段（灰色为已预约）：</div>
          <el-tag
            v-for="slot in occupiedSlots"
            :key="slot.orderId"
            type="info"
            style="margin:3px"
          >
            {{ slot.start }} ~ {{ slot.end }} · {{ slot.performType }}
          </el-tag>
        </div>
        <div v-else-if="calendarDate" style="margin-bottom:12px;font-size:13px;color:#67c23a">
          该日期暂无预约，可自由选择时段
        </div>

        <el-divider>填写报备信息</el-divider>

        <el-form ref="reportFormRef" :model="reportForm" :rules="reportRules" label-position="top">
          <el-form-item label="演出日期" prop="performDate">
            <el-date-picker
              v-model="reportForm.performDate"
              type="date"
              placeholder="选择演出日期"
              :disabled-date="disabledDate"
              style="width:100%"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>

          <el-form-item label="演出时段" required>
            <el-row :gutter="12" style="width:100%">
              <el-col :span="12">
                <el-form-item prop="timeSlotStart" style="margin-bottom:0">
                  <el-time-picker v-model="reportForm.timeSlotStart" placeholder="开始时间" style="width:100%" value-format="HH:mm:ss" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item prop="timeSlotEnd" style="margin-bottom:0">
                  <el-time-picker v-model="reportForm.timeSlotEnd" placeholder="结束时间" style="width:100%" value-format="HH:mm:ss" />
                </el-form-item>
              </el-col>
            </el-row>
            <div style="font-size:12px;color:var(--text-muted);margin-top:4px">
              点位开放时段：{{ selectedVenue.openTimeStart }} ~ {{ selectedVenue.openTimeEnd }}
            </div>
          </el-form-item>

          <el-form-item label="演出类型" prop="performType">
            <el-select v-model="reportForm.performType" placeholder="请选择演出类型" style="width:100%">
              <el-option
                v-for="t in allowedPerformTypes"
                :key="t" :label="t" :value="t"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="演出内容/曲目">
            <el-input v-model="reportForm.performContent" type="textarea" :rows="3" placeholder="请简述演出曲目或内容" />
          </el-form-item>

          <el-form-item label="预计观众人数">
            <el-input-number v-model="reportForm.expectedAudience" :min="1" :max="selectedVenue.maxAudience" style="width:100%" />
            <div style="font-size:12px;color:var(--text-muted)">最多 {{ selectedVenue.maxAudience }} 人</div>
          </el-form-item>

          <el-form-item label="演出器材说明">
            <el-input v-model="reportForm.equipment" placeholder="如：吉他、音响（低于70dB）" />
          </el-form-item>
        </el-form>

        <!-- 冲突检测结果 -->
        <el-alert
          v-if="conflictInfo.show"
          :type="conflictInfo.type"
          :title="conflictInfo.message"
          show-icon :closable="false"
          style="margin-bottom:16px"
        />
      </div>

      <template #footer>
        <el-button @click="checkConflict" :loading="checking" type="warning" plain>冲突预检</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="conflictInfo.hasConflict === true || qualifyStatus !== 1"
          @click="handleSubmit"
        >
          提交报备
        </el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'
import { venueApi } from '@/api/venue'
import { reportApi } from '@/api/report'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const qualifyStatus = computed(() => userStore.userInfo?.qualifyStatus ?? -1)

const mapContainer = ref()
const drawerVisible = ref(false)
const selectedVenue = ref(null)
const checking = ref(false)
const submitting = ref(false)
const reportFormRef = ref()
const calendarDate = ref('')
const occupiedSlots = ref([])

let map = null
let infoWindow = null

const reportForm = ref({
  venueId: null,
  performDate: '',
  timeSlotStart: '',
  timeSlotEnd: '',
  performType: '',
  performContent: '',
  expectedAudience: 1,
  equipment: ''
})

const reportRules = {
  performDate: [{ required: true, message: '请选择演出日期', trigger: 'change' }],
  timeSlotStart: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  timeSlotEnd: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  performType: [{ required: true, message: '请选择演出类型', trigger: 'change' }]
}

const conflictInfo = ref({ show: false, hasConflict: null, type: 'success', message: '' })

// 仅展示该点位允许的演出类型
const allowedPerformTypes = computed(() => {
  if (!selectedVenue.value?.allowTypes) return []
  return selectedVenue.value.allowTypes.split(',').map(t => t.trim())
})

function disabledDate(time) {
  return time.getTime() < Date.now() - 8.64e7
}

function handleDrawerClose(done) {
  resetForm()
  done()
}

onMounted(initMap)
onUnmounted(() => { map?.destroy() })

async function initMap() {
  const amapKey = import.meta.env.VITE_AMAP_KEY
  if (!amapKey || amapKey.includes('请')) {
    ElMessage.error('高德地图 Key 未配置，请在 .env 文件中设置 VITE_AMAP_KEY')
    return
  }

  try {
    const AMap = await AMapLoader.load({
      key: amapKey,
      version: '2.0',
      plugins: ['AMap.InfoWindow', 'AMap.Marker']
    })

    map = new AMap.Map(mapContainer.value, {
      zoom: 13,
      center: [114.3055, 30.5929],
      mapStyle: 'amap://styles/normal'
    })

    infoWindow = new AMap.InfoWindow({ offset: new AMap.Pixel(0, -36) })

    const venues = await venueApi.getMapList()

    venues.forEach(venue => {
      const isOpen = venue.status === 1
      const markerOpts = {
        position: [Number(venue.longitude), Number(venue.latitude)],
        title: venue.name
      }
      if (!isOpen) {
        markerOpts.icon = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_r.png'
      }
      const marker = new AMap.Marker(markerOpts)

      marker.on('click', () => {
        infoWindow.setContent(`
          <div style="padding:12px;min-width:220px;font-family:sans-serif">
            <div style="font-weight:700;font-size:15px;color:#303133;margin-bottom:6px">${venue.name}</div>
            ${!isOpen ? '<div style="color:#f56c6c;font-size:12px;margin-bottom:6px">⚠ 该点位当前已关闭</div>' : ''}
            <div style="color:#666;font-size:13px;margin-bottom:4px">地址：${venue.address}</div>
            <div style="color:#666;font-size:13px;margin-bottom:4px">时间：${venue.openTimeStart} ~ ${venue.openTimeEnd}</div>
            <div style="color:#666;font-size:13px;margin-bottom:8px">类型：${venue.allowTypes}</div>
            ${isOpen
              ? `<button id="btn-select-${venue.id}" style="width:100%;padding:7px 0;background:#409eff;color:#fff;border:none;border-radius:5px;cursor:pointer;font-size:13px">选择此点位</button>`
              : `<button disabled style="width:100%;padding:7px 0;background:#ccc;color:#fff;border:none;border-radius:5px;font-size:13px;cursor:not-allowed">点位已关闭</button>`
            }
          </div>
        `)
        infoWindow.open(map, marker.getPosition())

        if (isOpen) {
          setTimeout(() => {
            const btn = document.getElementById(`btn-select-${venue.id}`)
            if (btn) btn.onclick = () => selectVenue(venue)
          }, 0)
        }
      })

      marker.setMap(map)
    })
  } catch (e) {
    ElMessage.warning('地图加载失败：' + e.message)
  }
}

function selectVenue(venue) {
  selectedVenue.value = venue
  reportForm.value.venueId = venue.id
  reportForm.value.performType = ''
  conflictInfo.value.show = false
  occupiedSlots.value = []
  calendarDate.value = ''
  infoWindow?.close()
  drawerVisible.value = true
}

async function loadSlots(date) {
  if (!date || !selectedVenue.value) return
  try {
    occupiedSlots.value = await reportApi.getSlots(selectedVenue.value.id, date)
  } catch {
    occupiedSlots.value = []
  }
}

async function checkConflict() {
  try { await reportFormRef.value.validate() } catch { return }
  checking.value = true
  try {
    const result = await reportApi.preCheck({
      venueId: reportForm.value.venueId,
      performDate: reportForm.value.performDate,
      timeSlotStart: reportForm.value.timeSlotStart,
      timeSlotEnd: reportForm.value.timeSlotEnd
    })
    if (result.hasConflict) {
      conflictInfo.value = { show: true, hasConflict: true, type: 'error', message: result.message }
    } else {
      conflictInfo.value = { show: true, hasConflict: false, type: 'success', message: '该时间段无冲突，可以提交报备' }
    }
  } finally {
    checking.value = false
  }
}

async function handleSubmit() {
  try { await reportFormRef.value.validate() } catch { return }
  submitting.value = true
  try {
    await reportApi.submit({ ...reportForm.value })
    ElMessage.success('报备申请提交成功，请等待审核')
    drawerVisible.value = false
    resetForm()
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  reportForm.value = { venueId: null, performDate: '', timeSlotStart: '', timeSlotEnd: '', performType: '', performContent: '', expectedAudience: 1, equipment: '' }
  conflictInfo.value = { show: false, hasConflict: null, type: 'success', message: '' }
  occupiedSlots.value = []
  calendarDate.value = ''
}
</script>

<style scoped>
.map-report { position: relative; height: calc(100vh - 60px); overflow: hidden; }
.map-container { width: 100%; height: 100%; }
.venue-card { background: var(--bg-hover) !important; border: 1px solid var(--border) !important; }
.venue-name { font-size: 15px; font-weight: 700; color: var(--text-primary); margin-bottom: 6px; }
.venue-meta { color: var(--text-muted); font-size: 13px; display: flex; align-items: center; gap: 4px; }
.meta-item { margin-top: 8px; }
.meta-item label { display: block; font-size: 11px; color: var(--text-muted); }
.meta-item span { font-size: 13px; color: var(--text-primary); }
</style>
