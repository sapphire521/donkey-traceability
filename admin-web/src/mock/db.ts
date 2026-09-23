// 内存演示数据库（仅用于前端独立运行演示，对接真实后端时由 API 替换）
import type {
  Org, User, Donkey, DonkeyEvent, Batch, BatchEvent, TransportOrder,
  TransportRecord, InventoryItem, OutputRecord, TraceCode, Alert, Cert,
  Inspection, Recall, Complaint, OperationLog, ConfigItem, ScanLog
} from '@/types'

const TODAY = '2026-09-11'
let _id = 1
const nid = () => _id++

// ---------- 组织 ----------
export const orgs: Org[] = [
  { id: nid(), orgCode: 'PLAT001', orgName: '驴链平台运营方', orgType: 'PLATFORM', mspId: 'OrgPlatformMSP', contact: '王运营', phone: '13800000001', status: 1, createTime: '2026-01-02 09:00:00' },
  { id: nid(), orgCode: 'FARM001', orgName: '唐县振海驴业养殖场', orgType: 'FARM', mspId: 'OrgFarmMSP', contact: '李振海', phone: '13800000002', status: 1, createTime: '2026-01-05 10:00:00' },
  { id: nid(), orgCode: 'FARM002', orgName: '易县兴牧驴业', orgType: 'FARM', mspId: 'OrgFarmMSP', contact: '赵兴牧', phone: '13800000003', status: 1, createTime: '2026-02-11 10:00:00' },
  { id: nid(), orgCode: 'PLANT001', orgName: '徐水漕河驴肉加工厂', orgType: 'PLANT', mspId: 'OrgPlantMSP', contact: '孙厂长', phone: '13800000004', status: 1, createTime: '2026-01-08 10:00:00' },
  { id: nid(), orgCode: 'LOGI001', orgName: '保运冷链物流', orgType: 'LOGISTICS', mspId: 'OrgLogisticsMSP', contact: '周调度', phone: '13800000005', status: 1, createTime: '2026-01-12 10:00:00' },
  { id: nid(), orgCode: 'SHOP001', orgName: '驴火·裕华路总店', orgType: 'SHOP', mspId: 'OrgShopMSP', contact: '钱店长', phone: '13800000006', status: 1, createTime: '2026-01-20 10:00:00' },
  { id: nid(), orgCode: 'SHOP002', orgName: '漕河驴火·竞秀店', orgType: 'SHOP', mspId: 'OrgShopMSP', contact: '孙店长', phone: '13800000007', status: 1, createTime: '2026-02-01 10:00:00' },
  { id: nid(), orgCode: 'REGU001', orgName: '保定市畜禽产品质量安全监督中心', orgType: 'REGULATOR', mspId: 'OrgRegulatorMSP', contact: '监管员', phone: '13800000008', status: 1, createTime: '2026-01-01 10:00:00' }
]
const org = (code: string) => orgs.find(o => o.orgCode === code)!

// ---------- 用户（演示账号密码均为 123456） ----------
export const users: User[] = [
  { id: nid(), orgId: org('PLAT001').id, username: 'admin', realName: '平台管理员', phone: '13800000001', roleCode: 'PLATFORM_ADMIN', status: 1, lastLoginTime: TODAY + ' 08:30:12' },
  { id: nid(), orgId: org('FARM001').id, username: 'farmer', realName: '养殖员小李', phone: '13800000002', roleCode: 'FARM_OPERATOR', status: 1, lastLoginTime: TODAY + ' 07:50:02' },
  { id: nid(), orgId: org('PLANT001').id, username: 'plant', realName: '加工员小孙', phone: '13800000004', roleCode: 'PLANT_OPERATOR', status: 1, lastLoginTime: TODAY + ' 08:10:33' },
  { id: nid(), orgId: org('PLANT001').id, username: 'qc', realName: '质检员小吴', phone: '13800000004', roleCode: 'PLANT_QC', status: 1, lastLoginTime: TODAY + ' 08:12:09' },
  { id: nid(), orgId: org('LOGI001').id, username: 'logi', realName: '调度员小周', phone: '13800000005', roleCode: 'LOGISTICS_DISPATCHER', status: 1, lastLoginTime: TODAY + ' 08:05:41' },
  { id: nid(), orgId: org('SHOP001').id, username: 'shop', realName: '店员小钱', phone: '13800000006', roleCode: 'SHOP_KEEPER', status: 1, lastLoginTime: TODAY + ' 09:01:55' },
  { id: nid(), orgId: org('REGU001').id, username: 'reg', realName: '监管员小郑', phone: '13800000008', roleCode: 'REGULATOR_OFFICER', status: 1, lastLoginTime: TODAY + ' 08:40:18' }
]
// 演示密码（仅 mock）
export const demoPasswords: Record<string, string> = Object.fromEntries(
  users.map(u => [u.username, '123456'])
)

