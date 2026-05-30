<template>
  <div class="dashboard">
    <!-- 概览卡片 -->
    <div class="stat-grid">
      <div class="stat-card" v-for="item in overviewCards" :key="item.key">
        <div class="stat-value" :style="{ color: item.color }">{{ overview[item.key] ?? 0 }}</div>
        <div class="stat-label">{{ item.label }}</div>
      </div>
    </div>

    <!-- 城市点位地图 -->
    <div class="chart-block" style="margin-bottom:20px">
      <div class="chart-header">
        <span>城市点位分布地图</span>
        <div style="display:flex;align-items:center;gap:12px">
          <el-select v-model="selectedCity" style="width:150px" @change="onCityChange">
            <el-option v-for="c in cities" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
          <div class="map-legend">
            <span class="legend-dot dot-open"></span><span>开放</span>
            <span class="legend-dot dot-closed" style="margin-left:10px"></span><span>暂停</span>
          </div>
        </div>
      </div>
      <div ref="cityMapRef" class="map-area"></div>
    </div>

    <div class="charts-row">
      <!-- 演出类型分布 -->
      <div class="chart-block">
        <div class="chart-header"><span>演出类型分布</span></div>
        <div ref="pieChartRef" style="height:240px"></div>
      </div>
      <!-- 点位预约量排行 -->
      <div class="chart-block">
        <div class="chart-header"><span>点位预约量 TOP8</span></div>
        <div ref="barChartRef" style="height:240px"></div>
      </div>
      <!-- 艺人活跃度 -->
      <div class="chart-block">
        <div class="chart-header"><span>艺人活跃度 TOP10</span></div>
        <el-table :data="artistRank" size="small" :show-header="false" style="height:240px;overflow-y:auto">
          <el-table-column type="index" width="32" />
          <el-table-column prop="realName" label="姓名" />
          <el-table-column prop="performCount" label="次数" width="54" align="right" />
          <el-table-column prop="creditScore" label="信用" width="66" align="right">
            <template #default="{ row }">
              <el-tag size="small" :type="row.creditScore >= 80 ? 'success' : 'warning'">{{ row.creditScore }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 演出热力图 -->
    <div class="chart-block">
      <div class="chart-header"><span>演出热力图（各点位演出频次）</span></div>
      <div ref="heatmapRef" style="height:380px"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import AMapLoader from '@amap/amap-jsapi-loader'
import { statsApi } from '@/api/statistics'
import { venueApi } from '@/api/venue'
import { ElMessage } from 'element-plus'

const overview = ref({})
const artistRank = ref([])
const pieChartRef = ref()
const barChartRef = ref()
const heatmapRef = ref()
const cityMapRef = ref()
const selectedCity = ref('wuhan')

let cityMapInstance = null
let cityMarkers = []
let AMapRef = null

const cities = [
  { label: '武汉市', value: 'wuhan', center: [114.3055, 30.5929], zoom: 12 },
  { label: '北京市', value: 'beijing', center: [116.4074, 39.9042], zoom: 11 },
  { label: '上海市', value: 'shanghai', center: [121.4737, 31.2304], zoom: 12 },
  { label: '广州市', value: 'guangzhou', center: [113.2644, 23.1291], zoom: 12 },
  { label: '深圳市', value: 'shenzhen', center: [114.0579, 22.5431], zoom: 12 },
]

const overviewCards = [
  { key: 'totalArtists',   label: '注册艺人', color: '#409eff' },
  { key: 'totalVenues',    label: '开放点位', color: '#67c23a' },
  { key: 'totalReports',   label: '总报备数', color: '#e6a23c' },
  { key: 'approvedReports',label: '已通过报备', color: '#67c23a' },
  { key: 'pendingReports', label: '待审报备', color: '#f56c6c' },
  { key: 'todayReports',   label: '今日演出', color: '#909399' },
]

onMounted(async () => {
  const results = await Promise.allSettled([
    statsApi.overview(),
    statsApi.performType(),
    statsApi.venueRate(),
    statsApi.artistRank(),
    statsApi.heatmap()
  ])

  const [overviewRes, typeRes, venueRes, rankRes, heatRes] = results
  const overviewData = overviewRes.status === 'fulfilled' ? overviewRes.value : {}
  const typeData     = typeRes.status === 'fulfilled'     ? typeRes.value     : []
  const venueData    = venueRes.status === 'fulfilled'    ? venueRes.value    : []
  const rankData     = rankRes.status === 'fulfilled'     ? rankRes.value     : []
  const heatData     = heatRes.status === 'fulfilled'     ? heatRes.value     : []

  overview.value = overviewData
  artistRank.value = rankData

  const lightTooltip = { backgroundColor: '#fff', borderColor: '#e4e7ed', textStyle: { color: '#303133' }, extraCssText: 'box-shadow:0 4px 12px rgba(0,0,0,0.1)' }

  // 饼图
  const pie = echarts.init(pieChartRef.value)
  pie.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', formatter: '{b}: {c}场 ({d}%)', ...lightTooltip },
    legend: { bottom: 0, type: 'scroll', textStyle: { color: '#606266' } },
    color: ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#9b59b6'],
    series: [{
      type: 'pie', radius: ['38%', '62%'],
      data: typeData.length ? typeData : [
        { name: '歌唱', value: 8 }, { name: '舞蹈', value: 5 },
        { name: '乐器', value: 4 }, { name: '魔术', value: 2 }, { name: '杂技', value: 1 }
      ],
      label: { color: '#606266' },
      emphasis: { itemStyle: { shadowBlur: 10 } }
    }]
  })

  // 柱状图
  const bar = echarts.init(barChartRef.value)
  const topVenues = venueData.length ? venueData.slice(0, 8) : [
    { venueName: '江汉路步行街A区', count: 7 },
    { venueName: '光谷步行街广场', count: 5 },
    { venueName: '楚河汉街演出区', count: 4 },
    { venueName: '黄鹤楼景区广场', count: 2 },
    { venueName: '水果湖广场', count: 2 },
  ]
  bar.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', ...lightTooltip },
    grid: { left: '5%', right: '10%', containLabel: true, top: 8, bottom: 8 },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#ebeef5' } }, axisLabel: { color: '#909399' } },
    yAxis: { type: 'category', data: topVenues.map(v => v.venueName.slice(0, 8)), axisLabel: { color: '#606266', fontSize: 11 } },
    series: [{
      type: 'bar', data: topVenues.map(v => v.count),
      itemStyle: { color: '#409eff', borderRadius: [0, 4, 4, 0] },
      label: { show: true, position: 'right', color: '#909399', fontSize: 11 }
    }]
  })

  // 地图异步初始化，不阻塞页面渲染
  initCityMap()
  initHeatmap(heatData)
})

