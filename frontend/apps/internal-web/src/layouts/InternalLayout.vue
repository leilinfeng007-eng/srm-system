<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NavigationItem from '../components/NavigationItem.vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const breadcrumbs = computed(() => [route.meta.domainLabel, route.meta.title].filter(Boolean))

const signOut = async () => {
  await auth.logout()
  await router.replace('/login')
}
</script>

<template>
  <div class="shell">
    <aside>
      <div class="brand"><span>SRM</span><small>供应商协同管理系统</small></div>
      <el-scrollbar class="menu-scroll">
        <el-menu :default-active="route.path" router>
          <NavigationItem v-for="node in auth.menus" :key="node.menuCode" :node="node" />
        </el-menu>
      </el-scrollbar>
    </aside>
    <div class="workspace">
      <header>
        <el-breadcrumb separator="/">
          <el-breadcrumb-item v-for="item in breadcrumbs" :key="String(item)">{{ item }}</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="user-area">
          <span>{{ auth.user?.displayName }}</span>
          <small>{{ auth.user?.roles.join(' · ') }}</small>
          <el-button link type="primary" @click="signOut">退出登录</el-button>
        </div>
      </header>
      <main><RouterView /></main>
    </div>
  </div>
</template>

<style scoped>
.shell { display: grid; grid-template-columns: 248px 1fr; min-height: 100vh; }
aside { position: sticky; top: 0; height: 100vh; overflow: hidden; background: #12344d; color: white; }
.brand { display: grid; gap: 3px; height: 78px; padding: 17px 22px; border-bottom: 1px solid rgba(255,255,255,.1); }
.brand span { font-size: 22px; font-weight: 800; letter-spacing: .12em; }
.brand small { color: #b8cad6; }
.menu-scroll { height: calc(100vh - 78px); }
:deep(.el-menu) { border: 0; background: transparent; }
:deep(.el-menu-item), :deep(.el-sub-menu__title) { color: #d7e5ed; }
:deep(.el-menu-item:hover), :deep(.el-sub-menu__title:hover) { background: rgba(255,255,255,.08); }
:deep(.el-menu-item.is-active) { background: #0f766e; color: white; }
.workspace { min-width: 0; }
header { display: flex; align-items: center; justify-content: space-between; height: 78px; padding: 0 28px; border-bottom: 1px solid var(--srm-border); background: white; }
.user-area { display: flex; align-items: center; gap: 10px; }
.user-area small { color: var(--srm-muted); }
main { padding: 28px; }
@media (max-width: 900px) { .shell { grid-template-columns: 210px 1fr; } .user-area small { display: none; } }
</style>
