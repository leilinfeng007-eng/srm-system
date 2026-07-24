<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { portalSessionKey } from '../router'

const route = useRoute()
const router = useRouter()
const enterSkeleton = async () => {
  sessionStorage.setItem(portalSessionKey, 'true')
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/portal'
  await router.replace(redirect)
}
</script>

<template>
  <main class="portal-login">
    <section>
      <p class="eyebrow">SUPPLIER PORTAL · STAGE 0</p>
      <h1>SRM 供应商协同管理系统</h1>
      <h2>供应商门户</h2>
      <p>阶段 0 仅提供独立登录布局与九个门户导航骨架，不连接内部用户认证、菜单或令牌。</p>
      <button type="button" @click="enterSkeleton">进入门户骨架</button>
      <small>真实供应商登录、租户隔离及业务数据访问将在阶段 2 实现。</small>
    </section>
  </main>
</template>

<style scoped>
.portal-login { display: grid; place-items: center; min-height: 100vh; padding: 24px; background: radial-gradient(circle at 80% 15%, #b7e4dc 0, transparent 32%), linear-gradient(140deg, #eef7f5, #e5edf2); }
section { width: min(560px, 100%); padding: 52px; border: 1px solid rgba(255,255,255,.9); border-radius: 24px; background: rgba(255,255,255,.9); box-shadow: 0 30px 90px rgba(18,52,77,.14); }
.eyebrow { color: var(--srm-accent); font-weight: 800; letter-spacing: .12em; }
h1 { margin: 20px 0 6px; font-size: 34px; }
h2 { margin: 0 0 20px; color: var(--srm-muted); font-weight: 500; }
p { color: var(--srm-muted); line-height: 1.7; }
button { width: 100%; margin: 20px 0 14px; border: 0; border-radius: 10px; padding: 13px; background: var(--srm-accent); color: white; cursor: pointer; }
small { display: block; color: var(--srm-muted); line-height: 1.5; }
</style>
