<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应商管理",
  "featureLabel": "复审与变更",
  "route": "/supplier/reviews-changes",
  "permission": "supplier:review-change:view",
  "menuCode": "MENU_SUPPLIER_REVIEW_CHANGE",
  "phase": 2
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
