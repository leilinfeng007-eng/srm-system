<script setup lang="ts">
import { AppErrorPage } from '@srm/shared-ui'
</script>

<template>
  <AppErrorPage code="403" title="无权访问" description="当前角色未获得该页面的查看权限；菜单隐藏不能替代后端鉴权。">
    <RouterLink to="/workbench">返回工作台</RouterLink>
  </AppErrorPage>
</template>
