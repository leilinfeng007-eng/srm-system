<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应源管理",
  "featureLabel": "供应配额",
  "route": "/supply-source/quotas",
  "permission": "source:quota:view",
  "menuCode": "MENU_SOURCE_QUOTA",
  "phase": 3
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
