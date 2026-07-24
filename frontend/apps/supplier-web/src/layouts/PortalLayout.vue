<script setup lang="ts">
import { useRouter } from 'vue-router'
import portalManifest from '../../../../supplier-portal-manifest.json'
import { portalSessionKey } from '../router'

const router = useRouter()
const leave = async () => {
  sessionStorage.removeItem(portalSessionKey)
  await router.replace('/login')
}
</script>

<template>
  <div class="portal-shell">
    <header>
      <RouterLink class="brand" to="/portal"><strong>SRM</strong><span>供应商门户</span></RouterLink>
      <nav>
        <RouterLink v-for="entry in portalManifest.entries" :key="entry.menuCode" :to="entry.route">
          {{ entry.label }}
        </RouterLink>
      </nav>
      <button type="button" @click="leave">退出骨架</button>
    </header>
    <div class="notice">阶段 0 门户骨架：真实供应商身份认证与数据隔离将在阶段 2 实现。</div>
    <main><RouterView /></main>
  </div>
</template>

<style scoped>
.portal-shell { min-height: 100vh; }
header { display: flex; align-items: center; gap: 26px; min-height: 70px; padding: 12px 28px; background: #0c3542; color: white; }
.brand { display: flex; align-items: baseline; gap: 10px; text-decoration: none; white-space: nowrap; }
.brand strong { font-size: 22px; letter-spacing: .12em; }
.brand span { color: #b8d4d8; }
nav { display: flex; flex: 1; gap: 4px; overflow-x: auto; }
nav a { padding: 9px 11px; border-radius: 8px; color: #cfe2e5; font-size: 13px; text-decoration: none; white-space: nowrap; }
nav a.router-link-active { background: #0f766e; color: white; }
button { border: 1px solid rgba(255,255,255,.35); border-radius: 8px; padding: 8px 12px; background: transparent; color: white; cursor: pointer; white-space: nowrap; }
.notice { padding: 9px 28px; background: #fff4df; color: #76520e; font-size: 13px; }
main { padding: 30px; }
@media (max-width: 760px) { header { align-items: flex-start; flex-wrap: wrap; } nav { order: 3; flex-basis: 100%; } main { padding: 18px; } }
</style>
