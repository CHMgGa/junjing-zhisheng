<template>
  <section class="panel feedback-page" v-loading="loading">
    <aside class="conversation-list">
      <div class="chat-title">
        <h2 class="section-title">问题反馈</h2>
        <p class="muted">{{ isAdmin ? '选择农户会话，回复处理意见。' : '选择管理员，像聊天一样提交问题。' }}</p>
      </div>

      <button
        v-for="item in conversations"
        :key="itemKey(item)"
        class="conversation-item"
        :class="{ active: itemKey(item) === itemKey(selected || {}) }"
        type="button"
        @click="selectConversation(item)"
      >
        <el-avatar class="avatar" :size="54" :src="avatarUrl(item)">{{ avatarName(item) }}</el-avatar>
        <i v-if="Number(item.unread_count || 0)" class="conversation-badge">{{ item.unread_count }}</i>
        <div class="conversation-copy">
          <strong>{{ displayName(item) }}</strong>
          <span>{{ item.last_message || '暂无聊天记录' }}</span>
        </div>
      </button>
      <el-empty v-if="!conversations.length" description="暂无会话" />
    </aside>

    <main class="chat-panel">
      <template v-if="selected">
        <header class="chat-panel__head">
          <el-avatar :size="50" :src="avatarUrl(selected)">{{ avatarName(selected) }}</el-avatar>
          <div>
            <strong>{{ displayName(selected) }}</strong>
            <p>{{ isAdmin ? '农户反馈会话实时保存' : '管理员会话实时保存' }}</p>
          </div>
        </header>

        <div ref="messagesRef" class="messages">
          <div
            v-for="message in messages"
            :key="message.id"
            class="message"
            :class="{ mine: Number(message.sender_user_id) === Number(session.profile.id) }"
          >
            <el-avatar class="message-avatar" :size="38" :src="messageAvatar(message)">{{ messageAvatarName(message) }}</el-avatar>
            <div class="bubble">
              <img v-if="message.message_type === 'IMAGE' && message.image_url" :src="message.image_url" alt="反馈图片" />
              <p>{{ message.content }}</p>
            </div>
          </div>
          <el-empty v-if="!messages.length" description="还没有消息" />
        </div>

        <footer class="composer">
          <el-input
            v-model="draft"
            type="textarea"
            :rows="2"
            placeholder="输入反馈内容或处理意见"
            @keydown.enter="submitOnEnter"
          />
          <div class="upload-preview">
            <input ref="fileInput" type="file" accept="image/*" hidden @change="onImageChange" />
            <button type="button" class="upload-button" @click="fileInput?.click()">上传图片</button>
            <img v-if="imageData" :src="imageData" alt="待发送图片" />
          </div>
          <el-button type="primary" :loading="sending" @click="sendMessage">发送</el-button>
        </footer>
      </template>
      <el-empty v-else description="请选择一个会话" />
    </main>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchFeedbackConversations, fetchFeedbackMessages, sendFeedbackMessage } from '../services/user'
import { useSessionStore } from '../stores/session'

const emit = defineEmits(['feedback-read'])
const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const isAdmin = computed(() => session.profile?.role === 'ADMIN')
const loading = ref(false)
const sending = ref(false)
const conversations = ref([])
const selected = ref(null)
const messages = ref([])
const draft = ref('')
const imageData = ref('')
const fileInput = ref(null)
const messagesRef = ref(null)

const itemKey = item => item.conversation_id || item.id || `${item.admin_user_id || item.farmer_user_id}`
const displayName = item => isAdmin.value
  ? `${item.farmer_name || item.farmer_username || '农户'}`
  : `${item.display_name || item.username || '管理员'}`
const avatarName = item => displayName(item).slice(0, 1)

const defaultAvatar = (role, gender) => {
  const female = String(gender || '').toUpperCase() === 'FEMALE'
  if (String(role || '').toUpperCase() === 'ADMIN') return female ? '/avatars/female_admin.png' : '/avatars/male_admin.png'
  return female ? '/avatars/female_farmer.png' : '/avatars/male_farmer.jpg'
}

const avatarUrl = item => {
  if (isAdmin.value) return item.farmer_avatar_url || defaultAvatar('FARMER', item.farmer_gender)
  return item.avatar_url || item.admin_avatar_url || defaultAvatar('ADMIN', item.gender || item.admin_gender)
}

const messageRole = message => {
  if (Number(message.sender_user_id) === Number(session.profile.id)) return session.profile.role
  return isAdmin.value ? 'FARMER' : 'ADMIN'
}
const messageAvatar = message => message.avatar_url || defaultAvatar(messageRole(message), message.gender)
const messageAvatarName = message => `${message.display_name || message.username || '用户'}`.slice(0, 1)

const loadConversations = async (preferredId = route.query.conversationId) => {
  loading.value = true
  try {
    conversations.value = await fetchFeedbackConversations()
    let target = null
    if (preferredId) {
      target = conversations.value.find(item => String(item.conversation_id || item.id) === String(preferredId))
    }
    if (!target && selected.value) {
      target = conversations.value.find(item => itemKey(item) === itemKey(selected.value))
    }
    if (!target && conversations.value.length) target = conversations.value[0]
    if (target) await selectConversation(target, false)
  } finally {
    loading.value = false
  }
}

const selectConversation = async (item, updateQuery = true) => {
  selected.value = item
  messages.value = []
  const id = item.conversation_id || item.id
  if (id) {
    messages.value = await fetchFeedbackMessages(id)
    emit('feedback-read')
    const local = conversations.value.find(row => itemKey(row) === itemKey(item))
    if (local) local.unread_count = 0
    if (updateQuery) {
      router.replace({ path: route.path, query: { conversationId: id } })
    }
    await scrollMessagesToBottom()
  } else if (updateQuery) {
    router.replace({ path: route.path })
  }
}

const scrollMessagesToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

const onImageChange = event => {
  const file = event.target.files?.[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) {
    event.target.value = ''
    return ElMessage.warning('图片不能超过 2MB')
  }
  const reader = new FileReader()
  reader.onload = () => { imageData.value = reader.result }
  reader.readAsDataURL(file)
}

const submitOnEnter = event => {
  if (event.shiftKey || event.isComposing) return
  event.preventDefault()
  if (!sending.value) sendMessage()
}

const sendMessage = async () => {
  if (!selected.value) return
  if (!draft.value.trim() && !imageData.value) return ElMessage.warning('请输入消息内容或上传图片')
  sending.value = true
  try {
    const payload = isAdmin.value
      ? {
          conversationId: selected.value.conversation_id || selected.value.id || null,
          receiverUserId: selected.value.farmer_user_id,
          content: draft.value.trim() || '图片反馈',
          messageType: imageData.value ? 'IMAGE' : 'TEXT',
          imageUrl: imageData.value || null
        }
      : {
          adminUserId: selected.value.admin_user_id,
          content: draft.value.trim() || '图片反馈',
          messageType: imageData.value ? 'IMAGE' : 'TEXT',
          imageUrl: imageData.value || null
        }
    await sendFeedbackMessage(payload)
    draft.value = ''
    imageData.value = ''
    if (fileInput.value) fileInput.value.value = ''
    await loadConversations(payload.conversationId)
    await scrollMessagesToBottom()
  } finally {
    sending.value = false
  }
}

onMounted(() => loadConversations())
watch(() => route.query.conversationId, id => {
  if (id && String(selected.value?.conversation_id || selected.value?.id || '') !== String(id)) loadConversations(id)
})
</script>

<style scoped>
.feedback-page { display: grid; grid-template-columns: 340px minmax(0, 1fr); height: min(760px, calc(100vh - 132px)); min-height: 560px; padding: 0; overflow: hidden; }
.conversation-list { border-right: 1px solid var(--line); padding: 24px; background: rgba(255,255,255,.62); overflow: auto; }
.chat-title { margin-bottom: 18px; }
.conversation-item {
  position: relative;
  width: 100%;
  display: flex;
  gap: 14px;
  align-items: center;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: rgba(255,255,255,.88);
  padding: 14px;
  margin-bottom: 12px;
  text-align: left;
  cursor: pointer;
  transition: transform .18s ease, border-color .18s ease, box-shadow .18s ease;
}
.conversation-item:hover { transform: translateY(-1px); border-color: rgba(72, 158, 92, .45); }
.conversation-item.active { border-color: var(--brand); box-shadow: 0 14px 30px rgba(72, 158, 92, .15); }
.conversation-badge {
  position: absolute;
  left: 54px;
  top: 8px;
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
.avatar { flex: 0 0 auto; border: 2px solid rgba(255,255,255,.92); box-shadow: 0 10px 22px rgba(39, 80, 47, .14); }
.conversation-copy { min-width: 0; }
.conversation-copy strong, .conversation-copy span { display: block; }
.conversation-copy strong { color: var(--ink); }
.conversation-copy span { margin-top: 5px; color: var(--muted); font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.chat-panel { display: flex; min-width: 0; min-height: 0; flex-direction: column; }
.chat-panel__head { display: flex; align-items: center; gap: 14px; padding: 20px 24px; border-bottom: 1px solid var(--line); background: rgba(255,255,255,.48); }
.chat-panel__head strong { font-size: 22px; }
.chat-panel__head p { margin: 6px 0 0; color: var(--muted); }
.messages { flex: 1; min-height: 0; overflow: auto; padding: 20px 24px; }
.message { display: flex; align-items: flex-start; margin-bottom: 14px; }
.message.mine { justify-content: flex-end; }
.message-avatar { flex: 0 0 auto; margin-right: 10px; border: 2px solid rgba(255,255,255,.9); box-shadow: 0 8px 18px rgba(39, 80, 47, .13); }
.message.mine .message-avatar { order: 2; margin-right: 0; margin-left: 10px; }
.bubble {
  max-width: min(560px, 74%);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fff;
  padding: 12px 14px;
  box-shadow: 0 10px 24px rgba(35, 78, 45, .08);
}
.message.mine .bubble { background: #dff7de; border-color: rgba(78, 178, 88, .35); }
.bubble img { display: block; max-width: 280px; max-height: 190px; border-radius: 6px; margin-bottom: 8px; object-fit: cover; }
.bubble p { margin: 0; white-space: pre-wrap; }
.composer { display: grid; grid-template-columns: minmax(0, 1fr) 150px 96px; gap: 12px; align-items: stretch; padding: 14px 24px; border-top: 1px solid var(--line); background: rgba(255,255,255,.68); }
.composer :deep(.el-textarea__inner) { min-height: 60px !important; height: 60px; resize: none; }
.composer :deep(.el-button) { width: 96px; height: 60px; align-self: stretch; }
.upload-preview { display: grid; grid-template-columns: minmax(0, 1fr) 42px; align-items: stretch; gap: 8px; min-width: 0; height: 60px; }
.upload-button {
  width: 100%;
  height: 60px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fff;
  color: var(--ink);
  font-weight: 800;
  cursor: pointer;
}
.upload-preview img { width: 42px; height: 60px; border-radius: 8px; object-fit: cover; border: 1px solid var(--line); }

@media (max-width: 900px) {
  .feedback-page { grid-template-columns: 1fr; height: auto; }
  .conversation-list { border-right: 0; border-bottom: 1px solid var(--line); }
  .composer { grid-template-columns: 1fr; }
}
</style>
