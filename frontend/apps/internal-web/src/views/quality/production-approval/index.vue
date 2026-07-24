<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应商质量管理",
  "featureLabel": "量产认可",
  "route": "/quality/production-approvals",
  "permission": "quality:production-approval:view",
  "menuCode": "MENU_QUALITY_PRODUCTION_APPROVAL",
  "phase": 8
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
