<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "交付与收货协同",
  "featureLabel": "到货预约",
  "route": "/delivery/appointments",
  "permission": "delivery:appointment:view",
  "menuCode": "MENU_DELIVERY_APPOINTMENT",
  "phase": 7
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
