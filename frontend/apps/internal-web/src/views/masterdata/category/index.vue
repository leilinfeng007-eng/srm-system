<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "基础数据中心",
  "featureLabel": "品类",
  "route": "/master-data/categories",
  "permission": "masterdata:category:view",
  "menuCode": "MENU_MASTERDATA_CATEGORY",
  "phase": 1
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
