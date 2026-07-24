<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应商质量管理",
  "featureLabel": "检验结果",
  "route": "/quality/inspections",
  "permission": "quality:inspection:view",
  "menuCode": "MENU_QUALITY_INSPECTION",
  "phase": 8
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
