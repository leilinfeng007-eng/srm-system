<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "合同与价格管理",
  "featureLabel": "采购依据",
  "route": "/contract/purchasing-bases",
  "permission": "contract:purchasing-basis:view",
  "menuCode": "MENU_CONTRACT_PURCHASING_BASIS",
  "phase": 5
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