onBeforeUnmount(() => {
  if (cityMapInstance) cityMapInstance.destroy()
})

function withTimeout(promise, ms) {
  return Promise.race([
    promise,
    new Promise((_, reject) => setTimeout(() => reject(new Error('timeout')), ms))
  ])
}

async function initCityMap() {
  const amapKey = import.meta.env.VITE_AMAP_KEY
  if (!amapKey || amapKey.includes('请')) {
    renderFallbackCityMap()
    return
  }
  try {
    AMapRef = await withTimeout(AMapLoader.load({
      key: amapKey,
      version: '2.0',
      plugins: ['AMap.InfoWindow', 'AMap.Marker', 'AMap.HeatMap', 'AMap.Geocoder', 'AMap.PlaceSearch']
    }), 8000)
    const city = cities.find(c => c.value === selectedCity.value)
    cityMapInstance = new AMapRef.Map(cityMapRef.value, {
      zoom: city.zoom,
      center: city.center,
      mapStyle: 'amap://styles/fresh'
    })
    await loadVenueMarkers()
  } catch (e) {
    renderFallbackCityMap()
  }
}

async function loadVenueMarkers(fitView = true) {
  if (!cityMapInstance || !AMapRef) return
  cityMarkers.forEach(m => cityMapInstance.remove(m))
  cityMarkers = []

  let venues = []
  try { venues = await venueApi.getMapList() } catch (e) { return }
  if (!venues || !venues.length) return

  const infoWindow = new AMapRef.InfoWindow({ offset: new AMapRef.Pixel(0, -30) })

  venues.forEach(venue => {
    if (!venue.longitude || !venue.latitude) return
    const marker = new AMapRef.Marker({
      position: [venue.longitude, venue.latitude],
      title: venue.name,
      icon: new AMapRef.Icon({
        size: new AMapRef.Size(28, 28),
        image: venue.status === 1
          ? 'https://webapi.amap.com/theme/v1.3/markers/n/mark_b.png'
          : 'https://webapi.amap.com/theme/v1.3/markers/n/mark_r.png',
        imageSize: new AMapRef.Size(28, 28)
      })
    })
    marker.on('click', () => {
      infoWindow.setContent(`
        <div style="padding:10px;min-width:200px;font-size:13px">
          <div style="font-weight:700;font-size:14px;margin-bottom:6px;color:#303133">${venue.name}</div>
          <div style="color:#606266;margin-bottom:4px">[地址] ${venue.district} · ${venue.address}</div>
          <div style="color:#606266;margin-bottom:4px">[时间] ${venue.openTimeStart?.slice(0,5)} - ${venue.openTimeEnd?.slice(0,5)}</div>
          <div style="color:#606266;margin-bottom:4px">[类型] ${venue.allowTypes}</div>
          <div style="color:#606266">[限额] 最多 ${venue.maxAudience} 人</div>
        </div>
      `)
      infoWindow.open(cityMapInstance, marker.getPosition())
    })
    cityMapInstance.add(marker)
    cityMarkers.push(marker)
  })

  // 只有初次加载才自动缩放到所有点位，切换城市时保持选中城市的视角
  if (fitView && cityMarkers.length > 0) {
    cityMapInstance.setFitView(cityMarkers)
  }
}

async function onCityChange(val) {
  const city = cities.find(c => c.value === val)
  if (!city) return
  if (cityMapInstance) {
    cityMapInstance.setCenter(city.center)
    cityMapInstance.setZoom(city.zoom)
    // 重新加载标记但不 fitView，保持切换城市的中心和缩放
    await loadVenueMarkers(false)
  }
}

