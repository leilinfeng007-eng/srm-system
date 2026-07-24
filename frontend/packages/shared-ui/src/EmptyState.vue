<script setup lang="ts">
withDefaults(defineProps<{ title?: string; description?: string }>(), {
  title: '暂无内容',
  description: '当前模块尚未提供可展示的数据。',
})
</script>

<template>
  <div class="empty-state"><strong>{{ title }}</strong><span>{{ description }}</span></div>
</template>

<style scoped>
.empty-state { display: grid; gap: 8px; place-items: center; padding: 40px; color: var(--srm-muted); }
</style>
