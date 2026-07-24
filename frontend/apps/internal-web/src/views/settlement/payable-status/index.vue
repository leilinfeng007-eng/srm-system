<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "对账与结算协同",
  "featureLabel": "应付状态",
  "route": "/settlement/payable-status",
  "permission": "settlement:payable-status:view",
  "menuCode": "MENU_SETTLEMENT_PAYABLE_STATUS",
  "phase": 9
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
