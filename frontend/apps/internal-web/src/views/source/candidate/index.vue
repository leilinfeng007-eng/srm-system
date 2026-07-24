<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应源管理",
  "featureLabel": "候选供应源",
  "route": "/supply-source/candidates",
  "permission": "source:candidate:view",
  "menuCode": "MENU_SOURCE_CANDIDATE",
  "phase": 3
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
