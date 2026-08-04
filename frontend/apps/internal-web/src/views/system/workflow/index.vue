<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { internalApi } from '../../../api/internal-api'
import { usePermission } from '../../../composables/usePermission'

interface WorkflowDef { id:number; processCode:string; processName:string; businessType:string; definitionVersion:number; status:string; description:string; createdAt:string; updatedAt:string }
interface WorkflowNode { id:number|null; nodeCode:string; nodeName:string; nodeType:string; sortOrder:number; assigneeType:string; assigneeValue:string; durationHours:number|null }

const loading=ref(false); const list=ref<WorkflowDef[]>([]); const total=ref(0)
const page=ref(1); const pageSize=ref(10)
const searchForm=reactive({ keyword:'', status:'' })
const dialogVisible=ref(false); const nodeDialogVisible=ref(false)
const form=reactive({ id:null as number|null, processCode:'', processName:'', businessType:'SYSTEM_PARAMETER', description:'' })
const nodes=ref<WorkflowNode[]>([])
const canCreate=usePermission('system:workflow:create');const canUpdate=usePermission('system:workflow:update')
const canPublish=usePermission('system:workflow:publish');const canRetire=usePermission('system:workflow:retire')

async function fetchData() {
  loading.value=true
  try {
    const qs=new URLSearchParams({page:String(page.value),pageSize:String(pageSize.value)})
    if(searchForm.status) qs.set('status',searchForm.status)
    const r=await internalApi.get<{items:WorkflowDef[],total:number}>('/system/workflows?'+qs.toString())
    list.value=r?.items??[]; total.value=r?.total??0
  } catch(e){ void e; list.value=[]; total.value=0 } finally { loading.value=false }
}

function openCreate() { Object.assign(form,{id:null,processCode:'',processName:'',businessType:'SYSTEM_PARAMETER',description:''}); nodes.value=[]; dialogVisible.value=true }
async function openEdit(row:WorkflowDef) {
  Object.assign(form,{id:row.id,processCode:row.processCode,processName:row.processName,businessType:row.businessType,description:row.description})
  dialogVisible.value=true
  try { const r=await internalApi.get<WorkflowNode[]>('/system/workflows/'+row.id+'/nodes'); nodes.value=r??[] } catch(e){ void e; nodes.value=[] }
}
async function saveFlow() {
  try {
    if(!form.id){ await internalApi.post('/system/workflows',{...form,nodes:nodes.value}) }
    else { await internalApi.request('/system/workflows/'+form.id,{method:'PUT',body:{processName:form.processName,description:form.description,nodes:nodes.value}}) }
    dialogVisible.value=false; fetchData(); ElMessage.success('保存成功')
  } catch(e:unknown){ void e; ElMessage.error((e as Error)?.message||'保存失败') }
}
async function publishFlow(row:WorkflowDef) {
  try { await ElMessageBox.confirm('确认发布该流程?'); await internalApi.post('/system/workflows/'+row.id+'/publish'); fetchData(); ElMessage.success('已发布') } catch(e){ void e; }
}
async function retireFlow(row:WorkflowDef) {
  try { await ElMessageBox.confirm('确认退役该流程?'); await internalApi.post('/system/workflows/'+row.id+'/retire'); fetchData(); ElMessage.success('已退役') } catch(e){ void e; }
}
function addNode() { nodes.value.push({ id:null, nodeCode:'N'+(nodes.value.length+1), nodeName:'节点'+(nodes.value.length+1), nodeType:'APPROVAL', sortOrder:nodes.value.length+1, assigneeType:'USER', assigneeValue:'', durationHours:48 }); nodeDialogVisible.value=false }
function removeNode(idx:number) { nodes.value.splice(idx,1) }
function onSearch(){ page.value=1; fetchData() }
function onReset(){ searchForm.keyword=''; searchForm.status=''; onSearch() }
const statusType=(s:string)=>s==='PUBLISHED'?'success':s==='DRAFT'?'info':'danger'
const statusLabel=(s:string)=>s==='PUBLISHED'?'已发布':s==='DRAFT'?'草稿':'已退役'
onMounted(fetchData)
</script>

<template>
  <div style="padding:16px">
    <el-card>
      <div class="search-bar">
        <el-select v-model="searchForm.status" placeholder="状态" clearable style="width:120px"><el-option label="草稿" value="DRAFT"/><el-option label="已发布" value="PUBLISHED"/><el-option label="已退役" value="RETIRED"/></el-select>
        <el-button type="primary" @click="onSearch">搜索</el-button>
        <el-button @click="onReset">重置</el-button>
        <el-button v-if="canCreate" type="success" style="margin-left:auto" @click="openCreate">新增流程</el-button>
      </div>
      <el-table :data="list" v-loading="loading" border size="small" style="margin-top:12px" empty-text="暂无流程">
        <el-table-column prop="processCode" label="流程编码" width="150"/>
        <el-table-column prop="processName" label="流程名称" width="160"/>
        <el-table-column prop="businessType" label="业务类型" width="140"/>
        <el-table-column prop="definitionVersion" label="版本" width="70" align="center"/>
        <el-table-column label="状态" width="90"><template #default="{row}"><el-tag :type="statusType(row.status)" size="small">{{statusLabel(row.status)}}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{row}">
            <el-button v-if="canUpdate && row.status==='DRAFT'" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="canPublish && row.status==='DRAFT'" link type="success" size="small" @click="publishFlow(row)">发布</el-button>
            <el-button v-if="canRetire && row.status==='PUBLISHED'" link type="warning" size="small" @click="retireFlow(row)">退役</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" layout="total,prev,pager,next" @current-change="onSearch" style="margin-top:10px;justify-content:flex-end"/>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id?'编辑流程':'新增流程'" width="600px">
      <el-form label-width="80px" size="small">
        <el-form-item label="编码"><el-input v-model="form.processCode" :disabled="!!form.id"/></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.processName"/></el-form-item>
        <el-form-item label="业务类型"><el-input v-model="form.businessType" :disabled="!!form.id"/></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea"/></el-form-item>
        <el-form-item label="节点">
          <div style="width:100%">
            <el-table :data="nodes" size="small" border>
              <el-table-column prop="nodeCode" label="编码" width="70"/>
              <el-table-column prop="nodeName" label="名称" width="100"/>
              <el-table-column prop="assigneeType" label="审批人类型" width="100"/>
              <el-table-column prop="assigneeValue" label="审批人" width="90"/>
              <el-table-column prop="durationHours" label="时限(h)" width="70"/>
              <el-table-column label="操作" width="60"><template #default="{ $index }"><el-button link type="danger" size="small" @click="removeNode($index)">删</el-button></template></el-table-column>
            </el-table>
            <el-button type="primary" size="small" style="margin-top:6px" @click="addNode">添加节点</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="saveFlow">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.search-bar { display:flex; gap:10px; align-items:center; }
</style>
