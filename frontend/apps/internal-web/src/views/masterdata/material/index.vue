<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "基础数据中心",
  "featureLabel": "物料",
  "route": "/master-data/materials",
  "permission": "masterdata:material:view",
  "menuCode": "MENU_MASTERDATA_MATERIAL",
  "phase": 1
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
