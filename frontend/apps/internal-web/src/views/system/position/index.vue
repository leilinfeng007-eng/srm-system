<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { internalApi } from '../../../api/internal-api'
import { usePermission } from '../../../composables/usePermission'

interface PosItem {
  id: number; positionCode: string; positionName: string; departmentId: number; departmentName: string
  category: string; responsibility: string; sortOrder: number; status: string
  userCount: number; version: number; createdAt: string; updatedAt: string
}
interface DeptOption { id: number; deptName: string; organizationId: number }

const canCreate = usePermission('system:position:create')
const canUpdate = usePermission('system:position:update')
const canEnable = usePermission('system:position:enable')
const canDisable = usePermission('system:position:disable')

const loading = ref(false)
const list = ref<PosItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const searchForm = reactive({ positionCode: '', positionName: '', departmentId: null as number | null, category: '', status: '' })
const deptOptions = ref<DeptOption[]>([])
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit' | 'view'>('view')
const saving = ref(false)
const form = reactive({
  id: null as number | null, positionCode: '', positionName: '', departmentId: null as number | null,
  category: '', responsibility: '', sortOrder: 0, version: null as number | null
})
const categoryOptions = ref<{ itemCode: string; itemName: string }[]>([])

const isView = computed(() => dialogMode.value === 'view')

async function loadDeptOptions() {
  try {
    const r = await internalApi.get<{ items: DeptOption[] }>('/system/departments?pageSize=999')
    deptOptions.value = r?.items ?? []
  } catch { /* ignore */ }
}

async function loadCategoryOptions() {
  try {
    const r = await internalApi.get<{ items: { itemCode: string; itemName: string }[] }>('/system/dictionaries/items?dictCode=POSITION_CATEGORY')
    categoryOptions.value = r?.items ?? []
  } catch { /* ignore */ }
}

