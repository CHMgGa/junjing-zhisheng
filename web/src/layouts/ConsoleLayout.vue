<template>
  <el-container :class="['console', { 'console--admin': isAdmin }]">
    <el-aside width="248px" class="console__aside">
      <div class="brand">
        <div class="brand__mark">菌</div>
        <div>
          <strong>菌境智生</strong>
          <span>羊肚菌智慧种植调控系统</span>
        </div>
      </div>

      <el-menu router :default-active="$route.path" class="nav">
        <el-menu-item v-if="isAdmin" index="/">
          <el-icon><DataAnalysis /></el-icon>
          <span>管理总览</span>
        </el-menu-item>
        <el-menu-item v-else index="/farmer">
          <el-icon><House /></el-icon>
          <span>农户工作台</span>
        </el-menu-item>
        <el-menu-item index="/devices">
          <el-icon><Operation /></el-icon>
          <span>设备管理</span>
        </el-menu-item>
        <el-menu-item index="/alerts">
          <el-icon><Warning /></el-icon>
          <span>告警中心</span>
        </el-menu-item>
        <el-menu-item v-if="!isAdmin" index="/analytics">
          <el-icon><TrendCharts /></el-icon>
          <span>数据分析</span>
        </el-menu-item>
        <el-menu-item index="/ai-assistant">
          <el-icon><ChatLineRound /></el-icon>
          <span>AI助手</span>
        </el-menu-item>
        <el-menu-item index="/traceability">
          <el-icon><Tickets /></el-icon>
          <span>批次溯源</span>
        </el-menu-item>
        <el-menu-item v-if="!isAdmin" index="/farmer-feedback">
          <el-icon><ChatDotRound /></el-icon>
          <span>问题反馈</span>
          <i v-if="unreadCount" class="menu-dot">{{ unreadCount }}</i>
        </el-menu-item>
        <el-menu-item index="/profile">
          <el-icon><User /></el-icon>
          <span>个人中心</span>
        </el-menu-item>
        <template v-if="isAdmin">
          <el-menu-item index="/users">
            <el-icon><UserFilled /></el-icon>
            <span>农户管理</span>
          </el-menu-item>
          <el-menu-item index="/feedback">
            <el-icon><ChatDotRound /></el-icon>
            <span>反馈处理</span>
            <i v-if="unreadCount" class="menu-dot">{{ unreadCount }}</i>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container class="console__body">
      <el-header class="console__header">
        <div>
          <p>{{ currentDate }}</p>
          <h1>{{ $route.meta.title || '智慧大棚控制台' }}</h1>
        </div>
        <div class="header-actions">
          <button class="message-button" type="button" title="未读消息" @click="openUnread">
            <el-icon><Bell /></el-icon>
            <i v-if="unreadCount" class="badge">{{ unreadCount }}</i>
          </button>
          <el-dropdown trigger="click">
            <span class="operator">{{ operatorName }}</span>
            <template #dropdown>
              <el-dropdown-menu class="operator-menu">
                <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
                <el-dropdown-item divided @click="logout">
                  <span class="logout-item">退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="console__main">
        <RouterView @feedback-read="loadUnread" />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell,
  ChatDotRound,
  ChatLineRound,
  DataAnalysis,
  House,
  Operation,
  Tickets,
  TrendCharts,
  User,
  UserFilled,
  Warning
} from '@element-plus/icons-vue'
import { fetchUnreadFeedback } from '../services/user'
import { useSessionStore } from '../stores/session'

const router = useRouter()
const route = useRoute()
const session = useSessionStore()
const unreadCount = ref(0)
const firstConversationId = ref('')
const currentDate = ref(new Date().toLocaleString('zh-CN'))
let unreadTimer = null
let clockTimer = null

const isAdmin = computed(() => session.profile?.role === 'ADMIN')
const operatorName = computed(() => session.profile?.username || '用户')

const loadUnread = async () => {
  if (!session.token) return
  try {
    const summary = await fetchUnreadFeedback()
    unreadCount.value = Number(summary.unreadCount || 0)
    firstConversationId.value = summary.firstConversationId || ''
  } catch {
    unreadCount.value = 0
    firstConversationId.value = ''
  }
}

