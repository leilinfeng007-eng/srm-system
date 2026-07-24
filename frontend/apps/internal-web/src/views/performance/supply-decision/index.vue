<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "绩效与风险管理",
  "featureLabel": "供应决策",
  "route": "/performance/supply-decisions",
  "permission": "performance:supply-decision:view",
  "menuCode": "MENU_PERFORMANCE_SUPPLY_DECISION",
  "phase": 10
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
