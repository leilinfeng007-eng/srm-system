<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "绩效与风险管理",
  "featureLabel": "第二供应源计划",
  "route": "/performance/second-source-plans",
  "permission": "performance:second-source:view",
  "menuCode": "MENU_PERFORMANCE_SECOND_SOURCE",
  "phase": 10
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
