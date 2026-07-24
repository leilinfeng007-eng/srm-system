<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应商质量管理",
  "featureLabel": "审核计划",
  "route": "/quality/audits",
  "permission": "quality:audit:view",
  "menuCode": "MENU_QUALITY_AUDIT",
  "phase": 8
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
