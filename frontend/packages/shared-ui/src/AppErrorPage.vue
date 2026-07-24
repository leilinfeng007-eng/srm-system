<script setup lang="ts">
withDefaults(defineProps<{ code?: string; title: string; description: string }>(), { code: 'ERROR' })
</script>

<template>
  <main class="error-page">
    <strong>{{ code }}</strong>
    <h1>{{ title }}</h1>
    <p>{{ description }}</p>
    <slot />
  </main>
</template>

<style scoped>
.error-page { display: grid; place-content: center; min-height: 70vh; padding: 32px; text-align: center; }
strong { color: var(--srm-accent); font-size: 48px; letter-spacing: .06em; }
h1 { margin: 12px 0 8px; color: var(--srm-ink); }
p { max-width: 520px; color: var(--srm-muted); }
</style>
