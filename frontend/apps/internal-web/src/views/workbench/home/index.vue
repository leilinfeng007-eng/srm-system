<script setup lang="ts">
import type { WorkbenchBaseline } from '@srm/shared-types'
import { computed, onMounted, ref } from 'vue'
import { getWorkbenchBaseline } from '../../../api/internal-api'
import { useAuthStore } from '../../../stores/auth'

const auth = useAuthStore()
const accessibleDomains = computed(() => auth.menus.length)
const baseline = ref<WorkbenchBaseline>()
const baselineError = ref('')

onMounted(async () => {
  try {
    baseline.value = await getWorkbenchBaseline()
  } catch {
    baselineError.value = '后端工程基线暂不可用，请检查服务状态。'
  }
})
</script>

<template>
  <section class="workbench-home">
    <p class="eyebrow">阶段 0 · 工程架构基线</p>
    <h1>欢迎，{{ auth.user?.displayName }}</h1>
    <p>当前账号已通过后端认证与权限校验，可访问 {{ accessibleDomains }} 个一级功能域。</p>
    <div class="baseline-card">
      <strong v-if="baseline">后端标准分层链路已联通</strong>
      <strong v-else-if="baselineError">后端标准分层链路检查失败</strong>
      <strong v-else>正在检查后端标准分层链路</strong>
      <span v-if="baseline">
        模块 {{ baseline.moduleCode }} 状态 {{ baseline.status }}，数据库连接正常。
      </span>
      <span v-else-if="baselineError">{{ baselineError }}</span>
      <span v-else>正在通过统一 API 客户端执行只读连通性检查。</span>
      <small>本链路只验证工程分层与数据库连接，不读取或保存业务数据。</small>
    </div>
  </section>
</template>

<style scoped>
.workbench-home { max-width: 920px; margin: 0 auto; padding: 48px; }
.eyebrow { color: var(--srm-accent); font-weight: 700; letter-spacing: .08em; }
h1 { margin: 12px 0; font-size: 36px; color: var(--srm-ink); }
p { color: var(--srm-muted); }
.baseline-card { display: grid; gap: 10px; margin-top: 36px; padding: 28px; border: 1px solid var(--srm-border); border-radius: 16px; background: white; }
.baseline-card span { color: var(--srm-muted); }
.baseline-card small { color: var(--srm-muted); }
</style>
