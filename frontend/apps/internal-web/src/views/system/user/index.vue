<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { internalApi } from '../../../api/internal-api'
import { usePermission } from '../../../composables/usePermission'

interface UserItem { id:number; username:string; displayName:string; employeeCode:string; email:string; phone:string; status:string; mainOrganizationId:number|null; mainDepartmentId:number|null; mainPositionId:number|null; mainOrganizationName:string; mainDepartmentName:string; mainPositionName:string; mustChangePassword:boolean; lastLoginAt:string; createdAt:string }
interface AssignmentHistory { fieldName:string; oldValue:string; newValue:string; changeReason:string; changedBy:string; changedAt:string }
interface UserDetail extends UserItem { roles:string[]; assignmentHistory:AssignmentHistory[] }
interface OrgNode { id:number; orgName:string; orgType:string; children?:OrgNode[] }
interface DeptOption { id:number; deptName:string }
interface PosOption { id:number; positionName:string }

const canCreate = usePermission('system:user:create')
const canUpdate = usePermission('system:user:update')
const canEnable = usePermission('system:user:enable')
const canDisable = usePermission('system:user:disable')
const canResetPassword = usePermission('system:user:reset-password')
const loading = ref(false)
const detailLoading = ref(false)
const list = ref<UserItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const searchForm = reactive({ username:'', displayName:'', status:'' })
const dialogVisible = ref(false)
const dialogMode = ref<'create'|'edit'|'view'>('create')
const form = reactive({ id:null as number|null, username:'', password:'', displayName:'', employeeCode:'', email:'', phone:'', mainOrganizationId:null as number|null, mainDepartmentId:null as number|null, mainPositionId:null as number|null })
const detail = ref<UserDetail|null>(null)
const resetPwdVisible = ref(false)
const tempPassword = ref('')
const orgOptions = ref<OrgNode[]>([])
const deptOptions = ref<DeptOption[]>([])
const posOptions = ref<PosOption[]>([])

function flattenOrganizations(nodes: OrgNode[]): OrgNode[] {
  return nodes.flatMap(node => [node, ...flattenOrganizations(node.children ?? [])])
}

async function loadList() {
  loading.value = true
  try {
    const qs = new URLSearchParams({ page:String(page.value), pageSize:String(pageSize.value) })
    if (searchForm.username) qs.set('username', searchForm.username)
    if (searchForm.displayName) qs.set('displayName', searchForm.displayName)
    if (searchForm.status) qs.set('status', searchForm.status)
    const result = await internalApi.get<{items:UserItem[];total:number}>('/system/users?' + qs)
    list.value = result?.items ?? []; total.value = result?.total ?? 0
  } catch { ElMessage.error('加载用户失败') }
  finally { loading.value = false }
}

async function loadOrgs() {
  try { orgOptions.value = flattenOrganizations(await internalApi.get<OrgNode[]>('/master-data/organizations') ?? []) }
  catch { orgOptions.value = [] }
}

async function loadDepts(orgId:number|null, reset = true) {
  if (reset) { form.mainDepartmentId = null; form.mainPositionId = null }
  deptOptions.value = orgId ? await internalApi.get<DeptOption[]>('/system/departments?organizationId=' + orgId) ?? [] : []
  if (reset) posOptions.value = []
}

async function loadPositions(deptId:number|null, reset = true) {
  if (reset) form.mainPositionId = null
  posOptions.value = deptId ? await internalApi.get<PosOption[]>('/system/positions?departmentId=' + deptId) ?? [] : []
}

function openCreate() {
  dialogMode.value = 'create'; detail.value = null
  Object.assign(form, { id:null, username:'', password:'', displayName:'', employeeCode:'', email:'', phone:'', mainOrganizationId:null, mainDepartmentId:null, mainPositionId:null })
  deptOptions.value = []; posOptions.value = []; dialogVisible.value = true
}

