<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "对账与结算协同",
  "featureLabel": "差异处理",
  "route": "/settlement/differences",
  "permission": "settlement:difference:view",
  "menuCode": "MENU_SETTLEMENT_DIFFERENCE",
  "phase": 9
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
