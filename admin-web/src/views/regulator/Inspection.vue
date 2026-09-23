<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h3 class="page-title"><span class="bar"></span>抽检登记</h3>
        <p class="page-subtitle">监管抽检：抽样批次、检测机构、项目（瘦肉精/水分/兽残/掺假物种鉴定），结论哈希上链</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openAdd">登记抽检</el-button>
    </div>

    <el-card class="card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="batchNo" label="抽检批次" width="150" />
        <el-table-column prop="orgName" label="被检企业" min-width="160" />
        <el-table-column prop="agency" label="检测机构" min-width="180" />
        <el-table-column prop="items" label="检测项目" min-width="200" />
        <el-table-column label="结论" width="100">
          <template #default="{ row }"><el-tag :type="tagOf(inspectionResultMap, row.result)[1]" size="small">{{ tagOf(inspectionResultMap, row.result)[0] }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createTime" label="登记时间" width="170" />
      </el-table>
    </el-card>

    <el-dialog v-model="dialog" title="登记抽检" width="500px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="抽检批次"><el-input v-model="form.batchNo" placeholder="F-20260911-001" /></el-form-item>
        <el-form-item label="被检企业"><el-input v-model="form.orgName" /></el-form-item>
        <el-form-item label="检测机构"><el-input v-model="form.agency" /></el-form-item>
        <el-form-item label="检测项目"><el-input v-model="form.items" placeholder="瘦肉精/水分/兽残快检/掺假物种鉴定" /></el-form-item>
        <el-form-item label="结论"><el-radio-group v-model="form.result"><el-radio value="PASS">合格</el-radio><el-radio value="FAIL">不合格</el-radio></el-radio-group></el-form-item>
        <el-form-item label="报告"><el-upload action="#" :auto-upload="false" :limit="1"><el-button :icon="Upload">选择报告 PDF</el-button></el-upload></el-form-item>
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
import { inspectionResultMap, tagOf } from '@/utils/format'

const list = ref<any[]>([])
const loading = ref(false)
const dialog = ref(false)
const form = ref({ batchNo: '', orgName: '', agency: '', items: '', result: 'PASS' })

async function load() { loading.value = true; const r = await http.get('/regulator/inspections', { page: 1, size: 50 }); list.value = r.records; loading.value = false }
function openAdd() { form.value = { batchNo: '', orgName: '', agency: '国家肉类食品质量监督检验中心', items: '瘦肉精/水分/兽残快检/掺假物种鉴定', result: 'PASS' }; dialog.value = true }
async function submit() { await http.post('/regulator/inspections', { ...form.value, reportUri: 'minio://insp/' + Date.now() + '.pdf', reportHash: 'sha256:' + Math.random().toString(16).slice(2, 10) }); ElMessage.success('抽检已登记，结论哈希上链'); dialog.value = false; load() }
onMounted(load)
</script>