// ---------- 驴只 ----------
const breeds = ['德州驴', '关中驴', '新疆驴', '杂交']
export const donkeys: Donkey[] = []
const donkeyStatuses: Donkey['status'][] = ['RAISED', 'QUARANTINED', 'SLAUGHTERED']
for (let i = 0; i < 14; i++) {
  const code = 301 + i
  donkeys.push({
    id: nid(),
    earTagId: `E1309-2025-000${code}`,
    breed: breeds[i % breeds.length],
    gender: i % 2 === 0 ? 'M' : 'F',
    birthDate: `2025-0${(i % 9) + 1}-1${i % 9}`,
    orgId: i % 3 === 0 ? org('FARM002').id : org('FARM001').id,
    orgName: i % 3 === 0 ? org('FARM002').orgName : org('FARM001').orgName,
    barnNo: `A-${String.fromCharCode(65 + (i % 4))}${(i % 5) + 1}`,
    status: donkeyStatuses[i % 3],
    createTxid: `0x${Math.random().toString(16).slice(2, 10)}a${i}`,
    photos: ['minio://donkey/d1.jpg', 'minio://donkey/d2.jpg'],
    createTime: `2025-0${(i % 9) + 1}-1${i % 9} 09:00:00`
  })
}

// ---------- 驴只事件 ----------
export const donkeyEvents: DonkeyEvent[] = []
donkeys.forEach((d, idx) => {
  donkeyEvents.push({ id: nid(), donkeyId: d.id, earTagId: d.earTagId, eventType: 'FEED', eventTime: '2026-03-02 08:10:00', content: '饲喂：牧草+豆粕混合料 2.5kg', txId: `0xfeed${idx}`, blockNo: 9000 + idx })
  donkeyEvents.push({ id: nid(), donkeyId: d.id, earTagId: d.earTagId, eventType: 'IMMUNIZE', eventTime: '2026-05-12 10:30:00', content: '免疫：破伤风类毒素 批号TT2026-03', txId: `0ximm${idx}`, blockNo: 9100 + idx })
  if (d.status !== 'RAISED') {
    donkeyEvents.push({ id: nid(), donkeyId: d.id, earTagId: d.earTagId, eventType: 'QUARANTINE', eventTime: '2026-09-08 14:00:00', content: '出栏检疫合格 动物A证 No.1309263' + idx, detailHash: 'sha256:' + Math.random().toString(16).slice(2, 10), txId: `0xq${idx}`, blockNo: 9200 + idx })
  }
})

// ---------- 批次树 ----------
export const batches: Batch[] = []
export const batchEvents: BatchEvent[] = []