const openUnread = () => {
  const path = isAdmin.value ? '/feedback' : '/farmer-feedback'
  router.push({
    path,
    query: firstConversationId.value ? { conversationId: firstConversationId.value } : {}
  })
}

const logout = () => {
  session.signOut()
  router.push('/login')
}

onMounted(() => {
  loadUnread()
  unreadTimer = window.setInterval(loadUnread, 15000)
  clockTimer = window.setInterval(() => {
    currentDate.value = new Date().toLocaleString('zh-CN')
  }, 1000)
})

onBeforeUnmount(() => {
  if (unreadTimer) window.clearInterval(unreadTimer)
  if (clockTimer) window.clearInterval(clockTimer)
})

watch(() => route.fullPath, loadUnread)
</script>

<style scoped>
.console { min-height: 100vh; }
.console__aside {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 10;
  border-right: 1px solid var(--line);
  background: linear-gradient(180deg, #ffffff, #eff8eb);
  box-shadow: 16px 0 48px rgba(42, 91, 48, 0.1);
}
.console__body { min-height: 100vh; margin-left: 248px; }
.brand { display: flex; gap: 12px; align-items: center; padding: 24px 20px; }
.brand__mark {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--brand), var(--brand-strong));
  color: #fff;
  font-weight: 900;
}
.brand strong, .brand span { display: block; }
.brand strong { color: var(--ink); font-size: 18px; }
.brand span { margin-top: 4px; color: var(--muted); font-size: 12px; }
.nav { border: 0; background: transparent; }
.nav :deep(.el-menu-item) {
  position: relative;
  height: 48px;
  margin: 4px 12px;
  border-radius: var(--radius);
  color: #45604b;
  font-weight: 800;
}
.nav :deep(.el-menu-item.is-active), .nav :deep(.el-menu-item:hover) {
  background: rgba(83, 184, 106, 0.12);
  color: var(--brand-strong);
  box-shadow: inset 3px 0 0 var(--brand);
}
.menu-dot {
  position: absolute;
  right: 10px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: #f05252;
  color: #fff;
  font-size: 12px;
  line-height: 18px;
  text-align: center;
  font-style: normal;
}
.console__header {
  display: flex;
  height: 84px;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.84);
  backdrop-filter: blur(16px);
}
.console__header p, .console__header h1 { margin: 0; }
.console__header p { color: var(--muted); font-size: 13px; }
.console__header h1 { margin-top: 4px; color: var(--ink); font-size: 22px; }
.header-actions { display: flex; gap: 12px; align-items: center; }
.message-button {
  position: relative;
  display: inline-grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(255,255,255,.9);
  color: var(--ink);
  cursor: pointer;
}
.badge {
  position: absolute;
  top: -5px;
  right: -5px;
  min-width: 20px;
  height: 20px;
  padding: 0 5px;
  border: 2px solid #fff;
  border-radius: 999px;
  background: #f05252;
  color: #fff;
  font-size: 12px;
  line-height: 16px;
  font-style: normal;
}
.operator {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  border: 2px solid #1c2b22;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--ink);
  padding: 9px 16px;
  font-weight: 900;
}
.logout-item { color: #d44444; font-weight: 900; }
.console__main { min-height: calc(100vh - 84px); padding: 24px; }

.console--admin :deep(.el-button:not(.is-link):not(.is-text):not(.el-button--danger)) {
  border-color: rgba(83, 184, 106, 0.24);
  background: rgba(83, 184, 106, 0.1);
  color: var(--brand-strong);
  box-shadow: 0 10px 24px rgba(83, 184, 106, 0.12);
}

.console--admin :deep(.el-button:not(.is-link):not(.is-text):not(.el-button--danger):hover),
.console--admin :deep(.el-button:not(.is-link):not(.is-text):not(.el-button--danger):focus) {
  border-color: rgba(83, 184, 106, 0.42);
  background: rgba(83, 184, 106, 0.16);
  color: var(--brand-strong);
}
</style>
