<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "战略寻源",
  "featureLabel": "供应商报价",
  "route": "/sourcing/quotations",
  "permission": "sourcing:quotation:view",
  "menuCode": "MENU_SOURCING_QUOTATION",
  "phase": 4
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
