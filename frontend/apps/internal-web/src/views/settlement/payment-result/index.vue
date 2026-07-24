<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "对账与结算协同",
  "featureLabel": "付款结果",
  "route": "/settlement/payment-results",
  "permission": "settlement:payment-result:view",
  "menuCode": "MENU_SETTLEMENT_PAYMENT_RESULT",
  "phase": 9
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
