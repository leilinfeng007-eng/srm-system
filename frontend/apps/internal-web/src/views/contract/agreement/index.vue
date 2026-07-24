<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "合同与价格管理",
  "featureLabel": "框架/补充协议",
  "route": "/contract/agreements",
  "permission": "contract:agreement:view",
  "menuCode": "MENU_CONTRACT_AGREEMENT",
  "phase": 5
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
