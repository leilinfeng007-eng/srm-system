<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "战略寻源",
  "featureLabel": "澄清答疑",
  "route": "/sourcing/clarifications",
  "permission": "sourcing:clarification:view",
  "menuCode": "MENU_SOURCING_CLARIFICATION",
  "phase": 4
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
