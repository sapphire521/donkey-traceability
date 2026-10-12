<template>
  <div class="h5-page">
    <div class="h5-wrap" v-loading="loading" element-loading-text="正在从区块链核验…">
      <!-- 顶部品牌区 -->
      <header class="hero">
        <div class="brand-line">
          <span class="logo">驴链</span>
          <span class="brand">DonkeyTrace · 驴肉火烧区块链溯源</span>
        </div>
        <div class="verify" :class="state">
          <template v-if="state === 'PASSED'">
            <span class="verify-icon">✓</span>
            <div class="verify-text">
              <b>区块链验证通过</b>
              <i>全链路 {{ report?.timeline?.length || 0 }} 个环节 · 存证不可篡改</i>
            </div>
          </template>
          <template v-else-if="state === 'DESTROYED'">
            <span class="verify-icon">!</span>
            <div class="verify-text"><b>该商品已销毁</b><i>溯源码已作废，无法查询链路</i></div>
          </template>
          <template v-else>
            <span class="verify-icon">✕</span>
            <div class="verify-text"><b>未查询到溯源信息</b><i>请确认溯源码是否正确，谨防假冒</i></div>
          </template>
        </div>
      </header>

      <!-- 商品信息 -->
      <section v-if="state === 'PASSED'" class="card product">
        <div class="p-name">{{ report?.product?.name }}</div>
        <div class="p-rows">
          <div class="p-row"><span>批次编号</span><b class="mono">{{ report?.product?.batchNo }}</b></div>
          <div class="p-row"><span>生产/上市日期</span><b>{{ report?.product?.produceDate }}</b></div>
          <div class="p-row" v-if="report?.product?.expireDate"><span>保质期至</span><b>{{ report?.product?.expireDate }}</b></div>
          <div class="p-row"><span>销售门店</span><b>{{ report?.product?.shopName }}</b></div>
        </div>
      </section>

      <!-- 全链路时间线 -->
      <section v-if="state === 'PASSED' && report?.timeline?.length" class="card">
        <h4 class="card-title">全链路追溯</h4>
        <div class="timeline">
          <div v-for="(t, i) in report.timeline" :key="i" class="tl-item">
            <div class="tl-dot" :style="{ background: stageColor(t.stage) }"></div>
            <div class="tl-body">
              <div class="tl-head">
                <span class="tl-title" :style="{ color: stageColor(t.stage) }">{{ t.title }}</span>
                <span class="tl-time">{{ t.time }}</span>
              </div>
              <div class="tl-org">{{ t.orgName }}</div>
              <div class="tl-summary">{{ t.summary }}</div>
              <div v-if="t.txId" class="tl-tx" @click="copy(t.txId)">
                链上存证 <span class="mono">{{ shortTx(t.txId) }}</span>
                <em>{{ copiedTx === t.txId ? '已复制' : '点击复制' }}</em>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 区块链存证 -->
      <section v-if="state === 'PASSED'" class="card chain-card">
        <h4 class="card-title">区块链存证</h4>
        <div class="chain-grid">
          <div class="c-item"><span>通道</span><b class="mono">{{ report?.blockchain?.channel }}</b></div>
          <div class="c-item"><span>上链事件</span><b>{{ report?.blockchain?.eventCount }} 次</b></div>
          <div class="c-item"><span>最新区块</span><b>#{{ report?.blockchain?.latestBlock }}</b></div>
        </div>
        <div class="chain-hash mono" v-if="report?.blockchain?.latestTxId">
          {{ report?.blockchain?.latestTxId }}
        </div>
      </section>

      <footer class="foot">驴链 DonkeyTrace · 保定驴肉火烧产业联盟<br /><span class="mono">溯源码 {{ code }}</span></footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { http } from '@/utils/request'

const route = useRoute()
const code = computed(() => String(route.params.code || ''))
const report = ref<any>(null)
const loading = ref(true)
const copiedTx = ref('')

const state = computed(() => (loading.value ? 'LOADING' : report.value?.verifyStatus || 'FAILED'))

async function load() {
  loading.value = true
  copiedTx.value = ''
  try {
    report.value = await http.get(`/trace/codes/${code.value}`)
  } catch (e) {
    report.value = null
  } finally {
    loading.value = false
  }
}

onMounted(load)
// 同一标签页切换溯源码时重新拉取
watch(code, (v, o) => { if (v && v !== o) load() })

