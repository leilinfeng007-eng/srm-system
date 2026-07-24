<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "绩效与风险管理",
  "featureLabel": "评级结果",
  "route": "/performance/ratings",
  "permission": "performance:rating:view",
  "menuCode": "MENU_PERFORMANCE_RATING",
  "phase": 10
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
