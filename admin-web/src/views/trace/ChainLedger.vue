<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h3 class="page-title"><span class="bar"></span>区块链账本</h3>
        <p class="page-subtitle">donkey-channel 联盟链存证 · 全业务节点实时上链 · SHA-256 哈希链可完整性校验</p>
      </div>
      <div class="search">
        <el-input v-model="keyword" placeholder="搜索区块号 / txId / 链码方法" style="width: 250px" :prefix-icon="Search" clearable @keyup.enter="load" @clear="load" />
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
        <el-button :type="verifyResult?.valid ? 'success' : 'warning'" :icon="CircleCheck" :loading="verifying" @click="doVerify">完整性校验</el-button>
      </div>
    </div>

    <el-alert v-if="verifyResult" :type="verifyResult.valid ? 'success' : 'error'" :title="verifyResult.valid ? '校验通过：' + verifyResult.reason : '校验失败：' + verifyResult.reason"
      :description="`校验范围：#${0} ~ #${verifyResult.blocks - 1} 共 ${verifyResult.blocks} 个区块 · 校验时间 ${verifyResult.checkedAt}`" :closable="true" show-icon class="verify-alert" />

    <div class="stat-row">
      <div class="mini-stat" v-for="s in statsCards" :key="s.label">
        <div class="ms-icon"><el-icon><component :is="s.icon" /></el-icon></div>
        <div><div class="ms-val mono">{{ s.value }}</div><div class="ms-label">{{ s.label }}</div></div>
      </div>
    </div>

    <div class="chain-body">
      <el-card class="card blocks-card">
        <template #header>
          <div class="card-head">
            <span class="card-h">区块浏览</span>
            <el-switch v-model="autoRefresh" active-text="自动刷新" size="small" />
          </div>
        </template>
        <el-table :data="blocks" v-loading="loading" size="default" row-key="blockNo">
          <el-table-column label="区块" width="90">
            <template #default="{ row }"><span class="mono block-no">#{{ row.blockNo }}</span></template>
          </el-table-column>
          <el-table-column prop="time" label="出块时间" width="150" />
          <el-table-column label="交易" width="70" align="center">
            <template #default="{ row }"><el-tag size="small" type="info" effect="plain">{{ row.txCount }}</el-tag></template>
          </el-table-column>
          <el-table-column label="链码方法" min-width="130">
            <template #default="{ row }">
              <el-tag v-for="t in row.txs" :key="t.txId" size="small" class="op-tag" effect="light">{{ t.op }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交组织" min-width="150">
            <template #default="{ row }">{{ row.txs.map((t: any) => t.orgName).join('、') }}</template>
          </el-table-column>
          <el-table-column label="区块哈希" min-width="220">
            <template #default="{ row }"><span class="mono hash" :title="row.hash">{{ short(row.hash) }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="showBlock(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination background layout="prev, pager, next, total" :total="total" :page-size="size" v-model:current-page="page" @current-change="load" />
        </div>
      </el-card>

      <el-card class="card side-card">
        <template #header><span class="card-h">上链组织分布</span></template>
        <div v-for="o in stats.orgDist" :key="o.name" class="dist-row">
          <span class="dist-name">{{ o.name }}</span>
          <div class="dist-bar"><div class="dist-fill" :style="{ width: barW(o.value) }"></div></div>
          <span class="dist-val mono">{{ o.value }}</span>
        </div>
        <template v-if="!stats.orgDist?.length"><el-empty :image-size="60" description="暂无数据" /></template>
      </el-card>
      <el-card class="card side-card">
        <template #header><span class="card-h">链码方法调用 Top6</span></template>
        <div v-for="o in stats.opDist?.slice(0, 6)" :key="o.name" class="dist-row">
          <span class="dist-name mono">{{ o.name }}</span>
          <div class="dist-bar"><div class="dist-fill alt" :style="{ width: barW2(o.value) }"></div></div>
          <span class="dist-val mono">{{ o.value }}</span>
        </div>
      </el-card>
    </div>

    <el-drawer v-model="drawer" :title="`区块 #${current?.blockNo} 详情`" size="480px">
      <template v-if="current">
        <div class="kv"><span>区块高度</span><b class="mono">#{{ current.blockNo }}</b></div>
        <div class="kv"><span>通道</span><b class="mono">{{ current.channel }}</b></div>
        <div class="kv"><span>出块时间</span><b class="mono">{{ current.time }}</b></div>
        <div class="kv col"><span>前一区块哈希</span><b class="mono hash-full">{{ current.prevHash }}</b></div>
        <div class="kv col"><span>本区块哈希</span><b class="mono hash-full brand">{{ current.hash }}</b></div>
        <div class="tx-list">
          <div class="tx-title">交易明细（{{ current.txs.length }}）</div>
          <div v-for="t in current.txs" :key="t.txId" class="tx-item">
            <div class="tx-row"><el-tag size="small" effect="light">{{ t.op }}</el-tag><span class="mono txid">{{ t.txId }}</span></div>
            <div class="tx-meta">{{ t.orgName }} · {{ t.mspId }} · {{ t.time }}</div>
            <pre class="tx-payload">{{ JSON.stringify(t.payload, null, 2) }}</pre>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Search, CircleCheck } from '@element-plus/icons-vue'
import { http } from '@/utils/request'
import { ElMessage } from 'element-plus'