function addBatch(b: Omit<Batch, 'id' | 'createTime'>): Batch {
  const nb: Batch = { ...b, id: nid(), createTime: b.produceDate + ' 09:00:00' }
  batches.push(nb)
  return nb
}
const s1 = addBatch({ batchNo: 'S-20260911-001', batchType: 'SLAUGHTER', parentNos: [], sourceEarTags: ['E1309-2025-000301', 'E1309-2025-000302'], orgId: org('PLANT001').id, orgName: org('PLANT001').orgName, holderOrgId: org('PLANT001').id, holderOrgName: org('PLANT001').orgName, productName: '待分割胴体', weightKg: 280.5, produceDate: '2026-09-11', certHash: 'sha256:ab12', status: 'RECEIVED', createTxid: '0xs1' })
const p1 = addBatch({ batchNo: 'P-20260911-001', batchType: 'PROCESS', parentNos: [s1.batchNo], sourceEarTags: [], orgId: org('PLANT001').id, orgName: org('PLANT001').orgName, holderOrgId: org('PLANT001').id, holderOrgName: org('PLANT001').orgName, productName: '卤制驴肉(后腿)', weightKg: 132.0, produceDate: '2026-09-11', certHash: 'sha256:cd34', status: 'RECEIVED', createTxid: '0xp1' })
const p2 = addBatch({ batchNo: 'P-20260911-002', batchType: 'PROCESS', parentNos: [s1.batchNo], sourceEarTags: [], orgId: org('PLANT001').id, orgName: org('PLANT001').orgName, holderOrgId: org('PLANT001').id, holderOrgName: org('PLANT001').orgName, productName: '卤制驴肉(肋条)', weightKg: 96.5, produceDate: '2026-09-11', certHash: 'sha256:ef56', status: 'RECEIVED', createTxid: '0xp2' })
const f1 = addBatch({ batchNo: 'F-20260911-001', batchType: 'PRODUCT', parentNos: [p1.batchNo], sourceEarTags: [], orgId: org('PLANT001').id, orgName: org('PLANT001').orgName, holderOrgId: org('SHOP001').id, holderOrgName: org('SHOP001').orgName, productName: '驴肉火烧·卤制驴肉(后腿)', weightKg: 120.0, produceDate: '2026-09-11', expireDate: '2026-09-18', certHash: 'sha256:gh78', status: 'IN_STORE', createTxid: '0xf1', qcReportUri: 'minio://qc/f1.pdf' })
const f2 = addBatch({ batchNo: 'F-20260911-002', batchType: 'PRODUCT', parentNos: [p2.batchNo], sourceEarTags: [], orgId: org('PLANT001').id, orgName: org('PLANT001').orgName, holderOrgId: org('SHOP002').id, holderOrgName: org('SHOP002').orgName, productName: '驴肉火烧·卤制驴肉(肋条)', weightKg: 88.0, produceDate: '2026-09-11', expireDate: '2026-09-18', certHash: 'sha256:ij90', status: 'IN_STORE', createTxid: '0xf2', qcReportUri: 'minio://qc/f2.pdf' })
const f3 = addBatch({ batchNo: 'F-20260908-003', batchType: 'PRODUCT', parentNos: [p1.batchNo], sourceEarTags: [], orgId: org('PLANT001').id, orgName: org('PLANT001').orgName, holderOrgId: org('SHOP001').id, holderOrgName: org('SHOP001').orgName, productName: '驴肉火烧·卤制驴肉(后腿)', weightKg: 60.0, produceDate: '2026-09-08', expireDate: '2026-09-15', certHash: 'sha256:kl12', status: 'IN_STORE', createTxid: '0xf3', qcReportUri: 'minio://qc/f3.pdf' })

