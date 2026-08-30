<template>
  <div class="page" v-loading="loading">
    <section class="farmer-hero panel">
      <div>
        <span class="eyebrow">农户 Web 工作台</span>
        <h2>{{ displayName }} 的大棚看护台</h2>
        <p>查看已绑定大棚的环境指标、设备状态、待处理告警和今日巡检建议。</p>
      </div>
      <div class="hero-actions">
        <div class="clock-card">
          <span>{{ currentDate }}</span>
          <strong>{{ currentTime }}</strong>
        </div>
        <el-select v-if="overview.greenhouses.length" v-model="greenhouseId" style="width: 240px" @change="load">
          <el-option v-for="item in overview.greenhouses" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-button class="ghost-action" @click="openGreenhouseDialog">添加大棚</el-button>
        <el-button type="primary" @click="$router.push('/profile')">完善资料</el-button>
      </div>
    </section>

    <section v-if="isEmptyFarmer" class="panel empty-workbench">
      <div>
        <span class="eyebrow">可以先创建自己的大棚</span>
        <h3>当前账号还没有大棚或设备数据</h3>
        <p>你可以自己添加大棚，系统会自动绑定到当前账号。设备、批次和告警数据可以后续继续补充。</p>
      </div>
      <el-button type="primary" @click="openGreenhouseDialog">添加大棚</el-button>
    </section>

    <template v-else>
      <div class="action-grid">
        <button type="button" @click="$router.push('/devices')">
          <span>
            <b>设备管理</b>
          </span>
          <strong>{{ deviceCount }}<small>台设备</small></strong>
        </button>
        <button type="button" @click="$router.push('/alerts')">
          <span>
            <b>告警处理</b>
          </span>
          <strong>{{ unresolvedAlertCount }}<small>条待处理</small></strong>
        </button>
        <button type="button" @click="$router.push('/analytics')">
          <span>
            <b>数据分析</b>
          </span>
          <strong>{{ telemetryMetricCount }}<small>项指标</small></strong>
        </button>
        <button type="button" @click="$router.push('/traceability')">
          <span>
            <b>批次溯源</b>
          </span>
          <strong>{{ batchCount }}<small>个批次</small></strong>
        </button>
      </div>

      <section class="panel telemetry-panel" :class="{ expanded: telemetryExpanded }">
        <button class="telemetry-toggle" type="button" @click="telemetryExpanded = !telemetryExpanded">
          <div>
            <h2 class="section-title">环境实时数据</h2>
            <p>空气、土壤、pH、CO2 与光照</p>
          </div>
          <span class="toggle-icon" :class="{ open: telemetryExpanded }">⌃</span>
        </button>
        <div v-if="telemetryExpanded" class="metric-grid farmer-metrics">
          <button class="metric-card" type="button" @click="$router.push('/analytics')">
            <span>空气温湿度</span>
            <strong>{{ telemetry.airTemperature ?? '-' }} ℃ / {{ telemetry.airHumidity ?? '-' }} %</strong>
            <small>{{ temperatureAdvice }} · {{ humidityAdvice }}</small>
          </button>
          <button class="metric-card" type="button" @click="$router.push('/analytics')">
            <span>土壤温湿度</span>
            <strong>{{ telemetry.soilTemperature ?? '-' }} ℃ / {{ telemetry.soilHumidity ?? '-' }} %</strong>
            <small>{{ soilAdvice }}</small>
          </button>
          <button class="metric-card" type="button" @click="$router.push('/analytics')">
            <span>pH 值</span>
            <strong>{{ telemetry.phValue ?? '-' }}</strong>
            <small>{{ phAdvice }}</small>
          </button>
          <button class="metric-card" type="button" @click="$router.push('/analytics')">
            <span>二氧化碳</span>
            <strong>{{ telemetry.co2Ppm ?? '-' }} ppm</strong>
            <small>{{ co2Advice }}</small>
          </button>
          <button class="metric-card" type="button" @click="$router.push('/analytics')">
            <span>光照强度</span>
            <strong>{{ telemetry.lightLux ?? '-' }} lx</strong>
            <small>{{ lightAdvice }}</small>
          </button>
        </div>
      </section>

      <div class="split-grid">
        <section class="panel">
          <div class="panel-head compact">
            <h2 class="section-title">今日巡检建议</h2>
            <el-tag type="success">{{ selectedGreenhouse?.cropStage || '生产期' }}</el-tag>
          </div>
          <div class="task-list">
            <article v-for="(task, index) in dailyTasks" :key="task.title">
              <i>{{ index + 1 }}</i>
              <div>
                <strong>{{ task.title }}</strong>
                <p>{{ task.detail }}</p>
              </div>
            </article>
          </div>
        </section>

        <section class="panel">
          <div class="panel-head compact">
            <h2 class="section-title">待关注告警</h2>
            <el-button class="panel-more" @click="$router.push('/alerts')">查看全部</el-button>
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
              <el-tag :type="alert.level === 'CRITICAL' ? 'danger' : 'warning'" size="small">{{ levelText(alert.level) }}</el-tag>
              <div>
                <strong>{{ alert.title }}</strong>
                <p>{{ alert.description }}</p>
              </div>
            </article>
            <el-empty v-if="!overview.activeAlerts?.length" description="当前没有待关注告警" />
          </div>
        </section>
      </div>

      <section class="panel">
        <div class="panel-head compact">
          <h2 class="section-title">我的设备</h2>
          <el-button class="panel-more" @click="$router.push('/devices')">维护设备</el-button>
        </div>
        <div class="home-device-grid">
          <article v-for="device in overview.devices" :key="device.id" class="home-device-card">
            <div>
              <strong>{{ device.name }}</strong>
              <p>{{ device.location || '未填写安装位置' }}</p>
            </div>
            <span>{{ device.category || '智能设备' }}</span>
            <el-tag :type="deviceTag(device.status)" size="small">{{ statusText(device.status) }}</el-tag>
          </article>
          <el-empty v-if="!overview.devices?.length" description="当前大棚暂无设备" />
        </div>
      </section>
    </template>

    <el-dialog v-model="greenhouseDialog" title="添加大棚" width="560px">
      <el-form label-position="top">
        <el-form-item label="大棚名称"><el-input v-model.trim="greenhouseForm.name" /></el-form-item>
        <el-form-item label="位置"><el-input v-model.trim="greenhouseForm.location" /></el-form-item>
        <el-form-item label="面积（平方米）"><el-input-number v-model="greenhouseForm.area" :min="1" style="width: 100%" /></el-form-item>
        <el-form-item label="生产阶段"><el-input v-model.trim="greenhouseForm.cropStage" /></el-form-item>
        <div class="dialog-actions">
          <el-button @click="greenhouseDialog = false">取消</el-button>
          <el-button type="primary" :loading="savingGreenhouse" @click="submitGreenhouse">保存</el-button>
        </div>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createGreenhouse, fetchOverview } from '../services/greenhouse'
