// 驴链管理端 · 领域类型（对齐 docs/产品开发文档.md §8/§9/§10）

export type OrgType = 'PLATFORM' | 'FARM' | 'PLANT' | 'LOGISTICS' | 'SHOP' | 'REGULATOR'
export type RoleCode =
  | 'PLATFORM_ADMIN'
  | 'FARM_OPERATOR'
  | 'FARM_MANAGER'
  | 'PLANT_OPERATOR'
  | 'PLANT_QC'
  | 'LOGISTICS_DRIVER'
  | 'LOGISTICS_DISPATCHER'
  | 'SHOP_KEEPER'
  | 'REGULATOR_OFFICER'
  | 'REGULATOR_ADMIN'
  | 'CONSUMER'

export interface Org {
  id: number
  orgCode: string
  orgName: string
  orgType: OrgType
  mspId: string
  contact: string
  phone: string
  status: 0 | 1 | 2 // 0待启用 1正常 2停用
  createTime: string
}

export interface User {
  id: number
  orgId: number
  username: string
  realName: string
  phone: string
  roleCode: RoleCode
  status: 0 | 1
  lastLoginTime?: string
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  user: UserInfo
  permissions: string[]
  menus: MenuNode[]
}

export interface UserInfo {
  userId: number
  username: string
  realName: string
  orgId: number
  orgName: string
  orgType: OrgType
  roleCode: RoleCode
  roleName: string
  mspId: string
}

export interface MenuNode {
  path: string
  title: string
  icon?: string
  permission?: string
  children?: MenuNode[]
}

export type DonkeyStatus = 'RAISED' | 'QUARANTINED' | 'SLAUGHTERED'
export interface Donkey {
  id: number
  earTagId: string
  breed: string
  gender: 'M' | 'F'
  birthDate: string
  orgId: number
  orgName: string
  barnNo: string
  status: DonkeyStatus
  createTxid: string
  photos: string[]
  createTime: string
}
export interface DonkeyEvent {
  id: number
  donkeyId: number
  earTagId: string
  eventType: 'FEED' | 'IMMUNIZE' | 'HEALTH' | 'QUARANTINE'
  eventTime: string
  content: string
  detailUri?: string
  detailHash?: string
  txId?: string
  blockNo?: number
}

export type BatchType = 'SLAUGHTER' | 'PROCESS' | 'PRODUCT'
export type BatchStatus =
  | 'CREATED'
  | 'IN_TRANSIT'
  | 'RECEIVED'
  | 'IN_STORE'
  | 'SOLD'
  | 'RECALLED'
  | 'EXPIRED'

export interface Batch {
  id: number
  batchNo: string
  batchType: BatchType
  parentNos: string[]
  sourceEarTags: string[]
  orgId: number
  orgName: string
  holderOrgId: number
  holderOrgName: string
  productName: string
  weightKg: number
  produceDate: string
  expireDate?: string
  certHash?: string
  status: BatchStatus
  createTxid: string
  qcReportUri?: string
  createTime: string
}
export interface BatchEvent {
  id: number
  batchNo: string
  eventType: string
  orgName: string
  operator: string
  eventTime: string
  summary: string
  txId?: string
  blockNo?: number
}

export interface TransportOrder {
  id: number
  transportNo: string
  batchNo: string
  fromOrgId: number
  fromOrgName: string
  toOrgId: number
  toOrgName: string
  vehicleNo: string
  driverName: string
  driverPhone: string
  departTime: string
  expectArriveTime: string
  actualArriveTime?: string
  status: 'CREATED' | 'IN_TRANSIT' | 'RECEIVED' | 'REJECTED'
  fileUri?: string
  fileHash?: string
  txId?: string
  createTime: string
}
export interface TransportRecord {
  id: number
  orderId: number
  recordTime: string
  temperature: number
  humidity: number
  photoUri?: string
  abnormal: 0 | 1
}

export interface InventoryItem {
  batchNo: string
  productName: string
  orgName: string
  holderOrgName: string
  weightKg: number
  remainingKg: number
  produceDate: string
  expireDate: string
  status: BatchStatus
  daysLeft: number
}
export interface OutputRecord {
  id: number
  batchNo: string
  shopOrgId: number
  shopOrgName: string
  outputTime: string
  ovenNo: string
  chef: string
  qty: number
  usedWeight: number
}
export type TraceCodeStatus = 'UNUSED' | 'BOUND' | 'SOLD' | 'DESTROYED'
export interface TraceCode {
  id: number
  code: string
  batchNo: string
  shopOrgName: string
  outputId: number
  status: TraceCodeStatus
  bindTxid?: string
  printCount: number
  scanTimes: number
  createTime: string
}

export interface Alert {
  id: number
  alertType: string
  level: 'INFO' | 'WARN' | 'CRITICAL'
  targetType: string
  targetId: string
  content: string
  status: 'OPEN' | 'RESOLVED' | 'FALSE_POSITIVE'
  handler?: string
  handleNote?: string
  handleTime?: string
  createTime: string
}
export interface Cert {
  id: number
  orgId: number
  orgName: string
  certType: string
  certNo: string
  issueDate: string
  expireDate: string
  fileUri: string
  fileHash: string
  auditStatus: 'NONE' | 'PASSED' | 'REJECTED'
  status: 1 | 0
}
export interface Inspection {
  id: number
  batchNo: string
  orgName: string
  agency: string
  items: string
  result: 'PASS' | 'FAIL'
  reportUri: string
  reportHash: string
  txId?: string
  createTime: string
}
export interface Recall {
  id: number
  batchNo: string
  reason: string
  scopeJson: string
  status: 'RECALLING' | 'DONE'
  initiator: string
  createTime: string
  reportUri?: string
}
export interface Complaint {
  id: number
  code: string
  category: string
  content: string
  phone?: string
  status: 'PENDING' | 'PROCESSING' | 'DONE'
  handleNote?: string
  createTime: string
}
export interface OperationLog {
  id: number
  userId: number
  userName: string
  module: string
  operation: string
  method: string
  params: string
  resultCode: number
  ip: string
  costMs: number
  createTime: string
}
export interface ConfigItem {
  configKey: string
  configValue: string
  remark: string
}
export interface ScanLog {
  id: number
  code: string
  ip: string
  region: string
  ua: string
  scanTime: string
  riskFlag: 0 | 1
}

// 溯源报告（消费者 H5 同构，后台预览用）
export interface TraceTimelineNode {
  stage: string
  title: string
  orgName?: string
  time?: string
  summary?: string
  txId?: string
  photos?: string[]
}
export interface TraceReport {
  code: string
  verifyStatus: 'PASSED' | 'FAILED' | 'DESTROYED'
  product: {
    name: string
    batchNo: string
    produceDate: string
    shopName: string
    expireDate?: string
  }
  timeline: TraceTimelineNode[]
  blockchain: {
    channel: string
    eventCount: number
    latestBlock: number
    latestTxId: string
  }
}

export interface PageResult<T> {
  total: number
  records: T[]
}
export interface ApiResult<T> {
  code: number
  message: string
  data: T
  traceId: string
}
