<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "系统管理",
  "featureLabel": "流程",
  "route": "/system/workflows",
  "permission": "system:workflow:view",
  "menuCode": "MENU_SYSTEM_WORKFLOW",
  "phase": 1
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
