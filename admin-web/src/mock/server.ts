// Mock 服务端：解析 REST 风格请求，返回统一响应体；聚合溯源报告/批次树/统计。
import * as db from './db'
import type {
  ApiResult, RoleCode, MenuNode, LoginResult, UserInfo, TraceReport,
  Batch, Donkey, DonkeyEvent, BatchEvent, TransportRecord
} from '@/types'

import * as chain from './chain'

interface Ctx { params: Record<string, string>; query: Record<string, string>; body: any }
type Handler = (ctx: Ctx) => any

interface RouteDef { method: string; path: string; regex: RegExp; keys: string[]; handler: Handler }

const routes: RouteDef[] = []
function add(method: string, path: string, handler: Handler) {
  const keys: string[] = []
  const rx = path.replace(/:(\w+)/g, (_m, k: string) => { keys.push(k); return '([^/]+)' })
  routes.push({ method, path, regex: new RegExp('^' + rx + '$'), keys, handler })
}

function dispatch(method: string, path: string, body: any, query: Record<string, string>): ApiResult<any> {
  for (const r of routes) {
    if (r.method !== method) continue
    const m = r.regex.exec(path)
    if (!m) continue
    const params: Record<string, string> = {}
    r.keys.forEach((k, i) => (params[k] = decodeURIComponent(m[i + 1])))
    try {
      const data = r.handler({ params, query, body })
      return ok(data)
    } catch (e: any) {
      return fail(50001, e.message || '业务处理失败')
    }
  }
  return fail(40400, `未找到接口 ${method} ${path}`)
}

function ok(data: any): ApiResult<any> { return { code: 0, message: 'ok', data, traceId: 'mock-' + Math.random().toString(16).slice(2, 10) } }
function fail(code: number, message: string): ApiResult<any> { return { code, message, data: null, traceId: 'mock-' + Math.random().toString(16).slice(2, 10) } }

const delay = (ms: number) => new Promise(r => setTimeout(r, ms))

// ---------- 鉴权与角色 ----------
const roleName: Record<RoleCode, string> = {
  PLATFORM_ADMIN: '平台管理员', FARM_OPERATOR: '养殖员', FARM_MANAGER: '养殖场长',
  PLANT_OPERATOR: '加工员', PLANT_QC: '质检员', LOGISTICS_DRIVER: '司机',
  LOGISTICS_DISPATCHER: '调度员', SHOP_KEEPER: '门店店员', REGULATOR_OFFICER: '监管员',
  REGULATOR_ADMIN: '监管管理员', CONSUMER: '消费者'
}
const rolePerms: Record<RoleCode, string[]> = {
  PLATFORM_ADMIN: ['org:manage', 'donkey:write', 'batch:slaughter', 'batch:process', 'qc:report', 'transport:manage', 'shop:stock', 'code:bind', 'cert:manage', 'regulator:audit', 'regulator:recall', 'alert:view', 'stats:screen', 'trace:view', 'sys:manage'],
  FARM_OPERATOR: ['donkey:write', 'cert:manage', 'alert:view'],
  FARM_MANAGER: ['donkey:write', 'cert:manage', 'alert:view', 'trace:view'],
  PLANT_OPERATOR: ['batch:slaughter', 'batch:process', 'cert:manage', 'alert:view', 'trace:view'],
  PLANT_QC: ['qc:report', 'cert:manage', 'alert:view', 'trace:view'],
  LOGISTICS_DISPATCHER: ['transport:manage', 'alert:view', 'trace:view'],
  LOGISTICS_DRIVER: ['transport:manage', 'alert:view', 'trace:view'],
  SHOP_KEEPER: ['shop:stock', 'code:bind', 'cert:manage', 'alert:view', 'trace:view'],
  REGULATOR_OFFICER: ['regulator:audit', 'cert:manage', 'alert:view', 'stats:screen', 'trace:view'],
  REGULATOR_ADMIN: ['regulator:audit', 'regulator:recall', 'cert:manage', 'alert:view', 'stats:screen', 'trace:view'],
  CONSUMER: []
}

