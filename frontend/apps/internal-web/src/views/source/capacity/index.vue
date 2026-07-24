<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应源管理",
  "featureLabel": "能力与交期",
  "route": "/supply-source/capacities",
  "permission": "source:capacity:view",
  "menuCode": "MENU_SOURCE_CAPACITY",
  "phase": 3
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
