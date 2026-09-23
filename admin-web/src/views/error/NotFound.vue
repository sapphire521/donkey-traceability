<template>
  <div class="nf">
    <div class="nf-code mono">404</div>
    <div class="nf-text">页面不存在或您无权访问</div>
    <el-button type="primary" @click="$router.push('/dashboard')">返回工作台</el-button>
  </div>
</template>

<script setup lang="ts"></script>

<style scoped>
.nf { height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 16px; background: var(--bg-page); }
.nf-code { font-size: 96px; font-weight: 800; color: var(--brand); letter-spacing: 4px; }
.nf-text { color: var(--text-3); font-size: 15px; }
</style>
