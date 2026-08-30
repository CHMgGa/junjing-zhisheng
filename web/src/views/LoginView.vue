<template>
  <main class="login">
    <section class="login-brand">
      <span>智慧农业 · IoT · AI 调控</span>
      <h1>
        <span class="title-line">菌境智生 · 羊肚菌</span>
        <span class="title-line">智慧种植调控系统</span>
      </h1>
      <p>面向农户和管理员的大棚环境监测、设备管理、告警闭环与生产溯源平台。</p>
    </section>

    <section class="login-panel">
      <el-tabs v-model="mode" stretch>
        <el-tab-pane label="账号登录" name="password">
          <el-form label-position="top" @submit.prevent>
            <el-form-item>
              <el-input v-model.trim="loginForm.username" size="large" placeholder="用户名，如 admin1 / farmer001" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="loginForm.password" size="large" type="password" placeholder="密码" show-password />
            </el-form-item>
            <AgreementCheck v-model="loginAgreed" @open="openPolicy" />
            <el-button type="primary" size="large" :loading="loading" @click="submitLogin">进入系统</el-button>
            <el-button class="forgot-link" link @click="forgotVisible = true">忘记密码？</el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form label-position="top" @submit.prevent>
            <el-form-item>
              <el-input v-model.trim="registerForm.username" size="large" placeholder="用户名，不能以 admin 开头" />
            </el-form-item>
            <el-form-item>
              <el-input v-model.trim="registerForm.phone" size="large" placeholder="手机号，用于资料和联系信息" />
            </el-form-item>
            <el-form-item>
              <el-input v-model.trim="registerForm.email" size="large" placeholder="邮箱，验证码将发送到这里" />
            </el-form-item>
            <el-form-item label="性别">
              <el-select v-model="registerForm.gender" size="large" style="width: 100%">
                <el-option label="男" value="MALE" />
                <el-option label="女" value="FEMALE" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-input v-model="registerForm.password" size="large" type="password" placeholder="密码" show-password />
            </el-form-item>
            <div class="strength">
              <span>密码强度</span>
              <el-tag :type="strengthType">{{ passwordStrength }}</el-tag>
            </div>
            <el-form-item>
              <el-input v-model="registerForm.confirmPassword" size="large" type="password" placeholder="再次确认密码" show-password />
            </el-form-item>
            <el-form-item>
              <el-input v-model.trim="registerForm.verificationCode" size="large" placeholder="邮箱验证码">
                <template #append>
                  <el-button :disabled="!canSendRegisterCode" :loading="codeLoading.register" @click="sendRegisterCode">发送验证码</el-button>
                </template>
              </el-input>
            </el-form-item>
            <AgreementCheck v-model="registerAgreed" @open="openPolicy" />
            <el-button type="primary" size="large" :loading="loading" @click="submitRegister">注册并登录</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="policyVisible" :title="policy.title" width="680px">
      <div class="policy-content">{{ policy.content }}</div>
    </el-dialog>

    <el-dialog v-model="forgotVisible" title="重置密码" width="520px">
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="邮箱">
          <el-input v-model.trim="forgotForm.receiver" size="large" placeholder="请输入绑定的邮箱" />
        </el-form-item>
        <el-form-item label="邮箱验证码">
          <el-input v-model.trim="forgotForm.verificationCode" size="large" placeholder="邮箱验证码">
            <template #append>
              <el-button :disabled="!canSendResetCode" :loading="codeLoading.reset" @click="sendResetCode">发送验证码</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="forgotForm.newPassword" size="large" type="password" placeholder="新密码" show-password />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="forgotForm.confirmPassword" size="large" type="password" placeholder="确认新密码" show-password />
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading" @click="submitReset">确认重置</el-button>
      </el-form>
    </el-dialog>
  </main>
</template>

<script setup>
import { computed, defineComponent, h, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElButton, ElCheckbox, ElMessage } from 'element-plus'
import { fetchPolicy, resetPassword, sendCode } from '../services/auth'
import { useSessionStore } from '../stores/session'

const AgreementCheck = defineComponent({
  props: { modelValue: Boolean },
  emits: ['update:modelValue', 'open'],
  setup(props, { emit }) {
    return () => h('div', { class: 'agreement' }, [
      h(ElCheckbox, {
        modelValue: props.modelValue,
        'onUpdate:modelValue': value => emit('update:modelValue', value)
      }, () => '我已阅读并同意'),
      h(ElButton, { link: true, type: 'primary', onClick: () => emit('open', 'privacy') }, () => '隐私政策'),
      h('span', '和'),
      h(ElButton, { link: true, type: 'primary', onClick: () => emit('open', 'terms') }, () => '服务条款')
    ])
  }
})

const router = useRouter()
const session = useSessionStore()
const mode = ref('password')
const loading = ref(false)
const loginAgreed = ref(false)
const registerAgreed = ref(false)
const policyVisible = ref(false)
const forgotVisible = ref(false)
const policy = reactive({ title: '', content: '' })
const codeLoading = reactive({ register: false, reset: false })
const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', phone: '', email: '', gender: 'MALE', password: '', confirmPassword: '', verificationCode: '' })
const forgotForm = reactive({ receiver: '', verificationCode: '', newPassword: '', confirmPassword: '' })

const canSendRegisterCode = computed(() => /.+@.+\..+/.test(registerForm.email))
const canSendResetCode = computed(() => /.+@.+\..+/.test(forgotForm.receiver))