;[s1, p1, p2, f1, f2, f3].forEach((b, i) => {
  batchEvents.push({ id: nid(), batchNo: b.batchNo, eventType: 'BATCH_CREATE', orgName: b.orgName, operator: '小孙', eventTime: b.produceDate + ' 09:05:00', summary: `创建${b.batchType}批次 ${b.batchNo}`, txId: '0x' + (1000 + i), blockNo: 9300 + i })
})
;[f1, f2, f3].forEach((b, i) => {
  batchEvents.push({ id: nid(), batchNo: b.batchNo, eventType: 'QC_PASS', orgName: org('PLANT001').orgName, operator: '小吴', eventTime: b.produceDate + ' 11:20:00', summary: `出厂检验合格 ${b.productName}`, txId: '0xqc' + i, blockNo: 9350 + i })
  batchEvents.push({ id: nid(), batchNo: b.batchNo, eventType: 'TRANSFER', orgName: org('LOGI001').orgName, operator: '小周', eventTime: b.produceDate + ' 13:00:00', summary: `发往 ${b.holderOrgName}`, txId: '0xtr' + i, blockNo: 9360 + i })
  batchEvents.push({ id: nid(), batchNo: b.batchNo, eventType: 'RECEIVE', orgName: b.holderOrgName, operator: '店员', eventTime: b.produceDate + ' 17:30:00', summary: `${b.holderOrgName} 签收入库`, txId: '0xrc' + i, blockNo: 9370 + i })
})

// ---------- 运输单 ----------
export const transportOrders: TransportOrder[] = [
  { id: nid(), transportNo: 'T-20260911-001', batchNo: f1.batchNo, fromOrgId: org('PLANT001').id, fromOrgName: org('PLANT001').orgName, toOrgId: org('SHOP001').id, toOrgName: org('SHOP001').orgName, vehicleNo: '冀F·6X8K2', driverName: '吴师傅', driverPhone: '13900000001', departTime: '2026-09-11 13:00:00', expectArriveTime: '2026-09-11 17:00:00', actualArriveTime: '2026-09-11 17:25:00', status: 'RECEIVED', fileUri: 'minio://transport/t1.csv', fileHash: 'sha256:tr01', txId: '0xtr0', createTime: '2026-09-11 13:00:00' },
  { id: nid(), transportNo: 'T-20260911-002', batchNo: f2.batchNo, fromOrgId: org('PLANT001').id, fromOrgName: org('PLANT001').orgName, toOrgId: org('SHOP002').id, toOrgName: org('SHOP002').orgName, vehicleNo: '冀F·9L2M5', driverName: '郑师傅', driverPhone: '13900000002', departTime: '2026-09-11 13:10:00', expectArriveTime: '2026-09-11 18:30:00', actualArriveTime: '2026-09-11 18:20:00', status: 'RECEIVED', fileUri: 'minio://transport/t2.csv', fileHash: 'sha256:tr02', txId: '0xtr1', createTime: '2026-09-11 13:10:00' }
]
export const transportRecords: TransportRecord[] = []
transportOrders.forEach((o, i) => {
  for (let h = 0; h < 4; h++) {
    transportRecords.push({ id: nid(), orderId: o.id, recordTime: `2026-09-11 ${13 + h}:${10 + h}:00`, temperature: 1 + h * 0.4, humidity: 78 + h, abnormal: (i === 0 && h === 3) ? 1 : 0 })
  }
})

// ---------- 库存（门店在库成品批次） ----------
export const inventory: InventoryItem[] = [
  { batchNo: f1.batchNo, productName: f1.productName, orgName: org('PLANT001').orgName, holderOrgName: org('SHOP001').orgName, weightKg: 120, remainingKg: 84.5, produceDate: '2026-09-11', expireDate: '2026-09-18', status: 'IN_STORE', daysLeft: 7 },
  { batchNo: f3.batchNo, productName: f3.productName, orgName: org('PLANT001').orgName, holderOrgName: org('SHOP001').orgName, weightKg: 60, remainingKg: 12.0, produceDate: '2026-09-08', expireDate: '2026-09-15', status: 'IN_STORE', daysLeft: 4 },
  { batchNo: f2.batchNo, productName: f2.productName, orgName: org('PLANT001').orgName, holderOrgName: org('SHOP002').orgName, weightKg: 88, remainingKg: 70.0, produceDate: '2026-09-11', expireDate: '2026-09-18', status: 'IN_STORE', daysLeft: 7 }
]

