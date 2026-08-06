<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { internalApi } from '../../../api/internal-api'
import { usePermission } from '../../../composables/usePermission'

interface RoleItem { id:number; roleCode:string; roleName:string; description:string; status:string; builtIn:boolean; roleCategory:string; userCount:number; version:number }
interface DataPolicy { domainCode:string;dimensionCode:string;scopeType:string;operationMode:string;includeChildren:boolean }
interface RoleDetail extends RoleItem { userIds:number[]; menuIds:number[]; permissionIds:number[]; dataPolicies:DataPolicy[]; history:HistoryItem[] }
interface UserOption { id:number; username:string; displayName:string; status:string }
interface MenuNode { id:number; label:string; menuCode:string; children:MenuNode[] }
interface PermItem { id:number; permissionCode:string; description:string; enabled:boolean; domainCode:string; resourceCode:string; actionCode:string }
interface HistoryItem { changedAt:string; actionLabel:string; changedBy:string; beforeSummary:string|null; afterSummary:string|null }

const list=ref<RoleItem[]>([]); const loading=ref(false)
const dialogVisible=ref(false); const dialogMode=ref<'create'|'edit'>('create')
const form=reactive({ id:null as number|null, roleCode:'', roleName:'', description:'', roleCategory:'BUSINESS', version:null as number|null })
const selectedRole=ref<RoleDetail|null>(null); const activeTab=ref('info')
const roleUsers=ref<UserOption[]>([]); const allUsers=ref<UserOption[]>([])
const userSearchKeyword=ref(''); const userSearchLoading=ref(false)
const menuTree=ref<MenuNode[]>([]); const checkedMenus=ref<number[]>([])
const permissionList=ref<PermItem[]>([]); const checkedPermissions=ref<number[]>([])
const permissionFilter=ref('')
const dataPolicies=ref<DataPolicy[]>([])
const historyRecords=ref<HistoryItem[]>([])

const canCreate = usePermission('system:role:create')
const canUpdate = usePermission('system:role:update')
const canEnable = usePermission('system:role:enable')
const canDisable = usePermission('system:role:disable')
const canAssignUser = usePermission('system:role:assign-user')
const canAssignPermission = usePermission('system:role:assign-permission')
const canAssignDataScope = usePermission('system:role:assign-data-scope')

const DIMENSION_LABELS: Record<string,string> = { ORGANIZATION:'组织', PURCHASING_ORGANIZATION:'采购组织', PLANT:'工厂', CATEGORY:'品类', OWNER:'本人数据' }

const filteredPermissions = computed(() => {
  if (!permissionFilter.value) return permissionList.value
  const kw = permissionFilter.value.toLowerCase()
  return permissionList.value.filter(p => p.permissionCode.toLowerCase().includes(kw) || p.description?.toLowerCase().includes(kw))
})

async function loadList() {
  loading.value=true
  try {
    const r=await internalApi.get<{items:RoleItem[],total:number}>('/system/roles?page=1&pageSize=100')
    list.value=r?.items??[]
  } catch(e){ void e }
  finally{loading.value=false}
}

async function loadRoleDetail(id:number) {
  try {
    const r=await internalApi.get<RoleDetail>('/system/roles/'+id)
    selectedRole.value=r??null
    if(selectedRole.value) {
      checkedMenus.value=[...selectedRole.value.menuIds]
      checkedPermissions.value=[...selectedRole.value.permissionIds]
      dataPolicies.value=selectedRole.value.dataPolicies.map(p=>({...p}))
      historyRecords.value=selectedRole.value.history??[]
      activeTab.value='info'
      loadRoleUsers()
      loadRoleMenus()
      loadRolePermissions()
    }
  } catch(e){ void e }
}

async function searchAllUsers() {
  userSearchLoading.value=true
  try {
    const params = userSearchKeyword.value ? '?displayName='+encodeURIComponent(userSearchKeyword.value)+'&pageSize=50' : '?pageSize=50'
    const r=await internalApi.get<{items:UserOption[]}>(`/system/users${params}`)
    allUsers.value=r?.items??[]
  } catch(e){ void e }
  finally{userSearchLoading.value=false}
}

