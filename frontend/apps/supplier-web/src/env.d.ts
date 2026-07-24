/// <reference types="vite/client" />

import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    permission?: string
    requiresPortalEntry?: boolean
  }
}
