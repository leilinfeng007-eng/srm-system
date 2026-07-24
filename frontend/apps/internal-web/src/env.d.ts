/// <reference types="vite/client" />

import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    domainLabel?: string
    domainCode?: string
    menuCode?: string
    permission?: string
    requiresAuth?: boolean
  }
}
