<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { internalApi } from '../../../api/internal-api'
import { usePermission } from '../../../composables/usePermission'

interface DeptItem {
  id: number; deptCode: string; deptName: string; organizationId: number; organizationName: string
  parentId: number | null; managerId: number | null; managerName: string; sortOrder: number
  path: string; level: number; status: string; description: string
  positionCount: number; userCount: number; version: number; createdAt: string; updatedAt: string
}
interface OrgOption { id: number; orgName: string; orgType: string }
interface UserOption { id: number; username: string; displayName: string }

const canCreate = usePermission('system:department:create')
const canUpdate = usePermission('system:department:update')
const canEnable = usePermission('system:department:enable')
const canDisable = usePermission('system:department:disable')

const loading = ref(false)
const list = ref<DeptItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const searchForm = reactive({ deptCode: '', deptName: '', organizationId: null as number | null, status: '' })
const orgOptions = ref<OrgOption[]>([])
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit' | 'view'>('view')
const saving = ref(false)
const form = reactive({
  id: null as number | null, deptCode: '', deptName: '', organizationId: null as number | null,
  parentId: null as number | null, managerId: null as number | null, managerName: '',
  sortOrder: 0, description: '', version: null as number | null
})
const parentOptions = ref<DeptItem[]>([])
const userSearchKeyword = ref('')
const userOptions = ref<UserOption[]>([])

const isView = computed(() => dialogMode.value === 'view')

async function loadOrgOptions() {
  try {
    const r = await internalApi.get<{ items: OrgOption[] }>('/system/dictionaries/items?dictCode=ORG_TYPE')
    orgOptions.value = r?.items ?? []
  } catch { /* ignore */ }
}

