<template>
  <div class="profile-page" v-loading="loading">
    <section class="panel profile-head">
      <el-upload :show-file-list="false" :auto-upload="false" accept="image/*" @change="onAvatarChange">
        <el-avatar :size="92" :src="form.avatarUrl">{{ avatarText }}</el-avatar>
      </el-upload>
      <div>
        <h2 class="section-title">{{ form.username || '个人中心' }}</h2>
        <p class="muted">IP 方位：{{ ipLocationText }}</p>
      </div>
    </section>

    <div class="profile-stack">
      <section class="panel">
        <h3 class="card-title">信息设置</h3>
        <el-form label-width="96px" class="profile-form">
          <el-form-item label="用户名">
            <el-input v-model.trim="form.username" />
          </el-form-item>
          <el-form-item v-if="!isAdmin" label="昵称">
            <el-input v-model.trim="form.displayName" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model.trim="form.phone" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model.trim="form.email" />
          </el-form-item>
          <el-form-item label="性别">
            <el-select v-model="form.gender" style="width: 100%" @change="onGenderChange">
              <el-option label="男" value="MALE" />
              <el-option label="女" value="FEMALE" />
              <el-option label="未设置" value="UNKNOWN" />
            </el-select>
          </el-form-item>
          <el-form-item label="个人简介">
            <el-input v-model="form.bio" type="textarea" :rows="4" />
          </el-form-item>
        </el-form>
        <div class="save-actions">
          <el-button class="save-button" type="primary" :loading="saving" @click="save">保存资料</el-button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useSessionStore } from '../stores/session'
import { fetchProfile, updateProfile } from '../services/user'

const session = useSessionStore()
const loading = ref(false)
const saving = ref(false)
const usingDefaultAvatar = ref(true)
const form = reactive({
  username: '',
  phone: '',
  email: '',
  displayName: '',
  avatarUrl: '',
  gender: 'UNKNOWN',
  bio: '',
  allowAdminDelete: false,
  lastLoginIp: ''
})

const isAdmin = computed(() => session.profile?.role === 'ADMIN')
const avatarText = computed(() => (form.username || '用户').slice(0, 1))
const ipLocationText = computed(() => formatIpLocation(form.realtimeIp || form.lastLoginIp))

const defaultAvatar = (role, gender) => {
  const female = String(gender || '').toUpperCase() === 'FEMALE'
  if (String(role || '').toUpperCase() === 'ADMIN') return female ? '/avatars/female_admin.png' : '/avatars/male_admin.png'
  return female ? '/avatars/female_farmer.png' : '/avatars/male_farmer.jpg'
}

const formatIpLocation = ip => {
  if (!ip) return '未知'
  if (ip === '0:0:0:0:0:0:0:1' || ip === '::1' || ip === '127.0.0.1') return '辽宁省沈阳市（本机访问）'
  if (/^(10\.|192\.168\.|172\.(1[6-9]|2\d|3[01])\.)/.test(ip)) return '局域网访问'
  return `公网 IP：${ip}`
}

const load = async () => {
  loading.value = true
  try {
    const profile = await fetchProfile(session.profile.id)
    const role = profile.role_code || session.profile.role
    const gender = profile.gender || 'UNKNOWN'
    const avatarUrl = profile.avatar_url || profile.avatarUrl || defaultAvatar(role, gender)
    Object.assign(form, {
      ...profile,
      displayName: profile.display_name || profile.displayName || '',
      avatarUrl,
      gender,
      allowAdminDelete: Boolean(profile.allow_admin_delete ?? profile.allowAdminDelete)
    })
    usingDefaultAvatar.value = avatarUrl === defaultAvatar(role, gender) || avatarUrl === defaultAvatar(role, 'MALE') || avatarUrl === defaultAvatar(role, 'FEMALE')
  } finally {
    loading.value = false
  }
}

const onGenderChange = gender => {
  if (usingDefaultAvatar.value) {
    form.avatarUrl = defaultAvatar(session.profile.role, gender)
  }
}

const save = async () => {
  if (!form.username) return ElMessage.warning('用户名不能为空')
  if (isAdmin.value && !form.username.toLowerCase().startsWith('admin')) {
    return ElMessage.warning('管理员用户名必须以 admin 开头')
  }
  saving.value = true
  try {
    const profile = await updateProfile(session.profile.id, {
      username: form.username,
      phone: form.phone,
      email: form.email,
      displayName: isAdmin.value ? '' : form.displayName,
      avatarUrl: form.avatarUrl,
      gender: form.gender,
      bio: form.bio,
      allowAdminDelete: form.allowAdminDelete
    })
    Object.assign(form, {
      ...profile,
      displayName: profile.display_name || profile.displayName || '',
      avatarUrl: profile.avatar_url || profile.avatarUrl || '',
      allowAdminDelete: Boolean(profile.allow_admin_delete ?? profile.allowAdminDelete)
    })
    session.profile = { ...session.profile, ...profile, username: profile.username, role: profile.role_code || session.profile.role }
    localStorage.setItem('greenhouse_profile', JSON.stringify(session.profile))
    ElMessage.success('资料已保存')
  } finally {
    saving.value = false
  }
}

const onAvatarChange = file => {
  const reader = new FileReader()
  reader.onload = () => {
    form.avatarUrl = reader.result
    usingDefaultAvatar.value = false
  }
  reader.readAsDataURL(file.raw)
}

onMounted(load)
</script>

<style scoped>
.profile-page { display: grid; gap: 18px; max-width: 1160px; }
.profile-head {
  display: flex;
  gap: 18px;
  align-items: center;
}
.profile-head :deep(.el-avatar) { cursor: pointer; border: 1px solid var(--line); background: #fff; }
.profile-stack { display: grid; gap: 18px; }
.card-title { margin: 0 0 18px; color: var(--ink); font-size: 20px; }
.profile-form { max-width: 980px; margin: 0 auto; }
.save-actions {
  display: flex;
  justify-content: center;
  margin-top: 18px;
}
.save-button { width: 220px; margin-top: 18px; }
.save-actions .save-button { margin-top: 0; }
@media (max-width: 760px) {
  .profile-head { align-items: flex-start; }
  .save-button { width: 100%; }
}
</style>
