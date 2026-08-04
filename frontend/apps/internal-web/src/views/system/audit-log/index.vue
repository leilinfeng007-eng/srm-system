<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { internalApi } from '../../../api/internal-api'

interface AuditLog { id:number; userId:number; operatorName:string; actionCode:string; targetType:string; targetId:string; resultCode:string; reason:string; beforeHash:string; afterHash:string; occurredAt:string }

const loading=ref(false); const list=ref<AuditLog[]>([]); const total=ref(0)
const page=ref(1); const pageSize=ref(10)
const searchForm=reactive({ actionCode:'', targetType:'' })

async function fetchData() {
  loading.value=true
  try {
    const qs=new URLSearchParams({page:String(page.value),pageSize:String(pageSize.value)})
    if(searchForm.actionCode) qs.set('actionCode',searchForm.actionCode)
    if(searchForm.targetType) qs.set('targetType',searchForm.targetType)
    const r=await internalApi.get<{items:AuditLog[],total:number}>('/system/audit-logs?'+qs.toString())
    list.value=r?.items??[]; total.value=r?.total??0
  } catch(e){ void e; list.value=[]; total.value=0 } finally { loading.value=false }
}
function onSearch(){ page.value=1; fetchData() }
function onReset(){ searchForm.actionCode=''; searchForm.targetType=''; onSearch() }
const resultType=(s:string)=>s==='SUCCESS'?'success':s==='FAILED'?'danger':'warning'
onMounted(fetchData)
</script>

<template>
  <div style="padding:16px">
    <el-card>
      <div class="bar">
        <el-input v-model="searchForm.actionCode" placeholder="动作编码" clearable style="width:180px" @keyup.enter="onSearch"/>
        <el-input v-model="searchForm.targetType" placeholder="对象类型" clearable style="width:160px" @keyup.enter="onSearch"/>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </div>
      <el-table :data="list" v-loading="loading" border size="small" style="margin-top:12px" empty-text="暂无审计记录">
        <el-table-column prop="occurredAt" label="时间" width="160"/>
        <el-table-column prop="operatorName" label="操作者" width="100"/>
        <el-table-column prop="actionCode" label="动作" width="180"/>
        <el-table-column prop="targetType" label="对象类型" width="110"/>
        <el-table-column prop="targetId" label="对象ID" width="80"/>
        <el-table-column label="结果" width="80"><template #default="{row}"><el-tag :type="resultType(row.resultCode)" size="small">{{row.resultCode}}</el-tag></template></el-table-column>
        <el-table-column prop="reason" label="原因" min-width="140"/>
        <el-table-column prop="beforeHash" label="变更前哈希" width="110"/>
        <el-table-column prop="afterHash" label="变更后哈希" width="110"/>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" layout="total,prev,pager,next" @current-change="onSearch" style="margin-top:10px;justify-content:flex-end"/>
    </el-card>
  </div>
</template>

<style scoped>
.bar { display:flex; gap:10px; align-items:center; }
</style>
