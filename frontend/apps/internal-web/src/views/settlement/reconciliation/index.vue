<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "对账与结算协同",
  "featureLabel": "对账单",
  "route": "/settlement/reconciliations",
  "permission": "settlement:reconciliation:view",
  "menuCode": "MENU_SETTLEMENT_RECONCILIATION",
  "phase": 9
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
