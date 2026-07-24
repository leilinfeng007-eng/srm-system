<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "系统管理",
  "featureLabel": "模板附件",
  "route": "/system/templates-attachments",
  "permission": "system:template-attachment:view",
  "menuCode": "MENU_SYSTEM_TEMPLATE_ATTACHMENT",
  "phase": 1
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
