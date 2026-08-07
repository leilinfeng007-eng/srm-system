<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { internalApi } from '../../../api/internal-api'
import { usePermission } from '../../../composables/usePermission'

interface WorkflowDef { id:number; processCode:string; processName:string; businessType:string; definitionVersion:number; status:string; description:string; createdAt:string; updatedAt:string }
interface WorkflowNode { id:number|null; nodeCode:string; nodeName:string; nodeType:string; sortOrder:number; assigneeType:string; assigneeValue:string; durationHours:number|null }
interface UserOption { id:number; username:string; displayName:string; status:string }
interface RoleOption { id:number; roleCode:string; roleName:string; status:string }
interface PositionOption { id:number; positionCode:string; positionName:string; status:string }

const loading=ref(false); const list=ref<WorkflowDef[]>([]); const total=ref(0)
const page=ref(1); const pageSize=ref(10)
const searchForm=reactive({ keyword:'', status:'' })
const dialogVisible=ref(false); const nodeDialogVisible=ref(false); const detailVisible=ref(false)
const form=reactive({ id:null as number|null, processCode:'', processName:'', businessType:'SYSTEM_PARAMETER', description:'' })
const nodes=ref<WorkflowNode[]>([])
const nodeForm=reactive({ index:-1, nodeCode:'', nodeName:'', nodeType:'APPROVAL', assigneeType:'USER', assigneeValue:'', durationHours:48 })
const users=ref<UserOption[]>([]); const roles=ref<RoleOption[]>([]); const positions=ref<PositionOption[]>([])
const detailRow=ref<WorkflowDef|null>(null); const detailNodes=ref<WorkflowNode[]>([])
const canCreate=usePermission('system:workflow:create');const canUpdate=usePermission('system:workflow:update')
const canPublish=usePermission('system:workflow:publish');const canRetire=usePermission('system:workflow:retire')

