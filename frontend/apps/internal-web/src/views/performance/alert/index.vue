<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "绩效与风险管理",
  "featureLabel": "预警事件",
  "route": "/performance/alerts",
  "permission": "performance:alert:view",
  "menuCode": "MENU_PERFORMANCE_ALERT",
  "phase": 10
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
