<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "交付与收货协同",
  "featureLabel": "交付计划",
  "route": "/delivery/plans",
  "permission": "delivery:plan:view",
  "menuCode": "MENU_DELIVERY_PLAN",
  "phase": 7
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
