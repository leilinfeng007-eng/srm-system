<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "采购协同",
  "featureLabel": "采购订单",
  "route": "/procurement/purchase-orders",
  "permission": "procurement:purchase-order:view",
  "menuCode": "MENU_PROCUREMENT_PURCHASE_ORDER",
  "phase": 6
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
