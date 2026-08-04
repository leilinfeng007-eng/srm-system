<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { internalApi } from '../../../api/internal-api'
import { useAuthStore } from '../../../stores/auth'
import { usePermission } from '../../../composables/usePermission'

interface ApprovalItem { id:number; instanceCode:string; businessType:string; businessSummary:string; status:string; submittedAt:string }
interface ApprovalNode { id:number; nodeCode:string; nodeName:string; status:string; decision:string; actualAssignee:number; decidedAt:string }
interface TaskItem { id:number; sourceType:string; sourceId:string; approvalInstanceId:number|null; title:string; status:string; slaStatus:string; dueAt:string|null }

const auth = useAuthStore()
const pendingCount=ref(0); const overdueCount=ref(0); const unreadCount=ref(0)
const approvals=ref<ApprovalItem[]>([]); const approvalsLoading=ref(false)
const tasks=ref<TaskItem[]>([]); const tasksLoading=ref(false)
const drawerVisible=ref(false); const drawerInstance=ref<ApprovalItem|null>(null)
const drawerNodes=ref<ApprovalNode[]>([]); const drawerLoading=ref(false)
const rejectReason=ref(''); const rejectDialog=ref(false)
const canApprove=usePermission('system:approval:approve');const canReject=usePermission('system:approval:reject');const canWithdraw=usePermission('system:approval:withdraw')

async function loadCounts() {
  try { const r=await internalApi.get<{items:TaskItem[],total:number}>('/tasks/my?page=1&pageSize=1&status=PENDING'); pendingCount.value=r?.total??0 } catch(e){ void e; pendingCount.value=0 }
  try { overdueCount.value=await internalApi.get<number>('/tasks/overdue-count') ?? 0 } catch(e){ void e; overdueCount.value=0 }
  try { unreadCount.value=await internalApi.get<number>('/messages/unread-count') ?? 0 } catch(e){ void e; unreadCount.value=0 }
}
async function loadApprovals() {
  approvalsLoading.value=true
  try { const r=await internalApi.get<{items:ApprovalItem[],total:number}>('/system/approvals?page=1&pageSize=5'); approvals.value=r?.items??[] } catch(e){ void e; approvals.value=[] } finally { approvalsLoading.value=false }
}
async function loadTasks() {
  tasksLoading.value=true
  try { const r=await internalApi.get<{items:TaskItem[],total:number}>('/tasks/my?page=1&pageSize=10&status=PENDING'); tasks.value=r?.items??[] } catch(e){ void e; tasks.value=[] } finally { tasksLoading.value=false }
}
async function openTaskApproval(task:TaskItem) {
  if (!task.approvalInstanceId) { ElMessage.warning('该待办没有关联审批实例'); return }
  try { const found=await internalApi.get<ApprovalItem>('/system/approvals/'+task.approvalInstanceId); if(found)await openDrawer(found) }
  catch(e:unknown){ ElMessage.error((e as Error)?.message||'审批详情加载失败') }
}
async function openDrawer(item:ApprovalItem) {
  drawerInstance.value=item; drawerVisible.value=true; drawerLoading.value=true; drawerNodes.value=[]
  try {
    const r=await internalApi.get<ApprovalNode[]>('/system/approvals/'+item.id+'/nodes'); drawerNodes.value=r??[]
  } catch(e){ void e; drawerNodes.value=[] } finally { drawerLoading.value=false }
}
async function doApprove() {
  if(!drawerInstance.value) return
  try { await internalApi.post('/system/approvals/'+drawerInstance.value.id+'/approve',{decision:'同意'}); ElMessage.success('已同意'); closeAndReload() }
  catch(e:unknown){ ElMessage.error((e as Error)?.message||'操作失败') }
}
function openReject() { rejectReason.value=''; rejectDialog.value=true }
async function doReject() {
  if(!drawerInstance.value) return
  if(!rejectReason.value.trim()) { ElMessage.warning('请填写驳回意见'); return }
  try { await internalApi.post('/system/approvals/'+drawerInstance.value.id+'/reject',{reason:rejectReason.value}); ElMessage.success('已驳回'); rejectDialog.value=false; closeAndReload() }
  catch(e:unknown){ ElMessage.error((e as Error)?.message||'操作失败') }
}
async function doWithdraw(item?: ApprovalItem) {
  if(item) drawerInstance.value=item
  if(!drawerInstance.value) return
  try { await ElMessageBox.confirm('确认撤回该申请?'); await internalApi.post('/system/approvals/'+drawerInstance.value.id+'/withdraw'); ElMessage.success('已撤回'); closeAndReload() } catch(e){ void e; }
}
function closeAndReload() { drawerVisible.value=false; drawerInstance.value=null; loadCounts(); loadApprovals(); loadTasks() }
const statusLabel=(s:string)=>s==='PENDING'?'待审批':s==='APPROVED'?'已通过':s==='REJECTED'?'已驳回':s==='WITHDRAWN'?'已撤回':'已取消'
const statusType=(s:string)=>s==='PENDING'?'warning':s==='APPROVED'?'success':s==='REJECTED'?'danger':'info'
onMounted(()=>{loadCounts();loadApprovals();loadTasks()})
</script>

