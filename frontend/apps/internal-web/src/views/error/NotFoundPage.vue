<script setup lang="ts">
import { AppErrorPage } from '@srm/shared-ui'
</script>

<template>
  <AppErrorPage code="404" title="页面不存在" description="该地址未注册，或属于尚未启用的后续阶段功能。">
    <RouterLink to="/workbench">返回工作台</RouterLink>
  </AppErrorPage>
</template>
