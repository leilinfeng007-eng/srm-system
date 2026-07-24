<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "合同与价格管理",
  "featureLabel": "续签/终止",
  "route": "/contract/renewals-terminations",
  "permission": "contract:renewal-termination:view",
  "menuCode": "MENU_CONTRACT_RENEWAL_TERMINATION",
  "phase": 5
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