// ---------- 产出记录 ----------
export const outputRecords: OutputRecord[] = [
  { id: nid(), batchNo: f1.batchNo, shopOrgId: org('SHOP001').id, shopOrgName: org('SHOP001').orgName, outputTime: '2026-09-11 18:20:00', ovenNo: '3号炉', chef: '马师傅', qty: 200, usedWeight: 30 },
  { id: nid(), batchNo: f3.batchNo, shopOrgId: org('SHOP001').id, shopOrgName: org('SHOP001').orgName, outputTime: '2026-09-09 18:05:00', ovenNo: '1号炉', chef: '马师傅', qty: 80, usedWeight: 12 }
]

// ---------- 溯源码 ----------
const codeChars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
function genCode(): string {
  let s = ''
  for (let i = 0; i < 14; i++) s += codeChars[Math.floor(Math.random() * codeChars.length)]
  return 'DT' + s
}
export const traceCodes: TraceCode[] = []
outputRecords.forEach((o, oi) => {
  for (let k = 0; k < (oi === 0 ? 10 : 6); k++) {
    traceCodes.push({ id: nid(), code: genCode(), batchNo: o.batchNo, shopOrgName: o.shopOrgName, outputId: o.id, status: 'BOUND', bindTxid: '0xbc' + oi + k, printCount: 1, scanTimes: Math.floor(Math.random() * 8), createTime: '2026-09-11 18:25:00' })
  }
})
// 一个已作废码 + 一个待测
traceCodes.push({ id: nid(), code: 'DT9F3K2A8Q7M5X1', batchNo: f1.batchNo, shopOrgName: org('SHOP001').orgName, outputId: 1, status: 'BOUND', bindTxid: '0xbcs', printCount: 1, scanTimes: 3, createTime: '2026-09-11 18:25:00' })

// ---------- 预警 ----------
export const alerts: Alert[] = [
  { id: nid(), alertType: 'TEMP_EXCEED', level: 'WARN', targetType: 'TRANSPORT', targetId: 'T-20260911-001', content: '运输单 T-20260911-001 第4次温湿度记录 4.4℃ 超阈值(0~4℃)', status: 'OPEN', createTime: '2026-09-11 16:10:00' },
  { id: nid(), alertType: 'OVERDUE', level: 'WARN', targetType: 'BATCH', targetId: f3.batchNo, content: `成品批次 ${f3.batchNo} 剩余保质期不足30%`, status: 'OPEN', createTime: '2026-09-11 09:00:00' },
  { id: nid(), alertType: 'CERT_EXPIRED', level: 'INFO', targetType: 'CERT', targetId: 'C-0003', content: '畜禽养殖代码证 将于 2026-09-18 到期', status: 'RESOLVED', handler: '小郑', handleNote: '已提醒企业续期', handleTime: '2026-09-10 10:00:00', createTime: '2026-09-08 09:00:00' },
  { id: nid(), alertType: 'FAKE_SCAN', level: 'INFO', targetType: 'CODE', targetId: 'DT9F3K2A8Q7M5X1', content: '溯源码 24h 内扫描 >50 次，标记为可疑', status: 'OPEN', createTime: '2026-09-11 12:00:00' }
]

// ---------- 证照 ----------
export const certs: Cert[] = [
  { id: nid(), orgId: org('FARM001').id, orgName: org('FARM001').orgName, certType: '动物防疫条件合格证', certNo: '动防证(2024)第001号', issueDate: '2024-03-01', expireDate: '2027-03-01', fileUri: 'minio://cert/farm001.pdf', fileHash: 'sha256:c1', auditStatus: 'PASSED', status: 1 },
  { id: nid(), orgId: org('PLANT001').id, orgName: org('PLANT001').orgName, certType: '食品生产许可证', certNo: 'SC12430984123', issueDate: '2024-06-15', expireDate: '2027-06-14', fileUri: 'minio://cert/plant001.pdf', fileHash: 'sha256:c2', auditStatus: 'PASSED', status: 1 },
  { id: nid(), orgId: org('SHOP001').id, orgName: org('SHOP001').orgName, certType: '食品经营许可证', certNo: 'JY213092600123', issueDate: '2024-01-20', expireDate: '2026-09-18', fileUri: 'minio://cert/shop001.pdf', fileHash: 'sha256:c3', auditStatus: 'PASSED', status: 1 }
]

