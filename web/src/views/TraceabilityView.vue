<template>
  <div class="page" v-loading="loading">
    <section class="panel">
      <div class="panel-head">
        <div>
          <h2 class="section-title">批次溯源</h2>
          <p class="muted">按时间、批次码和大棚查询生产批次，查看关键生产记录与全流程溯源信息。</p>
        </div>
        <el-button v-if="isAdmin" type="primary" @click="openCreateBatch">新建批次</el-button>
      </div>

      <div :class="['filters', { 'filters--admin': isAdmin }]">
        <el-input v-model.trim="filters.batchNo" clearable placeholder="批次码" />
        <el-select v-if="isAdmin" v-model="filters.farmerId" clearable placeholder="选择农户" @change="onFarmerChange">
          <el-option v-for="item in farmers" :key="item.id" :label="userLabel(item)" :value="item.id" />
        </el-select>
        <el-select
          v-model="filters.greenhouseId"
          clearable
          :disabled="isAdmin && !filters.farmerId"
          :placeholder="isAdmin ? '选择该农户的大棚' : '大棚'"
          @change="onGreenhouseChange"
        >
          <el-option v-for="item in greenhouses" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
        <el-button type="primary" @click="load">查询</el-button>
      </div>

      <div class="batch-grid">
        <article v-for="batch in batches" :key="batch.id" class="batch-card" @click="openDetail(batch)">
          <span>{{ batch.greenhouse_name }}</span>
          <h3>{{ batch.batch_name }}</h3>
          <p>批次码：{{ batch.batch_no }}</p>
          <p>{{ batch.summary || '暂无说明' }}</p>
          <div>
            <el-tag :type="statusTagType(batch.status)">{{ statusText(batch.status) }}</el-tag>
            <small>{{ formatDate(batch.started_at) }} 开始</small>
          </div>
        </article>
        <el-empty v-if="!batches.length" description="暂无批次数据" />
      </div>
    </section>

    <el-dialog v-model="detailVisible" title="批次链路详情" width="940px">
      <div v-if="detail.batch" class="batch-detail">
        <div class="detail-head">
          <div>
            <span>{{ detail.batch.greenhouse_name }}</span>
            <h2>{{ detail.batch.batch_name }}</h2>
            <p>批次码：{{ detail.batch.batch_no }} · {{ statusText(detail.batch.status) }}</p>
          </div>
          <el-button v-if="isAdmin" type="primary" @click="openCreateEvent">确认新节点</el-button>
        </div>

        <div class="block-chain">
          <article v-for="(event, index) in detail.events" :key="event.id" class="block-card">
            <div class="block-index">{{ index + 1 }}</div>
            <div class="block-body">
              <strong>{{ event.event_title }}</strong>
              <p>{{ event.description }}</p>
              <small>{{ formatTime(event.event_time) }} · 管理员：{{ event.operator || '-' }}</small>
              <button class="detail-button" type="button" @click.stop="openEvent(event)">详细信息</button>
              <code>{{ event.previous_hash || 'GENESIS' }} -> {{ event.block_hash || '-' }}</code>
            </div>
            <el-tag :type="event.event_status === 'RUNNING' ? 'warning' : 'success'">
              {{ event.event_status === 'RUNNING' ? '进行中' : '已确认' }}
            </el-tag>
          </article>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="batchDialog" title="新建批次" width="620px">
      <el-form label-position="top">
        <el-form-item label="所属大棚">
          <el-select v-model="batchForm.greenhouseId" style="width: 100%">
            <el-option v-for="item in greenhouses" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="批次码"><el-input v-model.trim="batchForm.batchNo" /></el-form-item>
        <el-form-item label="批次名称"><el-input v-model.trim="batchForm.batchName" /></el-form-item>
        <el-form-item label="作物名称"><el-input v-model.trim="batchForm.cropName" /></el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="batchForm.startedAt" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预计采收日期">
          <el-date-picker v-model="batchForm.expectedHarvestAt" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="说明"><el-input v-model.trim="batchForm.summary" type="textarea" :rows="3" /></el-form-item>
        <div class="dialog-actions">
          <el-button @click="batchDialog = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitBatch">保存</el-button>
        </div>
      </el-form>
    </el-dialog>

    <el-dialog v-model="eventDialog" title="确认链路节点" width="620px">
      <el-form label-position="top">
        <el-form-item label="节点编码"><el-input v-model.trim="eventForm.eventCode" /></el-form-item>
        <el-form-item label="节点标题"><el-input v-model.trim="eventForm.eventTitle" /></el-form-item>
        <el-form-item label="节点状态">
          <el-select v-model="eventForm.eventStatus" style="width: 100%">
            <el-option label="已确认" value="DONE" />
            <el-option label="进行中" value="RUNNING" />
          </el-select>
        </el-form-item>
        <el-form-item label="上传图片">
          <div class="event-upload">
            <input ref="eventFileInput" type="file" accept="image/*" hidden @change="onEventImageChange" />
            <button type="button" class="upload-button" @click="eventFileInput?.click()">选择图片</button>
            <img v-if="eventImagePreview" :src="eventImagePreview" alt="待上传图片" />
            <button v-if="eventImagePreview" type="button" class="clear-button" @click="clearEventImage">移除</button>
          </div>
        </el-form-item>
        <el-form-item label="说明"><el-input v-model.trim="eventForm.description" type="textarea" :rows="4" /></el-form-item>
        <div class="dialog-actions">
          <el-button @click="eventDialog = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitEvent">确认上传</el-button>
        </div>
      </el-form>
    </el-dialog>

    <el-dialog v-model="eventVisible" title="记录详情" width="620px">
      <div v-if="selectedEvent" class="event-detail">
        <h3>{{ selectedEvent.event_title }}</h3>
        <p>{{ selectedEvent.description }}</p>
        <img :src="eventImage(selectedEvent)" alt="批次记录图片" />
        <small>{{ formatTime(selectedEvent.event_time) }} · 管理员：{{ selectedEvent.operator || '-' }}</small>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createBatch, createBatchEvent, fetchBatchDetail, fetchBatches, fetchGreenhouses } from '../services/greenhouse'
