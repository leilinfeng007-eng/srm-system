<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError } from '@srm/api-client'
import { useAuthStore } from '../../stores/auth'

const form = reactive({ username: '', password: '' })
const loading = ref(false)
const errorMessage = ref('')
const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const submit = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    await auth.login(form.username.trim(), form.password)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/workbench'
    await router.replace(redirect)
  } catch (error) {
    if (error instanceof ApiError) {
      errorMessage.value = `${error.message}（${error.traceId ?? '无跟踪号'}）`
    } else {
      console.error('SRM login failed before a request reached the server', String(error instanceof Error ? error.message : error))
      errorMessage.value = '登录失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="intro">
      <p>SRM · STAGE 0</p>
      <h1>让供应商协同<br />从清晰的边界开始</h1>
      <span>内部管理端工程架构基线</span>
    </section>
    <section class="login-card">
      <div><small>INTERNAL ACCESS</small><h2>登录内部管理端</h2><p>账号由启动环境变量初始化，仓库不保存固定密码。</p></div>
      <el-alert v-if="errorMessage" :title="errorMessage" type="error" :closable="false" show-icon />
      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名"><el-input v-model="form.username" autocomplete="username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" autocomplete="current-password" show-password /></el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" :disabled="!form.username || !form.password">登录</el-button>
      </el-form>
    </section>
  </main>
</template>

<style scoped>
.login-page { display: grid; grid-template-columns: minmax(320px, 1fr) minmax(380px, 520px); min-height: 100vh; background: linear-gradient(130deg, #0d2b3e 0%, #164e63 52%, #e9f0f2 52%); }
.intro { align-self: center; padding: 9vw; color: white; }
.intro p { color: #5eead4; font-weight: 800; letter-spacing: .16em; }
.intro h1 { margin: 22px 0; font-size: clamp(40px, 5vw, 68px); line-height: 1.15; }
.intro span { color: #c3d7df; }
.login-card { align-self: center; margin: 48px; padding: 40px; border: 1px solid rgba(255,255,255,.8); border-radius: 22px; background: rgba(255,255,255,.93); box-shadow: 0 30px 80px rgba(15,23,42,.18); }
.login-card small { color: var(--srm-accent); font-weight: 800; letter-spacing: .1em; }
.login-card h2 { margin: 10px 0; font-size: 28px; }
.login-card p { margin: 0 0 26px; color: var(--srm-muted); line-height: 1.6; }
.el-alert { margin-bottom: 18px; }
.el-button { width: 100%; height: 44px; }
@media (max-width: 820px) { .login-page { grid-template-columns: 1fr; background: #e9f0f2; } .intro { display: none; } .login-card { margin: 24px; } }
</style>