function renderFallbackCityMap() {
  const chart = echarts.init(cityMapRef.value)
  chart.setOption({
    backgroundColor: 'transparent',
    title: {
      text: '点位列表（未配置高德地图 Key）',
      left: 'center', top: 10,
      textStyle: { fontSize: 13, color: '#909399' }
    },
    tooltip: { trigger: 'item', formatter: p => `${p.name}<br/>经度：${p.value[0]}<br/>纬度：${p.value[1]}`,
      backgroundColor: '#fff', borderColor: '#e4e7ed', textStyle: { color: '#303133' } },
    grid: { left: '5%', right: '5%', top: '15%', bottom: '10%', containLabel: true },
    xAxis: { type: 'value', name: '经度', scale: true, axisLine: { show: false }, axisLabel: { color: '#909399' }, splitLine: { lineStyle: { color: '#ebeef5' } } },
    yAxis: { type: 'value', name: '纬度', scale: true, axisLine: { show: false }, axisLabel: { color: '#909399' }, splitLine: { lineStyle: { color: '#ebeef5' } } },
    series: [{
      type: 'scatter',
      data: [
        { name: '江汉路步行街A区',  value: [114.27195, 30.58209] },
        { name: '光谷步行街广场',   value: [114.41667, 30.49745] },
        { name: '楚河汉街演出区',   value: [114.34921, 30.54936] },
        { name: '黄鹤楼景区广场',   value: [114.30460, 30.54429] },
        { name: '水果湖广场',       value: [114.35694, 30.55694] },
      ],
      symbolSize: 20,
      itemStyle: { color: '#409eff', opacity: 0.85 },
      label: { show: true, formatter: p => p.name, position: 'right', fontSize: 12, color: '#606266' }
    }]
  })
}

async function initHeatmap(heatData) {
  const amapKey = import.meta.env.VITE_AMAP_KEY
  if (!amapKey || amapKey.includes('请')) {
    const heatChart = echarts.init(heatmapRef.value)
    heatChart.setOption({
      backgroundColor: 'transparent',
      title: { text: '（未配置高德地图Key，以气泡图替代）', left: 'center', textStyle: { fontSize: 13, color: '#909399' } },
      tooltip: { trigger: 'item', formatter: p => `${p.name}<br/>演出次数：${p.value[2]}`,
        backgroundColor: '#fff', borderColor: '#e4e7ed', textStyle: { color: '#303133' } },
      xAxis: { type: 'value', show: false },
      yAxis: { type: 'value', show: false },
      series: [{
        type: 'scatter',
        data: heatData.length ? heatData.map((d, i) => ({ name: d.name, value: [i, 0, d.count] })) : [
          { name: '江汉路', value: [0, 0, 8] }, { name: '光谷广场', value: [1, 0, 5] },
          { name: '楚河汉街', value: [2, 0, 4] }, { name: '黄鹤楼', value: [3, 0, 2] }
        ],
        symbolSize: d => Math.max(d[2] * 12, 24),
        itemStyle: { color: '#409eff', opacity: 0.75 },
        label: { show: true, formatter: p => p.name, position: 'bottom', fontSize: 12, color: '#606266' }
      }]
    })
    return
  }
  try {
    const AMap = AMapRef || await withTimeout(AMapLoader.load({ key: amapKey, version: '2.0', plugins: ['AMap.HeatMap'] }), 8000)
    const heatmap = new AMap.Map(heatmapRef.value, {
      zoom: 13, center: [114.3055, 30.5929], mapStyle: 'amap://styles/dark'
    })
    const heatmapLayer = new AMap.HeatMap(heatmap, {
      radius: 80,
      opacity: [0, 1],
      gradient: { 0.2: '#0000ff', 0.5: '#00ffff', 0.7: '#00ff00', 0.85: '#ffff00', 1.0: '#ff0000' }
    })
    const maxCount = Math.max(...heatData.map(d => Number(d.count)), 1)
    // max 设小一些，让每个点显示出来更饱满
    heatmapLayer.setDataSet({
      data: heatData.map(d => ({ lng: Number(d.lng), lat: Number(d.lat), count: Number(d.count) })),
      max: Math.ceil(maxCount * 0.6)
    })
  } catch (e) {
    ElMessage.warning('热力图加载失败：' + e.message)
  }
}
</script>

<style scoped>
.dashboard { padding: 4px; }

.stat-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}
.stat-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 16px;
  text-align: center;
}
.stat-value { font-size: 28px; font-weight: 700; line-height: 1.2; }
.stat-label { font-size: 12px; color: var(--text-muted); margin-top: 6px; }

.chart-block {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
}
.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-bottom: 1px solid var(--border);
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}
.map-area { height: 400px; }

.charts-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.map-legend { display: flex; align-items: center; font-size: 12px; color: var(--text-muted); }
.legend-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 4px; }
.dot-open { background: var(--accent); }
.dot-closed { background: var(--danger); }
</style>
