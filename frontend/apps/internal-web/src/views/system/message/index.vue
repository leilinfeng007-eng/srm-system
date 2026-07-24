<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "系统管理",
  "featureLabel": "消息",
  "route": "/system/messages",
  "permission": "system:message:view",
  "menuCode": "MENU_SYSTEM_MESSAGE",
  "phase": 1
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
