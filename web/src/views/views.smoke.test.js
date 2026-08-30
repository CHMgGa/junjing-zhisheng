// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import App from '../App.vue'
import ConsoleLayout from '../layouts/ConsoleLayout.vue'
import AiAssistantView from './AiAssistantView.vue'
import AlertView from './AlertView.vue'
import DashboardView from './DashboardView.vue'
import DeviceView from './DeviceView.vue'
import FarmerAnalyticsView from './FarmerAnalyticsView.vue'
import FarmerFeedbackView from './FarmerFeedbackView.vue'
import FarmerHomeView from './FarmerHomeView.vue'
import FeedbackAdminView from './FeedbackAdminView.vue'
import LoginView from './LoginView.vue'
import ProfileView from './ProfileView.vue'
import TraceabilityView from './TraceabilityView.vue'
import UserAdminView from './UserAdminView.vue'
import { useSessionStore } from '../stores/session'
import { fetchUnreadFeedback } from '../services/user'

const push = vi.fn()
const replace = vi.fn()
const route = {
  path: '/',
  fullPath: '/',
  name: 'dashboard',
  query: {},
  meta: { title: 'Dashboard' },
}

vi.mock('vue-router', () => ({
  RouterView: { template: '<div />' },
  useRoute: () => route,
  useRouter: () => ({ push, replace }),
}))

vi.mock('element-plus', async importOriginal => {
  const actual = await importOriginal()
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      warning: vi.fn(),
      error: vi.fn(),
    },
    ElMessageBox: {
      confirm: vi.fn(() => Promise.resolve()),
    },
  }
})

vi.mock('echarts', () => {
  const exerciseOptionCallbacks = option => {
    option?.tooltip?.formatter?.([
      { axisValue: '2026-07-03 10:00', marker: '', seriesName: '空气温度', value: 20 },
    ])
    const yAxes = Array.isArray(option?.yAxis) ? option.yAxis : option?.yAxis ? [option.yAxis] : []
    yAxes.forEach(axis => {
      if (typeof axis.min === 'function') axis.min({ min: 300 })
      if (typeof axis.max === 'function') axis.max({ max: 1300 })
    })
    const seriesList = Array.isArray(option?.series) ? option.series : option?.series ? [option.series] : []
    seriesList.forEach(series => {
      if (typeof series.itemStyle?.color === 'function') {
        series.itemStyle.color({ dataIndex: 0 })
        series.itemStyle.color({ dataIndex: 3 })
      }
    })
  }
  const createChart = () => ({
    setOption: vi.fn(exerciseOptionCallbacks),
    resize: vi.fn(),
    dispose: vi.fn(),
  })
  return {
    init: vi.fn(createChart),
    default: {
      init: vi.fn(createChart),
    },
  }
})

vi.mock('../services/auth', () => ({
  fetchPolicy: vi.fn(() => Promise.resolve({ title: 'policy', content: 'content' })),
  login: vi.fn(() => Promise.resolve({ token: 'token', profile: { id: 1, username: 'u', role: 'FARMER' } })),
  register: vi.fn(() => Promise.resolve({ token: 'token', profile: { id: 1, username: 'u', role: 'FARMER' } })),
  resetPassword: vi.fn(() => Promise.resolve()),
  sendCode: vi.fn(() => Promise.resolve({ message: 'sent', devCode: '123456' })),
}))

