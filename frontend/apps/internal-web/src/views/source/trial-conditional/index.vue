<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应源管理",
  "featureLabel": "试制/条件供应源",
  "route": "/supply-source/trial-conditional",
  "permission": "source:trial-conditional:view",
  "menuCode": "MENU_SOURCE_TRIAL_CONDITIONAL",
  "phase": 3
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
