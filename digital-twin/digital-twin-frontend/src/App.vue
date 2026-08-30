<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import GreenhouseScene from './components/GreenhouseScene.vue'
import { fetchTwinOverview, updateDeviceCommand } from './api/twinApi'
import { mockTwinData } from './data/mockTwinData'

const isLoggedIn = ref(localStorage.getItem('digitalTwinLoggedIn') === '1')
const loginForm = ref({ username: '', password: '', agreed: false })
const loginError = ref('')

const twinData = ref(structuredClone(mockTwinData))
const selected = ref({ type: 'greenhouse', id: 'GH-001' })
const dataSource = ref('MOCK')
const loading = ref(false)
const errorMessage = ref('')
const localDeviceOverrides = ref({})
const infoPage = ref(0)
const selectedAlert = ref(null)

const pageNames = ['告警信息', '设备运行情况', '环境数据']

const selectedGreenhouse = computed(() => {
  if (selected.value.type === 'greenhouse') {
    return twinData.value.greenhouses.find((item) => item.id === selected.value.id)
  }
  if (selected.value.type === 'device') {
    const device = twinData.value.devices.find((item) => item.deviceId === selected.value.id)
    return twinData.value.greenhouses.find((item) => item.id === device?.greenhouseId)
  }
  if (selected.value.type === 'sensor') {
    const sensor = twinData.value.sensors.find((item) => item.deviceId === selected.value.id)
    return twinData.value.greenhouses.find((item) => item.id === sensor?.greenhouseId)
  }
  return twinData.value.greenhouses[0]
})

const currentGreenhouseId = computed(() => selectedGreenhouse.value?.id || twinData.value.greenhouses[0]?.id)

const currentAlerts = computed(() => (
  twinData.value.alerts?.filter((item) => item.greenhouseId === currentGreenhouseId.value) || []
))

const currentDevices = computed(() => (
  twinData.value.devices.filter((item) => item.greenhouseId === currentGreenhouseId.value)
))

const currentSensors = computed(() => (
  twinData.value.sensors.filter((item) => item.greenhouseId === currentGreenhouseId.value)
))

const alarmCount = computed(() => twinData.value.alerts?.filter((alert) => alert.status === 'OPEN').length || 0)

