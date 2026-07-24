<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应商门户",
  "featureLabel": "订单",
  "route": "/portal/orders",
  "permission": "portal:order:view",
  "menuCode": "PORTAL_ORDER",
  "phase": 0
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
