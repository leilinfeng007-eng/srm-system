<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "战略寻源",
  "featureLabel": "寻源项目",
  "route": "/sourcing/projects",
  "permission": "sourcing:project:view",
  "menuCode": "MENU_SOURCING_PROJECT",
  "phase": 4
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
