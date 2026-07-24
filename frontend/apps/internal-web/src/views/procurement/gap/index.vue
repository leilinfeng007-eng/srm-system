<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "采购协同",
  "featureLabel": "需求缺口",
  "route": "/procurement/gaps",
  "permission": "procurement:gap:view",
  "menuCode": "MENU_PROCUREMENT_GAP",
  "phase": 6
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