<template>
  <div style="padding:20px;max-width:1100px;margin:0 auto">
    <h2 style="margin-bottom:4px">工作台</h2>
    <p style="color:#888;margin-bottom:16px">{{ auth.user?.displayName }} · {{ auth.user?.roles?.join(', ') }}</p>

    <div style="display:flex;gap:14px;margin-bottom:20px">
      <el-card style="flex:1" shadow="never"><div style="text-align:center"><div style="font-size:30px;font-weight:700;color:#409EFF">{{pendingCount}}</div><div style="color:#888;font-size:13px">我的待办</div></div></el-card>
      <el-card style="flex:1" shadow="never"><div style="text-align:center"><div style="font-size:30px;font-weight:700;color:#E6A23C">{{overdueCount}}</div><div style="color:#888;font-size:13px">超期任务</div></div></el-card>
      <el-card style="flex:1" shadow="never"><div style="text-align:center"><div style="font-size:30px;font-weight:700;color:#F56C6C">{{unreadCount}}</div><div style="color:#888;font-size:13px">未读消息</div></div></el-card>
    </div>

    <el-tabs>
      <el-tab-pane label="我发起的审批">
        <el-table :data="approvals" v-loading="approvalsLoading" border size="small" empty-text="暂无审批">
          <el-table-column prop="instanceCode" label="实例编码" width="150"/>
          <el-table-column prop="businessSummary" label="摘要" min-width="200"/>
          <el-table-column prop="businessType" label="业务类型" width="140"/>
          <el-table-column label="状态" width="90"><template #default="{row}"><el-tag :type="statusType(row.status)" size="small">{{statusLabel(row.status)}}</el-tag></template></el-table-column>
          <el-table-column prop="submittedAt" label="提交时间" width="160"/>
          <el-table-column label="操作" width="150"><template #default="{row}">
            <el-button link type="primary" size="small" @click="openDrawer(row)">详情</el-button>
            <el-button v-if="canWithdraw&&row.status==='PENDING'" link type="warning" size="small" @click="doWithdraw(row)">撤回</el-button>
          </template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="我的待办">
        <el-table :data="tasks" v-loading="tasksLoading" border size="small" empty-text="暂无待办">
          <el-table-column prop="title" label="标题" min-width="220"/>
          <el-table-column prop="slaStatus" label="SLA" width="80"><template #default="{row}"><el-tag :type="row.slaStatus==='OVERDUE'?'danger':'success'" size="small">{{row.slaStatus==='OVERDUE'?'超期':'正常'}}</el-tag></template></el-table-column>
          <el-table-column prop="dueAt" label="到期时间" width="160"/>
          <el-table-column label="操作" width="100"><template #default="{row}"><el-button link type="primary" size="small" @click="openTaskApproval(row)">处理</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="drawerVisible" title="审批详情" size="480px">
      <template v-if="drawerInstance">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="实例">{{drawerInstance.instanceCode}}</el-descriptions-item>
          <el-descriptions-item label="摘要">{{drawerInstance.businessSummary}}</el-descriptions-item>
          <el-descriptions-item label="状态">{{statusLabel(drawerInstance.status)}}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin:14px 0 8px">审批节点</h4>
        <el-table :data="drawerNodes" v-loading="drawerLoading" size="small" border>
          <el-table-column prop="nodeName" label="节点" width="90"/>
          <el-table-column prop="status" label="状态" width="80"/>
          <el-table-column prop="decision" label="意见" min-width="100"/>
          <el-table-column prop="decidedAt" label="时间" width="150"/>
        </el-table>
        <div v-if="drawerInstance.status==='PENDING'" style="margin-top:16px;display:flex;gap:8px">
          <el-button v-if="canApprove" type="success" @click="doApprove">同意</el-button>
          <el-button v-if="canReject" type="danger" @click="openReject">驳回</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="rejectDialog" title="驳回意见" width="400px">
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请填写驳回原因"/>
      <template #footer><el-button @click="rejectDialog=false">取消</el-button><el-button type="danger" @click="doReject">确认驳回</el-button></template>
    </el-dialog>
  </div>
</template>