const menuTemplate: MenuNode[] = [
  { path: '/dashboard', title: '工作台', icon: 'Odometer' },
  { path: '/platform', title: '联盟管理', icon: 'Connection', permission: 'org:manage', children: [
    { path: '/platform/org', title: '组织管理' },
    { path: '/platform/user', title: '用户管理' },
    { path: '/platform/notice', title: '联盟公告' }
  ] },
  { path: '/farm', title: '养殖管理', icon: 'Food', permission: 'donkey:write', children: [
    { path: '/farm/donkey', title: '驴只档案' }
  ] },
  { path: '/plant', title: '屠宰加工', icon: 'Goods', permission: 'batch:slaughter', children: [
    { path: '/plant/slaughter', title: '屠宰批次' },
    { path: '/plant/process', title: '加工批次' },
    { path: '/plant/product', title: '成品批次' },
    { path: '/plant/qc', title: '出厂检验' }
  ] },
  { path: '/logistics', title: '冷链物流', icon: 'Van', permission: 'transport:manage', children: [
    { path: '/logistics/transport', title: '运输管理' }
  ] },
  { path: '/shop', title: '门店管理', icon: 'Shop', permission: 'shop:stock', children: [
    { path: '/shop/inventory', title: '库存管理' },
    { path: '/shop/output', title: '产出记录' },
    { path: '/shop/trace-code', title: '溯源码管理' }
  ] },
  { path: '/trace', title: '溯源引擎', icon: 'Share', permission: 'trace:view', children: [
    { path: '/trace/tree', title: '批次树可视化' },
    { path: '/trace/report', title: '溯源报告预览' },
    { path: '/trace/chain', title: '区块链账本' }
  ] },
  { path: '/cert', title: '证照管理', icon: 'Postcard', permission: 'cert:manage' },
  { path: '/regulator', title: '监管中心', icon: 'Monitor', permission: 'regulator:audit', children: [
    { path: '/regulator/inspection', title: '抽检登记' },
    { path: '/regulator/recall', title: '召回管理' },
    { path: '/regulator/screen', title: '数据大屏' }
  ] },
  { path: '/alert', title: '预警中心', icon: 'Warning', permission: 'alert:view' },
  { path: '/system', title: '系统管理', icon: 'Setting', permission: 'sys:manage', children: [
    { path: '/system/log', title: '操作日志' },
    { path: '/system/config', title: '参数配置' }
  ] }
]

function buildMenus(role: RoleCode): MenuNode[] {
  const perms = rolePerms[role] || []
  const filterNode = (n: MenuNode): MenuNode | null => {
    if (n.permission && !perms.includes(n.permission)) return null
    if (n.children) {
      const kids = n.children.map(filterNode).filter(Boolean) as MenuNode[]
      if (n.permission) return { ...n, children: kids.length ? kids : undefined }
      // 分组节点：若有可见子节点则保留
      return kids.length ? { ...n, children: kids } : null
    }
    return { ...n }
  }
  return menuTemplate.map(filterNode).filter(Boolean) as MenuNode[]
}

// ---------- 路由：认证 ----------
add('POST', '/auth/login', (ctx) => {
  const { username, password } = ctx.body || {}
  const u = db.users.find(x => x.username === username)
  if (!u) throw new Error('账号不存在')
  if (db.demoPasswords[username] !== password) throw new Error('密码错误（演示密码 123456）')
  const org = db.orgs.find(o => o.id === u.orgId)!
  const info: UserInfo = {
    userId: u.id, username: u.username, realName: u.realName, orgId: u.orgId,
    orgName: org.orgName, orgType: org.orgType, roleCode: u.roleCode,
    roleName: roleName[u.roleCode], mspId: org.mspId
  }
  const perms = rolePerms[u.roleCode] || []
  const menus = buildMenus(u.roleCode)
  const res: LoginResult = {
    accessToken: 'mock-jwt-' + u.username, refreshToken: 'mock-refresh-' + u.username,
    user: info, permissions: perms, menus
  }
  return res
})
add('POST', '/auth/refresh', () => ({ accessToken: 'mock-jwt-refreshed' }))
add('POST', '/auth/logout', () => ({}))
add('GET', '/auth/profile', (ctx) => {
  // 演示：从 query 取 username
  return ctx.query
})
add('PUT', '/auth/password', () => ({ ok: true }))

