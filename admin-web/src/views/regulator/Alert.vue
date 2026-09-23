<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h3 class="page-title"><span class="bar"></span>预警中心</h3>
        <p class="page-subtitle">温度超标 / 证照到期 / 超期 / 拒收 / 风控等预警，按级别通知并支持处置闭环</p>
      </div>
    </div>

    <div class="filter">
      <el-select v-model="level" placeholder="级别" clearable style="width: 130px" @change="load">
        <el-option label="提示" value="INFO" /><el-option label="警告" value="WARN" /><el-option label="严重" value="CRITICAL" />
      </el-select>
      <el-select v-model="status" placeholder="状态" clearable style="width: 130px" @change="load">
        <el-option label="待处理" value="OPEN" /><el-option label="已处理" value="RESOLVED" /><el-option label="误报" value="FALSE_POSITIVE" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="load">查询</el-button>
    </div>

    <el-card class="card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="级别" width="90">
          <template #default="{ row }"><el-tag :type="tagOf(alertLevelMap, row.level)[1]" size="small">{{ tagOf(alertLevelMap, row.level)[0] }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="alertType" label="类型" width="140" />
        <el-table-column prop="content" label="内容" min-width="320" />
        <el-table-column prop="createTime" label="触发时间" width="170" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="tagOf(alertStatusMap, row.status)[1]" size="small">{{ tagOf(alertStatusMap, row.status)[0] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.status !== 'OPEN'" @click="handle(row)">处置</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog" title="预警处置" width="440px">
      <el-form :model="handleForm">
        <el-form-item label="处理意见"><el-input v-model="handleForm.handleNote" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="处置结果">
          <el-radio-group v-model="handleForm.status"><el-radio value="RESOLVED">已处理</el-radio><el-radio value="FALSE_POSITIVE">误报</el-radio></el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" @click="submitHandle">提交</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { http } from '@/utils/request'
import { ElMessage } from 'element-plus'
import { alertLevelMap, alertStatusMap, tagOf } from '@/utils/format'

const list = ref<any[]>([])
const loading = ref(false)
const dialog = ref(false); const current = ref<any>(null)
const level = ref(''); const status = ref('')
const handleForm = ref({ handleNote: '', status: 'RESOLVED' })

async function load() { loading.value = true; const r = await http.get('/alerts', { page: 1, size: 50, level: level.value || undefined, status: status.value || undefined }); list.value = r.records; loading.value = false }
function handle(row: any) { current.value = row; handleForm.value = { handleNote: '', status: 'RESOLVED' }; dialog.value = true }
async function submitHandle() { await http.put(`/alerts/${current.value.id}/handle`, { ...handleForm.value, handler: '当前用户' }); ElMessage.success('处置已记录'); dialog.value = false; load() }
onMounted(load)
</script>

<style scoped>
.filter { display: flex; gap: 10px; margin-bottom: 14px; flex-wrap: wrap; }
</style>
