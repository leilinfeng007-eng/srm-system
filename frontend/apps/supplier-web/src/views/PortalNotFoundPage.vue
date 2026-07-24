<script setup lang="ts">
import { AppErrorPage } from '@srm/shared-ui'
</script>

<template>
  <AppErrorPage code="404" title="门户页面不存在" description="该地址不属于阶段 0 的九个供应商门户入口。">
    <RouterLink to="/portal">返回门户首页</RouterLink>
  </AppErrorPage>
</template>
