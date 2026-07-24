<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "采购协同",
  "featureLabel": "供应源匹配",
  "route": "/procurement/matching",
  "permission": "procurement:matching:view",
  "menuCode": "MENU_PROCUREMENT_MATCHING",
  "phase": 6
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