vi.mock('../services/user', () => ({
  bindFarmerGreenhouses: vi.fn(() => Promise.resolve()),
  createUser: vi.fn(() => Promise.resolve(1)),
  deleteUser: vi.fn(() => Promise.resolve()),
  fetchAdmins: vi.fn(() => Promise.resolve([{ id: 1, username: 'admin' }])),
  fetchFarmerGreenhouses: vi.fn(() => Promise.resolve([{ id: 10, name: 'A棚' }])),
  fetchFeedbackConversations: vi.fn(() => Promise.resolve([{ conversation_id: 1, farmer_username: 'farmer' }])),
  fetchFeedbackMessages: vi.fn(() => Promise.resolve([{ id: 1, content: 'hello', sender_role: 'FARMER' }])),
  fetchFeedbacks: vi.fn(() => Promise.resolve([{ id: 1, status: 'OPEN' }])),
  fetchProfile: vi.fn(() => Promise.resolve({ id: 1, username: 'farmer', role_code: 'FARMER', gender: 'MALE' })),
  fetchUnreadFeedback: vi.fn(() => Promise.resolve({ unreadCount: 1, firstConversationId: 1 })),
  fetchUsers: vi.fn(() => Promise.resolve([
    { id: 1, username: 'farmer', display_name: 'Farmer', role_code: 'FARMER' },
    { id: 2, username: 'admin', display_name: 'Admin', role_code: 'ADMIN' },
  ])),
  sendFeedbackMessage: vi.fn(() => Promise.resolve()),
  submitFeedback: vi.fn(() => Promise.resolve()),
  unbindFarmerGreenhouse: vi.fn(() => Promise.resolve()),
  updateProfile: vi.fn(() => Promise.resolve({ id: 1, username: 'farmer', role_code: 'FARMER' })),
  updateUser: vi.fn(() => Promise.resolve()),
}))

vi.mock('../services/greenhouse', () => ({
  alertCommand: vi.fn(() => Promise.resolve()),
  createBatch: vi.fn(() => Promise.resolve({ id: 1 })),
  createBatchEvent: vi.fn(() => Promise.resolve({ id: 1 })),
  createDevice: vi.fn(() => Promise.resolve({ id: 1 })),
  createGreenhouse: vi.fn(() => Promise.resolve({ id: 10 })),
  deleteDevice: vi.fn(() => Promise.resolve()),
  deleteGreenhouse: vi.fn(() => Promise.resolve()),
  fetchAlertDetails: vi.fn(() => Promise.resolve([{ id: 1, level: 'WARNING', status: 'OPEN' }])),
  fetchAlerts: vi.fn(() => Promise.resolve([{ id: 1, title: 'Alert', level: 'WARNING', status: 'OPEN' }])),
  fetchAnalytics: vi.fn(() => Promise.resolve({
    telemetryTrend: [{ collectedAt: new Date().toISOString(), airTemperature: 20, airHumidity: 60, soilTemperature: 18, soilHumidity: 50, phValue: 6.5, co2Ppm: 400, lightLux: 1000 }],
    deviceStatus: [{ name: 'RUNNING', value: 1 }],
    alertLevel: [{ name: 'WARNING', value: 1 }],
    productionArea: [{ name: 'A', value: 1 }],
  })),
  fetchBatchDetail: vi.fn(() => Promise.resolve({ batch: { id: 1 }, events: [] })),
  fetchBatches: vi.fn(() => Promise.resolve([{ id: 1, batchNo: 'B1', status: 'RUNNING' }])),
  fetchDevices: vi.fn(() => Promise.resolve([{ id: 1, name: 'Fan', category: 'fan', status: 'RUNNING' }])),
  fetchGreenhouses: vi.fn(() => Promise.resolve([{ id: 10, name: 'A棚', cropStage: '出菇期' }])),
  fetchOverview: vi.fn(() => Promise.resolve({
    greenhouses: [{ id: 10, name: 'A棚', cropStage: '出菇期' }],
    devices: [{ id: 1, name: 'Fan', category: 'fan', status: 'RUNNING' }],
    activeAlerts: [{ id: 1, title: 'Alert', level: 'WARNING', status: 'OPEN' }],
    currentTelemetry: { airTemperature: 20, airHumidity: 60, soilHumidity: 50, phValue: 6.5, co2Ppm: 400, lightLux: 1000 },
    productionSummary: { batchCount: 1, unresolvedAlertCount: 1 },
  })),
  fetchTraceability: vi.fn(() => Promise.resolve([{ id: 1, eventTitle: 'Seed' }])),
  handleAlert: vi.fn(() => Promise.resolve()),
  sendDeviceCommand: vi.fn(() => Promise.resolve()),
  updateDevice: vi.fn(() => Promise.resolve()),
  updateGreenhouse: vi.fn(() => Promise.resolve()),
}))

