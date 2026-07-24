<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "交付与收货协同",
  "featureLabel": "发运/在途",
  "route": "/delivery/shipments",
  "permission": "delivery:shipment:view",
  "menuCode": "MENU_DELIVERY_SHIPMENT",
  "phase": 7
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
