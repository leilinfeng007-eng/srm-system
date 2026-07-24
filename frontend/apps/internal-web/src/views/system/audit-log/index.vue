<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "系统管理",
  "featureLabel": "审计日志",
  "route": "/system/audit-logs",
  "permission": "system:audit-log:view",
  "menuCode": "MENU_SYSTEM_AUDIT_LOG",
  "phase": 1
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
