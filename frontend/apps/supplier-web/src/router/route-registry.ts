// Generated from frontend/supplier-portal-manifest.json. Do not hand-edit.
import type { RouteRecordRaw } from 'vue-router'

export const supplierPortalRoutes: RouteRecordRaw[] = [
  {
    path: "/portal",
    name: "PORTAL_HOME",
    component: () => import('../views/portal/home/index.vue'),
    meta: { title: "首页/任务", permission: "portal:home:view", requiresPortalEntry: true },
  },
  {
    path: "/portal/company",
    name: "PORTAL_COMPANY",
    component: () => import('../views/portal/company/index.vue'),
    meta: { title: "企业资料", permission: "portal:company:view", requiresPortalEntry: true },
  },
  {
    path: "/portal/sourcing",
    name: "PORTAL_SOURCING",
    component: () => import('../views/portal/sourcing/index.vue'),
    meta: { title: "寻源与报价", permission: "portal:sourcing:view", requiresPortalEntry: true },
  },
  {
    path: "/portal/contracts",
    name: "PORTAL_CONTRACT",
    component: () => import('../views/portal/contract/index.vue'),
    meta: { title: "合同与协议", permission: "portal:contract:view", requiresPortalEntry: true },
  },
  {
    path: "/portal/orders",
    name: "PORTAL_ORDER",
    component: () => import('../views/portal/order/index.vue'),
    meta: { title: "订单", permission: "portal:order:view", requiresPortalEntry: true },
  },
  {
    path: "/portal/delivery",
    name: "PORTAL_DELIVERY",
    component: () => import('../views/portal/delivery/index.vue'),
    meta: { title: "发货与收货", permission: "portal:delivery:view", requiresPortalEntry: true },
  },
  {
    path: "/portal/quality",
    name: "PORTAL_QUALITY",
    component: () => import('../views/portal/quality/index.vue'),
    meta: { title: "质量", permission: "portal:quality:view", requiresPortalEntry: true },
  },
  {
    path: "/portal/settlement",
    name: "PORTAL_SETTLEMENT",
    component: () => import('../views/portal/settlement/index.vue'),
    meta: { title: "对账与发票", permission: "portal:settlement:view", requiresPortalEntry: true },
  },
  {
    path: "/portal/performance",
    name: "PORTAL_PERFORMANCE",
    component: () => import('../views/portal/performance/index.vue'),
    meta: { title: "绩效与改善", permission: "portal:performance:view", requiresPortalEntry: true },
  },
]

