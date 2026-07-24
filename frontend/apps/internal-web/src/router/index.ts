import type { Pinia } from 'pinia'
import { createRouter, createWebHistory, type Router } from 'vue-router'
import InternalLayout from '../layouts/InternalLayout.vue'
import LoginPage from '../views/auth/LoginPage.vue'
import ForbiddenPage from '../views/error/ForbiddenPage.vue'
import GenericErrorPage from '../views/error/GenericErrorPage.vue'
import NotFoundPage from '../views/error/NotFoundPage.vue'
import { useAuthStore } from '../stores/auth'
import { featureRoutes } from './route-registry'

export const createInternalRouter = () => createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', name: 'LOGIN', component: LoginPage, meta: { title: '登录' } },
    {
      path: '/',
      component: InternalLayout,
      children: [
        { path: '', redirect: '/workbench' },
        ...featureRoutes,
        { path: '403', name: 'FORBIDDEN', component: ForbiddenPage, meta: { title: '无权访问', requiresAuth: true } },
        { path: 'error', name: 'ERROR', component: GenericErrorPage, meta: { title: '系统错误', requiresAuth: true } },
      ],
    },
    { path: '/:pathMatch(.*)*', name: 'NOT_FOUND', component: NotFoundPage, meta: { title: '页面不存在' } },
  ],
})

export const installRouterGuards = (router: Router, pinia: Pinia) => {
  router.beforeEach(async (to) => {
    const auth = useAuthStore(pinia)
    await auth.restore()
    document.title = `${String(to.meta.title ?? 'SRM')} · SRM 供应商协同管理系统`
    if (to.path === '/login' && auth.isAuthenticated) return '/workbench'
    if (to.meta.requiresAuth && !auth.isAuthenticated) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    if (to.meta.requiresAuth && !auth.hasPermission(to.meta.permission)) {
      return { path: '/403', query: { from: to.fullPath } }
    }
    return true
  })
}
