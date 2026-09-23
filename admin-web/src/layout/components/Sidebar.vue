<template>
  <aside class="sidebar">
    <div class="brand">
      <div class="brand-logo">
        <svg viewBox="0 0 32 32" width="26" height="26" aria-hidden="true">
          <rect x="3" y="3" width="26" height="26" rx="7" fill="rgba(255,255,255,0.14)" />
          <path d="M10 12h12M10 16h12M10 20h8" stroke="#fff" stroke-width="2" stroke-linecap="round" />
          <circle cx="22" cy="20" r="2" fill="#4ade80" />
        </svg>
      </div>
      <div v-show="!app.sidebarCollapsed" class="brand-text">
        <div class="brand-name">驴链 DonkeyTrace</div>
        <div class="brand-sub">驴肉火烧溯源管理端</div>
      </div>
    </div>

    <el-scrollbar class="menu-scroll">
      <el-menu
        :default-active="activeMenu"
        :collapse="app.sidebarCollapsed"
        :collapse-transition="false"
        router
        background-color="transparent"
        text-color="var(--side-text)"
        active-text-color="#ffffff"
        class="menu"
      >
        <template v-for="node in user.menus" :key="node.path">
          <el-sub-menu v-if="node.children && node.children.length" :index="node.path">
            <template #title>
              <el-icon v-if="node.icon"><component :is="node.icon" /></el-icon>
              <span>{{ node.title }}</span>
            </template>
            <el-menu-item v-for="c in node.children" :key="c.path" :index="c.path">
              <el-icon v-if="c.icon"><component :is="c.icon" /></el-icon>
              <template #title>{{ c.title }}</template>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="node.path">
            <el-icon v-if="node.icon"><component :is="node.icon" /></el-icon>
            <template #title>{{ node.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-scrollbar>

    <div v-show="!app.sidebarCollapsed" class="sidebar-foot">
      <span class="pulse"></span> Fabric 联盟链 正常
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const app = useAppStore()
const user = useUserStore()
const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style scoped>
.sidebar {
  width: 228px;
  flex-shrink: 0;
  background: var(--grad-deep);
  display: flex;
  flex-direction: column;
  transition: width 0.22s ease;
  position: relative;
  overflow: hidden;
}
/* 顶部氛围光 + 纹理，呼应参考图的渐变侧栏 */
.sidebar::before {
  content: '';
  position: absolute;
  top: -80px;
  right: -80px;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(74, 222, 128, 0.16), transparent 70%);
  pointer-events: none;
}
.sidebar::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.025) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.025) 1px, transparent 1px);
  background-size: 26px 26px;
  pointer-events: none;
}
.layout.collapsed .sidebar { width: 64px; }

.brand {
  height: 62px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}
.brand-logo {
  width: 38px; height: 38px;
  display: grid; place-items: center;
  border-radius: 11px;
  background: var(--grad-brand);
  box-shadow: 0 4px 16px rgba(22, 163, 74, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.25);
  flex-shrink: 0;
}
.brand-name { color: #fff; font-weight: 600; font-size: 15px; letter-spacing: 0.3px; }
.brand-sub { color: var(--side-text-dim); font-size: 11px; margin-top: 1px; }

.menu-scroll { flex: 1; position: relative; z-index: 1; }
.menu {
  border-right: none !important;
  background: transparent !important;
  padding: 10px;
}
.menu :deep(.el-menu-item),
.menu :deep(.el-sub-menu__title) {
  border-radius: 10px;
  margin: 3px 2px;
  height: 44px;
  line-height: 44px;
  transition: background 0.18s ease, color 0.18s ease;
}
.menu :deep(.el-menu-item:hover),
.menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.07) !important;
  color: #fff !important;
}
.menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) { color: #fff !important; }
.menu :deep(.el-menu-item.is-active) {
  background: var(--grad-brand) !important;
  color: #fff !important;
  box-shadow: 0 6px 18px rgba(22, 163, 74, 0.45), inset 0 1px 0 rgba(255, 255, 255, 0.22);
}
.menu :deep(.el-menu--inline .el-menu-item) {
  font-size: 13px;
  padding-left: 48px !important;
  height: 40px;
  line-height: 40px;
}
.menu :deep(.el-icon) { font-size: 17px; }

.sidebar-foot {
  padding: 13px 18px;
  font-size: 12px;
  color: var(--side-text-dim);
  border-top: 1px solid rgba(255, 255, 255, 0.07);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  position: relative;
  z-index: 1;
}
.pulse {
  width: 8px; height: 8px; border-radius: 50%;
  background: #4ade80;
  margin-right: 8px;
  box-shadow: 0 0 0 0 rgba(74, 222, 128, 0.5);
  animation: side-pulse 2.2s infinite;
}
@keyframes side-pulse {
  0% { box-shadow: 0 0 0 0 rgba(74, 222, 128, 0.45); }
  70% { box-shadow: 0 0 0 7px rgba(74, 222, 128, 0); }
  100% { box-shadow: 0 0 0 0 rgba(74, 222, 128, 0); }
}
</style>
