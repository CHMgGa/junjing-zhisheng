<template>
  <div class="page" v-loading="loading">
    <section class="panel">
      <div class="panel-head">
        <div>
          <h2 class="section-title">设备管理</h2>
          <p class="muted">
            {{ isAdmin ? '管理员新增、编辑和删除设备档案，具体设备操作由农户完成。' : '管理自己绑定大棚中的设备，可新增、编辑和删除设备档案。' }}
          </p>
        </div>
        <div class="head-actions">
          <el-select v-if="isAdmin" v-model="farmerId" clearable placeholder="选择农户" style="width: 220px" @change="onFarmerChange">
            <el-option v-for="item in farmers" :key="item.id" :label="userLabel(item)" :value="item.id" />
          </el-select>
          <el-select v-model="greenhouseId" clearable placeholder="选择大棚" style="width: 250px" @change="onGreenhouseChange">
            <el-option v-for="item in greenhouses" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-button type="primary" :disabled="!greenhouseId" @click="openCreate">新增设备</el-button>
        </div>
      </div>

      <div class="device-grid">
        <article v-for="device in visibleDevices" :key="device.id" class="device-card">
          <div class="device-card__top">
            <span>{{ cleanText(device.category, '智能设备') }}</span>
            <el-tag :type="statusTag(device.status)">{{ statusText(device.status) }}</el-tag>
          </div>
          <h3>{{ cleanText(device.name, '设备信息待完善') }}</h3>
          <p>{{ cleanText(device.location, '未填写安装位置') }}</p>
          <p v-if="device.remark" class="remark">{{ cleanText(device.remark, '') }}</p>
          <div class="device-card__actions">
            <template v-if="isAdmin">
              <el-button size="small" @click="openEdit(device)">编辑档案</el-button>
              <el-button size="small" type="danger" @click="removeDevice(device)">删除档案</el-button>
            </template>
            <template v-else>
              <template v-if="isFanDevice(device)">
                <el-select
                  v-model="fanLevels[device.id]"
                  class="fan-level-select"
                  size="small"
                  :disabled="commandingId === device.id"
                >
                  <el-option v-for="level in fanLevelOptions" :key="level" :label="`${level} 档`" :value="level" />
                </el-select>
                <el-button
                  size="small"
                  type="success"
                  :loading="commandingId === device.id"
                  @click="setFanLevel(device)"
                >
                  下发
                </el-button>
              </template>
              <template v-else>
                <el-button
                  size="small"
                  :type="device.status === 'RUNNING' ? 'warning' : 'success'"
                  :loading="commandingId === device.id"
                  @click="toggleDevice(device)"
                >
                  {{ device.status === 'RUNNING' ? '停止' : '启动' }}
                </el-button>
                <el-button
                  size="small"
                  :disabled="device.status === 'MAINTENANCE'"
                  :loading="commandingId === device.id"
                  @click="commandDevice(device, 'MAINTENANCE')"
                >
                  维护
                </el-button>
              </template>
              <el-button size="small" @click="openEdit(device)">编辑</el-button>
              <el-button size="small" type="danger" @click="removeDevice(device)">删除</el-button>
            </template>
          </div>
        </article>
        <el-empty v-if="!visibleDevices.length" description="当前大棚暂无设备" />
      </div>
    </section>

    <el-dialog v-model="deviceDialog" :title="editingId ? '编辑设备' : '新增设备'" width="580px">
      <el-form label-position="top">
        <el-form-item label="绑定大棚">
          <el-select v-model="deviceForm.greenhouseId" style="width: 100%">
            <el-option v-for="item in greenhouses" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备名称"><el-input v-model.trim="deviceForm.name" /></el-form-item>
        <el-form-item label="设备类别"><el-input v-model.trim="deviceForm.category" placeholder="如：通风、灌溉、环境传感器" /></el-form-item>
        <el-form-item v-if="!isAdmin" label="设备状态">
          <el-select v-model="deviceForm.status" style="width: 100%">
            <el-option label="运行中" value="RUNNING" />
            <el-option label="已停止" value="STOPPED" />
            <el-option label="维护中" value="MAINTENANCE" />
          </el-select>
        </el-form-item>
        <el-form-item label="安装位置"><el-input v-model.trim="deviceForm.location" /></el-form-item>
        <el-form-item label="设备备注"><el-input v-model.trim="deviceForm.remark" type="textarea" :rows="3" /></el-form-item>
        <div class="dialog-actions">
          <el-button @click="deviceDialog = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitDevice">保存</el-button>
        </div>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createDevice, deleteDevice, fetchDevices, fetchGreenhouses, sendDeviceCommand, updateDevice } from '../services/greenhouse'
