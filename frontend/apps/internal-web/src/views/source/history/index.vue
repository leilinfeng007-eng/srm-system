<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应源管理",
  "featureLabel": "供应源变更历史",
  "route": "/supply-source/history",
  "permission": "source:history:view",
  "menuCode": "MENU_SOURCE_HISTORY",
  "phase": 3
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
