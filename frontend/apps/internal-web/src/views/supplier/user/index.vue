<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应商管理",
  "featureLabel": "供应商用户",
  "route": "/supplier/users",
  "permission": "supplier:user:view",
  "menuCode": "MENU_SUPPLIER_USER",
  "phase": 2
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
