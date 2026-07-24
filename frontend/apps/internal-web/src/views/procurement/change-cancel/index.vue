<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "采购协同",
  "featureLabel": "订单变更/取消",
  "route": "/procurement/changes-cancellations",
  "permission": "procurement:change-cancel:view",
  "menuCode": "MENU_PROCUREMENT_CHANGE_CANCEL",
  "phase": 6
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
