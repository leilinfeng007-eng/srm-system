<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "采购协同",
  "featureLabel": "订单确认",
  "route": "/procurement/confirmations",
  "permission": "procurement:confirmation:view",
  "menuCode": "MENU_PROCUREMENT_CONFIRMATION",
  "phase": 6
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