async function loadList() {
  loading.value = true
  try {
    const params = new URLSearchParams()
    if (searchForm.deptCode) params.set('deptCode', searchForm.deptCode)
    if (searchForm.deptName) params.set('deptName', searchForm.deptName)
    if (searchForm.organizationId) params.set('organizationId', String(searchForm.organizationId))
    if (searchForm.status) params.set('status', searchForm.status)
    params.set('page', String(page.value))
    params.set('pageSize', String(pageSize.value))
    const r = await internalApi.get<{ items: DeptItem[]; total: number }>('/system/departments?' + params.toString())
    list.value = r?.items ?? []
    total.value = r?.total ?? 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function loadParentOptions(orgId: number | null) {
  if (!orgId) { parentOptions.value = []; return }
  try {
    const r = await internalApi.get<{ items: DeptItem[] }>('/system/departments?organizationId=' + orgId + '&pageSize=999')
    parentOptions.value = r?.items ?? []
  } catch { parentOptions.value = [] }
}

async function searchUsers() {
  try {
    const r = await internalApi.get<{ items: UserOption[] }>('/system/users?pageSize=50&displayName=' + encodeURIComponent(userSearchKeyword.value))
    userOptions.value = r?.items ?? []
  } catch { userOptions.value = [] }
}

function openCreate() {
  dialogMode.value = 'create'
  Object.assign(form, {
    id: null, deptCode: '', deptName: '', organizationId: null,
    parentId: null, managerId: null, managerName: '', sortOrder: 0, description: '', version: null
  })
  parentOptions.value = []
  userOptions.value = []
  dialogVisible.value = true
}

function openEdit(row: DeptItem) {
  dialogMode.value = 'edit'
  Object.assign(form, {
    id: row.id, deptCode: row.deptCode, deptName: row.deptName, organizationId: row.organizationId,
    parentId: row.parentId, managerId: row.managerId, managerName: row.managerName,
    sortOrder: row.sortOrder, description: row.description, version: row.version
  })
  loadParentOptions(row.organizationId)
  userOptions.value = []
  dialogVisible.value = true
}

function openView(row: DeptItem) {
  dialogMode.value = 'view'
  Object.assign(form, {
    id: row.id, deptCode: row.deptCode, deptName: row.deptName, organizationId: row.organizationId,
    parentId: row.parentId, managerId: row.managerId, managerName: row.managerName,
    sortOrder: row.sortOrder, description: row.description, version: row.version
  })
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    const payload = {
      deptCode: form.deptCode, deptName: form.deptName, organizationId: form.organizationId,
      parentId: form.parentId, managerId: form.managerId, managerName: form.managerName,
      sortOrder: form.sortOrder, description: form.description, version: form.version
    }
    if (dialogMode.value === 'create') {
      await internalApi.post('/system/departments', payload)
    } else {
      await internalApi.request('/system/departments/' + form.id, { method: 'PUT', body: payload })
    }
    dialogVisible.value = false
    await loadList()
    ElMessage.success('保存成功')
  } catch (e: unknown) {
    ElMessage.error((e as Error)?.message || '保存失败')
  }
  finally { saving.value = false }
}

async function toggleStatus(row: DeptItem, action: string) {
  try {
    await ElMessageBox.confirm(action === 'enable' ? '确认启用该部门？' : '停用后该部门及其数据将被限制访问，确认继续？', '提示', { type: 'warning' })
    await internalApi.post('/system/departments/' + row.id + '/' + action)
    await loadList()
    ElMessage.success(action === 'enable' ? '已启用' : '已停用')
  } catch { /* cancelled */ }
}

function resetSearch() {
  searchForm.deptCode = ''
  searchForm.deptName = ''
  searchForm.organizationId = null
  searchForm.status = ''
  page.value = 1
  loadList()
}

onMounted(() => { loadOrgOptions(); loadList() })
</script>

<template>
  <div style="padding: 16px">
    <h3 style="margin: 0 0 12px">部门管理</h3>

    <el-form :inline="true" size="small" style="margin-bottom: 12px">
      <el-form-item label="部门编码"><el-input v-model="searchForm.deptCode" clearable placeholder="编码"/></el-form-item>
      <el-form-item label="部门名称"><el-input v-model="searchForm.deptName" clearable placeholder="名称"/></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" clearable placeholder="全部">
          <el-option label="启用" value="ACTIVE"/><el-option label="停用" value="DISABLED"/>
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" @click="page=1;loadList()">查询</el-button><el-button @click="resetSearch">重置</el-button></el-form-item>
    </el-form>

    <div style="margin-bottom: 12px">
      <el-button v-if="canCreate" type="primary" @click="openCreate">新增部门</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border size="small" empty-text="暂无部门数据">
      <el-table-column prop="deptCode" label="部门编码" width="120"/>
      <el-table-column prop="deptName" label="部门名称" min-width="140"/>
      <el-table-column prop="organizationName" label="所属组织" width="120"/>
      <el-table-column prop="managerName" label="负责人" width="100"/>
      <el-table-column prop="positionCount" label="岗位数" width="80"/>
      <el-table-column prop="userCount" label="用户数" width="80"/>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{row}"><el-tag :type="row.status==='ACTIVE'?'success':'info'" size="small">{{ row.status==='ACTIVE'?'启用':'停用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{row}">
          <el-button link type="primary" size="small" @click="openView(row)">查看</el-button>
          <el-button v-if="canUpdate" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status==='ACTIVE'?canDisable:canEnable" link size="small" :type="row.status==='ACTIVE'?'warning':'success'" @click="toggleStatus(row,row.status==='ACTIVE'?'disable':'enable')">{{ row.status==='ACTIVE'?'停用':'启用' }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-if="total>0" v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total,prev,pager,next,sizes" @change="loadList" style="margin-top:12px;justify-content:flex-end"/>

    <el-dialog v-model="dialogVisible" :title="dialogMode==='create'?'新增部门':dialogMode==='edit'?'编辑部门':'部门详情'" width="600px" :close-on-click-modal="false">
      <el-form label-width="100px" :disabled="isView">
        <el-form-item label="部门编码"><el-input v-model="form.deptCode" :disabled="dialogMode==='edit'||isView"/></el-form-item>
        <el-form-item label="部门名称"><el-input v-model="form.deptName"/></el-form-item>
        <el-form-item label="所属组织"><el-input v-model="form.organizationId" placeholder="组织ID" :disabled="dialogMode==='edit'"/></el-form-item>
        <el-form-item label="上级部门">
          <el-select v-model="form.parentId" clearable placeholder="无（根部门）" style="width:100%">
            <el-option v-for="p in parentOptions" :key="p.id" :label="p.deptName" :value="p.id" :disabled="p.id===form.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <div style="display:flex;gap:8px;width:100%">
            <el-input v-model="userSearchKeyword" placeholder="搜索用户" size="small" style="flex:1" @input="searchUsers"/>
            <el-select v-model="form.managerId" clearable placeholder="选择负责人" style="flex:2" filterable>
              <el-option v-for="u in userOptions" :key="u.id" :label="u.displayName + ' (' + u.username + ')'" :value="u.id"/>
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0"/></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.description" type="textarea" :rows="2"/></el-form-item>
      </el-form>
      <template #footer v-if="!isView">
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
