<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "绩效与风险管理",
  "featureLabel": "风险台账",
  "route": "/performance/risks",
  "permission": "performance:risk:view",
  "menuCode": "MENU_PERFORMANCE_RISK",
  "phase": 10
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
