<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "绩效与风险管理",
  "featureLabel": "绩效评分",
  "route": "/performance/scores",
  "permission": "performance:score:view",
  "menuCode": "MENU_PERFORMANCE_SCORE",
  "phase": 10
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
