<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { internalApi } from '../../../api/internal-api'

interface PermItem { id:number; permissionCode:string; domainCode:string; resourceCode:string; actionCode:string; description:string; enabled:boolean }
interface EffectivePerm { permissionCode:string; sourceRoles:string[]; authorizationPaths:string[] }

const list=ref<PermItem[]>([]); const loading=ref(false)
const searchForm=reactive({ domain:'', keyword:'' })
const effectiveUserId=ref(''); const effectivePerms=ref<EffectivePerm[]>([]); const effectiveLoading=ref(false)

async function loadList() {
  loading.value=true
  try{const r=await internalApi.get<{items:PermItem[]}>('/system/permissions?pageSize=999');list.value=r?.items??[]}catch(e){ void e; }
  finally{loading.value=false}
}
async function loadEffective() {
  if(!effectiveUserId.value) return
  effectiveLoading.value=true
  try{const r=await internalApi.get<{permissions:EffectivePerm[]}>('/system/permissions/effective-users/'+effectiveUserId.value);effectivePerms.value=r?.permissions??[]}catch(e){ void e; }
  finally{effectiveLoading.value=false}
}
const filtered=()=>list.value.filter(p=>{
  if(searchForm.domain&&p.domainCode!==searchForm.domain) return false
  if(searchForm.keyword&&!p.permissionCode.includes(searchForm.keyword)&&!p.description?.includes(searchForm.keyword)) return false
  return true
})

onMounted(()=>{loadList()})
</script>

<template>
  <div style="padding:12px">
    <el-tabs>
      <el-tab-pane label="权限目录">
        <el-form :inline="true" size="small"><el-form-item label="领域"><el-input v-model="searchForm.domain" clearable/></el-form-item><el-form-item label="关键词"><el-input v-model="searchForm.keyword" clearable/></el-form-item><el-form-item><el-button @click="searchForm.domain='';searchForm.keyword=''">重置</el-button></el-form-item></el-form>
        <el-table :data="filtered()" v-loading="loading" border size="small" empty-text="无匹配权限" max-height="calc(100vh - 220px)">
          <el-table-column prop="permissionCode" label="权限码" min-width="200"/>
          <el-table-column prop="domainCode" label="领域" width="100"/>
          <el-table-column prop="resourceCode" label="资源" width="100"/>
          <el-table-column prop="actionCode" label="动作" width="80"/>
          <el-table-column prop="description" label="说明" min-width="150"/>
          <el-table-column prop="enabled" label="状态" width="70"><template #default="{row}"><el-tag :type="row.enabled?'success':'info'" size="small">{{row.enabled?'启用':'停用'}}</el-tag></template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="有效权限查询">
        <el-form :inline="true" size="small"><el-form-item label="用户ID"><el-input v-model="effectiveUserId" /></el-form-item><el-form-item><el-button type="primary" @click="loadEffective">查询</el-button></el-form-item></el-form>
        <el-table :data="effectivePerms" v-loading="effectiveLoading" border size="small" empty-text="输入用户ID后查询">
          <el-table-column prop="permissionCode" label="权限码" min-width="180"/>
          <el-table-column prop="sourceRoles" label="来源角色"><template #default="{row}">{{row.sourceRoles?.join(', ')}}</template></el-table-column>
          <el-table-column prop="authorizationPaths" label="有效授权路径" min-width="260"><template #default="{row}">{{row.authorizationPaths?.join('; ')}}</template></el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>
