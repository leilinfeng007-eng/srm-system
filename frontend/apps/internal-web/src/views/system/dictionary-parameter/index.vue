<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { internalApi } from '../../../api/internal-api'
import { usePermission } from '../../../composables/usePermission'

interface DictItem { id:number; dictCode:string; dictName:string; status:string; description:string }
interface ParamItem { id:number; paramCode:string; paramName:string; paramType:string; defaultValue:string; validationRule:string|null; approvalRequired:boolean; description:string }
interface ParamVersion { id:number; version:number; paramValue:string; status:string; effectiveFrom:string|null; publishedAt:string|null; createdAt:string }
interface DictionaryItem { id:number; dictId:number; itemCode:string; itemName:string; sortOrder:number; status:string }
interface NumberRule { id:number; ruleCode:string; ruleName:string; objectType:string; prefix:string; dateFormat:string; serialLength:number; resetCycle:string; organizationDimension:boolean; status:string }

const activeTab=ref<'dict'|'param'|'number'>('dict')
const dictList=ref<DictItem[]>([]); const paramList=ref<ParamItem[]>([])
const loading=ref(false)
const dictFilter=reactive({ keyword:'', status:'' })
const dictDialog=ref(false); const dictForm=reactive({id:null as number|null,dictCode:'',dictName:'',description:''})
const paramDialog=ref(false); const paramForm=reactive({id:null as number|null,paramCode:'',paramName:'',paramType:'STRING',defaultValue:'',validationRule:'',approvalRequired:false,description:''})
const versionDialog=ref(false); const versions=ref<ParamVersion[]>([]); const newValue=ref(''); const currentParamId=ref<number|null>(null); const currentParamName=ref('')
const itemDialog=ref(false);const itemFormDialog=ref(false);const currentDictionary=ref<DictItem|null>(null);const items=ref<DictionaryItem[]>([])
const itemForm=reactive({id:null as number|null,itemCode:'',itemName:'',sortOrder:0})
const numberList=ref<NumberRule[]>([]);const numberDialog=ref(false);const numberForm=reactive({id:null as number|null,ruleCode:'',ruleName:'',objectType:'',prefix:'',dateFormat:'yyyyMMdd',serialLength:5,resetCycle:'DAY',organizationDimension:false})
const preview=ref(''); const previewLoading=ref(false); const previewRuleCode=ref('')
const canCreateDict=usePermission('system:dictionary:create');const canUpdateDict=usePermission('system:dictionary:update')
const canEnableDict=usePermission('system:dictionary:enable');const canDisableDict=usePermission('system:dictionary:disable')
const canCreateParam=usePermission('system:parameter:create');const canUpdateParam=usePermission('system:parameter:update');const canSubmitParam=usePermission('system:parameter:submit')
const canCreateNumber=usePermission('system:number-rule:create');const canUpdateNumber=usePermission('system:number-rule:update');const canEnableNumber=usePermission('system:number-rule:enable');const canDisableNumber=usePermission('system:number-rule:disable')

