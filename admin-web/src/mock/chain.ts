// 模拟 Fabric 区块链账本：SHA-256 哈希链式存证，支持追加交易、完整性校验、查询。
// 对接真实后端时，由 Fabric SDK 的 submitTransaction / queryBlock 替换。
import { db } from './db'

export interface ChainTx {
  txId: string
  op: string            // 链码方法名，如 CreateDonkey / CreateBatch
  mspId: string         // 提交组织 MSP
  orgName: string
  time: string
  payload: Record<string, any>
}

export interface ChainBlock {
  blockNo: number
  prevHash: string
  hash: string
  time: string
  txCount: number
  channel: string
  txs: ChainTx[]
}

export const CHANNEL = 'donkey-channel'
const blocks: ChainBlock[] = []
let txSeq = 0

// ---------- 纯同步 SHA-256（字节安全，支持中文 payload） ----------
function sha256(str: string): string {
  const bytes = Array.from(new TextEncoder().encode(str))
  const word: number[] = []
  for (let i = 0; i < bytes.length; i++) word[i >> 2] = (word[i >> 2] || 0) | (bytes[i] << ((3 - i) % 4) * 8)
  const bitLen = bytes.length * 8
  const rr = (v: number, a: number) => (v >>> a) | (v << (32 - a))
  const K: number[] = []
  const H0: number[] = []
  let pc = 0
  const comp: Record<number, number> = {}
  for (let cand = 2; pc < 64; cand++) {
    if (!comp[cand]) {
      for (let i = 0; i < 313; i += cand) comp[i] = cand
      H0[pc] = (Math.pow(cand, 0.5) * 4294967296) | 0
      K[pc++] = (Math.pow(cand, 1 / 3) * 4294967296) | 0
    }
  }
  word[bytes.length >> 2] = (word[bytes.length >> 2] || 0) | (0x80 << ((3 - bytes.length) % 4) * 8)
  const total = ((bytes.length + 8 >> 6) + 1) * 16
  word[total - 1] = bitLen
  word[total - 2] = Math.floor(bitLen / 4294967296)
  const H = H0.slice()
  for (let j = 0; j < total; j += 16) {
    const w = new Array(64).fill(0)
    for (let i = 0; i < 16; i++) w[i] = word[j + i] || 0
    for (let i = 16; i < 64; i++) {
      const s0 = rr(w[i - 15], 7) ^ rr(w[i - 15], 18) ^ (w[i - 15] >>> 3)
      const s1 = rr(w[i - 2], 17) ^ rr(w[i - 2], 19) ^ (w[i - 2] >>> 10)
      w[i] = (w[i - 16] + s0 + w[i - 7] + s1) | 0
    }
    const v = H.slice()
    for (let i = 0; i < 64; i++) {
      const S1 = rr(v[4], 6) ^ rr(v[4], 11) ^ rr(v[4], 25)
      const ch = (v[4] & v[5]) ^ (~v[4] & v[6])
      const t1 = (v[7] + S1 + ch + K[i] + w[i]) | 0
      const S0 = rr(v[0], 2) ^ rr(v[0], 13) ^ rr(v[0], 22)
      const maj = (v[0] & v[1]) ^ (v[0] & v[2]) ^ (v[1] & v[2])
      const t2 = (S0 + maj) | 0
      v[7] = v[6]; v[6] = v[5]; v[5] = v[4]; v[4] = (v[3] + t1) | 0
      v[3] = v[2]; v[2] = v[1]; v[1] = v[0]
      v[0] = (t1 + t2) | 0
    }
    for (let i = 0; i < 8; i++) H[i] = (H[i] + v[i]) | 0
  }
  return H.map(h => (h >>> 0).toString(16).padStart(8, '0')).join('')
}

function blockHash(blockNo: number, prevHash: string, time: string, txs: ChainTx[]): string {
  return '0x' + sha256(`${CHANNEL}|${blockNo}|${prevHash}|${time}|${JSON.stringify(txs)}`)
}

// ---------- 追加交易（一交易一块，演示最直观） ----------
export function appendTx(op: string, payload: Record<string, any>, mspId: string, orgName: string, time: string, fixedTxId?: string) {
  const prev = blocks[blocks.length - 1]
  const blockNo = prev ? prev.blockNo + 1 : 0
  const tx: ChainTx = {
    txId: fixedTxId || ('0x' + sha256(`tx|${CHANNEL}|${op}|${Date.now()}|${txSeq++}`).slice(0, 20)),
    op, mspId, orgName, time: time || nowStr(), payload
  }
  const prevHash = prev ? prev.hash : '0x' + '0'.repeat(24)
  const hash = blockHash(blockNo, prevHash, tx.time, [tx])
  blocks.push({ blockNo, prevHash, hash, time: tx.time, txCount: 1, channel: CHANNEL, txs: [tx] })
  return { txId: tx.txId, blockNo, hash }
}

// ---------- 完整性校验 ----------
export function verifyChain() {
  const checkedAt = nowStr()
  for (let i = 0; i < blocks.length; i++) {
    const b = blocks[i]
    const expectPrev = i === 0 ? '0x' + '0'.repeat(24) : blocks[i - 1].hash
    const expectHash = blockHash(b.blockNo, b.prevHash, b.time, b.txs)
    if (b.prevHash !== expectPrev) return { valid: false, blocks: blocks.length, brokenAt: b.blockNo, reason: `区块 #${b.blockNo} 前向哈希与上一块不匹配`, checkedAt }
    if (b.hash !== expectHash) return { valid: false, blocks: blocks.length, brokenAt: b.blockNo, reason: `区块 #${b.blockNo} 哈希自校验失败，数据疑似被篡改`, checkedAt }
  }
  return { valid: true, blocks: blocks.length, checkedAt, reason: '全链 SHA-256 哈希校验通过，存证完整可信' }
}

