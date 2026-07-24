<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应源管理",
  "featureLabel": "正式供应源",
  "route": "/supply-source/approved",
  "permission": "source:approved:view",
  "menuCode": "MENU_SOURCE_APPROVED",
  "phase": 3
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
