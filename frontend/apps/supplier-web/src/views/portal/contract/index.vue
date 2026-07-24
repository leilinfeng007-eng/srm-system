<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应商门户",
  "featureLabel": "合同与协议",
  "route": "/portal/contracts",
  "permission": "portal:contract:view",
  "menuCode": "PORTAL_CONTRACT",
  "phase": 0
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
