<template>
  <div class="page" v-loading="loading">
    <section class="panel analytics-head">
      <div>
        <h2 class="section-title">数据分析</h2>
        <p class="muted">展示空气温湿度、土壤温湿度、pH、二氧化碳和光照强度七项环境指标。</p>
      </div>
      <el-select v-model="greenhouseId" placeholder="选择大棚" style="width: 260px" @change="loadAnalytics">
        <el-option v-for="item in greenhouses" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
    </section>

    <div class="metric-strip">
      <article v-for="item in latestMetrics" :key="item.label">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }} {{ item.unit }}</strong>
      </article>
    </div>

    <section class="panel chart-panel chart-panel--wide">
      <div class="chart-title">
        <div>
          <h3>环境趋势</h3>
        </div>
        <div class="chart-controls">
          <el-select v-model="trendRangeHours" style="width: 150px" @change="loadAnalytics">
            <el-option v-for="item in trendRanges" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="trendMode" style="width: 180px" @change="renderCharts">
            <el-option v-for="item in trendModes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
      </div>
      <div ref="trendRef" class="chart chart--large"></div>
    </section>

    <div class="analytics-grid">
      <section class="panel chart-panel">
        <h3>设备状态</h3>
        <div ref="deviceRef" class="chart"></div>
      </section>
      <section class="panel chart-panel">
        <h3>告警级别</h3>
        <div ref="alertRef" class="chart"></div>
      </section>
      <section class="panel chart-panel">
        <h3>大棚面积结构</h3>
        <div ref="areaRef" class="chart"></div>
      </section>
    </div>
  </div>
</template>

<script setup>
import * as echarts from 'echarts'
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { fetchAnalytics, fetchGreenhouses } from '../services/greenhouse'

const loading = ref(false)
const greenhouses = ref([])
const greenhouseId = ref(null)
const analytics = ref(null)
const trendRef = ref(null)
const deviceRef = ref(null)
const alertRef = ref(null)
const areaRef = ref(null)
const trendMode = ref('all')
const trendRangeHours = ref(24)
const charts = []
let resizeHandler = null
let refreshTimer = null

const trendRanges = [
  { label: '近24小时', value: 24 },
  { label: '近3天', value: 72 },
  { label: '近7天', value: 168 },
  { label: '近14天', value: 336 }
]

const trendModes = [
  { label: '全部指标', value: 'all' },
  { label: '空气温度', value: 'airTemperature' },
  { label: '空气湿度', value: 'airHumidity' },
  { label: '土壤温度', value: 'soilTemperature' },
  { label: '土壤湿度', value: 'soilHumidity' },
  { label: 'pH 值', value: 'phValue' },
  { label: '二氧化碳', value: 'co2Ppm' },
  { label: '光照强度', value: 'lightLux' }
]

const labelMap = {
  RUNNING: '运行中',
  STOPPED: '已停止',
  MAINTENANCE: '维护中',
  WARNING: '警告',
  CRITICAL: '严重',
  INFO: '提示',
  OPEN: '待处理',
  ACKNOWLEDGED: '已下发建议',
  RESOLVED: '已解决'
}

const chartPalette = {
  green: '#5fa868',
  sage: '#9aa89e',
  amber: '#d8a342',
  blue: '#5f83c8',
  coral: '#d96a63',
  teal: '#5aa7a7',
  leaf: '#8abd76',
  gold: '#e3c45f',
  moss: '#7c9866'
}

const latest = computed(() => {
  const rows = analytics.value?.telemetryTrend || []
  return rows[rows.length - 1] || {}
})

const latestMetrics = computed(() => [
  { label: '空气温度', value: latest.value.airTemperature ?? '-', unit: '℃' },
  { label: '空气湿度', value: latest.value.airHumidity ?? '-', unit: '%' },
  { label: '土壤温度', value: latest.value.soilTemperature ?? '-', unit: '℃' },
  { label: '土壤湿度', value: latest.value.soilHumidity ?? '-', unit: '%' },
  { label: 'pH 值', value: latest.value.phValue ?? '-', unit: '' },
  { label: '二氧化碳', value: latest.value.co2Ppm ?? '-', unit: 'ppm' },
  { label: '光照强度', value: latest.value.lightLux ?? '-', unit: 'lx' }
])

