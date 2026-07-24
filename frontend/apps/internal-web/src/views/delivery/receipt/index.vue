<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "交付与收货协同",
  "featureLabel": "收货反馈",
  "route": "/delivery/receipts",
  "permission": "delivery:receipt:view",
  "menuCode": "MENU_DELIVERY_RECEIPT",
  "phase": 7
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