import { fetchFarmerGreenhouses, fetchUsers } from '../services/user'
import { useSessionStore } from '../stores/session'

const route = useRoute()
const session = useSessionStore()
const isAdmin = computed(() => session.profile?.role === 'ADMIN')
const loading = ref(false)
const saving = ref(false)
const batches = ref([])
const users = ref([])
const greenhouses = ref([])
const dateRange = ref([])
const detailVisible = ref(false)
const eventVisible = ref(false)
const batchDialog = ref(false)
const eventDialog = ref(false)
const selectedEvent = ref(null)
const eventFileInput = ref(null)
const eventImagePreview = ref('')
const detail = reactive({ batch: null, events: [] })
const filters = reactive({
  batchNo: '',
  farmerId: route.query.farmerId ? Number(route.query.farmerId) : null,
  greenhouseId: route.query.greenhouseId ? Number(route.query.greenhouseId) : null
})
const batchForm = reactive({
  greenhouseId: null,
  batchNo: '',
  batchName: '',
  cropName: '羊肚菌',
  status: 'RUNNING',
  startedAt: '',
  expectedHarvestAt: '',
  summary: ''
})
const eventForm = reactive({ eventCode: '', eventTitle: '', eventStatus: 'DONE', description: '', imageUrl: '' })

const farmers = computed(() => users.value.filter(item => item.role_code === 'FARMER'))
const userLabel = item => `${item.display_name || item.username}（${item.username}）`
const statusText = status => ({ RUNNING: '进行中', DONE: '已完成', CLOSED: '已归档' }[status] || status)
const statusTagType = status => ({ RUNNING: 'warning', DONE: 'success', CLOSED: 'info' }[status] || 'info')
const formatTime = value => value ? new Date(value).toLocaleString('zh-CN') : ''
const formatDate = value => value ? new Date(value).toLocaleDateString('zh-CN') : ''
const eventImage = event => event.image_url || event.imageUrl || '/greenhouse-bg.svg'

const load = async () => {
  loading.value = true
  try {
    batches.value = await fetchBatches({
      farmerId: filters.farmerId || undefined,
      batchNo: filters.batchNo || undefined,
      greenhouseId: filters.greenhouseId || undefined,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1]
    })
  } finally {
    loading.value = false
  }
}

const onFarmerChange = async () => {
  greenhouses.value = filters.farmerId ? await fetchFarmerGreenhouses(filters.farmerId) : []
  filters.greenhouseId = null
  await load()
}

const onGreenhouseChange = async () => {
  await load()
}

const openDetail = async batch => {
  Object.assign(detail, await fetchBatchDetail(batch.id))
  detailVisible.value = true
}

const openCreateBatch = () => {
  Object.assign(batchForm, {
    greenhouseId: filters.greenhouseId || greenhouses.value[0]?.id || null,
    batchNo: '',
    batchName: '',
    cropName: '羊肚菌',
    status: 'RUNNING',
    startedAt: '',
    expectedHarvestAt: '',
    summary: ''
  })
  batchDialog.value = true
}

const submitBatch = async () => {
  if (!batchForm.greenhouseId || !batchForm.batchNo || !batchForm.batchName || !batchForm.startedAt) {
    return ElMessage.warning('请补全批次信息')
  }
  saving.value = true
  try {
    await createBatch(batchForm)
    ElMessage.success('批次已创建')
    batchDialog.value = false
    await load()
  } finally {
    saving.value = false
  }
}

const openCreateEvent = () => {
  Object.assign(eventForm, { eventCode: '', eventTitle: '', eventStatus: 'DONE', description: '', imageUrl: '' })
  clearEventImage()
  eventDialog.value = true
}

