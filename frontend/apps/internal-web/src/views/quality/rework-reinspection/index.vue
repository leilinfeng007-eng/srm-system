<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应商质量管理",
  "featureLabel": "退货/返工/复验",
  "route": "/quality/rework-reinspection",
  "permission": "quality:rework-reinspection:view",
  "menuCode": "MENU_QUALITY_REWORK_REINSPECTION",
  "phase": 8
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
