<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应商门户",
  "featureLabel": "对账与发票",
  "route": "/portal/settlement",
  "permission": "portal:settlement:view",
  "menuCode": "PORTAL_SETTLEMENT",
  "phase": 0
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
