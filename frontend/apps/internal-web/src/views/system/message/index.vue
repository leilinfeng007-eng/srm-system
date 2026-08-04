<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { internalApi } from '../../../api/internal-api'
import { usePermission } from '../../../composables/usePermission'

interface MessageItem { id:number; title:string; content:string; messageType:string; status:string; sourceType:string; sourceId:string; readAt:string|null; createdAt:string }

const loading=ref(false); const data=ref<MessageItem[]>([]); const total=ref(0); const unread=ref(0)
const page=ref(1); const pageSize=ref(10)
const searchForm=reactive({ status:'' })
const canManage=usePermission('system:message:manage')

async function fetchData() {
  loading.value=true
  try {
    const qs=new URLSearchParams({page:String(page.value),pageSize:String(pageSize.value)})
    if(searchForm.status) qs.set('status',searchForm.status)
    const r=await internalApi.get<{items:MessageItem[],total:number}>('/messages/my?'+qs.toString())
    data.value=r?.items??[]; total.value=r?.total??0
  } catch { data.value=[]; total.value=0 } finally { loading.value=false }
}
async function loadUnread() {
  try { const r=await internalApi.get<number>('/messages/unread-count'); unread.value=r??0 } catch { unread.value=0 }
}
async function markRead(row:MessageItem) {
  try { await internalApi.post('/messages/'+row.id+'/read'); fetchData(); loadUnread(); ElMessage.success('已读') } catch(e){ void e; }
}
async function markAllRead() {
  try { await internalApi.post('/messages/read-all'); fetchData(); loadUnread(); ElMessage.success('全部已读') } catch(e){ void e; }
}
function onSearch(){ page.value=1; fetchData() }
const statusType=(s:string)=>s==='UNREAD'?'warning':s==='READ'?'info':'info'
const statusLabel=(s:string)=>s==='UNREAD'?'未读':s==='READ'?'已读':'已归档'
onMounted(()=>{fetchData();loadUnread()})
</script>

<template>
  <div style="padding:16px">
    <el-card>
      <div class="head-bar">
        <span style="font-weight:600">未读消息：<el-tag type="danger" size="small">{{unread}}</el-tag></span>
        <span style="margin-left:auto;display:flex;gap:8px">
          <el-select v-model="searchForm.status" placeholder="状态" clearable style="width:110px" @change="onSearch"><el-option label="未读" value="UNREAD"/><el-option label="已读" value="READ"/></el-select>
          <el-button v-if="canManage" @click="markAllRead">全部已读</el-button>
        </span>
      </div>
      <el-table :data="data" v-loading="loading" border size="small" style="margin-top:12px" empty-text="暂无消息">
        <el-table-column prop="title" label="标题" width="200"/>
        <el-table-column prop="content" label="内容" min-width="240" show-overflow-tooltip/>
        <el-table-column label="类型" width="120"><template #default="{row}">{{row.sourceType}}</template></el-table-column>
        <el-table-column label="状态" width="80"><template #default="{row}"><el-tag :type="statusType(row.status)" size="small">{{statusLabel(row.status)}}</el-tag></template></el-table-column>
        <el-table-column prop="createdAt" label="时间" width="160"/>
        <el-table-column label="操作" width="100"><template #default="{row}"><el-button v-if="canManage&&row.status==='UNREAD'" link type="primary" size="small" @click="markRead(row)">标为已读</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" layout="total,prev,pager,next" @current-change="onSearch" style="margin-top:10px;justify-content:flex-end"/>
    </el-card>
  </div>
</template>

<style scoped>
.head-bar { display:flex; align-items:center; gap:10px; }
</style>