// ---------- 组织 / 用户 ----------
add('GET', '/orgs', (ctx) => paginate(db.orgs, ctx.query))
add('POST', '/orgs', (ctx) => {
  const o = { ...ctx.body, id: db.orgs.length + 100, status: 1, createTime: now() }
  chain.appendTx('JoinNetwork', { orgCode: o.orgCode, orgName: o.orgName, mspId: o.mspId }, 'OrdererMSP', '驴链平台运营方', o.createTime)
  db.orgs.push(o); return o
})
add('PUT', '/orgs/:id/status', (ctx) => {
  const o = db.orgs.find(x => x.id === +ctx.params.id); if (!o) throw new Error('组织不存在')
  o.status = +ctx.body.status; return o
})
add('GET', '/users', (ctx) => paginate(db.users, ctx.query))
add('POST', '/users', (ctx) => {
  const u = { ...ctx.body, id: db.users.length + 100, status: 1 }; db.users.push(u); return u
})
add('PUT', '/users/:id/status', (ctx) => {
  const u = db.users.find(x => x.id === +ctx.params.id); if (!u) throw new Error('用户不存在')
  u.status = +ctx.body.status; return u
})
const notices = [
  { id: 1, title: '关于启用驴肉火烧溯源新码规则的通知', content: '自 2026-09-01 起，溯源码统一采用 DT+14 位格式。', publisher: '平台运营方', time: '2026-09-01 10:00:00' },
  { id: 2, title: '冷链温控阈值调整说明', content: '鲜驴肉/卤制品运输温度阈值维持 0~4℃，请各物流商严格执行。', publisher: '平台运营方', time: '2026-08-20 15:30:00' }
]
add('GET', '/platform/notices', () => notices)

// ---------- 驴只 ----------
add('GET', '/donkeys', (ctx) => paginate(db.donkeys, ctx.query))
add('POST', '/donkeys', (ctx) => {
  const d = { ...ctx.body, id: db.donkeys.length + 100, status: 'RAISED', createTxid: '0x' + rnd(), photos: [], createTime: now() }
  chain.appendTx('CreateDonkey', { earTagId: d.earTagId, breed: d.breed, orgName: d.orgName, barnNo: d.barnNo }, 'OrgFarmMSP', d.orgName, d.createTime, d.createTxid)
  db.donkeys.unshift(d); return d
})
add('GET', '/donkeys/:earTagId', (ctx) => {
  const d = db.donkeys.find(x => x.earTagId === ctx.params.earTagId)
  if (!d) throw new Error('驴只不存在')
  const events = db.donkeyEvents.filter(e => e.earTagId === d.earTagId)
  return { ...d, events }
})
add('POST', '/donkeys/:earTagId/events', (ctx) => {
  const d = db.donkeys.find(x => x.earTagId === ctx.params.earTagId); if (!d) throw new Error('驴只不存在')
  const tx = chain.appendTx('AddDonkeyEvent', { earTagId: d.earTagId, eventType: ctx.body.eventType, content: ctx.body.content }, 'OrgFarmMSP', d.orgName, now())
  const e: DonkeyEvent = { id: db.donkeyEvents.length + 100, donkeyId: d.id, earTagId: d.earTagId, eventType: ctx.body.eventType, eventTime: now(), content: ctx.body.content, txId: tx.txId, blockNo: tx.blockNo }
  db.donkeyEvents.push(e); return e
})
add('POST', '/donkeys/:earTagId/quarantine', (ctx) => {
  const d = db.donkeys.find(x => x.earTagId === ctx.params.earTagId); if (!d) throw new Error('驴只不存在')
  d.status = 'QUARANTINED'
  const tx = chain.appendTx('QuarantineDonkey', { earTagId: d.earTagId, certNo: ctx.body.certNo }, 'OrgFarmMSP', d.orgName, now())
  const e: DonkeyEvent = { id: db.donkeyEvents.length + 100, donkeyId: d.id, earTagId: d.earTagId, eventType: 'QUARANTINE', eventTime: now(), content: '出栏检疫 ' + ctx.body.certNo, detailHash: ctx.body.certHash, txId: tx.txId }
  db.donkeyEvents.push(e); return d
})
add('POST', '/donkeys/import', () => ({ success: 8, fail: 0 }))
add('GET', '/donkeys/:earTagId/chain-history', (ctx) => {
  const d = db.donkeys.find(x => x.earTagId === ctx.params.earTagId); if (!d) throw new Error('驴只不存在')
  return chain.txsByOp(['CreateDonkey', 'AddDonkeyEvent', 'QuarantineDonkey'], 'earTagId', ctx.params.earTagId).reverse()
})

