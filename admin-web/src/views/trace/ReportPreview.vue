<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h3 class="page-title"><span class="bar"></span>溯源报告预览</h3>
        <p class="page-subtitle">模拟消费者扫码后看到的 H5 溯源报告（时间轴 + 区块链存证卡）</p>
      </div>
      <div class="search">
        <el-input v-model="code" placeholder="输入溯源码，如 DT9F3K2A8Q7M5X1" style="width: 260px" :prefix-icon="Search" @keyup.enter="query" />
        <el-button type="primary" :icon="Search" @click="query">查询</el-button>
      </div>
    </div>

    <div class="preview-wrap">
      <div class="phone">
        <div class="phone-notch"></div>
        <div class="phone-screen" v-if="report">
          <div class="ph-head">
            <div class="ph-product">{{ report.product.name }}</div>
            <div class="ph-sub">{{ report.product.shopName }}</div>
            <div class="ph-badge" :class="report.verifyStatus === 'PASSED' ? 'ok' : report.verifyStatus === 'DESTROYED' ? 'bad' : 'warn'">
              <el-icon><CircleCheck v-if="report.verifyStatus === 'PASSED'" /><Warning v-else /></el-icon>
              {{ verifyText }}
            </div>
          </div>

          <div class="ph-meta">
            <div><span>批次</span>{{ report.product.batchNo }}</div>
            <div><span>出锅</span>{{ report.product.produceDate }}</div>
            <div v-if="report.product.expireDate"><span>保质期至</span>{{ report.product.expireDate }}</div>
          </div>

          <div class="ph-timeline">
            <div v-for="(n, i) in report.timeline" :key="i" class="ph-node">
              <div class="ph-dot"></div>
              <div class="ph-node-body">
                <div class="ph-node-title">{{ n.title }}<span v-if="n.orgName" class="ph-org">{{ n.orgName }}</span></div>
                <div class="ph-node-time mono" v-if="n.time">{{ n.time }}</div>
                <div class="ph-node-sum">{{ n.summary }}</div>
                <div class="ph-node-tx mono" v-if="n.txId">txId: {{ n.txId }}</div>
              </div>
            </div>
            <div v-if="!report.timeline.length" class="ph-empty">无溯源记录</div>
          </div>

          <div class="ph-chain">
            <div class="ph-chain-title"><el-icon><Link /></el-icon> 区块链存证</div>
            <div class="ph-chain-row"><span>通道</span>{{ report.blockchain.channel }}</div>
            <div class="ph-chain-row"><span>事件数</span>{{ report.blockchain.eventCount }}</div>
            <div class="ph-chain-row"><span>最新区块</span>#{{ report.blockchain.latestBlock }}</div>
            <div class="ph-chain-note">链上数据自上链起不可篡改</div>
          </div>
        </div>
        <el-empty v-else description="输入溯源码查看报告" :image-size="80" />
      </div>
      <div class="preview-side">
        <el-alert v-if="report && report.verifyStatus !== 'PASSED'" :title="verifyText" :type="report.verifyStatus === 'DESTROYED' ? 'error' : 'warning'" :closable="false" show-icon />
        <el-card class="card tip">
          <template #header><span class="card-h">核验说明</span></template>
          <p>· 每个链上事件重算详情哈希与链上 <code>detailHash</code> 比对</p>
          <p>· 全部通过则报告打「已验证」绿标</p>
          <p>· 码状态为已作废时显示作废页</p>
          <p>· 风控命中（高频/跨省）时显示黄色警示条</p>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Search, CircleCheck, Warning, Link } from '@element-plus/icons-vue'
import { http } from '@/utils/request'
import { ElMessage } from 'element-plus'

const route = useRoute()
const code = ref('')
const report = ref<any>(null)

const verifyText = computed(() => ({ PASSED: '已验证 · 数据可信', FAILED: '未找到该溯源码', DESTROYED: '该码已作废' }[report.value?.verifyStatus] || '—'))