vi.mock('../services/ai', () => ({
  chatWithAi: vi.fn(() => Promise.resolve({ answer: 'ok' })),
  diagnoseImage: vi.fn(() => Promise.resolve({ answer: 'diag', risk_level: 'LOW' })),
}))

const stubs = {
  RouterView: true,
  RouterLink: true,
  'el-alert': true,
  'el-aside': true,
  'el-avatar': true,
  'el-button': true,
  'el-card': true,
  'el-checkbox': true,
  'el-col': true,
  'el-container': true,
  'el-date-picker': true,
  'el-dialog': true,
  'el-dropdown': true,
  'el-dropdown-item': true,
  'el-dropdown-menu': true,
  'el-empty': true,
  'el-form': true,
  'el-form-item': true,
  'el-header': true,
  'el-icon': true,
  'el-image': true,
  'el-input': true,
  'el-input-number': true,
  'el-main': true,
  'el-menu': true,
  'el-menu-item': true,
  'el-option': true,
  'el-pagination': true,
  'el-popconfirm': true,
  'el-radio-button': true,
  'el-radio-group': true,
  'el-row': true,
  'el-select': true,
  'el-statistic': true,
  'el-switch': true,
  'el-table': true,
  'el-table-column': true,
  'el-tab-pane': true,
  'el-tabs': true,
  'el-tag': true,
  'el-timeline': true,
  'el-timeline-item': true,
  'el-upload': true,
}

const mountOptions = () => ({
  global: {
    stubs,
    mocks: {
      $router: { push, replace },
    },
    directives: {
      loading: {},
    },
  },
})

const triggerTemplateClicks = async wrapper => {
  for (const button of wrapper.findAll('el-button-stub')) {
    await button.trigger('click')
  }
  for (const button of wrapper.findAll('button')) {
    await button.trigger('click')
  }
}

const emitTemplateModelUpdates = async wrapper => {
  const componentNames = [
    'ElSelect',
    'ElInput',
    'ElInputNumber',
    'ElRadioGroup',
    'ElDatePicker',
    'ElSwitch',
    'ElTabs',
  ]
  for (const name of componentNames) {
    for (const component of wrapper.findAllComponents({ name })) {
      component.vm.$emit('update:modelValue', 'test-value')
      component.vm.$emit('change', 'test-value')
      await flushPromises()
    }
  }
}

