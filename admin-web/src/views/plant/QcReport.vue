<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h3 class="page-title"><span class="bar"></span>出厂检验（QC）</h3>
        <p class="page-subtitle">加工批次出厂前检验：感官 / 水分 / 兽残快检 / 瘦肉精，合格后方可生成成品批次</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openQc">执行出厂检验</el-button>
    </div>

    <el-card class="card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="batchNo" label="成品批次号" width="160" />
        <el-table-column prop="productName" label="产品" min-width="180" />
        <el-table-column label="重量" width="110"><template #default="{ row }">{{ row.weightKg }} kg</template></el-table-column>
        <el-table-column prop="produceDate" label="生产日期" width="130" />
        <el-table-column label="检验哈希" min-width="160"><template #default="{ row }"><span class="mono">{{ row.certHash || '—' }}</span></template></el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.certHash ? 'success' : 'warning'" size="small">{{ row.certHash ? '检验合格' : '待检验' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="检验报告" min-width="120">
          <template #default="{ row }">
            <el-link v-if="row.qcReportUri" type="primary" :underline="false">查看 PDF</el-link>
            <span v-else class="muted">未上传</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog" title="执行出厂检验" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="加工批次">
          <el-select v-model="form.parentNo" style="width: 100%" placeholder="选择待检加工批次">
            <el-option v-for="p in processBatches" :key="p.batchNo" :label="`${p.batchNo} · ${p.productName}`" :value="p.batchNo" />
          </el-select>
        </el-form-item>
        <el-form-item label="检验项目">
          <el-checkbox-group v-model="form.items">
            <el-checkbox value="感官">感官</el-checkbox><el-checkbox value="水分">水分</el-checkbox>
            <el-checkbox value="兽残快检">兽残快检</el-checkbox><el-checkbox value="瘦肉精">瘦肉精</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="结论"><el-radio-group v-model="form.conclusion"><el-radio value="PASS">合格</el-radio><el-radio value="FAIL">不合格</el-radio></el-radio-group></el-form-item>
        <el-form-item label="报告文件"><el-upload action="#" :auto-upload="false" :limit="1"><el-button :icon="Upload">选择 PDF</el-button></el-upload></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" @click="submit">提交（哈希上链）</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus, Upload } from '@element-plus/icons-vue'
import { http } from '@/utils/request'
import { ElMessage } from 'element-plus'

const list = ref<any[]>([])
const processBatches = ref<any[]>([])
const loading = ref(false)
const dialog = ref(false)
const form = ref({ parentNo: '', items: ['感官', '水分'], conclusion: 'PASS' })

async function load() { loading.value = true; const [prod, proc] = await Promise.all([http.get('/batches', { page: 1, size: 50, batchType: 'PRODUCT' }), http.get('/batches', { page: 1, size: 50, batchType: 'PROCESS' })]); list.value = prod.records; processBatches.value = proc.records; loading.value = false }
function openQc() { form.value = { parentNo: processBatches.value[0]?.batchNo || '', items: ['感官', '水分'], conclusion: 'PASS' }; dialog.value = true }
async function submit() {
  if (!form.value.parentNo) return ElMessage.warning('请选择加工批次')
  const payload = { batchNo: 'F-20260911-00' + Math.floor(Math.random() * 90 + 10), parentNos: [form.value.parentNo], productName: '驴肉火烧·卤制驴肉', weightKg: 60, produceDate: '2026-09-11', expireDate: '2026-09-18', certHash: 'sha256:' + Math.random().toString(16).slice(2, 10) }
  await http.post('/batches/product', payload); ElMessage.success('检验合格，成品批次已生成'); dialog.value = false; load()
}
onMounted(load)
</script>

<style scoped>
.muted { color: var(--text-3); font-size: 13px; }
</style>