const keyword = ref('')
const page = ref(1), size = 10, total = ref(0)
const blocks = ref<any[]>([])
const stats = ref<any>({ orgDist: [], opDist: [] })
const loading = ref(false)
const verifying = ref(false)
const verifyResult = ref<any>(null)
const autoRefresh = ref(false)
const drawer = ref(false)
const current = ref<any>(null)
let timer: number | undefined

const statsCards = computed(() => [
  { label: '区块总数', value: stats.value.totalBlocks ?? '-', icon: 'Box' },
  { label: '上链交易', value: stats.value.totalTx ?? '-', icon: 'Link' },
  { label: '今日上链', value: stats.value.todayTx ?? '-', icon: 'Odometer' },
  { label: '通道', value: stats.value.channel ?? '-', icon: 'Connection' }
])

function short(h: string) { return h ? h.slice(0, 10) + '…' + h.slice(-8) : '-' }
function barW(v: number) { const m = Math.max(...(stats.value.orgDist || []).map((x: any) => x.value), 1); return Math.round(v / m * 100) + '%' }
function barW2(v: number) { const m = Math.max(...(stats.value.opDist || []).slice(0, 6).map((x: any) => x.value), 1); return Math.round(v / m * 100) + '%' }

async function load() {
  loading.value = true
  try {
    const [b, s] = await Promise.all([
      http.get('/chain/blocks', { page: String(page.value), size: String(size), keyword: keyword.value }),
      http.get('/chain/stats')
    ])
    blocks.value = b.records; total.value = b.total; stats.value = s
  } finally { loading.value = false }
}
async function doVerify() {
  verifying.value = true
  try {
    verifyResult.value = await http.get('/chain/verify')
    verifyResult.value.valid ? ElMessage.success('链完整性校验通过') : ElMessage.error('链完整性校验失败')
  } finally { verifying.value = false }
}
function showBlock(row: any) { current.value = row; drawer.value = true }

onMounted(() => {
  load()
  timer = window.setInterval(() => { if (autoRefresh.value) load() }, 3000)
})
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.search { display: flex; gap: 8px; align-items: center; }
.verify-alert { margin-bottom: 14px; }
.stat-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 14px; }
.mini-stat { display: flex; align-items: center; gap: 12px; background: var(--card-bg, #fff); border: 1px solid var(--border-1, #e8edf3); border-radius: 14px; padding: 14px 18px; box-shadow: var(--shadow-sm, 0 2px 8px rgba(15,23,42,.04)); }
.ms-icon { width: 40px; height: 40px; border-radius: 12px; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #16a34a, #0d9488); color: #fff; font-size: 19px; flex-shrink: 0; }
.ms-val { font-size: 20px; font-weight: 700; color: var(--text-1, #1e293b); }
.ms-label { font-size: 12px; color: var(--text-3, #94a3b8); }
.chain-body { display: grid; grid-template-columns: 1fr 300px; gap: 14px; align-items: start; }
.side-card { grid-column: 2; }
.blocks-card { grid-column: 1; grid-row: 1 / span 2; }
.chain-body { grid-template-columns: 1fr 300px; }
.card-head { display: flex; justify-content: space-between; align-items: center; }
.card-h { font-weight: 600; }
.block-no { color: var(--brand, #16a34a); font-weight: 700; }
.hash { font-size: 12px; color: var(--text-2, #475569); }
.op-tag { margin-right: 4px; }
.pager { display: flex; justify-content: flex-end; padding-top: 12px; }
.dist-row { display: flex; align-items: center; gap: 8px; margin: 10px 0; }
.dist-name { width: 96px; font-size: 12px; color: var(--text-2, #475569); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dist-bar { flex: 1; height: 8px; border-radius: 4px; background: #eef2f7; overflow: hidden; }
.dist-fill { height: 100%; border-radius: 4px; background: linear-gradient(90deg, #16a34a, #22c55e); }
.dist-fill.alt { background: linear-gradient(90deg, #0d9488, #14b8a6); }
.dist-val { width: 34px; text-align: right; font-size: 12px; color: var(--text-1, #1e293b); }
.kv { display: flex; justify-content: space-between; gap: 10px; padding: 9px 0; border-bottom: 1px dashed #eef2f7; font-size: 13px; }
.kv span { color: var(--text-3, #94a3b8); }
.kv.col { flex-direction: column; gap: 4px; }
.hash-full { font-size: 11px; word-break: break-all; font-weight: 500; }
.hash-full.brand { color: var(--brand, #16a34a); }
.tx-list { margin-top: 14px; }
.tx-title { font-weight: 600; font-size: 13px; margin-bottom: 8px; }
.tx-item { border: 1px solid #eef2f7; border-radius: 10px; padding: 10px 12px; margin-bottom: 10px; }
.tx-row { display: flex; align-items: center; gap: 8px; }
.txid { font-size: 11px; color: var(--text-2, #475569); word-break: break-all; }
.tx-meta { font-size: 11px; color: var(--text-3, #94a3b8); margin: 6px 0; }
.tx-payload { background: #0f172a; color: #a7f3d0; font-size: 11px; border-radius: 8px; padding: 8px 10px; overflow-x: auto; margin: 0; font-family: var(--font-mono, monospace); }
@media (max-width: 1100px) { .chain-body { grid-template-columns: 1fr; } .blocks-card, .side-card { grid-column: 1; grid-row: auto; } .stat-row { grid-template-columns: repeat(2, 1fr); } }
</style>
