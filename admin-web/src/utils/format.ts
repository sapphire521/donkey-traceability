// 状态枚举 → 中文标签 + Element Plus tag 类型
type Map = Record<string, [string, '' | 'success' | 'info' | 'warning' | 'danger']>

export const batchStatusMap: Map = {
  CREATED: ['待发运', 'info'], IN_TRANSIT: ['在途', 'warning'], RECEIVED: ['已签收', ''],
  IN_STORE: ['在库', 'success'], SOLD: ['已售罄', 'success'], RECALLED: ['召回中', 'danger'], EXPIRED: ['已过期', 'info']
}
export const donkeyStatusMap: Map = {
  RAISED: ['饲养中', 'success'], QUARANTINED: ['已检疫', 'warning'], SLAUGHTERED: ['已屠宰', 'info']
}
export const codeStatusMap: Map = {
  UNUSED: ['未打印', 'info'], BOUND: ['已绑定', 'success'], SOLD: ['已售出', 'success'], DESTROYED: ['已作废', 'danger']
}
export const alertLevelMap: Map = {
  INFO: ['提示', 'info'], WARN: ['警告', 'warning'], CRITICAL: ['严重', 'danger']
}
export const alertStatusMap: Map = {
  OPEN: ['待处理', 'danger'], RESOLVED: ['已处理', 'success'], FALSE_POSITIVE: ['误报', 'info']
}
export const batchTypeMap: Map = {
  SLAUGHTER: ['屠宰批次', ''], PROCESS: ['加工批次', 'warning'], PRODUCT: ['成品批次', 'success']
}
export const inspectionResultMap: Map = { PASS: ['合格', 'success'], FAIL: ['不合格', 'danger'] }
export const certAuditMap: Map = { NONE: ['待审核', 'info'], PASSED: ['已通过', 'success'], REJECTED: ['已驳回', 'danger'] }
export const complaintStatusMap: Map = { PENDING: ['待处理', 'danger'], PROCESSING: ['处理中', 'warning'], DONE: ['已完成', 'success'] }

export function tagOf(map: Map, key: string) {
  return map[key] || [key, 'info' as const]
}

export function fmtTime(s?: string) {
  if (!s) return '-'
  return s.replace('T', ' ').slice(0, 16)
}
