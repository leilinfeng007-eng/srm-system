<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "战略寻源",
  "featureLabel": "招投标/竞价",
  "route": "/sourcing/bids-auctions",
  "permission": "sourcing:bid-auction:view",
  "menuCode": "MENU_SOURCING_BID_AUCTION",
  "phase": 4
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