import { useSessionStore } from '../stores/session'

const session = useSessionStore()
const router = useRouter()
const loading = ref(false)
const savingGreenhouse = ref(false)
const greenhouseDialog = ref(false)
const greenhouseId = ref(null)
const telemetryExpanded = ref(false)
const now = ref(new Date())
let clockTimer = null
let telemetryTimer = null
const greenhouseForm = reactive({ name: '', location: '', area: 300, cropStage: '出菇期' })
const overview = ref({ greenhouses: [], devices: [], activeAlerts: [], currentTelemetry: {} })

const displayName = computed(() => session.profile?.username || '农户')
const currentDate = computed(() => now.value.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', weekday: 'short' }))
const currentTime = computed(() => now.value.toLocaleTimeString('zh-CN', { hour12: false }))
const isEmptyFarmer = computed(() => !overview.value.greenhouses.length)
const telemetry = computed(() => overview.value.currentTelemetry || {})
const selectedGreenhouse = computed(() => overview.value.greenhouses.find(item => item.id === greenhouseId.value))
const deviceCount = computed(() => overview.value.devices?.length || 0)
const unresolvedAlertCount = computed(() => overview.value.productionSummary?.unresolvedAlertCount ?? overview.value.activeAlerts?.length ?? 0)
const batchCount = computed(() => overview.value.productionSummary?.batchCount || 0)
const telemetryMetricCount = computed(() => 7)
const latestActiveAlerts = computed(() => sortByAlertTime(overview.value.activeAlerts).slice(0, 4))

const statusText = status => ({ RUNNING: '运行中', STOPPED: '已停止', MAINTENANCE: '维护中' }[status] || status)
const deviceTag = status => status === 'RUNNING' ? 'success' : status === 'MAINTENANCE' ? 'warning' : 'info'
const levelText = level => ({ CRITICAL: '严重', WARNING: '警告', INFO: '提示' }[level] || level)
const sortByAlertTime = alerts => [...(alerts || [])].sort((a, b) => new Date(b.occurredAt || b.createdAt || 0) - new Date(a.occurredAt || a.createdAt || 0))
const goAlert = alert => {
  const query = {
    status: 'OPEN',
    ...(alert.greenhouseId || greenhouseId.value ? { greenhouseId: alert.greenhouseId || greenhouseId.value } : {})
  }
  router.push({ path: '/alerts', query })
}

const temperatureAdvice = computed(() => {
  const value = telemetry.value.airTemperature
  if (value == null) return '等待采集'
  if (value < 16) return '温度偏低，注意保温'
  if (value > 24) return '温度偏高，建议通风'
  return '适合羊肚菌生长'
})

const humidityAdvice = computed(() => {
  const value = telemetry.value.airHumidity
  if (value == null) return '等待采集'
  if (value < 70) return '湿度偏低，注意补湿'
  if (value > 92) return '湿度偏高，留意病害风险'
  return '湿度处于合理区间'
})

const co2Advice = computed(() => {
  const value = telemetry.value.co2Ppm
  if (value == null) return '等待采集'
  if (value > 1200) return '浓度偏高，建议通风'
  if (value < 450) return '浓度偏低，关注通风策略'
  return '浓度正常'
})

const soilAdvice = computed(() => {
  const humidity = telemetry.value.soilHumidity
  if (humidity == null) return '等待采集'
  if (humidity < 55) return '土壤偏干，注意补水'
  if (humidity > 75) return '土壤偏湿，注意通风'
  return '土壤水分适宜'
})

const phAdvice = computed(() => {
  const value = telemetry.value.phValue
  if (value == null) return '等待采集'
  if (value < 6) return '偏酸，建议复核基质'
  if (value > 7.2) return '偏碱，建议复核基质'
  return '酸碱度适宜'
})

const lightAdvice = computed(() => {
  const value = telemetry.value.lightLux
  if (value == null) return '等待采集'
  if (value < 2000) return '光照偏低'
  if (value > 6000) return '光照偏强'
  return '光照适宜'
})

const dailyTasks = computed(() => [
  { title: '检查棚内空气湿度和地表状态', detail: humidityAdvice.value },
  { title: '巡看通风和加湿设备', detail: `${overview.value.devices.filter(item => item.status === 'RUNNING').length} 台设备运行中，维护设备需优先确认。` },
  { title: '记录生产阶段观察情况', detail: selectedGreenhouse.value?.cropStage ? `当前阶段：${selectedGreenhouse.value.cropStage}` : '请补充当前生产阶段。' }
])

const openGreenhouseDialog = () => {
  Object.assign(greenhouseForm, { name: '', location: '', area: 300, cropStage: '出菇期' })
  greenhouseDialog.value = true
}

const submitGreenhouse = async () => {
  if (!greenhouseForm.name) return ElMessage.warning('请填写大棚名称')
  savingGreenhouse.value = true
  try {
    await createGreenhouse({ ...greenhouseForm })
    ElMessage.success('大棚已创建并自动绑定到当前账号')
    greenhouseDialog.value = false
    await load()
    greenhouseId.value = overview.value.greenhouses.at(-1)?.id || greenhouseId.value
  } finally {
    savingGreenhouse.value = false
  }
}

const load = async () => {
  loading.value = true
  try {
    overview.value = await fetchOverview(greenhouseId.value)
    greenhouseId.value = overview.value.currentTelemetry?.greenhouseId || greenhouseId.value || overview.value.greenhouses[0]?.id || null
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load()
  clockTimer = window.setInterval(() => {
    now.value = new Date()
  }, 1000)
  telemetryTimer = window.setInterval(load, 60 * 1000)
})

onBeforeUnmount(() => {
  if (clockTimer) {
    window.clearInterval(clockTimer)
  }
  if (telemetryTimer) {
    window.clearInterval(telemetryTimer)
  }
})
</script>

<style scoped>
.farmer-hero,
.empty-workbench,
.panel-head,
.hero-actions,
.dialog-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}
.dialog-actions { justify-content: flex-end; margin-top: 16px; }
.eyebrow { color: var(--brand-strong); font-weight: 900; }
.farmer-hero h2,
.empty-workbench h3 { margin: 8px 0; color: var(--ink); font-size: 30px; }
.farmer-hero p,
.empty-workbench p { max-width: 760px; color: var(--muted); line-height: 1.8; }
.hero-actions :deep(.el-select__wrapper),
.hero-actions :deep(.el-button) {
  min-height: 40px;
  height: 40px;
}
.ghost-action {
  border-color: rgba(73, 125, 78, 0.18);
  background: rgba(255, 255, 255, 0.72);
  color: var(--brand-strong);
}
.ghost-action:hover,
.ghost-action:focus {
  border-color: rgba(83, 184, 106, 0.42);
  background: rgba(83, 184, 106, 0.1);
  color: var(--brand-strong);
}
.empty-workbench {
  min-height: 260px;
  background: radial-gradient(circle at 12% 20%, rgba(83, 184, 106, 0.14), transparent 34%), linear-gradient(145deg, rgba(255, 255, 255, 0.94), rgba(239, 250, 235, 0.86));
}
.clock-card {
  width: max-content;
  min-width: 0;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  text-align: center;
}
.clock-card span {
  display: block;
  color: var(--muted);
  font-size: 13px;
  font-weight: 800;
}
.clock-card strong {
  display: block;
  margin-top: 4px;
  color: var(--ink);
  font-size: 24px;
  line-height: 1;
}
.telemetry-panel {
  padding: 0;
}
.telemetry-toggle {
  width: 100%;
  min-height: 86px;
  padding: 18px 20px;
  border: 0;
  background: transparent;
  color: var(--ink);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  text-align: left;
  cursor: pointer;
}
.telemetry-toggle p {
  margin: 8px 0 0;
  color: var(--muted);
  font-size: 14px;
}
.toggle-icon {
  width: 36px;
  height: 36px;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.92), rgba(241, 250, 237, 0.82));
  color: var(--brand-strong);
  display: grid;
  place-items: center;
  font-size: 18px;
  font-weight: 900;
  line-height: 1;
  box-shadow: 0 8px 18px rgba(42, 91, 48, 0.08);
  transition: transform 160ms ease, border-color 160ms ease, box-shadow 160ms ease;
}
.telemetry-toggle:hover .toggle-icon {
  border-color: rgba(83, 184, 106, 0.36);
  box-shadow: 0 10px 22px rgba(42, 91, 48, 0.12);
}
.toggle-icon.open {
  transform: rotate(180deg);
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
.panel-more:hover {
  border-color: rgba(83, 184, 106, 0.42);
  background: rgba(83, 184, 106, 0.1);
  color: var(--brand-strong);
}
.telemetry-panel .farmer-metrics {
  padding: 0 18px 18px;
}
.metric-card { text-align: left; cursor: pointer; }
.farmer-metrics small { display: block; margin-top: 8px; color: var(--muted); }
.action-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.action-grid button {
  min-height: 104px;
  padding: 18px;
  border: 1px solid rgba(73, 125, 78, 0.14);
  border-radius: var(--radius);
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.9), rgba(244, 252, 239, 0.82)),
    radial-gradient(circle at 90% 8%, rgba(83, 184, 106, 0.14), transparent 34%);
  color: var(--ink);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  text-align: left;
  cursor: pointer;
  box-shadow: 0 12px 30px rgba(42, 91, 48, 0.06);
  transition: transform 160ms ease, border-color 160ms ease, box-shadow 160ms ease;
}
.action-grid button:hover {
  transform: translateY(-2px);
  border-color: rgba(83, 184, 106, 0.38);
  box-shadow: 0 18px 38px rgba(42, 91, 48, 0.1);
}
.action-grid button span {
  display: grid;
  gap: 8px;
  min-width: 0;
}
.action-grid button b {
  color: var(--ink);
  font-size: 18px;
  font-weight: 900;
}
.action-grid button strong {
  color: var(--brand-strong);
  font-size: 38px;
  line-height: 1;
  min-width: 48px;
  display: grid;
  justify-items: end;
  gap: 6px;
  text-align: right;
}
.action-grid button small {
  color: var(--muted);
  font-size: 13px;
  font-weight: 700;
}
.task-list,
.alert-list { display: grid; gap: 12px; }
.task-list {
  margin-top: 14px;
  padding: 10px;
  border: 1px solid rgba(73, 125, 78, 0.12);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.48);
}
.task-list article {
  min-height: 72px;
  padding: 12px 14px;
  border: 0;
  border-radius: var(--radius);
  background: transparent;
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}
.task-list article + article { border-top: 1px solid rgba(73, 125, 78, 0.12); border-radius: 0; }
.task-list i {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 999px;
  background: rgba(83, 184, 106, 0.13);
  color: var(--brand-strong);
  font-style: normal;
  font-weight: 900;
}
.task-list strong { color: var(--ink); font-size: 16px; }
.alert-list article { padding: 14px; border: 1px solid var(--line); border-radius: var(--radius); background: rgba(255, 255, 255, 0.72); }
.alert-list article { display: flex; gap: 12px; }
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
.task-list p,
.alert-list p { margin: 6px 0 0; color: var(--muted); line-height: 1.7; }
.home-device-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}
.home-device-card {
  min-height: 76px;
  padding: 14px 16px;
  border: 1px solid rgba(73, 125, 78, 0.14);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.66);
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 12px;
  align-items: center;
}
.home-device-card strong {
  display: block;
  color: var(--ink);
  font-size: 16px;
}
.home-device-card p {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 13px;
}
.home-device-card > span {
  color: var(--muted);
  font-size: 13px;
  font-weight: 800;
  white-space: nowrap;
}
@media (max-width: 760px) {
  .farmer-hero,
  .empty-workbench,
  .panel-head,
  .hero-actions { flex-direction: column; align-items: stretch; }
  .clock-card { text-align: left; }
  .action-grid { grid-template-columns: 1fr; }
  .home-device-grid { grid-template-columns: 1fr; }
}
</style>
