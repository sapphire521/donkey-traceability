<template>
  <div class="page">
    <!-- 渐变横幅（参考政企平台页头风格） -->
    <div class="hero stagger">
      <div class="hero-main">
        <h2 class="hero-title">{{ greeting }}，{{ user.user?.realName }} 👋</h2>
        <div class="hero-chips">
          <span class="chip">{{ user.user?.orgName }}</span>
          <span class="chip">{{ user.user?.roleName }}</span>
          <span class="chip chain"><span class="pulse"></span>联盟链状态正常</span>
        </div>
      </div>
      <div class="hero-side">
        <div class="hero-date mono">{{ now }}</div>
        <div class="hero-sub">全链路溯源 · 一码到底</div>
      </div>
    </div>

    <div class="stagger cards">
      <StatCard v-for="c in cards" :key="c.key" :label="c.label" :value="c.value" :unit="c.unit" :icon="c.icon" :color="c.color" />
    </div>

    <div class="grid-2">
      <el-card class="chart-card">
        <template #header><span class="card-h">近 30 天上链事件 / 扫码趋势</span></template>
        <EChart :option="trendOption" height="280px" />
      </el-card>
      <el-card class="chart-card">
        <template #header><span class="card-h">溯源环节事件占比</span></template>
        <EChart :option="pieOption" height="280px" />
      </el-card>
    </div>

    <div class="grid-2">
      <el-card class="quick-card">
        <template #header><span class="card-h">快捷入口</span></template>
        <div class="quick">
          <div v-for="q in quickEntries" :key="q.path" class="quick-item" @click="go(q.path)">
            <div class="quick-icon"><el-icon v-if="q.icon"><component :is="q.icon" /></el-icon></div>
            <span>{{ q.title }}</span>
          </div>
        </div>
      </el-card>

      <el-card class="alert-card">
        <template #header>
          <span class="card-h">最新预警</span>
          <el-button link type="primary" size="small" @click="go('/alert')">全部</el-button>
        </template>
        <div v-for="a in alerts" :key="a.id" class="alert-row" @click="go('/alert')">
          <span class="dot" :class="levelClass(a.level)"></span>
          <span class="alert-content ellipsis">{{ a.content }}</span>
          <span class="alert-time mono">{{ a.createTime.slice(5, 16) }}</span>
        </div>
        <el-empty v-if="!alerts.length" description="暂无预警" :image-size="60" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { http } from '@/utils/request'
import StatCard from '@/components/StatCard.vue'
import EChart from '@/components/EChart.vue'
import type { MenuNode } from '@/types'

const user = useUserStore()
const router = useRouter()
const now = ref(new Date().toLocaleString('zh-CN'))
const cards = ref<any[]>([])
const alerts = ref<any[]>([])
const trends = ref<any[]>([])
const stageDist = ref<any[]>([])

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好'
  if (h < 12) return '上午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const quickEntries = computed(() => {
  const out: { path: string; title: string; icon?: string }[] = []
  const walk = (nodes: MenuNode[]) => nodes.forEach(n => {
    if (n.children?.length) walk(n.children)
    else if (n.path !== '/dashboard') out.push({ path: n.path, title: n.title, icon: n.icon })
  })
  walk(user.menus)
  return out.slice(0, 8)
})

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['上链事件', '扫码次数'], right: 0, top: 0, textStyle: { color: 'var(--text-3)' } },
  grid: { left: 36, right: 12, top: 36, bottom: 24 },
  xAxis: { type: 'category', data: trends.value.map(t => t.date), axisLine: { lineStyle: { color: 'var(--border-strong)' } }, axisLabel: { color: 'var(--text-3)', fontSize: 10 } },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: 'var(--border)' } }, axisLabel: { color: 'var(--text-3)' } },
  series: [
    { name: '上链事件', type: 'line', smooth: true, showSymbol: false, data: trends.value.map(t => t.events),
      lineStyle: { width: 3, color: '#16a34a', shadowColor: 'rgba(22,163,74,0.25)', shadowBlur: 10, shadowOffsetY: 6 },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(22,163,74,0.26)' }, { offset: 1, color: 'rgba(22,163,74,0.02)' }] } } },
    { name: '扫码次数', type: 'line', smooth: true, showSymbol: false, data: trends.value.map(t => t.scans),
      lineStyle: { width: 3, color: '#0d9488', shadowColor: 'rgba(13,148,136,0.25)', shadowBlur: 10, shadowOffsetY: 6 },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(13,148,136,0.22)' }, { offset: 1, color: 'rgba(13,148,136,0.02)' }] } } }
  ]
}))
const pieOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0, textStyle: { color: 'var(--text-3)' } },
  series: [{
    type: 'pie', radius: ['46%', '70%'], center: ['50%', '44%'], avoidLabelOverlap: true,
    itemStyle: { borderColor: 'var(--bg-card)', borderWidth: 3, borderRadius: 8 },
    label: { color: 'var(--text-2)' },
    emphasis: { scaleSize: 6 },
    data: stageDist.value.map(d => ({ name: d.name, value: d.value }))
  }],
  color: ['#16a34a', '#0d9488', '#3b82f6', '#8b5cf6', '#f59e0b', '#ef4444']
}))

