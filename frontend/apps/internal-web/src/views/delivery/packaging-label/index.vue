<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "交付与收货协同",
  "featureLabel": "包装与标签",
  "route": "/delivery/packaging-labels",
  "permission": "delivery:packaging-label:view",
  "menuCode": "MENU_DELIVERY_PACKAGING_LABEL",
  "phase": 7
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
