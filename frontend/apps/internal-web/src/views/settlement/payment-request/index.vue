<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "对账与结算协同",
  "featureLabel": "请款材料",
  "route": "/settlement/payment-requests",
  "permission": "settlement:payment-request:view",
  "menuCode": "MENU_SETTLEMENT_PAYMENT_REQUEST",
  "phase": 9
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
