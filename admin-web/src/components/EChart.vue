<template>
  <div ref="el" class="echart" :style="{ height }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps<{ option: any; height?: string }>()
const el = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

function render() {
  if (chart && props.option) chart.setOption(props.option, true)
}
function resize() { chart?.resize() }

onMounted(() => {
  chart = echarts.init(el.value as HTMLElement)
  render()
  window.addEventListener('resize', resize)
})
watch(() => props.option, render, { deep: true })
onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.echart { width: 100%; }
</style>
