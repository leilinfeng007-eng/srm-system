<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应商质量管理",
  "featureLabel": "供应商变更",
  "route": "/quality/supplier-changes",
  "permission": "quality:supplier-change:view",
  "menuCode": "MENU_QUALITY_SUPPLIER_CHANGE",
  "phase": 8
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
