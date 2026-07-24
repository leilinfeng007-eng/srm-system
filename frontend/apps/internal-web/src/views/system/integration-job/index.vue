<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "系统管理",
  "featureLabel": "接口与任务监控",
  "route": "/system/integrations-jobs",
  "permission": "system:integration-job:view",
  "menuCode": "MENU_SYSTEM_INTEGRATION_JOB",
  "phase": 1
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
