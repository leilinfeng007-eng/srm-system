<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { internalApi } from '../../../api/internal-api'

interface PermItem {
  id: number; permissionCode: string; domainCode: string; resourceCode: string
  actionCode: string; description: string; enabled: boolean
}
interface MenuNode { id: number; label: string; menuCode: string; children: MenuNode[] }

const loading = ref(false)
const list = ref<PermItem[]>([])
const menuTree = ref<MenuNode[]>([])
const selectedDomain = ref('')
const searchKeyword = ref('')

const filteredList = computed(() => {
  return list.value.filter(p => {
    if (selectedDomain.value && p.domainCode !== selectedDomain.value) return false
    if (searchKeyword.value && !p.permissionCode.includes(searchKeyword.value) && !p.description?.includes(searchKeyword.value)) return false
    return true
  })
})

async function loadData() {
  loading.value = true
  try {
    const [permRes, menuRes] = await Promise.all([
      internalApi.get<{ items: PermItem[] }>('/system/permissions?pageSize=999'),
      internalApi.get<MenuNode[]>('/system/menus')
    ])
    list.value = permRes?.items ?? []
    menuTree.value = menuRes ?? []
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function reset() {
  selectedDomain.value = ''
  searchKeyword.value = ''
}

onMounted(loadData)
</script>

<template>
  <div style="padding: 16px">
    <h3 style="margin: 0 0 12px">功能权限</h3>
    <p style="color: #909399; font-size: 13px; margin: 0 0 16px">查看系统权限资源目录，包括菜单、页面、操作及后端接口权限编码</p>

    <el-tabs>
      <el-tab-pane label="权限资源目录">
        <el-form :inline="true" size="small" style="margin-bottom: 12px">
          <el-form-item label="领域">
            <el-select v-model="selectedDomain" clearable placeholder="全部">
              <el-option label="system" value="system"/><el-option label="masterdata" value="masterdata"/>
              <el-option label="workbench" value="workbench"/>
            </el-select>
          </el-form-item>
          <el-form-item label="关键词"><el-input v-model="searchKeyword" clearable placeholder="权限编码/描述"/></el-form-item>
          <el-form-item><el-button @click="reset">重置</el-button></el-form-item>
        </el-form>

        <el-table :data="filteredList" v-loading="loading" border size="small" empty-text="无匹配权限" max-height="calc(100vh - 300px)">
          <el-table-column prop="permissionCode" label="权限编码" min-width="220"/>
          <el-table-column prop="domainCode" label="领域" width="100"/>
          <el-table-column prop="resourceCode" label="资源" width="120"/>
          <el-table-column prop="actionCode" label="操作" width="80"/>
          <el-table-column prop="description" label="说明" min-width="150"/>
          <el-table-column prop="enabled" label="状态" width="70">
            <template #default="{row}"><el-tag :type="row.enabled?'success':'info'" size="small">{{ row.enabled?'启用':'停用' }}</el-tag></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="菜单资源树">
        <el-tree :data="menuTree" node-key="id" :props="{children:'children',label:'label'}" default-expand-all highlight-current style="max-width:500px"/>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>
