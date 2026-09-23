<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h3 class="page-title"><span class="bar"></span>批次树可视化</h3>
        <p class="page-subtitle">从成品批次沿父批次递归回溯到驴只个体，直观呈现全链路血缘</p>
      </div>
      <el-select v-model="selected" placeholder="选择批次" style="width: 240px" @change="loadTree" filterable>
        <el-option v-for="b in batches" :key="b.batchNo" :label="`${b.batchNo} · ${b.productName}`" :value="b.batchNo" />
      </el-select>
    </div>

    <el-card class="card tree-card" v-loading="loading">
      <EChart :option="treeOption" height="560px" v-if="treeData" />
      <el-empty v-else description="请选择批次查看批次树" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { http } from '@/utils/request'
import EChart from '@/components/EChart.vue'

const route = useRoute()
const batches = ref<any[]>([])
const selected = ref('')
const treeData = ref<any>(null)
const loading = ref(false)

const statusColor: Record<string, string> = { CREATED: '#3b82f6', IN_TRANSIT: '#f59e0b', RECEIVED: '#94a3b8', IN_STORE: '#16a34a', SOLD: '#16a34a', RECALLED: '#ef4444', EXPIRED: '#64748b', RAISED: '#16a34a' }

function buildOption(node: any) {
  return {
    name: node.name,
    itemStyle: { color: statusColor[node.status] || '#16a34a' },
    lineStyle: { color: '#cbd5e1' },
    label: { color: 'var(--text-1)', fontSize: 12, fontWeight: 600 },
    children: (node.children || []).map(buildOption)
  }
}
const treeOption = computed(() => ({
  tooltip: { trigger: 'item', triggerOn: 'mousemove' },
  series: [{
    type: 'tree', data: [buildOption(treeData.value)],
    top: '4%', left: '10%', bottom: '4%', right: '22%',
    symbolSize: 14, symbol: 'circle',
    initialTreeDepth: 5, expandAndCollapse: false,
    lineStyle: { color: '#cbd5e1', width: 1.5 },
    label: { position: 'left', verticalAlign: 'middle', align: 'right', fontSize: 12, color: 'var(--text-1)' },
    leaves: { label: { position: 'right', verticalAlign: 'middle', align: 'left' } },
    emphasis: { focus: 'descendant' }
  }]
}))

async function loadTree() {
  if (!selected.value) return
  loading.value = true
  treeData.value = await http.get(`/trace/batch-tree/${selected.value}`)
  loading.value = false
}
onMounted(async () => {
  const r = await http.get('/batches', { page: 1, size: 100 })
  batches.value = r.records
  selected.value = (route.query.batchNo as string) || batches.value[0]?.batchNo || ''
  if (selected.value) loadTree()
})
</script>

<style scoped>
.tree-card { min-height: 600px; }
</style>
