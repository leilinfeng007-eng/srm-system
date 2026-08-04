<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { PageResult } from '@srm/shared-types'
import { internalApi } from '../../../api/internal-api'
import { usePermission } from '../../../composables/usePermission'

type ResourceType = 'UNIT' | 'CURRENCY' | 'TAX_CODE'
interface CommonItem { id:number; status:'ACTIVE'|'INACTIVE'; createdAt:string }
interface UnitItem extends CommonItem { unitCode:string; unitName:string }
interface CurrencyItem extends CommonItem { currencyCode:string; currencyName:string; symbol:string; decimalPlaces:number }
interface TaxItem extends CommonItem { taxCode:string; taxName:string; country:string; taxRate:number }
interface Row extends CommonItem { resourceType:ResourceType; code:string; name:string; symbol:string; value:string }

const loading=ref(false); const source=ref<Row[]>([]); const page=ref(1); const pageSize=ref(20)
const filter=reactive({keyword:'',resourceType:'' as ''|ResourceType,status:''})
const dialog=ref(false); const mode=ref<'create'|'edit'>('create'); const saving=ref(false)
const form=reactive({id:0,resourceType:'UNIT' as ResourceType,code:'',name:'',symbol:'',decimalPlaces:2,country:'CN',taxRate:0})

const paths:Record<ResourceType,string>={UNIT:'units',CURRENCY:'currencies',TAX_CODE:'tax-codes'}
const canCreate={UNIT:usePermission('masterdata:unit:create'),CURRENCY:usePermission('masterdata:currency:create'),TAX_CODE:usePermission('masterdata:tax-code:create')}
const canUpdate={UNIT:usePermission('masterdata:unit:update'),CURRENCY:usePermission('masterdata:currency:update'),TAX_CODE:usePermission('masterdata:tax-code:update')}
const canEnable={UNIT:usePermission('masterdata:unit:enable'),CURRENCY:usePermission('masterdata:currency:enable'),TAX_CODE:usePermission('masterdata:tax-code:enable')}
const canDisable={UNIT:usePermission('masterdata:unit:disable'),CURRENCY:usePermission('masterdata:currency:disable'),TAX_CODE:usePermission('masterdata:tax-code:disable')}

function unitRow(x:UnitItem):Row{return {...x,resourceType:'UNIT',code:x.unitCode,name:x.unitName,symbol:'',value:'—'}}
function currencyRow(x:CurrencyItem):Row{return {...x,resourceType:'CURRENCY',code:x.currencyCode,name:x.currencyName,symbol:x.symbol??'',value:`${x.decimalPlaces} 位小数`}}
function taxRow(x:TaxItem):Row{return {...x,resourceType:'TAX_CODE',code:x.taxCode,name:x.taxName,symbol:x.country??'',value:`${x.taxRate}%`}}
async function load(){loading.value=true;try{const [u,c,t]=await Promise.all([
  internalApi.get<PageResult<UnitItem>>('/master-data/units?page=1&size=200'),
  internalApi.get<PageResult<CurrencyItem>>('/master-data/currencies?page=1&size=200'),
  internalApi.get<PageResult<TaxItem>>('/master-data/tax-codes?page=1&size=200'),
]);source.value=[...u.items.map(unitRow),...c.items.map(currencyRow),...t.items.map(taxRow)]}catch(e:unknown){source.value=[];ElMessage.error((e as Error).message||'加载失败')}finally{loading.value=false}}
const filtered=computed(()=>source.value.filter(r=>(!filter.resourceType||r.resourceType===filter.resourceType)&&(!filter.status||r.status===filter.status)&&(!filter.keyword||`${r.code} ${r.name}`.toLowerCase().includes(filter.keyword.toLowerCase()))))
const rows=computed(()=>filtered.value.slice((page.value-1)*pageSize.value,page.value*pageSize.value))
function reset(){Object.assign(filter,{keyword:'',resourceType:'',status:''});page.value=1}
function openCreate(type:ResourceType){mode.value='create';Object.assign(form,{id:0,resourceType:type,code:'',name:'',symbol:'',decimalPlaces:2,country:'CN',taxRate:0});dialog.value=true}
async function openEdit(row:Row){try{const detail=await internalApi.get<UnitItem|CurrencyItem|TaxItem>(`/master-data/${paths[row.resourceType]}/${row.id}`);mode.value='edit';if(row.resourceType==='UNIT'){const x=detail as UnitItem;Object.assign(form,{id:x.id,resourceType:row.resourceType,code:x.unitCode,name:x.unitName,symbol:'',decimalPlaces:2,country:'CN',taxRate:0})}else if(row.resourceType==='CURRENCY'){const x=detail as CurrencyItem;Object.assign(form,{id:x.id,resourceType:row.resourceType,code:x.currencyCode,name:x.currencyName,symbol:x.symbol??'',decimalPlaces:x.decimalPlaces,country:'CN',taxRate:0})}else{const x=detail as TaxItem;Object.assign(form,{id:x.id,resourceType:row.resourceType,code:x.taxCode,name:x.taxName,symbol:'',decimalPlaces:2,country:x.country,taxRate:x.taxRate})}dialog.value=true}catch(e:unknown){ElMessage.error((e as Error).message||'详情加载失败')}}
function body(){if(form.resourceType==='UNIT')return{unitCode:form.code,unitName:form.name};if(form.resourceType==='CURRENCY')return{currencyCode:form.code,currencyName:form.name,symbol:form.symbol,decimalPlaces:form.decimalPlaces};return{taxCode:form.code,taxName:form.name,country:form.country,taxRate:form.taxRate}}
async function save(){saving.value=true;try{const path='/master-data/'+paths[form.resourceType];if(mode.value==='create')await internalApi.post(path,body());else await internalApi.request(`${path}/${form.id}`,{method:'PUT',body:body()});dialog.value=false;ElMessage.success('保存成功');load()}catch(e:unknown){ElMessage.error((e as Error).message||'保存失败')}finally{saving.value=false}}
async function setStatus(row:Row,action:'enable'|'disable'){try{await ElMessageBox.confirm(`确认${action==='enable'?'启用':'停用'} ${row.name}？`);await internalApi.post(`/master-data/${paths[row.resourceType]}/${row.id}/${action}`);ElMessage.success('操作成功');load()}catch(e){void e}}
function label(t:ResourceType){return t==='UNIT'?'单位':t==='CURRENCY'?'币种':'税码'}
function allowed(permissions:Record<ResourceType,{value:boolean}>,t:string){return permissions[t as ResourceType].value}
onMounted(load)
</script>

