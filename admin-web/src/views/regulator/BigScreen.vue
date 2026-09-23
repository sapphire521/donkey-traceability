<template>
  <div class="screen">
    <header class="screen-head">
      <div class="sh-left">
        <span class="sh-dot"></span>
        驴肉火烧 · 监管驾驶舱
      </div>
      <div class="sh-title">DONKEYTRACE 全链路溯源监管数据大屏</div>
      <div class="sh-right mono">{{ now }}</div>
    </header>

    <div class="screen-body">
      <div class="col">
        <div class="kpi-grid">
          <div class="kpi" v-for="k in kpis" :key="k.label">
            <div class="kpi-value mono">{{ k.value }}<span>{{ k.unit }}</span></div>
            <div class="kpi-label">{{ k.label }}</div>
          </div>
        </div>
        <el-card class="panel" body-style="padding:12px 16px">
          <div class="panel-h">近 30 天上链 / 扫码趋势</div>
          <EChart :option="trendOption" height="240px" />
        </el-card>
      </div>

      <div class="col">
        <el-card class="panel" body-style="padding:12px 16px">
          <div class="panel-h">环节事件占比</div>
          <EChart :option="pieOption" height="240px" />
        </el-card>
        <el-card class="panel" body-style="padding:12px 16px">
          <div class="panel-h">地域分布（门店维度）</div>
          <EChart :option="barOption" height="240px" />
        </el-card>
      </div>

      <div class="col">
        <el-card class="panel" body-style="padding:12px 16px">
          <div class="panel-h">抽检合格率</div>
          <EChart :option="gaugeOption" height="240px" />
        </el-card>
        <el-card class="panel alert-panel" body-style="padding:8px 12px">
          <div class="panel-h">实时预警 / 召回</div>
          <div class="alert-row" v-for="a in alerts" :key="a.id">
            <span class="dot" :class="levelClass(a.level)"></span>
            <span class="alert-c ellipsis">{{ a.content }}</span>
            <span class="alert-t mono">{{ a.createTime.slice(5, 16) }}</span>
          </div>
          <div class="recall-stat">
            待处理召回：<b class="mono">{{ recallActive }}</b> 起 ｜ 临期证照：<b class="mono">{{ certExpiring }}</b> 份
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { http } from '@/utils/request'
import EChart from '@/components/EChart.vue'

const now = ref('')
const data = ref<any>({})
const kpis = computed(() => (data.value.cards || []).map((c: any) => ({ label: c.label, value: c.value, unit: c.unit })))
const alerts = ref<any[]>([])
const recallActive = ref(0)
const certExpiring = ref(0)

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' }, grid: { left: 36, right: 12, top: 30, bottom: 24 },
  legend: { data: ['上链事件', '扫码'], textStyle: { color: '#94a3b8' }, top: 0, right: 0 },
  xAxis: { type: 'category', data: (data.value.trends || []).map((t: any) => t.date), axisLabel: { color: '#64748b', fontSize: 9 }, axisLine: { lineStyle: { color: '#27314a' } } },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#1f2940' } }, axisLabel: { color: '#64748b' } },
  series: [
    { name: '上链事件', type: 'line', smooth: true, showSymbol: false, data: (data.value.trends || []).map((t: any) => t.events), lineStyle: { color: '#22c55e', width: 2 }, areaStyle: { color: 'rgba(34,197,94,0.18)' } },
    { name: '扫码', type: 'line', smooth: true, showSymbol: false, data: (data.value.trends || []).map((t: any) => t.scans), lineStyle: { color: '#38bdf8', width: 2 } }
  ]
}))
const pieOption = computed(() => ({
  tooltip: { trigger: 'item' }, legend: { bottom: 0, textStyle: { color: '#94a3b8' } },
  series: [{ type: 'pie', radius: ['42%', '66%'], center: ['50%', '44%'], itemStyle: { borderColor: '#1b2336', borderWidth: 2 }, label: { color: '#cbd5e1' }, data: (data.value.stageDist || []).map((d: any) => ({ name: d.name, value: d.value })) }],
  color: ['#22c55e', '#4ade80', '#38bdf8', '#a78bfa', '#fbbf24', '#f87171']
}))
const barOption = computed(() => ({
  tooltip: { trigger: 'axis' }, grid: { left: 40, right: 16, top: 16, bottom: 24 },
  xAxis: { type: 'category', data: (data.value.regionDist || []).map((d: any) => d.name), axisLabel: { color: '#64748b' }, axisLine: { lineStyle: { color: '#27314a' } } },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#1f2940' } }, axisLabel: { color: '#64748b' } },
  series: [{ type: 'bar', data: (data.value.regionDist || []).map((d: any) => d.value), itemStyle: { color: '#22c55e', borderRadius: [4, 4, 0, 0] }, barWidth: '46%' }]
}))
const gaugeOption = computed(() => ({
  series: [{ type: 'gauge', startAngle: 210, endAngle: -30, radius: '92%', progress: { show: true, width: 14, itemStyle: { color: '#22c55e' } }, axisLine: { lineStyle: { width: 14, color: [[1, '#27314a']] } }, axisTick: { show: false }, splitLine: { show: false }, axisLabel: { show: false }, pointer: { show: false }, detail: { valueAnimation: true, fontSize: 30, color: '#22c55e', formatter: '{value}%', offsetCenter: [0, '5%'] }, data: [{ value: data.value.quality?.passRate || 98.6 }] }]
}))
function levelClass(l: string) { return l === 'CRITICAL' ? 'red' : l === 'WARN' ? 'amber' : 'blue' }

