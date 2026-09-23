<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h3 class="page-title"><span class="bar"></span>产出记录</h3>
        <p class="page-subtitle">门店制作火烧时领用成品批次，生成产出记录（炉次 / 师傅 / 数量）</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openAdd">新增产出</el-button>
    </div>

    <el-card class="card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="outputTime" label="出锅时间" width="170" />
        <el-table-column prop="batchNo" label="来源批次" width="150" />
        <el-table-column prop="shopOrgName" label="门店" min-width="160" />
        <el-table-column prop="ovenNo" label="炉次" width="100" />
        <el-table-column prop="chef" label="师傅" width="100" />
        <el-table-column label="领用重量" width="110"><template #default="{ row }">{{ row.usedWeight }} kg</template></el-table-column>
        <el-table-column label="成品数量" width="100"><template #default="{ row }">{{ row.qty }} 份</template></el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog" title="新增产出记录" width="440px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="来源批次"><el-input v-model="form.batchNo" placeholder="F-20260911-001" /></el-form-item>
        <el-form-item label="门店"><el-input v-model="form.shopOrgName" placeholder="驴火·裕华路总店" /></el-form-item>
        <el-form-item label="炉次"><el-input v-model="form.ovenNo" /></el-form-item>
        <el-form-item label="师傅"><el-input v-model="form.chef" /></el-form-item>
        <el-form-item label="领用重量"><el-input-number v-model="form.usedWeight" :precision="1" style="width: 100%" /></el-form-item>
        <el-form-item label="成品数量"><el-input-number v-model="form.qty" :min="1" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" @click="submit">生成</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { http } from '@/utils/request'
import { ElMessage } from 'element-plus'

const list = ref<any[]>([])
const loading = ref(false)
const dialog = ref(false)
const form = ref({ batchNo: '', shopOrgName: '', ovenNo: '3号炉', chef: '', usedWeight: 10, qty: 50 })

async function load() { loading.value = true; list.value = await http.get('/shop/outputs', { page: 1, size: 50 }); loading.value = false }
function openAdd() { form.value = { batchNo: '', shopOrgName: '', ovenNo: '3号炉', chef: '', usedWeight: 10, qty: 50 }; dialog.value = true }
async function submit() { await http.post('/shop/outputs', form.value); ElMessage.success('产出记录已生成'); dialog.value = false; load() }
onMounted(load)
</script>
