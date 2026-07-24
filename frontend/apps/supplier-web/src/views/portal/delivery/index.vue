<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应商门户",
  "featureLabel": "发货与收货",
  "route": "/portal/delivery",
  "permission": "portal:delivery:view",
  "menuCode": "PORTAL_DELIVERY",
  "phase": 0
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
