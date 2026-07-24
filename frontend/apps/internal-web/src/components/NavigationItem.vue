<script setup lang="ts">
import type { MenuNode } from '@srm/shared-types'

defineProps<{ node: MenuNode }>()
</script>

<template>
  <el-menu-item
    v-if="node.menuCode === 'MENU_WORKBENCH'"
    :index="node.children[0]?.route ?? '/workbench'"
  >
    <span>{{ node.label }}</span>
  </el-menu-item>
  <el-sub-menu v-else :index="node.menuCode">
    <template #title><span>{{ node.label }}</span></template>
    <el-menu-item v-for="child in node.children" :key="child.menuCode" :index="child.route">
      {{ child.label }}
    </el-menu-item>
  </el-sub-menu>
</template>
