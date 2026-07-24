<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "合同与价格管理",
  "featureLabel": "价格条件",
  "route": "/contract/price-conditions",
  "permission": "contract:price-condition:view",
  "menuCode": "MENU_CONTRACT_PRICE_CONDITION",
  "phase": 5
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
