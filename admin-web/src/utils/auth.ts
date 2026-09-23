// 鉴权与会话持久化
import type { UserInfo, MenuNode } from '@/types'

const TOKEN_KEY = 'dt_token'
const USER_KEY = 'dt_user'
const PERMS_KEY = 'dt_perms'
const MENUS_KEY = 'dt_menus'

export function getToken(): string { return localStorage.getItem(TOKEN_KEY) || '' }
export function setToken(t: string) { localStorage.setItem(TOKEN_KEY, t) }
export function clearToken() {
  ;[TOKEN_KEY, USER_KEY, PERMS_KEY, MENUS_KEY].forEach(k => localStorage.removeItem(k))
}

export function saveSession(token: string, user: UserInfo, perms: string[], menus: MenuNode[]) {
  setToken(token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  localStorage.setItem(PERMS_KEY, JSON.stringify(perms))
  localStorage.setItem(MENUS_KEY, JSON.stringify(menus))
}

export function getUser(): UserInfo | null {
  const s = localStorage.getItem(USER_KEY)
  return s ? (JSON.parse(s) as UserInfo) : null
}
export function getPermissions(): string[] {
  const s = localStorage.getItem(PERMS_KEY)
  return s ? (JSON.parse(s) as string[]) : []
}
export function getMenus(): MenuNode[] {
  const s = localStorage.getItem(MENUS_KEY)
  return s ? (JSON.parse(s) as MenuNode[]) : []
}

// 由菜单树展平出可访问路径集合（含父级前缀），用于路由守卫
export function accessiblePaths(menus: MenuNode[]): Set<string> {
  const set = new Set<string>()
  const walk = (nodes: MenuNode[], prefix = '') => {
    nodes.forEach(n => {
      set.add(n.path)
      const seg = (prefix + '/' + n.path).replace(/\/+/g, '/')
      set.add(seg)
      if (n.children) walk(n.children, n.path)
    })
  }
  walk(menus)
  return set
}