function stageColor(stage: string) {
  const m: Record<string, string> = {
    FARM: '#16A34A', QUARANTINE: '#0EA5E9', SLAUGHTER: '#F59E0B',
    PROCESS: '#8B5CF6', PRODUCT: '#EC4899', QC: '#14B8A6',
    TRANSPORT: '#3B82F6', SHOP: '#10B981'
  }
  return m[stage] || '#64748B'
}
function shortTx(tx: string) {
  return tx.length > 22 ? tx.slice(0, 12) + '…' + tx.slice(-8) : tx
}
async function copy(tx: string) {
  try { await navigator.clipboard.writeText(tx); copiedTx.value = tx; setTimeout(() => (copiedTx.value = ''), 1500) } catch { /* 忽略 */ }
}
</script>

<style scoped>
.h5-page { min-height: 100vh; background: #eef2f7; }
.h5-wrap { max-width: 480px; margin: 0 auto; padding: 0 14px 24px; }
.hero { background: linear-gradient(135deg, #16A34A, #0D9488); margin: 0 -14px; padding: 26px 18px 20px; border-radius: 0 0 22px 22px; color: #fff; }
.brand-line { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
.logo { background: rgba(255,255,255,.18); border: 1px solid rgba(255,255,255,.45); border-radius: 8px; padding: 2px 8px; font-weight: 700; letter-spacing: 2px; }
.brand { font-size: 12px; opacity: .92; }
.verify { display: flex; align-items: center; gap: 12px; background: rgba(255,255,255,.14); border: 1px solid rgba(255,255,255,.35); border-radius: 14px; padding: 12px 14px; }
.verify-icon { width: 34px; height: 34px; border-radius: 50%; background: #fff; color: #16A34A; font-size: 20px; font-weight: 800; display: grid; place-items: center; flex: none; }
.verify.FAILED .verify-icon { color: #DC2626; }
.verify.DESTROYED .verify-icon { color: #D97706; }
.verify-text b { display: block; font-size: 16px; }
.verify-text i { display: block; font-style: normal; font-size: 12px; opacity: .9; margin-top: 2px; }
.card { background: #fff; border-radius: 16px; padding: 16px; margin-top: 14px; box-shadow: 0 2px 10px rgba(15,23,42,.06); }
.product { margin-top: -26px; }
.p-name { font-size: 17px; font-weight: 700; color: #0f172a; margin-bottom: 10px; }
.p-row { display: flex; justify-content: space-between; gap: 10px; padding: 6px 0; font-size: 13px; border-bottom: 1px dashed #eef2f7; }
.p-row:last-child { border-bottom: 0; }
.p-row span { color: #64748b; flex: none; }
.p-row b { color: #0f172a; text-align: right; font-weight: 600; }
.card-title { margin: 0 0 12px; font-size: 15px; color: #0f172a; }
.timeline { position: relative; }
.tl-item { position: relative; padding: 0 0 18px 22px; }
.tl-item::before { content: ''; position: absolute; left: 5px; top: 14px; bottom: -2px; width: 2px; background: #e2e8f0; }
.tl-item:last-child::before { display: none; }
.tl-dot { position: absolute; left: 0; top: 3px; width: 12px; height: 12px; border-radius: 50%; border: 2px solid #fff; box-shadow: 0 0 0 2px #e2e8f0; }
.tl-head { display: flex; justify-content: space-between; align-items: baseline; }
.tl-title { font-weight: 700; font-size: 14px; }
.tl-time { font-size: 12px; color: #94a3b8; }
.tl-org { font-size: 12px; color: #475569; margin-top: 2px; }
.tl-summary { font-size: 12.5px; color: #334155; margin-top: 4px; line-height: 1.55; }
.tl-tx { margin-top: 6px; font-size: 11.5px; color: #64748b; background: #f1f5f9; border-radius: 8px; padding: 6px 8px; display: flex; align-items: center; gap: 6px; cursor: pointer; }
.tl-tx em { font-style: normal; margin-left: auto; color: #16A34A; }
.chain-card { background: linear-gradient(135deg, #f8fafc, #ecfdf5); }
.chain-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 8px; }
.c-item { background: #fff; border-radius: 10px; padding: 10px 8px; text-align: center; }
.c-item span { display: block; font-size: 11px; color: #64748b; }
.c-item b { display: block; font-size: 14px; color: #0f172a; margin-top: 3px; }
.chain-hash { margin-top: 10px; font-size: 10.5px; color: #64748b; word-break: break-all; background: #fff; border-radius: 8px; padding: 8px; }
.foot { text-align: center; color: #94a3b8; font-size: 11.5px; line-height: 1.8; margin-top: 22px; }
.mono { font-family: Consolas, Menlo, monospace; }
</style>
