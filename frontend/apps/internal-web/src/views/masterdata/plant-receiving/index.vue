<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "基础数据中心",
  "featureLabel": "工厂/收货地点",
  "route": "/master-data/plants-receiving",
  "permission": "masterdata:plant-receiving:view",
  "menuCode": "MENU_MASTERDATA_PLANT_RECEIVING",
  "phase": 1
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