import { fetchFarmerGreenhouses, fetchUsers } from '../services/user'
import { useSessionStore } from '../stores/session'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const isAdmin = computed(() => session.profile?.role === 'ADMIN')
const loading = ref(false)
const saving = ref(false)
const commandingId = ref(null)
const users = ref([])
const farmerId = ref(route.query.farmerId ? Number(route.query.farmerId) : null)
const greenhouses = ref([])
const greenhouseId = ref(route.query.greenhouseId ? Number(route.query.greenhouseId) : null)
const devices = ref([])
const deviceDialog = ref(false)
const editingId = ref(null)
const deviceForm = reactive({ greenhouseId: null, name: '', category: '', status: 'STOPPED', location: '', remark: '', autoMode: true, healthScore: 100 })
const fanLevels = reactive({})
const fanLevelOptions = Array.from({ length: 10 }, (_, index) => index)

const farmers = computed(() => users.value.filter(item => item.role_code === 'FARMER'))
const visibleDevices = computed(() => devices.value.filter(item => !looksInvalid(item.name) && !looksInvalid(item.category)))
const userLabel = item => `${item.display_name || item.username}（${item.username}）`
const statusText = status => ({ RUNNING: '运行中', STOPPED: '已停止', MAINTENANCE: '维护中' }[status] || status)
const statusTag = status => status === 'RUNNING' ? 'success' : status === 'MAINTENANCE' ? 'warning' : 'info'

const looksInvalid = value => {
  const text = String(value || '').toLowerCase()
  return text.includes('????') || text.startsWith('farmer-device-') || text === 'test'
}
const cleanText = (value, fallback) => {
  if (!value || looksInvalid(value)) return fallback
  return String(value).replace(/CO2/g, 'CO₂')
}
const isFanDevice = device => {
  const text = `${device.name || ''} ${device.category || ''} ${device.typeCode || device.type_code || ''}`.toLowerCase()
  return text.includes('fan') || text.includes('feng') || text.includes('风机') || text.includes('通风')
}

const syncQuery = () => {
  if (!isAdmin.value) return
  router.replace({
    path: '/devices',
    query: {
      ...(farmerId.value ? { farmerId: farmerId.value } : {}),
      ...(greenhouseId.value ? { greenhouseId: greenhouseId.value } : {})
    }
  })
}

const onFarmerChange = async () => {
  greenhouses.value = farmerId.value ? await fetchFarmerGreenhouses(farmerId.value) : []
  greenhouseId.value = greenhouses.value[0]?.id || null
  syncQuery()
  await loadDevices()
}

const onGreenhouseChange = async () => {
  syncQuery()
  await loadDevices()
}

const loadGreenhouses = async () => {
  if (isAdmin.value) {
    greenhouses.value = farmerId.value ? await fetchFarmerGreenhouses(farmerId.value) : []
    if (!greenhouseId.value) greenhouseId.value = greenhouses.value[0]?.id || null
    return
  }
  greenhouses.value = await fetchGreenhouses()
  greenhouseId.value = greenhouseId.value || greenhouses.value[0]?.id || null
}

const loadDevices = async () => {
  if (!greenhouseId.value) {
    devices.value = []
    return
  }
  loading.value = true
  try {
    devices.value = await fetchDevices(greenhouseId.value)
    devices.value.forEach(device => {
      if (isFanDevice(device) && fanLevels[device.id] === undefined) {
        fanLevels[device.id] = device.status === 'RUNNING' ? 5 : 0
      }
    })
  } finally {
    loading.value = false
  }
}