async function loadDicts() { loading.value=true; try{ const q=new URLSearchParams(); if(dictFilter.keyword)q.set('keyword',dictFilter.keyword); if(dictFilter.status)q.set('status',dictFilter.status); const r=await internalApi.get<DictItem[]>('/system/dictionaries?'+q.toString()); dictList.value=r??[] } catch(e){ void e; dictList.value=[] } finally { loading.value=false } }
async function loadParams() { loading.value=true; try{ const r=await internalApi.get<ParamItem[]>('/system/parameters'); paramList.value=r??[] } catch(e){ void e; paramList.value=[] } finally { loading.value=false } }
async function loadNumbers(){loading.value=true;try{numberList.value=await internalApi.get<NumberRule[]>('/system/number-rules')??[]}catch{numberList.value=[]}finally{loading.value=false}}
function onTab(t:string){ activeTab.value=t as 'dict'|'param'|'number'; if(t==='dict')loadDicts();else if(t==='param')loadParams();else loadNumbers() }
function openDictCreate(){Object.assign(dictForm,{id:null,dictCode:'',dictName:'',description:''});dictDialog.value=true}
function openDictEdit(row:DictItem){Object.assign(dictForm,{id:row.id,dictCode:row.dictCode,dictName:row.dictName,description:row.description});dictDialog.value=true}
function openParamCreate(){Object.assign(paramForm,{id:null,paramCode:'',paramName:'',paramType:'STRING',defaultValue:'',validationRule:'',approvalRequired:false,description:''});paramDialog.value=true}
function openParamEdit(row:ParamItem){Object.assign(paramForm,{id:row.id,paramCode:row.paramCode,paramName:row.paramName,paramType:row.paramType,defaultValue:row.defaultValue,validationRule:row.validationRule,approvalRequired:row.approvalRequired,description:row.description});paramDialog.value=true}
async function openItems(row:DictItem){currentDictionary.value=row;items.value=await internalApi.get<DictionaryItem[]>('/system/dictionaries/'+row.id+'/items')??[];itemDialog.value=true}
function openItemCreate(){Object.assign(itemForm,{id:null,itemCode:'',itemName:'',sortOrder:0});itemFormDialog.value=true}
function openItemEdit(row:DictionaryItem){Object.assign(itemForm,row);itemFormDialog.value=true}
async function saveItem(){if(!currentDictionary.value)return;try{const base='/system/dictionaries/'+currentDictionary.value.id+'/items';if(!itemForm.id)await internalApi.post(base,itemForm);else await internalApi.request(base+'/'+itemForm.id,{method:'PUT',body:itemForm});itemFormDialog.value=false;await openItems(currentDictionary.value);ElMessage.success('字典项保存成功')}catch(e:unknown){ElMessage.error((e as Error)?.message||'保存失败')}}
async function toggleItem(row:DictionaryItem,action:'enable'|'disable'){if(!currentDictionary.value)return;try{await internalApi.post('/system/dictionaries/'+currentDictionary.value.id+'/items/'+row.id+'/'+action);await openItems(currentDictionary.value);ElMessage.success('操作成功')}catch(e:unknown){ElMessage.error((e as Error)?.message||'操作失败')}}
function openNumberCreate(){Object.assign(numberForm,{id:null,ruleCode:'',ruleName:'',objectType:'',prefix:'',dateFormat:'yyyyMMdd',serialLength:5,resetCycle:'DAY',organizationDimension:false});numberDialog.value=true}
function openNumberEdit(row:NumberRule){Object.assign(numberForm,row);numberDialog.value=true}
async function saveNumber(){try{if(!numberForm.id)await internalApi.post('/system/number-rules',numberForm);else await internalApi.request('/system/number-rules/'+numberForm.id,{method:'PUT',body:numberForm});numberDialog.value=false;loadNumbers();ElMessage.success('编号规则保存成功')}catch(e:unknown){ElMessage.error((e as Error)?.message||'保存失败')}}
async function toggleNumber(row:NumberRule,action:'enable'|'disable'){try{await internalApi.post('/system/number-rules/'+row.id+'/'+action);loadNumbers();ElMessage.success('操作成功')}catch(e:unknown){ElMessage.error((e as Error)?.message||'操作失败')}}
async function previewNumber(row:NumberRule){previewLoading.value=true;try{preview.value=await internalApi.post<string>('/system/number-rules/preview',{ruleCode:row.ruleCode,orgId:null});previewRuleCode.value=row.ruleCode}catch(e:unknown){ElMessage.error((e as Error)?.message||'试生成失败');preview.value=''}finally{previewLoading.value=false}}

async function saveDict(){ try{ if(!dictForm.id){ await internalApi.post('/system/dictionaries',dictForm) } else { await internalApi.request('/system/dictionaries/'+dictForm.id,{method:'PUT',body:dictForm}) } dictDialog.value=false; loadDicts(); ElMessage.success('保存成功') } catch(e:unknown){ void e; ElMessage.error((e as Error)?.message||'保存失败') } }
async function toggleDict(row:DictItem,action:string){ try{ await internalApi.post('/system/dictionaries/'+row.id+'/'+action); loadDicts(); ElMessage.success('操作成功') } catch(e:unknown){ void e; ElMessage.error((e as Error)?.message||'操作失败') } }