describe('view and layout components', () => {
  const storage = new Map()

  beforeEach(() => {
    vi.useFakeTimers()
    storage.clear()
    vi.stubGlobal('localStorage', {
      getItem: vi.fn((key) => storage.get(key) ?? null),
      setItem: vi.fn((key, value) => storage.set(key, String(value))),
      removeItem: vi.fn((key) => storage.delete(key)),
      clear: vi.fn(() => storage.clear()),
    })
    setActivePinia(createPinia())
    const store = useSessionStore()
    store.setSession({ token: 'token', profile: { id: 1, username: 'farmer', role: 'FARMER' } })
    Object.assign(route, { path: '/', fullPath: '/', name: 'dashboard', query: {}, meta: { title: 'Dashboard' } })
    push.mockClear()
    replace.mockClear()
    Object.defineProperty(window.HTMLMediaElement.prototype, 'play', {
      configurable: true,
      value: vi.fn(() => Promise.resolve()),
    })
    sessionStorage.clear()
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('mounts app shell and console layout', async () => {
    const app = shallowMount(App, mountOptions())
    expect(app.exists()).toBe(true)

    const layout = shallowMount(ConsoleLayout, mountOptions())
    await flushPromises()
    expect(layout.exists()).toBe(true)
    layout.unmount()
  })

  it('mounts all main views with mocked services', async () => {
    const components = [
      AiAssistantView,
      AlertView,
      DashboardView,
      DeviceView,
      FarmerAnalyticsView,
      FarmerFeedbackView,
      FarmerHomeView,
      FeedbackAdminView,
      LoginView,
      ProfileView,
      TraceabilityView,
      UserAdminView,
    ]

    for (const component of components) {
      const wrapper = shallowMount(component, mountOptions())
      await flushPromises()
      await emitTemplateModelUpdates(wrapper)
      window.dispatchEvent(new Event('resize'))
      expect(wrapper.exists()).toBe(true)
      wrapper.unmount()
    }
  })

  it('exercises form workflows in core views', async () => {
    const login = shallowMount(LoginView, mountOptions())
    await emitTemplateModelUpdates(login)
    login.vm.loginForm.username = 'farmer'
    login.vm.loginForm.password = 'secret'
    login.vm.loginAgreed = true
    await login.vm.submitLogin()
    expect(replace).toHaveBeenCalled()

    login.vm.registerForm.username = 'farmer2'
    login.vm.registerForm.phone = '13800000000'
    login.vm.registerForm.email = 'farmer@example.com'
    login.vm.registerForm.password = 'Secret#1'
    login.vm.registerForm.confirmPassword = 'Secret#1'
    login.vm.registerForm.verificationCode = '123456'
    login.vm.registerAgreed = true
    await login.vm.sendRegisterCode()
    await login.vm.submitRegister()

    login.vm.forgotForm.receiver = 'farmer@example.com'
    login.vm.forgotForm.verificationCode = '123456'
    login.vm.forgotForm.newPassword = 'Secret#2'
    login.vm.forgotForm.confirmPassword = 'Secret#2'
    await login.vm.sendResetCode()
    await login.vm.submitReset()
    await login.vm.openPolicy('privacy')
    login.unmount()

    const profile = shallowMount(ProfileView, mountOptions())
    await flushPromises()
    await emitTemplateModelUpdates(profile)
    profile.vm.form.displayName = 'Farmer'
    profile.vm.form.phone = '13800000000'
    profile.vm.form.email = 'farmer@example.com'
    profile.vm.onGenderChange('FEMALE')
    await profile.vm.save()
    expect(profile.vm.defaultAvatar('ADMIN', 'FEMALE')).toContain('female')
    expect(profile.vm.formatIpLocation('127.0.0.1')).toBeTruthy()
    profile.unmount()

    const store = useSessionStore()
    store.setSession({ token: 'token', profile: { id: 2, username: 'admin', role: 'ADMIN' } })
    const adminProfile = shallowMount(ProfileView, mountOptions())
    await flushPromises()
    await emitTemplateModelUpdates(adminProfile)
    adminProfile.vm.form.username = ''
    await adminProfile.vm.save()
    adminProfile.vm.form.username = 'farmer'
    await adminProfile.vm.save()
    adminProfile.vm.form.username = 'admin-main'
    adminProfile.vm.usingDefaultAvatar = true
    adminProfile.vm.onGenderChange('MALE')
    expect(adminProfile.vm.defaultAvatar('FARMER', 'FEMALE')).toContain('female')
    expect(adminProfile.vm.formatIpLocation('')).toBeTruthy()
    await adminProfile.vm.save()
    adminProfile.unmount()

    const device = shallowMount(DeviceView, mountOptions())
    await flushPromises()
    await emitTemplateModelUpdates(device)
    await triggerTemplateClicks(device)
    expect(device.vm.userLabel({ display_name: 'Farmer', username: 'farmer' })).toBeTruthy()
    expect(device.vm.statusText('RUNNING')).toBeTruthy()
    expect(device.vm.statusText('UNKNOWN')).toBe('UNKNOWN')
    expect(device.vm.statusTag('RUNNING')).toBe('success')
    expect(device.vm.statusTag('MAINTENANCE')).toBe('warning')
    expect(device.vm.statusTag('STOPPED')).toBe('info')
    expect(device.vm.looksInvalid('farmer-device-1')).toBe(true)
    expect(device.vm.looksInvalid('????')).toBe(true)
    expect(device.vm.looksInvalid('test')).toBe(true)
    expect(device.vm.looksInvalid('Fan')).toBe(false)
    expect(device.vm.cleanText('', 'fallback')).toBe('fallback')
    expect(device.vm.cleanText('CO2 sensor', 'fallback')).toBeTruthy()
    device.vm.openCreate()
    await device.vm.submitDevice()
    device.vm.deviceForm.greenhouseId = 10
    device.vm.deviceForm.name = 'Fan'
    device.vm.deviceForm.category = 'fan'
    await device.vm.submitDevice()
    device.vm.openEdit({
      id: 1,
      greenhouse_id: 10,
      name: 'Fan',
      category: 'fan',
      status: 'RUNNING',
      auto_mode: false,
      health_score: 80,
    })
    await device.vm.submitDevice()
    device.vm.toggleDevice({ id: 1, name: 'Fan', status: 'RUNNING' })
    device.vm.toggleDevice({ id: 1, name: 'Fan', status: 'STOPPED' })
    await device.vm.commandDevice({ id: 1, name: 'Fan' }, 'START')
    expect(device.vm.commandText('MAINTENANCE')).toBeTruthy()
    expect(device.vm.commandText('UNKNOWN')).toBeTruthy()
    device.vm.greenhouseId = null
    await device.vm.loadDevices()
    await device.vm.removeDevice({ id: 1, name: 'Fan' })
    device.unmount()

    store.setSession({ token: 'token', profile: { id: 1, username: 'farmer', role: 'FARMER' } })
    const farmerDevice = shallowMount(DeviceView, mountOptions())
    await flushPromises()
    await emitTemplateModelUpdates(farmerDevice)
    await farmerDevice.vm.loadGreenhouses()
    await farmerDevice.vm.onGreenhouseChange()
    farmerDevice.unmount()

    const users = shallowMount(UserAdminView, mountOptions())
    await flushPromises()
    await emitTemplateModelUpdates(users)
    expect(users.vm.defaultAvatar('ADMIN', 'MALE')).toContain('admin')
    expect(users.vm.defaultAvatar('ADMIN', 'FEMALE')).toContain('female')
    expect(users.vm.defaultAvatar('FARMER', 'MALE')).toContain('farmer')
    expect(users.vm.defaultAvatar('FARMER', 'FEMALE')).toContain('female')
    expect(users.vm.avatarText({ username: 'farmer' })).toBe('f')
    expect(users.vm.avatarText({})).toBeTruthy()
    expect(users.vm.rowToPayload({ username: 'x', role_code: 'FARMER', enabled: 0 }).enabled).toBe(false)
    await users.vm.resetFilters()
    users.vm.openCreate('ADMIN')
    expect(users.vm.userForm.username).toBe('admin')
    users.vm.openCreate('FARMER')
    await users.vm.submitUser()
    users.vm.userForm.username = 'newfarmer'
    users.vm.userForm.password = 'secret'
    await users.vm.submitUser()
    users.vm.openEdit({ id: 2, username: 'farmer', role_code: 'FARMER', enabled: true })
    await users.vm.submitUser()
    await users.vm.toggleEnabled({ id: 2, username: 'farmer', role_code: 'FARMER', enabled: true })
    await users.vm.toggleEnabled({ id: 2, username: 'farmer', role_code: 'FARMER', enabled: false })
    await users.vm.openBind({ id: 2, username: 'farmer', role_code: 'FARMER' })
    users.vm.bindingIds = [10]
    await users.vm.submitBind()
    users.vm.bindingUser = { id: 2 }
    await users.vm.unbindOne({ id: 10, name: 'A' })
    await users.vm.removeUser({ id: 2, username: 'farmer' })
    users.unmount()
  })

  it('exercises alert, feedback and traceability workflows', async () => {
    const store = useSessionStore()
    store.setSession({ token: 'token', profile: { id: 99, username: 'admin', role: 'ADMIN' } })

    const alert = shallowMount(AlertView, mountOptions())
    await flushPromises()
    await emitTemplateModelUpdates(alert)
    const alertRow = {
      id: 1,
      greenhouseId: 10,
      deviceId: 1,
      title: 'CO2 warning',
      description: 'needs ventilation',
      deviceName: 'Fan',
      status: 'OPEN',
    }
    await alert.vm.openHandle(alertRow)
    await alert.vm.submitHandle()
    alert.vm.handleForm.handler = 'admin'
    alert.vm.handleForm.note = 'checked'
    await alert.vm.submitHandle()
    await alert.vm.openCommand(alertRow)
    alert.vm.commandForm.command = ''
    await alert.vm.submitCommand()
    alert.vm.commandForm.command = 'START'
    await alert.vm.submitCommand()
    alert.vm.openDetail({ ...alertRow, status: 'RESOLVED', handledBy: 'admin', handleNote: 'done' })
    alert.vm.statusFilter = 'OPEN'
    alert.vm.farmerId = 999
    alert.vm.alerts = [
      { id: 1, status: 'OPEN', farmerId: 1 },
      { id: 2, status: 'RESOLVED', farmerId: 999 },
    ]
    expect(alert.vm.filteredAlerts.length).toBe(0)
    alert.vm.farmerId = 1
    expect(alert.vm.filteredAlerts.length).toBe(1)
    await alert.vm.onFilterChange()
    await alert.vm.onFarmerChange()
    expect(alert.vm.levelTag('CRITICAL')).toBe('danger')
    expect(alert.vm.levelTag('INFO')).toBe('info')
    expect(alert.vm.levelText()).toBe('-')
    expect(alert.vm.statusTag('RESOLVED')).toBe('success')
    expect(alert.vm.statusText()).toBe('-')
    expect(alert.vm.deviceLabel({ name: '', status: '' })).toBeTruthy()
    expect(alert.vm.displayHandledAt({ status: 'OPEN' })).toBe('-')
    expect(alert.vm.displayHandleNote({ status: 'OPEN' })).toBeTruthy()
    expect(alert.vm.displayHandler(alert.vm.selectedAlert)).toBe('admin')
    alert.unmount()

    store.setSession({ token: 'token', profile: { id: 1, username: 'farmer', role: 'FARMER' } })
    const feedback = shallowMount(FarmerFeedbackView, mountOptions())
    await flushPromises()
    await emitTemplateModelUpdates(feedback)
    expect(feedback.vm.defaultAvatar('ADMIN', 'FEMALE')).toContain('female')
    expect(feedback.vm.avatarName({ display_name: 'Admin' })).toBeTruthy()
    feedback.vm.draft = ''
    await feedback.vm.sendMessage()
    feedback.vm.draft = 'hello admin'
    await feedback.vm.sendMessage()
    const preventDefault = vi.fn()
    feedback.vm.submitOnEnter({ shiftKey: true, isComposing: false, preventDefault })
    expect(preventDefault).not.toHaveBeenCalled()
    feedback.vm.submitOnEnter({ shiftKey: false, isComposing: false, preventDefault })
    expect(preventDefault).toHaveBeenCalled()
    feedback.vm.onImageChange({ target: { files: [{ size: 3 * 1024 * 1024 }], value: 'x' } })
    feedback.unmount()

    store.setSession({ token: 'token', profile: { id: 99, username: 'admin', role: 'ADMIN' } })
    const adminFeedback = shallowMount(FarmerFeedbackView, mountOptions())
    await flushPromises()
    expect(adminFeedback.vm.avatarUrl({ farmer_gender: 'FEMALE' })).toContain('female')
    expect(adminFeedback.vm.messageRole({ sender_user_id: 1 })).toBe('FARMER')
    await adminFeedback.vm.selectConversation({ conversation_id: 1, farmer_user_id: 1 }, true)
    adminFeedback.vm.draft = ''
    adminFeedback.vm.imageData = 'data:image/png;base64,abc'
    await adminFeedback.vm.sendMessage()
    await adminFeedback.vm.selectConversation({ admin_user_id: 1 }, true)
    adminFeedback.unmount()

    const trace = shallowMount(TraceabilityView, mountOptions())
    await flushPromises()
    await emitTemplateModelUpdates(trace)
    await triggerTemplateClicks(trace)
    expect(trace.vm.userLabel({ display_name: 'Farmer', username: 'farmer' })).toBeTruthy()
    expect(trace.vm.statusTagType('DONE')).toBe('success')
    expect(trace.vm.statusText('CLOSED')).toBeTruthy()
    expect(trace.vm.statusTagType('UNKNOWN')).toBe('info')
    expect(trace.vm.formatTime()).toBe('')
    expect(trace.vm.formatDate()).toBe('')
    expect(trace.vm.eventImage({})).toContain('greenhouse')
    trace.vm.filters.farmerId = 1
    await trace.vm.onFarmerChange()
    await trace.vm.onGreenhouseChange()
    await trace.vm.openDetail({ id: 1 })
    trace.vm.openCreateBatch()
    await trace.vm.submitBatch()
    trace.vm.batchForm.greenhouseId = 10
    trace.vm.batchForm.batchNo = 'B-001'
    trace.vm.batchForm.batchName = 'Batch 1'
    trace.vm.batchForm.startedAt = '2026-07-01'
    await trace.vm.submitBatch()
    trace.vm.openCreateEvent()
    await trace.vm.submitEvent()
    trace.vm.eventForm.eventCode = 'SOW'
    trace.vm.eventForm.eventTitle = 'Sowing'
    await trace.vm.submitEvent()
    trace.vm.openEvent({ eventTitle: 'Sowing', imageUrl: '/seed.png' })
    trace.vm.onEventImageChange({ target: { files: [{ size: 3 * 1024 * 1024 }], value: 'x' } })
    trace.vm.clearEventImage()
    trace.unmount()
  })

  it('covers boundary branches in login, dashboard, layout and farmer home', async () => {
    const login = shallowMount(LoginView, mountOptions())
    await login.vm.submitLogin()
    login.vm.loginForm.username = 'farmer'
    await login.vm.submitLogin()
    login.vm.loginForm.password = 'secret'
    login.vm.loginAgreed = false
    await login.vm.submitLogin()
    login.vm.registerForm.username = ''
    await login.vm.submitRegister()
    login.vm.registerForm.username = 'admin-test'
    await login.vm.submitRegister()
    login.vm.registerForm.username = 'farmer'
    await login.vm.submitRegister()
    login.vm.registerForm.phone = '13800000000'
    await login.vm.submitRegister()
    login.vm.registerForm.email = 'bad-email'
    await login.vm.sendRegisterCode()
    login.vm.registerForm.email = 'farmer@example.com'
    await login.vm.submitRegister()
    login.vm.registerForm.password = 'Secret#1'
    login.vm.registerForm.confirmPassword = 'Secret#2'
    await login.vm.submitRegister()
    login.vm.registerForm.confirmPassword = 'Secret#1'
    await login.vm.submitRegister()
    login.vm.forgotForm.receiver = ''
    await login.vm.submitReset()
    login.vm.forgotForm.receiver = 'farmer@example.com'
    await login.vm.submitReset()
    login.vm.forgotForm.verificationCode = '123456'
    await login.vm.submitReset()
    login.vm.forgotForm.newPassword = 'Secret#1'
    login.vm.forgotForm.confirmPassword = 'Secret#2'
    await login.vm.submitReset()
    expect(login.vm.canSendRegisterCode).toBe(true)
    login.unmount()

    Object.assign(route, { path: '/', fullPath: '/', name: 'dashboard', query: {}, meta: { title: 'Dashboard' } })
    const dashboard = shallowMount(DashboardView, mountOptions())
    await flushPromises()
    await emitTemplateModelUpdates(dashboard)
    await triggerTemplateClicks(dashboard)
    expect(dashboard.vm.userLabel({ display_name: 'Farmer', username: 'farmer' })).toBeTruthy()
    expect(dashboard.vm.deviceStatus('STOPPED')).toBeTruthy()
    expect(dashboard.vm.alertLevel('INFO')).toBeTruthy()
    dashboard.vm.farmerId = 1
    await dashboard.vm.onFarmerChange()
    dashboard.vm.greenhouseId = 10
    await dashboard.vm.loadOverview()
    dashboard.vm.go('/alerts', { status: 'OPEN' })
    dashboard.vm.greenhouseId = 999
    await dashboard.vm.loadFarmerGreenhouses()
    dashboard.unmount()

    const layout = shallowMount(ConsoleLayout, mountOptions())
    await flushPromises()
    await layout.vm.loadUnread()
    fetchUnreadFeedback.mockRejectedValueOnce(new Error('network'))
    await layout.vm.loadUnread()
    expect(layout.vm.unreadCount).toBe(0)
    layout.vm.firstConversationId = ''
    layout.vm.openUnread()
    layout.vm.firstConversationId = 1
    layout.vm.openUnread()
    const layoutStore = useSessionStore()
    layoutStore.setSession({ token: 'token', profile: { id: 1, username: 'farmer', role: 'FARMER' } })
    layout.vm.openUnread()
    layoutStore.token = ''
    await layout.vm.loadUnread()
    layout.vm.logout()
    expect(push).toHaveBeenCalled()
    layout.unmount()

    const farmerHome = shallowMount(FarmerHomeView, mountOptions())
    await flushPromises()
    await emitTemplateModelUpdates(farmerHome)
    await triggerTemplateClicks(farmerHome)
    await triggerTemplateClicks(farmerHome)
    expect(farmerHome.vm.statusText('MAINTENANCE')).toBeTruthy()
    expect(farmerHome.vm.deviceTag('RUNNING')).toBe('success')
    expect(farmerHome.vm.levelText('CRITICAL')).toBeTruthy()
    farmerHome.vm.overview.currentTelemetry = {}
    expect(farmerHome.vm.temperatureAdvice).toBeTruthy()
    expect(farmerHome.vm.humidityAdvice).toBeTruthy()
    expect(farmerHome.vm.co2Advice).toBeTruthy()
    expect(farmerHome.vm.soilAdvice).toBeTruthy()
    expect(farmerHome.vm.phAdvice).toBeTruthy()
    expect(farmerHome.vm.lightAdvice).toBeTruthy()
    farmerHome.vm.overview.currentTelemetry = { airTemperature: 10, airHumidity: 60, co2Ppm: 1300, soilHumidity: 40, phValue: 5, lightLux: 1000 }
    expect(farmerHome.vm.humidityAdvice).toBeTruthy()
    expect(farmerHome.vm.co2Advice).toBeTruthy()
    expect(farmerHome.vm.soilAdvice).toBeTruthy()
    expect(farmerHome.vm.phAdvice).toBeTruthy()
    expect(farmerHome.vm.lightAdvice).toBeTruthy()
    expect(farmerHome.vm.dailyTasks.length).toBe(3)
    farmerHome.vm.overview.currentTelemetry = { airTemperature: 30, airHumidity: 95, co2Ppm: 300, soilHumidity: 80, phValue: 8, lightLux: 7000 }
    expect(farmerHome.vm.temperatureAdvice).toBeTruthy()
    expect(farmerHome.vm.humidityAdvice).toBeTruthy()
    expect(farmerHome.vm.co2Advice).toBeTruthy()
    expect(farmerHome.vm.soilAdvice).toBeTruthy()
    expect(farmerHome.vm.phAdvice).toBeTruthy()
    expect(farmerHome.vm.lightAdvice).toBeTruthy()
    farmerHome.vm.overview.currentTelemetry = { airTemperature: 20, airHumidity: 80, co2Ppm: 700, soilHumidity: 60, phValue: 6.5, lightLux: 3000 }
    expect(farmerHome.vm.temperatureAdvice).toBeTruthy()
    expect(farmerHome.vm.humidityAdvice).toBeTruthy()
    expect(farmerHome.vm.co2Advice).toBeTruthy()
    expect(farmerHome.vm.soilAdvice).toBeTruthy()
    expect(farmerHome.vm.phAdvice).toBeTruthy()
    expect(farmerHome.vm.lightAdvice).toBeTruthy()
    farmerHome.vm.openGreenhouseDialog()
    await farmerHome.vm.submitGreenhouse()
    farmerHome.vm.greenhouseForm.name = 'Greenhouse'
    await farmerHome.vm.submitGreenhouse()
    farmerHome.unmount()
  })
})