const passwordStrength = computed(() => {
  const password = registerForm.password
  let score = 0
  if (password.length >= 8) score += 1
  if (/[A-Z]/.test(password) && /[a-z]/.test(password)) score += 1
  if (/\d/.test(password) && /[^A-Za-z0-9]/.test(password)) score += 1
  return score >= 3 ? '强' : score === 2 ? '中等' : '弱'
})
const strengthType = computed(() => passwordStrength.value === '强' ? 'success' : passwordStrength.value === '中等' ? 'warning' : 'danger')

const requireAgreement = agreed => {
  if (!agreed) {
    ElMessage.warning('请先阅读并同意隐私政策和服务条款')
    return false
  }
  return true
}

const submitLogin = async () => {
  if (!loginForm.username) return ElMessage.warning('用户名不能为空')
  if (!loginForm.password) return ElMessage.warning('密码不能为空')
  if (!requireAgreement(loginAgreed.value)) return
  loading.value = true
  try {
    await session.signIn(loginForm)
    ElMessage.success('登录成功')
    router.replace(session.profile?.role === 'FARMER' ? '/farmer' : '/')
  } finally {
    loading.value = false
  }
}

const submitRegister = async () => {
  if (!registerForm.username) return ElMessage.warning('用户名不能为空')
  if (registerForm.username.toLowerCase().startsWith('admin')) return ElMessage.warning('农户用户名不能以 admin 开头')
  if (!registerForm.phone) return ElMessage.warning('手机号不能为空')
  if (!registerForm.email) return ElMessage.warning('邮箱不能为空')
  if (!registerForm.password) return ElMessage.warning('密码不能为空')
  if (registerForm.password !== registerForm.confirmPassword) return ElMessage.warning('两次输入的密码不一致')
  if (!registerForm.verificationCode) return ElMessage.warning('验证码不能为空')
  if (!requireAgreement(registerAgreed.value)) return
  loading.value = true
  try {
    await session.signUp(registerForm)
    ElMessage.success('注册成功')
    router.replace('/farmer')
  } finally {
    loading.value = false
  }
}

const submitReset = async () => {
  if (!forgotForm.receiver) return ElMessage.warning('邮箱不能为空')
  if (!forgotForm.verificationCode) return ElMessage.warning('验证码不能为空')
  if (!forgotForm.newPassword) return ElMessage.warning('新密码不能为空')
  if (forgotForm.newPassword !== forgotForm.confirmPassword) return ElMessage.warning('两次输入的密码不一致')
  loading.value = true
  try {
    await resetPassword(forgotForm)
    ElMessage.success('密码已重置，请重新登录')
    forgotVisible.value = false
  } finally {
    loading.value = false
  }
}

const sendRegisterCode = () => requestCode(registerForm.email, 'REGISTER', 'register')
const sendResetCode = () => requestCode(forgotForm.receiver, 'RESET_PASSWORD', 'reset')

const requestCode = async (receiver, scene, key) => {
  if (!/.+@.+\..+/.test(receiver)) return ElMessage.warning('请先填写正确的邮箱地址')
  codeLoading[key] = true
  try {
    const res = await sendCode({ receiver, scene })
    const devText = res.devCode ? `，开发验证码：${res.devCode}` : ''
    ElMessage.success(`${res.message}${devText}`)
  } finally {
    codeLoading[key] = false
  }
}

const openPolicy = async (type) => {
  Object.assign(policy, await fetchPolicy(type))
  policyVisible.value = true
}
</script>

<style scoped>
.login {
  position: relative;
  display: grid;
  grid-template-columns: minmax(420px, 0.95fr) minmax(460px, 560px);
  gap: 48px;
  min-height: 100vh;
  align-items: center;
  padding: 56px clamp(48px, 7vw, 120px);
  background:
    radial-gradient(circle at 18% 18%, rgba(83, 184, 106, 0.2), transparent 34%),
    radial-gradient(circle at 86% 18%, rgba(213, 155, 56, 0.12), transparent 30%),
    linear-gradient(135deg, #eef8ea, #f8fcf4 52%, #e5f4df);
}

.login-brand {
  max-width: 620px;
}

.login-brand span {
  display: inline-block;
  margin-bottom: 14px;
  color: var(--brand-strong);
  font-weight: 900;
}

.login-brand h1 {
  margin: 0;
  color: var(--ink);
  font-size: clamp(38px, 4.4vw, 62px);
  line-height: 1.08;
  letter-spacing: 0;
}

.login-brand .title-line {
  display: block;
}

.login-brand p {
  max-width: none;
  margin: 22px 0 0;
  color: var(--muted);
  font-size: 17px;
  line-height: 1.9;
  white-space: nowrap;
}

.login-panel {
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: var(--shadow);
  padding: 38px 42px;
}

.login-panel :deep(.el-tabs__item) {
  color: var(--muted);
  font-weight: 900;
}

.login-panel :deep(.el-tabs__item.is-active) {
  color: var(--brand-strong);
}

.login-panel .el-button:not(.is-link),
.el-dialog .el-button:not(.is-link) {
  width: 100%;
}

.forgot-link {
  margin-top: 12px;
}

.agreement {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  margin: 4px 0 18px;
  color: var(--muted);
}

.strength {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -8px 0 12px;
  color: var(--muted);
}

.policy-content {
  white-space: pre-line;
  line-height: 1.9;
  color: var(--ink);
}

@media (max-width: 980px) {
  .login {
    grid-template-columns: 1fr;
    padding: 32px 22px;
  }

  .login-brand h1 {
    font-size: 34px;
  }
}
</style>