function levelClass(l: string) { return l === 'CRITICAL' ? 'red' : l === 'WARN' ? 'amber' : 'blue' }
function go(p: string) { router.push(p) }

onMounted(async () => {
  const [ov, al] = await Promise.all([
    http.get('/stats/overview'),
    http.get('/alerts', { page: 1, size: 5 })
  ])
  cards.value = ov.cards
  trends.value = ov.trends
  stageDist.value = ov.stageDist
  alerts.value = al.records
  setInterval(() => (now.value = new Date().toLocaleString('zh-CN')), 1000)
})
</script>

<style scoped>
/* 渐变横幅 */
.hero {
  position: relative;
  overflow: hidden;
  border-radius: var(--radius);
  padding: 24px 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  background: var(--grad-banner);
  color: #fff;
  box-shadow: 0 14px 34px rgba(13, 84, 46, 0.32);
  margin-bottom: 18px;
}
.hero::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 30px 30px;
  mask-image: linear-gradient(100deg, transparent 40%, #000 100%);
}
.hero::after {
  content: '';
  position: absolute;
  right: -70px;
  top: -90px;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.14);
}
.hero-main, .hero-side { position: relative; z-index: 1; }
.hero-title { margin: 0; font-size: 21px; font-weight: 700; letter-spacing: 0.4px; }
.hero-chips { display: flex; gap: 8px; margin-top: 12px; flex-wrap: wrap; }
.chip {
  font-size: 12px;
  padding: 4px 11px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.13);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.16);
  display: inline-flex;
  align-items: center;
}
.chip.chain { background: rgba(74, 222, 128, 0.18); }
.pulse {
  width: 7px; height: 7px; border-radius: 50%;
  background: #4ade80;
  margin-right: 7px;
  animation: hero-pulse 2.2s infinite;
}
@keyframes hero-pulse {
  0% { box-shadow: 0 0 0 0 rgba(74, 222, 128, 0.5); }
  70% { box-shadow: 0 0 0 6px rgba(74, 222, 128, 0); }
  100% { box-shadow: 0 0 0 0 rgba(74, 222, 128, 0); }
}
.hero-side { text-align: right; }
.hero-date { font-size: 13px; color: rgba(255, 255, 255, 0.92); }
.hero-sub { font-size: 12px; color: rgba(255, 255, 255, 0.6); margin-top: 4px; }

.cards { display: grid; grid-template-columns: repeat(6, 1fr); gap: 14px; margin-bottom: 16px; }
.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
.card-h { font-weight: 600; color: var(--text-1); display: inline-flex; align-items: center; gap: 8px; }
.card-h::before { content: ''; width: 3px; height: 13px; border-radius: 2px; background: var(--grad-brand); }
.chart-card :deep(.el-card__header) { padding: 14px 18px; border-bottom: 1px solid var(--border); }
.quick { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.quick-item {
  display: flex; flex-direction: column; align-items: center; gap: 9px;
  padding: 16px 8px; border-radius: 12px; border: 1px solid var(--border);
  cursor: pointer; transition: all 0.18s ease; color: var(--text-2); font-size: 13px;
  background: var(--bg-card);
}
.quick-icon {
  width: 42px; height: 42px; border-radius: 12px;
  display: grid; place-items: center; font-size: 20px;
  color: #fff;
  background: var(--grad-brand);
  box-shadow: 0 5px 12px rgba(22, 163, 74, 0.3);
}
.quick-item:hover { border-color: var(--brand); color: var(--brand); transform: translateY(-3px); box-shadow: var(--shadow-md); }
.alert-card :deep(.el-card__header) { display: flex; justify-content: space-between; align-items: center; padding: 14px 18px; }
.alert-row { display: flex; align-items: center; gap: 10px; padding: 10px 4px; border-bottom: 1px dashed var(--border); cursor: pointer; border-radius: 8px; transition: background 0.15s ease; }
.alert-row:hover { background: var(--bg-hover); }
.alert-row:last-child { border-bottom: none; }
.alert-content { flex: 1; color: var(--text-2); font-size: 13px; }
.alert-time { color: var(--text-3); font-size: 12px; }
@media (max-width: 1280px) { .cards { grid-template-columns: repeat(3, 1fr); } .grid-2 { grid-template-columns: 1fr; } }
</style>