function formatTime(value) {
  if (!value) return '暂无时间'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

function handleLogin() {
  const username = loginForm.value.username.trim()
  if (!username || !loginForm.value.password) {
    loginError.value = '请输入用户名和密码'
    return
  }
  if (!loginForm.value.agreed) {
    loginError.value = '请先勾选已阅读并同意相关条款'
    return
  }
  localStorage.setItem('digitalTwinLoggedIn', '1')
  localStorage.setItem('digitalTwinUsername', username)
  isLoggedIn.value = true
  loginError.value = ''
  startDataRefresh()
}

function handleLogout() {
  localStorage.removeItem('digitalTwinLoggedIn')
  isLoggedIn.value = false
  window.clearInterval(refreshTimer)
}

function applyLocalDeviceOverrides(data) {
  data.devices = data.devices.map((device) => ({
    ...device,
    runningStatus: localDeviceOverrides.value[device.deviceId] || device.runningStatus,
  }))
  return data
}

async function loadTwinData() {
  loading.value = true
  try {
    const remoteData = await fetchTwinOverview()
    twinData.value = applyLocalDeviceOverrides(remoteData)
    dataSource.value = 'MYSQL'
    errorMessage.value = ''
  } catch (error) {
    dataSource.value = 'MOCK'
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

function handleSelect(payload) {
  selected.value = payload
  if (payload.type === 'greenhouse') infoPage.value = 0
}

function prevPage() {
  infoPage.value = (infoPage.value + pageNames.length - 1) % pageNames.length
}

function nextPage() {
  infoPage.value = (infoPage.value + 1) % pageNames.length
}

function openAlert(alert) {
  selectedAlert.value = alert
}

async function toggleDevice(deviceId) {
  const device = twinData.value.devices.find((item) => item.deviceId === deviceId)
  if (!device) return

  const nextStatus = device.runningStatus === 'ON' ? 'OFF' : 'ON'
  localDeviceOverrides.value = {
    ...localDeviceOverrides.value,
    [device.deviceId]: nextStatus,
  }
  device.runningStatus = nextStatus
  twinData.value.updatedAt = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  selected.value = { type: 'device', id: device.deviceId }

  try {
    const result = await updateDeviceCommand(device, nextStatus)
    if (!result?.localOnly) await loadTwinData()
  } catch (error) {
    errorMessage.value = `设备状态已在数字孪生端临时切换，但写入数据库失败：${error.message}`
  }
}

let refreshTimer

function startDataRefresh() {
  window.clearInterval(refreshTimer)
  loadTwinData()
  refreshTimer = window.setInterval(loadTwinData, 5000)
}

onMounted(() => {
  if (isLoggedIn.value) startDataRefresh()
})

onBeforeUnmount(() => window.clearInterval(refreshTimer))
</script>

<template>
  <main v-if="!isLoggedIn" class="login-shell">
    <section class="login-copy">
      <p>智慧农业 · IoT · AI 调控</p>
      <h1>菌境智生：羊肚菌智慧种植调控系统</h1>
      <span>面向管理员和农户的大棚环境监测、设备管理、告警闭环与生产溯源平台。</span>
    </section>

    <form class="login-card" @submit.prevent="handleLogin">
      <h2>账号登录</h2>
      <input v-model="loginForm.username" type="text" placeholder="用户名，如 admin1 / farmer001" autocomplete="username">
      <input v-model="loginForm.password" type="password" placeholder="密码" autocomplete="current-password">
      <label class="agreement-row">
        <input v-model="loginForm.agreed" type="checkbox">
        <span>我已阅读并同意 <b>隐私政策</b> 和 <b>服务条款</b></span>
      </label>
      <p v-if="loginError" class="login-error">{{ loginError }}</p>
      <button class="login-submit" type="submit">进入系统</button>
      <button class="forgot-button" type="button">忘记密码?</button>
    </form>
  </main>

  <main v-else class="dashboard-shell">
    <header class="topbar">
      <div class="brand-block">
        <div class="title-line">
          <h1>智慧羊肚菌大棚 · 数字孪生中心</h1>
          <span class="title-badge">SMART MOREL GREENHOUSE</span>
        </div>
      </div>
      <div class="topbar-actions">
        <div :class="['connection-chip', dataSource.toLowerCase()]">
          <span></span>
          {{ dataSource === 'MYSQL' ? 'MySQL 数据库已连接' : '模拟数据运行中' }}
        </div>
        <button class="logout-button" type="button" @click="handleLogout">退出登录</button>
      </div>
    </header>

    <p v-if="errorMessage" class="error-banner">
      {{ errorMessage }}。请确认后端已启动，并且 MySQL 数据库连接正常。
    </p>

    <section class="info-carousel">
      <button class="page-arrow" @click="prevPage">‹</button>
      <article class="info-panel">
        <div class="info-header">
          <div>
            <h2>{{ pageNames[infoPage] }}</h2>
          </div>
          <div class="info-tools">
            <p class="panel-kicker greenhouse-name">{{ selectedGreenhouse?.name || '大棚总览' }}</p>
            <div class="page-dots">
              <button
                v-for="(_, index) in pageNames"
                :key="index"
                :class="{ active: infoPage === index }"
                @click="infoPage = index"
              />
            </div>
          </div>
        </div>

        <div v-if="infoPage === 0" class="info-list alarm-list">
          <div v-if="!currentAlerts.length" class="empty-text">当前大棚暂无告警。</div>
          <button
            v-for="alert in currentAlerts.slice(0, 4)"
            :key="alert.id"
            class="info-item alert-card"
            type="button"
            @click="openAlert(alert)"
          >
            <strong>{{ alert.title || '未命名告警' }}</strong>
            <span>{{ alert.level }} · {{ alert.status }}</span>
            <p>{{ alert.description || '暂无告警描述' }}</p>
          </button>
        </div>

        <div v-else-if="infoPage === 1" class="info-list device-summary">
          <div v-if="!currentDevices.length" class="empty-text">当前大棚暂无接入设备。</div>
          <button
            v-for="device in currentDevices"
            :key="device.deviceId"
            :class="['device-control-card', device.runningStatus.toLowerCase()]"
            @click="toggleDevice(device.deviceId)"
          >
            <span>
              <strong>{{ device.name }}</strong>
              <small>{{ device.location || device.deviceId }}</small>
            </span>
            <b>{{ device.runningStatus === 'ON' ? '运行中' : '已关闭' }}</b>
          </button>
        </div>

        <div v-else class="env-grid">
          <div v-if="!currentSensors.length" class="empty-text">当前大棚暂无环境数据。</div>
          <div v-for="sensor in currentSensors" :key="sensor.deviceId" class="env-item">
            <span>{{ sensor.name }}</span>
            <strong>{{ sensor.value }}<small>{{ sensor.unit }}</small></strong>
            <em :class="sensor.status.toLowerCase()">{{ sensor.status === 'NORMAL' ? '正常' : '预警' }}</em>
          </div>
        </div>
      </article>
      <button class="page-arrow" @click="nextPage">›</button>
    </section>

    <section class="content-grid">
      <article class="scene-panel">
        <div class="panel-heading scene-heading">
          <div>
            <p class="panel-kicker scene-title">大棚实时孪生场景</p>
          </div>
          <div class="scene-actions">
            <span class="sync-text">{{ loading ? '正在同步数据...' : twinData.updatedAt }}</span>
          </div>
        </div>
        <GreenhouseScene :twin-data="twinData" :selected="selected" @entity-select="handleSelect" />
      </article>
    </section>

    <div v-if="selectedAlert" class="modal-backdrop" @click.self="selectedAlert = null">
      <article class="alert-modal">
        <header>
          <div>
            <p>告警详情</p>
            <h2>{{ selectedAlert.title || '未命名告警' }}</h2>
          </div>
          <button type="button" @click="selectedAlert = null">×</button>
        </header>
        <dl>
          <div>
            <dt>所属大棚</dt>
            <dd>{{ selectedGreenhouse?.name || selectedAlert.greenhouseId }}</dd>
          </div>
          <div>
            <dt>告警级别</dt>
            <dd>{{ selectedAlert.level || '未知' }}</dd>
          </div>
          <div>
            <dt>处理状态</dt>
            <dd>{{ selectedAlert.status || '未知' }}</dd>
          </div>
          <div>
            <dt>发生时间</dt>
            <dd>{{ formatTime(selectedAlert.occurredAt) }}</dd>
          </div>
        </dl>
        <section>
          <h3>完整描述</h3>
          <p>{{ selectedAlert.description || '暂无告警描述' }}</p>
        </section>
      </article>
    </div>
  </main>
</template>