// ---------- 批次 ----------
add('GET', '/batches', (ctx) => paginate(db.batches, ctx.query))
add('GET', '/batches/:batchNo', (ctx) => {
  const b = db.batches.find(x => x.batchNo === ctx.params.batchNo); if (!b) throw new Error('批次不存在')
  return b
})
add('GET', '/batches/:batchNo/events', (ctx) =>
  db.batchEvents.filter(e => e.batchNo === ctx.params.batchNo).sort((a, b) => a.eventTime.localeCompare(b.eventTime)))
add('GET', '/batches/:batchNo/chain-history', (ctx) => [
  { txId: '0x' + rnd(), blockNo: 9300, time: now(), op: 'BatchCreated' },
  { txId: '0x' + rnd(), blockNo: 9370, time: now(), op: 'ReceiveBatch' }
])
function createBatch(body: any, type: string) {
  const b: Batch = {
    id: db.batches.length + 100, batchNo: body.batchNo, batchType: type as any,
    parentNos: body.parentNos || [], sourceEarTags: body.sourceEarTags || [],
    orgId: 4, orgName: '徐水漕河驴肉加工厂', holderOrgId: 4, holderOrgName: '徐水漕河驴肉加工厂',
    productName: body.productName, weightKg: body.weightKg, produceDate: body.produceDate,
    expireDate: body.expireDate, certHash: body.certHash, status: type === 'PRODUCT' ? 'IN_STORE' : 'CREATED',
    createTxid: '', createTime: now()
  }
  const tx = chain.appendTx('CreateBatch', { batchNo: b.batchNo, batchType: type, productName: b.productName, weightKg: b.weightKg, orgName: b.orgName }, 'OrgPlantMSP', b.orgName, b.createTime)
  b.createTxid = tx.txId
  db.batches.push(b); return b
}
add('POST', '/batches/slaughter', (ctx) => createBatch(ctx.body, 'SLAUGHTER'))
add('POST', '/batches/process', (ctx) => createBatch(ctx.body, 'PROCESS'))
add('POST', '/batches/product', (ctx) => createBatch(ctx.body, 'PRODUCT'))
add('POST', '/batches/expire-scan', () => {
  db.batches.forEach(b => { if (b.expireDate && b.expireDate < '2026-09-11') b.status = 'EXPIRED' }); return { scanned: db.batches.length }
})

