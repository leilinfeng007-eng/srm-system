import { defineStore } from 'pinia'
import type { MenuNode, TokenResponse, UserPrincipal } from '@srm/shared-types'
import { internalApi, refreshInternalSession } from '../api/internal-api'
import { clearAccessToken, getAccessToken, setAccessToken } from '../auth/session'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: undefined as UserPrincipal | undefined,
    menus: [] as MenuNode[],
    initialized: false,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.user && getAccessToken()),
    hasPermission: (state) => (permission?: string) =>
      !permission || Boolean(state.user?.permissions.includes(permission)),
  },
  actions: {
    async login(username: string, password: string) {
      const token = await internalApi.request<TokenResponse>('/auth/login', {
        method: 'POST',
        body: { username, password },
        retryAuthentication: false,
      })
      setAccessToken(token.accessToken)
      this.user = token.user
      await this.loadMenus()
      this.initialized = true
    },
    async restore() {
      if (this.initialized) return
      try {
        if (!getAccessToken()) await refreshInternalSession()
        if (getAccessToken()) {
          this.user = await internalApi.get<UserPrincipal>('/auth/me')
          await this.loadMenus()
        }
      } catch {
        this.clearSession()
      } finally {
        this.initialized = true
      }
    },
    async loadMenus() {
      this.menus = await internalApi.get<MenuNode[]>('/navigation/menus')
    },
    async logout() {
      try {
        await internalApi.request<void>('/auth/logout', { method: 'POST', retryAuthentication: false })
      } finally {
        this.clearSession()
      }
    },
    clearSession() {
      clearAccessToken()
      this.user = undefined
      this.menus = []
    },
  },
})