const normalizeSeries = rows => (rows || []).map(item => ({ name: labelMap[item.name] || item.name, value: item.value }))
const chart = el => {
  const instance = echarts.init(el)
  charts.push(instance)
  return instance
}

const unit = name => ({
  空气温度: '℃',
  空气湿度: '%',
  土壤温度: '℃',
  土壤湿度: '%',
  'pH 值': '',
  二氧化碳: 'ppm',
  光照强度: 'lx'
}[name] || '')

const formatAxisTime = value => {
  if (!value) return ''
  const date = new Date(value)
  const options = trendRangeHours.value > 24
    ? { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }
    : { hour: '2-digit', minute: '2-digit' }
  return date.toLocaleString('zh-CN', options).replace(/\//g, '/')
}

const trendSeries = trend => {
  const base = [
    { mode: 'airTemperature', name: '空气温度', type: 'line', smooth: true, symbolSize: 7, yAxisIndex: 0, data: trend.map(item => item.airTemperature), color: '#4f75d8' },
    { mode: 'airHumidity', name: '空气湿度', type: 'line', smooth: true, symbolSize: 7, yAxisIndex: 0, data: trend.map(item => item.airHumidity), color: '#67b75b' },
    { mode: 'soilTemperature', name: '土壤温度', type: 'line', smooth: true, symbolSize: 7, yAxisIndex: 0, data: trend.map(item => item.soilTemperature), color: '#8b6bd6' },
    { mode: 'soilHumidity', name: '土壤湿度', type: 'line', smooth: true, symbolSize: 7, yAxisIndex: 0, data: trend.map(item => item.soilHumidity), color: '#e55b62' },
    { mode: 'phValue', name: 'pH 值', type: 'line', smooth: true, symbolSize: 7, yAxisIndex: 2, data: trend.map(item => item.phValue), color: '#8c6a3d' },
    { mode: 'co2Ppm', name: '二氧化碳', type: 'line', smooth: true, symbolSize: 7, yAxisIndex: 1, data: trend.map(item => item.co2Ppm), color: '#f2a93b' },
    { mode: 'lightLux', name: '光照强度', type: 'line', smooth: true, symbolSize: 7, yAxisIndex: 3, data: trend.map(item => item.lightLux), color: '#13a8a8' }
  ]
  return trendMode.value === 'all' ? base : base.filter(item => item.mode === trendMode.value)
}

const renderCharts = async () => {
  await nextTick()
  charts.splice(0).forEach(item => item.dispose())
  if (!analytics.value) return
  const trend = analytics.value.telemetryTrend || []
  chart(trendRef.value).setOption({
    color: ['#4f75d8', '#67b75b', '#8b6bd6', '#e55b62', '#8c6a3d', '#f2a93b', '#13a8a8'],
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,.96)',
      borderColor: '#d7ead5',
      textStyle: { color: '#1e3526' },
      formatter: params => {
        const title = params[0]?.axisValue || ''
        const lines = params.map(item => `${item.marker}${item.seriesName}: ${item.value} ${unit(item.seriesName)}`)
        return [title, ...lines].join('<br/>')
      }
    },
    legend: { top: 8, data: ['空气温度', '空气湿度', '土壤温度', '土壤湿度', 'pH 值', '二氧化碳', '光照强度'] },
    grid: { left: 54, right: 172, top: 72, bottom: 72 },
    dataZoom: [
      { type: 'inside', throttle: 50 },
      { type: 'slider', height: 22, bottom: 22, borderColor: '#d7ead5', fillerColor: 'rgba(91, 173, 92, .18)' }
    ],
    xAxis: {
      type: 'category',
      boundaryGap: false,
      axisLabel: { hideOverlap: true, rotate: trendRangeHours.value > 24 ? 28 : 0 },
      data: trend.map(item => formatAxisTime(item.collectedAt))
    },
    yAxis: [
      { type: 'value', name: '℃ / %', min: 0, max: 100, splitLine: { lineStyle: { color: '#dfebdd' } } },
      {
        type: 'value',
        name: 'CO2 ppm',
        position: 'right',
        nameGap: 24,
        min: value => Math.max(0, Math.floor(value.min / 100) * 100 - 100),
        max: value => Math.ceil(value.max / 100) * 100 + 100,
        splitLine: { show: false },
        axisLabel: { align: 'center', margin: 22, width: 48 },
        nameTextStyle: { align: 'center', padding: [0, 0, 0, 46] }
      },
      {
        type: 'value',
        name: 'pH',
        position: 'right',
        offset: 58,
        nameGap: 24,
        min: 4,
        max: 9,
        splitLine: { show: false },
        axisLabel: { align: 'center', margin: 12, width: 32 },
        nameTextStyle: { align: 'center', padding: [0, 0, 0, 28] }
      },
      {
        type: 'value',
        name: 'lx',
        position: 'right',
        offset: 116,
        nameGap: 24,
        splitLine: { show: false },
        axisLabel: { align: 'center', margin: 12, width: 48 },
        nameTextStyle: { align: 'center', padding: [0, 0, 0, 36] }
      }
    ],
    series: trendSeries(trend)
  })

  const deviceRows = normalizeSeries(analytics.value.deviceStatus)
  chart(deviceRef.value).setOption({
    tooltip: {},
    grid: { left: 42, right: 16, top: 28, bottom: 34 },
    xAxis: { type: 'category', data: deviceRows.map(item => item.name) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'bar', data: deviceRows.map(item => item.value), itemStyle: { color: params => [chartPalette.green, chartPalette.sage, chartPalette.amber][params.dataIndex] || chartPalette.green, borderRadius: [6, 6, 0, 0] } }]
  })

  const alertRows = normalizeSeries(analytics.value.alertLevel)
  chart(alertRef.value).setOption({
    color: [chartPalette.blue, chartPalette.amber, chartPalette.coral],
    tooltip: { trigger: 'axis' },
    grid: { left: 42, right: 16, top: 28, bottom: 34 },
    xAxis: { type: 'category', data: alertRows.map(item => item.name) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'bar', data: alertRows.map(item => item.value), itemStyle: { color: params => [chartPalette.blue, chartPalette.amber, chartPalette.coral][params.dataIndex], borderRadius: [6, 6, 0, 0] } }]
  })

  chart(areaRef.value).setOption({
    color: [chartPalette.green, chartPalette.gold, chartPalette.teal, chartPalette.leaf, chartPalette.moss, chartPalette.sage],
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      roseType: 'area',
      radius: [20, 100],
      label: { color: '#516157' },
      labelLine: { lineStyle: { color: '#aac6a7' } },
      itemStyle: { borderColor: 'rgba(255,255,255,.82)', borderWidth: 2 },
      data: normalizeSeries(analytics.value.greenhouseAreas)
    }]
  })
}

