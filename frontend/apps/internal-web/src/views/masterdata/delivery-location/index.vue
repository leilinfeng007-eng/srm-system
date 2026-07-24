<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "基础数据中心",
  "featureLabel": "交付地点",
  "route": "/master-data/delivery-locations",
  "permission": "masterdata:delivery-location:view",
  "menuCode": "MENU_MASTERDATA_DELIVERY_LOCATION",
  "phase": 1
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