async function openUser(row:UserItem, mode:'edit'|'view') {
  detailLoading.value = true
  try {
    const current = await internalApi.get<UserDetail>('/system/users/' + row.id)
    if (!current) return
    detail.value = current; dialogMode.value = mode
    Object.assign(form, { id:current.id, username:current.username, password:'', displayName:current.displayName, employeeCode:current.employeeCode, email:current.email, phone:current.phone, mainOrganizationId:current.mainOrganizationId, mainDepartmentId:current.mainDepartmentId, mainPositionId:current.mainPositionId })
    await loadDepts(current.mainOrganizationId, false)
    await loadPositions(current.mainDepartmentId, false)
    dialogVisible.value = true
  } catch { ElMessage.error('加载用户详情失败') }
  finally { detailLoading.value = false }
}

async function saveUser() {
  try {
    if (dialogMode.value === 'create') await internalApi.post('/system/users', { ...form })
    else await internalApi.request('/system/users/' + form.id, { method:'PUT', body:{ displayName:form.displayName, employeeCode:form.employeeCode, email:form.email, phone:form.phone, mainOrganizationId:form.mainOrganizationId, mainDepartmentId:form.mainDepartmentId, mainPositionId:form.mainPositionId } })
    dialogVisible.value = false; await loadList(); ElMessage.success('保存成功')
  } catch (e:unknown) { ElMessage.error((e as Error)?.message || '保存失败') }
}

async function toggleUser(row:UserItem, action:'enable'|'disable') {
  try { await ElMessageBox.confirm(`确认${action === 'enable' ? '启用' : '停用'}用户 ${row.username}？`); await internalApi.post('/system/users/' + row.id + '/' + action); await loadList(); ElMessage.success('操作成功') }
  catch { /* 用户取消或接口已提示 */ }
}

async function resetPassword(row:UserItem) {
  try { await ElMessageBox.confirm(`确认重置用户 ${row.username} 的密码？`); const result = await internalApi.post<{temporaryPassword:string}>('/system/users/' + row.id + '/reset-password'); tempPassword.value = result?.temporaryPassword ?? ''; resetPwdVisible.value = true }
  catch { /* 用户取消或接口已提示 */ }
}

function onSearch() { page.value = 1; loadList() }
function onReset() { Object.assign(searchForm, { username:'', displayName:'', status:'' }); onSearch() }
onMounted(() => { loadList(); loadOrgs() })
</script>

