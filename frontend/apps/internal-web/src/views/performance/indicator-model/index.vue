<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "绩效与风险管理",
  "featureLabel": "指标模型",
  "route": "/performance/indicator-models",
  "permission": "performance:indicator-model:view",
  "menuCode": "MENU_PERFORMANCE_INDICATOR_MODEL",
  "phase": 10
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
