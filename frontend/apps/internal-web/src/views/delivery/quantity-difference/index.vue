<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "交付与收货协同",
  "featureLabel": "数量差异",
  "route": "/delivery/quantity-differences",
  "permission": "delivery:quantity-difference:view",
  "menuCode": "MENU_DELIVERY_QUANTITY_DIFFERENCE",
  "phase": 7
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