async function loadRoleUsers() {
  if(!selectedRole.value) return
  try {
    const r=await internalApi.get<UserOption[]>('/system/roles/'+selectedRole.value.id+'/users')
    roleUsers.value=r??[]
  } catch(e){ void e }
}
async function loadRoleMenus() {
  try{const r=await internalApi.get<MenuNode[]>('/system/menus');menuTree.value=r??[]}catch(e){ void e }
}
async function loadRolePermissions() {
  try{const r=await internalApi.get<{items:PermItem[]}>('/system/permissions?pageSize=999');permissionList.value=(r?.items as PermItem[])??[]}catch(e){ void e }
}

function openCreate() {
  dialogMode.value='create'
  Object.assign(form,{id:null,roleCode:'',roleName:'',description:'',roleCategory:'BUSINESS',version:null})
  dialogVisible.value=true
}
function openEdit(row:RoleItem) {
  dialogMode.value='edit'
  Object.assign(form,{id:row.id,roleCode:row.roleCode,roleName:row.roleName,description:row.description,roleCategory:row.roleCategory,version:row.version})
  dialogVisible.value=true
}

async function saveRole() {
  try {
    const payload = { roleCode:form.roleCode, roleName:form.roleName, description:form.description, roleCategory:form.roleCategory, version:form.version }
    if(dialogMode.value==='create') {
      await internalApi.post('/system/roles', payload)
    } else {
      await internalApi.request('/system/roles/'+form.id, { method:'PUT', body:payload })
    }
    dialogVisible.value=false
    await loadList()
    if(form.id) await loadRoleDetail(form.id)
    ElMessage.success('保存成功')
  } catch(e:unknown) { ElMessage.error((e as Error)?.message||'保存失败') }
}

async function toggleRole(row:RoleItem, action:string) {
  try {
    if (action==='disable') {
      if (row.userCount>0) await ElMessageBox.confirm(`该角色当前有 ${row.userCount} 个挂载用户，停用后将立即失效。确认继续？`, '提示', { type:'warning' })
      else await ElMessageBox.confirm('确认停用该角色？', '提示', { type:'warning' })
    }
    await internalApi.post('/system/roles/'+row.id+'/'+action)
    loadList()
  } catch(e){ void e }
}

async function assignUser(userId:number) {
  if(!selectedRole.value) return
  try {
    await internalApi.request('/system/roles/'+selectedRole.value.id+'/users', { method:'PUT', body:{userIds:[userId]} })
    loadRoleUsers()
    await loadList()
    ElMessage.success('已添加')
  } catch(e:unknown) { ElMessage.error((e as Error)?.message||'添加失败') }
}

async function removeUser(userId:number) {
  if(!selectedRole.value) return
  try {
    await ElMessageBox.confirm('确认从该角色移除该用户？', '提示', { type:'warning' })
    await internalApi.request('/system/roles/'+selectedRole.value.id+'/users/'+userId, { method:'DELETE' })
    loadRoleUsers()
    await loadList()
    ElMessage.success('已移除')
  } catch(e:unknown) { if(e!=='cancel') ElMessage.error((e as Error)?.message||'移除失败') }
}

function onMenuCheck(_node:unknown, keys:{checkedKeys:number[]}) { checkedMenus.value=keys.checkedKeys }

async function saveMenusPermissions() {
  if(!selectedRole.value) return
  try {
    await internalApi.request('/system/roles/'+selectedRole.value.id+'/menus-permissions', { method:'PUT', body:{menuIds:checkedMenus.value, permissionIds:checkedPermissions.value} })
    navigationCacheInvalidator()
    ElMessage.success('已保存')
  } catch(e:unknown) { ElMessage.error((e as Error)?.message||'保存失败') }
}

async function savePolicies() {
  if(!selectedRole.value) return
  try {
    await internalApi.request('/system/roles/'+selectedRole.value.id+'/data-policies', { method:'PUT', body:dataPolicies.value })
    navigationCacheInvalidator()
    ElMessage.success('已保存')
  } catch(e:unknown) { ElMessage.error((e as Error)?.message||'保存失败') }
}

function navigationCacheInvalidator() { /* The backend handles this */ }

function addPolicy() { dataPolicies.value.push({domainCode:'system',dimensionCode:'ORGANIZATION',scopeType:'ORG',operationMode:'READONLY',includeChildren:false}) }
function removePolicy(index:number) { dataPolicies.value.splice(index,1) }

onMounted(()=>{ loadList(); searchAllUsers() })
</script>

