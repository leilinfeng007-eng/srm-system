<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "合同与价格管理",
  "featureLabel": "履约与临期预警",
  "route": "/contract/performance-alerts",
  "permission": "contract:performance-alert:view",
  "menuCode": "MENU_CONTRACT_PERFORMANCE_ALERT",
  "phase": 5
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
