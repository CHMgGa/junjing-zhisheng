<template>
  <section class="panel" v-loading="loading">
    <div class="panel-head">
      <div>
        <h2 class="section-title">农户与管理员管理</h2>
        <p class="muted">支持按控制大棚、账号、手机号和邮箱查询用户，并维护农户的大棚绑定关系。</p>
      </div>
      <div class="head-actions">
        <el-button @click="openCreate('FARMER')">新增农户</el-button>
        <el-button type="primary" @click="openCreate('ADMIN')">新增管理员</el-button>
      </div>
    </div>

    <div class="filters">
      <label class="filter-field">
        <span>关键词</span>
        <el-input
          v-model.trim="filters.keyword"
          clearable
          placeholder="账号、手机号、邮箱、昵称"
          @keyup.enter="load"
        />
      </label>
      <label class="filter-field">
        <span>绑定大棚</span>
        <el-input
          v-model.trim="filters.greenhouse"
          clearable
          placeholder="输入大棚名称"
          @keyup.enter="load"
        />
      </label>
      <div class="filter-actions">
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>
    </div>

    <el-table :data="users" style="width: 100%; margin-top: 16px" row-key="id">
      <el-table-column label="头像" width="82">
        <template #default="{ row }">
          <el-avatar :size="42" :src="row.avatar_url || defaultAvatar(row.role_code, row.gender)">{{ avatarText(row) }}</el-avatar>
        </template>
      </el-table-column>
      <el-table-column prop="username" label="账号" min-width="140" />
      <el-table-column prop="display_name" label="昵称" min-width="130" />
      <el-table-column prop="role_code" label="角色" width="110">
        <template #default="{ row }">{{ row.role_code === 'ADMIN' ? '管理员' : '农户' }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" min-width="140" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="greenhouse_count" label="控制大棚" width="110" />
      <el-table-column prop="enabled" label="状态" width="120">
        <template #default="{ row }">
          <el-button :type="row.enabled ? 'success' : 'info'" size="small" plain @click="toggleEnabled(row)">
            {{ row.enabled ? '启用中' : '已停用' }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button class="table-action table-action--edit" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.role_code === 'FARMER'" class="table-action table-action--bind" @click="openBind(row)">大棚绑定</el-button>
            <el-button class="table-action table-action--danger" @click="removeUser(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="userDialog" :title="editingId ? '编辑用户' : '新增用户'" width="560px">
      <el-form label-position="top">
        <el-form-item label="角色">
          <el-select v-model="userForm.roleCode" style="width: 100%">
            <el-option label="农户" value="FARMER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号"><el-input v-model.trim="userForm.username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="userForm.password" type="password" show-password placeholder="留空则不修改，新增默认 123456" /></el-form-item>
        <el-form-item label="昵称"><el-input v-model.trim="userForm.displayName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model.trim="userForm.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model.trim="userForm.email" /></el-form-item>
        <el-form-item label="性别">
          <el-select v-model="userForm.gender" style="width: 100%">
            <el-option label="未知" value="UNKNOWN" />
            <el-option label="男" value="MALE" />
            <el-option label="女" value="FEMALE" />
          </el-select>
        </el-form-item>
        <el-form-item label="简介"><el-input v-model.trim="userForm.bio" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="userForm.enabled" /></el-form-item>
        <div class="dialog-actions">
          <el-button @click="userDialog = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitUser">保存</el-button>
        </div>
      </el-form>
    </el-dialog>

    <el-dialog v-model="bindDialog" title="农户大棚绑定" width="680px">
      <p class="muted">一个大棚同一时间只能绑定给一个农户。可以保存多选绑定，也可以单独解除某个大棚。</p>
      <el-select v-model="bindingIds" multiple filterable style="width: 100%; margin-top: 12px" placeholder="选择大棚">
        <el-option v-for="item in greenhouses" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <div class="bound-list">
        <article v-for="item in boundGreenhouses" :key="item.id">
          <div>
            <strong>{{ item.name }}</strong>
            <p>{{ item.location || '-' }} · {{ item.crop_stage || '未填写阶段' }}</p>
          </div>
          <el-button class="unbind-button" size="small" @click="unbindOne(item)">解除绑定</el-button>
        </article>
      </div>
      <div class="dialog-actions">
        <el-button @click="bindDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitBind">保存绑定</el-button>
      </div>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchGreenhouses } from '../services/greenhouse'
import {
  bindFarmerGreenhouses,
  createUser,
  deleteUser,
  fetchFarmerGreenhouses,
  fetchUsers,
  unbindFarmerGreenhouse,
  updateUser
} from '../services/user'

const loading = ref(false)
const saving = ref(false)
const users = ref([])
const greenhouses = ref([])
const userDialog = ref(false)
const bindDialog = ref(false)
const editingId = ref(null)
const bindingUser = ref(null)
const bindingIds = ref([])
const boundGreenhouses = ref([])
const filters = reactive({ keyword: '', greenhouse: '' })
const userForm = reactive({ username: '', password: '', roleCode: 'FARMER', phone: '', email: '', displayName: '', gender: 'UNKNOWN', bio: '', enabled: true })

const defaultAvatar = (role, gender) => {
  const female = String(gender || '').toUpperCase() === 'FEMALE'
  if (String(role || '').toUpperCase() === 'ADMIN') return female ? '/avatars/female_admin.png' : '/avatars/male_admin.png'
  return female ? '/avatars/female_farmer.png' : '/avatars/male_farmer.jpg'
}

