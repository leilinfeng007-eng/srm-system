<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "战略寻源",
  "featureLabel": "中选转化任务",
  "route": "/sourcing/conversions",
  "permission": "sourcing:conversion:view",
  "menuCode": "MENU_SOURCING_CONVERSION",
  "phase": 4
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