// ---------- 抽检 ----------
export const inspections: Inspection[] = [
  { id: nid(), batchNo: f1.batchNo, orgName: org('SHOP001').orgName, agency: '国家肉类食品质量监督检验中心', items: '瘦肉精/水分/兽残快检/掺假物种鉴定', result: 'PASS', reportUri: 'minio://insp/i1.pdf', reportHash: 'sha256:i1', txId: '0xi1', createTime: '2026-09-12 10:00:00' }
]

// ---------- 召回 ----------
export const recalls: Recall[] = []

// ---------- 投诉 ----------
export const complaints: Complaint[] = [
  { id: nid(), code: 'DT9F3K2A8Q7M5X1', category: '怀疑非驴肉', content: '口感不对，怀疑不是真驴肉', phone: '13700000001', status: 'PENDING', createTime: '2026-09-11 20:00:00' }
]

// ---------- 操作日志 ----------
export const operationLogs: OperationLog[] = []
const mods = ['认证', '组织', '用户', '驴只', '批次', '运输', '门店', '溯源', '预警', '系统']
const ops = ['登录', '查询', '新增', '导出', '审核']
for (let i = 0; i < 60; i++) {
  operationLogs.push({
    id: nid(), userId: 1, userName: '平台管理员', module: mods[i % mods.length], operation: ops[i % ops.length],
    method: ['GET', 'POST', 'PUT'][i % 3], params: '{"id":' + (i + 1) + '}', resultCode: 0, ip: '192.168.1.' + (i % 50), costMs: 20 + (i % 80),
    createTime: '2026-09-' + String(11 - (i % 5)).padStart(2, '0') + ' ' + String(8 + (i % 12)).padStart(2, '0') + ':' + String(i % 60).padStart(2, '0') + ':00'
  })
}

// ---------- 参数 ----------
export const configs: ConfigItem[] = [
  { configKey: 'temp.threshold.fresh', configValue: '0~4', remark: '鲜驴肉温度阈值(℃)' },
  { configKey: 'temp.threshold.frozen', configValue: '<=-18', remark: '冷冻品温度阈值(℃)' },
  { configKey: 'batch.expire.default.days', configValue: '7', remark: '成品批次默认保质期(天)' },
  { configKey: 'login.lock.attempts', configValue: '5', remark: '登录失败锁定次数' },
  { configKey: 'login.lock.minutes', configValue: '10', remark: '登录锁定分钟数' },
  { configKey: 'trace.code.length', configValue: '14', remark: '溯源码随机位长度' },
  { configKey: 'scan.limit.ip.per.min', configValue: '10', remark: '单IP每分钟扫码上限' }
]

// ---------- 扫码日志 ----------
export const scanLogs: ScanLog[] = [
  { id: nid(), code: 'DT9F3K2A8Q7M5X1', ip: '223.104.3.12', region: '河北·保定', ua: 'WeChat/8.0', scanTime: '2026-09-11 19:30:00', riskFlag: 0 },
  { id: nid(), code: 'DT9F3K2A8Q7M5X1', ip: '223.104.3.12', region: '河北·保定', ua: 'WeChat/8.0', scanTime: '2026-09-11 19:35:00', riskFlag: 0 }
]

export const db = {
  orgs, users, donkeys, donkeyEvents, batches, batchEvents, transportOrders,
  transportRecords, inventory, outputRecords, traceCodes, alerts, certs,
  inspections, recalls, complaints, operationLogs, configs, scanLogs, demoPasswords
}