<template>
  <section class="page"><el-card>
    <div class="toolbar"><el-input v-model="filter.keyword" clearable placeholder="名称或编码" style="width:220px" @keyup.enter="page=1"/><el-select v-model="filter.resourceType" clearable placeholder="类型" style="width:120px"><el-option label="单位" value="UNIT"/><el-option label="币种" value="CURRENCY"/><el-option label="税码" value="TAX_CODE"/></el-select><el-select v-model="filter.status" clearable placeholder="状态" style="width:120px"><el-option label="启用" value="ACTIVE"/><el-option label="停用" value="INACTIVE"/></el-select><el-button @click="reset">重置</el-button><el-dropdown style="margin-left:auto" @command="openCreate"><el-button type="primary">新建</el-button><template #dropdown><el-dropdown-menu><el-dropdown-item v-if="canCreate.UNIT" command="UNIT">单位</el-dropdown-item><el-dropdown-item v-if="canCreate.CURRENCY" command="CURRENCY">币种</el-dropdown-item><el-dropdown-item v-if="canCreate.TAX_CODE" command="TAX_CODE">税码</el-dropdown-item></el-dropdown-menu></template></el-dropdown></div>
    <el-table v-loading="loading" :data="rows" border stripe style="margin-top:16px"><el-table-column prop="code" label="编码" width="150"/><el-table-column prop="name" label="名称" min-width="180"/><el-table-column label="类型" width="90"><template #default="{row}">{{label(row.resourceType)}}</template></el-table-column><el-table-column prop="symbol" label="符号/国家" width="110"/><el-table-column prop="value" label="精度/税率" width="120"/><el-table-column label="状态" width="90"><template #default="{row}"><el-tag :type="row.status==='ACTIVE'?'success':'info'">{{row.status==='ACTIVE'?'启用':'停用'}}</el-tag></template></el-table-column><el-table-column prop="createdAt" label="创建时间" width="180"/><el-table-column label="操作" width="180"><template #default="{row}"><el-button v-if="allowed(canUpdate,row.resourceType)" link type="primary" @click="openEdit(row)">编辑</el-button><el-button v-if="row.status==='ACTIVE'&&allowed(canDisable,row.resourceType)" link type="warning" @click="setStatus(row,'disable')">停用</el-button><el-button v-if="row.status==='INACTIVE'&&allowed(canEnable,row.resourceType)" link type="success" @click="setStatus(row,'enable')">启用</el-button></template></el-table-column></el-table>
    <div class="pager"><el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="filtered.length" layout="total, sizes, prev, pager, next"/></div>
  </el-card>
  <el-dialog v-model="dialog" :title="`${mode==='create'?'新增':'编辑'}${label(form.resourceType)}`" width="500px"><el-form :model="form" label-width="90px"><el-form-item label="编码" required><el-input v-model="form.code" :disabled="mode==='edit'"/></el-form-item><el-form-item label="名称" required><el-input v-model="form.name"/></el-form-item><template v-if="form.resourceType==='CURRENCY'"><el-form-item label="符号"><el-input v-model="form.symbol"/></el-form-item><el-form-item label="小数位"><el-input-number v-model="form.decimalPlaces" :min="0" :max="8"/></el-form-item></template><template v-if="form.resourceType==='TAX_CODE'"><el-form-item label="国家"><el-input v-model="form.country"/></el-form-item><el-form-item label="税率"><el-input-number v-model="form.taxRate" :min="0" :max="100" :precision="4"/></el-form-item></template></el-form><template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" :loading="saving" :disabled="!form.code||!form.name" @click="save">保存</el-button></template></el-dialog></section>
</template>

<style scoped>.page{padding:24px}.toolbar{display:flex;gap:12px;align-items:center}.pager{display:flex;justify-content:flex-end;margin-top:16px}</style>
