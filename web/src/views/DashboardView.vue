<template>
  <div class="page admin-dashboard" v-loading="loading">
    <section class="hero-panel">
      <div>
        <span class="eyebrow">管理员工作台</span>
        <h2>管理总览</h2>
      </div>
      <div class="query-box">
        <el-select v-model="farmerId" clearable placeholder="选择农户" style="width: 220px" @change="onFarmerChange">
          <el-option v-for="item in farmers" :key="item.id" :label="userLabel(item)" :value="item.id" />
        </el-select>
        <el-select v-model="greenhouseId" clearable placeholder="选择该农户的大棚" style="width: 240px" :disabled="!farmerId" @change="loadOverview">
          <el-option v-for="item in farmerGreenhouses" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-button type="primary" :disabled="!greenhouseId" @click="loadOverview">查询</el-button>
      </div>
    </section>

    <div class="admin-summary">
      <button class="summary-card" type="button" @click="go('/users')">
        <span>农户账号</span>
        <strong>{{ farmers.length }}</strong>
        <small>进入农户管理和大棚分配</small>
      </button>
      <button class="summary-card" type="button" @click="go('/devices')">
        <span>当前设备</span>
        <strong>{{ overview.devices.length }}</strong>
        <small>{{ selectedGreenhouse?.name || '请先查询大棚' }}</small>
      </button>
      <button class="summary-card danger" type="button" @click="go('/alerts', { status: 'OPEN' })">
        <span>未处理告警</span>
        <strong>{{ overview.activeAlerts.length }}</strong>
        <small>点击进入告警中心</small>
      </button>
      <button class="summary-card trace-card" type="button" @click="go('/traceability')">
        <span>批次溯源</span>
        <strong>{{ overview.productionSummary?.batchCount || 0 }}</strong>
        <small>查看生产批次时间链</small>
      </button>
    </div>

    <section class="panel env-panel" :class="{ 'env-panel--active': queried }">
      <div class="panel-head">
        <div>
          <h2 class="section-title">当前大棚环境态势</h2>
          <p class="muted">{{ queried ? `当前数据来自 ${selectedGreenhouse?.name}` : '选择农户和大棚后展示实时环境数据。' }}</p>
        </div>
        <el-tag v-if="queried" type="success" effect="plain">{{ selectedGreenhouse?.cropStage || '生产监测' }}</el-tag>
      </div>

      <el-empty v-if="!queried" description="尚未查询大棚" />
      <div v-else class="env-content">
        <div class="env-data-grid">
          <div class="env-card">
            <span>空气温度</span>
            <strong>{{ telemetry.airTemperature ?? '-' }} ℃</strong>
          </div>
          <div class="env-card">
            <span>空气湿度</span>
            <strong>{{ telemetry.airHumidity ?? '-' }} %</strong>
          </div>
          <div class="env-card">
            <span>土壤温度</span>
            <strong>{{ telemetry.soilTemperature ?? '-' }} ℃</strong>
          </div>
          <div class="env-card">
            <span>土壤湿度</span>
            <strong>{{ telemetry.soilHumidity ?? '-' }} %</strong>
          </div>
          <div class="env-card">
            <span>pH 值</span>
            <strong>{{ telemetry.phValue ?? '-' }}</strong>
          </div>
          <div class="env-card">
            <span>二氧化碳</span>
            <strong>{{ telemetry.co2Ppm ?? '-' }} ppm</strong>
          </div>
          <div class="env-card">
            <span>光照强度</span>
            <strong>{{ telemetry.lightLux ?? '-' }} lx</strong>
          </div>
        </div>
        <div class="camera-card">
          <button
            class="camera-card__screen"
            :class="{ 'camera-card__screen--playing': cameraPlaying }"
            type="button"
            @click="playCamera"
          >
            <video
              ref="cameraVideoRef"
              class="camera-card__video"
              src="/media/a01-camera-demo.mp4?v=20260703"
              poster="/media/a01-camera-poster.jpg?v=20260703"
              muted
              loop
              playsinline
              preload="auto"
              @play="cameraPlaying = true"
              @pause="cameraPlaying = false"
            ></video>
            <template v-if="!cameraPlaying">
              <span>实时摄像头</span>
              <strong>{{ selectedGreenhouse?.name }}</strong>
              <small>当前为模拟画面，点击播放实时监控。</small>
            </template>
          </button>
        </div>
      </div>
    </section>

    <div class="split-grid dashboard-bottom">
      <section class="panel">
        <div class="panel-head compact">
          <h2 class="section-title">设备简况</h2>
          <el-button class="panel-more" @click="go('/devices')">查看设备</el-button>
        </div>
        <el-table :data="overview.devices" style="width: 100%; margin-top: 12px">
          <el-table-column prop="name" label="设备名称" min-width="180" />
          <el-table-column prop="category" label="类别" width="130" />
          <el-table-column prop="location" label="位置" min-width="150" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }"><el-tag>{{ deviceStatus(row.status) }}</el-tag></template>
          </el-table-column>
        </el-table>
      </section>
      <section class="panel">
        <div class="panel-head compact">
          <h2 class="section-title">当前大棚告警</h2>
          <el-button class="panel-more" @click="go('/alerts', { status: 'OPEN' })">查看告警</el-button>
        </div>
        <div class="alert-list">
          <article
            v-for="alert in latestActiveAlerts"
            :key="alert.id"
            class="alert-list__item"
            role="button"
            tabindex="0"
            @click="goAlert(alert)"
            @keydown.enter.prevent="goAlert(alert)"
            @keydown.space.prevent="goAlert(alert)"
          >
            <el-tag :type="alert.level === 'CRITICAL' ? 'danger' : 'warning'">{{ alertLevel(alert.level) }}</el-tag>
            <div><strong>{{ alert.title }}</strong><p>{{ alert.description }}</p></div>
          </article>
          <el-empty v-if="!overview.activeAlerts.length" description="当前没有未处理告警" />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchOverview } from '../services/greenhouse'
