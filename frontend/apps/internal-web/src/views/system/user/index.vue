<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { internalApi } from '../../../api/internal-api'
import { usePermission } from '../../../composables/usePermission'

interface RoleSummary { roleId:number; roleName:string; roleStatus:string }
interface UserItem { id:number; username:string; displayName:string; employeeCode:string; status:string; mainOrganizationId:number|null; mainOrganizationName:string; mainDepartmentName:string; mainPositionName:string; mustChangePassword:boolean; version:number; roles:RoleSummary[] }
interface AssignmentHistory { fieldName:string; oldValue:string; newValue:string; changeReason:string; changedBy:string; changedAt:string }
interface UserDetail extends UserItem { email:string; phone:string; mainDepartmentId:number|null; mainPositionId:number|null; roleSummaries:RoleSummary[]; assignmentHistory:AssignmentHistory[] }
interface OrgNode { id:number; orgName:string; orgType:string; children?:OrgNode[] }
interface DeptOption { id:number; deptName:string; status:string }
interface PosOption { id:number; positionName:string; status:string }
interface RoleOption { roleId:number; roleCode:string; roleName:string; status:string; assigned:boolean; assignable:boolean }
interface DataPolicyView { domainCode:string; dimensionCode:string; scopeType:string; scopeLabel:string; includeChildren:boolean; operationMode:string; effective:boolean; ineffectiveReason:string|null }
interface RoleScopeView { roleId:number; roleCode:string; roleName:string; policies:DataPolicyView[] }
interface EffectivePermission { permissionCode:string; domainCode:string; resourceCode:string; actionCode:string; sourceRoles:string[] }
interface EffectivePermissionsView { userId:number; username:string; status:string; effectiveRoleCount:number; accessibleMenuCount:number; permissionCount:number; organizationScopeLabel:string; permissions:EffectivePermission[]; roleScopes:RoleScopeView[] }
interface AuthorizationRecord { occurredAt:string; actionCode:string; actionLabel:string; roleId:number|null; roleName:string|null; beforeSummary:string|null; afterSummary:string|null; resultCode:string; changedBy:string }

const canCreate = usePermission('system:user:create')
const canUpdate = usePermission('system:user:update')
const canEnable = usePermission('system:user:enable')
const canDisable = usePermission('system:user:disable')
const canResetPassword = usePermission('system:user:reset-password')
const canAssignRole = usePermission('system:user:assign-role')
const canViewPermissions = usePermission('system:user:view-permissions')
const canViewAuthorization = usePermission('system:user:view-authorization')

const loading = ref(false)
const detailLoading = ref(false)
const errorMessage = ref('')
const list = ref<UserItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const searchForm = reactive({ username:'', displayName:'' })
const drawerVisible = ref(false)
const drawerMode = ref<'create'|'edit'|'view'>('view')
const activeTab = ref('info')
const saving = ref(false)
const roleSaving = ref(false)
const form = reactive({ id:null as number|null, username:'', password:'', displayName:'', employeeCode:'', mainOrganizationId:null as number|null, mainDepartmentId:null as number|null, mainPositionId:null as number|null, version:null as number|null })
const roleOptions = ref<RoleOption[]>([])
const selectedRoleIds = ref<number[]>([])
const detail = ref<UserDetail|null>(null)
const effective = ref<EffectivePermissionsView|null>(null)
const authorizationRecords = ref<AuthorizationRecord[]>([])
const orgOptions = ref<OrgNode[]>([])
const deptOptions = ref<DeptOption[]>([])
const posOptions = ref<PosOption[]>([])
const resetPwdVisible = ref(false)
const tempPassword = ref('')
const resetTarget = ref('')

const isView = computed(() => drawerMode.value === 'view')
const effectiveRoleCount = computed(() => effective.value?.effectiveRoleCount ?? 0)
const accessibleMenuCount = computed(() => effective.value?.accessibleMenuCount ?? 0)
const permissionCount = computed(() => effective.value?.permissionCount ?? 0)

const ROLE_TAB_VISIBLE = computed(() => canAssignRole.value && !isView.value)
const SCOPE_TAB_VISIBLE = computed(() => canViewPermissions.value)
const PERM_TAB_VISIBLE = computed(() => canViewPermissions.value)
const HISTORY_TAB_VISIBLE = computed(() => canViewAuthorization.value && isView.value)

