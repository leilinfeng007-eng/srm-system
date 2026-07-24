<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应商管理",
  "featureLabel": "供应商档案",
  "route": "/supplier/profiles",
  "permission": "supplier:profile:view",
  "menuCode": "MENU_SUPPLIER_PROFILE",
  "phase": 2
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