// ---------- 物流 ----------
add('GET', '/transport/orders', (ctx) => paginate(db.transportOrders, ctx.query))
add('POST', '/transport/orders', (ctx) => {
  const o = { ...ctx.body, id: db.transportOrders.length + 100, status: 'IN_TRANSIT', txId: '0x' + rnd(), createTime: now() }
  chain.appendTx('CreateTransport', { transportNo: o.transportNo, batchNo: o.batchNo, vehicleNo: o.vehicleNo, fromOrgName: o.fromOrgName, toOrgName: o.toOrgName }, 'OrgLogisticsMSP', o.fromOrgName, o.createTime, o.txId)
  db.transportOrders.unshift(o); return o
})
add('POST', '/transport/orders/:no/records', (ctx) => {
  const o = db.transportOrders.find(x => x.transportNo === ctx.params.no); if (!o) throw new Error('运输单不存在')
  const recs = Array.isArray(ctx.body) ? ctx.body : [ctx.body]
  recs.forEach(r => db.transportRecords.push({ id: db.transportRecords.length + 100, orderId: o.id, ...r, abnormal: r.abnormal ? 1 : 0 }))
  return { added: recs.length }
})
add('POST', '/transport/orders/:no/finish', (ctx) => {
  const o = db.transportOrders.find(x => x.transportNo === ctx.params.no); if (!o) throw new Error('运输单不存在')
  o.status = 'RECEIVED'; o.fileUri = ctx.body.fileUri; o.fileHash = ctx.body.fileHash; return o
})
add('POST', '/transport/orders/:no/receive', (ctx) => {
  const o = db.transportOrders.find(x => x.transportNo === ctx.params.no); if (!o) throw new Error('运输单不存在')
  o.status = ctx.body.rejected ? 'REJECTED' : 'RECEIVED'; o.actualArriveTime = now()
  const tx = chain.appendTx('ReceiveBatch', { transportNo: o.transportNo, batchNo: o.batchNo, rejected: !!ctx.body.rejected }, 'OrgShopMSP', o.toOrgName, now())
  o.txId = tx.txId; return o
})

// ---------- 门店 ----------
add('GET', '/shop/inventory', () => db.inventory)
add('GET', '/shop/outputs', () => db.outputRecords)
add('POST', '/shop/outputs', (ctx) => {
  const o = { id: db.outputRecords.length + 100, ...ctx.body, createTime: now() }; db.outputRecords.unshift(o); return o
})
const codeChars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
function genCode() { let s = ''; for (let i = 0; i < 14; i++) s += codeChars[Math.floor(Math.random() * codeChars.length)]; return 'DT' + s }
add('POST', '/shop/trace-codes', (ctx) => {
  const n = ctx.body.count || 1; const arr = []
  for (let i = 0; i < n; i++) {
    const c = { id: db.traceCodes.length + 100 + i, code: genCode(), batchNo: ctx.body.batchNo, shopOrgName: ctx.body.shopOrgName, outputId: ctx.body.outputId, status: 'BOUND', bindTxid: '0x' + rnd(), printCount: 0, scanTimes: 0, createTime: now() }
    chain.appendTx('BindTraceCode', { code: c.code, batchNo: c.batchNo, shopOrgName: c.shopOrgName }, 'OrgShopMSP', c.shopOrgName, c.createTime, c.bindTxid)
    arr.push(c)
  }
  db.traceCodes.unshift(...arr); return arr
})
add('GET', '/shop/trace-codes', (ctx) => paginate(db.traceCodes, ctx.query))
add('GET', '/shop/trace-codes/:id/qrcode', (ctx) => {
  const c = db.traceCodes.find(x => x.id === +ctx.params.id); if (!c) throw new Error('码不存在')
  return { code: c.code, url: 'https://trace.donkeychain.cn/t/' + c.code }
})

// ---------- 溯源 ----------
add('GET', '/trace/codes/:code', (ctx) => buildTraceReport(ctx.params.code))
add('POST', '/trace/codes/:code/complaint', (ctx) => {
  db.complaints.unshift({ id: db.complaints.length + 100, code: ctx.params.code, ...ctx.body, status: 'PENDING', createTime: now() })
  chain.appendTx('AddComplaint', { code: ctx.params.code, category: ctx.body.category }, 'OrgPlatformMSP', '消费者投诉', now())
  return { ok: true }
})

// ---------- 区块链 ----------
add('GET', '/chain/stats', () => chain.chainStats())
add('GET', '/chain/verify', () => chain.verifyChain())
add('GET', '/chain/blocks', (ctx) => chain.listBlocks(ctx.query))
add('GET', '/chain/tx/:txId', (ctx) => {
  const t = chain.findTx(ctx.params.txId)
  if (!t) throw new Error('交易不存在')
  return t
})
add('GET', '/trace/batch-tree/:batchNo', (ctx) => buildBatchTree(ctx.params.batchNo))

