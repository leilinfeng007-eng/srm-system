<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { PageResult } from '@srm/shared-types'
import { internalApi } from '../../../api/internal-api'
import { usePermission } from '../../../composables/usePermission'

interface ExternalMapping {
  id: number
  objectType: string
  internalId: string
  sourceSystem: string
  externalId: string
  externalLineId: string|null
  externalVersion: number
  mappingStatus: string
  conflictSummary: string|null
  createdAt: string
}

const canCreate = usePermission('masterdata:external-mapping:create')

const loading = ref(false)
const data = ref<ExternalMapping[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const searchForm = reactive({ keyword: '', status: '' as string })

const fetchData = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams({ page: String(page.value), size: String(pageSize.value) })
    if (searchForm.keyword) params.set('sourceSystem', searchForm.keyword)
    if (searchForm.status) params.set('status', searchForm.status)
    const result = await internalApi.request<PageResult<ExternalMapping>>(`/master-data/external-mappings?${params.toString()}`, { method: 'GET' })
    data.value = result.items; total.value = result.total
  } catch { data.value = []; total.value = 0 } finally { loading.value = false }
}

const handleSearch = () => { page.value = 1; fetchData() }
const handleReset = () => { searchForm.keyword = ''; searchForm.status = ''; handleSearch() }
const handlePageChange = (p: number) => { page.value = p; fetchData() }
const handleSizeChange = (s: number) => { pageSize.value = s; page.value = 1; fetchData() }

const dialogVisible = ref(false)
const detailVisible = ref(false)
const selected = ref<ExternalMapping|null>(null)
const submitting = ref(false)
const formRef = ref()
const form = reactive({ objectType: '', internalId: '', sourceSystem: '', externalId: '', externalLineId: '', externalVersion: 1, mappingStatus: 'ACTIVE', conflictSummary: '' })

const openCreate = () => { Object.assign(form, { objectType: '', internalId: '', sourceSystem: '', externalId: '', externalLineId: '', externalVersion: 1, mappingStatus: 'ACTIVE', conflictSummary: '' }); dialogVisible.value = true }
const openDetail = async (row: ExternalMapping) => { try { selected.value = await internalApi.get<ExternalMapping>(`/master-data/external-mappings/${row.id}`); detailVisible.value = true } catch { ElMessage.error('详情加载失败') } }

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    await internalApi.post('/master-data/external-mappings', { ...form }); ElMessage.success('创建成功')
    dialogVisible.value = false; fetchData()
  } catch (e: unknown) { if (e instanceof Error && e.name !== 'ApiError') throw e } finally { submitting.value = false }
}

const formRules = { objectType: [{ required: true, message: '请输入对象类型', trigger: 'blur' }], internalId: [{ required: true, message: '请输入内部ID', trigger: 'blur' }], sourceSystem: [{ required: true, message: '请输入源系统', trigger: 'blur' }], externalId: [{ required: true, message: '请输入外部ID', trigger: 'blur' }] }

onMounted(fetchData)
</script>

<template>
  <section class="crud-page">
    <el-card>
      <div class="search-bar">
        <el-input v-model="searchForm.keyword" placeholder="按源系统查询" clearable style="width: 260px" @keyup.enter="handleSearch" />
        <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 120px"><el-option label="启用" value="ACTIVE"/><el-option label="冲突" value="CONFLICT"/><el-option label="停用" value="INACTIVE"/></el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button v-if="canCreate" type="primary" style="margin-left: auto" @click="openCreate">新增映射</el-button>
      </div>
      <el-table v-loading="loading" :data="data" stripe border style="margin-top: 16px">
        <el-table-column prop="objectType" label="对象类型" width="130" />
        <el-table-column prop="internalId" label="内部ID" width="120" />
        <el-table-column prop="sourceSystem" label="源系统" width="120" />
        <el-table-column prop="externalId" label="外部ID" width="140" />
        <el-table-column prop="externalVersion" label="外部版本" width="90" />
        <el-table-column prop="mappingStatus" label="状态" width="90" />
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="90" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openDetail(row)">详情</el-button></template></el-table-column>
        <template #empty><el-empty description="暂无映射数据" :image-size="80" /></template>
      </el-table>
      <div class="pagination-bar"><el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @current-change="handlePageChange" @size-change="handleSizeChange" /></div>
    </el-card>
    <el-dialog v-model="dialogVisible" title="新增映射" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="对象类型" prop="objectType"><el-input v-model="form.objectType" maxlength="64" /></el-form-item>
        <el-form-item label="内部ID" prop="internalId"><el-input v-model="form.internalId" maxlength="100" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="源系统" prop="sourceSystem"><el-input v-model="form.sourceSystem" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="外部ID" prop="externalId"><el-input v-model="form.externalId" maxlength="100" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="外部行ID"><el-input v-model="form.externalLineId" maxlength="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="外部版本"><el-input-number v-model="form.externalVersion" :min="0" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="映射状态"><el-input v-model="form.mappingStatus" maxlength="20" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button></template>
    </el-dialog>
    <el-dialog v-model="detailVisible" title="外围映射详情" width="620px"><el-descriptions v-if="selected" :column="2" border><el-descriptions-item label="对象类型">{{selected.objectType}}</el-descriptions-item><el-descriptions-item label="内部ID">{{selected.internalId}}</el-descriptions-item><el-descriptions-item label="源系统">{{selected.sourceSystem}}</el-descriptions-item><el-descriptions-item label="外部ID">{{selected.externalId}}</el-descriptions-item><el-descriptions-item label="外部行ID">{{selected.externalLineId||'-'}}</el-descriptions-item><el-descriptions-item label="外部版本">{{selected.externalVersion}}</el-descriptions-item><el-descriptions-item label="状态">{{selected.mappingStatus}}</el-descriptions-item><el-descriptions-item label="冲突摘要">{{selected.conflictSummary||'-'}}</el-descriptions-item></el-descriptions></el-dialog>
  </section>
</template>

<style scoped>
.crud-page { padding: 24px; }
.search-bar { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.pagination-bar { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
