<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "战略寻源",
  "featureLabel": "RFI/RFQ",
  "route": "/sourcing/rfx",
  "permission": "sourcing:rfx:view",
  "menuCode": "MENU_SOURCING_RFX",
  "phase": 4
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