// ---------- 查询 ----------
export function chainStats() {
  const orgDist: Record<string, number> = {}
  const opDist: Record<string, number> = {}
  blocks.forEach(b => b.txs.forEach(t => {
    orgDist[t.orgName] = (orgDist[t.orgName] || 0) + 1
    opDist[t.op] = (opDist[t.op] || 0) + 1
  }))
  return {
    channel: CHANNEL,
    totalBlocks: blocks.length,
    totalTx: blocks.reduce((s, b) => s + b.txCount, 0),
    todayTx: blocks.filter(b => b.time.startsWith('2026-09-11')).reduce((s, b) => s + b.txCount, 0),
    orgDist: Object.entries(orgDist).map(([name, value]) => ({ name, value })).sort((a, b) => b.value - a.value),
    opDist: Object.entries(opDist).map(([name, value]) => ({ name, value })).sort((a, b) => b.value - a.value)
  }
}
export function listBlocks(query: Record<string, string>) {
  const page = +(query.page || 1), size = +(query.size || 10)
  const kw = (query.keyword || '').trim().toLowerCase()
  let list = blocks.slice().reverse()
  if (kw) list = list.filter(b => JSON.stringify(b).toLowerCase().includes(kw))
  const total = list.length
  return { total, records: list.slice((page - 1) * size, (page - 1) * size + size) }
}
export function findTx(txId: string) {
  for (const b of blocks) {
    const t = b.txs.find(x => x.txId.toLowerCase() === txId.toLowerCase())
    if (t) return { ...t, blockNo: b.blockNo, blockHash: b.hash, channel: b.channel }
  }
  return null
}
export function txsByOp(opKeywords: string[], payloadField: string, value: string) {
  const hits: { txId: string; blockNo: number; time: string; op: string; payload: Record<string, any> }[] = []
  blocks.forEach(b => b.txs.forEach(t => {
    if (opKeywords.includes(t.op) && String(t.payload[payloadField] || '') === value)
      hits.push({ txId: t.txId, blockNo: b.blockNo, time: t.time, op: t.op, payload: t.payload })
  }))
  return hits
}
function nowStr() {
  return '2026-09-11 ' + String(new Date().getHours()).padStart(2, '0') + ':' + String(new Date().getMinutes()).padStart(2, '0') + ':00'
}

// ---------- 创世块 + 存量业务数据回放上链 ----------
appendTx('GENESIS', {
  channel: CHANNEL, orderer: 'orderer.baoding-donkey.cn:7050',
  orgs: Array.from(new Set(db.orgs.map(o => o.mspId))),
  chaincode: 'donkey-trace-cc v1.2'
}, 'OrdererMSP', '驴链平台运营方', '2026-01-01 09:00:00')

// 组织入网
db.orgs.forEach(o => appendTx('JoinNetwork', { orgCode: o.orgCode, orgName: o.orgName, mspId: o.mspId }, o.mspId, o.orgName, o.createTime))

// 驴只建档 + 事件
db.donkeys.forEach(d => appendTx('CreateDonkey', { earTagId: d.earTagId, breed: d.breed, orgName: d.orgName, barnNo: d.barnNo }, 'OrgFarmMSP', d.orgName, d.createTime, d.createTxid))
db.donkeyEvents.forEach(e => {
  const d = db.donkeys.find(x => x.earTagId === e.earTagId)
  const op = e.eventType === 'QUARANTINE' ? 'QuarantineDonkey' : 'AddDonkeyEvent'
  appendTx(op, { earTagId: e.earTagId, eventType: e.eventType, content: e.content, orgName: d?.orgName }, 'OrgFarmMSP', d?.orgName || '', e.eventTime, e.txId)
})

// 批次创建 + 批次事件
db.batches.forEach(b => appendTx('CreateBatch', { batchNo: b.batchNo, batchType: b.batchType, productName: b.productName, parentNos: b.parentNos, sourceEarTags: b.sourceEarTags, weightKg: b.weightKg, orgName: b.orgName }, 'OrgPlantMSP', b.orgName, b.createTime, b.createTxid))
db.batchEvents.forEach(e => appendTx('AddBatchEvent', { batchNo: e.batchNo, eventType: e.eventType, summary: e.summary, operator: e.operator, orgName: e.orgName }, 'OrgPlantMSP', e.orgName, e.eventTime, e.txId))

// 运输单
db.transportOrders.forEach(o => appendTx('CreateTransport', { transportNo: o.transportNo, batchNo: o.batchNo, vehicleNo: o.vehicleNo, fromOrgName: o.fromOrgName, toOrgName: o.toOrgName }, 'OrgLogisticsMSP', o.fromOrgName, o.createTime, o.txId))

// 证照
db.certs.forEach(c => appendTx('AddCert', { certNo: c.certNo, certType: c.certType, orgName: c.orgName, fileHash: c.fileHash }, 'OrgPlatformMSP', c.orgName, c.issueDate + ' 10:00:00'))

// 抽检报告
db.inspections.forEach(i => appendTx('AddInspection', { batchNo: i.batchNo, agency: i.agency, result: i.result, reportHash: i.reportHash }, 'OrgRegulatorMSP', i.orgName, i.createTime, i.txId))

// 溯源码绑定
db.traceCodes.forEach(c => appendTx('BindTraceCode', { code: c.code, batchNo: c.batchNo, shopOrgName: c.shopOrgName }, 'OrgShopMSP', c.shopOrgName, c.createTime, c.bindTxid))
