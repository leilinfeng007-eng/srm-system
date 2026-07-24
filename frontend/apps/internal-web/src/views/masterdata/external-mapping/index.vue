<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "基础数据中心",
  "featureLabel": "外围映射",
  "route": "/master-data/external-mappings",
  "permission": "masterdata:external-mapping:view",
  "menuCode": "MENU_MASTERDATA_EXTERNAL_MAPPING",
  "phase": 1
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