// ---------- 证照 ----------
add('GET', '/certs', (ctx) => {
  const list = ctx.query.scope === 'all' ? db.certs : db.certs.filter(c => c.orgId === 4 || c.auditStatus)
  return paginate(list, ctx.query)
})
add('POST', '/certs', (ctx) => {
  const c = { id: db.certs.length + 100, ...ctx.body, status: 1, auditStatus: 'NONE' }
  chain.appendTx('AddCert', { certNo: c.certNo, certType: c.certType, orgName: c.orgName }, 'OrgPlatformMSP', c.orgName || '', now())
  db.certs.push(c); return c
})
add('GET', '/certs/expiring', () => db.certs.filter(c => c.expireDate <= '2026-12-31'))

// ---------- 监管 ----------
add('GET', '/regulator/dashboard', () => buildRegDashboard())
add('GET', '/regulator/inspections', (ctx) => paginate(db.inspections, ctx.query))
add('POST', '/regulator/inspections', (ctx) => {
  const i = { id: db.inspections.length + 100, ...ctx.body, createTime: now() }
  chain.appendTx('AddInspection', { batchNo: i.batchNo, agency: i.agency, result: i.result, orgName: i.orgName }, 'OrgRegulatorMSP', i.orgName || '', i.createTime)
  db.inspections.unshift(i); return i
})
add('GET', '/regulator/recalls', (ctx) => paginate(db.recalls, ctx.query))
add('POST', '/regulator/recalls', (ctx) => {
  const scope = { downstreamBatches: ['F-20260911-001'], shops: ['驴火·裕华路总店'], codes: 10, scanned: 35 }
  const r = { id: db.recalls.length + 100, batchNo: ctx.body.batchNo, reason: ctx.body.reason, scopeJson: JSON.stringify(scope), status: 'RECALLING', initiator: '监管员小郑', createTime: now() }
  chain.appendTx('CreateRecall', { batchNo: ctx.body.batchNo, reason: ctx.body.reason, scope }, 'OrgRegulatorMSP', '保定市畜禽产品质量安全监督中心', now())
  db.recalls.unshift(r); return r
})
add('GET', '/regulator/complaints', (ctx) => paginate(db.complaints, ctx.query))

// ---------- 预警 / 统计 / 系统 ----------
add('GET', '/alerts', (ctx) => paginate(db.alerts, ctx.query))
add('PUT', '/alerts/:id/handle', (ctx) => {
  const a = db.alerts.find(x => x.id === +ctx.params.id); if (!a) throw new Error('预警不存在')
  a.status = ctx.body.status || 'RESOLVED'; a.handler = ctx.body.handler || '当前用户'; a.handleNote = ctx.body.handleNote; a.handleTime = now(); return a
})
add('GET', '/stats/overview', () => buildOverview())
add('GET', '/system/logs', (ctx) => paginate(db.operationLogs, ctx.query))
add('GET', '/system/configs', () => db.configs)
add('PUT', '/system/configs/:key', (ctx) => {
  const c = db.configs.find(x => x.configKey === ctx.params.key); if (!c) throw new Error('参数不存在')
  c.configValue = ctx.body.configValue; return c
})

