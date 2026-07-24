<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应商质量管理",
  "featureLabel": "样件/试制",
  "route": "/quality/samples-trials",
  "permission": "quality:sample-trial:view",
  "menuCode": "MENU_QUALITY_SAMPLE_TRIAL",
  "phase": 8
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
