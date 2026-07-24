<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "合同与价格管理",
  "featureLabel": "调价与版本",
  "route": "/contract/price-versions",
  "permission": "contract:price-version:view",
  "menuCode": "MENU_CONTRACT_PRICE_VERSION",
  "phase": 5
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
