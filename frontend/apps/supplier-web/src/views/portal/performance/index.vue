<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应商门户",
  "featureLabel": "绩效与改善",
  "route": "/portal/performance",
  "permission": "portal:performance:view",
  "menuCode": "PORTAL_PERFORMANCE",
  "phase": 0
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