async function saveParam(){ try{ if(!paramForm.id){ await internalApi.post('/system/parameters',paramForm) } else { await internalApi.request('/system/parameters/'+paramForm.id,{method:'PUT',body:paramForm}) } paramDialog.value=false; loadParams(); ElMessage.success('保存成功') } catch(e:unknown){ void e; ElMessage.error((e as Error)?.message||'保存失败') } }
async function openVersions(row:ParamItem){ currentParamId.value=row.id; currentParamName.value=row.paramName; newValue.value=''; try{ const r=await internalApi.get<ParamVersion[]>('/system/parameters/'+row.id+'/versions'); versions.value=r??[] } catch(e){ void e; versions.value=[] } versionDialog.value=true }
async function addVersion(){ if(!currentParamId.value) return; try{ await internalApi.post('/system/parameters/'+currentParamId.value+'/versions',{paramValue:newValue.value}); ElMessage.success('已创建草稿版本'); openVersions({id:currentParamId.value} as ParamItem) } catch(e:unknown){ void e; ElMessage.error((e as Error)?.message||'创建失败') } }
async function submitVersion(row:ParamVersion){ if(!currentParamId.value) return; try{ await internalApi.post('/system/parameters/'+currentParamId.value+'/versions/'+row.id+'/submit'); ElMessage.success('已提交审批'); openVersions({id:currentParamId.value} as ParamItem) } catch(e:unknown){ void e; ElMessage.error((e as Error)?.message||'提交失败') } }

const versionStatusLabel=(s:string)=>s==='ACTIVE'?'生效中':s==='PENDING_APPROVAL'?'待审批':s==='REJECTED'?'已驳回':s==='RETIRED'?'已退休':s==='DRAFT'?'草稿':s
const versionStatusType=(s:string)=>s==='ACTIVE'?'success':s==='PENDING_APPROVAL'?'warning':s==='REJECTED'?'danger':'info'
onMounted(()=>{loadDicts()})
</script>

