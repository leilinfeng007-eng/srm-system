<script setup lang="ts">
import { SkeletonFeaturePage } from '@srm/shared-ui'

const metadata = {
  "domainLabel": "供应商管理",
  "featureLabel": "注册申请",
  "route": "/supplier/registrations",
  "permission": "supplier:registration:view",
  "menuCode": "MENU_SUPPLIER_REGISTRATION",
  "phase": 2
} as const
</script>

<template>
  <SkeletonFeaturePage v-bind="metadata" />
</template>