async function fetchData() {
  loading.value=true
  try {
    const qs=new URLSearchParams({page:String(page.value),pageSize:String(pageSize.value)})
    if(searchForm.status) qs.set('status',searchForm.status)
    if(searchForm.keyword) qs.set('keyword',searchForm.keyword)
    const r=await internalApi.get<{items:WorkflowDef[],total:number}>('/system/workflows?'+qs.toString())
    list.value=r?.items??[]; total.value=r?.total??0
  } catch(e){ void e; list.value=[]; total.value=0 } finally { loading.value=false }
}
async function loadAssignees() {
  try { users.value=(await internalApi.get<{items:UserOption[],total:number}>('/system/users?page=1&pageSize=500'))?.items??[] } catch(e){ void e; users.value=[] }
  try { roles.value=(await internalApi.get<{items:RoleOption[],total:number}>('/system/roles?page=1&pageSize=500'))?.items??[] } catch(e){ void e; roles.value=[] }
  try { positions.value=(await internalApi.get<{items:PositionOption[],total:number}>('/system/positions?page=1&pageSize=500'))?.items??[] } catch(e){ void e; positions.value=[] }
}
function openCreate() { Object.assign(form,{id:null,processCode:'',processName:'',businessType:'SYSTEM_PARAMETER',description:''}); nodes.value=[]; dialogVisible.value=true; loadAssignees() }
async function openEdit(row:WorkflowDef) {
  Object.assign(form,{id:row.id,processCode:row.processCode,processName:row.processName,businessType:row.businessType,description:row.description})
  dialogVisible.value=true; loadAssignees()
  try { const r=await internalApi.get<WorkflowNode[]>('/system/workflows/'+row.id+'/nodes'); nodes.value=r??[] } catch(e){ void e; nodes.value=[] }
}
async function openDetail(row:WorkflowDef) {
  detailRow.value=row
  try { const r=await internalApi.get<WorkflowNode[]>('/system/workflows/'+row.id+'/nodes'); detailNodes.value=r??[] } catch(e){ void e; detailNodes.value=[] }
  detailVisible.value=true
}
async function saveFlow() {
  try {
    if(!form.id){ await internalApi.post('/system/workflows',{...form,nodes:nodes.value}) }
    else { await internalApi.request('/system/workflows/'+form.id,{method:'PUT',body:{processName:form.processName,description:form.description,nodes:nodes.value}}) }
    dialogVisible.value=false; fetchData(); ElMessage.success('保存成功')
  } catch(e:unknown){ void e; ElMessage.error((e as Error)?.message||'保存失败') }
}
async function publishFlow(row:WorkflowDef) {
  try { await ElMessageBox.confirm('确认发布该流程?发布后版本将冻结'); await internalApi.post('/system/workflows/'+row.id+'/publish'); fetchData(); ElMessage.success('已发布') } catch(e){ void e; }
}
async function retireFlow(row:WorkflowDef) {
  try { await ElMessageBox.confirm('确认退役该流程?'); await internalApi.post('/system/workflows/'+row.id+'/retire'); fetchData(); ElMessage.success('已退役') } catch(e){ void e; }
}
function addNode() { Object.assign(nodeForm,{index:-1,nodeCode:'N'+(nodes.value.length+1),nodeName:'节点'+(nodes.value.length+1),nodeType:'APPROVAL',assigneeType:'USER',assigneeValue:'',durationHours:48}); nodeDialogVisible.value=true }
function editNode(index:number) { Object.assign(nodeForm,{...nodes.value[index],index}); nodeDialogVisible.value=true }
function removeNode(idx:number) { nodes.value.splice(idx,1); renumber() }
function moveNode(idx:number,delta:number) { const target=idx+delta; if(target<0||target>=nodes.value.length)return; const tmp=nodes.value[idx]; nodes.value[idx]=nodes.value[target]; nodes.value[target]=tmp; renumber() }
function renumber(){ nodes.value.forEach((n,i)=>{ n.sortOrder=i+1 }) }
function saveNode() {
  if(!nodeForm.nodeCode.trim()||!nodeForm.nodeName.trim()){ ElMessage.warning('节点编码和名称必填'); return }
  if(!nodeForm.assigneeValue){ ElMessage.warning('请选择审批对象'); return }
  const duplicated=nodes.value.some((n,i)=>i!==nodeForm.index&&n.nodeCode===nodeForm.nodeCode.trim())
  if(duplicated){ ElMessage.warning('节点编码重复'); return }
  if(nodeForm.index<0){ nodes.value.push({id:null,nodeCode:nodeForm.nodeCode,nodeName:nodeForm.nodeName,nodeType:nodeForm.nodeType,sortOrder:nodes.value.length+1,assigneeType:nodeForm.assigneeType,assigneeValue:nodeForm.assigneeValue,durationHours:nodeForm.durationHours}) }
  else { Object.assign(nodes.value[nodeForm.index],{nodeCode:nodeForm.nodeCode,nodeName:nodeForm.nodeName,nodeType:nodeForm.nodeType,assigneeType:nodeForm.assigneeType,assigneeValue:nodeForm.assigneeValue,durationHours:nodeForm.durationHours}) }
  renumber(); nodeDialogVisible.value=false
}
function assigneeLabel(node:WorkflowNode):string {
  if(node.assigneeType==='USER'){ const u=users.value.find(x=>String(x.id)===String(node.assigneeValue)); return u?(u.displayName||u.username):node.assigneeValue }
  if(node.assigneeType==='ROLE'){ const r=roles.value.find(x=>x.roleCode===node.assigneeValue); return r?(r.roleName||r.roleCode):node.assigneeValue }
  const p=positions.value.find(x=>String(x.id)===String(node.assigneeValue)); return p?(p.positionName||p.positionCode):node.assigneeValue
}
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
        <el-input v-model="searchForm.keyword" placeholder="流程编码/名称" clearable style="width:200px" @keyup.enter="onSearch"/>
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
        <el-table-column label="操作" width="270">
          <template #default="{row}">
            <el-button link type="primary" size="small" @click="openDetail(row)">查看</el-button>
            <el-button v-if="canUpdate && row.status==='DRAFT'" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="canPublish && row.status==='DRAFT'" link type="success" size="small" @click="publishFlow(row)">发布</el-button>
            <el-button v-if="canRetire && row.status==='PUBLISHED'" link type="warning" size="small" @click="retireFlow(row)">退役</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" layout="total,prev,pager,next" @current-change="onSearch" style="margin-top:10px;justify-content:flex-end"/>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id?'编辑流程':'新增流程'" width="760px">
      <el-form label-width="80px" size="small">
        <el-form-item label="编码"><el-input v-model="form.processCode" :disabled="!!form.id"/></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.processName"/></el-form-item>
        <el-form-item label="业务类型"><el-input v-model="form.businessType" :disabled="!!form.id" placeholder="SYSTEM_PARAMETER"/></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea"/></el-form-item>
        <el-form-item label="节点">
          <div style="width:100%">
            <el-table :data="nodes" size="small" border>
              <el-table-column prop="sortOrder" label="排序" width="60" align="center"/>
              <el-table-column prop="nodeCode" label="编码" width="80"/>
              <el-table-column prop="nodeName" label="名称" width="110"/>
              <el-table-column label="审批人类型" width="100"><template #default="{row}">{{row.assigneeType==='USER'?'用户':row.assigneeType==='ROLE'?'角色':'岗位'}}</template></el-table-column>
              <el-table-column label="审批对象" min-width="120"><template #default="{row}">{{assigneeLabel(row)}}</template></el-table-column>
              <el-table-column prop="durationHours" label="时限(h)" width="80"/>
              <el-table-column label="操作" width="150"><template #default="{ $index }">
                <el-button link type="primary" size="small" @click="editNode($index)">编辑</el-button>
                <el-button link type="primary" size="small" @click="moveNode($index,-1)">上移</el-button>
                <el-button link type="primary" size="small" @click="moveNode($index,1)">下移</el-button>
                <el-button link type="danger" size="small" @click="removeNode($index)">删</el-button>
              </template></el-table-column>
            </el-table>
            <el-button type="primary" size="small" style="margin-top:6px" @click="addNode">添加节点</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="saveFlow">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="nodeDialogVisible" :title="nodeForm.index<0?'新增审批节点':'编辑审批节点'" width="520px">
      <el-form label-width="100px" size="small">
        <el-form-item label="节点编码"><el-input v-model="nodeForm.nodeCode" placeholder="如 N1"/></el-form-item>
        <el-form-item label="节点名称"><el-input v-model="nodeForm.nodeName" placeholder="如 部门经理审批"/></el-form-item>
        <el-form-item label="节点类型"><el-select v-model="nodeForm.nodeType" style="width:100%"><el-option label="审批节点" value="APPROVAL"/></el-select></el-form-item>
        <el-form-item label="审批人类型"><el-select v-model="nodeForm.assigneeType" style="width:100%"><el-option label="指定用户" value="USER"/><el-option label="角色" value="ROLE"/><el-option label="岗位" value="POSITION"/></el-select></el-form-item>
        <el-form-item label="审批对象">
          <el-select v-if="nodeForm.assigneeType==='USER'" v-model="nodeForm.assigneeValue" filterable style="width:100%"><el-option v-for="u in users" :key="u.id" :label="(u.displayName||u.username)+' ('+u.username+')'" :value="String(u.id)"/></el-select>
          <el-select v-else-if="nodeForm.assigneeType==='ROLE'" v-model="nodeForm.assigneeValue" filterable style="width:100%"><el-option v-for="r in roles" :key="r.roleCode" :label="(r.roleName||r.roleCode)+' ('+r.roleCode+')'" :value="r.roleCode"/></el-select>
          <el-select v-else v-model="nodeForm.assigneeValue" filterable style="width:100%"><el-option v-for="p in positions" :key="p.id" :label="(p.positionName||p.positionCode)+' ('+p.positionCode+')'" :value="String(p.id)"/></el-select>
        </el-form-item>
        <el-form-item label="处理时限(h)"><el-input-number v-model="nodeForm.durationHours" :min="0" :max="8760"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="nodeDialogVisible=false">取消</el-button><el-button type="primary" @click="saveNode">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="detailVisible" :title="'流程详情：'+(detailRow?.processName||'')" width="680px">
      <el-descriptions v-if="detailRow" :column="2" border size="small">
        <el-descriptions-item label="流程编码">{{detailRow.processCode}}</el-descriptions-item>
        <el-descriptions-item label="版本">{{detailRow.definitionVersion}}</el-descriptions-item>
        <el-descriptions-item label="业务类型">{{detailRow.businessType}}</el-descriptions-item>
        <el-descriptions-item label="状态">{{statusLabel(detailRow.status)}}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{detailRow.description||'-'}}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{detailRow.createdAt}}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{detailRow.updatedAt}}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detailNodes" border size="small" style="margin-top:10px" empty-text="暂无节点">
        <el-table-column prop="sortOrder" label="排序" width="60" align="center"/>
        <el-table-column prop="nodeCode" label="编码" width="90"/>
        <el-table-column prop="nodeName" label="名称" width="130"/>
        <el-table-column label="审批人类型" width="100"><template #default="{row}">{{row.assigneeType==='USER'?'用户':row.assigneeType==='ROLE'?'角色':'岗位'}}</template></el-table-column>
        <el-table-column label="审批对象" min-width="120"><template #default="{row}">{{assigneeLabel(row)}}</template></el-table-column>
        <el-table-column prop="durationHours" label="时限(h)" width="80"/>
      </el-table>
      <template #footer><el-button @click="detailVisible=false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.search-bar { display:flex; gap:10px; align-items:center; }
</style>