const loadAnalytics = async () => {
  if (!greenhouseId.value) return
  loading.value = true
  try {
    analytics.value = await fetchAnalytics(greenhouseId.value, trendRangeHours.value)
    await renderCharts()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  greenhouses.value = await fetchGreenhouses()
  greenhouseId.value = greenhouses.value[0]?.id || null
  await loadAnalytics()
  resizeHandler = () => charts.forEach(item => item.resize())
  window.addEventListener('resize', resizeHandler)
  refreshTimer = window.setInterval(loadAnalytics, 60 * 1000)
})

onBeforeUnmount(() => {
  charts.splice(0).forEach(item => item.dispose())
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
  if (refreshTimer) window.clearInterval(refreshTimer)
})
</script>

<style scoped>
.analytics-head,
.chart-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.metric-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 14px;
}
.metric-strip article {
  min-height: 92px;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, .82);
}
.metric-strip span {
  color: var(--muted);
  font-weight: 800;
}
.metric-strip strong {
  display: block;
  margin-top: 10px;
  color: var(--ink);
  font-size: 24px;
}
.analytics-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}
.chart-panel h3 {
  margin: 0 0 12px;
  color: var(--ink);
}
.chart-title h3,
.chart-title p {
  margin: 0;
}
.chart-title p {
  margin-top: 6px;
}
.chart-controls {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}
.chart {
  width: 100%;
  height: 330px;
}
.chart--large {
  height: 470px;
}
@media (max-width: 1100px) {
  .analytics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 760px) {
  .analytics-head,
  .chart-title {
    align-items: stretch;
    flex-direction: column;
  }
  .chart-controls {
    justify-content: flex-start;
  }
  .analytics-grid {
    grid-template-columns: 1fr;
  }
}
</style>
