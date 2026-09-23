<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h3 class="page-title"><span class="bar"></span>召回管理</h3>
        <p class="page-subtitle">输入批次号计算影响范围（下游批次 / 门店 / 已绑定码 / 已扫码），确认后链码 RecallBatch 递归标记</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openAdd">发起召回</el-button>
    </div>

    <el-card class="card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="batchNo" label="召回批次" width="160" />
        <el-table-column prop="reason" label="召回原因" min-width="200" />
        <el-table-column prop="initiator" label="发起方" width="130" />
        <el-table-column label="影响范围" min-width="220">
          <template #default="{ row }">
            <span v-if="row.scopeJson">门店 {{ JSON.parse(row.scopeJson).shops.length }} 家 · 码 {{ JSON.parse(row.scopeJson).codes }} 枚</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发起时间" width="170" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.status === 'RECALLING' ? 'warning' : 'success'" size="small">{{ row.status === 'RECALLING' ? '召回中' : '已完成' }}</el-tag></template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog" title="发起召回" width="480px">
      <el-form :model="form">
        <el-form-item label="批次号"><el-input v-model="form.batchNo" placeholder="F-20260911-001" /></el-form-item>
        <el-form-item label="召回原因"><el-input v-model="form.reason" type="textarea" :rows="3" placeholder="如：抽检瘦肉精超标" /></el-form-item>
      </el-form>
      <el-alert type="warning" :closable="false" title="确认后将递归标记该批次及其下游全部码，相关组织工作台将收到红色提醒" />
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="danger" @click="submit">确认召回</el-button></template>
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
const form = ref({ batchNo: '', reason: '' })

async function load() { loading.value = true; const r = await http.get('/regulator/recalls', { page: 1, size: 50 }); list.value = r.records; loading.value = false }
function openAdd() { form.value = { batchNo: '', reason: '' }; dialog.value = true }
async function submit() { await http.post('/regulator/recalls', form.value); ElMessage.success('召回已发起，RecursiveBatch 标记完成'); dialog.value = false; load() }
onMounted(load)
</script>
