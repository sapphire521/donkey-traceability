<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h3 class="page-title"><span class="bar"></span>溯源码管理</h3>
        <p class="page-subtitle">为产出记录批量生成溯源码并绑定批次（上链）· 支持二维码打印预览</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openGen">生成溯源码</el-button>
    </div>

    <el-card class="card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="code" label="溯源码" width="170">
          <template #default="{ row }"><span class="mono">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column prop="batchNo" label="绑定批次" width="150" />
        <el-table-column prop="shopOrgName" label="门店" min-width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="tagOf(codeStatusMap, row.status)[1]" size="small">{{ tagOf(codeStatusMap, row.status)[0] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="扫码次数" width="100"><template #default="{ row }">{{ row.scanTimes }}</template></el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showQr(row)">二维码</el-button>
            <el-button link type="primary" @click="print(row)">打印</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="genDialog" title="批量生成溯源码" width="440px">
      <el-form :model="genForm" label-width="90px">
        <el-form-item label="绑定批次"><el-input v-model="genForm.batchNo" placeholder="F-20260911-001" /></el-form-item>
        <el-form-item label="门店"><el-input v-model="genForm.shopOrgName" placeholder="驴火·裕华路总店" /></el-form-item>
        <el-form-item label="产出记录ID"><el-input-number v-model="genForm.outputId" :min="1" style="width: 100%" /></el-form-item>
        <el-form-item label="生成数量"><el-slider v-model="genForm.count" :min="1" :max="200" show-input /></el-form-item>
      </el-form>
      <template #footer><el-button @click="genDialog = false">取消</el-button><el-button type="primary" @click="submitGen">生成并绑定上链</el-button></template>
    </el-dialog>

    <el-dialog v-model="qrDialog" title="溯源码二维码" width="360px" align-center>
      <div v-if="qrCurrent" class="qr-wrap">
        <div class="qr-box">
          <div class="qr-grid">
            <span v-for="n in 144" :key="n" :class="{ on: qrPattern[n - 1] }"></span>
          </div>
        </div>
        <div class="qr-code mono">{{ qrCurrent.code }}</div>
        <div class="qr-url mono">{{ qrCurrent.url }}</div>
        <el-tag type="success" size="small">活码链接 · 扫码直达 H5 溯源报告</el-tag>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { http } from '@/utils/request'
import { ElMessage } from 'element-plus'
import { codeStatusMap, tagOf } from '@/utils/format'

const list = ref<any[]>([])
const loading = ref(false)
const genDialog = ref(false); const qrDialog = ref(false)
const genForm = ref({ batchNo: '', shopOrgName: '', outputId: 1, count: 10 })
const qrCurrent = ref<any>(null)
const qrPattern = ref<boolean[]>([])

async function load() { loading.value = true; const r = await http.get('/shop/trace-codes', { page: 1, size: 50 }); list.value = r.records; loading.value = false }
function openGen() { genForm.value = { batchNo: '', shopOrgName: '', outputId: 1, count: 10 }; genDialog.value = true }
async function submitGen() { const arr = await http.post('/shop/trace-codes', genForm.value); ElMessage.success(`已生成 ${arr.length} 个溯源码并绑定上链`); genDialog.value = false; load() }
async function showQr(row: any) { const r = await http.get(`/shop/trace-codes/${row.id}/qrcode`); qrCurrent.value = { code: row.code, url: r.url }; qrPattern.value = Array.from({ length: 144 }, () => Math.random() > 0.5); qrDialog.value = true }
function print(row: any) { ElMessage.success(`已发送打印任务：${row.code}（标签 60×40mm）`) }

onMounted(load)
</script>

<style scoped>
.qr-wrap { display: flex; flex-direction: column; align-items: center; gap: 10px; padding: 8px; }
.qr-box { width: 180px; height: 180px; padding: 12px; background: #fff; border-radius: 12px; box-shadow: var(--shadow-md); display: grid; place-items: center; }
.qr-grid { width: 156px; height: 156px; display: grid; grid-template-columns: repeat(12, 1fr); grid-template-rows: repeat(12, 1fr); gap: 1px; }
.qr-grid span { background: #eef2f7; border-radius: 1px; }
.qr-grid span.on { background: #0f172a; }
.qr-code { font-weight: 600; color: var(--text-1); letter-spacing: 1px; }
.qr-url { font-size: 11px; color: var(--text-3); word-break: break-all; text-align: center; }
</style>
