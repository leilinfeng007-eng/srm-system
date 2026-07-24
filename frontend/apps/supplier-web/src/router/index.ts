import { createRouter, createWebHistory } from 'vue-router'
import PortalLayout from '../layouts/PortalLayout.vue'
import PortalLoginPage from '../views/PortalLoginPage.vue'
import PortalNotFoundPage from '../views/PortalNotFoundPage.vue'
import { supplierPortalRoutes } from './route-registry'

export const portalSessionKey = 'srm-supplier-stage0-entry'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', name: 'PORTAL_LOGIN', component: PortalLoginPage, meta: { title: '供应商门户登录' } },
    {
      path: '/',
      component: PortalLayout,
      children: [
        { path: '', redirect: '/portal' },
        ...supplierPortalRoutes,
      ],
    },
    { path: '/:pathMatch(.*)*', name: 'PORTAL_NOT_FOUND', component: PortalNotFoundPage, meta: { title: '页面不存在' } },
  ],
})

router.beforeEach((to) => {
  const entered = sessionStorage.getItem(portalSessionKey) === 'true'
  document.title = `${String(to.meta.title ?? '供应商门户')} · SRM 供应商协同管理系统`
  if (to.path === '/login' && entered) return '/portal'
  if (to.meta.requiresPortalEntry && !entered) return { path: '/login', query: { redirect: to.fullPath } }
  return true
})

export default router
