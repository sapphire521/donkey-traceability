<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h3 class="page-title"><span class="bar"></span>门店库存管理</h3>
        <p class="page-subtitle">在库成品批次、剩余重量与保质期倒计时，临期标黄、过期标红禁止领用</p>
      </div>
    </div>

    <el-card class="card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="productName" label="产品" min-width="200" />
        <el-table-column prop="batchNo" label="批次号" width="150" />
        <el-table-column label="总重量" width="110"><template #default="{ row }">{{ row.weightKg }} kg</template></el-table-column>
        <el-table-column label="剩余" width="120"><template #default="{ row }"><b :class="row.daysLeft <= 3 ? 'warn' : ''">{{ row.remainingKg }} kg</b></template></el-table-column>
        <el-table-column prop="produceDate" label="生产日期" width="120" />
        <el-table-column prop="expireDate" label="保质期至" width="120" />
        <el-table-column label="剩余天数" width="110">
          <template #default="{ row }">
            <el-tag :type="row.daysLeft <= 3 ? 'danger' : row.daysLeft <= 7 ? 'warning' : 'success'" size="small">{{ row.daysLeft }} 天</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.daysLeft <= 0" @click="openOutput(row)">领用制作</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog" title="制作火烧（领用批次）" width="460px">
      <el-descriptions :column="1" border size="small" v-if="current">
        <el-descriptions-item label="批次">{{ current.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="产品">{{ current.productName }}</el-descriptions-item>
        <el-descriptions-item label="剩余">{{ current.remainingKg }} kg</el-descriptions-item>
      </el-descriptions>
      <el-form :model="form" label-width="90px" style="margin-top: 14px">
        <el-form-item label="领用重量"><el-input-number v-model="form.usedWeight" :min="1" :max="current?.remainingKg || 99" :precision="1" style="width: 100%" /></el-form-item>
        <el-form-item label="炉次"><el-input v-model="form.ovenNo" placeholder="3号炉" /></el-form-item>
        <el-form-item label="师傅"><el-input v-model="form.chef" /></el-form-item>
        <el-form-item label="成品数量"><el-input-number v-model="form.qty" :min="1" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" @click="submit">确认领用</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { http } from '@/utils/request'
import { ElMessage } from 'element-plus'

const list = ref<any[]>([])
const loading = ref(false)
const dialog = ref(false); const current = ref<any>(null)
const form = ref({ usedWeight: 10, ovenNo: '', chef: '', qty: 50 })

async function load() { loading.value = true; list.value = await http.get('/shop/inventory'); loading.value = false }
function openOutput(row: any) { current.value = row; form.value = { usedWeight: Math.min(10, row.remainingKg), ovenNo: '3号炉', chef: '', qty: 50 }; dialog.value = true }
async function submit() { await http.post('/shop/outputs', { ...form.value, batchNo: current.value.batchNo, shopOrgName: current.value.holderOrgName }); ElMessage.success('产出记录已生成（领用上链）'); dialog.value = false; load() }
onMounted(load)
</script>

<style scoped>
.warn { color: var(--c-warning); }
</style>
