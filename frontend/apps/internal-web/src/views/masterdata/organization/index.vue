<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "基础数据中心",
  "featureLabel": "企业组织",
  "route": "/master-data/organizations",
  "permission": "masterdata:organization:view",
  "menuCode": "MENU_MASTERDATA_ORGANIZATION",
  "phase": 1
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
