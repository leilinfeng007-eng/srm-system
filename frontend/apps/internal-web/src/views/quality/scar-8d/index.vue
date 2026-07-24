<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应商质量管理",
  "featureLabel": "SCAR/8D",
  "route": "/quality/scar-8d",
  "permission": "quality:scar-8d:view",
  "menuCode": "MENU_QUALITY_SCAR_8D",
  "phase": 8
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
