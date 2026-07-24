<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "采购协同",
  "featureLabel": "需求分配",
  "route": "/procurement/allocations",
  "permission": "procurement:allocation:view",
  "menuCode": "MENU_PROCUREMENT_ALLOCATION",
  "phase": 6
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