<template>
  <div style="display:flex;height:calc(100vh - 120px);gap:12px;padding:12px">
    <div style="width:320px;overflow:auto;border:1px solid #e5e5e5;border-radius:4px;padding:8px">
      <div style="display:flex;justify-content:space-between;margin-bottom:8px"><b>角色列表</b><el-button v-if="canCreate" type="primary" size="small" @click="openCreate">新增</el-button></div>
      <el-table :data="list" v-loading="loading" size="small" @row-click="loadRoleDetail" highlight-current-row empty-text="暂无角色">
        <el-table-column prop="roleName" label="名称" min-width="100"/>
        <el-table-column prop="roleCategory" label="分类" width="70">
          <template #default="{row}"><el-tag size="small" :type="row.roleCategory==='SYSTEM'?'danger':row.roleCategory==='AUDIT'?'warning':''">{{ row.roleCategory }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="userCount" label="用户数" width="58"/>
        <el-table-column label="操作" width="90">
          <template #default="{row}">
            <el-button v-if="canUpdate" link type="primary" size="small" @click.stop="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status==='ACTIVE'?canDisable:canEnable" link size="small" @click.stop="toggleRole(row,row.status==='ACTIVE'?'disable':'enable')">{{ row.status==='ACTIVE'?'停':'启' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div style="flex:1;overflow:auto">
      <template v-if="selectedRole">
        <h3>{{ selectedRole.roleName }} <el-tag size="small" :type="selectedRole.status==='ACTIVE'?'success':'info'">{{ selectedRole.status==='ACTIVE'?'启用':'停用' }}</el-tag></h3>
        <el-tabs v-model="activeTab">
          <el-tab-pane label="基本信息" name="info">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="编码">{{ selectedRole.roleCode }}</el-descriptions-item>
              <el-descriptions-item label="名称">{{ selectedRole.roleName }}</el-descriptions-item>
              <el-descriptions-item label="分类">{{ selectedRole.roleCategory }}</el-descriptions-item>
              <el-descriptions-item label="内建">{{ selectedRole.builtIn?'是':'否' }}</el-descriptions-item>
              <el-descriptions-item label="描述" :span="2">{{ selectedRole.description }}</el-descriptions-item>
              <el-descriptions-item label="挂载用户数">{{ selectedRole.userCount }}</el-descriptions-item>
              <el-descriptions-item label="版本">{{ selectedRole.version }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <el-tab-pane label="已挂用户" name="users" lazy>
            <div style="display:flex;gap:12px">
              <div style="flex:1">
                <b>已挂载用户 ({{ roleUsers.length }})</b>
                <el-table :data="roleUsers" size="small" border empty-text="无" max-height="400">
                  <el-table-column prop="username" label="用户名"/>
                  <el-table-column prop="displayName" label="姓名"/>
                  <el-table-column prop="status" label="状态" width="70">
                    <template #default="{row}"><el-tag size="small" :type="row.status==='ACTIVE'?'success':'info'">{{ row.status==='ACTIVE'?'启用':'停用' }}</el-tag></template>
                  </el-table-column>
                  <el-table-column v-if="canAssignUser" label="操作" width="80"><template #default="{row}"><el-button link type="danger" size="small" @click="removeUser(row.id)">移除</el-button></template></el-table-column>
                </el-table>
              </div>
              <div v-if="canAssignUser" style="flex:1">
                <div style="display:flex;gap:4px;margin-bottom:8px"><el-input v-model="userSearchKeyword" placeholder="搜索用户" size="small" @input="searchAllUsers"/></div>
                <el-table :data="allUsers.filter(u=>!roleUsers.some(ru=>ru.id===u.id))" size="small" border max-height="360" empty-text="无" v-loading="userSearchLoading">
                  <el-table-column prop="username" label="用户名"/>
                  <el-table-column prop="displayName" label="姓名"/>
                  <el-table-column label="操作" width="80"><template #default="{row}"><el-button link type="primary" size="small" @click="assignUser(row.id)">添加</el-button></template></el-table-column>
                </el-table>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="功能权限" name="permissions" lazy>
            <el-form :inline="true" size="small"><el-form-item label="过滤"><el-input v-model="permissionFilter" clearable placeholder="权限码"/></el-form-item></el-form>
            <p style="margin:8px 0;color:#909399;font-size:13px">菜单权限树</p>
            <el-tree :key="'menu-'+selectedRole.id" :data="menuTree" show-checkbox node-key="id" :default-checked-keys="checkedMenus" :props="{children:'children',label:'label'}" :disabled="!canAssignPermission" @check="onMenuCheck" style="max-height:220px;overflow:auto"/>
            <p style="margin:8px 0;color:#909399;font-size:13px">接口权限 ({{ checkedPermissions.length }}/{{ filteredPermissions.length }})</p>
            <el-table :data="filteredPermissions" size="small" border max-height="200" empty-text="无">
              <el-table-column width="40"><template #default="{row}"><el-checkbox v-model="checkedPermissions" :value="row.id" :disabled="!canAssignPermission"/></template></el-table-column>
              <el-table-column prop="permissionCode" label="权限码" min-width="180"/>
              <el-table-column prop="description" label="说明" min-width="120"/>
              <el-table-column prop="domainCode" label="领域" width="90"/>
            </el-table>
            <el-button v-if="canAssignPermission" type="primary" size="small" @click="saveMenusPermissions" style="margin-top:8px">保存功能权限</el-button>
          </el-tab-pane>

          <el-tab-pane label="数据权限" name="scope" lazy>
            <el-button v-if="canAssignDataScope" type="primary" size="small" @click="addPolicy" style="margin-bottom:8px">新增策略</el-button>
            <el-table :data="dataPolicies" size="small" border empty-text="未配置数据策略">
              <el-table-column label="领域" width="100"><template #default="{row}"><el-select v-model="row.domainCode" size="small" :disabled="!canAssignDataScope"><el-option label="system" value="system"/><el-option label="masterdata" value="masterdata"/></el-select></template></el-table-column>
              <el-table-column label="维度" width="150"><template #default="{row}"><el-select v-model="row.dimensionCode" size="small" :disabled="!canAssignDataScope"><el-option v-for="(label,code) in DIMENSION_LABELS" :key="code" :label="label" :value="code"/></el-select></template></el-table-column>
              <el-table-column label="范围" width="90"><template #default="{row}"><el-select v-model="row.scopeType" size="small" :disabled="!canAssignDataScope"><el-option label="ALL" value="ALL"/><el-option label="ORG" value="ORG"/><el-option label="SELF" value="SELF"/></el-select></template></el-table-column>
              <el-table-column label="模式" width="90"><template #default="{row}"><el-select v-model="row.operationMode" size="small" :disabled="!canAssignDataScope"><el-option label="读写" value="READ_WRITE"/><el-option label="只读" value="READONLY"/></el-select></template></el-table-column>
              <el-table-column label="含下级" width="70"><template #default="{row}"><el-switch v-model="row.includeChildren" size="small" :disabled="!canAssignDataScope"/></template></el-table-column>
              <el-table-column v-if="canAssignDataScope" label="操作" width="70"><template #default="{ $index }"><el-button link type="danger" size="small" @click="removePolicy($index)">删除</el-button></template></el-table-column>
            </el-table>
            <el-button v-if="canAssignDataScope" type="primary" size="small" @click="savePolicies" style="margin-top:8px">保存策略</el-button>
          </el-tab-pane>

          <el-tab-pane label="授权记录" name="history" lazy>
            <el-table :data="historyRecords" size="small" border empty-text="暂无记录" max-height="400">
              <el-table-column prop="changedAt" label="时间" width="170"/>
              <el-table-column prop="actionLabel" label="操作" width="160"/>
              <el-table-column prop="changedBy" label="操作人" width="120"/>
              <el-table-column prop="beforeSummary" label="变更前" min-width="150"><template #default="{row}"><span style="font-size:12px;color:#909399">{{ row.beforeSummary }}</span></template></el-table-column>
              <el-table-column prop="afterSummary" label="变更后" min-width="150"><template #default="{row}"><span style="font-size:12px;color:#909399">{{ row.afterSummary }}</span></template></el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </template>
      <el-empty v-else description="请选择左侧角色查看详情"/>
    </div>
  </div>

  <el-dialog v-model="dialogVisible" :title="dialogMode==='create'?'新增角色':'编辑角色'" width="500px">
    <el-form label-width="90px">
      <el-form-item label="编码"><el-input v-model="form.roleCode" :disabled="dialogMode==='edit'"/></el-form-item>
      <el-form-item label="名称"><el-input v-model="form.roleName"/></el-form-item>
      <el-form-item label="分类">
        <el-select v-model="form.roleCategory" style="width:100%">
          <el-option label="系统级" value="SYSTEM"/><el-option label="业务级" value="BUSINESS"/><el-option label="审计级" value="AUDIT"/>
        </el-select>
      </el-form-item>
      <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2"/></el-form-item>
    </el-form>
    <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="saveRole">保存</el-button></template>
  </el-dialog>
</template>
