<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "绩效与风险管理",
  "featureLabel": "改善计划",
  "route": "/performance/improvements",
  "permission": "performance:improvement:view",
  "menuCode": "MENU_PERFORMANCE_IMPROVEMENT",
  "phase": 10
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
