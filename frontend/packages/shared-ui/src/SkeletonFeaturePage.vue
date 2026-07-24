<script setup lang="ts">
defineProps<{
  domainLabel: string
  featureLabel: string
  route: string
  permission: string
  menuCode: string
  phase: number
}>()
</script>

<template>
  <section class="skeleton-feature">
    <div class="heading">
      <p class="eyebrow">{{ domainLabel }}</p>
      <h1>{{ featureLabel }}</h1>
      <span>业务功能待开发</span>
    </div>
    <p class="boundary">本页为阶段 0 功能骨架，仅验证路由、菜单与权限边界，不请求或保存业务数据。</p>
    <dl>
      <div><dt>路由</dt><dd>{{ route }}</dd></div>
      <div><dt>权限码</dt><dd>{{ permission }}</dd></div>
      <div><dt>菜单编码</dt><dd>{{ menuCode }}</dd></div>
      <div><dt>计划阶段</dt><dd>阶段 {{ phase }}</dd></div>
    </dl>
  </section>
</template>

<style scoped>
.skeleton-feature { min-height: 420px; padding: 36px; border: 1px solid var(--srm-border); border-radius: 18px; background: #fff; box-shadow: 0 18px 48px rgba(23, 43, 77, .06); }
.heading { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; }
.eyebrow { width: 100%; margin: 0; color: var(--srm-accent); font-weight: 700; }
h1 { margin: 0; color: var(--srm-ink); font-size: 28px; }
.heading span { padding: 5px 10px; border-radius: 999px; background: #fff4df; color: #8a5a00; font-size: 13px; }
.boundary { max-width: 680px; margin: 22px 0 32px; color: var(--srm-muted); line-height: 1.7; }
dl { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; max-width: 780px; margin: 0; }
dl div { padding: 16px; border: 1px solid var(--srm-border); border-radius: 12px; background: #f8fafc; }
dt { margin-bottom: 6px; color: var(--srm-muted); font-size: 12px; }
dd { margin: 0; color: var(--srm-ink); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; overflow-wrap: anywhere; }
@media (max-width: 680px) { dl { grid-template-columns: 1fr; } .skeleton-feature { padding: 24px; } }
</style>
