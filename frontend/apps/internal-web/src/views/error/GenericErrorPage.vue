<script setup lang="ts">
import { AppErrorPage } from '@srm/shared-ui'
</script>

<template>
  <AppErrorPage code="ERROR" title="暂时无法完成请求" description="请稍后重试；如问题持续，请向管理员提供响应中的 traceId。">
    <RouterLink to="/workbench">返回工作台</RouterLink>
  </AppErrorPage>
</template>