import { fetchFarmerGreenhouses, fetchUsers } from '../services/user'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const users = ref([])
const farmerId = ref(route.query.farmerId ? Number(route.query.farmerId) : Number(sessionStorage.getItem('admin_filter_farmer')) || null)
const greenhouseId = ref(route.query.greenhouseId ? Number(route.query.greenhouseId) : Number(sessionStorage.getItem('admin_filter_greenhouse')) || null)
const farmerGreenhouses = ref([])
const queried = ref(false)
const overview = ref({ greenhouses: [], devices: [], activeAlerts: [], currentTelemetry: {} })
const cameraVideoRef = ref(null)
const cameraPlaying = ref(false)

const farmers = computed(() => users.value.filter(item => item.role_code === 'FARMER'))
const telemetry = computed(() => overview.value.currentTelemetry || {})
const selectedGreenhouse = computed(() => farmerGreenhouses.value.find(item => Number(item.id) === Number(greenhouseId.value)))
const latestActiveAlerts = computed(() => sortByAlertTime(overview.value.activeAlerts).slice(0, 4))
const userLabel = item => `${item.display_name || item.username}（${item.username}）`
const deviceStatus = status => ({ RUNNING: '运行中', STOPPED: '已停止', MAINTENANCE: '维护中' }[status] || status)
const alertLevel = level => ({ CRITICAL: '严重', WARNING: '警告', INFO: '提示' }[level] || level)
const sortByAlertTime = alerts => [...(alerts || [])].sort((a, b) => new Date(b.occurredAt || b.createdAt || 0) - new Date(a.occurredAt || a.createdAt || 0))

const currentQuery = extra => ({
  ...(farmerId.value ? { farmerId: farmerId.value } : {}),
  ...(greenhouseId.value ? { greenhouseId: greenhouseId.value } : {}),
  ...extra
})

const persistQuery = () => {
  if (farmerId.value) sessionStorage.setItem('admin_filter_farmer', farmerId.value)
  else sessionStorage.removeItem('admin_filter_farmer')
  if (greenhouseId.value) sessionStorage.setItem('admin_filter_greenhouse', greenhouseId.value)
  else sessionStorage.removeItem('admin_filter_greenhouse')
  router.replace({ path: '/', query: currentQuery() })
}

const go = (path, extra = {}) => router.push({ path, query: currentQuery(extra) })
const goAlert = alert => go('/alerts', {
  status: 'OPEN',
  greenhouseId: alert.greenhouseId || greenhouseId.value
})

const playCamera = async () => {
  if (!cameraVideoRef.value || cameraPlaying.value) return
  await cameraVideoRef.value.play()
}

const onFarmerChange = async () => {
  greenhouseId.value = null
  farmerGreenhouses.value = farmerId.value ? await fetchFarmerGreenhouses(farmerId.value) : []
  overview.value = { greenhouses: [], devices: [], activeAlerts: [], currentTelemetry: {} }
  queried.value = false
  persistQuery()
}

const loadFarmerGreenhouses = async () => {
  farmerGreenhouses.value = farmerId.value ? await fetchFarmerGreenhouses(farmerId.value) : []
  if (greenhouseId.value && !farmerGreenhouses.value.some(item => Number(item.id) === Number(greenhouseId.value))) {
    greenhouseId.value = null
  }
}

const loadOverview = async () => {
  if (!greenhouseId.value) return
  loading.value = true
  try {
    overview.value = await fetchOverview(greenhouseId.value)
    queried.value = true
    persistQuery()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  users.value = await fetchUsers()
  await loadFarmerGreenhouses()
  if (greenhouseId.value) await loadOverview()
})

watch(() => route.query.greenhouseId, async value => {
  if (value && Number(value) !== Number(greenhouseId.value)) {
    greenhouseId.value = Number(value)
    await loadOverview()
  }
})
</script>

<style scoped>
.hero-panel,
.query-box,
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.hero-panel {
  padding: 26px 28px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: var(--shadow);
}

