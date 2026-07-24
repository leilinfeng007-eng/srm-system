<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应商质量管理",
  "featureLabel": "质量异常",
  "route": "/quality/issues",
  "permission": "quality:issue:view",
  "menuCode": "MENU_QUALITY_ISSUE",
  "phase": 8
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
