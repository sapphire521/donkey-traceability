// 请求层：默认走真实后端 (VITE_API_MODE=server)；设 VITE_API_MODE=mock 可回退到内存演示。
// 后端响应体与 mock 保持一致: { code, message, data, traceId }
import { mockServer } from '@/mock/server'
import { getToken } from './auth'
import { ElMessage } from 'element-plus'

interface ReqConfig {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
  params?: Record<string, any>
}

const PUBLIC_PREFIXES = ['/auth/login', '/auth/refresh', '/auth/logout', '/trace/codes']
const API_MODE = (import.meta.env.VITE_API_MODE as string) || 'server'
// 本地开发默认走 Vite 代理 /api；线上构建时通过 VITE_API_BASE 指向真实后端（如 https://xxx.onrender.com/api）
const BASE = (import.meta.env.VITE_API_BASE as string) || '/api'

function buildQuery(params: Record<string, any> | undefined): string {
  const query: string[] = []
  Object.entries(params || {}).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') query.push(encodeURIComponent(k) + '=' + encodeURIComponent(String(v)))
  })
  return query.length ? '?' + query.join('&') : ''
}

async function serverRequest<T>(config: ReqConfig): Promise<T> {
  const method = (config.method || 'GET').toUpperCase()
  const url = BASE + config.url + (method === 'GET' ? buildQuery(config.params) : '')
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  const token = getToken()
  if (token) headers.Authorization = 'Bearer ' + token

  const resp = await fetch(url, {
    method,
    headers,
    body: method === 'GET' ? undefined : JSON.stringify(config.data ?? {})
  })
  if (resp.status === 401) {
    ElMessage.error('登录状态已失效，请重新登录')
    throw new Error('未授权')
  }
  const res = await resp.json()
  if (res.code !== 0) {
    ElMessage.error(res.message || '请求失败')
    throw new Error(res.message || '请求失败')
  }
  return res.data as T
}

async function mockRequest<T>(config: ReqConfig): Promise<T> {
  const method = (config.method || 'GET').toUpperCase()
  let url = config.url
  const query: Record<string, string> = {}
  Object.entries(config.params || {}).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') query[k] = String(v) })
  const qi = url.indexOf('?')
  if (qi >= 0) {
    const qs = url.slice(qi + 1)
    url = url.slice(0, qi)
    qs.split('&').forEach(p => {
      const idx = p.indexOf('=')
      const k = idx >= 0 ? p.slice(0, idx) : p
      const v = idx >= 0 ? p.slice(idx + 1) : ''
      if (k) query[decodeURIComponent(k)] = decodeURIComponent(v)
    })
  }
  const res = await mockServer(method, url, config.data, query)
  if (res.code !== 0) {
    ElMessage.error(res.message || '请求失败')
    throw new Error(res.message || '请求失败')
  }
  return res.data as T
}

export async function request<T = any>(config: ReqConfig): Promise<T> {
  const method = (config.method || 'GET').toUpperCase()
  const bare = config.url.split('?')[0]
  const isPublic = PUBLIC_PREFIXES.some(p => bare.startsWith(p))
  if (!isPublic && !getToken()) {
    ElMessage.error('登录状态已失效，请重新登录')
    throw new Error('未登录')
  }
  if (API_MODE === 'server') {
    return serverRequest<T>(config)
  }
  return mockRequest<T>(config)
}

// 便捷方法
export const http = {
  get: <T = any>(url: string, params?: Record<string, any>) => request<T>({ url, method: 'GET', params }),
  post: <T = any>(url: string, data?: any) => request<T>({ url, method: 'POST', data }),
  put: <T = any>(url: string, data?: any) => request<T>({ url, method: 'PUT', data }),
  delete: <T = any>(url: string, data?: any) => request<T>({ url, method: 'DELETE', data })
}
