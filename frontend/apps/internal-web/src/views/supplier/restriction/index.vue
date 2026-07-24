<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应商管理",
  "featureLabel": "冻结与退出",
  "route": "/supplier/restrictions",
  "permission": "supplier:restriction:view",
  "menuCode": "MENU_SUPPLIER_RESTRICTION",
  "phase": 2
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
