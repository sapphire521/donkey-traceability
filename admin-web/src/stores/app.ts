import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const cockpitTheme = ref(false) // 监管大屏暗色主题开关

  function toggleSidebar() { sidebarCollapsed.value = !sidebarCollapsed.value }
  function setCockpit(on: boolean) { cockpitTheme.value = on }

  return { sidebarCollapsed, cockpitTheme, toggleSidebar, setCockpit }
})