const avatarText = row => `${row.display_name || row.username || '用'}`.slice(0, 1)
const rowToPayload = row => ({
  username: row.username,
  password: '',
  roleCode: row.role_code,
  phone: row.phone || '',
  email: row.email || '',
  displayName: row.display_name || '',
  gender: row.gender || 'UNKNOWN',
  bio: row.bio || '',
  enabled: Boolean(row.enabled)
})

const load = async () => {
  loading.value = true
  try {
    const [userRows, greenhouseRows] = await Promise.all([
      fetchUsers({ keyword: filters.keyword || undefined, greenhouse: filters.greenhouse || undefined }),
      fetchGreenhouses()
    ])
    users.value = userRows
    greenhouses.value = greenhouseRows
  } finally {
    loading.value = false
  }
}

const resetFilters = async () => {
  Object.assign(filters, { keyword: '', greenhouse: '' })
  await load()
}

const resetForm = role => Object.assign(userForm, { username: role === 'ADMIN' ? 'admin' : '', password: '', roleCode: role, phone: '', email: '', displayName: '', gender: 'UNKNOWN', bio: '', enabled: true })
const openCreate = role => { editingId.value = null; resetForm(role); userDialog.value = true }
const openEdit = row => { editingId.value = row.id; Object.assign(userForm, rowToPayload(row)); userDialog.value = true }

const submitUser = async () => {
  if (!userForm.username) return ElMessage.warning('请填写账号')
  saving.value = true
  try {
    if (editingId.value) await updateUser(editingId.value, userForm)
    else await createUser(userForm)
    ElMessage.success('用户已保存')
    userDialog.value = false
    await load()
  } finally {
    saving.value = false
  }
}

const toggleEnabled = async row => {
  const payload = rowToPayload(row)
  payload.enabled = !row.enabled
  await updateUser(row.id, payload)
  ElMessage.success(payload.enabled ? '账号已启用' : '账号已停用')
  await load()
}

const removeUser = async row => {
  await ElMessageBox.confirm(`确认删除用户“${row.username}”？`, '删除用户', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('用户已删除')
  await load()
}

const openBind = async row => {
  bindingUser.value = row
  boundGreenhouses.value = await fetchFarmerGreenhouses(row.id)
  bindingIds.value = boundGreenhouses.value.map(item => item.id)
  bindDialog.value = true
}

const submitBind = async () => {
  saving.value = true
  try {
    await bindFarmerGreenhouses(bindingUser.value.id, { greenhouseIds: bindingIds.value })
    ElMessage.success('大棚绑定已保存')
    boundGreenhouses.value = await fetchFarmerGreenhouses(bindingUser.value.id)
    bindDialog.value = false
    await load()
  } finally {
    saving.value = false
  }
}

const unbindOne = async greenhouse => {
  await ElMessageBox.confirm(`确认解除“${greenhouse.name}”与该农户的绑定？`, '解除绑定', { type: 'warning' })
  await unbindFarmerGreenhouse(bindingUser.value.id, greenhouse.id)
  ElMessage.success('绑定已解除')
  boundGreenhouses.value = await fetchFarmerGreenhouses(bindingUser.value.id)
  bindingIds.value = boundGreenhouses.value.map(item => item.id)
  await load()
}

onMounted(load)
</script>

<style scoped>
.panel-head,
.head-actions,
.filters,
.dialog-actions {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.filters {
  align-items: end;
  justify-content: start;
  flex-wrap: wrap;
  width: fit-content;
  max-width: 100%;
  margin-top: 18px;
  padding: 12px;
  border: 1px solid rgba(73, 125, 78, 0.12);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.48);
}
.filter-field {
  display: grid;
  gap: 6px;
  width: 280px;
}
.filter-field span {
  color: var(--muted);
  font-size: 13px;
  font-weight: 800;
}
.filter-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.filter-actions :deep(.el-button) {
  min-width: 78px;
  height: 40px;
  margin-left: 0;
}
.table-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.table-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}
.table-action {
  height: 30px;
  min-width: 56px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 800;
  box-shadow: none;
}
.table-action--edit {
  border-color: rgba(83, 184, 106, 0.2);
  background: rgba(83, 184, 106, 0.1);
  color: var(--brand-strong);
}
.table-action--bind {
  border-color: rgba(53, 184, 166, 0.22);
  background: rgba(53, 184, 166, 0.1);
  color: #258877;
}
.table-action--danger,
.unbind-button {
  border-color: rgba(230, 79, 90, 0.22);
  background: rgba(230, 79, 90, 0.08);
  color: #d9535d;
}
.table-action--edit:hover,
.table-action--edit:focus {
  border-color: rgba(83, 184, 106, 0.42);
  background: rgba(83, 184, 106, 0.16);
  color: var(--brand-strong);
}
.table-action--bind:hover,
.table-action--bind:focus {
  border-color: rgba(53, 184, 166, 0.42);
  background: rgba(53, 184, 166, 0.16);
  color: #258877;
}
.table-action--danger:hover,
.table-action--danger:focus,
.unbind-button:hover,
.unbind-button:focus {
  border-color: rgba(230, 79, 90, 0.38);
  background: rgba(230, 79, 90, 0.14);
  color: #c9444f;
}
.dialog-actions { justify-content: flex-end; margin-top: 18px; }
.bound-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}
.bound-list article {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: rgba(255,255,255,.72);
}
.bound-list p { margin: 4px 0 0; color: var(--muted); }
.unbind-button {
  height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  font-weight: 800;
  box-shadow: none;
}
</style>