async function query() {
  if (!code.value.trim()) return ElMessage.warning('请输入溯源码')
  report.value = await http.get(`/trace/codes/${code.value.trim()}`)
}
onMounted(async () => {
  const q = route.query.code as string
  if (q) { code.value = q; await query(); return }
  try {
    const res: any = await http.get('/shop/trace-codes')
    const first = (Array.isArray(res) ? res : res?.records || res?.list || [])[0]
    if (first?.code) code.value = first.code
  } catch {}
  await query()
})
</script>

<style scoped>
.search { display: flex; gap: 8px; }
.preview-wrap { display: flex; gap: 24px; align-items: flex-start; flex-wrap: wrap; }
.phone {
  width: 340px; height: 680px; background: #0f172a; border-radius: 36px; padding: 14px;
  box-shadow: var(--shadow-lg); position: relative; border: 8px solid #1e293b; flex-shrink: 0;
}
.phone-notch { width: 120px; height: 22px; background: #1e293b; border-radius: 0 0 14px 14px; margin: -14px auto 8px; }
.phone-screen { height: calc(100% - 30px); background: #f8fafc; border-radius: 24px; overflow-y: auto; padding: 16px; }
.ph-head { text-align: center; padding-bottom: 12px; border-bottom: 1px solid #e2e8f0; }
.ph-product { font-size: 17px; font-weight: 700; color: #1e293b; }
.ph-sub { font-size: 12px; color: #64748b; margin-top: 2px; }
.ph-badge { display: inline-flex; align-items: center; gap: 4px; margin-top: 8px; padding: 3px 10px; border-radius: 20px; font-size: 12px; font-weight: 600; }
.ph-badge.ok { background: #dcfce7; color: #16a34a; }
.ph-badge.bad { background: #fee2e2; color: #ef4444; }
.ph-badge.warn { background: #fef3c7; color: #f59e0b; }
.ph-meta { display: flex; justify-content: space-between; padding: 12px 4px; flex-wrap: wrap; gap: 6px; }
.ph-meta div { font-size: 12px; color: #1e293b; }
.ph-meta span { display: block; color: #94a3b8; font-size: 10px; margin-bottom: 2px; }
.ph-timeline { padding: 4px 0; }
.ph-node { display: flex; gap: 10px; padding-bottom: 14px; position: relative; }
.ph-node::before { content: ''; position: absolute; left: 4px; top: 14px; bottom: -2px; width: 2px; background: #e2e8f0; }
.ph-node:last-child::before { display: none; }
.ph-dot { width: 10px; height: 10px; border-radius: 50%; background: #16a34a; margin-top: 4px; flex-shrink: 0; z-index: 1; box-shadow: 0 0 0 3px #dcfce7; }
.ph-node-title { font-size: 13px; font-weight: 600; color: #1e293b; display: flex; align-items: center; gap: 6px; }
.ph-org { font-size: 11px; color: #16a34a; font-weight: 500; }
.ph-node-time { font-size: 11px; color: #94a3b8; }
.ph-node-sum { font-size: 12px; color: #475569; margin-top: 2px; line-height: 1.5; }
.ph-node-tx { font-size: 10px; color: #94a3b8; margin-top: 2px; word-break: break-all; }
.ph-empty { color: #94a3b8; text-align: center; padding: 20px; }
.ph-chain { background: #0f172a; border-radius: 14px; padding: 14px; color: #f8fafc; margin-top: 8px; }
.ph-chain-title { font-size: 13px; font-weight: 600; display: flex; align-items: center; gap: 6px; margin-bottom: 10px; }
.ph-chain-row { display: flex; justify-content: space-between; font-size: 12px; padding: 4px 0; color: #cbd5e1; }
.ph-chain-row span { color: #64748b; }
.ph-chain-note { font-size: 11px; color: #64748b; margin-top: 8px; text-align: center; }
.preview-side { flex: 1; min-width: 280px; max-width: 420px; }
.tip p { font-size: 13px; color: var(--text-2); margin: 6px 0; }
.tip code { background: var(--brand-soft-2); color: var(--brand); padding: 1px 5px; border-radius: 4px; font-family: var(--font-mono); }
.card-h { font-weight: 600; }
</style>
