<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "系统管理",
  "featureLabel": "字典参数",
  "route": "/system/dictionaries-parameters",
  "permission": "system:dictionary-parameter:view",
  "menuCode": "MENU_SYSTEM_DICTIONARY_PARAMETER",
  "phase": 1
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
