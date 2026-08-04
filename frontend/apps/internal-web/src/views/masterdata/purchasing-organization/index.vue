<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { PageResult } from '@srm/shared-types'
import { internalApi } from '../../../api/internal-api'
import { usePermission } from '../../../composables/usePermission'

interface PurchasingOrg {
  id: number
  poName: string
  poCode: string
  companyOrgId: number
  defaultCurrency: string
  status: 'ACTIVE' | 'INACTIVE'
  createdAt: string
}

const canCreate = usePermission('masterdata:purchasing-organization:create')
const canEdit = usePermission('masterdata:purchasing-organization:update')
const canEnable = usePermission('masterdata:purchasing-organization:enable')
const canDisable = usePermission('masterdata:purchasing-organization:disable')

const loading = ref(false)
const data = ref<PurchasingOrg[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const searchForm = reactive({ keyword: '', status: '' as string })

const fetchData = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams({ page: String(page.value), size: String(pageSize.value) })
    if (searchForm.keyword) params.set('keyword', searchForm.keyword)
    if (searchForm.status) params.set('status', searchForm.status)
    const result = await internalApi.request<PageResult<PurchasingOrg>>(`/master-data/purchasing-organizations?${params.toString()}`, { method: 'GET' })
    data.value = result.items; total.value = result.total
  } catch { data.value = []; total.value = 0 } finally { loading.value = false }
}

const handleSearch = () => { page.value = 1; fetchData() }
const handleReset = () => { searchForm.keyword = ''; searchForm.status = ''; handleSearch() }
const handlePageChange = (p: number) => { page.value = p; fetchData() }
const handleSizeChange = (s: number) => { pageSize.value = s; page.value = 1; fetchData() }

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitting = ref(false)
const formRef = ref()
const form = reactive({ id: 0, poName: '', poCode: '', companyOrgId: null as number | null, defaultCurrency: 'CNY' })

const openCreate = () => { dialogMode.value = 'create'; Object.assign(form, { id: 0, poName: '', poCode: '', companyOrgId: null, defaultCurrency: 'CNY' }); dialogVisible.value = true }
const openEdit = async (row: PurchasingOrg) => { try { const detail = await internalApi.get<PurchasingOrg>(`/master-data/purchasing-organizations/${row.id}`); dialogMode.value = 'edit'; Object.assign(form, detail); dialogVisible.value = true } catch { ElMessage.error('详情加载失败') } }

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (dialogMode.value === 'create') { await internalApi.post('/master-data/purchasing-organizations', { ...form }); ElMessage.success('创建成功') }
    else { await internalApi.request(`/master-data/purchasing-organizations/${form.id}`, { method: 'PUT', body: { ...form } }); ElMessage.success('更新成功') }
    dialogVisible.value = false; fetchData()
  } catch (e: unknown) { if (e instanceof Error && e.name !== 'ApiError') throw e } finally { submitting.value = false }
}

const handleEnable = async (row: PurchasingOrg) => { try { await internalApi.post(`/master-data/purchasing-organizations/${row.id}/enable`); ElMessage.success('已启用'); fetchData() } catch { void 0 } }
const handleDisable = async (row: PurchasingOrg) => { try { await ElMessageBox.confirm(`确定禁用 "${row.poName}" 吗？`, '确认', { type: 'warning' }); await internalApi.post(`/master-data/purchasing-organizations/${row.id}/disable`); ElMessage.success('已禁用'); fetchData() } catch { void 0 } }

const formRules = { poName: [{ required: true, message: '请输入名称', trigger: 'blur' }], poCode: [{ required: true, message: '请输入编码', trigger: 'blur' }], companyOrgId: [{ required: true, message: '请输入公司组织ID', trigger: 'change' }] }

onMounted(fetchData)
</script>

<template>
  <section class="crud-page">
    <el-card>
      <div class="search-bar">
        <el-input v-model="searchForm.keyword" placeholder="搜索名称/编码" clearable style="width: 220px" @keyup.enter="handleSearch" />
        <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 120px">
          <el-option label="启用" value="ACTIVE" /><el-option label="禁用" value="INACTIVE" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button v-if="canCreate" type="primary" style="margin-left: auto" @click="openCreate">新增采购组织</el-button>
      </div>
      <el-table v-loading="loading" :data="data" stripe border style="margin-top: 16px">
        <el-table-column prop="poName" label="名称" width="180" />
        <el-table-column prop="poCode" label="编码" width="140" />
        <el-table-column prop="companyOrgId" label="公司组织ID" width="120" />
        <el-table-column prop="defaultCurrency" label="默认币种" width="100" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }"><el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">{{ row.status === 'ACTIVE' ? '启用' : '禁用' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canEdit" type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="canEnable && row.status !== 'ACTIVE'" type="success" link size="small" @click="handleEnable(row)">启用</el-button>
            <el-button v-if="canDisable && row.status === 'ACTIVE'" type="warning" link size="small" @click="handleDisable(row)">禁用</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无数据" :image-size="80" /></template>
      </el-table>
      <div class="pagination-bar"><el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @current-change="handlePageChange" @size-change="handleSizeChange" /></div>
    </el-card>
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增采购组织' : '编辑采购组织'" width="500px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="名称" prop="poName"><el-input v-model="form.poName" maxlength="100" /></el-form-item>
        <el-form-item label="编码" prop="poCode"><el-input v-model="form.poCode" maxlength="50" :disabled="dialogMode === 'edit'" /></el-form-item>
        <el-form-item label="公司组织" prop="companyOrgId"><el-input-number v-model="form.companyOrgId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="默认币种"><el-input v-model="form.defaultCurrency" maxlength="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button></template>
    </el-dialog>
  </section>
</template>

<style scoped>
.crud-page { padding: 24px; }
.search-bar { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.pagination-bar { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
