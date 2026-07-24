<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应商管理",
  "featureLabel": "资质证照",
  "route": "/supplier/qualifications",
  "permission": "supplier:qualification:view",
  "menuCode": "MENU_SUPPLIER_QUALIFICATION",
  "phase": 2
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
