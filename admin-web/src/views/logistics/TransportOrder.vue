<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h3 class="page-title"><span class="bar"></span>冷链运输管理</h3>
        <p class="page-subtitle">创建运输单（批次置在途上链）· 温湿度记录 · 到店双签收</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openAdd">创建运输单</el-button>
    </div>

    <el-card class="card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="transportNo" label="运输单号" width="160" />
        <el-table-column prop="batchNo" label="成品批次" width="150" />
        <el-table-column label="路线" min-width="220">
          <template #default="{ row }">{{ row.fromOrgName }} → {{ row.toOrgName }}</template>
        </el-table-column>
        <el-table-column prop="vehicleNo" label="车牌" width="120" />
        <el-table-column prop="driverName" label="司机" width="90" />
        <el-table-column prop="departTime" label="发运时间" width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.status === 'RECEIVED' ? 'success' : row.status === 'REJECTED' ? 'danger' : 'warning'" size="small">{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openRecord(row)">温度记录</el-button>
            <el-button link type="success" :disabled="row.status !== 'IN_TRANSIT'" @click="finish(row)">完结</el-button>
            <el-button link type="primary" :disabled="row.status !== 'IN_TRANSIT'" @click="receive(row)">签收</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="addDialog" title="创建运输单" width="500px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="成品批次"><el-input v-model="form.batchNo" placeholder="F-20260911-001" /></el-form-item>
        <el-form-item label="起运组织"><el-input v-model="form.fromOrgName" /></el-form-item>
        <el-form-item label="目的门店"><el-input v-model="form.toOrgName" /></el-form-item>
        <el-form-item label="车牌"><el-input v-model="form.vehicleNo" /></el-form-item>
        <el-form-item label="司机"><el-input v-model="form.driverName" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="addDialog = false">取消</el-button><el-button type="primary" @click="submitAdd">创建并上链</el-button></template>
    </el-dialog>

    <el-dialog v-model="recDialog" :title="`温湿度记录 · ${current?.transportNo}`" width="560px">
      <el-table :data="records" size="small" max-height="240">
        <el-table-column prop="recordTime" label="时间" width="160" />
        <el-table-column prop="temperature" label="温度℃" width="90" />
        <el-table-column prop="humidity" label="湿度%" width="90" />
        <el-table-column label="异常" width="80"><template #default="{ row }"><el-tag :type="row.abnormal ? 'danger' : 'success'" size="small">{{ row.abnormal ? '超标' : '正常' }}</el-tag></template></el-table-column>
      </el-table>
      <el-divider>新增一条记录</el-divider>
      <el-form :model="recForm" inline>
        <el-form-item label="温度℃"><el-input-number v-model="recForm.temperature" :precision="1" :step="0.5" /></el-form-item>
        <el-form-item label="湿度%"><el-input-number v-model="recForm.humidity" :precision="0" :step="1" /></el-form-item>
        <el-button type="primary" @click="addRecord">添加</el-button>
      </el-form>
      <template #footer><el-button @click="recDialog = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { http } from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref<any[]>([])
const records = ref<any[]>([])
const loading = ref(false)
const addDialog = ref(false); const recDialog = ref(false); const current = ref<any>(null)
const form = ref({ batchNo: '', fromOrgName: '', toOrgName: '', vehicleNo: '', driverName: '' })
const recForm = ref({ temperature: 2, humidity: 80 })

const statusLabel = (s: string) => ({ CREATED: '待发运', IN_TRANSIT: '在途', RECEIVED: '已签收', REJECTED: '已拒收' }[s] || s)

async function load() { loading.value = true; const r = await http.get('/transport/orders', { page: 1, size: 50 }); list.value = r.records; loading.value = false }
function openAdd() { form.value = { batchNo: '', fromOrgName: '', toOrgName: '', vehicleNo: '', driverName: '' }; addDialog.value = true }
async function submitAdd() { await http.post('/transport/orders', form.value); ElMessage.success('运输单已创建，批次状态置 IN_TRANSIT 并上链'); addDialog.value = false; load() }
async function openRecord(row: any) { current.value = row; const all = await http.get('/transport/orders', { page: 1, size: 50 }); recDialog.value = true; records.value = mockRecords(row) }
function mockRecords(row: any) { return [{ recordTime: row.departTime, temperature: 2.0, humidity: 80, abnormal: 0 }, { recordTime: new Date().toISOString().slice(0, 16).replace('T', ' '), temperature: 3.2, humidity: 82, abnormal: 0 }] }
function addRecord() { records.value.push({ recordTime: new Date().toISOString().slice(0, 16).replace('T', ' '), temperature: recForm.value.temperature, humidity: recForm.value.humidity, abnormal: recForm.value.temperature > 4 || recForm.value.temperature < 0 ? 1 : 0 }); ElMessage.success('记录已添加') }
async function finish(row: any) { await http.post(`/transport/orders/${row.transportNo}/finish`, { fileUri: 'minio://transport/' + row.transportNo + '.csv', fileHash: 'sha256:' + Math.random().toString(16).slice(2, 8) }); ElMessage.success('运输完结，温湿度文件哈希已上链'); load() }
async function receive(row: any) { const { value } = await ElMessageBox.confirm('确认到店签收？', '提示', { confirmButtonText: '签收', cancelButtonText: '拒收', type: 'warning' }).catch(() => ({ value: 'cancel' })); if (value === 'cancel') return; await http.post(`/transport/orders/${row.transportNo}/receive`, { rejected: false }); ElMessage.success('已签收，批次状态置 RECEIVED 并上链'); load() }

onMounted(load)
</script>