// ---------- 聚合：溯源报告 ----------
function findBatch(no: string) { return db.batches.find(b => b.batchNo === no) }
function ancestors(no: string): Batch[] {
  const chain: Batch[] = []
  const walk = (cur: string) => { const b = findBatch(cur); if (!b) return; chain.push(b); b.parentNos.forEach(walk) }
  walk(no); return chain
}
function buildTraceReport(codeStr: string): TraceReport {
  const tc = db.traceCodes.find(c => c.code === codeStr)
  if (!tc) return { code: codeStr, verifyStatus: 'FAILED', product: { name: '—', batchNo: '—', produceDate: '—', shopName: '—' }, timeline: [], blockchain: { channel: chain.CHANNEL, eventCount: 0, latestBlock: Math.max(0, chain.chainStats().totalBlocks - 1), latestTxId: '-' } }
  if (tc.status === 'DESTROYED') return { code: codeStr, verifyStatus: 'DESTROYED', product: { name: '—', batchNo: '—', produceDate: '—', shopName: '—' }, timeline: [], blockchain: { channel: chain.CHANNEL, eventCount: 0, latestBlock: Math.max(0, chain.chainStats().totalBlocks - 1), latestTxId: '-' } }
  const product = findBatch(tc.batchNo)!
  const chainBatches = ancestors(tc.batchNo)
  const root = chainBatches[chainBatches.length - 1]
  const sourceDonkeys: Donkey[] = root.sourceEarTags.map(t => db.donkeys.find(d => d.earTagId === t)).filter(Boolean) as Donkey[]
  const timeline: TraceReport['timeline'] = []
  sourceDonkeys.forEach(d => {
    timeline.push({ stage: 'FARM', title: '养殖档案', orgName: d.orgName, time: d.createTime.slice(0, 10), summary: `${d.breed} 入栏建档 耳标${d.earTagId}`, txId: d.createTxid, photos: d.photos })
    const q = db.donkeyEvents.find(e => e.earTagId === d.earTagId && e.eventType === 'QUARANTINE')
    if (q) timeline.push({ stage: 'QUARANTINE', title: '出栏检疫', orgName: d.orgName, time: q.eventTime.slice(0, 10), summary: q.content, txId: q.txId })
  })
  chainBatches.forEach(b => {
    const evs = db.batchEvents.filter(e => e.batchNo === b.batchNo).sort((a, c) => a.eventTime.localeCompare(c.eventTime))
    evs.forEach(e => {
      const stageMap: Record<string, string> = { BATCH_CREATE: b.batchType, QC_PASS: 'QC', TRANSFER: 'TRANSPORT', RECEIVE: 'SHOP', BIND_CODE: 'SHOP' }
      timeline.push({ stage: stageMap[e.eventType] || 'PROCESS', title: titleOf(e.eventType), orgName: e.orgName, time: e.eventTime.slice(0, 10), summary: e.summary, txId: e.txId })
    })
  })
  const qc = db.batchEvents.find(e => e.batchNo === product.batchNo && e.eventType === 'QC_PASS')
  const transport = db.transportOrders.find(o => o.batchNo === product.batchNo)
  const recs: TransportRecord[] = transport ? db.transportRecords.filter(r => r.orderId === transport.id) : []
  const tempRange = recs.length ? `${Math.min(...recs.map(r => r.temperature))}~${Math.max(...recs.map(r => r.temperature))}℃` : '0~4℃'
  if (transport) timeline.push({ stage: 'TRANSPORT', title: '冷链运输', orgName: transport.fromOrgName, time: transport.departTime.slice(0, 10), summary: `全程${tempRange} 符合要求 车牌${transport.vehicleNo}`, txId: transport.txId })
  timeline.push({ stage: 'SHOP', title: '门店制作', orgName: product.holderOrgName, time: product.produceDate, summary: `${product.produceDate} 第3炉出锅` })
  const eventCount = timeline.filter(t => t.txId).length
  const cs = chain.chainStats()
  const latest = chain.listBlocks({ page: '1', size: '1' }).records[0]
  return {
    code: codeStr, verifyStatus: 'PASSED',
    product: { name: '驴肉火烧·卤制驴肉', batchNo: product.batchNo, produceDate: product.produceDate, shopName: product.holderOrgName, expireDate: product.expireDate },
    timeline, blockchain: { channel: cs.channel, eventCount, latestBlock: latest ? latest.blockNo : 0, latestTxId: latest ? latest.hash : '-' }
  }
}
function titleOf(t: string) {
  return { BATCH_CREATE: '批次创建', QC_PASS: '出厂检验', TRANSFER: '冷链发运', RECEIVE: '门店签收', BIND_CODE: '溯源码绑定' }[t] || t
}

