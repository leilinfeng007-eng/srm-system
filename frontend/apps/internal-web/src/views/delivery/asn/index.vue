<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "交付与收货协同",
  "featureLabel": "ASN",
  "route": "/delivery/asn",
  "permission": "delivery:asn:view",
  "menuCode": "MENU_DELIVERY_ASN",
  "phase": 7
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