const onEventImageChange = event => {
  const file = event.target.files?.[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) {
    event.target.value = ''
    return ElMessage.warning('图片不能超过 2MB')
  }
  const reader = new FileReader()
  reader.onload = () => {
    eventForm.imageUrl = String(reader.result || '')
    eventImagePreview.value = eventForm.imageUrl
  }
  reader.readAsDataURL(file)
}

const clearEventImage = () => {
  eventForm.imageUrl = ''
  eventImagePreview.value = ''
  if (eventFileInput.value) eventFileInput.value.value = ''
}

const submitEvent = async () => {
  if (!eventForm.eventCode || !eventForm.eventTitle) return ElMessage.warning('请填写节点编码和标题')
  saving.value = true
  try {
    await createBatchEvent(detail.batch.id, eventForm)
    ElMessage.success('链路节点已确认')
    eventDialog.value = false
    clearEventImage()
    Object.assign(detail, await fetchBatchDetail(detail.batch.id))
  } finally {
    saving.value = false
  }
}

const openEvent = event => {
  selectedEvent.value = event
  eventVisible.value = true
}

onMounted(async () => {
  if (isAdmin.value) {
    users.value = await fetchUsers()
    greenhouses.value = filters.farmerId ? await fetchFarmerGreenhouses(filters.farmerId) : []
  } else {
    greenhouses.value = await fetchGreenhouses()
  }
  await load()
})
</script>

<style scoped>
.panel-head,
.filters,
.dialog-actions {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.filters {
  display: grid;
  grid-template-columns: 220px 220px 420px 86px;
  align-items: center;
  justify-content: start;
  margin-top: 18px;
}
.filters--admin {
  grid-template-columns: 220px 220px 240px 420px 86px;
}
.filters .el-input,
.filters .el-select,
.filters .el-date-editor {
  width: 100%;
  height: 40px;
}
.filters :deep(.el-input__wrapper),
.filters :deep(.el-select__wrapper),
.filters :deep(.el-date-editor.el-input__wrapper) {
  min-height: 40px;
  height: 40px;
}
.filters :deep(.el-button) {
  width: 86px;
  height: 40px;
}
.batch-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, 320px);
  justify-content: start;
  gap: 16px;
  margin-top: 18px;
}
.batch-card {
  width: 320px;
  min-height: 210px;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: rgba(255,255,255,.82);
  cursor: pointer;
  transition: transform .18s ease, border-color .18s ease;
}
.batch-card:hover { transform: translateY(-2px); border-color: var(--brand); }
.batch-card span,
.batch-card p,
.batch-card small { color: var(--muted); }
.batch-card h3 { margin: 8px 0; color: var(--ink); }
.batch-card div { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: rgba(83,184,106,.1);
}
.detail-head h2 { margin: 6px 0; }
.block-chain { display: grid; gap: 14px; margin-top: 18px; }
.block-card { position: relative; display: grid; grid-template-columns: 52px 1fr auto; gap: 14px; align-items: start; padding: 16px; border: 1px solid var(--line); border-radius: var(--radius); background: rgba(255,255,255,.88); }
.block-card::before { content: ""; position: absolute; left: 41px; top: -16px; width: 2px; height: 16px; background: var(--brand); }
.block-card:first-child::before { display: none; }
.block-index { display: grid; width: 38px; height: 38px; place-items: center; border-radius: 50%; background: var(--brand); color: #fff; font-weight: 900; }
.block-body p { margin: 8px 0; color: var(--muted); }
.block-body small { display: block; color: var(--muted); }
.block-body code { display: block; margin-top: 8px; color: #2f6f45; white-space: normal; }
.detail-button { margin-top: 10px; border: 1px solid var(--line); border-radius: 8px; background: #fff; color: var(--brand-strong); padding: 7px 12px; font-weight: 800; cursor: pointer; }
.event-upload {
  display: grid;
  grid-template-columns: 120px 96px auto;
  align-items: center;
  justify-content: start;
  gap: 10px;
}
.upload-button,
.clear-button {
  height: 40px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fff;
  color: var(--ink);
  font-weight: 800;
  cursor: pointer;
}
.upload-button { width: 120px; }
.clear-button { width: 72px; color: #d44444; }
.event-upload img {
  width: 96px;
  height: 64px;
  border: 1px solid var(--line);
  border-radius: 8px;
  object-fit: cover;
  background: #f6fbf2;
}
.event-detail { display: grid; gap: 12px; }
.event-detail h3,
.event-detail p { margin: 0; }
.event-detail p,
.event-detail small { color: var(--muted); }
.event-detail img { width: 100%; max-height: 340px; object-fit: cover; border-radius: 8px; border: 1px solid var(--line); background: #f6fbf2; }
.dialog-actions { justify-content: flex-end; margin-top: 18px; }
</style>
