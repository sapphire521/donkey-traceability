<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h3 class="page-title"><span class="bar"></span>操作日志</h3>
        <p class="page-subtitle">AOP 全量记录（谁 / 何时 / 何接口 / 参数摘要 / 结果），保留 ≥3 年</p>
      </div>
    </div>

    <el-card class="card">
      <div class="filter">
        <el-input v-model="kw" placeholder="接口 / 参数关键字" clearable style="width: 220px" :prefix-icon="Search" @keyup.enter="load" />
        <el-select v-model="module" placeholder="模块" clearable style="width: 130px" @change="load">
          <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
      </div>
      <el-table :data="list" v-loading="loading" stripe max-height="560">
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column prop="userName" label="操作人" width="110" />
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column prop="operation" label="操作" width="100" />
        <el-table-column prop="method" label="方法" width="80"><template #default="{ row }"><el-tag size="small" :type="row.method === 'GET' ? 'info' : 'success'">{{ row.method }}</el-tag></template></el-table-column>
        <el-table-column prop="params" label="参数摘要" min-width="220" show-overflow-tooltip />
        <el-table-column label="结果" width="90"><template #default="{ row }"><el-tag :type="row.resultCode === 0 ? 'success' : 'danger'" size="small">{{ row.resultCode === 0 ? '成功' : '失败' }}</el-tag></template></el-table-column>
        <el-table-column prop="costMs" label="耗时" width="90"><template #default="{ row }">{{ row.costMs }} ms</template></el-table-column>
        <el-table-column prop="ip" label="IP" width="130" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { http } from '@/utils/request'

const list = ref<any[]>([])
const loading = ref(false)
const kw = ref(''); const module = ref('')
const modules = ['认证', '组织', '用户', '驴只', '批次', '运输', '门店', '溯源', '预警', '系统']

async function load() { loading.value = true; const r = await http.get('/system/logs', { page: 1, size: 50, keyword: kw.value || undefined, module: module.value || undefined }); list.value = r.records; loading.value = false }
onMounted(load)
</script>

<style scoped>
.filter { display: flex; gap: 10px; margin-bottom: 14px; flex-wrap: wrap; }
</style>
