<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "基础数据中心",
  "featureLabel": "单位/币种/税码",
  "route": "/master-data/units-currencies-taxes",
  "permission": "masterdata:unit-currency-tax:view",
  "menuCode": "MENU_MASTERDATA_UNIT_CURRENCY_TAX",
  "phase": 1
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