const ACTION_LABELS: Record<string,string> = { view:'查看', create:'新增', update:'编辑', enable:'启用', disable:'停用', 'reset-password':'重置密码', 'assign-role':'配置角色', 'view-permissions':'查看权限', 'view-authorization':'查看授权记录', 'assign-user':'挂载用户', 'assign-permission':'分配权限', 'assign-data-scope':'配置数据范围', import:'导入', export:'导出', sync:'同步', sort:'排序', approve:'审批通过', reject:'审批驳回', withdraw:'撤回', manage:'管理', submit:'提交', publish:'发布', retire:'退役', retry:'重试' }
const DIMENSION_LABELS: Record<string,string> = { ORGANIZATION:'组织', PURCHASING_ORGANIZATION:'采购组织', PLANT:'工厂', CATEGORY:'品类', OWNER:'本人数据', SUPPLIER:'供应商' }
const DOMAIN_LABELS: Record<string,string> = { system:'系统管理', masterdata:'主数据管理' }

function flattenOrganizations(nodes: OrgNode[]): OrgNode[] {
  return nodes.flatMap(node => [node, ...flattenOrganizations(node.children ?? [])])
}

async function loadList() {
  loading.value = true
  errorMessage.value = ''
  try {
    const qs = new URLSearchParams({ page:String(page.value), pageSize:String(pageSize.value) })
    if (searchForm.username) qs.set('username', searchForm.username)
    if (searchForm.displayName) qs.set('displayName', searchForm.displayName)
    const result = await internalApi.get<{items:UserItem[];total:number}>('/system/users?' + qs)
    list.value = result?.items ?? []
    total.value = result?.total ?? 0
  } catch (e:unknown) {
    list.value = []
    errorMessage.value = (e as Error)?.message || '加载失败，请稍后重试'
  } finally { loading.value = false }
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

async function loadRoleOptions() {
  try {
    if (form.id) {
      roleOptions.value = await internalApi.get<RoleOption[]>('/system/users/' + form.id + '/roles') ?? []
      selectedRoleIds.value = roleOptions.value.filter(r => r.assigned).map(r => r.roleId)
    } else {
      roleOptions.value = await internalApi.get<RoleOption[]>('/system/users/assignable-roles') ?? []
      selectedRoleIds.value = []
    }
  } catch (e:unknown) { ElMessage.error((e as Error)?.message || '角色列表加载失败') }
}

async function loadEffective(id:number) {
  try { effective.value = await internalApi.get<EffectivePermissionsView>('/system/users/' + id + '/effective-permissions') ?? null }
  catch (e:unknown) { effective.value = null; ElMessage.error((e as Error)?.message || '生效权限加载失败') }
}

async function loadAuthorization(id:number) {
  try { authorizationRecords.value = await internalApi.get<AuthorizationRecord[]>('/system/users/' + id + '/authorization-history') ?? [] }
  catch (e:unknown) { authorizationRecords.value = []; ElMessage.error((e as Error)?.message || '授权记录加载失败') }
}

function openCreate() {
  drawerMode.value = 'create'
  activeTab.value = 'info'
  detail.value = null
  effective.value = null
  authorizationRecords.value = []
  Object.assign(form, { id:null, username:'', password:'', displayName:'', employeeCode:'', mainOrganizationId:null, mainDepartmentId:null, mainPositionId:null, version:null })
  deptOptions.value = []; posOptions.value = []
  drawerVisible.value = true
  if (canAssignRole.value) loadRoleOptions()
}

async function openUser(row:UserItem, mode:'edit'|'view') {
  detailLoading.value = true
  drawerMode.value = mode
  activeTab.value = 'info'
  errorMessage.value = ''
  try {
    const current = await internalApi.get<UserDetail>('/system/users/' + row.id)
    if (!current) return
    detail.value = current
    Object.assign(form, { id:current.id, username:current.username, password:'', displayName:current.displayName, employeeCode:current.employeeCode, mainOrganizationId:current.mainOrganizationId, mainDepartmentId:current.mainDepartmentId, mainPositionId:current.mainPositionId, version:current.version })
    await loadDepts(current.mainOrganizationId, false)
    await loadPositions(current.mainDepartmentId, false)
    if (canAssignRole.value) await loadRoleOptions()
    if (canViewPermissions.value) await loadEffective(current.id)
    if (canViewAuthorization.value) await loadAuthorization(current.id)
    drawerVisible.value = true
  } catch (e:unknown) {
    errorMessage.value = (e as Error)?.message || '加载用户详情失败'
  } finally { detailLoading.value = false }
}

async function saveUser() {
  if (!form.displayName || !form.mainOrganizationId) { ElMessage.warning('请填写用户名称并选择主组织'); return }
  saving.value = true
  try {
    if (drawerMode.value === 'create') {
      const body: Record<string, unknown> = { username:form.username, password:form.password, displayName:form.displayName, employeeCode:form.employeeCode, mainOrganizationId:form.mainOrganizationId, mainDepartmentId:form.mainDepartmentId, mainPositionId:form.mainPositionId }
      if (canAssignRole.value && selectedRoleIds.value.length > 0) body.roleIds = [...selectedRoleIds.value]
      await internalApi.post('/system/users', body)
    } else {
      await internalApi.request('/system/users/' + form.id, { method:'PUT', body:{ displayName:form.displayName, employeeCode:form.employeeCode, mainOrganizationId:form.mainOrganizationId, mainDepartmentId:form.mainDepartmentId, mainPositionId:form.mainPositionId, version:form.version } })
    }
    drawerVisible.value = false
    await loadList()
    ElMessage.success('保存成功')
  } catch (e:unknown) {
    ElMessage.error((e as Error)?.message || '保存失败')
  } finally { saving.value = false }
}

async function saveRoles() {
  if (!form.id) return
  const removed = roleOptions.value.filter(r => r.assigned && !selectedRoleIds.value.includes(r.roleId))
  if (removed.length > 0) {
    try {
      await ElMessageBox.confirm(`移除角色后，以下角色相关权限将立即失效：\n${removed.map(r => r.roleName).join('、')}\n确认移除？`)
    } catch { return }
  }
  roleSaving.value = true
  try {
    await internalApi.request('/system/users/' + form.id + '/roles', { method:'PUT', body:{ roleIds:[...selectedRoleIds.value] } })
    ElMessage.success('角色授权已保存')
    if (canViewPermissions.value) await loadEffective(form.id)
    await loadList()
  } catch (e:unknown) {
    ElMessage.error((e as Error)?.message || '角色保存失败')
  } finally { roleSaving.value = false }
}

async function toggleUser(row:UserItem, action:'enable'|'disable') {
  try {
    await ElMessageBox.confirm(`确认${action === 'enable' ? '启用' : '停用'}用户 ${row.username}？`)
    await internalApi.post('/system/users/' + row.id + '/' + action)
    await loadList()
    ElMessage.success(action === 'enable' ? '已启用' : '已停用')
  } catch (e:unknown) {
    if ((e as Error)?.message) ElMessage.error((e as Error).message)
  }
}

async function resetPassword(row:UserItem) {
  try {
    await ElMessageBox.confirm(`确认重置用户 ${row.username} 的密码？重置后原会话将全部失效。`)
    const result = await internalApi.post<{temporaryPassword:string}>('/system/users/' + row.id + '/reset-password')
    tempPassword.value = result?.temporaryPassword ?? ''
    resetTarget.value = row.username
    resetPwdVisible.value = true
  } catch (e:unknown) {
    if ((e as Error)?.message) ElMessage.error((e as Error).message)
  }
}

async function copyTempPassword() {
  try {
    await navigator.clipboard.writeText(tempPassword.value)
    ElMessage.success('已复制临时密码')
  } catch { ElMessage.warning('复制失败，请手动选择复制') }
}

function onSearch() { page.value = 1; loadList() }
function onReset() { Object.assign(searchForm, { username:'', displayName:'' }); onSearch() }
function visibleRoleSummaries(roles: RoleSummary[]) { return roles.slice(0, 5) }
function hiddenRoleCount(roles: RoleSummary[]) { return Math.max(0, roles.length - 5) }
function hiddenRoleLabels(roles: RoleSummary[]) { return roles.slice(5).map(roleTagLabel).join('、') }
function roleTagType(role: RoleSummary) { return role.roleStatus === 'ACTIVE' ? 'primary' : 'info' }
function roleTagLabel(role: RoleSummary) { return role.roleStatus === 'ACTIVE' ? role.roleName : role.roleName + '（已停用）' }
function operationModeLabel(mode: string) { return mode === 'READONLY' ? '只读' : '读写' }
function dimensionLabel(dimension: string) { return DIMENSION_LABELS[dimension] ?? dimension }
function domainLabel(domain: string) { return DOMAIN_LABELS[domain] ?? domain }
function actionLabel(code: string) { return ACTION_LABELS[code] ?? code }
function statusText(status: string) { return status === 'ACTIVE' ? '启用' : '停用' }
function resultText(result: string) { return result === 'SUCCESS' ? '成功' : result }

onMounted(() => { loadList(); loadOrgs() })
</script>

<template>
  <section class="user-page">
    <div class="page-header">
      <div>
        <h2>内部用户</h2>
        <p>管理企业内部人员账号、角色授权、数据范围和权限审计；不包含供应商用户。</p>
      </div>
      <el-button v-if="canCreate" type="primary" @click="openCreate">新增用户</el-button>
    </div>

    <el-alert v-if="errorMessage" :title="errorMessage" type="error" :closable="true" show-icon @close="errorMessage = ''" />

    <el-form :inline="true" size="default" class="search-bar" @submit.prevent="onSearch">
      <el-form-item label="用户账号"><el-input v-model="searchForm.username" clearable placeholder="按用户账号查询" @keyup.enter="onSearch" /></el-form-item>
      <el-form-item label="用户名称"><el-input v-model="searchForm.displayName" clearable placeholder="按用户名称查询" @keyup.enter="onSearch" /></el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="list" v-loading="loading" border size="small" empty-text="暂无数据">
      <el-table-column prop="username" label="用户账号" width="140" show-overflow-tooltip />
      <el-table-column prop="displayName" label="用户名称" width="140" show-overflow-tooltip />
      <el-table-column label="工号" width="110">
        <template #default="{row}">{{ row.employeeCode || '—' }}</template>
      </el-table-column>
      <el-table-column prop="mainOrganizationName" label="主组织" min-width="130" show-overflow-tooltip />
      <el-table-column prop="mainDepartmentName" label="主部门" min-width="130" show-overflow-tooltip />
      <el-table-column prop="mainPositionName" label="主岗位" min-width="120" show-overflow-tooltip />
      <el-table-column label="角色" min-width="200">
        <template #default="{row}">
          <template v-if="(row.roles || []).length > 0">
            <el-tooltip v-for="role in visibleRoleSummaries(row.roles || [])" :key="role.roleId" :content="roleTagLabel(role)">
              <el-tag :type="roleTagType(role)" size="small" style="margin:1px 4px 1px 0">{{ roleTagLabel(role) }}</el-tag>
            </el-tooltip>
            <el-tooltip v-if="hiddenRoleCount(row.roles || []) > 0" :content="hiddenRoleLabels(row.roles || [])">
              <el-tag size="small" type="warning">+{{ hiddenRoleCount(row.roles || []) }}</el-tag>
            </el-tooltip>
          </template>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{row}">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{row}">
          <el-button link type="primary" size="small" @click="openUser(row, 'view')">查看</el-button>
          <el-button v-if="canUpdate" link type="primary" size="small" @click="openUser(row, 'edit')">编辑</el-button>
          <el-button v-if="row.status === 'ACTIVE' ? canDisable : canEnable" link size="small" @click="toggleUser(row, row.status === 'ACTIVE' ? 'disable' : 'enable')">{{ row.status === 'ACTIVE' ? '停用' : '启用' }}</el-button>
          <el-button v-if="canResetPassword" link type="warning" size="small" @click="resetPassword(row)">重置密码</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" @current-change="loadList" layout="total,prev,pager,next" class="pager" />

    <el-drawer v-model="drawerVisible" :title="drawerMode === 'create' ? '新增内部用户' : drawerMode === 'edit' ? '编辑内部用户' : '内部用户详情'" size="760px" v-loading="detailLoading">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="用户信息" name="info">
          <el-alert v-if="isView && detail?.status === 'DISABLED'" title="该用户已停用，所有权限当前不生效" type="warning" :closable="false" show-icon class="tab-alert" />
          <el-form label-width="96px" size="default" :disabled="isView" class="user-form">
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="用户账号"><el-input v-model="form.username" :disabled="true" placeholder="创建后不可修改" /></el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="用户名称"><el-input v-model="form.displayName" /></el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="工号"><el-input v-model="form.employeeCode" placeholder="选填，填写后唯一" /></el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item v-if="drawerMode === 'create'" label="初始密码"><el-input v-model="form.password" type="password" show-password placeholder="至少12位" /></el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="主组织"><el-select v-model="form.mainOrganizationId" filterable clearable @change="loadDepts(form.mainOrganizationId)"><el-option v-for="o in orgOptions" :key="o.id" :label="o.orgName" :value="o.id" /></el-select></el-form-item>
            <el-form-item label="主部门"><el-select v-model="form.mainDepartmentId" clearable :disabled="!form.mainOrganizationId" @change="loadPositions(form.mainDepartmentId)"><el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName" :value="d.id" /></el-select></el-form-item>
            <el-form-item label="主岗位"><el-select v-model="form.mainPositionId" clearable :disabled="!form.mainDepartmentId"><el-option v-for="p in posOptions" :key="p.id" :label="p.positionName" :value="p.id" /></el-select></el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane v-if="ROLE_TAB_VISIBLE" label="角色授权" name="roles">
          <p class="tab-tip">用户功能权限由所有启用角色实时计算；停用角色与无权转授的角色不可勾选。</p>
          <div class="role-grid">
            <div v-for="option in roleOptions" :key="option.roleId" class="role-option">
              <el-checkbox v-model="selectedRoleIds" :value="option.roleId" :disabled="option.status !== 'ACTIVE' || !option.assignable" />
              <span class="role-name">{{ option.roleName }}</span>
              <span class="role-code">{{ option.roleCode }}</span>
              <el-tag v-if="option.status !== 'ACTIVE'" type="info" size="small">已停用</el-tag>
              <el-tag v-else-if="!option.assignable" type="warning" size="small">无权转授</el-tag>
            </div>
          </div>
          <div class="tab-footer">
            <el-button type="primary" :loading="roleSaving" @click="saveRoles">保存角色授权</el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane v-if="SCOPE_TAB_VISIBLE" label="数据范围" name="scope">
          <el-empty v-if="!effective" description="暂无数据范围信息" :image-size="60" />
          <template v-else>
            <div v-for="scope in effective.roleScopes" :key="scope.roleId" class="scope-block">
              <h4>{{ scope.roleName }} <span class="muted">{{ scope.roleCode }}</span></h4>
              <el-table :data="scope.policies" size="small" border empty-text="该角色未配置数据策略">
                <el-table-column label="数据域" width="110">
                  <template #default="{row}">{{ domainLabel(row.domainCode) }}</template>
                </el-table-column>
                <el-table-column label="维度" width="110">
                  <template #default="{row}">{{ dimensionLabel(row.dimensionCode) }}</template>
                </el-table-column>
                <el-table-column label="范围说明" min-width="160">
                  <template #default="{row}">{{ row.scopeLabel }}{{ row.includeChildren ? '' : '' }}</template>
                </el-table-column>
                <el-table-column label="操作模式" width="90">
                  <template #default="{row}">{{ operationModeLabel(row.operationMode) }}</template>
                </el-table-column>
                <el-table-column label="是否生效" width="100">
                  <template #default="{row}">
                    <el-tag :type="row.effective ? 'success' : 'danger'" size="small">{{ row.effective ? '生效' : '不生效' }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="不生效原因" min-width="140">
                  <template #default="{row}">{{ row.ineffectiveReason || '—' }}</template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-tab-pane>

        <el-tab-pane v-if="PERM_TAB_VISIBLE" label="生效权限预览" name="permissions">
          <el-empty v-if="!effective" description="暂无权限信息" :image-size="60" />
          <template v-else>
            <div class="perm-stats">
              <el-statistic title="有效角色数量" :value="effectiveRoleCount" />
              <el-statistic title="可访问页面" :value="accessibleMenuCount" />
              <el-statistic title="操作权限数量" :value="permissionCount" />
              <el-statistic title="当前组织数据范围" :value="effective.organizationScopeLabel" />
            </div>
            <el-alert v-if="effective.status === 'DISABLED'" title="该用户已停用，以下权限当前不生效" type="warning" :closable="false" show-icon class="tab-alert" />
            <el-table :data="effective.permissions" size="small" border>
              <el-table-column label="一级模块" width="120">
                <template #default="{row}">{{ domainLabel(row.domainCode) }}</template>
              </el-table-column>
              <el-table-column label="二级页面" width="140">
                <template #default="{row}">{{ row.resourceCode }}</template>
              </el-table-column>
              <el-table-column label="操作权限" min-width="200">
                <template #default="{row}">
                  <el-tag size="small">{{ actionLabel(row.actionCode) }}</el-tag>
                  <span class="muted code-text">{{ row.permissionCode }}</span>
                </template>
              </el-table-column>
              <el-table-column label="来源角色" min-width="160">
                <template #default="{row}">
                  <el-tooltip v-if="row.sourceRoles.length > 2" :content="row.sourceRoles.join('、')">
                    <el-tag size="small" type="primary">{{ row.sourceRoles.length }} 个角色</el-tag>
                  </el-tooltip>
                  <template v-else>
                    <el-tag v-for="role in row.sourceRoles" :key="role" size="small" type="primary" style="margin-right:4px">{{ role }}</el-tag>
                  </template>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </el-tab-pane>

        <el-tab-pane v-if="HISTORY_TAB_VISIBLE" label="授权记录" name="history">
          <el-empty v-if="authorizationRecords.length === 0" description="暂无授权变更记录" :image-size="60" />
          <el-table v-else :data="authorizationRecords" size="small" border>
            <el-table-column label="操作时间" width="170">
              <template #default="{row}">{{ row.occurredAt }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{row}">{{ row.actionLabel }}</template>
            </el-table-column>
            <el-table-column label="对象" min-width="130">
              <template #default="{row}">{{ row.roleName || '内部用户' }}</template>
            </el-table-column>
            <el-table-column label="变更前" min-width="130">
              <template #default="{row}">{{ row.beforeSummary || '—' }}</template>
            </el-table-column>
            <el-table-column label="变更后" min-width="130">
              <template #default="{row}">{{ row.afterSummary || '—' }}</template>
            </el-table-column>
            <el-table-column label="结果" width="80">
              <template #default="{row}">{{ resultText(row.resultCode) }}</template>
            </el-table-column>
            <el-table-column label="操作人" width="120">
              <template #default="{row}">{{ row.changedBy }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="drawerVisible = false">关闭</el-button>
        <el-button v-if="!isView" type="primary" :loading="saving" @click="saveUser">保存用户资料</el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="resetPwdVisible" title="密码重置结果" width="440px" :close-on-click-modal="false">
      <p>用户 <b>{{ resetTarget }}</b> 的临时密码如下，仅显示一次，原会话已全部失效：</p>
      <div class="temp-password"><b>{{ tempPassword }}</b><el-button link type="primary" @click="copyTempPassword">复制</el-button></div>
      <p class="warning">请通过安全渠道交付给用户本人；用户下次登录将被强制修改密码。</p>
      <template #footer><el-button type="primary" @click="resetPwdVisible = false">关闭</el-button></template>
    </el-dialog>
  </section>
</template>

<style scoped>
.user-page { padding:16px; }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:14px; }
.page-header h2 { margin:0 0 4px; font-size:20px; }
.page-header p { margin:0; color:var(--srm-muted, #7a8a99); font-size:13px; }
.search-bar { margin-bottom:12px; }
.pager { margin-top:12px; justify-content:flex-end; }
.muted { color:var(--srm-muted, #7a8a99); }
.code-text { margin-left:6px; font-size:12px; }
.tab-alert { margin-bottom:12px; }
.tab-tip { margin:0 0 10px; color:var(--srm-muted, #7a8a99); font-size:13px; }
.user-form { max-width:560px; }
.role-grid { display:grid; grid-template-columns:repeat(2, minmax(260px, 1fr)); gap:8px; max-height:420px; overflow:auto; }
.role-option { display:flex; align-items:center; gap:8px; padding:6px 8px; border:1px solid #e5e5e5; border-radius:6px; }
.role-name { font-weight:600; }
.role-code { color:var(--srm-muted, #7a8a99); font-size:12px; }
.tab-footer { margin-top:12px; }
.scope-block { margin-bottom:14px; }
.scope-block h4 { margin:0 0 6px; }
.perm-stats { display:flex; gap:28px; margin-bottom:14px; padding:12px 16px; border:1px solid #e5e5e5; border-radius:8px; }
.temp-password { display:flex; align-items:center; gap:8px; margin:10px 0; padding:10px 12px; background:#f5f7fa; border-radius:6px; font-size:16px; }
.warning { color:#d03050; }
</style>