.query-box {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.eyebrow {
  color: var(--brand-strong);
  font-weight: 900;
}

.hero-panel h2 {
  margin: 8px 0 0;
  color: var(--ink);
  font-size: 34px;
}

.admin-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  min-height: 112px;
  padding: 18px 20px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.94), rgba(244, 252, 239, 0.86)),
    radial-gradient(circle at 92% 8%, rgba(83, 184, 106, 0.12), transparent 34%);
  text-align: left;
  cursor: pointer;
  box-shadow: 0 12px 28px rgba(42, 91, 48, 0.08);
  transition: transform 160ms ease, border-color 160ms ease, box-shadow 160ms ease;
}

.summary-card:hover {
  transform: translateY(-2px);
  border-color: rgba(83, 184, 106, 0.42);
  box-shadow: 0 18px 38px rgba(42, 91, 48, 0.12);
}

.summary-card span,
.env-card span {
  color: var(--muted);
  font-size: 14px;
  font-weight: 800;
}

.summary-card strong,
.env-card strong {
  display: block;
  margin-top: 12px;
  color: var(--ink);
  font-size: 30px;
  line-height: 1.12;
}

.summary-card small {
  display: block;
  margin-top: 8px;
  color: var(--ink);
  font-size: 13px;
  font-weight: 700;
}

.summary-card.danger strong {
  color: var(--danger);
}

.env-panel {
  transition: transform 180ms ease, box-shadow 180ms ease;
}

.env-panel--active {
  box-shadow: 0 22px 58px rgba(42, 91, 48, 0.16);
}

.env-content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  align-items: stretch;
  gap: 18px;
  margin-top: 18px;
}

.env-data-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.env-card {
  min-height: 124px;
  padding: 18px;
  border: 1px solid rgba(73, 125, 78, 0.16);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 10px 24px rgba(42, 91, 48, 0.06);
  transition: transform 160ms ease, border-color 160ms ease, box-shadow 160ms ease;
}

.env-card:hover {
  transform: translateY(-2px);
  border-color: rgba(83, 184, 106, 0.44);
  box-shadow: 0 16px 34px rgba(42, 91, 48, 0.12);
}

.camera-card {
  min-height: 262px;
  border: 1px solid rgba(73, 125, 78, 0.24);
  border-radius: var(--radius);
  overflow: hidden;
  background: #10261b;
}

.camera-card__screen {
  position: relative;
  isolation: isolate;
  display: flex;
  width: 100%;
  min-height: 262px;
  height: 100%;
  flex-direction: column;
  justify-content: flex-end;
  padding: 20px;
  border: 0;
  color: #fff;
  background:
    linear-gradient(180deg, rgba(10, 30, 18, 0.08), rgba(10, 30, 18, 0.78)),
    linear-gradient(90deg, rgba(10, 30, 18, 0.34), transparent 58%),
    url("/media/a01-camera-poster.jpg?v=20260703") center / cover;
  text-align: left;
  cursor: pointer;
}

.camera-card__screen::after {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  content: "";
  background:
    linear-gradient(180deg, rgba(10, 30, 18, 0.06), rgba(10, 30, 18, 0.82)),
    linear-gradient(90deg, rgba(10, 30, 18, 0.22), transparent 42%);
}

.camera-card__screen--playing {
  padding: 0;
  cursor: default;
}

.camera-card__screen--playing::after {
  display: none;
}

.camera-card__video {
  position: absolute;
  inset: 0;
  z-index: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
}

.camera-card__screen--playing .camera-card__video {
  opacity: 1;
}

.camera-card__screen span {
  position: relative;
  z-index: 2;
  color: #b8f4bf;
  font-weight: 900;
}

.camera-card__screen strong {
  position: relative;
  z-index: 2;
  margin-top: 8px;
  font-size: 22px;
}

.camera-card__screen small {
  position: relative;
  z-index: 2;
  margin-top: 8px;
  color: rgba(255, 255, 255, 0.72);
  font-weight: 700;
}

.dashboard-bottom {
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, 0.9fr);
}

.panel-head.compact {
  align-items: center;
}

.panel-more {
  height: 32px;
  padding: 0 14px;
  border: 1px solid rgba(73, 125, 78, 0.16);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--brand-strong);
  font-size: 13px;
  font-weight: 800;
  box-shadow: none;
}

.alert-list {
  display: grid;
  gap: 12px;
  margin-top: 12px;
}

.alert-list article {
  display: flex;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.7);
}

.alert-list__item {
  cursor: pointer;
  transition: transform 160ms ease, border-color 160ms ease, box-shadow 160ms ease;
}

.alert-list__item:hover,
.alert-list__item:focus-visible {
  transform: translateY(-2px);
  border-color: rgba(83, 184, 106, 0.4);
  box-shadow: 0 14px 30px rgba(42, 91, 48, 0.12);
  outline: none;
}

.alert-list p {
  margin: 6px 0 0;
  color: var(--muted);
}

@media (max-width: 1280px) {
  .env-content {
    grid-template-columns: 1fr;
  }

  .env-data-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .camera-card,
  .camera-card__screen {
    min-height: 220px;
  }
}

@media (max-width: 1100px) {
  .hero-panel,
  .panel-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .admin-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-bottom {
    grid-template-columns: 1fr;
  }

  .env-data-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
