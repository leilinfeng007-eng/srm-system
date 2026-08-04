<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { internalApi } from '../../../api/internal-api'
import { usePermission } from '../../../composables/usePermission'

interface RoleItem { id:number; roleCode:string; roleName:string; description:string; status:string; builtIn:boolean; userCount:number; version:number }
interface DataPolicy {domainCode:string;dimensionCode:string;scopeType:string;operationMode:string;includeChildren:boolean}
interface RoleDetail extends RoleItem { version:number; userIds:number[]; menuIds:number[]; permissionIds:number[]; dataPolicies:DataPolicy[] }
interface UserOption { id:number; username:string; displayName:string }
interface MenuNode { id:number; label:string; menuCode:string; children:MenuNode[] }

const list=ref<RoleItem[]>([]); const loading=ref(false); const dialogVisible=ref(false)
const dialogMode=ref<'create'|'edit'>('create')
const form=reactive({ id:null as number|null, roleCode:'', roleName:'', description:'', version:null as number|null })
const selectedRole=ref<RoleDetail|null>(null); const activeTab=ref('info')
const roleUsers=ref<UserOption[]>([]); const allUsers=ref<UserOption[]>([])
const menuTree=ref<MenuNode[]>([]); const checkedMenus=ref<number[]>([])
interface PermItem { id:number; permissionCode:string; description:string; enabled:boolean }
const permissionList=ref<PermItem[]>([]); const checkedPermissions=ref<number[]>([])
const dataPolicies=ref<DataPolicy[]>([])
const canCreate = usePermission('system:role:create')
const canUpdate = usePermission('system:role:update')
const canEnable = usePermission('system:role:enable')
const canDisable = usePermission('system:role:disable')
const canAssignUser = usePermission('system:role:assign-user')
const canAssignPermission = usePermission('system:role:assign-permission')
const canAssignDataScope = usePermission('system:role:assign-data-scope')

async function loadList() { loading.value=true; try{const r=await internalApi.get<{items:RoleItem[],total:number}>('/system/roles?page=1&pageSize=100');list.value=r?.items??[]}catch(e){ void e; }finally{loading.value=false} }
async function loadRoleDetail(id:number) { try{const r=await internalApi.get<RoleDetail>('/system/roles/'+id);selectedRole.value=r??null;if(selectedRole.value){checkedMenus.value=[...selectedRole.value.menuIds];checkedPermissions.value=[...selectedRole.value.permissionIds];dataPolicies.value=selectedRole.value.dataPolicies.map(p=>({...p}));activeTab.value='info';loadRoleUsers();loadRoleMenus();loadRolePermissions()}}catch(e){ void e; } }
async function loadAllUsers() { try{const r=await internalApi.get<{items:UserOption[]}>('/system/users?page=1&pageSize=999');allUsers.value=r?.items??[]}catch(e){ void e; } }
async function loadRoleUsers() { if(!selectedRole.value) return; try{const r=await internalApi.get<UserOption[]>('/system/roles/'+selectedRole.value.id+'/users');roleUsers.value=r??[]}catch(e){ void e; } }
async function loadRoleMenus(){try{const r=await internalApi.get<MenuNode[]>('/system/menus');menuTree.value=r??[]}catch(e){ void e; }}
async function loadRolePermissions(){try{const r=await internalApi.get<{items:PermItem[]}>('/system/permissions?pageSize=999');permissionList.value=(r?.items as PermItem[])??[]}catch(e){ void e; }}