async function load() {
  const [d, a] = await Promise.all([http.get('/regulator/dashboard'), http.get('/alerts', { page: 1, size: 6 })])
  data.value = d; alerts.value = a.records; recallActive.value = d.recallActive; certExpiring.value = d.certExpiring
}
onMounted(() => { load(); setInterval(() => (now.value = new Date().toLocaleString('zh-CN')), 1000) })
</script>

<style scoped>
.screen { height: 100%; background: #0f172a; color: #f8fafc; padding: 14px 18px; display: flex; flex-direction: column; }
.screen-head { display: flex; align-items: center; justify-content: space-between; padding-bottom: 12px; border-bottom: 1px solid #27314a; }
.sh-left { font-size: 15px; font-weight: 600; display: flex; align-items: center; gap: 8px; color: #22c55e; }
.sh-dot { width: 10px; height: 10px; border-radius: 50%; background: #22c55e; box-shadow: 0 0 10px #22c55e; }
.sh-title { font-size: 22px; font-weight: 700; letter-spacing: 2px; background: linear-gradient(90deg, #22c55e, #38bdf8); -webkit-background-clip: text; background-clip: text; color: transparent; }
.sh-right { color: #94a3b8; font-size: 13px; }
.screen-body { flex: 1; display: grid; grid-template-columns: 1.1fr 1fr 1fr; gap: 14px; padding-top: 14px; min-height: 0; }
.col { display: flex; flex-direction: column; gap: 14px; min-height: 0; }
.kpi-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.kpi { background: #1b2336; border: 1px solid #27314a; border-radius: 12px; padding: 14px; text-align: center; }
.kpi-value { font-size: 26px; font-weight: 700; color: #22c55e; }
.kpi-value span { font-size: 12px; color: #64748b; margin-left: 3px; }
.kpi-label { font-size: 12px; color: #94a3b8; margin-top: 4px; }
.panel { background: #1b2336; border: 1px solid #27314a; border-radius: 12px; flex: 1; min-height: 0; }
.panel-h { font-size: 14px; font-weight: 600; color: #f8fafc; margin-bottom: 6px; }
.alert-panel { display: flex; flex-direction: column; }
.alert-row { display: flex; align-items: center; gap: 8px; padding: 8px 0; border-bottom: 1px dashed #27314a; font-size: 12px; color: #cbd5e1; }
.alert-c { flex: 1; }
.alert-t { color: #64748b; }
.recall-stat { margin-top: auto; padding-top: 10px; font-size: 13px; color: #fbbf24; }
</style>
