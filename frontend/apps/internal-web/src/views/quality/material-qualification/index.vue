<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应商质量管理",
  "featureLabel": "物料资格",
  "route": "/quality/material-qualifications",
  "permission": "quality:material-qualification:view",
  "menuCode": "MENU_QUALITY_MATERIAL_QUALIFICATION",
  "phase": 8
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
