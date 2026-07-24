<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "战略寻源",
  "featureLabel": "评审与定标",
  "route": "/sourcing/evaluations-awards",
  "permission": "sourcing:evaluation-award:view",
  "menuCode": "MENU_SOURCING_EVALUATION_AWARD",
  "phase": 4
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
