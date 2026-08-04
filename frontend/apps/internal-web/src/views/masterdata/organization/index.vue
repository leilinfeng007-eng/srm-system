<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { internalApi } from '../../../api/internal-api'
import { usePermission } from '../../../composables/usePermission'

interface PlantInfo { id: number; plantCode: string; plantName: string; timezone: string; country: string; address: string }
interface WarehouseInfo { id: number; warehouseCode: string; warehouseName: string; plantId: number; warehouseType: string }
interface OrgNode { id: number; orgCode: string; orgName: string; orgType: string; sortOrder: number; status: string; level: number; children: OrgNode[] }
interface OrgDetail extends Omit<OrgNode, 'children'> { parentId: number|null; path: string; description: string; version: number; plant: PlantInfo|null; warehouse: WarehouseInfo|null }
interface DeptItem { id: number; deptCode: string; deptName: string; organizationId: number; parentId: number|null; status: string; managerName: string; sortOrder: number; description: string; version: number }
interface PosItem { id: number; positionCode: string; positionName: string; departmentId: number; status: string; responsibility: string; sortOrder: number; version: number }

const canCreate = usePermission('masterdata:organization:create')
const canUpdate = usePermission('masterdata:organization:update')
const canEnable = usePermission('masterdata:organization:enable')
const canDisable = usePermission('masterdata:organization:disable')
const treeLoading = ref(false)
const treeData = ref<OrgNode[]>([])
const activeTab = ref<'dept'|'pos'>('dept')
const selectedOrg = ref<OrgDetail|null>(null)
const selectedDeptId = ref<number|null>(null)
const depts = ref<DeptItem[]>([])
const positions = ref<PosItem[]>([])
const detailLoading = ref(false)

const orgFormVisible = ref(false)
const orgFormMode = ref<'create'|'edit'>('create')
const parentOptions = ref<OrgDetail[]>([])
const orgForm = reactive({ id: null as number|null, orgCode: '', orgName: '', orgType: 'GROUP', parentId: null as number|null, sortOrder: 0, description: '', version: null as number|null, plantCode: '', plantName: '', timezone: '', country: '', address: '', warehouseCode: '', warehouseName: '', plantId: null as number|null, warehouseType: '' })
const deptFormVisible = ref(false)
const deptForm = reactive({ id: null as number|null, deptCode: '', deptName: '', organizationId: null as number|null, parentId: null as number|null, managerName: '', sortOrder: 0, description: '', version: null as number|null })
const posFormVisible = ref(false)
const posForm = reactive({ id: null as number|null, positionCode: '', positionName: '', departmentId: null as number|null, responsibility: '', sortOrder: 0, version: null as number|null })

const orgTypes = [
  { value: 'GROUP', label: '集团' }, { value: 'COMPANY', label: '公司' },
  { value: 'BUSINESS_UNIT', label: '事业部' }, { value: 'PLANT', label: '工厂' },
  { value: 'WAREHOUSE', label: '仓库' },
]

async function loadTree() {
  treeLoading.value = true
  try { treeData.value = await internalApi.get<OrgNode[]>('/master-data/organizations') ?? [] }
  catch { ElMessage.error('加载组织树失败') }
  finally { treeLoading.value = false }
}

async function selectOrganization(id: number) {
  detailLoading.value = true
  try {
    selectedOrg.value = await internalApi.get<OrgDetail>('/master-data/organizations/' + id) ?? null
    depts.value = await internalApi.get<DeptItem[]>('/system/departments?organizationId=' + id) ?? []
    selectedDeptId.value = null
    positions.value = []
    activeTab.value = 'dept'
  } catch { ElMessage.error('加载组织详情失败') }
  finally { detailLoading.value = false }
}

async function loadParents(orgType: string) {
  orgForm.parentId = null
  orgForm.plantId = null
  if (orgType === 'GROUP') { parentOptions.value = []; return }
  try { parentOptions.value = await internalApi.get<OrgDetail[]>('/master-data/organizations/valid-parents?orgType=' + encodeURIComponent(orgType)) ?? [] }
  catch { parentOptions.value = [] }
}

function onParentChange(parentId: number|null) {
  const parent = parentOptions.value.find(item => item.id === parentId)
  orgForm.plantId = orgForm.orgType === 'WAREHOUSE' ? parent?.plant?.id ?? null : null
}

async function openOrgCreate() {
  orgFormMode.value = 'create'
  Object.assign(orgForm, { id: null, orgCode: '', orgName: '', orgType: 'GROUP', parentId: null, sortOrder: 0, description: '', version: null, plantCode: '', plantName: '', timezone: '', country: '', address: '', warehouseCode: '', warehouseName: '', plantId: null, warehouseType: '' })
  parentOptions.value = []
  orgFormVisible.value = true
}

