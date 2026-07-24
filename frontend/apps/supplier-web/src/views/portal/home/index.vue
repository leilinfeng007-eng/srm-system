<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应商门户",
  "featureLabel": "首页/任务",
  "route": "/portal",
  "permission": "portal:home:view",
  "menuCode": "PORTAL_HOME",
  "phase": 0
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
