<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "系统管理",
  "featureLabel": "权限与数据域",
  "route": "/system/permissions",
  "permission": "system:permission:view",
  "menuCode": "MENU_SYSTEM_PERMISSION",
  "phase": 1
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
