<template>
  <header class="navbar">
    <div class="nav-left">
      <el-icon class="toggle" @click="app.toggleSidebar()">
        <Fold v-if="!app.sidebarCollapsed" /><Expand v-else />
      </el-icon>
      <el-breadcrumb separator="/" class="crumb">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">工作台</el-breadcrumb-item>
        <el-breadcrumb-item v-if="crumbParent">{{ crumbParent }}</el-breadcrumb-item>
        <el-breadcrumb-item v-if="crumbCurrent">{{ crumbCurrent }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="nav-right">
      <el-tooltip content="全屏" placement="bottom">
        <div class="nav-chip" @click="toggleFullscreen"><el-icon class="nav-icon"><FullScreen /></el-icon></div>
      </el-tooltip>
      <el-tooltip :content="app.cockpitTheme ? '退出大屏' : '监管大屏'" placement="bottom">
        <div class="nav-chip" @click="goScreen"><el-icon class="nav-icon"><DataAnalysis /></el-icon></div>
      </el-tooltip>

      <div class="divider"></div>

      <el-dropdown trigger="click" @command="onCommand">
        <div class="user">
          <el-avatar :size="34" class="avatar">{{ avatarText }}</el-avatar>
          <div class="user-meta">
            <div class="user-name">{{ user.user?.realName }}</div>
            <div class="user-role">{{ user.user?.orgName }}</div>
          </div>
          <el-icon class="arrow"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="role">
              <el-icon><Postcard /></el-icon> 角色：{{ user.user?.roleName }}
            </el-dropdown-item>
            <el-dropdown-item command="logout" divided>
              <el-icon><SwitchButton /></el-icon> 退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'

const app = useAppStore()
const user = useUserStore()
const route = useRoute()
const router = useRouter()

const avatarText = computed(() => (user.user?.realName || '用').slice(0, 1))

const crumbParent = computed(() => {
  const menus = user.menus
  for (const m of menus) {
    if (m.children?.some(c => c.path === route.path)) return m.title
  }
  return ''
})
const crumbCurrent = computed(() => (route.meta.title as string) || '')

function toggleFullscreen() {
  if (!document.fullscreenElement) document.documentElement.requestFullscreen?.()
  else document.exitFullscreen?.()
}
function goScreen() {
  if (app.cockpitTheme) router.push('/dashboard')
  else router.push('/regulator/screen')
}
async function onCommand(cmd: string) {
  if (cmd === 'logout') {
    try { await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' }) } catch { return }
    user.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.navbar {
  height: 58px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 18px;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border);
  position: relative;
  z-index: 5;
}
.nav-left { display: flex; align-items: center; gap: 14px; }
.toggle {
  font-size: 19px; cursor: pointer; color: var(--text-2);
  width: 34px; height: 34px; border-radius: 9px;
  display: grid; place-items: center;
  transition: all 0.16s ease;
}
.toggle:hover { color: var(--brand); background: var(--brand-soft-2); }
.crumb { font-size: 13px; }

.nav-right { display: flex; align-items: center; gap: 8px; }
.nav-chip {
  width: 34px; height: 34px; border-radius: 9px;
  display: grid; place-items: center;
  cursor: pointer;
  transition: all 0.16s ease;
}
.nav-chip:hover { background: var(--brand-soft-2); }
.nav-icon { font-size: 18px; color: var(--text-2); transition: color 0.15s; }
.nav-chip:hover .nav-icon { color: var(--brand); }
.divider { width: 1px; height: 20px; background: var(--border); margin: 0 6px; }

.user { display: flex; align-items: center; gap: 9px; cursor: pointer; outline: none; padding: 4px 6px; border-radius: 10px; transition: background 0.16s ease; }
.user:hover { background: var(--bg-hover); }
.avatar {
  background: var(--grad-brand);
  color: #fff; font-weight: 600;
  box-shadow: 0 4px 10px rgba(22, 163, 74, 0.32);
}
.user-meta { line-height: 1.2; }
.user-name { font-size: 13px; font-weight: 600; color: var(--text-1); }
.user-role { font-size: 11px; color: var(--text-3); }
.arrow { font-size: 12px; color: var(--text-3); }
.theme-dark .navbar { background: rgba(27, 35, 54, 0.9); border-color: #27314a; }
.theme-dark .user-name { color: #f8fafc; }
.theme-dark .user:hover { background: #222c44; }
.theme-dark .nav-chip:hover { background: #222c44; }
.theme-dark .nav-chip:hover .nav-icon { color: #22c55e; }
.theme-dark .toggle:hover { background: #222c44; color: #22c55e; }
</style>
