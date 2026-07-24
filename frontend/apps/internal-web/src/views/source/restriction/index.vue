<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应源管理",
  "featureLabel": "限制/暂停",
  "route": "/supply-source/restrictions",
  "permission": "source:restriction:view",
  "menuCode": "MENU_SOURCE_RESTRICTION",
  "phase": 3
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
