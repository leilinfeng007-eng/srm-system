<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "战略寻源",
  "featureLabel": "寻源需求",
  "route": "/sourcing/requirements",
  "permission": "sourcing:requirement:view",
  "menuCode": "MENU_SOURCING_REQUIREMENT",
  "phase": 4
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
