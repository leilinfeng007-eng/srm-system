<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应商门户",
  "featureLabel": "寻源与报价",
  "route": "/portal/sourcing",
  "permission": "portal:sourcing:view",
  "menuCode": "PORTAL_SOURCING",
  "phase": 0
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