async function loadList() {
  loading.value = true
  try {
    const params = new URLSearchParams()
    if (searchForm.positionCode) params.set('positionCode', searchForm.positionCode)
    if (searchForm.positionName) params.set('positionName', searchForm.positionName)
    if (searchForm.departmentId) params.set('departmentId', String(searchForm.departmentId))
    if (searchForm.category) params.set('category', searchForm.category)
    if (searchForm.status) params.set('status', searchForm.status)
    params.set('page', String(page.value))
    params.set('pageSize', String(pageSize.value))
    const r = await internalApi.get<{ items: PosItem[]; total: number }>('/system/positions?' + params.toString())
    list.value = r?.items ?? []
    total.value = r?.total ?? 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function openCreate() {
  dialogMode.value = 'create'
  Object.assign(form, { id: null, positionCode: '', positionName: '', departmentId: null, category: '', responsibility: '', sortOrder: 0, version: null })
  dialogVisible.value = true
}

function openEdit(row: PosItem) {
  dialogMode.value = 'edit'
  Object.assign(form, {
    id: row.id, positionCode: row.positionCode, positionName: row.positionName, departmentId: row.departmentId,
    category: row.category, responsibility: row.responsibility, sortOrder: row.sortOrder, version: row.version
  })
  dialogVisible.value = true
}

function openView(row: PosItem) {
  dialogMode.value = 'view'
  Object.assign(form, {
    id: row.id, positionCode: row.positionCode, positionName: row.positionName, departmentId: row.departmentId,
    category: row.category, responsibility: row.responsibility, sortOrder: row.sortOrder, version: row.version
  })
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    const payload = {
      positionCode: form.positionCode, positionName: form.positionName, departmentId: form.departmentId,
      category: form.category, responsibility: form.responsibility, sortOrder: form.sortOrder, version: form.version
    }
    if (dialogMode.value === 'create') {
      await internalApi.post('/system/positions', payload)
    } else {
      await internalApi.request('/system/positions/' + form.id, { method: 'PUT', body: payload })
    }
    dialogVisible.value = false
    await loadList()
    ElMessage.success('保存成功')
  } catch (e: unknown) {
    ElMessage.error((e as Error)?.message || '保存失败')
  }
  finally { saving.value = false }
}

async function toggleStatus(row: PosItem, action: string) {
  try {
    await ElMessageBox.confirm(action === 'enable' ? '确认启用该岗位？' : '停用后该岗位将被限制使用，确认继续？', '提示', { type: 'warning' })
    await internalApi.post('/system/positions/' + row.id + '/' + action)
    await loadList()
    ElMessage.success(action === 'enable' ? '已启用' : '已停用')
  } catch { /* cancelled */ }
}

function resetSearch() {
  searchForm.positionCode = ''
  searchForm.positionName = ''
  searchForm.departmentId = null
  searchForm.category = ''
  searchForm.status = ''
  page.value = 1
  loadList()
}

onMounted(() => { loadDeptOptions(); loadCategoryOptions(); loadList() })
</script>

<template>
  <div style="padding: 16px">
    <h3 style="margin: 0 0 12px">岗位管理</h3>

    <el-form :inline="true" size="small" style="margin-bottom: 12px">
      <el-form-item label="岗位编码"><el-input v-model="searchForm.positionCode" clearable placeholder="编码"/></el-form-item>
      <el-form-item label="岗位名称"><el-input v-model="searchForm.positionName" clearable placeholder="名称"/></el-form-item>
      <el-form-item label="所属部门">
        <el-select v-model="searchForm.departmentId" clearable filterable placeholder="全部">
          <el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName" :value="d.id"/>
        </el-select>
      </el-form-item>
      <el-form-item label="岗位类别">
        <el-select v-model="searchForm.category" clearable placeholder="全部">
          <el-option v-for="c in categoryOptions" :key="c.itemCode" :label="c.itemName" :value="c.itemCode"/>
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" clearable placeholder="全部">
          <el-option label="启用" value="ACTIVE"/><el-option label="停用" value="DISABLED"/>
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" @click="page=1;loadList()">查询</el-button><el-button @click="resetSearch">重置</el-button></el-form-item>
    </el-form>

    <div style="margin-bottom: 12px">
      <el-button v-if="canCreate" type="primary" @click="openCreate">新增岗位</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border size="small" empty-text="暂无岗位数据">
      <el-table-column prop="positionCode" label="岗位编码" width="120"/>
      <el-table-column prop="positionName" label="岗位名称" min-width="140"/>
      <el-table-column prop="departmentName" label="所属部门" width="120"/>
      <el-table-column prop="category" label="岗位类别" width="100">
        <template #default="{row}"><el-tag size="small">{{ row.category || '-' }}</el-tag></template>
      </el-table-column>
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

    <el-dialog v-model="dialogVisible" :title="dialogMode==='create'?'新增岗位':dialogMode==='edit'?'编辑岗位':'岗位详情'" width="550px" :close-on-click-modal="false">
      <el-form label-width="100px" :disabled="isView">
        <el-form-item label="岗位编码"><el-input v-model="form.positionCode" :disabled="dialogMode==='edit'||isView"/></el-form-item>
        <el-form-item label="岗位名称"><el-input v-model="form.positionName"/></el-form-item>
        <el-form-item label="所属部门">
          <el-select v-model="form.departmentId" filterable placeholder="选择部门" style="width:100%">
            <el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName" :value="d.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="岗位类别">
          <el-select v-model="form.category" clearable placeholder="选择类别" style="width:100%">
            <el-option v-for="c in categoryOptions" :key="c.itemCode" :label="c.itemName" :value="c.itemCode"/>
          </el-select>
        </el-form-item>
        <el-form-item label="岗位职责"><el-input v-model="form.responsibility" type="textarea" :rows="2"/></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0"/></el-form-item>
      </el-form>
      <template #footer v-if="!isView">
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