function openCreate(){dialogMode.value='create';Object.assign(form,{id:null,roleCode:'',roleName:'',description:'',version:null});dialogVisible.value=true}
function openEdit(row:RoleItem){dialogMode.value='edit';Object.assign(form,{id:row.id,roleCode:row.roleCode,roleName:row.roleName,description:row.description,version:row.version});dialogVisible.value=true}
async function saveRole(){try{if(dialogMode.value==='create')await internalApi.post('/system/roles',{roleCode:form.roleCode,roleName:form.roleName,description:form.description});else await internalApi.request('/system/roles/'+form.id,{method:'PUT',body:{roleName:form.roleName,description:form.description,version:form.version}});dialogVisible.value=false;await loadList();if(form.id)await loadRoleDetail(form.id);ElMessage.success('保存成功')}catch(e:unknown){ElMessage.error((e as Error)?.message||'保存失败')}}
async function toggleRole(row:RoleItem,action:string){try{await ElMessageBox.confirm('确认?');await internalApi.post('/system/roles/'+row.id+'/'+action);loadList()}catch(e){ void e; }}
async function assignUser(userId:number){if(!selectedRole.value)return;try{await internalApi.request('/system/roles/'+selectedRole.value.id+'/users',{method:'PUT',body:{userIds:[userId]}});loadRoleUsers();ElMessage.success('已添加')}catch(e:unknown){ElMessage.error((e as Error)?.message||'添加失败')}}
async function removeUser(userId:number){if(!selectedRole.value)return;try{await internalApi.request('/system/roles/'+selectedRole.value.id+'/users/'+userId,{method:'DELETE'});loadRoleUsers();ElMessage.success('已移除')}catch(e:unknown){ElMessage.error((e as Error)?.message||'移除失败')}}
function onMenuCheck(_node:unknown,keys:{checkedKeys:number[]}){checkedMenus.value=keys.checkedKeys}
async function saveMenus(){if(!selectedRole.value)return;try{await internalApi.request('/system/roles/'+selectedRole.value.id+'/menus-permissions',{method:'PUT',body:{menuIds:checkedMenus.value}});ElMessage.success('已保存')}catch(e:unknown){ElMessage.error((e as Error)?.message||'保存失败')}}
async function savePermissions(){if(!selectedRole.value)return;try{await internalApi.request('/system/roles/'+selectedRole.value.id+'/menus-permissions',{method:'PUT',body:{permissionIds:checkedPermissions.value}});ElMessage.success('已保存')}catch(e:unknown){ElMessage.error((e as Error)?.message||'保存失败')}}
async function savePolicies(){if(!selectedRole.value)return;try{await internalApi.request('/system/roles/'+selectedRole.value.id+'/data-policies',{method:'PUT',body:dataPolicies.value});ElMessage.success('已保存')}catch(e:unknown){ElMessage.error((e as Error)?.message||'保存失败')}}
function addPolicy(){dataPolicies.value.push({domainCode:'',dimensionCode:'ORGANIZATION',scopeType:'ORG',operationMode:'READONLY',includeChildren:false})}
function removePolicy(index:number){dataPolicies.value.splice(index,1)}

onMounted(()=>{loadList();loadAllUsers()})
</script>