async function openOrgEdit(node: OrgNode) {
  try {
    const detail = await internalApi.get<OrgDetail>('/master-data/organizations/' + node.id)
    if (!detail) return
    orgFormMode.value = 'edit'
    Object.assign(orgForm, { id: detail.id, orgCode: detail.orgCode, orgName: detail.orgName, orgType: detail.orgType, parentId: detail.parentId, sortOrder: detail.sortOrder, description: detail.description ?? '', version: detail.version, plantCode: detail.plant?.plantCode ?? '', plantName: detail.plant?.plantName ?? '', timezone: detail.plant?.timezone ?? '', country: detail.plant?.country ?? '', address: detail.plant?.address ?? '', warehouseCode: detail.warehouse?.warehouseCode ?? '', warehouseName: detail.warehouse?.warehouseName ?? '', plantId: detail.warehouse?.plantId ?? null, warehouseType: detail.warehouse?.warehouseType ?? '' })
    orgFormVisible.value = true
  } catch { ElMessage.error('加载组织详情失败') }
}

async function saveOrg() {
  try {
    if (orgFormMode.value === 'create') await internalApi.post('/master-data/organizations', { ...orgForm })
    else await internalApi.request('/master-data/organizations/' + orgForm.id, { method: 'PUT', body: { orgName: orgForm.orgName, sortOrder: orgForm.sortOrder, description: orgForm.description, version: orgForm.version, plantName: orgForm.plantName, timezone: orgForm.timezone, country: orgForm.country, address: orgForm.address, warehouseName: orgForm.warehouseName, plantId: orgForm.plantId, warehouseType: orgForm.warehouseType } })
    orgFormVisible.value = false
    await loadTree()
    if (orgForm.id) await selectOrganization(orgForm.id)
    ElMessage.success('保存成功')
  } catch (e: unknown) { ElMessage.error((e as Error)?.message || '保存失败') }
}

async function toggle(path: string, label: string, reload: () => Promise<void>) {
  try { await ElMessageBox.confirm(`确认${label}？`); await internalApi.post(path); await reload(); ElMessage.success(`${label}成功`) }
  catch { /* 用户取消或接口已提示 */ }
}

function openDeptCreate() {
  if (!selectedOrg.value) return
  Object.assign(deptForm, { id: null, deptCode: '', deptName: '', organizationId: selectedOrg.value.id, parentId: null, managerName: '', sortOrder: 0, description: '', version: null })
  deptFormVisible.value = true
}

function openDeptEdit(row: DeptItem) { Object.assign(deptForm, row); deptFormVisible.value = true }

async function saveDept() {
  try {
    if (!deptForm.id) await internalApi.post('/system/departments', { ...deptForm })
    else await internalApi.request('/system/departments/' + deptForm.id, { method: 'PUT', body: { ...deptForm } })
    deptFormVisible.value = false
    if (selectedOrg.value) await selectOrganization(selectedOrg.value.id)
    ElMessage.success('保存成功')
  } catch (e: unknown) { ElMessage.error((e as Error)?.message || '保存失败') }
}

async function selectDepartment(row: DeptItem) {
  selectedDeptId.value = row.id
  positions.value = await internalApi.get<PosItem[]>('/system/positions?departmentId=' + row.id) ?? []
  activeTab.value = 'pos'
}

function openPosCreate() {
  if (!selectedDeptId.value) return
  Object.assign(posForm, { id: null, positionCode: '', positionName: '', departmentId: selectedDeptId.value, responsibility: '', sortOrder: 0, version: null })
  posFormVisible.value = true
}

function openPosEdit(row: PosItem) { Object.assign(posForm, row); posFormVisible.value = true }

async function savePos() {
  try {
    if (!posForm.id) await internalApi.post('/system/positions', { ...posForm })
    else await internalApi.request('/system/positions/' + posForm.id, { method: 'PUT', body: { ...posForm } })
    posFormVisible.value = false
    if (selectedDeptId.value) positions.value = await internalApi.get<PosItem[]>('/system/positions?departmentId=' + selectedDeptId.value) ?? []
    ElMessage.success('保存成功')
  } catch (e: unknown) { ElMessage.error((e as Error)?.message || '保存失败') }
}

onMounted(loadTree)
</script>

