import { createRouter, createWebHistory } from 'vue-router'
import { useSessionStore } from '../stores/session'
import LoginView from '../views/LoginView.vue'
import ConsoleLayout from '../layouts/ConsoleLayout.vue'
import DashboardView from '../views/DashboardView.vue'
import DeviceView from '../views/DeviceView.vue'
import AlertView from '../views/AlertView.vue'
import TraceabilityView from '../views/TraceabilityView.vue'
import ProfileView from '../views/ProfileView.vue'
import UserAdminView from '../views/UserAdminView.vue'
import FeedbackAdminView from '../views/FeedbackAdminView.vue'
import FarmerHomeView from '../views/FarmerHomeView.vue'
import FarmerFeedbackView from '../views/FarmerFeedbackView.vue'
import FarmerAnalyticsView from '../views/FarmerAnalyticsView.vue'
import AiAssistantView from '../views/AiAssistantView.vue'

const routes = [
  { path: '/login', component: LoginView },
  {
    path: '/',
    component: ConsoleLayout,
    children: [
      { path: '', name: 'dashboard', component: DashboardView, meta: { title: '管理总览', role: 'ADMIN' } },
      { path: 'farmer', name: 'farmer', component: FarmerHomeView, meta: { title: '农户工作台', role: 'FARMER' } },
      { path: 'devices', name: 'devices', component: DeviceView, meta: { title: '设备管理' } },
      { path: 'alerts', name: 'alerts', component: AlertView, meta: { title: '告警中心' } },
      { path: 'analytics', name: 'farmerAnalytics', component: FarmerAnalyticsView, meta: { title: '数据分析', role: 'FARMER' } },
      { path: 'ai-assistant', name: 'aiAssistant', component: AiAssistantView, meta: { title: 'AI助手' } },
      { path: 'traceability', name: 'traceability', component: TraceabilityView, meta: { title: '批次溯源' } },
      { path: 'profile', name: 'profile', component: ProfileView, meta: { title: '个人中心' } },
      { path: 'users', name: 'users', component: UserAdminView, meta: { title: '农户管理', role: 'ADMIN' } },
      { path: 'feedback', name: 'feedback', component: FeedbackAdminView, meta: { title: '反馈处理', role: 'ADMIN' } },
      { path: 'farmer-feedback', name: 'farmerFeedback', component: FarmerFeedbackView, meta: { title: '问题反馈', role: 'FARMER' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const session = useSessionStore()
  const role = session.profile?.role
  if (to.path !== '/login' && !session.token) {
    return '/login'
  }
  if (to.path === '/login' && session.token) {
    return role === 'ADMIN' ? '/' : '/farmer'
  }
  if (to.meta.role && role !== to.meta.role) {
    return role === 'ADMIN' ? '/' : '/farmer'
  }
  if (to.name === 'dashboard' && role && role !== 'ADMIN') {
    return '/farmer'
  }
  return true
})

export default router
