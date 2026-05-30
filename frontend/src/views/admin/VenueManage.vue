<template>
  <div class="venue-page">
    <div class="toolbar">
      <el-button type="primary" @click="openForm(null)">新增点位</el-button>
      <span class="total-tip">共 {{ list.length }} 个点位</span>
    </div>

    <el-table :data="list" v-loading="loading">
      <el-table-column prop="name" label="点位名称" />
      <el-table-column prop="district" label="区域" width="100" />
      <el-table-column prop="address" label="地址" show-overflow-tooltip />
      <el-table-column prop="allowTypes" label="允许类型" width="160" show-overflow-tooltip />
      <el-table-column label="开放时间" width="160">
        <template #default="{ row }">{{ row.openTimeStart }} ~ {{ row.openTimeEnd }}</template>
      </el-table-column>
      <el-table-column label="开放日" width="160">
        <template #default="{ row }">
          <span class="day-tags">
            <span v-for="d in weekDays" :key="d.value"
              class="day-tag"
              :class="{ active: (row.openDays || '1,2,3,4,5,6,7').split(',').includes(String(d.value)) }"
            >{{ d.label }}</span>
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="maxDecibel" label="最大音量(dB)" width="110" align="center" />
      <el-table-column prop="maxAudience" label="最大人数" width="90" align="center" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="toggleStatus(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openForm(row)">编辑</el-button>
          <el-popconfirm title="确认删除该点位？" @confirm="removeVenue(row.id)">
            <template #reference>
              <el-button size="small" type="danger" plain>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="formVisible" :title="editRow ? '编辑点位' : '新增点位'" width="760px" @close="destroyPickerMap">
      <el-form :model="form" label-width="110px">
        <el-form-item label="点位名称">
          <el-input v-model="form.name" placeholder="请输入点位名称" />
        </el-form-item>

        <!-- 地图选点 -->
        <el-form-item label="地图选点">
          <div style="width:100%">
            <div style="display:flex;gap:8px;margin-bottom:8px;align-items:center">
              <!-- 城市限定 -->
              <el-select v-model="searchCity" style="width:130px" placeholder="城市">
                <el-option v-for="c in cityOptions" :key="c" :label="c" :value="c" />
              </el-select>
              <el-input
                v-model="searchKeyword"
                placeholder="输入地名搜索，如：博物馆、步行街"
                style="flex:1"
                @keyup.enter="searchAddress"
                clearable
              />
              <el-button type="primary" plain @click="searchAddress" :loading="searching">搜索</el-button>
            </div>
            <div ref="pickerMapRef" class="picker-map">
              <span v-if="!hasAmap" class="map-placeholder">未配置高德地图 Key，请手动输入经纬度</span>
              <span v-else-if="mapLoading" class="map-placeholder">地图加载中...</span>
            </div>
            <div class="coord-tip" :class="{ active: form.longitude }">
              <el-icon v-if="form.longitude" style="vertical-align:middle"><CircleCheck /></el-icon>
              <el-icon v-else style="vertical-align:middle"><Pointer /></el-icon>
              {{ form.longitude ? `已选点：${form.longitude}, ${form.latitude}` : '点击地图任意位置选择坐标' }}
            </div>
            <div v-if="form.longitude" class="fence-tip">
              <span class="fence-dot"></span>橙色圆圈为 100m 时空冲突检测围栏，同一时段内该范围不允许其他报备
            </div>
          </div>
        </el-form-item>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="省份"><el-input v-model="form.province" placeholder="自动填入" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="城市"><el-input v-model="form.city" placeholder="自动填入" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="所在区域"><el-input v-model="form.district" placeholder="自动填入" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="详细地址"><el-input v-model="form.address" placeholder="自动填入或手动修改" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="经度"><el-input v-model="form.longitude" placeholder="点击地图自动填入" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度"><el-input v-model="form.latitude" placeholder="点击地图自动填入" /></el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="开放星期">
          <el-checkbox-group v-model="openDaysList">
            <el-checkbox v-for="d in weekDays" :key="d.value" :value="d.value">{{ d.label }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="允许类型">
          <!-- 用 value 替代 label，消除 Element Plus 3.x 弃用警告 -->
          <el-checkbox-group v-model="allowTypesList">
            <el-checkbox v-for="t in performTypes" :key="t" :value="t">{{ t }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="开放开始时间">
              <el-time-picker v-model="form.openTimeStart" style="width:100%" value-format="HH:mm:ss" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开放结束时间">
              <el-time-picker v-model="form.openTimeEnd" style="width:100%" value-format="HH:mm:ss" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="最大音量(dB)">
              <el-input-number v-model="form.maxDecibel" :min="50" :max="100" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大观众人数">
              <el-input-number v-model="form.maxAudience" :min="10" :max="1000" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveVenue">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick, computed } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'
import { venueApi } from '@/api/venue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const saving = ref(false)
const searching = ref(false)
const mapLoading = ref(false)
const list = ref([])
const formVisible = ref(false)
const editRow = ref(null)
const form = ref({})
const pickerMapRef = ref()
const searchKeyword = ref('')
const searchCity = ref('武汉市')
const allowTypesList = ref([])
const openDaysList = ref([1, 2, 3, 4, 5, 6, 7])

const performTypes = ['歌唱', '乐器', '舞蹈', '魔术', '杂技', '其他']
const weekDays = [
  { label: '周一', value: 1 }, { label: '周二', value: 2 }, { label: '周三', value: 3 },
  { label: '周四', value: 4 }, { label: '周五', value: 5 }, { label: '周六', value: 6 },
  { label: '周日', value: 7 }
]
const cityOptions = ['武汉市', '北京市', '上海市', '广州市', '深圳市', '成都市', '杭州市', '西安市', '全国']

const hasAmap = computed(() => {
  const k = import.meta.env.VITE_AMAP_KEY
  return k && !k.includes('请')
})

let pickerMap = null
let pickerMarker = null
let conflictCircle = null
let venueMarkers = []
let AMapIns = null
let geocoder = null
let placeSearch = null

onMounted(loadData)

watch(formVisible, async (val) => {
  if (val && hasAmap.value) {
    await nextTick()
    setTimeout(initPickerMap, 150)
  }
  if (!val) destroyPickerMap()
})

watch(allowTypesList, (val) => {
  form.value.allowTypes = val.join(',')
})

watch(openDaysList, (val) => {
  form.value.openDays = val.slice().sort((a, b) => a - b).join(',')
})

// 城市切换时更新 PlaceSearch 搜索范围
watch(searchCity, (city) => {
  if (placeSearch) placeSearch.setCity(city === '全国' ? '' : city)
})

async function loadData() {
  loading.value = true
  try { list.value = await venueApi.adminList() }
  finally { loading.value = false }
}

function openForm(row) {
  editRow.value = row
  form.value = row ? { ...row } : {
    status: 1, maxDecibel: 70, maxAudience: 50,
    province: '湖北省', city: '武汉市'
  }
  allowTypesList.value = form.value.allowTypes
    ? form.value.allowTypes.split(',').map(s => s.trim()).filter(Boolean)
    : []
  openDaysList.value = form.value.openDays
    ? form.value.openDays.split(',').map(Number).filter(Boolean)
    : [1, 2, 3, 4, 5, 6, 7]
  searchKeyword.value = ''
  // 默认搜索城市跟随表单城市
  searchCity.value = form.value.city || '武汉市'
  formVisible.value = true
}

async function initPickerMap() {
  if (!pickerMapRef.value || !hasAmap.value) return
  mapLoading.value = true
  try {
    // window.AMap 由 Dashboard 加载时统一注册（含 Geocoder/PlaceSearch）
    // 若用户直接访问点位页面（未经过 Dashboard），则重新加载
    if (window.AMap) {
      AMapIns = window.AMap
    } else {
      AMapIns = await AMapLoader.load({
        key: import.meta.env.VITE_AMAP_KEY,
        version: '2.0',
        plugins: ['AMap.Geocoder', 'AMap.PlaceSearch']
      })
    }

    pickerMap = new AMapIns.Map(pickerMapRef.value, {
      zoom: 13,
      center: [114.3055, 30.5929],
      mapStyle: 'amap://styles/fresh'
    })

    geocoder = new AMapIns.Geocoder({ radius: 1000, extensions: 'all' })
    placeSearch = new AMapIns.PlaceSearch({
      pageSize: 1,
      city: searchCity.value === '全国' ? '' : searchCity.value,
      citylimit: searchCity.value !== '全国'
    })

    // 绘制已有点位标记和 100m 围栏圆圈
    list.value.forEach(venue => {
      if (!venue.longitude || !venue.latitude) return
      const isEditing = editRow.value && editRow.value.id === venue.id
      if (isEditing) return  // 当前编辑的点位用 pickerMarker 表示，不重复绘制
      const pos = [Number(venue.longitude), Number(venue.latitude)]
      const marker = new AMapIns.Marker({
        position: pos,
        title: venue.name,
        label: { content: `<div style="background:#fff;border:1px solid #e6a23c;border-radius:4px;padding:2px 6px;font-size:11px;color:#e6a23c;white-space:nowrap">${venue.name}</div>`, offset: new AMapIns.Pixel(-20, -30) }
      })
      const circle = new AMapIns.Circle({
        center: pos,
        radius: 100,
        strokeColor: '#e6a23c',
        strokeWeight: 2,
        strokeOpacity: 0.8,
        fillColor: '#e6a23c',
        fillOpacity: 0.12,
        zIndex: 10
      })
      pickerMap.add([marker, circle])
      venueMarkers.push(marker, circle)
    })

    if (form.value.longitude && form.value.latitude) {
      const pos = [Number(form.value.longitude), Number(form.value.latitude)]
      pickerMap.setCenter(pos)
      pickerMap.setZoom(15)
      addOrMoveMarker(pos)
    }

    pickerMap.on('click', (e) => {
      const { lng, lat } = e.lnglat
      addOrMoveMarker([lng, lat])
      reverseGeocodeRest(lng, lat)
    })
  } catch (e) {
    console.warn('地图初始化失败', e)
    ElMessage.warning('地图加载失败，请手动填写经纬度')
  } finally {
    mapLoading.value = false
  }
}

function addOrMoveMarker(pos) {
  if (pickerMarker) {
    pickerMarker.setPosition(pos)
  } else {
    pickerMarker = new AMapIns.Marker({ position: pos })
    pickerMap.add(pickerMarker)
  }
  // 绘制 100m 时空冲突围栏
  if (conflictCircle) {
    conflictCircle.setCenter(pos)
  } else {
    conflictCircle = new AMapIns.Circle({
      center: pos,
      radius: 100,
      strokeColor: '#e6a23c',
      strokeWeight: 2,
      strokeOpacity: 0.8,
      fillColor: '#e6a23c',
      fillOpacity: 0.12,
      zIndex: 10
    })
    pickerMap.add(conflictCircle)
  }
  form.value.longitude = String(pos[0].toFixed(6))
  form.value.latitude = String(pos[1].toFixed(6))
}

async function searchAddress() {
  const kw = searchKeyword.value.trim()
  if (!kw) { ElMessage.warning('请输入搜索关键词'); return }
  if (!pickerMap) { ElMessage.warning('地图尚未加载，请稍候'); return }

  const city = searchCity.value !== '全国' ? searchCity.value : ''
  searching.value = true
  try {
    // 先用 POI 搜索
    const poiRes = await fetch(`/api/amap/place/search?keywords=${encodeURIComponent(kw)}&city=${encodeURIComponent(city)}`)
    const poiData = await poiRes.json()
    if (poiData.code === 200 && poiData.data?.pois?.length) {
      const [lng, lat] = poiData.data.pois[0].location.split(',').map(Number)
      locateOnMap(lng, lat)
      return
    }
    // 降级：地理编码搜地址
    const query = city ? `${city}${kw}` : kw
    const geoRes = await fetch(`/api/amap/geocode?address=${encodeURIComponent(query)}&city=${encodeURIComponent(city)}`)
    const geoData = await geoRes.json()
    if (geoData.code === 200 && geoData.data?.geocodes?.length) {
      const [lng, lat] = geoData.data.geocodes[0].location.split(',').map(Number)
      locateOnMap(lng, lat)
    } else {
      ElMessage.warning(`未找到"${kw}"，请换个关键词或切换城市`)
    }
  } catch (e) {
    console.error('搜索出错', e)
    ElMessage.error('搜索失败：' + e.message)
  } finally {
    searching.value = false
  }
}

function locateOnMap(lng, lat) {
  pickerMap.setCenter([lng, lat])
  pickerMap.setZoom(16)
  addOrMoveMarker([lng, lat])
  reverseGeocodeRest(lng, lat)
}

async function reverseGeocodeRest(lng, lat) {
  try {
    const res = await fetch(`/api/amap/regeo?location=${lng},${lat}`)
    const data = await res.json()
    if (data.code === 200 && data.data?.regeocode) {
      const comp = data.data.regeocode.addressComponent
      const full = data.data.regeocode.formatted_address || ''
      const province = comp.province || ''
      const city = comp.city || comp.province || ''
      const district = comp.district || comp.township || ''
      const detail = full.replace(province, '').replace(city, '').replace(district, '').trim()
      form.value.province = province
      form.value.city = city
      form.value.district = district
      form.value.address = detail || full
      if (city) searchCity.value = city
    }
  } catch (e) {
    console.warn('逆地理编码失败', e)
  }
}

function destroyPickerMap() {
  if (pickerMap) { pickerMap.destroy(); pickerMap = null }
  pickerMarker = null
  conflictCircle = null
  venueMarkers = []
  geocoder = null
  placeSearch = null
}

async function saveVenue() {
  if (!form.value.name) { ElMessage.warning('请输入点位名称'); return }
  if (!form.value.longitude || !form.value.latitude) { ElMessage.warning('请在地图上选点或手动填写经纬度'); return }
  saving.value = true
  try {
    if (editRow.value) {
      await venueApi.update(editRow.value.id, form.value)
    } else {
      await venueApi.add(form.value)
    }
    ElMessage.success('保存成功')
    formVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function removeVenue(id) {
  await venueApi.remove(id)
  ElMessage.success('已删除')
  loadData()
}

async function toggleStatus(row) {
  await venueApi.update(row.id, { status: row.status })
}
</script>

<style scoped>
.venue-page { display: flex; flex-direction: column; }
.toolbar { display: flex; align-items: center; gap: 12px; padding-bottom: 14px; }
.total-tip { font-size: 12px; color: var(--text-muted); margin-left: auto; }
.picker-map {
  width: 100%;
  height: 300px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-light);
  overflow: hidden;
  background: var(--bg-hover);
  display: flex;
  align-items: center;
  justify-content: center;
}
.map-placeholder { color: var(--text-muted); font-size: 13px; }
.coord-tip { font-size: 12px; color: var(--text-muted); margin-top: 6px; display: flex; align-items: center; gap: 4px; }
.coord-tip.active { color: var(--success); }
.fence-tip { font-size: 11px; color: var(--text-muted); margin-top: 4px; display: flex; align-items: center; gap: 5px; }
.fence-dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: rgba(230,162,60,0.3); border: 2px solid #e6a23c; flex-shrink: 0; }
.day-tags { display: flex; flex-wrap: wrap; gap: 2px; }
.day-tag { font-size: 11px; padding: 1px 4px; border-radius: 3px; background: #f5f7fa; color: #c0c4cc; }
.day-tag.active { background: rgba(64,158,255,0.1); color: #409eff; font-weight: 600; }
</style>
