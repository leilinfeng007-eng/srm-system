<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { internalApi } from '../../../api/internal-api'

interface AuditLog { id:number; userId:number|null; operatorName:string; actionCode:string; targetType:string; targetId:string; resultCode:string; reason:string|null; fieldChanges:string|null; beforeHash:string|null; afterHash:string|null; traceId:string; occurredAt:string }

const loading=ref(false); const list=ref<AuditLog[]>([]); const total=ref(0)
const page=ref(1); const pageSize=ref(10)
const searchForm=reactive({ operatorName:'', actionCode:'', targetType:'', targetId:'', resultCode:'', traceId:'', from:'', to:'' })
const detailVisible=ref(false); const detail=ref<AuditLog|null>(null)

async function fetchData() {
  loading.value=true
  try {
    const qs=new URLSearchParams({page:String(page.value),pageSize:String(pageSize.value)})
    if(searchForm.operatorName) qs.set('operatorName',searchForm.operatorName)
    if(searchForm.actionCode) qs.set('actionCode',searchForm.actionCode)
    if(searchForm.targetType) qs.set('targetType',searchForm.targetType)
    if(searchForm.targetId) qs.set('targetId',searchForm.targetId)
    if(searchForm.resultCode) qs.set('resultCode',searchForm.resultCode)
    if(searchForm.traceId) qs.set('traceId',searchForm.traceId)
    if(searchForm.from) qs.set('from',new Date(searchForm.from).toISOString())
    if(searchForm.to) qs.set('to',new Date(searchForm.to).toISOString())
    const r=await internalApi.get<{items:AuditLog[],total:number}>('/system/audit-logs?'+qs.toString())
    list.value=r?.items??[]; total.value=r?.total??0
  } catch(e){ void e; list.value=[]; total.value=0 } finally { loading.value=false }
}
async function openDetail(row:AuditLog) {
  try { detail.value=await internalApi.get<AuditLog>('/system/audit-logs/'+row.id) } catch(e){ void e; detail.value=row }
  detailVisible.value=true
}
function parseChanges(raw:string|null):{before:string;after:string}|null {
  if(!raw) return null
  try { const o=JSON.parse(raw); return {before:String(o.before??''),after:String(o.after??'')} } catch { return null }
}
function changesOf(row:AuditLog|null):{before:string;after:string}|null { return row?parseChanges(row.fieldChanges):null }
function onSearch(){ page.value=1; fetchData() }
function onReset(){ Object.assign(searchForm,{operatorName:'',actionCode:'',targetType:'',targetId:'',resultCode:'',traceId:'',from:'',to:''}); onSearch() }
const resultType=(s:string)=>s==='SUCCESS'?'success':s==='FAILED'?'danger':'warning'
onMounted(fetchData)
</script>

<template>
  <div style="padding:16px">
    <el-card>
      <div class="bar">
        <el-input v-model="searchForm.operatorName" placeholder="操作者" clearable style="width:130px" @keyup.enter="onSearch"/>
        <el-input v-model="searchForm.actionCode" placeholder="动作编码" clearable style="width:160px" @keyup.enter="onSearch"/>
        <el-input v-model="searchForm.targetType" placeholder="对象类型" clearable style="width:130px" @keyup.enter="onSearch"/>
        <el-input v-model="searchForm.targetId" placeholder="对象ID" clearable style="width:110px" @keyup.enter="onSearch"/>
        <el-select v-model="searchForm.resultCode" placeholder="结果" clearable style="width:110px" @change="onSearch"><el-option label="成功" value="SUCCESS"/><el-option label="失败" value="FAILED"/></el-select>
        <el-input v-model="searchForm.traceId" placeholder="traceId" clearable style="width:190px" @keyup.enter="onSearch"/>
        <el-date-picker v-model="searchForm.from" type="datetime" placeholder="开始时间" style="width:170px" @change="onSearch"/>
        <el-date-picker v-model="searchForm.to" type="datetime" placeholder="结束时间" style="width:170px" @change="onSearch"/>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </div>
      <el-table :data="list" v-loading="loading" border size="small" style="margin-top:12px" empty-text="暂无审计记录">
        <el-table-column prop="occurredAt" label="时间" width="160"/>
        <el-table-column prop="operatorName" label="操作者" width="110"/>
        <el-table-column prop="actionCode" label="动作" width="200"/>
        <el-table-column prop="targetType" label="对象类型" width="120"/>
        <el-table-column prop="targetId" label="对象ID" width="90"/>
        <el-table-column label="结果" width="80"><template #default="{row}"><el-tag :type="resultType(row.resultCode)" size="small">{{row.resultCode}}</el-tag></template></el-table-column>
        <el-table-column prop="reason" label="原因" min-width="130" show-overflow-tooltip/>
        <el-table-column prop="traceId" label="traceId" min-width="180" show-overflow-tooltip/>
        <el-table-column label="操作" width="70"><template #default="{row}"><el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" layout="total,prev,pager,next" @current-change="onSearch" style="margin-top:10px;justify-content:flex-end"/>
    </el-card>

    <el-dialog v-model="detailVisible" :title="'审计详情 #'+(detail?.id??'')" width="720px">
      <el-descriptions v-if="detail" :column="2" border size="small">
        <el-descriptions-item label="时间">{{detail.occurredAt}}</el-descriptions-item>
        <el-descriptions-item label="操作者">{{detail.operatorName||'-'}} (ID: {{detail.userId??'-'}})</el-descriptions-item>
        <el-descriptions-item label="动作">{{detail.actionCode}}</el-descriptions-item>
        <el-descriptions-item label="结果"><el-tag :type="resultType(detail.resultCode)" size="small">{{detail.resultCode}}</el-tag></el-descriptions-item>
        <el-descriptions-item label="对象类型">{{detail.targetType||'-'}}</el-descriptions-item>
        <el-descriptions-item label="对象ID">{{detail.targetId||'-'}}</el-descriptions-item>
        <el-descriptions-item label="原因" :span="2">{{detail.reason||'-'}}</el-descriptions-item>
        <el-descriptions-item label="traceId" :span="2">{{detail.traceId}}</el-descriptions-item>
        <el-descriptions-item label="变更前哈希" :span="2" ><span class="hash">{{detail.beforeHash||'-'}}</span></el-descriptions-item>
        <el-descriptions-item label="变更后哈希" :span="2"><span class="hash">{{detail.afterHash||'-'}}</span></el-descriptions-item>
        <el-descriptions-item label="变更摘要" :span="2">
          <template v-if="changesOf(detail)">
            <div>变更前：{{changesOf(detail)!.before||'-'}}</div>
            <div>变更后：{{changesOf(detail)!.after||'-'}}</div>
          </template>
          <span v-else>{{detail.fieldChanges||'-'}}</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer><el-button @click="detailVisible=false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.bar { display:flex; gap:8px; align-items:center; flex-wrap:wrap; }
.hash { font-family:monospace; font-size:12px; word-break:break-all; }
</style>
