<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应商管理",
  "featureLabel": "准入与认证",
  "route": "/supplier/admissions",
  "permission": "supplier:admission:view",
  "menuCode": "MENU_SUPPLIER_ADMISSION",
  "phase": 2
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
