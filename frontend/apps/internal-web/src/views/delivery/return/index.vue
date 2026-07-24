<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "交付与收货协同",
  "featureLabel": "退货发运",
  "route": "/delivery/returns",
  "permission": "delivery:return:view",
  "menuCode": "MENU_DELIVERY_RETURN",
  "phase": 7
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
