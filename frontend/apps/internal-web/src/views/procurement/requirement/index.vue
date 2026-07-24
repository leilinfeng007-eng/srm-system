<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "采购协同",
  "featureLabel": "采购需求",
  "route": "/procurement/requirements",
  "permission": "procurement:requirement:view",
  "menuCode": "MENU_PROCUREMENT_REQUIREMENT",
  "phase": 6
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
