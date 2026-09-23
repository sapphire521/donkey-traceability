<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h3 class="page-title"><span class="bar"></span>联盟公告</h3>
        <p class="page-subtitle">平台运营方发布的联盟级通知（链下公示）</p>
      </div>
    </div>
    <div class="notice-list stagger">
      <el-card v-for="n in notices" :key="n.id" class="notice" shadow="hover">
        <div class="notice-head">
          <el-icon class="notice-icon"><Bell /></el-icon>
          <span class="notice-title">{{ n.title }}</span>
          <span class="notice-time mono">{{ n.time }}</span>
        </div>
        <div class="notice-content">{{ n.content }}</div>
        <div class="notice-pub">发布方：{{ n.publisher }}</div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { http } from '@/utils/request'

const notices = ref<any[]>([])
onMounted(async () => { notices.value = await http.get('/platform/notices') })
</script>

<style scoped>
.notice-list { display: grid; gap: 14px; max-width: 880px; }
.notice { border-radius: var(--radius); }
.notice-head { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.notice-icon { color: var(--brand); font-size: 18px; }
.notice-title { font-weight: 600; color: var(--text-1); font-size: 15px; }
.notice-time { margin-left: auto; color: var(--text-3); font-size: 12px; }
.notice-content { color: var(--text-2); font-size: 13px; line-height: 1.7; }
.notice-pub { margin-top: 10px; color: var(--text-3); font-size: 12px; }
</style>