// ---------- 聚合：批次树 ----------
function buildBatchTree(no: string) {
  const toNode = (b: Batch): any => ({
    id: b.batchNo, name: `${b.batchNo}\n${b.productName}`, type: b.batchType, status: b.status, weight: b.weightKg,
    children: []
  })
  const visited = new Set<string>()
  const build = (cur: string): any => {
    const b = findBatch(cur); if (!b || visited.has(cur)) return null; visited.add(cur)
    const node = toNode(b)
    db.batches.filter(x => x.parentNos.includes(cur)).forEach(c => { const cn = build(c.batchNo); if (cn) node.children.push(cn) })
    if (b.batchType === 'SLAUGHTER') b.sourceEarTags.forEach(t => node.children.push({ id: t, name: t, type: 'DONKEY', status: 'RAISED', leaf: true }))
    return node
  }
  return build(no) || { id: no, name: no, type: 'UNKNOWN', status: 'CREATED', children: [] }
}

// ---------- 聚合：统计 ----------
function buildOverview() {
  return {
    cards: [
      { key: 'org', label: '入网组织', value: db.orgs.length, unit: '个', icon: 'Connection', color: 'var(--c-info)' },
      { key: 'donkey', label: '驴只建档', value: db.donkeys.length, unit: '头', icon: 'Goods', color: 'var(--brand)' },
      { key: 'batch', label: '在链批次', value: db.batches.length, unit: '批', icon: 'Box', color: 'var(--c-purple)' },
      { key: 'code', label: '溯源码发放', value: db.traceCodes.length, unit: '枚', icon: 'Postcard', color: 'var(--c-warning)' },
      { key: 'event', label: '上链事件', value: db.batchEvents.length + db.donkeyEvents.length, unit: '次', icon: 'Link', color: 'var(--brand)' },
      { key: 'scan', label: '累计扫码', value: db.traceCodes.reduce((s, c) => s + c.scanTimes, 0), unit: '次', icon: 'View', color: 'var(--c-info)' }
    ],
    trends: Array.from({ length: 30 }, (_, i) => ({ date: `09-${String(i + 1).padStart(2, '0')}`, events: 20 + Math.round(40 * Math.abs(Math.sin(i / 3))), scans: 10 + Math.round(30 * Math.abs(Math.cos(i / 4))) })),
    stageDist: [
      { name: '养殖', value: 30 }, { name: '检疫', value: 12 }, { name: '屠宰', value: 18 },
      { name: '加工', value: 20 }, { name: '物流', value: 10 }, { name: '门店', value: 10 }
    ],
    quality: { alerts: db.alerts.filter(a => a.status === 'OPEN').length, complaints: db.complaints.length, passRate: 98.6 }
  }
}
function buildRegDashboard() {
  const o = buildOverview()
  return {
    ...o,
    regionDist: [
      { name: '保定', value: 14 }, { name: '石家庄', value: 6 }, { name: '沧州', value: 4 },
      { name: '衡水', value: 3 }, { name: '廊坊', value: 3 }
    ],
    certExpiring: db.certs.filter(c => c.expireDate <= '2026-12-31').length,
    recallActive: db.recalls.filter(r => r.status === 'RECALLING').length
  }
}

// ---------- 工具 ----------
function now() { return '2026-09-11 ' + String(new Date().getHours()).padStart(2, '0') + ':' + String(new Date().getMinutes()).padStart(2, '0') + ':00' }
function rnd() { return Math.random().toString(16).slice(2, 10) }
function paginate<T>(list: T[], query: Record<string, string>) {
  const page = +(query.page || 1), size = +(query.size || 20)
  const keyword = (query.keyword || query.q || '').trim().toLowerCase()
  let data = list
  if (keyword && (list as any[]).length && typeof (list[0] as any) === 'object') {
    data = (list as any[]).filter(x => JSON.stringify(x).toLowerCase().includes(keyword)) as T[]
  }
  const total = data.length
  const start = (page - 1) * size
  return { total, records: data.slice(start, start + size) }
}

export async function mockServer(method: string, path: string, body: any, query: Record<string, string>): Promise<ApiResult<any>> {
  const protectedPrefix = !['/auth/login', '/auth/refresh', '/auth/logout', '/trace/codes'].some(p => path.startsWith(p) || (method === 'GET' && path.startsWith('/trace/codes')))
  // 简化处理：公开接口仅有登录与溯源查询
  await delay(120 + Math.random() * 180)
  return dispatch(method, path, body, query)
}