<template>
  <div class="org-page">
    <div class="org-tree-panel">
      <div class="tree-header"><b>企业组织</b><el-button v-if="canCreate" type="primary" size="small" @click="openOrgCreate">新增</el-button></div>
      <el-tree :data="treeData" :props="{ children: 'children', label: 'orgName' }" node-key="id" default-expand-all highlight-current v-loading="treeLoading" @node-click="(data: OrgNode) => selectOrganization(data.id)">
        <template #default="{ data }"><span class="tree-node"><span><el-tag size="small" class="org-tag">{{ data.orgType }}</el-tag>{{ data.orgName }}</span><span class="tree-actions"><el-button v-if="canUpdate" link type="primary" size="small" @click.stop="openOrgEdit(data)">编辑</el-button><el-button v-if="data.status === 'ACTIVE' ? canDisable : canEnable" link size="small" @click.stop="toggle('/master-data/organizations/' + data.id + '/' + (data.status === 'ACTIVE' ? 'disable' : 'enable'), data.status === 'ACTIVE' ? '停用' : '启用', loadTree)">{{ data.status === 'ACTIVE' ? '停用' : '启用' }}</el-button></span></span></template>
      </el-tree>
    </div>
    <div class="org-detail" v-loading="detailLoading">
      <template v-if="selectedOrg">
        <el-descriptions :title="selectedOrg.orgName" :column="2" border size="small">
          <el-descriptions-item label="编码">{{ selectedOrg.orgCode }}</el-descriptions-item><el-descriptions-item label="类型">{{ orgTypes.find(t => t.value === selectedOrg?.orgType)?.label }}</el-descriptions-item>
          <el-descriptions-item label="上级ID">{{ selectedOrg.parentId ?? '-' }}</el-descriptions-item><el-descriptions-item label="状态">{{ selectedOrg.status }}</el-descriptions-item>
          <el-descriptions-item v-if="selectedOrg.plant" label="工厂">{{ selectedOrg.plant.plantCode }} / {{ selectedOrg.plant.plantName }}</el-descriptions-item>
          <el-descriptions-item v-if="selectedOrg.warehouse" label="仓库">{{ selectedOrg.warehouse.warehouseCode }} / {{ selectedOrg.warehouse.warehouseName }}</el-descriptions-item>
        </el-descriptions>
        <el-tabs v-model="activeTab" class="org-tabs">
          <el-tab-pane label="部门" name="dept">
            <div class="tab-header"><b>部门列表</b><el-button v-if="canCreate" type="primary" size="small" @click="openDeptCreate">新增</el-button></div>
            <el-table :data="depts" size="small" border empty-text="暂无部门">
              <el-table-column prop="deptCode" label="编码"/><el-table-column prop="deptName" label="名称"/><el-table-column prop="managerName" label="负责人"/><el-table-column prop="status" label="状态" width="90"/>
              <el-table-column label="操作" width="220"><template #default="{ row }"><el-button link size="small" @click="selectDepartment(row)">岗位</el-button><el-button v-if="canUpdate" link type="primary" size="small" @click="openDeptEdit(row)">编辑</el-button><el-button v-if="row.status === 'ACTIVE' ? canDisable : canEnable" link size="small" @click="toggle('/system/departments/' + row.id + '/' + (row.status === 'ACTIVE' ? 'disable' : 'enable'), row.status === 'ACTIVE' ? '停用' : '启用', async () => { if (selectedOrg) await selectOrganization(selectedOrg.id) })">{{ row.status === 'ACTIVE' ? '停用' : '启用' }}</el-button></template></el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="岗位" name="pos">
            <div class="tab-header"><b>岗位列表</b><el-button v-if="canCreate" type="primary" size="small" :disabled="!selectedDeptId" @click="openPosCreate">新增</el-button></div>
            <el-table :data="positions" size="small" border empty-text="选择部门后查看岗位">
              <el-table-column prop="positionCode" label="编码"/><el-table-column prop="positionName" label="名称"/><el-table-column prop="responsibility" label="职责"/><el-table-column prop="status" label="状态" width="90"/>
              <el-table-column label="操作" width="160"><template #default="{ row }"><el-button v-if="canUpdate" link type="primary" size="small" @click="openPosEdit(row)">编辑</el-button><el-button v-if="row.status === 'ACTIVE' ? canDisable : canEnable" link size="small" @click="toggle('/system/positions/' + row.id + '/' + (row.status === 'ACTIVE' ? 'disable' : 'enable'), row.status === 'ACTIVE' ? '停用' : '启用', async () => { if (selectedDeptId) positions = await internalApi.get('/system/positions?departmentId=' + selectedDeptId) ?? [] })">{{ row.status === 'ACTIVE' ? '停用' : '启用' }}</el-button></template></el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </template>
      <el-empty v-else description="请选择左侧组织节点"/>
    </div>
  </div>

  <el-dialog v-model="orgFormVisible" :title="orgFormMode === 'create' ? '新增组织' : '编辑组织'" width="620px">
    <el-form label-width="90px"><el-row :gutter="12"><el-col :span="12"><el-form-item label="编码"><el-input v-model="orgForm.orgCode" :disabled="orgFormMode === 'edit'"/></el-form-item></el-col><el-col :span="12"><el-form-item label="名称"><el-input v-model="orgForm.orgName"/></el-form-item></el-col></el-row>
      <el-row :gutter="12"><el-col :span="12"><el-form-item label="类型"><el-select v-model="orgForm.orgType" :disabled="orgFormMode === 'edit'" @change="loadParents"><el-option v-for="t in orgTypes" :key="t.value" :label="t.label" :value="t.value"/></el-select></el-form-item></el-col><el-col :span="12"><el-form-item v-if="orgFormMode === 'create' && orgForm.orgType !== 'GROUP'" label="上级组织"><el-select v-model="orgForm.parentId" @change="onParentChange"><el-option v-for="p in parentOptions" :key="p.id" :label="p.orgName" :value="p.id"/></el-select></el-form-item></el-col></el-row>
      <el-form-item label="说明"><el-input v-model="orgForm.description"/></el-form-item>
      <template v-if="orgForm.orgType === 'PLANT'"><el-row :gutter="12"><el-col :span="12"><el-form-item label="工厂编码"><el-input v-model="orgForm.plantCode" :disabled="orgFormMode === 'edit'"/></el-form-item></el-col><el-col :span="12"><el-form-item label="工厂名称"><el-input v-model="orgForm.plantName"/></el-form-item></el-col></el-row><el-row :gutter="12"><el-col :span="12"><el-form-item label="时区"><el-input v-model="orgForm.timezone"/></el-form-item></el-col><el-col :span="12"><el-form-item label="国家"><el-input v-model="orgForm.country"/></el-form-item></el-col></el-row><el-form-item label="地址"><el-input v-model="orgForm.address"/></el-form-item></template>
      <template v-if="orgForm.orgType === 'WAREHOUSE'"><el-row :gutter="12"><el-col :span="12"><el-form-item label="仓库编码"><el-input v-model="orgForm.warehouseCode" :disabled="orgFormMode === 'edit'"/></el-form-item></el-col><el-col :span="12"><el-form-item label="仓库名称"><el-input v-model="orgForm.warehouseName"/></el-form-item></el-col></el-row><el-form-item label="仓库类型"><el-input v-model="orgForm.warehouseType"/></el-form-item></template>
    </el-form><template #footer><el-button @click="orgFormVisible = false">取消</el-button><el-button type="primary" @click="saveOrg">保存</el-button></template>
  </el-dialog>

  <el-dialog v-model="deptFormVisible" :title="deptForm.id ? '编辑部门' : '新增部门'" width="460px"><el-form label-width="80px"><el-form-item label="编码"><el-input v-model="deptForm.deptCode" :disabled="!!deptForm.id"/></el-form-item><el-form-item label="名称"><el-input v-model="deptForm.deptName"/></el-form-item><el-form-item label="负责人"><el-input v-model="deptForm.managerName"/></el-form-item><el-form-item label="说明"><el-input v-model="deptForm.description"/></el-form-item></el-form><template #footer><el-button @click="deptFormVisible = false">取消</el-button><el-button type="primary" @click="saveDept">保存</el-button></template></el-dialog>
  <el-dialog v-model="posFormVisible" :title="posForm.id ? '编辑岗位' : '新增岗位'" width="460px"><el-form label-width="80px"><el-form-item label="编码"><el-input v-model="posForm.positionCode" :disabled="!!posForm.id"/></el-form-item><el-form-item label="名称"><el-input v-model="posForm.positionName"/></el-form-item><el-form-item label="职责"><el-input v-model="posForm.responsibility" type="textarea"/></el-form-item></el-form><template #footer><el-button @click="posFormVisible = false">取消</el-button><el-button type="primary" @click="savePos">保存</el-button></template></el-dialog>
</template>

<style scoped>
.org-page { display:flex; height:calc(100vh - 130px); gap:12px; padding:12px; }
.org-tree-panel { width:320px; border:1px solid #e5e5e5; border-radius:4px; overflow:auto; padding:10px; }
.org-detail { flex:1; overflow:auto; }
.tree-header,.tree-node,.tab-header { display:flex; justify-content:space-between; align-items:center; }
.tree-node { width:100%; }.tree-actions { flex-shrink:0; }.org-tag { margin-right:4px; }.org-tabs { margin-top:10px; }
</style>
