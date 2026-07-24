<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "合同与价格管理",
  "featureLabel": "合同台账",
  "route": "/contract/contracts",
  "permission": "contract:contract:view",
  "menuCode": "MENU_CONTRACT_CONTRACT",
  "phase": 5
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
