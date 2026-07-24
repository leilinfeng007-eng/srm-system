<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应商管理",
  "featureLabel": "供应商资源池",
  "route": "/supplier/pool",
  "permission": "supplier:pool:view",
  "menuCode": "MENU_SUPPLIER_POOL",
  "phase": 2
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
