<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "基础数据中心",
  "featureLabel": "采购组织",
  "route": "/master-data/purchasing-organizations",
  "permission": "masterdata:purchasing-organization:view",
  "menuCode": "MENU_MASTERDATA_PURCHASING_ORG",
  "phase": 1
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