<template>
  <div style="display:flex;height:calc(100vh - 120px);gap:12px;padding:12px">
    <div style="width:300px;overflow:auto;border:1px solid #e5e5e5;border-radius:4px;padding:8px">
      <div style="display:flex;justify-content:space-between;margin-bottom:8px"><b>角色列表</b><el-button v-if="canCreate" type="primary" size="small" @click="openCreate">新增</el-button></div>
      <el-table :data="list" v-loading="loading" size="small" @row-click="loadRoleDetail" highlight-current-row empty-text="暂无角色">
        <el-table-column prop="roleName" label="名称"/><el-table-column prop="userCount" label="用户数" width="60"/>
        <el-table-column label="操作" width="100"><template #default="{row}">
          <el-button v-if="canUpdate" link type="primary" size="small" @click.stop="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 'ACTIVE' ? canDisable : canEnable" link size="small" @click.stop="toggleRole(row,row.status==='ACTIVE'?'disable':'enable')">{{row.status==='ACTIVE'?'停':'启'}}</el-button>
        </template></el-table-column>
      </el-table>
    </div>
    <div style="flex:1;overflow:auto">
      <template v-if="selectedRole">
        <h3>{{selectedRole.roleName}} ({{selectedRole.roleCode}})</h3>
        <el-tabs v-model="activeTab">
          <el-tab-pane label="基本信息" name="info"><el-descriptions :column="2" border size="small"><el-descriptions-item label="编码">{{selectedRole.roleCode}}</el-descriptions-item><el-descriptions-item label="名称">{{selectedRole.roleName}}</el-descriptions-item><el-descriptions-item label="描述">{{selectedRole.description}}</el-descriptions-item><el-descriptions-item label="内建">{{selectedRole.builtIn?'是':'否'}}</el-descriptions-item></el-descriptions></el-tab-pane>
          <el-tab-pane label="挂载用户" name="users">
            <div style="display:flex;gap:12px"><div style="flex:1"><b>已挂载用户</b><el-table :data="roleUsers" size="small" border empty-text="无"><el-table-column prop="username" label="用户名"/><el-table-column label="操作" width="80"><template #default="{row}"><el-button v-if="canAssignUser" link type="danger" size="small" @click="removeUser(row.id)">移除</el-button></template></el-table-column></el-table></div><div style="flex:1"><b>可选用户</b><el-table :data="allUsers.filter(u=>!roleUsers.some(ru=>ru.id===u.id))" size="small" border max-height="300" empty-text="无"><el-table-column prop="username" label="用户名"/><el-table-column label="操作" width="80"><template #default="{row}"><el-button v-if="canAssignUser" link type="primary" size="small" @click="assignUser(row.id)">添加</el-button></template></el-table-column></el-table></div></div>
          </el-tab-pane>
          <el-tab-pane label="菜单权限" name="menus">
            <el-tree :key="selectedRole.id" :data="menuTree" show-checkbox node-key="id" :default-checked-keys="checkedMenus" :props="{children:'children',label:'label'}" :disabled="!canAssignPermission" @check="onMenuCheck"/><el-button v-if="canAssignPermission" type="primary" size="small" @click="saveMenus" style="margin-top:8px">保存菜单</el-button>
          </el-tab-pane>
          <el-tab-pane label="接口权限" name="permissions">
            <el-table :data="permissionList" size="small" border empty-text="无"><el-table-column width="45"><template #default="{row}"><el-checkbox v-model="checkedPermissions" :value="row.id" :disabled="!canAssignPermission"/></template></el-table-column><el-table-column prop="permissionCode" label="权限码"/><el-table-column prop="description" label="说明"/></el-table><el-button v-if="canAssignPermission" type="primary" size="small" @click="savePermissions" style="margin-top:8px">保存权限</el-button>
          </el-tab-pane>
          <el-tab-pane label="数据范围" name="scope">
            <el-button v-if="canAssignDataScope" type="primary" size="small" @click="addPolicy" style="margin-bottom:8px">新增策略</el-button>
            <el-table :data="dataPolicies" size="small" border empty-text="未配置数据策略">
              <el-table-column label="领域"><template #default="{row}"><el-select v-model="row.domainCode" size="small" :disabled="!canAssignDataScope"><el-option label="system" value="system"/><el-option label="masterdata" value="masterdata"/></el-select></template></el-table-column>
              <el-table-column label="维度"><template #default="{row}"><el-select v-model="row.dimensionCode" size="small" :disabled="!canAssignDataScope"><el-option label="ORGANIZATION" value="ORGANIZATION"/><el-option label="PURCHASING_ORG" value="PURCHASING_ORGANIZATION"/><el-option label="PLANT" value="PLANT"/><el-option label="CATEGORY" value="CATEGORY"/><el-option label="OWNER" value="OWNER"/></el-select></template></el-table-column>
              <el-table-column label="范围" width="90"><template #default="{row}"><el-select v-model="row.scopeType" size="small" :disabled="!canAssignDataScope"><el-option label="ALL" value="ALL"/><el-option label="ORG" value="ORG"/><el-option label="SELF" value="SELF"/></el-select></template></el-table-column>
              <el-table-column label="模式" width="120"><template #default="{row}"><el-select v-model="row.operationMode" size="small" :disabled="!canAssignDataScope"><el-option label="读写" value="READ_WRITE"/><el-option label="只读" value="READONLY"/></el-select></template></el-table-column>
              <el-table-column label="含下级" width="70"><template #default="{row}"><el-switch v-model="row.includeChildren" size="small" :disabled="!canAssignDataScope"/></template></el-table-column>
              <el-table-column v-if="canAssignDataScope" label="操作" width="70"><template #default="{ $index }"><el-button link type="danger" @click="removePolicy($index)">删除</el-button></template></el-table-column>
            </el-table><el-button v-if="canAssignDataScope" type="primary" size="small" @click="savePolicies" style="margin-top:8px">保存策略</el-button>
          </el-tab-pane>
        </el-tabs>
      </template>
      <el-empty v-else description="请选择左侧角色"/>
    </div>
  </div>

  <el-dialog v-model="dialogVisible" :title="dialogMode==='create'?'新增角色':'编辑角色'" width="450px">
    <el-form label-width="80px"><el-form-item label="编码"><el-input v-model="form.roleCode" :disabled="dialogMode==='edit'"/></el-form-item><el-form-item label="名称"><el-input v-model="form.roleName"/></el-form-item><el-form-item label="描述"><el-input v-model="form.description" type="textarea"/></el-form-item></el-form>
    <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="saveRole">保存</el-button></template>
  </el-dialog>
</template>