<template>
  <div style="padding:16px">
    <el-card>
      <el-tabs v-model="activeTab" @tab-change="onTab">
        <el-tab-pane label="字典" name="dict">
          <div class="bar">
            <b>字典列表</b>
            <el-input v-model="dictFilter.keyword" placeholder="编码/名称" clearable style="width:180px" @keyup.enter="loadDicts"/>
            <el-select v-model="dictFilter.status" placeholder="状态" clearable style="width:110px" @change="loadDicts"><el-option label="启用" value="ACTIVE"/><el-option label="停用" value="INACTIVE"/></el-select>
            <el-button type="primary" size="small" @click="loadDicts">查询</el-button>
            <el-button v-if="canCreateDict" type="success" size="small" style="margin-left:auto" @click="openDictCreate">新增字典</el-button>
          </div>
          <el-table :data="dictList" v-loading="loading" border size="small" style="margin-top:10px" empty-text="暂无字典">
            <el-table-column prop="dictCode" label="编码" width="150"/>
            <el-table-column prop="dictName" label="名称" width="150"/>
            <el-table-column prop="status" label="状态" width="80"><template #default="{row}"><el-tag :type="row.status==='ACTIVE'?'success':'info'" size="small">{{row.status==='ACTIVE'?'启用':'停用'}}</el-tag></template></el-table-column>
            <el-table-column prop="description" label="说明" min-width="160"/>
            <el-table-column label="操作" width="230"><template #default="{row}">
              <el-button link type="primary" size="small" @click="openItems(row)">字典项</el-button>
              <el-button v-if="canUpdateDict" link size="small" @click="openDictEdit(row)">编辑</el-button>
              <el-button v-if="row.status==='ACTIVE'?canDisableDict:canEnableDict" link size="small" @click="toggleDict(row,row.status==='ACTIVE'?'disable':'enable')">{{row.status==='ACTIVE'?'停用':'启用'}}</el-button>
            </template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="编号规则" name="number">
          <div class="bar"><b>编号规则</b><el-button v-if="canCreateNumber" type="success" size="small" style="margin-left:auto" @click="openNumberCreate">新增规则</el-button></div>
          <el-table :data="numberList" v-loading="loading" border size="small" style="margin-top:10px" empty-text="暂无编号规则"><el-table-column prop="ruleCode" label="规则编码" width="150"/><el-table-column prop="ruleName" label="名称" min-width="120"/><el-table-column prop="objectType" label="对象" width="130"/><el-table-column prop="prefix" label="前缀" width="100"/><el-table-column prop="dateFormat" label="日期格式" width="110"/><el-table-column prop="serialLength" label="流水位数" width="80"/><el-table-column prop="resetCycle" label="重置周期" width="90"/><el-table-column label="按组织" width="70"><template #default="{row}">{{row.organizationDimension?'是':'否'}}</template></el-table-column><el-table-column prop="status" label="状态" width="90"/><el-table-column label="操作" width="230"><template #default="{row}"><el-button link type="primary" size="small" :loading="previewLoading&&previewRuleCode===row.ruleCode" @click="previewNumber(row)">试生成</el-button><el-button v-if="canUpdateNumber" link type="primary" @click="openNumberEdit(row)">编辑</el-button><el-button v-if="row.status==='ACTIVE'?canDisableNumber:canEnableNumber" link @click="toggleNumber(row,row.status==='ACTIVE'?'disable':'enable')">{{row.status==='ACTIVE'?'停用':'启用'}}</el-button></template></el-table-column>
            <template #append v-if="preview"><div class="preview-bar">规则 {{previewRuleCode}} 试生成结果：<el-tag type="success" size="small">{{preview}}</el-tag><el-button link type="primary" size="small" @click="preview=''">清除</el-button></div></template>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="参数" name="param">
          <div class="bar"><b>参数列表</b><el-button v-if="canCreateParam" type="success" size="small" style="margin-left:auto" @click="openParamCreate">新增参数</el-button></div>
          <el-table :data="paramList" v-loading="loading" border size="small" style="margin-top:10px" empty-text="暂无参数">
            <el-table-column prop="paramCode" label="编码" width="150"/>
            <el-table-column prop="paramName" label="名称" width="150"/>
            <el-table-column prop="paramType" label="类型" width="90"/>
            <el-table-column prop="defaultValue" label="默认值" width="90"/>
            <el-table-column label="需审批" width="80"><template #default="{row}"><el-tag :type="row.approvalRequired?'warning':'info'" size="small">{{row.approvalRequired?'是':'否'}}</el-tag></template></el-table-column>
            <el-table-column label="操作" width="160"><template #default="{row}"><el-button link type="primary" size="small" @click="openVersions(row)">版本</el-button><el-button v-if="canUpdateParam" link size="small" @click="openParamEdit(row)">编辑</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="dictDialog" :title="dictForm.id?'编辑字典':'新增字典'" width="420px">
      <el-form label-width="70px"><el-form-item label="编码"><el-input v-model="dictForm.dictCode" :disabled="!!dictForm.id"/></el-form-item><el-form-item label="名称"><el-input v-model="dictForm.dictName"/></el-form-item><el-form-item label="说明"><el-input v-model="dictForm.description" type="textarea"/></el-form-item></el-form>
      <template #footer><el-button @click="dictDialog=false">取消</el-button><el-button type="primary" @click="saveDict">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="paramDialog" :title="paramForm.id?'编辑参数':'新增参数'" width="520px">
      <el-form label-width="90px"><el-form-item label="编码"><el-input v-model="paramForm.paramCode" :disabled="!!paramForm.id"/></el-form-item><el-form-item label="名称"><el-input v-model="paramForm.paramName"/></el-form-item><el-form-item label="类型"><el-select v-model="paramForm.paramType"><el-option label="字符串" value="STRING"/><el-option label="整数" value="INTEGER"/><el-option label="小数" value="DECIMAL"/><el-option label="布尔" value="BOOLEAN"/><el-option label="日期" value="DATE"/></el-select></el-form-item><el-form-item label="默认值"><el-input v-model="paramForm.defaultValue"/></el-form-item><el-form-item label="校验规则"><el-input v-model="paramForm.validationRule" placeholder="正则表达式(可选)"/></el-form-item><el-form-item label="需审批"><el-switch v-model="paramForm.approvalRequired"/></el-form-item><el-form-item label="说明"><el-input v-model="paramForm.description" type="textarea"/></el-form-item></el-form>
      <template #footer><el-button @click="paramDialog=false">取消</el-button><el-button type="primary" @click="saveParam">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="versionDialog" :title="currentParamName+' - 参数版本'" width="720px">
      <div class="bar" style="margin-bottom:8px"><el-input v-model="newValue" placeholder="新参数值" style="width:220px" :disabled="!canUpdateParam"/><el-button v-if="canUpdateParam" type="primary" @click="addVersion">创建版本草稿</el-button></div>
      <el-table :data="versions" border size="small">
        <el-table-column prop="version" label="版本" width="70"/>
        <el-table-column prop="paramValue" label="值" width="140"/>
        <el-table-column label="状态" width="110"><template #default="{row}"><el-tag :type="versionStatusType(row.status)" size="small">{{versionStatusLabel(row.status)}}</el-tag></template></el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160"/>
        <el-table-column prop="effectiveFrom" label="生效时间" min-width="160"/>
        <el-table-column label="操作" width="100"><template #default="{row}"><el-button v-if="canSubmitParam&&row.status==='DRAFT'" link type="primary" @click="submitVersion(row)">提交审批</el-button></template></el-table-column>
      </el-table>
      <template #footer><el-button @click="versionDialog=false">关闭</el-button></template>
    </el-dialog>
    <el-dialog v-model="itemDialog" :title="(currentDictionary?.dictName||'')+' - 字典项'" width="680px"><div class="bar" style="margin-bottom:8px"><el-button v-if="canCreateDict" type="primary" @click="openItemCreate">新增字典项</el-button></div><el-table :data="items" border size="small"><el-table-column prop="itemCode" label="编码"/><el-table-column prop="itemName" label="名称"/><el-table-column prop="sortOrder" label="排序" width="80"/><el-table-column prop="status" label="状态" width="90"/><el-table-column label="操作" width="150"><template #default="{row}"><el-button v-if="canUpdateDict" link @click="openItemEdit(row)">编辑</el-button><el-button v-if="row.status==='ACTIVE'?canDisableDict:canEnableDict" link @click="toggleItem(row,row.status==='ACTIVE'?'disable':'enable')">{{row.status==='ACTIVE'?'停用':'启用'}}</el-button></template></el-table-column></el-table></el-dialog>
    <el-dialog v-model="itemFormDialog" :title="itemForm.id?'编辑字典项':'新增字典项'" width="420px"><el-form label-width="80px"><el-form-item label="编码"><el-input v-model="itemForm.itemCode" :disabled="!!itemForm.id"/></el-form-item><el-form-item label="名称"><el-input v-model="itemForm.itemName"/></el-form-item><el-form-item label="排序"><el-input-number v-model="itemForm.sortOrder" :min="0"/></el-form-item></el-form><template #footer><el-button @click="itemFormDialog=false">取消</el-button><el-button type="primary" @click="saveItem">保存</el-button></template></el-dialog>
    <el-dialog v-model="numberDialog" :title="numberForm.id?'编辑编号规则':'新增编号规则'" width="520px"><el-form label-width="100px"><el-form-item label="规则编码"><el-input v-model="numberForm.ruleCode" :disabled="!!numberForm.id"/></el-form-item><el-form-item label="规则名称"><el-input v-model="numberForm.ruleName"/></el-form-item><el-form-item label="对象类型"><el-input v-model="numberForm.objectType" :disabled="!!numberForm.id"/></el-form-item><el-row :gutter="12"><el-col :span="12"><el-form-item label="前缀"><el-input v-model="numberForm.prefix"/></el-form-item></el-col><el-col :span="12"><el-form-item label="日期格式"><el-input v-model="numberForm.dateFormat" placeholder="如 yyyyMMdd"/></el-form-item></el-col></el-row><el-row :gutter="12"><el-col :span="12"><el-form-item label="流水位数"><el-input-number v-model="numberForm.serialLength" :min="1" :max="20"/></el-form-item></el-col><el-col :span="12"><el-form-item label="重置周期"><el-select v-model="numberForm.resetCycle"><el-option label="不重置" value="NONE"/><el-option label="每日" value="DAY"/><el-option label="每月" value="MONTH"/><el-option label="每年" value="YEAR"/></el-select></el-form-item></el-col></el-row><el-form-item label="按组织编号"><el-switch v-model="numberForm.organizationDimension" :disabled="!!numberForm.id"/></el-form-item></el-form><template #footer><el-button @click="numberDialog=false">取消</el-button><el-button type="primary" @click="saveNumber">保存</el-button></template></el-dialog>
  </div>
</template>

<style scoped>
.bar { display:flex; gap:10px; align-items:center; }
.preview-bar { display:flex; gap:10px; align-items:center; padding:8px 12px; }
</style>
