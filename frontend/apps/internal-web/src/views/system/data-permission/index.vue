<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { internalApi } from '../../../api/internal-api'

interface RoleOption { id: number; roleCode: string; roleName: string; status: string; builtIn: boolean; roleCategory: string }
interface DataPolicy {
  domainCode: string; dimensionCode: string; scopeType: string; operationMode: string
  includeChildren: boolean; scopeOrgIds: string | null
}
interface DataPolicyView extends DataPolicy { roleId: number; roleCode: string; roleName: string }

const loading = ref(false)
const list = ref<DataPolicyView[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const roleOptions = ref<RoleOption[]>([])
const searchForm = reactive({ roleCode: '', scopeType: '', operationMode: '' })
const detailVisible = ref(false)
const detailPolicy = ref<DataPolicyView | null>(null)

const DIMENSION_LABELS: Record<string,string> = { ORGANIZATION:'组织', PURCHASING_ORGANIZATION:'采购组织', PLANT:'工厂', CATEGORY:'品类', OWNER:'本人数据', SUPPLIER:'供应商' }

async function loadRoleOptions() {
  try {
    const r = await internalApi.get<{ items: RoleOption[] }>('/system/roles?pageSize=999')
    roleOptions.value = r?.items ?? []
  } catch { /* ignore */ }
}

async function loadList() {
  loading.value = true
  try {
    const policies: DataPolicyView[] = []
    for (const role of roleOptions.value) {
      try {
        const detail = await internalApi.get<{ dataPolicies: DataPolicy[] }>('/system/roles/' + role.id)
        if (detail?.dataPolicies) {
          for (const p of detail.dataPolicies) {
            if ((!searchForm.roleCode || role.roleCode.includes(searchForm.roleCode)) &&
                (!searchForm.scopeType || p.scopeType === searchForm.scopeType) &&
                (!searchForm.operationMode || p.operationMode === searchForm.operationMode)) {
              policies.push({ ...p, roleId: role.id, roleCode: role.roleCode, roleName: role.roleName })
            }
          }
        }
      } catch { /* skip role without access */ }
    }
    total.value = policies.length
    const start = (page.value - 1) * pageSize.value
    list.value = policies.slice(start, start + pageSize.value)
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function viewDetail(row: DataPolicyView) {
  detailPolicy.value = row
  detailVisible.value = true
}

function resetSearch() {
  searchForm.roleCode = ''
  searchForm.scopeType = ''
  searchForm.operationMode = ''
  page.value = 1
  loadList()
}

onMounted(async () => { await loadRoleOptions(); await loadList() })
</script>

<template>
  <div style="padding: 16px">
    <h3 style="margin: 0 0 12px">数据权限</h3>
    <p style="color: #909399; font-size: 13px; margin: 0 0 16px">查看各角色配置的数据权限策略，包括数据范围、操作模式和生效状态</p>

    <el-form :inline="true" size="small" style="margin-bottom: 12px">
      <el-form-item label="角色"><el-input v-model="searchForm.roleCode" clearable placeholder="角色编码"/></el-form-item>
      <el-form-item label="范围类型">
        <el-select v-model="searchForm.scopeType" clearable placeholder="全部">
          <el-option label="ALL" value="ALL"/><el-option label="ORG" value="ORG"/><el-option label="SELF" value="SELF"/>
        </el-select>
      </el-form-item>
      <el-form-item label="操作模式">
        <el-select v-model="searchForm.operationMode" clearable placeholder="全部">
          <el-option label="读写" value="READ_WRITE"/><el-option label="只读" value="READONLY"/>
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" @click="page=1;loadList()">查询</el-button><el-button @click="resetSearch">重置</el-button></el-form-item>
    </el-form>

    <el-table :data="list" v-loading="loading" border size="small" empty-text="暂无数据权限策略">
      <el-table-column prop="roleCode" label="角色编码" width="140"/>
      <el-table-column prop="roleName" label="角色名称" width="120"/>
      <el-table-column prop="domainCode" label="领域" width="100"/>
      <el-table-column label="维度" width="140">
        <template #default="{row}">{{ DIMENSION_LABELS[row.dimensionCode] || row.dimensionCode }}</template>
      </el-table-column>
      <el-table-column prop="scopeType" label="范围类型" width="80"/>
      <el-table-column prop="operationMode" label="操作模式" width="80">
        <template #default="{row}"><el-tag :type="row.operationMode==='READ_WRITE'?'success':'warning'" size="small">{{ row.operationMode==='READ_WRITE'?'读写':'只读' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="含下级" width="70">
        <template #default="{row}">{{ row.includeChildren?'是':'否' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="80">
        <template #default="{row}"><el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button></template>
      </el-table-column>
    </el-table>

    <el-pagination v-if="total>0" v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total,prev,pager,next" @change="loadList" style="margin-top:12px;justify-content:flex-end"/>

    <el-dialog v-model="detailVisible" title="数据权限策略详情" width="500px">
      <el-descriptions v-if="detailPolicy" :column="1" border size="small">
        <el-descriptions-item label="角色">{{ detailPolicy.roleName }} ({{ detailPolicy.roleCode }})</el-descriptions-item>
        <el-descriptions-item label="领域">{{ detailPolicy.domainCode }}</el-descriptions-item>
        <el-descriptions-item label="维度">{{ DIMENSION_LABELS[detailPolicy.dimensionCode] || detailPolicy.dimensionCode }}</el-descriptions-item>
        <el-descriptions-item label="范围类型">{{ detailPolicy.scopeType }}</el-descriptions-item>
        <el-descriptions-item label="操作模式">{{ detailPolicy.operationMode==='READ_WRITE'?'可读写':'只读' }}</el-descriptions-item>
        <el-descriptions-item label="包含下级组织">{{ detailPolicy.includeChildren?'是':'否' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>
