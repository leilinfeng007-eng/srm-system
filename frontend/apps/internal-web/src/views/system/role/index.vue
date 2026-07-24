<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "系统管理",
  "featureLabel": "角色与挂载用户",
  "route": "/system/roles",
  "permission": "system:role:view",
  "menuCode": "MENU_SYSTEM_ROLE",
  "phase": 1
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
