<template>
  <div class="layout" :class="{ collapsed: app.sidebarCollapsed, 'theme-dark': app.cockpitTheme }">
    <Sidebar />
    <div class="layout-main">
      <Navbar />
      <div class="layout-content" :class="{ 'layout-content-light': !app.cockpitTheme }">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAppStore } from '@/stores/app'
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'

const app = useAppStore()
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}
.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--bg-page);
}
.layout-content {
  flex: 1;
  overflow: auto;
  position: relative;
}
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.fade-slide-enter-from { opacity: 0; transform: translateY(8px); }
.fade-slide-leave-to { opacity: 0; transform: translateY(-8px); }
/* 驾驶舱暗色时内容区透明，由页面自身铺满 */
.layout.theme-dark .layout-main { background: transparent; }
.layout.theme-dark .layout-content { overflow: hidden; }
</style>