<template>
  <div class="user-page">
    <el-form :inline="true" size="small"><el-form-item label="用户名"><el-input v-model="searchForm.username" clearable/></el-form-item><el-form-item label="显示名"><el-input v-model="searchForm.displayName" clearable/></el-form-item><el-form-item label="状态"><el-select v-model="searchForm.status" clearable><el-option label="正常" value="ACTIVE"/><el-option label="停用" value="DISABLED"/></el-select></el-form-item><el-form-item><el-button type="primary" @click="onSearch">查询</el-button><el-button @click="onReset">重置</el-button></el-form-item><el-form-item><el-button v-if="canCreate" type="success" @click="openCreate">新增</el-button></el-form-item></el-form>
    <el-table :data="list" v-loading="loading" border size="small" empty-text="暂无数据">
      <el-table-column prop="username" label="用户名" width="120"/><el-table-column prop="displayName" label="显示名" width="120"/><el-table-column prop="employeeCode" label="工号" width="100"/><el-table-column prop="email" label="邮箱" min-width="170"/><el-table-column prop="mainOrganizationName" label="主组织" width="140"/><el-table-column prop="mainDepartmentName" label="主部门" width="120"/>
      <el-table-column prop="status" label="状态" width="80"><template #default="{row}"><el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">{{ row.status === 'ACTIVE' ? '正常' : '停用' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="310" fixed="right"><template #default="{row}"><el-button link size="small" @click="openUser(row, 'view')">详情</el-button><el-button v-if="canUpdate" link type="primary" size="small" @click="openUser(row, 'edit')">编辑</el-button><el-button v-if="row.status === 'ACTIVE' ? canDisable : canEnable" link size="small" @click="toggleUser(row, row.status === 'ACTIVE' ? 'disable' : 'enable')">{{ row.status === 'ACTIVE' ? '停用' : '启用' }}</el-button><el-button v-if="canResetPassword" link type="warning" size="small" @click="resetPassword(row)">重置密码</el-button></template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" @current-change="loadList" layout="total,prev,pager,next" class="pager"/>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增用户' : dialogMode === 'edit' ? '编辑用户' : '用户详情'" width="620px" v-loading="detailLoading">
      <el-form label-width="90px" size="small" :disabled="dialogMode === 'view'">
        <el-row :gutter="12"><el-col :span="12"><el-form-item label="用户名"><el-input v-model="form.username" :disabled="dialogMode !== 'create'"/></el-form-item></el-col><el-col :span="12"><el-form-item v-if="dialogMode === 'create'" label="初始密码"><el-input v-model="form.password" type="password" show-password placeholder="至少12位"/></el-form-item></el-col></el-row>
        <el-row :gutter="12"><el-col :span="12"><el-form-item label="显示名"><el-input v-model="form.displayName"/></el-form-item></el-col><el-col :span="12"><el-form-item label="工号"><el-input v-model="form.employeeCode"/></el-form-item></el-col></el-row>
        <el-row :gutter="12"><el-col :span="12"><el-form-item label="邮箱"><el-input v-model="form.email"/></el-form-item></el-col><el-col :span="12"><el-form-item label="手机"><el-input v-model="form.phone"/></el-form-item></el-col></el-row>
        <el-form-item label="主组织"><el-select v-model="form.mainOrganizationId" filterable clearable @change="loadDepts(form.mainOrganizationId)"><el-option v-for="o in orgOptions" :key="o.id" :label="o.orgName + ' (' + o.orgType + ')'" :value="o.id"/></el-select></el-form-item>
        <el-form-item label="主部门"><el-select v-model="form.mainDepartmentId" clearable :disabled="!form.mainOrganizationId" @change="loadPositions(form.mainDepartmentId)"><el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName" :value="d.id"/></el-select></el-form-item>
        <el-form-item label="主岗位"><el-select v-model="form.mainPositionId" clearable :disabled="!form.mainDepartmentId"><el-option v-for="p in posOptions" :key="p.id" :label="p.positionName" :value="p.id"/></el-select></el-form-item>
      </el-form>
      <el-descriptions v-if="dialogMode === 'view' && detail" title="授权与状态" :column="2" border size="small"><el-descriptions-item label="角色">{{ detail.roles.join('、') || '-' }}</el-descriptions-item><el-descriptions-item label="首次改密">{{ detail.mustChangePassword ? '是' : '否' }}</el-descriptions-item><el-descriptions-item label="最近登录">{{ detail.lastLoginAt || '-' }}</el-descriptions-item><el-descriptions-item label="创建时间">{{ detail.createdAt }}</el-descriptions-item></el-descriptions>
      <el-table v-if="dialogMode === 'view' && detail" :data="detail.assignmentHistory" size="small" border class="history" empty-text="暂无调岗记录"><el-table-column prop="fieldName" label="变更字段"/><el-table-column prop="oldValue" label="原值"/><el-table-column prop="newValue" label="新值"/><el-table-column prop="changedAt" label="时间" width="170"/></el-table>
      <template #footer><el-button @click="dialogVisible = false">关闭</el-button><el-button v-if="dialogMode !== 'view'" type="primary" @click="saveUser">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="resetPwdVisible" title="密码重置结果" width="420px"><p>临时密码：<b>{{ tempPassword }}</b></p><p class="warning">请通过安全渠道交付，此密码仅显示一次。</p><template #footer><el-button type="primary" @click="resetPwdVisible = false">关闭</el-button></template></el-dialog>
  </div>
</template>

<style scoped>
.user-page { padding:12px; }.pager { margin-top:12px; justify-content:flex-end; }.history { margin-top:12px; }.warning { color:#d03050; }
</style>