const resetForm = () => Object.assign(deviceForm, { greenhouseId: greenhouseId.value, name: '', category: '', status: 'STOPPED', location: '', remark: '', autoMode: true, healthScore: 100 })
const openCreate = () => { editingId.value = null; resetForm(); deviceDialog.value = true }
const openEdit = device => {
  editingId.value = device.id
  Object.assign(deviceForm, {
    greenhouseId: device.greenhouseId || device.greenhouse_id || greenhouseId.value,
    name: cleanText(device.name, ''),
    category: cleanText(device.category, ''),
    status: device.status || 'STOPPED',
    location: cleanText(device.location, ''),
    remark: cleanText(device.remark, ''),
    autoMode: device.autoMode ?? device.auto_mode ?? true,
    healthScore: device.healthScore ?? device.health_score ?? 100
  })
  deviceDialog.value = true
}

const toggleDevice = device => {
  commandDevice(device, device.status === 'RUNNING' ? 'STOP' : 'START')
}

const commandDevice = async (device, command) => {
  commandingId.value = device.id
  try {
    await sendDeviceCommand({
      deviceId: device.id,
      command,
      value: command
    })
    ElMessage.success(`${cleanText(device.name, '设备')}已${commandText(command)}`)
    await loadDevices()
  } finally {
    commandingId.value = null
  }
}

const setFanLevel = async device => {
  const level = Number(fanLevels[device.id] ?? 0)
  commandingId.value = device.id
  try {
    await sendDeviceCommand({
      deviceId: device.id,
      command: 'FENGDEGREE',
      value: String(Math.max(0, Math.min(9, level)))
    })
    ElMessage.success(`${cleanText(device.name, '风机')}已下发 ${Math.max(0, Math.min(9, level))} 档`)
    await loadDevices()
  } finally {
    commandingId.value = null
  }
}

const commandText = command => ({ START: '启动', STOP: '停止', MAINTENANCE: '标记维护' }[command] || '调控')

const submitDevice = async () => {
  if (!deviceForm.greenhouseId || !deviceForm.name || !deviceForm.category) return ElMessage.warning('请补全设备信息')
  saving.value = true
  try {
    if (editingId.value) await updateDevice(editingId.value, deviceForm)
    else await createDevice(deviceForm)
    greenhouseId.value = deviceForm.greenhouseId
    ElMessage.success('设备档案已保存')
    deviceDialog.value = false
    await loadDevices()
  } finally {
    saving.value = false
  }
}

const removeDevice = async device => {
  await ElMessageBox.confirm(`确认删除设备“${cleanText(device.name, '设备')}”？`, '删除设备', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
  await deleteDevice(device.id)
  ElMessage.success('设备已删除')
  await loadDevices()
}

onMounted(async () => {
  if (isAdmin.value) users.value = await fetchUsers()
  await loadGreenhouses()
  await loadDevices()
})
</script>

<style scoped>
.panel-head, .head-actions, .dialog-actions { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.head-actions { align-items: center; flex-wrap: wrap; justify-content: flex-end; }
.head-actions :deep(.el-select__wrapper),
.head-actions :deep(.el-button) {
  min-height: 40px;
  height: 40px;
}
.device-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 16px; margin-top: 18px; }
.device-card { min-height: 208px; padding: 18px; border: 1px solid var(--line); border-radius: var(--radius); background: rgba(255,255,255,.82); box-shadow: 0 12px 28px rgba(39, 80, 47, .06); display: flex; flex-direction: column; }
.device-card__top, .device-card__actions { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.device-card h3 { margin: 16px 0 6px; color: var(--ink); font-size: 20px; }
.device-card p { margin: 0 0 10px; color: var(--muted); }
.remark { color: #45604b; min-height: 22px; }
.device-card__actions { justify-content: center; flex-wrap: wrap; margin-top: auto; padding-top: 16px; }
.device-card__actions :deep(.el-button) { min-width: 62px; font-weight: 700; }
.device-card__actions :deep(.el-button + .el-button) { margin-left: 0; }
.device-card__actions :deep(.el-button--danger) { color: #fff; font-weight: 800; background: #e95a5a; border-color: #e95a5a; }
.fan-level-select { width: 92px; }
.fan-level-select :deep(.el-select__wrapper) { min-height: 32px; }
.dialog-actions { justify-content: flex-end; }
</style>
