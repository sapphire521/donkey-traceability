import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo, MenuNode } from '@/types'
import { http } from '@/utils/request'
import {
  saveSession, clearToken, getUser, getPermissions, getMenus, accessiblePaths
} from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>('')
  const user = ref<UserInfo | null>(getUser())
  const permissions = ref<string[]>(getPermissions())
  const menus = ref<MenuNode[]>(getMenus())
  const accessible = computed(() => accessiblePaths(menus.value))

  async function login(username: string, password: string) {
    const res = await http.post<{
      accessToken: string
      user: UserInfo
      permissions: string[]
      menus: MenuNode[]
    }>('/auth/login', { username, password })
    token.value = res.accessToken
    user.value = res.user
    permissions.value = res.permissions
    menus.value = res.menus
    saveSession(res.accessToken, res.user, res.permissions, res.menus)
    return res
  }

  function logout() {
    clearToken()
    token.value = ''
    user.value = null
    permissions.value = []
    menus.value = []
  }

  function hasPerm(code: string) {
    return permissions.value.includes(code)
  }

  return { token, user, permissions, menus, accessible, login, logout, hasPerm }
})
